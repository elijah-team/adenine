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

package edu.mit.lcs.haystack.ozone.core.utils;

import java.util.EventObject;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.MultipleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent;
import edu.mit.lcs.haystack.ozone.core.PossibleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 */
abstract public class DragAndDropHandler {
	public void initialize(Control control) {
		Transfer[]		types = new Transfer[] { MultipleResourceListTransfer.getInstance(), PossibleResourceListTransfer.getInstance(), FileTransfer.getInstance(), TextTransfer.getInstance() };
		int			operations = DND.DROP_LINK | DND.DROP_COPY | DND.DROP_DEFAULT;
		
		DragSource source = new DragSource (control, operations);
		source.setTransfer(types);
		source.addDragListener (new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
				event.data = getMouseDownPoint();
				
				if (isDraggable() && event.data != null && handleEvent(PartConstants.s_eventDrag, event)) {
					IPart part = (IPart) event.data;

					Ozone.s_context.putGlobalProperty(OzoneConstants.s_draggedPart, part);
				} else {
					event.doit = false;
				}
			}
			public void dragSetData (DragSourceEvent event) {
				if (isDraggable()) {
					IPart part = (IPart) Ozone.s_context.getProperty(OzoneConstants.s_draggedPart);
					if (part != null) {
						part.handleEvent(PartConstants.s_eventDragSetData, event);
					}
				}
			}
			public void dragFinished(DragSourceEvent event) {
				if (isDraggable()) {
					IPart part = (IPart) Ozone.s_context.getProperty(OzoneConstants.s_draggedPart);
					if (part != null) {
						part.handleEvent(PartConstants.s_eventDragEnd, event);
					}
					
					handleEvent(PartConstants.s_eventMouseExit, new MouseEvent(new Event()));
				}
			}
		});
		
		DropTarget target = new DropTarget(control, operations);
		target.setTransfer(types);
		target.addDropListener (new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
				if (isDroppable()) {
					handleDropEvent(PartConstants.s_eventDragEnter, new OzoneDropTargetEvent(event));
				}
			}
			public void dragOver(DropTargetEvent event) {
				if (isDroppable()) {
					handleDropEvent(PartConstants.s_eventDragHover, new OzoneDropTargetEvent(event));
				}
			}
			public void dragLeave(DropTargetEvent event) {
				if (isDroppable()) {
					handleDropEvent(PartConstants.s_eventDragExit, new OzoneDropTargetEvent(event));
				}
			}
			public void dragOperationChanged(DropTargetEvent event) {
				if (isDroppable()) {
					handleDropEvent(PartConstants.s_eventDragOperationChanged, new OzoneDropTargetEvent(event));
				}
			}
			public void dropAccept(DropTargetEvent event) {
				if (isDroppable()) {
					handleDropEvent(PartConstants.s_eventDropAccept, new OzoneDropTargetEvent(event));
				}
			}
			public void drop(DropTargetEvent event) {
				if (isDroppable()) {
					handleDropEvent(PartConstants.s_eventDrop, new OzoneDropTargetEvent(event));
				}
			}
	 	});
	}
	
	protected Point getMouseDownPoint() {
		return null;
	}
	
	protected boolean isDraggable() {
		return false;
	}
	
	protected boolean isDroppable() {
		return false;
	}
	
	protected boolean handleEvent(Resource eventType, EventObject event) {
		return false;
	}
	
	protected boolean handleDropEvent(Resource eventType, OzoneDropTargetEvent event) {
		return false;
	}
}
