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

import java.util.HashSet;
import java.util.Set;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  A leaf in a suffix tree.
 *
 *  @author Andrew Hogue
 */
public class SuffixMultitreeLeaf extends SuffixMultitreeNode {

    public static final boolean DEBUG = true;
    
    protected INode constructionNode;

    /**
     *  The left character of a leaf is the character immediately
     *  preceeding the suffix represented by this leaf.  Because we
     *  insist on a unique terminator for each string added to the
     *  tree, each leaf has a unique left character.  In a multitree,
     *  the "left" node is actually the parent node of the subtree
     *  this suffix represents.
     */
    protected INode leftNode;

    public SuffixMultitreeLeaf(INode thisNode, INode leftNode) {
	this.constructionNode = thisNode;
	this.leftNode = leftNode;
    }

    public INode[] getConstructionNodes() {
	return new INode[] {this.constructionNode};
    }

    public INode getConstructionNode() {
	return this.constructionNode;
    }

    public boolean isLeftDiverse() {
	return false;
    }

    public Set getBranchSets() {
	return new HashSet();
    }

    public INode getLeftNode() {
	return this.leftNode;
    }

    public SuffixMultitreeNode[] getChildren() {
	return new SuffixMultitreeNode[0];
    }

    public SuffixMultitreeNode getChild(INode key) {
	return null;
    }

    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append(this.constructionNode.getTagName());
	out.append("   ");
	out.append("[lc = ");
	out.append(this.leftNode);
	out.append("]\n");
	return out.toString();
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) out.append(indent);
	out.append(this.constructionNode.getTagName());
	out.append("   ");
	out.append("[lc = ");
	out.append(this.leftNode);
	out.append("]\n");
	return out.toString();
    }

}
