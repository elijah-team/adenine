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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.GenericDataProvider;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;

import java.util.*;

/**
 * @author David Huynh
 */
public class EqualDataProvider extends GenericDataProvider {
	protected ArrayList	m_dataProviders = new ArrayList();
	protected HashMap		m_providerToValue = new HashMap();
	protected boolean		m_initializing = true;
	protected boolean		m_value;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		Iterator i = m_dataProviders.iterator();
		while (i.hasNext()) {
			IDataProvider dp = (IDataProvider) i.next();
			dp.initializeFromDeserialization(source);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		RDFNode[] chainedDataSources = Utilities.getResourceProperties(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
		for (int i = 0; i < chainedDataSources.length; i++) {
			Resource 		chainedDataSource = (Resource) chainedDataSources[i];
			IDataProvider	dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_partDataSource, m_partDataSource);
		
			if (dataProvider != null) {
				IDataConsumer dataConsumer = createDataConsumer(dataProvider);

				m_dataProviders.add(dataProvider);
				m_providerToValue.put(dataProvider, null);
				
				dataProvider.registerConsumer(dataConsumer);
			}
		}
		refresh();
		
		m_initializing = false;
	}
	
	protected IDataConsumer createDataConsumer(IDataProvider dataProvider) {
		return new IDataConsumer() {
			IDataProvider m_dataProvider;
			
			public void reset() {
			}

			public void onDataChanged(Resource changeType, Object change)
				throws IllegalArgumentException {
					
				if (
					changeType.equals(DataConstants.BOOLEAN_CHANGE) ||
					changeType.equals(DataConstants.RESOURCE_CHANGE) ||
					changeType.equals(DataConstants.STRING_CHANGE) ||
					changeType.equals(DataConstants.LITERAL_CHANGE)) {
						
					onData(m_dataProvider, change);
				} else if (
					changeType.equals(DataConstants.LITERAL_CHANGE)) {
					onData(m_dataProvider, ((Literal) change).getContent());
				} else if (
					changeType.equals(DataConstants.RESOURCE_DELETION) ||
					changeType.equals(DataConstants.STRING_DELETION) ||
					changeType.equals(DataConstants.LITERAL_DELETION) ||
					changeType.equals(DataConstants.INTEGER_DELETION)) {
					onData(m_dataProvider, null);
				}
			}

			public void onStatusChanged(Resource status) {
			}
			
			public IDataConsumer init(IDataProvider dataProvider) {
				m_dataProvider = dataProvider;
				return this;
			}
		}.init(dataProvider);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		Iterator i = m_dataProviders.iterator();
		while (i.hasNext()) {
			((IDataProvider) i.next()).dispose();
		}
		m_dataProviders.clear();
		m_dataProviders = null;
		
		m_providerToValue.clear();
		m_providerToValue = null;
		
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		throw new UnsupportedOperationException("Data provider does not support changes");
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.BOOLEAN_CHANGE, new Boolean(m_value));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {

		if (dataType.equals(DataConstants.BOOLEAN)) {
			return new Boolean(m_value);
		}
		throw new DataNotAvailableException("No data available of type " + dataType);
	}

	protected void onValue(boolean newValue) {
		if (newValue != m_value) {
			m_value = newValue;
			if (!m_initializing) {
				notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_value));
			}
		}
	}
	
	protected void onData(IDataProvider dataProvider, Object data) {
		synchronized (m_providerToValue) {
			Object oldData = m_providerToValue.get(dataProvider);
			
			if (oldData != null ? !oldData.equals(data) : data != null ) {
				m_providerToValue.put(dataProvider, data);
				
				if (!m_initializing) {
					refresh();
				}
			}
		}
	}
	
	protected void refresh() {
		boolean 	result = true;
		Object		previousData = null;
		boolean	firstData = true;
		
		Iterator i = m_providerToValue.values().iterator();
		while (i.hasNext()) {
			Object data = i.next();
			
			if (firstData) {
				previousData = data;
			} else {
				if (previousData != null ? !previousData.equals(data) : data != null) {
					result = false;
					break;
				}
			}
			
			firstData = false;
		}
		
		onValue(result);
	}
}
