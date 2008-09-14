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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * Merges a series of set and resource data providers into a single set data provider.
 * @author Dennis Quan
 */
public class MergeSetDataProvider extends GenericDataProvider {
	protected HashSet m_data = new HashSet();
	protected HashSet m_sources = new HashSet();
	protected HashSet m_sourcesToBeDisposed = new HashSet();
	protected ArrayList m_separatedData = new ArrayList();
	protected Resource m_onAdd;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		Iterator i = m_sourcesToBeDisposed.iterator();
		while (i.hasNext()) {
			IDataProvider dp = (IDataProvider) i.next();
			dp.initializeFromDeserialization(source);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (!m_data.isEmpty()) {
			HashSet data = new HashSet();
			synchronized (m_data) {
				data.addAll(m_data);
			}
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, data);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.SET)) {
			synchronized (m_data) {
				return new HashSet(m_data);
			}
		}
		return null;
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
		
		m_onAdd = Utilities.getResourceProperty(m_prescription, DataConstants.s_onAdd, m_partDataSource);
		
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

	protected void updateCacheAndNotify() {
		HashSet newSet = new HashSet();
		Iterator i = m_separatedData.iterator();
		while (i.hasNext()) {
			Set set = (Set)i.next();
			newSet.addAll(set);
		}
		
		HashSet addedSet = new HashSet();
		HashSet removedSet = new HashSet();
		synchronized (m_data) {
			addedSet.addAll(newSet);
			addedSet.removeAll(m_data);
			
			removedSet.addAll(m_data);
			removedSet.removeAll(newSet);

			m_data.clear();
			m_data.addAll(newSet);
		}

		if (!addedSet.isEmpty()) {
			notifyDataConsumers(DataConstants.SET_ADDITION, addedSet);
		}
		if (!removedSet.isEmpty()) {
			notifyDataConsumers(DataConstants.SET_REMOVAL, removedSet);
		}
	}

	protected void setupConsumer(IDataProvider dp) {
		dp.registerConsumer(new IDataConsumer() {
			HashSet m_myData = new HashSet();
			
			protected IDataConsumer init() {
				m_separatedData.add(m_myData);
				return this;
			}
			
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onDataChanged(Resource, Object)
			 */
			public void onDataChanged(Resource changeType, Object change)
				throws IllegalArgumentException {
				if (DataConstants.RESOURCE_CHANGE.equals(changeType)) {
					m_myData.clear();
					m_myData.add(change);
				} else if (DataConstants.RESOURCE_DELETION.equals(changeType)) {
					m_myData.clear();
				} else if (DataConstants.SET_ADDITION.equals(changeType)) {
					m_myData.addAll((Set)change);
				} else if (DataConstants.SET_CLEAR.equals(changeType)) {
					m_myData.clear();
				} else if (DataConstants.SET_REMOVAL.equals(changeType)) {
					m_myData.removeAll((Set)change);
				} else {
					return;
				}
				updateCacheAndNotify();		
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
		}.init());
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		Iterator i = m_sourcesToBeDisposed.iterator();
		while (i.hasNext()) {
			IDataProvider dp = (IDataProvider)i.next();
			dp.dispose();
		}
		m_sourcesToBeDisposed.clear();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (DataConstants.SET_ADDITION.equals(changeType)) {
			Set set;
			if (change instanceof Set) {
				set = (Set) change;
			} else {
				set = new HashSet();
				set.add(change);
			}
			synchronized (m_data) {
				if (m_data.containsAll(set)) {
					return;
				}
			}

			if (m_onAdd != null) {
				Interpreter i = Ozone.getInterpreter();
				DynamicEnvironment denv = new DynamicEnvironment(m_source);
				Ozone.initializeDynamicEnvironment(denv, m_context);

				try {
					i.callMethod(m_onAdd, new Object[] { change }, denv);
				} catch (Exception e) {
					e.printStackTrace();
//					s_logger.error("Error calling method onChange " + m_resOnChange, e);
				}
			}
		} else if (DataConstants.SET_REMOVAL.equals(changeType)) {
			Iterator i = m_sources.iterator();
			while (i.hasNext()) {
				try {
					IDataProvider dp = (IDataProvider) i.next();
					dp.requestChange(DataConstants.SET_REMOVAL, change);
				} catch (Exception e) {}
			}
		}

		super.requestChange(changeType, change);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return DataConstants.SET_REMOVAL.equals(changeType) || DataConstants.SET_ADDITION.equals(changeType);
	}

}
