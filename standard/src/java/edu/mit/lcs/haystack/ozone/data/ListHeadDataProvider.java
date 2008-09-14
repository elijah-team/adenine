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
import java.util.Iterator;
import java.util.List;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * A List Data Provider for the first n items in the (input) list. This class
 * is also a consumer of a list data provider, which is then provided
 * to other consumers. 
 * 
 * @author Vineet Sinha
 */
public class ListHeadDataProvider extends GenericPart implements IDataProvider, IDataConsumer {

	protected ArrayList m_dataConsumers = new ArrayList();
	protected IDataProvider m_dataProvider;
	protected boolean m_ownsDataProvider = false;

	transient protected RDFListener m_rdfListener;

	int m_headItemsCount = 5;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ListHeadDataProvider.class);

	final static Resource s_itemsCount = new Resource(DataConstants.NAMESPACE + "itemsCount");


	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_ownsDataProvider) {
			m_dataProvider.initializeFromDeserialization(source);
		}
		createHeadCountListener(m_prescription);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	synchronized public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);

		Resource dataSource = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);

		String s = Utilities.getLiteralProperty(dataSource, s_itemsCount, m_source);
		if (s != null) {
			m_headItemsCount = Integer.parseInt(s);
		}

		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource chainedDataSource =
				Utilities.getResourceProperty(dataSource, OzoneConstants.s_dataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_dataProvider =
					DataUtilities.createDataProvider(chainedDataSource, m_context, m_source, m_partDataSource);
				m_ownsDataProvider = true;
			}
		}

		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(this);
		}

		createHeadCountListener(dataSource);

		List outList = getOutputList();
		notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(0, outList.size(), outList));
	}

	protected void createHeadCountListener(Resource dataSource) {
		m_rdfListener =
			new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource) m_partDataSource) {
			public void statementAdded(Resource cookie, Statement s) {
				onChangeHeadItemsCount(Integer.parseInt(s.getObject().getContent()));
			}
		};
		m_rdfListener.start();
	
		try {
			m_rdfListener.addPattern(dataSource, s_itemsCount, null);
		} catch (Exception e) {
			s_logger.error("Failed to add pattern to watch sort order", e);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {

		if (m_dataProvider != null) {
			m_dataProvider.unregisterConsumer(this);
			if (m_ownsDataProvider) {
				m_dataProvider.dispose();
			}
			m_dataProvider = null;
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
	public void registerConsumer(IDataConsumer dataConsumer) {
		synchronized (m_dataConsumers) {
			if (dataConsumer == null) {
				s_logger.error("registerConsumer called with consumer null", new Exception());
				return;
			}

			dataConsumer.reset();
			m_dataConsumers.add(dataConsumer);

			List outputList = getOutputList();
			if (outputList.size() > 0) {
				dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(0, outputList.size(), outputList));
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
		synchronized (m_dataConsumers) {
			if (dataConsumer != null && m_dataConsumers.contains(dataConsumer)) {
				m_dataConsumers.remove(dataConsumer);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications) throws DataNotAvailableException {
		List dataProviderList = (List) m_dataProvider.getData(DataConstants.LIST, null);
		int listCount = Math.min(m_headItemsCount, dataProviderList.size());
		
		if (dataType == null || dataType.equals(DataConstants.LIST)) {
			return dataProviderList.subList(0, listCount);
		} else if (dataType.equals(DataConstants.LIST_COUNT)) {
			return new Integer(listCount);
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return dataProviderList.get(((Integer) specifications).intValue());
			} catch (Exception e) {
				s_logger.error("Failed to get element at " + specifications, e);
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {

			Object[] a = (Object[]) specifications;
			int index = ((Integer) a[0]).intValue();
			int count = ((Integer) a[1]).intValue();

			try {
				ArrayList elements = new ArrayList();

				elements.addAll(dataProviderList.subList(index, index + count));

				return elements;
			} catch (Exception e) {
				s_logger.error("Failed to get elements index=" + index + " count=" + count, e);
			}
		}

		return null;
	}

	protected List getOutputList() {
		try {
			List dataProviderList = (List) m_dataProvider.getData(DataConstants.LIST, null);
			int listCount = Math.min(m_headItemsCount, dataProviderList.size());
			return dataProviderList.subList(0, listCount);
		} catch (DataNotAvailableException e) {
			return null;
		}
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
			
		m_dataProvider.requestChange(changeType, change);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return m_dataProvider.supportsChange(changeType);
	}

	public void onChangeHeadItemsCount(int newHeadItemsCount) {		

		if (newHeadItemsCount == m_headItemsCount) {
			return;
		}

		List dataProviderList = null;
		try {
			dataProviderList = (List) m_dataProvider.getData(DataConstants.LIST, null);
			//int listCount = Math.min(m_headItemsCount, dataProviderList.size());
		} catch (DataNotAvailableException e) {
			return;
		}

		int oldListCount = Math.min(m_headItemsCount, dataProviderList.size());
		int newListCount = Math.min(newHeadItemsCount, dataProviderList.size());

		if (newHeadItemsCount > m_headItemsCount) {
			if (m_headItemsCount > dataProviderList.size()) {
				m_headItemsCount = newHeadItemsCount;
				return;
			}

			// size has increased
			notifyDataConsumers(
				DataConstants.LIST_ADDITION,
				new ListDataChange(
					oldListCount,
					newListCount - oldListCount,
					dataProviderList.subList(oldListCount, newListCount)));
			m_headItemsCount = newHeadItemsCount;
			return;
		}

		//if (newHeadItemsCount < m_headItemsCount) 
		{
			if (newHeadItemsCount > dataProviderList.size()) {
				m_headItemsCount = newHeadItemsCount;
				return;
			}
			// size has decreased
			notifyDataConsumers(
				DataConstants.LIST_REMOVAL,
				new ListDataChange(
					newListCount,
					oldListCount - newListCount,
					dataProviderList.subList(newListCount, oldListCount)));
			m_headItemsCount = newHeadItemsCount;
			return;
		}

	}

	protected void notifyDataConsumers(Resource changeType, Object change) {
		synchronized (m_dataConsumers) {
			Iterator i = m_dataConsumers.iterator();
			while (i.hasNext()) {
				IDataConsumer dataConsumer = (IDataConsumer) i.next();
	
				dataConsumer.onDataChanged(changeType, change);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#reset()
	 */
	public void reset() {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public void onStatusChanged(Resource status) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onDataChanged(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void onDataChanged(Resource changeType, Object change) throws IllegalArgumentException {
		if (changeType.equals(DataConstants.LIST_ADDITION)) {
			onItemsAdded((ListDataChange) change);
		} else if (changeType.equals(DataConstants.LIST_REMOVAL)) {
			onItemsRemoved((ListDataChange) change);
		} else if (changeType.equals(DataConstants.LIST_CLEAR)) {
			notifyDataConsumers(DataConstants.LIST_CLEAR, null);
		} else {
			throw new IllegalArgumentException("Unsupported change type " + changeType);
		}
	}

	synchronized protected void onItemsAdded(ListDataChange change) {
		ListDataChange newChange = null;
		
		if (change.m_elements == null) {
			newChange = change; // can't do much, just propogate notification
		} else if (change.m_indices == null) {
			if (change.m_index < m_headItemsCount) {
				int newCount = Math.min(m_headItemsCount, change.m_index + change.m_count) - change.m_index;
				newChange = new ListDataChange(change.m_index, newCount, change.m_elements.subList(0, newCount));
			}
		} else {
			List newElements = new ArrayList();
			List newIndices = new ArrayList();
			for (int i = 0; i < change.m_indices.size(); i++) {
				Integer index = (Integer) change.m_indices.get(i);
				if (index.intValue() < m_headItemsCount) {
					newIndices.add(index);
					newElements.add(change.m_elements.get(i));
				}
			}
			if (newElements.size() > 0) {
				newChange = new ListDataChange(newIndices, newElements);
			}
		}

		if (newChange != null) {
			notifyDataConsumers(DataConstants.LIST_ADDITION, newChange);
		}

	}

	synchronized protected void onItemsRemoved(ListDataChange change) {
		if (change.m_indices == null) {
			if (change.m_index < m_headItemsCount) {
				int newCount = Math.min(m_headItemsCount, change.m_index + change.m_count) - change.m_index;
				ListDataChange newChange =
					new ListDataChange(change.m_index, newCount, change.m_elements.subList(0, newCount));
				notifyDataConsumers(DataConstants.LIST_REMOVAL, newChange);
			}
		} else {
			List newElements = new ArrayList();
			List newIndices = new ArrayList();
			for (int i = 0; i < change.m_indices.size(); i++) {
				Integer index = (Integer) change.m_indices.get(i);
				if (index.intValue() < m_headItemsCount) {
					newIndices.add(index);
					newElements.add(change.m_elements.get(i));
				}
			}
			if (newElements.size() > 0) {
				ListDataChange newChange = new ListDataChange(newIndices, newElements);
				notifyDataConsumers(DataConstants.LIST_REMOVAL, newChange);
			}
		}
	}


}
