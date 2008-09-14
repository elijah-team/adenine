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

import java.util.Set;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  A node in a suffix multitree.
 *
 *  @author Andrew Hogue
 */
public abstract class SuffixMultitreeNode {

    public abstract String toString(int depth, String indent);

    /**
     *  Returns all children of this node.
     */
    public abstract SuffixMultitreeNode[] getChildren();

    /**
     *  Retrieves the child SuffixMultitreeNode of this node that is
     *  keyed with the given INode.
     */
    public abstract SuffixMultitreeNode getChild(INode key);

    /**
     *  Returns an array of all INodes used to construct this
     *  SuffixMultitreeNode
     */
    public abstract INode[] getConstructionNodes();

    /**
     *  Returns a representative INode from all of those used to
     *  construct this SuffixMultitreeNode
     */
    public abstract INode getConstructionNode();

    /**
     *  Returns true if this node is left-diverse.
     */
    public abstract boolean isLeftDiverse();

    /**
     *  Returns a Set containing all branch sets tying the children of
     *  this node together.
     */
    public abstract Set getBranchSets();

}
