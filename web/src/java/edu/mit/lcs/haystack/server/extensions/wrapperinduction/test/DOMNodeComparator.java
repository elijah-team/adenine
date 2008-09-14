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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.test;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeComparator;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMElement;

/**
 *  Provides for testing equality between two IDOMElements
 */
public class DOMNodeComparator extends NodeComparator {

    protected String tagName;
    protected String text;

    public DOMNodeComparator(String tagName, String text) {
	this.tagName = tagName;
	this.text = text;
    }

    public boolean equals(INode n) {
	if (!(n instanceof IEDOMElement)) {
	    return this.equals(n.getComparator());
	}

	String nTag = ((IEDOMElement)n).getTagName();
	if (!this.tagName.equalsIgnoreCase(nTag)) return false;

	if (!IEDOMElement.TREAT_TEXT_NODES_IDENTICALLY &&
	    ((IEDOMElement)n).getNodeType() == 3) {
	    return this.text.equals(((IEDOMElement)n).getNodeText());
	}

	// implement attribute tests?

	return true;
    }

    public boolean equals(NodeComparator c) {
	if (!(c instanceof DOMNodeComparator)) {
	    return false;
	}
	DOMNodeComparator d = (DOMNodeComparator)c;

	//	System.out.println(this.tagName + "->" + d.tagName + ", " + this.text + "->" + d.text);
	return (this.tagName.equalsIgnoreCase(d.tagName) &&
		(IEDOMElement.TREAT_TEXT_NODES_IDENTICALLY ||
		 this.text.equals(d.text)));
    }

    public void set(String property, Object value) {
	if (property.equalsIgnoreCase("tagname")) 
	    this.tagName = value.toString();
	else if (property.equalsIgnoreCase("text")) 
	    this.text = value.toString();
    }

    public int hashCode() {
	if (IEDOMElement.TREAT_TEXT_NODES_IDENTICALLY) {
	    return this.tagName.hashCode();
	}
	else {
	    return (tagName + text).hashCode();
	}
    }


    /**
     *  Creates an RDF resource representing this comparator.
     */
    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource compRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(compRes, Constants.s_rdf_type, WrapperManager.NODE_COMPARATOR_CLASS));
	rdfc.add(new Statement(compRes, Constants.s_rdf_type, WrapperManager.DOM_NODE_COMPARATOR_CLASS));
	rdfc.add(new Statement(compRes,
			       WrapperManager.NODE_COMPARATOR_JAVA_CLASS_PROP,
			       new Literal(this.getClass().getName())));
	rdfc.add(new Statement(compRes, WrapperManager.DOM_NODE_COMPARATOR_TAG_NAME_PROP, new Literal(this.tagName)));
	rdfc.add(new Statement(compRes, WrapperManager.DOM_NODE_COMPARATOR_TEXT_PROP, new Literal(this.text)));
	return compRes;
    }

    public static NodeComparator fromResource(Resource compRes, IRDFContainer rdfc) throws RDFException {
	return new DOMNodeComparator(rdfc.extract(compRes,
						  WrapperManager.DOM_NODE_COMPARATOR_TAG_NAME_PROP,
						  null).getContent(),
				     rdfc.extract(compRes,
						  WrapperManager.DOM_NODE_COMPARATOR_TEXT_PROP,
						  null).getContent());
    }

}


		/**
		 *  Returns true if the two INode objects' labels are equal, false if not.
		 */
/*		public boolean equals(INode n) {
		    String nLabel = n.getLabel();

		    if (!this.label.equalsIgnoreCase(nLabel)) {
			return false;
		    }

		    /*  can we find a way to compare #text nodes?
			
		      if (this.label.equalsIgnoreCase("#text")) { // #text
			return this.text.equals(((IDOMElement)n).getNodeText());
			}*/

		    /* remove until we figure out a way to preserve attributes
		    HashMap attrs = new HashMap();
		    attrs.put("img", "src"); // maybe want an md5 sum instead of relying on the source?
		    attrs.put("a", "href");
		    attrs.put("form", "action");
		    
		    if (this.attrs.containsKey(this.label)) {
			return compareAttributes((IDOMElement)n1, (IDOMElement)n2, (String)attrs.get(n1Tag));
		    }

		    // The tag didn't appear in the attr hash, so the fact that
		    // the tag names matched means elements are equal.
		    return true;
		}
		    */

		/**
		 *  Compares the given attribute accross the two given elements, returning
		 *  true if its value is the same for both (comparing strings, ignoring case),
		 *  and false if not.
		 */
		/*
		public static boolean compareAttributes(IDOMElement e1, IDOMElement e2, String attr) {
		    if (e1 == null || e2 == null || attr == null) return false;
		    String a1 = e1.getAttribute(attr);
		    String a2 = e2.getAttribute(attr);
		    if (a1 == null || a2 == null) return false;
		    return a1.equalsIgnoreCase(a2);
		    }
		    };*/
