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

import java.util.ArrayList;
import java.util.Iterator;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author David Huynh
 */
public class MappingDataProvider extends GenericPart implements IDataProvider {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(MappingDataProvider.class);
	
	protected ArrayList				m_dataConsumers = new ArrayList();
	
	protected IDataProvider			m_dataProvider;
	protected IDataConsumer			m_dataConsumer;
	
	protected ArrayList				m_domain = new ArrayList();
	protected ArrayList				m_range = new ArrayList();
	protected RDFNode					m_default;
	protected RDFNode					m_nullDefault;
	protected RDFNode					m_value;
	
	final static Resource 	DOMAIN 	= new Resource(DataConstants.NAMESPACE + "domain");
	final static Resource 	RANGE 	= new Resource(DataConstants.NAMESPACE + "range");
	final static Resource 	DEFAULT = new Resource(DataConstants.NAMESPACE + "default");
	final static Resource 	NULL_DEFAULT = new Resource(DataConstants.NAMESPACE + "nullDefault");
	
	class RDFNodeDataConsumer implements IDataConsumer {
		public void onDataChanged(Resource changeType, Object change)
			throws IllegalArgumentException {
			if (changeType.equals(DataConstants.DATA_CHANGE) ||
				changeType.equals(DataConstants.RESOURCE_CHANGE) ||
				changeType.equals(DataConstants.LITERAL_CHANGE) ||
				changeType.equals(DataConstants.STRING_CHANGE) ||
				changeType.equals(DataConstants.BOOLEAN_CHANGE)) {
				if (change instanceof String) {
					change = new Literal((String) change);
				} else if (change instanceof Boolean) {
					change = new Literal(((Boolean) change).booleanValue() ? "true" : "false");
				}
				try {
					processDataChange((RDFNode) change);
				} catch (Exception e) {
					processDataChange(null);
				}
			} else if (
				changeType.equals(DataConstants.DATA_DELETION) ||
				changeType.equals(DataConstants.RESOURCE_DELETION) ||
				changeType.equals(DataConstants.STRING_DELETION) ||
				changeType.equals(DataConstants.LITERAL_DELETION)) {
				
				processDataChange(null);
			} else if (
				changeType.equals(DataConstants.BOOLEAN_CHANGE)) {
				processDataChange(new Literal(((Boolean) change).booleanValue() ? "true" : "false"));
			} else {
				throw new IllegalArgumentException("Unsupported change type " + changeType);
			}
		}

		public void onStatusChanged(Resource status) {
		}

		public void reset() {
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		Resource dataSource = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);
		
		Resource domain = (Resource) Utilities.getResourceProperty(dataSource, DOMAIN, m_source);
		Iterator i = ListUtilities.accessDAMLList(domain, m_source);
		while (i.hasNext()) {
			m_domain.add(i.next());
		}

		Resource range = (Resource) Utilities.getResourceProperty(dataSource, RANGE, m_source);
		i = ListUtilities.accessDAMLList(range, m_source);
		while (i.hasNext()) {
			m_range.add(i.next());
		}
		
		m_default = Utilities.getResourceProperty(dataSource, DEFAULT, m_source);
		m_nullDefault = Utilities.getResourceProperty(dataSource, NULL_DEFAULT, m_source);
		
		Resource chainedDataSource = Utilities.getResourceProperty(dataSource, OzoneConstants.s_dataSource, m_source);
		if (chainedDataSource != null) {
			m_dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_source);
			if (m_dataProvider != null) {
				m_dataConsumer = new RDFNodeDataConsumer();
				processDataChange(null);
				
				m_dataProvider.registerConsumer(m_dataConsumer);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_dataProvider != null) {
			m_dataProvider.dispose();
			m_dataProvider = null;
			m_dataConsumer = null;
		}
		
		m_dataConsumers.clear();
		m_domain.clear();
		m_range.clear();
		
		m_dataConsumers = null;
		m_domain = null;
		m_range = null;
		m_default = null;
		m_value = null;
		
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	synchronized public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer == null) {
			return;
		}
		
		dataConsumer.reset();
		m_dataConsumers.add(dataConsumer);
		
		dataConsumer.onDataChanged(
			m_value instanceof Resource ? DataConstants.RESOURCE_CHANGE : DataConstants.LITERAL_CHANGE, m_value);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	synchronized public void unregisterConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer != null && m_dataConsumers.contains(dataConsumer)) {
			m_dataConsumers.remove(dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null) {
			return m_value;
		} else if (dataType.equals(DataConstants.RESOURCE) && m_value instanceof Resource) {
			return m_value;
		} else if (dataType.equals(DataConstants.LITERAL) && m_value instanceof Literal) {
			return m_value;
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
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (DataConstants.RESOURCE_CHANGE.equals(changeType) && m_dataProvider != null) {
			int index = m_range.indexOf(change);
			if (index != -1) {
				Object value = m_domain.get(index);
				m_dataProvider.requestChange(			
					value instanceof Resource ? DataConstants.RESOURCE_CHANGE : DataConstants.LITERAL_CHANGE,
					value
				);
			}
		} else {			
			throw new UnsupportedOperationException("Mapping data provider supports no change operation");
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return DataConstants.RESOURCE_CHANGE.equals(changeType);
	}
	
	protected void processDataChange(RDFNode newFrom) {
		RDFNode newTo = null;
		if (newFrom != null) {
			int index = m_domain.indexOf(newFrom);
			if (index != -1) {
				try { newTo = (RDFNode) m_range.get(index); }
				catch (IndexOutOfBoundsException e) { s_logger.error("Index " + index + " out of bounds"); }
			}
		}

		if (newTo == null) {
			if (newFrom == null) newTo = m_nullDefault != null ? m_nullDefault : m_default;
			else newTo = m_default;
		}
		
		if (newTo != m_value) {
			if (m_value == null) {
				notifyDataConsumers(
					newTo instanceof Resource ? DataConstants.RESOURCE_CHANGE : DataConstants.LITERAL_CHANGE,
					newTo
				);
			} else if (newTo == null) {
				notifyDataConsumers(
					m_value instanceof Resource ? DataConstants.RESOURCE_DELETION : DataConstants.LITERAL_DELETION,
					m_value
				);
			} else if (newTo.getClass().equals(m_value.getClass())) {
				notifyDataConsumers(
					m_value instanceof Resource ? DataConstants.RESOURCE_CHANGE : DataConstants.LITERAL_CHANGE,
					newTo
				);
			} else {
				notifyDataConsumers(
					m_value instanceof Resource ? DataConstants.RESOURCE_DELETION : DataConstants.LITERAL_DELETION,
					m_value
				);
				notifyDataConsumers(
					newTo instanceof Resource ? DataConstants.RESOURCE_CHANGE : DataConstants.LITERAL_CHANGE,
					newTo
				);
			}
			m_value = newTo;
		}
	}
	
	protected void notifyDataConsumers(Resource changeType, Object change) {
		Iterator i = m_dataConsumers.iterator();
		while (i.hasNext()) {
			IDataConsumer dataConsumer = (IDataConsumer) i.next();
			
			dataConsumer.onDataChanged(changeType, change);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_dataProvider != null) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}
}
