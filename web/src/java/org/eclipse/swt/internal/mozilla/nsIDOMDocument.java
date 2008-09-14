/*
 * Created on Aug 6, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMDocument extends nsIDOMNode {

	static final int LAST_METHOD_ID = nsIDOMNode.LAST_METHOD_ID + 17;
	
	public static final String NS_IDOMDOCUMENT_IID_STRING =
		"a6cf9075-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_IDOMDOCUMENT_IID =
		new nsID(NS_IDOMNODE_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMDocument(int address) {
		super(address);
	}

	
	public int GetDocType(int[] aType) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 1, getAddress(), aType);
	}
	
	public int GetImplementation(int[] aImplementation) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 2, getAddress(), aImplementation);
	}
	
	public int GetDocumentElement(int[] aElement) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 3, getAddress(), aElement);
	}
	
	public int CreateElement(int aTagName, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 4, getAddress(), aTagName, aReturn);
	}
	
	public int CreateDocumentFragment(int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 5, getAddress(), aReturn);
	}
	
	public int CreateTextNode(int aData, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 6, getAddress(), aData, aReturn);
	}
	
	public int CreateComment(int aData, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 7, getAddress(), aData, aReturn);
	}
	
	public int CreateCDATASection(int aData, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 8, getAddress(), aData, aReturn);
	}
	
	public int CreateProcessingInstruction(int aTarget, int aData, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 9, getAddress(), aTarget, aData, aReturn);
	}
	
	public int CreateAttribute(int aName, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 10, getAddress(), aName, aReturn);
	}
	
	public int CreateEntityReference(int aName, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 11, getAddress(), aName, aReturn);
	}
	
	public int GetElementsByTagName(int aName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 12, getAddress(), aName);
	}
	
	public int ImportNode(int aNode, boolean deep, int[] aReturn) throws DOMException{
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 13, getAddress(), aNode, deep, aReturn);
	}
	
	public int CreateElementNS(int aNamespace, int aName, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 14, getAddress(), aNamespace, aName, aReturn);
	}
	
	public int CreateAttributeNS(int aNamespace, int aName, int[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 15, getAddress(), aNamespace, aName, aReturn);
	}
	
	public int GetElementsByTagNameNS(int aNamespace, int aName, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 16, getAddress(), aNamespace, aName, aReturn);
	}
	
	public int GetElementByID(int aElementId, int[] aReturn) {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 17, getAddress(), aElementId, aReturn);
	}
}
