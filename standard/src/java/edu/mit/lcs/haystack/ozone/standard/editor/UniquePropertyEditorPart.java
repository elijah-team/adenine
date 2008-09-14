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

package edu.mit.lcs.haystack.ozone.standard.editor;

import edu.mit.lcs.haystack.Constants;
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
import edu.mit.lcs.haystack.ozone.data.StringDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class UniquePropertyEditorPart extends SingleChildContainerPartBase {
	protected IDataProvider			m_dataProvider = null;
	protected boolean				m_ownsDataProvider = false;
	
	protected boolean				m_disallowBlanks = false;
	
	protected LiteralEditorPart		m_literalEditorPart = null;
	protected IVisualPart			m_messagePart = null;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(UniquePropertyEditorPart.class);
	static public final Resource s_clickHereToCreate = new Resource("http://haystack.lcs.mit.edu/ui/ozoneeditor#clickHereToCreate");
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.SingleChildContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_messagePart != null) {
			m_messagePart.initializeFromDeserialization(source);
		}
		
		if (m_literalEditorPart != null) {
			m_literalEditorPart.initializeFromDeserialization(source);
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		super.dispose();

		if (m_literalEditorPart != null) { 
			m_literalEditorPart.dispose();
		}
		
		if (m_messagePart != null) {
			m_messagePart.dispose();
		}

		if (m_ownsDataProvider && m_dataProvider != null) {
			m_dataProvider.dispose();
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		try {
			
			Resource resPart = Ozone.findPart(s_clickHereToCreate, m_source, m_partDataSource);
			m_messagePart = (IVisualPart)Utilities.loadClass(resPart, m_source).newInstance();
			Context childContext = new Context(m_context);
			childContext.putLocalProperty(OzoneConstants.s_partData, s_clickHereToCreate);
			childContext.putLocalProperty(OzoneConstants.s_part, resPart);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			m_messagePart.initialize(m_source, childContext);
			setVisible(m_messagePart, false);
		} catch (Exception e) {
			s_logger.error("Failed to initialize message part", e);
		}

		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (dataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
				m_ownsDataProvider = true;
			}
		}

		try {
			m_literalEditorPart = new LiteralEditorPart();
			Context childContext = new Context(m_context);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			childContext.putLocalProperty(OzoneConstants.s_dataProvider, m_dataProvider);
			m_literalEditorPart.initialize(m_source, childContext);
			setVisible(m_literalEditorPart, false);
		} catch (Exception e) {
			s_logger.error("Failed to initialize literal editor part", e);
		}

		// Check for disallow blanks property
		Resource predicate = (Resource) m_context.getDescendantLocalProperty(OzoneConstants.s_aspect);
		if (predicate != null) {
			m_disallowBlanks = Utilities.checkBooleanProperty(predicate, Constants.s_editor_disallowBlanks, m_source);
			if (m_disallowBlanks) m_literalEditorPart.enableBlankSupport();
		}

		if (m_disallowBlanks) {
			setPart(m_literalEditorPart);
		} else {
			setPart(m_messagePart);
		}

		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(new StringDataConsumer() {
				/**
				 * @see edu.mit.lcs.haystack.ozone.data.StringDataConsumer#onStringChanged(java.lang.String)
				 */
				protected void onStringChanged(String newString) {
					Ozone.idleExec(new IdleRunnable(m_context) {
						/**
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							setPart(m_literalEditorPart);
						}
					});				
				}

				/**
				 * @see edu.mit.lcs.haystack.ozone.data.StringDataConsumer#onStringDeleted(java.lang.String)
				 */
				protected void onStringDeleted(String previousString) {
					Ozone.idleExec(new IdleRunnable(m_context) {
						/**
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							if (!m_disallowBlanks) {
								setPart(m_messagePart);
							}
						}
					});
				}
			});
		}
	}

	public void setPart(IVisualPart part) {
		if (Ozone.isUIThread()) {
			internalSetPart(part);
		} else {
			Ozone.idleExec(new IdleRunnable(m_context) {
				IVisualPart m_part;

				public IdleRunnable initialize(IVisualPart part) {
					m_part = part;
					return this;
				}

				public void run() {
					internalSetPart(m_part);
				}
			}.initialize(part));
		}
	}

	protected void internalSetPart(IVisualPart newVisualPart) {
		if (newVisualPart != m_child) {
			setVisible(m_child, false);
			setVisible(newVisualPart, true);

			m_child = newVisualPart;
			onChildResize(new ChildPartEvent(this));
		}
	}

	protected void setVisible(IVisualPart vp, boolean visible) {
		if (vp != null) {
			IGUIHandler gh = vp.getGUIHandler(null);
			if (gh != null) {
				gh.setVisible(visible);
			}
		}
	}
}
