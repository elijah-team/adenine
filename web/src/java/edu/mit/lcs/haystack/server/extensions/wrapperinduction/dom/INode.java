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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom;

import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeComparator;

/**
 *  Extends the W3C DOM Element with utility methods.
 * 
 *  @author Andrew Hogue
 */
public interface INode extends Element {

    /**
     *  Returns the NodeID object representing this INode's position
     *  in the tree.
     */
    public NodeID getNodeID();

    /**
     *  Returns the size (number of nodes) of the tree rooted at this node
     */
    public int getSize();
    
    /**
     *  Returns the height of this node, or the length of the longest
     *  path from this node to a leaf.  A leaf is defined to have
     *  height 1.
     */
    public int getHeight();

    /**
     *  Retreives the nodes in the subtree rooted at this node using a
     *  post-order traversal.
     */
    public INode[] getPostorderNodes();
    public void getPostorderNodesHelper(List nodes);

    /**
     *  Retreives the nodes in the subtree rooted at this node using a
     *  pre-order traversal.
     */
    public INode[] getPreorderNodes();
    public void getPreorderNodesHelper(List nodes);

    /**
     *  Returns the sibling number of this child (i.e. which index
     *  child it is of its parent).
     */
    public int getSiblingNo();

    /**
     *  Sets the sibling number of this child (i.e. which index child
     *  it is of its parent).
     */
    public void setSiblingNo(int siblingNo);

    /**
     *  Returns true if this INode is the only child of its parent.
     */
    public boolean isOnlyChild();

    /**
     *  Returns the child of this node at the given index.
     */
    public INode getChild(int index);

    /**
     *  Returns all direct children of this node with the given tag
     *  name.
     */
    public INode[] getChildren(String tagName);

    /**
     *  Retrieves the Nth ancestor of this node.  N=0 returns this
     *  node, N=1 returns its parent, N=2 its grandparent, etc.
     *  Returns null if that ancestor does not exist.
     */
    public INode getAncestor(int generation);

    /**
     *  Sets the parent of this node to the given node
     */
    public void setParent(INode parent);

    /**
     *  Retreives an array containing the siblings of this node,
     *  including this node.
     */
    public NodeList getSiblings();

    /**
     *  Removes this node and all its decendents from the tree.
     */
    public INode removeNode();

    /**
     *  Removes the children of this node, returning them as an
     *  ordered list.
     */
    public List removeChildNodes() throws DOMException;

    /**
     *  Returns true if the node given is the same as this node for the purposes
     *  of editing distance between trees.
     */
    public boolean equals(INode other);

    /**
     *  Implements the hashCode method for storing this INode in a
     *  hash data structure.
     */
    public int hashCode();

    /**
     *  Returns this node recursively as a string, with the specified indentation
     */
    public String toString(int depth, String indent);

    /**
     *  Returns a NodeComparator object which implements the same equals() method
     *  as this object.
     */
    public NodeComparator getComparator();

    /**
     *  Returns the cost to delete this node.
     */
    public int getDeleteCost();

    /**
     *  Returns the cost to insert this node.
     */
    public int getInsertCost();

    /**
     *  Returns the cost to change this node to the given other node.
     */
    public int getChangeCost(INode other);

}
