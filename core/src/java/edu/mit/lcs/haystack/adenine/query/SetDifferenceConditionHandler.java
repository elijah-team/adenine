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
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Moved from adenine to here, to improve performance
 * @version 	1.0
 * @author		Vineet Sinha
 */
public class SetDifferenceConditionHandler implements IConditionHandler {

	/*
	# the adenine implementation 
	method :setDifference a b c adenine:existentials = existentials adenine:currentResults = currentResults ; 
	rdf:type adenine:ConditionHandler
		# we really invalidate b after this function
	
		# Both parameters must be existentials with 
		if (or (! (existentials.contains a)) (! (existentials.contains b)))
			return (Set)
	
		= results (Set)
	
		= aExistIndex (existentials.indexOf a)
		= aData currentResults[aExistIndex]
	
		if (!= null c)
			#print (c.getClass)
			= bData (ask c @(b))
		
			for x in aData
				= inB false
				for y in bData
					if (== x y[0])
						= inB true
						break
				if (== inB false)
					= datum @()
					for w in existentials
						if (== w a)
							datum.add x
						else
							datum.add null
					results.add datum
			return results
	
		# else
		= bExistIndex (existentials.indexOf b)
		= bData currentResults[bExistIndex]
	
		for x in aData
			= inB false
			for y in bData
				if (== x y)
					= inB true
			if (== inB false)
				= datum @()
				for w in existentials
					if (== w a)
						datum.add x
					else
						# we really want to do: 'datum.add null'
						# but then 'x null' is not a value from before so merge will drop line
						# i.e. we need to add any value from prev. and ignore it
						datum.add bData[0]
				results.add datum
		return results
		*/

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SetDifferenceConditionHandler.class);

	public SetDifferenceConditionHandler() {
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

		int paramCount = condition.getParameterCount();
		if (paramCount < 2) {
			return new HashSet();
		}

		// Get indexes for first two params
		Resource extExistential = (Resource) condition.getParameter(0);
		int extExistNdx = -1;
		Resource intExistential = (Resource) condition.getParameter(1);
		int intExistNdx = -1;
		for (int existNdx = 0; existNdx < existentials.length; existNdx++) {
			if (existentials[existNdx].equals(extExistential)) {
				extExistNdx = existNdx;
			}
			if (existentials[existNdx].equals(intExistential)) {
				intExistNdx = existNdx;
			}
		}
		// First two parameters must be existentials
		if (extExistNdx == -1 || intExistNdx == -1) {
			return new HashSet();
		}
		
		if (hints[extExistNdx] == null) {
			// expected common error, when people don't use existential before 
			s_logger.error("Universe of extertnal existential needs to be constrained", new Exception());
		}

		Set set = null;
		if (paramCount == 2) {
			set = setDifference(denv, extExistential, extExistNdx, intExistential, intExistNdx, existentials, hints);
		} else { // let's just ignore more params
			set =
				setDifference(
					denv,
					extExistential,
					extExistNdx,
					intExistential,
					intExistNdx,
					(ConditionSet) condition.getParameter(2),
					existentials,
					hints);
		}

		// Convert items in the set from collections to arrays, if necessary
		HashSet set2 = new HashSet();
		Iterator i = set.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof Collection) {
				Collection c = (Collection) o;
				RDFNode[] datum = new RDFNode[c.size()];
				c.toArray(datum);
				set2.add(datum);
			} else {
				set2.add(o);
			}
		}

		return set2;
	}

	public Set setDifference(
		DynamicEnvironment denv,
		Resource extExistential,
		int extExistNdx,
		Resource intExistential,
		int intExistNdx,
		Resource[] existentials,
		RDFNode[][] currentResults) {

		Set results = new HashSet();

		RDFNode[] aData = currentResults[extExistNdx];
		RDFNode[] bData = currentResults[intExistNdx];

		for (int tx = 0; tx < aData.length; tx++) {
			RDFNode x = aData[tx];

			boolean inB = false;
			for (int ty = 0; ty < bData.length; ty++) {
				if (x.equals(bData[ty])) {
					inB = true;
					break;
				}
			}

			if (inB == false) {
				ArrayList datum = new ArrayList();
				for (int w = 0; w < existentials.length; w++) {
					if (existentials[w].equals(extExistential)) {
						datum.add(x);
					} else {
						// we really want to do: 'datum.add null'
						// but then 'x null' is not a value from before so merge will drop line
						// i.e. we need to add any value from prev. and ignore it
						datum.add(bData[0]);
					}
				}
				results.add(datum);
			}
		}
		return results;
	}

	public Set setDifference(
		DynamicEnvironment denv,
		Resource extExistential,
		int extExistNdx,
		Resource intExistential,
		int intExistNdx,
		ConditionSet nestedCS,
		Resource[] existentials,
		RDFNode[][] currentResults) {

		Set results = new HashSet();

		RDFNode[] aData = currentResults[extExistNdx];

		// get bData
		//RDFNode[] bData = currentResults[intExistNdx];
		IQueryEngine qe = denv.getQueryEngine();
		Set bData = null;
		try {
			/*
			 * for the nested condition set the existentials:
			 * 1] do not effect the parent existentials (few items in child will not mean anything for parent)
			 * 2] are effected by the parent (reducing the size of parent will mean smaller size here)
			 * 
			 * callees are telling us to do ext-int or ext-(int^ext) ==> i.e. we can do int==>int^ext or for 
			 * the simple (common) case that int==universe then int can become ext
			 */
			if (currentResults[extExistNdx] != null && currentResults[intExistNdx] == null) {
				//currentResults[intExistNdx] = currentResults[extExistNdx].clone();
				currentResults[intExistNdx] = new RDFNode[currentResults[extExistNdx].length];
				for (int i = 0; i < currentResults[extExistNdx].length; i++) {
					currentResults[intExistNdx][i] = currentResults[extExistNdx][i];
				}
			}
			bData = qe.query(denv, nestedCS, true, new Resource[] { intExistential }, existentials, currentResults);
			//s_logger.info("bData.size: " + bData.size());
		} catch (AdenineException e) {
			s_logger.error("Exception while evaluation nested CS", e);
			return results;
		}

		for (int tx = 0; tx < aData.length; tx++) {
			RDFNode x = aData[tx];

			boolean inB = false;
			Iterator bDataIt = bData.iterator();
			while (bDataIt.hasNext()) {
				RDFNode y = ((RDFNode[]) bDataIt.next())[0];
				if (x.equals(y)) {
					inB = true;
					break;
				}
			}

			if (inB == false) {
				ArrayList datum = new ArrayList();
				for (int w = 0; w < existentials.length; w++) {
					if (existentials[w].equals(extExistential)) {
						datum.add(x);
					} else {
						datum.add(null);
					}
				}
				results.add(datum);
			}
		}
		return results;

	}

}
