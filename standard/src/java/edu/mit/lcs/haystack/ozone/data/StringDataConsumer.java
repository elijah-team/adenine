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
import edu.mit.lcs.haystack.rdf.Literal;

/**
 * @author David Huynh
 */
abstract public class StringDataConsumer implements IDataConsumer {
	
	/**
	 * Notifies that the string data has been changed to newString.
	 */
	abstract protected void onStringChanged(String newString);
	
	/**
	 * Notifies that the string data has been deleted.
	 */
	abstract protected void onStringDeleted(String previousString);

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
			
		String s = null;
		if (change instanceof String) {
			s = (String) change;
		} else if (change instanceof Literal) {
			s = ((Literal) change).getContent();
		}
			
		if (s != null) {
			if (changeType.equals(DataConstants.STRING_CHANGE) ||
				changeType.equals(DataConstants.LITERAL_CHANGE)) {
				onStringChanged(s);
			} else if (changeType.equals(DataConstants.STRING_DELETION) ||
						changeType.equals(DataConstants.LITERAL_DELETION)) {
				onStringDeleted(s);
			} else {
				throw new IllegalArgumentException("Unrecognized type of change for StringDataConsumer: " + changeType + ", " + changeType.getContent());
			}
		} else {
			String fault = change == null ? "null" : change.getClass().getName();
			throw new IllegalArgumentException("A String object is expected, not " + fault);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(Resource)
	 */
	public void onStatusChanged(Resource status) {
	}

}
