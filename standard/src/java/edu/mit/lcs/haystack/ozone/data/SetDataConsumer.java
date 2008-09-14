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

import edu.mit.lcs.haystack.rdf.Resource;
import java.util.Set;

/**
 * @author David Huynh
 */
abstract public class SetDataConsumer implements IDataConsumer {
	
	/**
	 * Notifies that some items have been added.
	 */
	abstract protected void onItemsAdded(Set items);
	
	/**
	 * Notifies that some items have been removed.
	 */
	abstract protected void onItemsRemoved(Set items);

	/**
	 * Notifies that all items have been removed.
	 */
	abstract protected void onSetCleared();

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
			
		if (changeType.equals(DataConstants.SET_CLEAR)) {
			onSetCleared();
		} else if (change instanceof Set) {
			Set set = (Set) change;
			
			if (changeType.equals(DataConstants.SET_ADDITION)) {
				onItemsAdded(set);
			} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
				onItemsRemoved(set);
			} else {
				throw new IllegalArgumentException("Unrecognized type of change for " + this + ": " + changeType + ", " + change);
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
}
