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
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.*;

/**
 * @author David Huynh
 */
public class SortingDataProvider extends GenericPart implements IDataProvider {
	protected ArrayList		m_dataConsumers = new ArrayList();
	protected IDataProvider	m_dataProvider;
	protected IDataConsumer	m_dataConsumer;
	protected boolean			m_ownsDataProvider = false;
	
	transient protected RDFListener		m_rdfListener;
	
	Resource					m_sortDataSource;
	int						m_sortOrder = 1; // ascending
	boolean					m_sortAsDateTime = false;
	boolean					m_sortAsFloat = false;
	boolean					m_sortCaseSensitive = true;
	boolean					m_sortAgainWhenChanged = false;
	
	protected Set				m_unsortedItems = new HashSet(1024, (float) 0.5);
	protected Set				m_toSortItems = new HashSet(1024, (float) 0.5);
	protected List			m_sortedItems = new ArrayList();
	
	protected Hashtable		m_itemToDataProviderMap = new Hashtable(1024, (float) 0.5);
	protected Hashtable		m_itemToValueMap = new Hashtable(1024, (float) 0.5);
	
	protected boolean			m_initializing = true;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SortingDataProvider.class);
	
	final static Resource SORT_DATA_SOURCE = new Resource(DataConstants.NAMESPACE + "sortDataSource");
	final static Resource SORT_VALUE_TYPE = new Resource(DataConstants.NAMESPACE + "sortValueType");
	final static Resource SORT_ORDER = new Resource(DataConstants.NAMESPACE + "sortOrder");
	final static Resource SORT_CASE_SENSITIVE = new Resource(DataConstants.NAMESPACE + "sortCaseSensitive");
	final static Resource SORT_AGAIN_WHEN_CHANGED = new Resource(DataConstants.NAMESPACE + "sortAgainWhenChanged");
	
	class SortingDataConsumer implements IDataConsumer {
		public void onDataChanged(Resource changeType, Object change)
			throws IllegalArgumentException {

			if (changeType.equals(DataConstants.LIST_ADDITION)) {
				ListDataChange	c = (ListDataChange) change;
				Set				newItems = new HashSet();
				
				for (int i = 0; i < c.m_count; i++) {
					try {
						newItems.add(m_dataProvider.getData(DataConstants.LIST_ELEMENT, new Integer(c.m_index + i)));
					} catch (DataNotAvailableException e) {
					}
				}
				
				onItemsAdded(newItems);
			} else if (changeType.equals(DataConstants.SET_ADDITION)) {
				onItemsAdded((Set) change);
			} else if (changeType.equals(DataConstants.LIST_REMOVAL)) {
				ListDataChange c = (ListDataChange) change;
				
				onItemsRemoved(c.m_elements);
			} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
				onItemsRemoved((Set) change);
			} else if (changeType.equals(DataConstants.LIST_CLEAR) || changeType.equals(DataConstants.SET_CLEAR)) {
				onCleared();
			} else {
				throw new IllegalArgumentException("Unsupported change type " + changeType);
			}
		}

		public void onStatusChanged(Resource status) {
		}

		public void reset() {
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		Iterator i = m_itemToDataProviderMap.values().iterator();
		while (i.hasNext()) {
			IDataProvider dp = (IDataProvider) i.next();
			dp.initializeFromDeserialization(source);
		}

		if (m_ownsDataProvider) {
			m_dataProvider.initializeFromDeserialization(source);
		}
		createListener(m_prescription);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	synchronized public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		Resource dataSource = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);

		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource chainedDataSource = Utilities.getResourceProperty(dataSource, OzoneConstants.s_dataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_source, m_partDataSource);
				m_ownsDataProvider = true;
			}
		}
		
		m_sortDataSource = Utilities.getResourceProperty(dataSource, SORT_DATA_SOURCE, m_partDataSource);
		
		String s = Utilities.getLiteralProperty(dataSource, SORT_ORDER, m_source);
		if (s != null && s.equalsIgnoreCase("descending")) {
			m_sortOrder = -1;
		}
		
		m_sortCaseSensitive = Utilities.checkBooleanProperty(dataSource, SORT_CASE_SENSITIVE, m_partDataSource, true);
		m_sortAgainWhenChanged = Utilities.checkBooleanProperty(dataSource, SORT_AGAIN_WHEN_CHANGED, m_partDataSource, false);

		Resource r = Utilities.getResourceProperty(dataSource, SORT_VALUE_TYPE, m_partDataSource);
		if (r != null && r.equals(Constants.s_xsd_dateTime)) {
			m_sortAsDateTime = true;
			m_sortAsFloat = false;
			m_sortCaseSensitive = false;
		}
		if (r != null && r.equals(Constants.s_xsd_float)) {
			m_sortAsDateTime = false;
			m_sortAsFloat = true;
			m_sortCaseSensitive = false;
		}
		
		if (m_dataProvider != null) {
			m_dataConsumer = new SortingDataConsumer();
			m_dataProvider.registerConsumer(m_dataConsumer);
		}
		
		createListener(dataSource);
		internalSort();

		m_initializing = false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		m_unsortedItems.clear();
		m_toSortItems.clear();
		m_sortedItems.clear();
	
		Iterator i = m_itemToDataProviderMap.values().iterator();
		while (i.hasNext()) {
			((IDataProvider) i.next()).dispose();
		}
		m_itemToDataProviderMap.clear();
		m_itemToValueMap.clear();

		m_unsortedItems = null;
		m_toSortItems = null;
		m_sortedItems = null;
		m_itemToDataProviderMap = null;
		m_itemToValueMap = null;

		if (m_dataProvider != null) {
			m_dataProvider.unregisterConsumer(m_dataConsumer);
			if (m_ownsDataProvider) {
				m_dataProvider.dispose();
			}
			m_dataProvider = null;
			m_dataConsumer = null;
		}
		
		m_dataConsumers.clear();
		m_dataConsumers = null;
		
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}

		super.dispose();		
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	synchronized public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer == null) {
			return;
		}
		
		dataConsumer.reset();
		m_dataConsumers.add(dataConsumer);
		
		if (m_sortedItems.size() > 0) {
			dataConsumer.onDataChanged(
				DataConstants.LIST_ADDITION, new ListDataChange(0, m_sortedItems.size(), null));
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	synchronized public void unregisterConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null && m_dataConsumers.contains(dataConsumer)) {
			m_dataConsumers.remove(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.LIST)) {
			return m_sortedItems;
		} else if (dataType.equals(DataConstants.LIST_COUNT)) {
			return new Integer(m_sortedItems.size());
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_sortedItems.get(((Integer) specifications).intValue());
			} catch (Exception e) {
				s_logger.error("Failed to get element at " + specifications, e);
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();
			
			try {
				ArrayList 	elements = new ArrayList();
				
				elements.addAll(m_sortedItems.subList(index, index + count));

				return elements;
			} catch (Exception e) {
				s_logger.error("Failed to get elements index=" + index + " count=" + count, e);
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
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (changeType.equals(DataConstants.SET_ADDITION) ||
			changeType.equals(DataConstants.SET_REMOVAL) ||
			changeType.equals(DataConstants.SET_CLEAR)) {
				
			m_dataProvider.requestChange(changeType, change);
		} else if (changeType.equals(DataConstants.LIST_ADDITION)) {
			try {
				m_dataProvider.requestChange(changeType, change);
			} catch (Exception e) {
				ListDataChange c = (ListDataChange) change;
				
				for (int i = 0; i < c.m_elements.size(); i++) {
					m_dataProvider.requestChange(DataConstants.SET_ADDITION, c.m_elements.get(i));
				}
			}
		} else if (changeType.equals(DataConstants.LIST_REMOVAL)) {
			try {
				m_dataProvider.requestChange(changeType, change);
			} catch (Exception e) {
				ListDataChange c = (ListDataChange) change;
				
				if (c.m_indices != null) {
					ArrayList resources = new ArrayList();
					
					synchronized (this) {
						for (int i = 0; i < c.m_indices.size(); i++) {
							Integer index = (Integer) c.m_indices.get(i);
							resources.add(m_sortedItems.get(index.intValue()));
						}
					}
					for (int i = 0; i < resources.size(); i++) {
						m_dataProvider.requestChange(DataConstants.SET_REMOVAL, resources.get(i));
					}
				} else {
					for (int i = 0; i < c.m_count; i++) {
						m_dataProvider.requestChange(DataConstants.SET_REMOVAL, m_sortedItems.get(c.m_index + i));
					}
				}
			}
		} else if (changeType.equals(DataConstants.LIST_CLEAR)) {
			try {
				m_dataProvider.requestChange(changeType, change);
			} catch (Exception e) {
				m_dataProvider.requestChange(DataConstants.SET_CLEAR, null);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		if (changeType.equals(DataConstants.LIST_ADDITION)) {
			return false;
		}
		return m_dataProvider.supportsChange(changeType);
	}
	
	protected void createListener(Resource dataSource) {
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource) m_partDataSource) {
			public void statementAdded(Resource cookie, Statement s) {
				onStatementAdded(s);
			}
		};
		m_rdfListener.start();
		
		try {
			m_rdfListener.addPattern(dataSource, SORT_ORDER, null);
		} catch (Exception e) {
			s_logger.error("Failed to add pattern to watch sort order", e);
		}
	}
	
	synchronized protected void onItemsAdded(Collection newItems) {
		Iterator i = newItems.iterator();
		
		if (m_initializing) {
			while (i.hasNext()) {
				Object o = i.next();
				RDFNode item = (RDFNode) o;
				
				if (item instanceof Literal) {
					m_toSortItems.add(item);
				} else {
					Context	childContext = new Context(m_context);
					
					childContext.putLocalProperty(DataConstants.RESOURCE, item);
					
					IDataProvider dataProvider = createSortDataProvider(childContext);
					
					IDataConsumer dataConsumer = new StringDataConsumer() {
						Resource m_item;
						
						public IDataConsumer initialize(Resource item) {
							m_item = item;
							
							return this;
						}
						
						protected void onStringChanged(String newString) {
							onItemValueChanged(m_item, newString);
						}
	
						protected void onStringDeleted(String previousString) {
							onItemValueDeleted(m_item, previousString);
						}
					}.initialize((Resource) item);
					
					m_itemToDataProviderMap.put(item, dataProvider);
					
					dataProvider.registerConsumer(dataConsumer);
					
					if (!m_toSortItems.contains(item)) {
						m_unsortedItems.add(item);
					}
				}
			}
		} else {
			while (i.hasNext()) {
				Object o = i.next();
				RDFNode item = (RDFNode) o;
				
				if (item instanceof Literal) {
					m_toSortItems.add(item);
					if (m_runnable == null && m_toSortItems.size() > 0) {
						m_runnable = new IdleRunnable(m_context) {
							public void run() {
								synchronized (SortingDataProvider.this) {
									m_runnable = null;
									if (m_context != null) {
										internalSort();
									}
								}
							}
						};
						Ozone.idleExec(m_runnable);
					}
				} else if (!m_itemToDataProviderMap.contains(item)) {
					Context	childContext = new Context(m_context);
					
					childContext.putLocalProperty(DataConstants.RESOURCE, item);
					
					IDataProvider dataProvider = createSortDataProvider(childContext);
					
					m_unsortedItems.add(item);
					
					IDataConsumer dataConsumer = new StringDataConsumer() {
						Resource m_item;
						
						public IDataConsumer initialize(Resource item) {
							m_item = item;
							
							return this;
						}
						
						protected void onStringChanged(String newString) {
							onItemValueChanged(m_item, newString);
						}
	
						protected void onStringDeleted(String previousString) {
							onItemValueDeleted(m_item, previousString);
						}
					}.initialize((Resource)item);
					
					m_itemToDataProviderMap.put(item, dataProvider);
					
					dataProvider.registerConsumer(dataConsumer);
				}
			}
		}
	}
	
	synchronized protected void onItemsRemoved(Collection removedItems) {
		Iterator i = removedItems.iterator();
		
		while (i.hasNext()) {
			RDFNode item = (RDFNode) i.next();
			
			if (m_unsortedItems.contains(item)) {
				m_unsortedItems.remove(item);
			} else if (m_toSortItems.contains(item)) {
				m_toSortItems.remove(item);
				m_itemToValueMap.remove(item);
			} else if (m_sortedItems.contains(item)) {
				int 	index = m_sortedItems.indexOf(item);
				List	change = new ArrayList();
				
				m_sortedItems.remove(item);
				m_itemToValueMap.remove(item);
				
				change.add(item);
				
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index, 1, change));
			} else {
				continue;
			}
			
			IDataProvider dataProvider = (IDataProvider) m_itemToDataProviderMap.remove(item);
			if (dataProvider != null) {
				dataProvider.dispose();
			}
		}
	}
	
	synchronized protected void onCleared() {
		m_unsortedItems.clear();
		m_sortedItems.clear();
		m_itemToValueMap.clear();
		
		Iterator i = m_itemToDataProviderMap.values().iterator();
		while (i.hasNext()) {
			IDataProvider dataProvider = (IDataProvider) i.next();
			
			dataProvider.dispose();
		}
		
		notifyDataConsumers(DataConstants.LIST_CLEAR, null);
	}
	
	transient protected IdleRunnable m_runnable;
	protected void onItemValueChanged(Resource item, String newString) {
		if (m_initializing) {
			Object newValue = convertToObject(item, newString);
			
			m_itemToValueMap.put(item, newValue);
			
			m_unsortedItems.remove(item);
			m_toSortItems.add(item);
		} else {
			IdleRunnable runnable = null;

			synchronized (this) {
				Object oldValue = m_itemToValueMap.get(item);
				if (oldValue != null && !m_sortAgainWhenChanged) {
					return;
				}
				
				Object newValue = convertToObject(item, newString);
				if (newValue == null || newValue.equals(oldValue)) {
					return;
				}
				
				m_itemToValueMap.put(item, newValue);
				
				if (m_unsortedItems.contains(item)) {
					m_unsortedItems.remove(item);
					m_toSortItems.add(item);
				} else if (m_sortedItems.contains(item)) {
					int 	index = m_sortedItems.indexOf(item);
					List	change = new ArrayList();
					
					m_sortedItems.remove(index);
					change.add(item);
					
					notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index, 1, change));
					
					m_toSortItems.add(item);
				}
				
				if (m_runnable == null && m_toSortItems.size() > 0) {
					m_runnable = new IdleRunnable(m_context) {
						public void run() {
							synchronized (SortingDataProvider.this) {
								m_runnable = null;
								if (m_context != null) {
									internalSort();
								}
							}
						}
					};
					runnable = m_runnable;
				}
			}
			
			if (runnable != null) {
				Ozone.idleExec(runnable);
			}
		}
	}
	protected Object convertToObject(Resource item, String newString) {
		Object newValue = null;
		
		if (m_sortAsDateTime) {
			newValue = Utilities.parseDateTime(newString);
			if (newValue == null) {
				newValue = new Date();
			}
		} else if (m_sortAsFloat) {
			try {
				newValue = new Float(newString);
			} catch (NumberFormatException e) {
				s_logger.error("Unrecognized float " + newString + " for item " + item, e);
				newValue = newString;
			}
		} else if (m_sortCaseSensitive) {
			newValue = newString;
		} else {
			newValue = newString.toLowerCase();
		}
		
		return newValue;
	}
	
	synchronized protected void onItemValueDeleted(Resource item, String previousString) {
	}
	
	protected void notifyDataConsumers(Resource changeType, Object change) {
		Iterator i = m_dataConsumers.iterator();
		while (i.hasNext()) {
			IDataConsumer dataConsumer = (IDataConsumer) i.next();
			
			dataConsumer.onDataChanged(changeType, change);
		}
	}
	
	protected void internalSort() {
		if (m_sortedItems == null) {
			return;
		}
		
		if (m_sortedItems.size() == 0) {
			mergeSort(m_toSortItems, m_sortedItems);

			notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(0, m_sortedItems.size(), null));
		} else {
			ArrayList newItems = new ArrayList();
			
			mergeSort(m_toSortItems, newItems);
			binaryInsertion(newItems); // does notifications
		}
		m_toSortItems.clear();
	}
	
	protected void mergeSort(Set toSortItems, List sortedItems) {
		sortedItems.addAll(toSortItems);
		internalMergeSort(sortedItems, 0, sortedItems.size() - 1);
	}
	
	/*
	 * Adapted from http://www.cs.ubc.ca/spider/harrison/Java/MergeSortAlgorithm.java.html
	 */
	protected void internalMergeSort(List toSortItems, int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;

		if (lo >= hi) {
		    return;
		}
		int mid = (lo + hi) / 2;
	
        internalMergeSort(toSortItems, lo, mid);
        internalMergeSort(toSortItems, mid + 1, hi);
	
		int 		end_lo = mid;
        int 		start_hi = mid + 1;
		RDFNode	item1, item2;
		
		while ((lo <= end_lo) && (start_hi <= hi)) {
			item1 = (RDFNode) toSortItems.get(lo);
			item2 = (RDFNode) toSortItems.get(start_hi);
			
            if (compareItems(item1, item2) < 0) {
                lo++;
            } else {
                /*  
                 *  a[lo] >= a[start_hi]
                 *  The next element comes from the second list, 
                 *  move the toSortItems[start_hi] element into the next 
                 *  position and shuffle all the other elements up.
                 */
				item1 = (RDFNode) toSortItems.get(start_hi);
                for (int k = start_hi - 1; k >= lo; k--) {
                	toSortItems.set(k+1, toSortItems.get(k));
                }
                toSortItems.set(lo, item1);
                lo++;
                end_lo++;
                start_hi++;
	        }
	    }
	}
	
	protected int compareItems(RDFNode item1, RDFNode item2) {
		if (m_sortAsDateTime) {
			try {
				Date date1 = (Date) m_itemToValueMap.get(item1);
				Date date2 = (Date) m_itemToValueMap.get(item2);
				return date1.compareTo(date2) * m_sortOrder;
			} catch (Exception e) {
			}
		}
		
		if (m_sortAsFloat) {
			try {
				Float float1 = (Float) m_itemToValueMap.get(item1);
				Float float2 = (Float) m_itemToValueMap.get(item2);
				return float1.compareTo(float2) * m_sortOrder;
			} catch (Exception e) {
			}
		}
		
		// m_sortCaseSensitive
		String value1 = (String) (item1 instanceof Resource ? m_itemToValueMap.get(item1) : item1.getContent());
		String value2 = (String) (item2 instanceof Resource ? m_itemToValueMap.get(item2) : item2.getContent());
		return value1.compareTo(value2) * m_sortOrder;
	}
	
	protected void binaryInsertion(List newSortedItems) {
		int lo = 0;
		int hi = m_sortedItems.size();
		
		for (int i = 0; i < newSortedItems.size(); i++) {
			RDFNode newItem = (RDFNode) newSortedItems.get(i);
			
			while (lo < hi) {
				int middle = (lo + hi) / 2;
				
				RDFNode 	itemMiddle = (RDFNode) m_sortedItems.get(middle);
				int			compare = compareItems(newItem, itemMiddle);
				
				if (compare < 0) {
					hi = middle;
				} else {
					lo = middle + 1;
				}
			}
			
			m_sortedItems.add(lo, newItem);
			
			notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(lo, 1, null));
			
			hi = m_sortedItems.size() - 1;
		}
	}
	
	protected void onStatementAdded(Statement s) {
		int sortOrder = 1;
		if (s.getObject().getContent().equalsIgnoreCase("descending")) {
			sortOrder = -1;
		}
		
		IdleRunnable runnable = null;
		synchronized (this) {
			if (sortOrder != m_sortOrder && m_sortedItems.size() > 0) {
				List oldSortedItems = m_sortedItems;
				
				m_sortedItems = new ArrayList();
				
				m_toSortItems.addAll(oldSortedItems);
				
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(0, oldSortedItems.size(), oldSortedItems));
				
				oldSortedItems.clear();
			
				if (!m_initializing && m_runnable == null) {
					m_runnable = new IdleRunnable(m_context) {
						public void run() {
							synchronized (SortingDataProvider.this) {
								m_runnable = null;
								if (m_context != null) {
									internalSort();
								}
							}
						}
					};
					runnable = m_runnable;
				}
			}
			m_sortOrder = sortOrder;
		}
		
		if (runnable != null) {
			Ozone.idleExec(runnable);
		}
	}


	protected Class		m_itemDataProviderClass;
	protected Resource	m_sortDataProviderPart;
	
	protected IDataProvider createSortDataProvider(Context context) {
		if (m_itemDataProviderClass == null) {
			m_sortDataProviderPart = DataUtilities.findDataProvider(m_sortDataSource, m_source, m_partDataSource);
			if (m_sortDataProviderPart != null) {
				try {
					m_itemDataProviderClass = Utilities.loadClass(m_sortDataProviderPart, m_source);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}

		if (m_itemDataProviderClass != null) {
			IDataProvider dataProvider;
			try {
				dataProvider =
					(IDataProvider) m_itemDataProviderClass.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
				
			context.putLocalProperty(OzoneConstants.s_partData, m_sortDataSource);
			context.putLocalProperty(OzoneConstants.s_part, m_sortDataProviderPart);
			
			dataProvider.initialize(m_source, context);
			
			return dataProvider;
		}
		System.out.println(">> failed to create sort data provider");
		return null;
	}
}
