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
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler;
import edu.mit.lcs.haystack.ozone.core.INavigationMaster;
import edu.mit.lcs.haystack.ozone.core.ITextFlowCounter;
import edu.mit.lcs.haystack.ozone.core.ITextSpan;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataProviderConsumerConnection;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.ViewContainerPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SpanTextFlowCounter;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author David Huynh
 */
public class InlineLayoutManager extends ListLayoutManagerBase implements IInlineGUIHandler {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(InlineLayoutManager.class);
	
	protected int	m_maxVisibleCount;
	protected int	m_widthOfSeparator;
	
	protected CustomSpanTextFlowCounter m_counter = new CustomSpanTextFlowCounter();
	
	class Element implements Serializable {
		public Resource		m_underlying;
		public IVisualPart		m_vp;
		public List			m_spanSet;
	}
	protected ArrayList		m_elements = new ArrayList();
	protected ArrayList		m_visibleItems = new ArrayList();
	protected ArrayList		m_items = new ArrayList();
	
	protected boolean			m_highlightBackground = false;
	protected List			m_spanSet;
	
	protected Resource		m_separatorPartData;
	protected Resource		m_lastSeparatorPartData;
	protected Resource		m_moreItemsPartData;
	protected Resource		m_noItemsPartData;

	protected int	m_itemsCountOverride = -1;	// support overriding default count
	protected DataProviderConsumerConnection m_itemsCountOverrideDPCC;

	static final Resource	s_visibleCount = new Resource(LayoutConstants.s_namespace + "visibleCount");
	static final Resource	s_invisibleCount = new Resource(LayoutConstants.s_namespace + "invisibleCount");
	static final Resource	s_totalCount = new Resource(LayoutConstants.s_namespace + "totalCount");
	
	static final Resource	s_separatorPartData = new Resource(LayoutConstants.s_namespace + "separatorPartData");
	static final Resource	s_lastSeparatorPartData = new Resource(LayoutConstants.s_namespace + "lastSeparatorPartData");
	static final Resource	s_moreItemsPartData = new Resource(LayoutConstants.s_namespace + "moreItemsPartData");
	static final Resource	s_noItemsPartData = new Resource(LayoutConstants.s_namespace + "noItemsPartData");

