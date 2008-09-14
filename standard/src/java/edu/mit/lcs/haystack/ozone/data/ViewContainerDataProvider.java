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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ListDataChange;
import edu.mit.lcs.haystack.ozone.data.ListDataConsumer;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.ViewContainerPart;

import java.util.*;

/**
 * Data provider that turns a list of information objects into part data consisting of view containers showing 
 * the underlying objects with the specified viewpartclass.
 * @author Karun Bakshi
 */
public class ViewContainerDataProvider extends ChainedDataProvider {
	protected List m_viewContainerList = new ArrayList();
	protected Resource m_viewPartClass;	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ViewContainerDataProvider.class);
	
	synchronized public Object getData(Resource dataType, Object specifications) throws DataNotAvailableException {
		Object retVal = null;
		int i;
		int numElements, beginIndex, endIndex;
		
		if (dataType == null) {
			throw new DataNotAvailableException("Null type specified for Data Provider");		
		}
		else if (dataType.equals(DataConstants.LIST)) {			
			if (specifications == null) {
				retVal = new ArrayList(m_viewContainerList);
			}
			else {
				ArrayList newList = new ArrayList();
				numElements = ((Integer) specifications).intValue();
				if (numElements > 0) {
					beginIndex = 0;
					endIndex = numElements - 1;
				}
				else {
					beginIndex = m_viewContainerList.size() + numElements;
					endIndex = m_viewContainerList.size() - 1;		
				}
				for (i = beginIndex; i <= endIndex; i++) {
					newList.add(m_viewContainerList.get(i));						
				}
				retVal = newList;
			}
		}
		else if (dataType.equals(DataConstants.LIST_ELEMENT)) {
			try {
				retVal =  m_viewContainerList.get(((Integer) specifications).intValue());
			} catch (Exception e) {
			}
		}
		else if (dataType.equals(DataConstants.LIST_ELEMENTS)) {
			Object[] a = (Object[]) specifications;
			int		index = ((Integer) a[0]).intValue();
			int		count = ((Integer) a[1]).intValue();
			
			try {
				ArrayList newList = new ArrayList();
				newList.addAll(m_viewContainerList.subList(index, index + count));
				retVal = newList;
			} catch (Exception e) {
					s_logger.error("Failed to get list elements", e);
			}
		}
		else if (dataType.equals(DataConstants.LIST_COUNT)) {
			retVal = new Integer(m_viewContainerList.size());
		}
		
		return retVal;		
	}
		
	synchronized protected void onConsumerAdded(IDataConsumer dataConsumer) {		
		dataConsumer.reset();
		if (m_viewContainerList.size() > 0) {
				dataConsumer.onDataChanged(
					DataConstants.LIST_ADDITION, new ListDataChange(0, m_viewContainerList.size(), m_viewContainerList));
		}
	}
	
	synchronized protected IDataConsumer createDataConsumer() {
		return new DataConsumer();
	}
	
	class DataConsumer extends ListDataConsumer implements IDataConsumer {		
		
		/**
		 * Notifies that a "count" number of new elements starting at 
		 * "index" have been inserted. These elements are available in the
		 * list returned from IDataProvider.getData().
		 */
		synchronized protected void onElementsAdded(int index, int count) {
			
			try {				
				ArrayList newItemsList = (ArrayList)m_dataProvider.getData(DataConstants.LIST, null);
				Iterator it = newItemsList.iterator();

				while  (it.hasNext()) {
					m_viewContainerList.add(addViewContainer((Resource)it.next()));
				}
				notifyDataConsumers(DataConstants.LIST_ADDITION, new ListDataChange(index, count, m_viewContainerList.subList(index, index+count)));
			} catch (DataNotAvailableException e) {
				s_logger.error(e);
			} 						
		}
	
		/**
		 * Notifies that a "count" number of existing elements
		 * starting at "index" have been removed. These elements are
		 * no longer available in the list returned from 
		 * IDataProvider.getData().
		 */
		synchronized protected void onElementsRemoved(int index, int count, List removedElements) {
			s_logger.error("ViewContainerDataProvider: Does not handle elements removed changed event");
		}
	
		/**
		 * Notifies that a "count" number of existing elements starting
		 * at "index" have changed.
		 */
		synchronized protected void onElementsChanged(int index, int count) {
			s_logger.error("ViewContainerDataProvider: Does not handle elements changed event");
		}
	
		/**
		 * Notifies that a list of existing elements as indexed by
		 * changedIndices have been changed.
		 * 
		 * Elements of changedIndices are Integers.
		 */
		synchronized protected void onElementsChanged(List changedIndices) {
			s_logger.error("ViewContainerDataProvider: Does not handle elements changed event");
		}
	
		synchronized protected void onListCleared() {
			clearViewContainersList();
			notifyDataConsumers(DataConstants.LIST_CLEAR, null);
		}

		synchronized public void reset() {
			clearViewContainersList();			
		}

	}
	
	/**
		 * Initializes the part.
		 * @param source The RDF data source for configuration information.
		 * @param context The Ozone Context.
		 */
	synchronized public void initialize(IRDFContainer source, Context context) {
			RDFNode node;
			
			setupSources(source, context);

			try {
				node = m_partDataSource.extract(m_prescription, OzoneConstants.s_viewPartClass, null);
				if (node instanceof Resource) {
					m_viewPartClass = (Resource)node;				
				}
				else {
					s_logger.error("Error: ViewContainerDataProvider expects a resource for the viewPartClass");
				}
			}			
			catch (RDFException e) {
				s_logger.error("Error: could not get view part class");
			}			

			internalInitialize(source, context, false);			
		}
	
		/**
		 * Disposes the part.
		 */
	synchronized public void dispose() {
			
			clearViewContainersList();
			
			super.dispose();		
			
		}		
		
	synchronized protected void clearViewContainersList() {
			Resource viewContainer = null;
			Iterator it = m_viewContainerList.iterator();
			while (it.hasNext()) {
				viewContainer = (Resource)it.next();
				removeViewContainer(viewContainer);
			}
			m_viewContainerList.clear();
		}
		
	synchronized protected void removeViewContainer(Resource viewContainer) {
			try {
				m_partDataSource.remove(new Statement(viewContainer, Constants.s_rdf_type, ViewContainerPart.s_ViewContainer), new Resource [] {});
				m_partDataSource.remove(new Statement(viewContainer, OzoneConstants.s_underlying, Utilities.generateWildcardResource(0)),Utilities.generateWildcardResourceArray(0));	
				m_partDataSource.remove(new Statement(viewContainer, OzoneConstants.s_viewPartClass, Utilities.generateWildcardResource(0)),Utilities.generateWildcardResourceArray(0));
			}	
			catch (RDFException e) {			
			}
		}
		
	synchronized protected Resource addViewContainer(Resource underlying) {
			Resource retVal = null;
			try {				
				retVal = Utilities.generateUniqueResource();				
				m_partDataSource.add(retVal, Constants.s_rdf_type, ViewContainerPart.s_ViewContainer);
				m_partDataSource.add(retVal, OzoneConstants.s_initialResource, underlying);	
				m_partDataSource.add(retVal, OzoneConstants.s_viewPartClass, m_viewPartClass);								
			}
			catch (RDFException e) {
			}		
			return retVal;
			
		}
}

