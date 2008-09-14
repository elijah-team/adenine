/*
 * Created on Aug 8, 2004
 */
package org.eclipse.swt.internal.mozilla;

/**
 * @author Ryan Manuel
 */
public class nsISelection extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 19;
	
	public static final String NS_ISELECTION_IID_STRING =
		"a6cf9078-15b3-11d2-932e-00805f8add32";
	
	public static final nsID NS_ISELECTION_IID =
		new nsID(NS_ISELECTION_IID_STRING);
	
	/**
	 * @param address
	 */
	public nsISelection(int address) {
		super(address);
	}

	public int GetAnchorNode(int[] aNode) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 1, getAddress(), aNode);
	}
	
	public int GetAnchorOffset(int[] aOffset) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 2, getAddress(), aOffset);
	}
	
	public int GetFocusNode(int[] aNode) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 3, getAddress(), aNode);
	}
	
	public int GetFocusOffset(int[] aFocusOffset) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 4, getAddress(), aFocusOffset);
	}
	
	public int GetIsCollapsed(boolean[] aIsCollapsed) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 5, getAddress(), aIsCollapsed);
	}
	
	public int GetRangeCount(int[] aRangeCount) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 6, getAddress(), aRangeCount);
	}

	public int GetRangeAt(int index, int[] aRange) {
		return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 7, getAddress(), index, aRange);
	}
	
    public int Collapse(int aParentNode, int offset) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 8, getAddress(), aParentNode, offset);
    }

    public int Extend(int aParentNode, int offset) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 9, getAddress(), aParentNode, offset);
    }
    
    public int CollapseToStart() {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 10, getAddress());
    }

    public int CollapseToEnd() {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 11, getAddress());
    }
    
    public int ContainsNode(int aNode, boolean aEntirelyContained, boolean[] aReturn) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 12, getAddress(), aNode, aEntirelyContained, aReturn);
    }

    public int SelectAllChildren(int aNode) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 13, getAddress(), aNode);
    }
    
    public int AddRange(int aRange) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 14, getAddress(), aRange);
    }
    
    public int RemoveRange(int aRange) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 15, getAddress(), aRange);
    }
    
    public int RemoveAllRanges() {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 16, getAddress());
    }

    public int DeleteFromDocument() {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 17, getAddress());
    }
    
    public int SelectionLanguageChange(boolean langRTL) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 18, getAddress(), langRTL);
    }

    public int ToString(int[] aString) {
    	return XPCOM.VtblCall(nsISupports.LAST_METHOD_ID + 19, getAddress(), aString);
    }
	
}
