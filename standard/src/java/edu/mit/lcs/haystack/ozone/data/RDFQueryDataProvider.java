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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * @author Dennis Quan
 */
public class RDFQueryDataProvider extends GenericPart implements IDataProvider {
	static UpdatingThread s_updatingThread;
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(RDFQueryDataProvider.class);
	static {
		s_updatingThread = new UpdatingThread();
		s_updatingThread.setDaemon(true);
		s_updatingThread.setPriority(Thread.MIN_PRIORITY);
		s_updatingThread.start();		
	}
	
	class QueryLine implements Serializable {
		boolean m_subjectX;
		boolean m_predicateX;
		boolean m_objectX;

		QueryLine(Resource r) {
			m_subject = Utilities.getResourceProperty(r, DataConstants.SUBJECT, m_partDataSource);
			m_predicate = Utilities.getResourceProperty(r, DataConstants.PREDICATE, m_partDataSource);
			m_object = Utilities.getProperty(r, DataConstants.OBJECT, m_partDataSource);
			
			Resource chainedDataSource = (Resource) Utilities.getResourceProperty(r, DataConstants.s_subjectDataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_subjectProvider = new ResourceDataProviderWrapper(DataUtilities.createDataProvider(chainedDataSource, m_context, m_partDataSource, m_partDataSource));
				m_subjectConsumer = new ResourceDataConsumer() {
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						m_subject = newResource;

						setupNotification();
						handleChange();
					}
					
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						m_subject = null;
						handleChange();
					}
				};
				m_subjectProvider.getDataProvider().registerConsumer(m_subjectConsumer);
			}
			
			chainedDataSource = (Resource) Utilities.getResourceProperty(r, DataConstants.s_predicateDataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_predicateProvider = new ResourceDataProviderWrapper(DataUtilities.createDataProvider(chainedDataSource, m_context, m_partDataSource, m_partDataSource));
				m_predicateConsumer = new ResourceDataConsumer() {
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						m_predicate = newResource;

						setupNotification();
						handleChange();
					}
					
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						m_predicate = null;
						handleChange();
					}
				};
				m_predicateProvider.getDataProvider().registerConsumer(m_predicateConsumer);
			}
			
			chainedDataSource = (Resource) Utilities.getResourceProperty(r, DataConstants.s_objectDataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_objectProvider = new ResourceDataProviderWrapper(DataUtilities.createDataProvider(chainedDataSource, m_context, m_partDataSource, m_partDataSource));
				m_objectConsumer = new ResourceDataConsumer() {
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						m_object = newResource;
						
						setupNotification();
						handleChange();
					}
					
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						m_object = null;
						handleChange();
					}
				};
				m_objectProvider.getDataProvider().registerConsumer(m_objectConsumer);
			}

			m_subjectX = m_existentials.contains(m_subject);
			m_predicateX = m_existentials.contains(m_predicate);
			m_objectX = m_existentials.contains(m_object);
						
			setupNotification();			
//			if (!(subjectX && objectX && !predicateX && Constants.s_rdf_type.equals(predicate))) {
				// Set up notification
