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

import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AdenineConditionHandler implements IConditionHandler {
	public AdenineConditionHandler() {
	}
	
	/**
	 * @see edu.mit.lcs.haystack.adenine.query.IConditionHandler#resolveCondition(DynamicEnvironment,IQueryEngine, Condition, Resource[], RDFNode[][])
	 */
	public Set resolveCondition(
		DynamicEnvironment denv,
		IQueryEngine engine,
		Condition condition,
		Resource[] existentials,
		RDFNode[][] hints)
		throws AdenineException {
		Interpreter interpreter = new Interpreter(denv.getInstructionSource());
		HashMap namedParams = new HashMap();
		namedParams.put(AdenineConstants.existentials, Arrays.asList(existentials));
		namedParams.put(AdenineConstants.currentResults, hints);
		Message message = new Message(condition.getParameters(), namedParams);
		Set set = (Set)interpreter.callMethod(condition.getFunction(), message, denv).getPrimaryValue();
		
		// Convert items in the set from collections to arrays, if necessary
		HashSet set2 = new HashSet();
		Iterator i = set.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof Collection) {
				Collection c = (Collection)o;
				RDFNode[] datum = new RDFNode[c.size()];
				c.toArray(datum);
				set2.add(datum);
			} else {
				set2.add(o);
			}
		}
		
		return set2;
	}

}

