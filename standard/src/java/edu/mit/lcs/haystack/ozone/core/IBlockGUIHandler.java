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

package edu.mit.lcs.haystack.ozone.core;

import org.eclipse.swt.graphics.*;


/**
 * @version 	1.0
 * @author		David Huynh
 */
public interface IBlockGUIHandler extends IGUIHandler {
	public static final int FIXED_SIZE = 0;
	public static final int WIDTH = 1;
	public static final int HEIGHT = 2;
	public static final int BOTH = 3;

	/**
	 * Returns the dimension(s) that the parent part must hint when it calls
	 * calculateSize(). Or returns FIXED_SIZE if the part has a fixed size
	 * in both dimensions and the parent part must call getFixedSize()
	 * instead.
	 */
	public int getHintedDimensions();
	
	/**
	 * Returns the text align mode.
	 */
	public int getTextAlign();
	
	/**
	 * Returns the desired size of the part given the hinted dimensions.
	 * When there is no hint, specify -1. May return null if size cannot
	 * be calculated given the hints.
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight);
	
	/**
	 * Returns the fixed size of the part, or null if the part is not
	 * fixed in size.
	 */
	public BlockScreenspace getFixedSize();
	
	/**
	 * Repositions the part.
	 */
	public void setBounds(Rectangle r);
	
	/**
	 * Draws the visual part. The GC is already clipped to r.
	 */
	public void draw(GC gc, Rectangle r);
	
	/**
	 * Renders the visual part in HTML.
	 * @author		Stephen Garland
	 */
	public void renderHTML(HTMLengine he);
		 
}

