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
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 *  A generic implementation of a W3C DOM Element.
 * 
 *  @author Andrew Hogue
 */
public class ElementImpl implements INode {

    protected String tagName;
    protected ArrayList children;
    protected INode parent;
    protected int siblingNo;
    protected String textValue;

    protected HashMap attributes;

    protected int size;

    protected ElementImpl() {
	this.tagName = null;
	this.children = new ArrayList();
	this.parent = null;
	this.siblingNo = 0;
	this.attributes = new HashMap();
	this.size = 1;
	this.textValue = null;
    }

    public ElementImpl(String tagName) {
	this.tagName = tagName;
	this.children = new ArrayList();
	this.parent = null;
	this.siblingNo = 0;
	this.attributes = new HashMap();
	this.size = 1;
	this.textValue = null;
    }

    public ElementImpl(String tagName, INode parent) {
	this(tagName);
	this.parent = parent;
	this.siblingNo = 0;
	this.attributes = new HashMap();
	this.size = 1;
	this.textValue = null;
    }

    public ElementImpl(String tagName, INode parent, int siblingNo) {
	this(tagName);
	this.parent = parent;
	this.siblingNo = siblingNo;
	this.attributes = new HashMap();
	this.size = 1;
	this.textValue = null;
    }

    public void setAttributes(HashMap attrs) {
	this.attributes = attrs;
    }


    ////////////////////////////
    ///     INode methods    ///
    ////////////////////////////

    public NodeID getNodeID() {
	try {
	    if (this.parent == null) {
		return new NodeID(new int[] {this.getSiblingNo()});
	    }
	    else {
		return this.parent.getNodeID().makeChildNodeID(this.getSiblingNo());
	    }
	}
	catch (NodeIDException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    /**
     *  Returns the size (number of nodes) of the tree.
     */
    public int getSize() {
	return this.size;
    }
    
    public int getHeight() {
	int maxHeight = 0;
	for (int i = 0; i < this.children.size(); i++) {
	    int currHeight = ((INode)this.children.get(i)).getHeight();
	    if (currHeight > maxHeight)
		maxHeight = currHeight;
	}
	return maxHeight + 1;
    }

    public int getSiblingNo() {
	return this.siblingNo;
    }


    public void setSiblingNo(int siblingNo) {
	this.siblingNo = siblingNo;
    }

    public boolean isOnlyChild() {
	return (this.getSiblingNo() == 0 && this.getParentNode().getChildNodes().getLength() == 0);
    }

    public INode getChild(int index) {
	return (INode)this.children.get(index);
    }

    
    public INode[] getChildren(String tagName) {
	ArrayList byTagName = new ArrayList();
	for (int i = 0; i < this.children.size(); i++) {
	    if (((INode)children.get(i)).getTagName().equalsIgnoreCase(tagName)) {
		byTagName.add(children.get(i));
	    }
	}
	return (INode[])byTagName.toArray(new INode[0]);
    }

    public INode[] getPostorderNodes() {
	ArrayList postorder = new ArrayList();
	postorder.add(null);
	getPostorderNodesHelper(postorder);
	return (INode[])postorder.toArray(new INode[0]);
    }

    public void getPostorderNodesHelper(List postorder) {
	for (int i = 0; i < this.children.size(); i++) {
	    ((INode)this.children.get(i)).getPostorderNodesHelper(postorder);
	}
	postorder.add(this);
    }

    public INode[] getPreorderNodes() {
	ArrayList preorder = new ArrayList();
	preorder.add(null);
	getPreorderNodesHelper(preorder);
	return (INode[])preorder.toArray(new INode[0]);
    }

    public void getPreorderNodesHelper(List preorder) {
	preorder.add(this);
	for (int i = 0; i < this.children.size(); i++) {
	    ((INode)this.children.get(i)).getPreorderNodesHelper(preorder);
	}
    }

    /**
     *  Retrieves the Nth ancestor of this node.  N=0 returns this
     *  node, N=1 returns its parent, N=2 its grandparent, etc.
     *  Returns null if that ancestor does not exist.
     */
    public INode getAncestor(int generation) {
	if (generation == 0) return this;
	if (this.parent == null) return null;
	return this.parent.getAncestor(generation-1);
    }

    /**
     *  Sets the parent of this node to the given node
     */
    public void setParent(INode parent) {
	this.parent = parent;
    }					  

    public NodeList getSiblings() {
	if (this.parent == null) return new NodeListImpl();
	return this.parent.getChildNodes();
    }

    public INode removeNode() {
	return (INode)this.getAncestor(1).removeChild(this);
    }

    public String toString() {
	return tagName;
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) out.append(indent);
	out.append(this.tagName + "\n");
	for (int i = 0; i < this.children.size(); i++) {
	    out.append(((INode)this.children.get(i)).toString(depth+1, indent));
	}
	return out.toString();
    }
    
    public boolean equals(INode other) {
	return this.tagName.equals(other.getTagName());
    }

    public boolean equals(Object other) {
	if (!(other instanceof INode)) return false;
	return this.equals((INode)other);
    }

    public int hashCode() {
	return this.tagName.hashCode();
    }

    public NodeComparator getComparator() {
	return new StringNodeComparator(this.tagName);
    }

    /**
     *  Returns the cost to delete this node.
     */
    public int getDeleteCost() {
	return this.getSize();
    }

    /**
     *  Returns the cost to insert this node.
     */
    public int getInsertCost() {
	return this.getSize();
    }

    /**
     *  Returns the cost to change this node to the given other node.
     */
    public int getChangeCost(INode other) {
	//	return (this.tagName.equalsIgnoreCase(other.getTagName())) ? 0 : 1;
	return (this.tagName.equalsIgnoreCase(other.getTagName())) ? 0 : this.getDeleteCost()+other.getInsertCost();
    }

    
    ///////////////////////////////////
    /// org.w3c.dom.Element methods ///
    ///////////////////////////////////

    public String getAttribute(String name) {
	return (String)this.attributes.get(name);
    }

    public Attr getAttributeNode(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public String getAttributeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public NodeList getElementsByTagName(String name) {
	INode[] preorder = this.getPreorderNodes();
	ArrayList nodes = new ArrayList();
	for (int i = 0; i < preorder.length; i++) {
	    if (preorder[i] != null && preorder[i].getTagName().equalsIgnoreCase(name))
		nodes.add(preorder[i]);
	}
	return new NodeListImpl(nodes);
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public String getTagName() {
	return this.tagName;
    }

    public boolean hasAttribute(String name) {
	return this.attributes.containsKey(name);
    }

    public boolean hasAttributeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public void removeAttribute(String name) {
	this.attributes.remove(name);
    }

    public Attr removeAttributeNode(Attr oldAttr) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public void removeAttributeNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public void setAttribute(String name, String value) {
	this.attributes.put(name, value);
    }

    public Attr setAttributeNode(Attr newAttr) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public Attr setAttributeNodeNS(Attr newAttr) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }


    ///////////////////////////////////
    ///  org.w3c.dom.Node methods   ///
    ///////////////////////////////////

    /**
     *  Appends the given child to this node, returning this node.
     */
    public Node appendChild(Node newChild) {
	if (!(newChild instanceof INode))
	    throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "New child node is of the wrong class");
	((INode)newChild).setParent(this);
	children.add(newChild);
	this.size += ((INode)newChild).getSize();
	return this;
    }

    public Node cloneNode(boolean deep) {
	ElementImpl clone = new ElementImpl(this.tagName, this.parent);
	if (deep) {
	    for (int i = 0; i < this.children.size(); i++) {
		clone.appendChild(((INode)this.children.get(i)).cloneNode(deep));
	    }
	}
	return clone;
    }

    public NamedNodeMap getAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public NodeList getChildNodes() {
	return new NodeListImpl(this.children);
    }

    public Node getFirstChild() {
	return (Node)this.children.get(0);
    }

    public Node getLastChild() {
	return (Node)this.children.get(this.children.size()-1);
    }

    public String getLocalName() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public String getNamespaceURI() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public Node getNextSibling() {
	if (this.parent == null) return null;
	return this.getParentNode().getChildNodes().item(this.siblingNo+1);
    }

    public String getNodeName() {
	return this.getTagName();
    }

    public short getNodeType() {
	return Node.ELEMENT_NODE;
    }

    public String getNodeValue() {
	return this.textValue;		// as per DOM spec
    }

    public Document getOwnerDocument() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");	
    }