//			}
		}
		
		transient Resource m_cookie = null;
		
		void setupNotification() {
			if (m_rdfListener == null) {
				// Have not initialized yet; we will be called when initialization occurs
				return;
			}
			
			if (m_cookie != null) {
				m_rdfListener.removePattern(m_cookie);
			}
			
			Resource actualSubject;
			Resource actualPredicate;
			RDFNode  actualObject;
			actualSubject = m_subjectX ? null : m_subject;
			actualPredicate = m_predicateX ? null : m_predicate;
			actualObject = m_objectX ? null : m_object;
			try {
				m_cookie = m_rdfListener.addPattern(actualSubject, actualPredicate, actualObject);
			} catch (RDFException e) {
				s_logger.error("Failed to add pattern " + actualSubject + " " + actualPredicate + " " + actualObject, e);
			}
		}
		
		Statement produceStatement() {
			if (m_subject != null && m_predicate != null && m_object != null) {
				return new Statement(m_subject.equals(m_sourceExistential) ? m_sourceResource : m_subject, m_predicate.equals(m_sourceExistential) ? m_sourceResource : m_predicate, m_object.equals(m_sourceExistential) ? m_sourceResource : m_object);
			} else {
				return null;
			}
		}

		public String toString() {
			return m_subject + "," + m_predicate + "," + m_object + m_subjectProvider + "," + m_predicateProvider + "," + m_objectProvider;
		}
		
		Resource m_subject;
		Resource m_predicate;
		RDFNode m_object;
		
		ResourceDataConsumer m_subjectConsumer = null;
		ResourceDataProviderWrapper m_subjectProvider = null;
		
		ResourceDataConsumer m_predicateConsumer = null;
		ResourceDataProviderWrapper m_predicateProvider = null;
		
		ResourceDataConsumer m_objectConsumer = null;
		ResourceDataProviderWrapper m_objectProvider = null;
		
		void dispose() {
			if (m_subjectProvider != null) {
				m_subjectProvider.dispose();
			}
			if (m_predicateProvider != null) {
				m_predicateProvider.dispose();
			}
			if (m_objectProvider != null) {
				m_objectProvider.dispose();
			}
		}
	}
	
	protected Set			m_dataConsumers = new HashSet();
	transient protected RDFListener	m_rdfListener;
	protected ArrayList		m_queryLines = new ArrayList();
	protected ArrayList 	m_existentials = new ArrayList();
	protected Resource		m_targetExistential;
	protected Resource[]	m_variableArray;
	protected Resource[]	m_existentialArray;
	protected boolean		m_done = false;
	protected long			m_lastRequestTime = -1;
	transient protected Object m_lastRequestTimeLock = new Object();
	protected Set			m_cachedData = null;
	protected boolean		m_extract = false;
	protected RDFNode		m_extractedNode = null;
	protected Resource		m_sourceExistential;
	
	synchronized protected RDFNode extract() {
		if (m_cachedData == null) {
			m_extractedNode = null;
		} else if (m_cachedData.size() == 0) {
			m_extractedNode = null;
		} else if (m_extractedNode == null || !m_cachedData.contains(m_extractedNode)) {
			m_extractedNode = (RDFNode) m_cachedData.iterator().next();
		}
		
		return m_extractedNode;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	public void registerConsumer(IDataConsumer dataConsumer) {
		synchronized (m_dataConsumers) {
			if (dataConsumer != null) {
				m_dataConsumers.add(dataConsumer);
			}
		}

		if (dataConsumer != null) {
			synchronized (this) {
				if (m_cachedData != null && m_cachedData.size() > 0) {
					if (m_extract) {
						extract();
					}
					dataConsumer.onDataChanged(m_extract ? (m_extractedNode instanceof Literal ? DataConstants.LITERAL_CHANGE : DataConstants.RESOURCE_CHANGE) : DataConstants.SET_ADDITION, m_extract ? (Object) m_extractedNode : new HashSet(m_cachedData));
				}
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
		synchronized (m_dataConsumers) {
			m_dataConsumers.remove(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null) {
			return m_extract ? (Object) extract() : m_cachedData; 
		}
		
		if (dataType.equals(DataConstants.SET)) {
			return m_cachedData;
		}

		if (dataType.equals(DataConstants.RESOURCE)) {
			return extract();
		}

		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (m_extract && m_queryLines.size() == 1) {
			QueryLine ql = (QueryLine) m_queryLines.get(0);
			Statement s = ql.produceStatement();
			if (s == null) {
				throw new UnsupportedOperationException("Cannot remove set because data provider is not completely initialized yet");
			}
			if (DataConstants.LITERAL_DELETION.equals(changeType) || 
					DataConstants.STRING_DELETION.equals(changeType) || 
					DataConstants.RESOURCE_DELETION.equals(changeType)) {
				Resource res = Utilities.generateWildcardResource(1);
				Resource[] wildcards = Utilities.generateWildcardResourceArray(1);
				if (s.getSubject().equals(m_targetExistential)) {
					try {
						m_infoSource.remove(new Statement(res, s.getPredicate(), s.getObject()), wildcards);
					} catch (Exception e) {}
				} else if (s.getPredicate().equals(m_targetExistential)) {
					try {
						m_infoSource.remove(new Statement(s.getSubject(), res, s.getObject()), wildcards);
					} catch (Exception e) {}
				} else if (s.getObject().equals(m_targetExistential)) {
					try {
						m_infoSource.remove(new Statement(s.getSubject(), s.getPredicate(), res), wildcards);
					} catch (Exception e) {}
				}
				return;
			} else if (DataConstants.STRING_CHANGE.equals(changeType)) {
				try {
					m_infoSource.replace(s.getSubject(), s.getPredicate(), null, new Literal((String) change));
				} catch (Exception e) {}
				return;
			} else if (DataConstants.LITERAL_CHANGE.equals(changeType)) {
				try {
					m_infoSource.replace(s.getSubject(), s.getPredicate(), null, (Literal) change);
				} catch (Exception e) {}
				return;
			}
		} else if (DataConstants.SET_REMOVAL.equals(changeType)) {
			if (m_queryLines.size() == 1) {
				QueryLine ql = (QueryLine) m_queryLines.get(0);
				Statement s = ql.produceStatement();
				if (s == null) {
					throw new UnsupportedOperationException("Cannot remove set because data provider is not completely initialized yet");
				}
				if (!(change instanceof Set)) {
					change = Collections.singleton(change);
				}
				Iterator i = ((Set) change).iterator();
				while (i.hasNext()) {
					Resource res = (Resource) i.next();
					if (s.getSubject().equals(m_targetExistential)) {
						try {
							m_infoSource.remove(new Statement(res, s.getPredicate(), s.getObject()), new Resource[0]);
						} catch (Exception e) {}
					} else if (s.getPredicate().equals(m_targetExistential)) {
						try {
							m_infoSource.remove(new Statement(s.getSubject(), res, s.getObject()), new Resource[0]);
						} catch (Exception e) {}
					} else if (s.getObject().equals(m_targetExistential)) {
						try {
							m_infoSource.remove(new Statement(s.getSubject(), s.getPredicate(), res), new Resource[0]);
						} catch (Exception e) {}
					}
				}
				return;
			} else {
				throw new UnsupportedOperationException("Data provider does not support removal");
			}
		} else if (DataConstants.SET_ADDITION.equals(changeType)) {
			// See if the portion of the graph to satisfy is unique
			HashSet statements = new HashSet();
			HashSet statementsToFill = new HashSet(); 
			Iterator i = m_queryLines.iterator();
			while (i.hasNext()) {
				QueryLine ql = (QueryLine) i.next();

				Statement s = ql.produceStatement();
				if (s == null) {
					throw new UnsupportedOperationException("Data provider does not support changes");
				}

				if (m_targetExistential.equals(ql.m_subject) || m_targetExistential.equals(ql.m_predicate) || m_targetExistential.equals(ql.m_object)) {
					statementsToFill.add(s);
				} else {
					statements.add(s);
				}
			}
			
			try {
				if (statements.isEmpty()) {
					Iterator j = ((Set) change).iterator();
					while (j.hasNext()) {
						RDFNode datum = (RDFNode) j.next();
						i = statementsToFill.iterator();
						while (i.hasNext()) {
							Statement s = (Statement) i.next();
							int subjectIndex = Utilities.indexOfResource(m_existentialArray, s.getSubject());
							int predicateIndex = Utilities.indexOfResource(m_existentialArray, s.getPredicate());
							int objectIndex = Utilities.indexOfResource(m_existentialArray, s.getObject());
							Statement s2 = new Statement(subjectIndex >= 0 ? (Resource) datum : s.getSubject(),
								predicateIndex >= 0 ? (Resource) datum : s.getPredicate(),
								objectIndex >= 0 ? (RDFNode) datum : s.getObject());
							m_infoSource.add(s2); 		
						}
					}
				} else {
					Statement[] query = new Statement[statements.size()];
					statements.toArray(query);
					Set data;
					data =
						m_infoSource.query(
							query,
							m_existentialArray,
							m_existentialArray);
					if (data.size() == 1) {
						LocalRDFContainer toAdd = new LocalRDFContainer();
						Iterator j = ((Set) change).iterator();
						while (j.hasNext()) {
							Resource res = (Resource) j.next();
							i = statementsToFill.iterator();
							RDFNode[] datum = (RDFNode[]) data.iterator().next();
							datum[Utilities.indexOfResource(m_existentialArray, m_targetExistential)] = res;
							while (i.hasNext()) {
								Statement s = (Statement) i.next();
								int subjectIndex = Utilities.indexOfResource(m_existentialArray, s.getSubject());
								int predicateIndex = Utilities.indexOfResource(m_existentialArray, s.getPredicate());
								int objectIndex = Utilities.indexOfResource(m_existentialArray, s.getObject());
								toAdd.add(new Statement(subjectIndex >= 0 ? (Resource) datum[subjectIndex] : s.getSubject(),
									predicateIndex >= 0 ? (Resource) datum[predicateIndex] : s.getPredicate(),
									objectIndex >= 0 ? datum[objectIndex] : s.getObject())); 		
							}
						}
						m_infoSource.add(toAdd);
						return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		throw new UnsupportedOperationException("Data provider does not support changes");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return DataConstants.SET_ADDITION.equals(changeType);
	}
	
	protected void onStatementAdded(Statement s) {
		handleChange();
	}

	protected void handleChange() {
		synchronized (m_lastRequestTimeLock) {
			m_lastRequestTime = System.currentTimeMillis();
		}
	}

	protected void onStatementRemoved(Statement s) {
		handleChange();
	}

	protected IDataProvider			m_chainedDataProvider;
	protected IDataConsumer			m_chainedDataConsumer;
	protected Resource				m_sourceResource;
	protected boolean				m_ownsChainedDataProvider;

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);

		m_extract = Utilities.checkBooleanProperty(m_prescription, DataConstants.s_extract, m_partDataSource);

		// Set up listener
		setupListener();

		// Retrieve existentials
		Iterator k = ListUtilities.accessDAMLList(Utilities.getResourceProperty(m_prescription, DataConstants.EXISTENTIALS, m_partDataSource), m_partDataSource);
		while (k.hasNext()) {
			Object o = k.next();
			m_existentials.add(o);
		}
		m_existentialArray = new Resource[m_existentials.size()];
		m_existentials.toArray(m_existentialArray);
		
		// Retrieve pattern statements
		Resource[] statementResources = Utilities.getResourceProperties(m_prescription, DataConstants.STATEMENT, m_partDataSource);
		for (int j = 0; j < statementResources.length; j++) {
			Resource r = statementResources[j];
			QueryLine ql = new QueryLine(r);			
			m_queryLines.add(ql);
		}
		
		m_targetExistential = Utilities.getResourceProperty(m_prescription, DataConstants.TARGET_EXISTENTIAL, m_partDataSource);
		m_sourceExistential = Utilities.getResourceProperty(m_prescription, DataConstants.s_sourceExistential, m_partDataSource);
		
		// Support working as an aspect data source
		if (m_sourceExistential != null) {
			Boolean b = (Boolean) context.getLocalProperty(DataConstants.REVERSE);
			if (b != null) {
				if (b.booleanValue()) {
					// Reverse source and target existentials
					Resource tmp = m_targetExistential;
					m_targetExistential = m_sourceExistential;
					m_sourceExistential = tmp;
				}
			}
			m_existentials.remove(m_sourceExistential);
			
			// Initialize chained data provider
			m_chainedDataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
			if (m_chainedDataProvider == null) {
				m_chainedDataProvider = DataUtilities.createDataProvider(Constants.s_lensui_underlyingSource, m_context, m_source);
				m_ownsChainedDataProvider = true;
			}
			if (m_chainedDataProvider != null) {
				m_chainedDataConsumer = new ResourceDataConsumer() {
					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceChanged(Resource newResource) {
						synchronized (this) {
							m_sourceResource = newResource;
						}
						handleChange();
					}
					
					/* (non-Javadoc)
					 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
					 */
					protected void onResourceDeleted(Resource previousResource) {
						synchronized (this) {
							m_sourceResource = null;
						}
						handleChange();
					}
				};
				m_chainedDataProvider.registerConsumer(m_chainedDataConsumer);
			}
		}

		m_variableArray = new Resource[] { m_targetExistential };
		
		m_cachedData = performQuery();
		
		s_updatingThread.m_providers.add(this);
	}
	
	protected void setupListener() {
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource) m_infoSource) {
			public void statementAdded(Resource cookie, Statement s) {
				onStatementAdded(s);
			}
			public void statementRemoved(Resource cookie, Statement s) {
				onStatementRemoved(s);
			}
		};
		m_rdfListener.start();
	}
	
	protected Set performQuery() {
		if (m_sourceExistential != null && m_sourceResource == null) {
			return new HashSet();
		}
		
		try {
//			System.out.println("<< " + m_queryLines);
			Statement[] statements = new Statement[m_queryLines.size()];
			Iterator i = m_queryLines.iterator();
			int j = 0;
			while (i.hasNext()) {
				QueryLine ql = (QueryLine) i.next();
				Statement s = ql.produceStatement();
				if (s == null) {
					// Not ready yet
					return new HashSet();
				}
				statements[j++] = s;
			}
			
			Set s = m_infoSource.query(statements, m_variableArray, m_existentialArray);
			if (s == null) {
				s = new HashSet();
			} else {
				s = Utilities.extractFirstItems(s);
			}
//			System.out.println(">> " + s);
			return s;
		} catch (RDFException rdfe) {
			s_logger.error("Failed to perform query", rdfe);
			return new HashSet();
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		s_updatingThread.m_providers.remove(this);
		
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}

		if (m_dataConsumers != null) {
			m_dataConsumers.clear();
			m_dataConsumers = null;
		}

		Iterator i = m_queryLines.iterator();
		while (i.hasNext()) {
			QueryLine ql = (QueryLine) i.next();
			ql.dispose();
		}
		m_queryLines.clear();
		
		if (m_chainedDataProvider != null) {
			m_chainedDataProvider.unregisterConsumer(m_chainedDataConsumer);
			if (m_ownsChainedDataProvider) {
				m_chainedDataProvider.dispose();
			}
			m_chainedDataProvider = null;
		}
		
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		m_lastRequestTimeLock = new Object();
		super.initializeFromDeserialization(source);
		setupListener();
		s_updatingThread.m_providers.add(this);
		Iterator i = m_queryLines.iterator(); 
		while (i.hasNext()) {
			QueryLine ql = (QueryLine) i.next();			
			if (ql.m_subjectProvider != null) {
				ql.m_subjectProvider.getDataProvider().initializeFromDeserialization(source);
			}
			if (ql.m_predicateProvider != null) {
				ql.m_predicateProvider.getDataProvider().initializeFromDeserialization(source);
			}
			if (ql.m_objectProvider != null) {
				ql.m_objectProvider.getDataProvider().initializeFromDeserialization(source);
			}
			ql.setupNotification();
		}
		handleChange();
	}
}

class UpdatingThread extends Thread {
	Set m_providers = Collections.synchronizedSet(new HashSet());
	
	/**
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (true) {
			try {
				Iterator i = m_providers.iterator();
				boolean sleep = true;
				while (i.hasNext()) {
					RDFQueryDataProvider _this = (RDFQueryDataProvider) i.next();
					
					long l;
					long l2 = System.currentTimeMillis();
					boolean run = false;
					synchronized (_this.m_lastRequestTimeLock) {
						l = _this.m_lastRequestTime;
						if (l != -1 && ((l2 - l) > 200 || l2 < l)) {
							_this.m_lastRequestTime = -1;
							run = true;
							sleep = false;
						}
					}
						
					if (run) {
						synchronized (_this) {
							Set s = _this.performQuery();
							if (!s.equals(_this.m_cachedData)) {
								if (_this.m_extract) {
									_this.m_cachedData = s;
									RDFNode oldExtract = _this.m_extractedNode;
									RDFNode newExtract = _this.extract();
									if ((oldExtract == null && newExtract != null) ||
										(oldExtract != null && newExtract == null) ||
										(oldExtract != null && !oldExtract.equals(newExtract))) {
										// Notify
										Iterator i2 = _this.m_dataConsumers.iterator();
										while (i2.hasNext()) {
											IDataConsumer dc = (IDataConsumer) i2.next();
											dc.onDataChanged(newExtract == null ? 
												((oldExtract instanceof Literal) ? DataConstants.LITERAL_DELETION : DataConstants.RESOURCE_DELETION) : 
												((newExtract instanceof Literal) ? DataConstants.LITERAL_CHANGE : DataConstants.RESOURCE_CHANGE), newExtract);
										}
									}
								} else {
									// Find new and deleted members
									HashSet newMembers = new HashSet();
									newMembers.addAll(s);
									if (_this.m_cachedData != null) {
										newMembers.removeAll(_this.m_cachedData);
									}
					
									HashSet deletedMembers = new HashSet();
									if (_this.m_cachedData != null) {
										deletedMembers.addAll(_this.m_cachedData);
										deletedMembers.removeAll(s);
									}
					
									// Notify
									Iterator i2 = _this.m_dataConsumers.iterator();
									while (i2.hasNext()) {
										IDataConsumer dc = (IDataConsumer) i2.next();
										if (!newMembers.isEmpty()) {
											dc.onDataChanged(DataConstants.SET_ADDITION, newMembers);
										}
										if (!deletedMembers.isEmpty()) {
											dc.onDataChanged(DataConstants.SET_REMOVAL, deletedMembers);
										}
									}

									_this.m_cachedData = s;
								}
							}
						}
					}
				}
			
				if (sleep) {
					try {
						sleep(200);
					} catch (InterruptedException e) {
					}
				}
			} catch (Exception e) {}
		}
	}
}
