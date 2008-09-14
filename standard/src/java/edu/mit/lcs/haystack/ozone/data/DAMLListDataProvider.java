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
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @author Karun Bakshi
 */
public class DAMLListDataProvider extends ChainedDataProvider {
	protected ArrayList 		m_list = null;	
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(DAMLListDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	synchronized public void initialize(IRDFContainer source, Context context) {
		Resource 	list;		
		
		setupSources(source, context);
		list = (Resource) Utilities.getResourceProperty(m_prescription, DataConstants.DAML_LIST, m_partDataSource);
		if (list == null) {
			super.internalInitialize(source, context, false);
			try {
				list = (Resource) m_dataProvider.getData(DataConstants.RESOURCE, null);
			}	
			catch (DataNotAvailableException e) {											
			}			
		}	
		m_list = createListFromDAMLListResource(list);		
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		super.dispose();
		m_list.clear();
		m_list = null;
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.LIST)) {
			return m_list;
		} else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				return m_list.get(((Integer) specifications).intValue());
			} catch (Exception e) {
				s_logger.error("Failed to get element with spec " + specifications, e);
			}
		} else if (dataType.equals(DataConstants.LIST_ELEMENTS) && specifications instanceof Object[]) {
			Object[]	a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();
			
			try {
				ArrayList 	elements = new ArrayList();
				
				elements.addAll(m_list.subList(index, index + count));

				return elements;
			} catch (Exception e) {
				s_logger.error("Failed to get elements with index=" + index + " count=" + count, e);
			}
		}
		return null;
	}	
	
	synchronized protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer () {
			protected void onResourceChanged(Resource newResource) {
				if (m_list != null){
					m_list.clear();	
					notifyDataConsumers(DataConstants.LIST_CLEAR, null);
				}				
				m_list = createListFromDAMLListResource(newResource);
				notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(0, m_list.size(), m_list));
			}

			protected void onResourceDeleted(Resource previousResource) {
				m_list.clear();	
				notifyDataConsumers(DataConstants.LIST_CLEAR, null);
			}

			public void reset() {								
			}
		};
	}

	synchronized protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.reset();
		if (m_list.size() > 0) {
				dataConsumer.onDataChanged(
					DataConstants.LIST_ADDITION, new ListDataChange(0, m_list.size(), m_list));
		}
	}
	
	synchronized protected ArrayList createListFromDAMLListResource(Resource list) {
		ArrayList retVal = new ArrayList();
		Iterator	i;
		
		i = ListUtilities.accessDAMLList(list, m_partDataSource);		
		while (i.hasNext()) {
			retVal.add(i.next());
		}
		
		return retVal;
	}
}