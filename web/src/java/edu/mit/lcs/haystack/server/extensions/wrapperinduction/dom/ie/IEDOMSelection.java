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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import java.util.ArrayList;

import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DOMSelection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.Range;

/**
 *  Represents a selection within a DOM, possibly of several sibling
 *  elements.
 */
public class IEDOMSelection implements DOMSelection {

    protected boolean isEmpty;
    protected boolean isActiveElementSelection;
    protected boolean isPartialSelection;
    protected String htmlText;
    protected IEDOMElement parentElement;
    protected IEDOMElement[] selectedElements;

    /**
     *  Creates a new DOMSelection from the given document.  If
     *  'unselect' is true, also de-selects the current selection.
     */
    public IEDOMSelection(IEDOMDocument doc, boolean unselect) {
	this.isEmpty = true;
	this.isActiveElementSelection = false;
	this.isPartialSelection = false;

	OleAutomation selection = OLEUtils.getPropertyInterface(doc.getDocument2(),
								"selection",
								OLEUtils.UUID_IHTML_SELECTION_OBJECT);
	String selectionType = OLEUtils.getProperty(selection,
						    "type").getString();
	if (selectionType.equalsIgnoreCase("none")) {
	    this.selectedElements = new IEDOMElement[] {(IEDOMElement)doc.getActiveElement()};
	    this.parentElement = (IEDOMElement)this.selectedElements[0].getAncestor(1);
	    this.htmlText = this.selectedElements[0].getOuterHTML();
	    this.isEmpty = false;
	    this.isPartialSelection = true;
	}
	else {
	    try {
		OleAutomation range = OLEUtils.invokeCommand(selection,
							     "createRange",
							     new Variant[0]).getAutomation();
		OleAutomation txtRange = OLEUtils.getInterface(range,
							       OLEUtils.UUID_IHTML_TXT_RANGE);
		this.htmlText = OLEUtils.getProperty(txtRange, "htmlText").getString();
		System.out.println("htmlText:\n" + this.htmlText);

		this.parentElement =
		    new IEDOMElement(doc,
				     OLEUtils.getInterface(OLEUtils.invokeCommand(txtRange,
										  "parentElement",
										  new Variant[0]).getAutomation(),
							   OLEUtils.UUID_IHTML_DOM_NODE));
		OleAutomation otherRange = OLEUtils.invokeCommand(txtRange,
								  "duplicate",
								  new Variant[0]).getAutomation();

		// find child nodes
		String fixedHtmlText = WrapperManager.fixHTMLString(this.htmlText);
		NodeList children = parentElement.getChildNodes();
		ArrayList selectedChildren = new ArrayList();
		boolean foundStart = false;
		for (int i = 0; i < children.getLength(); i++) {
		    IEDOMElement currChild = (IEDOMElement)children.item(i);

		    if (currChild.getNodeType() != Node.TEXT_NODE) {
			try {
			    Variant currChildVar = new Variant(currChild.getElement());
			    OLEUtils.invokeCommand(otherRange,
						   "moveToElementText",
						   new Variant[] {currChildVar});
			    Variant otherRangeVar = new Variant(otherRange);
			    if (OLEUtils.invokeCommand(txtRange,
						       "inRange",
						       new Variant[] {otherRangeVar}).getBoolean()) {
				this.isEmpty = false;
				foundStart = true;
				selectedChildren.add(currChild);
				System.out.println("Added " + currChild);
			    }
			    else {
				try { 
				    String currChildTextBroken = currChild.getOuterHTML();
				    String currChildText = WrapperManager.fixHTMLString(currChildTextBroken);
				    System.out.println("currChildText: " + currChildText);
				    if (currChildText.length() > 10 && // don't do this for short nodes
					fixedHtmlText.indexOf(currChildText) >= 0) {
					this.isEmpty = false;
					foundStart = true;
					selectedChildren.add(currChild);
					System.out.println("Added " + currChild + " by substring");
				    }
				    else {
					if (foundStart) break;
				    }
				}
				catch (NullPointerException e) {
				    if (foundStart) selectedChildren.add(currChild);
				}
			    }
			}
			catch (NullPointerException e) {
			    // weird errors in oleutils... seems to die on FONT elements???
			    // can't apply moveToElementText method to FONT elements???
			    if (foundStart) {
				selectedChildren.add(currChild);
//  				System.out.println("Exception: ");
//  				e.printStackTrace();
 				System.out.println("Added " + currChild + " with exception");
			    }    
			}
		    }
		    else {
			if (foundStart) {
			    System.out.println("Added " + currChild);
			    selectedChildren.add(currChild);
			}
			else {
			    // handle very small text-node selections
			    System.out.println("considering " + currChild);
			    if (!foundStart && selectedChildren.size() == 0 && 
				this.htmlText.indexOf(currChild.toString()) >= 0) {
				selectedChildren.add(currChild);
				this.isEmpty = false;
				System.out.println("Added text fragment (" + currChild.getSiblingNo() + "): " + currChild);
			    }
			}
		    }
		}

		this.selectedElements = (IEDOMElement[])selectedChildren.toArray(new IEDOMElement[0]);

		// IE inRange() is weird when there's only one element
		// selected, if we get this case, simply move the
		// selection up to the parent node 
		if (this.selectedElements.length == 0 && this.parentElement != null) {
		    System.out.println("Adding parent element (" + parentElement + ") instead");
		    this.selectedElements = new IEDOMElement[] {this.parentElement};
		    this.parentElement = (IEDOMElement)this.parentElement.getParentNode();
		    this.isEmpty = false;
		}

		// move the selection up the tree to the highest
		// element which only encompasses the selected
		// elements.  If the selection is only a subset of
		// child nodes of the parent, we don't move up at all.
		// If the selection is all of the child nodes of the
		// parent, we move it up until it isn't all of the
		// child nodes.
		System.out.println("selection length " + this.selectedElements.length + " parent children " + this.parentElement.getChildNodes().getLength());
		while (this.selectedElements.length > .6*this.parentElement.getChildNodes().getLength()) {
		    //		    System.out.print("Moving selection up from " + parentElement + " (" + parentElement.getNodeID() + ")");
		    this.selectedElements = new IEDOMElement[] {parentElement};
		    this.parentElement = (IEDOMElement)this.parentElement.getParentNode();
		    //		    System.out.println(" to " + parentElement + " (" + parentElement.getNodeID() + ")");
		}
		
// 		for (int i = 0; i < this.selectedElements.length; i++) 
// 		    System.out.println(">>> " + selectedElements[i]);

		/*if (unselect) {
		    Variant varTrue = new Variant(true);
		    OLEUtils.invokeCommand(txtRange,
					   "collapse",
					   new Variant[] {varTrue});
		    varTrue.dispose();
		    OLEUtils.invokeCommand(txtRange,
					   "select",
					   new Variant[0]);
		}*/
		if(unselect)
			clearSelection(txtRange);

		otherRange.dispose();
		txtRange.dispose();
		range.dispose();

		this.isPartialSelection = (this.selectedElements.length > 1 &&
					   this.selectedElements.length < this.parentElement.getChildNodes().getLength());
		this.isActiveElementSelection = false;
	    }
	    catch (Throwable e) {
		System.out.println("Error creating IEDOMSelection:\n");
		e.printStackTrace();
		this.isEmpty = true;
		this.isActiveElementSelection = false;
		this.isPartialSelection = false;
	    }
	}

	selection.dispose();
    }

