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
import java.util.List;

import org.eclipse.swt.internal.mozilla.XPCOM;
import org.eclipse.swt.internal.mozilla.nsIDOMCSSStyleDeclaration;
import org.eclipse.swt.internal.mozilla.nsIDOMElement;
import org.eclipse.swt.internal.mozilla.nsIDOMElementCSSInlineStyle;
import org.eclipse.swt.internal.mozilla.nsIDOMNode;
import org.eclipse.swt.internal.mozilla.nsIDOMNodeList;
import org.eclipse.swt.internal.mozilla.nsIDOMNSHTMLElement;
import org.eclipse.swt.internal.mozilla.nsEmbedString;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeComparator;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 * @author Ryan Manuel
 * @version 1.0
 */
public class MozDOMElement implements Text, Comment, IDOMElement {
	
	public static boolean TREAT_TEXT_NODES_IDENTICALLY = false;
	protected nsIDOMElement nsIElt;
	protected Document containingDocument;
	protected nsIDOMNode nsINode;
	protected MozDOMElement parent;
	protected NodeID nodeID;
	protected int size;
	protected String originalBgColor;
    protected String originalTextColor;
    protected nsIDOMNSHTMLElement nsINSHTMLElt;
    protected nsIDOMCSSStyleDeclaration nsIStyle;
	
	public MozDOMElement(Document containingDocument, nsIDOMNode nsINode) {
		this.containingDocument = containingDocument;
		this.nsINode = nsINode;
	}
	
	protected nsIDOMCSSStyleDeclaration getStyle() {
		if(this.nsIStyle == null) {
			int[] aInlineStyle = new int[1];
			int[] aStyle = new int[1];
			int rc = this.getDOMNode().QueryInterface(
					nsIDOMElementCSSInlineStyle.NS_IDOMELEMENTCSSINLINESTYLE_IID, 
					aInlineStyle);
			MozUtils.mozCheckReturn(rc);
			if(aInlineStyle[0] == 0) {
				this.nsIStyle = null;
			}
			else {
				nsIDOMElementCSSInlineStyle nsIElementStyle = new nsIDOMElementCSSInlineStyle(aInlineStyle[0]);
				rc = nsIElementStyle.GetStyle(aStyle);
				MozUtils.mozCheckReturn(rc, aStyle[0]);
				this.nsIStyle = new nsIDOMCSSStyleDeclaration(aStyle[0]);
				nsIElementStyle.Release();
			}
		}
		return this.nsIStyle;
	}
	
	protected nsIDOMNode getDOMNode() {
		return nsINode;
	}
	
	protected nsIDOMElement getDOMElement() {
		if(this.nsIElt == null) {
			int[] aElement = new int[1];
			int rc = this.nsINode.QueryInterface(nsIDOMElement.NS_IDOMELEMENT_IID, aElement);
			MozUtils.mozCheckReturn(rc, aElement[0]);
			this.nsIElt = new nsIDOMElement(aElement[0]);
		}
		return this.nsIElt;
	}
	
	protected nsIDOMNSHTMLElement getDOMNSHTMLElement() {
		if(this.nsINSHTMLElt == null) {
			int[] aNSHTMLElement = new int[1];
			int rc = this.nsINode.QueryInterface(nsIDOMNSHTMLElement.NS_IDOMNSHTMLELEMENT_IID, aNSHTMLElement);
			MozUtils.mozCheckReturn(rc, aNSHTMLElement[0]);
			this.nsINSHTMLElt = new nsIDOMNSHTMLElement(aNSHTMLElement[0]);
		}
		return this.nsINSHTMLElt;
	}
	
