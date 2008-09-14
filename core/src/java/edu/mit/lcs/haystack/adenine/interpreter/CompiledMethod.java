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

package edu.mit.lcs.haystack.adenine.interpreter;

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
abstract public class CompiledMethod {
	public Interpreter 		__interpreter__;
	public DynamicEnvironment 	__dynamicenvironment__;
	public Resource			__methodresource__;
	
	abstract protected Message doInvoke() throws AdenineException, RDFException;
	abstract protected void initializeParameters(Message msg);
	
	public Message invoke(Interpreter interpreter, DynamicEnvironment denv,
		Message message, Resource methodResource) throws AdenineException {
		__interpreter__ = interpreter;
		__dynamicenvironment__ = denv;
		__methodresource__ = methodResource;
		Object o = __dynamicenvironment__.getMessageIfAny();
		__dynamicenvironment__.setMessage(message);
		initializeParameters(message);
		try {
			return doInvoke();
		} catch (RDFException rdfe) {
			throw new AdenineException("RDF error", rdfe);
		} finally {
			__dynamicenvironment__.setMessageIfAny((Message)o);
		}
	}
}
