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
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.NGramsFragmentSet;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ElementImpl;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeComparator;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.test.DOMNodeComparator;

/**
 * @version 1.0
 * @author Andrew Hogue
 * @author Ryan Manuel
 * @author Yuan Shen (IAugmentedElement)
 */
public class IEDOMElement implements IDOMElement, INode,
        org.w3c.dom.Text, org.w3c.dom.Attr, org.w3c.dom.Comment {

    public static boolean TREAT_TEXT_NODES_IDENTICALLY = false;

    protected Document containingDocument;

    protected OleAutomation domNode;

    protected OleAutomation element;

    protected OleAutomation style;

    protected NodeID nodeID;

    protected String originalBgColor;

    protected String originalTextColor;

    protected IEDOMElement[] domChildren;

    protected IEDOMElement parent;

    protected int size;

    /**
     * Constructs a new IDOMElement using an IE IHTMLDOMNode interface
     * automation. Because no parent is provided, it is assumed this is the root
     * node (and its 'childId' is 0)
     * 
     * @param _domNode
     *            an IHTMLDOMNode interface automation representing the element.
     */
    public IEDOMElement(Document _containingDocument, OleAutomation _domNode) {
        this.containingDocument = _containingDocument;
        this.domNode = _domNode;
    }

    public String toString() {
        return (this.getNodeType() == Node.TEXT_NODE) ? this.getNodeText()
                : this.getTagName();
    }

    public String toString(int depth, String indent) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < depth; i++) {
            out.append(indent);
        }
        out.append(this.getLabel());
        out.append("\n");
        NodeList children = this.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            out.append(((IEDOMElement) children.item(i)).toString(depth + 1,
                    indent));
        }
        return out.toString();
    }

    /**
     * Returns all HTML for this node and its children
     */
    public String getOuterHTML() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeText();
        } else {
            return OLEUtils.getProperty(this.getElement(), "outerHTML")
                    .getString();
        }
    }

    /**
     * Returns all HTML for this node's children (not including this node)
     */
    public String getInnerHTML() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            return "";
        } else {
            Variant innerVar = OLEUtils.getProperty(this.getElement(),
                    "innerHTML");
            if (innerVar == null) {
                return "";
            } else {
                String innerHTML = "";
                if (innerVar.getType() != OLE.VT_EMPTY) {
                    innerHTML = innerVar.getString();
                }
                innerVar.dispose();
                return innerHTML;
            }
        }
    }

    /**
     * Retrieves the text of html representing the start tag of this object. If
     * this is a TEXT_NODE, returns the text of this node.
     */
    public String startTagHTML() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeText();
        } else {
            String outer = this.getOuterHTML();
            String inner = this.getInnerHTML();
            if (inner != null && !inner.equals("") && outer.indexOf(inner) >= 0) {
                return outer.substring(0, outer.indexOf(inner));
            } else {
                return outer;
            }
        }
    }

    /**
     * Retrieves the text of html representing the end tag of this object, or ""
     * if this object does not have a closing tag (e.g. <BR>). If this is a
     * TEXT_NODE, returns the empty string ("").
     */
    public String endTagHTML() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            return "";
        } else {
            String outer = this.getOuterHTML();
            String inner = this.getInnerHTML();
            if (inner != null && !inner.equals("")) {
                return outer.substring(outer.indexOf(inner) + inner.length());
            } else {
                return "";
            }
        }
    }

    public String htmlToString() {
        return OLEUtils.getProperty(this.getElement(), "toString").getString();
    }

    /**
     * Retrieves the IHTMLDOMNode interface for this element
     */
    protected OleAutomation getDOMNode() {
        return this.domNode;
    }

    protected OleAutomation getElement() {
        if (this.element == null) {
            this.element = OLEUtils.getInterface(domNode,
                    OLEUtils.UUID_IHTML_ELEMENT);
            // 	    if (this.element == null) {
            // 		System.out.println(">>>>>>>> getElement() is null!");
            // 	    }
        }
        return this.element;
    }

    protected OleAutomation getStyle() {
        if (this.style == null) {
            this.style = OLEUtils.getPropertyInterface(getElement(), "style",
                    OLEUtils.UUID_IHTML_STYLE);
        }
        return this.style;
    }

    public INode removeNode() throws DOMException {
        Variant varTrue = new Variant(true);
        OLEUtils.invokeCommand(this.getDOMNode(), "removeNode",
                new Variant[] { varTrue });
        varTrue.dispose();
        return this;
    }

    public java.util.List removeChildNodes() throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public INode replaceNode(INode newNode) throws DOMException {
        Variant varNewNode = new Variant(((IEDOMElement) newNode).getDOMNode());
        OLEUtils.invokeCommand(this.getDOMNode(), "replaceNode",
                new Variant[] { varNewNode });
        varNewNode.dispose();
        return this;
    }

    /**
     * Creates an INode copy of this element.
     */
    public INode copy() {
        ElementImpl copy = new ElementImpl(this.getTagName());
        if(this.getNodeType() == Node.TEXT_NODE) {
        	copy.setNodeValue(this.getNodeText());
        }
        copy.setAttributes(this.getAttributesMap());
        NodeList children = this.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            INode childCopy = ((IEDOMElement) children.item(i)).copy();
            childCopy.setParent(copy);
            childCopy.setSiblingNo(i);
            copy.appendChild(childCopy);
        }
        return copy;
    }

    public HashMap getAttributesMap() {
        HashMap attribs = new HashMap();

        OleAutomation domAttributeCollection = OLEUtils.getPropertyInterface(
                getDOMNode(), "attributes",
                OLEUtils.UUID_IHTML_ATTRIBUTE_COLLECTION);
        if (domAttributeCollection != null) {
            Variant varLen = OLEUtils.getProperty(domAttributeCollection,
                    "length");
            int noAttributes = varLen.getInt();
            varLen.dispose();

            for (int i = 0; i < noAttributes; i++) {
                try {
                    Variant varInd = new Variant(i);
                    Variant varAttrib = OLEUtils.invokeCommand(
                            domAttributeCollection, "item",
                            new Variant[] { varInd });
                    if (varAttrib != null) {
                        OleAutomation autoAttrib = varAttrib.getAutomation();
                        if (autoAttrib != null) {
                        	String name = OLEUtils.getProperty(autoAttrib, "Name").getString();
                        	String value = OLEUtils.getProperty(autoAttrib, "Value").getString();
                        	if(value != null && value.equals("") && 
                        			name != null && name.equalsIgnoreCase("value")) {
                        		value = this.getAttribute(name);
                        	}
                            if (name != null && value != null) {
                                attribs.put(name, value);
                            }
                        }
                        varAttrib.dispose();
                    }
                    varInd.dispose();
                } catch (Throwable e) {
                    System.out
                            .println("##### Exception in IEDOMElement.getAttributes() (i="
                                    + i + "): " + e);
                    e.printStackTrace();
                }
            }
        }
        return attribs;
    }

    /////////////////////////////////
    /// INode interface methods ///
    /////////////////////////////////

    public NodeID getNodeID() {
        if (this.nodeID == null) {
            try {
                this.nodeID = new NodeID(this.getSiblingNo());
                // fill in the path to root
                INode anc = this.getAncestor(1);
                while (anc != null && !anc.toString().equals("#document")) {
                    this.nodeID = this.nodeID.makeParentNodeID(anc
                            .getSiblingNo());
                    anc = anc.getAncestor(1);
                }
            } catch (NodeIDException e) {
                e.printStackTrace();
            }
        }

        return this.nodeID;
    }

    public int getSiblingNo() {
        if (this.getParentNode() == null)
            return 0;
        OleAutomation currSib = OLEUtils.getPropertyInterface(this.domNode,
                "previousSibling", OLEUtils.UUID_IHTML_DOM_NODE);
        int siblingNo = 0;
        while (currSib != null) {
            OleAutomation oldSib = currSib;
            currSib = OLEUtils.getPropertyInterface(currSib, "previousSibling",
                    OLEUtils.UUID_IHTML_DOM_NODE);
            oldSib.dispose();
            siblingNo++;
        }
        return siblingNo;
    }

    public void setSiblingNo(int siblingNo) {
    }

    public boolean isOnlyChild() {
        return (this.getSiblingNo() == 0 && this.getParentNode()
                .getChildNodes().getLength() == 0);
    }

    public INode[] getPostorderNodes() {
        ArrayList postorder = new ArrayList();
        postorder.add(null); // to make array 1-based
        getPostorderNodesHelper(postorder);
        return (INode[]) postorder.toArray(new INode[0]);
    }

    public void getPostorderNodesHelper(List postorder) {
        NodeList childNodes = getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            ((IEDOMElement) childNodes.item(i))
                    .getPostorderNodesHelper(postorder);
        }
        postorder.add(this);
    }

    public INode[] getPreorderNodes() {
        ArrayList preorder = new ArrayList();
        preorder.add(null); // to make array 1-based
        getPreorderNodesHelper(preorder);
        return (INode[]) preorder.toArray(new INode[0]);
    }

    public void getPreorderNodesHelper(List preorder) {
        NodeList childNodes = getChildNodes();
        preorder.add(this);
        for (int i = 0; i < childNodes.getLength(); i++) {
            ((IEDOMElement) childNodes.item(i))
                    .getPreorderNodesHelper(preorder);
        }
    }

    public INode getChild(int index) {
        NodeList children = this.getChildNodes();
        if (index > children.getLength() - 1) {
            return null;
        } else {
            return (INode) children.item(index);
        }
    }

    public INode[] getChildren(String tagName) {
        NodeList children = this.getChildNodes();
        ArrayList tagChildren = new ArrayList();
        for (int i = 0; i < children.getLength(); i++) {
            if (((INode) children.item(i)).getTagName().equalsIgnoreCase(
                    tagName)) {
                tagChildren.add(children.item(i));
            }
        }
        return (IEDOMElement[]) tagChildren.toArray(new IEDOMElement[0]);
    }

    /**
     * Retrieves all text (i.e. non-markup) from within this element and its
     * children. For IE, this means all elements with a nodeType of 3 or a
     * nodeName of "#text"
     */
    public String getNodeText() {
        if (getNodeType() == Node.TEXT_NODE) {
            return OLEUtils.getProperty(domNode, "nodeValue").getString();
        }

        if (this.getTagName().equalsIgnoreCase(WrapperManager.URL_IDENTIFIER)
                || this.getTagName().equalsIgnoreCase(WrapperManager.SRC_IDENTIFIER)) {
            if (this.getChild(0) != null)
                return this.getChild(0).getTagName();
        }

        StringBuffer text = new StringBuffer();
        NodeList children = this.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!((IEDOMElement) children.item(i)).getTagName().equalsIgnoreCase(
                    WrapperManager.URL_IDENTIFIER)
                    && !((IEDOMElement) children.item(i)).getTagName().equalsIgnoreCase(
                            WrapperManager.SRC_IDENTIFIER))
                text.append(((IEDOMElement) children.item(i)).getNodeText());
        }

        return text.toString();
   }

    /**
     * If this node is of type Node.TEXT_NODE, resets the text of this node to
     * the given String. Otherwise, does nothing.
     */
    public void setNodeText(String text) {
        Variant varText = new Variant(text);
        if (this.getNodeType() == Node.TEXT_NODE) {
            OLEUtils.setProperty(this.getDOMNode(), "nodeValue", varText);
        }
        varText.dispose();
    }

    /**
     * Highlights this element by changing its CSS to have a Yellow background
     * color. If specified, also changes the text to 'textcolor'. Returns the
     * highlighted element (which may not be the same as this element in the
     * case of text nodes).
     */
    public IDOMElement highlight(String highlightColor, String textColor) {
        if (this.getNodeType() == Node.TEXT_NODE) {
            //	    System.out.println(">>> highlighting text node " + this);
            // implement by surrounding text with a <SPAN> element
            IEDOMElement spanEl = (IEDOMElement) this.containingDocument
                    .createElement("SPAN");
            String text = this.getNodeText();
            IEDOMElement textEl = (IEDOMElement) this.containingDocument
                    .createTextNode(text);
            spanEl.appendChild(textEl);
            spanEl.highlight(highlightColor, textColor);
            this.replaceNode(spanEl);
            return textEl;
        } else {
            if (this.originalBgColor == null) {
                try {
                    originalBgColor = OLEUtils.getProperty(getStyle(),
                            "backgroundColor").getString();
                    originalTextColor = OLEUtils.getProperty(getStyle(),
                            "color").getString();
                } catch (Exception e) {
                    // just means that there was no background color (?)
                }
            }

            Variant varHighlight = new Variant(highlightColor);
            OLEUtils.setProperty(getStyle(), "backgroundColor", varHighlight);
            varHighlight.dispose();
            Variant varText = new Variant(textColor);
            OLEUtils.setProperty(getStyle(), "color", varText);
            varText.dispose();
            return this;
        }
    }

    public void unhighlight() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            IEDOMElement spanNode = (IEDOMElement) this.getParentNode();
            if (spanNode == null)
                return;
            IEDOMElement spanParent = (IEDOMElement) spanNode.getParentNode();
            spanParent.insertBefore(this, spanNode);
            spanNode.removeNode();
        } else {
            if (this.originalBgColor != null) {
                Variant varBG = new Variant(originalBgColor);
                OLEUtils.setProperty(getStyle(), "backgroundColor", varBG);
                varBG.dispose();
            } else {
                Variant varBG = new Variant("backgroundColor");
                Variant varFlags = new Variant(0);
                OLEUtils.invokeCommand(getStyle(), "removeAttribute",
                        new Variant[] { varBG, varFlags });
                varBG.dispose();
                varFlags.dispose();
            }
            if (this.originalTextColor != null) {
                Variant varText = new Variant(originalTextColor);
                OLEUtils.setProperty(getStyle(), "color", varText);
                varText.dispose();
            } else {
                Variant varText = new Variant("color");
                Variant varFlags = new Variant(0);
                OLEUtils.invokeCommand(getStyle(), "removeAttribute",
                        new Variant[] { varText, varFlags });
                varText.dispose();
                varFlags.dispose();
            }
        }
    }

    /**
     * Override to dispose of member OleAutomations
     */
    protected void finalize() {
        // this is giving JVM errors, very bad...
        // 	if (this.domNode != null) this.domNode.dispose();
        // 	if (this.element != null) this.element.dispose();
        // 	if (this.style != null) this.style.dispose();
    }

    // TODO: remove this caching if we allow mutable trees
    /**
     * Returns the size (number of nodes) of the tree.
     */
    public int getSize() {
        if (this.size == 0) {
            NodeList children = this.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                this.size += ((INode) children.item(i)).getSize();
            }
            this.size += 1; // this node
        }
        return this.size;
    }

    public int getHeight() {
        int maxHeight = 0;
        NodeList childNodes = this.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            int currHeight = ((INode) childNodes.item(i)).getHeight();
            if (currHeight > maxHeight)
                maxHeight = currHeight;
        }
        return maxHeight + 1;
    }

    public String getLabel() {
        if (!TREAT_TEXT_NODES_IDENTICALLY
                && this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeText();
        }
        return this.getTagName();
    }

    /**
     * Retrieves the Nth ancestor of this node. N=0 returns this node, N=1
     * returns its parent, N=2 its grandparent, etc. Returns null if that
     * ancestor does not exist.
     */
    public INode getAncestor(int generation) {
        if (generation == 0)
            return this;
        if (this.getParentNode() == null)
            return null;
        return ((INode) this.getParentNode()).getAncestor(generation - 1);
    }

    /**
     * Unimplemented until we allow editing of DOM trees
     */
    public void setParent(INode parent) {
        this.parent = (IEDOMElement) parent;
    }

    public NodeList getSiblings() {
        if (this.getParentNode() == null)
            return new IEDOMNodeList();
        return this.getParentNode().getChildNodes();
    }

    /**
     * Implement some version of equals dependent on tag name
     */
    public boolean equals(INode other) {
        return this.getComparator().equals(other);
    }

    // 	if (!(other instanceof IEDOMElement)) return false;
    // 	String thisTag = this.getTagName();
    // 	String otherTag = ((IEDOMElement)other).getTagName();

    // 	if (!thisTag.equalsIgnoreCase(otherTag)) return false;

    // 	if (!TREAT_TEXT_NODES_IDENTICALLY && this.getNodeType() ==
    // Node.TEXT_NODE) {
    // 	    return this.getNodeText().equals(((IEDOMElement)other).getNodeText());
    // 	}

    // 	// A HashMap mapping tag names to the attributes
    // 	// used to compare them.
    // 	HashMap attrs = new HashMap();
    // 	attrs.put("img", "src"); // maybe want an md5 sum instead of relying on
    // the source?
    // 	attrs.put("a", "href");
    // 	attrs.put("form", "action");

    // 	if (attrs.containsKey(thisTag)) {
    // 	    return compareAttributes(this, (IEDOMElement)other,
    // (String)attrs.get(thisTag));
    // 	}

    // 	// The tag didn't appear in the attr hash, so the fact that
    // 	// the tag names matched means elements are equal.
    // 	return true;
    //     }

    public int hashCode() {
        if (!TREAT_TEXT_NODES_IDENTICALLY
                && this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeText().hashCode();
        } else {
            return this.getTagName().hashCode();
        }
    }

    /**
     * Compares the given attribute accross the two given elements, returning
     * true if its value is the same for both (comparing strings, ignoring
     * case), and false if not.
     */
    public static boolean compareAttributes(IEDOMElement e1, IEDOMElement e2,
            String attr) {
        if (e1 == null || e2 == null || attr == null)
            return false;
        String a1 = e1.getAttribute(attr);
        String a2 = e2.getAttribute(attr);
        if (a1 == null || a2 == null)
            return false;
        return a1.equalsIgnoreCase(a2);
    }

    public NodeComparator getComparator() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            return new DOMNodeComparator(this.getTagName(), this.getNodeText());
        } else {
            return new DOMNodeComparator(this.getTagName(), this.getTagName());
        }
    }

    /**
     * Returns the cost to delete this node.
     */
    public int getDeleteCost() {
        return this.getSize();
    }

    /**
     * Returns the cost to insert this node.
     */
    public int getInsertCost() {
        return this.getSize();
    }

    /**
     * Returns the cost to change this node to the given other node.
     */
    public int getChangeCost(INode other) {
        return (this.equals(other)) ? 0 : this.getSize() + other.getSize();
    }

    ///////////////////////////////////
    /// org.w3c.dom.Element methods ///
    ///////////////////////////////////

    public String getAttribute(String name) {
        Variant varName = new Variant(name);
        Variant varZero = new Variant(0);
        Variant varAttr = OLEUtils.invokeCommand(this.getElement(),
                "getAttribute", new Variant[] { varName, varZero });
        varName.dispose();
        varZero.dispose();

        String attribute = null;
        if (varAttr != null) {
            try {
                attribute = varAttr.getString();
            } catch (SWTException e) {
                attribute = null;
            }
            varAttr.dispose();
        }

        return attribute;
    }

    public Attr getAttributeNode(String name) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getAttributeNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public NodeList getElementsByTagName(String name) {
        Variant varName = new Variant(name);
        Variant domChildrenCollection = OLEUtils.invokeCommand(getDOMNode(),
                "getElementsByTagName", new Variant[] { varName });
        if (domChildrenCollection == null)
            return new IEDOMNodeList();
        int noChildren = OLEUtils.getProperty(
                domChildrenCollection.getAutomation(), "length").getInt();

        IEDOMElement[] tempDomChildren = new IEDOMElement[noChildren];

        for (int i = 0; i < noChildren; i++) {
            try {
                Variant varInd = new Variant(i);
                Variant varChild = OLEUtils.invokeCommand(domChildrenCollection
                        .getAutomation(), "item", new Variant[] { varInd });
                if (varChild != null) {
                    OleAutomation autoChild = OLEUtils.getInterface(varChild,
                            OLEUtils.UUID_IHTML_DOM_NODE);
                    if (autoChild != null) {
                        IEDOMElement child = new IEDOMElement(
                                this.containingDocument, autoChild);
                        tempDomChildren[i] = child;
                    }
                    varChild.dispose();
                }
                varInd.dispose();
            } catch (Throwable e) {
                System.out
                        .println("##### Exception in IEDOMElement.getChildren() (i="
                                + i + "): " + e);
                e.printStackTrace();
            }
        }
        return new IEDOMNodeList(tempDomChildren);
        //throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented
        // method in IEDOMElement");
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getTagName() {
        try {
            Variant varTag = OLEUtils.getProperty(getDOMNode(), "nodeName");
            String tagName = null;
            if (varTag != null) {
                tagName = varTag.getString();
                varTag.dispose();
            }
            return tagName;
        } catch (NullPointerException e) {
            return "";
        }
    }

    public boolean hasAttribute(String name) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public boolean hasAttributeNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void removeAttribute(String name) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Attr removeAttributeNode(Attr oldAttr) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void removeAttributeNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void setAttribute(String name, String value) {
        Variant varName = new Variant(name);
        Variant varVal = new Variant(value);
        Variant varZero = new Variant(0l);

        OLEUtils.invokeCommand(this.getElement(), "setAttribute",
                new Variant[] { varName, varVal, varZero });
        varName.dispose();
        varVal.dispose();
        varZero.dispose();
    }

    public Attr setAttributeNode(Attr newAttr) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Attr setAttributeNodeNS(Attr newAttr) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void setAttributeNS(String namespaceURI, String qualifiedName,
            String value) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    ///////////////////////////////////
    /// org.w3c.dom.Node methods ///
    ///////////////////////////////////

    public Node appendChild(Node newChild) {
        if (!(newChild instanceof IEDOMElement)) {
            newChild = this.containingDocument.createElement(((INode) newChild)
                    .getTagName());
        }

        Variant varChild = new Variant(((IEDOMElement) newChild).getDOMNode());
        OLEUtils.invokeCommand(this.getDOMNode(), "appendChild",
                new Variant[] { varChild });
        varChild.dispose();

        return newChild;
    }

    /**
     * Creates an ExampleNode based on this element.
     */
    public Node cloneNode(boolean deep) {
        //	System.out.println("Cloning " + this);
        Variant varDeep = new Variant(deep);
        IEDOMElement clone = new IEDOMElement(this.containingDocument, OLEUtils
                .getInterface(OLEUtils.invokeCommand(this.getDOMNode(),
                        "cloneNode", new Variant[] { varDeep }),
                        OLEUtils.UUID_IHTML_DOM_NODE));
        varDeep.dispose();
        return clone;
    }

    public NamedNodeMap getAttributes() {
        Vector attribs = new Vector();

        OleAutomation domAttributeCollection = OLEUtils.getPropertyInterface(
                getDOMNode(), "attributes",
                OLEUtils.UUID_IHTML_ATTRIBUTE_COLLECTION);
        if (domAttributeCollection != null) {
            Variant varLen = OLEUtils.getProperty(domAttributeCollection,
                    "length");
            int noAttributes = varLen.getInt();
            varLen.dispose();

            for (int i = 0; i < noAttributes; i++) {
                try {
                    Variant varInd = new Variant(i);
                    Variant varAttrib = OLEUtils.invokeCommand(
                            domAttributeCollection, "item",
                            new Variant[] { varInd });
                    if (varAttrib != null) {
                        OleAutomation autoAttrib = varAttrib.getAutomation();
                        if (autoAttrib != null) {
                            IEDOMElement attribElement = new IEDOMElement(
                                    this.containingDocument, autoAttrib);
                            if (attribElement != null) {
                                attribs.add(attribElement);
                            }
                        }
                        varAttrib.dispose();
                    }
                    varInd.dispose();
                } catch (Throwable e) {
                    System.out
                            .println("##### Exception in IEDOMElement.getAttributes() (i="
                                    + i + "): " + e);
                    e.printStackTrace();
                }
            }
        }
        return new IEDOMNamedNodeMap(attribs);
    }

    public NodeList getChildNodes() {
        OleAutomation domChildrenCollection = OLEUtils.getPropertyInterface(
                getDOMNode(), "childNodes",
                OLEUtils.UUID_IHTML_DOM_CHILDREN_COLLECTION);
        Variant varLen = OLEUtils.getProperty(domChildrenCollection, "length");
        int noChildren = varLen.getInt();
        varLen.dispose();

        if ((this.getTagName().equalsIgnoreCase(WrapperManager.URL_TAGNAME) && this
                .getAttribute(WrapperManager.URL_IDENTIFIER) != null)
                || (this.getTagName().equalsIgnoreCase(WrapperManager.SRC_TAGNAME) && this
                        .getAttribute(WrapperManager.SRC_IDENTIFIER) != null)) {
            this.domChildren = new IEDOMElement[noChildren + 1];
        } else
            this.domChildren = new IEDOMElement[noChildren];

        for (int i = 0; i < noChildren; i++) {
            try {
                Variant varInd = new Variant(i);
                Variant varChild = OLEUtils
                        .invokeCommand(domChildrenCollection, "item",
                                new Variant[] { varInd });
                if (varChild != null) {
                    OleAutomation autoChild = OLEUtils.getInterface(varChild,
                            OLEUtils.UUID_IHTML_DOM_NODE);
                    if (autoChild != null) {
                        IEDOMElement child = new IEDOMElement(
                                this.containingDocument, autoChild);
                        this.domChildren[i] = child;
                    }
                    varChild.dispose();
                }
                varInd.dispose();
            } catch (Throwable e) {
                System.out
                        .println("##### Exception in IEDOMElement.getChildren() (i="
                                + i + "): " + e);
                e.printStackTrace();
            }
        }

        // add in a href node to handle links

        if (this.getTagName().equalsIgnoreCase(WrapperManager.URL_TAGNAME)
                && this.getAttribute(WrapperManager.URL_IDENTIFIER) != null) {
            IEDOMElement hrefElement = (IEDOMElement) this.containingDocument
                    .createElement(WrapperManager.URL_IDENTIFIER);
            IEDOMElement hrefUrl = (IEDOMElement) this.containingDocument
                    .createElement(this
                            .getAttribute(WrapperManager.URL_IDENTIFIER));
            try {
                //this.appendChild(hrefElement);
                hrefElement.setParent(this);
                hrefElement.nodeID = this.getNodeID().makeChildNodeID(
                        noChildren);
            } catch (NodeIDException e) {
                return null;
            }
            hrefElement.appendChild(hrefUrl);
            this.domChildren[noChildren] = hrefElement;
        }

        // add in a src node to handle images

        if (this.getTagName().equalsIgnoreCase(WrapperManager.SRC_TAGNAME)
                && this.getAttribute(WrapperManager.SRC_IDENTIFIER) != null) {
            IEDOMElement srcElement = (IEDOMElement) this.containingDocument
                    .createElement(WrapperManager.SRC_IDENTIFIER);
            IEDOMElement srcUrl = (IEDOMElement) this.containingDocument
                    .createElement(this
                            .getAttribute(WrapperManager.SRC_IDENTIFIER));
            try {
                srcElement.setParent(this);
                srcElement.nodeID = this.getNodeID()
                        .makeChildNodeID(noChildren);
            } catch (NodeIDException e) {
                return null;
            }
            srcElement.appendChild(srcUrl);
            this.domChildren[noChildren] = srcElement;
        }

        domChildrenCollection.dispose();

        return new IEDOMNodeList(this.domChildren);
    }

    public Node getFirstChild() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Node getLastChild() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getLocalName() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getNamespaceURI() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Node getNextSibling() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getNodeName() {
        return this.getTagName();
    }

    public short getNodeType() {
        try {
            Variant nodeTypeVar = OLEUtils.getProperty(domNode, "nodeType");
            if (nodeTypeVar == null) {
                return Node.ATTRIBUTE_NODE; // is this right?
            } else {
                short nodeType = nodeTypeVar.getShort();
                nodeTypeVar.dispose();
                return nodeType;
            }
        } catch (NullPointerException e) {
            return Node.ELEMENT_NODE;
        }
    }

    public String getNodeValue() {
        return null; // as per DOM spec
    }

    public Document getOwnerDocument() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Node getParentNode() {
        if (this.parent == null) {
            OleAutomation pn = OLEUtils.getPropertyInterface(this.domNode,
                    "parentNode", OLEUtils.UUID_IHTML_DOM_NODE);
            if (pn != null) {
                this.parent = new IEDOMElement(this.containingDocument, pn);
            }
        }
        return this.parent;
    }

    public String getPrefix() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Node getPreviousSibling() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public boolean hasAttributes() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public boolean hasChildNodes() {
        return OLEUtils.invokeCommand(this.getDOMNode(), "hasChildNodes",
                new Variant[0]).getBoolean();
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        Variant varNew = new Variant(((IEDOMElement) newChild).getDOMNode());
        Variant varRef = new Variant(((IEDOMElement) refChild).getDOMNode());
        IEDOMElement retNode = new IEDOMElement(this.containingDocument,
                OLEUtils.getInterface(OLEUtils.invokeCommand(this.getDOMNode(),
                        "insertBefore", new Variant[] { varNew, varRef }),

                OLEUtils.UUID_IHTML_DOM_NODE));
        varNew.dispose();
        varRef.dispose();
        return retNode;
    }

    public boolean isSupported(String feature, String version) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void normalize() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public Node removeChild(Node oldChild) throws DOMException {
        Variant varOld = new Variant(((IEDOMElement) oldChild).getDOMNode());
        IEDOMElement old = new IEDOMElement(this.containingDocument, OLEUtils
                .getInterface(OLEUtils.invokeCommand(this.getDOMNode(),
                        "removeChild", new Variant[] { varOld }),
                        OLEUtils.UUID_IHTML_DOM_NODE));
        varOld.dispose();
        return old;
    }

    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        Variant varNew = new Variant(((IEDOMElement) newChild).getDOMNode());
        Variant varOld = new Variant(((IEDOMElement) oldChild).getDOMNode());
        IEDOMElement old = new IEDOMElement(this.containingDocument, OLEUtils
                .getInterface(OLEUtils.invokeCommand(this.getDOMNode(),
                        "replaceChild", new Variant[] { varNew, varOld }),

                OLEUtils.UUID_IHTML_DOM_NODE));
        varNew.dispose();
        varOld.dispose();
        return old;
    }

    public void setNodeValue(String nodeValue) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void setPrefix(String prefix) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    ///////////////////////////////////
    /// org.w3c.dom.Text methods ///
    ///////////////////////////////////

    public org.w3c.dom.Text splitText(int offset) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    ////////////////////////////////////////////
    /// org.w3c.dom.CharacterData methods ///
    ////////////////////////////////////////////

    public void appendData(String arg) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void deleteData(int offset, int count) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getData() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public int getLength() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void insertData(int offset, String arg) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void replaceData(int offset, int count, String arg) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public void setData(String data) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String substringData(int offset, int count) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR,
                "Unimplemented method in IEDOMElement");
    }

    public String getHighlightedColor() {
        String bgColor = "";
        try {
            bgColor = OLEUtils.getProperty(getStyle(), "backgroundColor")
                    .getString();
        } catch (Exception e) {
            // exception because no background color
            INode[] span = this.getChildren("SPAN");
            if (span != null && span.length > 1) {
                bgColor = ((IDOMElement) span[0]).getHighlightedColor();
                System.out.println(bgColor);
            }
        }
        return bgColor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Attr#getName()
     */
    public String getName() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Attr#getSpecified()
     */
    public boolean getSpecified() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Attr#getValue()
     */
    public String getValue() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Attr#setValue(java.lang.String)
     */
    public void setValue(String arg0) throws DOMException { }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Attr#getOwnerElement()
     */
    public Element getOwnerElement() {
        return null;
    }

    private IFeatureSet featureSet = null;

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#addFeature(java.lang.Object)
     */

    public void addFeature(AbstractFeature feature) {
        if (featureSet == null) {
            featureSet = new NGramsFragmentSet(this);
        }

        //System.err.println(this.nodeName() + ": "+feature.toString());
        featureSet.addFeature(feature);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#getFeatures(int
     *      n)
     */
    public AbstractFeature[] getFeatures(int n) {
        if (featureSet != null) {
            System.err.println("IEDom: getFeatures: " + n);
            return featureSet.getFeatures(n);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#getFeatures()
     */
    public IFeatureSet getFeatureSet() {
        if (featureSet != null) {
            return featureSet;
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#getFeatures()
     */
    public AbstractFeature[] getFeatures() {
        if (featureSet != null) {
            return featureSet.getFeatures();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#nodeName()
     */
    public String nodeName() {
        switch (this.getNodeType()) {
        case INode.TEXT_NODE:
            return "#TEXT#";
        case INode.COMMENT_NODE:
            return "#COMMENT#";
        default:
            return this.getNodeName();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#height()
     */
    public int height() {
        return this.getHeight();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#numChildren()
     */
    public int numChildren() {
        return this.getChildNodes().getLength();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#numDescendants()
     */
    public int numDescendants() {
        int numDescendants = 0;
        int numChildren = this.numChildren();
        for (int i = 0; i < numChildren; i++) {
            IAugmentedNode child = (IAugmentedNode) this.getChild(i);
            numDescendants += child.numDescendants();
        }
        return numDescendants;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#textSize()
     */
    public int textSize() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            String nodeValue = this.getNodeValue();
            if (nodeValue != null) {
                return nodeValue.length();
            } else {
                return 0;
            }
        } else {
            /* recursively calculate text size */
            int textSize = 0;
            int numChildren = this.numChildren();
            for (int i = 0; i < numChildren; i++) {
                IAugmentedNode child = (IAugmentedNode) this.getChild(i);
                textSize += child.textSize();
            }
            return textSize;
        }
    }

    ICluster cluster;
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#getCluster()
     */
    public ICluster getCluster() {
        return cluster;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#setCluster(edu.mit.lcs.haystack.server.infoextraction.ICluster)
     */
    public void setCluster(ICluster cluster) {
        this.cluster = cluster;
    }

    private int index;
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#setIndex(int)
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#getIndex()
     */
    public int getIndex() {
        return this.index;
    }
}

