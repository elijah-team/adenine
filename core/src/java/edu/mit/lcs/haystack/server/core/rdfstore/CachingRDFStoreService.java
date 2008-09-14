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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.FederationRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.Iterator;
import java.util.Set;

/**
 * @author Dennis Quan
 */
public class CachingRDFStoreService
	extends CholesterolRDFStoreService {
		
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(CachingRDFStoreService.class);
	
	public static Resource s_source = new Resource("http://haystack.lcs.mit.edu/agents/rdfstore#source");

	protected String m_basePath;
	protected ServiceManager m_serviceManager;
	protected Resource m_serviceResource;
	protected Resource m_userResource;
	protected IRDFContainer m_infoSource;

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

		synchronize();
	}
	
	synchronized public void synchronize() {
		try {
			Resource[] cacheSources = Utilities.getResourceProperties(m_serviceResource, s_source, m_serviceManager.getRootRDFContainer());
			
			for (int i = 0; i < cacheSources.length; i++) {
				IRDFContainer cacheSource = (IRDFContainer) m_serviceManager.connectToService(cacheSources[i], null);
	
				Set set = cacheSource.query(new Statement[] { 
					new Statement(Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2), Utilities.generateWildcardResource(3))
				}, Utilities.generateWildcardResourceArray(3), Utilities.generateWildcardResourceArray(3));
				LocalRDFContainer rdfc = new LocalRDFContainer();
				Iterator j = set.iterator();
				while (j.hasNext()) {
					RDFNode[] datum = (RDFNode[]) j.next();
					rdfc.add(new Statement((Resource) datum[0], (Resource) datum[1], datum[2]));
				}
				super.add("", rdfc);
			}
		} catch (Exception e) {
			s_logger.error("Failed to synchronize information", e);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.server.rdfstore.IRDFStore#add(java.lang.String, edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void add(String ticket, IRDFContainer c)
		throws ServiceException {
		//super.add(ticket, c);
	}

	/**
	 * @see edu.mit.lcs.haystack.server.rdfstore.IRDFStore#remove(java.lang.String, edu.mit.lcs.haystack.rdf.Statement, edu.mit.lcs.haystack.rdf.Resource)
	 */
	public void remove(String ticket, Statement s, Resource[] existentials)
		throws ServiceException {
		//super.remove(ticket, s, existentials);
	}

	/**
	 * @see edu.mit.lcs.haystack.server.rdfstore.IRDFStore#replace(java.lang.String, edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.RDFNode, edu.mit.lcs.haystack.rdf.RDFNode)
	 */
	public void replace(
		String ticket,
		Resource subject,
		Resource predicate,
		RDFNode object,
		RDFNode newValue)
		throws ServiceException {
		//super.replace(ticket, subject, predicate, object, newValue);
	}

}
