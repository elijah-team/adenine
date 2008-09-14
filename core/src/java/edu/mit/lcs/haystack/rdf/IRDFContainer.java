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

import java.util.Iterator;
import java.util.Set;

/**
 * Base interface of all RDF containers.
 * @author Dennis Quan
 */
public interface IRDFContainer {
	/**
	 * Adds a single statement into this RDF container.
	 */
	public void add(Statement s) throws RDFException;

	/**
	 * Adds a single statement into this RDF container.
	 */
	public void add(Resource subject, Resource predicate, RDFNode object) 
		throws RDFException;

	/**
	 * Adds the statements in the given enumerable RDF container into this RDF container.
	 */
	public void add(IRDFContainer c) throws RDFException;

	/**
	 * Removes statements from this RDF container. 
	 * @param pattern		Statement to be removed (may include wildcards).
	 * @param existentials	Array of existential resources that serve as wildcards in the pattern.
	 */
	public void remove(Statement pattern, Resource[] existentials) throws RDFException;
	
	/**
	 * Performs a query against this RDF container.
	 * @param	query			Array of statements that specify constraints 
	 * 							on the given existential variables.
	 * @param	variables		Array of existential resources whose values are to be 
	 * 							determined and returned.
	 * @param	existentials	Array of existential resources that serve as 
	 * 							wildcards in the pattern. Instances of each 
	 * 							existential variable must satisfy each statement in
	 * 							the query.
	 * @return	A Set of RDFNode[], where the indices on the RDFNode[] correspond in order
	 * 			with the specified variables array.
	 */
	public Set query(Statement[] query, Resource[] variables, Resource[] existentials) throws RDFException;

	public int querySize(Statement[] query, Resource[] variables, Resource[] existentials) throws RDFException;

	/**
	 * Performs a query against this RDF container.
	 * @param	query			Array of statements that specify constraints 
	 * 							on the given existential variables.
	 * @param	variables		Array of existential resources whose values are to be 
	 * 							determined and returned.
	 * @param	existentials	Array of existential resources that serve as 
	 * 							wildcards in the pattern. Instances of each 
	 * 							existential variable must satisfy each statement in
	 * 							the query.
	 * @param	hints			Array of of array of strings that constrain the result of the query.
	 *							hints[0] corresponds to the first existential, hints[1] the second, etc.
	 *							each element of hints contains an array of allowed values.
	 * @return	A Set of RDFNode[], where the indices on the RDFNode[] correspond in order
	 * 			with the specified variables array.
	 */
	public Set queryMulti(Statement[] query, Resource[] variables, Resource[] existentials, RDFNode [][] hints) throws RDFException;


	public Set query(Statement s, Resource[] existentials) throws RDFException;

	/**
	 * Performs a query against this RDF container.
	 * @param	s				Query specification containing existential variables.
	 * @param	existentials	Array of existential resources that serve as 
	 * 							wildcards in the pattern. Instances of each 
	 * 							existential variable must satisfy each statement in
	 * 							the query.
	 * @param	hints			Array of of array of strings that constrain the result of the query.
	 *							hints[0] corresponds to the first existential, hints[1] the second, etc.
	 *							each element of hints contains an array of allowed values.
	 * @return	A Set of RDFNode[], where the indices on the RDFNode[] correspond in order
	 * 			with the specified existentials array.
	 */
	public Set queryMulti(Statement s, Resource[] existentials, RDFNode [][] hints) throws RDFException;
	
	/**
	 * Extracts an arbitrary value that satisfies the given constraint
	 * in this <code>IRDFContainer</code>. Exactly one of subject,
	 * predicate, and object must be <code>null</code>.  If a
	 * statement exists in this <code>IRDFContainer</code> such that
	 * an <code>RDFNode</code> can be substituted into the null
	 * parameter, returns this <code>RDFNode</code>. Otherwise,
	 * returns <code>null</code>.
	 *
	 * @return A value that when substituted into null parameter
	 * 			yields a statement present in this
	 * 			<code>IRDFContainer</code>, or <code>null</code> if no
	 * 			such statement exists. */
	public RDFNode extract(Resource subject, Resource predicate, RDFNode object) throws RDFException;
	
	public RDFNode[] queryExtract(Statement[] query, Resource[] variables, Resource[] existentials) throws RDFException;
	
	/**
	 * Determines if the given statement exists in this RDF container.
	 * @return	true if the statement exists; false otherwise.
	 */
	public boolean contains(Statement s) throws RDFException;

	/**
	 * Returns the resource identifying the given statement.
	 */
	public Resource getStatementID(Statement s) throws RDFException;

	public Resource[] getAuthors(Statement s) throws RDFException;
	public Resource[] getAuthors(Resource id) throws RDFException;
	
	/**
	 * Returns the statement identified by the specified resource.
	 */
	public Statement getStatement(Resource id) throws RDFException;
	public Resource[] getAuthoredStatementIDs(Resource author) throws RDFException;
	public Statement[] getAuthoredStatements(Resource author) throws RDFException;

	/**
	 * Returns the number of statements present in this RDF container.
	 */
	public int size() throws RDFException;
	
	/**
	 * Returns an Iterator over the Statement objects in this RDF container.
	 */
	public Iterator iterator() throws RDFException;
	
	/**
	 * Returns true if the container supports enumeration; otherwise returns false.
	 */
	public boolean supportsEnumeration();
	
	/**
	 * Returns true if the container stores author information; otherwise returns false.
	 */
	public boolean supportsAuthoring();
	
	public void replace(Resource subject, Resource predicate, RDFNode object, RDFNode newValue) throws RDFException;
}

