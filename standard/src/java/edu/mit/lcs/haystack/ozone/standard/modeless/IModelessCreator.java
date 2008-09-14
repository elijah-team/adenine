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

package edu.mit.lcs.haystack.ozone.standard.modeless;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.Resource;
import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public interface IModelessCreator {
	/*
	 * Creates and aligns modeless part to the border of the specified rectangle.
	 * If alignTopOrBottom, then the part will touch either the top or bottom border;
	 * otherwise, it will touch the left or right border.
	 * 
	 * If discardWhenExitBase is true and the mouse falls into any region in the
	 * parent modeless part but outside the base, then all modeless children are
	 * discarded.
	 */
	public ModelessPart createModelessPart(Resource resData, Rectangle rectBase, boolean alignTopOrBottom, boolean discardWhenOutsideBase, Context context) throws Exception;
	
	public ModelessPart createModelessPart(Resource resData, Point pointBase, Context context) throws Exception;
}
