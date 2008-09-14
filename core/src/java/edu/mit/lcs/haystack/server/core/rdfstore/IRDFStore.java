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

package edu.mit.lcs.haystack.server.core.rdfstore;
import java.rmi.RemoteException;
import java.util.Set;

import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.server.core.service.ISessionBasedService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
/**
 * Haystack RDF store interface.
 * @author Dennis Quan
 */
public interface IRDFStore extends ISessionBasedService {
	public void add(String ticket, IRDFContainer c) throws ServiceException, RemoteException;
	public void remove(String ticket, Statement s, Resource existentials[]) throws ServiceException, RemoteException;
	public Set query(String ticket, Statement[] query, Resource[] variables, Resource[] existential) throws ServiceException, RemoteException;
	public int querySize(String ticket, Statement[] query, Resource[] variables, Resource[] existential) throws ServiceException, RemoteException;
	public Set queryMulti(String ticket, Statement[] query, Resource[] variables, Resource[] existential, RDFNode [][] hints) throws ServiceException, RemoteException;
	public boolean contains(String ticket, Statement s) throws ServiceException, RemoteException;
	public RDFNode extract(String ticket, Resource subject, Resource predicate, RDFNode object) throws ServiceException, RemoteException;
	public RDFNode[] queryExtract(String ticket, Statement[] query, Resource[] variables, Resource[] existential) throws ServiceException, RemoteException;
	public Resource[] getAuthors(String ticket, Resource id) throws ServiceException, RemoteException;
	public Statement getStatement(String ticket, Resource id) throws ServiceException, RemoteException;
	public Resource[] getAuthoredStatementIDs(String ticket, Resource author) throws ServiceException, RemoteException;
	public void addRDFListener(String ticket, Resource rdfListener, Resource subject, Resource predicate, RDFNode object, Resource cookie) throws ServiceException, RemoteException;
	public void removeRDFListener(String ticket, Resource cookie) throws ServiceException, RemoteException;
	public void replace(String ticket, Resource subject, Resource predicate, RDFNode object, RDFNode newValue) throws ServiceException, RemoteException;
}

