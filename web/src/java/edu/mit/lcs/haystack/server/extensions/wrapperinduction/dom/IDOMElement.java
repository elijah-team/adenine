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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom;


/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public interface IDOMElement extends INode {

    public String getTagName();

    /**
     *  Retrieves all text (i.e. non-markup) from within this element and its children
     */
    public String getNodeText();

    /**
     *  Returns all HTML for this node and its children
     */
    public String getOuterHTML();

    /**
     *  Returns all HTML for this node's children (not including this
     *  node)
     */
    public String getInnerHTML();

    /**
     *  Retrieves the text of html representing the start tag of this
     *  object.  If this is a TEXT_NODE, returns the text of this node.
     */
    public String startTagHTML();
    
    /**
     *  Retrieves the text of html representing the end tag of this
     *  object, or "" if this object does not have a closing tag
     *  (e.g. <BR>).  If this is a TEXT_NODE, returns the empty string
     *  ("").
     */
    public String endTagHTML();

    /**
     *  Highlights this element.
     */
    public IDOMElement highlight(String highlightColor, String textColor);
    /**
     *  Returns this element to its original, un-highlight()-ed style.
     */
    public void unhighlight();
    
    /**
     *  Returns the highlight color of the element
     */
    public String getHighlightedColor();

    /**
     *  Returns the value of the given attribute
     */
    public String getAttribute(String attributeName);

    /**
     * @return a copy of the IDOMElement
     */
    public INode copy();
    
}
