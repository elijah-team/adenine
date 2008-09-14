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

import edu.mit.lcs.haystack.Constants;

import java.util.Iterator;
import java.util.Set;

/**
 * Filters an existing RDF container for add requests; adds 
 * haystack:packageStatement assertions to all added statements.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class PackageFilterRDFContainer implements IRDFContainer {
	IRDFContainer m_source;
	Resource m_resPackage;
	
	public PackageFilterRDFContainer(IRDFContainer source, Resource resPackage) {
		m_source = source;
		m_resPackage = resPackage;
		if (m_resPackage != null) {
			try {
				m_source.add(new Statement(m_resPackage, Constants.s_rdf_type, Constants.s_haystack_Package));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void changePackage(Resource resPackage) {
		m_resPackage = resPackage;
		try {
			m_source.add(new Statement(m_resPackage, Constants.s_rdf_type, Constants.s_haystack_Package));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see IRDFContainer#add(Statement)
	 */
	public void add(Statement s) throws RDFException {
		m_source.add(s);
 		if (m_resPackage != null) {
			m_source.add(new Statement(m_resPackage, Constants.s_haystack_packageStatement, s.getMD5HashResource()));
		}
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
	 * @see IRDFContainer#add(IRDFContainer)
	 */
	public void add(IRDFContainer c) throws RDFException {
		m_source.add(c);
		if (m_resPackage == null) {
			return;
		}
		
		Iterator i = c.iterator();
		LocalRDFContainer rdfc = new LocalRDFContainer();
		while (i.hasNext()) {
			Statement s = (Statement)i.next();
			rdfc.add(new Statement(m_resPackage, Constants.s_haystack_packageStatement, s.getMD5HashResource()));
		}
		m_source.add(rdfc);
	}
	
	/**
	 * @see IRDFContainer#remove(Statement, Resource[])
	 */
	public void remove(Statement s, Resource[] existential) throws RDFException {
		// TODO[dquan]: remove from container all package statements
		m_source.remove(s, existential);
	}

	/**
	 * @see IRDFContainer#query(Statement[], Resource[], Resource[])
	 */
	public Set query(
		Statement[] query,
		Resource[] variables,
		Resource[] existential)
		throws RDFException {
		return m_source.query(query, variables, existential);
	}

	/**
	 * @see IRDFContainer#query(Statement, Resource[])
	 */
	public Set query(Statement s, Resource[] existential) throws RDFException {
		return m_source.query(s, existential);
	}

	/**
	 * @see IRDFContainer#contains(Statement)
	 */
	public boolean contains(Statement s) throws RDFException {
		return m_source.contains(s);
	}

	/**
	 * @see IRDFContainer#extract(Resource, Resource, RDFNode)
	 */
	public RDFNode extract(Resource subject, Resource predicate, RDFNode object)
		throws RDFException {
		return m_source.extract(subject, predicate, object);
	}

	/**
	 * @see IRDFContainer#getAuthoredStatementIDs(Resource)
	 */
	public Resource[] getAuthoredStatementIDs(Resource author)
		throws RDFException {
		return m_source.getAuthoredStatementIDs(author);
	}

	/**
	 * @see IRDFContainer#getAuthoredStatements(Resource)
	 */
	public Statement[] getAuthoredStatements(Resource author) throws RDFException {
		return m_source.getAuthoredStatements(author);
	}

	/**
	 * @see IRDFContainer#getAuthors(Resource)
	 */
	public Resource[] getAuthors(Resource id) throws RDFException {
		return m_source.getAuthors(id);
	}

	/**
	 * @see IRDFContainer#getAuthors(Statement)
	 */
	public Resource[] getAuthors(Statement s) throws RDFException {
		return m_source.getAuthors(s);
	}

	/**
	 * @see IRDFContainer#getStatement(Resource)
	 */
	public Statement getStatement(Resource id) throws RDFException {
		return m_source.getStatement(id);
	}

	/**
	 * @see IRDFContainer#getStatementID(Statement)
	 */
	public Resource getStatementID(Statement s) throws RDFException {
		return m_source.getStatementID(s);
	}

	/**
	 * @see IRDFContainer#iterator()
	 */
	public Iterator iterator() throws RDFException {
		return m_source.iterator();
	}

	/**
	 * @see IRDFContainer#size()
	 */
	public int size() throws RDFException {
		return m_source.size();
	}

	/**
	 * @see IRDFContainer#supportsAuthoring()
	 */
	public boolean supportsAuthoring() {
		return m_source.supportsAuthoring();
	}

	/**
	 * @see IRDFContainer#supportsEnumeration()
	 */
	public boolean supportsEnumeration() {
		return m_source.supportsEnumeration();
	}

	/**
	 * @see IRDFContainer#queryExtract(Statement[], Resource[], Resource[])
	 */
	public RDFNode[] queryExtract(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException {
		return m_source.queryExtract(query, variables, existentials);
	}


	/**
	 * @see IRDFContainer#queryMulti(Statement, Resource[], RDFNode[][])
	 */
	public Set queryMulti(Statement s, Resource[] existentials, RDFNode[][] hints)
		throws RDFException {
		return m_source.queryMulti(s, existentials, hints);
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
		return m_source.queryMulti(query, variables, existentials, hints);
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
		return m_source.querySize(query, variables, existentials);
	}
}
