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
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 * @author Dennis Quan
 */
public class ViewInstanceDataProvider extends GenericDataProvider {
	Resource m_underlying;
	
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_underlying != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_underlying);
		}
	}

	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (dataType.equals(DataConstants.RESOURCE) && m_underlying != null) {
			return m_underlying;
		}
		throw new DataNotAvailableException("No data available of type " + dataType);
	}

	static public Resource getViewInstance(Context context) {
		while (context != null) {
			Resource underlying = (Resource) context.getLocalProperty(OzoneConstants.s_viewInstance);
			
			if (underlying != null) {
				return underlying;
			}
			
			context = context.getParentContext();
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		Resource underlying = getViewInstance(m_context);
		if (underlying != null && !underlying.equals(m_underlying)) {
			m_underlying = underlying;
			notifyDataConsumers(DataConstants.RESOURCE_CHANGE, underlying);
		}
	}

	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		m_underlying = getViewInstance(context);
	}
}
