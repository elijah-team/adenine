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

import java.util.List;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 * 
 * Provides custom notifications for data of list nature. The data
 * list contains an ordered list of RDFNode tuples.
 */
abstract public class ListDataConsumer implements IDataConsumer {
	/**
	 * Notifies that a "count" number of new elements starting at 
	 * "index" have been inserted. These elements are available in the
	 * list returned from IDataProvider.getData().
	 */
	abstract protected void onElementsAdded(int index, int count);
	
	/**
	 * Notifies that a "count" number of existing elements
	 * starting at "index" have been removed. These elements are
	 * no longer available in the list returned from 
	 * IDataProvider.getData().
	 */
	abstract protected void onElementsRemoved(int index, int count, List removedElements);
	
	/**
	 * Notifies that a "count" number of existing elements starting
	 * at "index" have changed.
	 */
	abstract protected void onElementsChanged(int index, int count);
	
	/**
	 * Notifies that a list of existing elements as indexed by
	 * changedIndices have been changed.
	 * 
	 * Elements of changedIndices are Integers.
	 */
	abstract protected void onElementsChanged(List changedIndices);
	
	/**
	 * Notifies that the list has been cleared.
	 *
	 * Elements of changedIndices are Integers.
	 */
	abstract protected void onListCleared();

	/**
	 * @see IDataConsumer#onDataChanged(Resource, Object)
	 */
	public void onDataChanged(Resource changeType, Object change) throws IllegalArgumentException {
		if (change instanceof ListDataChange) {
			ListDataChange c = (ListDataChange) change;
			
			if (changeType.equals(DataConstants.LIST_ADDITION)) {
				onElementsAdded(c.m_index, c.m_count);
			} else if (changeType.equals(DataConstants.LIST_REMOVAL)) {
				onElementsRemoved(c.m_index, c.m_count, c.m_elements);
			} else if (changeType.equals(DataConstants.LIST_CHANGE)) {
				if (c.m_indices == null) {
					onElementsChanged(c.m_index, c.m_count);
				} else {
					onElementsChanged(c.m_indices);
				}
			} else {
				throw new IllegalArgumentException("ListDataChange object specifies unrecognized type of change");
			}
		} else if (changeType.equals(DataConstants.LIST_CLEAR)) {
			onListCleared();
		} else {
			throw new IllegalArgumentException("A ListDataChange object is expected: " + change);
		}
	}
	
	/**
	 * @see IDataConsumer#onStatusChanged(Resource)
	 */
	public void onStatusChanged(Resource status) {
	}

	/**
	 * @see IDataConsumer#reset()
	 */
	public void reset() {
	}
}
