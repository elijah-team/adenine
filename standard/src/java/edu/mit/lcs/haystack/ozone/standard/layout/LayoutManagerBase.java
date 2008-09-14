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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.ILayoutManager;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.MultipleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent;
import edu.mit.lcs.haystack.ozone.core.ParentChildDropHandler;
import edu.mit.lcs.haystack.ozone.core.ParentChildFocusHandler;
import edu.mit.lcs.haystack.ozone.core.ParentChildMouseHandler;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.PossibleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.SWT;
import java.util.*;

/**
 * @author David Huynh
 * 
 * This base part maintains a list of visual child parts and a list of 
 * other child parts. It also handles logic of passing mouse events
 * to its visual child parts.
 */
abstract public class LayoutManagerBase extends GenericPart implements ILayoutManager {
	protected IVisualPart			m_layoutPart;
	
	protected Resource			m_layoutManagerPart;
	protected Resource			m_id;

	protected IDataProvider		m_dataProvider;

	protected java.util.List		m_visualChildParts = new ArrayList();
	protected java.util.List		m_otherChildParts = new ArrayList();
	protected IVisualPart			m_previousVisualChildPart = null;
	
	protected Resource			m_nestingRelation;
	
	protected boolean				m_initializing = true;
	
	protected ParentChildMouseHandler	m_parentChildMouseHandler = new ParentChildMouseHandler() {
		protected IVisualPart getPreviousChildPart() {
			return m_previousVisualChildPart;
		}

		protected void setPreviousChildPart(IVisualPart vp) {
			m_previousVisualChildPart = vp;
		}

		protected IVisualPart hittest(int x, int y) {
			return LayoutManagerBase.this.hittest(x, y, false);
		}
	};
	protected ParentChildDropHandler m_parentChildDropHandler = new ParentChildDropHandler() {
		protected IVisualPart getPreviousChildPart() {
			return m_previousVisualChildPart;
		}

		protected void setPreviousChildPart(IVisualPart vp) {
			m_previousVisualChildPart = vp;
		}

		protected IVisualPart hittest(int x, int y) {
			return LayoutManagerBase.this.hittest(x, y, true);
		}

		protected void onChildEnter(IVisualPart childPart, OzoneDropTargetEvent e) {
			onChildDragEnter(e);
		}

		protected void onChildExit(IVisualPart previousChildPart, OzoneDropTargetEvent e) {
			onChildDragExit(e);
		}
	};
	protected ParentChildFocusHandler m_parentChildFocusHandler = new ParentChildFocusHandler() {
		protected java.util.List getChildParts() {
			return getFocusableChildParts();
		}
	};
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(LayoutManagerBase.class);
	
	protected class ChildResizeRunnable extends IdleRunnable {
		ChildResizeRunnable() {
			super(m_context);
		}

