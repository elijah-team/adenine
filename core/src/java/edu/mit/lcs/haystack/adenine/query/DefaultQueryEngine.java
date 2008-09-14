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
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author Dennis Quan
 *
 * @version 1.2 Janis Sermulins performance optimization
 *
 */

public class DefaultQueryEngine implements IQueryEngine {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(DefaultQueryEngine.class);
	IRDFContainer m_source;

	/**
	 * Constructor for DefaultQueryEngine.
	 */
	public DefaultQueryEngine() {
		m_source = null;
	}

	/**
	 * Constructs a DefaultQueryEngine tied to a specific RDF source.
	 * @param rdfc The RDF source to which this engine should be tied.
	 */
	public DefaultQueryEngine(IRDFContainer rdfc) {
		m_source = rdfc;
	}

	public IConditionHandler resolveConditionHandler(DynamicEnvironment denv, Condition c) {
		if (c.getFunction().equals(AdenineConstants.or)) {
			return new OrConditionHandler();
		} else if (c.getFunction().equals(AdenineConstants.and)) {
			return new AndConditionHandler();
		} else if (c.getFunction().equals(AdenineConstants.multiContains)) {
			return new RDFMultiContainsConditionHandler();
		} else if (c.getFunction().equals(AdenineConstants.contains)) {
			return new RDFContainsConditionHandler();
		} else if (c.getFunction().equals(AdenineConstants.setDifference)) {
			return new SetDifferenceConditionHandler();
		} else if (Utilities.isType(c.getFunction(), AdenineConstants.ConditionHandler, denv.getSource())) {
			return new AdenineConditionHandler();
		} else {
			return new RDFPredicateConditionHandler();
		}
	}

	public boolean isRDFContainerCondition(DynamicEnvironment denv, Condition c) {
		if (c.getFunction().equals(AdenineConstants.or)) {
			return false;
		} else if (c.getFunction().equals(AdenineConstants.and)) {
			return false;
		} else if (c.getFunction().equals(AdenineConstants.contains)) {
			return false;
		} else if (c.getFunction().equals(AdenineConstants.multiContains)) {
			return false;
		} else if (Utilities.isType(c.getFunction(), AdenineConstants.ConditionHandler, denv.getSource())) {
			return false;
		} else {
			return true;
		}
	}

