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
import java.util.Iterator;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ArrayUtils;

/**
 *  An internal node in a suffix tree.
 *
 *  @author Andrew Hogue
 */
public class SuffixTreeInternalNode extends SuffixTreeNode {

    public static final boolean DEBUG = false;

    /**
     *  Maps first characters to SuffixTreeBranch objects.
     */
    protected HashMap branches;

    public SuffixTreeInternalNode() {
	this.branches = new HashMap();
    }

    public SuffixTreeBranch[] getBranches() {
	return (SuffixTreeBranch[])branches.values().toArray(new SuffixTreeBranch[0]);
    }

    public SuffixTreeNode[] getChildren() {
	SuffixTreeBranch[] branchArray = this.getBranches();
	ArrayList children = new ArrayList();
	for (int i = 0; i < branchArray.length; i++) {
	    children.add(branchArray[i].getTerminal());
	}
	return (SuffixTreeNode[])children.toArray(new SuffixTreeNode[0]);
    }

    public SuffixTreeInternalNode addBranch(SuffixTreeBranch branch) {
	this.branches.put(branch.getElement(0), branch);
	return this;
    }

    public SuffixTreeInternalNode addSuffix(Object[] input, Object leftCharacter) {
	if (input.length == 0) return this;
	if (DEBUG) System.out.println("    Current branches: " + this.branches.keySet());
	if (branches.containsKey(input[0])) {
	    SuffixTreeBranch branch = (SuffixTreeBranch)branches.get(input[0]);
	    if (DEBUG) System.out.print("    (" + input.length + "|" + branch.getElements().length + ") Found branch: " + input[0] + "|" + branch.getElement(0));

	    if (branch.getLength() == 1) {
		// pass on to terminal node
		if (DEBUG) System.out.print(" ==> Passing on " + ArrayUtils.toString(ArrayUtils.slice(input, 1, input.length-1)) + "\n");
		((SuffixTreeInternalNode)branch.getTerminal()).addSuffix(ArrayUtils.slice(input, 1, input.length-1), leftCharacter);
	    }

	    for (int i = 1; i < input.length && i < branch.getLength(); i++) {
		if (DEBUG) System.out.print(" " + input[i] + "|" + branch.getElement(i));
		if (!input[i].equals(branch.getElement(i))) {
		    if (DEBUG) System.out.print(" >> split!");
		    // split branch, create new node
		    branch.split(i).addBranch(new SuffixTreeBranch(ArrayUtils.slice(input, i, input.length-1), leftCharacter));
		    break;
		}
		if (i == branch.getLength()-1) {
		    // pass on to terminal node
		    if (DEBUG) System.out.print(" ==> Passing on " + ArrayUtils.toString(ArrayUtils.slice(input, i+1, input.length-1)));
		    ((SuffixTreeInternalNode)branch.getTerminal()).addSuffix(ArrayUtils.slice(input, i+1, input.length-1), leftCharacter);
		    break;
		}
	    }
	    if (DEBUG) System.out.println();
	}
	else {
	    // no branch exists - just add all of input as new branch.
	    branches.put(input[0], new SuffixTreeBranch(input, leftCharacter));
	}

	return this;
    }
    
    public String toString() {
	return this.toString(0, "  ");
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	out.append("\n");
	Iterator branchIter = this.branches.values().iterator();
	while (branchIter.hasNext()) {
	    for (int i = 0; i < depth; i++) out.append(indent);
	    SuffixTreeBranch next = (SuffixTreeBranch)branchIter.next();
	    out.append(next.toString(depth+1, indent));
	}
	return out.toString();
    }


}
