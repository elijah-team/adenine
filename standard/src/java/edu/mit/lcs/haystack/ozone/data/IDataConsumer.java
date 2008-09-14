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

import java.io.Serializable;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 * 
 * A data consumer is given to a data provider to receive
 * notifications of changes to a data set.
 */
public interface IDataConsumer extends Serializable {
	
	/**
	 * Resets the data consumer. This method is called by the
	 * data provider once the provider is given this consumer.
	 */
	void reset();
	
	/**
	 * Notifies that the data encapsulated inside the data
	 * provider (who holds this data consumer) has changed.
	 * The "change" object describes the change, the
	 * changeType specifies the type of change.
	 */
	void onDataChanged(Resource changeType, Object change) throws IllegalArgumentException;
	
	/**
	 * Notifies that the data provider's status has changed.
	 * The current status is described by the provided resource.
	 */
	void onStatusChanged(Resource status);
}
