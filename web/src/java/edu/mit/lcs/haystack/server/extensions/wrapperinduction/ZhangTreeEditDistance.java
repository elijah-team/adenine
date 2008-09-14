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
import java.util.Stack;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ITree;

/**
 *  Methods for finding the distance between two ITree objects and
 *  retrieving the mappings that produce that distance.
 *
 *  Based on algorithms from K. Zhang and D. Shasha, "Simple fast
 *  algorithms for the editing distance between trees and related
 *  problems", SIAM Journal on Computing 18 (1989), pp. 1245-1262.
 * 
 *  @author Andrew Hogue
 */
public class ZhangTreeEditDistance {

    public static boolean DEBUG = false;

    protected InternalTree t1;
    protected InternalTree t2;

    protected PostorderMapping[][] bestMaps;
    protected PostorderMapping[][] tempMaps;

    protected boolean distanceComputed;

    public ZhangTreeEditDistance(ITree t1, ITree t2) {
	this((INode)t1.getDocumentElement(), (INode)t2.getDocumentElement());
    }

    public ZhangTreeEditDistance(INode n1, INode n2) {
 	this.t1 = new InternalTree(n1);
	this.t2 = new InternalTree(n2);

	bestMaps = new PostorderMapping[this.t1.getSize()+1][this.t2.getSize()+1];
	tempMaps = new PostorderMapping[this.t1.getSize()+1][this.t2.getSize()+1];
	for (int i = 0; i < bestMaps.length; i++) {
	    for (int j = 0; j < bestMaps[i].length; j++) {
		bestMaps[i][j] = new PostorderMapping();
		tempMaps[i][j] = new PostorderMapping();
	    }
	}

	this.distanceComputed = false;
    }


    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append("bestMaps[][]:\n");
	for (int i = 0; i < bestMaps.length; i++) {
	    out.append("\t");
	    for (int j = 0; j < bestMaps[i].length; j++) {
		out.append(bestMaps[i][j].getCost());
		if (j < bestMaps[i].length-1) {
		    out.append(" ");
		}
	    }
	    out.append("\n");
	}
	return out.toString();
    }


    public String tempMapsToString() {
	StringBuffer out = new StringBuffer();
	out.append("tempMaps[][]:\n");
	for (int i = 0; i < tempMaps.length; i++) {
	    out.append("\t");
	    for (int j = 0; j < tempMaps[i].length; j++) {
		out.append(tempMaps[i][j].getCost());
		if (j < tempMaps[i].length-1) {
		    out.append(" ");
		}
	    }
	    out.append("\n");
	}
	return out.toString();
    }

    protected int treeToStringNodeNo;

    public String tree1ToString() {
	this.treeToStringNodeNo = 1;
	return treeToStringHelper(t1.root, 0, "  ");
    }

    public String tree2ToString() {
	this.treeToStringNodeNo = 1;
	return treeToStringHelper(t2.root, 0, "  ");
    }

    protected String treeToStringHelper(INode n, int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) {
	    out.append(indent);
	}
	out.append(n.getTagName());
	out.append(" ");

	StringBuffer childrenString = new StringBuffer();
	NodeList children = n.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
	    childrenString.append(treeToStringHelper((INode)children.item(i), depth+1, indent));
	}

	out.append("(" + treeToStringNodeNo++ + ")");
	out.append("\n");
	out.append(childrenString.toString());

	return out.toString();	    
    }

    // private debug vars
    private int compsetNo = 0;

    /**
     *  Computes the distance between the two trees, according to the
     *  cost methods in INode.
     */
    public int getDistance() {
	if (DEBUG) System.out.println("getDistance() for compsets of length " + t1.compSet().length + " and " + t2.compSet().length + " (" + (t1.compSet().length*t2.compSet().length) + " total)");
	for (int i = 0; i < t1.compSet().length; i++) {
	    for (int j = 0; j < t2.compSet().length; j++) {
		if (DEBUG) {
		    compsetNo++;
		    if (compsetNo % 50 == 0) System.out.print(".");
		    if (compsetNo % 2500 == 0) System.out.println(String.valueOf(compsetNo));
		}
		subtreeDistance(t1.compSet()[i], t2.compSet()[j]);
	    }
	}

	this.distanceComputed = true;
	return bestMaps[t1.getSize()][t2.getSize()].getCost();
    }

    /**
     *  Computes the distance between the two subtrees rooted at the
     *  given values.  Results are in the treedist matrix.
     *
     *  Modifies tempdist matrix, from [l(subtree1)-1][l(subtree2)] to
     *  [subtree1][subtree2], to contain forest distances generated
     *  during computation.
     */
    protected void subtreeDistance(int subtree1, int subtree2) {
	int l1 = t1.left_most[subtree1];
	int l2 = t2.left_most[subtree2];

	int cost3;
	PostorderMapping map1 = null;
	PostorderMapping map2 = null;
	PostorderMapping map3 = null;
	PostorderMapping map12 = null;

	tempMaps[l1-1][l2-1] = new PostorderMapping();
	for (int i = l1; i <= subtree1; ++i) {
	    tempMaps[i][l2-1] = tempMaps[i-1][l2-1].append(i, t1.getTagName(i), -1, null, t1.getDeleteCost(i));
	}
	for (int j = l2; j <= subtree2; ++j) {
	    tempMaps[l1-1][j] = tempMaps[l1-1][j-1].append(-1, null, j, t2.getTagName(j), t2.getInsertCost(j));
	}
	for (int i = l1; i <= subtree1; ++i) {
	    for (int j = l2; j <= subtree2; ++j) {
		map1 = tempMaps[i-1][j].append(i, t1.getTagName(i), -1, null, t1.getDeleteCost(i));
		map2 = tempMaps[i][j-1].append(-1, null, j, t2.getTagName(j), t2.getInsertCost(j));
		map12 = (map1.getCost() <= map2.getCost()) ? map1 : map2;

		if (t1.left_most[i] == l1 && t2.left_most[j] == l2) {
		    cost3 = t1.getChangeCost(i, t2.nodes[j]);
		    map3 = tempMaps[i-1][j-1].append(i, t1.getTagName(i), j, t2.getTagName(j), cost3);

		    tempMaps[i][j] = (map12.getCost() <= map3.getCost()) ? map12 : map3;
		    bestMaps[i][j] = tempMaps[i][j];
		}
		else {
		    map3 = PostorderMapping.merge(tempMaps[t1.left_most[i]-1][t2.left_most[j]-1], bestMaps[i][j]);
		    tempMaps[i][j] = (map12.getCost() <= map3.getCost()) ? map12 : map3;	    
		}
	    }
	}	    
    }

    /**
     *  Returns the best mapping for this tree.
     */
    public PostorderMapping getMapping() {
	return getMapping(t1.getSize(), t2.getSize());
    }

    public PostorderMapping getMapping(int subtree1, int subtree2) {
	if (!this.distanceComputed) {
	    this.getDistance();
	}
	return bestMaps[subtree1][subtree2];
    }

}

