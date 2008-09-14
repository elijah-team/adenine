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

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class FieldSetContainerPart extends VisualPartBase implements IBlockGUIHandler {
	IVisualPart				m_visualPart;
	IBlockGUIHandler		m_bgh;
	
	FieldDataProvider		m_fieldDataProvider;
	
	Rectangle				m_rect = new Rectangle(0, 0, 0, 0);
		
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FieldSetContainerPart.class);
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_visualPart != null) {
			m_visualPart.dispose();
			m_visualPart = null;
		}
		
		if (m_fieldDataProvider != null) {
			m_fieldDataProvider.dispose();
			m_fieldDataProvider = null;
		}

		super.dispose();
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if ((cls == null || cls.equals(IBlockGUIHandler.class)) && m_bgh != null) {
			return this;
		} else {
			return null;
		}
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object e) {
		boolean r = false;
		if (m_visualPart != null) {
			if (!eventType.equals(PartConstants.s_eventChildResize) &&
				!eventType.equals(PartConstants.s_eventDragSetData)) {
				
				r = m_visualPart.handleEvent(eventType, e);
			} else if (eventType.equals(PartConstants.s_eventChildResize)) {
				m_bgh = (IBlockGUIHandler) m_visualPart.getGUIHandler(IBlockGUIHandler.class);
			}
		}
		if (!r) {
			r = super.handleEvent(eventType, e);
		}
		return r;
	}

	protected void internalInitialize() {
		//m_context.putProperty(OzoneConstants.s_partCache, new UnserializableWrapper(new PartCache()));

		Resource dataSource = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_fields, m_partDataSource);
		if (dataSource != null) {
			Context	childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, dataSource);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			m_fieldDataProvider = new FieldDataProvider();
			m_fieldDataProvider.initialize(m_source, childContext);
			
			m_context.putProperty(LayoutConstants.s_fieldsDataProvider, m_fieldDataProvider);
		}
		
		Resource childPartData = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_child, m_partDataSource);
		if (childPartData != null) {
			try {
				Resource resPart = Ozone.findPart(childPartData, m_source, m_partDataSource);
				Class c = Utilities.loadClass(resPart, m_source);
				
				m_visualPart = (IVisualPart) c.newInstance();
				if (m_visualPart != null) {
					Context childContext = new Context(m_context);
				
					childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
					childContext.putLocalProperty(OzoneConstants.s_partData, childPartData);
					childContext.putLocalProperty(OzoneConstants.s_part, resPart);
					
					m_visualPart.initialize(m_source, childContext);
					m_bgh = (IBlockGUIHandler) m_visualPart.getGUIHandler(IBlockGUIHandler.class);
				}
			} catch (Exception e) {
				s_logger.error("Error initializing child part " + childPartData, e);
			}
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (m_bgh != null) {
			return m_bgh.calculateSize(hintedWidth, hintedHeight);
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		internalSetBounds(r);
		if (m_bgh != null) {
			m_bgh.draw(gc, r);
		}
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("FieldSetContainerPart");
		if (m_bgh != null) m_bgh.renderHTML(he);
		he.exit("FieldSetContainerPart");
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		if (m_bgh != null) {
			return m_bgh.getFixedSize();
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		if (m_bgh != null) {
			return m_bgh.getHintedDimensions();
		}
		return IBlockGUIHandler.FIXED_SIZE;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		if (m_bgh != null) {
			return m_bgh.getTextAlign();
		}
		return BlockScreenspace.ALIGN_TEXT_BASE_LINE;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		internalSetBounds(r);
		if (m_bgh != null) {
			m_bgh.setBounds(r);
		}
	}
	
	void internalSetBounds(Rectangle r) {
		if (!r.equals(m_rect)) {
			if (m_fieldDataProvider != null) {
				m_fieldDataProvider.setDimension(r.width);
			}
			m_rect.x = r.x; m_rect.y = r.y; m_rect.width = r.width; m_rect.height = r.height;
		}
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (m_bgh != null) {
			m_bgh.setVisible(visible);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		//m_context.putProperty(OzoneConstants.s_partCache, new UnserializableWrapper(new PartCache()));

		if (m_visualPart != null) {
			m_visualPart.initializeFromDeserialization(source);
		}

		if (m_fieldDataProvider != null) {
			m_fieldDataProvider.initializeFromDeserialization(source);
		}
	}
}
