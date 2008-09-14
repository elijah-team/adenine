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

import edu.mit.lcs.haystack.rdf.Resource;
import java.util.EventObject;

/**
 * Base interface for all visual parts that use SWT or the web browser interface.
 * 
 * @version 	2.0
 * @author		Dennis Quan
 * @author		David Huynh
 * @author		Stephen Garland
 */
public interface IVisualPart extends IPart {
	
	/**
	 * Returns the gui handler that is an instance of the specified class.
	 */
	public IGUIHandler getGUIHandler(Class cls);
	
	/*
	 * Returns this IVisualPart if it responds to mouse clicks.  Otherwise
	 * returns the nearest ancestor of this IVisualPart that responds to
	 * mouse clicks.  Returns null if there is no such ancestor.
	 * 
	 * @see edu.mit.lcs.haystack.server.http.OzoneServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	public IVisualPart getClickHandler();
	
	/**
	 * Returns true if an action was taken in response to the event.
	 */
	public boolean handleGUIEvent(Resource res, EventObject e);
}
