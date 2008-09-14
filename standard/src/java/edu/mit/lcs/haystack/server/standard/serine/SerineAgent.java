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

package edu.mit.lcs.haystack.server.standard.serine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.adenine.query.DefaultQueryEngine;
//import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
//import edu.mit.lcs.haystack.server.standard.melatonin.MelatoninAgent;
import edu.mit.lcs.haystack.server.standard.scheduler.IScheduledTask;

/**
 * Serine Agent.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class SerineAgent extends GenericService implements ISerineAgent, IRDFListener, IScheduledTask {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SerineAgent.class);
	
	protected HashMap m_monitoredPatterns;
	protected HashMap m_monitorCookies;
	protected boolean m_done = false;
	protected LinkedList m_todo;
	protected LinkedList m_priorityTodo;
	protected SerineThread m_thread = null;
	protected TransformationThread m_thread2 = null;
	protected TransformationThread m_thread3 = null;
	protected IRDFContainer m_source;
	protected Timer m_timer;
	
	/**
	 * @see IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
		m_done = true;

		IRDFEventSource rdfes = (IRDFEventSource) m_infoSource;
		synchronized (m_monitorCookies) {
			Iterator l = m_monitorCookies.keySet().iterator();
			while (l.hasNext()) {
				Resource cookie = (Resource) l.next();
				try { rdfes.removeRDFListener(cookie); }
				catch (RDFException e) { HaystackException.uncaught(e); }
			}
			m_monitorCookies.clear();
		}
		
		m_timer.cancel();
		m_thread2.interrupt();
		m_thread3.interrupt();
		while (/*m_thread.isAlive() ||*/ m_thread2.isAlive() || m_thread3.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	protected boolean m_paused = false;
	
	synchronized public void pause() throws ServiceException {
		if (!m_paused) {
			cleanup();
			m_paused = true;
		}
	}
	
	synchronized public void resume() throws ServiceException {
		if (m_paused) {
			m_paused = false;
			startup();
		}
	}

	class Transformation {
		RDFNode[] m_data;
		Resource m_pattern;
		Resource[] m_existentials;
		Statement[] m_resultStatements;

		Transformation(Resource pattern, RDFNode[] data, Resource[] existentials, Statement[] resultStatements) {
			m_pattern = pattern;
			m_data = data;
			m_existentials = existentials;
			m_resultStatements = resultStatements;
		}
		
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Transformation)) {
				return false;
			}
			
			Transformation t = (Transformation)o;
			return t.m_pattern.equals(m_pattern);
		}
		
		void doTransform(Interpreter interpreter) {
			s_logger.info("Performing transform " + m_pattern);
			
			// Add result statements, if any
			for (int j = 0; j < m_resultStatements.length; j++) {
				Statement s = m_resultStatements[j];
				Resource subject, predicate;
				RDFNode object;

				int index = Utilities.indexOfResource(m_existentials, s.getSubject());
				if (index >= 0) {
					subject = (Resource)m_data[index];
				} else {
					subject = s.getSubject();
				}

				index = Utilities.indexOfResource(m_existentials, s.getPredicate());
				if (index >= 0) {
					predicate = (Resource)m_data[index];
				} else {
					predicate = s.getPredicate();
				}

				index = Utilities.indexOfResource(m_existentials, s.getObject());
				if (index >= 0) {
					object = m_data[index];
				} else {
					object = s.getObject();
				}
				
				try {
					m_infoSource.add(new Statement(subject, predicate, object));
				} catch (RDFException rdfe) {
				}
			}
											
			Resource method = Utilities.getResourceProperty(m_pattern, SerineConstants.adenineMethod, m_source);
			if (method != null) {
				try {
					Message msg = new Message(m_data);
					msg.setNamedValue(SerineConstants.transformation, m_pattern);
					DynamicEnvironment denv = new DynamicEnvironment(m_source, m_serviceManager);
					denv.setValue("__infosource__", m_infoSource);

					try {
						// Doesn't work if Ozone isn't loaded
//						denv.setValue("__context__", Ozone.s_context);
					} catch (Exception e) {}
					
					if (m_userResource != null) {
						denv.setIdentity(m_serviceManager.getIdentityManager().getUnauthenticatedIdentity(m_userResource));
					}
					Object o = interpreter.callMethod(method, msg, denv);
				} catch (AdenineException e) {
					s_logger.error("An error occurred processing transformation " + m_pattern, e);
				}
			}
		}
	}
	
	class MonitoredTransformation extends Transformation {
		/**
		 * Constructor for MonitoredTransformation.
		 * @param pattern
		 * @param queryStatements
		 * @param existentials
		 * @param resultStatements
		 */
		public MonitoredTransformation(
			Resource pattern,
			Statement[] queryStatements,
			Resource[] existentials,
			Statement[] resultStatements,
			boolean priority,
			ArrayList cookies) {
			super(pattern, null, existentials, resultStatements);
			m_queryStatements = queryStatements;
			m_priority = priority;
			m_cookies = cookies;
		}
		
		ArrayList m_cookies;
		Statement[] m_queryStatements;
		boolean m_priority;
		TimerTask m_delayedResponseTask = null;
		
		void delayedProcess() {
			synchronized (this) {
				if (m_delayedResponseTask != null) {
					try {
						m_delayedResponseTask.cancel();
					} catch (Exception e) {}
				}
			
				m_delayedResponseTask = new TimerTask() {
					/* (non-Javadoc)
					 * @see java.util.TimerTask#run()
					 */
					public void run() {
						try {
							MonitoredTransformation t = MonitoredTransformation.this;
							/*RDFNode[][] data = new RDFNode[t.m_existentials.length][];
							int statementIndex = t.m_cookies.indexOf(cookie);
							Statement s0 = t.m_queryStatements[statementIndex];
							for (int i = 0; i < t.m_existentials.length; i++) {
								data[i] = new RDFNode[0]; 
							}
					
							int sIndex = Utilities.indexOfResource(t.m_existentials, s0.getSubject());
							if (sIndex != -1) {
								data[sIndex] = new RDFNode[] { s.getSubject() };
							}
							int pIndex = Utilities.indexOfResource(t.m_existentials, s0.getPredicate());
							if (pIndex != -1) {
								data[pIndex] = new RDFNode[] { s.getPredicate() };
							}
							int oIndex = Utilities.indexOfResource(t.m_existentials, s0.getObject());
							if (oIndex != -1) {
								data[oIndex] = new RDFNode[] { s.getObject() };
							}
					
							Set results = m_infoSource.queryMulti(t.m_queryStatements, t.m_existentials, t.m_existentials, data);
							if (results == null) {*/ Set
								results = m_infoSource.query(t.m_queryStatements, t.m_existentials, t.m_existentials);
							//}
							Iterator k = results.iterator();
					
							while (k.hasNext()) {
								RDFNode[] datum = (RDFNode[]) k.next();
								//TransformationJob t2 = new TransformationJob(getServiceResource(), t.m_pattern, datum, t.m_existentials, t.m_resultStatements);
								//MelatoninAgent.getMelatoninAgent(m_source, m_serviceManager).submitJob(t2, null, t.m_priority);
								/*Transformation t2 = new Transformation(t.m_pattern, , t.m_existentials, t.m_resultStatements);
								if (t.m_priority) {
									synchronized (m_priorityTodo) {
										m_priorityTodo.add(t2);
									}
								} else {
									synchronized (m_todo) {
										m_todo.add(t2);
									}
								}*/
							}
						} catch (RDFException e) {
							s_logger.error("RDF error", e);
						}
					}
				};
				
				try { m_timer.schedule(m_delayedResponseTask, m_priority ? 2500 : 30000); }
				catch (Exception e) { HaystackException.uncaught(e); }
			} 
		}
	}
	
	class TransformationThread extends Thread {
		LinkedList m_todo;
		int m_wait;
		
		TransformationThread(ThreadGroup tg, LinkedList todo, int wait) {
			super(tg, tg.getName() + "-worker");
			setPriority(Thread.MIN_PRIORITY);
			this.m_todo = todo;
			m_wait = wait;
			start();
		}
		
		public void run() {
			Interpreter interpreter = new Interpreter(m_source);
			int c = 0;
			while (!m_done) {
				Transformation t = null;
				synchronized (m_todo) {
					if (!m_todo.isEmpty()) {
						t = (Transformation)m_todo.removeFirst();
					}
				}
				if (t == null) {
					try {
						sleep(m_wait);
					} catch (InterruptedException e) {
					}
					continue;
				}

				t.doTransform(interpreter);
			
				++c;
				if (c >= 100) {
					try {
						sleep(m_wait);
					} catch (InterruptedException e) {
					}
					c = 0;
				}
			}
		}
		
	}
	
	/**
	 * Informs Serine to process patterns with the given subject immediately.
	 * @param res
	 */
	public void processSubjectImmediately(Resource subject) {
	}

	protected void _processSubjectImmediately(Resource subject) {
		if (subject == null) {
			return;
		}
		new Thread() {
			public void run() {
				// Get a list of all transformations first
				Resource[] transforms = Utilities.getResourceSubjects(Constants.s_rdf_type, SerineConstants.Transformation, m_source);
				for (int i = 0; i < transforms.length; i++) {
					Resource transform = transforms[i];
					Resource precond = Utilities.getResourceProperty(transform, SerineConstants.precondition, m_source);
					
					if (precond != null) {
						// Retrieve existentials
						Iterator k = ListUtilities.accessDAMLList(Utilities.getResourceProperty(precond, SerineConstants.existentials, m_source), m_source);
						ArrayList al = new ArrayList();
						while (k.hasNext()) {
							Object o = k.next();
							al.add(o);
						}
						Resource[] existentials = new Resource[al.size()];
						al.toArray(existentials);
						
						// TODO[dquan]: support Adenine queries
						Resource pattern = Utilities.getResourceProperty(precond, SerineConstants.pattern, m_source);
						Statement[] statements = null;
						if (pattern != null) {
							continue;
						}
		/*				// Retrieve Adenine query pattern, if any
						ConditionSet cs = null;
						if (pattern != null) {
							cs = new ConditionSet(pattern, m_source);
						} else*/ {
							// Retrieve pattern statements
							Resource[] statementResources = Utilities.getResourceProperties(precond, SerineConstants.statement, m_source);
							statements = new Statement[statementResources.length];
							for (int j = 0; j < statements.length; j++) {
								Resource r = statementResources[j];
								statements[j] = new Statement(Utilities.getResourceProperty(r, SerineConstants.subject, m_source),
									Utilities.getResourceProperty(r, SerineConstants.predicate, m_source),
									Utilities.getProperty(r, SerineConstants.object, m_source));
							}
						}
						
						// Perform query
						try {
							Set results;
		/*					if (cs != null) {
								results = new DefaultQueryEngine().query(new DynamicEnvironment(m_source, m_serviceManager), cs, true, existentials, existentials);
							} else*/ {
								// See if any of the query statements is satisfied with the given subject
								boolean found = false;
								for (int l = 0; l < statements.length; l++) {
									Statement s = statements[l];
									if (Utilities.containsResource(existentials, s.getSubject())) {
										try {
											if (!Utilities.containsResource(existentials, s.getPredicate()) && !Utilities.containsResource(existentials, s.getObject())) {
												if (m_infoSource.contains(new Statement(subject, s.getPredicate(), s.getObject()))) {
													found = true;
												}
											} else {
												RDFNode[] results0 = m_infoSource.queryExtract(new Statement[] { new Statement(subject, s.getPredicate(), s.getObject()) }, existentials, existentials);
												if (results0 != null) {
													found = true;
												}							
											}
										} catch (RDFException rdfe) {
											s_logger.warn("Unexpected RDF error in processSubjectImmediately", rdfe);
										}
									}
								}
								if (!found) {
									continue;
								}
								
								results = m_infoSource.query(statements, existentials, existentials);
							}
		
							Statement[] resultStatements = getStatements(transform, SerineConstants.resultStatement);
							boolean priority = Utilities.checkBooleanProperty(transform, SerineConstants.priority, m_source);
							
							k = results.iterator();
							
							if (priority) {
								while (k.hasNext()) {
									Transformation t = new Transformation(transform, (RDFNode[])k.next(), existentials, resultStatements);
									synchronized (m_priorityTodo) {
										m_priorityTodo.add(0, t);
									}
								}
							} else {
								while (k.hasNext()) {
									Transformation t = new Transformation(transform, (RDFNode[])k.next(), existentials, resultStatements);
									synchronized (m_todo) {
										m_todo.add(0, t);
									}
								}
							}
						} catch (Exception e) {
							s_logger.error("Error", e);
						}
					}
				}
			}
			
			void init(Resource subject) {
				this.subject = subject;
				start();
			}
			
			Resource subject;
		}.init(subject);
	}

	class SerineThread extends Thread {
		SerineThread(ThreadGroup tg) {
			super(tg, tg.getName() + "-main");
			setPriority(Thread.MIN_PRIORITY);		
			start();
		}
		
		public void run() {
			while (!m_done) {
				int c, c2;
				synchronized (m_todo) {
					c = m_todo.size();
				}
				
				synchronized (m_priorityTodo) {
					c2 = m_priorityTodo.size();
				}
				
				if ((c < 100) || (c2 < 100)) {
					// Get a list of all transformations first
					Resource[] transforms = Utilities.getResourceSubjects(Constants.s_rdf_type, SerineConstants.Transformation, m_source);
					for (int i = 0; i < transforms.length; i++) {
						Resource transform = transforms[i];
						
						if (Utilities.checkBooleanProperty(transform, SerineConstants.runOnIdle, m_source)) {
							continue;
						}
						
						// Do not handle monitored events
						synchronized (m_monitoredPatterns) {
							if (m_monitoredPatterns.containsKey(transform)) {
/*								if (m_precheckBeforeMonitor.contains(transform)) {
									m_precheckBeforeMonitor.remove(transform);
								} else {
									continue;
								}*/
							}
						}
						
						Resource precond = Utilities.getResourceProperty(transform, SerineConstants.precondition, m_source);
						//Resource postcond = Utilities.getResourceProperty(transform, SerineConstants.postcondition, m_source);
						
						if (precond != null) {
							s_logger.info("Performing unmonitorable pattern " + transform);

							// Retrieve existentials
							Iterator k = ListUtilities.accessDAMLList(Utilities.getResourceProperty(precond, SerineConstants.existentials, m_source), m_source);
							ArrayList al = new ArrayList();
							while (k.hasNext()) {
								Object o = k.next();
								al.add(o);
							}
							Resource[] existentials = new Resource[al.size()];
							al.toArray(existentials);
							
							// Retrieve Adenine query pattern, if any
							Resource pattern = Utilities.getResourceProperty(precond, SerineConstants.pattern, m_source);
							ConditionSet cs = null;
							Statement[] statements = null;
							if (pattern != null) {
								cs = new ConditionSet(pattern, m_source);
							} else {
								// Retrieve pattern statements
								statements = getStatements(precond, SerineConstants.statement);
							}
							
							// Perform query
							try {
								Set results;
								if (cs != null) {
									results = new DefaultQueryEngine().query(new DynamicEnvironment(m_source, m_serviceManager), cs, true, existentials, existentials);
								} else {
									results = m_infoSource.query(statements, existentials, existentials);
								}

								Statement[] resultStatements = getStatements(transform, SerineConstants.resultStatement);
								boolean priority = Utilities.checkBooleanProperty(transform, SerineConstants.priority, m_source);
								
								k = results.iterator();
								
								if (priority) {
									while (k.hasNext()) {
										Transformation t = new Transformation(transform, (RDFNode[])k.next(), existentials, resultStatements);
										synchronized (m_priorityTodo) {
											m_priorityTodo.add(t);
										}
									}
								} else {
									while (k.hasNext()) {
										Transformation t = new Transformation(transform, (RDFNode[])k.next(), existentials, resultStatements);
										synchronized (m_todo) {
											m_todo.add(t);
										}
									}
								}
							} catch (Exception e) {
								s_logger.error("Error", e);
							}
						}
					}
				}
									
				try {
					sleep(5000);
				} catch (InterruptedException ie) {
				}
			}
		}
	}
	
	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
		super.init(basePath, manager, res);
		m_source = manager.getRootRDFContainer();
		
		startup();
	}

	protected void startup() {
		m_monitoredPatterns = new HashMap();
		m_monitorCookies = new HashMap();
		m_todo = new LinkedList();
		m_priorityTodo = new LinkedList();
		m_timer = new Timer();

		m_done = false;
		ThreadGroup tg = new ThreadGroup("Serine Agent " + m_serviceResource);
//		m_thread = new SerineThread(tg);
		m_thread2 = new TransformationThread(tg, m_todo, 60000);
		m_thread3 = new TransformationThread(tg, m_priorityTodo, 5000);
		
		m_timer.schedule(new TimerTask() {
			/**
			 * @see java.util.TimerTask#run()
			 */
			public void run() {
				refreshMonitorList(/*true*/false);
			}
		}, 0);

		m_timer.schedule(new TimerTask() {
			/**
			 * @see java.util.TimerTask#run()
			 */
			public void run() {
				refreshMonitorList(false);
			}
		}, 60 * 1000 * 15, 60 * 1000 * 15);
	}

	void refreshMonitorList(boolean firstRun) {
		s_logger.info("Refreshing patterns list");
		try {
			if (!(m_source instanceof IRDFEventSource)) {
				// If events are not supported, stop here
				return;
			}
			
			IRDFEventSource rdfes = (IRDFEventSource)m_infoSource;
			
			HashMap monitoredPatterns = new HashMap();
			HashMap monitorCookies = new HashMap();
			
			// Now, search for suitable patterns
			Resource[] transforms = Utilities.getResourceSubjects(Constants.s_rdf_type, SerineConstants.Transformation, m_source);
transform_loop:	
			for (int i = 0; i < transforms.length; i++) {
				Resource transform = transforms[i];
				
				if (Utilities.checkBooleanProperty(transform, SerineConstants.runOnIdle, m_source)) {
					continue;
				}
				
				boolean priority = Utilities.checkBooleanProperty(transform, SerineConstants.priority, m_source);
				Resource precond = Utilities.getResourceProperty(transform, SerineConstants.precondition, m_source);
				
				if (precond != null) {
					// Adenine query patterns not supported here
					Resource pattern = Utilities.getResourceProperty(precond, SerineConstants.pattern, m_source);
					if (pattern != null) {
						continue;
					}
					
					// Retrieve existentials
					Iterator k = ListUtilities.accessDAMLList(Utilities.getResourceProperty(precond, SerineConstants.existentials, m_source), m_source);
					ArrayList existentials = new ArrayList();
					while (k.hasNext()) {
						Object o = k.next();
						existentials.add(o);
					}
					
					// Retrieve pattern statements
					Resource[] statementResources = Utilities.getResourceProperties(precond, SerineConstants.statement, m_source);
					ArrayList statements = new ArrayList();
					ArrayList cookies = new ArrayList();
					for (int j = 0; j < statementResources.length; j++) {
						Resource r = statementResources[j];
						Resource subject = Utilities.getResourceProperty(r, SerineConstants.subject, m_source);
						Resource predicate = Utilities.getResourceProperty(r, SerineConstants.predicate, m_source);
						RDFNode object = Utilities.getProperty(r, SerineConstants.object, m_source);
						
						if (subject == null || predicate == null || object == null) {
							s_logger.warn("Statement resource " + r + " has a missing component; skipping it");
							continue;
						}

						boolean subjectX = existentials.contains(subject);
						boolean predicateX = existentials.contains(predicate);
						boolean objectX = existentials.contains(object);
						
						if (subjectX && objectX && !predicateX && Constants.s_rdf_type.equals(predicate)) {
							continue transform_loop;
						}
		
						// Set up notification
						Resource cookie = Utilities.generateUniqueResource();
						synchronized (m_monitorCookies) {						
							rdfes.addRDFListener(m_serviceResource, 
								subjectX ? null : subject,
								predicateX ? null : predicate,
								objectX ? null : object, cookie);
							m_monitorCookies.put(cookie, transform);
						}
						monitorCookies.put(cookie, transform);
						statements.add(new Statement(subject, predicate, object));
						cookies.add(cookie);
					}
		
					Resource[] existentials2 = new Resource[existentials.size()];
					existentials.toArray(existentials2);
					Statement[] queryStatements = new Statement[statements.size()];
					statements.toArray(queryStatements);
					Statement[] resultStatements = getStatements(transform, SerineConstants.resultStatement);
					MonitoredTransformation t = new MonitoredTransformation(transform, queryStatements, existentials2, resultStatements, priority, cookies);
					monitoredPatterns.put(transform, t);
					
					if (!firstRun) {
						continue;
					}

					// Perform query
					try {
						Set results;
						results = m_infoSource.query(queryStatements, existentials2, existentials2);

						k = results.iterator();

						if (priority) {
							while (k.hasNext()) {
								Transformation t2 = new Transformation(transform, (RDFNode[])k.next(), existentials2, resultStatements);
								synchronized (m_priorityTodo) {
									m_priorityTodo.add(t2);
								}
							}
						} else {
							while (k.hasNext()) {
								Transformation t2 = new Transformation(transform, (RDFNode[])k.next(), existentials2, resultStatements);
								synchronized (m_todo) {
									m_todo.add(t2);
								}
							}
						}
					} catch (Exception e) {
						s_logger.error("Error", e);
					}
				}
			}					

			// Convert over
			synchronized (m_monitoredPatterns) {
				m_monitoredPatterns.clear();
				m_monitoredPatterns.putAll(monitoredPatterns);
			}				

			synchronized (m_monitorCookies) {
				Iterator l = m_monitorCookies.keySet().iterator();
				while (l.hasNext()) {
					Resource cookie = (Resource) l.next();
					if (!monitorCookies.containsKey(cookie)) {
						rdfes.removeRDFListener(cookie);
					}
				}
				m_monitorCookies.clear();
				m_monitorCookies.putAll(monitorCookies);
			}
			
		} catch (RDFException e) {
			s_logger.error("RDF error", e);
		} finally {
			s_logger.info("Finished refreshing patterns list");
		}
	}

	Statement[] getStatements(Resource base, Resource property) {
		Resource[] statementResources = Utilities.getResourceProperties(base, SerineConstants.resultStatement, m_source);
		HashSet set = new HashSet();
		for (int j = 0; j < statementResources.length; j++) {
			Resource r = statementResources[j];
			Resource subject = Utilities.getResourceProperty(r, SerineConstants.subject, m_source);
			Resource predicate = Utilities.getResourceProperty(r, SerineConstants.predicate, m_source);
			RDFNode object = Utilities.getProperty(r, SerineConstants.object, m_source);
			if (subject == null || predicate == null || object == null) {
				s_logger.warn("Statement resource " + r + " has a missing component; skipping it");
				continue;
			}
			set.add(new Statement(subject, predicate, object));
		}
		Statement[] resultStatements = new Statement[set.size()];
		set.toArray(resultStatements);
		return resultStatements;
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener#statementAdded(Resource, Statement)
	 */
	public void statementAdded(Resource cookie, Statement s) {
		Resource pattern = null;
		
		synchronized (m_monitorCookies) {
			pattern = (Resource)m_monitorCookies.get(cookie);
		}
		
		if (pattern == null) {
			return;
		}
		
		synchronized (m_monitoredPatterns) {
			MonitoredTransformation t = (MonitoredTransformation) m_monitoredPatterns.get(pattern);
		
			if (t == null) {
				return;
			}
			
			if (t.m_queryStatements.length == 1) {
				RDFNode[] datum = new RDFNode[t.m_existentials.length];
				Statement s0 = t.m_queryStatements[0];
				int sIndex = Utilities.indexOfResource(t.m_existentials, s0.getSubject());
				if (sIndex != -1) {
					datum[sIndex] = s.getSubject();
				}
				int pIndex = Utilities.indexOfResource(t.m_existentials, s0.getPredicate());
				if (pIndex != -1) {
					datum[pIndex] = s.getPredicate();
				}
				int oIndex = Utilities.indexOfResource(t.m_existentials, s0.getObject());
				if (oIndex != -1) {
					datum[oIndex] = s.getObject();
				}
				/*Transformation t2 = new Transformation(t.m_pattern, datum, t.m_existentials, t.m_resultStatements);
				if (t.m_priority) {
					synchronized (m_priorityTodo) {
						m_priorityTodo.add(t2);
					}
				} else {
					synchronized (m_todo) {
						m_todo.add(t2);
					}
				}*/
				//TransformationJob t2 = new TransformationJob(getServiceResource(), t.m_pattern, datum, t.m_existentials, t.m_resultStatements);
				//MelatoninAgent.getMelatoninAgent(m_source, m_serviceManager).submitJob(t2, null, t.m_priority);
			} else {
				t.delayedProcess();
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener#statementRemoved(Resource, Statement)
	 */
	public void statementRemoved(Resource cookie, Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.server.standard.serine.ISerineAgent#performIdleTransforms()
	 */
	synchronized public void performIdleTransforms() {
		// Get a list of all transformations first
		Resource[] transforms = Utilities.getResourceSubjects(Constants.s_rdf_type, SerineConstants.Transformation, m_source);
		for (int i = 0; i < transforms.length; i++) {
			Resource transform = transforms[i];
			
			if (!Utilities.checkBooleanProperty(transform, SerineConstants.runOnIdle, m_source)) {
				continue;
			}
			
			// Do not handle monitored events
			synchronized (m_monitoredPatterns) {
				if (m_monitoredPatterns.containsKey(transform)) {
/*								if (m_precheckBeforeMonitor.contains(transform)) {
									m_precheckBeforeMonitor.remove(transform);
								} else {
									continue;
								}*/
				}
			}
			
			Resource precond = Utilities.getResourceProperty(transform, SerineConstants.precondition, m_source);
			//Resource postcond = Utilities.getResourceProperty(transform, SerineConstants.postcondition, m_source);
			
			if (precond != null) {
				s_logger.info("Performing unmonitorable pattern " + transform);
	
				// Retrieve existentials
				Iterator k = ListUtilities.accessDAMLList(Utilities.getResourceProperty(precond, SerineConstants.existentials, m_source), m_source);
				ArrayList al = new ArrayList();
				while (k.hasNext()) {
					Object o = k.next();
					al.add(o);
				}
				Resource[] existentials = new Resource[al.size()];
				al.toArray(existentials);
				
				// Retrieve Adenine query pattern, if any
				Resource pattern = Utilities.getResourceProperty(precond, SerineConstants.pattern, m_source);
				ConditionSet cs = null;
				Statement[] statements = null;
				if (pattern != null) {
					cs = new ConditionSet(pattern, m_source);
				} else {
					// Retrieve pattern statements
					statements = getStatements(precond, SerineConstants.statement);
				}
				
				// Perform query
				try {
					Set results;
					if (cs != null) {
						results = new DefaultQueryEngine().query(new DynamicEnvironment(m_source, m_serviceManager), cs, true, existentials, existentials);
					} else {
						results = m_infoSource.query(statements, existentials, existentials);
					}
	
					Statement[] resultStatements = getStatements(transform, SerineConstants.resultStatement);
					k = results.iterator();
					
					while (k.hasNext()) {
						Transformation t = new Transformation(transform, (RDFNode[]) k.next(), existentials, resultStatements);
						synchronized (m_priorityTodo) {
							m_priorityTodo.add(t);
						}
					}
				} catch (Exception e) {
					s_logger.error("Error", e);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.scheduler.IScheduledTask#performScheduledTask(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public void performScheduledTask(Resource resTask) throws ServiceException {
		performIdleTransforms();
	}
}
