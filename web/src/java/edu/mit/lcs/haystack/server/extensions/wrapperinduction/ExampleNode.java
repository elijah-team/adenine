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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  Represents a node from an example given by the user
 */
public class ExampleNode extends ElementImpl  {

    protected NodeComparator nodeComparator;

    /**
     *  Recursively constructs a new ExampleNode from the given INode.
     */
    public ExampleNode(INode template) {
	this.nodeComparator = template.getComparator();
	this.tagName = template.toString();
	this.children = new ArrayList();
	this.siblingNo = template.getSiblingNo();
	
	NodeList childNodes = template.getChildNodes();
	for (int i = 0; i < childNodes.getLength(); i++) {
	    ExampleNode newChild = new ExampleNode((INode)childNodes.item(i));
	    this.children.add(newChild);
	    newChild.setParent(this);
	}
    }

    /**
     *  Constructor for use by fromResource()
     */
    protected ExampleNode(String tagName, int siblingNo, NodeComparator nodeComparator) {
	this.tagName = tagName;
	this.siblingNo = siblingNo;
	this.nodeComparator = nodeComparator;
	this.children = new ArrayList();
    }

    public int getSize() {
	int size = 0;
	for (int i = 0; i < this.children.size(); i++) {
	    size += ((INode)this.children.get(i)).getSize();
	}
	size++;			// this node
	return size;
    }
    
    /**
     *  Returns true if the given INode is the same as this node
     *  (based on this node's comparator.
     */
    public boolean equals(INode other) {
	return this.nodeComparator.equals(other);
    }

    public boolean equals(Object other) {
	if (!(other instanceof INode)) return false;
	return this.equals((INode)other);
    }

    public NodeComparator getComparator() {
	return this.nodeComparator;
    }

    public int getChangeCost(INode other) {
	return (this.equals(other)) ? 0 : this.getDeleteCost()+other.getInsertCost();
    }


    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource nodeRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(nodeRes, Constants.s_rdf_type, WrapperManager.EXAMPLE_NODE_CLASS));
	rdfc.add(new Statement(nodeRes,
			       WrapperManager.EXAMPLE_NODE_TAG_NAME_PROP,
			       new Literal(this.getTagName())));
	rdfc.add(new Statement(nodeRes,
			       WrapperManager.EXAMPLE_NODE_SIBLING_NO_PROP,
			       new Literal(String.valueOf(this.getSiblingNo()))));
	rdfc.add(new Statement(nodeRes,
			       WrapperManager.EXAMPLE_NODE_COMPARATOR_PROP,
			       this.nodeComparator.makeResource(rdfc)));

	for (int i = 0; i < this.children.size(); i++) {
	    rdfc.add(new Statement(nodeRes,
				   WrapperManager.EXAMPLE_NODE_CHILD_NODE_PROP,
				   ((ExampleNode)this.children.get(i)).makeResource(rdfc)));
	}
	return nodeRes;
    }

    public static ExampleNode fromResource(Resource nodeRes, IRDFContainer rdfc) throws RDFException {
	System.out.println("Entering ExampleNode.fromResource()");
	String tagName = rdfc.extract(nodeRes,
				      WrapperManager.EXAMPLE_NODE_TAG_NAME_PROP,
				      null).getContent();
	int siblingNo = Integer.parseInt(rdfc.extract(nodeRes,
						      WrapperManager.EXAMPLE_NODE_SIBLING_NO_PROP,
						      null).getContent());
	NodeComparator nodeComparator =
	    NodeComparator.fromResource((Resource)rdfc.extract(nodeRes,
							       WrapperManager.EXAMPLE_NODE_COMPARATOR_PROP,
							       null),
					rdfc);
	ExampleNode node = new ExampleNode(tagName, siblingNo, nodeComparator);

	Set childrenSet = rdfc.query(new Statement[] {new Statement(nodeRes,
								    WrapperManager.EXAMPLE_NODE_CHILD_NODE_PROP,
								    Utilities.generateWildcardResource(1))},
				     Utilities.generateWildcardResourceArray(1),
				     Utilities.generateWildcardResourceArray(1));

	int size = (childrenSet == null) ? 0 : childrenSet.size();
	ExampleNode[] childNodes = new ExampleNode[size];
	if (childrenSet != null) {
	    Iterator iter = childrenSet.iterator();
	    for (int i = 0; iter.hasNext(); i++) {
		System.out.println("ExampleNode.fromResource() (" + tagName + "), loop " + i);
		ExampleNode child = ExampleNode.fromResource((Resource)((RDFNode[])iter.next())[0], rdfc);
		child.setParent(node);
		childNodes[child.getSiblingNo()] = child;
	    }
	}

	ArrayList children = new ArrayList(size);
	for (int i = 0; i < childNodes.length; i++) {
	    if (childNodes[i] == null) continue;
	    children.add(childNodes[i]);
	}
	node.children = children;

	return node;
    }

    

}
