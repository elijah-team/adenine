/*
 * Created on Aug 7, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMHTMLDocument extends nsIDOMDocument {

	static final int LAST_METHOD_ID = nsIDOMDocument.LAST_METHOD_ID + 18;
	
	public static final String NS_IDOMHTMLDOCUMENT_IID_STRING =
		"a6cf9084-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_IDOMHTMLDOCUMENT_IID =
		new nsID(NS_IDOMHTMLDOCUMENT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMHTMLDocument(int address) {
		super(address);
	}
	
	public int GetTitle(int aTitle) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 1, getAddress(), aTitle);
	}
	
	public int SetTitle(int aTitle) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 2, getAddress(), aTitle);
	}
	
	public int GetReferrer(int aReferrer) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 3, getAddress(), aReferrer);
	}
	
	public int GetDomain(int aDomain) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 4, getAddress(), aDomain);
	}
	
	public int GetURL(int aURL) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 5, getAddress(), aURL);
	}
	
	public int GetBody(int[] aBody) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 6, getAddress(), aBody);
	}
	
	public int GetImages(int[] aImages) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 7, getAddress(), aImages);
	}
	
	public int GetApplets(int[] aApplets) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 8, getAddress(), aApplets);
	}
	
	public int GetLinks(int[] aLinks) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 9, getAddress(), aLinks);
	}
	
	public int GetForms(int[] aForms) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 10, getAddress(), aForms);
	}
	  
	public int GetAnchors(int[] aAnchors) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 11, getAddress(), aAnchors);
	}
	
	public int GetCookie(int[] aCookie) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 12, getAddress(), aCookie);
	}
	
	public int SetCookie(int[] aCookie) throws DOMException {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 13, getAddress(), aCookie);
	}
	                                             
	public int Open() {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 14, getAddress());
	}
	
	public int Close() {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 15, getAddress());
	}
	
	public int Write(int aText) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 16, getAddress(), aText);
	}
	
	public int WriteLn(int aText) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 17, getAddress(), aText);
	}
	
	public int GetElementsByName(int aElementName) {
		return XPCOM.VtblCall(nsIDOMDocument.LAST_METHOD_ID + 18, getAddress(), aElementName);
	}
}
