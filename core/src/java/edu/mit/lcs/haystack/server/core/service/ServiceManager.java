/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.mit.lcs.haystack.server.core.service;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.proxy.ProxyManager;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.security.Identity;
import edu.mit.lcs.haystack.security.IdentityManager;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Manages services in the Haystack server.
 * @author Dennis Quan
 */
public class ServiceManager extends ProxyManager {
	
	protected Resource m_baseRes;
	protected String m_basePath;
	protected Hashtable m_instanceMap = new Hashtable();
	protected HashSet m_instances = new HashSet();
	protected boolean m_shuttingDown = false;
	protected boolean m_startingUp = true;
	protected Thread m_startupThread;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ServiceManager.class);
	static org.apache.log4j.Logger s_logger2 = org.apache.log4j.Logger.getLogger(ServiceManager.class.getName() + ".2");
	
	public ServiceManager(String basePath, IRDFContainer root, Resource baseRes, IdentityManager im) {
		super(root, im);
		this.m_baseRes = baseRes;
		this.m_basePath = basePath;
	}
	
	public String getBasePath() { return m_basePath; }
	
	/**
	 * Returns the resource that identifies this server instance.
	 */
	public Resource getResource() { return m_baseRes; }
	
	/**
	 * Starts the service manager.
	 */
	public void start() throws Exception {
		s_logger.info("Service manager " + m_baseRes + " starting up");
		m_startupThread = Thread.currentThread();
		Resource[] servicesToStart = Utilities.getResourceProperties(m_baseRes, Constants.s_config_hostsService, m_root);
		// Perform transfer if necessary
		Resource transferRes = Utilities.getResourceProperty(m_baseRes, Constants.s_config_transferTo, m_root);
		Resource earlyRes = Utilities.getResourceProperty(m_baseRes, Constants.s_config_startEarly, m_root);
		if (transferRes != null) {
			IRDFContainer oldRoot = m_root;
			m_root = (IRDFContainer) connectToService(transferRes, null);
			// Transfer bootstrap data over
			m_root.add(oldRoot);
			// Start services defined in bootstrap first
			if (earlyRes != null) getService(earlyRes);
			for (int j = 0; j < servicesToStart.length; j++) getService(servicesToStart[j]);
		}
		startServices();
	}
	
	/**
	 * Starts all services.
	 */
	public void startServices() throws Exception {
		// Listen for service additions
		if (m_root instanceof IRDFEventSource) {
			RDFListener rdfl = new RDFListener(this, (IRDFEventSource)m_root) {
				public void statementAdded(Resource cookie, Statement s) {
					try {
						Resource service = (Resource)s.getObject();
						if (!service.equals(getServiceResource())) getService(service);
					} catch (Exception e) {
						s_logger2.error("An error occurred trying to start service " + s.getObject(), e);
					}
				}
				
				public void statementRemoved(Resource cookie, Statement st) {
					try {
						Resource service = (Resource)st.getObject();
						if (!service.equals(getServiceResource())) {
							synchronized (this) {
								if (Utilities.checkBooleanProperty(service, Constants.s_config_singleton, m_root)) {
									Object o = m_instanceMap.get(service);
									if (o != null) {
										IService s = getService(service);
										s.cleanup();
										s.shutdown();
										m_instanceMap.remove(service);
									}
								} else {
									Thread t = Thread.currentThread();
									Hashtable h = (Hashtable) m_instanceMap.get(t);
									if (h == null) return;

									IService s = (IService)h.get(service);
									if (s != null) {
										s.cleanup();
										s.shutdown();
										h.remove(service);
									}
								}
							}
						}
					} catch (Exception e) {
						s_logger.error("An error occurred trying to stop service " + st.getObject(), e);
					}
				}
			};
			
			rdfl.addPattern(m_baseRes, Constants.s_config_hostsService, null);
			rdfl.start();
		}
		
		Resource[] services = Utilities.getResourceProperties(m_baseRes, Constants.s_config_hostsService, m_root);
		for (int j = 0; j < services.length; j++) {
			Resource serviceRes = services[j];
			try { getService(serviceRes); }
			catch (Exception e) { s_logger2.error("Failed to instantiate service " + serviceRes, e); }
		}
		
		m_startingUp = false;
	}
	
	/**
	 * Returns an instance of the service.
	 */
	public Object connectToService(Resource res, Identity id) throws Exception {
		if (res == null || m_shuttingDown) return null;
		Object o = getSingletonServiceInstance(res);
		if (o != null) return o;
		
		// Confirm the service is on this service
		if (!m_root.contains(new Statement(m_baseRes, Constants.s_config_hostsService, res))) {
			return super.connectToService(res, id);
		} else {
			o = getService(res);
			if (o instanceof ISessionBasedService) {
				ISessionBasedService sbs = (ISessionBasedService) o;
				String className = sbs.getClientClassName();
				Class c = CoreLoader.loadClass(className);
				if (c == null) throw new Exception("Could not load class " + className);
				Constructor[] constructors = c.getConstructors();
				for (int i = 0; i < constructors.length; i++) {
					Class[] params = constructors[i].getParameterTypes();
					if (params.length == 2 && params[1].equals(Identity.class))
						return constructors[i].newInstance(new Object[] { o, id });
				}
			}
			
			return o;
		}
	}
	
	Hashtable m_classes = new Hashtable();
	
	// Called by EventListenerService.start()
	public void setSingletonServiceInstance(Resource res, IService service, Class c) throws ServiceException {
		if (m_shuttingDown) return;		
		synchronized (this) {
			service.init(m_basePath + "data" + File.separatorChar, this, res);
			m_instanceMap.put(res, service);
			m_instances.add(service);
			m_classes.put(res, c);
		}
	}

	// Called by EventListenerService.stop()
	public void removeSingletonServiceInstance(Resource res) throws ServiceException {
		synchronized (this) {
			IService service = (IService) m_instanceMap.get(res);
			if (service != null) {
				service.shutdown();
				m_instanceMap.remove(res);
				m_instances.remove(service);
			}
		}
	}
	
	//called by connectToService
	public IService getSingletonServiceInstance(Resource res) throws ServiceException {
		synchronized (this) {
			IService service = (IService) m_instanceMap.get(res);
			if (service != null) return service;
		}
		return null;
	}
	
	protected IService instantiateService(Resource res) throws ServiceException {
		if (m_shuttingDown) return null;
		IService service = null;
		try {
			Class serviceClass = Utilities.loadClass(res, m_root);
			service = (IService)serviceClass.newInstance();
		} catch (Exception e) {
			// TODO[dquan]: implement code download
		}
		if (service == null) {
			throw new ServiceException("Unable to instantiate service " + res.getURI() + ".");
		}
		
		StringBuffer sb = new StringBuffer();
		String str = res.getURI();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (Character.isLetterOrDigit(ch)) sb.append(ch);
		}
		String filename = m_basePath + "data" + File.separatorChar + sb.toString() + File.separatorChar;
		File file = new File(filename);
		try { file.mkdirs(); }
		catch (Exception e) {}
		
		s_logger.info("Instantiating service " + service + " " + res);
		service.init(filename, this, res);
		
		// Register this instance
		synchronized (this) { m_instances.add(service); }
		
		return service;
	}
	
	public IService getService(Resource res) throws ServiceException, RDFException {
		if (m_shuttingDown) return null;
		Hashtable h = null;
		if (Utilities.checkBooleanProperty(res, Constants.s_config_singleton, m_root)) {
			if (m_startingUp && m_startupThread != Thread.currentThread()) {
				while (m_startingUp) {
					try { Thread.sleep(100); }
					catch (InterruptedException e) { }
				}
			}
			
			while (true) {
				synchronized (this) {
					Object o = m_instanceMap.get(res);
					if (o != null && o != this) return (IService) o;
					if (o == this) {
						try { wait(); }
						catch (InterruptedException ie) {}
					} else {
						// Indicate that we are working on it
						m_instanceMap.put(res, this);
						break;
					}
				}
			}
			
			IService s = null;
			try {
				s = instantiateService(res);
				synchronized (this) { m_instanceMap.put(res, s); }
				return s;
			} finally {
				synchronized (this) {
					if (s == null) m_instanceMap.remove(res);
					notifyAll();
				}
			}
		} else {
			Thread t = Thread.currentThread();
			h = (Hashtable) m_instanceMap.get(t);
			if (h == null) {
				h = new Hashtable();
				m_instanceMap.put(t, h);
			}
			
			IService s = (IService)h.get(res);
			if (s != null) return s;
		}
		
		IService s = instantiateService(res);
		h.put(res, s);
		return s;
	}
	
	public void stop() throws Exception {
		s_logger.info("Service manager " + m_baseRes + " shutting down...");
		
		while (true) {
			m_shuttingDown = false;
			
			HashSet instances;
			Iterator i;
			synchronized (m_instanceMap) {
				instances = (HashSet) m_instances.clone();
				i = instances.iterator();
				if (!i.hasNext()) break;
			}
			
			while (i.hasNext()) {
				try { ((IService) i.next()).cleanup(); }
				catch (Exception e2) { s_logger.error("Error occurred cleaning up service", e2); }
			}
			m_shuttingDown = true;
			
			i = instances.iterator();
			while (i.hasNext()) {
				try { ((IService) i.next()).shutdown(); }
				catch (Exception e2) { s_logger.error("Error occurred shutting down service", e2); }
			}
			
			synchronized (m_instanceMap) { m_instances.removeAll(instances); }
		}
		m_instances.clear();
		m_instanceMap.clear();
	} 
	
	/**
	 * @see edu.mit.lcs.haystack.proxy.IServiceAccessor#isShuttingDown()
	 */
	public boolean isShuttingDown() { return m_shuttingDown; }
	
}
