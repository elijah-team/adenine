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

/*
 * Created on Jun 28, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class ContextHostedResourceDataProvider extends GenericDataProvider {
	protected Resource m_lastResource = null;
	protected Resource m_contextPropertyName = null;
	transient protected IDataProvider m_contextProvider = null;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		m_contextProvider.requestChange(changeType, change);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		Resource lastResource = m_lastResource;
		if (lastResource != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, lastResource);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.RESOURCE)) {
			return m_lastResource;
		} else {
			return null;
		}
	}
	
	protected void setupConsumer() {
		m_contextProvider.registerConsumer(new ContextHostedResourceConsumer(this));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.setupSources(source, context);
		
		m_contextPropertyName = Utilities.getResourceProperty(m_prescription, DataConstants.s_property, m_partDataSource);
		if (m_contextPropertyName != null) {
			m_contextProvider = (IDataProvider) m_context.getProperty(m_contextPropertyName);
			if (m_contextProvider != null) {
				setupConsumer();
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_contextPropertyName != null) {
			m_contextProvider = (IDataProvider) m_context.getProperty(m_contextPropertyName);
			Resource lastResource = m_lastResource;
			if (m_contextProvider != null) {
				m_lastResource = null;
				setupConsumer();
				
				if (m_lastResource == null && lastResource != null) {
					notifyDataConsumers(DataConstants.RESOURCE_DELETION, lastResource);
				}
			}
		}
	}
}

class ContextHostedResourceConsumer extends ResourceDataConsumer {
	transient ContextHostedResourceDataProvider m_provider = null;
	
	ContextHostedResourceConsumer() {
	}
	
	ContextHostedResourceConsumer(ContextHostedResourceDataProvider provider) {
		m_provider = provider;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
	 */
	protected void onResourceChanged(Resource newResource) {
		if (m_provider != null) {
			synchronized (m_provider) {
				m_provider.m_lastResource = newResource;
			}
			m_provider.notifyDataConsumers(DataConstants.RESOURCE_CHANGE, newResource);
		}
	}
			
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
	 */
	protected void onResourceDeleted(Resource previousResource) {
		if (m_provider != null) {
			synchronized (m_provider) {
				m_provider.m_lastResource = null;
			}
			m_provider.notifyDataConsumers(DataConstants.RESOURCE_DELETION, previousResource);
		}
	}
}