class InternalTree {

    public static boolean DEBUG = ZhangTreeEditDistance.DEBUG;

    public INode root;

    // these two arrays are stored with indices in post-order order
    // along the tree.
    // nodes:
    public INode[] nodes;
    // l(i), the left-most leaf decendent from node i:
    public int[] left_most;

    // LR_keyroots(T), the roots of all subtrees in the tree that need
    // separate computation
    //     LR_keyroots(T) = {k | there exists no k' > k s.t. l(k) = l(k')
    // i.e. if k is in LR_keyroots(t) then either k is the root of T,
    // or l(k) != l(p(k)), i.e. k has a left sibling
    public int[] comp_set;
    

    // temporary variables used during the preprocess traversal
    protected IntStack leftMostStack;
    protected int leftMostIndex;
    protected Stack nodeStack;
    protected ArrayList nodesTemp;
    protected IntArray leftMostTemp;
    protected IntArray compSetTemp;


    public InternalTree(INode n) {
	if (DEBUG) System.out.println("Constructing InternalTree of size " + n.getSize());
	this.root = n;
	preprocess();
	if (DEBUG) System.out.println("done");
    }
    
    protected void preprocess() {
	this.leftMostStack = new IntStack();
	this.leftMostIndex = 1;
	this.nodeStack = new Stack();
	this.nodesTemp = new ArrayList();
	this.leftMostTemp = new IntArray();
	this.compSetTemp = new IntArray();

	// to make the tree arrays 1-based
	this.nodesTemp.add(new ElementImpl("x"));
	this.leftMostTemp.add(0);

	// to prevent a stack exception when we peek at this stack and
	// we're out of nodes
	this.leftMostStack.push(0);

	preprocessHelper(this.root);

	this.nodes = (INode[])nodesTemp.toArray(new INode[0]);
	this.left_most = leftMostTemp.toArray();
	this.comp_set = compSetTemp.toArray();

	this.leftMostStack = null;
	this.nodeStack = null;
	this.nodesTemp = null;
	this.leftMostTemp = null;
	this.compSetTemp = null;
    }
    
