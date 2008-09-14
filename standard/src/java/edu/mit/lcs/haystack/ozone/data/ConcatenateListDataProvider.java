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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.GenericDataProvider;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ListDataChange;
import edu.mit.lcs.haystack.ozone.data.ListDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ListDataProviderWrapper;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * Concatenates a list of lists and resource data providers into a single list data provider.
 * @author David Huynh
 */
public class ConcatenateListDataProvider extends GenericDataProvider {
	protected List		m_data = new ArrayList();
	protected List		m_sources = new ArrayList();
	protected HashSet	m_sourcesToBeDisposed = new HashSet();

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ConcatenateListDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (!m_data.isEmpty()) {
			ArrayList data = new ArrayList();
			synchronized (m_data) {
				data.addAll(m_data);
			}
			dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(0, data.size(), data));
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.LIST)) {
			synchronized (m_data) {
				return new ArrayList(m_data);
			}
		} else if (dataType.equals(DataConstants.LIST_COUNT)) {
			synchronized (m_data) {
				return new Integer(m_data.size());
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			int i = ((Integer) specifications).intValue();
			
			synchronized(m_data) {
				if (i >= 0 && i < m_data.size()) {
					return m_data.get(i);
				}
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();
			
			try {
				ArrayList 	elements = new ArrayList();
				
				synchronized (m_data) {
					elements.addAll(m_data.subList(index, index + count));
				}

				return elements;
			} catch (Exception e) {
				s_logger.error(e);
			}
		} else if (dataType.equals(DataConstants.SET)) {
			synchronized (m_data) {
				return new HashSet(m_data);
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
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		Resource dataSources = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSources, m_partDataSource);
		if (dataSources != null) {
			Iterator i = ListUtilities.accessDAMLList(dataSources, m_partDataSource);
			while (i.hasNext()) {
				Resource dataSource = (Resource) i.next();
				
				IDataProvider dp = DataUtilities.createDataProvider(dataSource, m_context, m_partDataSource);
				
				setupConsumer(dp);
			}
		}
	}
	
	class MyListDataConsumer extends ListDataConsumer {
		protected void onElementsAdded(int index, int count) {
			synchronized (ConcatenateListDataProvider.this) {
				try {
					List newElements = m_dpw.getElements(index, count);
					int index2 = countPreviousElements(m_index);
					
					m_data.addAll(index2 + index, newElements);
					
					notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(index2 + index, newElements.size(), newElements));
				} catch (DataNotAvailableException e) {
					s_logger.error(e);
				} catch (DataMismatchException e) {
					s_logger.error(e);
				}
			}
		}
	
		protected void onElementsChanged(int index, int count) {
		}
	
		protected void onElementsChanged(List changedIndices) {
		}
	
		protected void onElementsRemoved(
			int index,
			int count,
			List removedElements) {

			synchronized (ConcatenateListDataProvider.this) {
				int index2 = countPreviousElements(m_index);
			
				for (int i = 0; i < count; i++) {
					m_data.remove(index2 + index);
				}
			
				notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index2 + index, count, removedElements));
			}
		}
	
		protected void onListCleared() {
			onClear(m_index);	
		}
				
		int						m_index;
		ListDataProviderWrapper	m_dpw;
				
		public IDataConsumer init(ListDataProviderWrapper dpw, int index) {
			m_dpw = dpw;
			m_index = index;
			return this;
		}
	}

	protected void setupConsumer(IDataProvider dp) {
		int i = m_sources.size();
		
		ListDataProviderWrapper dpw = new ListDataProviderWrapper(dp); 
		
		dp.registerConsumer(new MyListDataConsumer().init(dpw, i));

		m_sources.add(dpw);
		m_sourcesToBeDisposed.add(dpw);
	}
	
	protected int countPreviousElements(int index) {
		int c = 0;
		for (int i = 0; i < index; i++) {
			try {
				c += ((ListDataProviderWrapper) m_sources.get(i)).getListCount();
			} catch (DataNotAvailableException e) {
				s_logger.error(e);
			} catch (DataMismatchException e) {
				s_logger.error(e);
			}
		}
		return c;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		Iterator i = m_sourcesToBeDisposed.iterator();
		while (i.hasNext()) {
			ListDataProviderWrapper dpw = (ListDataProviderWrapper)i.next();
			dpw.dispose();
		}
		m_sourcesToBeDisposed.clear();
	}
	
	synchronized protected void onClear(int index) {
		int before = 0;
		int remaining = 0;
		
		for (int i = 0; i < index; i++) {
			try {
				before += ((ListDataProviderWrapper) m_sources.get(i)).getListCount();
			} catch (DataNotAvailableException e) {
				s_logger.error(e);
			} catch (DataMismatchException e) {
				s_logger.error(e);
			}
		}
		
		remaining = before;
		
		for (int j = index + 1; j < m_data.size(); j++) {
			try {
				remaining += ((ListDataProviderWrapper) m_sources.get(j)).getListCount();
			} catch (DataNotAvailableException e) {
				s_logger.error(e);
			} catch (DataMismatchException e) {
				s_logger.error(e);
			}
		}

		int removed = m_data.size() - remaining;
		
		for (int j = removed; j > 0; j--) {
			m_data.remove(before);
		}
		
		notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(before, removed, null));
	}
}
