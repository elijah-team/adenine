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

/*
 * Created on Aug 3, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.List;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ListDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ListDataProviderWrapper;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class RandomAccessDataProvider extends ChainedDataProvider {
	protected IDataProvider m_focusDataProvider;
	int m_i = -1;
	protected ListDataProviderWrapper m_wrapper;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new ListDataConsumer() {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ListDataConsumer#onElementsAdded(int, int)
			 */
			protected void onElementsAdded(int index, int count) {
				if (m_i == -1) {
					return;
				}
				if (m_i >= index) {
					m_i += count;
				}
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ListDataConsumer#onElementsChanged(int, int)
			 */
			protected void onElementsChanged(int index, int count) { }
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ListDataConsumer#onElementsChanged(java.util.List)
			 */
			protected void onElementsChanged(List changedIndices) { }
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ListDataConsumer#onElementsRemoved(int, int, java.util.List)
			 */
			protected void onElementsRemoved(
				int index,
				int count,
				List removedElements) {
				if (m_i == -1) {
					return;
				} else if (m_i > (index + count)) {
					m_i -= count;
				} else if (m_i > index){
					m_i = index;
				} else if (m_i == index) {
					--m_i;
				}
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ListDataConsumer#onListCleared()
			 */
			protected void onListCleared() { }
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) { }

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.internalInitialize(source, context, false);

		Resource focusDataSource = Utilities.getResourceProperty(m_prescription, DataConstants.s_focusDataSource, m_partDataSource);
		if (focusDataSource != null) {
			m_focusDataProvider = DataUtilities.createDataProvider(focusDataSource, m_context, m_partDataSource);
			m_focusDataProvider.registerConsumer(new ResourceDataConsumer() {
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceChanged(Resource newResource) {
					List list = getList();
					if (list != null) {
						int i = list.indexOf(newResource);
						if (i != -1) {
							m_i = i;
						}
					}
				}
				
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceDeleted(Resource previousResource) { }
			});
		}
		
		if (m_dataProvider != null) {
			m_wrapper = new ListDataProviderWrapper(m_dataProvider);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#dispose()
	 */
	public synchronized void dispose() {
		if (m_focusDataProvider != null) {
			m_focusDataProvider.dispose();
			m_focusDataProvider = null;
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_focusDataProvider != null) {
			m_focusDataProvider.initializeFromDeserialization(source);
		}
	}
	
	protected Resource getFocus() {
		if (m_focusDataProvider != null) {
			try {
				return (Resource) m_focusDataProvider.getData(DataConstants.RESOURCE, null);
			} catch (DataNotAvailableException e) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	protected List getList() {
		if (m_wrapper != null) {
			try {
				return m_wrapper.getList();
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public void next() {
		List list = getList();
		if (list != null) {
			try {
				++m_i;
				
				Resource res = (Resource) m_wrapper.getElement(m_i);
				
				m_focusDataProvider.requestChange(DataConstants.RESOURCE_CHANGE, res);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void back() {
		List list = getList();
		if (list != null) {
			try {
				if (m_i == -1) {
					m_i = 0;
				} else if (m_i > 0) {
					--m_i;
				} else {
					return;
				}
				
				Resource res = (Resource) m_wrapper.getElement(m_i);
				
				m_focusDataProvider.requestChange(DataConstants.RESOURCE_CHANGE, res);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
