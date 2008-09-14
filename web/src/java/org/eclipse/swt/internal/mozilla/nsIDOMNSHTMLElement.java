/*
 * Created on Aug 12, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsIDOMNSHTMLElement extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 19;

	public static final String NS_IDOMNSHTMLELEMENT_IID_STRING =
		"da83b2ec-8264-4410-8496-ada3acd2ae42";

	public static final nsID NS_IDOMNSHTMLELEMENT_IID =
		new nsID(NS_IDOMNSHTMLELEMENT_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMNSHTMLElement(int address) {
		super(address);
	}

	public int GetOffsetTop(int[] aOffsetTop) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aOffsetTop);
	}
	
	public int GetOffsetLeft(int[] aOffsetLeft) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aOffsetLeft);
	}
	
	public int GetOffsetWidth(int[] aOffsetWidth) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aOffsetWidth);
	}
	
	public int GetOffsetHeight(int[] aOffsetHeight) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 4, getAddress(), aOffsetHeight);
	}
	
	public int GetOffsetParent(int[] aOffsetParent) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 5, getAddress(), aOffsetParent);
	}
	
	public int GetInnerHTML(int aInnerHTML) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 6, getAddress(), aInnerHTML);
	}
	
	public int GetScrollTop(int[] aScrollTop) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 7, getAddress(), aScrollTop);
	}
	
	public int SetScrollTop(int scrollTop) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 8, getAddress(), scrollTop);
	}
	
	public int GetScrollLeft(int[] aScrollLeft) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 9, getAddress(), aScrollLeft);
	}
	
	public int SetScrollLeft(int scrollLeft) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 10, getAddress(), scrollLeft);
	}
	
	public int GetScrollHeight(int[] aScrollHeight) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 11, getAddress(), aScrollHeight);
	}
	
	public int GetScrollWidth(int[] aScrollWidth) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 12, getAddress(), aScrollWidth);
	}

	public int GetClientHeight(int[] aClientHeight) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 13, getAddress(), aClientHeight);
	}
	
	public int GetClientWidth(int[] aClientWidth) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 14, getAddress(), aClientWidth);
	}

	public int GetTabIndex(int[] aTabIndex) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 15, getAddress(), aTabIndex);
	}
	
	public int SetTabIndex(int tabIndex) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 16, getAddress(), tabIndex);
	}

	public int Blur() {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 17, getAddress());
	}
	
	public int Focus() {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 18, getAddress());
	}
	
	public int ScrollIntoView(boolean top) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 19, getAddress(), top);
	}
}
