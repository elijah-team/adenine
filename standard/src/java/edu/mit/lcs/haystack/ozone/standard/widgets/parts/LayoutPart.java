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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.ILayoutManager;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.ozone.standard.layout.LayoutConstants;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * @author David Huynh
 */
public class LayoutPart extends VisualPartBase {
	protected Resource		m_dataSource;
	protected IDataProvider	m_dataProvider;
	protected IDataProvider	m_selectionDataProvider;
	protected IDataProvider	m_focusDataProvider;
	
	protected Resource		m_layoutConstraint;	
	protected ILayoutManager	m_layoutManager;
	
	protected Resource		m_properties;
	
	transient protected RDFListener		m_rdfListener;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(LayoutPart.class);
	
	protected void getInitializationData() {
		super.getInitializationData();
		
		m_dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
		m_layoutConstraint = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_layoutConstraint, m_partDataSource);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_layoutManager != null) {
			m_layoutManager.initializeFromDeserialization(source);
		}
		if (m_focusDataProvider != null) {
			m_focusDataProvider.initializeFromDeserialization(source);
		}
		if (m_dataProvider != null) {
			m_dataProvider.initializeFromDeserialization(source);
		}
		if (m_selectionDataProvider != null) {
			m_selectionDataProvider.initializeFromDeserialization(source);
		}
		setupListener();
	}
	
	protected void internalInitialize() {
		super.internalInitialize();
		
		if (m_dataSource != null) {
			m_context.putProperty(OzoneConstants.s_dataSource, m_dataSource);
			m_dataProvider = DataUtilities.createDataProvider(m_dataSource, m_context, m_source, m_partDataSource);
		}

		Resource selection = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_selection, m_partDataSource);
		if (selection != null) {
			m_selectionDataProvider = DataUtilities.createDataProvider(selection, m_context, m_source, m_partDataSource);
		}
	
		Resource focus = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_focus, m_partDataSource);
		if (focus != null) {
			m_focusDataProvider = DataUtilities.createDataProvider(focus, m_context, m_source, m_partDataSource);
		}

		setupListener();	
		createLayoutManager();	
	}
	
	protected void setupListener() {
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource) {
			public void statementAdded(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					public void run() {
						Resource layoutConstraint = (Resource) m_s.getObject(); 
						if (m_context != null && !m_initializing && !layoutConstraint.equals(m_layoutConstraint)) {
							m_layoutConstraint = layoutConstraint;
							createLayoutManager();
							onChildResize(new ChildPartEvent(LayoutPart.this));
						}
					}
					
					Statement m_s;
					
					public IdleRunnable init(Statement s) {
						m_s = s;
						return this;
					}
				}.init(s));
			}
		};
		
		m_rdfListener.start();
		try {
			m_rdfListener.addPattern(m_prescription, LayoutConstants.s_layoutConstraint, null);
		} catch (Exception e) {
			s_logger.error("Failed to watch for layout constraint on " + m_prescription, e);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_layoutManager != null) {
			m_layoutManager.dispose();
			m_layoutManager = null;
		}
		m_layoutConstraint = null;
		
		if (m_selectionDataProvider != null) {
			m_selectionDataProvider.dispose();
			m_selectionDataProvider = null;
		}
		if (m_focusDataProvider != null) {
			m_focusDataProvider.dispose();
			m_focusDataProvider = null;
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.dispose();
			m_dataProvider = null;
		}
		m_dataSource = null;
		
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}
		
		super.dispose();
	}
	
	protected void createLayoutManager() {
		if (m_layoutManager != null) {
			m_layoutManager.dispose();
			m_layoutManager = null;
		}
		
		if (m_layoutConstraint == null) {
			s_logger.error("No layout constraint specified");
			return;
		}

		Resource layoutManagerPart = null;
		try {
			layoutManagerPart = Ozone.findPart(m_layoutConstraint, m_infoSource, m_partDataSource);
			if (layoutManagerPart == null) {
				s_logger.error("Failed to find part for layout constraint " + m_layoutConstraint);
				return;
			}
		} catch (RDFException e) {
			s_logger.error("Failed to find part for layout constraint " + m_layoutConstraint, e);
		}

		try {
			Class cls = Utilities.loadClass(layoutManagerPart, m_source);
		
			m_layoutManager = (ILayoutManager) cls.newInstance();
			if (m_layoutManager == null) {
				s_logger.error("Failed to instantiate part " + cls + " for layout constraint " + m_layoutConstraint);
				return;
			}			
		} catch (Exception e) {
			s_logger.error("Failed to instantiate part for layout constraint " + m_layoutConstraint, e);
		}

		Context context = new Context(m_context);
		
		context.putLocalProperty(OzoneConstants.s_partData, m_layoutConstraint);
		context.putLocalProperty(OzoneConstants.s_part, layoutManagerPart);
		context.putLocalProperty(OzoneConstants.s_parentPart, this);
		context.putLocalProperty(OzoneConstants.s_dataProvider, m_dataProvider);
		context.putLocalProperty(OzoneConstants.s_layoutInstance, Utilities.generateUniqueResource());
		
		if (m_focusDataProvider != null) {
			context.putLocalProperty(LayoutConstants.s_focus, m_focusDataProvider);
		}
		if (m_selectionDataProvider != null) {
			context.putLocalProperty(LayoutConstants.s_selection, m_selectionDataProvider);
		}
		
		m_layoutManager.initialize(m_source, context);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (m_layoutManager != null) {
			return m_layoutManager.getGUIHandler(cls);
		}
		return null;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#handleEvent(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		if (eventType.equals(PartConstants.s_eventChildResize)) {
			return super.handleEvent(eventType, event);
		}
		return m_layoutManager.handleEvent(eventType, event);
	}
	
	public ILayoutManager getCurrentLayoutManager() {
		return m_layoutManager;
	}
}
