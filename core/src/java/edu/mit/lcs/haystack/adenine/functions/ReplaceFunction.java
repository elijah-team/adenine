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

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.instructions.ModelInstruction;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ReplaceFunction implements ICallable {

	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		if (parameters.length != 4) {
			throw new SyntaxException("replace requires four parameters");
		}
		
		Resource subject = (Resource)parameters[0];
		Resource predicate = (Resource)parameters[1];
		RDFNode object;
		RDFNode newValue;
		
		if (parameters[2] instanceof RDFNode) {
			object = (RDFNode)parameters[2];
		} else if (parameters[2] instanceof String) {
			object = new Literal((String)parameters[2]);
		} else {
			object = new Literal(parameters[2].toString());
		}
		
		boolean anonSubject = subject.getURI().indexOf(Constants.s_wildcard_namespace) == 0;
		boolean anonPredicate = predicate.getURI().indexOf(Constants.s_wildcard_namespace) == 0;
		boolean anonObject = (object instanceof Resource) && (object.getContent().indexOf(Constants.s_wildcard_namespace) == 0);

		if (((anonSubject ? 1 : 0) + (anonPredicate ? 1 : 0) + (anonObject ? 1 : 0)) != 1) {
			throw new SyntaxException("replace requires exactly one wildcard parameter");
		}
		
		try {
			IRDFContainer target = (IRDFContainer) message.getNamedValue(AdenineConstants.target);
			
			if (target == null) {
				target = denv.getTarget();
			}

			newValue = ModelInstruction.extractNode(parameters[3], target);
			target.replace(anonSubject ? null : subject, anonPredicate ? null : predicate, anonObject ? null : object, newValue);
/*			
			Set results = env.getSource().query(new Statement(subject, predicate, object), new Resource[] {
				anonSubject ? subject : (anonPredicate ? predicate : (Resource)object) } );
			RDFNode[] a = (RDFNode[])results.iterator().next();
			return a[0];*/
		} catch (NoSuchElementException nsee) { return new Message(); }
		catch (Exception e) { HaystackException.uncaught(e); }
		
		return new Message();
	}

}
