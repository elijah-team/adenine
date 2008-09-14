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

package edu.mit.lcs.haystack.server.rdfstore;

import edu.mit.lcs.haystack.lucene.document.Document;
import edu.mit.lcs.haystack.lucene.document.Field;
import edu.mit.lcs.haystack.lucene.index.ForwardIndexWriter;
import edu.mit.lcs.haystack.lucene.index.IndexReader;
import edu.mit.lcs.haystack.lucene.index.IndexWriter;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener;
import edu.mit.lcs.haystack.server.core.rdfstore.IRDFStore;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Cholesterol RDF store, version 3 and 4.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Cholesterol3RDFStoreService implements IRDFContainer, IService, IRDFEventSource {
	static {
		String database = System.getProperty("Haystack.Cholesterol.dll");
		if (database == null || database.length() == 0) {
			database = "Cholesterol3";
		}
		
		System.loadLibrary(database);
	}

	final static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Cholesterol3RDFStoreService.class);
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#add(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void add(IRDFContainer c) throws RDFException {
		try {
			ArrayList toAdd = new ArrayList();
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Statement s = (Statement)i.next();
				int n = add(s.getSubject().toString(), s.getPredicate().toString(), s.getObject().toString(), s.getMD5HashResource().toString()); 
				if ((n & 1) != 0) {
					toAdd.add(new Object[] { s, null, (n & 32) != 0 ? s : null });
				}
			}
			if (!toAdd.isEmpty()) {
				i = toAdd.iterator();
				while (i.hasNext()) {
					Object x = i.next();
					synchronized (m_addedStatements) {
						m_addedStatements.add(x);
						m_addedStatements.notifyAll();
					}
				}
			}
		} catch (Exception e) {
			s_logger.error("", e);
			throw new RDFException("", e);
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

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#add(edu.mit.lcs.haystack.rdf.Statement)
	 */
	public void add(Statement s) throws RDFException {
		try {
			int n = add(s.getSubject().toString(), s.getPredicate().toString(), s.getObject().toString(), s.getMD5HashResource().toString()); 
			if ((n & 1) != 0) {
				synchronized (m_addedStatements) {
					m_addedStatements.add(new Object[] { s, null, (n & 32) != 0 ? s : null });
					m_addedStatements.notifyAll();
				}
			}
		} catch (NullPointerException e) {
			s_logger.error("Null in add statement: " + s, e);
			throw new RDFException("", e);
		} catch (Exception e) {
			s_logger.error("", e);
			throw new RDFException("", e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#contains(edu.mit.lcs.haystack.rdf.Statement)
	 */
	public boolean contains(Statement s) throws RDFException {
		try {
			return contains(s.getSubject().toString(), s.getPredicate().toString(), s.getObject().toString());
		} catch (NullPointerException e) {
			s_logger.error("Null in contains statement: " + s, e);
			return false;
		}
	}

	private native boolean contains(String subj, String pred, String obj);

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#extract(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.RDFNode)
	 */
	native public RDFNode extract(
		Resource subject,
		Resource predicate,
		RDFNode object)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthoredStatementIDs(edu.mit.lcs.haystack.rdf.Resource)
	 */
	native public Resource[] getAuthoredStatementIDs(Resource author)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthoredStatements(edu.mit.lcs.haystack.rdf.Resource)
	 */
	native public Statement[] getAuthoredStatements(Resource author)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthors(edu.mit.lcs.haystack.rdf.Resource)
	 */
	native public Resource[] getAuthors(Resource id) throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getAuthors(edu.mit.lcs.haystack.rdf.Statement)
	 */
	native public Resource[] getAuthors(Statement s) throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getStatement(edu.mit.lcs.haystack.rdf.Resource)
	 */
	native public Statement getStatement(Resource id) throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#getStatementID(edu.mit.lcs.haystack.rdf.Statement)
	 */
	public Resource getStatementID(Statement s) throws RDFException {
		return s.getMD5HashResource();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#iterator()
	 */
	public Iterator iterator() throws RDFException {
		// TODO[dquan]: support iterator
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#query(edu.mit.lcs.haystack.rdf.Statement, edu.mit.lcs.haystack.rdf.Resource[])
	 */
	public Set query(Statement s, Resource[] existentials)
		throws RDFException {
		return query(new Statement[] { s }, existentials, existentials);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#query(edu.mit.lcs.haystack.rdf.Statement[], edu.mit.lcs.haystack.rdf.Resource[], edu.mit.lcs.haystack.rdf.Resource[])
	 */
	native public Set query(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#queryExtract(edu.mit.lcs.haystack.rdf.Statement[], edu.mit.lcs.haystack.rdf.Resource[], edu.mit.lcs.haystack.rdf.Resource[])
	 */
	native public RDFNode[] queryExtract(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#queryMulti(edu.mit.lcs.haystack.rdf.Statement, edu.mit.lcs.haystack.rdf.Resource[], edu.mit.lcs.haystack.rdf.RDFNode[][])
	 */
	public Set queryMulti(
		Statement s,
		Resource[] existentials,
		RDFNode[][] hints)
		throws RDFException {
		return queryMulti(new Statement[] { s }, existentials, existentials, hints);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#queryMulti(edu.mit.lcs.haystack.rdf.Statement[], edu.mit.lcs.haystack.rdf.Resource[], edu.mit.lcs.haystack.rdf.Resource[], edu.mit.lcs.haystack.rdf.RDFNode[][])
	 */
	native public Set queryMulti(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials,
		RDFNode[][] hints)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#querySize(edu.mit.lcs.haystack.rdf.Statement[], edu.mit.lcs.haystack.rdf.Resource[], edu.mit.lcs.haystack.rdf.Resource[])
	 */
	native public int querySize(
		Statement[] query,
		Resource[] variables,
		Resource[] existentials)
		throws RDFException;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#remove(edu.mit.lcs.haystack.rdf.Statement, edu.mit.lcs.haystack.rdf.Resource[])
	 */
	native public void remove(Statement pattern, Resource[] existentials)
		throws RDFException;
		
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#replace(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.RDFNode, edu.mit.lcs.haystack.rdf.RDFNode)
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

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#size()
	 */
	public int size() throws RDFException {
		// TODO[dquan]: support size()
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#supportsAuthoring()
	 */
	public boolean supportsAuthoring() {
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.rdf.IRDFContainer#supportsEnumeration()
	 */
	public boolean supportsEnumeration() {
		return false;
	}

	/**
	 * @see IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
		m_done = true;

		while (m_eventThread1.isAlive() || m_eventThread2.isAlive()) {
			try {
				Thread.sleep(100);
				synchronized (m_addedStatements) {
					m_addedStatements.notifyAll();
				}
			} catch (InterruptedException e) {
			}
		}
		
		m_timer.cancel();
	}

	class Listener {
		Resource m_cookie;
		Resource m_subject;
		Resource m_predicate;
		RDFNode m_object;
		Resource m_service;
	}

	class EventThread extends Thread {
		EventThread() {
			setPriority(MIN_PRIORITY);
			setName("Cholesterol3 Event Thread");
		}
		
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
	
				try {
					o = null;
					synchronized (m_addedStatements) {
						while (!m_done) {
							if (!m_addedStatements.isEmpty()) {
								o = (Object[]) m_addedStatements.removeFirst();
								break;
							} else {
								try {
									m_addedStatements.wait();
								} catch (InterruptedException e) {
								}
							}
						}
					}
					
					if (m_done) {
						return;
					}

					if (o != null) {
						statement = (o[0] == null) ? (Statement)o[1] : (Statement)o[0];

						Object subj = statement.getSubject();
						Object pred = statement.getPredicate();
						Object obj = statement.getObject();
								
						if (o[2] != null) {
							indexLiteral(((Literal) obj).getContent());
						}

						HashSet toCall = new HashSet();
								
						Set s;
						synchronized (m_subjectPatterns) {
							s = (Set) m_subjectPatterns.get(subj);
						}
						if (s != null) {
							synchronized (s) {
								toCall.addAll(s);
							}
						}

						synchronized (m_predicatePatterns) {
							s = (Set) m_predicatePatterns.get(pred);
						}
						if (s != null) {
							synchronized (s) {
								toCall.addAll(s);
							}
						}

						synchronized (m_objectPatterns) {
							s = (Set) m_objectPatterns.get(obj);
						}
						if (s != null) {
							synchronized (s) {
								toCall.addAll(s);
							}
						}

						Iterator i = toCall.iterator();
						while (i.hasNext()) {
							Listener listener = (Listener) i.next();
							
							if (m_done) {
								return;
							}

							if (checkStatement(statement, listener)) {
								try {
									IRDFListener rdfl = (IRDFListener) m_manager.connectToService(listener.m_service, null);
									if (rdfl != null) {
										if (o[0] == null) {
											rdfl.statementRemoved(listener.m_cookie, statement);
										} else {
											rdfl.statementAdded(listener.m_cookie, statement);
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
	
			s_logger.info("Event dispatching thread terminated");
		}
	}

	protected ServiceManager	m_manager;
	protected Resource 			m_res;
	protected boolean m_done = false;
	
	protected Map			m_rdfListeners = Collections.synchronizedMap(new HashMap());
	protected Map			m_textListeners = Collections.synchronizedMap(new HashMap());
	protected LinkedList	m_addedStatements = new LinkedList();
	protected Map			m_subjectPatterns = new HashMap();
	protected Map			m_predicatePatterns = new HashMap();
	protected Map			m_objectPatterns = new HashMap();

	protected int m_database;
	protected Thread m_eventThread1;
	protected Thread m_eventThread2;

	public Resource getServiceResource() {
		return m_res;
	}

	private native int add(String subj, String pred, String obj, String id);
	private native boolean addAuthored(String subj, String pred, String obj, String id, Object [] authors);
	public native void setDefrag();

	protected void addToRemoveQueue(String subject, String predicate, String object) {
		String innerObject = object.substring(1, object.length() - 1);
		RDFNode obj = object.charAt(0) == '<' ? (RDFNode)new Resource(innerObject) : new Literal(innerObject);
		Statement s = new Statement(new Resource(subject.substring(1, subject.length() - 1)), new Resource(predicate.substring(1, predicate.length() - 1)), obj);
		synchronized (m_addedStatements) {
			m_addedStatements.addLast(new Object[] { null, s, null });
			m_addedStatements.notifyAll();
		}
	}
	
	protected String m_indexFilename;
	protected Timer m_timer = new Timer();
	protected TimerTask m_literalIndexingTask = new TimerTask() {
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run() {
			LinkedList list = new LinkedList();
			
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			
			synchronized (m_literalsToIndex) {
				list.addAll(m_literalsToIndex);
				m_literalsToIndex.clear();
			}
			
			if (list.isEmpty()) {
				return;
			}
			
			synchronized (m_indexFilename) {
				IndexWriter writer = null;
				try {
					writer = new ForwardIndexWriter("literal",
						m_indexFilename,
						new StopAnalyzer(),
						!IndexReader.indexExists(m_indexFilename));
					writer.mergeFactor = 20;
		
					Iterator i = list.iterator();
					while (i.hasNext()) {
						String s = (String) i.next();
						Document doc = new Document();
						doc.add(Field.Keyword("literal", s));
						doc.add(Field.Text("data", s));
						writer.addDocument(doc);
					}
		
					writer.optimize();
				} catch (IOException e) {
					s_logger.error(
						"An error occurred trying to add literals to index ",
						e);
				} finally {
					try {
						writer.close();
					} catch (Exception e) {}
				}
			}			
		}

	};
	protected LinkedList m_literalsToIndex = new LinkedList(); 
	protected void indexLiteral(String x) {
		synchronized (m_literalsToIndex) {
			m_literalsToIndex.addFirst(x);
		}
	}
	
	public List searchLiterals(String queryString) {
		try {
			synchronized (m_indexFilename) {
				Searcher searcher = new IndexSearcher(m_indexFilename);
				Analyzer analyzer = new StopAnalyzer();
				Query query = QueryParser.parse(queryString, "data", analyzer);
				Hits hits = searcher.search(query);
				List results = new LinkedList();
				for (int i = 0; i < hits.length(); i++) {
					results.add(new Literal(hits.doc(i).get("literal")));
				}
				return results;
			}
		} catch (Exception e) {
			s_logger.error("Could not resolve query" , e);
			return null;
		}
	}

	/**
	 * A simpler debug constructor suitable for debug purposes only.
	 */
	public void debugInit(String basePath) {
		doNativeInit(basePath + "Cholesterol.db");
	}

	native protected void doNativeInit(String basePath);
	native protected void doNativeKill();

	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {

		m_manager = manager;
		m_res = res;
		
		m_indexFilename = basePath + "literalIndex";
		new File(m_indexFilename, "write.lock").delete();
		new File(m_indexFilename, "commit.lock").delete();
		m_timer.scheduleAtFixedRate(m_literalIndexingTask, 0, 20000);

		doNativeInit(basePath + "Cholesterol.db");

		m_eventThread1 = new EventThread();
		m_eventThread1.start();
		m_eventThread2 = new EventThread();
		m_eventThread2.start();

		if (System.getProperty("edu.mit.csail.haystack.postclean", "false").equals("true"))
			setDefrag();
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
		Resource rdfListener,
		Resource subject,
		Resource predicate,
		RDFNode object,
		Resource res)
		throws RDFException {
		Listener l = new Listener();
		l.m_subject = subject;
		l.m_predicate = predicate;
		l.m_object = object;
		l.m_cookie = res;
		l.m_service = rdfListener;
		
		m_rdfListeners.put(res, l);
		
		Set patterns = null;
		if (subject != null) {
			synchronized (m_subjectPatterns) {
				patterns = (Set) m_subjectPatterns.get(subject);
				if (patterns == null) {
					m_subjectPatterns.put(subject, patterns = new HashSet());
				}
			}
			synchronized (patterns) {
				patterns.add(l);	
			}
		} else if (object != null) {
			synchronized (m_objectPatterns) {
				patterns = (Set) m_objectPatterns.get(object);
				if (patterns == null) {
					m_objectPatterns.put(object, patterns = new HashSet());
				}
			}
			synchronized (patterns) {
				patterns.add(l);	
			}
		} else if (predicate != null) {
			synchronized (m_predicatePatterns) {
				patterns = (Set) m_predicatePatterns.get(predicate);
				if (patterns == null) {
					m_predicatePatterns.put(predicate, patterns = new HashSet());
				}
			}
			synchronized (patterns) {
				patterns.add(l);	
			}
		}
	}

	/**
	 * @see IRDFStore#removeRDFListener(String, Resource)
	 */
	public void removeRDFListener(Resource cookie)
		throws RDFException {
		Listener l = (Listener) m_rdfListeners.remove(cookie);
		
		if (l != null) {
			if (l.m_subject != null) {
				Set patterns;
				synchronized (m_subjectPatterns) {
					patterns = (Set) m_subjectPatterns.get(l.m_subject);
				}
				if (patterns != null) {
					synchronized (patterns) {
						patterns.remove(l);
					}
				}
			}
		
			if (l.m_predicate != null) {
				Set patterns;
				synchronized (m_predicatePatterns) {
					patterns = (Set) m_predicatePatterns.get(l.m_predicate);
				}
				if (patterns != null) {
					synchronized (patterns) {
						patterns.remove(l);
					}
				}
			}
		
			if (l.m_object != null) {
				Set patterns;
				synchronized (m_objectPatterns) {
					patterns = (Set) m_objectPatterns.get(l.m_object);
				}
				if (patterns != null) {
					synchronized (patterns) {
						patterns.remove(l);
					}
				}
			}
		}
	}
}
