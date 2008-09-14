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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;

/**
 * @author David Huynh
 */
public class LiteralPropertyDataProvider extends PredicatePropertyDataProvider {
	Literal m_default;
	
	final static Resource s_default = new Resource(DataConstants.NAMESPACE + "default");
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);

		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource != null) {
			try {
				m_default = (Literal) m_partDataSource.extract(dataSource, s_default, null);
			} catch (RDFException e) {
			}
		}
		
		super.initialize(source, context);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.PredicatePropertyDataProvider#onObjectDeleted(RDFNode)
	 */
	protected void onPropertyDeleted(RDFNode oldProperty) {
		if (!m_initializing && !m_reverse) {
			if (oldProperty instanceof Literal) {
				notifyDataConsumers(DataConstants.LITERAL_DELETION, oldProperty);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.PredicatePropertyDataProvider#onObjectChanged(RDFNode)
	 */
	protected void onPropertyChanged(RDFNode oldProperty) {
		if (!m_initializing && !m_reverse) {
			if (m_object instanceof Literal) {
				notifyDataConsumers(DataConstants.LITERAL_CHANGE, m_object);
			} else if (oldProperty instanceof Literal) {
				notifyDataConsumers(DataConstants.LITERAL_DELETION, oldProperty);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_object instanceof Literal) {
			dataConsumer.onDataChanged(DataConstants.LITERAL_CHANGE, m_object);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		
		if (m_subject == null || m_predicate == null) {
			throw new DataNotAvailableException("No literal property available");
		}
		
		if (m_object instanceof Literal) {
			if (dataType.equals(DataConstants.LITERAL))  {
				return m_object;
			} else if (dataType.equals(DataConstants.STRING)) {
				return m_object.getContent();
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		if (m_subject == null || m_predicate == null) {
			throw new UnsupportedOperationException("No data available to change");
		}
		
		if (changeType.equals(DataConstants.STRING_CHANGE)) {
			if (change instanceof String) {
				try {
					m_infoSource.replace(m_subject, m_predicate, null, new Literal((String) change));
				} catch (RDFException e) {
				}
			} else {
				throw new DataMismatchException("Expected a String change object");
			}
		} else if (changeType.equals(DataConstants.LITERAL_CHANGE)) {
			if (change instanceof Literal) {
				try {
					m_infoSource.replace(m_subject, m_predicate, null, (Literal) change);
				} catch (RDFException e) {
				}
			} else {
				throw new DataMismatchException("Expected a Literal change object");
			}
		} else if (changeType.equals(DataConstants.STRING_DELETION) ||
					changeType.equals(DataConstants.LITERAL_DELETION)) {
			try {
				m_infoSource.remove(new Statement(m_subject, m_predicate, Utilities.generateWildcardResource(1)),
					Utilities.generateWildcardResourceArray(1));
			} catch (RDFException e) {
			}
		} else {
			throw new UnsupportedOperationException("Unsupported change type " + changeType);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return 
			changeType.equals(DataConstants.LITERAL_CHANGE) ||
			changeType.equals(DataConstants.LITERAL_DELETION) ||
			changeType.equals(DataConstants.STRING_CHANGE) ||
			changeType.equals(DataConstants.STRING_DELETION);
	}

	protected RDFNode getObject() {
		RDFNode n = super.getObject();
		if (n == null && m_default != null) {
			n = m_default;
		}
		return n;
	}
}
