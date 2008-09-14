/*
 * Created on Aug 15, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsIDOMDocumentFragment extends nsIDOMNode {

	static final int LAST_METHOD_ID = nsIDOMNode.LAST_METHOD_ID;
	
	public static final String NS_IDOMDOCUMENTFRAGMENT_IID_STRING =
		"a6cf9076-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_IDOMDOCUMENT_IID_STRING =
		new nsID(NS_IDOMDOCUMENTFRAGMENT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMDocumentFragment(int address) {
		super(address);
	}

}
