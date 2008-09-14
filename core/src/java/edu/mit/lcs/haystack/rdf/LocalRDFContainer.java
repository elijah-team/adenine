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

package edu.mit.lcs.haystack.rdf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An implementation of a local RDF container.
 * @author Dennis Quan
 */
public class LocalRDFContainer implements IRDFContainer, Serializable {
	protected Set m_data = new HashSet();

	/**
	 * Creates an empty local RDF container.
	 */
	public LocalRDFContainer() {
	}

	/**
	 * Creates a local RDF container and fills it with the given statements.
	 */
	public LocalRDFContainer(Statement[] statements) {
		for (int i = 0; i < statements.length; i++) {
			add(statements[i]);
		}
	}

	/**
	 * @see IRDFContainer#add(Statement)
	 */
	public void add(Statement s) {
		m_data.add(s);
	}
	
	/**
	 * @see IRDFContainer#add(Resource, Resource, RDFNode)
	 */
	public void add(Resource subject, Resource predicate, RDFNode object) {
		m_data.add(new Statement(subject, predicate, object));
	}

	public String toString() {
		return "" + this.getClass() + " - " + size() + " statements";
	}

	/**
	 * @see IRDFContainer#add(IRDFContainer)
	 */
	public void add(IRDFContainer c) throws RDFException {
		Iterator i = c.iterator();
		while (i.hasNext()) {
			add((Statement)i.next());
		}
	}

	/**
	 * @see IRDFContainer#remove(Statement, Resource[])
	 */
	public void remove(Statement pattern, Resource[] existentials) throws RDFException {
		Iterator i = m_data.iterator();
		Set toRemove = new HashSet();
		while (i.hasNext()) {
			Statement s = (Statement)i.next();
			if (matchStatement(s, pattern, existentials)) {
				toRemove.add(s);
			}
		}
		m_data.removeAll(toRemove);
	}

	public RDFNode extract(Resource subject, Resource predicate, RDFNode object) throws RDFException {
		boolean nullSubj = subject == null, nullPred = predicate == null, nullObj = object == null;
		if (((nullSubj ? 1 : 0) + (nullPred ? 1 : 0) + (nullObj ? 1 : 0)) != 1) {
			throw new RDFException("extract expects exactly one null parameter");
		}
		Iterator i = m_data.iterator();
		while (i.hasNext()) {
			Statement s = (Statement)i.next();
			if ((nullSubj || s.getSubject().equals(subject)) && 
				(nullPred || s.getPredicate().equals(predicate)) &&
				(nullObj || s.getObject().equals(object))) {
				return nullSubj ? (RDFNode)s.getSubject() : 
					(nullPred ? (RDFNode)s.getPredicate() : s.getObject());
			}
		}
		return null;
	}

