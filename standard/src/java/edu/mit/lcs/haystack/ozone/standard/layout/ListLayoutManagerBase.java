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

package edu.mit.lcs.haystack.ozone.standard.layout;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.*;

/**
 * @author David Huynh
 * @author Dennis Quan
 */
abstract public class ListLayoutManagerBase extends LayoutManagerBase {
	protected Resource				m_groupingDataSource;
	protected Resource				m_sortingDataSource;
	
	protected IDataConsumer			m_dataConsumer;
	protected ListDataProviderWrapper	m_dataProviderWrapper;
	protected IDataProvider			m_groupingDataProvider;
	protected IDataProvider			m_sortingDataProvider;
	protected ListDataProviderWrapper	m_listDataProviderWrapper;
	protected boolean					m_supportsListInsertion;
	protected boolean					m_supportsSetAddition;
	transient protected RDFListener				m_rdfListener;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ListLayoutManagerBase.class);

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_groupingDataProvider != null) {
			m_groupingDataProvider.initializeFromDeserialization(source);
		}
		
		if (m_sortingDataProvider != null) {
			m_sortingDataProvider.initializeFromDeserialization(source);
		}
	}

	protected void makeDataConsumers() {
		internalMakeDataConsumers();

		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource) m_source) {
			public void statementAdded(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					Statement m_s;
					public void run() {
						if (m_context != null && !m_initializing) {
							onStatementAdded(m_s);
						}
					}
					public IdleRunnable init(Statement s) {
						m_s = s;
						return this;
					}
				}.init(s));
			}
		};
		m_rdfListener.start();
		
		try {
			m_rdfListener.addPattern(m_prescription, LayoutConstants.s_groupBy, null);
			m_rdfListener.addPattern(m_prescription, LayoutConstants.s_sortBy, null);
		} catch (Exception e) {
			s_logger.error("Failed to watch for groupBy and sortBy on layout constraint " + m_prescription, e);
		}
	}
	
	protected void internalMakeDataConsumers() {
		disposeDataConsumers();

		m_dataProviderWrapper = new ListDataProviderWrapper(m_dataProvider);
		m_dataConsumer = new ListDataConsumer() {
			protected void onElementsAdded(int index, int count) {
				ListLayoutManagerBase.this.onElementsAdded(index, count);
			}
			
			protected void onElementsRemoved(int index, int count, List removedElements) {
				ListLayoutManagerBase.this.onElementsRemoved(index, count, removedElements);
			}
			
			protected void onElementsChanged(int index, int count) {
				ListLayoutManagerBase.this.onElementsChanged(index, count);
			}
			
			protected void onElementsChanged(List changedIndices) {
				s_logger.error("Unimplemented handler for list change by indices", new Exception());
			}

			protected void onListCleared() {
				ListLayoutManagerBase.this.onListCleared();
			}
		};

		m_groupingDataSource = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_groupBy, m_partDataSource);
		if (m_groupingDataSource != null) {
			Context context = new Context(m_context);
			context.putLocalProperty(OzoneConstants.s_dataProvider, m_dataProvider);
			m_groupingDataProvider = DataUtilities.createDataProvider2(m_groupingDataSource, context, m_source, m_partDataSource);
		}

		m_sortingDataSource = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_sortBy, m_partDataSource);
		if (m_sortingDataSource != null) {
			Context context = new Context(m_context);
			if (m_groupingDataProvider != null) {
				context.putLocalProperty(OzoneConstants.s_dataProvider, m_groupingDataProvider);
			} else {
				context.putLocalProperty(OzoneConstants.s_dataProvider, m_dataProvider);
			}

			m_sortingDataProvider = DataUtilities.createDataProvider2(m_sortingDataSource, context, m_source, m_partDataSource);
		}
		
		m_dataProviderWrapper = new ListDataProviderWrapper(m_dataProvider);
		if (m_sortingDataProvider != null) {
			m_listDataProviderWrapper = new ListDataProviderWrapper(m_sortingDataProvider);
			m_sortingDataProvider.registerConsumer(m_dataConsumer);
		} else if (m_groupingDataProvider != null) {
			m_listDataProviderWrapper = new ListDataProviderWrapper(m_groupingDataProvider);
			m_groupingDataProvider.registerConsumer(m_dataConsumer);
		} else {
			m_listDataProviderWrapper = new ListDataProviderWrapper(m_dataProvider);
			m_dataProvider.registerConsumer(m_dataConsumer);
		}
		
		m_supportsListInsertion = m_listDataProviderWrapper.supportsListInsertion();
		m_supportsSetAddition = m_listDataProviderWrapper.getDataProvider().supportsChange(DataConstants.SET_ADDITION);
	}

	protected void disposeDataConsumers() {
		if (m_dataConsumer != null) {
			if (m_sortingDataProvider != null) {
				m_sortingDataProvider.unregisterConsumer(m_dataConsumer);
			} else if (m_groupingDataProvider != null) {
				m_groupingDataProvider.unregisterConsumer(m_dataConsumer);
			} else if (m_dataProvider != null) {
				m_dataProvider.unregisterConsumer(m_dataConsumer);
			}
			m_dataConsumer = null;
		}
		if (m_groupingDataProvider != null) {
			m_groupingDataProvider.dispose();
			m_groupingDataProvider = null;
		}
		if (m_sortingDataProvider != null) {
			m_sortingDataProvider.dispose();
			m_sortingDataProvider = null;
		}
		m_dataProviderWrapper = null;
		
		m_groupingDataSource = null;
		m_sortingDataSource = null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_dataConsumer != null) {
			m_dataProvider.unregisterConsumer(m_dataConsumer);
			m_dataConsumer = null;
		}
		m_dataProviderWrapper = null;
		
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}
		
		super.dispose();
	}

	final static protected int	s_elementsAdded = 0;
	final static protected int	s_elementsRemoved = 1;
	final static protected int	s_elementsChanged = 2;
	final static protected int	s_listCleared = 3;
	
	protected class ElementsEvent extends IdleRunnable {
		int	m_event;
		int	m_index;
		List	m_elements;
		
		public ElementsEvent(int event, int index, List elements) {
			super(m_context);
			m_event = event;
			m_index = index;
			m_elements = elements;
		}
		
		public void run() {
			if (m_dataConsumer == null) {
				return;
			}
			
			switch (m_event) {
			case s_elementsAdded:
				processElementsAdded(m_index, m_elements);
				break;
			case s_elementsRemoved:
				processElementsRemoved(m_index, m_elements);
				break;
			case s_elementsChanged:
				processElementsChanged(m_index, m_elements);
				break;
			case s_listCleared:
				processListCleared();
				break;
			}
		}
	}

	protected void onElementsAdded(int index, int count) {
		try {
			List newElements = m_listDataProviderWrapper.getElements(index, count);

			if (m_initializing && Ozone.isUIThread()) {
				processElementsAdded(index, newElements);
			} else {
				Ozone.idleExec(new ElementsEvent(s_elementsAdded, index, newElements));
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("List data not available when notification of added elements received", e);
		} catch (DataMismatchException e) {
			s_logger.error("List data not provided by data provider", e);
		} catch (Exception e) {
			s_logger.error("Unknown exception from data provider " + m_listDataProviderWrapper.getDataProvider(), e);
		}
	}
	
	protected void onElementsRemoved(int index, int count, List removedElements) {
		if (m_initializing && Ozone.isUIThread()) {
			processElementsRemoved(index, removedElements);
		} else {
			Ozone.idleExec(new ElementsEvent(s_elementsRemoved, index, removedElements));
		}
	}
	
	protected void onElementsChanged(int index, int count) {
		try {
			List		allElements = m_listDataProviderWrapper.getList();
			ArrayList	changedElements = new ArrayList();

			changedElements.addAll(allElements.subList(index, index + count));
		
			if (m_initializing && Ozone.isUIThread()) {
				processElementsChanged(index, changedElements);
			} else {
				Ozone.idleExec(new ElementsEvent(s_elementsAdded, index, changedElements));
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("List data not available when notification of changed elements received", e);
		} catch (DataMismatchException e) {
			s_logger.error("List data not provided by data provider", e);
		}
	}
	
	protected void onListCleared() {
		if (m_initializing && Ozone.isUIThread()) {
			processListCleared();
		} else {
			Ozone.idleExec(new ElementsEvent(s_listCleared, 0, null));
		}
	}
	
	protected void processElementsAdded(int index, List addedElements) {
	}
	protected void processElementsRemoved(int index, List removedElements) {
	}
	protected void processElementsChanged(int index, List changedElements) {
	}
	protected void processListCleared() {
	}
	
	protected void onStatementAdded(Statement s) {
		if ((s.getPredicate().equals(LayoutConstants.s_groupBy) && !s.getObject().equals(m_groupingDataSource)) ||
			(s.getPredicate().equals(LayoutConstants.s_sortBy) && !s.getObject().equals(m_sortingDataSource))) {
			try {
				List allElements = m_listDataProviderWrapper.getList();
	
				processElementsRemoved(0, allElements);
	
				internalMakeDataConsumers();
			} catch (Exception e) {
				s_logger.error("Failed to get elements from data provider " + m_listDataProviderWrapper.getDataProvider(), e);
			}
		}
	}
}
