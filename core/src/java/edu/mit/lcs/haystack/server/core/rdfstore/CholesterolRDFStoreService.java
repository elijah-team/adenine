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

import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.security.Identity;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Cholesterol RDF store.
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class CholesterolRDFStoreService implements IRDFStore, IService {
	static {
		System.loadLibrary("Cholesterol");
	}

	boolean m_done = false;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(CholesterolRDFStoreService.class);

	/**
	 * @see IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
		m_done = true;

		while (m_eventThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	class Listener {
		Resource m_cookie;
		Resource m_subject;
		Resource m_predicate;
		RDFNode m_object;
		Resource m_service;
	}

	ServiceManager	m_manager;
		
	HashMap		m_rdfListeners = new HashMap();
	List		m_addedStatements = new ArrayList();
	
	Resource m_res;
	public Resource getServiceResource() {
		return m_res;
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.service.ISessionBasedService#login(Identity)
	 */
	public String login(Identity id) throws ServiceException {
		return "";
	}
	
	/**
	 * @see edu.mit.lcs.haystack.server.core.service.ISessionBasedService#getClientClassName()
	 */
	public String getClientClassName() {
		return "edu.mit.lcs.haystack.server.rdfstore.RemoteRDFContainer";
	}
	
	/**
	 * @see IRDFStore#logout(String)
	 */
	public void logout(String ticket) throws ServiceException {
	}

	/**
	 * @see IRDFStore#add(String, IRDFContainer)
	 */
	public void add(String ticket, IRDFContainer c)
		throws ServiceException {
		try {
			ArrayList toAdd = new ArrayList();
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Statement s = (Statement)i.next();
				if (add(s.getSubject().toString(), s.getPredicate().toString(), s.getObject().toString(), s.getMD5HashResource().toString())) {
					toAdd.add(new Object[] { s, null });
				}
			}
			i = c.iterator();
			if (!toAdd.isEmpty()) {
				synchronized (m_addedStatements) {
					m_addedStatements.addAll(toAdd);
				}
			}
		} catch (Exception e) {
			s_logger.error("", e);
			throw new ServiceException("", e);
		}
	}
	
	// TODO[dquan]: change back to be private
	native boolean add(String subj, String pred, String obj, String id);
	native boolean addAuthored(String subj, String pred, String obj, String id, Object [] authors);

	/**
	 * @see IRDFStore#remove(String, Statement, Resource[])
	 */
	native public void remove(String ticket, Statement s, Resource[] existentials)
		throws ServiceException;

	protected void addToRemoveQueue(String subject, String predicate, String object) {
		String innerObject = object.substring(1, object.length() - 1);
		RDFNode obj = object.charAt(0) == '<' ? (RDFNode)new Resource(innerObject) : new Literal(innerObject);
		Statement s = new Statement(new Resource(subject.substring(1, subject.length() - 1)), new Resource(predicate.substring(1, predicate.length() - 1)), obj);
		synchronized (m_addedStatements) {
			m_addedStatements.add(new Object[] { null, s });
		}
	}
		
	/**
	 * @see IRDFStore#query(String, Statement[], Resource[], Resource[])
	 */
	native public Set query(
		String ticket,
		Statement[] query,
		Resource[] variables,
		Resource[] existential)
		throws ServiceException;
		
	native public Set queryMulti(
		String ticket,
		Statement[] query,
		Resource[] variables,
		Resource[] existential,
		RDFNode [][] hints
		)
		throws ServiceException;
		
	/**
	 * @see IRDFStore#contains(String, Statement)
	 */
	public boolean contains(String ticket, Statement s) throws ServiceException {
		return contains(s.getSubject().toString(), s.getPredicate().toString(), s.getObject().toString());
	}

	private native boolean contains(String subj, String pred, String obj);

	/**
	 * @see IRDFStore#extract(String, Resource, Resource, RDFNode)
	 */
	native public RDFNode extract(
		String ticket,
		Resource subject,
		Resource predicate,
		RDFNode object)
		throws ServiceException;

	/**
	 * @see IRDFStore#getAuthors(String, Resource)
	 */
	public native Resource[] getAuthors(String ticket, Resource id) throws ServiceException;

	/**
	 * @see IRDFStore#getStatement(String, Resource)
	 */
	native public Statement getStatement(String ticket, Resource id)
		throws ServiceException;

	/**
	 * @see IRDFStore#getAuthoredStatementIDs(String, Resource)
	 */
	public native Resource[] getAuthoredStatementIDs(String ticket, Resource author) throws ServiceException;
	
	/**
	 * A simpler debug constructor suitable for debug purposes only.
	 */
	public void debugInit(String basePath) {
		doNativeInit(basePath + "Cholesterol.db");
	}
	
	native protected void doNativeInit(String basePath);
	native protected void doNativeKill();
	
	protected int m_database;
	protected Thread m_eventThread;

	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
			
		m_manager = manager;
		m_res = res;
		
		doNativeInit(basePath + "Cholesterol.db");
		
		m_eventThread = new Thread() {
			protected boolean checkStatement(Statement s, Listener l) {
				if (l.m_subject != null && !s.getSubject().equals(l.m_subject)) {
					return false;
				}
				if (l.m_predicate != null && !s.getPredicate().equals(l.m_predicate)) {
					return false;
				}
				if (l.m_object != null && !s.getObject().equals(l.m_object)) {
					return false;
				}
				return true;
			}
			
			public void run() {
				while (!m_done) {
					Object[] o = null;
					Statement statement = null;
					boolean more = true;
					int runs = 0;
					HashMap rdfListeners = null;

					while (more && !m_done) {
						++runs;
						try {
							o = null;
							synchronized (m_addedStatements) {
								if (!m_addedStatements.isEmpty()) {
									o = (Object[]) m_addedStatements.remove(0);
								}
								more = !m_addedStatements.isEmpty();
							}
		
							if (runs >= 10 || rdfListeners == null) {
								synchronized (m_rdfListeners) {
									rdfListeners = (HashMap)m_rdfListeners.clone();
								}
								runs = 0;
							}
							
							if (o != null) {
								statement = (o[0] == null) ? (Statement)o[1] : (Statement)o[0];
								Iterator i = rdfListeners.keySet().iterator();
								
								while (i.hasNext() && !m_done) {
									Resource key = (Resource)i.next();
									Listener listener = (Listener)rdfListeners.get(key);
									
									if (checkStatement(statement, listener)) {
										try {
											IRDFListener rdfl = (IRDFListener) m_manager.connectToService(listener.m_service, null);
											if (rdfl != null) {
												if (o[0] == null) {
													rdfl.statementRemoved(key, statement);
												} else {
													rdfl.statementAdded(key, statement);
												}
											} else if (m_done || m_manager.isShuttingDown()) {
												break;
											} else {
												s_logger.error("Unable to connect to event listener " + listener.m_service);
											}
										} catch (Exception e) {
											s_logger.error("", e);
										}
									}
								}
							}
						} catch (Exception e) {
							s_logger.error("", e);
						}
					} 
										
					try {
						sleep(500);
					} catch (Exception e) {
						s_logger.error("Sleeping error", e);
					}
				}
				
				s_logger.info("Event dispatching thread terminated");
			}
		};
		
		m_eventThread.start();
	}

	/**
	 * @see IService#shutdown()
	 */
	public void shutdown() throws ServiceException {
		doNativeKill();
	}

	/**
	 * @see IRDFStore#addRDFListener(String, Resource, Resource, Resource, RDFNode, Resource)
	 */
	public void addRDFListener(
		String ticket,
		Resource rdfListener,
		Resource subject,
		Resource predicate,
		RDFNode object,
		Resource res)
		throws ServiceException {
		Listener l = new Listener();
		l.m_subject = subject;
		l.m_predicate = predicate;
		l.m_object = object;
		l.m_cookie = res;
		l.m_service = rdfListener;
		synchronized (m_rdfListeners) {
			m_rdfListeners.put(res, l);
		}
	}

	/**
	 * @see IRDFStore#removeRDFListener(String, Resource)
	 */
	public void removeRDFListener(String ticket, Resource cookie)
		throws ServiceException {
		synchronized (m_rdfListeners) {
			m_rdfListeners.remove(cookie);
		}
	}

	/**
	 * @see IRDFStore#queryExtract(String, Statement[], Resource[], Resource[])
	 */
	public RDFNode[] queryExtract(
		String ticket,
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws ServiceException {
			existentials = Utilities.combineResourceArrays(variables, existentials);
			Set s = query(ticket, query, variables, existentials);
			if (s.isEmpty()) {
				return null;
			} else {
				return (RDFNode[])s.iterator().next();
			}
	}

	/**
	 * @see IRDFStore#replace(String, Resource, Resource, RDFNode, RDFNode)
	 */
	public void replace(
		String ticket,
		Resource subject,
		Resource predicate,
		RDFNode object,
		RDFNode newValue)
		throws ServiceException {
		boolean nullSubj = subject == null, nullPred = predicate == null, nullObj = object == null;
		if (((nullSubj ? 1 : 0) + (nullPred ? 1 : 0) + (nullObj ? 1 : 0)) != 1) {
			throw new ServiceException("replace expects exactly one null parameter");
		}
		Resource wildcard = Utilities.generateWildcardResource(1);
		remove(ticket, new Statement(nullSubj ? wildcard : subject,
			nullPred ? wildcard : predicate,
			nullObj ? (RDFNode)wildcard : object), new Resource[] { wildcard });
		add(ticket, new LocalRDFContainer(new Statement[] { new Statement(nullSubj ? (Resource)newValue : subject,
			nullPred ? (Resource)newValue : predicate,
			nullObj ? newValue : object)}));
	}

	/**
	 * @see IRDFStore#querySize(String, Statement[], Resource[], Resource[])
	 */
	public int querySize(
		String ticket,
		Statement[] query,
		Resource[] variables,
		Resource[] existential)
		throws ServiceException {
		return query(ticket, query, variables, existential).size();
	}
}
