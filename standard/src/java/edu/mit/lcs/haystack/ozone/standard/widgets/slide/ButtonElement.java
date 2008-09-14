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

package edu.mit.lcs.haystack.ozone.standard.widgets.slide;


/**
 * @version 	1.0
 * @author		David Huynh
 */
public class ButtonElement extends HighlightableBlockElement {
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.widgets.slide.BlockElement#getMarginProperties()
	 */
	protected void getMarginProperties() {
		m_marginLeft = -1;
		m_marginRight = -1;
		m_marginTop = -1;
		m_marginBottom = -1;
		
		super.getMarginProperties();
		
		int fontSize = SlideUtilities.getAmbientFontSize(m_context);
		int marginX = fontSize / 2;
		int marginY = fontSize / 3;
		
		if (m_marginLeft == -1) {
			m_marginLeft = marginX;
		}
		if (m_marginRight == -1) {
			m_marginRight = marginX;
		}
		if (m_marginTop == -1) {
			m_marginTop = marginY;
		}
		if (m_marginBottom == -1) {
			m_marginBottom = marginY;
		}
	}
}
