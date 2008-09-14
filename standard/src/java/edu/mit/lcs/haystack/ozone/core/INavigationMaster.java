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

import edu.mit.lcs.haystack.rdf.*;

/**
 * Interface exposed by objects that can navigate to different resources.
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public interface INavigationMaster {
	/**
	 * Requests a viewing of the given resource. The navigation
	 * master chooses the appropriate view navigator to perform
	 * the required navigation.
	 */
	public void requestViewing(Resource res);

	/**
	 * Requests a viewing of the given resource. The navigation
	 * master chooses the appropriate view navigator to perform
	 * the required navigation.
	 */
	public void requestViewing(Resource res, Resource viewInstance);

	/**
	 * Registers a view navigator with the given id and returns
	 * a cookie.
	 */
	public Object registerViewNavigator(Resource id, IViewNavigator vn);
	
	/**
	 * Unregisters a view navigator given its id.
	 */
	public void unregisterViewNavigator(Object cookie);
	
	/**
	 * Retrieves a view navigator given its id.
	 */
	public IViewNavigator getViewNavigator(Resource id);
}
