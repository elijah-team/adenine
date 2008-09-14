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
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class QuerySizeFunction implements ICallable {
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		// Get a list of the existentials
		Set s = new HashSet();
		IRDFContainer rdfc = (IRDFContainer)parameters[0];
		try {
			Statement[] query = new Statement[rdfc.size()];
			Iterator i = rdfc.iterator();
			int j = 0;
			while (i.hasNext()) {
				Statement st = (Statement)i.next();
				RDFNode n = st.getSubject();
				if (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
					s.add(n);
				}
	
				n = st.getPredicate();
				if (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0) {
					s.add(n);
				}
	
				n = st.getObject();
				if ((n instanceof Resource) && (n.getContent().indexOf(Constants.s_wildcard_namespace) == 0)) {
					s.add(n);
				}
				query[j++] = st;
			}
			
			if (parameters.length == 2) {
				return new JavaMethodWrapper(denv.getSource(), "querySize").invoke(new Message(new Object[] {
					query, parameters[1], s}), denv);
			} else if (parameters.length == 1) {
				return new JavaMethodWrapper(denv.getSource(), "querySize").invoke(new Message(new Object[] {
					query, s, s}), denv);
			} else {
				return new JavaMethodWrapper(denv.getSource(), "querySize").invoke(new Message(new Object[] {
					query, parameters[1], parameters[2] }), denv);
			}
		} catch (RDFException rdfe) {
			throw new AdenineException("Unknown error", rdfe);
		}
	}

}