	public void constructVariableList(Condition c, Resource[] existentials, HashSet currentQueryVars) {
		Iterator j = c.getParameterIterator();
		while (j.hasNext()) {
			Object o = j.next();
			if ((o instanceof Resource) && Utilities.containsResource(existentials, (Resource) o)) {
				currentQueryVars.add((Resource) o);
			} else if (o instanceof Condition) {
				constructVariableList((Condition) o, existentials, currentQueryVars);
			} else if (o instanceof ConditionSet) {
				Iterator i = ((ConditionSet) o).iterator();
				while (i.hasNext()) {
					constructVariableList((Condition) i.next(), existentials, currentQueryVars);
				}
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.adenine.query.IQueryEngine#query(DynamicEnvironment, ConditionSet, Resource[], Resource[])
	 */
	public Set query(
		DynamicEnvironment denv,
		ConditionSet query,
		boolean and,
		Resource[] variables,
		Resource[] existentials)
		throws AdenineException {

		RDFNode[][] hints = new RDFNode[existentials.length][];

		for (int t = 0; t < existentials.length; t++) {
			hints[t] = null;
		}

		return query(denv, query, and, variables, existentials, hints);
	}

	/**
	 * @see edu.mit.lcs.haystack.adenine.query.IQueryEngine#query(DynamicEnvironment, ConditionSet, Resource[], Resource[], RDFNode[][])
	 */
	public Set query(
		DynamicEnvironment denv,
		ConditionSet query,
		boolean and,
		Resource[] variables,
		Resource[] existentials,
		RDFNode[][] hints)
		throws AdenineException {

		IRDFContainer poppedSource = null;
		if (m_source != null) {
			poppedSource = denv.getSource();
			denv.setSource(m_source);
		}
		try {
			// Set of the current results. 
			Set currentResults = new HashSet();

			// assoc of Existential name -> HashSet (possible values)
			Hashtable assoc = new Hashtable();

			if (hints.length == existentials.length) {
				for (int t = 0; t < hints.length; t++) {
					if (hints[t] != null) {
						RDFNode[] arr = hints[t];
						HashSet set = new HashSet();

						for (int tt = 0; tt < arr.length; tt++) {
							set.add(arr[tt]);
						}
						assoc.put(existentials[t].toString(), set);
					}
				}
			}

			// optimize condition set to group RDFContainer conditions together
			if (and && query.count() > 1) {
				ConditionSet origQuery = query;
				query = new ConditionSet();
				Iterator origQueryIt = origQuery.iterator();
				ArrayList optimizeableConditions = new ArrayList(query.count());
				while (origQueryIt.hasNext()) {
					Condition curCondition = (Condition) origQueryIt.next();
					if (isRDFContainerCondition(denv, curCondition)) {
						optimizeableConditions.add(curCondition);
					} else {
						if (!optimizeableConditions.isEmpty()) {
							query.add(new Condition(AdenineConstants.multiContains, optimizeableConditions));
							optimizeableConditions = new ArrayList(query.count());
						}
						query.add(curCondition);
					}
				}
				if (!optimizeableConditions.isEmpty()) {
					query.add(new Condition(AdenineConstants.multiContains, optimizeableConditions));
				}
			}

			// list of the current existentials in use
			ArrayList currentVariables = new ArrayList();
			Iterator i = query.iterator();
			while (i.hasNext()) {
				Condition pattern = (Condition) i.next();

				// Determine new variable list
				ArrayList currentQueryVars = new ArrayList();
				HashSet hs = new HashSet();
				constructVariableList(pattern, existentials, hs);
				currentQueryVars.addAll(hs);

				// Perform query
				Resource[] existentials2 = new Resource[currentQueryVars.size()];
				currentQueryVars.toArray(existentials2);

				RDFNode[][] va = new RDFNode[existentials2.length][];
				for (int x = 0; x < existentials2.length; x++) {
					String index = existentials2[x].toString();
					HashSet set = (HashSet) assoc.get(index);

					if (set == null) {
						va[x] = null;
					} else {
						va[x] = new RDFNode[set.size()];
						int y = 0;
						Iterator iter = set.iterator();
						while (iter.hasNext()) {
							va[x][y++] = (RDFNode) iter.next();
						}
					}
				}

				Set newResults =
					resolveConditionHandler(denv, pattern).resolveCondition(denv, this, pattern, existentials2, va);

				if (currentVariables.isEmpty()) {
					currentVariables = currentQueryVars;
					currentResults = newResults;
				} else {

					Set s2 = new HashSet();
					s2.addAll(currentQueryVars);
					s2.addAll(currentVariables);
					ArrayList newVariables = new ArrayList();
					newVariables.addAll(s2);

					// Merge current with new
					if (and) {
						currentResults =
							mergeAnd(currentResults, newResults, currentVariables, currentQueryVars, newVariables);
					} else {
						currentResults =
							mergeOr(currentResults, newResults, currentVariables, currentQueryVars, newVariables);
					}
					currentVariables = newVariables;
				}

				if (and) {
					if ((currentResults == null) || (currentResults.size() == 0)) {
						return new HashSet();
					}

					assoc = new Hashtable();

					for (int j = 0; j < currentVariables.size(); j++) {
						String index = currentVariables.get(j).toString();
						HashSet values = new HashSet();

						Iterator k = currentResults.iterator();
						while (k.hasNext()) {
							RDFNode[] datum = (RDFNode[]) k.next();
							if (datum[j] != null)
								values.add(datum[j]);
						}
						assoc.put(index, values);
					}
				}
			}

			// Return the requested variables
			Set results = new HashSet();

			int[] varIndices = new int[variables.length];
			for (int j = 0; j < variables.length; j++) {
				varIndices[j] = currentVariables.indexOf(variables[j]);
			}

			Iterator l = currentResults.iterator();
			outer : while (l.hasNext()) {
				RDFNode[] datum1 = (RDFNode[]) l.next();
				RDFNode[] datum2 = new RDFNode[variables.length];

				for (int j = 0; j < variables.length; j++) {
					if (varIndices[j] == -1)
						datum2[j] = null;
					else
						datum2[j] = datum1[varIndices[j]];
				}
				results.add(datum2);
			}

			return results;
		} finally {
			if (poppedSource != null) {
				denv.setSource(poppedSource);
			}
		}
	}

	/*
	private Set mergeAnd_Old(
		Set currentResults,
		Set newResults,
		ArrayList currentVariables,
		ArrayList currentQueryVars,
		ArrayList newVariables) {

		int commonCount = 0, oldOnlyCount = 0, newOnlyCount = 0;

		int[] commonVariables = new int[newVariables.size()];
		int[] commonVarsOldPos = new int[newVariables.size()];
		int[] commonVarsNewPos = new int[newVariables.size()];

		int[] oldOnlyVariables = new int[newVariables.size()];
		int[] oldOnlyVarsPos = new int[newVariables.size()];

		int[] newOnlyVariables = new int[newVariables.size()];
		int[] newOnlyVarsPos = new int[newVariables.size()];

		Set newResults2 = new HashSet();

		for (int t = 0; t < newVariables.size(); t++) {
			Resource var = (Resource) newVariables.get(t);
			int newIndex = currentQueryVars.indexOf(var);
			int oldIndex = currentVariables.indexOf(var);

			if ((newIndex == -1) && (oldIndex >= 0)) {
				oldOnlyVarsPos[oldOnlyCount] = oldIndex;
				oldOnlyVariables[oldOnlyCount] = t;
				oldOnlyCount++;
			} else if ((oldIndex == -1) && (newIndex >= 0)) {
				newOnlyVarsPos[newOnlyCount] = newIndex;
				newOnlyVariables[newOnlyCount] = t;
				newOnlyCount++;
			} else if ((oldIndex >= 0) && (newIndex >= 0)) {
				commonVarsOldPos[commonCount] = oldIndex;
				commonVarsNewPos[commonCount] = newIndex;
				commonVariables[commonCount] = t;
				commonCount++;
			}
		}

		Iterator j = newResults.iterator();

		while (j.hasNext()) {
			RDFNode[] datum1 = (RDFNode[]) j.next();
			Iterator k = currentResults.iterator();

			next : while (k.hasNext()) {
				RDFNode[] datum2 = (RDFNode[]) k.next();

				for (int i = 0; i < commonCount; i++) {
					if (!datum1[commonVarsNewPos[i]].equals(datum2[commonVarsOldPos[i]])) {
						continue next;
					}
				}

				RDFNode[] newDatum = new RDFNode[newVariables.size()];
				for (int i = 0; i < commonCount; i++) {
					newDatum[commonVariables[i]] = datum1[commonVarsNewPos[i]];
				}
				for (int i = 0; i < newOnlyCount; i++) {
					newDatum[newOnlyVariables[i]] = datum1[newOnlyVarsPos[i]];
				}
				for (int i = 0; i < oldOnlyCount; i++) {
					newDatum[oldOnlyVariables[i]] = datum2[oldOnlyVarsPos[i]];
				}
				newResults2.add(newDatum);
			}
		}
		return newResults2;
	}
	*/

	private Set mergeAnd(
		Set currentResults,
		Set newResults,
		ArrayList currentVariables,
		ArrayList currentQueryVars,
		ArrayList newVariables) {

		int commonCount = 0, oldOnlyCount = 0, newOnlyCount = 0;

		int[] commonVariables = new int[newVariables.size()];
		int[] commonVarsOldPos = new int[newVariables.size()];
		int[] commonVarsNewPos = new int[newVariables.size()];

		int[] oldOnlyVariables = new int[newVariables.size()];
		int[] oldOnlyVarsPos = new int[newVariables.size()];

		int[] newOnlyVariables = new int[newVariables.size()];
		int[] newOnlyVarsPos = new int[newVariables.size()];

		// init above variables for merging
		for (int t = 0; t < newVariables.size(); t++) {
			Resource var = (Resource) newVariables.get(t);
			int newIndex = currentQueryVars.indexOf(var);
			int oldIndex = currentVariables.indexOf(var);

			if ((newIndex == -1) && (oldIndex >= 0)) {
				oldOnlyVarsPos[oldOnlyCount] = oldIndex;
				oldOnlyVariables[oldOnlyCount] = t;
				oldOnlyCount++;
			} else if ((oldIndex == -1) && (newIndex >= 0)) {
				newOnlyVarsPos[newOnlyCount] = newIndex;
				newOnlyVariables[newOnlyCount] = t;
				newOnlyCount++;
			} else if ((oldIndex >= 0) && (newIndex >= 0)) {
				commonVarsOldPos[commonCount] = oldIndex;
				commonVarsNewPos[commonCount] = newIndex;
				commonVariables[commonCount] = t;
				commonCount++;
			}
		}

		/*
		 * move newResults (since it should be smaller - it should be constrained on the currentResults already)
		 * into a map with the key (actually supposed to be the key.Hash) being the common variables hash... 
		 */
		HashMap newResultsMap = new HashMap(newResults.size());
		Iterator newResIt = newResults.iterator();
		while (newResIt.hasNext()) {
			RDFNode[] newResultDatum = (RDFNode[]) newResIt.next();

			// get hash code for common variables
			int newResultDatumHashCode = 0;
			for (int i = 0; i < commonCount; i++) {
				newResultDatumHashCode += newResultDatum[commonVarsNewPos[i]].hashCode();
			}
			Integer newResultDatumHashCodeInt = new Integer(newResultDatumHashCode);

			LinkedList ll = (LinkedList) newResultsMap.get(newResultDatumHashCodeInt);
			if (ll == null) {
				ll = new LinkedList();
				newResultsMap.put(newResultDatumHashCodeInt, ll);
			}
			ll.addLast(newResultDatum);
		}

		// merge results
		Set mergedResults = new HashSet();

		Iterator curResultIt = currentResults.iterator();
		while (curResultIt.hasNext()) {
			RDFNode[] curResultDatum = (RDFNode[]) curResultIt.next();

			// get hash code for common variables
			int curResultDatumHashCode = 0;
			for (int i = 0; i < commonCount; i++) {
				curResultDatumHashCode += curResultDatum[commonVarsOldPos[i]].hashCode();
			}
			Integer curResultDatumHashCodeInt = new Integer(curResultDatumHashCode);

			LinkedList ll = (LinkedList) newResultsMap.get(curResultDatumHashCodeInt);
			if (ll != null) {
				// for common variables: newResDatum == curResDatum ==> merge
				// actually only the hashes are the same, we should really check if they are equal
				//  but, the odds of that happening are very low...

				Iterator newResultIt = ll.iterator();
				while (newResultIt.hasNext()) {
					RDFNode[] newResultDatum = (RDFNode[]) newResultIt.next();
					
					RDFNode[] newDatum = new RDFNode[newVariables.size()];

					for (int i = 0; i < commonCount; i++) {
						newDatum[commonVariables[i]] = newResultDatum[commonVarsNewPos[i]];
					}
					for (int i = 0; i < newOnlyCount; i++) {
						newDatum[newOnlyVariables[i]] = newResultDatum[newOnlyVarsPos[i]];
					}
					for (int i = 0; i < oldOnlyCount; i++) {
						newDatum[oldOnlyVariables[i]] = curResultDatum[oldOnlyVarsPos[i]];
					}
					mergedResults.add(newDatum);
				}
			}

		}

		return mergedResults;
	}

	private Set mergeOr(
		Set currentResults,
		Set newResults,
		ArrayList currentVariables,
		ArrayList currentQueryVars,
		ArrayList newVariables) {

		int commonCount = 0, oldOnlyCount = 0, newOnlyCount = 0;

		int[] commonVariables = new int[newVariables.size()];
		int[] commonVarsOldPos = new int[newVariables.size()];
		int[] commonVarsNewPos = new int[newVariables.size()];

		int[] oldOnlyVariables = new int[newVariables.size()];
		int[] oldOnlyVarsPos = new int[newVariables.size()];

		int[] newOnlyVariables = new int[newVariables.size()];
		int[] newOnlyVarsPos = new int[newVariables.size()];

		RDFNode[][] newResults1 = new RDFNode[newResults.size()][];
		RDFNode[][] newResults2 = new RDFNode[currentResults.size()][];

		int[] hash1 = new int[newResults.size()];
		int[] hash2 = new int[currentResults.size()];

		Set finalResult = new HashSet();
		int count;

		for (int t = 0; t < newVariables.size(); t++) {
			Resource var = (Resource) newVariables.get(t);
			int newIndex = currentQueryVars.indexOf(var);
			int oldIndex = currentVariables.indexOf(var);

			if ((newIndex == -1) && (oldIndex >= 0)) {
				oldOnlyVarsPos[oldOnlyCount] = oldIndex;
				oldOnlyVariables[oldOnlyCount] = t;
				oldOnlyCount++;
			} else if ((oldIndex == -1) && (newIndex >= 0)) {
				newOnlyVarsPos[newOnlyCount] = newIndex;
				newOnlyVariables[newOnlyCount] = t;
				newOnlyCount++;
			} else if ((oldIndex >= 0) && (newIndex >= 0)) {
				commonVarsOldPos[commonCount] = oldIndex;
				commonVarsNewPos[commonCount] = newIndex;
				commonVariables[commonCount] = t;
				commonCount++;
			}
		}

		count = 0;
		Iterator j = newResults.iterator();
		while (j.hasNext()) {
			int hash = 0;
			RDFNode[] datum1 = (RDFNode[]) j.next();
			RDFNode[] newDatum = new RDFNode[newVariables.size()];

			for (int i = 0; i < commonCount; i++) {
				RDFNode node = datum1[commonVarsNewPos[i]];
				newDatum[commonVariables[i]] = node;
				hash += node.hashCode();
			}
			for (int i = 0; i < newOnlyCount; i++) {
				RDFNode node = datum1[newOnlyVarsPos[i]];
				newDatum[newOnlyVariables[i]] = node;
				hash += node.hashCode();
			}
			for (int i = 0; i < oldOnlyCount; i++) {
				newDatum[oldOnlyVariables[i]] = null;
			}
			hash1[count] = hash;
			newResults1[count++] = newDatum;
			finalResult.add(newDatum);
		}

		count = 0;
		Iterator k = currentResults.iterator();
		while (k.hasNext()) {
			int hash = 0;
			RDFNode[] datum2 = (RDFNode[]) k.next();
			RDFNode[] newDatum = new RDFNode[newVariables.size()];

			for (int i = 0; i < commonCount; i++) {
				RDFNode node = datum2[commonVarsOldPos[i]];
				newDatum[commonVariables[i]] = node;
				hash += node.hashCode();
			}
			for (int i = 0; i < newOnlyCount; i++) {
				newDatum[newOnlyVariables[i]] = null;
			}
			for (int i = 0; i < oldOnlyCount; i++) {
				RDFNode node = datum2[oldOnlyVarsPos[i]];
				newDatum[oldOnlyVariables[i]] = node;
				hash += node.hashCode();
			}
			hash2[count] = hash;
			newResults2[count++] = newDatum;
		}

		next : for (int t = 0; t < newResults2.length; t++) {
			for (int tt = 0; tt < newResults1.length; tt++) {
				if (hash2[t] == hash1[tt]) {
					if (Arrays.equals((Object[]) newResults2[t], (Object[]) newResults1[tt])) {
						continue next;
					}
				}
			}
			finalResult.add(newResults2[t]);
		}
		return finalResult;
	}
}
