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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.ozone.standard.editor.LiteralEditorPart;
import edu.mit.lcs.haystack.ozone.standard.editor.MetadataEditorConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.ViewContainerPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.TextElement;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.INavigationMaster;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewContainerFactory;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.data.*;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

import java.io.Serializable;
import java.util.*;

/**
 * Stacks a series of parts as specified by a list data provider.
 * @author David Huynh
 * @author Dennis Quan
 */
public class RowStackerLayoutManager extends ListLayoutManagerBase implements IBlockGUIHandler {
	protected class Element implements Serializable {
		public RDFNode				m_underlying;
		public Rectangle			m_bounds = new Rectangle(0, 0, 0, 0);
		public BlockScreenspace	m_blockScreenspace;
		public boolean			m_needsRecalculation = true;
		
		public Element(RDFNode underlying) {
			m_underlying = underlying;
		}
		
		public String toString() {
			return m_underlying.toString();
		}
	}
	
	ArrayList				m_elements = new ArrayList(); // all elements, displayed or not
	BlockScreenspace		m_blockScreenspace = new BlockScreenspace();
	Rectangle				m_bounds = new Rectangle(0, 0, 0, 0);
	boolean				m_heightRestricted = false;
	boolean				m_needsRecalculation = true;
	
	int					m_defaultShowCount;
	int					m_elementsDisplayed; // number of elements being displayed
	
	Resource				m_morePartData;
	IVisualPart				m_moreItemsChild;
	Rectangle				m_moreBounds = new Rectangle(0, 0, 0, 0);
	boolean				m_showingMore;

	Resource				m_emptyPartData;
	IVisualPart				m_noItemChild;
	Rectangle				m_emptyBounds = new Rectangle(0, 0, 0, 0);
	boolean				m_showingEmpty;
	
	transient IViewContainerFactory	m_viewContainerFactory;
	
	Rectangle				m_insertRect;
	
	final static protected Resource s_moreItems = new Resource(LayoutConstants.s_namespace + "moreItems");
	final static protected Resource s_noItems = new Resource(LayoutConstants.s_namespace + "noItems");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(RowStackerLayoutManager.class);
	
	synchronized public void dispose() {
		disposeDataConsumers();
		
		m_elements.clear();
		m_elements = null;

		m_blockScreenspace = null;
		m_bounds = null;
		
		m_moreItemsChild.dispose();
		m_moreItemsChild = null;
		m_morePartData = null;
		m_moreBounds = null;

		m_noItemChild.dispose();
		m_noItemChild = null;
		m_emptyPartData = null;
		m_emptyBounds = null;

		super.dispose();
	}
	
	protected void internalInitialize() {
		super.internalInitialize();
		
		m_context.putProperty(LayoutConstants.s_layoutManager, this);
		
		m_viewContainerFactory = m_context.getViewContainerFactory();
		
		String s = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_defaultShowCount, m_partDataSource);
		if (s != null) {
			m_defaultShowCount = Math.max(0, Integer.parseInt(s));
		}
		
