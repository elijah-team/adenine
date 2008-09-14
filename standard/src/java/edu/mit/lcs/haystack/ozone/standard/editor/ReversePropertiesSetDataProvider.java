/*
 * Created on Jan 30, 2004
 */
package edu.mit.lcs.haystack.ozone.standard.editor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class ReversePropertiesSetDataProvider extends ChainedDataProvider {
	protected HashSet m_propertiesToDisplay = new HashSet();
	protected Set m_cachedData = Collections.synchronizedSet(new HashSet());
	protected Resource m_object;
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceChanged(Resource newResource) {
				setObject(newResource);
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void onResourceDeleted(Resource previousResource) {
				setObject(null);
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
		if (s.getObject().equals(m_object)) {
			Resource predicate = s.getPredicate();
			if (!m_cachedData.contains(predicate)) {
				m_cachedData.add(predicate);
				HashSet set = new HashSet();
				set.add(predicate);
				notifyDataConsumers(DataConstants.SET_ADDITION, set);
			}
/*		} else if (s.getPredicate().equals(Constants.s_rdfs_domain)) {
			Resource type = (Resource)s.getObject();
			Resource predicate = s.getPredicate();
			if (Utilities.isType(m_object, type, m_infoSource) && !m_cachedData.contains(predicate)) {
				m_cachedData.add(predicate);
				HashSet set = new HashSet();
				set.add(predicate);
				notifyDataConsumers(DataConstants.SET_ADDITION, set);
			}*/
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
	throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.SET)) {
			return m_cachedData;
		} else if (dataType.equals(DataConstants.SET_COUNT)) {
			return new Integer(m_cachedData.size());
		} else if (dataType.equals(DataConstants.RESOURCE)) {
			return m_object;
		}
		
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.SET_ADDITION, m_cachedData);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.internalInitialize(source, context, true);
		m_propertiesToDisplay.addAll((Arrays.asList(Utilities.getResourceProperties(m_prescription, MetadataEditorConstants.propertyToDisplay, m_partDataSource))));
		Resource object = Utilities.getResourceProperty(m_prescription, DataConstants.OBJECT, m_partDataSource);
		if (object != null) {
			setObject(object);
		}
	}
	
	transient protected Resource m_cookie1;
//	transient protected Resource m_cookie2;
	
	synchronized protected void setObject(Resource object) {
		if (m_cookie1 != null) {
			removePattern(m_cookie1);
			m_cookie1 = null;
		}
		
/*		if (m_cookie2 != null) {
			removePattern(m_cookie2);
			m_cookie2 = null;
		}*/
		
		m_object = object;
		
		if (object != null) {
			try {
				m_cookie1 = addPattern(null, null, m_object);
//				m_cookie2 = addPattern(null, Constants.s_rdfs_domain, null);
			} catch (RDFException e) {
			}
		}
		
		HashSet oldData = new HashSet();
		oldData.addAll(m_cachedData);
		
		updateApplicableProperties();
		
		HashSet addedSet = new HashSet();
		HashSet removedSet = new HashSet();
		addedSet.addAll(m_cachedData);
		addedSet.removeAll(oldData);
		
		removedSet.addAll(oldData);
		removedSet.removeAll(m_cachedData);

		if (!addedSet.isEmpty()) {
			notifyDataConsumers(DataConstants.SET_ADDITION, addedSet);
		}
		if (!removedSet.isEmpty()) {
			notifyDataConsumers(DataConstants.SET_REMOVAL, removedSet);
		}
	}
	
	protected void updateApplicableProperties() {
		m_cachedData.clear();
		if (m_object != null) {
			try {
				m_cachedData.addAll(
						Utilities.extractFirstItems(
								m_infoSource.query(
										new Statement(
												Utilities.generateWildcardResource(2),
												Utilities.generateWildcardResource(1),
												m_object),
												Utilities.generateWildcardResourceArray(2))));
			} catch (RDFException e) {
			}
/*			try {
				m_cachedData.addAll(
						Utilities.extractFirstItems(
								m_infoSource.query(
										new Statement[] {
												new Statement(
														m_object,
														Constants.s_rdf_type,
														Utilities.generateWildcardResource(2)),
														new Statement(
																Utilities.generateWildcardResource(1),
																Constants.s_rdfs_domain,
																Utilities.generateWildcardResource(2))},
																Utilities.generateWildcardResourceArray(1),
																Utilities.generateWildcardResourceArray(2))));
			} catch (RDFException e) {
			}*/
		}
		m_cachedData.addAll(m_propertiesToDisplay);
	}
}