    /**
     *  Returns true if this selection is empty (i.e. contains no elements)
     */
    public boolean isEmpty() {
	return this.isEmpty;
    }

    public boolean isActiveElementSelection() {
	return this.isActiveElementSelection;
    }

    public boolean isPartialSelection() {
	return this.isPartialSelection;
    }

    /**
     *  Retrieves the htmlText of the selection.
     */
    public String getHtmlText() {
	return this.htmlText;
    }

    /**
     *  Retrieves the parent element of the selection
     */
    public IDOMElement getParentElement() {
	return this.parentElement;
    }

    /**
     *  Retrieves the subset of children of the parent element
     *  contained by this selection.
     */
    public IDOMElement[] getSelectedElements() {
	return this.selectedElements;
    }

    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append(this.parentElement + "\n");
	for (int i = 0; i < this.selectedElements.length; i++) {
	    out.append("   " + this.selectedElements[i] + "\n");
	}
	return out.toString();
    }

    /**
     *  Highlights this selection in the document.  If all children of
     *  the parent are selected, just highlights the
     *  parent. Otherwise, highlights the children individually.
     */
    public void highlight(String highlightColor, String textColor) {
	//	System.out.println("selectedElements="+selectedElements.length+", parent=" + parentElement.getChildNodes().getLength());
	if (this.selectedElements.length == this.parentElement.getChildNodes().getLength()) {
	    this.parentElement.highlight(highlightColor, textColor);
	}
	else {
	    for (int i = 0; i < this.selectedElements.length; i++) {
		//		System.out.println(this.selectedElements[i].startTagHTML());
		this.selectedElements[i].highlight(highlightColor, textColor);
	    }
	}
    }

    /**
     *  Generates a NodeID from this selection
     */
    public NodeID getNodeID() throws NodeIDException {
	if (this.isEmpty) return null;

	NodeID id =
	    parentElement.getNodeID().makeChildNodeID(new Range(this.selectedElements[0].getSiblingNo(),
								this.selectedElements[this.selectedElements.length-1].getSiblingNo()));

	return id;
    }

    /**
     * Clears the text selection
     */
    public void clearSelection(OleAutomation txtRange) {
    	Variant varTrue = new Variant(true);
    	OLEUtils.invokeCommand(txtRange,
    			"collapse",
				new Variant[] {varTrue});
    	varTrue.dispose();
    	OLEUtils.invokeCommand(txtRange,
    			"select",
				new Variant[0]);
    }  
}
