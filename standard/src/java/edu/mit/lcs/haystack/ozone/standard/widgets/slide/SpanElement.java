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
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.ITextFlowCounter;
import edu.mit.lcs.haystack.ozone.core.ITextSpan;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.*;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class SpanElement extends ContainerPartBase implements IInlineGUIHandler {
	protected SpanTextFlowCounter	m_counter;

	transient protected Color 		m_bgcolor = null;
	
	protected int			m_marginLeft = 0;
	protected int			m_marginRight = 0;
	protected int			m_marginTop = 0;
	protected int			m_marginBottom = 0;
	
	protected int			m_borderLeftWidth = 0;
	transient protected Color		m_borderLeftColor;
	protected int			m_borderRightWidth = 0;
	transient protected Color		m_borderRightColor;
	protected int			m_borderTopWidth = 0;
	transient protected Color		m_borderTopColor;
	protected int			m_borderBottomWidth = 0;
	transient protected Color		m_borderBottomColor;
	
	protected java.util.List	m_spanSet; // cached
	
	protected boolean		m_highlightBackground = false;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SpanElement.class);
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		
		GraphicsManager.releaseColor(m_bgcolor);
		GraphicsManager.releaseColor(m_borderLeftColor);
		GraphicsManager.releaseColor(m_borderRightColor);
		GraphicsManager.releaseColor(m_borderTopColor);
		GraphicsManager.releaseColor(m_borderBottomColor);
		
		m_bgcolor = null;
		m_borderLeftColor = null;
		m_borderRightColor = null;
		m_borderTopColor = null;
		m_borderBottomColor = null;
				
		m_counter = null;

		super.dispose();
	}
	
	/**
	 * @see VisualPartBase#getBounds()
	 */
	public Rectangle getBounds() {
		Rectangle	r = null;
		
		if (m_spanSet != null) {
			Iterator	iTextSpan = m_spanSet.iterator();
			ITextSpan	textSpan;
			Rectangle	r2;
			
			while (iTextSpan.hasNext()) {
				textSpan = (ITextSpan) iTextSpan.next();
				
				r2 = textSpan.getArea();
				if (r == null) {
					r = r2;
				} else {
					r = r.union(r2);
				}
			}
		}
		if (r != null) {
			return r;
		} else {
			return new Rectangle(0, 0, 0, 0);
		}
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IInlineGUIHandler.class)) {
			return this;
		}
		return null;
	}

	/**
	 * @see IInlineGUIHandler#calculateTextFlow(ITextFlowCounter)
	 */
	public void calculateTextFlow(ITextFlowCounter tfc) {
		// TODO[dfhuynh]: do something in event this is null
		if (m_counter == null) {
			return;
		}
		
		m_counter.beginCounter(tfc);
		{
			Iterator i = m_childParts.iterator();
			while (i.hasNext()) {
				m_counter.beginSegmentSet();
					
				try {
					IVisualPart	vp = (IVisualPart) i.next();
					IGUIHandler	guiHandler = vp.getGUIHandler(null);
					
					if (guiHandler instanceof IInlineGUIHandler) {
						((IInlineGUIHandler) guiHandler).calculateTextFlow(m_counter);
					} else if (guiHandler instanceof IBlockGUIHandler) {
						IBlockGUIHandler 	blockGUIHandler = (IBlockGUIHandler) guiHandler;
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
				m_counter.endSegmentSet();
			}
		}
		m_counter.endCounter();
	}

	/**
	 * @see IInlineGUIHandler#draw(GC, List)
	 */
	public void draw(GC gc, List textSpans) {
		m_spanSet = textSpans;
		{
			Color		oldBgcolor = gc.getBackground();
			Iterator 	iTextSpan = textSpans.iterator();
			
			// TODO[dfhuynh]: do something when this iterator is empty
			if (!iTextSpan.hasNext()) {
				return;
			}
			
			ITextSpan	textSpan = (ITextSpan) iTextSpan.next();
			boolean	firstRow = true;
			Rectangle	r;
			Rectangle	r2 = null;
			Color		background = m_highlightBackground ? SlideUtilities.getAmbientHighlightBgcolor(m_context) : m_bgcolor;
			Region		region = new Region();
			
			gc.getClipping(region);
						
			r = textSpan.getArea();
			
			while (true) {
				// Try to finish up the current row
				while (iTextSpan.hasNext()) {
					textSpan = (ITextSpan) iTextSpan.next();
					
					r2 = textSpan.getArea();
					
					if (r2.x < r.x + r.width) {
						break; // new row
					} else {
						r = r.union(r2);
						r2 = null;
					}
				}
				
				gc.setClipping(r);
				
				// Paint background if any
				if (background != null) {
					gc.setBackground(background);
					gc.fillRectangle(r);
				}
				
				// Paint top and bottom borders if any
				if (m_borderTopWidth > 0) {
					gc.setBackground(m_borderTopColor);
					gc.fillRectangle(r.x, r.y, r.width, m_borderTopWidth);
				}
				if (m_borderBottomWidth > 0) {
					gc.setBackground(m_borderBottomColor);
					gc.fillRectangle(r.x, r.y + r.height - m_borderBottomWidth, r.width, m_borderBottomWidth);
				}
				
				// Paint left border if any
				if (firstRow && m_borderLeftWidth > 0) {
					gc.setBackground(m_borderLeftColor);
					gc.fillRectangle(r.x, r.y, m_borderLeftWidth, r.height);
				}
				
				// New row
				if (r2 != null) {
					r = r2;
					r2 = null;
				} else {
					break;
				}
				
				firstRow = false;
			}
			
			// Last row: paint right border if any
			if (m_borderRightWidth > 0) {
				gc.setBackground(m_borderLeftColor);
				gc.fillRectangle(r.x + r.width - m_borderRightWidth, r.y, m_borderRightWidth, r.height);
			}
			
			gc.setBackground(oldBgcolor);
			gc.setClipping(region);
			region.dispose();
		}
		
		// Draw children
		{
			Iterator iPart = m_childParts.iterator();
			Iterator iSpanSet = m_counter.getSpanSets().iterator();
			
			while (iPart.hasNext()) {
				try {
					IVisualPart 	vp = (IVisualPart) iPart.next();
					java.util.List	spanSet = (java.util.List) iSpanSet.next();
					IGUIHandler		guiHandler = vp.getGUIHandler(null);
					
					if (guiHandler instanceof IInlineGUIHandler) {
						((IInlineGUIHandler) guiHandler).draw(gc, spanSet);
					} else if (guiHandler instanceof IBlockGUIHandler) {
						Rectangle r = ((ITextSpan) spanSet.get(0)).getArea();
						
						((IBlockGUIHandler) guiHandler).draw(gc, r);
					}
				} catch (Exception e) {
					s_logger.error("Failed to draw child", e);
					break;
				}
			}
		}
	}
	
	/**
	 * @see IInlineGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enterSpan("SpanElement", m_CSSstyle, this, m_tooltip);
		Iterator iPart = m_childParts.iterator();
		Iterator iSpanSet = m_counter.getSpanSets().iterator();
		while (iPart.hasNext()) {
		  try {
		    IVisualPart 	vp = (IVisualPart) iPart.next();
		    java.util.List	spanSet = (java.util.List) iSpanSet.next();
		    IGUIHandler		guiHandler = vp.getGUIHandler(null);
		    if (guiHandler instanceof IInlineGUIHandler) {
		      ((IInlineGUIHandler) guiHandler).renderHTML(he);
		    } else if (guiHandler instanceof IBlockGUIHandler) {
		      ((IBlockGUIHandler) guiHandler).renderHTML(he);
		    }
		  } catch (Exception e) {
		    s_logger.error("Failed to render child", e);
		    break;
		  }
		}
		he.exitSpan("SpanElement");
	}
	
	/**
	 * @see ContainerPartBase#hittest(EventObject, int, int)
	 */
	protected IVisualPart hittest(int x, int y) {
		IVisualPart 			vp = null;
		java.util.List			spanSet = null;
		IGUIHandler				guiHandler = null;
		
		Iterator 		iVisualPart = m_childParts.iterator();
		Iterator 		iSpanSet = m_counter.getSpanSets().iterator();
		boolean		done = false;
		
		while (iVisualPart.hasNext() && !done) {
			try {
				vp = (IVisualPart) iVisualPart.next();
				guiHandler = vp.getGUIHandler(null);
				spanSet = (java.util.List) iSpanSet.next();
				
				if (guiHandler instanceof IInlineGUIHandler) {
					Iterator	iTextSpan = spanSet.iterator();
					int			index = 0;
					
					while (iTextSpan.hasNext()) {
						ITextSpan textSpan = (ITextSpan) iTextSpan.next();
						
						if (textSpan.getArea().contains(x, y)) {
							done = true;
							break;
						}
					}
				} else if (guiHandler instanceof IBlockGUIHandler) {
					ITextSpan	textSpan = (ITextSpan) spanSet.get(0);
					Rectangle	rect = textSpan.getArea();
					
					if (rect.contains(x, y)) {
						done = true;
					}
				}
			} catch (Exception ex) {
				s_logger.error("Failed to hittest", ex);
				break;
			}
		}
			
		if (!done) {
			vp = null;
		}
		return vp;
	}

	/**
	 * @see ContainerPartBase#onMouseEvent(Resource, MouseEvent)
	 */	
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (eventType == PartConstants.s_eventMouseEnter) {
			handleMouseEnter(event);
		} else if (eventType == PartConstants.s_eventMouseExit) {
			handleMouseExit(event);
		}
		return super.onMouseEvent(eventType, event);
	}
	
	protected void handleMouseEnter(MouseEvent e) {
		m_spanSet = (java.util.List) e.data;
	}
	
	protected void handleMouseExit(MouseEvent e) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHighlight(java.util.EventObject, boolean)
	 */
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		m_highlightBackground = event.m_highlight;
		repaint();
		return true;
	}

	protected void internalInitialize() {
		super.internalInitialize();
		
		if (m_prescription == null) {
			m_prescription = m_resUnderlying;
		}
		
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		
		getMarginProperties();
		getBorderProperties();
		getOtherProperties();
		initializeChildren();
		
		m_counter = new SpanTextFlowCounter(m_marginLeft, m_marginRight, m_marginTop, m_marginBottom);
	}
	
	protected void getMarginProperties() {
		String	s;
		int		i;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_margin, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginLeft = i;
				m_marginRight = i;
				m_marginTop = i;
				m_marginBottom = i;
				m_CSSstyle.setAttribute("margin", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginX, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginLeft = i;
				m_marginRight = i;
				m_CSSstyle.setAttribute("margin-left", i);
				m_CSSstyle.setAttribute("margin-right", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginY, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginTop = i;
				m_marginBottom = i;
				m_CSSstyle.setAttribute("margin-top", i);
				m_CSSstyle.setAttribute("margin-bottom", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginLeft, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginLeft = i;
				m_CSSstyle.setAttribute("margin-left", i);			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginRight, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginRight = i;
				m_CSSstyle.setAttribute("margin-right", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginTop, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginTop = i;
				m_CSSstyle.setAttribute("margin-top", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginBottom, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginBottom = i;
				m_CSSstyle.setAttribute("margin-bottom", i);
			}
		}
	}

	protected void getBorderProperties() {
		String	s;
		int	borderWidth;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderWidth, m_partDataSource);
		if (s != null) {
			borderWidth = Integer.parseInt(s);
			
			if (borderWidth > 0) {
				m_borderLeftWidth = borderWidth;
				m_borderRightWidth = borderWidth;
				m_borderTopWidth = borderWidth;
				m_borderBottomWidth = borderWidth;
				m_CSSstyle.setAttribute("border-width", borderWidth);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderXWidth, m_partDataSource);
		if (s != null) {
			borderWidth = Integer.parseInt(s);
			
			if (borderWidth > 0) {
				m_borderLeftWidth = borderWidth;
				m_borderRightWidth = borderWidth;
				m_CSSstyle.setAttribute("border-left-width", borderWidth);
				m_CSSstyle.setAttribute("border-right-width", borderWidth);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderYWidth, m_partDataSource);
		if (s != null) {
			borderWidth = Integer.parseInt(s);
			
			if (borderWidth > 0) {
				m_borderTopWidth = borderWidth;
				m_borderBottomWidth = borderWidth;
				m_CSSstyle.setAttribute("border-top-width", borderWidth);
				m_CSSstyle.setAttribute("border-bottom-width", borderWidth);
			}
		}

		String	commonColor = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderColor, m_partDataSource);
		Color	ambientColor = SlideUtilities.getAmbientColor(m_context);
		
		String colorX = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderXColor, m_partDataSource);
		if (colorX == null) {
			colorX = commonColor;
		}
		String colorY = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderYColor, m_partDataSource);
		if (colorY == null) {
			colorY = commonColor;
		}

		String color;

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderLeftWidth, m_partDataSource);
		if (s != null) {
			m_borderLeftWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-left-width", m_borderLeftWidth);
		}
		if (m_borderLeftWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderLeftColor, m_partDataSource);
			m_borderLeftColor = GraphicsManager.acquireColor(color != null ? color : colorX, ambientColor);
			m_CSSstyle.setAttribute("border-left-color", m_borderLeftColor);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderRightWidth, m_partDataSource);
		if (s != null) {
			m_borderRightWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-right-width", m_borderRightWidth);
		}
		if (m_borderRightWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderRightColor, m_partDataSource);
			m_borderRightColor = GraphicsManager.acquireColor(color != null ? color : colorX, ambientColor);
			m_CSSstyle.setAttribute("border-right-color", m_borderRightColor);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderTopWidth, m_partDataSource);
		if (s != null) {
			m_borderTopWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-top-width", m_borderTopWidth);
		}
		if (m_borderTopWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderTopColor, m_partDataSource);
			m_borderTopColor = GraphicsManager.acquireColor(color != null ? color : colorY, ambientColor);
			m_CSSstyle.setAttribute("border-top-color", m_borderTopColor);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderBottomWidth, m_partDataSource);
		if (s != null) {
			m_borderBottomWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-bottom-width", m_borderBottomWidth);
		}
		if (m_borderBottomWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderBottomColor, m_partDataSource);
			m_borderBottomColor = GraphicsManager.acquireColor(color != null ? color : colorY, ambientColor);
			m_CSSstyle.setAttribute("border-bottom-color", m_borderBottomColor);
		}
	}
	
	private void getOtherProperties() {
		String s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_bgcolor, m_partDataSource);
		if (s != null) {
			m_bgcolor = GraphicsManager.acquireColor(s, SlideUtilities.getAmbientBgcolor(m_context));
		} else {
			m_bgcolor = GraphicsManager.acquireColorBySample((Color) m_context.getUnchainedProperty(SlideUtilities.s_ambientBgcolor));
		}
		m_CSSstyle.setAttribute("background-color", m_bgcolor);
	}
	
	protected void initializeChildren() {
		Iterator i = SlidePart.getChildren(m_partDataSource, m_prescription);
		while (i != null && i.hasNext()) {
			Resource resChild = (Resource) i.next();
			
			initializeChild(resChild, m_childParts.size());
		}
	}
	
	protected void initializeChild(Resource resChild, int index) {
		try {
			Context		childContext = new Context(m_context);

			IPart p = null;
			{
				Resource resPart = Ozone.findPart(resChild, m_source, m_partDataSource);
				if (resPart == null) {
					s_logger.error("Could not find part for " + resChild);
				}
				Class c = Utilities.loadClass(resPart, m_source);

				childContext.putLocalProperty(OzoneConstants.s_part, resPart);
				childContext.putLocalProperty(OzoneConstants.s_partData, resChild);
				childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
				
				p = (IPart)c.newInstance();
				p.initialize(m_source, childContext);
			}
			
			if (p instanceof IVisualPart) {
				m_childParts.add(index, p);
			} else {
				m_otherChildParts.add(p);
			}
		} catch (Exception e) {
			s_logger.error("Failed to initialize child " + resChild, e);
		}
	}
	
	protected void repaint() {
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
			repaint(r);
		}
	}
}
