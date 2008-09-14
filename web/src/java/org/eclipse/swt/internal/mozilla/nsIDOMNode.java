/*
 * Created on Jul 29, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.eclipse.swt.internal.mozilla.XPCOM;
import org.eclipse.swt.internal.mozilla.nsID;
import org.eclipse.swt.internal.mozilla.nsISupports;
import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMNode extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 25;
	
	public static final String NS_IDOMNODE_IID_STRING =
		"a6cf907c-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_IDOMNODE_IID =
		new nsID(NS_IDOMNODE_IID_STRING);
	
	public static final int ELEMENT_NODE = 1;
	public static final int ATTRIBUTE_NODE = 2;
	public static final int TEXT_NODE = 3;
	public static final int CDATA_SECTION_NODE = 4;
	public static final int ENTITY_REFERENCE_NODE = 5;
	public static final int ENTITY_NODE = 6;
	public static final int PROCESSING_INSTRUCTION_NODE = 7;
	public static final int COMMENT_NODE = 8;
	public static final int DOCUMENT_NODE = 9;
	public static final int DOCUMENT_TYPE_NODE = 10;
	public static final int DOCUMENT_FRAGMENT_NODE = 11;
	public static final int NOTATION_NODE = 12;
	
	/**
	 * @param address
	 */
	public nsIDOMNode(int address) {
		super(address);
	}
	
	public int GetNodeName(int aName) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aName);
	}
	
    public int GetNodeValue(int aValue) throws DOMException{
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aValue);
    }
    
    public int SetNodeValue(int aValue) throws DOMException {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aValue);
    }
    
    public int GetNodeType(short[] aNodeType) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 4, getAddress(), aNodeType);
    }
    
    public int GetParentNode(int[] aParentNode) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 5, getAddress(), aParentNode);
    }
    
    public int GetChildNodes(int[] aChildNodes) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 6, getAddress(), aChildNodes);
    }
    
    public int GetFirstChild(int[] aFirstChild) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 7, getAddress(), aFirstChild);
    }
    
    public int GetLastChild(int[] aLastChild) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 8, getAddress(), aLastChild);
    }
    
    public int GetPreviousSiblingChild(int[] aPreviousSibling) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 9, getAddress(), aPreviousSibling);
    }
    
    public int GetNextSibling(int[] aNextSibling) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 10, getAddress(), aNextSibling);
    }
    
    public int GetAttributes(int[] aAttributes) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 11, getAddress(), aAttributes);
    }
    
    public int GetOwnerDocument(int[] aOwnerDocument) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 12, getAddress(), aOwnerDocument);
    }
    
    public int InsertBefore(int aNewChild, int aRefChild, int[] aReturn) throws DOMException {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 13, getAddress(), aNewChild, aRefChild, aReturn);
    }
    
    public int ReplaceChild(int aNewChild, int aOldChild, int[] aReturn) throws DOMException {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 14, getAddress(), aNewChild, aOldChild, aReturn);
    }
    
    
    public int RemoveChild(int aOldChild, int[] aReturn) throws DOMException {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 15, getAddress(), aOldChild, aReturn);
    }
    
    public int AppendChild(int aNewChild, int[] aReturn) throws DOMException {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 16, getAddress(), aNewChild, aReturn);
    }

    public int HasChildNodes(boolean[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 17, getAddress(), aReturn);
    }
    

    public int CloneNode(boolean deep, int[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 18, getAddress(), deep, aReturn);
    }
    
    public int Normalize() {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 19, getAddress());
    }
    
    public int IsSupported(int feature, int version, boolean[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 20, getAddress(), feature, version, aReturn);
    }
    
    public int GetNameSpaceURI(int[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 21, getAddress(), aReturn);
    }
    
    public int GetPrefix(int[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 22, getAddress(), aReturn);
    }
    
    public int SetPrefix(int aPrefix) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 23, getAddress(), aPrefix);
    }
    
    public int GetLocalName(int[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 24, getAddress(), aReturn);
    }
    
    public int HasAttributes(boolean[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 25, getAddress(), aReturn);
    }
}
