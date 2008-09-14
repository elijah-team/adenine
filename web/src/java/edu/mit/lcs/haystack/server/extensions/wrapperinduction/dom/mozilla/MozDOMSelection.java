/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.mozilla;

import java.util.ArrayList;

import org.eclipse.swt.internal.mozilla.nsIDOMDocumentFragment;
import org.eclipse.swt.internal.mozilla.nsIDOMNode;
import org.eclipse.swt.internal.mozilla.nsIDOMRange;
import org.eclipse.swt.internal.mozilla.nsIDOMWindow;
import org.eclipse.swt.internal.mozilla.nsISelection;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.Range;

/**
 * @author Ryan Manuel
 * @version 1.0
 */
public class MozDOMSelection implements DOMSelection {

	protected nsISelection selection;
	protected MozDOMElement parentElement;
    protected MozDOMElement[] selectedElements;
    protected boolean isEmpty;
    protected String htmlText;
    protected boolean isActiveElementSelection;
    protected boolean isPartialSelection;
    protected MozDOMDocument document;
	
    public MozDOMSelection(MozDOMDocument doc, boolean unselect) {
    	int[] aDOMWindow = new int[1];
    	int[] aSelection = new int[1];
    	int[] aRange = new int[1];
    	int[] aNode = new int[1];
    	int[] aElement = new int[1];
    	this.document = doc;
    	int rc = this.document.getNSWebBrowser().GetContentDOMWindow(aDOMWindow);
		MozUtils.mozCheckReturn(rc, aDOMWindow[0]);
    	nsIDOMWindow nsIWindow = new nsIDOMWindow(aDOMWindow[0]);
    	rc = nsIWindow.GetSelection(aSelection);
    	MozUtils.mozCheckReturn(rc, aSelection[0]);
    	this.selection = new nsISelection(aSelection[0]);
    	rc = this.selection.GetRangeAt(0, aRange);
    	MozUtils.mozCheckReturn(rc, aRange[0]);
    	nsIDOMRange nsIRange = new nsIDOMRange(aRange[0]);
    	rc = nsIRange.GetCommonAncestorContainer(aNode);
		MozUtils.mozCheckReturn(rc, aNode[0]);
    	nsIDOMNode nsINode = new nsIDOMNode(aNode[0]);
    	this.parentElement = new MozDOMElement(this.document, nsINode);
    	NodeList childList = this.parentElement.getChildNodes();
    	int lengthChildren = childList.getLength();
		boolean foundStart = false;
		ArrayList selectedChildren = new ArrayList();
		this.isEmpty = true;
    	for(int i = 0; i < lengthChildren; i++) {
    		MozDOMElement currChild = (MozDOMElement) childList.item(i);
    		if(currChild.getNodeType() != Node.TEXT_NODE) {
    			boolean[] aContainsNode = new boolean[1];
    			rc = this.selection.ContainsNode(currChild.getDOMNode().getAddress(), true, aContainsNode);
    			MozUtils.mozCheckReturn(rc);
    			if(aContainsNode[0]) {
    				this.isEmpty = false;
					foundStart = true;
					selectedChildren.add(currChild);
					System.out.println("Added " + currChild);
    			}
    			else {
    				if(foundStart) {
    					break;
    				}
    			}
    		}
    		else {
    			if(foundStart) {
    				System.out.println("Added " + currChild);
					selectedChildren.add(currChild);
    			}
    			else {
    				// handle very small text-node selections
    			    System.out.println("considering " + currChild);
    			    if (!foundStart && selectedChildren.size() == 0 && 
    				this.getHtmlText().indexOf(currChild.toString()) >= 0) {
    				selectedChildren.add(currChild);
    				this.isEmpty = false;
    				System.out.println("Added text fragment (" + currChild.getSiblingNo() + "): " + currChild);
    			    }
    			}
    		}
    	}
		
		this.selectedElements = (MozDOMElement[])selectedChildren.toArray(new MozDOMElement[0]);
		
		// when there's only one element
		// selected, if we get this case, simply move the
		// selection up to the parent node 
		if (this.selectedElements.length == 0 && this.parentElement != null) {
		    System.out.println("Adding parent element (" + parentElement + ") instead");
		    this.selectedElements = new MozDOMElement[] {this.parentElement};
		    this.parentElement = (MozDOMElement)this.parentElement.getParentNode();
		    this.isEmpty = false;
		}
		
		System.out.println("selection length " + this.selectedElements.length + " parent children " + this.parentElement.getChildNodes().getLength());
		while (this.selectedElements.length > .6*this.parentElement.getChildNodes().getLength()) {
		    //		    System.out.print("Moving selection up from " + parentElement + " (" + parentElement.getNodeID() + ")");
		    this.selectedElements = new MozDOMElement[] {parentElement};
		    this.parentElement = (MozDOMElement)this.parentElement.getParentNode();
		    //		    System.out.println(" to " + parentElement + " (" + parentElement.getNodeID() + ")");
		}
		
		if(unselect)
			this.selection.CollapseToStart();
		
		nsIWindow.Release();
		nsIRange.Release();
    }

