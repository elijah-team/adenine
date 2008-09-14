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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class StickyResourceDataProvider extends GenericDataProvider {
	protected Resource m_datum = null;
	protected IDataProvider m_persistentDataProvider = null;
	
	protected HashSet m_sources = new HashSet();
	protected HashSet m_sourcesToBeDisposed = new HashSet();

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_persistentDataProvider != null) {
			m_persistentDataProvider.initializeFromDeserialization(source);
		}
		Iterator i = m_sourcesToBeDisposed.iterator();
		while (i.hasNext()) {
			IDataProvider dp = (IDataProvider) i.next();
			dp.initializeFromDeserialization(source);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		Resource persistentDataSource = Utilities.getResourceProperty(m_prescription, DataConstants.s_persistentDataSource, m_partDataSource);
		if (persistentDataSource != null) {
			m_persistentDataProvider = DataUtilities.createDataProvider(persistentDataSource, m_context, m_partDataSource);
		}

		// Locate sources to merge
		IDataProvider dp = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (dp != null) {
			m_sources.add(dp);
			setupConsumer(dp);
		}
		
		Resource[] chainedSources = Utilities.getResourceProperties(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
		for (int i = 0; i < chainedSources.length; i++) {
			dp = DataUtilities.createDataProvider(chainedSources[i], m_context, m_partDataSource);
			m_sources.add(dp);
			m_sourcesToBeDisposed.add(dp);
			setupConsumer(dp);
		}
	}

	protected void setupConsumer(IDataProvider dp) {
		dp.registerConsumer(new IDataConsumer() {
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onDataChanged(Resource, Object)
			 */
			public void onDataChanged(Resource changeType, Object change)
				throws IllegalArgumentException {
				if (DataConstants.RESOURCE_CHANGE.equals(changeType)) {
					setResource((Resource) change);
				} else if (DataConstants.SET_ADDITION.equals(changeType)) {
					Set items = (Set) change;
					if (!items.isEmpty()) {
						setResource((Resource) items.iterator().next());
					}
				} else {
					return;
				}
			}
			
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#reset()
			 */
			public void reset() {
			}
			
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(Resource)
			 */
			public void onStatusChanged(Resource status) {
			}
		});
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_datum != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_datum);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.RESOURCE)) {
			return m_datum;
		}
		throw new DataNotAvailableException("Data type " + dataType + " not supported");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (DataConstants.RESOURCE_CHANGE.equals(changeType)) {
			setResource((Resource) change);
		} else if (!DataConstants.RESOURCE_DELETION.equals(changeType)) {
			super.requestChange(changeType, change);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return DataConstants.RESOURCE_CHANGE.equals(changeType);
	}

	protected void setResource(Resource res) {
		synchronized (this) {
			if ((m_datum == null && res == null) || (m_datum != null && m_datum.equals(res)) || (res != null && res.equals(m_datum))) {
				return;
			}
			m_datum = res;
		}
		
		if (m_persistentDataProvider != null) {
			try {
				m_persistentDataProvider.requestChange(DataConstants.RESOURCE_CHANGE, m_datum);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_datum);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		super.dispose();

		Iterator i = m_sourcesToBeDisposed.iterator();
		while (i.hasNext()) {
			IDataProvider dp = (IDataProvider)i.next();
			dp.dispose();
		}
		m_sourcesToBeDisposed.clear();
		
		if (m_persistentDataProvider != null) {
			m_persistentDataProvider.dispose();
			m_persistentDataProvider = null;
		}
	}

}
