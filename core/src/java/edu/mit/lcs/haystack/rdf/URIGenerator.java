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

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class URIGenerator implements IURIGenerator {
	protected String m_base;
	protected int m_c = 0;
	
	public URIGenerator() {
		m_base = null;
	}
	
	public URIGenerator(String base) {
		m_base = base;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.rdf.IURIGenerator#generateAnonymousResource()
	 */
	public Resource generateAnonymousResource() {
		if (m_base != null) {
			return new Resource(m_base + "_" + (++m_c));
		} else {
			return Utilities.generateUniqueResource();
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IURIGenerator#generateNewResource()
	 */
	public Resource generateNewResource() {
		return Utilities.generateUniqueResource();
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IURIGenerator#generateUnknownResource()
	 */
	public Resource generateUnknownResource() {
		return Utilities.generateUnknownResource();
	}

}

