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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.data.AdenineDataConsumer;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class DataConsumerPart extends GenericPart {
	protected AdenineDataConsumer m_consumer;
	protected IDataProvider m_dataProvider;
	protected boolean m_ownsDataProvider = false;

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		// Set up data source
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource chainedDataSource = (Resource) Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_source, m_partDataSource);
				m_ownsDataProvider = m_dataProvider != null;
			}
		}
		
		if (m_dataProvider != null) {
			// Set up data consumer methods
			m_consumer = new AdenineDataConsumer(source, context,
				Utilities.getResourceProperty(m_prescription, PartConstants.s_onDataChanged, m_partDataSource),
				Utilities.getResourceProperty(m_prescription, PartConstants.s_onStatusChanged, m_partDataSource),
				Utilities.getResourceProperty(m_prescription, PartConstants.s_reset, m_partDataSource));
			
			// Attach consumer
			m_dataProvider.registerConsumer(m_consumer);	
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		
		if (m_ownsDataProvider) {
			m_dataProvider.dispose();
		}
		
		m_dataProvider = null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.GenericPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		m_consumer.initializeFromDeserialization(source);
		if (m_ownsDataProvider) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}
}
