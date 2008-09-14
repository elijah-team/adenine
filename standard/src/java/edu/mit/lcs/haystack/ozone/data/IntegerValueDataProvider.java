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
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 */
public class IntegerValueDataProvider extends GenericDataProvider {
	protected int m_value;
	
	public IntegerValueDataProvider(int defaultValue) {
		m_value = defaultValue;
	}
	
	public void setValue(int value) {
		if (value != m_value) {
			m_value = value;
		
			notifyDataConsumers(DataConstants.INTEGER_CHANGE, new Integer(m_value));
		}
	}
	
	public int getValue() {
		return m_value;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.INTEGER_CHANGE, new Integer(m_value));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (dataType.equals(DataConstants.INTEGER)) {
			return new Integer(m_value);
		}
		throw new DataNotAvailableException("No data available of type " + dataType);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
	}
}
