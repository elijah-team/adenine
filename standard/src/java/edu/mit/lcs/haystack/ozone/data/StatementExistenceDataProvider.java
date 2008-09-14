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
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @author David Huynh
 */
public class StatementExistenceDataProvider extends ChainedDataProvider {
	protected Resource				m_cookie;

	protected Resource				m_subject;
	protected Resource				m_predicate;
	protected RDFNode					m_object;
	protected boolean					m_dynamicSubject = true;
	protected boolean					m_exists = false;

	final static Resource SUBJECT		= new Resource(DataConstants.NAMESPACE + "subject");
	final static Resource PREDICATE		= new Resource(DataConstants.NAMESPACE + "predicate");
	final static Resource OBJECT		= new Resource(DataConstants.NAMESPACE + "object");

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(StatementExistenceDataProvider.class);
	
	protected ResourceDataProviderWrapper m_subjectProvider = null;
	protected ResourceDataProviderWrapper m_objectProvider = null;
	protected ResourceDataProviderWrapper m_predicateProvider = null;
	
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
		m_subject = Utilities.getResourceProperty(dataSource, SUBJECT, m_partDataSource);
		m_object = Utilities.getProperty(dataSource, OBJECT, m_partDataSource);
		if (m_subject == null) {
			m_subject = (Resource) m_context.getLocalProperty(DataConstants.RESOURCE);
		}
		
		if (m_object == null) {
			m_dynamicSubject = false;
		} 

		internalInitialize(source, context, true);

		if (m_subject == null) {
			Resource subjectDataProvider = Utilities.getResourceProperty(dataSource, DataConstants.s_subjectDataSource, m_partDataSource);
			if (subjectDataProvider != null) {
				m_subjectProvider = new ResourceDataProviderWrapper(DataUtilities.createDataProvider(subjectDataProvider, m_context, m_partDataSource));
				m_subjectProvider.m_provider.registerConsumer(new ResourceDataConsumer() {
					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						synchronized (StatementExistenceDataProvider.this) {
							m_subject = newResource;
							cacheData(true);		
						}
					}

					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						synchronized (StatementExistenceDataProvider.this) {
							m_subject = null;
							if (m_exists && !m_initializing) {
								m_exists = false;
								notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_exists));
							}
						}
					}
				});
			}
		}

		if (m_object == null) {
			Resource objectDataProvider = Utilities.getResourceProperty(dataSource, DataConstants.s_objectDataSource, m_partDataSource);
			if (objectDataProvider != null) {
				m_objectProvider = new ResourceDataProviderWrapper(DataUtilities.createDataProvider(objectDataProvider, m_context, m_partDataSource));
				m_objectProvider.m_provider.registerConsumer(new ResourceDataConsumer() {
					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						synchronized (StatementExistenceDataProvider.this) {
							m_object = newResource;
							cacheData(true);		
						}
					}

					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						synchronized (StatementExistenceDataProvider.this) {
							m_object = null;
							if (m_exists && !m_initializing) {
								m_exists = false;
								notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_exists));
							}
						}
					}
				});
			}
		}
		
		if (m_predicate == null) {
			Resource predicateDataProvider = Utilities.getResourceProperty(dataSource, DataConstants.s_predicateDataSource, m_partDataSource);
			if (predicateDataProvider != null) {
				m_predicateProvider = new ResourceDataProviderWrapper(DataUtilities.createDataProvider(predicateDataProvider, m_context, m_partDataSource));
				m_predicateProvider.m_provider.registerConsumer(new ResourceDataConsumer() {
					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						synchronized (StatementExistenceDataProvider.this) {							
							m_predicate = newResource;
							cacheData(true);
						}
					}

					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						synchronized (StatementExistenceDataProvider.this) {
							m_predicate = null;
							if (m_exists && !m_initializing) {
								m_exists = false;
								notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_exists));
							}
						}
					}
				});				
			}
		}		
		

		if (m_dataProvider == null && m_subjectProvider == null && m_objectProvider == null && (m_predicateProvider == null)) { // no chained data provider
			cacheData(true);
		}

		m_initializing = false;
	}

	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				onChainedResourceChanged(newResource);
			}

			protected void onResourceDeleted(Resource previousResource) {
				onChainedResourceRemoved();
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

		if (m_subjectProvider != null) {
			m_subjectProvider.dispose();
			m_subjectProvider = null;
		}

		if (m_objectProvider != null) {
			m_objectProvider.dispose();
			m_objectProvider = null;
		}
		
		if (m_predicateProvider != null) {
			m_predicateProvider.dispose();
			m_predicateProvider = null;
		}

		super.dispose();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	synchronized protected void onStatementAdded(Statement s) {
		cacheData(false);
	}

	synchronized protected void onStatementRemoved(Statement s) {
		cacheData(false);
	}

	protected void cacheData(boolean changePattern) {
		if (m_cookie != null && changePattern) {
			removePattern(m_cookie);
			m_cookie = null;
		}

		try {
			boolean exists = m_exists;
			
			if (m_subject == null || m_predicate == null || m_object == null) {
				m_exists = false;
			} else {
				m_exists = m_infoSource.contains(new Statement(m_subject, m_predicate, m_object));				
				
				if (changePattern) {
					m_cookie = addPattern(m_subject, m_predicate, m_object);
				}
			}
			
			if (exists != m_exists && !m_initializing) {
				notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_exists));
			}
		} catch (Exception e) {
			s_logger.error("Failed to cache data in statement existence provider", e);
		}
	}

	synchronized protected void onChainedResourceChanged(Resource newResource) {
		if (m_dynamicSubject) {
			if (!newResource.equals(m_subject)) {
				m_subject = newResource;
	
				boolean exists = m_exists;
	
				cacheData(true);
			}
		} else {
			if (!newResource.equals(m_object)) {
				m_object = newResource;

				boolean exists = m_exists;

				cacheData(true);
			}
		}
	}

	synchronized protected void onChainedResourceRemoved() {
		if (m_dynamicSubject) {
			m_subject = null;
		} else {
			m_object = null;
		}
		
		if (m_exists && !m_initializing) {
			m_exists = false;
			notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_exists));
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {

		if (dataType.equals(DataConstants.BOOLEAN)) {
			return new Boolean(m_exists);
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.BOOLEAN_CHANGE, new Boolean(m_exists));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (changeType.equals(DataConstants.BOOLEAN_CHANGE)) {
			if (change instanceof Boolean) {
				Boolean		exists = (Boolean) change;

				if (exists.booleanValue() != m_exists &&
					m_subject != null &&
					m_predicate != null &&
					m_object != null) {
					try {
						if (exists.booleanValue()) {
							m_infoSource.add(new Statement(m_subject, m_predicate, m_object));
						} else {
							m_infoSource.remove(new Statement(m_subject, m_predicate, m_object), new Resource[] {});
						}
					} catch (RDFException e) {
						s_logger.error("Failed to perform change", e);
					}
				}
			} else {
				throw new DataMismatchException("Change data type mismatch: expecting java.util.Set instead of " + change.getClass());
			}
		} else {
			throw new UnsupportedOperationException("Data provider does not support change to type " + changeType);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return changeType.equals(DataConstants.BOOLEAN_CHANGE);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_subjectProvider != null) {
			m_subjectProvider.getDataProvider().initializeFromDeserialization(source);
		}
		
		if (m_objectProvider != null) {
			m_objectProvider.getDataProvider().initializeFromDeserialization(source);
		}
		if (m_predicateProvider != null) {
			m_predicateProvider.getDataProvider().initializeFromDeserialization(source);
		}		
		
		cacheData(true);
	}
}