    public Node getParentNode() {
	return this.parent;
    }

    public String getPrefix() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");	
    }

    public Node getPreviousSibling() {
	if (this.parent == null) return null;
	if (this.siblingNo == 0) return null;
	return this.getParentNode().getChildNodes().item(this.siblingNo-1);
    }

    public boolean hasAttributes() {
	return !(this.attributes.isEmpty());
    }

    public boolean hasChildNodes() {
	return (this.children.size() > 0);
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
	if (!(newChild instanceof INode))
	    throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "New child node is of the wrong class");
	for (int i = 0; i < this.children.size(); i++) {
	    if (this.children.get(i) == refChild) {
		this.children.add(i, newChild);
		this.size -= ((INode)refChild).getSize();
		this.size += ((INode)newChild).getSize();
		return refChild;
	    }
	}
	throw new DOMException(DOMException.NOT_FOUND_ERR, "New child node not found");
    }

    public boolean isSupported(String feature, String version) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");	
    }

    public void normalize() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }

    public Node removeChild(Node oldChild) throws DOMException {
	boolean found = false;
	for (int i = 0; i < this.children.size(); i++) {
	    if (found) {
		((INode)this.children.get(i)).setSiblingNo(i);
	    }
	    else if (this.children.get(i) == oldChild) {
		this.children.remove(i);
		this.size -= ((INode)oldChild).getSize();
		found = true;
		i--;		// repeat this index for the element that got shifted over
	    }
	}
	if (found) {
	    return oldChild;
	}
	else {
	    throw new DOMException(DOMException.NOT_FOUND_ERR, "Old child node not found");
	}
    }

    public List removeChildNodes() {
	ArrayList oldChildren = (ArrayList)this.children.clone();
	this.children.clear();
	return oldChildren;
    }


    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
	if (!(newChild instanceof INode))
	    throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "New child node is of the wrong class");
	for (int i = 0; i < this.children.size(); i++) {
	    if (this.children.get(i) == oldChild) {
		this.children.set(i, newChild);
		this.size -= ((INode)oldChild).getSize();
		this.size += ((INode)newChild).getSize();
		return oldChild;
	    }
	}
	throw new DOMException(DOMException.NOT_FOUND_ERR, "Old child node not found");
    }

    public void setNodeValue(String nodeValue) {
    	this.textValue = nodeValue;
    }

    public void setPrefix(String prefix) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in ElementImpl");
    }


}
