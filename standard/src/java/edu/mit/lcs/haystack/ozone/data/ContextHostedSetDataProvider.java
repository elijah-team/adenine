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
 * Created on Jun 28, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.Set;

import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.SetDataConsumer;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/** 
 * @author Dennis Quan
 */
public class ContextHostedSetDataProvider extends BufferedSetDataProvider {
	protected Resource m_contextPropertyName = null;
	transient protected IDataProvider m_contextProvider = null;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.BufferedSetDataProvider#getProvider()
	 */
	protected IDataProvider getProvider() {
		return m_contextProvider;
	}
	
	protected void setupConsumer() {
		m_contextProvider.registerConsumer(new ContextHostedSetConsumer(this));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.setupSources(source, context);
		
		m_contextPropertyName = Utilities.getResourceProperty(m_prescription, DataConstants.s_property, m_partDataSource);
		if (m_contextPropertyName != null) {
			m_contextProvider = (IDataProvider) m_context.getProperty(m_contextPropertyName);
			if (m_contextProvider != null) {
				setupConsumer();
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_contextPropertyName != null) {
			m_contextProvider = (IDataProvider) m_context.getProperty(m_contextPropertyName);
			if (m_contextProvider != null) {
				m_lastSet.clear();
				setupConsumer();
				
				if (m_lastSet.isEmpty()) {
					notifyDataConsumers(DataConstants.SET_CLEAR, null);
				}
			}
		}
	}
}

class ContextHostedSetConsumer extends SetDataConsumer {
	transient ContextHostedSetDataProvider m_provider = null;
	
	ContextHostedSetConsumer() {
	}
	
	ContextHostedSetConsumer(ContextHostedSetDataProvider provider) {
		m_provider = provider;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onSetCleared()
	 */
	protected void onSetCleared() {
		if (m_provider != null) {
			synchronized (m_provider) {
				m_provider.m_lastSet.clear();
			}
			m_provider.notifyDataConsumers(DataConstants.SET_CLEAR, null);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsAdded(java.util.Set)
	 */
	protected void onItemsAdded(Set items) {
		if (m_provider != null) {
			synchronized (m_provider) {
				m_provider.m_lastSet.addAll(items);
			}
			m_provider.notifyDataConsumers(DataConstants.SET_ADDITION, items);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(java.util.Set)
	 */
	protected void onItemsRemoved(Set items) {
		if (m_provider != null) {
			synchronized (m_provider) {
				m_provider.m_lastSet.removeAll(items);
			}
			m_provider.notifyDataConsumers(DataConstants.SET_REMOVAL, items);
		}
	}
}