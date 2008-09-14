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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  Tests equality based on INode.label
 */
public class StringNodeComparator extends NodeComparator {

    protected String label;

    public StringNodeComparator(String label) {
	this.label = label;
    }

    public boolean equals(INode n) {
	return this.label.equalsIgnoreCase(n.getTagName());
    }

    public boolean equals(NodeComparator c) {
	return ((c instanceof StringNodeComparator) &&
		this.label.equalsIgnoreCase(((StringNodeComparator)c).label));
    }

    public void set(String property, Object value) {
	if (property.equalsIgnoreCase("label")) 
	    this.label = value.toString();
    }

    public int hashCode() {
	return this.label.hashCode();
    }

    /**
     *  Creates an RDF resource representing this comparator.
     */
    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource compRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(compRes, Constants.s_rdf_type, WrapperManager.NODE_COMPARATOR_CLASS));
	rdfc.add(new Statement(compRes, Constants.s_rdf_type, WrapperManager.STRING_NODE_COMPARATOR_CLASS));
	rdfc.add(new Statement(compRes,
			       WrapperManager.NODE_COMPARATOR_JAVA_CLASS_PROP,
			       new Literal(this.getClass().getName())));
	rdfc.add(new Statement(compRes, WrapperManager.STRING_NODE_COMPARATOR_TAG_NAME_PROP, new Literal(label)));
	return compRes;
    }

    public static NodeComparator fromResource(Resource compRes, IRDFContainer rdfc) throws RDFException {
	return new StringNodeComparator(rdfc.extract(compRes, WrapperManager.STRING_NODE_COMPARATOR_TAG_NAME_PROP, null).getContent());
    }






}
