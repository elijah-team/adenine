/*
 * Created on Aug 10, 2004
 */
package org.eclipse.swt.internal.mozilla;

import org.w3c.dom.DOMException;

/**
 * @author Ryan Manuel
 */
public class nsIDOMRange extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 24;

	public static final String NS_IDOMRANGE_IID_STRING =
		"a6cf90ce-15b3-11d2-932e-00805f8add32";

	public static final nsID NS_IDOMRANGE_IID =
		new nsID(NS_IDOMRANGE_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsIDOMRange(int address) {
		super(address);
	}

	public int GetStartContainer(int[] aContainer) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aContainer);
	}
	
	public int GetStartOffset(int[] aOffset) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aOffset);
	}
	
	public int GetEndContainer(int[] aContainer) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aContainer);
	}
	
	public int GetEndOffset(int[] aOffset) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 4, getAddress(), aOffset);
	}

	public int GetCollapsed(boolean[] aCollapsed) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 5, getAddress(), aCollapsed);
	}
	
	public int GetCommonAncestorContainer(int[] aContainer) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 6, getAddress(), aContainer);
	}
	
	public int SetStart(int aRefNode, int offset) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 7, getAddress(), aRefNode, offset);
	}

	public int SetEnd(int aRefNode, int offset) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 8, getAddress(), aRefNode, offset);
	}
	
	public int SetStartBefore(int aRefNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 9, getAddress(), aRefNode);
	}
	
	public int SetStartAfter(int aRefNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 10, getAddress(), aRefNode);
	}
	
	public int SetEndBefore(int aRefNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 11, getAddress(), aRefNode);
	}
	
	public int SetEndAfter(int aRefNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 12, getAddress(), aRefNode);
	}
	
	public int Collapse(boolean toStart) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 13, getAddress(), toStart);
	}
	
	public int SelectNode(int aRefNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 14, getAddress(), aRefNode);
	}
	
	public int SelectNodeContents(int aRefNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 15, getAddress(), aRefNode);
	}
	
	public final static short START_TO_START = 0;
	public final static short START_TO_END = 1;
	public final static short END_TO_END = 2;
	public final static short END_TO_START = 3;

	public int CompareBoundaryPoints(short how, int aSourceRange, short[] aReturn) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 16, getAddress(), how, aSourceRange, aReturn);
	}

	public int DeleteContents() throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 17, getAddress());
	}
	
	public int ExtractContents(int[] aFragment) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 18, getAddress(), aFragment);
	}

	public int CloneContents(int[] aFragment) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 19, getAddress(), aFragment);
	}
	
	public int InsertNode(int aNewNode) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 20, getAddress(), aNewNode);
	}
	
	public int SurroundContents(int aNewParent) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 21, getAddress(), aNewParent);
	}

	public int CloneRange(int[] aRange) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 22, getAddress(), aRange);
	}
	
	public int ToString(int aString) throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 23, getAddress(), aString);
	}
	
	public int Detatch() throws DOMException {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 24, getAddress());
	}	
}