    protected void finalize() {
    	/*selection.Release();*/
    }
    
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#isEmpty()
	 */
	public boolean isEmpty() {
		return this.isEmpty;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#isActiveElementSelection()
	 */
	public boolean isActiveElementSelection() {
		return this.isActiveElementSelection;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#isPartialSelection()
	 */
	public boolean isPartialSelection() {
		return this.isPartialSelection;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#getHtmlText()
	 */
	public String getHtmlText() {
		if(this.htmlText == null) {
			int[] aRange = new int[1];
			int[] aFragment = new int[1];
			int[] aNode = new int[1];
			MozDOMElement elt = (MozDOMElement) this.document.createElement("SPAN");
			int rc = this.selection.GetRangeAt(0, aRange);
			MozUtils.mozCheckReturn(rc, aRange[0]);
			nsIDOMRange nsIRange = new nsIDOMRange(aRange[0]);
			rc = nsIRange.CloneContents(aFragment);
			MozUtils.mozCheckReturn(rc, aFragment[0]);
			nsIDOMDocumentFragment nsIDocFragment = new nsIDOMDocumentFragment(aFragment[0]);
			rc = nsIDocFragment.QueryInterface(nsIDOMNode.NS_IDOMNODE_IID, aNode);
			MozUtils.mozCheckReturn(rc, aNode[0]);
			nsIDOMNode nsINode = new nsIDOMNode(aNode[0]);
			MozDOMElement mozFrag = new MozDOMElement(this.document, nsINode);
			elt.appendChild(mozFrag);
			this.htmlText = elt.getInnerHTML();
			nsIRange.Release();
		}
		return this.htmlText;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#getParentElement()
	 */
	public IDOMElement getParentElement() {
		return this.parentElement;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#getSelectedElements()
	 */
	public IDOMElement[] getSelectedElements() {
		return this.selectedElements;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#highlight(java.lang.String, java.lang.String)
	 */
	public void highlight(String highlightColor, String textColor) {
		if (this.selectedElements.length == this.parentElement.getChildNodes().getLength()) {
		    this.parentElement.highlight(highlightColor, textColor);
		}
		else {
		    for (int i = 0; i < this.selectedElements.length; i++) {
		    	this.selectedElements[i].highlight(highlightColor, textColor);
		    }
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection#getNodeID()
	 */
	public NodeID getNodeID() throws NodeIDException {
		if (this.isEmpty) return null;

		NodeID id =
		    parentElement.getNodeID().makeChildNodeID(new Range(this.selectedElements[0].getSiblingNo(),
									this.selectedElements[this.selectedElements.length-1].getSiblingNo()));
		return id;
	}
	
	public String toString() {
		StringBuffer out = new StringBuffer();
		out.append(this.parentElement + "\n");
		for (int i = 0; i < this.selectedElements.length; i++) {
		    out.append("   " + this.selectedElements[i] + "\n");
		}
		return out.toString();
	}

}
