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

import org.apache.log4j.Category;

import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

/**
 * @author Dennis Quan
 */
public class ContentStringDataProvider extends ChainedDataProvider {
	public static final Category s_logger = Category.getInstance(ContentStringDataProvider.class);
	protected Resource m_res = null;
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceChanged(Resource newResource) {
				m_res = newResource;
				notifyChanged();
			}
			
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceDeleted(Resource previousResource) {
				m_res = null;
				notifyChanged();
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		String content = getContent();
		if (content != null) {
			dataConsumer.onDataChanged(DataConstants.STRING_CHANGE, content);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (DataConstants.STRING.equals(dataType)) {
			return getContent();
		} else {
			return null;
		}
	}

	protected void notifyChanged() {
		String str = getContent();
		if (str != null) {
			super.notifyDataConsumers(DataConstants.STRING_CHANGE, str);
		} else {
			super.notifyDataConsumers(DataConstants.STRING_DELETION, null);
		}
	}

	protected String getContent() {
		try {
			if (m_res == null) {
				return null;
			}
			return ContentClient.getContentClient(m_res, m_infoSource, m_context.getServiceAccessor()).getContentAsString();
		} catch (Exception e) {
			s_logger.error("Failed to get content", e);
			return null;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);
	}

}
