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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.ozone.core.INavigationMaster;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IViewNavigator;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.server.standard.serine.ISerineAgent;
import edu.mit.lcs.haystack.server.standard.serine.SerineConstants;

import java.util.*;

/**
 * @author David Huynh
 * @author Dennis Quan
 */
public class AdenineViewNavigatorPart extends AdeninePartContainerPart implements IViewNavigator, IDataProvider {
	ArrayList			m_backResources = new ArrayList();
	ArrayList			m_forwardResources = new ArrayList();
	Resource			m_currentResource = null;
	Resource			m_currentResourceView = null;
	Resource			m_homeResource;
	Resource			m_onNavigate;
	Object				m_cookie;
	IViewContainerPart	m_viewContainer;
	ArrayList			m_dataConsumers = new ArrayList();
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineViewNavigatorPart.class);

	final static Resource s_home = new Resource(OzoneConstants.s_namespace + "home");
	
	public AdenineViewNavigatorPart() {
		super();
	}

	protected void getInitializationData() {
		super.getInitializationData();	
		// Look up home resource
		m_homeResource = Utilities.getResourceProperty(m_context.getUserResource(), s_home, m_infoSource);
		// Look up onNavigate handler
		m_onNavigate = Utilities.getResourceProperty(m_prescription, PartConstants.s_onNavigate, m_partDataSource);
	}
	
	protected void internalInitialize() {
		m_context.putProperty(OzoneConstants.s_viewNavigator, this);
		super.internalInitialize();
		INavigationMaster master = (INavigationMaster) m_context.getProperty(OzoneConstants.s_navigationMaster);
		m_cookie = master.registerViewNavigator(m_prescription, this);
	}
	
	public void dispose() {
		INavigationMaster master = (INavigationMaster) m_context.getProperty(OzoneConstants.s_navigationMaster);
		if (master != null) master.unregisterViewNavigator(m_cookie);
		m_cookie = null;
		
		m_backResources.clear();
		m_forwardResources.clear();
		
		m_backResources = null;
		m_forwardResources = null;
		m_currentResource = null;
		m_currentResourceView = null;
		m_homeResource = null;
		
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#refresh()
	 */
	public void refresh() {
		if (m_viewContainer != null) {
			s_logger.info("Refreshing embedded view container");
			m_viewContainer.refresh();
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#back()
	 */
	public void back() {
		if (m_backResources.size() == 0) return;
		m_forwardResources.add(m_currentResource);
		setCurrentResource((Resource)m_backResources.remove(m_backResources.size() - 1), null);
	}
	
	public Resource getBack(int i) {
		if (i > m_backResources.size() - 1) return null;
		return (Resource) m_backResources.get(m_backResources.size() - 1 - i);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#forward()
	 */
	public void forward() {
		if (m_forwardResources.size() == 0) return;
		m_backResources.add(m_currentResource);
		setCurrentResource((Resource)m_forwardResources.remove(m_forwardResources.size() - 1), null);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#home()
	 */
	public void home() { 
		requestNavigation(m_homeResource, null, null); 
		}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#notifyNavigation(Resource)
	 */
	public void notifyNavigation(Resource underlying) {
		if (underlying.equals(m_currentResource)) return;
		if (m_currentResource != null) {
			m_backResources.add(m_currentResource);
			m_forwardResources.clear();
		}
		m_currentResource = underlying;
		handleNavigationRecord(underlying);
		notifyDataConsumers();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#getCurrentResource()
	 */
	public Resource getCurrentResource() { return m_currentResource; }

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#getCurrentViewInstance()
	 */
	public Resource getCurrentViewInstance() {
		return m_viewContainer.getCurrentViewInstance();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#getNavigationDataProvider()
	 */
	public IDataProvider getNavigationDataProvider() { return this; }

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#registerViewContainer(IViewContainerPart)
	 */
	public void registerViewContainer(IViewContainerPart vcp) {
		if (m_viewContainer == null && vcp != null) m_viewContainer = vcp;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#unregisterViewContainer(IViewContainerPart)
	 */
	public void unregisterViewContainer(IViewContainerPart vcp) {
		if (vcp == m_viewContainer) m_viewContainer = null;
	}

	/** 
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#getViewContainer()
	 */
	public IViewContainerPart getViewContainer() { return m_viewContainer; }
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator#requestNavigation(Resource, Resource, Resource)
	 */
	public void requestNavigation(Resource underlying, Resource viewPartClass, Resource viewInstance) {
		if (m_currentResource != null) {
			m_backResources.add(m_currentResource);
			m_forwardResources.clear();
		}
		handleNavigationRecord(underlying);
		setCurrentResource(underlying, viewInstance);
	}
	
	private void handleNavigationRecord(Resource underlying) {
		if (m_onNavigate != null) {
			try {
				Interpreter 		interpreter = Ozone.getInterpreter();
				DynamicEnvironment	denv = new DynamicEnvironment(m_source);
				Ozone.initializeDynamicEnvironment(denv, m_context);
				interpreter.callMethod(m_onNavigate, new Object[] { underlying }, denv);
			} catch (AdenineException e) { }
		}

		// Prompt Serine to act on this resource
		try {
			RDFNode[] serine = m_source.queryExtract(new Statement[] {
				new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, SerineConstants.SerineAgent),
				new Statement(Utilities.generateWildcardResource(1), Constants.s_haystack_user, m_context.getUserResource())
			}, Utilities.generateWildcardResourceArray(1),  Utilities.generateWildcardResourceArray(1));
			if (serine != null) {
				ISerineAgent sa = (ISerineAgent)m_context.getServiceAccessor().connectToService((Resource)serine[0], m_context.getUserIdentity());
				sa.processSubjectImmediately(underlying);
			}
		} catch (Exception e) {
			s_logger.error("Failed to inform Serine agent of currently viewed resource", e);
		}
	}
	
	/**
	 * Returns the current resource if the requested type is <code>DataConstants.RESOURCE</code>.
	 * Otherwise returns null.
	 * 
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (m_currentResource == null)
			throw new DataNotAvailableException("No current resource is set");
		if (dataType.equals(DataConstants.RESOURCE)) return m_currentResource;
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() { return null; }

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		throw new UnsupportedOperationException("AdenineViewNavigatorPart data provider does not support any change request");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) { return false; }
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null && !m_dataConsumers.contains(dataConsumer)) {
			m_dataConsumers.add(dataConsumer);
			dataConsumer.reset();
			if (m_currentResource != null)
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_currentResource);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null) m_dataConsumers.remove(dataConsumer);
	}

	protected void setCurrentResource(Resource res, Resource resView) {
		m_currentResource = res;
		m_currentResourceView = resView;
		if (m_viewContainer != null) {
			s_logger.info("Navigating embedded view container to " + res.getURI());
			
			try {
				Resource properties = (Resource) m_source.extract(m_currentResource, PartConstants.s_navigatorProperties, null);
				if (properties == null) {
					properties = Utilities.generateUniqueResource();
					m_source.add(new Statement(m_currentResource, PartConstants.s_navigatorProperties, properties));
				}
			} catch (RDFException e) { }
			m_viewContainer.navigate(res, resView);
			notifyDataConsumers();
		} else {
			s_logger.info("No embedded view container to navigate to " + res.getURI());
		}
	}
	
	protected void notifyDataConsumers() {
		Iterator i = m_dataConsumers.iterator();
		while (i.hasNext()) {
			if (m_currentResource != null) 
				((IDataConsumer)i.next()).onDataChanged(DataConstants.RESOURCE_CHANGE, m_currentResource);
			else 
				((IDataConsumer)i.next()).onDataChanged(DataConstants.RESOURCE_DELETION, null);
		}
	}
}
