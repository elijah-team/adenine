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
import java.util.Date;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ArrayUtils;

/**
 *  Implements the Suffix Tree data structure.  See D. Gusfield,
 *  <i>Algorithms on Strings, Trees, and Sequences</i>, Cambridge
 *  University Press, 1997, chapters 5-7.
 *
 *  @author Andrew Hogue
 */
public class SuffixTree {

    public static final boolean DEBUG = false;

    protected SuffixTreeInternalNode root;

    /**
     *  Creates a suffix tree out of the given string.
     */
    public SuffixTree(String input) {
	Object[] split = input.split("");
	this.construct(ArrayUtils.slice(split, 1, split.length-1)); // get rid of extra "" at beginning of split
    }

    /**
     *  Creates a suffix tree using the given objects, in sequence, as
     *  input.
     */
    public SuffixTree(Object[] input) {
	this.construct(input);
    }

    public void construct(Object[] input) {
	if (this.root == null) this.root = new SuffixTreeInternalNode();
	
	Object[] unique = ArrayUtils.push(input, new UniqueTerminator());
	if (DEBUG) System.out.println("Constructing Suffix Tree with input " + ArrayUtils.toString(unique));
	for (int i = 0; i < unique.length; i++) {
	    if (DEBUG) System.out.println("  Inserting: " + ArrayUtils.toString(ArrayUtils.slice(unique, i, unique.length-1)));
	    this.root.addSuffix(ArrayUtils.slice(unique, i, unique.length-1),
				(i==0) ? null : unique[i-1]);
	}
	//	if (DEBUG) System.out.println(this);
    }

    /**
     *  Retreives all maximal repeats from this tree in a
     *  2-dimensional array.  The first dimension iterates over the
     *  repeats, while the second dimension iterates over the objects
     *  within each repeat.
     */
    public Object[][] getMaximalRepeats() {
	return (Object[][])leftDiverseTraversal(this.root).getRepeats().toArray(new Object[0][]);
    }

    /*
     * 	Performs a depth-first traversal of tree, propagating
     * 	left-characters upwards.  Any node which is left-diverse (that
     * 	is, which has at least 2 leaves in its subtree with different
     * 	left-characters) adds its path-label to the return ArrayList.
     */
    protected LeftDiverseIndicator leftDiverseTraversal(SuffixTreeNode current) {
	if (current instanceof SuffixTreeLeaf) {
	    return new LeftDiverseIndicator(((SuffixTreeLeaf)current).getLeftCharacter());
	}
	else {
	    SuffixTreeBranch[] branches = ((SuffixTreeInternalNode)current).getBranches();
	    LeftDiverseIndicator thisInd = leftDiverseTraversal(branches[0].getTerminal());

	    if (DEBUG) System.out.println("  branch 0: " + ArrayUtils.toString(branches[0].getElements()));
	    if (thisInd.isLeftDiverse()) { // add repeats from first branch
		ArrayList currReps = thisInd.getRepeats();		
		if (DEBUG) System.out.println("  First branch reps:     " + repsToString(currReps));
		for (int j = 0; j < currReps.size(); j++) {
		    currReps.set(j, ArrayUtils.shift((Object[])currReps.get(j), branches[0].getElements()));
		}
		if (DEBUG) System.out.println("  First branch new reps: " + repsToString(currReps));
	    }

	    for (int i = 1; i < branches.length; i++) {
		if (DEBUG) System.out.println("  branch " + i + ": " + ArrayUtils.toString(branches[i].getElements()));
		LeftDiverseIndicator currInd = leftDiverseTraversal(branches[i].getTerminal());
		if (thisInd.equalCommonElements(currInd)) {
		    continue;
		}
		else {
		    ArrayList currReps = currInd.getRepeats();
		    if (DEBUG) System.out.println("  branch " + i + " reps:     " + repsToString(currReps));		    

		    for (int j = 0; j < currReps.size(); j++) {
			currReps.set(j, ArrayUtils.shift((Object[])currReps.get(j), branches[i].getElements()));
		    }

		    if (DEBUG) System.out.println("  branch " + i + " new reps: " + repsToString(currReps));		    

		    thisInd.addRepeats(currReps);

		    if (DEBUG) System.out.println("  branch " + i + " all reps: " + repsToString(thisInd.getRepeats()));		    
		}
	    }

	    if (thisInd.isLeftDiverse() && current != this.root) {
		thisInd.addRepeat(new Object[0]);
	    }

	    return thisInd;
	}
    }

    protected String repsToString(ArrayList reps) {
	StringBuffer out = new StringBuffer();
	out.append("[");
	for (int i = 0; i < reps.size(); i++) {
	    out.append(ArrayUtils.toString((Object[])reps.get(i)));
	    if (i != reps.size()-1) out.append(",");
	}
	out.append("]");
	return out.toString();
    }

    public String toString() {
	return "Root:" + this.root.toString(1, "   ");
    }
			

    /////////////////////////
    //////  TEST CODE  //////
    /////////////////////////

    public static void main(String[] argv) throws Exception {
	testTraversals("subtrees");
	testTraversals("siblings");
    }

