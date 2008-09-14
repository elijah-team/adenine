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
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.verbs.IVerb;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public abstract class VisualPartBase extends GenericPart implements IVisualPart, IGUIHandler {
	protected Resource			m_resUnderlying;
	protected Resource			m_resViewInstance;
	
	protected Resource			m_resID;
	protected Resource			m_resOnClick;
	protected Resource			m_resOnEnterPressed;
	
	protected String			m_tooltip;
	public int m_uid = 0;
	static int s_uid = 0;
	
	protected boolean			m_initializing = true;
	
	protected Resource[]		m_services;
	
	protected CSSstyle			m_CSSstyle;	
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(VisualPartBase.class);
	
	/*
	// Leak detection code
	static HashMap m_leaks = new HashMap();
	
	public static Set getInstances() {
		HashSet s = new HashSet();
		s.addAll(m_leaks.keySet());
		return s;
	}

	public static void removeInstances(Set s) {
		m_leaks.keySet().removeAll(s);
	}

	public static void dump() {
		TreeSet ts = new TreeSet(new Comparator() {
			int depth(Context c) {
				Context c2 = c.getParentContext();
				if (c2 == null) {
					return 1;
				} else {
					return depth(c2) + 1;
				}
			}
			
			public int compare(Object arg0, Object arg1) {
				Object[] a0 = (Object[]) arg0;
				Object[] a1 = (Object[]) arg1;
			
				Context c0 = (Context) a0[1];
				Context c1 = (Context) a1[1];
				
				return depth(c0) - depth(c1);
			}
		});
		ts.addAll(m_leaks.values());
		
		System.out.println(">> " + ts.size() + " leaks:");
		Iterator i = ts.iterator();
		for (int j = 0; j < 60 && i.hasNext(); j++) {
			Object[] x = (Object[])i.next();
			System.out.println(x[0]);
			System.out.println(x[1]);
			System.out.println();
		}
	}*/

	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		/*synchronized (getClass()) {
			java.io.StringWriter sw = new java.io.StringWriter();
			new Exception().printStackTrace(new java.io.PrintWriter(sw));
			m_leaks.put(this, new Object[] { sw.toString(), context });
		}*/

		setupSources(source, context);
		
		getInitializationData();
		internalInitialize();
		
		m_initializing = false;
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_context != null) {
			PartUtilities.unregisterToolbarItems(m_partDataSource, m_context, m_prescription);
			
			if (m_services != null) {
				for (int i = 0; i < m_services.length; i++) {
					m_context.removeGlobalProperty(m_services[i]);
				}
			}
			m_context = null;
		}

		super.dispose();				
		
