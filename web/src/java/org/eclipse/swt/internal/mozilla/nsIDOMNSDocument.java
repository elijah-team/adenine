/*
 * Created on Aug 8, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsIDOMNSDocument extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 11;

	public static final String NS_IDOMNSDOCUMENT_IID_STRING =
		"a6cf90cd-15b3-11d2-932e-00805f8add32";

	public static final nsID NS_IDOMNSDOCUMENT_IID =
		new nsID(NS_IDOMNSDOCUMENT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMNSDocument(int address) {
		super(address);
	}

	public int GetCharacterSet(int aCharacterSet) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aCharacterSet);
	}


	public int GetDir(int aDir) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aDir);
	}
	
	public int SetDir(int aDir) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aDir);
	}
	
	public int GetLocation(int[] aLocation) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 4, getAddress(), aLocation);
	}

	public int GetTitle(int aTitle) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 5, getAddress(), aTitle);
	}
	
	public int SetTitle(int[] aTitle) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 6, getAddress(), aTitle);
	}
	
	public int GetContentType(int aContentType) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 7, getAddress(), aContentType);
	}
	
	public int GetLastModified(int aLastModified) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 8, getAddress(), aLastModified);
	}

	public int GetReferrer(int aReferrer) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 9, getAddress(), aReferrer);
	}
	
	public int GetBoxObjectFor(int aElt, int[] aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 10, getAddress(), aElt, aReturn);
	}

	public int SetBoxObjectFor(int aElt, int aBoxObject) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 11, getAddress(), aElt, aBoxObject);
	}
}
