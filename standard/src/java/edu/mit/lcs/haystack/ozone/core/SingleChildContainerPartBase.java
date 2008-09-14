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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.utils.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;

import org.apache.log4j.Logger;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.events.*;

/**
 * This class is mostly useful for handling mouse event on
 * several child parts.
 * 
 * @version 	1.0
 * @author		David Huynh
 */
public class SingleChildContainerPartBase extends VisualPartBase implements IGUIHandler, IVisualPart {
	protected IVisualPart		m_child;
	protected IGUIHandler		m_gh;
	protected boolean 			m_hitChild = false;
	
	static Logger s_logger = Logger.getLogger(SingleChildContainerPartBase.class);
	
	protected Context createChildContext(Resource childPrescription, Resource childPart) {
		Context childContext = new Context(m_context);
					
		childContext.putLocalProperty(OzoneConstants.s_part, childPart);
		childContext.putLocalProperty(OzoneConstants.s_partData, childPrescription);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);

		return childContext;
	}
	
	protected Resource findPart(Resource prescription, IRDFContainer source, IRDFContainer partDataSource) throws RDFException {
		return Ozone.findPart(prescription, source, partDataSource);
	}

	protected void createChild() {
		if (m_child != null) {
			m_child.dispose();
			m_child = null;
		}

		Resource childPrescription = Utilities.getResourceProperty(m_prescription, SlideConstants.s_child, m_partDataSource);
		if (childPrescription == null) {
			childPrescription = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_child, m_partDataSource);
		}
		
		if (childPrescription != null) {
			try {
				Resource childPart = findPart(childPrescription, m_source, m_partDataSource);
				Class c = Utilities.loadClass(childPart, m_source);
					
				if (IVisualPart.class.isAssignableFrom(c)) {
					m_child = (IVisualPart) c.newInstance();
					m_child.initialize(m_source, createChildContext(childPrescription, childPart));
				}
			} catch (Exception e) {
				s_logger.error("Failed to initialize child prescription " + childPrescription, e);
			}
		}
	}
	
	protected ParentChildMouseHandler m_parentChildMouseHandler = new ParentChildMouseHandler() {
		protected IVisualPart getPreviousChildPart() {
			return m_hitChild ? m_child : null;
		}

		protected void setPreviousChildPart(IVisualPart vp) {
			m_hitChild = vp == m_child;
		}

		protected IVisualPart hittest(int x, int y) {
			return SingleChildContainerPartBase.this.hittestChild(x, y) ? m_child : null;
		}
	};
	ParentChildDropHandler m_parentChildDropHandler = new ParentChildDropHandler() {
		protected IVisualPart getPreviousChildPart() {
			return m_hitChild ? m_child : null;
		}

		protected void setPreviousChildPart(IVisualPart vp) {
			m_hitChild = vp == m_child;
		}

		protected IVisualPart hittest(int x, int y) {
			return SingleChildContainerPartBase.this.hittestChild(x, y) ? m_child : null;
		}
	};

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_child != null) {
			m_child.dispose();
			m_child = null;
		}
		
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (m_gh != null) {
			m_gh.setVisible(visible);
		}
	}

	/**
	 * @see VisualPartBase#onMouseEvent(Resource, MouseEvent)
	 */
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (eventType.equals(PartConstants.s_eventMouseUp) && event.button == 3) {
			prepareContextMenu(event);
		}
		
		boolean r = m_parentChildMouseHandler.letChildrenHandleEvent(eventType, event);
		if (!r) {
			r = super.onMouseEvent(eventType, event);
		} else {
			onChildHasHandledMouseEvent(event);
		}
		return r;
	}
	protected void onChildHasHandledMouseEvent(MouseEvent event) {
	}
	
	/**
	 * @see VisualPartBase#onGotInputFocus(FocusEvent)
	 */
	protected boolean onGotInputFocus(FocusEvent e) {
		if (m_child != null) {
			return m_child.handleEvent(PartConstants.s_eventGotInputFocus, e);
		}
		return false;
	}
	
	/**
	 * @see VisualPartBase#onLostInputFocus(FocusEvent)
	 */
	protected boolean onLostInputFocus(FocusEvent e) {
		if (m_child != null) {
			return m_child.handleEvent(PartConstants.s_eventLostInputFocus, e);
		}
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHittest(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onContentHittest(ContentHittestEvent e) {
		boolean 	r = false;
		boolean	hitChild = hittestChild(e.m_x, e.m_y);
		
		if (hitChild) {
			r = m_child.handleEvent(PartConstants.s_eventContentHittest, e);
		}
		if (!r) {
			r = internalContentHittest(e);
		}
		
		return r;
	}
	protected boolean internalContentHittest(ContentHittestEvent e) {
		return false;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHighlight(edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent)
	 */
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		if (m_child != null) {
			return m_child.handleEvent(PartConstants.s_eventContentHighlight, event);
		}
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onDrag(DragSourceEvent)
	 */
	protected boolean onDrag(DragSourceEvent e) {
		boolean r = false;
		
		if (m_hitChild) {
			r = m_child.handleEvent(PartConstants.s_eventDrag, e);
		}
		if (!r) {
			r = super.onDrag(e);
		}
		return r;
	}

	/**
	 * @see VisualPartBase#onDropTargetEvent(Resource, DropTargetEvent)
	 */
	protected boolean onDropTargetEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		boolean r = m_parentChildDropHandler.letChildrenHandleEvent(eventType, event);

		if (!r) {
			r = super.onDropTargetEvent(eventType, event);
		} else {
			onChildHasHandledDragAndDropEvent(eventType, event);
		}
		return r;
	}
	protected void onChildHasHandledDragAndDropEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_dropOperations != DND.DROP_NONE) {
			handleEvent(PartConstants.s_eventContentHighlight, new ContentHighlightEvent(this, false));
			m_dropOperations = DND.DROP_NONE;
		}
	}

	protected boolean hittestChild(int x, int y) {
		return true;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IVisualPart#getGUIHandler(java.lang.Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (m_child != null) {
			m_gh = m_child.getGUIHandler(cls);
		} else {
			m_gh = null;
		} 
		return m_gh;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onChildResize(edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		if (m_child != null) {
			m_gh = m_child.getGUIHandler(null);
		}
		return super.onChildResize(e);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_child != null) {
			m_child.initializeFromDeserialization(source);
		}
	}
}
