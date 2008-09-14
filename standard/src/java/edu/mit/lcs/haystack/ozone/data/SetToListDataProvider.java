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
 * Data provider that turns a set into a list with arbitrary order.
 * @author David Huynh
 */
public class SetToListDataProvider extends ChainedDataProvider {
	protected List			m_list = new ArrayList();
	protected HashSet			m_set = new HashSet();
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SetToListDataProvider.class);
	
	class DataConsumer implements IDataConsumer {
		public void onDataChanged(Resource changeType, Object change)
			throws IllegalArgumentException {

			if (changeType.equals(DataConstants.SET_ADDITION)) {
				onItemsAdded((Set) change);
			} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
				onItemsRemoved((Set) change);
			} else if (changeType.equals(DataConstants.SET_CLEAR)){
				onItemsCleared();
			}
			else {
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
			return new ArrayList(m_list);
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_list.get(((Integer) specifications).intValue());
			} catch (Exception e) {
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();

			try {
				ArrayList 	elements = new ArrayList();

				elements.addAll(m_list.subList(index, index + count));

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
				requestSetAdditions((Set) change);
			} else {
				HashSet set = new HashSet();
				set.add(change);
				requestSetAdditions(set);
			}
		} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
			if (change instanceof Set) {
				requestSetRemovals((Set) change);
			} else {
				HashSet set = new HashSet();
				set.add(change);
				requestSetRemovals(set);
			}
		} else if ((changeType.equals(DataConstants.SET_CLEAR)) || (changeType.equals(DataConstants.LIST_CLEAR))) {
			requestSetClear();
		}
		}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return (
			changeType.equals(DataConstants.SET_ADDITION) ||
			changeType.equals(DataConstants.SET_REMOVAL) ||
			(changeType.equals(DataConstants.SET_CLEAR)) || 
			(changeType.equals(DataConstants.LIST_CLEAR))
		);
	}
	
	synchronized protected void onItemsAdded(Set newItems) {
		ArrayList 	newItems2 = new ArrayList(newItems);
		int 		oldSize = m_list.size();
		
		m_set.addAll(newItems);
		m_list.addAll(newItems2);
		
		ListDataChange ldc = new ListDataChange(oldSize, newItems2.size(), newItems2);
		notifyDataConsumers(DataConstants.LIST_ADDITION, ldc);
	}
	
	synchronized protected void onItemsRemoved(Set removedItems) {
		Iterator i = removedItems.iterator();
		
		while (i.hasNext()) {
			Resource 	item = (Resource) i.next();
			int		index = m_list.indexOf(item);
			
			if (index >= 0) {
				List	change = new ArrayList();
				
				change.add(item);
				
				m_list.remove(index);
				m_set.remove(item);
				
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index, 1, change));
			}
		}
	}
	
	synchronized protected void onItemsCleared() {
		m_set.clear();
		m_list.clear();
		notifyDataConsumers(DataConstants.LIST_CLEAR, null);
	}
	
	synchronized protected void requestSetAdditions(Set newItems) {
		try {
			m_dataProvider.requestChange(DataConstants.SET_ADDITION, newItems);
		} catch (Exception e) {
			s_logger.error(e);
		}
	}
	
	synchronized protected void requestSetRemovals(Set oldItems) {
		try {
			m_dataProvider.requestChange(DataConstants.SET_REMOVAL, oldItems);
		} catch (Exception e) {
			s_logger.error(e);
		}
	}
	
	synchronized protected void requestSetClear() {
		try {
			m_dataProvider.requestChange(DataConstants.SET_CLEAR, null);
		} catch (Exception e) {
			s_logger.error(e);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new DataConsumer();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.reset();
		if (m_list.size() > 0) {
			dataConsumer.onDataChanged(
				DataConstants.LIST_ADDITION, new ListDataChange(0, m_list.size(), null));
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);
	}

}
