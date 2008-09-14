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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  Represents a set of labels of branches in a suffix multitree.
 */
public class BranchSet extends HashSet {

    public BranchSet(NodeList nodes) {
	for (int i = 0; i < nodes.getLength(); i++) {
	    this.add(((INode)nodes.item(i)).getTagName());
	}
    }

    /** 
     *  Returns true if the tagName of the given node is contained in
     *  this BranchSet.
     */
    public boolean containsNode(INode node) {
	return this.containsTagName(node.getTagName());
    }

    /** 
     *  Returns true if the tagName of the given node is contained in
     *  this BranchSet.
     */
    public boolean containsTagName(String tagName) {
	return this.contains(tagName);
    }

    /**
     *  Two branch sets are considered equal if they contain the same
     *  tagNames.
     */
    public boolean equals(Object o) {
	if (!(o instanceof BranchSet)) return false;
	BranchSet other = (BranchSet)o;
	if (this.size() != other.size()) return false;
	Iterator thisIter = this.iterator();
	while (thisIter.hasNext()) {
	    if (!other.contains(thisIter.next())) return false;
	}
	return true;
    }

    /**
     *  Given a set of child nodes, returns the subset which is
     *  contained in this branch set.
     */
    public SuffixMultitreeNode[] getNodes(SuffixMultitreeNode[] children) {
	ArrayList contained = new ArrayList();
	for (int i = 0; i < children.length; i++) {
	    if (this.containsNode(children[i].getConstructionNode())) {
		contained.add(children[i]);
	    }
	}
	return (SuffixMultitreeNode[])contained.toArray(new SuffixMultitreeNode[0]);
    }


    /**
     *  Given a set of BranchSets and a set of nodes which is
     *  left-diverse, returns unique sets of SuffixMultitreeNodes that
     *  appear together in at least one of the given BranchSets.  The
     *  first dimension of the return array iterates over the sets,
     *  the second dimension over the SuffixMultitreeNodes in each
     *  set.
     */
    public static SuffixMultitreeNode[][] getUniqueBranchSetNodes(Set branchSets,
								  SuffixMultitreeNode[] leftDiverseNodes) {
	Iterator branchSetIter = branchSets.iterator();
	Set nodeSets = new HashSet();
	while (branchSetIter.hasNext()) {
	    BranchSet currSet = (BranchSet)branchSetIter.next();
	    SuffixMultitreeNode[] currLeftDiverseNodes = currSet.getNodes(leftDiverseNodes);
	    if (currLeftDiverseNodes.length > 0) {
		nodeSets.add(new SuffixMultitreeNodeSet(currLeftDiverseNodes));
	    }
	}

	SuffixMultitreeNode[][] returnNodes = new SuffixMultitreeNode[nodeSets.size()][];
	Iterator nodeSetsIter = nodeSets.iterator();
	for (int i = 0; nodeSetsIter.hasNext(); i++) {
	    returnNodes[i] = (SuffixMultitreeNode[])((SuffixMultitreeNodeSet)nodeSetsIter.next()).toArray(new SuffixMultitreeNode[0]);
	}

	return returnNodes;
    }

}

/**
 *  Represents a set of SuffixMultitreeNodes.  
 */
class SuffixMultitreeNodeSet extends HashSet {

    public SuffixMultitreeNodeSet(SuffixMultitreeNode[] nodes) {
	for (int i = 0; i < nodes.length; i++) {
	    this.add(nodes[i]);
	}
    }

    /**
     *  Two SuffixMultitreeNodeSets are considered equal if they
     *  contain the SuffixMultitreeNodes which are equal.
     */
    public boolean equals(Object o) {
	if (!(o instanceof SuffixMultitreeNodeSet)) return false;
	SuffixMultitreeNodeSet other = (SuffixMultitreeNodeSet)o;
	if (this.size() != other.size()) return false;
	Iterator thisIter = this.iterator();
	while (thisIter.hasNext()) {
	    if (!other.contains(thisIter.next())) return false;
	}
	return true;
    }
}
    
