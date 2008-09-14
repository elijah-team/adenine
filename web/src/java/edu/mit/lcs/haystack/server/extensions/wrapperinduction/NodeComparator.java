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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction;

import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  Provides a permanent way to test node equality even after the original
 *  node has lost scope
 */
public abstract class NodeComparator {

    /**
     *  Returns true if the given INode is equal to the node that
     *  generated this comparator.
     */
    public abstract boolean equals(INode n);

    /**
     *  Returns true if the given NodeComparator represents an INode
     *  that is equal to the one that generated this one.
     */
    public abstract boolean equals(NodeComparator c);

    /**
     *  Implements a viable hashCode for the object represented by
     *  this NodeComparator, so the object may be placed in Hash
     *  tables.
     */
    public abstract int hashCode();

    /**
     *  Sets the given property
     */
    public abstract void set(String property, Object value);

    /**
     *  Creates an RDF resource representing this comparator.
     */
    public abstract Resource makeResource(IRDFContainer rdfc) throws RDFException;

    public static NodeComparator fromResource(Resource compRes, IRDFContainer rdfc)
	throws RDFException {
	try {
	    String className = rdfc.extract(compRes,
					    WrapperManager.NODE_COMPARATOR_JAVA_CLASS_PROP,
					    null).getContent();
	    return (NodeComparator)
		CoreLoader.loadClass(className)
		.getMethod("fromResource",
			   new Class[] {CoreLoader.loadClass("edu.mit.lcs.haystack.rdf.Resource"),
				CoreLoader.loadClass("edu.mit.lcs.haystack.rdf.IRDFContainer")})
		.invoke(null, new Object[] {compRes, rdfc});	    
	}
	catch (Exception e) {
	    throw new RDFException("Error in NodeComparator.fromResource: " + e);
	}
    }

}
