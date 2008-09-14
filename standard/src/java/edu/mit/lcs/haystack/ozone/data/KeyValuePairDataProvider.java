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
 * Created on Jul 13, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class KeyValuePairDataProvider extends ChainedDataProvider {
	static Resource s_key = new Resource(DataConstants.NAMESPACE + "key");
	static Resource s_keyResource = new Resource(DataConstants.NAMESPACE + "keyResource");
	static Resource s_valueName = new Resource(DataConstants.NAMESPACE + "valueName");
	static Resource s_base = new Resource(DataConstants.NAMESPACE + "base");
	
	protected Resource m_key;
	protected Resource m_valueName;
	protected Resource m_base;
	protected Resource m_predicate;
	
	protected Resource m_pattern1Cookie;
	protected Resource m_pattern2Cookie;
	protected Resource m_pattern3Cookie;
	
	protected Resource m_value = null;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		Resource value = m_value;
		if (value != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, value);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
		updateValue();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
		updateValue();
	}
	
	synchronized protected void updateValue() {
		Resource newValue = getValue();
		if ((m_value == null && newValue != null) ||
			(m_value != null && !m_value.equals(newValue))) {
			m_value = newValue;
			notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_value);
		}
	}
	
	protected Resource getValue() {
		try {
			RDFNode[] datum = m_infoSource.queryExtract(new Statement[] {
				new Statement(m_base, m_predicate, Utilities.generateWildcardResource(2)),
				new Statement(Utilities.generateWildcardResource(2), s_keyResource, m_key),
				new Statement(Utilities.generateWildcardResource(2), m_valueName, Utilities.generateWildcardResource(1))
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
			
			if (datum == null) {
				return null;
			} else {
				return (Resource) datum[0];
			}
		} catch (RDFException e) {
			e.printStackTrace();
			return null;
		} 
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType != null && !dataType.equals(DataConstants.RESOURCE)) {
			return null;
		}
		
		return m_value;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, true);
		
		m_key = Utilities.getResourceProperty(m_prescription, s_key, m_partDataSource);
		m_base = Utilities.getResourceProperty(m_prescription, s_base, m_partDataSource);
		m_valueName = Utilities.getResourceProperty(m_prescription, s_valueName, m_partDataSource);
		m_predicate = Utilities.getResourceProperty(m_prescription, DataConstants.PREDICATE, m_partDataSource);

		m_value = getValue();

		try {		
			m_pattern1Cookie = addPattern(m_base, m_predicate, null);
			m_pattern2Cookie = addPattern(null, s_keyResource, m_key);
			m_pattern3Cookie = addPattern(null, m_valueName, null);
		} catch (RDFException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (changeType.equals(DataConstants.RESOURCE_CHANGE)) {
			Resource newValue = (Resource) change;
			if (newValue.equals(m_value)) {
				return;
			}

			Resource midpoint;			
			try {
				RDFNode[] datum = m_infoSource.queryExtract(new Statement[] {
					new Statement(m_base, m_predicate, Utilities.generateWildcardResource(1)),
					new Statement(Utilities.generateWildcardResource(1), s_keyResource, m_key)
				}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
				if (datum == null) {
					midpoint = Utilities.generateUniqueResource();
					m_infoSource.add(new Statement(m_base, m_predicate, midpoint));
					m_infoSource.add(new Statement(midpoint, s_keyResource, m_key));
				} else {
					midpoint = (Resource) datum[0];
				}
				
				m_infoSource.replace(midpoint, m_valueName, null, newValue);
			} catch (RDFException e) {
				e.printStackTrace();
			}
		} else if (changeType.equals(DataConstants.RESOURCE_DELETION)) {
			if (m_value == null) {
				return;
			}

			Resource midpoint;			
			try {
				RDFNode[] datum = m_infoSource.queryExtract(new Statement[] {
					new Statement(m_base, m_predicate, Utilities.generateWildcardResource(1)),
					new Statement(Utilities.generateWildcardResource(1), s_keyResource, m_key)
				}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
				if (datum != null) {
					midpoint = (Resource) datum[0];
					m_infoSource.remove(new Statement(midpoint, m_valueName, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
				}
			} catch (RDFException e) {
				e.printStackTrace();
			}
		}
	}
}
