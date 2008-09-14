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

/**
 * @author David Huynh
 */
abstract public class PredicatePropertyDataProvider extends ChainedDataProvider {
	protected Resource				m_cookie;

	protected Resource				m_subject;
	protected Resource				m_predicate;
	protected RDFNode				m_object;
	protected boolean				m_reverse;
	protected boolean				m_disposed = false;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PredicatePropertyDataProvider.class);
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		Ozone.idleExec(new IdleRunnable(10) {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				if (!m_disposed) {
					cacheData();
				}
			}
		});
	}
	
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
		
		m_predicate = Utilities.getResourceProperty(dataSource, DataConstants.PREDICATE, m_partDataSource);
		if (m_predicate == null) {
			return;
		}
		
		m_reverse = Utilities.checkBooleanProperty(dataSource, DataConstants.REVERSE, m_partDataSource);
		
		internalInitialize(source, context, true);
		
		if (m_dataProvider == null) {
			if (m_reverse) {
				m_object = Utilities.getResourceProperty(dataSource, DataConstants.OBJECT, m_partDataSource);
				if (m_object == null) {
					m_object = (Resource) m_context.getLocalProperty(DataConstants.RESOURCE);
				}
			} else {
				m_subject = Utilities.getResourceProperty(dataSource, DataConstants.SUBJECT, m_partDataSource);
				if (m_subject == null) {
					m_subject = (Resource) m_context.getLocalProperty(DataConstants.RESOURCE);
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
					onChainedObjectDeleted();
				} else {
					onChainedSubjectDeleted();
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		m_disposed = true;
		
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

	synchronized protected void onChainedSubjectChanged(Resource newSubject) {
		if (!newSubject.equals(m_subject)) {
			m_subject = newSubject;
			cacheData();
		}
	}
	
	synchronized protected void onChainedSubjectDeleted() {
		m_subject = null;
		cacheData();
	}
	
	synchronized protected void onChainedObjectChanged(Resource newObject) {
		if (!newObject.equals(m_object)) {
			m_object = newObject;
			cacheData();
		}
	}
	
	synchronized protected void onChainedObjectDeleted() {
		m_object = null;
		cacheData();
	}
	
	synchronized protected void onStatementAdded(Statement s) {
		RDFNode oldProperty = m_reverse ? m_subject : m_object;
		
		if (m_reverse) {
			m_subject = s.getSubject();
	
			if (m_subject == null) {
				if (oldProperty != null) {
					onPropertyDeleted(oldProperty);
				}
			} else if (!m_subject.equals(oldProperty)) {
				onPropertyChanged(oldProperty);
			}
		} else {
			m_object = s.getObject();

			if (m_object == null) {
				if (oldProperty != null) {
					onPropertyDeleted(oldProperty);
				}
			} else if (!m_object.equals(oldProperty)) {
				onPropertyChanged(oldProperty);
			}
		}
	}
	
	synchronized protected void onStatementRemoved(Statement s) {
		RDFNode oldProperty = m_reverse ? m_subject : m_object;
		
		if (m_reverse) {
			m_subject = getSubject();
			if (m_subject == null) {
				if (oldProperty != null) {
					onPropertyDeleted(oldProperty);
				}
			} else if (!m_subject.equals(oldProperty)) {
				onPropertyChanged(oldProperty);
			}
		} else {
			m_object = getObject();
			if (m_object == null) {
				if (oldProperty != null) {
					onPropertyDeleted(oldProperty);
				}
			} else if (!m_object.equals(oldProperty)) {
				onPropertyChanged(oldProperty);
			}
		}
	}
	
	protected void cacheData() {
		if (m_cookie != null) {
			removePattern(m_cookie);
			m_cookie = null;
		}
		
		if (m_reverse) {
			RDFNode oldSubject = m_subject;
			m_subject = null;
			
			if (m_object != null) {
				m_subject = getSubject();
				try {
					m_cookie = addPattern(null, m_predicate, m_subject);
				} catch (RDFException e) {
				}
			}
			
			if (m_subject == null) {
				if (oldSubject != null) {
					onPropertyDeleted(oldSubject);
				}
			} else if (!m_subject.equals(oldSubject)) {
				onPropertyChanged(oldSubject);
			}
		} else {
			RDFNode oldObject = m_object;
			m_object = null;
			
			if (m_subject != null) {
				m_object = getObject();
				try {
					m_cookie = addPattern(m_subject, m_predicate, null);
				} catch (RDFException e) {
				}
			}
			
			if (m_object == null) {
				if (oldObject != null) {
					onPropertyDeleted(oldObject);
				}
			} else if (!m_object.equals(oldObject)) {
				onPropertyChanged(oldObject);
			}
		}
	}
	protected RDFNode getObject() {
		try {
			return m_infoSource.extract(m_subject, m_predicate, null);
		} catch (Exception e) {
			s_logger.info("Failed to get object for " + m_subject + " " + m_predicate, e);
			return null;
		}
	}
	protected Resource getSubject() {
		try {
			return (Resource) m_infoSource.extract(null, m_predicate, m_object);
		} catch (Exception e) {
			s_logger.info("Failed to get subject for " + m_object + " " + m_predicate, e);
			return null;
		}
	}
	
	abstract protected void onPropertyDeleted(RDFNode oldProperty);
	abstract protected void onPropertyChanged(RDFNode oldProperty);
}
