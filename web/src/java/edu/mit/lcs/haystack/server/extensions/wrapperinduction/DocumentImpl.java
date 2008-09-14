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

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ITree;

/**
 *  A generic implementation of the W3C Document class
 * 
 *  @author Andrew Hogue
 */
public class DocumentImpl implements ITree {

    protected INode root;
    
    public DocumentImpl(Node root) {
	this.root = (INode)root;
    }

    /**
     *  Returns the size (number of nodes) of the tree.
     */
    public int getSize() {
	return root.getSize();
    }
    
    /**
     *  Returns the nodes of this tree, in postorder 
     */
    public INode[] getNodes() {
	return this.root.getPostorderNodes();
    }

    /**
     *  Returns the root of the tree.
     */
    public INode getRoot() {
	return root;
    }

    public String toString() {
	return root.toString(0, "  ");
    }


    ////////////////////////////////////
    /// org.w3c.dom.Document methods ///
    ////////////////////////////////////

    public Attr createAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Attr createAttributeNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public CDATASection createCDATASection(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Comment createComment(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public DocumentFragment createDocumentFragment() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Element createElement(String tagName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Element createElementNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public EntityReference createEntityReference(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public ProcessingInstruction createProcessingInstruction(String target, String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Text createTextNode(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public DocumentType getDoctype() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Element getDocumentElement() {
	return this.root;
    }

    public Element getElementById(String elementId) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public NodeList getElementsByTagName(String tagname) {
	return this.root.getElementsByTagName(tagname);
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public DOMImplementation getImplementation() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Node importNode(Node importedNode, boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }


    ///////////////////////////////////
    ///  org.w3c.dom.Node methods   ///
    ///////////////////////////////////



    public Node appendChild(Node newChild) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Node cloneNode(boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public NamedNodeMap getAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public NodeList getChildNodes() {
	return new NodeListImpl(new INode[] {this.root});
    }

    public Node getFirstChild() {
	return this.root;
    }

    public Node getLastChild() {
	return this.root;
    }

    public String getLocalName() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public String getNamespaceURI() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Node getNextSibling() {
	return null;
    }

    public String getNodeName() {
	return "#document";
    }

    public short getNodeType() {
	return Node.DOCUMENT_NODE;
    }

    public String getNodeValue() {
	return null;		// as per DOM spec
    }

    public Document getOwnerDocument() {
	return this;
    }

    public Node getParentNode() {
	return null;
    }

    public String getPrefix() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");	
    }

    public Node getPreviousSibling() {
	return null;
    }

    public boolean hasAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public boolean hasChildNodes() {
	return (this.root != null);
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public boolean isSupported(String feature, String version) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");	
    }

    public void normalize() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }

    public Node removeChild(Node oldChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public void setNodeValue(String nodeValue) {
    }

    public void setPrefix(String prefix) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in DocumentImpl");
    }


    //////////////
    //// test ////
    //////////////

    public static void main(String args[]) throws Exception {
	DocumentImpl t1 =
	    new DocumentImpl(new ElementImpl("A")
			     .appendChild(new ElementImpl("text1")));
	DocumentImpl t2 =
	    new DocumentImpl(new ElementImpl("A")
			     .appendChild(new ElementImpl("text2")));
	Mapping m = new TreeDistance((INode)t1.getDocumentElement(),
				     (INode)t2.getDocumentElement(),
				     1.0).getMapping();
	System.out.println(m);
    }
}

