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
 *  Represents a pattern, with wildcards, for matching nodes in a tree.
 */
public class PatternTree implements ITree {

    protected PatternNode root;

    /**
     *  Creates a PatternTree based on the given INode as a root
     */
    public PatternTree(INode templateRoot) {
	//	this.root = new PatternNode(null, 0, templateRoot, true);
    }

    protected PatternTree(PatternNode root) {
	this.root = root;
    }

    public String toString() {
	return this.root.toString();
    }

    /*    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource treeRes = Utilities.generateUniqueResource();

	Resource rootRes = this.root.makeResource(rdfc);
	rdfc.add(new Statement(treeRes, Ergo.PATTERN_TREE_ROOT_PROP, rootRes));

	rdfc.add(new Statement(treeRes, Constants.s_rdf_type, Ergo.PATTERN_TREE_CLASS));

	return treeRes;
    }


    public static PatternTree fromResource(Resource treeRes, IRDFContainer rdfc) throws RDFException {
	return new PatternTree(PatternNode.fromResource((Resource)rdfc.extract(treeRes, Ergo.PATTERN_TREE_ROOT_PROP, null), rdfc));
    }
    */
    //////// ITree interface methods ////////

    /**
     *  Returns the size (number of nodes) of the tree.
     */
    public int getSize() {
	return this.root.getSize();
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
	return this.root;
    }

    ////////////////////////////////////
    /// org.w3c.dom.Document methods ///
    ////////////////////////////////////

    public Attr createAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Attr createAttributeNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public CDATASection createCDATASection(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Comment createComment(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public DocumentFragment createDocumentFragment() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Element createElement(String tagName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Element createElementNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public EntityReference createEntityReference(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public ProcessingInstruction createProcessingInstruction(String target, String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Text createTextNode(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public DocumentType getDoctype() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Element getDocumentElement() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Element getElementById(String elementId) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public NodeList getElementsByTagName(String tagname) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public DOMImplementation getImplementation() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Node importNode(Node importedNode, boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }


    ///////////////////////////////////
    ///  org.w3c.dom.Node methods   ///
    ///////////////////////////////////



    public Node appendChild(Node newChild) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Node cloneNode(boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public NamedNodeMap getAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public NodeList getChildNodes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Node getFirstChild() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Node getLastChild() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public String getLocalName() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public String getNamespaceURI() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
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
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");	
    }

    public Node getPreviousSibling() {
	return null;
    }

    public boolean hasAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public boolean hasChildNodes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public boolean isSupported(String feature, String version) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");	
    }

    public void normalize() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
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
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in PatternTree");
    }


}
