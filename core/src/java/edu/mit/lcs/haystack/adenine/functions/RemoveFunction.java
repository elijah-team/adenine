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

package edu.mit.lcs.haystack.adenine.functions;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.JavaMethodWrapper;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.HashSet;
import java.util.Set;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class RemoveFunction implements ICallable {
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		Resource subject, predicate;
		RDFNode object;
		
		if (parameters.length == 1) {
			Statement s = (Statement)parameters[0];
			subject = s.getSubject();
			predicate = s.getPredicate();
			object = s.getObject();
		} else {
			subject = (Resource)parameters[0];
			predicate = (Resource)parameters[1];
			object = (RDFNode)parameters[2];
		}
		
		// Get a list of the existentials
		Set s = new HashSet();
		if (subject.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
			s.add(subject);
		}
		if (predicate.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
			s.add(predicate);
		}
		if ((object instanceof Resource) && (object.getContent().indexOf(Constants.s_wildcard_namespace) == 0)) {
			s.add(object);
		}
		
		IRDFContainer target = (IRDFContainer) message.getNamedValue(AdenineConstants.target);
		
		if (target == null) {
			target = denv.getTarget();
		}
		
		return new JavaMethodWrapper(target, "remove").invoke(new Message(new Object[] {
			new Statement(subject, predicate, object), s
		}), denv);
	}

}
