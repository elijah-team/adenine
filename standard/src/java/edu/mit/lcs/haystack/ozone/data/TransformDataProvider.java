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
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * @author David Huynh
 */
public class TransformDataProvider extends ChainedDataProvider {
	protected Resource	m_original;
	protected Resource	m_transformed;
	protected Resource	m_adenineTransform;
	
	final static Resource s_adenineTransform = new Resource(DataConstants.NAMESPACE + "adenineTransform");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TransformDataProvider.class);
	
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
		
		if (m_original == null) {
			setOriginal(null);
		}
		
		m_initializing = false;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				synchronized (TransformDataProvider.this) {
					if (!newResource.equals(m_original)) {
						setOriginal(newResource);
					}
				}
			}

			protected void onResourceDeleted(Resource previousResource) {
				synchronized (TransformDataProvider.this) {
					setOriginal(null);
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		m_original = null;
		m_transformed = null;
		m_adenineTransform = null;
		
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
			
		if (dataType.equals(DataConstants.RESOURCE)) {
			return m_transformed;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_transformed != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_transformed);
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
	
	void setOriginal(Resource newResource) {
		Interpreter 		interpreter = Ozone.getInterpreter();
		DynamicEnvironment 	denv = new DynamicEnvironment(m_source);
		
		Ozone.initializeDynamicEnvironment(denv, m_context);
		
		m_original = newResource;
		
		Resource oldTransformed = m_transformed;
		
		try {
			m_transformed = (Resource) interpreter.callMethod(m_adenineTransform, new Object[] {m_original, m_prescription}, denv);
		} catch (AdenineException e) {
			s_logger.error("Error calling method " + m_adenineTransform, e);
			m_transformed = null;
		}
		
		if (oldTransformed != null) {
			if (m_transformed == null) {
				notifyDataConsumers(DataConstants.RESOURCE_DELETION, oldTransformed);
			} else {
				notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_transformed);
			}
		} else if (m_transformed != null) {
			notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_transformed);
		}
	}
}
