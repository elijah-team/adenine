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

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ArrayUtils;

/**
 *  A single branch in a suffix tree.
 *
 *  @author Andrew Hogue
 */
public class SuffixTreeBranch {

    public static final boolean DEBUG = true;

    protected Object[] elements;

    /**
     *  The node that terminates this branch, either an internal node or a leaf.
     */
    protected SuffixTreeNode terminal;

    public SuffixTreeBranch(Object[] elements, Object leftCharacter) {
	this.elements = elements;
	this.terminal = new SuffixTreeLeaf(leftCharacter);
    }

    public SuffixTreeBranch(Object[] elements, SuffixTreeNode terminal) {
	this.elements = elements;
	this.terminal = terminal;
    }

    public Object[] getElements() {
	return this.elements;
    }

    public Object getElement(int index) {
	return elements[index];
    }

    public SuffixTreeNode getTerminal() {
	return this.terminal;
    }

    public int getLength() {
	return elements.length;
    }

    /**
     *  Splits this branch at the given index, creating a new node in
     *  the middle with a single branch, and returning that new node.
     *  The split is made _before_ the node at the given index.
     */
    public SuffixTreeInternalNode split(int index) {
	SuffixTreeBranch newBranch = new SuffixTreeBranch(ArrayUtils.slice(this.elements, index, this.elements.length-1),
							  this.terminal);
	this.elements = ArrayUtils.slice(this.elements, 0, index-1);
	this.terminal = new SuffixTreeInternalNode().addBranch(newBranch);;
	return (SuffixTreeInternalNode)this.terminal;
    }

    public String toString() {
	return this.toString(0, "  ");
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	out.append(ArrayUtils.toString(this.elements));
	out.append(terminal.toString(depth, indent));
	return out.toString();
    }

}
