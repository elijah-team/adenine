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
import edu.mit.lcs.haystack.rdf.FederationRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class GenericService implements IService {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(GenericService.class);

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
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IService#shutdown()
	 */
	public void shutdown() throws ServiceException {
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IPersistent#getServiceResource()
	 */
	public Resource getServiceResource() {
		return m_serviceResource;
	}

	public ServiceManager getServiceManager() {
		return m_serviceManager;
	}
	
	public IRDFContainer getInfoSource() {
		return m_infoSource;
	} 
	
	public Resource getUserResource() {
		return m_userResource;
	} 
}