		public void run() {
			m_childResizeRunnable = null;
			if (m_context != null) {
				processChildResizeEvents();
			}
		}
	}
	transient protected ChildResizeRunnable	m_childResizeRunnable;
	protected java.util.List		m_childrenToResize = new ArrayList();
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);		
		
		m_layoutPart = (IVisualPart) m_context.getLocalProperty(OzoneConstants.s_parentPart);
		
		m_layoutManagerPart = (Resource) m_context.getLocalProperty(OzoneConstants.s_part);
		m_id = Utilities.getResourceProperty(m_prescription, PartConstants.s_id, m_partDataSource);
		
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		
		PartUtilities.registerViewPartClass(m_partDataSource, m_context, m_prescription);		
		PartUtilities.registerServices(m_partDataSource, m_context, m_prescription);
		PartUtilities.registerToolbarItems(m_partDataSource, m_context, m_prescription);
		
		m_nestingRelation = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_nestingRelation, m_partDataSource);
		
		internalInitialize();
		
		m_initializing = false;
	}
	
	protected void internalInitialize() {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		PartUtilities.unregisterToolbarItems(m_partDataSource, m_context, m_prescription);
		
		if (m_childResizeRunnable != null) {
			m_childResizeRunnable.expire();
			m_childResizeRunnable = null;
		}
		
		disposeChildParts();
		
		m_visualChildParts = null;
		m_otherChildParts = null;

		super.dispose();		
	}
	
	protected void disposeChildParts() {
		Iterator i;
		
		i = m_visualChildParts.iterator();
		while (i.hasNext()) {
			IVisualPart childPart = (IVisualPart) i.next();
			childPart.dispose();
		}
		m_visualChildParts.clear();

		i = m_otherChildParts.iterator();
		while (i.hasNext()) {
			IPart childPart = (IPart) i.next();
			childPart.dispose();
		}
		m_otherChildParts.clear();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		if (event instanceof EventObject) {
			return internalHandleGUIEvent(eventType, (EventObject) event);
		}
		return false;
	}
	
	/**
	 * Internal default GUI handling.
	 */
	
	protected boolean internalHandleGUIEvent(Resource eventType, EventObject event) {
		if (event instanceof MouseEvent) {
			return onMouseEvent(eventType, (MouseEvent) event);
			
		} else if (event instanceof KeyEvent) {
			return onKeyEvent(eventType, (KeyEvent) event);

		} else if (event instanceof FocusEvent) {
			return onFocusEvent(eventType, (FocusEvent) event);
			
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
		}
		return false;
	}

	public void internalSetVisible(boolean visible) {
		for (int i = 0; i < m_visualChildParts.size(); i++) {
			IVisualPart vp = (IVisualPart) m_visualChildParts.get(i);
			IGUIHandler	guiHandler = vp.getGUIHandler(null);
			
			if (guiHandler != null) {
				guiHandler.setVisible(visible);
			}
		}
	}
	
	protected boolean onChildResize(ChildPartEvent event) {
		if (!m_initializing) {
			if (!m_childrenToResize.contains(event.m_childPart)) {
				m_childrenToResize.add(event.m_childPart);
			}

			if (m_childResizeRunnable == null) {
				m_childResizeRunnable = new ChildResizeRunnable();
				Ozone.idleExec(m_childResizeRunnable);
			}
		}
		return true;
	}
	
	protected boolean onContentHittest(ContentHittestEvent event) {
		IVisualPart	visualChildPart = hittest(event.m_x, event.m_y, false);
		
		if (visualChildPart != null) {
			return visualChildPart.handleEvent(PartConstants.s_eventContentHittest, event);
		}
		return false;
	}
	
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		return false;
	}
	
	/**
	 * Mouse handling utilities.
	 */
	
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (eventType.equals(PartConstants.s_eventMouseUp) && event.button == 3) {
			prepareContextMenu(event);
		}
		
		if (PartUtilities.filterFakeDropEvents(eventType, event, m_context, m_source, this)) {
			return true;
		}
			
		boolean r = m_parentChildMouseHandler.letChildrenHandleEvent(eventType, event);
		if (!r) {
			if (PartUtilities.filterFakeDragEvents(eventType, event, m_context, m_source, this)) {
				return true;
			}
			
			if (eventType.equals(PartConstants.s_eventMouseHover)) {
				r = onMouseHover(event);
			} else if (eventType.equals(PartConstants.s_eventMouseMove)) {
				r = onMouseMove(event);
			} else if (eventType.equals(PartConstants.s_eventMouseEnter)) {
				r = onMouseEnter(event);
			} else if (eventType.equals(PartConstants.s_eventMouseExit)) {
				r = onMouseExit(event);
			} else if (eventType.equals(PartConstants.s_eventMouseDown)) {
				r = onMouseDown(event);
			} else if (eventType.equals(PartConstants.s_eventMouseUp)) {
				r = onMouseUp(event);
			} else if (eventType.equals(PartConstants.s_eventMouseDoubleClick)) {
				r = onMouseDoubleClick(event);
			}
		}
		return r;
	}
	protected void prepareContextMenu(MouseEvent e) {
	}

	protected boolean onMouseUp(MouseEvent e) {
		if (e.button == 3 
			|| (e.button == 1 && ((e.stateMask & SWT.CONTROL) != 0))){
			if (handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, e.x, e.y))) {
				Control control = (Control) m_context.getSWTControl();
				Point	point = control.toDisplay(new Point(e.x, e.y));
				
				PartUtilities.showContextMenu(m_source, m_context, point);
				
				return true;
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
		return false;
	}

	protected boolean onMouseExit(MouseEvent e) {
		return false;
	}
	
	protected boolean onFocusEvent(Resource eventType, FocusEvent event) {
		boolean r = false;
		if (eventType.equals(PartConstants.s_eventGotInputFocus)) {
			r = onGotInputFocus(event);
		} else if (eventType.equals(PartConstants.s_eventLostInputFocus)) {
			r = onLostInputFocus(event);
		}
		return r;
	}

	protected boolean onGotInputFocus(FocusEvent e) {
		return m_parentChildFocusHandler.letChildHandleGotFocusEvent(e);
	}

	protected boolean onLostInputFocus(FocusEvent e) {
		return m_parentChildFocusHandler.letChildHandleLostFocusEvent(e);
	}

	protected boolean onKeyEvent(Resource eventType, KeyEvent event) {
		boolean r = false;
		if (eventType.equals(PartConstants.s_eventKeyPressed)) {
			r = onKeyPressed(event);
		} else if (eventType.equals(PartConstants.s_eventKeyReleased)) {
			r = onKeyReleased(event);
		}
		return r;
	}
	
	protected boolean onKeyPressed(KeyEvent e) {
		return false;
	}
	
	protected boolean onKeyReleased(KeyEvent e) {
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
		Point 		p = (Point) e.data;
		IVisualPart	vp = hittest(p.x, p.y, false);
		
		if (vp != null) {
			return vp.handleEvent(PartConstants.s_eventDrag, e);
		}
		return false;
	}
	
	protected boolean onDragEnd(DragSourceEvent e) {
		return true;
	}

	protected boolean onDragSetData(DragSourceEvent e) {
		System.out.println(">> layoutmanagerbase.ondragsetdata");
		return PartUtilities.setDragData(e, m_context);
	}
	
	protected boolean onDropTargetEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		boolean r = false;
		
		r = m_parentChildDropHandler.letChildrenHandleEvent(eventType, event) || r;
		if (!r) {
			if (eventType.equals(PartConstants.s_eventDragHover)) {
				r = onDragHover(event);
			} else if (eventType.equals(PartConstants.s_eventDragEnter)) {
				r = onDragEnter(event);
			} else if (eventType.equals(PartConstants.s_eventDragExit)) {
				r = onDragExit(event);
			} else if (eventType.equals(PartConstants.s_eventDragOperationChanged)) {
				r = onDragOperationChanged(event);
			} else if (eventType.equals(PartConstants.s_eventDropAccept)) {
				r = onDropAccept(event);
			} else if (eventType.equals(PartConstants.s_eventDrop)) {
				r = onDrop(event);
			} else if (eventType.equals(PartConstants.s_eventFakeDrop)) {
				r = onFakeDrop(event);
			}
		} else {
			onChildHasHandledDragAndDropEvent(eventType, event);
		}
		return r;
	}
	
	protected void onChildHasHandledDragAndDropEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
	}
	
	boolean m_defaultDropTargetResult = true;
	int m_defaultDropOperation = DND.DROP_LINK;
	
	protected boolean onDragEnter(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_defaultDropOperation;
		return m_defaultDropTargetResult;
	}
	protected void onChildDragEnter(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
	}
	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_defaultDropOperation;
		return m_defaultDropTargetResult;
	}
	protected void onChildDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
	}
	protected boolean onDragHover(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_defaultDropOperation;
		return m_defaultDropTargetResult;
	}
	protected boolean onDragOperationChanged(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_defaultDropOperation;
		return m_defaultDropTargetResult;
	}
	protected boolean onDropAccept(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_defaultDropTargetResult && !PossibleResourceListTransfer.getInstance().isSupportedType(event.m_dropTargetEvent.currentDataType) && !MultipleResourceListTransfer.getInstance().isSupportedType(event.m_dropTargetEvent.currentDataType)) {
			TransferData[]	dataTypes = event.m_dropTargetEvent.dataTypes;
			TransferData	dataType = null;
			
			for (int i = 0; i < dataTypes.length; i++) {
				if (dataType == null && FileTransfer.getInstance().isSupportedType(dataTypes[i])) {
					dataType = dataTypes[i];
				} else if (PossibleResourceListTransfer.getInstance().isSupportedType(dataTypes[i])) {
					dataType = dataTypes[i];
					break;
				} else if (MultipleResourceListTransfer.getInstance().isSupportedType(dataTypes[i])) {
					dataType = dataTypes[i];
					break;
				}
			}
			
			if (dataType != null) {
				event.m_dropTargetEvent.currentDataType = dataType;
			}
		}
		event.m_dropTargetEvent.detail = m_defaultDropOperation;
		return m_defaultDropTargetResult;
	}
	
	protected boolean onDrop(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_defaultDropTargetResult) {
			if (PossibleResourceListTransfer.getInstance().isSupportedType(event.m_dropTargetEvent.currentDataType)) {
				return onDropResourceList((java.util.List) event.m_dropTargetEvent.data, event);
			} else if (MultipleResourceListTransfer.getInstance().isSupportedType(event.m_dropTargetEvent.currentDataType)) {
				return onDropResources((java.util.List) event.m_dropTargetEvent.data, event);
			} else if (FileTransfer.getInstance().isSupportedType(event.m_dropTargetEvent.currentDataType)) {
				return onDropFiles((String[]) event.m_dropTargetEvent.data, event);
			} else if (TextTransfer.getInstance().isSupportedType(event.m_dropTargetEvent.currentDataType)) {
				return onDropString((String) event.m_dropTargetEvent.data, event);
			}
		}
		return m_defaultDropTargetResult;
	}

	protected boolean onDropFiles(String[] filePaths, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		java.util.List	resources = new ArrayList();
		
		for (int i = 0; i < filePaths.length; i++) {
			Resource resource = Utilities.pathToURI(filePaths[i], m_source);
			
			if (resource.getContent().startsWith("http://")) {
				try {
					m_infoSource.add(new Statement(resource, Constants.s_rdf_type, Constants.s_web_WebPage));
				} catch (RDFException e) {
				}
			}
			resources.add(resource);
		}
		return onDropResources(resources, event);
	}

	protected boolean onDropString(String string, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		s_logger.info("Dropped " + string);
		return true;
	}
	
	protected boolean onDropResourceList(java.util.List resourceList, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		try {
			m_dataProvider.requestChange(DataConstants.SET_ADDITION, resourceList.get(0));
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch on drop", e);
		}
		return false;
	}

	protected boolean onDropResources(java.util.List resources, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		try {
			m_dataProvider.requestChange(DataConstants.SET_ADDITION, resources);
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch on drop", e);
		}
		return false;
	}
	
	protected java.util.List getFocusableChildParts() {
		return m_visualChildParts;
	}

	abstract protected IVisualPart hittest(int x, int y, boolean favorParent);

	protected void processChildResizeEvents() {
		if (m_childrenToResize.size() == 0) {
			return;
		}
		
		/* Lets derived class determines whether or not to pass
		 * child resize event up to parent or force a redraw.
		 */
		if (passChildResizeToParent()) {
			ChildPartEvent event = new ChildPartEvent(this);
			m_layoutPart.handleEvent(PartConstants.s_eventChildResize, event);
		} else {
			redraw();
		}
		m_childrenToResize.clear();

		if (m_childResizeRunnable != null) {
			m_childResizeRunnable.expire();
			m_childResizeRunnable = null;
		}
	}
	protected boolean passChildResizeToParent() {
		return true;
	}
	protected void redraw() {
	}


	static final Resource CONTEXT_OPERATIONS = new Resource("http://haystack.lcs.mit.edu/ui/contextMenu#contextOperation");
	protected void removeContextOperations() {
		try {
			m_source.remove(
				new Statement(m_prescription, CONTEXT_OPERATIONS, Utilities.generateWildcardResource(1)),
				Utilities.generateWildcardResourceArray(1));
		} catch (RDFException e) {
		}
	}

	static final Resource CREATE_REMOVE_OPERATION = new Resource("http://haystack.lcs.mit.edu/ui/collectionView#createRemoveOperation");
	protected void createRemoveContextOperation(Resource element, int index) {
		Interpreter 		i = Ozone.getInterpreter();
		DynamicEnvironment 	denv = new DynamicEnvironment(m_source);
		Resource			dataSource = (Resource) m_context.getProperty(OzoneConstants.s_dataSource);
		
		Ozone.initializeDynamicEnvironment(denv, m_context);
		
		try {
			i.callMethod(CREATE_REMOVE_OPERATION, new Message(new Object[] { m_prescription, m_context.getLocalProperty(OzoneConstants.s_layoutInstance), element, new Integer(index) }), denv);
		} catch (AdenineException e) {
			s_logger.error("Error invoking " + CREATE_REMOVE_OPERATION, e);
		}
	}

	protected void debugPrint(Object o) {
		if (m_id != null && m_id.equals(OzoneConstants.s_debug)) {
			System.err.println("----" + this + ": " + o);
		}
	}
	
	protected boolean isDebug() {
		return (m_id != null && m_id.equals(OzoneConstants.s_debug));
	}
	
	protected boolean onFakeDrop(OzoneDropTargetEvent event) {
		System.out.println(">> fake dropping " + event.m_data);
		return onDropResourceList((java.util.List) event.m_data, null);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		super.initializeFromDeserialization(source);

		Iterator i = m_visualChildParts.iterator();
		while (i.hasNext()) {
			IPart part = (IPart) i.next();
			part.initializeFromDeserialization(source);
		}

		i = m_otherChildParts.iterator();
		while (i.hasNext()) {
			IPart part = (IPart) i.next();
			part.initializeFromDeserialization(source);
		}
	}
}
