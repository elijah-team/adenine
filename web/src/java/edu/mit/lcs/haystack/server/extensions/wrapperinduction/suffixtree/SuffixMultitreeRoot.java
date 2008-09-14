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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.suffixtree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  An internal node in a suffix multitree.
 *
 *  @author Andrew Hogue
 */
public class SuffixMultitreeRoot extends SuffixMultitreeNode {

    public static final boolean DEBUG = false;

    /**
     *  The children of this node.
     */
    protected HashMap children;

    public SuffixMultitreeRoot() {
	this.children = new HashMap();
    }

    public SuffixMultitreeNode[] getChildren() {
	return (SuffixMultitreeNode[])this.children.values().toArray(new SuffixMultitreeNode[0]);
    }

    public SuffixMultitreeNode getChild(INode key) {
	return (SuffixMultitreeNode)this.children.get(key);
    }

    /**
     *  Adds the given node as a branch out of the root, then passes
     *  control to that child's addSuffixChildren().  
     */
    public SuffixMultitreeRoot addSuffix(INode node) throws SuffixMultitreeException {
	if (this.children.containsKey(node.getTagName())) {
	    // this is sloppy, and assumes that we'll never extract a
	    // SuffixMultitreeLeaf from this HashMap... Which is true,
	    // if the trees are all terminated uniquely at the leaves,
	    // but still sloppy
	    ((SuffixMultitreeInternalNode)children.get(node.getTagName())).addSuffixChildren(node, node.getAncestor(1));
	}
	else {
	    SuffixMultitreeNode suffixChild = null;
	    if (node.getChildNodes().getLength() > 0) {
		suffixChild = new SuffixMultitreeInternalNode();
		((SuffixMultitreeInternalNode)suffixChild).addSuffixChildren(node, node.getAncestor(1));
	    }
	    else {
		suffixChild = new SuffixMultitreeLeaf(node, node.getAncestor(1));
	    }
	    this.children.put(node.getTagName(), suffixChild);
	}

	return this;
    }

    public INode[] getConstructionNodes() {
	return new INode[0];
    }

    public INode getConstructionNode() {
	return null;
    }

    public boolean isLeftDiverse() {
	return true;
    }

    public Set getBranchSets() {
	return new HashSet();
    }

    public String toString() {
	return this.toString(0, "  ");
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	System.out.println("SuffixMultitreeRoot.toString(int,String)");
	out.append("Root:\n");
	Iterator childIter = this.children.values().iterator();
	while (childIter.hasNext()) {
	    out.append(((SuffixMultitreeNode)childIter.next()).toString(depth+1, indent));
	}
	return out.toString();
    }

}


