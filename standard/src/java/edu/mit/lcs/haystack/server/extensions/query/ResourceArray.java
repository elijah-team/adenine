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

package edu.mit.lcs.haystack.server.extensions.query;

import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Represents a term (effectively a word/dimension for text based learning).
 * 
 * @author Vineet Sinha
 */
public class ResourceArray {
	RDFNode[] data = null;
	public RDFNode[] getArray() {
		return data;
	}
	public RDFNode getData(int ndx) {
		return data[ndx];
	}
	public int getLength() {
		return data.length;
	}

	public ResourceArray(Resource predicate) {
		this.data = new Resource[1];
		this.data[0] = predicate;
		calcHashCode();
	}

	public ResourceArray(Resource[] predicate) {
		this.data = predicate;
		calcHashCode();
	}

	public ResourceArray(RDFNode[] predicate, int hashCode) {
		this.data = predicate;
		m_hashCode = hashCode;
	}

	public boolean equals(Object o) {
		if ((o == null) || !(o instanceof ResourceArray)) {
			return false;
		}

		ResourceArray po = (ResourceArray) o;

		if (this.data == null && po.data == null) {
			return true;
		}
		if (this.data == null || po.data == null) {
			// only one is null
			return false;
		}
		if (this.data.length != po.data.length) {
			return false;
		}
		for (int i = 0; i < data.length; i++) {
			if (!this.data[i].equals(po.data[i])) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		if (data == null)
			return "";
		StringBuffer retStr = new StringBuffer(data[0].toString());
		for (int i = 1; i < data.length; i++) {
			retStr.append(" ");
			retStr.append(data[i].toString());
		}

		return retStr.toString();
	}

	int m_hashCode;
	private void calcHashCode() {
		if (data == null) {
			m_hashCode = 0;
		}
		m_hashCode = 0;
		for (int i = 0; i < data.length; i++) {
			m_hashCode += data[i].hashCode();
		}
	}
	public int hashCode() {
		return m_hashCode;
	}
}
