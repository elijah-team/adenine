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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;




/**
 *  Represents a selection within a DOM, possibly of several sibling
 *  elements.
 */
public interface DOMSelection {

    /**
     *  Returns true if this selection is empty (contains no elements).
     */
    public boolean isEmpty();

    /**
     *  Returns true if this selection was generated from the active
     *  element, rather than from an actual selection.
     */
    public boolean isActiveElementSelection();

    /**
     *  Returns true if this selection covers a subset (of size
     *  greater than one) of the parent element's child nodes.
     */
    public boolean isPartialSelection();

    /**
     *  Retrieves the htmlText of the selection.
     */
    public String getHtmlText();

    /**
     *  Retrieves the parent element of the selection
     */
    public IDOMElement getParentElement();

    /**
     *  Retrieves the subset of children of the parent element
     *  contained by this selection.
     */
    public IDOMElement[] getSelectedElements();

    /**
     *  Highlights this selection in the document.  If all children of
     *  the parent are selected, just highlights the
     *  parent. Otherwise, highlights the children individually.
     */
    public void highlight(String highlightColor, String textColor);

    /**
     *  Generates a NodeID from this selection
     */
    public NodeID getNodeID() throws NodeIDException;

}
