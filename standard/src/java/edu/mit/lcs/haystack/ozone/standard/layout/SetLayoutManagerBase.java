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

package edu.mit.lcs.haystack.ozone.standard.layout;

import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.data.*;

import java.util.*;

/**
 * @author Dennis Quan
 * @author David Huynh
 */
abstract public class SetLayoutManagerBase extends LayoutManagerBase {
	protected IDataConsumer				m_dataConsumer;
	protected SetDataProviderWrapper	m_dataProviderWrapper;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SetLayoutManagerBase.class);

	protected void makeDataConsumer() {
		m_dataProviderWrapper = new SetDataProviderWrapper(m_dataProvider);
		m_dataConsumer = new SetDataConsumer() {
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsAdded(Set)
			 */
			protected void onItemsAdded(Set items) {
				SetLayoutManagerBase.this.onItemsAdded(items);
			}
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(Set)
			 */
			protected void onItemsRemoved(Set items) {
				SetLayoutManagerBase.this.onItemsRemoved(items);
			}
			/**
			 * @see edu.mit.lcs.haystack.ozone.data.
			 * SetDataConsumer#onSetCleared()
			 */
			protected void onSetCleared() {
				SetLayoutManagerBase.this.onSetCleared();
			}
		};
		m_dataProvider.registerConsumer(m_dataConsumer);		
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_dataConsumer != null) {
			m_dataProvider.unregisterConsumer(m_dataConsumer);
			m_dataConsumer = null;
		}
		m_dataProviderWrapper = null;
		
		super.dispose();
	}

	final static protected int	s_elementsAdded = 0;
	final static protected int	s_elementsRemoved = 1;
	final static protected int	s_elementsChanged = 2;
	final static protected int	s_setCleared = 3;
	
	protected class ElementsEvent extends IdleRunnable {
		int		m_event;
		Set			m_elements;
		
		public ElementsEvent(int event, Set elements) {
			super(m_context);
			m_event = event;
			m_elements = elements;
		}
		
		public void run() {
			if (m_dataConsumer == null) {
				return;
			}
			
			switch (m_event) {
			case s_elementsAdded:
				processElementsAdded(m_elements);
				break;
			case s_elementsRemoved:
				processElementsRemoved(m_elements);
				break;
			case s_setCleared:
				processSetCleared();
				break;
			}
		}
	}

	protected void onItemsAdded(Set items) {
		if (Ozone.isUIThread()) {
			processElementsAdded(items);
		} else {
			Ozone.idleExec(new ElementsEvent(s_elementsAdded, items));
		}
	}
	
	protected void onItemsRemoved(Set removedElements) {
		if (Ozone.isUIThread()) {
			processElementsRemoved(removedElements);
		} else {
			Ozone.idleExec(new ElementsEvent(s_setCleared, null));
		}
	}
	
	protected void onSetCleared() {
		if (Ozone.isUIThread()) {
			processSetCleared();
		} else {
			Ozone.idleExec(new ElementsEvent(s_elementsRemoved, null));
		}
	}
	
	abstract protected void processElementsAdded(Set addedElements);
	abstract protected void processElementsRemoved(Set removedElements);
	abstract protected void processSetCleared();
}
