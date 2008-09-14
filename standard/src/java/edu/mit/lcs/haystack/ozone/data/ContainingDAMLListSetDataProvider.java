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
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class ContainingDAMLListSetDataProvider extends ChainedDataProvider {
	Resource	m_cookie;
	HashSet		m_damlLists = new HashSet();
	HashMap		m_nodesToLists = new HashMap();
	Resource	m_item;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ContainingDAMLListSetDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	protected boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource == null) {
			return;
		}

		setupSources(source, context);
		
		internalInitialize(source, context, true);
		
		m_initializing = false;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				synchronized (ContainingDAMLListSetDataProvider.this) {
					if (!newResource.equals(m_item)) {
						onChainedResourceChanged(newResource);
					}
				}
			}
	
			protected void onResourceDeleted(Resource previousResource) {
				synchronized (ContainingDAMLListSetDataProvider.this) {
					if (m_item != null) {
						onChainedResourceChanged(null);
					}
				}
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		m_damlLists.clear();
		m_nodesToLists.clear();
		m_damlLists = null;
		m_nodesToLists = null;
		
		m_item = null;
		
		super.dispose();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	protected void onChainedResourceChanged(Resource newSubject) {
		int count = m_damlLists.size();
		
		m_damlLists.clear();
		m_nodesToLists.clear();
		if (m_cookie != null) {
			removePattern(m_cookie);
			m_cookie = null;
		}
		
		if (count > 0) {
			notifyDataConsumers(DataConstants.SET_CLEAR, null);
		}
		
		m_item = newSubject;
		if (m_item != null) {
			cacheData();
		}

		System.err.println("----onChainedResourceChanged " + newSubject + " " + m_damlLists);
		
	}
	
	synchronized protected void onStatementAdded(Statement s) {
		if (s.getObject().equals(m_item)) {
			Resource node = s.getSubject();
			
			if (m_nodesToLists.get(node) == null) {
				try {
					Resource list = getListFromNode(node);
					
					m_nodesToLists.put(node, list);
					
					if (!m_damlLists.contains(list)) {
						HashSet addedItems = new HashSet();
						
						addedItems.add(list);
						m_damlLists.add(list);
						
						notifyDataConsumers(DataConstants.SET_ADDITION, addedItems);
					}
				} catch (RDFException e) {
					s_logger.error("Failed to get DAML list from node", e);
				}
			}
		}
	}
	
	synchronized protected void onStatementRemoved(Statement s) {
		if (s.getObject().equals(m_item)) {
			Resource node = s.getSubject();
			Resource list = (Resource) m_nodesToLists.remove(node);
			
			if (list != null) {
				if (!m_nodesToLists.containsValue(list)) {
					HashSet removedItems = new HashSet();
					
					removedItems.add(list);
					
					m_damlLists.remove(list);
					
					notifyDataConsumers(DataConstants.SET_REMOVAL, removedItems);
				}
			}
		}
	}
	
	protected void cacheData() {
		try {
			m_cookie = addPattern(null, Constants.s_daml_first, m_item);
			
			Set nodes = m_infoSource.query(
				new Statement(
					Utilities.generateWildcardResource(1), 
					Constants.s_daml_first, 
					m_item),
				Utilities.generateWildcardResourceArray(1)
			);
			Iterator i = nodes.iterator();
			
			while (i.hasNext()) {
				Resource node = (Resource) ((RDFNode[]) i.next())[0];
				Resource list = getListFromNode(node);
				
				m_damlLists.add(list);
				m_nodesToLists.put(node, list);
			}
		} catch (RDFException e) {
			s_logger.error("Error while caching data", e);
		}
	}
	
	protected Resource getListFromNode(Resource node) throws RDFException {
		while (true) {
			Resource previousNode = (Resource) m_infoSource.extract(null, Constants.s_daml_rest, node);
			
			if (previousNode == null) {
				break;
			}
			
			node = previousNode;
		}
		return node;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_damlLists.size() > 0) {
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, m_damlLists.clone());
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType.equals(DataConstants.SET)) {
			return m_damlLists.clone();
		}
		throw new DataNotAvailableException("No data of type " + dataType);
	}

}
