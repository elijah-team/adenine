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
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.eclipse.Plugin;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;


import java.util.*;

/**
 * @author David Huynh
 */
public class PartUtilities {
	final static Resource SHOW_CONTEXT_MENU	= new Resource("http://haystack.lcs.mit.edu/ui/contextMenu#showContextMenu");
	final static Resource SHOW_SUMMARY		= new Resource("http://haystack.lcs.mit.edu/ui/contextMenu#showSummary");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PartUtilities.class);
	
	public static void showContextMenu(IRDFContainer source, Context context, Point displayPt) {
		showContextMenu(source, context, displayPt, -1);
	}

	public static void showContextMenu(IRDFContainer source, Context context, Point displayPt, int maxCount) {
		Interpreter i = Ozone.getInterpreter();
		DynamicEnvironment denv = new DynamicEnvironment(source);
		Ozone.initializeDynamicEnvironment(denv, context);
		context.setViewContainerFactory(new IViewContainerFactory() {
			public IViewContainerPart createViewContainer() {
				return new ViewContainerPart();
			}
		});
		
		ArrayList params = new ArrayList();
		params.add(displayPt);
		params.add(new Integer(maxCount));
		params.add(new Boolean(Plugin.getHaystack().isDebug()));
		
		Resource resResult;
		try {
			i.callMethod(SHOW_CONTEXT_MENU, params.toArray(), denv);
		} catch (Exception ex) {
			s_logger.error("Error calling method showContextMenu", ex);
		}
	}
	
	public static void showSummary(IRDFContainer source, Context context, Point displayPt) {
		Interpreter i = Ozone.getInterpreter();
		DynamicEnvironment denv = new DynamicEnvironment(source);
		Ozone.initializeDynamicEnvironment(denv, context);
		context.setViewContainerFactory(new IViewContainerFactory() {
			public IViewContainerPart createViewContainer() {
				return new ViewContainerPart();
			}
		});
		
		ArrayList params = new ArrayList();
		params.add(displayPt);
		
		Resource resResult;
		try {
			i.callMethod(SHOW_SUMMARY, params.toArray(), denv);
		} catch (Exception ex) {
			s_logger.error("Error calling method showSummary", ex);
		}
	}
	
	public static void repaint(Rectangle bounds, Context context) {
		Control control = (Control) context.getSWTControl();
		control.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
	}
		
	public static boolean setDragData(DragSourceEvent event, Context context) {
		if (PossibleResourceListTransfer.getInstance().isSupportedType(event.dataType)) {
			java.util.List underlyings = getUnderlyingsList(context);
			
			if (underlyings == null) {
				return false;
			} else {
				event.data = underlyings;
				return true;
			}
		} else if (MultipleResourceListTransfer.getInstance().isSupportedType(event.dataType)) {
			java.util.List underlyings = getUnderlyingsList(context);
			
			if (underlyings != null && underlyings.size() > 0) {
				java.util.List data = new ArrayList();
				data.add(underlyings.get(0));
				event.data = data;
				return true;
			}
		} else if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
			ArrayList	resources = new ArrayList();
			Context		current = context;
				
			while (current != null) {
				Resource underlying = (Resource) current.getLocalProperty(OzoneConstants.s_underlying);
				
				if (underlying != null) {
					resources.add(underlying.getContent());
				}
				
				current = current.getParentContext();
			}
			
			if (resources.size() == 0) {
				return false;
			} else {
				event.data = resources.toArray();
				return true;
			}
		} else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
			Resource	resource = null;
			Context		current = context;
				
			while (current != null) {
				resource = (Resource) current.getLocalProperty(OzoneConstants.s_underlying);
				if (resource != null) {
					break;
				}
				current = current.getParentContext();
			}
			
			if (resource == null) {
				return false;
			} else {
				event.data = resource.getContent();
				return true;
			}
		}
		return false;
	}
	
	static public void setContainerControlDraggable(boolean draggable, Context context) {
		Control control = (Control) context.getSWTControl();
		
		try {
			((VisualPartAwareComposite) control).setDraggable(draggable);
		} catch (Exception e) {
		}
	}

	static public boolean showContextMenu(StyledText styledText, MouseEvent me, IRDFContainer source, Context context) {
		try {
			int 	offset = styledText.getOffsetAtLocation(new Point(me.x, me.y));
			String	text = styledText.getText();
			char[]	chars = text.toCharArray();
			
			if (offset >= 0 && offset < chars.length && Character.isLetter(chars[offset])) {
				int start = offset;
				int end = offset + 1;
				
				while (start > 0 && Character.isLetter(chars[start - 1])) {
					start--;
				}
				while (end < chars.length && Character.isLetter(chars[end])) {
					end++;
				}
				
				Resource word = Utilities.generateUniqueResource();
				
				Context childContext = new Context(context);
				
				childContext.putLocalProperty(OzoneConstants.s_underlying, word);
				
				PartUtilities.showContextMenu(source, childContext, styledText.toDisplay(new Point(me.x, me.y)));
				
				return true;
			}
		} catch (Exception e) {
			s_logger.error("Failed to show context menu", e);
		}
		
		return false;
	}
	
	final static Resource s_name		= new Resource(OzoneConstants.s_namespace + "name");
	final static Resource s_value		= new Resource(OzoneConstants.s_namespace + "value");
	final static Resource s_nullValue	= new Resource(OzoneConstants.s_namespace + "nullValue");
	
	static public void registerServices(IRDFContainer source, Context context, Resource partData) {
/*		Resource resRegisterServices = Utilities.getResourceProperty(partData, OzoneConstants.s_registerServices, source);
		if (resRegisterServices != null) {
			Resource resNames = Utilities.getResourceProperty(resRegisterServices, s_names, source);
			Resource resValues = Utilities.getResourceProperty(resRegisterServices, s_values, source);
			Iterator iName = ListUtilities.accessDAMLList(resNames, source);
			Iterator iValue = ListUtilities.accessDAMLList(resValues, source);
			
			while (iName.hasNext() && iValue.hasNext()) {
				Resource	resName = (Resource) iName.next();
				RDFNode		resValue = (RDFNode) iValue.next();
				
				context.putProperty(resName, resValue);
			}
		}
*/
		Resource[] properties = Utilities.getResourceProperties(partData, OzoneConstants.s_putProperty, source);
		for (int i = 0; i < properties.length; i++) {
			Resource name = Utilities.getResourceProperty(properties[i], s_name, source);
			RDFNode value = Utilities.getProperty(properties[i], s_value, source);
			
			if (name != null) {
				if (value != null) {
					context.putProperty(name, value);
				} else if (Utilities.checkBooleanProperty(properties[i], s_nullValue, source)) {
					context.putProperty(name, null);
				}
			}
		}

		properties = Utilities.getResourceProperties(partData, OzoneConstants.s_putLocalProperty, source);
		for (int i = 0; i < properties.length; i++) {
			Resource name = Utilities.getResourceProperty(properties[i], s_name, source);
			RDFNode value = Utilities.getProperty(properties[i], s_value, source);
			
			if (name != null && value != null) {
				context.putLocalProperty(name, value);
			}
		}
	}
	
	static public void registerViewPartClass(IRDFContainer source, Context context, Resource partData) {
		Resource resViewPartClass = Utilities.getResourceProperty(partData, OzoneConstants.s_viewPartClass, source);
		if (resViewPartClass != null) {
			context.putProperty(OzoneConstants.s_viewPartClass, resViewPartClass);
		}
	}

	final static public Resource s_toolbarItemSetSource = new Resource("http://haystack.lcs.mit.edu/ui/viewContainer#toolbarItemSetSource");
	final static public Resource s_registerToolbar = new Resource(OzoneConstants.s_namespace + "registerToolbar");
	static public void registerToolbarItems(IRDFContainer source, Context context, Resource partData) {
		Resource 	dataSource = (Resource) context.getProperty(s_toolbarItemSetSource);
		Resource	registerToolbar = Utilities.getResourceProperty(partData, s_registerToolbar, source);
		if (dataSource != null && registerToolbar != null) {
			IDataProvider 	dataProvider = DataUtilities.createDataProvider(dataSource, context, source);
			Iterator		i = ListUtilities.accessDAMLList(registerToolbar, source);
			
			while (i.hasNext()) {
				Resource 	item = (Resource) i.next();
				boolean	done = false;
				
				if (!done) {
					try {
						dataProvider.requestChange(DataConstants.SET_ADDITION, item);
						done = true;
					} catch (DataMismatchException e) {
					}
				}
			}
		}
	}

	static public void unregisterToolbarItems(IRDFContainer source, Context context, Resource partData) {
		Resource 	dataSource = (Resource) context.getProperty(s_toolbarItemSetSource);
		Resource	registerToolbar = Utilities.getResourceProperty(partData, s_registerToolbar, source);
		if (dataSource != null && registerToolbar != null) {
			IDataProvider 	dataProvider = DataUtilities.createDataProvider(dataSource, context, source);
			Iterator		i = ListUtilities.accessDAMLList(registerToolbar, source);
			
			while (i.hasNext()) {
				Resource 	item = (Resource) i.next();
				boolean	done = false;
				
				if (!done) {
					try {
						dataProvider.requestChange(DataConstants.SET_REMOVAL, item);
						done = true;
					} catch (DataMismatchException e) {
					}
				}
			}
		}
	}
	
	static final Resource s_queryDroppable = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#queryDroppable");
	static final Resource s_queryMultiDroppable = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#queryMultiDroppable");
	static final Resource s_performDrop = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#performDrop");

	static final Resource s_operation = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#operation");
	static final Resource s_sources = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#sources");
	static final Resource s_target = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#target");
	static final Resource s_dragParam = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#dragParam");
	static final Resource s_dropParam = new Resource("http://haystack.lcs.mit.edu/schemata/dnd#dropParam");

	static public Object s_draggedItem = null;
	
	static public boolean filterFakeDragEvents(Resource eventType, MouseEvent event, Context context, IRDFContainer source, IPart part) {
		if (eventType.equals(PartConstants.s_eventMouseUp) && (event.button == 2)) {
			if (((event.stateMask & SWT.ALT) != 0)) {
				java.util.List underlyings = PartUtilities.getUnderlyingsList(context);
				if (underlyings != null && underlyings.size() == 0) {
					underlyings = null;
				}
				s_draggedItem = underlyings;
				System.out.println(">> accepted " + s_draggedItem);
				return true;
			}
		}
		
		return false;
	}

	static public boolean filterFakeDropEvents(Resource eventType, MouseEvent event, Context context, IRDFContainer source, IPart part) {
		if (eventType.equals(PartConstants.s_eventMouseUp) && (event.button == 2)) {
			if (((event.stateMask & SWT.ALT) == 0) && (s_draggedItem != null)) {
				System.out.println(">> dropping " + s_draggedItem + " onto " + part);
				part.handleEvent(PartConstants.s_eventFakeDrop, new OzoneDropTargetEvent(s_draggedItem, event.x, event.y));
				return true;
			}
		}
		
		return false;
	}

	static boolean performDrop(Object data, Context dropContext, IRDFContainer source) {
		java.util.List		targetsList = getUnderlyingsList(dropContext);
		Interpreter 		i = Ozone.getInterpreter();
		DynamicEnvironment 	denv = new DynamicEnvironment(source);
		Message				result = null;
		
		Ozone.initializeDynamicEnvironment(denv, dropContext);
		
		try {
			result = i.callMethod(
				s_queryDroppable, 
				new Message(new Object[] { data, targetsList }), 
				denv
			);
		} catch (Exception e) {
			s_logger.error("Error calling method queryDroppable", e);
		}
		
		DNDOperationInfo	dndOperationInfo; 
		if (result != null && result.getNamedValues() != null && result.getNamedValues().size() > 1) {
			dndOperationInfo = new DNDOperationInfo(
				(Resource) result.getNamedValue(s_operation),
				(java.util.List) result.getNamedValue(s_sources),
				(Resource) result.getNamedValue(s_target),
				(Resource) result.getNamedValue(s_dragParam),
				(Resource) result.getNamedValue(s_dropParam)
			);
		} else {
			return false;
		}

		if (dndOperationInfo != null) {
				denv = new DynamicEnvironment(source);
	
			Ozone.initializeDynamicEnvironment(denv, dropContext);
			
			try {
				i.callMethod(
					s_performDrop, 
					new Object[] {
						dndOperationInfo.m_operation, 
						dndOperationInfo.m_sources, 
						dndOperationInfo.m_target,
						dndOperationInfo.m_dragParam,
						dndOperationInfo.m_dropParam 
					}, 
					denv
				);
				return true;
			} catch (Exception e) {
				s_logger.error("Error calling method performDrop", e);
			}
		}		
		return false;
	}
	
	static DNDOperationInfo queryDroppable(OzoneDropTargetEvent dte, Context dropContext, IRDFContainer source) {
		if (dte.m_dropTargetEvent.data == null) {
			return null;
		}
		
		java.util.List		targetsList = getUnderlyingsList(dropContext);
		Interpreter 		i = Ozone.getInterpreter();
		DynamicEnvironment 	denv = new DynamicEnvironment(source);
		Message				result = null;
		
		Ozone.initializeDynamicEnvironment(denv, dropContext);
		
		if (PossibleResourceListTransfer.getInstance().isSupportedType(dte.m_dropTargetEvent.currentDataType)) {
			try {
				result = i.callMethod(
					s_queryDroppable, 
					new Message(new Object[] { dte.m_dropTargetEvent.data, targetsList }), 
					denv
				);
			} catch (Exception e) {
				s_logger.error("Error calling method queryDroppable", e);
			}
		} else if (MultipleResourceListTransfer.getInstance().isSupportedType(dte.m_dropTargetEvent.currentDataType)) {
			try {
				result = i.callMethod(
					s_queryDroppable, 
					new Message(new Object[] { dte.m_dropTargetEvent.data, targetsList }), 
					denv
				);
			} catch (Exception e) {
				s_logger.error("Error calling method queryDroppable", e);
			}
		} else if (FileTransfer.getInstance().isSupportedType(dte.m_dropTargetEvent.currentDataType)) {
			try {
				result = i.callMethod(
					s_queryMultiDroppable, 
					new Message(new Object[] { dte.m_dropTargetEvent.data, targetsList }), 
					denv
				);
			} catch (Exception e) {
				s_logger.error("Error calling method queryMultiDroppable", e);
			}
		} else if (TextTransfer.getInstance().isSupportedType(dte.m_dropTargetEvent.currentDataType)) {
			java.util.List sources = new LinkedList();
			
			sources.add(new Resource((String) dte.m_dropTargetEvent.data));
			
			try {
				result = i.callMethod(
					s_queryDroppable, 
					new Message(new Object[] { sources, targetsList }), 
					denv
				);
			} catch (Exception e) {
				s_logger.error("Error calling method queryDroppable", e);
			}
		}
		
		if (result != null && result.getNamedValues().size() > 1) {
			return new DNDOperationInfo(
				(Resource) result.getNamedValue(s_operation),
				(java.util.List) result.getNamedValue(s_sources),
				(Resource) result.getNamedValue(s_target),
				(Resource) result.getNamedValue(s_dragParam),
				(Resource) result.getNamedValue(s_dropParam)
			);
		}
		return null;
	}
	
	static public boolean performDrop(OzoneDropTargetEvent dte, IRDFContainer source, Context dropContext) {
		DNDOperationInfo	dndOperationInfo = queryDroppable(dte, dropContext, source);
		
		if (dndOperationInfo != null) {
			Interpreter 		i = Ozone.getInterpreter();
			DynamicEnvironment 	denv = new DynamicEnvironment(source);
	
			Ozone.initializeDynamicEnvironment(denv, dropContext);
			
			try {
				i.callMethod(
					s_performDrop, 
					new Object[] {
						dndOperationInfo.m_operation, 
						dndOperationInfo.m_sources, 
						dndOperationInfo.m_target,
						dndOperationInfo.m_dragParam,
						dndOperationInfo.m_dropParam 
					}, 
					denv
				);
				return true;
			} catch (Exception e) {
				s_logger.error("Error calling method performDrop", e);
			}
		}		
		return false;
	}
	
	static java.util.List getUnderlyingsList(Context context) {
		java.util.List	resources = new LinkedList();
		Resource		resource = null;
			
		while (context != null) {
			resource = (Resource) context.getLocalProperty(OzoneConstants.s_underlying);
			
			if (resource != null) {
				resources.add(resource);
			}
			
			context = context.getParentContext();
		}
		
		return resources;
	}
}

class DNDOperationInfo {
	public Resource			m_operation;
	public java.util.List	m_sources;
	public Resource			m_target;
	public Resource			m_dragParam;
	public Resource			m_dropParam;
	
	public DNDOperationInfo(Resource operation, java.util.List sources, Resource target, Resource dragParam, Resource dropParam) {
		m_operation = operation;
		m_sources = sources;
		m_target = target;
		m_dragParam = dragParam;
		m_dropParam = dropParam;
	}
}
