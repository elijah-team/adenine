/*
 * Created on Nov 7, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.Iterator;
import java.util.List;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ListDataChange;
import edu.mit.lcs.haystack.ozone.data.ProviderConstructorDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 * @author Dennis Quan
 */
public class ListProviderConstructorDataProvider extends ChainedDataProvider {
	transient protected IDataProvider m_constructedDataProvider;
	transient protected boolean m_init = true;
	protected Resource m_dataSourceToConstruct;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ProviderConstructorDataProvider.class);
	
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
		List list;
		try {
			list = (List) m_constructedDataProvider.getData(DataConstants.LIST, null);
		} catch (DataNotAvailableException e) {
			list = null;
		}
		
		if (list != null && !list.isEmpty()) {
			dataConsumer.onDataChanged(DataConstants.LIST_ADDITION, new ListDataChange(0, list.size(), list));
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		return m_constructedDataProvider.getData(dataType, specifications);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);		
		m_init = false;
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

					Iterator i = m_consumers.iterator();
					while (i.hasNext()) {
						IDataConsumer dc = (IDataConsumer) i.next();
						dc.onDataChanged(DataConstants.LIST_CLEAR, null);
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
			m_dataSourceToConstruct = resource;
			m_constructedDataProvider = DataUtilities.createDataProvider(resource, m_context, m_partDataSource, m_partDataSource);
			m_constructedDataProvider.registerConsumer(new IDataConsumer() {
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onDataChanged(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
				 */
				public void onDataChanged(Resource changeType, Object change)
					throws IllegalArgumentException {
					if (!m_init) {
						notifyDataConsumers(changeType, change);
					}
				}
				
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(edu.mit.lcs.haystack.rdf.Resource)
				 */
				public void onStatusChanged(Resource status) {
				}
				
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#reset()
				 */
				public void reset() {
				}
			});
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		Resource res = m_dataSourceToConstruct;
		notifyDataConsumers(DataConstants.LIST_CLEAR, null);
		super.initializeFromDeserialization(source);
		m_init = false;
		
		if (res == m_dataSourceToConstruct && res != null) {
			makeDataProvider(m_dataSourceToConstruct);
		}
	}
}