    // debug vars
    private int nodeNumber = 0;

    protected void preprocessHelper(INode node) {
	if (DEBUG) {
	    nodeNumber++;
	    if (nodeNumber % 10 == 0) System.out.print(".");
	    if (nodeNumber % 500 == 0) System.out.println(String.valueOf(nodeNumber));
	}
	this.leftMostStack.push(this.leftMostIndex);
	this.nodeStack.push(node);

	NodeList children = node.getChildNodes();
	// traverse in pre-order
	for (int i = 0; i < children.getLength(); i++) {
	    preprocessHelper((INode)children.item(i));
	}
	
	this.leftMostTemp.add(leftMostStack.pop());
	this.nodesTemp.add((INode)nodeStack.pop());

	if (this.leftMostStack.peek() != this.leftMostTemp.get(this.leftMostIndex)) {
	    this.compSetTemp.add(this.leftMostIndex);
	}

	this.leftMostIndex++;
    }

    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append("Nodes(i) [l(i)]:\n");
	for (int i = 0; i < nodes.length; i++) {
	    out.append("\t" + nodes[i].getTagName() + " [" + left_most[i] + "]\n");
	}
	out.append("LR_keyroots: {");
	for (int i = 0; i < comp_set.length; i++) {
	    out.append(comp_set[i]);
	    if (i < comp_set.length-1) {
		out.append(", ");
	    }
	}
	out.append("}\n");
	return out.toString();
    }

    /**
     *  Returns the number of nodes in this tree (which is
     *  nodes.length-1, since nodes[] is 1-based)
     */
    public int getSize() {
	return root.getSize();
    }

    public int[] compSet() {
	return this.comp_set;
    }

    /**
     *  Returns the tagName of the node with the given index (in postorder)
     */
    public String getTagName(int index) {
	return this.nodes[index].getTagName();
    }

    public int getDeleteCost(int index) {
	return this.nodes[index].getDeleteCost();
    }

    public int getInsertCost(int index) {
	return this.nodes[index].getInsertCost();
    }

    public int getChangeCost(int index, INode other) {
	return this.nodes[index].getChangeCost(other);
    }


}


/**
 *  A stack of ints
 */
class IntStack {
    
    protected Stack stack;

    public IntStack() {
	stack = new Stack();
    }

    public void push(int i) {
	stack.push(new Integer(i));
    }

    public int pop() {
	return ((Integer)stack.pop()).intValue();
    }
    
    public int peek() {
	return ((Integer)stack.peek()).intValue();
    }

    public int[] toArray() {
	Integer[] integers = (Integer[])stack.toArray(new Integer[0]);
	int[] ints = new int[integers.length];
	for (int i = 0; i < ints.length; i++) {
	    ints[i] = integers[i].intValue();
	}
	return ints;
    }

}

/**
 *  A flexible array of ints
 */
class IntArray {

    protected ArrayList array;

    public IntArray() {
	this.array = new ArrayList();
    }

    public void add(int i) {
	array.add(new Integer(i));
    }

    public int get(int index) {
	return ((Integer)array.get(index)).intValue();
    }

    public int[] toArray() {
	Integer[] integers = (Integer[])array.toArray(new Integer[0]);
	int[] ints = new int[integers.length];
	for (int i = 0; i < ints.length; i++) {
	    ints[i] = integers[i].intValue();
	}
	return ints;
    }

}


/**
 *  A triple of {int, int, boolean}, representing a possible mapping.
 *  If the boolean is false, the best mapping between subtrees i and j
 *  will be part of the current mapping.  If it is true, (i, j) will
 *  be in the mapping.
 */
class Triple {

    public int i;
    public int j;
    public boolean inMapping;

    public Triple(int i, int j, boolean inMapping) {
	this.i = i; 
	this.j = j; 
	this.inMapping = inMapping; 
    }
}
