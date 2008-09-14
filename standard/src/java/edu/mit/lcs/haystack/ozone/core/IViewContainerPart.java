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

/**
 * The container interface with which views communicate.
 * @author David Huynh
 * @author Dennis Quan
 */
public interface IViewContainerPart extends IVisualPart {
	
	/**
	 * Navigates to the given resource. The given
	 * resource should specify how to instantiate
	 * an IViewPart.
	 */
	public void navigate(Resource res);
	
	/**
	 * Navigates to the given resource. The given
	 * resource should specify how to instantiate
	 * an IViewPart.
	 */
	public void navigate(Resource res, Resource resView);
	
	/**
	 * Refreshes the current resource.
	 */
	public void refresh();
	
	/**
	 * Returns the resource currently contained in
	 * this view container.
	 */
	public Resource getCurrentResource();
	
	/**
	 * Returns the view instance resource currently 
	 * used in this view container.
	 */
	public Resource getCurrentViewInstance();

	/**
	 * Returns the currently contained IViewPart.
	 */
	public IPart getCurrentViewPart();
	
	void onNavigateComplete(Resource resource, IPart childPart);
}
