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

import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class EventListenerService implements IService {
	protected ServiceManager m_manager;
	Resource m_resService;
	LocalRDFContainer m_serviceMetadata;

	/**
	 * Returns the interface name for this event listener.
	 * Default implementation checks the implemented interfaces list
	 * and chooses the first one that isn't IService.
	 */
	protected String getInterfaceName() {
		try {
			Class[] a = getClass().getInterfaces();
			for (int i = 0; i < a.length; i++) {
				if (a[i] != IService.class) return a[i].getName();
			}
		} catch (Exception e) {}
		throw new RuntimeException("Event listener interface ill-defined.");
	}

	public EventListenerService(ServiceManager sm) {
		m_resService = new Resource("urn:eventlistener:" + Utilities.generateUniqueIdentifier());
		m_manager = sm;

		// Create initial metadata
/*		IRDFContainer rdfc = m_manager.getRootRDFContainer();
		Resource resPort = Utilities.generateUniqueResource();
		Resource resPortType = Utilities.generateUniqueResource();
		Resource resBinding = Utilities.generateUniqueResource();
		Resource resJavaInterface = Utilities.generateUniqueResource();
		m_serviceMetadata = new LocalRDFContainer(new Statement[] {
			new Statement(m_manager.getResource(), Constants.s_config_hostsService, m_resService),
//			new Statement(m_resService, Constants.s_rdf_type, Constants.s_config_Service),
			new Statement(m_resService, Constants.s_config_singleton, new Literal("true")),
//			new Statement(m_resService, Constants.s_wsdl_port, resPort),
//			new Statement(resPort, Constants.s_rdf_type, Constants.s_wsdl_Port),
//			new Statement(resPort, Constants.s_wsdl_binding, resBinding),
//			new Statement(resBinding, Constants.s_rdf_type, Constants.s_wsdl_Binding),
//			new Statement(resBinding, Constants.HTTP_VERB, new Literal("GET")), 
//			new Statement(resBinding, Constants.s_wsdl_type, resPortType),
//			new Statement(resPortType, Constants.s_rdf_type, Constants.s_wsdl_PortType),
//			new Statement(resPortType, Constants.s_config_javaInterface, resJavaInterface),
//			new Statement(resJavaInterface, Constants.s_rdf_type, Constants.s_haystack_JavaClass),
//			new Statement(resJavaInterface, Constants.s_haystack_className, new Literal(getInterfaceName()))
		});*/
	}
	
	public Resource getServiceResource() { return m_resService; }
	
	public void start() {
		try {
			// Register service
			//m_manager.getRootRDFContainer().add(m_serviceMetadata);
			
			// Start service
			m_manager.setSingletonServiceInstance(m_resService, this, this.getClass());
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
	}

	/**
	 * @see IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
		// Unregister service
/*		try {
			IRDFContainer rdfc = m_manager.getRootRDFContainer();
			Utilities.removeStatements(rdfc, m_serviceMetadata);
		} catch(RDFException e) {
			throw new ServiceException("RDF error", e);
		}*/
	}

	/**
	 * @see IService#shutdown()
	 */
	public void shutdown() throws ServiceException {
	}

	public void stop() {
		try {
			// Stop service
			m_manager.removeSingletonServiceInstance(m_resService);
			
			cleanup();
			shutdown();
		} catch (Exception e) { e.printStackTrace(); }
	}
	
}
