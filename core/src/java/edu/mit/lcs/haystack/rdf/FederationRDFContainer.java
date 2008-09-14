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

import edu.mit.lcs.haystack.server.core.service.IPersistent;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.*;

/**
 * @author Dennis Quan
 */
public class FederationRDFContainer implements IRDFContainer, IRDFEventSource, IPersistent {
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append("[ ");
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source s = (Source)i.next();
			sb.append(s.m_rdfc);
			sb.append(" ");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public void addSource(IRDFContainer src, int priority) {
		if (src == null) {
			throw new IllegalArgumentException("src parameter cannot be null");
		}

		m_sources.add(new Source(src, priority));

		// Sort by priority
		Collections.sort(m_sources, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Source)o1).m_priority - ((Source)o2).m_priority;
			}
		});
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#add(Statement)
	 */
	public void add(Statement s) throws RDFException {
		((Source)m_sources.get(0)).m_rdfc.add(s);
	}

	/**
	 * @see IRDFContainer#add(Resource, Resource, RDFNode)
	 */
	public void add(Resource subject, Resource predicate, RDFNode object) 
		throws RDFException
	{
		add(new Statement(subject, predicate, object));
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#add(IRDFContainer)
	 */
	public void add(IRDFContainer c) throws RDFException {
		((Source)m_sources.get(0)).m_rdfc.add(c);
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#remove(Statement, Resource[])
	 */
	public void remove(Statement pattern, Resource[] existentials)
		throws RDFException {
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			src.m_rdfc.remove(pattern, existentials);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#query(Statement[], Resource[], Resource[])
	 */
	public Set query(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException {
		if (m_sources.size() == 1) {
			return ((Source)m_sources.get(0)).m_rdfc.query(query, variables, existentials);
		}
			
		existentials = Utilities.combineResourceArrays(variables, existentials);
		ArrayList sources = buildSourceList(query, existentials);

		if (sources.size() == 1) {
			return ((Source)sources.get(0)).m_rdfc.query(query, variables, existentials);
		}
		
		return queryInternal(query, variables, existentials, query.length == 1 ? m_sources : sources, null);
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#querySize(Statement[], Resource[], Resource[])
	 */
	public int querySize(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException {
		if (m_sources.size() == 1) {
			return ((Source)m_sources.get(0)).m_rdfc.querySize(query, variables, existentials);
		}
			
		return query(query, variables, existentials).size();
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#queryMulti(Statement[], Resource[], Resource[], RDFNode[][])
	 */
	public Set queryMulti(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials,
		RDFNode[][] hints)
		throws RDFException {
		if (m_sources.size() == 1) {
			return ((Source)m_sources.get(0)).m_rdfc.queryMulti(query, variables, existentials, hints);
		}
			
		existentials = Utilities.combineResourceArrays(variables, existentials);
		ArrayList sources = buildSourceList(query, existentials);

		if (sources.size() == 1) {
			return ((Source)sources.get(0)).m_rdfc.queryMulti(query, variables, existentials, hints);
		}
		
		return queryInternal(query, variables, existentials, query.length == 1 ? m_sources : sources, hints);
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#query(Statement, Resource[])
	 */
	public Set query(Statement s, Resource[] existentials)
		throws RDFException {
		if (m_sources.size() == 1) {
			return ((Source)m_sources.get(0)).m_rdfc.query(s, existentials);
		}
			
		return query(new Statement[] { s }, existentials, existentials);
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#queryMulti(Statement, Resource[], RDFNode[][])
	 */
	public Set queryMulti(
		Statement s,
		Resource[] existentials,
		RDFNode[][] hints)
		throws RDFException {
		if (m_sources.size() == 1) {
			return ((Source)m_sources.get(0)).m_rdfc.queryMulti(s, existentials, hints);
		}
			
		return queryInternal(new Statement[] { s }, existentials, existentials, m_sources, hints);
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#extract(Resource, Resource, RDFNode)
	 */
	public RDFNode extract(
		Resource subject,
		Resource predicate,
		RDFNode object)
		throws RDFException {
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			RDFNode rdfn = src.m_rdfc.extract(subject, predicate, object);
			if (rdfn != null) {
				return rdfn;
			}
		}
		
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#queryExtract(Statement[], Resource[], Resource[])
	 */
	public RDFNode[] queryExtract(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException {
		ArrayList sources = buildSourceList(query, existentials);
		
		Iterator i = sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			RDFNode[] rdfn = src.m_rdfc.queryExtract(query, variables, existentials);
			if (rdfn != null) {
				return rdfn;
			}
		}
		
		if (sources.size() == 1) {
			return null;
		}

		Set s = queryInternal(query, variables, existentials, sources, null);
		if (s.isEmpty()) {
			return null;
		} else {
			return (RDFNode[])s.iterator().next();
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#contains(Statement)
	 */
	public boolean contains(Statement s) throws RDFException {
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			if (src.m_rdfc.contains(s)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getStatementID(Statement)
	 */
	public Resource getStatementID(Statement s) throws RDFException {
		return s.getMD5HashResource();
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthors(Statement)
	 */
	public Resource[] getAuthors(Statement s) throws RDFException {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthors(Resource)
	 */
	public Resource[] getAuthors(Resource id) throws RDFException {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getStatement(Resource)
	 */
	public Statement getStatement(Resource id) throws RDFException {
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			Statement s = src.m_rdfc.getStatement(id);
			if (s != null) {
				return s;
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthoredStatementIDs(Resource)
	 */
	public Resource[] getAuthoredStatementIDs(Resource author)
		throws RDFException {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthoredStatements(Resource)
	 */
	public Statement[] getAuthoredStatements(Resource author)
		throws RDFException {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#size()
	 */
	public int size() throws RDFException {
		return 0;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#iterator()
	 */
	public Iterator iterator() throws RDFException {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#supportsEnumeration()
	 */
	public boolean supportsEnumeration() {
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#supportsAuthoring()
	 */
	public boolean supportsAuthoring() {
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#replace(Resource, Resource, RDFNode, RDFNode)
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
		add(new LocalRDFContainer(new Statement[] { new Statement(nullSubj ? (Resource)newValue : subject,
			nullPred ? (Resource)newValue : predicate,
			nullObj ? newValue : object)}));
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFEventSource#addRDFListener(Resource, Resource, Resource, RDFNode, Resource)
	 */
	public void addRDFListener(
		Resource rdfListener,
		Resource subject,
		Resource predicate,
		RDFNode object,
		Resource cookie)
		throws RDFException {
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			IRDFEventSource rdfes;
			try {
				rdfes = (IRDFEventSource)src.m_rdfc;
			} catch (Exception e) {
				continue;
			}
			rdfes.addRDFListener(rdfListener, subject, predicate, object, cookie);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFEventSource#removeRDFListener(Resource)
	 */
	public void removeRDFListener(Resource cookie) throws RDFException {
		Iterator i = m_sources.iterator();
		while (i.hasNext()) {
			Source src = (Source)i.next();
			IRDFEventSource rdfes;
			try {
				rdfes = (IRDFEventSource)src.m_rdfc;
			} catch (Exception e) {
				continue;
			}
			rdfes.removeRDFListener(cookie);
		}
	}

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FederationRDFContainer.class);

	class Source {
		Source(IRDFContainer rdfc, int priority) {
			m_rdfc = rdfc;
			m_priority = priority;
		}
		
		IRDFContainer m_rdfc;
		int m_priority;
		HashSet m_predicates = new HashSet();
	}
	
	ArrayList m_sources = new ArrayList();

	protected ArrayList buildSourceList(Statement[] query, Resource[] existentials) {
		// Build list of predicates
		HashSet predicates = new HashSet();
		boolean anonPredicate = false;
		for (int i = 0; !anonPredicate && (i < query.length); i++) {
			Resource predicate = query[i].getPredicate();
			predicates.add(predicate);
			if (Utilities.containsResource(existentials, predicate)) {
				anonPredicate = true;
			}
		}
		
		// Compile list of sources to check given the predicates in the query
		ArrayList sources;
		if (anonPredicate) {
			sources = m_sources;
		} else {
			sources = new ArrayList();
			Iterator i = m_sources.iterator(); 
			while (i.hasNext()) {
				Source src = (Source)i.next();
				
				if (src.m_predicates.isEmpty()) {
					sources.add(src);
				} else {
					HashSet set = new HashSet();
					set.addAll(predicates);
					set.retainAll(src.m_predicates);
					if (!set.isEmpty()) {
						sources.add(src);
					}
				}
			}
		}
		
		return sources;
	}
	
	protected Set queryInternal(Statement[] query, Resource[] variables, Resource[] existentials, ArrayList sources, RDFNode[][] va)
		throws RDFException {
		// Set of the current results. 
		Set currentResults = new HashSet();
		
		// Current hints
		HashMap currentHints = new HashMap();
		
		// list of the current existentials in use
		ArrayList currentVariables = new ArrayList();
		
		// Process query
		HashSet queryList = new HashSet();
		queryList.addAll(Arrays.asList(query));
		while (!queryList.isEmpty()) {
			// Find the best query line to work on
			int nBestScore = 0;
			Statement pattern = null;

			if (queryList.size() == 1) {
				pattern = (Statement) queryList.iterator().next();
				queryList.clear();
			} else {
				Iterator i = queryList.iterator();
				while (i.hasNext()) {
					Statement s = (Statement) i.next();
					Resource subject = s.getSubject();
					Resource predicate = s.getPredicate();
					RDFNode object = s.getObject();

					ArrayList subject0 = (ArrayList) currentHints.get(subject);
					ArrayList predicate0 = (ArrayList) currentHints.get(predicate);
					ArrayList object0 = (ArrayList) currentHints.get(object);

					int nScore = !Utilities.containsResource(existentials, subject) ? 30000 : (subject0 != null && subject0.size() > 0 ? 30000 / subject0.size() : -1);
					nScore += !Utilities.containsResource(existentials, predicate) ? 10000 : (predicate0 != null && predicate0.size() > 0 ? 10000 / predicate0.size() : -1);
					nScore += !Utilities.containsResource(existentials, object) ? 30000 : (object0 != null && object0.size() > 0 ? 30000 / object0.size() : -1);

					if ((pattern == null) || (nScore > nBestScore)) {
						pattern = s;
						nBestScore = nScore;
					}
				}

				queryList.remove(pattern);
			}

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
			
			// Perform query
			Set newResults = new HashSet();
			Iterator j = sources.iterator();

			Resource[] existentials2;

			if (va == null) {
				existentials2 = new Resource[currentQueryVars.size()];
			   	currentQueryVars.toArray(existentials2);
				va = new RDFNode[existentials2.length][];
				for (int x = 0; x < existentials2.length; x++) {
					Object index = existentials2[x];
					ArrayList vec = (ArrayList) currentHints.get(index);
	
					if (vec == null) {
						va[x] = null;
					} else {
						va[x] = new RDFNode[vec.size()];
						vec.toArray(va[x]);
					}
				}
			} else {
				existentials2 = existentials; 
			}

			while (j.hasNext()) {
				Source src = (Source) j.next();

				Set s;
				try {
					s = src.m_rdfc.queryMulti(pattern, existentials2, va);
				} catch (UnsupportedOperationException ue) {
					s = src.m_rdfc.query(pattern, existentials2);
				}

				if (s == null) {
					return null;
				}

				newResults.addAll(s);
			}

			va = null;

			Set s2 = new HashSet();
			s2.addAll(currentQueryVars);
			s2.addAll(currentVariables);

			ArrayList newVariables = new ArrayList();
			newVariables.addAll(s2);

			// Make entries in the table for all existential variables
			j = newVariables.iterator();
			while (j.hasNext()) {
				Object index = j.next();
				ArrayList v = new ArrayList();
				currentHints.put(index, v);
			}

			if (currentVariables.isEmpty()) {
				// Copy values over to currentHints quickly
				j = newResults.iterator();
				while (j.hasNext()) {
					RDFNode[] datum1 = (RDFNode[]) j.next();

					for (int l = 0; l < newVariables.size(); l++) {
						Resource var = (Resource) newVariables.get(l);
						ArrayList al = (ArrayList) currentHints.get(var);
						al.add(datum1[l]);
					}
				}

				currentVariables = currentQueryVars;
				currentResults = newResults;
				continue;
			}

			// Merge current with new
			Set newResults2 = new HashSet();
			j = newResults.iterator();
			while (j.hasNext()) {
				RDFNode[] datum1 = (RDFNode[]) j.next();

				Iterator k = currentResults.iterator();
				next : while (k.hasNext()) {
					RDFNode[] datum2 = (RDFNode[]) k.next();

					RDFNode[] newDatum = new RDFNode[newVariables.size()];
					for (int l = 0; l < newVariables.size(); l++) {
						Resource var = (Resource) newVariables.get(l);
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

					for (int l = 0; l < newVariables.size(); l++) {
						Resource var = (Resource) newVariables.get(l);
						ArrayList al = (ArrayList) currentHints.get(var);
						al.add(newDatum[l]);
					}
				}
			}

			currentResults = newResults2;
			currentVariables = newVariables;
		}

		// Return the requested variables
		Set results = new HashSet();
		Set arrayedResults = new HashSet();
		Iterator l = currentResults.iterator();
		while (l.hasNext()) {
			RDFNode[] datum1 = (RDFNode[]) l.next();
			RDFNode[] datum2 = new RDFNode[variables.length];

			for (int j = 0; j < variables.length; j++) {
				int k = currentVariables.indexOf(variables[j]);
				if (k == -1) {
					datum2[j] = null;
				} else {
					datum2[j] = datum1[k];
				}
			}
			List l2 = Arrays.asList(datum2);
			if (!arrayedResults.contains(l2)) {
				arrayedResults.add(l2);
				results.add(datum2);
			}
		}

		return results;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IPersistent#getServiceResource()
	 */
	public Resource getServiceResource() {
		IRDFContainer rdfc = ((Source)m_sources.get(0)).m_rdfc;
		if (rdfc instanceof IPersistent) {
			return ((IPersistent)rdfc).getServiceResource();
		} else {
			return null;
		}
	}
}
