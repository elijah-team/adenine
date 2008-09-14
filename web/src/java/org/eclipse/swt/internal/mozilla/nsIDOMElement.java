/*
 * Created on Aug 3, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMElement extends nsIDOMNode {

	static final int LAST_METHOD_ID = nsIDOMNode.LAST_METHOD_ID + 16;
	
	public static final String NS_IDOMELEMENT_IID_STRING =
		"a6cf9078-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_IDOMELEMENT_IID =
		new nsID(NS_IDOMELEMENT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMElement(int address) {
		super(address);
	}
	
	public int GetTagName(int aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 1, getAddress(), aReturn);
	}
	
	public int GetAttribute(int aName, int aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 2, getAddress(), aName, aReturn);
	}
	
	public int SetAttribute(int aName, int aValue) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 3, getAddress(), aName, aValue);
	}
	
	public int RemoveAttribute(int aName) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 4, getAddress(), aName);
	}
	
	public int GetAttributeNode(int aName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 5, getAddress(), aName, aReturn);
	}

	public int SetAttributeNode(int aNewAttr, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 6, getAddress(), aNewAttr, aReturn);
	}
	
	public int RemoveAttributeNode(int aOldAttr, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 7, getAddress(), aOldAttr, aReturn);
	}
	
	public int GetElementsByTagName(int aName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 8, getAddress(), aName, aReturn);
	}
	
	public int GetAttributeNS(int aNamespaceURI, int aLocalName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 9, getAddress(), aNamespaceURI, aLocalName, aReturn);
	}
	
	public int SetAttributeNS(int aNamespaceURI, int aQualifiedName, int aValue) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 10, getAddress(), aNamespaceURI, aValue);
	}
	
	public int RemoveAttributeNS(int aNamespaceURI, int aLocalName) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 11, getAddress(), aNamespaceURI, aLocalName);
	}
	
	public int GetAttributeNodeNS(int aNamespaceURI, int aLocalName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 12, getAddress(), aNamespaceURI, aLocalName, aReturn);
	}
	
	public int SetAttributeNodeNS(int aNewAttr, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 13, getAddress(), aNewAttr, aReturn);
	}
	
	public int GetElementsByTagNameNS(int aNamespaceURI, int aLocalName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 14, getAddress(), aNamespaceURI, aLocalName, aReturn);
	}
	
	public int HasAttribute(int aName, boolean[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 15, getAddress(), aName, aReturn);
	}
	
	public int HasAttributeNS(int aNamespaceURI, int aLocalName, boolean[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 16, getAddress(), aNamespaceURI, aLocalName, aReturn);
	}
}
