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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.mozilla;

import java.util.ArrayList;

import org.eclipse.swt.internal.mozilla.nsIDOMComment;
import org.eclipse.swt.internal.mozilla.nsIDOMDocument;
import org.eclipse.swt.internal.mozilla.nsIDOMElement;
import org.eclipse.swt.internal.mozilla.nsIDOMHTMLDocument;
import org.eclipse.swt.internal.mozilla.nsIDOMLocation;
import org.eclipse.swt.internal.mozilla.nsIDOMNSDocument;
import org.eclipse.swt.internal.mozilla.nsIDOMNode;
import org.eclipse.swt.internal.mozilla.nsIDOMText;
import org.eclipse.swt.internal.mozilla.nsIDOMWindow;
import org.eclipse.swt.internal.mozilla.nsIWebBrowser;
import org.eclipse.swt.internal.mozilla.nsEmbedString;
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

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @version 1.0
 * @author Ryan Manuel
 */
public class MozDOMDocument implements IDOMDocument {
	
	protected nsIWebBrowser m_webbrowser;
	protected nsIDOMHTMLDocument m_domhtmldocument;
	protected nsIDOMDocument m_domdocument;
	protected nsIDOMNSDocument m_domnsdocument;
	protected int size;
	
	protected ArrayList highlightedElements;
	
	/**
	 * @param webBrowser
	 */
	public MozDOMDocument(nsIWebBrowser webBrowser) {
		this.m_webbrowser = webBrowser;
		this.highlightedElements = new ArrayList();
	}
	
	protected void finalize() {
		// commented out because it causes big errors to release aspects of the document while
		// it is being displayed
		
		/*m_webbrowser.Release();
		if(m_domhtmldocument != null) m_domhtmldocument.Release();
		if(m_domdocument != null) m_domdocument.Release();
		if(m_domnsdocument != null) m_domnsdocument.Release();*/
	}
	
	protected nsIDOMDocument getDOMDocument() {
		if(this.m_domdocument == null) {
			int[] aContainerWindow = new int[1];
			int[] aDocument = new int[1];
			
			int rc = m_webbrowser.GetContentDOMWindow(aContainerWindow);
			MozUtils.mozCheckReturn(rc, aContainerWindow[0]);
			nsIDOMWindow window = new nsIDOMWindow(aContainerWindow[0]);
			
			rc = window.GetDocument(aDocument);
			MozUtils.mozCheckReturn(rc, aDocument[0]);
			this.m_domdocument = new nsIDOMDocument(aDocument[0]);
		}
		return this.m_domdocument;
	}
	
	protected nsIDOMHTMLDocument getDOMHTMLDocument() {
		if(this.m_domhtmldocument == null) {
			int[] aNSHTMLDocument = new int[1];
			
			nsIDOMDocument nsDocument = this.getDOMDocument();
			int rc = nsDocument.QueryInterface(nsIDOMHTMLDocument.NS_IDOMHTMLDOCUMENT_IID, aNSHTMLDocument);
			MozUtils.mozCheckReturn(rc, aNSHTMLDocument[0]);
			this.m_domhtmldocument = new nsIDOMHTMLDocument(aNSHTMLDocument[0]);
		}
		return this.m_domhtmldocument;
	}
	
	protected nsIDOMNSDocument getDOMNSDocument() {
		if(this.m_domnsdocument == null) {
			int[] aNSDocument = new int[1];
			
			nsIDOMDocument nsDocument = this.getDOMDocument();
			int rc = nsDocument.QueryInterface(nsIDOMNSDocument.NS_IDOMNSDOCUMENT_IID, aNSDocument);
			MozUtils.mozCheckReturn(rc, aNSDocument[0]);
			this.m_domnsdocument = new nsIDOMNSDocument(aNSDocument[0]);
		}
		return this.m_domnsdocument;
	}
	
	protected nsIWebBrowser getNSWebBrowser() {
		return this.m_webbrowser;
	}
	
