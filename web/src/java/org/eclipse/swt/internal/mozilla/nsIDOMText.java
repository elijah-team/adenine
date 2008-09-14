/*
 * Created on Aug 10, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMText extends nsIDOMCharacterData {

	static final int LAST_METHOD_ID = nsIDOMCharacterData.LAST_METHOD_ID + 1;

	public static final String NS_IDOMTEXT_IID_STRING =
		"a6cf9082-15b3-11d2-932e-00805f8add32";

	public static final nsID NS_IDOMTEXT_IID =
		new nsID(NS_IDOMTEXT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMText(int address) {
		super(address);
	}

	public int SplitText(int offset, int aTextNode) throws DOMException {
		return XPCOM.VtblCall(nsIDOMCharacterData.LAST_METHOD_ID + 1, getAddress(), offset, aTextNode);
	}

}
