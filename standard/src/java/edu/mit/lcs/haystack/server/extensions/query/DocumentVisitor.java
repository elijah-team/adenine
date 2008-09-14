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
 * Created on Jul 21, 2003
 */

package edu.mit.lcs.haystack.server.extensions.query;

import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Used for DocSet.visitDoc* 
 * Interface for adding literals for to a document visiting nodes
 * during a graph traversal
 * @author vineet
 */
abstract public class DocumentVisitor {
	abstract public IServiceAccessor getSA();
	abstract public IRDFContainer getRDFContainer();
	abstract public void enter(Resource field);
	abstract public void exit();

	// tokenize here
	abstract public void visitText(String field, String value);
	abstract public void visitContent(String string, ContentClient cc);

	public void visitLiteral(String field, Literal value) {
		visitText(field, value.getContent());
	}
	// would like to add some more specific default code here, but everyone should want to customize
	abstract public void visitResource(String field, Resource value);
	abstract public void visitReverseResource(String field, Resource value);

}