	synchronized public void dispose() {
		disposeDataConsumers();
		
		for (int i = 0; i < m_elements.size(); i++) {
			((Element) m_elements.get(i)).m_vp.dispose();
		}
		
		m_elements.clear();
		m_elements = null;
		
		m_visibleItems.clear();
		m_visibleItems = null;
		
		m_items.clear();
		m_items = null;
		
		m_counter = null;

		if (m_itemsCountOverrideDPCC != null) {
			m_itemsCountOverrideDPCC.dispose();
		}
		m_itemsCountOverrideDPCC = null;
		
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element) i.next();
			if (e.m_vp != null) {
				e.m_vp.initializeFromDeserialization(source);
			}
		}

		m_itemsCountOverrideDPCC = new DataProviderConsumerConnection(m_context, m_source);
		if (m_itemsCountOverrideDPCC.m_provider != null) {
			m_itemsCountOverrideDPCC.m_provider.initializeFromDeserialization(source);
		}
	}
	
	protected void internalInitialize() {
		super.internalInitialize();
		
		Resource viewPartClass = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_viewPartClass, m_partDataSource);
		if (viewPartClass != null) {
			m_context.putProperty(OzoneConstants.s_viewPartClass, viewPartClass);
		}
		
		String s = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_maxVisibleCount, m_partDataSource);
		if (s != null) {
			m_maxVisibleCount = Math.max(0, Integer.parseInt(s));
		}

		Resource itemsCountOverrideDataSource =
			Utilities.getResourceProperty(m_prescription, LayoutConstants.s_itemsCountCollection, m_partDataSource);
		if (itemsCountOverrideDataSource != null) {
			m_itemsCountOverrideDPCC = new DataProviderConsumerConnection(m_context, m_source, m_partDataSource);
			m_itemsCountOverrideDPCC.connect(itemsCountOverrideDataSource, new IDataConsumer() {
				public void reset() {}
				public void onStatusChanged(Resource status) {}
				public void onDataChanged(Resource changeType, Object change) throws IllegalArgumentException {
					Integer val = null;
					try {
						val = (Integer) m_itemsCountOverrideDPCC.m_provider.getData(DataConstants.SET_COUNT, null);
					} catch (DataNotAvailableException e) {
						return;
					}
					if (val != null) {
						m_itemsCountOverride = val.intValue();
						IdleRunnable runnable = new IdleRunnable(m_context) {
							public void run() {
								if (!m_initializing) {
									reconstructElementList();
								}
							}
						};
						Ozone.idleExec(runnable);
					}
				}
			});
			m_itemsCountOverrideDPCC.m_consumer.onDataChanged(null,null); //force update
		}

		// TODO[vineet] delete 4 lines below and see if it still works		
		s = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_itemsCountCollection, m_partDataSource);
		if (s != null) {
			m_itemsCountOverride = Math.max(0, Integer.parseInt(s));
		}
		
		Resource r = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_separator, m_partDataSource);
		m_separatorPartData = r != null ? r : s_separatorPartData;

		r = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_lastSeparator, m_partDataSource);
		m_lastSeparatorPartData = r != null ? r : s_lastSeparatorPartData;

		r = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_moreItems, m_partDataSource);
		m_moreItemsPartData = r != null ? r : s_moreItemsPartData;

		r = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_noItems, m_partDataSource);
		m_noItemsPartData = r != null ? r : s_noItemsPartData;
		
		m_context.putProperty(LayoutConstants.s_layoutManager, this);
		
		makeDataConsumers();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ILayoutManager#getGUIHandler(java.lang.Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IInlineGUIHandler.class)) {
			return this;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#handleGUIEvent(edu.mit.lcs.haystack.rdf.Resource, java.util.EventObject)
	 */
	public boolean handleGUIEvent(Resource eventType, EventObject event) {
		return internalHandleGUIEvent(eventType, (EventObject) event);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		Iterator i = m_elements.iterator();
		
		while (i.hasNext()) {
			Element elmt = (Element) i.next();
			
			IGUIHandler guih = elmt.m_vp.getGUIHandler(null);
			if (guih != null) {
				guih.setVisible(visible);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler#calculateTextFlow(edu.mit.lcs.haystack.ozone.ITextFlowCounter)
	 */
	public void calculateTextFlow(ITextFlowCounter tfc) {
		m_counter.beginCounter(tfc);
		{
			Iterator i = m_elements.iterator();
			while (i.hasNext()) {
				Element elmt = (Element) i.next();
				
				m_counter.setLineLengthShortening(elmt.m_underlying != null ? m_widthOfSeparator : 0);
					
				m_counter.beginSegmentSet();
				try {
					IGUIHandler	guiHandler = elmt.m_vp.getGUIHandler(null);
					
					if (guiHandler instanceof IInlineGUIHandler) {
						((IInlineGUIHandler) guiHandler).calculateTextFlow(m_counter);
					} else if (guiHandler instanceof IBlockGUIHandler) {
						IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) guiHandler;
						BlockScreenspace	bs = null;
						int 				align = blockGUIHandler.getTextAlign();
						
						switch (blockGUIHandler.getHintedDimensions()) {
						case IBlockGUIHandler.FIXED_SIZE:
							bs = blockGUIHandler.getFixedSize();
							if (bs.m_align == BlockScreenspace.ALIGN_TEXT_CLEAR) {
								m_counter.addLineBreak();
								m_counter.addSpan(bs);
								m_counter.addLineBreak();
							} else {
								if (bs.m_size.x > m_counter.getRemainingLineLength()) {
									m_counter.addLineBreak();
								}
								m_counter.addSpan(bs);
							}
							break;
							
						case IBlockGUIHandler.WIDTH:
							if (align == BlockScreenspace.ALIGN_TEXT_CLEAR) {
								bs = blockGUIHandler.calculateSize(m_counter.getLineLength(), -1);
								
								m_counter.addLineBreak();
								m_counter.addSpan(bs);
								m_counter.addLineBreak();
							} else {
								bs = blockGUIHandler.calculateSize(m_counter.getRemainingLineLength(), -1);
								if (bs.m_size.x <= m_counter.getRemainingLineLength()) {
									m_counter.addSpan(bs);
								} else {
									if (bs.m_size.x < m_counter.getLineLength()) {
										bs = blockGUIHandler.calculateSize(m_counter.getLineLength(), -1);
									}
									m_counter.addLineBreak();
									m_counter.addSpan(bs);
								}
							}
							break;
							
						case IBlockGUIHandler.HEIGHT:
							if (align == BlockScreenspace.ALIGN_TEXT_CLEAR) {
								bs = blockGUIHandler.calculateSize(m_counter.getLineLength(), m_counter.getAverageLineHeight());
								
								m_counter.addLineBreak();
								m_counter.addSpan(bs);
								m_counter.addLineBreak();
							} else {
								bs = blockGUIHandler.calculateSize(m_counter.getRemainingLineLength(), m_counter.getAverageLineHeight());
								if (bs.m_size.x <= m_counter.getRemainingLineLength()) {
									m_counter.addSpan(bs);
								} else {
									if (bs.m_size.x < m_counter.getLineLength()) {
										bs = blockGUIHandler.calculateSize(m_counter.getLineLength(), m_counter.getAverageLineHeight());
									}
									m_counter.addLineBreak();
									m_counter.addSpan(bs);
								}
							}
							break;
						}
					}
				} catch (Exception e) {
					s_logger.error("Failed to calculate text flow", e);
				}
				
				elmt.m_spanSet = m_counter.getCurrentSpanSet();
				m_counter.endSegmentSet();
			}
		}
		m_counter.endCounter();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler#draw(GC, List)
	 */
	public void draw(GC gc, List textSpans) {
		m_spanSet = textSpans;
		
		if (m_highlightBackground) {
			Color background = gc.getBackground();
			
			gc.setBackground(SlideUtilities.getAmbientHighlightBgcolor(m_context));
			
			Iterator j = textSpans.iterator();
			
			while (j.hasNext()) {
				ITextSpan textSpan = (ITextSpan) j.next();
				
				gc.fillRectangle(textSpan.getArea());
			}
			
			gc.setBackground(background);
		}
		
		Iterator i = m_elements.iterator();
		
		while (i.hasNext()) {
			try {
				Element		elmt = (Element) i.next();
				IGUIHandler guiHandler = elmt.m_vp.getGUIHandler(null);
				
				if (elmt.m_spanSet != null && guiHandler != null) {
					if (guiHandler instanceof IInlineGUIHandler) {
						((IInlineGUIHandler) guiHandler).draw(gc, elmt.m_spanSet);
					} else if (guiHandler instanceof IBlockGUIHandler && elmt.m_spanSet.size() > 0) {
						Rectangle r = ((ITextSpan) elmt.m_spanSet.get(0)).getArea();
						
						((IBlockGUIHandler) guiHandler).draw(gc, r);
					}
				}
			} catch (Exception e) {
				s_logger.error("Failed to draw child part", e);
				break;
			}
		}
	}

	/**
	 * @see IInlineGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("InlineLayoutManager");
		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
		  try {
		    Element		elmt = (Element) i.next();
		    IGUIHandler guiHandler = elmt.m_vp.getGUIHandler(null);
		    if (elmt.m_spanSet != null && guiHandler != null) {
		      if (guiHandler instanceof IInlineGUIHandler) {
			((IInlineGUIHandler) guiHandler).renderHTML(he);
		      } else if (guiHandler instanceof IBlockGUIHandler && elmt.m_spanSet.size() > 0) {
			((IBlockGUIHandler) guiHandler).renderHTML(he);
		      }
		    }
		  } catch (Exception e) {
		    s_logger.error("Failed to render child part", e);
		    break;
		  }
		}
		he.exit("InlineLayoutManager");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsAdded(int, java.util.List)
	 */
	protected void processElementsAdded(int index, List addedElements) {
		m_items.addAll(index, addedElements);
		
		if (m_maxVisibleCount > 0) {
			if (index < m_maxVisibleCount) {
				int actualCount = Math.min(m_maxVisibleCount - index, addedElements.size());
				
				for (int i = 0; i < actualCount; i++) {
					Element elmt = new Element();
					
					elmt.m_underlying = (Resource) addedElements.get(i);
					elmt.m_vp = createViewContainer(elmt.m_underlying);
					
					m_visibleItems.add(index + i, elmt);
				}
				
				int extraCount = m_visibleItems.size() - m_maxVisibleCount;
				
				while (extraCount > 0) {
					Element elmt = (Element) m_visibleItems.remove(m_maxVisibleCount);
					
					elmt.m_vp.dispose();
					
					extraCount--;
				}
			}
		} else {
			for (int i = 0; i < addedElements.size(); i++) {
				Element elmt = new Element();
				
				elmt.m_underlying = (Resource) addedElements.get(i);
				elmt.m_vp = createViewContainer(elmt.m_underlying);
				
				m_visibleItems.add(index + i, elmt);
			}
		}
		
		reconstructElementList();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsRemoved(int, java.util.List)
	 */
	protected void processElementsRemoved(int index, List removedElements) {
		for (int i = 0; i < removedElements.size(); i++) {
			m_items.remove(index);
		}
		
		if (m_maxVisibleCount > 0) {
			if (index < m_maxVisibleCount) {
				int actualCount = Math.min(m_maxVisibleCount - index, removedElements.size());
				
				for (int i = 0; i < actualCount; i++) {
					Element elmt = (Element) m_visibleItems.remove(index);
					
					elmt.m_vp.dispose();
				}
				
				for (int i = m_visibleItems.size(); i < Math.min(m_maxVisibleCount, m_items.size()); i++) {
					Element elmt = new Element();
					
					elmt.m_underlying = (Resource) m_items.get(i);
					elmt.m_vp = createViewContainer(elmt.m_underlying);
					
					m_visibleItems.add(elmt);
				}
			}
		} else {
			for (int i = 0; i < removedElements.size(); i++) {
				Element elmt = (Element) m_visibleItems.remove(index);
				
				elmt.m_vp.dispose();
			}
		}
		
		reconstructElementList();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processListCleared()
	 */
	protected void processListCleared() {
		Iterator i = m_visibleItems.iterator();
		while (i.hasNext()) {
			Element elmt = (Element) i.next();
			
			elmt.m_vp.dispose();
		}
		
		m_visibleItems.clear();
		m_items.clear();

		reconstructElementList();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragEnter(DropTargetEvent)
	 */
	protected boolean onDragEnter(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		m_highlightBackground = true;
		redraw();
		return super.onDragEnter(event);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragExit(DropTargetEvent)
	 */
	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		m_highlightBackground = false;
		redraw();
		return super.onDragExit(event);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#hittest
	 * (int, int, boolean)
	 */
	protected IVisualPart hittest(int x, int y, boolean favorParent) {
		int index = internalHittest(x, y);
		
		if (index < 0) {
			return null;
		} else {
			return ((Element) m_elements.get(index)).m_vp;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onContentHighlight(EventObject, boolean)
	 */
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		m_highlightBackground = event.m_highlight;
		redraw();
		return true;
	}

	protected IVisualPart createViewContainer(Resource underlying) {
		ViewContainerPart vcp = new ViewContainerPart(true);
		
		vcp.setNestingRelation(m_nestingRelation);

		Context	childContext = new Context(m_context);

		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		
		vcp.initialize(m_source, childContext);

		vcp.navigate(underlying);
		
		return vcp;
	}

	protected void reconstructElementList() {
		{
			Iterator i = m_elements.iterator();
			while (i.hasNext()) {
				Element elmt = (Element) i.next();
				
				if (elmt.m_underlying == null) {
					elmt.m_vp.dispose();
				}
			}
			m_elements.clear();
		}
		
		m_context.putProperty(s_visibleCount, new Literal(Integer.toString(m_visibleItems.size())));
		m_context.putProperty(s_invisibleCount, new Literal(Integer.toString(m_items.size() - m_visibleItems.size())));
		m_context.putProperty(s_totalCount, new Literal(Integer.toString(m_items.size())));
		
		if (m_items.size() == 0) {
			appendNoItem();
		} else {
			boolean showMore = m_visibleItems.size() < m_items.size();
			if (m_itemsCountOverride != -1) {
				showMore = m_visibleItems.size() < m_itemsCountOverride;
			}
			
			for (int i = 0; i < m_visibleItems.size(); i++) {
				m_elements.add(m_visibleItems.get(i));
				
				if (i < m_visibleItems.size() - 2) {
					appendSeparator();
				} else if (i == m_visibleItems.size() - 2) {
					if (showMore) {
						appendSeparator();
					} else {
						appendLastSeparator();
					}
				} else if (showMore) {
					appendLastSeparator();
				}
			}
			
			if (showMore) {
				appendMore();
			}
			
		}
		
		if (!m_initializing) {
			ChildPartEvent event = new ChildPartEvent(this);
			m_layoutPart.handleEvent(PartConstants.s_eventChildResize, event);
		}
	}
	
	protected void appendSeparator() {
		try {
			Element elmt = new Element();
			
			Resource	part = Ozone.findPart(m_separatorPartData, m_source, m_partDataSource);
			Class		c = Utilities.loadClass(part, m_source);
			
			elmt.m_vp = (IVisualPart) c.newInstance();
			
			Context childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, m_separatorPartData);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			elmt.m_vp.initialize(m_source, childContext);
			
			m_elements.add(elmt);
		} catch (Exception e) {
			s_logger.error("Failed to append separator in inline layout", e);
		}
	}
	
	protected void appendLastSeparator() {
		try {
			Element elmt = new Element();
			
			Resource	part = Ozone.findPart(m_lastSeparatorPartData, m_source, m_partDataSource);
			Class		c = Utilities.loadClass(part, m_source);
			
			elmt.m_vp = (IVisualPart) c.newInstance();
			
			Context childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, m_lastSeparatorPartData);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			elmt.m_vp.initialize(m_source, childContext);
			
			m_elements.add(elmt);
		} catch (Exception e) {
			s_logger.error("Failed to append last separator in inline layout", e);
		}
	}
	
	protected void appendMore() {
		try {
			Element elmt = new Element();
			
			Resource	part = Ozone.findPart(m_moreItemsPartData, m_source, m_partDataSource);
			Class		c = Utilities.loadClass(part, m_source);
			
			elmt.m_vp = (IVisualPart) c.newInstance();
			
			Context childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, m_moreItemsPartData);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			elmt.m_vp.initialize(m_source, childContext);
			
			m_elements.add(elmt);
		} catch (Exception e) {
			s_logger.error("Failed to append more part in inline layout", e);
		}
	}

	protected void appendNoItem() {
		try {
			Element elmt = new Element();
			
			Resource	part = Ozone.findPart(m_noItemsPartData, m_source, m_partDataSource);
			Class		c = Utilities.loadClass(part, m_source);
			
			elmt.m_vp = (IVisualPart) c.newInstance();
			
			Context childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, m_noItemsPartData);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			elmt.m_vp.initialize(m_source, childContext);
			
			m_elements.add(elmt);
		} catch (Exception e) {
			s_logger.error("Failed to append No Item in inline layout", e);
		}
	}
	
	protected void redraw() {
		if (m_spanSet == null) {
			return;
		}
		
		Rectangle	r = null;
		Iterator	i = m_spanSet.iterator();
		
		while (i.hasNext()) {
			ITextSpan	textSpan = (ITextSpan) i.next();
			Rectangle	area = textSpan.getArea();
			
			if (r == null) {
				r = area;
			} else {
				r = r.union(area);
			}
		}

		if (r != null) {
			PartUtilities.repaint(r, m_context);
		}
	}

	protected void prepareContextMenu(MouseEvent e) {
		removeContextOperations();
		
		int index = internalHittest(e.x, e.y);
		if (index >= 0) {
			Element elmt = (Element) m_elements.get(index);
			
			if (elmt.m_underlying != null && elmt.m_vp.handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, e.x, e.y))) {
				createRemoveContextOperation(elmt.m_underlying, index);
			}
		}
	}

	protected int internalHittest(int x, int y) {
		int			index = -1;
		IGUIHandler		guiHandler = null;
		
		for (int i = 0; i < m_elements.size() && index < 0; i++) {
			Element elmt = (Element) m_elements.get(i);
			
			try {
				guiHandler = elmt.m_vp.getGUIHandler(null);
				
				if (elmt.m_spanSet != null) {
					if (guiHandler instanceof IInlineGUIHandler) {
						Iterator	iTextSpan = elmt.m_spanSet.iterator();
						
						while (iTextSpan.hasNext()) {
							ITextSpan textSpan = (ITextSpan) iTextSpan.next();
							
							if (textSpan.getArea().contains(x, y)) {
								index = i;
							}
						}
					} else if (guiHandler instanceof IBlockGUIHandler && elmt.m_spanSet.size() > 0) {
						ITextSpan	textSpan = (ITextSpan) elmt.m_spanSet.get(0);
						Rectangle	rect = textSpan.getArea();
						
						if (rect.contains(x, y)) {
							index = i;
						}
					}
				}
			} catch (Exception ex) {
				s_logger.error("Failed internal hittest", ex);
			}
		}
			
		return index;
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDrop(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	protected boolean onDrop(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		m_highlightBackground = false;
		redraw();
		return super.onDrop(event);
	}

	public void browseToSource() {
		try {
			Resource resource = (Resource) m_dataProvider.getData(DataConstants.RESOURCE, null);
			
			if (resource != null) {
				INavigationMaster nm = (INavigationMaster) m_context.getProperty(OzoneConstants.s_navigationMaster);
				
				nm.requestViewing(resource);
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("Failed to browse to source of inline layout", e);
		}
	}
}

class CustomSpanTextFlowCounter extends SpanTextFlowCounter {
	int m_lineLengthShortening;
	
	public CustomSpanTextFlowCounter() {
		super(0, 0, 0, 0);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ITextFlowCounter#getLineLength()
	 */
	public int getLineLength() {
		return super.getLineLength() - m_lineLengthShortening;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ITextFlowCounter#getRemainingLineLength()
	 */
	public int getRemainingLineLength() {
		return super.getRemainingLineLength() - m_lineLengthShortening;
	}

	public void setLineLengthShortening(int v) {
		m_lineLengthShortening = v;
	}
}
