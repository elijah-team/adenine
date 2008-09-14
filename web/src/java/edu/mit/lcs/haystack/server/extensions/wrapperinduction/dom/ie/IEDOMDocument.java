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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import java.util.ArrayList;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;
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

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class IEDOMDocument implements IDOMDocument {

    protected OleAutomation browser;
    protected OleAutomation document2;
    protected OleAutomation document3;
    protected OleAutomation document5;
    
    protected int size;

    protected ArrayList highlightedElements;

    /**
     *  Constructs a new IDOMDocument using an IE WebBrowser interface automation.
     *
     *  @param document a WebBrowser (Shell.Explorer) interface automation representing the document.
     */
    public IEDOMDocument(OleAutomation browser) {
	this.browser = browser;
	this.highlightedElements = new ArrayList();
    }

    protected OleAutomation getDocument2() {
	if (this.document2 == null) {
	    this.document2 = OLEUtils.getPropertyInterface(browser,
							   "Document",
							   OLEUtils.UUID_IHTML_DOCUMENT2);
	}
	return this.document2;
    }

    protected OleAutomation getDocument3() {
	if (this.document3 == null) {
	    this.document3 = OLEUtils.getPropertyInterface(browser,
							   "Document",
							   OLEUtils.UUID_IHTML_DOCUMENT3);
	}
	return this.document3;
    }

    protected OleAutomation getDocument5() {
	if (this.document5 == null) {
	    this.document5 = OLEUtils.getPropertyInterface(browser,
							   "Document",
							   OLEUtils.UUID_IHTML_DOCUMENT5);
	}
	return this.document5;
    }

    public INode getRoot() {
	OleAutomation element = OLEUtils.getPropertyInterface(getDocument3(),
							      "documentElement",
							      OLEUtils.UUID_IHTML_DOM_NODE);
	return new IEDOMElement(this, element);
    }

    public String getBaseUrl() {
	String url = getURL();
	String lastChar = (url.startsWith("file")) ? "\\" : "/";
	if (url.indexOf("?") >= 0) {
	    return url.substring(0, url.lastIndexOf(lastChar, url.indexOf("?"))+1);
	}
	else {
	    return url.substring(0, url.lastIndexOf(lastChar)+1);
	}
    }


    public String getURL() {
	return OLEUtils.getProperty(getDocument2(), "url").getString();
    }
    
    public String getTitle() {
	return OLEUtils.getProperty(getDocument2(), "title").getString();
    }

    public String getDomain() {
	return OLEUtils.getProperty(getDocument2(), "domain").getString();
    }

    public String getPathname() {
	OleAutomation locationAuto = OLEUtils.getPropertyInterface(getDocument2(),
								   "location",
								   OLEUtils.UUID_IHTML_LOCATION);
	String pathname = OLEUtils.getProperty(locationAuto,
					       "pathname").getString();
	locationAuto.dispose();
	return pathname;					       
    }

    public IDOMElement getActiveElement() {
	return new IEDOMElement(this,
				OLEUtils.getPropertyInterface(getDocument2(),
							      "activeElement",
							      OLEUtils.UUID_IHTML_DOM_NODE));
    }

    public IDOMElement getElementAtPoint(int x, int y) {
	Variant varX = new Variant(x);
	Variant varY = new Variant(y);
	IEDOMElement element = new IEDOMElement(this,
						OLEUtils.getInterface(OLEUtils.invokeCommand(getDocument2(),
											     "elementFromPoint",
											     new Variant[] {varX, varY}).getAutomation(),
								      OLEUtils.UUID_IHTML_DOM_NODE));
	varX.dispose();
	varY.dispose();
	return element;
    }

    public DOMSelection getSelection(boolean unselect) {
	DOMSelection sel = new IEDOMSelection(this, unselect);
	return sel;
    }

    // TODO: remove this caching if we allow mutable trees
    public int getSize() {
	if (this.size == 0) {
	    this.size = getRoot().getSize();
	}
	return this.size;
    }

    public INode[] getNodes() {
	return this.getRoot().getPostorderNodes();
    }



    /**
     *  Override to dispose of member OleAutomations
     */
    protected void finalize() {
	if (this.browser != null) this.browser.dispose();
	if (this.document2 != null) this.document2.dispose();
	if (this.document3 != null) this.document3.dispose();
	if (this.document5 != null) this.document5.dispose();
    }

    public void write(String input) {
	Variant varInput = new Variant(input);
	OLEUtils.invokeCommand(getDocument2(),
			       "open",
			       new Variant[0]);
	OLEUtils.invokeCommand(getDocument2(),
			       "write",
			       new Variant[] {varInput});
	OLEUtils.invokeCommand(getDocument2(),
			       "close",
			       new Variant[0]);
	varInput.dispose();
    }


    public void addHighlightedElement(IDOMElement e) {
	this.highlightedElements.add(e);
    }

    public void removeHighlightedElement(IDOMElement e) {
	this.highlightedElements.remove(e);
    }

    public IDOMElement[] getHighlightedElements() {
	return (IDOMElement[])this.highlightedElements.toArray(new IDOMElement[0]);
    }

    public void clearHighlightedElements() {
	this.highlightedElements.clear();
    }


    /**
     *  Retreives an OleAutomation object representing an
     *  IHTMLStyleSheetsCollection for this document.
     */
    public OleAutomation getStylesheets() {
	return OLEUtils.getPropertyInterface(this.getDocument2(),
					     "styleSheets",
					     OLEUtils.UUID_IHTML_STYLE_SHEETS_COLLECTION);
    }

    /**
     *  Adds stylesheets to this document from the given OleAutomation
     *  representing an IHTMLStyleSheetsCollection.
     */
    public void setStylesheets(OleAutomation stylesheets) {
	int numStyles = OLEUtils.getProperty(stylesheets, "length").getInt();
	for (int i = 0; i < numStyles; i++) {
	    try {
		Variant indVar = new Variant(i);
		Variant styleVar = OLEUtils.invokeCommand(stylesheets,
							  "item",
							  new Variant[] {indVar});
		
		Variant cssTextVar = new Variant(getCssText(styleVar));
		OLEUtils.invokeCommand(this.getDocument2(),
				       "createStyleSheet",
				       new String[] {"bstrHref"},
				       new Variant[] {cssTextVar});
		indVar.dispose();
		styleVar.dispose();
		cssTextVar.dispose();
	    }
	    catch (Throwable e) {
		e.printStackTrace();
	    }
	}
    }

    public static String getCssText(Variant styleVar) {
	OleAutomation styleAuto = OLEUtils.getInterface(styleVar,
							OLEUtils.UUID_IHTML_STYLE);
	if (styleAuto == null) { // maybe it's a IHTMLStyleSheet
	    styleAuto = OLEUtils.getInterface(styleVar,
					      OLEUtils.UUID_IHTML_STYLE_SHEET);
	    if (styleAuto == null) {
		System.out.println("styleSheet is null too!");
		return "";
	    }
	}
		
	Variant cssVar = OLEUtils.getProperty(styleAuto,
					      "cssText");
	String cssString = (cssVar == null) ? "" : cssVar.getString();

	styleAuto.dispose();
	cssVar.dispose();

	return cssString;
    }


    /**
     *  Creates an INode copy of this document.
     */
    public INode copy() {
	return ((IEDOMElement)this.getRoot()).copy();
    }


    ////////////////////////////////////
    /// org.w3c.dom.Document methods ///
    ////////////////////////////////////

    public Attr createAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Attr createAttributeNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public CDATASection createCDATASection(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Comment createComment(String data) {
	Variant dataVar = new Variant(data);
	OleAutomation element = OLEUtils.invokeCommand(this.getDocument5(),
						       "createComment",
						       new Variant[] {dataVar}).getAutomation();
	dataVar.dispose();
	return new IEDOMElement(this, element);
    }

    public DocumentFragment createDocumentFragment() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Element createElement(String tagName) {
	Variant tagNameVar = new Variant(tagName);
	OleAutomation element = OLEUtils.invokeCommand(this.getDocument2(),
						       "createElement",
						       new Variant[] {tagNameVar}).getAutomation();
	tagNameVar.dispose();
	return new IEDOMElement(this, element);
    }

    public Element createElementNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public EntityReference createEntityReference(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public ProcessingInstruction createProcessingInstruction(String target, String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public org.w3c.dom.Text createTextNode(String data) {
	Variant dataVar = new Variant(data);
	OleAutomation element = OLEUtils.invokeCommand(this.getDocument3(),
						       "createTextNode",
						       new Variant[] {dataVar}).getAutomation();
	dataVar.dispose();
	return new IEDOMElement(this, element);
    }

    public DocumentType getDoctype() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Element getDocumentElement() {
	return this.getRoot();
    }

    public Element getElementById(String elementId) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public NodeList getElementsByTagName(String tagname) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public DOMImplementation getImplementation() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Node importNode(Node importedNode, boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }


    ///////////////////////////////////
    ///  org.w3c.dom.Node methods   ///
    ///////////////////////////////////



    public Node appendChild(Node newChild) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Node cloneNode(boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public NamedNodeMap getAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public NodeList getChildNodes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Node getFirstChild() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Node getLastChild() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public String getLocalName() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public String getNamespaceURI() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
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
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");	
    }

    public Node getPreviousSibling() {
	return null;
    }

    public boolean hasAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public boolean hasChildNodes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public boolean isSupported(String feature, String version) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");	
    }

    public void normalize() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
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
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in IEDOMDocument");
    }


}
