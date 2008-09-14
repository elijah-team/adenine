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

package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.INavigationMaster;
import edu.mit.lcs.haystack.ozone.core.IViewNavigator;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class NavigationPropertiesDataProvider extends GenericPart implements IDataProvider {
	IDataProvider			m_dataProvider;
	ResourceDataConsumer	m_dataConsumer;
	ArrayList				m_dataConsumers = new ArrayList();
	Resource				m_properties;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(NavigationPropertiesDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		IViewNavigator viewNavigator;
		Resource resViewNavigator = Utilities.getResourceProperty(m_prescription, NavigationDataProvider.VIEW_NAVIGATOR, m_partDataSource);
		if (resViewNavigator != null) {
			INavigationMaster nm = (INavigationMaster)context.getProperty(OzoneConstants.s_navigationMaster);
			viewNavigator = (IViewNavigator)nm.getViewNavigator(resViewNavigator);
		} else {
			viewNavigator = (IViewNavigator) context.getProperty(OzoneConstants.s_viewNavigator);
		}
		if (viewNavigator != null) {
			m_dataProvider = viewNavigator.getNavigationDataProvider();
			m_dataConsumer = new ResourceDataConsumer() {
				protected void onResourceChanged(Resource newResource) {
					setCurrentResource(newResource);
				}
			
				protected void onResourceDeleted(Resource previousResource) {
					setCurrentResource(null);
				}
			};
			m_dataProvider.registerConsumer(m_dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		if (m_dataProvider != null) {
			m_dataProvider.unregisterConsumer(m_dataConsumer);
			m_dataProvider = null;
			m_dataConsumer = null;
		}
		m_properties = null;
		
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null) {
			m_dataConsumers.add(dataConsumer);
			
			dataConsumer.reset();
			if (m_properties != null) {
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_properties);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null) {
			m_dataConsumers.remove(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.RESOURCE) && m_properties != null) {
			return m_properties;
		}
		throw new DataNotAvailableException("Navigation properties data provider has no data");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		throw new UnsupportedOperationException("Navigation properties data provider supports no change");
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
		
	protected void setCurrentResource(Resource resource) {
		Resource properties = null;
		
		if (resource != null) {
			try {
				properties = (Resource) m_source.extract(resource, PartConstants.s_navigatorProperties, null);
			} catch (RDFException e) {
				s_logger.error("Failed to extract navigator properties for " + resource, e);
			}
		}
		
		if (properties != m_properties) {
			if (properties != null) {
				notifyDataConsumers(DataConstants.RESOURCE_CHANGE, properties);
			} else {
				notifyDataConsumers(DataConstants.RESOURCE_DELETION, m_properties);
			}
			m_properties = properties;
		}
	}

	protected void notifyDataConsumers(Resource changeType, Resource change) {
		Iterator i = m_dataConsumers.iterator();
		while (i.hasNext()) {
			((IDataConsumer) i.next()).onDataChanged(changeType, change);
		}
	}
}
