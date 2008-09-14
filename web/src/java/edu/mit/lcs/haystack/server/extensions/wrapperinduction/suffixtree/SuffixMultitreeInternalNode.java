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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  An internal node in a suffix multitree.
 *
 *  @author Andrew Hogue
 */
public class SuffixMultitreeInternalNode extends SuffixMultitreeNode {

    public static final boolean DEBUG = false;

    /**
     *  The INodes which were used to construct this node
     */
    protected ArrayList constructionNodes;

    /**
     *  An indicator as to whether this node is left diverse
     */
    protected MultitreeLeftDiverseIndicator leftDiverseIndicator;

    /**
     *  The children of this node.  Maps an INodes used to construct
     *  this multitree node's children to SuffixMultitreeNodes that
     *  _are_ the children of this node.
     */
    protected HashMap children;

    /**
     *  The branch sets of this node.  Branch sets represent node
     *  labels which have been "seen together" while building this
     *  multitree.
     */
    protected Set branchSets;

    public SuffixMultitreeInternalNode() {
	this.children = new HashMap();
	this.branchSets = new HashSet();
	this.constructionNodes = new ArrayList();
    }

    public SuffixMultitreeNode[] getChildren() {
	return (SuffixMultitreeNode[])this.children.values().toArray(new SuffixMultitreeNode[0]);
    }

    public SuffixMultitreeNode getChild(INode key) {
	return (SuffixMultitreeNode)this.children.get(key);
    }

    public Set getBranchSets() {
	return this.branchSets;
    }

    /**
     *  Returns the number of times this node was traversed during
     *  construction.
     */
    public int getNoTraversals() {
	return this.constructionNodes.size();
    }

    /**
     *  Returns the INodes which were used to construct this node.
     */
    public INode[] getConstructionNodes() {
	return (INode[])this.constructionNodes.toArray(new INode[0]);
    }

    /**
     *  Returns one of the INodes which was used to construct this node.
     */
    public INode getConstructionNode() {
	if (this.constructionNodes.size() > 0) {
	    return (INode)this.constructionNodes.get(0);
	}
	else {
	    return null;
	}
    }

    /**
     *  Adds all paths of the given node as branches out of this node.
     *  @return this node
     */
    protected SuffixMultitreeInternalNode addSuffixChildren(INode node, INode leftNode) throws SuffixMultitreeException {
	if (this.constructionNodes.size() > 0 && 
	    !((INode)this.constructionNodes.get(0)).equals(node)) {
	    // something's wrong - only equal nodes should go down the same branch!
	    throw new SuffixMultitreeException("Error constructing nodes, node " + node + " doesn't match this multitree node's existing construction nodes (" + this.constructionNodes.get(0) + ")");
	}
	this.constructionNodes.add(node);
	
	if (this.leftDiverseIndicator == null)
	    this.leftDiverseIndicator = new MultitreeLeftDiverseIndicator(leftNode);
	else 
	    this.leftDiverseIndicator.addNode(leftNode);

	NodeList nodeChildren = node.getChildNodes();
	BranchSet branchSet = new BranchSet(nodeChildren);
	this.branchSets.add(branchSet);
	for (int i = 0; i < nodeChildren.getLength(); i++) {
	    if (!nodeChildren.item(i).hasChildNodes()) {
		this.children.put((INode)nodeChildren.item(i), new SuffixMultitreeLeaf((INode)nodeChildren.item(i), leftNode));
	    }
	    else {
		if (getSuffixChild((INode)nodeChildren.item(i)).addSuffixChildren((INode)nodeChildren.item(i), leftNode).isLeftDiverse()) {
		    this.leftDiverseIndicator.isLeftDiverse = true;
		}
	    }
	}
	return this;
    }	

    protected SuffixMultitreeInternalNode getSuffixChild(INode key) {
	SuffixMultitreeInternalNode suffixChild = null;
	if (this.children.containsKey(key)) {
	    suffixChild = (SuffixMultitreeInternalNode)children.get(key);
	}
	else {
	    suffixChild = new SuffixMultitreeInternalNode();
	    this.children.put(key, suffixChild);
	}
	return suffixChild;
    }

    public boolean isLeftDiverse() {
	return this.leftDiverseIndicator.isLeftDiverse;
    }

    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append(this.getConstructionNode().getTagName());
	out.append("   (");
	out.append(this.getNoTraversals());
	out.append(")   ");
	out.append(branchSets);
	return out.toString();
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) out.append(indent);
	out.append(this.getConstructionNode().getTagName());
	out.append("   (");
	out.append(this.getNoTraversals());
	out.append(")   ");
	out.append(branchSets);
	if (this.isLeftDiverse()) 
	    out.append("   **LD**");
	out.append("\n");
	
	Iterator childIter = this.children.values().iterator();
	while (childIter.hasNext()) {
	    out.append(((SuffixMultitreeNode)childIter.next()).toString(depth+1, indent));
	}

	return out.toString();
    }

    /**
     *  Two SuffixMultitreeInternalNodes are considered equal if their
     *  constructionNodes are equal.
     */
    public boolean equals(Object other) {
	if (!(other instanceof SuffixMultitreeInternalNode)) return false;
	return this.constructionNodes.get(0).equals(((SuffixMultitreeInternalNode)other).getConstructionNodes()[0]);
    }

}


/**
 *  Stores an indicator for a node as to whether it is left-diverse or
 *  not (that is, whether any two leaves in its subtree have different
 *  left-characters).
 */
class MultitreeLeftDiverseIndicator {

    public boolean isLeftDiverse;
    public INode commonNode;

    /**
     *  Creates a new left-diverse indicator with the given common
     *  node.
     */
    public MultitreeLeftDiverseIndicator(INode commonNode) {
	this.isLeftDiverse = false;
	this.commonNode = commonNode;
    }

    /**
     *  Adds the given left-node to this indicator.  If it is
     *  different than the current commonNode, or if at least one of
     *  them is null, it makes this indicator true (that is, the node
     *  is now left-diverse).
     */
    public boolean addNode(INode node) {
	if (this.commonNode == null || !this.commonNode.equals(node)) {
	    this.isLeftDiverse = true;
	}
	return this.isLeftDiverse;
    }
    
    public String toString() {
	if (this.isLeftDiverse) 
	    return "true";
	else 
	    return "false (commonNode=" + commonNode + ")";
	    
    }

}
