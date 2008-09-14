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
import java.util.List;
import java.util.LinkedList;

/**
 * @author David Huynh
 */
public class ListDataProviderWrapper implements java.io.Serializable {
	IDataProvider m_provider;
	
	public ListDataProviderWrapper(IDataProvider provider) {
		m_provider = provider;
		if (provider == null) {
			throw new NullPointerException("IDataProvider object to wrap is null");
		}
	}
	
	public void dispose() {
		m_provider.dispose();
		m_provider = null;
	}

	public IDataProvider getDataProvider() {
		return m_provider;
	}

	public List getList() throws DataNotAvailableException, DataMismatchException {
		try {
			return (List) m_provider.getData(DataConstants.LIST, null);
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide a List");
		}
	}

	public List getListHead(int count) throws DataNotAvailableException, DataMismatchException {
		try {
			return (List) m_provider.getData(DataConstants.LIST, new Integer(count));
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide a List");
		}
	}

	public List getListTail(int count) throws DataNotAvailableException, DataMismatchException {
		try {
			return (List) m_provider.getData(DataConstants.LIST, new Integer(-count));
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide a List");
		}
	}

	public Object getElement(int index) throws DataNotAvailableException, DataMismatchException {
		try {
			return m_provider.getData(DataConstants.LIST_ELEMENT, new Integer(index));
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide an element");
		}
	}

	public List getElements(int index, int count) throws DataNotAvailableException, DataMismatchException {
		try {
			return (List) m_provider.getData(DataConstants.LIST_ELEMENTS, new Object[] { new Integer(index), new Integer(count) });
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide a list of elements");
		}
	}

	public int getListCount() throws DataNotAvailableException, DataMismatchException {
		try {
			return ((Integer) m_provider.getData(DataConstants.LIST_COUNT, null)).intValue();
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide a List");
		}
	}

	/**
	 * Requests addition of a new element into the data list of this
	 * data provider. The new element might not be inserted immediately
	 * or at all. If it is inserted, a notification will eventually
	 * be sent to the data consumer.
	 * 
	 * Specify index = -1 for appending.
	 */
	public void requestAddition(Object element, int index) throws 
		UnsupportedOperationException, DataMismatchException, IndexOutOfBoundsException, DataNotAvailableException {
		
		List list = new LinkedList();
		
		list.add(element);
		
		requestAdditions(list, index);
	}
		
	/**
	 * Requests addition of several new elements.
	 */
	public void requestAdditions(List elements, int index) throws 
		UnsupportedOperationException, DataMismatchException, IndexOutOfBoundsException, DataNotAvailableException {
			
		m_provider.requestChange(DataConstants.LIST_ADDITION, 
			new ListDataChange(index, elements.size(), elements));
	}
		
	/**
	 * Requests change to one element identified.
	 * 
	 * The element might not be changed immediately or at all. If it
	 * is changed, a notification will eventually be sent to the data
	 * consumer.
	 */
	public void requestChange(int index, Object element) throws 
		UnsupportedOperationException, DataMismatchException, IndexOutOfBoundsException, DataNotAvailableException {

		ArrayList elements = new ArrayList();
		
		elements.add(element);
		
		m_provider.requestChange(DataConstants.LIST_CHANGE, new ListDataChange(index, 1, elements));
	}

	/**
	 * Requests removal of one or more existing element(s) identified 
	 * by a starting index and a count.
	 * 
	 * The element might not be removed immediately or at all. If it
	 * is removed, a notification will eventually be sent to the data
	 * consumer.
	 */
	public void requestRemoval(int index, int count) throws 
		UnsupportedOperationException, DataMismatchException, IndexOutOfBoundsException, DataNotAvailableException {
			
		m_provider.requestChange(DataConstants.LIST_REMOVAL,
			new ListDataChange(index, count, null));
	}

	/**
	 * Requests removal of all items corresponding to indices in the
	 * given list.
	 */
	public void requestRemoval(List indices) throws 
		UnsupportedOperationException, DataMismatchException, IndexOutOfBoundsException, DataNotAvailableException {
			
		m_provider.requestChange(DataConstants.LIST_REMOVAL,
			new ListDataChange(indices));
	}

	public void requestClear() throws
		UnsupportedOperationException, DataMismatchException {
			
		m_provider.requestChange(DataConstants.LIST_CLEAR, null);
	}
	
	public boolean supportsListInsertion() {
		boolean b = m_provider.supportsChange(DataConstants.LIST_ADDITION);
		return b;
	}
}
