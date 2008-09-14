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

package edu.mit.lcs.haystack.rdf;

import edu.mit.lcs.haystack.Constants;

import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class DAMLListIterator implements Iterator {
	Resource m_res;
	IRDFContainer m_source;

	DAMLListIterator(Resource res, IRDFContainer source) {
		m_source = source;
		m_res = res;
	}
	
	public Resource getCurrentListNode() {
		return m_res;
	}

	/**
	 * @see Iterator#hasNext()
	 */
	public boolean hasNext() {
		return (m_res != null) && !m_res.equals(Constants.s_daml_nil);
	}

	/**
	 * @see Iterator#next()
	 */
	public Object next() {
		if (m_res == null) {
			return null;
		}

		RDFNode first =
			Utilities.getProperty(m_res, Constants.s_daml_first, m_source);
		m_res =
			Utilities.getResourceProperty(m_res, Constants.s_daml_rest, m_source);
		return first;
	}

	/**
	 * @see Iterator#remove()
	 */
	public void remove() {
	}

}

