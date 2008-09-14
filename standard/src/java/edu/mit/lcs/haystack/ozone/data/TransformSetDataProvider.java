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

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.SetDataConsumer;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * @author David Huynh
 */
public class TransformSetDataProvider extends ChainedDataProvider {
	protected Set						m_data = new HashSet();
	protected HashMap					m_originalsToTransformed = new HashMap();
	protected Resource				m_adenineTransform;
	
	final static Resource s_adenineTransform = new Resource(DataConstants.NAMESPACE + "adenineTransform");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TransformSetDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	protected boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource == null) {
			return;
		}

		setupSources(source, context);
		
		m_adenineTransform = Utilities.getResourceProperty(dataSource, s_adenineTransform, m_partDataSource);
		if (m_adenineTransform == null) {
			s_logger.error("No adenine transform on " + dataSource);
			return;
		}
		
		internalInitialize(source, context, true);
		
		m_initializing = false;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new SetDataConsumer() {
			protected void onItemsAdded(Set items) {
				synchronized (TransformSetDataProvider.this) {
					HashSet		newItems = new HashSet();

					Interpreter 		interpreter = Ozone.getInterpreter();
					DynamicEnvironment 	denv = new DynamicEnvironment(m_source);
					
					Ozone.initializeDynamicEnvironment(denv, m_context);
					
					Iterator i = items.iterator();
					while (i.hasNext()) {
						Resource original = (Resource) i.next();
						
						if (!m_originalsToTransformed.containsKey(original)) {
							try {
								Object o = interpreter.callMethod(m_adenineTransform, new Object[] { original }, denv);
								
								m_originalsToTransformed.put(original, o);
								
								if (o != null && !m_data.contains(o)) {
									newItems.add(o);
									m_data.add(o);
								}
							} catch(AdenineException e) {
								s_logger.error("Failed to call method " + m_adenineTransform, e);
							}
						}
					}
					
					if (newItems.size() > 0) {
						notifyDataConsumers(DataConstants.SET_ADDITION, newItems);
					}
				}
			}

			protected void onItemsRemoved(Set items) {
				synchronized (TransformSetDataProvider.this) {
					HashSet	removedItems = new HashSet();
					Iterator i = items.iterator();
					while (i.hasNext()) {
						Resource 	original = (Resource) i.next();
						Object		transformed = m_originalsToTransformed.remove(original);
						
						if (transformed != null) {
							removedItems.add(transformed);
							m_data.remove(transformed);
						}
					}
					
					if (removedItems.size() > 0) {
						notifyDataConsumers(DataConstants.SET_REMOVAL, removedItems);
					}
				}
			}

			protected void onSetCleared() {
				synchronized (TransformSetDataProvider.this) {
					int count = m_data.size();
					
					m_data.clear();
					m_originalsToTransformed.clear();
					
					if (count > 0) {
						notifyDataConsumers(DataConstants.SET_CLEAR, null);
					}
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		m_data.clear();
		m_originalsToTransformed.clear();

		super.dispose();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	synchronized protected void onStatementAdded(Statement s) {
	}
	
	synchronized protected void onStatementRemoved(Statement s) {
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (dataType.equals(DataConstants.SET)) {
			return new HashSet(m_data);
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_data.size() > 0) {
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, new HashSet(m_data));
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {

		throw new UnsupportedOperationException("Data provider does not support any change");
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
}