    /**
     *  Tests the traversals of the tree.
     */
    public static void testTraversals(String key) {
// 	System.out.println("\n>>> POSTORDER <<< ");
// 	SuffixTree postorder = new SuffixTree(TestTrees.getNodesPostorder(key));
// 	//	System.out.println(postorder);
// 	printRepeats(postorder);
// 	System.out.println("\n>>> INORDER <<< ");
// 	SuffixTree inorder = new SuffixTree(TestTrees.getNodesInorder(key));
// 	//	System.out.println(inorder);
// 	printRepeats(inorder);
// 	System.out.println("\n>>> PREORDER <<< ");
// 	SuffixTree preorder = new SuffixTree(TestTrees.getNodesPreorder(key));
// 	//	System.out.println(preorder);
// 	printRepeats(preorder);
// 	System.out.println("\n>>> PREORDER-ALL <<< ");
// 	SuffixTree preorderAll = new SuffixTree(TestTrees.getNodesPreorderAll(key));
// 	//	System.out.println(preorderAll);
// 	printRepeats(preorderAll);
// 	System.out.println("\n>>> BFS <<< ");
// 	SuffixTree bfs = new SuffixTree(TestTrees.getNodesBFS(key));
// 	//	System.out.println(bfs);
// 	printRepeats(bfs);

	System.out.println("\n>>> ROOT->LEAF <<< ");
	Object[][] rootToLeaf = TestTrees.getRootToLeafPaths(key, false);
	SuffixTree rootToLeafTree = new SuffixTree(rootToLeaf[0]);
	for (int i = 1; i < rootToLeaf.length; i++) {
	    rootToLeafTree.construct(rootToLeaf[i]);
	}
	System.out.println(rootToLeafTree);
	printRepeats(rootToLeafTree);

// 	System.out.println("\n>>> LEAF->ROOT <<< ");
// 	Object[][] leafToRoot = TestTrees.getLeafToRootPaths(key);
// 	SuffixTree leafToRootTree = new SuffixTree(leafToRoot[0]);
// 	for (int i = 1; i < leafToRoot.length; i++) {
// 	    leafToRootTree.construct(leafToRoot[i]);
// 	}
// 	System.out.println(leafToRootTree);
// 	printRepeats(leafToRootTree);

    }

    public static void testBasic() {
	try {
	    SuffixTree t = new SuffixTree("xabjxabcabj");
	    System.out.println(t);

	    printRepeats(t);
	}
	catch (Throwable e) {
	    try { Thread.sleep(500); } catch(InterruptedException ee) { }
	    e.printStackTrace();
	}
    }
			
    protected static void printRepeats(SuffixTree t) {
	Object[][] repeats = t.getMaximalRepeats();
	System.out.println("Repeats:");
	for (int i = 0; i < repeats.length; i++) {
	    System.out.println(ArrayUtils.toString(repeats[i]));
	}
    }
	    

}
    

class UniqueTerminator {

    protected static long seedStamp;

    static {
	seedStamp = new Date().getTime();
    }

    protected long uniqueStamp;
    protected String stringRep;

    public UniqueTerminator() {
	this.uniqueStamp = getUniqueStamp();
    }

    protected synchronized long getUniqueStamp() {
	return ++seedStamp;
    }

    public boolean equals(Object other) {
	if (!(other instanceof UniqueTerminator)) return false;
	return ((UniqueTerminator)other).uniqueStamp == this.uniqueStamp;
    }

    public int hashCode() {
	return String.valueOf(uniqueStamp).hashCode();
    }

    public String toString() {
	if (this.stringRep == null) {
	    this.stringRep = "$" + (this.uniqueStamp % 1000);
	}
	return this.stringRep;
    }
    
}

/**
 *  Stores an indicator for a node as to whether it is left-diverse or
 *  not (that is, whether any two leaves in its subtree have different
 *  left-characters.  If it is left-diverse, stores the maximal
 *  repeats of all nodes in the subtree.  If not, stores the common
 *  element of all leaves in the subtree.
 */
class LeftDiverseIndicator {

    protected boolean isLeftDiverse;

    protected Object commonElement;
    protected ArrayList repeats;

    public LeftDiverseIndicator() {
	this.isLeftDiverse = false;
	this.commonElement = null;
	this.repeats = new ArrayList();
    }

    public LeftDiverseIndicator(Object commonElement) {
	this.isLeftDiverse = false;
	this.commonElement = commonElement;
	this.repeats = new ArrayList();
    }

    public LeftDiverseIndicator(ArrayList repeats) {
	this.commonElement = null;
	this.isLeftDiverse = true;
	this.repeats = repeats;
    }

    public void setLeftDiverse(ArrayList repeats) {
	this.isLeftDiverse = true;
	this.repeats = repeats;
    }

    // implicitly sets left diverse
    public void addRepeats(ArrayList repeats) {
	this.isLeftDiverse = true;
	this.repeats.addAll(repeats);
    }

    // implicitly sets left diverse
    public void addRepeat(Object[] elements) {
	this.isLeftDiverse = true;
	this.repeats.add(elements);
    }

    public ArrayList getRepeats() {
	return this.repeats;
    }

    public boolean isLeftDiverse() {
	return this.isLeftDiverse;
    }

    public Object getCommonElement() {
	return this.commonElement;
    }

    /**
     *  Two LeftDiverseIndicators are equal if they have the same common elements.
     */
    public boolean equalCommonElements(LeftDiverseIndicator other) {
	if (this.commonElement == null && other.commonElement == null) return true;
	if (this.commonElement == null || other.commonElement == null) return false;
	if (this.isLeftDiverse() || other.isLeftDiverse()) return false;
	return this.commonElement.equals(other.commonElement);
    }

}
