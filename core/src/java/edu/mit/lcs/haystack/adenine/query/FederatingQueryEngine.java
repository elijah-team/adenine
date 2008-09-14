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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class FederatingQueryEngine implements IQueryEngine {
	protected ArrayList m_sources = new ArrayList();

	public FederatingQueryEngine(Collection sources, DynamicEnvironment denv) {
		Iterator i = sources.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof Resource) {
				try {
					o = denv.getServiceAccessor().connectToService((Resource)o, denv.getIdentity());
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}

			if (o instanceof IRDFContainer) {
				m_sources.add(new DefaultQueryEngine((IRDFContainer)o));
			} else if (o instanceof IQueryEngine) {
				m_sources.add(o);
			}
		}
	}

	public void constructVariableList(Condition c, Resource[] existentials, HashSet currentQueryVars) {
		Iterator j = c.getParameterIterator();
		while (j.hasNext()) {
			Object o = j.next();
			if ((o instanceof Resource) && Utilities.containsResource(existentials, (Resource)o)) {
				currentQueryVars.add((Resource)o);
			} else if (o instanceof ConditionSet) {
				Iterator i = ((ConditionSet)o).iterator();
				while (i.hasNext()) {
					constructVariableList((Condition)i.next(), existentials, currentQueryVars);
				}
			}
		}
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
		
		return query(denv, query, and, variables, existentials);
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
		// Set of the current results. 
		Set currentResults = new HashSet();
		
		// assoc of Existential -> Set(possible values)
		Hashtable assoc = new Hashtable();
		
		// list of the current existentials in use
		ArrayList currentVariables = new ArrayList();
		Iterator i = query.iterator();
		while (i.hasNext()) {
			Condition pattern = (Condition)i.next();

			// Determine new variable list
			ArrayList currentQueryVars = new ArrayList();
			HashSet hs = new HashSet();
			constructVariableList(pattern, existentials, hs);
			currentQueryVars.addAll(hs);
			
			// Perform query
			Set newResults = new HashSet();
			Resource[] existentials2 = new Resource[currentQueryVars.size()];
			currentQueryVars.toArray(existentials2);
			
			RDFNode[][] va = new RDFNode[existentials2.length][];
			for (int x = 0; x < existentials2.length; x++) {
				String index = existentials2[x].toString();
				ArrayList vec = (ArrayList) assoc.get(index);

				/*						
				if (vec == null) System.out.println("vec = null for index = " + index);
				else System.out.println("vec = NOT null for index = " + index);
				*/

				if ((vec == null) || !and) {
					va[x] = null;
				} else {
					va[x] = new RDFNode[vec.size()];
					for (int y = 0; y < vec.size(); y++) {
						va[x][y] = (RDFNode)vec.get(y);
					}
				}
			}

			Iterator i2 = m_sources.iterator();
			HashSet s = new HashSet();
			while (i2.hasNext()) {
				// TODO[dquan]: be smarter about passing along incremental results
				IQueryEngine qe = (IQueryEngine)i2.next();
				ConditionSet cs = new ConditionSet();
				cs.add(pattern);
				Set s2 = qe.query(denv, cs, true, existentials2, existentials2);
				s.addAll(s2);
			}
			
			// Make entries in the table for all existential variables
			for (int x = 0; x < existentials2.length; x++) {
				// append values to what is already in there
				Object index = existentials2[x].toString();
				ArrayList old = (ArrayList) assoc.get(index);

				if (old == null) {
					// first time, just add
					ArrayList v = new ArrayList();
					assoc.put(index, v);
				}
			}

			// Add the statements to the federator's associative array
			Iterator iter = s.iterator();
			while (iter.hasNext()) {
				// loop through each existential / value pair
				RDFNode [] values = (RDFNode[]) iter.next();
				if (values.length != existentials2.length) {
					System.err.println(">> FederatingQueryEngine: error: noncompliant RDF container");
					return null;
				}
				for (int x = 0; x < values.length; x++) {
					// append values to what is already in there
					String index = existentials2[x].toString();
					
					ArrayList old = (ArrayList) assoc.get(index);
					old.add((RDFNode)values[x]);
				}
			}					
			newResults.addAll(s);

			if (currentVariables.isEmpty()) {
				currentVariables = currentQueryVars;
				currentResults = newResults;
				continue;
			}

			Set s2 = new HashSet();
			s2.addAll(currentQueryVars);
			s2.addAll(currentVariables);		
			
			ArrayList newVariables = new ArrayList();
			newVariables.addAll(s2);
			Object[] newVariablesArray = newVariables.toArray();
			
			// Merge current with new
			Set newResults2 = new HashSet();
			Iterator j = newResults.iterator();
			if (!and) {
				if (!j.hasNext()) {
					continue;
				}
				
				// Merge in old results first
				Iterator k = currentResults.iterator();
				while (k.hasNext()) {
					RDFNode[] datum2 = (RDFNode[])k.next();
					RDFNode[] newDatum = new RDFNode[newVariables.size()];
					for (int l = 0; l < newVariables.size(); l++) {
						Resource var = (Resource)newVariablesArray[l];
						int newIndex = currentQueryVars.indexOf(var);
						int oldIndex = currentVariables.indexOf(var);
	
						if ((newIndex == -1) && (oldIndex >= 0)) {
							newDatum[l] = datum2[oldIndex];
						} else if ((oldIndex == -1) && (newIndex >= 0)) {
							newDatum[l] = null;
						} else if ((oldIndex >= 0) && (newIndex >= 0)) {
							newDatum[l] = datum2[oldIndex];
						}
					}
					newResults2.add(newDatum);
				}				
				
				// Merge in new results next
				while (j.hasNext()) {
					RDFNode[] datum1 = (RDFNode[])j.next();
					RDFNode[] newDatum = new RDFNode[newVariables.size()];
					for (int l = 0; l < newVariables.size(); l++) {
						Resource var = (Resource)newVariablesArray[l];
						int newIndex = currentQueryVars.indexOf(var);
						int oldIndex = currentVariables.indexOf(var);
	
						if ((newIndex == -1) && (oldIndex >= 0)) {
							newDatum[l] = null;
						} else if ((oldIndex == -1) && (newIndex >= 0)) {
							newDatum[l] = datum1[newIndex];
						} else if ((oldIndex >= 0) && (newIndex >= 0)) {
							newDatum[l] = datum1[newIndex];
						}
					}
					newResults2.add(newDatum);
				}				
			} else {
				while (j.hasNext()) {
					RDFNode[] datum1 = (RDFNode[])j.next();
					
					Iterator k = currentResults.iterator();
next:				while (k.hasNext()) {
						RDFNode[] datum2 = (RDFNode[])k.next();
						
						RDFNode[] newDatum = new RDFNode[newVariables.size()];
						for (int l = 0; l < newVariables.size(); l++) {
							Resource var = (Resource)newVariablesArray[l];
							int newIndex = currentQueryVars.indexOf(var);
							int oldIndex = currentVariables.indexOf(var);
							
							if ((newIndex == -1) && (oldIndex >= 0)) {
								newDatum[l] = datum2[oldIndex];
							} else if ((oldIndex == -1) && (newIndex >= 0)) {
								newDatum[l] = datum1[newIndex];
							} else if ((oldIndex >= 0) && (newIndex >= 0)) {
								if (!datum1[newIndex].equals(datum2[oldIndex])) {
									continue next;
								}
																
								newDatum[l] = datum1[newIndex];
							}
						}
						newResults2.add(newDatum);
					}
				}
			}
							
			currentResults = newResults2;
			currentVariables = newVariables;
		}

		// Return the requested variables
		Set results = new HashSet();
		Iterator l = currentResults.iterator();
outer:	while (l.hasNext()) {
			RDFNode[] datum1 = (RDFNode[])l.next();
			RDFNode[] datum2 = new RDFNode[variables.length];
			
			for (int j = 0; j < variables.length; j++) {
				int k = currentVariables.indexOf(variables[j]);
				if (k == -1) {
					datum2[j] = null;
				} else {
					datum2[j] = datum1[k];
				}
			}
			results.add(datum2);
		}

		return results;
	}

}
