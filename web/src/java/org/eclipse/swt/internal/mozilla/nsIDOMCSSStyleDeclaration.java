/*
 * Created on Aug 12, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.eclipse.swt.internal.mozilla.nsISupports;
import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMCSSStyleDeclaration extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 10;

	public static final String NS_IDOMCSSSTYLEDECLARATION_IID_STRING =
		"a6cf90be-15b3-11d2-932e-00805f8add32";

	public static final nsID NS_IDOMCSSSTYLEDECLARATION_IID =
		new nsID(NS_IDOMCSSSTYLEDECLARATION_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMCSSStyleDeclaration(int address) {
		super(address);
	}
	
	public int GetCSSText(int[] aCSSText) { 
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aCSSText);
	}
	
	public int SetCSSText(int[] aCSSText) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aCSSText);
	}
	
	public int GetPropertyValue(int aPropertyName, int aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aPropertyName, aReturn);
	}
	
	public int GetPropertyCSSValue(int aPropertyName, int[] aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 4, getAddress(), aPropertyName, aReturn);
	}

	public int RemoveProperty(int aPropertyName, int aReturn) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 5, getAddress(), aPropertyName, aReturn);
	}
	
	public int GetPropertyPriority(int aPropertyName, int aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 6, getAddress(), aPropertyName, aReturn);
	}

	public int SetProperty(int aPropertyName, int aValue, int aPriority)  throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 7, getAddress(), aPropertyName, aValue, aPriority);
	}
	
	public int GetLength(int[] aLength) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 8, getAddress(), aLength);
	}
	
	public int Item(int aIndex, int aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 9, getAddress(), aIndex, aReturn);
	}
	
	public int GetParentRule(int[] aParentRule) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 10, getAddress(), aParentRule);
	}	
}
