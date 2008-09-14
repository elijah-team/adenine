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

package edu.mit.lcs.haystack.ozone.core;

import java.io.Serializable;

import edu.mit.lcs.haystack.rdf.*;

/**
 * IPart
 * 
 * Interface to a user-interface part in Ozone.
 * 
 * @author David Huynh
 * @author Dennis Quan
 */

public interface IPart extends Serializable {
	/**
	 * Initializes the part.
	 * @param source The RDF data source for configuration information.
	 * @param context The Ozone Context.
	 */
	public void initialize(
		IRDFContainer	source, 
		Context			context
	);
	
	/**
	 * Disposes the part.
	 */
	public void dispose();
	
	/**
	 * Handles an event. Returns true if event has been handled
	 * and should not be handled further by caller.
	 */
	public boolean handleEvent(Resource eventType, Object event);
	
	public void initializeFromDeserialization(IRDFContainer source);
}
