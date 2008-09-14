/*
 * Created on Aug 12, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsIDOMElementCSSInlineStyle extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 1;

	public static final String NS_IDOMELEMENTCSSINLINESTYLE_IID_STRING =
		"99715845-95fc-4a56-aa53-214b65c26e22";

	public static final nsID NS_IDOMELEMENTCSSINLINESTYLE_IID =
		new nsID(NS_IDOMELEMENTCSSINLINESTYLE_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMElementCSSInlineStyle(int address) {
		super(address);
	}
	
	public int GetStyle(int[] aStyle) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aStyle);
	}

}
