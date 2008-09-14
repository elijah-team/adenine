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

package edu.mit.lcs.haystack.server.core.rdfstore;

import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.FederationRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.server.rdfstore.Cholesterol3RDFStoreService;

/**
 * @author Dennis Quan
 */
public class ContentBasedRDFStoreService
	extends Cholesterol3RDFStoreService {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ContentBasedRDFStoreService.class);
	
	public static Resource s_mountedInformation = new Resource("http://haystack.lcs.mit.edu/agents/rdfstore#mountedInformation");

	protected String m_basePath;
	protected ServiceManager m_serviceManager;
	protected Resource m_serviceResource;
	protected Resource m_userResource;
	protected IRDFContainer m_infoSource;
	protected Resource m_mountedInformation;
	protected Timer m_timer = new Timer();
	protected boolean m_dirty = false;

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
		super.init(basePath, manager, res);
		
		m_basePath = basePath;
		m_serviceManager = manager;
		m_serviceResource = res;

		IRDFContainer source = manager.getRootRDFContainer();
		m_userResource = Utilities.getResourceProperty(res, Constants.s_haystack_user, source);

		// Find the info source
		m_infoSource = source;
		Resource resInfoSource = Utilities.getResourceProperty(res, Constants.s_config_informationSource, source);
		if (resInfoSource != null) {
			try {
				IRDFContainer rdfc2 = (IRDFContainer)manager.connectToService(resInfoSource, null);
				FederationRDFContainer infoSource = new FederationRDFContainer();
				infoSource.addSource(source, 1);
				infoSource.addSource(rdfc2, 0);
				m_infoSource = infoSource;
			} catch (Exception e) {
				s_logger.error("Service " + res + " could not connect to information source " + resInfoSource + "; using default", e);
			}
		} else if (m_userResource != null) {
			resInfoSource = Utilities.getResourceProperty(m_userResource, Constants.s_config_defaultInformationSource, source);
			FederationRDFContainer infoSource = new FederationRDFContainer();
			infoSource.addSource(source, 1);
			try {
				if (resInfoSource != null) {
					IRDFContainer rdfc2 = (IRDFContainer)manager.connectToService(resInfoSource, null);
					infoSource.addSource(rdfc2, 0);
				}

				Resource[] resInfoSources = Utilities.getResourceProperties(m_userResource, Constants.s_config_secondaryInformationSource, source);
				if (resInfoSources.length != 0) {
					for (int i = 0; i < resInfoSources.length; i++) {
						IRDFContainer rdfc2 = (IRDFContainer)manager.connectToService(resInfoSources[i], null);
						infoSource.addSource(rdfc2, 2);
					}
				}
			} catch (Exception e) {
				s_logger.error("Service " + res + " could not connect to information source " + resInfoSource + "; using default", e);
			}
			m_infoSource = infoSource;
		}

		m_mountedInformation = Utilities.getResourceProperty(m_serviceResource, s_mountedInformation, source);
		load();
		
		m_timer.scheduleAtFixedRate(new TimerTask() {
			/**
			 * @see java.util.TimerTask#run()
			 */
			public void run() {
				save();
			}
		}, 60000, 60000);
	}
	
	synchronized public void load() {
		try {
			LocalRDFContainer rdfc = new LocalRDFContainer();
			Utilities.parseRDF(ContentClient.getContentClient(m_mountedInformation, m_infoSource, m_serviceManager).getContent(), rdfc);
			remove(new Statement(Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2), Utilities.generateWildcardResource(3)), Utilities.generateWildcardResourceArray(3));
			add(rdfc);
			m_dirty = false;
		} catch (Exception e) {
			s_logger.error("Failed to load information", e);
		}
	}
	
	synchronized public void save() {
		if (!m_dirty) {
			return;
		}
		
		try {
			Set set = query(new Statement[] { 
				new Statement(Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2), Utilities.generateWildcardResource(3))
			}, Utilities.generateWildcardResourceArray(3), Utilities.generateWildcardResourceArray(3));
			LocalRDFContainer rdfc = new LocalRDFContainer();
			Iterator i = set.iterator();
			while (i.hasNext()) {
				RDFNode[] datum = (RDFNode[]) i.next();
				rdfc.add(new Statement((Resource) datum[0], (Resource) datum[1], datum[2]));
			}
			ContentClient.getContentClient(m_mountedInformation, m_infoSource, m_serviceManager).setContent(Utilities.generateRDF(rdfc));

			m_dirty = false;
		} catch (Exception e) {
			s_logger.error("Failed to save information", e);
		}
	}

	protected void setDirty() {
		m_dirty = true;
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
		m_timer.cancel();
		save();
		super.cleanup();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.rdfstore.Cholesterol3RDFStoreService#add(edu.mit.lcs.haystack.rdf.Statement)
	 */
	public void add(Statement s) throws RDFException {
		setDirty();
		super.add(s);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.rdfstore.Cholesterol3RDFStoreService#remove(edu.mit.lcs.haystack.rdf.Statement, edu.mit.lcs.haystack.rdf.Resource[])
	 */
	public void remove(Statement pattern, Resource[] existentials)
			throws RDFException {
		setDirty();
		super.remove(pattern, existentials);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.rdfstore.Cholesterol3RDFStoreService#replace(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.RDFNode, edu.mit.lcs.haystack.rdf.RDFNode)
	 */
	public void replace(Resource subject, Resource predicate, RDFNode object,
			RDFNode newValue) throws RDFException {
		setDirty();
		super.replace(subject, predicate, object, newValue);
	}
}
