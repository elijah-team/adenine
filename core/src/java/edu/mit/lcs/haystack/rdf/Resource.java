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

/**
 * Represents an RDF resource.
 * @author Dennis Quan
 */
public class Resource extends RDFNode {
	/**
	 * Constructs a Resource object with the specified URI.
	 */
	public Resource(String uri) {
		super(uri);
	}
	
	/**
	 * Constructs a Resource object with the concatenation of <code>base</code> and
	 * <code>suffix</code>.
	 */
	public Resource(String base, String suffix){
		super(base + suffix);
	}

	/**
	 * Returns this resource's URI.
	 */
	final public String getURI() {
		return content;
	}
	
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (o.hashCode() != hashCode) {
			return false;
		}
		return (o.getClass() == getClass()) && 
			((Resource)o).getContent().equals(getContent());
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer(100);
		sb.append('<');
		sb.append(content);
		sb.append('>');
		return sb.toString();
	}

	/** Creates a unique resource in <code>target</code> with the specified RDF type. */
	public static Resource createUnique(IRDFContainer target, Resource type)
		throws RDFException {
		Resource res = Utilities.generateUniqueResource();
		target.add(new Statement(res, Constants.s_rdf_type, type));
		return res;
	}

	/** Creates an unknown resource in <code>target</code> with the specified RDF type. 
	 *  TODO [sjg]: Remove definition of unused private method "createUnknown"
	private static Resource createUnknown(IRDFContainer target, Resource type)
		throws RDFException {
		Resource res = Utilities.generateUnknownResource();
		target.add(new Statement(res, Constants.s_rdf_type, type));
		return res;
	}
	*/
}

