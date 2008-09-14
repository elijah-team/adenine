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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.io.Serializable;
import java.util.*;

/**
 * @author David Huynh
 */
public class HSListDataProvider extends ChainedDataProvider {
	protected ArrayList 		m_list = new ArrayList();
	transient protected Object	m_cookie;
	protected Resource			m_listURI;
	
	class EventListener implements IEventListener, Serializable {
		public void onItemAdded(RDFNode item, int index) {
			synchronized (HSListDataProvider.this) {
				m_list.add(index, item);
						
				notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(index, 1, null));
			}
		}
	
		public void onItemChanged(RDFNode newItem, int index) {
			synchronized (HSListDataProvider.this) {
				m_list.set(index, newItem);
						
				notifyDataConsumers(DataConstants.LIST_CHANGE, new ListDataChange(index, 1, null));
			}
		}
	
		public void onItemRemoved(int index, int count) {
			synchronized (HSListDataProvider.this) {
				List list = new ArrayList();
						
				Iterator i = m_list.listIterator(index);
				while (count > 0 && i.hasNext()) {
					list.add(i.next());
					i.remove();
					count--;
				}
						
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index, list.size(), list));
			}
		}
	
		public void onListCleared() {
			synchronized (HSListDataProvider.this) {
				m_list.clear();
				notifyDataConsumers(DataConstants.LIST_CLEAR, null);
			}
		}
	}
	
	transient protected EventListener m_eventListener = new EventListener();
	
	final static public Resource HS_LIST = new Resource(DataConstants.NAMESPACE + "hsList");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(HSListDataProvider.class);
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.reset();
		dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(0, m_list.size(), m_list));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);
		
		Resource listURI = (Resource) Utilities.getResourceProperty(m_prescription, HS_LIST, m_partDataSource);
		if (listURI != null) {
			setList(listURI);
		}
	}
	
	synchronized protected void setList(Resource listURI) {
		cleanup();
		notifyDataConsumers(DataConstants.LIST_CLEAR, null);
		m_cookie = populateAndWatchHSList(m_listURI = listURI, m_list, m_eventListener, m_context, m_infoSource);
		notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(0, m_list.size(), m_list));
	}
	
	protected void cleanup() {
		if (m_cookie != null) {
			ListUtilities.unwatchHSList(m_cookie, m_infoSource);
			m_cookie = null;
		}
		
		m_list.clear();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		cleanup();
		m_list = null;
		super.dispose();		
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.LIST)) {
			return m_list;
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_list.get(((Integer) specifications).intValue());
			} catch (Exception e) {
				s_logger.error("Failed to get list element", e);
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS)) {
			try {
				Object[]	a = (Object[]) specifications;
				int		index = ((Integer) a[0]).intValue();
				int		count = ((Integer) a[1]).intValue();
				ArrayList 	elements = new ArrayList();
				
				elements.addAll(m_list.subList(index, index + count));

				return elements;
			} catch (Exception e) {
				s_logger.error("Failed to get list elements", e);
			}
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
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		if (changeType.equals(DataConstants.LIST_ADDITION)) {
			ListDataChange listDataChange = (ListDataChange) change;
			
			for (int i = 0; i < listDataChange.m_count; i++) {
				ListUtilities.addToHSList(m_listURI, (RDFNode) listDataChange.m_elements.get(i), listDataChange.m_index + i, m_infoSource);
			}
		} else if (changeType.equals(DataConstants.LIST_REMOVAL)) {
			ListDataChange listDataChange = (ListDataChange) change;
			
			for (int i = 0; i < listDataChange.m_count; i++) {
				ListUtilities.removeFromHSList(m_listURI, listDataChange.m_index, m_infoSource);
			}
		} else if (changeType.equals(DataConstants.LIST_CHANGE)) {
			ListDataChange listDataChange = (ListDataChange) change;
			
			for (int i = 0; i < listDataChange.m_count; i++) {
				ListUtilities.changeHSList(m_listURI, (RDFNode) listDataChange.m_elements.get(i), listDataChange.m_index, m_infoSource);
			}
		} else if (changeType.equals(DataConstants.LIST_CLEAR)) {
			ListUtilities.clearHSList(m_listURI, m_infoSource);
		} else if (changeType.equals(DataConstants.SET_ADDITION)) {
			if (!m_list.contains(change)) {
				ListUtilities.appendToHSList(m_listURI, (RDFNode) change, m_infoSource);
			}
		} else if (changeType.equals(DataConstants.SET_ADDITION)) {
			if (change instanceof Set) {
				Iterator i = ((Set) change).iterator();
				while (i.hasNext()) {
					ListUtilities.appendToHSList(m_listURI, (RDFNode) i.next(), m_infoSource);
				}
			} else if (change instanceof RDFNode) {
				ListUtilities.appendToHSList(m_listURI, (RDFNode) change, m_infoSource);
			}
		} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
			if (change instanceof Set) {
				Iterator i = ((Set) change).iterator();
				while (i.hasNext()) {
					ListUtilities.removeFromHSList(m_listURI, (RDFNode) i.next(), m_infoSource);
				}
			} else if (change instanceof RDFNode) {
				ListUtilities.removeFromHSList(m_listURI, (RDFNode) change, m_infoSource);
			}
		} else {	
			throw new UnsupportedOperationException("HS list data provider supports no operation.");
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return changeType.equals(DataConstants.LIST_ADDITION) ||
				changeType.equals(DataConstants.LIST_REMOVAL) ||
				changeType.equals(DataConstants.LIST_CHANGE) ||
				changeType.equals(DataConstants.LIST_CLEAR) ||
				changeType.equals(DataConstants.SET_REMOVAL) ||
				changeType.equals(DataConstants.SET_ADDITION);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		super.initializeFromDeserialization(source);
		notifyDataConsumers(DataConstants.LIST_CLEAR, null);
		m_list.clear();
		m_cookie = populateAndWatchHSList(m_listURI, m_list, m_eventListener, m_context, m_infoSource);
		notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(0, m_list.size(), m_list));
	}
	

	public static Object populateAndWatchHSList(Resource list, List elements, EventListener listener, Context context, IRDFContainer source) {
		ListWatchRecord record = new ListWatchRecord();
		
		// Set up the record generically
		{
			record.m_list = list;
			record.m_source = source;
			record.m_listener = listener;
			
			record.m_rdfListener = new RDFListener((ServiceManager) context.getServiceAccessor(), (IRDFEventSource) source) {
				ListWatchRecord m_record;
				//EventListener	m_listener;
				
				public void statementAdded(Resource cookie, Statement s) {
					synchronized (m_record) {
						NodeListener nodeListener = (NodeListener) m_record.m_nodeListeners.get(cookie);
						
						if (nodeListener != null) {
							nodeListener.statementAdded(s, m_record);
						}
					}
				}
				public void statementRemoved(Resource cookie, Statement s) {
					synchronized (m_record) {
						NodeListener nodeListener = (NodeListener) m_record.m_nodeListeners.get(cookie);
					
						if (nodeListener != null) {
							nodeListener.statementRemoved(s, m_record);
						}
					}
				}
				public RDFListener init(ListWatchRecord record) {
					m_record = record;
					return this;
				}
			}.init(record);
			record.m_rdfListener.start();
		}
		
		synchronized (record) {
			// Add watch for root
			try {
				Resource cookie = record.m_rdfListener.addPattern(list, Constants.s_haystack_list, null);
				
				record.m_nodeListeners.put(cookie, new NodeListener(list, null, cookie) {
					public void statementAdded(Statement s0, ListWatchRecord record) {
						Resource list = Utilities.getResourceProperty(m_node, Constants.s_haystack_list, record.m_source);
						if (list.equals(Constants.s_daml_nil)) {
							Iterator i = record.m_nodeListeners.values().iterator();
							while (i.hasNext()) {
								NodeListener nodeListener = (NodeListener) i.next();
								
								if (nodeListener != this) {
									record.m_rdfListener.removePattern(nodeListener.m_firstCookie);
									record.m_rdfListener.removePattern(nodeListener.m_restCookie);
								}
							}
							
							record.m_nodeListeners.clear();
							record.m_nodeListeners.put(m_restCookie, this);
							
							record.m_nodeListenersAsList.clear();
	
							record.m_listener.onListCleared();
						} else {
							ListUtilities.processListNodeChanged(record, list, 0);
						}
					}
				});
			
				// Add watches for element nodes
				Resource currentNode = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
				while (!currentNode.equals(Constants.s_daml_nil)) {
					elements.add(ListUtilities.makeElementNodeListener(currentNode, record, -1));
					
					currentNode = Utilities.getResourceProperty(currentNode, Constants.s_daml_rest, source);
				}
			} catch (Exception e) {
				ListUtilities.cleanUpRecord(record);
				s_logger.error("Failed to process root of HS list", e);
				return null;
			}
		}
		
		return record;
	}
	

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceChanged(Resource newResource) {
				setList(newResource);
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceDeleted(Resource previousResource) {
				cleanup();
			}
		};
	}
	
}
	