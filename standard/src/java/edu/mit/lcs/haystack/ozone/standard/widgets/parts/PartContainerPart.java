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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class PartContainerPart extends SingleChildContainerPartBase {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PartContainerPart.class);
	
	protected IDataProvider m_dataProvider = null;
	protected boolean m_ownsDataProvider = false;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.SingleChildContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_ownsDataProvider) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}
	
	protected void internalInitialize() {
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource chainedDataSource = (Resource) Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_partDataSource, m_partDataSource);
				m_ownsDataProvider = m_dataProvider != null;
			}
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(new ResourceDataConsumer() {
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceChanged(Resource newResource) {
					asyncInitializeChild(newResource);
				}
				
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceDeleted(Resource previousResource) {
					asyncInitializeChild(null);
				}
			});
		}
	}
	
	protected void asyncInitializeChild(Resource child) {
		if (Ozone.isUIThread()) {
			initializeChild(child);
		} else {
			Ozone.idleExec(new IdleRunnable(m_context) {
				/* (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					initializeChild(m_child);
				}
				
				Resource m_child;
				
				IdleRunnable init(Resource child2) {
					m_child = child2;
					return this;
				}
			}.init(child));
		}
	}
	
	protected void initializeChild(Resource child) {
		if (m_child != null) {
			m_child.dispose();
		}

		if (child != null) {
			try {
				Resource part = Ozone.findPart(child, m_source, m_partDataSource);
				Class c = Utilities.loadClass(part, m_source);
				
				if (IVisualPart.class.isAssignableFrom(c)) {
					Context childContext = new Context(m_context);
					
					childContext.putLocalProperty(OzoneConstants.s_part, part);
					childContext.putLocalProperty(OzoneConstants.s_partData, child);
					childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
					
					m_child = (IVisualPart) c.newInstance();
					m_child.initialize(m_source, childContext);
					onChildResize(new ChildPartEvent(this));
				}
			} catch (Exception e) {
				s_logger.error("Failed to initialize child part " + child, e);
			}
		}
	}
	
	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (m_child != null) {
			return m_child.getGUIHandler(cls);
		} else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		
		if (m_ownsDataProvider) {
			m_dataProvider.dispose();
			m_ownsDataProvider = false;
		}
	}

}
