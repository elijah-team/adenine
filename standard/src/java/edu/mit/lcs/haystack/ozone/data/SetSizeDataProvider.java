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

import java.util.HashSet;
import java.util.Set;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;

/**
 * @author Dennis Quan
 */
public class SetSizeDataProvider extends ChainedDataProvider {
	protected HashSet m_baseData = new HashSet();
	protected int m_size = 0;

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new SetDataConsumer() {
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onDataChanged(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
			 */
			public void onDataChanged(Resource changeType, Object change)
				throws IllegalArgumentException {
				super.onDataChanged(changeType, change);
				update();
			}

			/**
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsAdded(java.util.Set)
			 */
			protected void onItemsAdded(Set items) {
				synchronized (m_baseData) {
					m_baseData.addAll(items);
				}
				update();
			}

			/**
			 * @see edu.mit.lcs.haystack.ozone.data.
			 * SetDataConsumer#onSetCleared()
			 */
			protected void onSetCleared() {
				synchronized (m_baseData) {
					m_baseData.clear();
				}
				update();
			}

			/**
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(java.util.Set)
			 */
			protected void onItemsRemoved(Set items) {
				synchronized (m_baseData) {
					m_baseData.removeAll(items);
				}
				update();
			}

			/**
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onStatusChanged(edu.mit.lcs.haystack.rdf.Resource)
			 */
			public void onStatusChanged(Resource status) {
				super.onStatusChanged(status);
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(Statement)
	 */
	protected void onStatementAdded(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}
	
	protected void update() {
		int newSize = 0;
		synchronized (m_baseData) {
			newSize = m_baseData.size();
		}
		if (newSize != m_size) {
			m_size = newSize;
			notifyDataConsumers(DataConstants.STRING_CHANGE, Integer.toString(m_size));
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.STRING_CHANGE, Integer.toString(m_size));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.STRING)) {
			return Integer.toString(m_size);
		}
		throw new DataNotAvailableException("Data type " + dataType + " not supported");
	}

}
