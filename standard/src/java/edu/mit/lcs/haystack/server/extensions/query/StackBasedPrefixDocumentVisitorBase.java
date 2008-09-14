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

/*
 * Created on Jun 23, 2003
 */
package edu.mit.lcs.haystack.server.extensions.query;

import java.util.Iterator;
import java.util.Stack;

import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.server.extensions.navigation.NavigationConstants;

/**
 * Abstract class that builds a stack of the fields that has been visited by a document visitor
 * implements a "getFieldPrefix" which returns a concatenation of the fields in the node-visited stack
 * 
 * @author Vineet Sinha
 * @author yks
 */
abstract public class StackBasedPrefixDocumentVisitorBase extends DocumentVisitor {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(StackBasedPrefixDocumentVisitorBase.class);

	Stack fieldPrefixStack = new Stack();
	public void enter(Resource field) {
		fieldPrefixStack.push(field.toString() + ".");
		if (fieldPrefixStack.size()>50) {
			s_logger.warn("Too much nesting: " + getFieldPrefix(), new Exception());
			throw new OutOfMemoryError();
		}
	}
	public void exit() {
		fieldPrefixStack.pop();
	}
	protected String getFieldPrefix() {
		StringBuffer sb = new StringBuffer();
		Iterator fieldPrefixStackIt = fieldPrefixStack.iterator();
		while (fieldPrefixStackIt.hasNext()) {
			sb.append(fieldPrefixStackIt.next());
		}
		return sb.toString();
	}
	
	// added to fieldPrefix to indicate that the next resource is a back-link
	protected String getReverseExt() {
		return NavigationConstants.s_nav_reverseNextPred.toString() + ".";
	}
}
