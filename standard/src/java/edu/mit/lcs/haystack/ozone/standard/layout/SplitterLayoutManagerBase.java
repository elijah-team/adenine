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

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.ILayoutManager;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;

import java.io.Serializable;
import java.util.*;

/**
 * @author David Huynh
 * 
 * If an element is not resizable, then it takes as much space as it requires unless
 * it is collapsed or it is cropped.
 * 
 * If an element is resizable, then it takes as much space as it initially requires,
 * or as much space it is initially constrained to, or as much space as it is resized
 * to.
 */
abstract public class SplitterLayoutManagerBase extends ListLayoutManagerBase implements IBlockGUIHandler {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SplitterLayoutManagerBase.class);

	class Constraint implements Serializable {
		public Resource		m_spec;
		
		public int			m_elementIndex;		// index identifying targeted element
		public Resource		m_elementResource;	// or resource identifying targeted element
		
		public boolean		m_resizable;		// default: true
		public boolean		m_persistent;		// default: true
		
		public int			m_pixels;			// size in pixels
		public float		m_ratio;			// or size in ratio of parent
	}
	
	class Element implements Serializable {
		public Resource			m_childPartData;
		public Constraint		m_constraint;
		public boolean			m_initializedSize = false;
		
		public Rectangle		m_bounds = new Rectangle(0, 0, 0, 0);
		public boolean			m_collapsed = false;

		public BlockScreenspace	m_blockScreenspace;
		public boolean			m_needsRecalculation = true;
		
		public Element(Resource childPartData) {
			m_childPartData = childPartData;
		}
	}
	
	protected Hashtable			m_constraintsByIndex = new Hashtable();
	protected Hashtable			m_constraintsByResource = new Hashtable();
	
	protected List				m_elements = new ArrayList();
	protected Rectangle			m_bounds = new Rectangle(0, 0, 0, 0); // bounds of elements

	protected BlockScreenspace	m_calculatedBS = new BlockScreenspace(0, 0); // total space desired
	
	protected boolean				m_packLast = false;
	protected int					m_fillElement; // element that takes up the rest of the space
	
	transient protected Control				m_parent;
	transient protected Cursor				m_resizeCursor;
	protected Point				m_resizeInitialPoint;
	protected int					m_resizeElement;
	
	static final protected int	s_resizeHotspotWidth = 5;
	
	/**
	 * @see LayoutManagerBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		String s;
				
		s = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_pack, m_partDataSource);
		if (s != null && s.equalsIgnoreCase("last")) {
			m_packLast = true;
		}

		loadConstraints();		
		
		m_parent = (Control) m_context.getSWTControl();
		
		makeDataConsumers();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.ListLayoutManagerBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		m_parent = (Control) m_context.getSWTControl();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		saveConstraints();
		
		m_constraintsByIndex.clear();
		m_constraintsByResource.clear();
		m_elements.clear();

		m_constraintsByIndex = null;
		m_constraintsByResource = null;
		m_elements = null;
		m_bounds = null;
		m_calculatedBS = null;
		
		m_parent = null;
		m_resizeCursor = null;
		m_resizeInitialPoint = null;

		super.dispose();
	}
	
	protected void loadConstraints() {
		String		s;
		Resource[]	constraints = Utilities.getResourceProperties(m_prescription, LayoutConstants.s_constraint, m_source);
		
		for (int i = 0; i < constraints.length; i++) {
			Constraint	constraint = new Constraint();
			boolean		byIndex = false;
			
			constraint.m_spec = constraints[i];

			try {
				RDFNode node = m_partDataSource.extract(constraints[i], LayoutConstants.s_element, null);
				if (node instanceof Literal) {
					constraint.m_elementIndex = Integer.parseInt(node.getContent());
					byIndex = true;
				} else if (node instanceof Resource) {
					constraint.m_elementResource = (Resource) node;
				} else {
					continue;
				}
			} catch (RDFException e) {
				continue;
			}
			
			constraint.m_resizable = Utilities.checkBooleanProperty(constraints[i], LayoutConstants.s_resizable, m_partDataSource, true);
			constraint.m_persistent = Utilities.checkBooleanProperty(constraints[i], LayoutConstants.s_persistent, m_partDataSource, true);
			
			s = Utilities.getLiteralProperty(constraints[i], LayoutConstants.s_dimension, m_partDataSource);
			if (s == null) {
				constraint.m_pixels = -1;
				constraint.m_ratio = -1;
			} else if (s.endsWith("%")) {
				constraint.m_ratio = Float.parseFloat(s.substring(0, s.length() - 1)) / 100;
				constraint.m_pixels = -1;
			} else {
				constraint.m_pixels = Math.max(0, Integer.parseInt(s));
				constraint.m_ratio = -1;
			}

			if (byIndex) {
				m_constraintsByIndex.put(new Integer(constraint.m_elementIndex), constraint);
			} else {
				m_constraintsByResource.put(constraint.m_elementResource, constraint);
			}
		}
	}
	
	protected void saveConstraints() {
		Iterator i = m_constraintsByIndex.values().iterator();
		while (i.hasNext()) {
			Constraint constraint = (Constraint) i.next();
			
			if (constraint.m_persistent) {
				try {
					m_partDataSource.replace(constraint.m_spec, LayoutConstants.s_dimension, null,
						new Literal(Integer.toString(constraint.m_pixels)));
				} catch (RDFException e) {
				}
			}
		}

		i = m_constraintsByResource.values().iterator();
		while (i.hasNext()) {
			Constraint constraint = (Constraint) i.next();
			
			if (constraint.m_persistent) {
				try {
					m_partDataSource.replace(constraint.m_spec, LayoutConstants.s_dimension, null,
						new Literal(Float.toString(constraint.m_ratio * 100) + "%"));
				} catch (RDFException e) {
				}
			}
		}
	}

	/**
	 * @see ILayoutManager#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		}
		return null;
	}

	/**
	 * @see IGUIHandler@setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		internalSetVisible(visible);
	}

	protected boolean m_resizing = false;
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (m_resizing) {
			return true;
		}
		
		if (eventType.equals(PartConstants.s_eventMouseMove) ||
			eventType.equals(PartConstants.s_eventMouseHover)) {
			int previousResizeElement = m_resizeElement;
			
			m_resizeElement = hittestResizeElement(event.x, event.y);
			if (m_resizeElement >= 0) {
				m_parent.setCursor(m_resizeCursor);
				return true;
			} else if (previousResizeElement >= 0) {
				m_parent.setCursor(null);
			}
		} else if (eventType.equals(PartConstants.s_eventMouseDown)) {
			m_resizeElement = hittestResizeElement(event.x, event.y);
			if (m_resizeElement >= 0) {
				m_parent.setCursor(m_resizeCursor);
				onResizeMouseDown(event);
				m_resizing = true;
				return true;
			}
		} else if (eventType.equals(PartConstants.s_eventMouseExit)) {
			m_parent.setCursor(null);
		}

		return super.onMouseEvent(eventType, event);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {

		Region	region = new Region();
		boolean	redrawAll = false;
		
		gc.getClipping(region);
		
		if (!r.equals(m_bounds)) {
			setBounds(r);
			redrawAll = true;
			gc.setClipping(r);
		}
		
		for (int i = 0; i < m_visualChildParts.size(); i++) {
			IVisualPart 		vp = (IVisualPart) m_visualChildParts.get(i);
			Element				element = (Element) m_elements.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (blockGUIHandler != null) {
				if (redrawAll || region.intersects(element.m_bounds)) {
					blockGUIHandler.draw(gc, element.m_bounds);
				}
			}
		}
		
		if (redrawAll) {
			gc.setClipping(region);
		}
		
		region.dispose();
		region = null;
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public abstract void renderHTML(HTMLengine he);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/**
	 * @see LayoutManagerBase#passChildResizeToParent(ChildPartEvent)
	 */
	protected boolean passChildResizeToParent() {
		Iterator iChildPart = m_childrenToResize.iterator();
		
		while (iChildPart.hasNext()) {
			IPart	childPart = (IPart) iChildPart.next();
			int		i = m_visualChildParts.indexOf(childPart);
			
			if (i >= 0) {
				Element element = (Element) m_elements.get(i);
				
				if (!element.m_initializedSize || !elementResizable(element)) {
					element.m_needsRecalculation = true;
				}
			}
		}
		
		redistributeElements();
		return (m_calculatedBS.m_size.x != m_bounds.width || m_calculatedBS.m_size.y != m_bounds.height);
	}
	
	/**
	 * @see LayoutManagerBase#redraw()
	 */
	protected void redraw() {
		Region region = new Region();
		
		internalSetBounds(m_bounds, region);
		
		Rectangle r = region.getBounds();
		
		m_parent.redraw(r.x, r.y, r.width, r.height, true);
		
		region.dispose();
		region = null;
		r = null;
	}
	abstract protected void internalSetBounds(Rectangle r, Region region);

	/**
	 * @see LayoutManagerBase#hittest(int, int, boolean)
	 */
	int m_previousHittestElementIndex;
	
	protected IVisualPart hittest(int x, int y, boolean favorParent) {
		if (m_elements == null) {
			return null;
		}

		try {
			Element element = (Element) m_elements.get(m_previousHittestElementIndex);
			if (element != null && element.m_bounds.contains(x, y)) {
				return (IVisualPart) m_visualChildParts.get(m_previousHittestElementIndex);
			}
		} catch (Exception ex) {
		}
				
		for (int i = 0; i < m_elements.size(); i++) {
			Element element = (Element) m_elements.get(i);
			if (element.m_bounds.contains(x, y)) {
				m_previousHittestElementIndex = i;
				return (IVisualPart) m_visualChildParts.get(i);
			}
		}
		return null;
	}

	/**
	 * @see ListLayoutManagerBase#processElementsAdded(int, List)
	 */
	protected void processElementsAdded(int index, List addedElements) {
		Iterator i = addedElements.iterator();
		
		while (i.hasNext()) {
			Resource childPartData = (Resource) i.next();
			
			try {
				Resource part = Ozone.findPart(childPartData, m_source, m_partDataSource);
				if (part == null) {
					s_logger.error("Could not find part for " + childPartData);
					continue;
				}
				
				Class	c = Utilities.loadClass(part, m_source);
				Context	childContext = new Context(m_context);
				
				childContext.putLocalProperty(OzoneConstants.s_part, part);
				childContext.putLocalProperty(OzoneConstants.s_partData, childPartData);
				childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
				
				IVisualPart	vp = (IVisualPart) c.newInstance();
				vp.initialize(m_source, childContext);
				
				m_visualChildParts.add(index, vp);
				m_elements.add(index, new Element(childPartData));
				
				index++;
			} catch(Exception e) {
				s_logger.error("Failed to initialize child part " + childPartData, e);
			}
		}
		
		matchConstraints();
		bubbleResizeOrRedraw();
	}

	/**
	 * @see ListLayoutManagerBase#processElementsChanged(int, List)
	 */
	protected void processElementsChanged(int index, List changedElements) {
	}

	/**
	 * @see ListLayoutManagerBase#processElementsRemoved(int, List)
	 */
	protected void processElementsRemoved(int index, List removedElements) {
		for (int i = 0; i < removedElements.size(); i++) {
			IVisualPart vp = (IVisualPart) m_visualChildParts.get(index);
			
			vp.dispose();
			if (vp == m_previousVisualChildPart) {
				m_previousVisualChildPart = null;
			}
			
			m_visualChildParts.remove(index);
			m_elements.remove(index);
		}
		
		matchConstraints();
		bubbleResizeOrRedraw();
	}

	/**
	 * @see ListLayoutManagerBase#processListCleared()
	 */
	protected void processListCleared() {
		Iterator i = m_visualChildParts.iterator();
		while (i.hasNext()) {
			IVisualPart vp = (IVisualPart) i.next();
			
			vp.dispose();
		}

		m_previousVisualChildPart = null;
		m_visualChildParts.clear();
		m_elements.clear();

		matchConstraints();
		bubbleResizeOrRedraw();
	}
	
	protected void matchConstraints() {
		for (int i = 0; i < m_elements.size(); i++) {
			Element element = (Element) m_elements.get(i);
			
			Constraint constraint = null;
			
			while (true) {
				constraint = (Constraint) m_constraintsByResource.get(element.m_childPartData);
				if (constraint != null) {
					break;
				}
				
				constraint = (Constraint) m_constraintsByIndex.get(new Integer(i));
				if (constraint != null) {
					break;
				}
				
				constraint = (Constraint) m_constraintsByIndex.get(new Integer(i - m_elements.size()));
				break;
			}
			
			if (constraint != element.m_constraint) {
				element.m_constraint = constraint;
				element.m_needsRecalculation = true;
			}
		}

		/*	Find out which element to fill the rest of unfilled space.
		 */
		m_fillElement = -1;
		if (m_packLast) {
			for (int i = 0; i < m_elements.size(); i++) {
				Element elmt = (Element) m_elements.get(i);
				if (elmt.m_constraint == null || elmt.m_constraint.m_resizable) {
					m_fillElement = i;
					break;
				}
			}
		} else {
			for (int i = m_elements.size() - 1; i >= 0; i--) {
				Element elmt = (Element) m_elements.get(i);
				if (elmt.m_constraint == null || elmt.m_constraint.m_resizable) {
					m_fillElement = i;
					break;
				}
			}
		}
	}
	
	protected void bubbleResizeOrRedraw() {
		if (Ozone.isUIThread() && m_initializing) {
			return;
		}
		
		for (int i = 0; i < m_elements.size(); i++) {
			Element element = (Element) m_elements.get(i);
			element.m_needsRecalculation = true;
		}
		
		BlockScreenspace bs = internalCalculateSize(m_bounds.width, m_bounds.height, true);
		
		if (bs.m_size.x != m_bounds.width || bs.m_size.y != m_bounds.height) {
			IVisualPart parent = (IVisualPart) m_context.getLocalProperty(OzoneConstants.s_parentPart);
			
			parent.handleEvent(PartConstants.s_eventChildResize, new ChildPartEvent(this));
		} else {
			Region region = new Region();
			
			internalSetBounds(m_bounds, region);
			
			Rectangle r = region.getBounds();
			
			m_parent.redraw(r.x, r.y, r.width, r.height, true);
			
			region.dispose();
			region = null;
			r = null;
		}
	}
	abstract protected BlockScreenspace internalCalculateSize(int hintedWidth, int hintedHeight, boolean forceRedistribute);
	
	/**
	 * Redistributes the elements if any of them hasn't been set bounds or
	 * if any of them needs recalculation.
	 */
	abstract protected void redistributeElements();
	
	protected void onResizeMouseDown(MouseEvent e) {
		m_parent.setCursor(m_resizeCursor);
		m_resizeInitialPoint = new Point(e.x, e.y);
		
		MouseMoveListener mml = new MouseMoveListener() {
			Control m_control;
			
			public MouseMoveListener initialize(Control control) {
				m_control = control;
				return this;
			}
			
			public void mouseMove(MouseEvent e) {
				onResizing(e.x, e.y);
			}
		}.initialize(m_parent);
		
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
				
				onEndResizing();
				PartUtilities.setContainerControlDraggable(true, m_context);
				
				m_parent.setCursor(GraphicsManager.s_arrowCursor);
				m_resizing = false;
			}				
		}.initialize(m_parent, mml);
		
		PartUtilities.setContainerControlDraggable(false, m_context);
		onStartResizing();
		
		m_parent.setCapture(true);
		m_parent.addMouseMoveListener(mml);
		m_parent.addMouseListener(ml);
	}
	
	abstract protected int hittestResizeElement(int x, int y);
	abstract protected void onStartResizing();
	abstract protected void onResizing(int x, int y);
	abstract protected void onEndResizing();
	
	protected boolean elementResizable(Element e) {
		return e.m_constraint == null || e.m_constraint.m_resizable;
	}
}
