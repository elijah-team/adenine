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

package edu.mit.lcs.haystack.ozone.core;

import java.util.EventObject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.eclipse.Plugin;
import edu.mit.lcs.haystack.ozone.core.utils.DragAndDropHandler;
import edu.mit.lcs.haystack.ozone.core.utils.FocusHandler;
import edu.mit.lcs.haystack.ozone.core.utils.KeyHandler;
import edu.mit.lcs.haystack.ozone.core.utils.MouseHandler;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class VisualPartAwareComposite extends Composite {
	protected IPart		m_part;
	protected boolean		m_draggable = true;
	protected Point		m_mouseDown;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(VisualPartAwareComposite.class);
	
	public VisualPartAwareComposite(Composite parent, boolean allowsDragDrop) {
		this(parent, allowsDragDrop, true);
	}
	
	public VisualPartAwareComposite(Composite parent, boolean allowsDragDrop, boolean noBackground) {
		super(parent, noBackground ? SWT.NO_BACKGROUND : 0);

		setBackground(GraphicsManager.s_white);

		addInnerEventListeners();
		if (allowsDragDrop) {
			addDragDrop();
		}
	}
	
	protected boolean handleEvent(Resource eventType, Object event) {
		if (m_part != null) {
			return m_part.handleEvent(eventType, event);
		}
		return false;
	}
	protected void addInnerEventListeners() {
		new PaintHandler() {
			protected void drawContent(GC gc, Rectangle r) {
				VisualPartAwareComposite.this.drawContent(gc, r);
			}
			protected void renderHTML(HTMLengine he) {
				VisualPartAwareComposite.this.renderHTML(he);
			}
		}.initialize(this);
		
		new MouseHandler() {
			protected boolean handleEvent(Resource eventType, MouseEvent e) {
				return internalHandleEvent(eventType, e);
			}

			protected boolean isDisposed() {
				return VisualPartAwareComposite.this.isDisposed();
			}

			protected void onMouseDown(MouseEvent me) {
				m_mouseDown = new Point(me.x, me.y);
				super.onMouseDown(me);
				m_control.setFocus();
			}

			protected void onMouseEnter(MouseEvent me) {
				setCursor(null);
				super.onMouseEnter(me);
			}
		}.initialize(this);
		
		new KeyHandler() {
			protected boolean handleEvent(Resource eventType, KeyEvent e) {
				return internalHandleEvent(eventType, e);
			}

			protected boolean isDisposed() {
				return VisualPartAwareComposite.this.isDisposed();
			}
		}.initialize(this);
		
		new FocusHandler() {
			protected boolean handleEvent(Resource eventType, FocusEvent e) {
				return internalHandleEvent(eventType, e);
			}

			protected boolean isDisposed() {
				return VisualPartAwareComposite.this.isDisposed();
			}
		}.initialize(this);
	}

	protected void addDragDrop() {
		new DragAndDropHandler() {
			protected Point getMouseDownPoint() {
				return m_mouseDown;
			}

			protected boolean handleDropEvent(
				Resource eventType,
				edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
				//if (!eventType.equals(PartConstants.s_eventDrop)) {
					Point pt = toControl(new Point(event.m_x, event.m_y));
					event.m_x = pt.x;
					event.m_y = pt.y;
				//}			
				if (!handleEvent(eventType, event)) {
					event.m_dropTargetEvent.detail = DND.DROP_NONE;
					return false;
				}
				return true;
			}

			protected boolean handleEvent(
				Resource eventType,
				EventObject event) {
					
				return internalHandleEvent(eventType, event);
			}

			protected boolean isDraggable() {
				return m_draggable && !VisualPartAwareComposite.this.isDisposed();
			}

			protected boolean isDroppable() {
				return !VisualPartAwareComposite.this.isDisposed();
			}
		}.initialize(this);
	}
	
	public void layout() {
		if (m_part != null) {
			try {
				IVisualPart 		vp = (IVisualPart) m_part;
				IBlockGUIHandler 	sn = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
				if (sn != null) {
					sn.setBounds(getClientArea());
				}
				
				redraw();
			} catch (Exception e) {
			}
		}
	}

	public void setVisualPart(IPart part) {
		m_part = part;
	}
	
	public void setDraggable(boolean draggable) {
		m_draggable = draggable;
	}
	
	protected boolean internalHandleEvent(Resource eventType, EventObject event) {
		if (PartConstants.s_eventKeyPressed == eventType) {
			KeyEvent ke = (KeyEvent) event;
			if (ke.keyCode == 16777237) { // F12
				Plugin.getHaystack().toggleDebug();
				return true;
			}
		}
		
		if (m_part != null) {
			return m_part.handleEvent(eventType, event);
		}
		return false;
	}
	
	protected void drawContent(GC gc, Rectangle r) {
		if (m_part instanceof IVisualPart) {
			IVisualPart 		vp = (IVisualPart) m_part;
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
	
			if (blockGUIHandler != null) {
				blockGUIHandler.draw(gc, r);
			}
		}
	}
	
	public void renderHTML(HTMLengine he) {
		he.enter("VisualPartAwareComposite");
		if (m_part instanceof IVisualPart) {
			IVisualPart 		vp = (IVisualPart) m_part;
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
	
			if (blockGUIHandler != null) {
				blockGUIHandler.renderHTML(he);
			}
		}
		he.exit("VisualPartAwareComposite");
	}	
}
