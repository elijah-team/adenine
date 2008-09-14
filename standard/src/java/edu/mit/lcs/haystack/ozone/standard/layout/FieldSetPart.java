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

import java.io.Serializable;
import java.util.List;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartCache;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.MultipleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.PossibleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Control;

import java.util.*;

/**
 * @author David Huynh
 */
public class FieldSetPart extends ContainerPartBase implements IBlockGUIHandler {
	transient protected ListDataProviderWrapper	m_dataProviderWrapper;
	transient protected ListDataConsumer		m_dataConsumer;
	
	transient protected Control			m_parent;
	
	protected Resource					m_fieldPartDataGenerator;
	protected HashMap					m_fieldPartDataMap;
	protected LinkedList				m_fields = new LinkedList();
	
	protected boolean					m_drawDividers;
	protected boolean					m_changesEnabled = false;
	protected boolean m_defaultDropTargetResult = false;
	protected int m_defaultDropOperation = DND.DROP_NONE;

	transient protected Color			m_borderColor;
	
	protected BlockScreenspace			m_bs = new BlockScreenspace(BlockScreenspace.ALIGN_TEXT_BASE_LINE);
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FieldSetPart.class);
	
	class FieldLocation implements Serializable {
		public Rectangle			m_rect;
		public BlockScreenspace	m_bs;
		public boolean			m_resizable;
		public int				m_dimension;
		
		public FieldLocation(int dimension, BlockScreenspace bs, boolean resizable) {
			m_bs = bs;
			m_rect = new Rectangle(0, 0, bs.m_size.x, bs.m_size.y);
			m_resizable = resizable;
			m_dimension = dimension;
		}
	}
	
	transient HashMap m_cachedFieldParts; 
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		m_cachedFieldParts = new HashMap();
		
		// Save fields' visual parts for later use
		for (int i = 0; i < m_fields.size(); i++) {
			FieldData fd = (FieldData) m_fields.get(i);
			Object vp = m_childParts.get(i);
			if (fd != null) {
				m_cachedFieldParts.put(fd.m_fieldID, vp);
			}
		}
		m_childParts.clear();
		m_childData.clear();
		m_fields.clear();

		super.initializeFromDeserialization(source);
		m_parent = (Control) m_context.getSWTControl();
		m_borderColor = GraphicsManager.acquireColor("85%", SlideUtilities.getAmbientBgcolor(m_context));
		
		Ozone.idleExec(new IdleRunnable(10) {
			public void run() {
				if (m_context == null) {
					return;
				}
				setupDataSource();
				
				// Clean up remaining
				Iterator j = m_cachedFieldParts.values().iterator();
				while (j.hasNext()) {
					Object o = j.next();
					try {
						((IPart) o).dispose();
					} catch (Exception e) {
						s_logger.warn("Unclean dispose without initializeFromDeserialization from part " + o, e);
					}
				}
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.IdleRunnable#hasExpired()
			 */
			public boolean hasExpired() {
				return m_dataProviderWrapper != null;
			}
		});
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().unregisterConsumer(m_dataConsumer);
			m_dataProviderWrapper = null;
		}
		m_dataConsumer = null;
		
		m_fieldPartDataGenerator = null;
		if (m_fieldPartDataMap != null) {
			m_fieldPartDataMap.clear();
			m_fieldPartDataMap = null;
		}
		
		GraphicsManager.releaseColor(m_borderColor);
		
		m_parent = null;
		
		super.dispose();
	}
	
	protected void internalInitialize() {
		super.internalInitialize();

		m_parent = (Control) m_context.getSWTControl();
		m_changesEnabled = Utilities.checkBooleanProperty(m_prescription, LayoutConstants.s_enableChanges, m_partDataSource);
		
		if (m_changesEnabled) {
			m_defaultDropTargetResult = true;
			m_defaultDropOperation = DND.DROP_LINK;
		}

		setupFieldSource();		
		
		m_borderColor = GraphicsManager.acquireColor("85%", SlideUtilities.getAmbientBgcolor(m_context));
		m_drawDividers = Utilities.checkBooleanProperty(m_prescription, LayoutConstants.s_drawFieldDividers, m_partDataSource);
		
		setupDataSource();
	}
	
	protected void setupFieldSource() {
		m_fieldPartDataGenerator = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_fieldPartDataGenerator, m_partDataSource);
		if (m_fieldPartDataGenerator == null) {
			Resource fieldPartDataMap = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_fieldPartDataMap, m_partDataSource);
			if (fieldPartDataMap == null) {
				return;
			}
			
			try {
				Iterator fieldID = ListUtilities.accessDAMLList(
					Utilities.getResourceProperty(
						fieldPartDataMap, LayoutConstants.s_fieldIDs, m_partDataSource), 
					m_partDataSource
				);
				Iterator partData = ListUtilities.accessDAMLList(
					Utilities.getResourceProperty(
						fieldPartDataMap, LayoutConstants.s_fieldPartData, m_partDataSource), 
					m_partDataSource
				);
				
				m_fieldPartDataMap = new HashMap();
				
				while (fieldID.hasNext() && partData.hasNext()) {
					m_fieldPartDataMap.put(fieldID.next(), partData.next());
				}
			} catch (Exception e) {
				s_logger.error(e);
			}
		}
	}
	
	protected void setupDataSource() {
		IDataProvider dataProvider = (IDataProvider) m_context.getProperty(LayoutConstants.s_fieldsDataProvider);

		if (dataProvider != null) {
			m_dataProviderWrapper = new ListDataProviderWrapper(dataProvider);
			m_dataConsumer = new ListDataConsumer() {
				protected void onElementsAdded(int index, int count) {
					FieldSetPart.this.onElementsAdded(index, count);
				}

				protected void onElementsChanged(int index, int count) {
					FieldSetPart.this.onElementsChanged(index, count);
				}

				protected void onElementsChanged(List changedIndices) {
					FieldSetPart.this.onElementsChanged(changedIndices);
				}

				protected void onElementsRemoved(
					int index,
					int count,
					List removedElements) {
					FieldSetPart.this.onElementsRemoved(index, count, removedElements);
				}
				
				protected void onListCleared() {
					FieldSetPart.this.onListCleared();
				}
			};
			
			dataProvider.registerConsumer(m_dataConsumer);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (hintedWidth == -1) {
			return null;
		}
		
		layoutChildParts(false, hintedWidth != m_rect.width);
		
		BlockScreenspace bs = new BlockScreenspace(m_bs);
		
		bs.m_size.x = hintedWidth;
		
		return bs;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {

		setBounds(r);

		Rectangle r2 = new Rectangle(0, 0, 0, 0);
		
		for (int i = 0; i < m_childData.size(); i++) {
			FieldLocation		fl = (FieldLocation) m_childData.get(i);

			r2.x = m_rect.x + fl.m_rect.x;
			r2.y = m_rect.y + fl.m_rect.y;
			r2.width = fl.m_rect.width;
			r2.height = fl.m_rect.height;
				
			IVisualPart			vp = (IVisualPart) m_childParts.get(i);
			IBlockGUIHandler 	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (bgh != null) {
				bgh.draw(gc, r2);
			}
		}
		
		if (m_drawDividers) {
			int	lineWidth = gc.getLineWidth();
			int	lineStyle = gc.getLineStyle();
			Color	foreground = gc.getForeground();
			
			gc.setLineWidth(1);
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setForeground(m_borderColor);
			
			for (int i = 0; i < m_childData.size(); i++) {
				FieldLocation	fl = (FieldLocation) m_childData.get(i);
				int			x = m_rect.x + fl.m_rect.x + fl.m_rect.width - 1;
				int			y = m_rect.y + fl.m_rect.y + 1;
			
				gc.drawLine(x, m_rect.y + 1, x, m_rect.y + m_rect.height - 2);
			}
	
			gc.setLineWidth(lineWidth);
			gc.setLineStyle(lineStyle);
			gc.setForeground(foreground);
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
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("FieldSetPart");
		for (int i = 0; i < m_childData.size(); i++) {
		  IVisualPart			vp = (IVisualPart) m_childParts.get(i);
		  IBlockGUIHandler 	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
		  if (bgh != null) bgh.renderHTML(he);
		}
		he.exit("FieldSetPart");
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
		return BlockScreenspace.ALIGN_TEXT_BASE_LINE;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		if (!r.equals(m_rect)) {
			boolean recalculate = r.width != m_rect.width;
			cacheBounds(r);
			layoutChildParts(true, recalculate);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onChildResize(ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		int index = m_childParts.indexOf(e.m_childPart);
		
		if (index >= 0) {
			FieldLocation		fl = (FieldLocation) m_childData.get(index);
			BlockScreenspace	bs = calculateChildPartSize((IVisualPart) e.m_childPart, fl.m_bs.m_size.x);
			
			if (!bs.equals(fl.m_bs)) {
				fl.m_bs = bs;
				fl.m_rect.width = bs.m_size.x;
				fl.m_rect.height = bs.m_size.y;
				return super.onChildResize(e);
			}
		}
		return true;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase#hittest(EventObject, int, int)
	 */
	protected IVisualPart hittest(int x, int y) {
		x = x - m_rect.x;
		for (int i = 0; i < m_childData.size(); i++) {
			FieldLocation fl = (FieldLocation) m_childData.get(i);
			
			if (x > fl.m_rect.x + s_resizeHotspotWidth && 
				x < fl.m_rect.x + fl.m_rect.width - s_resizeHotspotWidth) {
				return (IVisualPart) m_childParts.get(i);
			}
		}
		return null;
	}

	final static protected int	s_elementsAdded = 0;
	final static protected int	s_elementsRemoved = 1;
	final static protected int	s_elementsChanged = 2;
	final static protected int	s_listCleared = 3;
	
	protected class ElementsEvent extends IdleRunnable {
		int	m_event;
		int	m_index;
		List	m_indices;
		List	m_elements;
		
		public ElementsEvent(int event, int index, List elements) {
			super(m_context);
			m_event = event;
			m_index = index;
			m_elements = elements;
			synchronized (FieldSetPart.this) {
				m_eventCount++;
			}
		}
		
		public ElementsEvent(int event, List indices, List elements) {
			super(m_context);
			m_event = event;
			m_indices = indices;
			m_elements = elements;
			synchronized (FieldSetPart.this) {
				m_eventCount++;
			}
		}
		
		public void run() {
			if (m_dataConsumer == null) {
				return;
			}
			
			switch (m_event) {
			case s_elementsAdded:
				processElementsAdded(m_index, m_elements);
				break;
			case s_elementsRemoved:
				processElementsRemoved(m_index, m_elements);
				break;
			case s_elementsChanged:
				processElementsChanged(m_indices, m_elements);
				break;
			case s_listCleared:
				processListCleared();
				break;
			}
			
			synchronized (FieldSetPart.this) {
				m_eventCount--;
			}
		}
	}
	int m_eventCount = 0;

	protected void onElementsAdded(int index, int count) {
		if (m_dataProviderWrapper == null) {
			setupDataSource();
		}
		
		try {
			List newElements = m_dataProviderWrapper.getElements(index, count);
			
			if (Ozone.isUIThread() && m_eventCount == 0) {
				processElementsAdded(index, newElements);
			} else {
				Ozone.idleExec(new ElementsEvent(s_elementsAdded, index, newElements));
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("List data not available when notification of added elements received", e);
		} catch (DataMismatchException e) {
			s_logger.error("List data not provided by data provider", e);
		}
	}
	
	protected void onElementsRemoved(int index, int count, List removedElements) {
		if (Ozone.isUIThread() && m_eventCount == 0) {
			processElementsRemoved(index, removedElements);
		} else {
			Ozone.idleExec(new ElementsEvent(s_elementsRemoved, index, removedElements));
		}
	}
	
	protected void onElementsChanged(int index, int count) {
		if (m_dataProviderWrapper == null) {
			setupDataSource();
		}
		
		try {
			List changedElements = m_dataProviderWrapper.getElements(index, count);
			List changedIndices = new ArrayList();
			
			for (int i = 0; i < count; i++) {
				changedIndices.add(new Integer(index + i));
			}

			if (Ozone.isUIThread() && m_eventCount == 0) {
				processElementsChanged(changedIndices, changedElements);
			} else {
				Ozone.idleExec(new ElementsEvent(s_elementsAdded, changedIndices, changedElements));
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("List data not available when notification of changed elements received", e);
		} catch (DataMismatchException e) {
			s_logger.error("List data not provided by data provider", e);
		}
	}
	
	protected void onElementsChanged(List changedIndices) {
		if (m_dataProviderWrapper == null) {
			setupDataSource();
		}
		
		try {
			ArrayList changedElements = new ArrayList();
			
			Iterator i = changedIndices.iterator();
			while (i.hasNext()) {
				Integer index = (Integer) i.next();
				changedElements.add(m_dataProviderWrapper.getElement(index.intValue()));
			}

			if (Ozone.isUIThread() && m_eventCount == 0) {
				processElementsChanged(changedIndices, changedElements);
			} else {
				Ozone.idleExec(new ElementsEvent(s_elementsAdded, changedIndices, changedElements));
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("List data not available when notification of changed elements received", e);
		} catch (DataMismatchException e) {
			s_logger.error("List data not provided by data provider", e);
		}
	}
	
	protected void onListCleared() {
		if (Ozone.isUIThread() && m_eventCount == 0) {
			processListCleared();
		} else {
			Ozone.idleExec(new ElementsEvent(s_listCleared, null, null));
		}
	}
	
	static final Resource s_uiOfFieldID = new Resource(LayoutConstants.s_namespace + "uiOfFieldID"); 
	static final Resource s_generatedBy = new Resource(LayoutConstants.s_namespace + "generatedBy");
	 
	protected void processElementsAdded(int index, List addedElements) {
		Interpreter interpreter = Ozone.getInterpreter();
			
		for (int i = 0; i < addedElements.size(); i++) {
			try {
				FieldData	fieldData = (FieldData) addedElements.get(i);
				Resource	childPartData = null;
				IVisualPart visualPart;		
				if ((m_cachedFieldParts != null) && m_cachedFieldParts.containsKey(fieldData.m_fieldID)) {
					visualPart = (IVisualPart) m_cachedFieldParts.remove(fieldData.m_fieldID);
					visualPart.initializeFromDeserialization(m_source); 
				} else {
					if (m_fieldPartDataMap != null) {
						childPartData = (Resource) m_fieldPartDataMap.get(fieldData.m_fieldID);
					} else {
						RDFNode[] nodes = m_source.queryExtract(new Statement[] {
								new Statement(Utilities.generateWildcardResource(1), s_uiOfFieldID, fieldData.getFieldID()),
								new Statement(Utilities.generateWildcardResource(1), s_generatedBy, m_fieldPartDataGenerator)
							},
							Utilities.generateWildcardResourceArray(1),
							Utilities.generateWildcardResourceArray(1)
						);
						childPartData = (nodes != null && nodes.length > 0) ? (Resource) nodes[0] : null;
						
						if (childPartData == null) {
							DynamicEnvironment denv = new DynamicEnvironment(m_source);
							Ozone.initializeDynamicEnvironment(denv, m_context);
							
							childPartData = (Resource) interpreter.callMethod(m_fieldPartDataGenerator, new Object[] { fieldData.getFieldData(), fieldData.getFieldID() }, denv);
						}
					}
					
					visualPart = (IVisualPart) PartCache.deserialize(childPartData, this, m_context, m_source);
					if (visualPart == null) {
						Resource	part = Ozone.findPart(childPartData, m_source, m_partDataSource);
						Class		c = Utilities.loadClass(part, m_source);
						visualPart = (IVisualPart) c.newInstance();
						Context		childContext = new Context(m_context);
					
						childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
						childContext.putLocalProperty(OzoneConstants.s_partData, childPartData);
						childContext.putLocalProperty(OzoneConstants.s_part, part);
					
						visualPart.initialize(m_source, childContext);
						
						PartCache.serialize(childPartData, this, m_context, visualPart, childContext);
					}
				}
				
				m_childParts.add(index + i, visualPart);
				m_fields.add(index + i, fieldData);
				m_childData.add(index + i, new FieldLocation(fieldData.getDimension(), calculateChildPartSize(visualPart, fieldData.getDimension()), fieldData.isResizable()));
			} catch (Exception e) {
				s_logger.error("Failed to process newly added element", e);
			}
		}
		
		if (!m_initializing) {
			layoutChildParts(true, true);
		}
	}
	
	protected void processElementsRemoved(int index, List removedElements) {
		for (int i = 0; i < removedElements.size(); i++) {
			IVisualPart vp = (IVisualPart) m_childParts.remove(index);
			
			vp.dispose();
			
			m_childData.remove(index);
			m_fields.remove(index);
		}
		
		if (!m_initializing) {
			layoutChildParts(false, true);
		}
	}
	
	protected void processElementsChanged(List changedIndices, List changedElements) {
		Iterator	iIndex = changedIndices.iterator();
		Iterator	iFieldData = changedElements.iterator();
		
		while (iIndex.hasNext()) {
			Integer		index = (Integer) iIndex.next();
			FieldData	fieldData = (FieldData) iFieldData.next();
			IVisualPart	vp = (IVisualPart) m_childParts.get(index.intValue());
			
			m_childData.set(index.intValue(), new FieldLocation(fieldData.getDimension(), calculateChildPartSize(vp, fieldData.getDimension()), fieldData.isResizable()));
			m_fields.set(index.intValue(), fieldData);
		}
		
		if (!m_initializing) {
			layoutChildParts(true, true);
		}
	}
	
	protected void processListCleared() {
		while (m_childParts.size() > 0) {
			IVisualPart vp = (IVisualPart) m_childParts.remove(0);
			vp.dispose();
			m_childData.remove(0);
		}
		
		if (!m_initializing) {
			layoutChildParts(false, true);
		}
	}
	
	protected BlockScreenspace calculateChildPartSize(IVisualPart vp, int width) {
		IBlockGUIHandler bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
		BlockScreenspace bs = null;
		
		if (bgh != null) {
			switch (bgh.getHintedDimensions()) {
			case IBlockGUIHandler.FIXED_SIZE:
				bs = bgh.getFixedSize();
				break;
			case IBlockGUIHandler.WIDTH:
				bs = bgh.calculateSize(width, -1);
				break;
			case IBlockGUIHandler.BOTH:
			case IBlockGUIHandler.HEIGHT:
				bs = bgh.calculateSize(width, width / 3); // TODO[dfhuynh]: what's a good height to suggest?
				break;
			}
		}
				
		if (bs != null) {
			bs.m_size.x = width;
		} else {
			bs = new BlockScreenspace(width, 0);
		}
		
		return bs;
	}
	
	protected void layoutChildParts(boolean setBounds, boolean recalculate) {
		m_bs.m_alignOffset = 0;
		m_bs.m_size.y = 0;

		for (int i = 0; i < m_childData.size(); i++) {
			FieldLocation fl = (FieldLocation) m_childData.get(i);
			
			if (recalculate) {
				IVisualPart vp = (IVisualPart) m_childParts.get(i);
				
				fl.m_bs = calculateChildPartSize(vp, fl.m_dimension);
				
			}
			if (setBounds) {
				fl.m_rect.width = fl.m_bs.m_size.x;
				fl.m_rect.height = fl.m_bs.m_size.y;
			}
			
			if (fl.m_bs.m_align == BlockScreenspace.ALIGN_TEXT_BASE_LINE) {
				m_bs.m_alignOffset = Math.max(m_bs.m_alignOffset, fl.m_bs.m_alignOffset);
			}
		}
		
		Point 		p = new Point(0, 0);
		
		for (int i = 0; i < m_childData.size(); i++) {
			FieldLocation	fl = (FieldLocation) m_childData.get(i);
			IVisualPart		vp = (IVisualPart) m_childParts.get(i);
			
			if (fl.m_bs.m_align == BlockScreenspace.ALIGN_TEXT_BASE_LINE) {
				p.y = m_bs.m_alignOffset - fl.m_bs.m_alignOffset;
			} else {
				p.y = 0;
			}
			m_bs.m_size.y = Math.max(m_bs.m_size.y, p.y + fl.m_bs.m_size.y);
			
			if (setBounds) {
				if (p.x != fl.m_rect.x || p.y != fl.m_rect.y) {
					fl.m_rect.x = p.x;
					fl.m_rect.y = p.y;
					
					IBlockGUIHandler bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
					if (bgh != null) {
						bgh.setBounds(new Rectangle(m_rect.x + p.x, m_rect.y + p.y, fl.m_rect.width, fl.m_rect.height));
					}
				}
			}
			
			p.x += fl.m_bs.m_size.x;
		}
		
		m_bs.m_size.x = p.x;
		
		if (setBounds) {
			if (!m_initializing) {
				if (m_bs.m_size.y != m_rect.height) {
					super.onChildResize(new ChildPartEvent(this));
				} else {
					repaint(m_rect);
				}
			}
		}
	}

	protected boolean 	m_resizing = false;
	protected int			m_resizeElement = -1;
	protected Point		m_resizeInitialPoint;
	protected int			m_widthBeforeResize;
	
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (m_resizing) {
			return true;
		}
		
		if (eventType.equals(PartConstants.s_eventMouseMove) ||
			eventType.equals(PartConstants.s_eventMouseHover)) {
			int previousResizeElement = m_resizeElement;
			
			m_resizeElement = hittestResizeElement(event.x, event.y);
			if (m_resizeElement >= 0) {
				m_parent.setCursor(GraphicsManager.s_horzResizeCursor);
				return true;
			} else if (previousResizeElement >= 0) {
				m_parent.setCursor(null);
			}
		} else if (eventType.equals(PartConstants.s_eventMouseDown)) {
			m_resizeElement = hittestResizeElement(event.x, event.y);
			if (m_resizeElement >= 0) {
				m_parent.setCursor(GraphicsManager.s_horzResizeCursor);
				onResizeMouseDown(event);
				m_resizing = true;
				return true;
			}
		} else if (eventType.equals(PartConstants.s_eventMouseExit)) {
			m_parent.setCursor(null);
		}

		return super.onMouseEvent(eventType, event);
	}
	
	protected void onResizeMouseDown(MouseEvent e) {
		m_parent.setCursor(GraphicsManager.s_horzResizeCursor);
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
	
	protected void onStartResizing() {
		FieldLocation fl = (FieldLocation) m_childData.get(m_resizeElement);
		
		m_widthBeforeResize = fl.m_rect.width;
	}
	
	protected void onResizing(int x, int y) {
		int diff = x - m_resizeInitialPoint.x;
		int newWidth = m_widthBeforeResize + diff;
		
		try {
			m_dataProviderWrapper.requestChange(m_resizeElement, new Integer(newWidth));
		} catch (DataMismatchException e) {
		} catch (DataNotAvailableException e) {
		}
	}
	
	protected void onEndResizing() {
	}

	static final int s_resizeHotspotWidth = 3;
	protected int hittestResizeElement(int x, int y) {
		for (int i = 0; i < m_childData.size(); i++) {
			FieldLocation fl = (FieldLocation) m_childData.get(i);
			
			if (fl.m_resizable) {
				int border = m_rect.x + fl.m_rect.x + fl.m_rect.width;
				if (x <= border + s_resizeHotspotWidth && x >= border - s_resizeHotspotWidth) {
					return i;
				}
			}
		}
		return -1;
	}
	
	protected boolean onDragEnter(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		event.m_dropTargetEvent.detail = m_defaultDropOperation;
		return m_defaultDropTargetResult;
	}

	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertRect != null) {
			m_parent.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			m_insertRect = null;
		}
		return super.onDragExit(event);
	}

	protected void onChildHasHandledDragAndDropEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertRect != null) {
			m_parent.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			m_insertRect = null;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragHover(DropTargetEvent)
	 */
	private Rectangle m_insertRect;
	protected boolean onDragHover(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		Rectangle r = calcDropRect(event.m_x, event.m_y);
		
		if (r == null || !r.equals(m_insertRect)) {
			if (m_insertRect != null) {
				m_parent.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			}
			if (r != null) {
				m_parent.redraw(r.x, r.y, r.width, r.height, true);
			}
			m_insertRect = r;
		}
		return super.onDragHover(event);
	}
	protected Rectangle calcDropRect(int x, int y) {
		int i = determineAdditionIndex(x, y);
		if (i < 0) {
			return null;
		} else if (i == m_childData.size()) {
			return new Rectangle(
					m_rect.x + m_rect.width - s_resizeHotspotWidth, 
					m_rect.y,
					s_resizeHotspotWidth * 2, 
					m_rect.height);
		} else {
			FieldLocation fl = (FieldLocation) m_childData.get(i);
			return new Rectangle(
				m_rect.x + fl.m_rect.x - s_resizeHotspotWidth, 
				m_rect.y,
				s_resizeHotspotWidth * 2, 
				m_rect.height);
		}
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
	
	protected int determineAdditionIndex(int x, int y) {
		x = x - m_rect.x;
		for (int i = 0; i < m_childData.size(); i++) {
			FieldLocation fl = (FieldLocation) m_childData.get(i);
			
			if (x < fl.m_rect.x + s_resizeHotspotWidth) {
				return i;
			}
		}
		return m_childData.size();
	}
	
	protected boolean onDropResourceList(java.util.List resourceList, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		try {
			m_dataProviderWrapper.requestAddition(resourceList.get(0), determineAdditionIndex(event.m_x, event.m_y));
			return true;
		} catch (Exception e) {
			s_logger.error("Data mismatch on drop", e);
		}
		return false;
	}

	protected boolean onDropResources(java.util.List resources, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		try {
			m_dataProviderWrapper.requestAdditions(resources, determineAdditionIndex(event.m_x, event.m_y));
			return true;
		} catch (Exception e) {
			s_logger.error("Data mismatch on drop", e);
		}
		return false;
	}
}