		m_morePartData = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_morePartData, m_partDataSource);
		if (m_morePartData == null) {
			m_morePartData = s_moreItems;
		}
		try {
			Resource part = Ozone.findPart(m_morePartData, m_source, m_partDataSource);
			Class	 klass = Utilities.loadClass(part, m_source);

			m_moreItemsChild = (IVisualPart) klass.newInstance(); 

			Context childContext = new Context(m_context);

			childContext.putLocalProperty(OzoneConstants.s_partData, m_morePartData);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);

			m_moreItemsChild.initialize(m_source, childContext);
		} catch (Exception e) {
			s_logger.error("Failed to create more items child part", e);
		}

		m_emptyPartData = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_emptyPartData, m_partDataSource);
		if (m_emptyPartData == null) {
			m_emptyPartData = s_noItems;
		}
		try {
			Resource part = Ozone.findPart(m_morePartData, m_source, m_partDataSource);
			Class	 klass = Utilities.loadClass(part, m_source);

			m_noItemChild = (IVisualPart) klass.newInstance();

			Context childContext = new Context(m_context);

			childContext.putLocalProperty(OzoneConstants.s_partData, m_emptyPartData);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);

			m_noItemChild.initialize(m_source, childContext);
		} catch (Exception e) {
			s_logger.error("Failed to create no items child part", e);
		}

		Resource viewPartClass = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_viewPartClass, m_partDataSource);
		if (viewPartClass != null) {
			m_context.putProperty(OzoneConstants.s_viewPartClass, viewPartClass);
		}
		
		makeDataConsumers();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (hintedWidth < 0) {
			return null;
		}

		internalCalculateSize(hintedWidth, hintedHeight, false);
		
		return new BlockScreenspace(m_blockScreenspace);
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
		
		for (int i = 0; i < m_elementsDisplayed; i++) {
			IVisualPart 		vp = (IVisualPart) m_visualChildParts.get(i);
			Element				element = (Element) m_elements.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (blockGUIHandler != null) {
				if (redrawAll || region.intersects(element.m_bounds)) {
					blockGUIHandler.draw(gc, element.m_bounds);
				}
			}
		}
		
		if (m_showingMore) {
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) m_moreItemsChild.getGUIHandler(IBlockGUIHandler.class);
			
			if (blockGUIHandler != null) {
				if (redrawAll || region.intersects(m_moreBounds)) {
					blockGUIHandler.draw(gc, m_moreBounds);
				}
			}
		} else if (m_showingEmpty) {
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) m_noItemChild.getGUIHandler(IBlockGUIHandler.class);

			if (blockGUIHandler != null) {
				if (redrawAll || region.intersects(m_emptyBounds)) {
					blockGUIHandler.draw(gc, m_emptyBounds);
				}
			}
		}
		
		if (redrawAll) {
			gc.setClipping(region);
		}
		
		if (m_insertRect != null) {
			boolean	xor = gc.getXORMode();
			int		lineStyle = gc.getLineStyle();
			int		lineWidth = gc.getLineWidth();
			Color		color = gc.getForeground();

			gc.setXORMode(true);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.setLineWidth(1);
			gc.setForeground(SlideUtilities.getAmbientColor(m_context));

			gc.drawRectangle(m_insertRect.x, m_insertRect.y, m_insertRect.width - 1, m_insertRect.height - 1);
			
			gc.setXORMode(xor);
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			gc.setForeground(color);
		}
		
		region.dispose();
		region = null;
	}

	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
  public void renderHTML(HTMLengine he) {
    he.enter("RowStackerLayoutManager");
    for (int i = 0; i < m_elementsDisplayed; i++) {
      IVisualPart vp = (IVisualPart) m_visualChildParts.get(i);
      Element	  element = (Element) m_elements.get(i);
      IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
      if (blockGUIHandler != null) blockGUIHandler.renderHTML(he);
    }
    if (m_showingMore) {
      IBlockGUIHandler	blockGUIHandler = 
	(IBlockGUIHandler) m_moreItemsChild.getGUIHandler(IBlockGUIHandler.class);
      if (blockGUIHandler != null) blockGUIHandler.renderHTML(he);
    } else if (m_showingEmpty) {
      IBlockGUIHandler	blockGUIHandler = 
	(IBlockGUIHandler) m_noItemChild.getGUIHandler(IBlockGUIHandler.class);
      if (blockGUIHandler != null) blockGUIHandler.renderHTML(he);
    }
    he.exit("RowStackerLayoutManager");
  }
		
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.WIDTH;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		if (m_visualChildParts.size() > 0) {
			IVisualPart 		vp = (IVisualPart) m_visualChildParts.get(0);
			IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (bgh != null) {
				return bgh.getTextAlign();
			}
		}
		return BlockScreenspace.ALIGN_TEXT_BASE_LINE;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		internalSetBounds(r);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#handleGUIEvent(Resource, EventObject)
	 */
	public boolean handleGUIEvent(Resource eventType, EventObject event) {
		return internalHandleGUIEvent(eventType, event);
	}

	/**
	 * @see IGUIHandler@setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		for (int i = 0; i < m_elementsDisplayed; i++) {
			IVisualPart vp = (IVisualPart) m_visualChildParts.get(i);
			IGUIHandler	guiHandler = vp.getGUIHandler(null);
			
			if (guiHandler != null) {
				guiHandler.setVisible(visible);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ILayoutManager#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		}
		return null;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragExit(DropTargetEvent)
	 */
	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertRect != null) {
			PartUtilities.repaint(m_insertRect, m_context);
			m_insertRect = null;
		}
		return true;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragEnter(DropTargetEvent)
	 */
	protected boolean onDragHover(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		Rectangle r = calcBoundaryRect(event.m_x, event.m_y);
		if (r == null || !r.equals(m_insertRect)) {
			if (m_insertRect != null) {
				PartUtilities.repaint(m_insertRect, m_context);
			}
			if (r != null) {
				PartUtilities.repaint(r, m_context);
			}
			m_insertRect = r;
		}
		return super.onDragEnter(event);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onChildHasHandledDragAndDropEvent(Resource, DropTargetEvent)
	 */
	protected void onChildHasHandledDragAndDropEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertRect != null) {
			PartUtilities.repaint(m_insertRect, m_context);
			m_insertRect = null;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDropResourceList(List, DropTargetEvent)
	 */
	protected boolean onDropResourceList(List resourceList, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		highlightContent(false);
		try {
			Resource resourceToDrop = (Resource) resourceList.get(0);
			
			if (m_supportsListInsertion) {
				for (int i = 0; i < m_elements.size(); i++) {
					Element element = (Element) m_elements.get(i);
					
					if (element.m_underlying.equals(resourceToDrop)) {
						if (m_lastInsertionPoint == i) {
							return true;
						} else {
							m_listDataProviderWrapper.requestRemoval(i, 1);
							if (i < m_lastInsertionPoint) {
								m_lastInsertionPoint--;
							}
							break;
						}
					}
				}
			}
						
			if (m_lastInsertionPoint != -1 && m_supportsListInsertion) {
				m_listDataProviderWrapper.requestAddition(resourceToDrop, m_lastInsertionPoint);
			} else if (m_supportsSetAddition) {
				m_listDataProviderWrapper.getDataProvider().requestChange(DataConstants.SET_ADDITION, resourceToDrop);
			}
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch in drop", e);
		} catch (DataNotAvailableException e) {
			s_logger.error("Data not available in drop", e);
		}
		return false;
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDropResources(java.util.List, DropTargetEvent)
	 */
	protected boolean onDropResources(List resources, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		highlightContent(false);
		try {
			if (m_lastInsertionPoint != -1 && m_supportsListInsertion) {
				m_listDataProviderWrapper.requestAdditions(resources, m_lastInsertionPoint);
			} else if (m_supportsSetAddition) {
				HashSet set = new HashSet();
				
				set.addAll(resources);
				
				m_listDataProviderWrapper.getDataProvider().requestChange(DataConstants.SET_ADDITION, set);
			}
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch in drop", e);
		} catch (DataNotAvailableException e) {
			s_logger.error("Data not available in drop", e);
		}
		return false;
	}

	public void browseToSource() {
		try {
			Resource resource = (Resource) m_dataProvider.getData(DataConstants.RESOURCE, null);
			
			if (resource != null) {
				INavigationMaster nm = (INavigationMaster) m_context.getProperty(OzoneConstants.s_navigationMaster);
				
				nm.requestViewing(resource);
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("Failed to browse to source of row stacker layout", e);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#hittest
	 * (int, int, boolean)
	 */
	int m_previousHittestElementIndex;
	protected IVisualPart hittest(int x, int y, boolean favorParent) {
		int margin = favorParent ? 4 : 0;
		
		if (m_previousHittestElementIndex >= 0 && m_previousHittestElementIndex < m_elementsDisplayed) {
			Element element = (Element) m_elements.get(m_previousHittestElementIndex);
			if (element != null &&
				x >= element.m_bounds.x && x < element.m_bounds.x + element.m_bounds.width &&
				y >= element.m_bounds.y + margin && y < element.m_bounds.y + element.m_bounds.height - margin) {
				return (IVisualPart) m_visualChildParts.get(m_previousHittestElementIndex);
			}
		}
		
		for (int i = 0; i < m_elementsDisplayed; i++) {
			Element element = (Element) m_elements.get(i);
			if (x >= element.m_bounds.x && x < element.m_bounds.x + element.m_bounds.width &&
				y >= element.m_bounds.y + margin && y < element.m_bounds.y + element.m_bounds.height - margin) {
				m_previousHittestElementIndex = i;
				return (IVisualPart) m_visualChildParts.get(i);
			}
		}
		
		m_previousHittestElementIndex = -1;
		if (m_showingMore && m_moreBounds.contains(x, y)) {
			return m_moreItemsChild;
		} else if (m_showingEmpty && m_emptyBounds.contains(x, y)) {
			return m_noItemChild;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#passChildResizeToParent()
	 */
	protected boolean passChildResizeToParent() {
		Iterator iChildPart = m_childrenToResize.iterator();
		
		while (iChildPart.hasNext()) {
			IPart	childPart = (IPart) iChildPart.next();
			int	i = m_visualChildParts.indexOf(childPart);
			
			if (i >= 0) {
				Element element = (Element) m_elements.get(i);
				element.m_needsRecalculation = true;
				
				if (i < m_elementsDisplayed) {
					m_needsRecalculation = true;
				}
			}
		}
		return !m_heightRestricted;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#redraw()
	 */
	protected void redraw() {
		m_needsRecalculation = true;
		setBounds(m_bounds);
		PartUtilities.repaint(m_bounds, m_context);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsAdded(int, List)
	 */
	protected void processElementsAdded(int index, List addedElements) {
		if (m_context == null) {
			return;
		}
		
		for (int i = 0; i < addedElements.size(); i++) {
			try {
				RDFNode underlying = (RDFNode) addedElements.get(i);
				
				m_elements.add(index + i, new Element(underlying));
				
				if (m_defaultShowCount == 0 || index < m_visualChildParts.size()) {
					IPart part = createPart(underlying);
					m_visualChildParts.add(index + i, part);
					m_elementsDisplayed++;
				}
			} catch (Exception e) {
				s_logger.error("Failed while processing added elements", e);
			}
		}

		onDisplayedElementsChanged();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsRemoved(int, List)
	 */
	protected void processElementsRemoved(int index, List removedElements) {
		if (m_context == null || removedElements == null) {
			return;
		}

		for (int i = 0; i < removedElements.size(); i++) {
			if (index < m_visualChildParts.size()) {
				((IPart) m_visualChildParts.remove(index)).dispose();
			}
			m_elements.remove(index);
		}

		if (index < m_elementsDisplayed) {
			m_elementsDisplayed -= Math.min(m_elementsDisplayed - index, removedElements.size());
		}
		
		onDisplayedElementsChanged();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processListCleared()
	 */
	protected void processListCleared() {
		if (m_context == null) {
			return;
		}

		Iterator i = m_visualChildParts.iterator();
		while (i.hasNext()) {
			IVisualPart vp = (IVisualPart) i.next();
			
			vp.dispose();
		}
		
		m_visualChildParts.clear();
		m_elements.clear();

		m_elementsDisplayed = 0;
		
		onDisplayedElementsChanged();
	}
	
	protected void onDisplayedElementsChanged() {
		if (Ozone.isUIThread() && m_initializing) {
			return;
		}

		if (m_heightRestricted) {
			redraw();
		} else {
			Ozone.idleExec(new IdleRunnable(m_context) {
				public void run() {
					if (m_context != null) {
						ChildPartEvent event = new ChildPartEvent(RowStackerLayoutManager.this);
						m_layoutPart.handleEvent(PartConstants.s_eventChildResize, event);
					}
				}
			});
		}
	}
	
	protected void internalCalculateSize(int hintedWidth, int hintedHeight, boolean settingBounds) {
		BlockScreenspace moreBS = calculateElementSize(
			(IBlockGUIHandler) (m_showingEmpty ? m_noItemChild : m_moreItemsChild).getGUIHandler(IBlockGUIHandler.class), hintedWidth);
		
		boolean widthChanged = m_blockScreenspace.m_size.x != hintedWidth;
			
		m_blockScreenspace.m_align = BlockScreenspace.ALIGN_TEXT_CLEAR;
		m_blockScreenspace.m_alignOffset = 0;
		m_blockScreenspace.m_size.x = 0;
		m_blockScreenspace.m_size.y = 0;
		m_blockScreenspace.m_clearanceLeft = 0;
		m_blockScreenspace.m_clearanceRight = 0;
		m_blockScreenspace.m_clearanceTop = 0;
		m_blockScreenspace.m_clearanceBottom = 0;
		
		if (!settingBounds) {
			m_heightRestricted = hintedHeight >= 0;
		}
		
		int max;
		if (hintedHeight < 0) {		
			max = m_defaultShowCount > 0 ? Math.min(m_defaultShowCount, m_elements.size()) : m_elements.size();
		} else {
			max = m_elements.size();
		}
		
		int 		previousBottomClearance = 0;
		boolean	firstTopClearance = true;
		boolean	continueAppending = true;
		
		for (int i = 0; i < Math.min(m_visualChildParts.size(), max); i++) {
			IVisualPart			vp = (IVisualPart) m_visualChildParts.get(i);
			Element				element = (Element) m_elements.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (m_needsRecalculation || element.m_needsRecalculation || element.m_blockScreenspace == null || widthChanged) {
				if (blockGUIHandler != null) {
					element.m_blockScreenspace = calculateElementSize(blockGUIHandler, hintedWidth);
					element.m_needsRecalculation = false;
				}
			}
			
			if (element.m_blockScreenspace != null && continueAppending) {
				BlockScreenspace bsToAppend = element.m_blockScreenspace;
				
				if (hintedHeight >= 0) {
					if (i == 0 && m_heightRestricted && bsToAppend.m_size.y + Math.max(bsToAppend.m_clearanceBottom, moreBS.m_clearanceTop) + moreBS.m_size.y > hintedHeight) {
						// Can't even fit the first child
					
						bsToAppend = new BlockScreenspace(bsToAppend);
						bsToAppend.m_size.y = hintedHeight - moreBS.m_size.y - Math.max(bsToAppend.m_clearanceBottom, moreBS.m_clearanceTop);
					} else if (i < m_elements.size() - 1) { // not the last element
						if (m_blockScreenspace.m_size.y + 
							(firstTopClearance ? 0 : Math.max(previousBottomClearance, element.m_blockScreenspace.m_clearanceTop)) +
							element.m_blockScreenspace.m_size.y + 
							Math.max(element.m_blockScreenspace.m_clearanceBottom, moreBS.m_clearanceTop) +
							moreBS.m_size.y > hintedHeight) {
								
							bsToAppend = moreBS;
							continueAppending = false;
						}
					} else { // the last element
						if (m_blockScreenspace.m_size.y + 
							(firstTopClearance ? 0 : Math.max(previousBottomClearance, element.m_blockScreenspace.m_clearanceTop)) +
							element.m_blockScreenspace.m_size.y > hintedHeight) {
								
							bsToAppend = moreBS;
							continueAppending = false;
						}
					}
				}
				
				m_blockScreenspace.m_size.x =
					Math.max(m_blockScreenspace.m_size.x, bsToAppend.m_size.x);
				m_blockScreenspace.m_size.y +=
					(firstTopClearance ? 0 : Math.max(previousBottomClearance, bsToAppend.m_clearanceTop)) +
					bsToAppend.m_size.y;
					
				m_blockScreenspace.m_clearanceLeft =
					Math.max(m_blockScreenspace.m_clearanceLeft, bsToAppend.m_clearanceLeft);
				m_blockScreenspace.m_clearanceRight =
					Math.max(m_blockScreenspace.m_clearanceRight, bsToAppend.m_clearanceRight);
					
				if (firstTopClearance) {
					m_blockScreenspace.m_clearanceTop = bsToAppend.m_clearanceTop;
					
					m_blockScreenspace.m_alignOffset = bsToAppend.m_alignOffset;
					m_blockScreenspace.m_align = bsToAppend.m_align;
				}
				
				previousBottomClearance = bsToAppend.m_clearanceBottom;

				firstTopClearance = false;
			}
			
		}
		
		if (m_elements.isEmpty()) {
			m_blockScreenspace.m_size.x =
				Math.max(m_blockScreenspace.m_size.x, moreBS.m_size.x);
			m_blockScreenspace.m_size.y += 
				moreBS.m_size.y;

			m_blockScreenspace.m_clearanceLeft =
				Math.max(m_blockScreenspace.m_clearanceLeft, moreBS.m_clearanceLeft);
			m_blockScreenspace.m_clearanceRight =
				Math.max(m_blockScreenspace.m_clearanceRight, moreBS.m_clearanceRight);

			if (firstTopClearance) {
				m_blockScreenspace.m_clearanceTop = moreBS.m_clearanceTop;
				
				m_blockScreenspace.m_alignOffset = moreBS.m_alignOffset;
				m_blockScreenspace.m_align = moreBS.m_align;
			}
			
			previousBottomClearance = moreBS.m_clearanceBottom;
		}

		m_blockScreenspace.m_clearanceBottom = previousBottomClearance;
		
		m_blockScreenspace.m_size.x = Math.max(m_blockScreenspace.m_size.x, hintedWidth);
		m_blockScreenspace.m_size.y = Math.max(m_blockScreenspace.m_size.y, hintedHeight);
		
		m_needsRecalculation = false;
	}
	
	protected void internalSetBounds(Rectangle r) {
		if (m_needsRecalculation || r.width != m_blockScreenspace.m_size.x) {
			internalCalculateSize(r.width, r.height, true);
		}
		m_bounds.x = r.x; m_bounds.y = r.y; m_bounds.width = r.width; m_bounds.height = r.height;
		
		IBlockGUIHandler moreBGH = (IBlockGUIHandler) m_moreItemsChild.getGUIHandler(IBlockGUIHandler.class);
		BlockScreenspace moreBS = calculateElementSize(moreBGH, r.width);
		IBlockGUIHandler emptyBGH = (IBlockGUIHandler) m_noItemChild.getGUIHandler(IBlockGUIHandler.class);
		BlockScreenspace emptyBS = calculateElementSize(emptyBGH, r.width);
			
		int		previousElementsDisplayed = m_elementsDisplayed;
		int 		previousBottomClearance = 0;
		Rectangle	r2 = new Rectangle(r.x, 0, r.width, 0);
		int		i;
		int		y = 0;
		boolean	first = true;
		
		m_elementsDisplayed = 0;
		m_showingMore = false;
		m_showingEmpty = false;
		
		for (i = 0; i < m_visualChildParts.size(); i++) {
			IVisualPart			vp = (IVisualPart) m_visualChildParts.get(i);
			Element				element = (Element) m_elements.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			r2.height = 0;
			
			if (element.m_blockScreenspace != null) {
				BlockScreenspace	bsToAppend = element.m_blockScreenspace;
				IBlockGUIHandler	bgh = blockGUIHandler;
				
				if (first && m_heightRestricted && bsToAppend.m_size.y + Math.max(bsToAppend.m_clearanceBottom, moreBS.m_clearanceTop) + moreBS.m_size.y > r.height) {
					// Can't even fit the first child
					
					bsToAppend = new BlockScreenspace(bsToAppend);
					bsToAppend.m_size.y = r.height - moreBS.m_size.y - Math.max(bsToAppend.m_clearanceBottom, moreBS.m_clearanceTop);
					
					m_elementsDisplayed++;
				} else if (i < m_visualChildParts.size() - 1) { // not the last element
					if (y + 
						(first ? 0 : Math.max(previousBottomClearance, element.m_blockScreenspace.m_clearanceTop)) +
						element.m_blockScreenspace.m_size.y + 
						Math.max(element.m_blockScreenspace.m_clearanceBottom, moreBS.m_clearanceTop) +
						moreBS.m_size.y > r.height) {
							
						bsToAppend = moreBS;
						bgh = moreBGH;
					} else {
						m_elementsDisplayed++;
					}
				} else { // the last element
					if (y + 
						(first ? 0 : Math.max(previousBottomClearance, element.m_blockScreenspace.m_clearanceTop)) +
						element.m_blockScreenspace.m_size.y > r.height) {
						
						bsToAppend = moreBS;
						bgh = moreBGH;
					} else {
						m_elementsDisplayed++;
					}
				}
				
				y += (first ? 0 : Math.max(previousBottomClearance, bsToAppend.m_clearanceTop));
				
				r2.y = r.y + y;
				r2.height += bsToAppend.m_size.y;
				
				y += bsToAppend.m_size.y;
				
				if (bsToAppend != moreBS) {
					if (!r2.equals(element.m_bounds)) {
						element.m_bounds.x = r2.x; element.m_bounds.y = r2.y; element.m_bounds.width = r2.width; element.m_bounds.height = r2.height;
						if (bgh != null) {
							bgh.setBounds(r2);
						}
					}
				} else {
					int diff = m_elements.size() - m_elementsDisplayed;
					
					if (m_morePartData.equals(s_moreItems)) {
						((TextElement) m_moreItemsChild).setText(diff + (diff > 1 ? " more items (" : " more item (") + m_elements.size() + " total)");
					}
					
					m_moreBounds.x = r2.x; m_moreBounds.y = r2.y; m_moreBounds.width = r2.width; m_moreBounds.height = r2.height;
					
					if (bgh != null) {
						bgh.setBounds(r2);
					}
					
					m_showingMore = true;
					
					break;
				}
				
				previousBottomClearance = bsToAppend.m_clearanceBottom;
				
				first = false;
			}
		}
		
		if (m_elements.isEmpty()) {
			m_showingMore = false;
			m_showingEmpty = true;
			
			y += (first ? 0 : Math.max(previousBottomClearance, emptyBS.m_clearanceTop));
			
			r2.y = r.y + y;
			r2.height = emptyBS.m_size.y;
			
			y += emptyBS.m_size.y;
			
			emptyBGH.setBounds(r2);
			
			m_emptyBounds.x = r2.x; m_emptyBounds.y = r2.y; m_emptyBounds.width = r2.width; m_emptyBounds.height = r2.height;
		}
		
		/* Construct new view containers to fill up space later.
		 */
		if (i == m_visualChildParts.size() && i < m_elements.size()) {
			boolean moreElementsToShow = false;
			int count = 
				m_heightRestricted ?
					(y > 0 ? 
						(m_bounds.height - y) * i / y : 
						(m_defaultShowCount > 0 ? m_defaultShowCount : m_elements.size() - i)) :
					(m_defaultShowCount > 0 ? m_defaultShowCount : m_elements.size() - i);
			count = Math.min(count, m_elements.size() - i);
			
			for (int j = i; j < i + count; j++) {
				Element element = (Element) m_elements.get(j);
				IPart	part = createPart(element.m_underlying);
				
				m_visualChildParts.add(j, part);
				
				moreElementsToShow = true;
			}
			
			if (moreElementsToShow) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					public void run() {
						if (m_context != null) {
							ChildPartEvent event = new ChildPartEvent(RowStackerLayoutManager.this);
							m_layoutPart.handleEvent(PartConstants.s_eventChildResize, event);
						}
					}
				});
			}
		}

		/*
		 * Show/hide elements.
		 */
		IBlockGUIHandler blockGUIHandler;
		if (previousElementsDisplayed >= 0 && previousElementsDisplayed < m_elementsDisplayed) {
			for (i = previousElementsDisplayed; i < m_elementsDisplayed; i++) {
				Element 	element = (Element) m_elements.get(i);
				IVisualPart vp = (IVisualPart) m_visualChildParts.get(i);
				
				blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
				if (blockGUIHandler != null) {
					blockGUIHandler.setVisible(true);
				}
			}
		} else if (previousElementsDisplayed > m_elementsDisplayed) {
			for (i = m_elementsDisplayed; i < previousElementsDisplayed; i++) {
				Element 	element = (Element) m_elements.get(i);
				IVisualPart vp = (IVisualPart) m_visualChildParts.get(i);
				
				blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
				if (blockGUIHandler != null) {
					blockGUIHandler.setVisible(false);
				}
			}
		}	
	}
	
	protected BlockScreenspace calculateElementSize(IBlockGUIHandler blockGUIHandler, int width) {
		BlockScreenspace bs = null;
		
		switch (blockGUIHandler.getHintedDimensions()) {
			case IBlockGUIHandler.FIXED_SIZE:
				bs = blockGUIHandler.getFixedSize();
				break;
			case IBlockGUIHandler.WIDTH:
				bs = blockGUIHandler.calculateSize(width, -1);
				break;
			default:
				bs = blockGUIHandler.calculateSize(width, 0);
		}
		return bs;
	}
	
	protected IPart createPart(RDFNode node) {
		if (node instanceof Resource) {
			return createViewContainer((Resource) node);
		} else {
			Context	childContext = new Context(m_context);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			childContext.putLocalProperty(MetadataEditorConstants.object, node);

			// TODO[dquan]: generalize
			LiteralEditorPart lep = new LiteralEditorPart();
			lep.initialize(m_source, childContext);

			return lep;
		}
	}
	
	protected IViewContainerPart createViewContainer(Resource underlying) {
		IViewContainerPart vcp = m_viewContainerFactory != null ?
			m_viewContainerFactory.createViewContainer() :
			new ViewContainerPart(true);
			
		if (vcp instanceof ViewContainerPart) {
			((ViewContainerPart) vcp).setNestingRelation(m_nestingRelation);
		}
		
		Context	childContext = new Context(m_context);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		
		vcp.initialize(m_source, childContext);
		vcp.navigate(underlying);
		
		return vcp;
	}
	
	protected int m_lastInsertionPoint = -1;
	
	protected Rectangle calcBoundaryRect(int x, int y) {
		if (m_supportsListInsertion) {
			Rectangle 	bounds = null;
			Rectangle	previousBounds = null;
			int		i;
			
			for (i = 0; i < m_elementsDisplayed; i++) {
				Element element = (Element) m_elements.get(i);
				
				previousBounds = bounds;
				bounds = element.m_bounds;
				
				if (y < (bounds.y + bounds.height / 2)) {
					break;
				}
			}

			if (bounds != null) {
				m_lastInsertionPoint = i;
				
				if (i == m_elementsDisplayed) {
					return new Rectangle(bounds.x, bounds.y + bounds.height, bounds.width, 3);
				} else if (previousBounds != null) {
					return new Rectangle(bounds.x, (previousBounds.y + previousBounds.height + bounds.y) / 2 - 2, bounds.width, 3);
				} else {
					return new Rectangle(bounds.x, bounds.y - 2, bounds.width, 3);
				}
			}
		} else if (m_supportsSetAddition) {
			m_lastInsertionPoint = -1;
			return new Rectangle(m_bounds.x, m_bounds.y, m_bounds.width, m_bounds.height);
		}
		return null;
	}
	
	protected void prepareContextMenu(MouseEvent e) {
		removeContextOperations();

		for (int i = 0; i < m_elementsDisplayed; i++) {
			Element element = (Element) m_elements.get(i);
			if (element.m_bounds.contains(e.x, e.y)) {
				if (((IPart) m_visualChildParts.get(i)).handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, e.x, e.y))) {
					if (element.m_underlying instanceof Resource) {
						createRemoveContextOperation((Resource) element.m_underlying, i);
					}
				}
				break;
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onContentHighlight(EventObject, boolean)
	 */
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		highlightContent(event.m_highlight);
		return true;
	}
	
	protected void highlightContent(boolean highlight) {
		if (highlight) {
			if (!m_bounds.equals(m_insertRect)) {
				//PartUtilities.repaint(m_insertRect, m_context);
				PartUtilities.repaint(m_bounds, m_context);
				m_insertRect = m_bounds;
			}
		} else if (m_insertRect != null) {
			PartUtilities.repaint(m_insertRect, m_context);
			m_insertRect = null;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_moreItemsChild != null) {
			m_moreItemsChild.initializeFromDeserialization(source);
		}
		if (m_noItemChild != null) {
			m_noItemChild.initializeFromDeserialization(source);
		}
		m_viewContainerFactory = m_context.getViewContainerFactory();
	}
}