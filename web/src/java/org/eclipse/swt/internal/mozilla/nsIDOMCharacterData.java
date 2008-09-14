/*
 * Created on Aug 10, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMCharacterData extends nsIDOMNode {

	static final int LAST_METHOD_ID = nsIDOMNode.LAST_METHOD_ID + 8;

	public static final String NS_IDOMCHARACTERDATA_IID_STRING =
		"a6cf9072-15b3-11d2-932e-00805f8add32";

	public static final nsID NS_IDOMCHARACTERDATA_IID =
		new nsID(NS_IDOMCHARACTERDATA_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMCharacterData(int address) {
		super(address);
	}
	
	public int GetData(int aData) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 1, getAddress(), aData);
	}
	
	public int SetData(int aData) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 2, getAddress(), aData);
	}
	
	public int GetLength(int aLength) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 3, getAddress(), aLength);
	}

	public int SubstringData(int offset, int count, int aReturn) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 4, getAddress(), offset, count, aReturn);
	}
	
	public int AppendData(int aData) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 5, getAddress(), aData);
	}
	
	public int InsertData(int offset, int aData) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 6, getAddress(), offset, aData);
	}
	
	public int DeleteData(int offset, int count) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 7, getAddress(), offset, count);
	}
	
	public int ReplaceData(int offset, int count, int aData) throws DOMException {
		return XPCOM.VtblCall(nsIDOMNode.LAST_METHOD_ID + 8, getAddress(), offset, count, aData);
	}
	
}
