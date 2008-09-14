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

package edu.mit.lcs.haystack.adenine.query;

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.Set;

/**
 * @author Dennis Quan
 */
public class RDFContainsConditionHandler implements IConditionHandler {

	/**
	 * @see edu.mit.lcs.haystack.adenine.query.IConditionHandler#resolveCondition(DynamicEnvironment,IQueryEngine, Condition, Resource[], RDFNode[][])
	 */
	public Set resolveCondition(
		DynamicEnvironment denv,
		IQueryEngine qe,
		Condition condition,
		Resource[] existentials,
		RDFNode[][] hints)
		throws AdenineException {
		Statement pattern2 = new Statement((Resource)condition.getParameter(0),
			(Resource)condition.getParameter(1),
			(RDFNode)condition.getParameter(2));

		Set s;
		try {
			try {
				s = denv.getSource().queryMulti(pattern2, existentials, hints);
			} catch (UnsupportedOperationException ue) {
				s = denv.getSource().query(pattern2, existentials);
			}
		} catch (RDFException rdfe) {
			throw new AdenineException("", rdfe);
		}
		return s;
	}

}

