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

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @version 	1.0
 * @author
 */
public class ModelessConstants {
	final public static String NAMESPACE = "http://haystack.lcs.mit.edu/schemata/ozonemodeless#";
	
	final public static Resource MODELESS_PARENT = new Resource(NAMESPACE + "modelessParent");
	final public static Resource MODELESS_CREATOR = new Resource(NAMESPACE + "modelessCreator");
	final public static Resource CHILD = new Resource(NAMESPACE + "child");
	
	final public static Resource BASE_RECT = new Resource(NAMESPACE + "baseRect");
	final public static Resource BASE_RECT_ALIGN = new Resource(NAMESPACE + "baseRectAlign");
	final public static Resource BASE_POINT = new Resource(NAMESPACE + "basePoint");
}
