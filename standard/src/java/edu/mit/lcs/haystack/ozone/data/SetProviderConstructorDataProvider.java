/*
 * Created on Nov 7, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ProviderConstructorDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.ozone.data.SetDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 * @author Dennis Quan
 */
public class SetProviderConstructorDataProvider extends ChainedDataProvider {
	protected IDataProvider	m_constructedDataProvider;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ProviderConstructorDataProvider.class);
	
	protected HashSet m_lastSet = new HashSet();

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.requestChange(changeType, change);
		} else {
			throw new UnsupportedOperationException("No constructed data provider");
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		HashSet set = new HashSet();

		synchronized (this) {
			set.addAll(m_lastSet);
		}
		
		if (!set.isEmpty()) {
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, set);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.SET)) {
			HashSet set = new HashSet();

			synchronized (this) {
				set.addAll(m_lastSet);
			}
		
			return set;
		} else {
			return null;
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);		
		internalInitialize(source, context, true);
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				if (m_constructedDataProvider == null) {
					makeDataProvider(newResource);
				} else {
					m_constructedDataProvider.dispose();
					makeDataProvider(newResource);
				}
			}
	
			protected void onResourceDeleted(Resource previousResource) {
				if (m_constructedDataProvider != null) {
					m_constructedDataProvider.dispose();
					m_constructedDataProvider = null;

					synchronized (this) {
						m_lastSet.clear();
					}
					
					Iterator i = m_consumers.iterator();
					while (i.hasNext()) {
						IDataConsumer dc = (IDataConsumer) i.next();
						dc.onDataChanged(DataConstants.SET_CLEAR, null);
					}
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.dispose();
			m_constructedDataProvider = null;
		}
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		if (m_constructedDataProvider != null) {
			return m_constructedDataProvider.supportsChange(changeType);
		}
		return false;
	}
	
	void makeDataProvider(Resource resource) {
		if (m_constructedDataProvider == null) {
			m_constructedDataProvider = DataUtilities.createDataProvider(resource, m_context, m_partDataSource, m_partDataSource);
			m_constructedDataProvider.registerConsumer(new ProviderConstructorSetConsumer(this));
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_constructedDataProvider != null) {
			m_constructedDataProvider.initializeFromDeserialization(source);
		}
	}
}

class ProviderConstructorSetConsumer extends SetDataConsumer {
	SetProviderConstructorDataProvider m_provider = null;
	boolean m_firstTime = true;
	
	ProviderConstructorSetConsumer() {
	}
	
	ProviderConstructorSetConsumer(SetProviderConstructorDataProvider provider) {
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
				HashSet set = new HashSet();
				if (!m_firstTime) {
					synchronized (m_provider) {
						set.addAll(m_provider.m_lastSet);
					}
				} else {
					m_firstTime = false;
				}
				set.addAll(items);
				updateCacheAndNotify(set);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(java.util.Set)
	 */
	protected void onItemsRemoved(Set items) {
		if (m_provider != null) {
			synchronized (m_provider) {
				HashSet set = new HashSet();
				set.addAll(m_provider.m_lastSet);
				set.removeAll(items);
				updateCacheAndNotify(set);
			}
		}
	}

	protected void updateCacheAndNotify(HashSet newSet) {
		HashSet addedSet = new HashSet();
		HashSet removedSet = new HashSet();
		addedSet.addAll(newSet);
		addedSet.removeAll(m_provider.m_lastSet);
		
		removedSet.addAll(m_provider.m_lastSet);
		removedSet.removeAll(newSet);

		m_provider.m_lastSet.clear();
		m_provider.m_lastSet.addAll(newSet);

		if (!addedSet.isEmpty()) {
			m_provider.notifyDataConsumers(DataConstants.SET_ADDITION, addedSet);
		}
		if (!removedSet.isEmpty()) {
			m_provider.notifyDataConsumers(DataConstants.SET_REMOVAL, removedSet);
		}
	}
}
