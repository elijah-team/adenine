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

import java.util.Iterator;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @author David Huynh
 */
public class ProviderConstructorDataProvider extends ChainedDataProvider {
	IDataProvider	m_constructedDataProvider;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ProviderConstructorDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	protected boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource == null) {
			return;
		}

		setupSources(source, context);
		
		internalInitialize(source, context, true);
		
		m_initializing = false;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				if (m_constructedDataProvider == null) {
					makeDataProvider(newResource);
				} else {
					onResourceDeleted(null);
					makeDataProvider(newResource);
					if (m_constructedDataProvider != null) {
						Iterator i = m_consumers.iterator();
						while (i.hasNext()) {
							IDataConsumer dc = (IDataConsumer) i.next();
							m_constructedDataProvider.registerConsumer(dc);
						}
					}
				}
			}
	
			protected void onResourceDeleted(Resource previousResource) {
				if (m_constructedDataProvider != null) {
					m_constructedDataProvider.dispose();
					m_constructedDataProvider = null;
						
					Iterator i = m_consumers.iterator();
					while (i.hasNext()) {
						IDataConsumer dc = (IDataConsumer) i.next();
						// TODO: generalize
						try {
							dc.onDataChanged(DataConstants.SET_CLEAR, null);
						} catch (Exception e) {}
						try {
							dc.onDataChanged(DataConstants.LIST_CLEAR, null);
						} catch (Exception e) {}
						try {
							dc.onDataChanged(DataConstants.INTEGER_DELETION, null);
						} catch (Exception e) {}
						try {
							dc.onDataChanged(DataConstants.STRING_DELETION, null);
						} catch (Exception e) {}
					}
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.dispose();
			m_constructedDataProvider = null;
		}
		super.dispose();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (m_constructedDataProvider != null) {
			return m_constructedDataProvider.getData(dataType, specifications);
		}
		
		throw new DataNotAvailableException("No constructed data provider");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.registerConsumer(dataConsumer);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.requestChange(changeType, change);
		} else {
			throw new UnsupportedOperationException("No constructed data provider");
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		if (m_constructedDataProvider != null) {
			return m_constructedDataProvider.supportsChange(changeType);
		}
		return false;
	}
	
	void makeDataProvider(Resource resource) {
		if (m_constructedDataProvider == null) {
			m_constructedDataProvider = DataUtilities.createDataProvider(resource, m_context, m_partDataSource, m_partDataSource);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.initializeFromDeserialization(source);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(Statement)
	 */
	protected void onStatementAdded(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}
}
