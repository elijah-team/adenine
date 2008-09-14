/*
 * Created on Aug 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class nsIDOMEventTarget extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 3;
	
	public static final String NS_IDOMEVENTTARGET_IID_STRING =
		"1c773b30-d1cf-11d2-bd95-00805f8ae3f4";
	
	public static final nsID NS_IDOMELEMENT_IID =
		new nsID(NS_IDOMEVENTTARGET_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMEventTarget(int address) {
		super(address);
	}

	public int AddEventListener(int aType, int aListener, boolean useCapture) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aType, aListener, useCapture);
	}
	
	public int RemoveEventListener(int aType, int aListener, boolean useCapture) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aType, aListener, useCapture);
	}
	
	public int DispatchEvent(int aEvt, boolean[] aReturn) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aEvt, aReturn);
	}
}