//		m_resPartData = null;
		m_resUnderlying = null;
		m_resViewInstance = null;

		m_resID = null;
		m_resOnClick = null;
		m_tooltip = null;

		/*synchronized (getClass()) {
			m_leaks.remove(this);
		}*/
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		if (event instanceof EventObject) {
			return handleGUIEvent(eventType, (EventObject) event);
		}
		return false;
	}
	
	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		return null;
	}
	
	/*
	 *  @see IVisualPart#getClickHandler()
	 */
	public IVisualPart getClickHandler() { 
		if (m_resOnClick != null) return this;
		Object obj = m_context.getLocalProperty(OzoneConstants.s_parentPart);
		if (!(obj instanceof IVisualPart)) return null;
		IVisualPart parent = (IVisualPart)obj;
		return parent.getClickHandler();
	}
	
	public boolean handleGUIEvent(Resource eventType, EventObject event) {
		if (eventType.equals(PartConstants.s_eventServletRequest)) {
			return onServletRequest(event);
		} else if (event instanceof MouseEvent) {
			return onMouseEvent(eventType, (MouseEvent) event);
		} else if (event instanceof DragSourceEvent) {
			return onDragSourceEvent(eventType, (DragSourceEvent) event);
		} else if (event instanceof OzoneDropTargetEvent) {
			return onDropTargetEvent(eventType, (OzoneDropTargetEvent) event);
		} else if (event instanceof ContentHighlightEvent) {
			return onContentHighlight((ContentHighlightEvent) event);
		} else if (event instanceof ContentHittestEvent) {
			return onContentHittest((ContentHittestEvent) event);
		} else if (eventType.equals(PartConstants.s_eventChildResize)) {
			return onChildResize((ChildPartEvent) event);
		} else if (eventType.equals(PartConstants.s_eventGotInputFocus)) {
			return onGotInputFocus((FocusEvent) event);
		} else if (eventType.equals(PartConstants.s_eventLostInputFocus)) {
			return onLostInputFocus((FocusEvent) event);
		}
		return false;		
	}
	
	/**
	 * @see IGUIHandler@setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
	}
	
	protected void getInitializationData() {
		if (m_uid == 0) m_uid = s_uid++;
		m_resUnderlying = (Resource) m_context.getLocalProperty(OzoneConstants.s_underlying);
		m_resViewInstance = (Resource) m_context.getLocalProperty(OzoneConstants.s_viewInstance);
		
		m_resID = Utilities.getResourceProperty(m_prescription, PartConstants.s_id, m_partDataSource);
		m_resOnClick = Utilities.getResourceProperty(m_prescription, PartConstants.s_onClick, m_partDataSource);
		m_resOnEnterPressed = Utilities.getResourceProperty(m_prescription, PartConstants.s_onEnterPressed, m_partDataSource);
		m_tooltip = Utilities.getLiteralProperty(m_prescription, PartConstants.s_tooltip, m_partDataSource);

		m_resID = m_resID != null ? m_resID : (Resource) m_context.getLocalProperty(PartConstants.s_id);
		m_resOnClick = m_resOnClick != null ? m_resOnClick : (Resource) m_context.getLocalProperty(PartConstants.s_onClick);
		m_resOnEnterPressed = m_resOnEnterPressed != null ? m_resOnEnterPressed : (Resource) m_context.getLocalProperty(PartConstants.s_onEnterPressed);
		m_tooltip = m_tooltip != null ? m_tooltip : (String) m_context.getLocalProperty(PartConstants.s_tooltip);		
		if (m_CSSstyle == null) m_CSSstyle = new CSSstyle();
	}
	
	protected void internalInitialize() {
		if (m_uid == 0) m_uid = s_uid++;
		m_services = Utilities.getResourceProperties(m_prescription, OzoneConstants.s_registerService, m_partDataSource);
		m_CSSstyle = new CSSstyle();
		if (m_services != null) {
			for (int i = 0; i < m_services.length; i++) {
				m_context.putGlobalProperty(m_services[i], this);
			}
		}
		
		if (true) { //(m_resOnClick != null) {
			m_context.putProperty(PartConstants.s_hovered, new Boolean(false));
			m_CSSstyle.setAttribute("cursor", "pointer");
		}

		PartUtilities.registerServices(m_partDataSource, m_context, m_prescription);
		PartUtilities.registerToolbarItems(m_partDataSource, m_context, m_prescription);
	}
	
	protected boolean onServletRequest(EventObject event) {
		Ozone.idleExec(new ServletRunnable(event));
		return true;
	}
	
	class ServletRunnable extends IdleRunnable {
		EventObject m_event;
		ServletRunnable(EventObject event) {
			super(m_context);
			m_event = event;
		}
		public void run() {	handleEvent(PartConstants.s_eventMouseUp, m_event); }
	}

	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {	
		if (PartUtilities.filterFakeDropEvents(eventType, event, m_context, m_source, this)) {
			return true;
		}
			
		if (eventType.equals(PartConstants.s_eventMouseHover)) {
			return onMouseHover(event);
		} else if (eventType.equals(PartConstants.s_eventMouseMove)) {
			return onMouseMove(event);
		} else if (eventType.equals(PartConstants.s_eventMouseEnter)) {
			return onMouseEnter(event);
		} else if (eventType.equals(PartConstants.s_eventMouseExit)) {
			return onMouseExit(event);
		} else if (eventType.equals(PartConstants.s_eventMouseDown)) {
			return onMouseDown(event);
		} else if (eventType.equals(PartConstants.s_eventMouseUp)) {
			return onMouseUp(event);
		} else if (eventType.equals(PartConstants.s_eventMouseDoubleClick)) {
			return onMouseDoubleClick(event);
		} else {
			return false;
		}
	}
	
	protected boolean onMouseUp(MouseEvent e) {
		switch (e.button) {
		case 1: 
			if((e.stateMask & SWT.CONTROL) == 0)
				return onClick(e);
		case 2:
		case 3:
			if (handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, e.x, e.y))) {
				return showContextMenu(e);
			}
		}
		return false;
	}

	protected boolean onMouseDown(MouseEvent e) {
		return false;
	}
	
	protected boolean onMouseDoubleClick(MouseEvent e) {
		return false;
	}

	protected boolean onMouseHover(MouseEvent e) {
		return false;
	}

	protected boolean onMouseMove(MouseEvent e) {
		return false;
	}
	
	protected boolean onMouseEnter(MouseEvent e) {
		Control c = (Control) m_context.getSWTControl();
		
		if (m_resOnClick != null) {
			c.setCursor(GraphicsManager.s_handCursor);
			m_context.putProperty(PartConstants.s_hovered, new Boolean(true));
		}
		if (m_tooltip != null) {
			c.setToolTipText(m_tooltip);
		}
		
		return false;
	}

	protected boolean onMouseExit(MouseEvent e) {
		if (m_context == null) {
			return false;
		}
		
		Control c = (Control) m_context.getSWTControl();
		
		if (m_resOnClick != null) {
			c.setCursor(null);
			m_context.putProperty(PartConstants.s_hovered, new Boolean(false));
		}
		if (m_tooltip != null) {
			c.setToolTipText(null);
		}
		
		return false;
	}
	
	protected boolean onContentHittest(ContentHittestEvent e) {
		return false;
	}

	protected boolean onContentHighlight(ContentHighlightEvent event) {
		return false;
	}
	
	protected boolean onDragSourceEvent(Resource eventType, DragSourceEvent event) {
		if (eventType.equals(PartConstants.s_eventDrag)) {
			return onDrag(event);
		} else if (eventType.equals(PartConstants.s_eventDragEnd)) {
			return onDragEnd(event);
		} else if (eventType.equals(PartConstants.s_eventDragSetData)) {
			return onDragSetData(event);
		}
		return false;
	}
	
	protected boolean onDrag(DragSourceEvent e) {
		if (m_resUnderlying != null) {
			Event e2 = new Event();
			
			e2.widget = e.widget;
			e2.time = e.time;
			e2.type = SWT.MouseExit;
			
			handleEvent(PartConstants.s_eventMouseExit, new MouseEvent(e2));

			e.data = this;
			return true;
		}
		return false;
	}
	
	protected boolean onDragEnd(DragSourceEvent e) {
		return true;
	}

	protected boolean onDragSetData(DragSourceEvent e) {
		return PartUtilities.setDragData(e, m_context);
	}
	
	protected boolean onDropTargetEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (eventType.equals(PartConstants.s_eventDragEnter)) {
			return onDragEnter(event);
		} else if (eventType.equals(PartConstants.s_eventDragHover)) {
			return onDragHover(event);
		} else if (eventType.equals(PartConstants.s_eventDragExit)) {
			return onDragExit(event);
		} else if (eventType.equals(PartConstants.s_eventDragOperationChanged)) {
			return onDragOperationChanged(event);
		} else if (eventType.equals(PartConstants.s_eventDropAccept)) {
			return onDropAccept(event);
		} else if (eventType.equals(PartConstants.s_eventDrop)) {
			return onDrop(event);
		} else if (eventType.equals(PartConstants.s_eventFakeDrop)) {
			return onFakeDrop(event);
		}
		return false;
	}
	
	protected int m_dropOperations = DND.DROP_NONE;
	protected boolean onDragEnter(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_resUnderlying != null && handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, event.m_x, event.m_y))) {
			handleEvent(PartConstants.s_eventContentHighlight, new ContentHighlightEvent(this, true));
			m_dropOperations = /*event.m_dropTargetEvent.operations*/ DND.DROP_COPY;
		}
		event.m_dropTargetEvent.detail = m_dropOperations;
		return m_dropOperations != DND.DROP_NONE;
	}
	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_dropOperations != DND.DROP_NONE) {
			handleEvent(PartConstants.s_eventContentHighlight, new ContentHighlightEvent(this, false));
			m_dropOperations = DND.DROP_NONE;
		}
		event.m_dropTargetEvent.detail = m_dropOperations;
		return m_dropOperations != DND.DROP_NONE;
	}
	protected boolean onDragHover(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_resUnderlying != null) {
			int dropOperations = handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, event.m_x, event.m_y))
				? /*event.m_dropTargetEvent.operations*/ DND.DROP_COPY : DND.DROP_NONE;
				
			if (dropOperations != m_dropOperations) { 	
				handleEvent(PartConstants.s_eventContentHighlight, new ContentHighlightEvent(this, dropOperations != DND.DROP_NONE));
				m_dropOperations = dropOperations;
			}
		}
		event.m_dropTargetEvent.detail = m_dropOperations;
		return m_dropOperations != DND.DROP_NONE;
	}
	protected boolean onDragOperationChanged(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_dropOperations;
		return m_dropOperations != DND.DROP_NONE;
	}
	protected boolean onDropAccept(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_dropOperations;
		return m_dropOperations != DND.DROP_NONE;
	}
	protected boolean onDrop(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_dropOperations != DND.DROP_NONE) {
			boolean r = PartUtilities.performDrop(event, m_source, m_context);
			handleEvent(PartConstants.s_eventContentHighlight, new ContentHighlightEvent(this, false));
			m_dropOperations = DND.DROP_NONE;
			
			return r;
		}
		return false;
	}
	
	protected boolean onFakeDrop(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		return PartUtilities.performDrop(event.m_data, m_context, m_source);
	}
	
	protected boolean onGotInputFocus(FocusEvent e) {
		return false;
	}
	
	protected boolean onLostInputFocus(FocusEvent e) {
		return false;
	}
	
	protected boolean onClick(MouseEvent e) {
		if (m_resOnClick != null) {
			try {
				Resource resPart = Ozone.findPart(m_resOnClick, m_source, m_partDataSource);
				if (resPart != null) {
					IPart	part = (IPart)Utilities.loadClass(resPart, m_source).newInstance();
					Context	childContext = new Context(m_context);
					
					childContext.putLocalProperty(OzoneConstants.s_part, resPart);
					childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
					childContext.putLocalProperty(OzoneConstants.s_partData, m_resOnClick);
					
					part.initialize(m_source, childContext);
					
					if (part instanceof IBehavior) {
						((IBehavior) part).activate(m_prescription, this, e);
					} else if (part instanceof IVerb) {
						((IVerb) part).activate();
					}
					part.dispose();
					
					return true;
				}
			} catch (Exception ex) {
				s_logger.error("Failed to invoke onClick behavior " + m_resOnClick, ex);
			}
		}
		return false;
	}
	
	protected boolean onEnterPressed() {
		if (m_resOnEnterPressed != null) {
			try {
				Resource resPart = Ozone.findPart(m_resOnEnterPressed, m_source, m_partDataSource);
				if (resPart != null) {
					IPart	part = (IPart)Utilities.loadClass(resPart, m_source).newInstance();
					Context	childContext = new Context(m_context);
					
					childContext.putLocalProperty(OzoneConstants.s_part, resPart);
					childContext.putLocalProperty(OzoneConstants.s_partData, m_resOnEnterPressed);
					
					part.initialize(m_source, childContext);
					
					if (part instanceof IBehavior) {
						((IBehavior) part).activate(m_prescription, this, null);
					} else if (part instanceof IVerb) {
						((IVerb) part).activate();
					}
					
					part.dispose();
					
					return true;
				}
			} catch (Exception ex) {
				s_logger.error("Error invoking onEnterPressed behavior "  + m_resOnEnterPressed, ex);
			}
		}
		return false;
	}
	
	protected boolean showContextMenu(MouseEvent e) {
		Control control = (Control) m_context.getSWTControl();
		Point	point = control.toDisplay(new Point(e.x, e.y));
		
		if ((e.stateMask & SWT.SHIFT) != 0) {
			PartUtilities.showSummary(m_source, m_context, point);
		} else {
			prepareContextMenu(e);
			showContextMenu(m_context, point);
		}
		
		return true;
	}
	
	protected void showContextMenu(Context c, Point displayPt) {
		PartUtilities.showContextMenu(m_source, c, displayPt);
	}
	
	protected void prepareContextMenu(MouseEvent e) {
	}
	
	protected boolean onChildResize(ChildPartEvent e) {
		if (!Ozone.isUIThread()) {
			s_logger.error("Starting a child resize event not in UI thread", new Exception());
			return false;
		}
		
		if (m_initializing || m_context == null) {
			return false;
		}
		
		try {
			IPart parent = (IPart) m_context.getLocalProperty(OzoneConstants.s_parentPart);
			e.m_childPart = this;
			return parent.handleEvent(PartConstants.s_eventChildResize, e);
		} catch (Exception ex) {
			s_logger.error("Failed to process onChildResize event on part with data " + m_prescription, ex);
		}
		return false;
	}

	protected void repaint(Rectangle area) {
		Control parent = (Control) m_context.getSWTControl();
		parent.redraw(area.x, area.y, area.width, area.height, true);
	}
	
	static final Resource CONTEXT_OPERATIONS = new Resource("http://haystack.lcs.mit.edu/ui/contextMenu#contextOperation");
	protected void removeContextOperations() {
		try {
			m_partDataSource.remove(
				new Statement(m_prescription, CONTEXT_OPERATIONS, Utilities.generateWildcardResource(1)),
				Utilities.generateWildcardResourceArray(1));
		} catch (RDFException e) {
		}
	}

	protected void debugPrint(Object o) {
		if (m_resID != null && m_resID.equals(OzoneConstants.s_debug)) {
			System.err.println("----" + this + ": " + o);
		}
	}
	
	protected boolean isDebug() {
		return (m_resID != null && m_resID.equals(OzoneConstants.s_debug));
	}
}
