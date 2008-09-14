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
 * Created on Feb 23, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.HashSet;
import java.util.Set;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class SetDifferenceDataProvider extends ChainedDataProvider {
	protected HashSet m_items = new HashSet();
	protected HashSet m_exclusion = new HashSet();
	protected HashSet m_last = new HashSet();
	
	protected SetDataProviderWrapper m_exclusionProvider = null;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#dispose()
	 */
	public synchronized void dispose() {
		if (m_exclusionProvider != null) {
			m_exclusionProvider.dispose();
			m_exclusionProvider = null;
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new SetDataConsumer() {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsAdded(java.util.Set)
			 */
			protected void onItemsAdded(Set items) {
				synchronized (m_items) {
					m_items.addAll(items);
				}
				update();
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(java.util.Set)
			 */
			protected void onItemsRemoved(Set items) {
				synchronized (m_items) {
					m_items.removeAll(items);
				}
				update();
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onSetCleared()
			 */
			protected void onSetCleared() {
				synchronized (m_items) {
					m_items.clear();
				}
				update();
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (dataConsumer != null) {
			synchronized (this) {
				if (!m_last.isEmpty()) {
					dataConsumer.onDataChanged(DataConstants.SET_ADDITION, m_last);
				}
			}
		}
	}
	
	synchronized protected void update() {
		HashSet newLast = new HashSet();
		newLast.addAll(m_items);
		newLast.removeAll(m_exclusion);
		
		HashSet addedSet = new HashSet();
		addedSet.addAll(newLast);
		addedSet.removeAll(m_last);
			
		HashSet removedSet = new HashSet();
		removedSet.addAll(m_last);
		removedSet.removeAll(newLast);

		if (!addedSet.isEmpty()) {
			notifyDataConsumers(DataConstants.SET_ADDITION, addedSet);
		}
		if (!removedSet.isEmpty()) {
			notifyDataConsumers(DataConstants.SET_REMOVAL, removedSet);
		}

		m_last = newLast;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (DataConstants.SET.equals(dataType)) {
			return m_last;
		} else if (DataConstants.SET_COUNT.equals(dataType)) {
			return new Integer(m_last.size());
		}
		return null; 
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.internalInitialize(source, context, false);
		
		Resource exclusionProvider = Utilities.getResourceProperty(m_prescription, DataConstants.s_exclusionDataSource, m_partDataSource);
		if (exclusionProvider != null) {
			m_exclusionProvider = new SetDataProviderWrapper(DataUtilities.createDataProvider(exclusionProvider, m_context, m_partDataSource));
			m_exclusionProvider.m_provider.registerConsumer(new IDataConsumer() {
				/**
				 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#reset()
				 */
				public void reset() {
				}

				/**
				 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onDataChanged(Resource, Object)
				 */
				public void onDataChanged(Resource changeType, Object change)
					throws IllegalArgumentException {
						
					if (change instanceof Set) {
						Set items = (Set) change;
						
						if (changeType.equals(DataConstants.SET_ADDITION)) {
							synchronized (m_exclusion) {
								m_exclusion.addAll(items);
							}
							update();
						} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
							synchronized (m_exclusion) {
								m_exclusion.removeAll(items);
							}
							update();
						} else if (changeType.equals(DataConstants.SET_CLEAR)) {
							synchronized (m_exclusion) {
								m_exclusion.clear();
							}
							update();
						} else {
							throw new IllegalArgumentException("Unrecognized type of change for SetDataConsumer");
						}
					} else if (change instanceof Resource) {
						Resource resource = (Resource) change;
						
						if (changeType.equals(DataConstants.RESOURCE_CHANGE)) {
							synchronized (m_exclusion) {
								m_exclusion.clear();
								m_exclusion.add(resource);
							}
							update();
						} else if (changeType.equals(DataConstants.RESOURCE_DELETION)) {
							synchronized (m_exclusion) {
								m_exclusion.clear();
							}
							update();
						} else {
							throw new IllegalArgumentException("Unrecognized type of change for ResourceDataConsumer");
						}
					} else {
						throw new IllegalArgumentException("A Set object is expected");
					}
				}

				/**
				 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(Resource)
				 */
				public void onStatusChanged(Resource status) {
				}
			}); 
		}
	}
}

