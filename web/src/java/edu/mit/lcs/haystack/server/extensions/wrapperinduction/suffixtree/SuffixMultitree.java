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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DocumentImpl;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeComparator;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeListImpl;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.StringNodeComparator;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ITree;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 *  An extension to the Suffix Tree data structure, allowing 
 *  trees to be inserted rather than strings.
 *
 *  @author Andrew Hogue
 */
public class SuffixMultitree {

    public static final boolean DEBUG = false;

    protected SuffixMultitreeRoot root;

    /**
     *  Creates a suffix multitree out of the given tree.
     */
    public SuffixMultitree(ITree tree) throws SuffixMultitreeException {
	this.construct((INode)tree.getDocumentElement().cloneNode(true));
    }

    public void construct(INode node) throws SuffixMultitreeException {
	if (this.root == null) this.root = new SuffixMultitreeRoot();
	
	INode[] preorder = node.getPreorderNodes();

	UniqueTerminatorNode.addUniqueTerminators(node);

	for (int i = 1; i < preorder.length; i++) {
	    if (DEBUG) System.out.println("  Inserting: " + preorder[i]);
	    this.root.addSuffix(preorder[i]);
	}
    }

    /**
     *  Retreives all maximal repeats from this multitree, each as a
     *  separate ITree.
     */
    public ITree[] getMaximalRepeats() {
	SuffixMultitreeNode[] rootChildren = this.root.getChildren();
	ArrayList nodes = new ArrayList();
	for (int i = 0; i < rootChildren.length; i++) {
	    if (rootChildren[i].isLeftDiverse()) {
		ArrayList roots = new ArrayList();
		INode firstRoot = (INode)rootChildren[i].getConstructionNode().cloneNode(false);
		roots.add(firstRoot);
		leftDiverseTraversal(rootChildren[i],
				     firstRoot,
				     new NodeID(0),
				     roots);
		nodes.addAll(roots);
	    }
	}

	ITree[] trees = new ITree[nodes.size()];
	for (int i = 0; i < nodes.size(); i++) {
	    trees[i] = new DocumentImpl((INode)nodes.get(i));
	}

	return trees;
    }

    
    /**
     *  Recursively traverses the multitree from the given node,
     *  maintaining an ArrayList containing the INode roots of the
     *  maximal repeats in the subtree.
     */
    protected void leftDiverseTraversal(SuffixMultitreeNode current,
					INode currentRoot,
					NodeID currentNodeID,
					ArrayList allRoots) {
	if (DEBUG) System.out.println(">> leftDiverseTraversal() with :\n\tcurrent = " + current + "\n\tcurrentRoot = " + currentRoot + "\n\tcurrentNodeID = " + currentNodeID + "\n\tallRoots = " + allRoots);

	SuffixMultitreeNode[] currentChildren = current.getChildren();
	Set leftDiverse = new HashSet();
	for (int i = 0; i < currentChildren.length; i++) {
	    if (currentChildren[i].isLeftDiverse()) {
		leftDiverse.add(currentChildren[i]);
	    }
	}
	SuffixMultitreeNode[] currentLeftDiverseChildren = (SuffixMultitreeNode[])leftDiverse.toArray(new SuffixMultitreeNode[0]);

	List toReturn = new ArrayList();
	SuffixMultitreeNode[][] uniqueBranchSetNodes = BranchSet.getUniqueBranchSetNodes(current.getBranchSets(),
											 currentLeftDiverseChildren);

	for (int i = 0; i < uniqueBranchSetNodes.length; i++) {
	    // only clone the root if we have more than one iteration to go.
	    INode newRoot = null;
	    if (i < uniqueBranchSetNodes.length - 1) {
		newRoot = (INode)currentRoot.cloneNode(true);
		allRoots.add(newRoot);
	    }
	    else {
		newRoot = currentRoot;
	    }
	    
	    INode newCurrentINode = currentNodeID.getNodes(newRoot)[0];
		
	    for (int j = 0; j < uniqueBranchSetNodes[i].length; j++) {
		try {
		    NodeID newNodeID = currentNodeID.makeChildNodeID(j);
		    newCurrentINode.appendChild(uniqueBranchSetNodes[i][j].getConstructionNode().cloneNode(false));
		    
		    leftDiverseTraversal(uniqueBranchSetNodes[i][j],
					 newRoot,
					 newNodeID,
					 allRoots);
		}
		catch (NodeIDException e) {
		    // this shouldn't happen
		    e.printStackTrace();
		}
	    }
	}
    }

    public String toString() {
	return this.root.toString(0, "   ");
    }
			

    /////////////////////////
    //////  TEST CODE  //////
    /////////////////////////

    public static void main(String[] argv) throws Exception {
	try {
	    testTraversals("simple");
	    testTraversals("simple2");
	    testTraversals("subtrees");
	    testTraversals("siblings");
	}
	catch (Throwable e) {
	    e.printStackTrace();
	}
    }

    /**
     *  Tests the traversals of the tree.
     */
    public static void testTraversals(String key) throws SuffixMultitreeException {
	SuffixMultitree multitree = new SuffixMultitree(TestTrees.getTree(key));
	System.out.println(TestTrees.getTree(key));
	System.out.println(multitree);
	printRepeats(multitree, 0);
    }

