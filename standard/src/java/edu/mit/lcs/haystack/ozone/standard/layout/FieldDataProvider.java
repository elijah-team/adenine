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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.data.*;

import java.util.*;

/**
 * @author David Huynh
 */
class CustomFieldData extends FieldData {
	public float 		m_dimensionInPercent = -1;
	
	protected CustomFieldData() {}
	
	public CustomFieldData(Resource fieldData, Resource fieldID) {
		super(fieldData, fieldID, 0, false);
	}
	
	public void setDimension(int dimension) {
		m_dimension = dimension;
	}
	
	public void setResizable(boolean resizable) {
		m_resizable = resizable;
	}
}
	
public class FieldDataProvider extends GenericPart implements IDataProvider {
	protected ArrayList 				m_fieldDataList = new ArrayList();
	protected ArrayList				m_dataConsumers = new ArrayList();
	
	protected ListDataConsumer		m_dataConsumer;
	protected ListDataProviderWrapper	m_dataProviderWrapper;
	
	protected int						m_dimension;
	
	final static Resource s_fieldID = new Resource(LayoutConstants.s_namespace + "fieldID");
	final static Resource s_fieldDimension = new Resource(LayoutConstants.s_namespace + "fieldDimension");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FieldDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);

		Resource dataSource = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource != null) {
			IDataProvider dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
			if (dataProvider != null) {
				m_dataProviderWrapper = new ListDataProviderWrapper(dataProvider);
				m_dataConsumer = new ListDataConsumer() {
					protected void onElementsAdded(int index, int count) {
						try {
							List addedElements = m_dataProviderWrapper.getElements(index, count);
							
							if (m_initializing) {
								FieldDataProvider.this.onElementsAdded(index, addedElements);
							} else {
								Ozone.idleExec(new AddRunnable(index, addedElements));
							}
						} catch (Exception e) {
							s_logger.error("Failed to get new elements", e);
						}
					}

					protected void onElementsChanged(int index, int count) {
					}
					
					protected void onElementsChanged(List changedIndices) {
					}

					protected void onElementsRemoved(
						int index,
						int count,
						java.util.List removedElements) {
						if (m_initializing) {
							FieldDataProvider.this.onElementsRemoved(index, count);
						} else {
							Ozone.idleExec(new RemoveRunnable(index, count));
						}
					}
					
					protected void onListCleared() {
						if (m_initializing) {
							FieldDataProvider.this.onListCleared();
						} else {
							Ozone.idleExec(new ClearRunnable());
						}
					}

					public void reset() {
					}
				};
				dataProvider.registerConsumer(m_dataConsumer);
			} else {
				s_logger.error("Failed to connect to data provider: " + dataSource);
			}
		}
		m_initializing = false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		m_fieldDataList.clear();
		m_fieldDataList = null;

		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.dispose();
			m_dataProviderWrapper = null;
		}
		m_dataConsumer = null;
				
		m_dataConsumers.clear();
		m_dataConsumers = null;

		super.dispose();		
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null) {
			dataConsumer.reset();
			
			if (m_fieldDataList.size() > 0) {
				dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(0, m_fieldDataList.size(), m_fieldDataList));
			}
			
			m_dataConsumers.add(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
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
			return m_fieldDataList.clone();
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_fieldDataList.get(((Integer) specifications).intValue());
			} catch (Exception e) {
				s_logger.error("Failed to get element " + specifications, e);
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();

			try {
				ArrayList 	elements = new ArrayList();
				
				elements.addAll(m_fieldDataList.subList(index, index + count));

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
		
		if (changeType.equals(DataConstants.LIST_CHANGE)) {
			ListDataChange	listDataChange = (ListDataChange) change;
			
			ArrayList		indices = new ArrayList();
			
			for (int i = 0; i < listDataChange.m_count; i++) {
				CustomFieldData fieldData = (CustomFieldData) m_fieldDataList.get(listDataChange.m_index + i);
				Object			data = listDataChange.m_elements.get(i);
				
				if (data instanceof Integer) {
					int dimension = ((Integer) data).intValue();
					
					if (dimension > 0) {
						fieldData.m_dimension = dimension;
						
						if (fieldData.m_dimensionInPercent >= 0) {
							fieldData.m_dimensionInPercent = dimension * 100 / m_dimension;
							
							try {
								m_partDataSource.replace(fieldData.m_fieldData, s_fieldDimension, null, new Literal(Float.toString(fieldData.m_dimensionInPercent) + "%"));
							} catch (RDFException e) {
							}
						} else {
							try {
								m_partDataSource.replace(fieldData.m_fieldData, s_fieldDimension, null, new Literal(Integer.toString(dimension)));
							} catch (RDFException e) {
							}
						}
						
						indices.add(new Integer(listDataChange.m_index + i));
					}
				}
			}
				
			if (indices.size() > 0) {
				notifyDataConsumers(DataConstants.LIST_CHANGE, new ListDataChange(indices));
			}
		} else {
			if (changeType.equals(DataConstants.LIST_ADDITION)) {
				// Convert resource list to field data
				ListDataChange ldc = (ListDataChange) change;
				ArrayList list = new ArrayList();
				Iterator i = ldc.m_elements.iterator();
				while (i.hasNext()) {
					Object o = i.next();
					if (o instanceof Resource) {
						Resource fieldData = Utilities.generateUniqueResource();
						try {
							m_partDataSource.add(new Statement(fieldData, Constants.s_rdf_type, LayoutConstants.s_Field));
							m_partDataSource.add(new Statement(fieldData, s_fieldID, (Resource) o));
							m_partDataSource.add(new Statement(fieldData, s_fieldDimension, new Literal("100")));
							m_partDataSource.add(new Statement(fieldData, LayoutConstants.s_resizable, new Literal("true")));
						} catch (RDFException e) {
						}
						list.add(fieldData);
					} else {
						list.add(o);
					}
				} 
				ldc.m_elements = list;
			}
			
			if (changeType.equals(DataConstants.LIST_ADDITION) ||
				changeType.equals(DataConstants.LIST_REMOVAL) ||
				changeType.equals(DataConstants.LIST_CLEAR)) {
				m_dataProviderWrapper.getDataProvider().requestChange(changeType, change);
			} else {
				throw new UnsupportedOperationException("DAML list data provider supports no operation.");
			}			
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
	
	public void setDimension(int dimension) {
		if (dimension != m_dimension) {
			m_dimension = dimension;
			
			ArrayList	indices = new ArrayList();
			
			for (int i = 0; i < m_fieldDataList.size(); i++) {
				CustomFieldData fieldData = (CustomFieldData) m_fieldDataList.get(i);
				
				if (fieldData.m_dimensionInPercent >= 0) {
					fieldData.m_dimension = Math.max(0, (int) (fieldData.m_dimensionInPercent * m_dimension / 100));
					indices.add(new Integer(i));
				}
			}
			notifyDataConsumers(DataConstants.LIST_CHANGE, new ListDataChange(indices));
		}
	}

	class AddRunnable extends IdleRunnable {
		int				m_index;
		java.util.List		m_newItems;
		
		public AddRunnable(int index, java.util.List newItems) {
			super(m_context);
			m_index = index;
			m_newItems = newItems;
		}
		
		public void run() {
			onElementsAdded(m_index, m_newItems);
		}
	}
	
	class RemoveRunnable extends IdleRunnable {
		int	m_index;
		int	m_count;
		
		public RemoveRunnable(int index, int count) {
			super(m_context);
			m_index = index;
			m_count = count;
		}
		
		public void run() {
			onElementsRemoved(m_index, m_count);
		}
	}

	class ClearRunnable extends IdleRunnable {
		public void run() {
			onListCleared();
		}

		public ClearRunnable() {
			super(m_context);
		}
	}
	
	protected void onElementsAdded(int index, java.util.List newItems) {
		Iterator 	i = newItems.iterator();
		String		s;
		int		count = 0;
		
		while (i.hasNext()) {
			Resource fieldData = (Resource) i.next();
			Resource fieldID = Utilities.getResourceProperty(fieldData, s_fieldID, m_partDataSource);
			
			CustomFieldData cfd = new CustomFieldData(fieldData, fieldID);
			readFieldData(cfd);			
			
			m_fieldDataList.add(index + count, cfd);
			count++;
		}
		
		notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(index, count, null));
	}
	
	protected void readFieldData(CustomFieldData cfd) {
		String s = Utilities.getLiteralProperty(cfd.getFieldData(), s_fieldDimension, m_partDataSource);
		if (s != null) {
			if (s.endsWith("%")) {
				cfd.m_dimensionInPercent = Math.max(0, Math.min(100, Float.parseFloat(s.substring(0, s.length() - 1))));
				cfd.setDimension((int) (cfd.m_dimensionInPercent * m_dimension / 100));
				cfd.setResizable(Utilities.checkBooleanProperty(cfd.m_fieldData, LayoutConstants.s_resizable, m_partDataSource, true));
			} else {
				cfd.setDimension(Integer.parseInt(s));
				cfd.setResizable(Utilities.checkBooleanProperty(cfd.m_fieldData, LayoutConstants.s_resizable, m_partDataSource, false));
			}
		}
	}
	
	protected void onElementsRemoved(int index, int count) {
		ArrayList removedItems = new ArrayList();
		
		for (int i = 0; i < count; i++) {
			removedItems.add(m_fieldDataList.remove(index));
		}
		
		notifyDataConsumers(DataConstants.LIST_REMOVAL, new ListDataChange(index, count, removedItems));
	}
	
	protected void onListCleared() {
		m_fieldDataList.clear();
		notifyDataConsumers(DataConstants.LIST_CLEAR, null);
	}
	
	protected void notifyDataConsumers(Resource changeType, Object change) {
		Collection c = (Collection) m_dataConsumers.clone();
		Iterator i = c.iterator();
		while (i.hasNext()) {
			((IDataConsumer) i.next()).onDataChanged(changeType, change);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		Iterator i = m_fieldDataList.iterator();
		while (i.hasNext()) {
			CustomFieldData cfd = (CustomFieldData) i.next();
			readFieldData(cfd);
		}
	}
}
