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

import java.util.*;

/**
 * Data provider that remembers the order imposed by the list data 
 * consumer on the underlying data provider.
 * @author Dennis Quan
 * @author David Huynh
 */
public class OrderedDataProvider extends ChainedDataProvider {
	protected List				m_ordering = new ArrayList();
	
	protected boolean			m_initializing = true;
	protected boolean			m_addNewItemsToBeginning = true;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SortingDataProvider.class);
	
	class DataConsumer implements IDataConsumer {
		public void onDataChanged(Resource changeType, Object change)
			throws IllegalArgumentException {

			if (changeType.equals(DataConstants.SET_ADDITION)) {
				onItemsAdded((Set) change);
			} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
				onItemsRemoved((Set) change);
			} else {
				throw new IllegalArgumentException("Unsupported change type " + changeType);
			}
		}

		public void onStatusChanged(Resource status) {
		}

		public void reset() {
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.LIST)) {
			return m_ordering;
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_ordering.get(((Integer) specifications).intValue());
			} catch (Exception e) {
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();

			try {
				ArrayList 	elements = new ArrayList();

				elements.addAll(m_ordering.subList(index, index + count));

				return elements;
			} catch (Exception e) {
				s_logger.error("Failed to get list elements", e);
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (changeType.equals(DataConstants.SET_ADDITION)) {
			if (change instanceof Set) {
				onItemsAdded((Set) change);
			} else {
				HashSet set = new HashSet();
				set.add(change);
				onItemsAdded(set);
			}
		} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
			if (change instanceof Set) {
				onItemsRemoved((Set) change);
			} else {
				HashSet set = new HashSet();
				set.add(change);
				onItemsRemoved(set);
			}
		} else if (changeType.equals(DataConstants.LIST_ADDITION)) {
			ListDataChange ldc = (ListDataChange)change;
			onItemsAdded2(ldc.m_elements, ldc.m_index);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return (
			changeType.equals(DataConstants.SET_ADDITION) ||
			changeType.equals(DataConstants.SET_REMOVAL) ||
			changeType.equals(DataConstants.LIST_ADDITION)
		);
	}
	
	synchronized protected void onItemsAdded(Set newItems) {
		HashSet set = new HashSet();
		Iterator j = newItems.iterator();
		while (j.hasNext()) {
			Object o = j.next();
			if (m_ordering.contains(o)) {
				continue;
			}

			set.add(o);	
			
			int i = m_addNewItemsToBeginning ? 0 : m_ordering.size();
			m_ordering.add(i, o);
			ArrayList al = new ArrayList();
			al.add(o);
			ListDataChange ldc = new ListDataChange(i++, al.size(), al);
			notifyDataConsumers(DataConstants.LIST_ADDITION, ldc);
		}

		if (!set.isEmpty()) {
			try {
				if (m_dataProvider != null) {
					m_dataProvider.requestChange(DataConstants.SET_ADDITION, set);
				}
			} catch (Exception e) {
				s_logger.error("Data provider " + m_dataProvider + " does not support set addition", e);
			}
		}
		
		save();
	}
	
	protected void onItemsAdded2(List l, int i) {
		Iterator j = l.iterator();
		HashSet set = new HashSet();
		while (j.hasNext()) {
			Object o = j.next();
			if (m_ordering.contains(o)) {
				// First remove the existing one
				int k = m_ordering.indexOf(o);
				if (k < i) {
					--i;
				}
				List change = new ArrayList();
				m_ordering.remove(o);
				change.add(o);
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(k, 1, change));
			} else {
				set.add(o);	
			}
			
			m_ordering.add(i, o);
			ArrayList al = new ArrayList();
			al.add(o);
			ListDataChange ldc = new ListDataChange(i++, al.size(), al);
			notifyDataConsumers(DataConstants.LIST_ADDITION, ldc);
		}

		if (!set.isEmpty()) {
			try {
				if (m_dataProvider != null) {
					m_dataProvider.requestChange(DataConstants.SET_ADDITION, set);
				}
			} catch (Exception e) {
				s_logger.error("", e);
			}
		}
		
		save();
	}
	
	synchronized protected void onItemsRemoved(Collection removedItems) {
		Iterator i = removedItems.iterator();
		HashSet set = new HashSet();
		
		while (i.hasNext()) {
			Resource item = (Resource) i.next();
			
			if (m_ordering.contains(item)) {
				int 	index = m_ordering.indexOf(item);
				List	change = new ArrayList();
				
				m_ordering.remove(item);
				
				change.add(item);
				
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index, 1, change));
				set.add(item);
			} else {
				continue;
			}
		}

		if (!set.isEmpty()) {
			try {
				if (m_dataProvider != null) {
					m_dataProvider.requestChange(DataConstants.SET_REMOVAL, set);
				}
			} catch (Exception e) {
				s_logger.error("", e);
			}
		}
		
		save();
	}
	
	protected void load() {
		List ordering;
		String s = Utilities.getLiteralProperty(m_prescription, DataConstants.ORDERING, m_partDataSource);
		if (s != null) {
			ordering = Utilities.decodeResourceList(s);
		} else {
			Resource 	list = (Resource) Utilities.getResourceProperty(m_prescription, DataConstants.DAML_LIST, m_partDataSource);
			Iterator	i = ListUtilities.accessDAMLList(list, m_partDataSource);
			ordering = new ArrayList();

			HashSet set = new HashSet();
			while (i.hasNext()) {
				Object o = i.next();
				ordering.add(o);
				set.add(o);
			}

			try {
				if (m_dataProvider != null) {
					m_dataProvider.requestChange(DataConstants.SET_ADDITION, set);
				}
			} catch (Exception e) {
				s_logger.error("Data provider " + m_dataProvider + " does not support set addition", e);
			}
		}
		
		if (m_ordering == null || !m_ordering.equals(ordering)) {
			ArrayList al = new ArrayList(m_ordering);
			al.removeAll(ordering);
			if (m_addNewItemsToBeginning) {
				ordering.addAll(0, al);
			} else {
				ordering.addAll(al);
			}
			m_ordering = ordering;
			save();

			if (!m_initializing) {
				notifyDataConsumers(DataConstants.LIST_CLEAR, null);
				ListDataChange ldc = new ListDataChange(0, m_ordering.size(), m_ordering);
				notifyDataConsumers(DataConstants.LIST_ADDITION, ldc);
			}
		}
	}

	protected void save() {
		if (m_initializing) {
			return;
		}
		
		try {
			m_partDataSource.replace(m_prescription, DataConstants.ORDERING, null, Utilities.encodeResourceList(m_ordering));
		} catch (RDFException e) {
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new DataConsumer();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
		load();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.reset();
		dataConsumer.onDataChanged(
			DataConstants.LIST_ADDITION, new ListDataChange(0, m_ordering.size(), null));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, true);
		m_addNewItemsToBeginning = Utilities.checkBooleanProperty(m_prescription, DataConstants.s_addNewItemsToBeginning, m_partDataSource);
		try {
			addPattern(m_prescription, DataConstants.ORDERING, null);
		} catch (RDFException e) {
		}
		load();
		m_initializing = false;
		save();
	}

}
