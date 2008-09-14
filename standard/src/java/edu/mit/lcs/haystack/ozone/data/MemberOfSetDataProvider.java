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
 * Created on Apr 12, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * A boolean data provider that indicates the presence of a resource in a set.
 * @author Dennis Quan
 */
public class MemberOfSetDataProvider extends GenericDataProvider {
	protected boolean m_isInSet = false;
	protected HashSet m_set = new HashSet();
	protected Resource m_member = null;
	protected IDataProvider m_setDataSource;
	protected IDataProvider m_memberDataSource;
	protected ArrayList m_separatedData = new ArrayList();

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.BOOLEAN_CHANGE, new Boolean(m_isInSet));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.BOOLEAN)) {
			return new Boolean(m_isInSet);
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	public Resource getStatus() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		Resource setDataSource = Utilities.getResourceProperty(m_prescription, DataConstants.s_setDataSource, m_partDataSource);
		m_setDataSource = DataUtilities.createDataProvider(setDataSource, m_context, m_partDataSource);
		m_setDataSource.registerConsumer(new SetDataConsumer() {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsAdded(java.util.Set)
			 */
			protected void onItemsAdded(Set items) {
				synchronized (MemberOfSetDataProvider.this) {
					m_set.addAll(items);
				}
				updateCacheAndNotify();
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(java.util.Set)
			 */
			protected void onItemsRemoved(Set items) {
				synchronized (MemberOfSetDataProvider.this) {
					m_set.removeAll(items);
				}
				updateCacheAndNotify();
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onSetCleared()
			 */
			protected void onSetCleared() {
				synchronized (MemberOfSetDataProvider.this) {
					m_set.clear();
				}
				updateCacheAndNotify();
			}
		});
		
		Resource memberDataSource = Utilities.getResourceProperty(m_prescription, DataConstants.s_memberDataSource, m_partDataSource);
		m_memberDataSource = DataUtilities.createDataProvider(memberDataSource, m_context, m_partDataSource);
		m_memberDataSource.registerConsumer(new ResourceDataConsumer() {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceChanged(Resource newResource) {
				synchronized (MemberOfSetDataProvider.this) {
					m_member = newResource;
				}
				updateCacheAndNotify();
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceDeleted(Resource previousResource) {
				synchronized (MemberOfSetDataProvider.this) {
					m_member = null;
				}
				updateCacheAndNotify();
			}
		});
		
	}

	protected void updateCacheAndNotify() {
		synchronized (this) {
			boolean b = m_member != null && m_set.contains(m_member);
			if (b == m_isInSet) {
				return;
			}
			m_isInSet = b;
		}
		
		notifyDataConsumers(DataConstants.BOOLEAN_CHANGE, new Boolean(m_isInSet));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		m_setDataSource.dispose();
		m_memberDataSource.dispose();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (DataConstants.BOOLEAN_CHANGE.equals(changeType)) {
			Resource member = m_member;
			if (member != null) {
				HashSet set = new HashSet();
				set.add(member);
				
				if (((Boolean) change).booleanValue()) {
					m_memberDataSource.requestChange(DataConstants.SET_ADDITION, set);
				} else {
					m_memberDataSource.requestChange(DataConstants.SET_REMOVAL, set);
				}
			}
		}

		super.requestChange(changeType, change);
	}
}
