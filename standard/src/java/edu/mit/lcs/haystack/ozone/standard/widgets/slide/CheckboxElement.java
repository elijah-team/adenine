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

package edu.mit.lcs.haystack.ozone.standard.widgets.slide;

import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class CheckboxElement extends SpanElement {
	protected CheckImageElement			m_checkImage;
	protected transient RDFListener		m_rdfListener;
	protected Resource					m_resOnChange;
	
	protected BooleanDataConsumer			m_dataConsumer;
	protected BooleanDataProviderWrapper	m_dataProviderWrapper;
	
	final static Resource s_checked	= new Resource(SlideConstants.s_namespace + "checked");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(CheckboxElement.class);
	
	public boolean isChecked() {
		return m_checkImage.isChecked();
	}
	
	boolean m_b = false;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		m_checkImage.initializeFromDeserialization(source);
		
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().initializeFromDeserialization(source);
		} else {
			setupListener();
		}
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().unregisterConsumer(m_dataConsumer);
			m_dataProviderWrapper.dispose();
			m_dataProviderWrapper = null;
		}
		m_dataConsumer = null;
		
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}
		
		super.dispose();
	}

	/**
	 * @see VisualPartBase#getInitializationData()
	 */
	protected void getInitializationData() {
		super.getInitializationData();

		m_resOnChange = Utilities.getResourceProperty(m_prescription, PartConstants.s_onChange, m_partDataSource);
	}
	
	protected void setupListener() {
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_source) {
			public void statementAdded(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					Statement m_s;
						
					public IdleRunnable init(Statement s) {
						m_s = s;
						return this;
					}
						
					public void run() {
						onCheckedChange(m_s);
					}
				}.init(s));
			}
		};
			
		m_rdfListener.start();
		try {
			m_rdfListener.addPattern(m_prescription, s_checked, null);
		} catch (Exception e) {
			s_logger.error("Failed to watch for checked on " + m_prescription, e);
		}
	}

	/**
	 * @see VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		Resource dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
		if (dataSource != null) {
			IDataProvider dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
			if (dataProvider == null) {
				return;
			}
			
			m_dataProviderWrapper = new BooleanDataProviderWrapper(dataProvider);
			m_dataConsumer = new BooleanDataConsumer() {
				
				protected void onBooleanChanged(Boolean b) {
					m_b = b.booleanValue();
					if (Ozone.isUIThread() && m_initializing) {
						if (m_checkImage.isChecked() != m_b) {
							m_checkImage.toggle();
						}
					} else {
						Ozone.idleExec(new IdleRunnable(m_context) {
							public void run() {
								if (m_context != null && m_checkImage.isChecked() != m_b) {
									m_checkImage.toggle();
									onChildResize(new ChildPartEvent(CheckboxElement.this));
								}
							}
						});
					}
				}
			};
			dataProvider.registerConsumer(m_dataConsumer);
		} else {
			setupListener();
		}
	}

	static final Resource CHECKED_IMAGE = new Resource(SlideConstants.s_namespace + "checkedImage");
	static final Resource UNCHECKED_IMAGE = new Resource(SlideConstants.s_namespace + "uncheckedImage");
	
	static final Resource DEFAULT_CHECKED_IMAGE = new Resource("http://haystack.lcs.mit.edu/data/ozone/common/checked.gif");
	static final Resource DEFAULT_UNCHECKED_IMAGE = new Resource("http://haystack.lcs.mit.edu/data/ozone/common/unchecked.gif");
	protected Resource m_part;
	protected Resource m_checked;
	protected Resource m_unchecked;
	
	protected void initializeChildren() {
		m_part = (Resource) m_context.getLocalProperty(OzoneConstants.s_part);
		m_checked = Utilities.getResourceProperty(m_part, CHECKED_IMAGE, m_source);
		m_unchecked = Utilities.getResourceProperty(m_part, UNCHECKED_IMAGE, m_source);
		CheckImageElement checkImage = new CheckImageElement();
		
		TextElement text = new TextElement(" ");
		
		Context childContext = new Context(m_context);
		
		childContext.putLocalProperty(OzoneConstants.s_partData, m_prescription);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		
		checkImage.initialize(m_source, childContext);
		
		childContext = new Context(m_context);

		childContext.putLocalProperty(OzoneConstants.s_partData, m_prescription);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		
		text.initialize(m_source, childContext);

		m_childParts.add(checkImage);
		m_childData.add(new Boolean(true));

		m_childParts.add(text);
		m_childData.add(new Boolean(true));
		
		m_checkImage = checkImage;
		
		super.initializeChildren();
	}

	/**
	 * @see SpanElement#handleMouseEnter(MouseEvent)
	 */
	protected void handleMouseEnter(MouseEvent e) {
		super.handleMouseEnter(e);

		Control c = (Control) m_context.getSWTControl();
		c.setCursor(GraphicsManager.s_handCursor);
	}

	/**
	 * @see SpanElement#handleMouseExit(MouseEvent)
	 */
	protected void handleMouseExit(MouseEvent e) {
		super.handleMouseExit(e);

		Control c = (Control) m_context.getSWTControl();
		c.setCursor(null);
	}
	
	public void toggle() {
		m_checkImage.toggle();
		
		try {
			m_source.remove(new Statement(
					m_prescription,
					s_checked,
					Utilities.generateWildcardResource(1)
				),
				Utilities.generateWildcardResourceArray(1)
			);
			m_source.add(new Statement(
				m_prescription,
				s_checked,
				new Literal(m_checkImage.isChecked() ? "true" : "false")
			));
		} catch (RDFException ex) {
			s_logger.error("Failed to toggle checked on " + m_prescription, ex);
		}
		
		if (m_dataProviderWrapper != null) {
			try {
				m_dataProviderWrapper.requestBooleanSet(new Boolean(m_checkImage.isChecked()));
			} catch (DataMismatchException ex) {
				s_logger.error("Failed to request boolean set", ex);
			}
		}
	}

	/**
	 * @see VisualPartBase#onMouseUp(MouseEvent)
	 */
	protected boolean onMouseUp(MouseEvent e) {
		toggle();

		if (m_resOnChange != null) {
			Interpreter i = Ozone.getInterpreter();
			DynamicEnvironment denv = new DynamicEnvironment(m_source);
			Ozone.initializeDynamicEnvironment(denv, m_context);
			try {
				Resource resResult = (Resource) i.callMethod(m_resOnChange, new Object[] { m_prescription, m_context, this }, denv);
			} catch(AdenineException ex) {
				s_logger.error("Error calling method onChange " + m_resOnChange, ex);
			}				
		}
				
		return true;
	}
	
	protected void onCheckedChange(Statement s) {
		RDFNode node = s.getObject();
		
		if (node instanceof Literal) {
			Literal 	literal = (Literal) node;
			boolean		checked = literal.getContent().equals("true");
			
			if (checked != m_checkImage.isChecked()) {
				m_checkImage.toggle();
			}
		}
	}

	class CheckImageElement extends ImageElement {
		transient Image		m_imageChecked;
		transient Image		m_imageUnchecked;
		Rectangle	m_rect = new Rectangle(0, 0, 0, 0);
		
		public CheckImageElement() {
			initImages();
		}
		
		public void initializeFromDeserialization(IRDFContainer source) {
			super.initializeFromDeserialization(source);

			initImages();
		}
		
		protected void retrieveImageData(boolean signalResize) {
		}
		
		protected void initImages() {
			Image imageChecked = GraphicsManager.acquireImage(m_checked != null ? m_checked : DEFAULT_CHECKED_IMAGE, SlideUtilities.getAmbientColor(CheckboxElement.this.m_context), 100, CheckboxElement.this.m_source, CheckboxElement.this.m_context.getServiceAccessor());
			Image imageUnchecked = GraphicsManager.acquireImage(m_unchecked != null ? m_unchecked : DEFAULT_UNCHECKED_IMAGE, SlideUtilities.getAmbientColor(CheckboxElement.this.m_context), 100, CheckboxElement.this.m_source, CheckboxElement.this.m_context.getServiceAccessor());
			m_imageChecked = imageChecked;
			m_imageUnchecked = imageUnchecked;
			m_image = m_hoverImage = m_b ? m_imageChecked : m_imageUnchecked;
		}
	
		/**
		 * @see IPart#dispose()
		 */
		public void dispose() {
			m_image = null;
			m_hoverImage = null;
			m_rect = null;
			
			if (m_imageChecked != null) {
				GraphicsManager.releaseImage(m_imageChecked);
				m_imageChecked = null;
			}
			if (m_imageUnchecked != null) {
				GraphicsManager.releaseImage(m_imageUnchecked);
				m_imageUnchecked = null;
			}
			
			super.dispose();
		}
	
		/**
		 * @see VisualPartBase#getInitializationData()
		 */
		protected void getInitializationData() {
			super.getInitializationData();
			
			if (Utilities.checkBooleanProperty(m_prescription, CheckboxElement.s_checked, m_partDataSource)) {
				m_image = m_imageChecked;
			} else {
				m_image = m_imageUnchecked;
			}
			
			m_textAlign = BlockScreenspace.ALIGN_TEXT_BASE_LINE;
			calculateImageAlignment();
		}
	
		protected void initializeSources() {
		}
		
		public void toggle() {
			if (m_image == m_imageChecked) {
				m_image = m_imageUnchecked;
			} else {
				m_image = m_imageChecked;
			}
			calculateImageAlignment();
			
			repaint(m_rect);
		}
		
		/**
		 * @see ImageElement#setBounds(Rectangle)
		 */
		public void setBounds(Rectangle r) {
			super.setBounds(r);
			
			m_rect.x = r.x; m_rect.y = r.y; m_rect.width = r.width; m_rect.height = r.height;
		}
	
		/**
		 * @see ImageElement#draw(GC, Rectangle)
		 */
		public void draw(GC gc, Rectangle r) {
			m_rect.x = r.x; m_rect.y = r.y; m_rect.width = r.width; m_rect.height = r.height;
			
			super.draw(gc, r);
		}
		
		public boolean isChecked() {
			return m_image == m_imageChecked;
		}
		
		protected void calculateImageAlignment() {
			FontMetrics fontMetrics = Ozone.getFontMetrics(SlideUtilities.getAmbientFont(m_context));
			
			m_baseLineOffset = (fontMetrics.getAscent() + m_image.getBounds().height) / 2 - 1;
			
			m_textAlign = BlockScreenspace.ALIGN_TEXT_BASE_LINE;
		}
	}
}
