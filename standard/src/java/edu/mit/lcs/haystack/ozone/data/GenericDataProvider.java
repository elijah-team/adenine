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
import java.util.Iterator;

import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
abstract public class GenericDataProvider extends GenericPart implements IDataProvider {
	protected HashSet m_consumers = new HashSet();
	
	abstract protected void onConsumerAdded(IDataConsumer dataConsumer);

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer == null) {
			return;
		}
		boolean b;
		synchronized (m_consumers) {
			if (!(b = m_consumers.contains(dataConsumer))) {
				m_consumers.add(dataConsumer);
			}
		}
		if (!b) {
			onConsumerAdded(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer == null) {
			return;
		}
		synchronized (m_consumers) {
			m_consumers.remove(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		throw new UnsupportedOperationException();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}

	public void notifyDataConsumers(Resource changeType, Object change) {
		HashSet consumers;
		synchronized (m_consumers) {
			consumers = (HashSet) m_consumers.clone();
		}
		Iterator i = consumers.iterator();
		while (i.hasNext()) {
			try {
				((IDataConsumer)i.next()).onDataChanged(changeType, change);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
