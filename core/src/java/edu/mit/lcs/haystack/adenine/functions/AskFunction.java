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
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.JavaMethodWrapper;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.adenine.query.Condition;
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.adenine.query.IQueryEngine;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AskFunction implements ICallable {
	public static void findWildcardsInConditionSet(ConditionSet cs, Set s) {
		Iterator i = cs.iterator();
		while (i.hasNext()) {
			Condition c = (Condition)i.next();
			Iterator j = c.getParameterIterator();
			while (j.hasNext()) {
				Object o = j.next();
				if (o instanceof Resource) {
					Resource n = (Resource)o;
					if (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
						s.add(n);
					}
				} else if (o instanceof ConditionSet) {
					findWildcardsInConditionSet((ConditionSet)o, s);
				}
			}
		}
	}
	
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		
		// Get a list of the existentials
		Set s = new HashSet();
		Statement[] query = null;
		ConditionSet cs = null;

		if (parameters[0] instanceof ConditionSet) {
			cs = (ConditionSet)parameters[0];
			if (parameters.length < 3) {
				findWildcardsInConditionSet(cs, s);
			}
		} else if (parameters[0] instanceof Resource) {
			cs = (ConditionSet)DeserializeFunction.deserialize((Resource)parameters[0], denv.getSource());
			if (parameters.length < 3) {
				findWildcardsInConditionSet(cs, s);
			}
		} else {
			throw new AdenineException("The first parameter to ask must be an RDF container or Adenine query specification: " + parameters[0]);
		}

		Resource[] variables = null, existentials = null;
		if (parameters.length >= 2) {
			if (parameters[1] instanceof Collection) {
				variables = (Resource[])JavaMethodWrapper.convertToArray(Resource[].class, (Collection)parameters[1]);
			} else if (!parameters[1].getClass().isArray()) {
				throw new AdenineException("The second parameter to ask must be a set of Resources");
			}
		} else {
			variables = new Resource[s.size()];
			s.toArray(variables);
		}
		
		if (parameters.length >= 3) {
			if (parameters[2] instanceof Collection) {
				existentials = (Resource[])JavaMethodWrapper.convertToArray(Resource[].class, (Collection)parameters[2]);
			} else if (!parameters[2].getClass().isArray()) {
				throw new AdenineException("The third parameter to ask must be a set of Resources");
			}
		} else {
			existentials = new Resource[s.size()];
			s.toArray(existentials);
		}

		IQueryEngine qe = denv.getQueryEngine();
		return new Message(qe.query(denv, cs, true, variables, existentials));
	}

}
