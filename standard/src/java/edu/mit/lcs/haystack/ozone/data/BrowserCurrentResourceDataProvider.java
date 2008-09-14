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

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @author David Huynh
 * @author Dennis Quan
 */
public class BrowserCurrentResourceDataProvider implements IDataProvider {
	transient IBrowserWindow	m_viewNavigator;
	transient IDataProvider		m_dataProvider;
	HashSet m_listeners = new HashSet();
	Resource m_propertyName;
	transient IRDFContainer m_source;

	Context m_context;
	
	protected Resource m_prescription;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		initialize(source, m_context);
	}
	
	protected transient IdleRunnable m_runner = new IdleRunnable(10) {
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			m_viewNavigator = (IBrowserWindow) m_context.getProperty(OzoneConstants.s_browserWindow);

			if (m_viewNavigator != null) {
				m_dataProvider = m_viewNavigator.getNavigationDataProvider();
				Iterator i = m_listeners.iterator();
				while (i.hasNext()) {
					m_dataProvider.registerConsumer((IDataConsumer) i.next());
				}
			} else {
				Ozone.idleExec(m_runner);
			}
		}	
	};
		
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		m_context = context;
		m_prescription = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		m_source = source;

		Ozone.idleExec(m_runner);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		m_dataProvider = null;
		m_viewNavigator = null;
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}	

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	public void registerConsumer(IDataConsumer dataConsumer) {
		m_listeners.add(dataConsumer);
		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	public void unregisterConsumer(IDataConsumer dataConsumer) {
		m_listeners.remove(dataConsumer);
		if (m_dataProvider != null) {
			m_dataProvider.unregisterConsumer(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (m_dataProvider != null) {
			return m_dataProvider.getData(dataType, specifications);
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() {
		if (m_dataProvider != null) {
			return m_dataProvider.getStatus();
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (m_dataProvider != null) {
			m_dataProvider.requestChange(changeType, change);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		if (m_dataProvider != null) {
			return m_dataProvider.supportsChange(changeType);
		}
		return false;
	}
}
