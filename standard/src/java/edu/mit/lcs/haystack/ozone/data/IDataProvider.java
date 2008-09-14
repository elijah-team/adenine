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

import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 * 
 * Given a part data of rdf:type hs:DataSource or any of its subtypes,
 * a data provider retrieves the data described in the part data and
 * keeps track of changes to that data.  Interactions with that data 
 * can be done through the data provider.
 */
public interface IDataProvider extends IPart {
	/**
	 * Registers the data consumer to receive notifications of changes.
	 * A data provider can operate without a data consumer or with several.
	 */
	void registerConsumer(IDataConsumer dataConsumer);
	
	/**
	 * Unregisters the data consumer.
	 */
	void unregisterConsumer(IDataConsumer dataConsumer);

	/**
	 * Returns the data of the given type and according to the given
	 * specifications.
	 */
	Object getData(Resource dataType, Object specifications) throws DataNotAvailableException;
	
	/**
	 * Returns a Resource describing the status of this data provider.  Returns null if no
	 * status information is available.  The returned status will generally have type
	 * <code>IStatus</code> or <code>IActivity</code>.
	 */
	Resource getStatus();
	
	/**
	 * Requests a change to the data.  The change might not be executed
	 * immediately if at all.
	 */
	void requestChange(Resource changeType, Object change) throws 
		UnsupportedOperationException, DataMismatchException;
		
	/**
	 * Asks whether the data provider supports the given type of change.
	 */
	boolean supportsChange(Resource changeType);
}
