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
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class QueryFunction implements ICallable {
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		
		// Get a list of the existentials
		Set s = new HashSet();
		Statement[] query = null;
		ConditionSet cs = null;
		try {
			Resource[] variables = null, existentials = null;
			if (parameters.length >= 2) {
				if (parameters[1] instanceof Collection) {
					variables = (Resource[])JavaMethodWrapper.convertToArray(Resource[].class, (Collection)parameters[1]);
				} else if (!parameters[1].getClass().isArray()) {
					throw new AdenineException("The second parameter to query must be a set of Resources");
				}
			}
			
			if (parameters.length >= 3) {
				if (parameters[2] instanceof Collection) {
					existentials = (Resource[])JavaMethodWrapper.convertToArray(Resource[].class, (Collection)parameters[2]);
				} else if (!parameters[2].getClass().isArray()) {
					throw new AdenineException("The third parameter to query must be a set of Resources");
				}
			}

			boolean isRDF = parameters[0] instanceof IRDFContainer;
			if (isRDF) {
				IRDFContainer rdfc = (IRDFContainer)parameters[0];
				query = new Statement[rdfc.size()];
				Statement[] query1 = new Statement[rdfc.size()];
				Statement[] query2 = new Statement[rdfc.size()];
				Statement[] query3 = new Statement[rdfc.size()];
				Iterator i = rdfc.iterator();
				int j1 = 0, j2 = 0, j3 = 0;
				while (i.hasNext()) {
					Statement st = (Statement)i.next();
					int cExistentials = 0;

					if (parameters.length < 3) {
						RDFNode n = st.getSubject();
						if (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
							s.add(n);
							++cExistentials;
						}
			
						n = st.getPredicate();
						if (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
							s.add(n);
							++cExistentials;
						}
			
						n = st.getObject();
						if ((n instanceof Resource) && (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0)) {
							s.add(n);
							++cExistentials;
						}
					} else {
						if (Utilities.containsResource(existentials, st.getSubject())) {
							++cExistentials;
						}
						if (Utilities.containsResource(existentials, st.getPredicate())) {
							++cExistentials;
						}
						if (Utilities.containsResource(existentials, st.getObject())) {
							++cExistentials;
						}
					}

					switch (cExistentials) {
					case 1:
						query1[j1++] = st;
						break;

					case 2:
						query2[j2++] = st;
						break;

					case 3:
						query3[j3++] = st;
						break;

					default:
						throw new AdenineException("Statements in query must have at least one existential:" + st);
					}
				}

				int j = 0;
				for (int i2 = 0; i2 < j1; i2++) {
					query[j++] = query1[i2];
				}
				for (int i2 = 0; i2 < j2; i2++) {
					query[j++] = query2[i2];
				}
				for (int i2 = 0; i2 < j3; i2++) {
					query[j++] = query3[i2];
				}
			} else {
				throw new AdenineException("The first parameter to query must be an RDF container.");
			}

			if (parameters.length < 2) {
				variables = new Resource[s.size()];
				s.toArray(variables);
			}
			
			if (parameters.length < 3) {
				existentials = new Resource[s.size()];
				s.toArray(existentials);
			}

			IRDFContainer source = (IRDFContainer) message.getNamedValue(AdenineConstants.source);
			
			if (source == null) {
				source = denv.getSource();
			}
			return new Message(source.query(query, variables, existentials));
		} catch (RDFException rdfe) {
			throw new AdenineException("RDF error", rdfe);
		}
	}

}
