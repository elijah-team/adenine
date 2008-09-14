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

package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class PredicateSetDataProvider extends ChainedDataProvider {
	protected Resource				m_cookie;

	protected Resource				m_subject;
	protected Resource				m_predicate;
	protected RDFNode				m_object;
	protected boolean				m_objectSpecified = false;
	protected HashSet				m_data = new HashSet();
	protected boolean				m_reverse;
	
	final static Resource SUBJECT		= new Resource(DataConstants.NAMESPACE + "subject");
	final static Resource PREDICATE	= new Resource(DataConstants.NAMESPACE + "predicate");
	final static Resource OBJECT		= new Resource(DataConstants.NAMESPACE + "object");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PredicateSetDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	protected boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource == null) {
			return;
		}

		setupSources(source, context);
		
		m_predicate = Utilities.getResourceProperty(dataSource, PREDICATE, m_partDataSource);
		if (m_predicate == null) {
			s_logger.error("" + dataSource + " has no predicate; ui source " + m_partDataSource + "; source " + m_source);
			return;
		}
		
		Boolean b = (Boolean) context.getLocalProperty(DataConstants.REVERSE);
		if (b != null) {
			m_objectSpecified = m_reverse = b.booleanValue();
		} else {
			m_objectSpecified = m_reverse = Utilities.checkBooleanProperty(dataSource, DataConstants.REVERSE, m_partDataSource);
		}

		internalInitialize(source, context, true);
		
		if (m_dataProvider == null) { // no chained data provider
			m_subject = Utilities.getResourceProperty(dataSource, SUBJECT, m_partDataSource);
			m_object = Utilities.getResourceProperty(dataSource, OBJECT, m_partDataSource);
			if (m_reverse) {
				if (m_object == null) {
					m_object = (Resource) m_context.getLocalProperty(DataConstants.RESOURCE);
				}
			} else {
				if (m_subject == null) {
					m_subject = (Resource) m_context.getLocalProperty(DataConstants.RESOURCE);
				}
				if (m_subject == null && m_object != null) {
					m_objectSpecified = true;
				}
			}
			
			cacheData();
		}
		
		m_initializing = false;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				if (m_reverse) {
					onChainedObjectChanged(newResource);
				} else {
					onChainedSubjectChanged(newResource);
				}
			}
	
			protected void onResourceDeleted(Resource previousResource) {
				if (m_reverse) {
					onChainedObjectRemoved();
				} else {
					onChainedSubjectRemoved();
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		m_subject = null;
		m_predicate = null;
		m_object = null;

		super.dispose();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	synchronized protected void onStatementAdded(Statement s) {
		if (m_objectSpecified) {
			onItemAdded(s.getSubject());
		} else {
			onItemAdded(s.getObject());
		}
	}
	
	synchronized protected void onStatementRemoved(Statement s) {
		if (m_objectSpecified) {
			onItemRemoved(s.getSubject());
		} else {
			onItemRemoved(s.getObject());
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		Ozone.idleExec(new IdleRunnable(10) {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				if (m_context == null) {
					return;
				}	
				
				HashSet oldData = (HashSet) m_data.clone();
				cacheData();
				
				HashSet newlyAdded = new HashSet();
				newlyAdded.addAll(m_data);
				newlyAdded.removeAll(oldData);
				
				HashSet newlyRemoved = new HashSet();
				newlyRemoved.addAll(oldData);
				newlyRemoved.removeAll(m_data);
				
				if (!newlyAdded.isEmpty()) {
					notifyDataConsumers(DataConstants.SET_ADDITION, newlyAdded);
				}

				if (!newlyRemoved.isEmpty()) {
					notifyDataConsumers(DataConstants.SET_REMOVAL, newlyRemoved);
				}
			}
		});
	}

	protected void cacheData() {
		if (m_cookie != null) {
			removePattern(m_cookie);
			m_cookie = null;
		}
		
		Set results = null;
		
		try {
			if (m_objectSpecified) {
				results = m_infoSource.query(
					new Statement(
						Utilities.generateWildcardResource(1),
						m_predicate,
						m_object
					),
					Utilities.generateWildcardResourceArray(1)
				);
				
				m_cookie = addPattern(null, m_predicate, m_object);
			} else if (m_subject != null) {
				results = m_infoSource.query(
					new Statement(
						m_subject,
						m_predicate,
						Utilities.generateWildcardResource(1)
					),
					Utilities.generateWildcardResourceArray(1)
				);
				
				m_cookie = addPattern(m_subject, m_predicate, null);
			}
		} catch (Exception e) {
			s_logger.error("Failed to cache data in predicate set", e);
		}

		if (results != null) {
			Iterator i = results.iterator();
			while (i.hasNext()) {
				m_data.add(((Object[]) i.next())[0]);
			}
		}
	}
	
	synchronized protected void onChainedSubjectChanged(Resource newSubject) {
		if (!newSubject.equals(m_subject)) {
			m_subject = newSubject;
			
			if (m_data.size() > 0) {
				Set oldData = m_data;
				
				m_data = new HashSet();
				
				notifyDataConsumers(DataConstants.SET_CLEAR, oldData);
			}
			
			cacheData();
			
			if (m_data.size() > 0) {
				notifyDataConsumers(DataConstants.SET_ADDITION, new HashSet(m_data));
			}
		}
	}
	
	synchronized protected void onChainedSubjectRemoved() {
		m_subject = null;

		if (m_data.size() > 0) {
			Set oldData = m_data;
			
			m_data = new HashSet();
			
			notifyDataConsumers(DataConstants.SET_CLEAR, oldData);
		}
		
		cacheData();
	}
	
	synchronized protected void onChainedObjectChanged(Resource newSubject) {
		if (!newSubject.equals(m_object)) {
			m_object = newSubject;
			
			if (m_data.size() > 0) {
				Set oldData = m_data;
				
				m_data = new HashSet();
				
				notifyDataConsumers(DataConstants.SET_CLEAR, oldData);
			}
			
			cacheData();
			
			if (m_data.size() > 0) {
				notifyDataConsumers(DataConstants.SET_ADDITION, new HashSet(m_data));
			}
		}
	}
	
	synchronized protected void onChainedObjectRemoved() {
		m_object = null;

		if (m_data.size() > 0) {
			Set oldData = m_data;
			
			m_data = new HashSet();
			
			notifyDataConsumers(DataConstants.SET_CLEAR, oldData);
		}
		
		cacheData();
	}
	
	synchronized protected void onItemAdded(RDFNode newItem) {
		if (m_data.add(newItem)) {
			HashSet items = new HashSet();
			
			items.add(newItem);
			
			notifyDataConsumers(DataConstants.SET_ADDITION, items);
		}
	}
	
	synchronized protected void onItemRemoved(RDFNode oldItem) {
		if (m_data.remove(oldItem)) {
			HashSet items = new HashSet();
			
			items.add(oldItem);
			
			notifyDataConsumers(DataConstants.SET_REMOVAL, items);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (dataType.equals(DataConstants.SET)) {
			return new HashSet(m_data);
		} else if (dataType.equals(DataConstants.SET_COUNT)) {
			return new Integer(m_data.size());
		} else if (dataType.equals(DataConstants.RESOURCE)) {
			if (m_objectSpecified) {
				return m_object;
			} else {
				return m_subject;
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_data.size() > 0) {
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, new HashSet(m_data));
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (changeType.equals(DataConstants.SET_ADDITION)) {
			if (change instanceof Set) {
				Set 		set = (Set) change;
				Iterator	i = set.iterator();
				
				if (m_objectSpecified) {
					while (i.hasNext()) {
						try {
							Resource subject = (Resource) i.next();
							
							m_infoSource.add(new Statement(subject, m_predicate, m_object));
						} catch (RDFException e) {
							s_logger.error("Failed to perform set additions", e);
						}
					}
				} else if (m_subject != null) {
					while (i.hasNext()) {
						try {
							RDFNode object = (RDFNode) i.next();
							
							m_infoSource.add(new Statement(m_subject, m_predicate, object));
						} catch (RDFException e) {
							s_logger.error("Failed to perform set additions", e);
						}
					}
				}
			} else if (change instanceof RDFNode) {
				if (m_objectSpecified) {
					try {
						m_infoSource.add(new Statement((Resource) change, m_predicate, m_object));
					} catch (RDFException e) {
						s_logger.error("Failed to perform set addition", e);
					}
				} else if (m_subject != null) {
					try {
						m_infoSource.add(new Statement(m_subject, m_predicate, (RDFNode) change));
					} catch (RDFException e) {
						s_logger.error("Failed to perform set addition", e);
					}
				}
			} else {
				throw new DataMismatchException("Change data type mismatch: expecting java.util.Set instead of " + change.getClass());
			}
		} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
			if (change instanceof Set) {
				Set 		set = (Set) change;
				Iterator	i = set.iterator();
				
				if (m_objectSpecified) {
					while (i.hasNext()) {
						try {
							Resource subject = (Resource) i.next();
							
							m_infoSource.remove(new Statement(subject, m_predicate, m_object), new Resource[]{});
						} catch (RDFException e) {
							s_logger.error("Failed to perform set removals", e);
						}
					}
				} else if (m_subject != null) {
					while (i.hasNext()) {
						try {
							RDFNode object = (RDFNode) i.next();
							
							m_infoSource.remove(new Statement(m_subject, m_predicate, object), new Resource[]{});
						} catch (RDFException e) {
							s_logger.error("Failed to perform set removals", e);
						}
					}
				}
			} else if (change instanceof RDFNode) {
				if (m_objectSpecified) {
					try {
						m_infoSource.remove(new Statement((Resource) change, m_predicate, m_object), new Resource[]{});
					} catch (RDFException e) {
						s_logger.error("Failed to perform set removal", e);
					}
				} else if (m_subject != null) {
					try {
						m_infoSource.remove(new Statement(m_subject, m_predicate, (RDFNode) change), new Resource[]{});
					} catch (RDFException e) {
						s_logger.error("Failed to perform set removal", e);
					}
				}
			} else {
				throw new DataMismatchException("Change data type mismatch: expecting java.util.Set instead of " + change.getClass());
			}
		} else if (changeType.equals(DataConstants.SET_CLEAR)) {
			if (m_objectSpecified) {
				try {
					m_infoSource.remove(
						new Statement(Utilities.generateWildcardResource(1), m_predicate, m_object), 
						Utilities.generateWildcardResourceArray(1)
					);
				} catch (RDFException e) {
					s_logger.error("Failed to perform set clear", e);
				}
			} else if (m_subject != null) {
				try {
					m_infoSource.remove(
						new Statement(m_subject, m_predicate, Utilities.generateWildcardResource(1)), 
						Utilities.generateWildcardResourceArray(1)
					);
				} catch (RDFException e) {
					s_logger.error("Failed to perform set clear", e);
				}
			}
		} else {
			throw new UnsupportedOperationException("Data provider does not support change to type " + changeType);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return 
			changeType.equals(DataConstants.SET_ADDITION) ||
			changeType.equals(DataConstants.SET_REMOVAL) ||
			changeType.equals(DataConstants.SET_CLEAR);
	}
}
