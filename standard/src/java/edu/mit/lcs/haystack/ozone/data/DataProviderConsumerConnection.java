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

import java.io.Serializable;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author vineet
 * This class may need to be converted to be a part.
 */
public class DataProviderConsumerConnection implements Serializable {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(DataProviderConsumerConnection.class);

	public IDataProvider m_provider;
	public IDataConsumer m_consumer;
	public Context m_context;

	transient protected IRDFContainer m_partDataSource = null;
	transient protected IRDFContainer m_source = null;

	public DataProviderConsumerConnection(Context context, IRDFContainer source) {
		m_context = new Context(context);
		m_source = source;
		m_partDataSource = source;
	}

	public DataProviderConsumerConnection(Context context, IRDFContainer source, IRDFContainer partDataSource) {
		m_context = new Context(context);
		m_source = source;
		m_partDataSource = partDataSource;
	}

	/**
	 * 
	 */
	public void dispose() {
		if (m_consumer != null) {
			m_provider.unregisterConsumer(m_consumer);
			m_consumer = null;
		}
		m_provider = null;
	}
	
	/**
	 * @param navSource
	 * @param consumer
	 */
	public void connect(Resource source, IDataConsumer consumer) {
		m_provider = DataUtilities.createDataProvider(source, m_context, m_source, m_partDataSource);
		if (m_provider != null) {
			m_consumer = consumer;
			m_provider.registerConsumer(m_consumer);
		} else {
			s_logger.error("Created provider is null, for source: " + source, new Exception());
		}
	}
}
