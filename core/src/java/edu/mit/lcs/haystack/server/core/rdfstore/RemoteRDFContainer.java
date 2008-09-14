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
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.security.Identity;
import edu.mit.lcs.haystack.server.core.service.IPersistent;
import edu.mit.lcs.haystack.server.core.service.ServiceException;

/**
 * Abstracts access to a remote RDF store as an RDF container object.
 * @author Dennis Quan
 */
public class RemoteRDFContainer implements IRDFContainer, IRDFEventSource, IPersistent {
	String ticket;
	IRDFStore store;
	
	public RemoteRDFContainer(IRDFStore store, String ticket) {
		this.store = store;
		this.ticket = ticket;
	}

	public RemoteRDFContainer(IRDFStore store, Identity id) throws RemoteException, ServiceException {
		this.store = store;
		this.ticket = store.login(id);
	}

	public IRDFStore getRDFStore() {
		return store;
	}
	
	public String getTicket() {
		return ticket;
	}

	public String toString() {
		return super.toString() + "[" + ((IPersistent)store).getServiceResource() + "]";
	}
	
	/**
	 * @see IRDFContainer#query(Statement[], Resource[], Resource[])
	 */
	public Set query(Statement[] query, Resource[] variables, Resource[] existentials)
		throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
			existentials = Utilities.combineResourceArrays(variables, existentials);
			Set s = store.query(ticket, query, variables, existentials);
			return s;
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see IRDFContainer#queryMulti(Statement[], Resource[], Resource[], RDFNode[][])
	 */
	public Set queryMulti(Statement[] query, Resource[] variables, Resource[] existentials, RDFNode [][] hints)
		throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
			existentials = Utilities.combineResourceArrays(variables, existentials);
			Set s = store.queryMulti(ticket, query, variables, existentials, hints);
			return s;
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}
	
	
	/**
	 * @see IRDFContainer#query(Statement, Resource[])
	 */
	public Set query(Statement s, Resource[] existentials) throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
			Set s2 = store.query(ticket, new Statement[] { s }, existentials, existentials);
			return s2;
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}	
	
	/**
	 * @see IRDFContainer#queryMulti(Statement, Resource[], RDFNode[][])
	 */
	public Set queryMulti(Statement s, Resource[] existentials, RDFNode [][] hints) throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
			Set s2 = store.queryMulti(ticket, new Statement[] { s }, existentials, existentials, hints);
			return s2;
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}
	
	/**
	 * @see IRDFContainer#contains(Statement)
	 */
	public boolean contains(Statement s) throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
			return store.contains(ticket, s);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see IRDFContainer#add(Statement)
	 */
	public void add(Statement s) throws RDFException {
		try {
			store.add(ticket, new LocalRDFContainer(new Statement[] { s }));
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see IRDFContainer#add(IRDFContainer)
	 */
	public void add(IRDFContainer c) throws RDFException {
		try {
			store.add(ticket, c);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
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
	 * @see IRDFContainer#remove(Statement, Resource[])
	 */
	public void remove(Statement s, Resource[] existentials) throws RDFException {
		try {
			store.remove(ticket, s, existentials);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}


	/**
	 * @see IRDFContainer#extract(Resource, Resource, RDFNode)
	 */
	public RDFNode extract(Resource subject, Resource predicate, RDFNode object)
		throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
//			System.out.println("Extract " + subject + " " + predicate + " " + object);
			RDFNode rdfn = store.extract(ticket, subject, predicate, object);
//			System.out.println(rdfn);
			return rdfn;
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see IAuthoredRDFContainer#getStatementID(Statement)
	 */
	public Resource getStatementID(Statement s) throws RDFException {
		return null;
	}

	/**
	 * @see IAuthoredRDFContainer#getAuthors(Statement)
	 */
	public Resource[] getAuthors(Statement s) throws RDFException {
		return null;
	}

	/**
	 * @see IAuthoredRDFContainer#getAuthors(Resource)
	 */
	public Resource[] getAuthors(Resource id) throws RDFException {
		return null;
	}

	/**
	 * @see IAuthoredRDFContainer#getStatement(Resource)
	 */
	public Statement getStatement(Resource id) throws RDFException {
		try {
			return store.getStatement(ticket, id);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see IAuthoredRDFContainer#getAuthoredStatementIDs(Resource)
	 */
	public Resource[] getAuthoredStatementIDs(Resource author)
		throws RDFException {
		return null;
	}

	/**
	 * @see IAuthoredRDFContainer#getAuthoredStatements(Resource)
	 */
	public Statement[] getAuthoredStatements(Resource author) throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#iterator()
	 */
	public Iterator iterator() throws RDFException {
		return null;
	}

	/**
	 * @see IRDFContainer#size()
	 */
	public int size() throws RDFException {
		return 0;
	}

	/**
	 * @see IRDFContainer#supportsAuthoring()
	 */
	public boolean supportsAuthoring() {
		return true;
	}

	/**
	 * @see IRDFContainer#supportsEnumeration()
	 */
	public boolean supportsEnumeration() {
		return false;
	}

	/**
	 * @see IRDFContainer#queryExtract(Statement[], Resource[], Resource[])
	 */
	public RDFNode[] queryExtract(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException {
		/*try {
			synchronized (RemoteRDFContainer.class) {
				java.io.FileOutputStream fos = new java.io.FileOutputStream("c:\\log", true);
				fos.write('x');
				fos.close();
			}
		} catch (java.io.IOException ioe) {}*/

		try {
			RDFNode[] rdfn = store.queryExtract(ticket, query, variables, existentials);
			return rdfn;
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
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
		try {
			store.replace(ticket, subject, predicate, object, newValue);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see IRDFContainer#querySize(Statement[], Resource[], Resource[])
	 */
	public int querySize(Statement[] query, Resource[] variables, Resource[] existentials)
		throws RDFException {
		try {
			return store.querySize(ticket, query, variables, existentials);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
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
		try {
			store.addRDFListener(ticket, rdfListener, subject, predicate, object, cookie);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.rdf.IRDFEventSource#removeRDFListener(Resource)
	 */
	public void removeRDFListener(Resource cookie) throws RDFException {
		try {
			store.removeRDFListener(ticket, cookie);
		} catch (ServiceException se) {
			throw new RDFException("RPC error.", se);
		} catch (RemoteException re) {
			throw new RDFException("RMI RPC error.", re);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.IPersistent#getServiceResource()
	 */
	public Resource getServiceResource() {
		return ((IPersistent)store).getServiceResource();
	}

}
