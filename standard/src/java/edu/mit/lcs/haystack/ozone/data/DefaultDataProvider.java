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
 * Created on Feb 12, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @author David Huynh
 */
public class DefaultDataProvider extends ChainedDataProvider {
	RDFNode		m_default;
	Object		m_data;
	boolean	m_initializing = true;
	
	final static Resource s_default = new Resource(DataConstants.NAMESPACE + "default");

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		m_default = Utilities.getProperty(m_prescription, s_default, m_source);
		m_data = m_default;
		
		internalInitialize(source, context, false);
		
		m_initializing = false;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new IDataConsumer() {
			public void reset() {
			}

			public void onDataChanged(Resource changeType, Object change)
				throws IllegalArgumentException {
				
				if (
					changeType.equals(DataConstants.RESOURCE_CHANGE) ||
					changeType.equals(DataConstants.STRING_CHANGE) ||
					changeType.equals(DataConstants.LITERAL_CHANGE)) {
						
					if (!change.equals(m_data)) {
						m_data = change;
						if (!m_initializing) {
							notifyDataConsumers(changeType, change);
						}
					}
				} else if (
					changeType.equals(DataConstants.RESOURCE_DELETION) ||
					changeType.equals(DataConstants.STRING_DELETION) ||
					changeType.equals(DataConstants.LITERAL_DELETION)) {
						
					if (m_default != null) {
						if (!m_default.equals(m_data)) {
							m_data = m_default;
							if (!m_initializing) {
								notifyDataConsumers(
									m_data instanceof Resource ? 
										DataConstants.RESOURCE_CHANGE : 
										DataConstants.STRING_CHANGE,
									m_data
								);
							}
						}
					} else if (m_data != null) {
						if (!m_initializing) {
							notifyDataConsumers(changeType, change);
						}
					}
				}
			}

			public void onStatusChanged(Resource status) {
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_data != null) {
			dataConsumer.onDataChanged(
				m_data instanceof Resource ? 
					DataConstants.RESOURCE_CHANGE : 
					DataConstants.STRING_CHANGE,
				m_data
			);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (m_data != null) {
			if (dataType.equals(DataConstants.RESOURCE)) {
				if (m_data instanceof Resource) {
					return m_data;
				}
			} else if (dataType.equals(DataConstants.LITERAL)) {
				if (m_data instanceof Literal) {
					return m_data;
				} else if (m_data instanceof String) {
					return new Literal((String) m_data);
				}
			} else if (dataType.equals(DataConstants.STRING)) {
				if (m_data instanceof Literal) {
					return ((Literal) m_data).getContent();
				} else if (m_data instanceof String) {
					return m_data;
				}
			}
		}
		throw new DataNotAvailableException("No data available of type " + dataType);
	}
}