	/**
	 * @see IRDFContainer#query(Statement[], Resource[], Resource[])
	 */
	public Set query(Statement[] query, Resource[] variables, Resource[] existentials)
		throws RDFException {
		existentials = Utilities.combineResourceArrays(variables, existentials);
		Set currentResults = new HashSet();
		ArrayList currentVariables = new ArrayList();
		for (int i = 0; i < query.length; i++) {
			Statement pattern = query[i];

			// Perform query			
			Set newResults = queryInternal(pattern, existentials);

			// Determine new variable list
			ArrayList currentQueryVars = new ArrayList();
			if (Utilities.containsResource(existentials, pattern.getSubject())) {
				currentQueryVars.add(pattern.getSubject());
			}
			if (Utilities.containsResource(existentials, pattern.getPredicate()) && !currentQueryVars.contains(pattern.getPredicate())) {
				currentQueryVars.add(pattern.getPredicate());
			}

			if (Utilities.containsResource(existentials, pattern.getObject()) && !currentQueryVars.contains(pattern.getObject())) {
				currentQueryVars.add(pattern.getObject());
			}
			
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
			
			// Merge current with new
			Set newResults2 = new HashSet();
			Iterator j = newResults.iterator();
			while (j.hasNext()) {
				RDFNode[] datum1 = (RDFNode[])j.next();
				
				Iterator k = currentResults.iterator();
next:			while (k.hasNext()) {
					RDFNode[] datum2 = (RDFNode[])k.next();
					
					RDFNode[] newDatum = new RDFNode[newVariables.size()];
					for (int l = 0; l < newVariables.size(); l++) {
						Resource var = (Resource)newVariables.get(l);
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
			
			currentResults = newResults2;
			currentVariables = newVariables;
		}

		// Return the requested variables
		Set results = new HashSet();
		Iterator l = currentResults.iterator();
		while (l.hasNext()) {
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

	protected boolean matchStatement(Statement s, Statement pattern, Resource[] existentials) {
		boolean anonSubject = Utilities.containsResource(existentials, pattern.getSubject());
		if (!anonSubject && !pattern.getSubject().equals(s.getSubject())) {
			return false;
		}

		boolean anonPredicate = Utilities.containsResource(existentials, pattern.getPredicate());
		if (!anonPredicate && !pattern.getPredicate().equals(s.getPredicate())) {
			return false;
		}

		boolean anonObject = Utilities.containsResource(existentials, pattern.getObject());
		if (!anonObject && !pattern.getObject().equals(s.getObject())) {
			return false;
		}
		
		// Check for duplicate pattern variables and make sure they match
		if (anonSubject) {
			if (anonPredicate && pattern.getSubject().equals(pattern.getPredicate()) &&
				!s.getSubject().equals(s.getPredicate())) {
				return false;
			}

			if (anonObject && pattern.getSubject().equals(pattern.getObject()) &&
				!s.getSubject().equals(s.getObject())) {
				return false;
			}
		}
		
		if (anonPredicate && anonObject && pattern.getPredicate().equals(pattern.getObject()) &&
			!s.getPredicate().equals(s.getObject())) {
			return false;
		}
		
		return true;
	}

/*	protected boolean matchStatement(Statement s, Statement pattern, Resource[] existentials) {
		boolean anonSubject = Utilities.containsResource(existentials, pattern.getSubject());
		boolean anonPredicate = Utilities.containsResource(existentials, pattern.getPredicate());
		boolean anonObject = Utilities.containsResource(existentials, pattern.getObject());

		if ((!anonSubject && (pattern.getSubject() != s.getSubject())) ||
			(!anonPredicate && (pattern.getPredicate() != s.getPredicate())) ||
			(!anonObject && (pattern.getObject() != s.getObject()))) {
			return false;
		}
		
		// Check for duplicate pattern variables and make sure they match
		if (anonSubject) {
			if (anonPredicate && (pattern.getSubject() == pattern.getPredicate()) &&
				(s.getSubject() != s.getPredicate())) {
				return false;
			}

			if (anonObject && (pattern.getSubject() == pattern.getObject()) &&
				(s.getSubject() != s.getObject())) {
				return false;
			}
		}
		
		if (anonPredicate && anonObject && (pattern.getPredicate() == pattern.getObject()) &&
			(s.getPredicate() != s.getObject())) {
			return false;
		}
		
		return true;
	}*/

	/**
	 * @see IRDFContainer#query(Statement, Resource[])
	 */
	public Set query(Statement s, Resource[] existentials) throws RDFException {
		return query(new Statement[] { s }, existentials, existentials);
	}

	protected Set queryInternal(Statement pattern, Resource[] existentials) throws RDFException {
		Iterator i = m_data.iterator();
		Set variables = new HashSet();
		boolean anonSubject = Utilities.containsResource(existentials, pattern.getSubject());
		boolean anonPredicate = Utilities.containsResource(existentials, pattern.getPredicate());
		boolean anonObject = Utilities.containsResource(existentials, pattern.getObject());
		
		if (anonSubject) {
			variables.add(pattern.getSubject());
		}
		if (anonPredicate) {
			variables.add(pattern.getPredicate());
		}
		if (anonObject) {
			variables.add(pattern.getObject());
		}
		int c = variables.size();
		if (c == 0) {
			// Nothing to match
			return new HashSet();
		}
		
		HashSet results = new HashSet();
		while (i.hasNext()) {
			Statement s = (Statement)i.next();
			if (matchStatement(s, pattern, existentials)) {
				RDFNode[] row = new RDFNode[c];
				int j = 0;
				Set s0 = new HashSet();
				
				if (anonSubject) {
					row[j++] = s.getSubject();
					s0.add(pattern.getSubject());
				}

				if (anonPredicate && !s0.contains(pattern.getPredicate())) {
					row[j++] = s.getPredicate();
					s0.add(pattern.getPredicate());
				}

				if (anonObject && !s0.contains(pattern.getObject())) {
					row[j++] = s.getObject();
				}
				
				results.add(row);
			}
		}
		return results;
	}

	/**
	 * @see IEnumerableRDFContainer#size()
	 */
	public int size() {
		return m_data.size();
	}

	/**
	 * @see IEnumerableRDFContainer#iterator()
	 */
	public Iterator iterator() {
		return m_data.iterator();
	}

	/**
	 * @see IRDFContainer#contains(Statement)
	 */
	public boolean contains(Statement s) throws RDFException {
		return m_data.contains(s);
	}
	
	/**
	 * @see IRDFContainer#getAuthoredStatementIDs(Resource)
	 */
	public Resource[] getAuthoredStatementIDs(Resource author)
		throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#getAuthoredStatements(Resource)
	 */
	public Statement[] getAuthoredStatements(Resource author) throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#getAuthors(Resource)
	 */
	public Resource[] getAuthors(Resource id) throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#getAuthors(Statement)
	 */
	public Resource[] getAuthors(Statement s) throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#getStatement(Resource)
	 */
	public Statement getStatement(Resource id) throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#getStatementID(Statement)
	 */
	public Resource getStatementID(Statement s) throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#supportsAuthoring()
	 */
	public boolean supportsAuthoring() {
		return false;
	}

	/**
	 * @see IRDFContainer#supportsEnumeration()
	 */
	public boolean supportsEnumeration() {
		return true;
	}

	/**
	 * @see IRDFContainer#queryExtract(Statement[], Resource[], Resource[])
	 */
	public RDFNode[] queryExtract(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException {
		Set s = query(query, variables, existentials);
		if (s.isEmpty()) {
			return null;
		} else {
			return (RDFNode[])s.iterator().next();
		}
	}

	/**
	 * @see IRDFContainer#queryMulti(Statement, Resource[], RDFNode[][])
	 */
	public Set queryMulti(Statement s, Resource[] existentials, RDFNode[][] hints)
		throws RDFException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see IRDFContainer#queryMulti(Statement[], Resource[], Resource[], RDFNode[][])
	 */
	public Set queryMulti(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials,
		RDFNode[][] hints)
		throws RDFException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see IRDFContainer#replace(Resource, Resource, RDFNode, RDFNode)
	 */
	public void replace(
		Resource subject,
		Resource predicate,
		RDFNode object,
		RDFNode newValue)
		throws RDFException {
		boolean nullSubj = subject == null, nullPred = predicate == null, nullObj = object == null;
		if (((nullSubj ? 1 : 0) + (nullPred ? 1 : 0) + (nullObj ? 1 : 0)) != 1) {
			throw new RDFException("replace expects exactly one null parameter");
		}
		Resource wildcard = Utilities.generateWildcardResource(1);
		remove(new Statement(nullSubj ? wildcard : subject,
			nullPred ? wildcard : predicate,
			nullObj ? (RDFNode)wildcard : object), new Resource[] { wildcard });
		add(new Statement(nullSubj ? (Resource)newValue : subject,
			nullPred ? (Resource)newValue : predicate,
			nullObj ? newValue : object));
	}

	/**
	 * @see IRDFContainer#querySize(Statement[], Resource[], Resource[])
	 */
	public int querySize(Statement[] query, Resource[] variables, Resource[] existentials)
		throws RDFException {
		return query(query, variables, existentials).size();
	}
}

