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
 * Created on Jun 22, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class ContextPropertyDataProvider extends GenericDataProvider {
	protected RDFNode m_datum = null;
	protected Resource m_propertyName = null;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_datum != null) {
			if (m_datum instanceof Literal) {
				dataConsumer.onDataChanged(DataConstants.LITERAL_CHANGE, m_datum);
			} else {
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_datum);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		return m_datum;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.setupSources(source, context);
		
		m_propertyName = Utilities.getResourceProperty(m_prescription, DataConstants.s_property, source);
		if (m_propertyName != null) {
			m_datum = (RDFNode) context.getProperty(m_propertyName);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_propertyName != null) {
			RDFNode datum = (RDFNode) m_context.getProperty(m_propertyName);
			if (datum != null && !datum.equals(m_datum)) {
				m_datum = datum;
				if (m_datum instanceof Literal) {
					notifyDataConsumers(DataConstants.LITERAL_CHANGE, m_datum);
				} else {
					notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_datum);
				}
			} else if (m_datum != null && datum == null) {
				if (m_datum instanceof Literal) {
					m_datum = datum;
					notifyDataConsumers(DataConstants.LITERAL_DELETION, m_datum);
				} else {
					m_datum = datum;
					notifyDataConsumers(DataConstants.RESOURCE_DELETION, m_datum);
				}
			}
		}
	}	
}