	public INode getRoot() {
		int[] aElement = new int[1];
		
		nsIDOMDocument nsIDoc = this.getDOMDocument();
		int rc = nsIDoc.GetDocumentElement(aElement);
		MozUtils.mozCheckReturn(rc, aElement[0]);
		nsIDOMElement nsIElt = new nsIDOMElement(aElement[0]);
		return new MozDOMElement(this, nsIElt);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getURL()
	 */
	public String getURL() {
		nsEmbedString nsURL = new nsEmbedString();
		int rc = this.getDOMHTMLDocument().GetURL(nsURL.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strURL = nsURL.toString();
		nsURL.dispose();
		return strURL;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getTitle()
	 */
	public String getTitle() {
		nsEmbedString nsTitle = new nsEmbedString();
		int rc = this.getDOMHTMLDocument().GetTitle(nsTitle.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strTitle = nsTitle.toString();
		nsTitle.dispose();
		return strTitle;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getDomain()
	 */
	public String getDomain() {
		nsEmbedString nsDomain = new nsEmbedString();
		int rc = this.getDOMHTMLDocument().GetDomain(nsDomain.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strDomain = nsDomain.toString();
		nsDomain.dispose();
		return strDomain;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getPathname()
	 */
	public String getPathname() {
		int[] aLocation = new int[1];
		
		nsEmbedString aPathname = new nsEmbedString();
		nsIDOMNSDocument nsDOMNSDocument = this.getDOMNSDocument();
		int rc = nsDOMNSDocument.GetLocation(aLocation);
		MozUtils.mozCheckReturn(rc, aLocation[0]);
		nsIDOMLocation nsDOMLocation = new nsIDOMLocation(aLocation[0]);
		
		rc = nsDOMLocation.GetPathname(aPathname.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strPathname = aPathname.toString();
		aPathname.dispose();
		nsDOMLocation.Release();
		return strPathname;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getActiveElement()
	 */
	public IDOMElement getActiveElement() {
		// There is no getActiveElement in the mozilla DOM
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getElementAtPoint(int, int)
	 */
	public IDOMElement getElementAtPoint(int x, int y) {
		// there is no getElementAtPoint in the mozilla DOM
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getSelection(boolean)
	 */
	public DOMSelection getSelection(boolean unselect) {
		DOMSelection sel = new MozDOMSelection(this, unselect);
		return sel;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#write(java.lang.String)
	 */
	public void write(String input) {
		nsEmbedString nsInput = new nsEmbedString(input);
		nsIDOMHTMLDocument nsDOMHTMLDocument = this.getDOMHTMLDocument();
		int rc = nsDOMHTMLDocument.Write(nsInput.getAddress());
		MozUtils.mozCheckReturn(rc);
		nsInput.dispose();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#addHighlightedElement(edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement)
	 */
	public void addHighlightedElement(IDOMElement e) {
		this.highlightedElements.add(e);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#removeHighlightedElement(edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement)
	 */
	public void removeHighlightedElement(IDOMElement e) {
		this.highlightedElements.remove(e);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#getHighlightedElements()
	 */
	public IDOMElement[] getHighlightedElements() {
		return (IDOMElement[]) this.highlightedElements.toArray();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMDocument#clearHighlightedElements()
	 */
	public void clearHighlightedElements() {
		this.highlightedElements.clear();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.ITree#getSize()
	 */
	public int getSize() {
		if (this.size == 0) {
		    this.size = this.getRoot().getSize();
		}
		return this.size;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.ITree#getNodes()
	 */
	public INode[] getNodes() {
		return this.getRoot().getPostorderNodes();
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#getDoctype()
	 */
	public DocumentType getDoctype() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#getImplementation()
	 */
	public DOMImplementation getImplementation() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#getDocumentElement()
	 */
	public Element getDocumentElement() {
		return this.getRoot();
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createElement(java.lang.String)
	 */
	public Element createElement(String tagName) throws DOMException {
		int[] aElement = new int[1];
		int[] aNode = new int[1];
		
		nsEmbedString aTagName = new nsEmbedString(tagName);
		int rc = this.getDOMDocument().CreateElement(aTagName.getAddress(), aElement);
		MozUtils.mozCheckReturn(rc, aElement[0]);
		nsIDOMElement nsIElement = new nsIDOMElement(aElement[0]);
		
		rc = nsIElement.QueryInterface(nsIDOMNode.NS_IDOMNODE_IID, aNode);
		MozUtils.mozCheckReturn(rc, aNode[0]);
		nsIDOMNode nsINode = new nsIDOMNode(aNode[0]);
		aTagName.dispose();
		return new MozDOMElement(this, nsINode);
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createDocumentFragment()
	 */
	public DocumentFragment createDocumentFragment() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createTextNode(java.lang.String)
	 */
	public Text createTextNode(String arg0) {
		int[] aDOMText = new int[1];
		int[] aNode = new int[1];
		
		nsEmbedString nsData = new nsEmbedString(arg0);
		int rc = this.getDOMDocument().CreateTextNode(nsData.getAddress(), aDOMText);
		MozUtils.mozCheckReturn(rc, aDOMText[0]);
		nsIDOMText nsIText = new nsIDOMText(aDOMText[0]);
		
		rc = nsIText.QueryInterface(nsIDOMNode.NS_IDOMNODE_IID, aNode);
		MozUtils.mozCheckReturn(rc, aNode[0]);
		nsIDOMNode nsINode = new nsIDOMNode(aNode[0]);
		nsData.dispose();
		return new MozDOMElement(this, nsINode);
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createComment(java.lang.String)
	 */
	public Comment createComment(String arg0) {
		int[] aDOMComment = new int[1];
		int[] aNode = new int[1];
		
		nsEmbedString nsData = new nsEmbedString(arg0);
		int rc = this.getDOMDocument().CreateComment(nsData.getAddress(), aDOMComment);
		MozUtils.mozCheckReturn(rc, aDOMComment[0]);
		nsIDOMComment nsIComment = new nsIDOMComment(aDOMComment[0]);
		
		rc = nsIComment.QueryInterface(nsIDOMNode.NS_IDOMNODE_IID, aNode);
		MozUtils.mozCheckReturn(rc, aNode[0]);
		nsIDOMNode nsINode = new nsIDOMNode(aNode[0]);
		nsData.dispose();
		return new MozDOMElement(this, nsINode);
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createCDATASection(java.lang.String)
	 */
	public CDATASection createCDATASection(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createProcessingInstruction(java.lang.String, java.lang.String)
	 */
	public ProcessingInstruction createProcessingInstruction(String arg0,
			String arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createAttribute(java.lang.String)
	 */
	public Attr createAttribute(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createEntityReference(java.lang.String)
	 */
	public EntityReference createEntityReference(String arg0)
			throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#getElementsByTagName(java.lang.String)
	 */
	public NodeList getElementsByTagName(String arg0) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#importNode(org.w3c.dom.Node, boolean)
	 */
	public Node importNode(Node arg0, boolean arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createElementNS(java.lang.String, java.lang.String)
	 */
	public Element createElementNS(String arg0, String arg1)
			throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#createAttributeNS(java.lang.String, java.lang.String)
	 */
	public Attr createAttributeNS(String arg0, String arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#getElementsByTagNameNS(java.lang.String, java.lang.String)
	 */
	public NodeList getElementsByTagNameNS(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Document#getElementById(java.lang.String)
	 */
	public Element getElementById(String arg0) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	public String getNodeName() {
		return "#document";
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeValue()
	 */
	public String getNodeValue() throws DOMException {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#setNodeValue(java.lang.String)
	 */
	public void setNodeValue(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeType()
	 */
	public short getNodeType() {
		return Node.DOCUMENT_NODE;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getParentNode()
	 */
	public Node getParentNode() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getChildNodes()
	 */
	public NodeList getChildNodes() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getFirstChild()
	 */
	public Node getFirstChild() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getLastChild()
	 */
	public Node getLastChild() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getPreviousSibling()
	 */
	public Node getPreviousSibling() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNextSibling()
	 */
	public Node getNextSibling() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getAttributes()
	 */
	public NamedNodeMap getAttributes() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getOwnerDocument()
	 */
	public Document getOwnerDocument() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#insertBefore(org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	public Node insertBefore(Node arg0, Node arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#replaceChild(org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		int[] aReturn = new int[1];
		int rc = this.getDOMDocument().ReplaceChild(((MozDOMElement)newChild).getDOMNode().getAddress(),
				((MozDOMElement) oldChild).getDOMNode().getAddress(), aReturn);
		MozUtils.mozCheckReturn(rc, aReturn[0]);
		nsIDOMNode nsIReturnNode = new nsIDOMNode(aReturn[0]);
		return new MozDOMElement(this, nsIReturnNode);
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#removeChild(org.w3c.dom.Node)
	 */
	public Node removeChild(Node arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
	 */
	public Node appendChild(Node arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#hasChildNodes()
	 */
	public boolean hasChildNodes() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#cloneNode(boolean)
	 */
	public Node cloneNode(boolean arg0) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#normalize()
	 */
	public void normalize() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#isSupported(java.lang.String, java.lang.String)
	 */
	public boolean isSupported(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNamespaceURI()
	 */
	public String getNamespaceURI() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getPrefix()
	 */
	public String getPrefix() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#setPrefix(java.lang.String)
	 */
	public void setPrefix(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getLocalName()
	 */
	public String getLocalName() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#hasAttributes()
	 */
	public boolean hasAttributes() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}

	public INode copy() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMDocument");
	}
}
