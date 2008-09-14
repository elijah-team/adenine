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

/**
 * @author David Huynh
 */
abstract public class IntegerDataConsumer implements IDataConsumer {
	
	/**
	 * Notifies that the integer data has been changed to newInteger.
	 */
	abstract protected void onIntegerChanged(int newInteger);
	
	/**
	 * Notifies that the integer data has been deleted.
	 */
	abstract protected void onIntegerDeleted(int previousInteger);

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
			
		Integer i = null;
		if (change instanceof Integer) {
			i = (Integer) change;
		}
			
		if (i != null) {
			if (changeType.equals(DataConstants.INTEGER_CHANGE)) {
				onIntegerChanged(i.intValue());
			} else if (changeType.equals(DataConstants.INTEGER_DELETION)) {
				onIntegerDeleted(i.intValue());
			} else {
				throw new IllegalArgumentException("Unrecognized type of change for IntegerDataConsumer");
			}
		} else {
			throw new IllegalArgumentException("An Integer object is expected");
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(Resource)
	 */
	public void onStatusChanged(Resource status) {
	}

}
