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

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class ResizablePartContainerPart extends SingleChildContainerPartBase implements IBlockGUIHandler {
	boolean			m_dimensionSet = false;
	int				m_minDimension = s_resizeGrip;
	int				m_dimension = -1;
	
	Rectangle			m_bounds = new Rectangle(0, 0, 0, 0);
	
	final static int	s_resizeGrip = 5; // pixels

	final public static Resource DIMENSION = new Resource(PartConstants.s_namespace + "dimension");
	final public static Resource MIN_DIMENSION = new Resource(PartConstants.s_namespace + "minDimension");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ResizablePartContainerPart.class);
	
	protected void internalInitialize() {
		String s = Utilities.getLiteralProperty(m_prescription, MIN_DIMENSION, m_partDataSource);
		if (s != null) {
			m_minDimension = Math.max(s_resizeGrip, Integer.parseInt(s));
		}

		s = Utilities.getLiteralProperty(m_prescription, DIMENSION, m_partDataSource);
		if (s != null) {
			m_dimension = Math.max(m_minDimension, Integer.parseInt(s));
			m_dimensionSet = true;
		}

		createChild();		
	}
	
	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			m_gh = m_child.getGUIHandler(IBlockGUIHandler.class);
			return this;
		}
		m_gh = null;
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (m_gh instanceof IBlockGUIHandler) {
			BlockScreenspace bs = ((IBlockGUIHandler) m_gh).calculateSize(hintedWidth, m_dimensionSet ? m_dimension : hintedHeight);
			if (bs != null) {
				if (m_dimensionSet) {
					bs.m_size.y = m_dimension;
				}
				return bs;
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (m_gh instanceof IBlockGUIHandler) {
			((IBlockGUIHandler) m_gh).draw(gc, r);
		}
	}

	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("ResizablePartContainerPart");
		if (m_gh instanceof IBlockGUIHandler) {
		  ((IBlockGUIHandler) m_gh).renderHTML(he);
		}
		he.exit("ResizablePartContainerPart");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		if (m_gh instanceof IBlockGUIHandler) {
			return ((IBlockGUIHandler) m_gh).getFixedSize();
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		if (m_gh instanceof IBlockGUIHandler) {
			return ((IBlockGUIHandler) m_gh).getHintedDimensions();
		}
		return IBlockGUIHandler.FIXED_SIZE;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		if (m_gh instanceof IBlockGUIHandler) {
			return ((IBlockGUIHandler) m_gh).getTextAlign();
		}
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		if (m_gh instanceof IBlockGUIHandler) {
			((IBlockGUIHandler) m_gh).setBounds(r);
		}
		m_bounds.x = r.x; m_bounds.y = r.y; m_bounds.width = r.width; m_bounds.height = r.height;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseEvent(Resource, MouseEvent)
	 */
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (m_resizing) {
			return true;
		}
		
		boolean r = false;
		if (eventType.equals(PartConstants.s_eventMouseHover)) {
			r = onMouseHover((MouseEvent) event);
		} else if (eventType.equals(PartConstants.s_eventMouseMove)) {
			r = onMouseMove((MouseEvent) event);
		} else if (eventType.equals(PartConstants.s_eventMouseEnter)) {
			r = onMouseEnter((MouseEvent) event);
		} else if (eventType.equals(PartConstants.s_eventMouseExit)) {
			r = onMouseExit((MouseEvent) event);
		} else if (eventType.equals(PartConstants.s_eventMouseDown)) {
			r = onMouseDown((MouseEvent) event);
		}

		if (!r) {
			r = super.onMouseEvent(eventType, event);
		}
		return r;
	}

	protected int		m_mouseDownY;	
	protected int		m_heightBeforeResize;
	protected boolean	m_resizing = false;

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseDown(MouseEvent)
	 */
	protected boolean onMouseDown(MouseEvent e) {
		if (inResizeRegion(e)) {
			setResizeCursor(true);
			startResize(e);
			return true;
		}
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseEnter(MouseEvent)
	 */
	protected boolean onMouseEnter(MouseEvent e) {
		if (inResizeRegion(e)) {
			setResizeCursor(true);
		}
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseExit(MouseEvent)
	 */
	protected boolean onMouseExit(MouseEvent e) {
		setResizeCursor(false);
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseHover(MouseEvent)
	 */
	protected boolean onMouseHover(MouseEvent e) {
		setResizeCursor(inResizeRegion(e));
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseMove(MouseEvent)
	 */
	protected boolean onMouseMove(MouseEvent e) {
		setResizeCursor(inResizeRegion(e));
		return false;
	}

	protected boolean inResizeRegion(MouseEvent e) {
		return Math.abs(e.y - (m_bounds.y + m_bounds.height)) <= s_resizeGrip;
	}

	protected boolean m_showingResizeCursor = false;
	protected void setResizeCursor(boolean resize) {
		if (resize != m_showingResizeCursor) {
			Control control = (Control) m_context.getSWTControl();
			if (resize) {
				control.setCursor(GraphicsManager.s_vertResizeCursor);
			} else {
				control.setCursor(null);
			}
			m_showingResizeCursor = resize;
		}
	}
	
	protected boolean m_movement = false;
	protected boolean startResize(MouseEvent e) {
		Control control = (Control) m_context.getSWTControl();
		m_movement = false;
		
		MouseMoveListener mml = new MouseMoveListener() {
			Control m_control;
			
			public MouseMoveListener initialize(Control control) {
				m_control = control;
				return this;
			}
			
			public void mouseMove(MouseEvent e) {
				onResizing(e);
			}
		}.initialize(control);
		
		MouseListener ml = new MouseAdapter() {
			Control				m_control;
			MouseMoveListener	m_mml;
			
			MouseListener initialize(Control control, MouseMoveListener mml) {
				m_control = control;
				m_mml = mml;
				return this;
			}
			
			public void mouseUp(MouseEvent me) {
				m_control.setCapture(false);
				m_control.removeMouseListener(this);
				m_control.removeMouseMoveListener(m_mml);
				m_resizing = false;
				PartUtilities.setContainerControlDraggable(true, m_context);

				if (!m_movement) {
					m_dimensionSet = false;
					m_dimension = -1;
					onChildResize(new ChildPartEvent(ResizablePartContainerPart.this));
				}
			}				
		}.initialize(control, mml);
		
		PartUtilities.setContainerControlDraggable(false, m_context);
		m_heightBeforeResize = m_bounds.height;
		m_mouseDownY = e.y;
		m_resizing = true;
		
		control.setCapture(true);
		control.addMouseMoveListener(mml);
		control.addMouseListener(ml);
		
		return true;
	}
	
	protected void onResizing(MouseEvent e) {
		int diff = e.y - m_mouseDownY;
		
		m_dimension = Math.max(m_minDimension, m_heightBeforeResize + diff);
		m_dimensionSet = true;
		
		onChildResize(new ChildPartEvent(this));
		m_movement = true;
	}
}
