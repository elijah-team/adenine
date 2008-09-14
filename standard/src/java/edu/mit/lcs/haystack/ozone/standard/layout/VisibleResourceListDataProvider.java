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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.*;

/**
 * @author David Huynh
 */
public class VisibleResourceListDataProvider extends GenericPart implements IDataProvider {
	protected ArrayList					m_dataConsumers = new ArrayList();
	
	protected ListDataProviderWrapper	m_dataProviderWrapper;
	protected ListDataConsumer			m_dataConsumer;
	
	transient protected RDFListener		m_rdfListener;
	
	protected ArrayList					m_resources = new ArrayList();
	protected ArrayList					m_cookies = new ArrayList();
	protected ArrayList					m_visibles = new ArrayList();
	protected ArrayList					m_visibleResources = new ArrayList();

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(VisibleResourceListDataProvider.class);

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		Resource dataSource = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);
		Resource chainedDataSource = Utilities.getResourceProperty(dataSource, OzoneConstants.s_dataSource, m_source);
		
		if (chainedDataSource != null) {
			IDataProvider dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_source);
			
			if (dataProvider != null) {
				m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_source) {
					public void statementAdded(Resource cookie, Statement s) {
						onStatementAdded(s);
					}
					public void statementRemoved(Resource cookie, Statement s) {
						onStatementRemoved(s);
					}
				};
				m_rdfListener.start();

				m_dataProviderWrapper = new ListDataProviderWrapper(dataProvider);
				m_dataConsumer = new ListDataConsumer() {
					protected void onElementsAdded(int index, int count) {
						handleElementsAdded(index, count);
					}

					protected void onElementsChanged(int index, int count) {
						handleElementsChanged(index, count);
					}
					
					protected void onElementsChanged(List changedIndices) {
						s_logger.error("Unimplemented handler for list change by indices", new Exception());
					}

					protected void onElementsRemoved(
						int index,
						int count,
						List removedElements) {
						handleElementsRemoved(index, count, removedElements);
					}
					
					protected void onListCleared() {
						handleListCleared();
					}
				};
				dataProvider.registerConsumer(m_dataConsumer);				
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_dataProviderWrapper != null) {
			m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_source) {
				public void statementAdded(Resource cookie, Statement s) {
					onStatementAdded(s);
				}
				public void statementRemoved(Resource cookie, Statement s) {
					onStatementRemoved(s);
				}
			};
			m_rdfListener.start();
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().unregisterConsumer(m_dataConsumer);
			m_dataProviderWrapper.getDataProvider().dispose();
			m_rdfListener.stop();
			
			m_dataProviderWrapper = null;
			m_dataConsumer = null;
			m_rdfListener = null;
		}
		
		m_dataConsumers.clear();		
		m_resources.clear();
		m_visibles.clear();
		m_visibleResources.clear();
		
		m_dataConsumers = null;
		m_resources = null;
		m_visibles = null;
		m_visibleResources = null;
		
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
		
		dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(0, m_visibleResources.size(), m_visibleResources));
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
			return m_visibleResources;
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_visibleResources.get(((Integer) specifications).intValue());
			} catch (Exception e) {
				s_logger.error("Failed to get element " + specifications);
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();
			
			try {
				ArrayList 	elements = new ArrayList();
				
				elements.addAll(m_visibleResources.subList(index, index + count));

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
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		throw new UnsupportedOperationException("Visible resource list data provider supports no change operation");
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
	
	synchronized protected void handleElementsAdded(int index, int count) {
		List newList;
		try {
			newList = m_dataProviderWrapper.getList();
		} catch (DataNotAvailableException e) {
			return;
		} catch (DataMismatchException e) {
			return;
		}
		
		List subList = newList.subList(index, index + count);
		
		int visibleIndex = -1;
		int visibleCount = 0;
		
		if (index >= m_resources.size()) {
			visibleIndex = m_visibleResources.size();
		} else {
			for (int i = index; i < m_resources.size(); i++) {
				Resource res = (Resource) m_resources.get(i);
				
				visibleIndex = m_visibleResources.indexOf(res);
				if (visibleIndex >= 0) {
					break;
				}
			}
			
			if (visibleIndex == -1) {
				visibleIndex = m_visibleResources.size();
			}
		}
		
		Iterator i = subList.iterator();
		while (i.hasNext()) {
			Resource resource = (Resource) i.next();
			
			boolean visible = Utilities.checkBooleanProperty(resource, PartConstants.s_visible, m_source, true);
			if (visible) {
				m_visibleResources.add(visibleIndex, resource);
				visibleIndex++;
				visibleCount++;
			}
			
			m_resources.add(index, resource);
			try {
				Resource cookie = m_rdfListener.addPattern(resource, PartConstants.s_visible, null);
				
				m_cookies.add(index, cookie);
			} catch (Exception e) {
				s_logger.error("Failed to watch for ozone:visible on " + resource, e);
				m_cookies.add(index, null);
			}
			index++;			
		}
		
		if (visibleCount > 0) {
			i = m_dataConsumers.iterator();
			while (i.hasNext()) {
				IDataConsumer dataConsumer = (IDataConsumer) i.next();
				
				dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(visibleIndex, visibleCount, null));
			}
		}
	}
	
	synchronized protected void handleElementsChanged(int index, int count) {
	}
	
	synchronized protected void handleElementsRemoved(int index, int count, List removedElements) {
		int visibleIndex = -1;
		int visibleCount = 0;
		
		for (int i = 0; i < count; i++) {
			Resource resource = (Resource) removedElements.get(i);
			
			visibleIndex = m_visibleResources.indexOf(resource);
			if (visibleIndex >= 0) {
				break;
			}
		}
		if (visibleIndex >= 0) {
			int i;
			for (i = visibleIndex; i < m_visibleResources.size(); i++) {
				Resource resource = (Resource) m_visibleResources.get(i);
				
				if (removedElements.indexOf(resource) < 0) {
					break;
				}
			}
			visibleCount = i - visibleIndex;

			ArrayList	visibleRemoved = new ArrayList();
			
			visibleRemoved.addAll(m_visibleResources.subList(visibleIndex, i));
			
			Iterator it = m_dataConsumers.iterator();
			while (it.hasNext()) {
				IDataConsumer dataConsumer = (IDataConsumer) it.next();
				
				dataConsumer.onDataChanged(DataConstants.LIST_REMOVAL, new ListDataChange(visibleIndex, visibleCount, visibleRemoved));
			}
		}
		
		for (int i = 0; i < count; i++) {
			Resource resource = (Resource) m_resources.get(index);
			
			m_rdfListener.removePattern((Resource) m_cookies.get(index));
			
			m_resources.remove(index);
			m_cookies.remove(index);
		}
	}
	
	synchronized protected void handleListCleared() {
		Iterator i = m_cookies.iterator();
		while (i.hasNext()) {
			m_rdfListener.removePattern((Resource) i.next());
		}
		
		m_cookies.clear();
		m_resources.clear();
		m_visibleResources.clear();

		Iterator it = m_dataConsumers.iterator();
		while (it.hasNext()) {
			IDataConsumer dataConsumer = (IDataConsumer) it.next();

			dataConsumer.onDataChanged(DataConstants.LIST_CLEAR, null);
		}
	}

	synchronized protected void onStatementAdded(Statement s) {
		int index = m_resources.indexOf(s.getSubject());
		int visibleIndex = m_visibleResources.indexOf(s.getSubject());
		
		boolean	previousState = visibleIndex >= 0;
		boolean 	currentState = Utilities.checkBooleanProperty(s.getSubject(), PartConstants.s_visible, m_source, true);
		if (previousState == currentState) {
			return;
		}
		
		onVisibleChange(s.getSubject(), currentState, index, visibleIndex);		
	}
	
	synchronized protected void onStatementRemoved(Statement s) {
		int index = m_resources.indexOf(s.getSubject());
		int visibleIndex = m_visibleResources.indexOf(s.getSubject());
		
		boolean	previousState = visibleIndex >= 0;
		boolean 	currentState = Utilities.checkBooleanProperty(s.getSubject(), PartConstants.s_visible, m_source, true);
		if (previousState == currentState) {
			return;
		}
		
		onVisibleChange(s.getSubject(), currentState, index, visibleIndex);
	}
	
	protected void onVisibleChange(Resource resource, boolean visible, int index, int visibleIndex) {
		if (visible) {
			visibleIndex = -1;
			for (int n = index + 1; n < m_resources.size(); n++) {
				visibleIndex = m_visibleResources.indexOf(m_resources.get(n));
				if (visibleIndex >= 0) {
					break;
				}
			}
			if (visibleIndex < 0) {
				visibleIndex = m_visibleResources.size();
			}
			
			m_visibleResources.add(visibleIndex, resource);

			Iterator i = m_dataConsumers.iterator();
			while (i.hasNext()) {
				IDataConsumer dataConsumer = (IDataConsumer) i.next();
				
				dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(visibleIndex, 1, null));
			}
		} else {
			ArrayList visibleRemoved = new ArrayList();
			
			visibleRemoved.add(resource);
			m_visibleResources.remove(visibleIndex);

			Iterator i = m_dataConsumers.iterator();
			while (i.hasNext()) {
				IDataConsumer dataConsumer = (IDataConsumer) i.next();
				
				dataConsumer.onDataChanged(DataConstants.LIST_REMOVAL, new ListDataChange(visibleIndex, 1, visibleRemoved));
			}
		}
	}
}
