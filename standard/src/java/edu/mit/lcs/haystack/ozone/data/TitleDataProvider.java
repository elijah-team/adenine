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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import java.util.*;

/**
 * @author David Huynh
 */
public class TitleDataProvider extends ChainedDataProvider {
	Resource	m_underlying;
	Resource	m_customCookie;
	
	Resource	m_titleSourcePredicate;
	
	String		m_rdfsLabel;
	String		m_dcTitle;
	String		m_custom;
	String		m_title;
	
	static final String 	SUMMARY_NAMESPACE = "http://haystack.lcs.mit.edu/ui/summaryView#";
	static final Resource	UNDERLYING = new Resource(SUMMARY_NAMESPACE + "underlying");
	static final Resource	TITLE_SOURCE_PREDICATE = new Resource("http://haystack.lcs.mit.edu/schemata/vowl#titleSourcePredicate");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TitleDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);

		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (m_dataProvider == null) {
			Resource underlying = null;
		
			if (dataSource != null) {
				underlying = Utilities.getResourceProperty(dataSource, UNDERLYING, m_partDataSource);
				if (underlying == null) {
					underlying = Utilities.getResourceProperty(dataSource, DataConstants.RESOURCE, m_partDataSource);
				}
			}
			
			if (underlying == null) {
				underlying = (Resource) m_context.getLocalProperty(UNDERLYING);
			}
			if (underlying == null) {
				underlying = (Resource) m_context.getProperty(DataConstants.RESOURCE);
			}
			
			setUnderlying(underlying);
		}
		
		m_initializing = false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_title != null) {
			dataConsumer.onDataChanged(DataConstants.STRING_CHANGE, m_title);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (m_underlying == null) {
			throw new DataNotAvailableException("No data available");
		}
		
		if (dataType.equals(DataConstants.STRING)) {
			return m_title;
		}
		return null;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				setUnderlying(newResource);
			}
	
			protected void onResourceDeleted(Resource previousResource) {
				setUnderlying(null);
			}
		};
	}


	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_underlying != null) {		
			removeDefaultListeners(m_underlying, this, (ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource);
		}
		
		m_underlying = null;
		m_customCookie = null;

		m_rdfsLabel = null;
		m_dcTitle = null;
		m_custom = null;
		m_title = null;

		super.dispose();
	}

	synchronized protected void reloadData() {
		m_rdfsLabel = Utilities.getLiteralProperty(m_underlying, Constants.s_rdfs_label, m_infoSource);
		m_dcTitle = Utilities.getLiteralProperty(m_underlying, Constants.s_dc_title, m_infoSource);
		if (m_titleSourcePredicate != null) {
			m_custom = Utilities.getLiteralProperty(m_underlying, m_titleSourcePredicate, m_infoSource);
		} else {
			m_custom = null;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(Statement)
	 */
	synchronized protected void onStatementAdded(Statement s) {
		/*Resource predicate = s.getPredicate();
		if (predicate.equals(m_titleSourcePredicate)) {
			m_custom = s.getObject().getContent();
		} else if (predicate.equals(Constants.s_dc_title)) {
			m_dcTitle = s.getObject().getContent();
		} else {
			m_rdfsLabel = s.getObject().getContent();
		}*/
		reloadData();
		cacheTitle();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(Statement)
	 */
	synchronized protected void onStatementRemoved(Statement s) {
/*		Resource predicate = s.getPredicate();
		if (predicate.equals(m_titleSourcePredicate)) {
			m_custom = null;
		} else if (predicate.equals(Constants.s_dc_title)) {
			m_dcTitle = null;
		} else {
			m_rdfsLabel = null;
		}*/
		reloadData();
		cacheTitle();
	}

	synchronized protected void setUnderlying(Resource underlying) {
		if (underlying != m_underlying) {
			removeDefaultListeners(m_underlying, this, (ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource);
			if (m_customCookie != null) {
				removePattern(m_customCookie);
				m_customCookie = null;
			}
			m_titleSourcePredicate = null;
			
			m_underlying = underlying;
			if (m_underlying != null) {
				addDefaultListeners(underlying, this, (ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource);
				
				try {
					RDFNode[] a = m_infoSource.queryExtract(new Statement[] {
						new Statement(m_underlying, Constants.s_rdf_type, Utilities.generateWildcardResource(2)),
						new Statement(Utilities.generateWildcardResource(2), TITLE_SOURCE_PREDICATE, Utilities.generateWildcardResource(1)) },
						Utilities.generateWildcardResourceArray(1), 
						Utilities.generateWildcardResourceArray(2));
	
					if (a != null) {
						m_titleSourcePredicate = (Resource) a[0];
						
						createListener();
						m_customCookie = addPattern(m_underlying, m_titleSourcePredicate, null);
					}
				} catch (RDFException e) {
				}

				reloadData();				
			} else {
				m_rdfsLabel = null;
				m_dcTitle = null;
				m_custom = null;
			}
				
			cacheTitle();
		}
	}
	
	protected void cacheTitle() {
		String oldTitle = m_title;

		if (m_custom != null) {
			m_title = m_custom;
		} else if (m_dcTitle != null) {
			m_title = m_dcTitle;
		} else if (m_rdfsLabel != null) {
			m_title = m_rdfsLabel;
		} else if (m_underlying != null) {
			m_title = m_underlying.getURI();

			// Support mailto:
			if (m_title.indexOf("mailto:") == 0) {
			    //TODO: Is the code in the bio plugin sufficient to handle lsids?
				m_title = m_title.substring(7);
				///*			} else if ((m_title.indexOf("http://") != 0) && (m_title.indexOf("urn:lsid:") != 0)){
				//				m_title = "";*/
			}
		}
		
		if (((m_title == null && oldTitle != null) || (m_title != null && !m_title.equals(oldTitle))) && !m_initializing) {
			if (m_title == null) {
				notifyDataConsumers(DataConstants.STRING_DELETION, oldTitle);
			} else {
				notifyDataConsumers(DataConstants.STRING_CHANGE, m_title);
			}
		}
	}
	
	static protected HashMap s_underlyingToDataProviders = new HashMap(1024, (float) 0.5);
	static protected HashMap s_eventSourceToRDFListener = new HashMap();
	
	static protected void addDefaultListeners(Resource underlying, TitleDataProvider dp, ServiceManager serviceManager, IRDFEventSource eventSource) {
		if (underlying != null) {
			synchronized(s_eventSourceToRDFListener) {
				RDFListener listener = (RDFListener) s_eventSourceToRDFListener.get(eventSource);
				if (listener == null) {
					listener = new RDFListener(serviceManager, eventSource) {
						public void statementAdded(Resource cookie, Statement s) {
							onStatementAdded2(s);
						}
						public void statementRemoved(Resource cookie, Statement s) {
							onStatementRemoved2(s);
						}
					};
					listener.start();
					
					s_eventSourceToRDFListener.put(eventSource, listener);
					
					try {
						listener.addPattern(null, Constants.s_dc_title, null);
						listener.addPattern(null, Constants.s_rdfs_label, null);
					} catch (RDFException e) {
						s_logger.error("Failed to add pattern", e);
					}
				}
			}
						
			synchronized(s_underlyingToDataProviders) {
				LinkedList dataProviders = (LinkedList) s_underlyingToDataProviders.get(underlying);
				if (dataProviders == null) {
					dataProviders = new LinkedList();
					s_underlyingToDataProviders.put(underlying, dataProviders);
				}
				dataProviders.add(0, dp);
			}
		}
	}
	
	static protected void removeDefaultListeners(Resource underlying, TitleDataProvider dp, ServiceManager serviceManager, IRDFEventSource eventSource) {
		if (underlying != null) {
			boolean removeAll = false;
			
			synchronized(s_underlyingToDataProviders) {
				LinkedList dataProviders = (LinkedList) s_underlyingToDataProviders.get(underlying);
				if (dataProviders != null) {
					dataProviders.remove(dp);
					
					if (dataProviders.size() == 0) {
						s_underlyingToDataProviders.remove(underlying);
					}
				}
				
				removeAll = s_underlyingToDataProviders.size() == 0;
			}
			
			if (removeAll) {
				synchronized(s_eventSourceToRDFListener) {
					Iterator i = s_eventSourceToRDFListener.values().iterator();
					
					while (i.hasNext()) {
						RDFListener l = (RDFListener) i.next();
						l.stop();
					}
					
					s_eventSourceToRDFListener.clear();
				}
			}
		}
	}
	
	static protected void onStatementAdded2(Statement s) {
		refresh(s.getSubject());
	}

	static protected void onStatementRemoved2(Statement s) {
		refresh(s.getSubject());
	}
	
	static protected void refresh(Resource underlying) {
		LinkedList dataProviders;
		synchronized(s_underlyingToDataProviders) {
			dataProviders = (LinkedList) s_underlyingToDataProviders.get(underlying);
			if (dataProviders != null) {
				dataProviders = (LinkedList) dataProviders.clone();
			}
		}
		if (dataProviders != null) {
			Iterator i = dataProviders.iterator();
			
			while (i.hasNext()) {
				TitleDataProvider dp = (TitleDataProvider) i.next();
				
				dp.reloadData();
				dp.cacheTitle();
			}
		}
	}
}
