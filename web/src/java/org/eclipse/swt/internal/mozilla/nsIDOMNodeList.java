/*
 * Created on Aug 6, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsIDOMNodeList extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 2;
	
	public static final String NS_IDOMNODELIST_IID_STRING =
		"a6cf9078-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_IDOMNODELIST_IID =
		new nsID(NS_IDOMNODELIST_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMNodeList(int address) {
		super(address);
	}

	public int Item(int index, int[] aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), index, aReturn);
	}
	
	public int GetLength(int[] aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aReturn);
	}
}
