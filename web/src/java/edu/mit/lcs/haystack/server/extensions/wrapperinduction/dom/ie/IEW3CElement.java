package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;

/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class IEW3CElement implements Element {

    protected OleAutomation domNode;
    protected OleAutomation element;
    protected OleAutomation style;

    protected int childNo;

    protected String originalBgColor;

    protected IEW3CElement[] domChildren;
    
    protected int size;

    /**
     *  Constructs a new Element using an IE IHTMLDOMNode interface automation.
     *  Because no parent is provided, it is assumed this is the root node (and
     *  its 'childId' is 0)
     *
     *  @param _domNode an IHTMLDOMNode interface automation representing the element.
     */
    public IEW3CElement(OleAutomation _domNode, int childNo) {
	this.domNode = _domNode;
	this.childNo = childNo;
    }

    public int getChildNo() {
	if (this.childNo != -1) return this.childNo;

	Node prev = this;
	this.childNo = 0;
	while ((prev = prev.getPreviousSibling()) != null) {
	    this.childNo++;
	    System.out.println(prev.getNodeName() + " " +this.childNo);
	}
	return this.childNo;
    }

    public String toString() {
	return this.toString(0, "  ");
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) {
	    out.append(indent);
	}
	out.append(this.getTagName());
	out.append("\n");
	Element[] children = this.getChildren();
	for (int i = 0; i < children.length; i++) {
	    out.append(((IEW3CElement)children[i]).toString(depth+1, indent));
	}
	return out.toString();	    
    }

    public String ancestorsToString() {
	int gen = 0;
	StringBuffer out = new StringBuffer();
	Element anc;
	while ((anc = this.getAncestor(gen)) != null) {
	    out.insert(0, anc.getNodeName() + ".");
	    gen++;
	}
	return out.toString();
    }

    /**
     *  Retrieves the IHTMLDOMNode interface for this element
     */
    protected OleAutomation getDOMNode() {
	return this.domNode;
    }

    protected OleAutomation getElement() {
	if (this.element == null) {
	    this.element = OLEUtils.getInterface(domNode,
						 OLEUtils.UUID_IHTML_ELEMENT);
	}
	return this.element;
    }

    protected OleAutomation getStyle() {
	if (this.style == null) {
	    this.style = OLEUtils.getPropertyInterface(getElement(),
						       "style",
						       OLEUtils.UUID_IHTML_STYLE);
	    if (this.style == null) System.out.println("Warning: style is null");	       
	}
	return this.style;
    }

    public Element[] getChildren() {
	if (OLEUtils.DEBUG) System.out.println("$$$ entering getChildren()");
	if (this.domChildren == null) {
	    OleAutomation domChildrenCollection = OLEUtils.getPropertyInterface(getDOMNode(),
										"childNodes",
										OLEUtils.UUID_IHTML_DOM_CHILDREN_COLLECTION);
	    Variant varLen = OLEUtils.getProperty(domChildrenCollection, "length");
	    int noChildren = varLen.getInt();
	    varLen.dispose();
 	    if (OLEUtils.DEBUG) System.out.println("$$$ length: " + noChildren);
	    this.domChildren = new IEW3CElement[noChildren];

	    for (int i = 0; i < noChildren; i++) {
		try {
		    Variant varChild = OLEUtils.invokeCommand(domChildrenCollection,
							      "item",
							      new Variant[] {new Variant(i)});
		    if (varChild == null) {
			if (OLEUtils.DEBUG) System.out.println("### varChild is null");
		    }
		    else {
			OleAutomation autoChild = OLEUtils.getInterface(varChild,
									OLEUtils.UUID_IHTML_DOM_NODE);
			if (autoChild == null) {
			    if (OLEUtils.DEBUG) System.out.println("### autoChild is null");
			}
			else {
			    IEW3CElement child = new IEW3CElement(autoChild, i);
			    if (OLEUtils.DEBUG) System.out.println("### created child with tagName: " + child.getTagName());
			    this.domChildren[i] = child;
			}
		    }
		}
		catch (Throwable e) {
		    System.out.println("##### Exception in getChildren() (i=" + i + "): " + e);
		    e.printStackTrace();
		}
	    }
	}
	return this.domChildren;
    }

    public Element[] getPostorderNodes() {
	ArrayList postorder = new ArrayList();
	postorder.add(null);	// to make array 1-based
	getPostorderNodeHelper(postorder);
	return (Element[])postorder.toArray(new Element[0]);
    }

    protected void getPostorderNodeHelper(ArrayList postorder) {
	IEW3CElement[] childNodes = (IEW3CElement[])getChildren();
	for (int i = 0; i < childNodes.length; i++) {
	    childNodes[i].getPostorderNodeHelper(postorder);
	}
	postorder.add(this);
    }

    public Element getChild(int index) {
	Element[] children = (Element[])getChildren();
	if (index < 0 || index >= children.length) {
	    return null;
	}
	else {
	    return children[index];
	}
    }

    public Element[] getChildren(String tagName) {
	IEW3CElement[] children = (IEW3CElement[])getChildren();
	ArrayList tagChildren = new ArrayList();
	for (int i = 0; i < children.length; i++) {
	    if (children[i].getTagName().equalsIgnoreCase(tagName)) {
		tagChildren.add(children[i]);
	    }
	}
	return (IEW3CElement[])tagChildren.toArray(new IEW3CElement[0]);
    }


    /**
     *  Retrieves all text (i.e. non-markup) from within this element and its children.
     *  For IE, this means all elements with a nodeType of 3 or a nodeName of "#text"
     */
    public String getNodeText() {
	if (getNodeType() == Element.TEXT_NODE) {
	    return OLEUtils.getProperty(domNode,
					"nodeValue").getString();
	}

	StringBuffer text = new StringBuffer();
	IDOMElement[] children = (IDOMElement[])getChildren();
	for (int i = 0; i < children.length; i++) {
	    text.append(children[i].getNodeText());
	}

	return text.toString();
    }    

    /**
     *  Highlights this element by changing its CSS to have a Yellow background color.
     */
    public void highlight() {
	if (this.originalBgColor == null) {
	    /*	    try {
		originalBgColor = OLEUtils.invokeCommand(getStyle(),
							 "getAttribute",
							 new String[] {"strAttributeName",
								       "lFlags"},
							 new Variant[] {new Variant("bgcolor"),
									new Variant(0)}
							 ).getString();
	    }
	    catch (Exception e) { }*/
	    try {
		originalBgColor = OLEUtils.getProperty(getStyle(),
						       "backgroundColor").getString();
	    }
	    catch (Exception e) {
		// just means that there was no background color
		originalBgColor = "transparent";
	    }
	}
	
	OLEUtils.setProperty(getStyle(),
			     "backgroundColor",
			     new Variant("#FFFF00"));
    }

    public void unhighlight() {
	if (this.originalBgColor != null) {
	    OLEUtils.setProperty(getStyle(),
				 "backgroundColor",
				 new Variant(originalBgColor));
	}
    }

    public void dispose() {
	if (this.domNode != null) this.domNode.dispose();	
	if (this.domChildren != null) {
	    for (int i = 0; i < domChildren.length; i++) {
		domChildren[i].dispose();
	    }
	}
    }

    // TODO: remove this caching if we allow mutable trees
    /**
     *  Returns the size (number of nodes) of the tree.
     */
    public int getSize() {
	if (this.size == 0) {
	    Element[] children = this.getChildren();
	    for (int i = 0; i < children.length; i++) {
		this.size += ((IEW3CElement)children[i]).getSize();
	    }
	    this.size += 1;		// this node
	}
	return this.size;
    }

    /**
     *  Retrieves the Nth ancestor of this node.  N=0 returns this
     *  node, N=1 returns its parent, N=2 its grandparent, etc.
     *  Returns null if that ancestor does not exist.
     */
    public Element getAncestor(int generation) {
	if (generation == 0) return this;
	if (this.getParentNode() == null) return null;
	return ((IEW3CElement)this.getParentNode()).getAncestor(generation-1);
    }

    /**
     *  Implement some version of equals dependent on tag name
     */
    public boolean equals(Element other) {
	if (!(other instanceof IEW3CElement)) return false;

	String thisTag = this.getTagName();
	String otherTag = ((IEW3CElement)other).getTagName();

	if (!thisTag.equalsIgnoreCase(otherTag)) return false;

	if (this.getNodeType() == Element.TEXT_NODE) { // #text
	    return this.getNodeText().equals(((IEW3CElement)other).getNodeText());
	}

	// A HashMap mapping tag names to the attributes
	// used to compare them.  
	HashMap attrs = new HashMap();
	attrs.put("img", "src"); // maybe want an md5 sum instead of relying on the source?
	attrs.put("a", "href");
	attrs.put("form", "action");

	if (attrs.containsKey(thisTag)) {
	    return compareAttributes(this, (IEW3CElement)other, (String)attrs.get(thisTag));
	}

	// The tag didn't appear in the attr hash, so the fact that
	// the tag names matched means elements are equal.
	return true;
    }

    /**
     *  Compares the given attribute accross the two given elements, returning
     *  true if its value is the same for both (comparing strings, ignoring case),
     *  and false if not.
     */
    public static boolean compareAttributes(IEW3CElement e1, IEW3CElement e2, String attr) {
	if (e1 == null || e2 == null || attr == null) return false;
	String a1 = e1.getAttribute(attr);
	String a2 = e2.getAttribute(attr);
	if (a1 == null || a2 == null) return false;
	return a1.equalsIgnoreCase(a2);
    }

    ////////////////////////////////////////
    //////// w3c Element interface  ////////
    ////////////////////////////////////////

    public Attr getAttributeNode(String name) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public Attr getAttributeNodeNS(String namespaceURI, String localName) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public String getAttributeNS(String namespaceURI, String localName) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public NodeList getElementsByTagName(String name) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public boolean hasAttribute(String name) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public boolean hasAttributeNS(String namespaceURI, String localName) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public void removeAttribute(String name) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public Attr removeAttributeNode(Attr oldAttr) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public void removeAttributeNS(String namespaceURI, String localName) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public void setAttribute(String name, String value) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public Attr setAttributeNode(Attr newAttr) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public Attr setAttributeNodeNS(Attr newAttr) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {throw new UnimplementedError("org.w3c.dom.Element method is unimplemented");}

    public String getTagName() {
	Variant varTag = OLEUtils.getProperty(getDOMNode(), "nodeName");
	String tagName = null;
	if (varTag != null) {
	    tagName = varTag.getString();
	    varTag.dispose();
	}
	return tagName.toLowerCase();
    }

    public String getAttribute(String name) {
	Variant varAttr = OLEUtils.invokeCommand(this.getElement(),
						 "getAttribute",
						 new Variant[] {new Variant(name),
								new Variant(0)});
	String attribute = null;
	if (varAttr != null) {
	    attribute = varAttr.getString();
	    varAttr.dispose();
	}
	return attribute;
    }




    ////////////////////////////////////////
    ////////   w3c Node interface   ////////
    ////////////////////////////////////////

    public Node appendChild(Node newChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Document getOwnerDocument() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public String getPrefix() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public boolean hasAttributes() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node insertBefore(Node newChild, Node refChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public boolean isSupported(String feature, String version) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public void normalize() {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node removeChild(Node oldChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public Node replaceChild(Node newChild, Node oldChild) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public void setNodeValue(String nodeValue) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}
    public void setPrefix(String prefix) {throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");}

    /**
     *  Retrieves the IHTMLDOMNode 'nodeType' property, which is 1 for an element,
     *  3 for #text, and -1 for an attribute
     */
    public short getNodeType() {
	Variant nodeTypeVar = OLEUtils.getProperty(domNode,
						   "nodeType");
	int nodeType = nodeTypeVar.getInt();
	nodeTypeVar.dispose();
	if (nodeType == 1) return Element.ELEMENT_NODE;
	if (nodeType == 3) return Element.TEXT_NODE;
	if (nodeType == -1) return Element.ATTRIBUTE_NODE;
	else return -1;
    }

    public NodeList getChildNodes() {
	return new IEW3CNodeList(this.getChildren());
    }

    public String getNamespaceURI() {
	return null;
    }

    public String getLocalName() {
	return null;
    }

    public Node getParentNode() {
	OleAutomation pn = OLEUtils.getPropertyInterface(this.domNode,
							 "parentNode",
							 OLEUtils.UUID_IHTML_DOM_NODE);
	if (pn != null) {
	    return new IEW3CElement(pn, -1);
	}
	else {
	    return null;
	}
    }

    public String getNodeName() {
	return this.getTagName().toLowerCase();
    }

    public NamedNodeMap getAttributes() {
	return new IEW3CNamedNodeMap();
    }

    public Node cloneNode(boolean deep) {
	throw new UnimplementedError("org.w3c.dom.Node method is unimplemented");
    }

    public boolean hasChildNodes() {
	//	System.out.println("hasChildNodes:  " + ancestorsToString() + "returned " + (this.getChildren().length > 0));
	return (this.getChildren().length > 0);
    }

    public Node getFirstChild() {
	//	System.out.println("getFirstChild:  " + ancestorsToString());
	OleAutomation n = OLEUtils.getPropertyInterface(this.domNode,
							 "firstChild",
							 OLEUtils.UUID_IHTML_DOM_NODE);
	if (n != null) {
	    return new IEW3CElement(n, 0);
	}
	else {
	    return null;
	}	
    }

    public Node getLastChild() {
	OleAutomation n = OLEUtils.getPropertyInterface(this.domNode,
							 "lastChild",
							 OLEUtils.UUID_IHTML_DOM_NODE);
	if (n != null) {
	    return new IEW3CElement(n, -1);
	}
	else {
	    return null;
	}	
    }
    
    public Node getPreviousSibling() {
	OleAutomation n = OLEUtils.getPropertyInterface(this.domNode,
							 "previousSibling",
							 OLEUtils.UUID_IHTML_DOM_NODE);
	if (n != null) {
	    return new IEW3CElement(n, this.childNo-1);
	}
	else {
	    return null;
	}	
    }

    public Node getNextSibling() {
	//	System.out.println("getNextSibling: " + ancestorsToString());
	OleAutomation n = OLEUtils.getPropertyInterface(this.domNode,
							 "nextSibling",
							 OLEUtils.UUID_IHTML_DOM_NODE);
	if (n != null) {
	    return new IEW3CElement(n, this.childNo+1);
	}
	else {
	    return null;
	}	
    }

    public String getNodeValue() {
	return null;
    }

}

