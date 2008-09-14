/*
 * Created on Aug 10, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsIDOMComment extends nsIDOMCharacterData {

	static final int LAST_METHOD_ID = nsIDOMCharacterData.LAST_METHOD_ID;

	public static final String NS_IDOMCOMMENT_IID_STRING =
		"a6cf9073-15b3-11d2-932e-00805f8add32";

	public static final nsID NS_IDOMCOMMENT_IID =
		new nsID(NS_IDOMCOMMENT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMComment(int address) {
		super(address);
	}

}
