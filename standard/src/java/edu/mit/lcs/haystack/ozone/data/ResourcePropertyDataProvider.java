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
public class ResourcePropertyDataProvider extends PredicatePropertyDataProvider {
	protected boolean	m_autoCreate;
	protected Resource m_default;

	final static Resource s_autoCreate = new Resource(DataConstants.NAMESPACE + "autoCreate");
	final static Resource s_default = new Resource(DataConstants.NAMESPACE + "default");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ResourcePropertyDataProvider.class);
	
	public String toString() {
		return "ResourcePropertyDataProvider:" + m_subject + " " + m_predicate + " " + m_object + " " + m_dataProvider + " " + m_disposed;
	}
	
	public void internalInitialize(IRDFContainer source, Context context, boolean createListener) {
		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		m_autoCreate = Utilities.checkBooleanProperty(dataSource, s_autoCreate, source);
		m_default = Utilities.getResourceProperty(dataSource, s_default, source);

		super.internalInitialize(source, context, createListener);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.PredicatePropertyDataProvider#onPropertyDeleted(RDFNode)
	 */
	protected void onPropertyDeleted(RDFNode oldProperty) {
		if (!m_initializing) {
			if (oldProperty instanceof Resource) {
				notifyDataConsumers(DataConstants.RESOURCE_DELETION, oldProperty);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.PredicatePropertyDataProvider#onPropertyChanged(RDFNode)
	 */
	protected void onPropertyChanged(RDFNode oldProperty) {
		if (!m_initializing) {
			if (m_reverse) {
				notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_subject);
			} else {
				if (m_object instanceof Resource) {
					notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_object);
				} else if (oldProperty instanceof Resource) {
					notifyDataConsumers(DataConstants.RESOURCE_DELETION, oldProperty);
				}
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_reverse) {
			if (m_subject != null) {
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_subject);
			}
		} else {
			if (m_object instanceof Resource) {
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_object);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {

		if (m_disposed) {
			throw new DataNotAvailableException("Data provider already disposed");
		}
		
		if (m_reverse) {
			if (m_object == null || m_predicate == null) {
				throw new DataNotAvailableException("No resource property available");
			}
		
			if (dataType.equals(DataConstants.RESOURCE))  {
				return m_subject;
			}
		} else {
			if (m_subject == null || m_predicate == null) {
				throw new DataNotAvailableException("No resource property available");
			}
			
			if (m_object instanceof Resource && dataType.equals(DataConstants.RESOURCE))  {
				return m_object;
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		
		if (m_reverse) {
			if (m_object == null || m_predicate == null) {
				throw new UnsupportedOperationException("No data available to change");
			}
		} else {
			if (m_subject == null || m_predicate == null) {
				throw new UnsupportedOperationException("No data available to change");
			}
		}
		
		if (changeType.equals(DataConstants.RESOURCE_CHANGE)) {
			if (change instanceof Resource) {
				try {
					if (m_reverse) {
						m_infoSource.replace(null, m_predicate, m_object, (Resource) change);
					} else {
						m_infoSource.replace(m_subject, m_predicate, null, (Resource) change);
					}
				} catch (RDFException e) {
				}
			} else {
				throw new DataMismatchException("Expected a Resource change object");
			}
		} else if (changeType.equals(DataConstants.RESOURCE_DELETION)) {
			try {
				if (m_reverse) {
					m_infoSource.remove(new Statement(Utilities.generateWildcardResource(1), m_predicate, m_object),
						Utilities.generateWildcardResourceArray(1));
				} else {
					m_infoSource.remove(new Statement(m_subject, m_predicate, Utilities.generateWildcardResource(1)),
						Utilities.generateWildcardResourceArray(1));
				}
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
			changeType.equals(DataConstants.RESOURCE_CHANGE) ||
			changeType.equals(DataConstants.RESOURCE_DELETION);
	}

	protected RDFNode getObject() {
		Resource object = null;
		try {
			object = (Resource) m_infoSource.extract(m_subject, m_predicate, null);
		} catch (Exception e) {
			s_logger.error("Failed to get object for " + m_subject + " " + m_predicate, e);
		}
		
		if (object == null) {
			if (m_autoCreate) {
				object = Utilities.generateUniqueResource();
				try {
					m_infoSource.replace(m_subject, m_predicate, null, object);
				} catch (Exception e) {
					s_logger.error("Failed to replace object for " + m_subject + " " + m_predicate, e);
				}
			} else {
				object = m_default;
			}
		}
		
		return object;
	}

	protected Resource getSubject() {
		Resource subject = null;
		try {
			subject = (Resource) m_infoSource.extract(null, m_predicate, m_object);
		} catch (RDFException e) {
			s_logger.error("Failed to get subject for " + m_object + " " + m_predicate, e);
		}
		
		if (subject == null) {
			if (m_autoCreate) {
				subject = Utilities.generateUniqueResource();
				try {
					m_infoSource.replace(null, m_predicate, m_object, subject);
				} catch (Exception e) {
					s_logger.error("Failed to replace subject for " + m_object + " " + m_predicate, e);
				}
			} else {
				subject = m_default;
			}
		}
		
		return subject;
	}
}