	protected void finalize() {
		/*if(this.nsIElt != null)
			this.nsIElt.Release();
		if(this.nsINSHTMLElt != null)
			this.nsINSHTMLElt.Release();
		if(this.nsIStyle != null)
			this.nsIStyle.Release();
		this.nsINode.Release();*/
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Text#splitText(int)
	 */
	public Text splitText(int arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getTagName()
	 */
	public String getTagName() {
		nsEmbedString nodeName = new nsEmbedString();
		int rc = this.getDOMNode().GetNodeName(nodeName.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strNodeName = nodeName.toString();
		nodeName.dispose();
		return strNodeName;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#getNodeText()
	 */
	public String getNodeText() {
		String strNodeText = new String();
		int rc = XPCOM.NS_OK;
		if(this.getNodeType() == Node.TEXT_NODE) {
			nsEmbedString strValue = new nsEmbedString();
			rc = this.getDOMNode().GetNodeValue(strValue.getAddress());
			MozUtils.mozCheckReturn(rc);
			strNodeText = strValue.toString();
			strValue.dispose();
		}
		else if (this.getTagName().equalsIgnoreCase(WrapperManager.URL_IDENTIFIER)
                || this.getTagName().equalsIgnoreCase(WrapperManager.SRC_IDENTIFIER)) {
            if (this.getChild(0) != null)
                strNodeText = this.getChild(0).getTagName();
        }
		else {
			StringBuffer text = new StringBuffer();
			NodeList children = this.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (!((IDOMElement) children.item(i)).getTagName().equalsIgnoreCase(
	                    WrapperManager.URL_IDENTIFIER)
	                    && !((IDOMElement) children.item(i)).getTagName().equalsIgnoreCase(
	                            WrapperManager.SRC_IDENTIFIER))
					text.append(((IDOMElement)children.item(i)).getNodeText());
			}
			strNodeText = text.toString();
		}
		
		return strNodeText;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#getOuterHTML()
	 */
	public String getOuterHTML() {
		int[] aReturnNode = new int[1];
		int rc = this.getDOMNode().CloneNode(true, aReturnNode);
		MozUtils.mozCheckReturn(rc, aReturnNode[0]);
		nsIDOMNode tempNode = new nsIDOMNode(aReturnNode[0]);
		MozDOMElement temp = (MozDOMElement) this.containingDocument.createElement("SPAN");
		temp.appendChild(new MozDOMElement(this.containingDocument, tempNode));
		return temp.getInnerHTML();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#getInnerHTML()
	 */
	public String getInnerHTML() {
		nsEmbedString nsInnerHTML = new nsEmbedString();
		int rc = this.getDOMNSHTMLElement().GetInnerHTML(nsInnerHTML.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strInnerHTML = nsInnerHTML.toString();
		nsInnerHTML.dispose();
		return strInnerHTML;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#startTagHTML()
	 */
	public String startTagHTML() {
		String returnString = null;
		if (this.getNodeType() == Node.TEXT_NODE) {
            returnString = this.getNodeText();
        } else {
            String outer = this.getOuterHTML();
            String inner = this.getInnerHTML();
            if (inner != null && !inner.equals("") && outer.indexOf(inner) >= 0) {
                returnString = outer.substring(0, outer.indexOf(inner));
            } else {
                returnString = outer;
            }
        }
		return returnString;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#endTagHTML()
	 */
	public String endTagHTML() {
		String returnString = null;
		if (this.getNodeType() == Node.TEXT_NODE) {
            returnString = "";
        } else {
            String outer = this.getOuterHTML();
            String inner = this.getInnerHTML();
            if (inner != null && !inner.equals("")) {
                returnString = outer.substring(outer.indexOf(inner) + inner.length());
            } else {
                returnString = "";
            }
        }
		return returnString;
	}
	
	public INode replaceNode(INode newNode) {
		MozDOMElement parent = (MozDOMElement) this.getParentNode();
		parent.replaceChild(newNode, this);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#highlight(java.lang.String, java.lang.String)
	 */
	public IDOMElement highlight(String highlightColor, String textColor) {
		if (this.getNodeType() == Node.TEXT_NODE) {
			if(((IDOMElement) this.getParentNode()).getTagName().equalsIgnoreCase(WrapperManager.URL_TAGNAME)) {
				return ((IDOMElement) this.getParentNode()).highlight(highlightColor, textColor);
			}
				    System.out.println(">>> highlighting text node " + this);
			// implement by surrounding text with a <SPAN> element
			MozDOMElement spanEl = (MozDOMElement) this.containingDocument.createElement("SPAN");
			String text = this.getNodeText();
			MozDOMElement textEl = (MozDOMElement) this.containingDocument.createTextNode(text);
			spanEl.appendChild(textEl);
			spanEl.highlight(highlightColor, textColor);
			this.replaceNode(spanEl);
			return textEl;
		} 
		else {
			nsEmbedString nsstrBackgroundColor = new nsEmbedString("background-Color");
			nsEmbedString nsstrColor = new nsEmbedString("color");
			nsEmbedString nsstrImportant = new nsEmbedString("important");
			nsEmbedString nsstrHighlightColor = new nsEmbedString(highlightColor);
			nsEmbedString nsstrTextColor = new nsEmbedString(textColor);
			nsIDOMCSSStyleDeclaration style = this.getStyle();
			if(style != null) {
				int rc = XPCOM.NS_OK;
				if (this.originalBgColor == null) {
					nsEmbedString nsstrBackgroundColorValue = new nsEmbedString();
					nsEmbedString nsstrColorValue = new nsEmbedString();
					rc = style.GetPropertyValue(nsstrBackgroundColor.getAddress(),
							nsstrBackgroundColorValue.getAddress());
					MozUtils.mozCheckReturn(rc);
					this.originalBgColor = nsstrBackgroundColorValue.toString();
					rc = style.GetPropertyValue(nsstrColor.getAddress(),
							nsstrColorValue.getAddress());
					MozUtils.mozCheckReturn(rc);
					this.originalTextColor = nsstrColorValue.toString();
					nsstrBackgroundColorValue.dispose();
					nsstrColorValue.dispose();
				}
				
				rc = style.SetProperty(nsstrBackgroundColor.getAddress(), 
						nsstrHighlightColor.getAddress(),
						nsstrImportant.getAddress());
				MozUtils.mozCheckReturn(rc);
				rc = style.SetProperty(nsstrColor.getAddress(), 
						nsstrTextColor.getAddress(),
						nsstrImportant.getAddress());
				MozUtils.mozCheckReturn(rc);
				nsstrBackgroundColor.dispose();
				nsstrColor.dispose();
				nsstrImportant.dispose();
				nsstrHighlightColor.dispose();
				nsstrTextColor.dispose();
			}
			else {
				System.out.println(">>> Style was null.  Could not highlight element");
			}
			return this;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#unhighlight()
	 */
	public void unhighlight() {
		nsEmbedString nsstrImportant = new nsEmbedString("important");
		if (this.getNodeType() == Node.TEXT_NODE) {
			if(((IDOMElement) this.getParentNode()).getTagName().
					equalsIgnoreCase(WrapperManager.URL_TAGNAME)) {
				((IDOMElement) this.getParentNode()).unhighlight();
			}
			else {
				MozDOMElement spanNode = (MozDOMElement) this.getParentNode();
				if (spanNode == null)
					return;
				MozDOMElement spanParent = (MozDOMElement) spanNode.getParentNode();
				spanParent.insertBefore(this, spanNode);
				spanNode.removeNode();
			}
        } 
		else {
			nsEmbedString nsstrBackgroundColor = new nsEmbedString("background-Color");
			nsEmbedString nsstrColor = new nsEmbedString("color");
	        nsIDOMCSSStyleDeclaration style = this.getStyle();
	        if(style != null) {
	        	int rc = XPCOM.NS_OK;
	        	if (this.originalBgColor != null) {
	        		nsEmbedString nsstrOriginalColor = new nsEmbedString(this.originalBgColor);
	        		rc = this.getStyle().SetProperty(nsstrBackgroundColor.getAddress(),
	        				nsstrOriginalColor.getAddress(),
							nsstrImportant.getAddress());
	        		MozUtils.mozCheckReturn(rc);
	        		nsstrOriginalColor.dispose();
	        	} 
	        	else {
	        		nsEmbedString retString = new nsEmbedString();
	        		rc = this.getStyle().RemoveProperty(nsstrBackgroundColor.getAddress(), retString.getAddress());
	        		MozUtils.mozCheckReturn(rc);
	        		retString.dispose();
	        	}
	        	if (this.originalTextColor != null) {
	        		nsEmbedString nsstrOriginalTextColor = new nsEmbedString(this.originalTextColor);
	        		rc = this.getStyle().SetProperty(nsstrBackgroundColor.getAddress(),
	        				nsstrOriginalTextColor.getAddress(),
							nsstrImportant.getAddress());
	        		MozUtils.mozCheckReturn(rc);
	        		nsstrOriginalTextColor.dispose();
	        	} 
	        	else {
	        		nsEmbedString retString = new nsEmbedString();
	        		rc = this.getStyle().RemoveProperty(nsstrColor.getAddress(), retString.getAddress());
	        		MozUtils.mozCheckReturn(rc);
	        		retString.dispose();
	        	}
	        }
	        else {
	        	System.out.println(">>>> Style was null could not unhighight element");
	        }
        }
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getAttribute(java.lang.String)
	 */
	public String getAttribute(String attributeName) {
		nsEmbedString attName = new nsEmbedString(attributeName);
		nsEmbedString attValue = new nsEmbedString();
		int rc = this.getDOMElement().GetAttribute(attName.getAddress(), attValue.getAddress());
		MozUtils.mozCheckReturn(rc);
		String strValue = attValue.toString();
		attValue.dispose();
		return strValue;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#getData()
	 */
	public String getData() throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#setData(java.lang.String)
	 */
	public void setData(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#getLength()
	 */
	public int getLength() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#substringData(int, int)
	 */
	public String substringData(int arg0, int arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#appendData(java.lang.String)
	 */
	public void appendData(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#insertData(int, java.lang.String)
	 */
	public void insertData(int arg0, String arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#deleteData(int, int)
	 */
	public void deleteData(int arg0, int arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.CharacterData#replaceData(int, int, java.lang.String)
	 */
	public void replaceData(int arg0, int arg1, String arg2)
	throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	public String getNodeName() {
		return this.getTagName();
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeValue()
	 */
	public String getNodeValue() throws DOMException {
		// as per dom spec
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#setNodeValue(java.lang.String)
	 */
	public void setNodeValue(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNodeType()
	 */
	public short getNodeType() {
		short[] aType = new short[1];
		int rc = nsINode.GetNodeType(aType);
		MozUtils.mozCheckReturn(rc);
		return aType[0];
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getParentNode()
	 */
	public Node getParentNode() {
		if(this.parent == null) {
			int[] aParentNode = new int[1];
			nsIDOMNode parentNode = null;
			int rc = nsINode.GetParentNode(aParentNode);	
			MozUtils.mozCheckReturn(rc);
			if(aParentNode[0] == 0) {
				this.parent = null;
			}
			else {
				parentNode = new nsIDOMNode(aParentNode[0]);	
				this.parent = new MozDOMElement(this.containingDocument, parentNode);
			}
		}		
		return this.parent;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getChildNodes()
	 */
	public NodeList getChildNodes() {
		int[] aChildNodeList = new int[1];
		int rc = this.getDOMNode().GetChildNodes(aChildNodeList);
		MozUtils.mozCheckReturn(rc, aChildNodeList[0]);
		nsIDOMNodeList childNodeList = new nsIDOMNodeList(aChildNodeList[0]);
		int[] aLength = new int[1];
		rc = childNodeList.GetLength(aLength);
		MozUtils.mozCheckReturn(rc);
		Element[] nodes = null;
		if ((this.getTagName().equalsIgnoreCase(WrapperManager.URL_TAGNAME) && this
                .getAttribute(WrapperManager.URL_IDENTIFIER) != null)
                || (this.getTagName().equalsIgnoreCase(WrapperManager.SRC_TAGNAME) && this
                        .getAttribute(WrapperManager.SRC_IDENTIFIER) != null)) {
			nodes = new Element[aLength[0] + 1];
		}
		else {
			nodes = new Element[aLength[0]];
		}
		
		for(int i = 0; i < aLength[0]; i++) {
			int[] aNode = new int[1];
			rc = childNodeList.Item(i, aNode);
			MozUtils.mozCheckReturn(rc, aNode[0]);
			nsIDOMNode node = new nsIDOMNode(aNode[0]);
			MozDOMElement mozElt = new MozDOMElement(this.containingDocument, node);
			nodes[i] = mozElt;
		}
		
		childNodeList.Release();
		
		// add in a href node to handle links

        if (this.getTagName().equalsIgnoreCase(WrapperManager.URL_TAGNAME)
                && this.getAttribute(WrapperManager.URL_IDENTIFIER) != null) {
            MozDOMElement hrefElement = (MozDOMElement) this.containingDocument
                    .createElement(WrapperManager.URL_IDENTIFIER);
            MozDOMElement hrefUrl = (MozDOMElement) this.containingDocument
                    .createElement(this
                            .getAttribute(WrapperManager.URL_IDENTIFIER));
            try {
                //this.appendChild(hrefElement);
                hrefElement.setParent(this);
                hrefElement.nodeID = this.getNodeID().makeChildNodeID(
                        aLength[0]);
            } catch (NodeIDException e) {
                return null;
            }
            hrefElement.appendChild(hrefUrl);
            nodes[aLength[0]] = hrefElement;
        }

        // add in a src node to handle images
        
        if (this.getTagName().equalsIgnoreCase(WrapperManager.SRC_TAGNAME)
                && this.getAttribute(WrapperManager.SRC_IDENTIFIER) != null) {
            MozDOMElement srcElement = (MozDOMElement) this.containingDocument
                    .createElement(WrapperManager.SRC_IDENTIFIER);
            MozDOMElement srcUrl = (MozDOMElement) this.containingDocument
                    .createElement(this
                            .getAttribute(WrapperManager.SRC_IDENTIFIER));
            try {
                srcElement.setParent(this);
                srcElement.nodeID = this.getNodeID()
                        .makeChildNodeID(aLength[0]);
            } catch (NodeIDException e) {
                return null;
            }
            srcElement.appendChild(srcUrl);
            nodes[aLength[0]] = srcElement;
        }
                
		return new MozDOMNodeList(nodes);
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getFirstChild()
	 */
	public Node getFirstChild() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getLastChild()
	 */
	public Node getLastChild() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getPreviousSibling()
	 */
	public Node getPreviousSibling() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNextSibling()
	 */
	public Node getNextSibling() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getAttributes()
	 */
	public NamedNodeMap getAttributes() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getOwnerDocument()
	 */
	public Document getOwnerDocument() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#insertBefore(org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		nsIDOMNode nsNewChildNode = ((MozDOMElement) newChild).getDOMNode();
		nsIDOMNode nsRefChildNode = ((MozDOMElement) refChild).getDOMNode();
		int[] aReturnChild = new int[1];
		int rc = nsINode.InsertBefore(nsNewChildNode.getAddress(), nsRefChildNode.getAddress(), aReturnChild);
		MozUtils.mozCheckReturn(rc, aReturnChild[0]);
		nsIDOMNode returnChildNode = new nsIDOMNode(aReturnChild[0]);
		MozDOMElement mozReturnElement = new MozDOMElement(this.containingDocument, returnChildNode);
		return mozReturnElement;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#replaceChild(org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		nsIDOMNode nsNewChildNode = ((MozDOMElement) newChild).getDOMNode();
		nsIDOMNode nsOldChildNode = ((MozDOMElement) oldChild).getDOMNode();
		int[] aReturnChild = new int[1];
		int rc = nsINode.ReplaceChild(nsNewChildNode.getAddress(), nsOldChildNode.getAddress(), aReturnChild);
		MozUtils.mozCheckReturn(rc, aReturnChild[0]);
		nsIDOMNode returnChildNode = new nsIDOMNode(aReturnChild[0]);
		MozDOMElement mozReturnElement = new MozDOMElement(this.containingDocument, returnChildNode);
		return mozReturnElement;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#removeChild(org.w3c.dom.Node)
	 */
	public Node removeChild(Node oldChild) throws DOMException {
		nsIDOMNode nsOldChildNode = ((MozDOMElement) oldChild).getDOMNode();
		int[] aReturnChild = new int[1];
		int rc = nsINode.RemoveChild(nsOldChildNode.getAddress(), aReturnChild);
		MozUtils.mozCheckReturn(rc, aReturnChild[0]);
		nsIDOMNode returnChildNode = new nsIDOMNode(aReturnChild[0]);
		MozDOMElement mozReturnElement = new MozDOMElement(this.containingDocument, returnChildNode);
		return mozReturnElement;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
	 */
	public Node appendChild(Node newChild) throws DOMException {
		nsIDOMNode nsNewChildNode = ((MozDOMElement) newChild).getDOMNode();
		int[] aReturnChild = new int[1];
		int rc = nsINode.AppendChild(nsNewChildNode.getAddress(), aReturnChild);
		MozUtils.mozCheckReturn(rc, aReturnChild[0]);
		nsIDOMNode returnChildNode = new nsIDOMNode(aReturnChild[0]);
		MozDOMElement mozReturnElement = new MozDOMElement(this.containingDocument, returnChildNode);
		return mozReturnElement;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#hasChildNodes()
	 */
	public boolean hasChildNodes() {
		boolean[] aReturn = new boolean[1];
		int rc = this.getDOMNode().HasChildNodes(aReturn);
		MozUtils.mozCheckReturn(rc);
		return aReturn[0];
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#cloneNode(boolean)
	 */
	public Node cloneNode(boolean deep) {
		int[] aReturnChild = new int[1];
		int rc = this.getDOMNode().CloneNode(deep, aReturnChild);
		MozUtils.mozCheckReturn(rc, aReturnChild[0]);
		nsIDOMNode returnChildNode = new nsIDOMNode(aReturnChild[0]);
		MozDOMElement mozReturnElement = new MozDOMElement(this.containingDocument, returnChildNode);
		return mozReturnElement;
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#normalize()
	 */
	public void normalize() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#isSupported(java.lang.String, java.lang.String)
	 */
	public boolean isSupported(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getNamespaceURI()
	 */
	public String getNamespaceURI() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getPrefix()
	 */
	public String getPrefix() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#setPrefix(java.lang.String)
	 */
	public void setPrefix(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#getLocalName()
	 */
	public String getLocalName() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Node#hasAttributes()
	 */
	public boolean hasAttributes() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getNodeID()
	 */
	public NodeID getNodeID() {
		if (this.nodeID == null) {
			try {
				this.nodeID = new NodeID(this.getSiblingNo());
				// fill in the path to root

				INode anc = this.getAncestor(1);
				while (anc != null && !anc.toString().equals("#document")) {
					this.nodeID = this.nodeID.makeParentNodeID(anc.getSiblingNo());
					anc = anc.getAncestor(1);
				}
			}
			catch (NodeIDException e) {
				e.printStackTrace();
			}
		}		
		
		return this.nodeID;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getSize()
	 */
	public int getSize() {
		if (this.size == 0) {
			NodeList children = this.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				this.size += ((INode)children.item(i)).getSize();
			}
			this.size += 1;		// this node
		}
		return this.size;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getHeight()
	 */
	public int getHeight() {
		int maxHeight = 0;
		NodeList childNodes = this.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			int currHeight = ((INode)childNodes.item(i)).getHeight();
			if (currHeight > maxHeight)
				maxHeight = currHeight;
		}
		return maxHeight + 1;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPostorderNodes()
	 */
	public INode[] getPostorderNodes() {
		ArrayList postorder = new ArrayList();
		postorder.add(null);	// to make array 1-based
		getPostorderNodesHelper(postorder);
		return (INode[])postorder.toArray(new INode[0]);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPostorderNodesHelper(java.util.List)
	 */
	public void getPostorderNodesHelper(List postorder) {
		NodeList childNodes = getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			((IDOMElement)childNodes.item(i)).getPostorderNodesHelper(postorder);
		}
		postorder.add(this);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPreorderNodes()
	 */
	public INode[] getPreorderNodes() {
		ArrayList preorder = new ArrayList();
		preorder.add(null);	// to make array 1-based
		getPreorderNodesHelper(preorder);
		return (INode[])preorder.toArray(new INode[0]);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPreorderNodesHelper(java.util.List)
	 */
	public void getPreorderNodesHelper(List preorder) {
		NodeList childNodes = getChildNodes();
		preorder.add(this);
		for (int i = 0; i < childNodes.getLength(); i++) {
			((IDOMElement)childNodes.item(i)).getPreorderNodesHelper(preorder);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getSiblingNo()
	 */
	public int getSiblingNo() {
		int siblingNo = 0;
		if (this.getParentNode() == null) {
			siblingNo = 0;
		}
		else {
			int[] aPreviousSibling = new int[1];
			int rc = this.getDOMNode().GetPreviousSiblingChild(aPreviousSibling);
			MozUtils.mozCheckReturn(rc);
			int[] aCurrSibling = null;
			while(aPreviousSibling[0] != 0) {
				aCurrSibling = aPreviousSibling;
				nsIDOMNode currNode = new nsIDOMNode(aPreviousSibling[0]);			
				rc = currNode.GetPreviousSiblingChild(aPreviousSibling);
				MozUtils.mozCheckReturn(rc);
				siblingNo++;
				currNode.Release();
			}
		}
		return siblingNo;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#setSiblingNo(int)
	 */
	public void setSiblingNo(int siblingNo) {
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#isOnlyChild()
	 */
	public boolean isOnlyChild() {
		return (this.getSiblingNo() == 0 && this.getParentNode().getChildNodes().getLength() == 0);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getChild(int)
	 */
	public INode getChild(int index) {
		NodeList children = this.getChildNodes();
		INode returnNode = null;
		if (index > children.getLength()-1) {
			returnNode = null;
		}
		else {
			returnNode = (INode)children.item(index);
		}
		return returnNode;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getChildren(java.lang.String)
	 */
	public INode[] getChildren(String tagName) {
		NodeList children = this.getChildNodes();
		ArrayList tagChildren = new ArrayList();
		for (int i = 0; i < children.getLength(); i++) {
			if (((INode)children.item(i)).getTagName().equalsIgnoreCase(tagName)) {
				tagChildren.add(children.item(i));
			}
		}
		return (IDOMElement[])tagChildren.toArray(new MozDOMElement[0]);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getAncestor(int)
	 */
	public INode getAncestor(int generation) {
		INode returnNode = null;
		if (generation == 0) {
			returnNode = this;
		}
		else if (this.getParentNode() == null) {
			returnNode = null;
		}
		else {
			returnNode = ((INode)this.getParentNode()).getAncestor(generation-1);
		}
		return returnNode;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#setParent(edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode)
	 */
	public void setParent(INode parent) {
		this.parent = (MozDOMElement) parent;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getSiblings()
	 */
	public NodeList getSiblings() {
		NodeList returnList = null;
		if (this.getParentNode() == null) 
			returnList = new MozDOMNodeList();
		else
			returnList = this.getParentNode().getChildNodes();
		return returnList;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#removeNode()
	 */
	public INode removeNode() {
		// TODO finish this
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#removeChildNodes()
	 */
	public List removeChildNodes() throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#equals(edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode)
	 */
	public boolean equals(INode other) {
		return this.getComparator().equals(other);
	}
	
	public String getLabel() {
        if (!TREAT_TEXT_NODES_IDENTICALLY
                && this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeText();
        }
        return this.getTagName();
    }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#toString(int, java.lang.String)
	 */
	public String toString(int depth, String indent) {
		StringBuffer out = new StringBuffer();
        for (int i = 0; i < depth; i++) {
            out.append(indent);
        }
        out.append(this.getLabel());
        out.append("\n");
        NodeList children = this.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            out.append(((MozDOMElement) children.item(i)).toString(depth + 1,
                    indent));
        }
        return out.toString();
	}
	
	public String toString() {
		return (this.getNodeType() == Node.TEXT_NODE) ? this.getNodeText() : this.getTagName();
	}
	
	public int hashCode() {
        if (!TREAT_TEXT_NODES_IDENTICALLY
                && this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeText().hashCode();
        } else {
            return this.getTagName().hashCode();
        }
    }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getComparator()
	 */
	public NodeComparator getComparator() {
		if (this.getNodeType() == Node.TEXT_NODE) {
			return new MozDOMNodeComparator(this.getTagName(), this.getNodeText());
		}
		else {
			return new MozDOMNodeComparator(this.getTagName(), this.getTagName());
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getDeleteCost()
	 */
	public int getDeleteCost() {
		return this.getSize();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getInsertCost()
	 */
	public int getInsertCost() {
		return this.getSize();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getChangeCost(edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode)
	 */
	public int getChangeCost(INode other) {
		return (this.equals(other)) ? 0 : this.getSize() + other.getSize();
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String name, String value) throws DOMException {
		nsEmbedString nsName = new nsEmbedString(name);
		nsEmbedString nsValue = new nsEmbedString(value);
		int rc = this.getDOMElement().SetAttribute(nsName.getAddress(), nsValue.getAddress());
		MozUtils.mozCheckReturn(rc);
		nsName.dispose();
		nsValue.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getAttributeNode(java.lang.String)
	 */
	public Attr getAttributeNode(String arg0) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#setAttributeNode(org.w3c.dom.Attr)
	 */
	public Attr setAttributeNode(Attr arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#removeAttributeNode(org.w3c.dom.Attr)
	 */
	public Attr removeAttributeNode(Attr arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getElementsByTagName(java.lang.String)
	 */
	public NodeList getElementsByTagName(String name) {
		nsEmbedString nsName = new nsEmbedString();
		int[] aElements = new int[1];
		int rc = this.getDOMElement().GetElementsByTagName(nsName.getAddress(), aElements);
		MozUtils.mozCheckReturn(rc, aElements[0]);
		nsName.dispose();
		nsIDOMNodeList elementList = new nsIDOMNodeList(aElements[0]);
		int[] aLength = new int[1];
		rc = elementList.GetLength(aLength);
		MozUtils.mozCheckReturn(rc);
		Element[] nodes = new Element[aLength[0]];
		for(int i = 0; i < aLength[0]; i++) {
			int[] aNode = new int[1];
			rc = elementList.Item(i, aNode);
			MozUtils.mozCheckReturn(rc, aNode[0]);
			nsIDOMNode node = new nsIDOMNode(aNode[0]);
			MozDOMElement mozElt = new MozDOMElement(this.containingDocument, node);
			nodes[i] = mozElt;
		}
		return new MozDOMNodeList(nodes);
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getAttributeNS(java.lang.String, java.lang.String)
	 */
	public String getAttributeNS(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#setAttributeNS(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void setAttributeNS(String arg0, String arg1, String arg2)
	throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#removeAttributeNS(java.lang.String, java.lang.String)
	 */
	public void removeAttributeNS(String arg0, String arg1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getAttributeNodeNS(java.lang.String, java.lang.String)
	 */
	public Attr getAttributeNodeNS(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#setAttributeNodeNS(org.w3c.dom.Attr)
	 */
	public Attr setAttributeNodeNS(Attr arg0) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#getElementsByTagNameNS(java.lang.String, java.lang.String)
	 */
	public NodeList getElementsByTagNameNS(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String arg0) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see org.w3c.dom.Element#hasAttributeNS(java.lang.String, java.lang.String)
	 */
	public boolean hasAttributeNS(String arg0, String arg1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMElement#getHighlightedColor()
	 */
	public String getHighlightedColor() {
		String strReturnColor = "";
		if(this.getNodeType() == Node.TEXT_NODE) {
			if(((IDOMElement) this.getParentNode()).
					getTagName().equalsIgnoreCase(WrapperManager.URL_TAGNAME) ||
					((IDOMElement) this.getParentNode()).getTagName().equalsIgnoreCase("SPAN"))
				strReturnColor = ((IDOMElement) this.getParentNode()).getHighlightedColor();
			else
				strReturnColor = "";
		}
		else {
			nsEmbedString backgroundColor = new nsEmbedString("background-Color");
			nsEmbedString returnColor = new nsEmbedString();
			nsIDOMCSSStyleDeclaration style = this.getStyle();
			if(style != null) {
				int rc = style.GetPropertyValue(backgroundColor.getAddress(), returnColor.getAddress());
				MozUtils.mozCheckReturn(rc);
				strReturnColor = returnColor.toString();
				backgroundColor.dispose();
				returnColor.dispose();
			}
		}
		return strReturnColor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement#copy()
	 */
	public INode copy() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in MozDOMElement");
	}
	
}