    public static void printRepeats(SuffixMultitree t, int minSize) {
	ITree[] repeats = t.getMaximalRepeats();
	System.out.println("Repeats:");
	int count = 0; 
	for (int i = 0; i < repeats.length; i++) {
	    if (repeats[i].getSize() >= minSize) {
		System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=");
		System.out.println(repeats[i]);
		count++;
	    }
	}
	System.out.println("Total: " + count + " repeats of at least size " + minSize);
    }
	    

}
    

class UniqueTerminatorNode implements INode {

    protected static long seedStamp;

    static {
	seedStamp = new Date().getTime();
    }

    protected INode parent;

    protected long uniqueStamp;
    protected String stringRep;

    public UniqueTerminatorNode() {
	this.uniqueStamp = getUniqueStamp();
    }

    protected synchronized long getUniqueStamp() {
	return ++seedStamp;
    }

    public boolean equals(Object other) {
	if (!(other instanceof UniqueTerminator)) return false;
	return ((UniqueTerminator)other).uniqueStamp == this.uniqueStamp;
    }

    public boolean equals(INode other) {
	return equals((Object)other);
    }

    public int hashCode() {
	return String.valueOf(uniqueStamp).hashCode();
    }

    public String toString() {
	if (this.stringRep == null) {
	    this.stringRep = "$" + (this.uniqueStamp % 100000);
	}
	return this.stringRep;
    }
    
    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) out.append(indent);
	out.append(this.toString());
	out.append("\n");
	return out.toString();
    }
    
    /**
     *  Recurses down the tree rooted at the given node and appends a
     *  unique terminator node to each leaf.
     */
    public static void addUniqueTerminators(INode node) {
	NodeList children = node.getChildNodes();
	if (children.getLength() == 0) {
	    UniqueTerminatorNode term = new UniqueTerminatorNode();
	    term.setParent(node);
	    node.appendChild(term);
	}
	else {
	    for (int i = 0; i < children.getLength(); i++) {
		addUniqueTerminators((INode)children.item(i));
	    }
	}
    }

    //// INode interface methods ////

    /**
     *  UNIMPLEMENTED
     */
    public NodeID getNodeID() {
	return null;
    }

    public int getSize() {
	return 1;
    }

    public int getHeight() {
	return 1;
    }

    public INode getChild(int index) {
	return null;
    }

    public INode[] getChildren(String tagName) {
	return new INode[0];
    }

    public INode[] getPostorderNodes() {
	return new INode[] {this};
    }

    public void getPostorderNodesHelper(List nodes) {
	nodes.add(this);
	return;
    }

    public INode[] getPreorderNodes() {
	return new INode[] {this};
    }

    public void getPreorderNodesHelper(List nodes) {
	nodes.add(this);
	return;
    }

    public int getSiblingNo() {
	return 0;
    }

    public void setSiblingNo(int siblingNo) {
    }

    public boolean isOnlyChild() {
	return true;		// by definition
    }

    public INode getAncestor(int generation) {
	if (generation == 0) return this;
	if (this.parent == null) return null;
	return this.parent.getAncestor(generation-1);
    }

    public void setParent(INode parent) {
	this.parent = parent;
    }					  

    public NodeList getSiblings() {
	return new NodeListImpl();
    }
    
    public INode removeNode() {
	return (INode)this.getAncestor(1).removeChild(this);
    }

    public List removeChildNodes() {
	return new ArrayList();
    }

    public NodeComparator getComparator() {
	return new StringNodeComparator(String.valueOf(uniqueStamp));
    }

    public int getDeleteCost() {
	return 0;
    }

    public int getInsertCost() {
	return 0;
    }

    public int getChangeCost(INode other) {
	return 0;
    }

    ///////////////////////////////////
    /// org.w3c.dom.Element methods ///
    ///////////////////////////////////

    public String getAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Attr getAttributeNode(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public String getAttributeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public NodeList getElementsByTagName(String name) {
	return new NodeListImpl();
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public String getTagName() {
	return this.toString();
    }

    public boolean hasAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public boolean hasAttributeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public void removeAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Attr removeAttributeNode(Attr oldAttr) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public void removeAttributeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public void setAttribute(String name, String value) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Attr setAttributeNode(Attr newAttr) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Attr setAttributeNodeNS(Attr newAttr) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }


    ///////////////////////////////////
    ///  org.w3c.dom.Node methods   ///
    ///////////////////////////////////

    /**
     *  Appends the given child to this node, returning this node.
     */
    public Node appendChild(Node newChild) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Node cloneNode(boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");	
    }

    public NamedNodeMap getAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public NodeList getChildNodes() {
	return new NodeListImpl();
    }

    public Node getFirstChild() {
	return null;
    }

    public Node getLastChild() {
	return null;
    }

    public String getLocalName() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public String getNamespaceURI() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Node getNextSibling() {
	return null;
    }

    public String getNodeName() {
	return this.getTagName();
    }

    public short getNodeType() {
	return Node.ELEMENT_NODE;
    }

    public String getNodeValue() {
	return null;		// as per DOM spec
    }

    public Document getOwnerDocument() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");	
    }

    public Node getParentNode() {
	return this.parent;
    }

    public String getPrefix() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");	
    }

    public Node getPreviousSibling() {
	return null;
    }

    public boolean hasAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public boolean hasChildNodes() {
	return false;
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");		
    }

    public boolean isSupported(String feature, String version) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");	
    }

    public void normalize() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

    public Node removeChild(Node oldChild) throws DOMException {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");	
    }

    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");	
    }

    public void setNodeValue(String nodeValue) {
    }

    public void setPrefix(String prefix) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in UniqueTerminatorNode");
    }

}


