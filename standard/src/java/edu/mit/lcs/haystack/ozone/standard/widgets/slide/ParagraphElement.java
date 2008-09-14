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

package edu.mit.lcs.haystack.ozone.standard.widgets.slide;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.CSSstyle;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.ITextSpan;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.*;
import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class ParagraphElement extends ContainerPartBase implements IBlockGUIHandler {
	protected TextFlowCounter	m_counter;
	protected boolean			m_highlightBackground = false;
	
	protected boolean			m_needsRecalculation = false;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ParagraphElement.class);
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		m_counter = null;
		super.dispose();
	}
	
	protected void internalInitialize() {
		super.internalInitialize();
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		getAlignmentProperties();
		initializeChildren();
	}
	
	protected void getAlignmentProperties() {
		int horzAlign = SlideUtilities.getAmbientAlignX(m_context);
		int vertAlign = SlideUtilities.getAmbientAlignY(m_context);
		m_counter = new TextFlowCounter(horzAlign, vertAlign);
		m_CSSstyle.setAttribute("text-align", CSSstyle.int2AlignX(horzAlign));
		m_CSSstyle.setAttribute("vertical-align", CSSstyle.int2AlignY(vertAlign));
	}
	
	protected void initializeChildren() {
		Iterator i = SlidePart.getChildren(m_partDataSource, m_prescription);
		int nChildren = 0;
		while (i.hasNext()) {
			nChildren++;
			Resource r = (Resource) i.next();
			try {
				Context	childContext = new Context(m_context);
				IPart p = null;
				
				Resource resPart = Ozone.findPart(r, m_source, m_partDataSource);
				if (resPart == null) s_logger.error("Could not find part for " + r);
				Class c = Utilities.loadClass(resPart, m_source);
				
				childContext.putLocalProperty(OzoneConstants.s_part, resPart);
				childContext.putLocalProperty(OzoneConstants.s_partData, r);
				childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
				
				p = (IPart)c.newInstance();
				p.initialize(m_source, childContext);
				
				if (p instanceof IVisualPart) m_childParts.add(p);
				else m_otherChildParts.add(p);
			} catch (Exception e) {  s_logger.error("Failed to initialize child prescription " + r, e); }
		}
		if (nChildren == 0) {
			s_logger.warn("Paragraph element has no children");
		}
	}
	
	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle rect) {
		
		recalculateExtent(rect.width, rect.height, false);
		m_counter.setOrigin(new Point(rect.x, rect.y));
		cacheBounds(rect);
		Region 		region = new Region();
		gc.getClipping(region);
		Iterator 	iVisualPart = m_childParts.iterator();
		Iterator 	iSpanSet = m_counter.getSpanSets().iterator();
		if (m_highlightBackground) {
			Color color = SlideUtilities.getAmbientHighlightBgcolor(m_context);
			Color background = gc.getBackground();
			gc.setBackground(color);
			while (iSpanSet.hasNext()) {
				java.util.List	spanSet = (java.util.List) iSpanSet.next();
				Iterator		i = spanSet.iterator();
				while (i.hasNext()) {
					ITextSpan	textSpan = (ITextSpan) i.next();
					Rectangle	r = textSpan.getArea();
					if (region.intersects(r)) gc.fillRectangle(r);
				}
			}
			gc.setBackground(background);
			iSpanSet = m_counter.getSpanSets().iterator();
		}
		
		//gc.setClipping(rect);
		while (iVisualPart.hasNext()) {
			try {
				IVisualPart			vp = (IVisualPart) iVisualPart.next();
				IGUIHandler			guiHandler = vp.getGUIHandler(null);
				java.util.List		spanSet = (java.util.List) iSpanSet.next();
				if (guiHandler instanceof IInlineGUIHandler) {
					boolean 		intersects = false;
					Iterator 		i = spanSet.iterator();
					while (i.hasNext()) {
						ITextSpan textSpan = (ITextSpan) i.next();
						if (region.intersects(textSpan.getArea())) {
							intersects = true;
							break;
						}
					}
					if (intersects) {
						Region region2 = new Region();
						i = spanSet.iterator();
						while (i.hasNext()) {
							ITextSpan textSpan = (ITextSpan) i.next();
							region2.add(textSpan.getArea().intersection(rect));
						}
						gc.setClipping(region2);
						((IInlineGUIHandler) guiHandler).draw(gc, spanSet);
						gc.setClipping(region);
						region2.dispose();
					}
				} else if (guiHandler instanceof IBlockGUIHandler) {
					try {
						Rectangle	r = ((ITextSpan) spanSet.get(0)).getArea();
						if (region.intersects(r)) {
							Rectangle r2 = rect.intersection(r);
							
							//gc.setClipping(r2);
							((IBlockGUIHandler) guiHandler).draw(gc, r2);
							//gc.setClipping(rect);
						}
					} catch (Exception e) { s_logger.error("Failed to draw block child part", e); }
				}
			} catch (Exception e) { s_logger.error("Failed to draw child part", e); }
		}
		
		//gc.setClipping(region);
		region.dispose();
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.paragraphStart(m_CSSstyle, this, m_tooltip, "ParagraphElement");
		Iterator 	iVisualPart = m_childParts.iterator();
		Iterator 	iSpanSet = m_counter.getSpanSets().iterator();
		while (iVisualPart.hasNext()) {
			IVisualPart	vp = (IVisualPart)iVisualPart.next();
			IGUIHandler	guiHandler = vp.getGUIHandler(null);
			java.util.List	spanSet = (java.util.List)iSpanSet.next();
			if (guiHandler instanceof IInlineGUIHandler) ((IInlineGUIHandler)guiHandler).renderHTML(he);
			else if (guiHandler instanceof IBlockGUIHandler) ((IBlockGUIHandler)guiHandler).renderHTML(he);
		}
		he.paragraphEnd("ParagraphElement");
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		recalculateExtent(hintedWidth, hintedHeight, true);
		if (hintedHeight >= 0) m_counter.setHeight(hintedHeight);
		return m_counter.getPreferredSize();
	}
	
	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		recalculateExtent(r.width, r.height, false);
		m_counter.setOrigin(new Point(r.x, r.y));
		cacheBounds(r);
		while (true) {
			// We need to move some children around.
			java.util.List	spanSets = m_counter.getSpanSets();
			Iterator 		iVisualPart = m_childParts.iterator();
			if (spanSets != null) {
				Iterator iSpanSet = spanSets.iterator();
				while (iVisualPart.hasNext()) {
					try {
						IVisualPart 		vp = (IVisualPart) iVisualPart.next();
						IGUIHandler			guiHandler = vp.getGUIHandler(null);
						java.util.List		spanSet = (java.util.List) iSpanSet.next();
						if (guiHandler instanceof IInlineGUIHandler) {
							// Inline elements don't need to be moved.
						} else if (guiHandler instanceof IBlockGUIHandler) {
							if (spanSet.isEmpty())
								s_logger.warn("Empty span set in paragraph element");
							else {
								Rectangle	r3 = ((ITextSpan) spanSet.get(0)).getArea();
								((IBlockGUIHandler) guiHandler).setBounds(r3.intersection(r));
							}
						}
					} catch (Exception e) { s_logger.info("Failed to set bounds of child part", e); }
				}
			}
			break;
		}
	}
	
	/**
	 * @see VisualPartBase#onChildResize(ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		m_needsRecalculation = true;
		return super.onChildResize(e);
	}
	
	protected IVisualPart hittest(int x, int y) {
		m_counter.setHeight(m_rect.height);
		IVisualPart 	vp = null;
		IGUIHandler		sn = null;
		Iterator 		iVisualPart = m_childParts.iterator();
		java.util.List	spanSets = m_counter.getSpanSets();
		if (spanSets == null) return null;
		Iterator 		iSpanSet = spanSets.iterator();
		boolean			done = false;
		while (iVisualPart.hasNext() && !done) {
			java.util.List spanSet = (java.util.List) iSpanSet.next();
			vp = (IVisualPart) iVisualPart.next();
			sn = vp.getGUIHandler(null);
			if (sn instanceof IInlineGUIHandler) {
				Iterator	iTextSpan = spanSet.iterator();
				int		index = 0;
				while (iTextSpan.hasNext()) {
					ITextSpan textSpan = (ITextSpan) iTextSpan.next();
					if (textSpan.getArea().contains(x, y)) {
						done = true;
						break;
					}
				}
			} else if (sn instanceof IBlockGUIHandler && spanSet != null && spanSet.size() > 0) {
				ITextSpan	textSpan = (ITextSpan) spanSet.get(0);
				Rectangle	rect = textSpan.getArea();
				if (rect.contains(x, y)) done = true;
			}
		}
		if (!done) vp = null;
		return vp;
	}
	
	private void recalculateExtent(int newWidth, int newHeight, boolean force) {
		if (newWidth != m_counter.getWidth() || force || m_needsRecalculation) {
			m_counter.beginCounter(newWidth, newHeight);
			Iterator i = m_childParts.iterator();
			while (i.hasNext()) {
				m_counter.beginSegmentSet();
				try {
					IVisualPart vp = (IVisualPart) i.next();
					IGUIHandler	guiHandler = vp.getGUIHandler(null);
					if (guiHandler instanceof IInlineGUIHandler) {
						((IInlineGUIHandler) guiHandler).calculateTextFlow(m_counter);
					} else if (guiHandler instanceof IBlockGUIHandler) {
						IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) guiHandler;
						BlockScreenspace bs = null;
						if (blockGUIHandler.getHintedDimensions() == IBlockGUIHandler.FIXED_SIZE) {
							if (blockGUIHandler.getTextAlign() == BlockScreenspace.ALIGN_TEXT_CLEAR) m_counter.addLineBreak();
							bs = blockGUIHandler.getFixedSize();
							if (bs.m_size.x > m_counter.getRemainingLineLength()) m_counter.addLineBreak();
						} else if (blockGUIHandler.getTextAlign() == BlockScreenspace.ALIGN_TEXT_CLEAR) {
							m_counter.addLineBreak();
							bs = blockGUIHandler.calculateSize(newWidth, -1);
						} else {
							bs = blockGUIHandler.calculateSize(m_counter.getRemainingLineLength(), -1);
							if (bs != null && bs.m_size.x > m_counter.getRemainingLineLength()) {
								m_counter.addLineBreak();
								bs = blockGUIHandler.calculateSize(newWidth, -1);
							}
						}
						if (bs != null) {
							m_counter.addSpan(bs);
							if (bs.m_align == BlockScreenspace.ALIGN_TEXT_CLEAR) m_counter.addLineBreak();
						}
					}
				} catch (Exception e) { s_logger.info("Failed to calculate extent", e); }
				m_counter.endSegmentSet();
			}
			m_counter.endCounter();
			m_needsRecalculation = false;
		} else m_counter.setHeight(newHeight);
	}
	
	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}
	
	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.WIDTH;
	}
	
	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_BASE_LINE;
	}
	
	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) return this;
		return null;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHighlight(java.util.EventObject, boolean)
	 */
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		m_highlightBackground = event.m_highlight;
		PartUtilities.repaint(m_rect, m_context);
		return true;
	}
}

