package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
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

/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class IEW3CDocument implements Document {

    protected OleAutomation browser;
    protected OleAutomation document2;
    protected OleAutomation document3;
    
    protected int size;

    /**
     *  Constructs a new IDOMDocument using an IE WebBrowser interface automation.
     *
     *  @param document a WebBrowser (Shell.Explorer) interface automation representing the document.
     */
    public IEW3CDocument(OleAutomation browser) {
	this.browser = browser;
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

    public Element getActiveElement() {
	return new IEW3CElement(OLEUtils.getPropertyInterface(getDocument2(),
							      "activeElement",
							      OLEUtils.UUID_IHTML_DOM_NODE),
				-1);
    }

    public String getSelection() {
	OleAutomation selection = OLEUtils.getPropertyInterface(getDocument2(),
								"selection",
								OLEUtils.UUID_IHTML_SELECTION_OBJECT);
	String selectionType = OLEUtils.getProperty(selection,
						    "type").getString();

	String selectionString = "";
	if (selectionType.equalsIgnoreCase("text")) {
	    OleAutomation textRange = OLEUtils.getPropertyInterface(selection,
								    "createRange",
								    OLEUtils.UUID_IHTML_TXT_RANGE);
	    selectionString = OLEUtils.getProperty(textRange,
						   "htmlText").getString();	    
	}
								
	return selectionString;
    }


    // TODO: remove this caching if we allow mutable trees
    public int getSize() {
	if (this.size == 0) {
	    this.size = ((IEW3CElement)getDocumentElement()).getSize();
	}
	return this.size;
    }

    public void dispose() {
	if (browser != null) browser.dispose();
	if (document2 != null) document2.dispose();
	if (document3 != null) document3.dispose();
    }

    public void write(String input) {
	OLEUtils.invokeCommand(getDocument2(),
			       "open",
			       new Variant[0]);
	OLEUtils.invokeCommand(getDocument2(),
			       "write",
			       new Variant[] {new Variant(input)});
	OLEUtils.invokeCommand(getDocument2(),
			       "close",
			       new Variant[0]);
    }



    ////////////////////////////////////////
    //////// w3c Document interface ////////
    ////////////////////////////////////////

    public Attr createAttribute(String name) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public CDATASection createCDATASection(String data) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Comment createComment(String data) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public DocumentFragment createDocumentFragment() {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Element createElement(String tagName) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Element createElementNS(String namespaceURI, String qualifiedName) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public EntityReference createEntityReference(String name) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public ProcessingInstruction createProcessingInstruction(String target, String data) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Text createTextNode(String data) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public DocumentType getDoctype() {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Element getElementById(String elementId) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public NodeList getElementsByTagName(String tagname) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}

    public DOMImplementation getImplementation() {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}
    public Node importNode(Node importedNode, boolean deep) {throw new UnimplementedError("org.w3c.dom.Document method is unimplemented");}

    public Element getDocumentElement() {
	OleAutomation element = OLEUtils.getPropertyInterface(getDocument3(),
							      "documentElement",
							      OLEUtils.UUID_IHTML_DOM_NODE);
	return new IEW3CElement(element, 0);
    }


    ////////////////////////////////////////
    ////////   w3c Node interface   ////////
    ////////////////////////////////////////

    public Node appendChild(Node newChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node cloneNode(boolean deep) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public NamedNodeMap getAttributes() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public NodeList getChildNodes() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node getLastChild() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node getNextSibling() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public String getNodeValue() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Document getOwnerDocument() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public String getPrefix() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node getPreviousSibling() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public boolean hasAttributes() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node insertBefore(Node newChild, Node refChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public boolean isSupported(String feature, String version) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public void normalize() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node removeChild(Node oldChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node replaceChild(Node newChild, Node oldChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public void setNodeValue(String nodeValue) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public void setPrefix(String prefix) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}

    public short getNodeType() {
	return Element.DOCUMENT_NODE;
    }

    public Node getParentNode() {
	return null;
    }

    public String getNamespaceURI() {
	return null;
    }

    public String getLocalName() {
	return null;
    }

    public boolean hasChildNodes() {
	return (this.getDocumentElement() != null);
    }

    public Node getFirstChild() {
	return this.getDocumentElement();
    }
    public String getNodeName() {
	return "#document";
    }
}
 
