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
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class TrainElement extends ContainerPartBase implements IBlockGUIHandler {
	protected BlockScreenspace m_bs;
	protected boolean			m_alignWithText;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TrainElement.class);
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		
		super.dispose();
	}
	
	protected void internalInitialize() {
		super.internalInitialize();

		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		
		m_alignWithText = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_textAlign, m_partDataSource, true);
	
		initializeChildren();
	}
	
	protected void initializeChildren() {
		Iterator i = SlidePart.getChildren(m_partDataSource, m_prescription);
		while (i.hasNext()) {
			try {
				Resource	r = (Resource)i.next();
				
				Resource resPart = Ozone.findPart(r, m_source, m_partDataSource);
				if (resPart == null) {
					s_logger.error("Could not find part for " + r);
				}
				
				Class c = Utilities.loadClass(resPart, m_source);

				IPart p = (IPart) c.newInstance();
				
				Context childContext = new Context(m_context);
				childContext.putLocalProperty(OzoneConstants.s_part, resPart);
				childContext.putLocalProperty(OzoneConstants.s_partData, r);
				childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
					
				p.initialize(m_source, childContext);

				if (p instanceof IVisualPart) {
					m_childParts.add(p);
				} else {
					m_otherChildParts.add(p);
				}
			} catch (Exception e) {
				s_logger.error("Failed to initialize children", e);
			}
		}
	}
	
	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle rect) {
		setBounds(rect);

		Iterator i = m_childParts.iterator();
		Iterator i2 = m_childData.iterator();
		
		int left = m_rect.x;
		
		while (i.hasNext() && i2.hasNext()) {
			IVisualPart			vp = (IVisualPart) i.next();
			IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			BlockScreenspace	bs = (BlockScreenspace) i2.next();
			
			if (bs != null && bgh != null) {
				int top = m_rect.y + m_bs.m_alignOffset - bs.m_alignOffset;
				
				bgh.draw(gc, new Rectangle(left, top, bs.m_size.x, bs.m_size.y));
				
				left += bs.m_size.x;
			}
		}
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
	  he.enterSpan("TrainElement", m_CSSstyle, this, m_tooltip);
	  Iterator i = m_childParts.iterator();
	  Iterator i2 = m_childData.iterator();
	  while (i.hasNext() && i2.hasNext()) {
	    IVisualPart		vp = (IVisualPart) i.next();
	    IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
	    BlockScreenspace	bs = (BlockScreenspace) i2.next();
	    if (bs != null && bgh != null) bgh.renderHTML(he);
	  }
	  he.exitSpan("TrainElement");
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		return null;
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		if (!m_rect.equals(r)) {
			getFixedSize();
			
			m_rect.x = r.x; m_rect.y = r.y; m_rect.width = r.width; m_rect.height = r.height;
			
			Iterator i = m_childParts.iterator();
			Iterator i2 = m_childData.iterator();
			
			int left = m_rect.x;
			
			while (i.hasNext()) {
				IVisualPart			vp = (IVisualPart) i.next();
				IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
				BlockScreenspace	bs = (BlockScreenspace) i2.next();
				
				if (bs != null && bgh != null) {
					int top = m_rect.y + m_bs.m_alignOffset - bs.m_alignOffset;
					
					bgh.setBounds(new Rectangle(left, top, bs.m_size.x, bs.m_size.y));
					
					left += bs.m_size.x;
				}
			}
		}
	}

	/**
	 * @see VisualPartBase#onChildResize(ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		m_bs = null;
		m_rect.width = 0;
		m_rect.height = 0;
		return super.onChildResize(e);
	}

	protected IVisualPart hittest(int x, int y) {
		IVisualPart vp = null;
		
		if (m_bs != null) {
			int left = m_rect.x;
			
			for (int i = 0; i < m_childData.size() && x > left; i++) {
				BlockScreenspace bs = (BlockScreenspace) m_childData.get(i);
				
				if (bs != null) {
					if (x < left + bs.m_size.x) {
						int top = m_rect.y + m_bs.m_alignOffset - bs.m_alignOffset;
						
						if (y >= top && y < top + bs.m_size.y) {
							vp = (IVisualPart) m_childParts.get(i);
						}
						
						break;
					}
					
					left += bs.m_size.x;
				}
			}
		}		
		return vp;
	}
	
	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		if (m_bs == null) {
			m_childData.clear();
			
			m_bs = new BlockScreenspace(m_alignWithText ? BlockScreenspace.ALIGN_TEXT_BASE_LINE : BlockScreenspace.ALIGN_TEXT_CLEAR);
			
			Iterator i = m_childParts.iterator();
			while (i.hasNext()) {
				IVisualPart 		vp = (IVisualPart) i.next();
				IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
				BlockScreenspace	bs = null;
				
				if (bgh != null) {
					switch (bgh.getHintedDimensions()) {
					case IBlockGUIHandler.FIXED_SIZE:
						bs = bgh.getFixedSize();
						break;
					case IBlockGUIHandler.WIDTH:
						bs = bgh.calculateSize(100, -1); // What width should we give?
						break;
					case IBlockGUIHandler.HEIGHT:
						bs = bgh.calculateSize(-1, 20); // What height should we give?
						break;
					case IBlockGUIHandler.BOTH:
						bs = bgh.calculateSize(100, 20);
						break;
					}
				}
								
				if (bs != null) {
					if (m_alignWithText) {
						if (bs.m_align == BlockScreenspace.ALIGN_TEXT_CLEAR) {
							bs.m_alignOffset = bs.m_size.y;
						}
											
						int lower = m_bs.m_size.y - m_bs.m_alignOffset;
						
						m_bs.m_alignOffset = Math.max(m_bs.m_alignOffset, bs.m_alignOffset);
						m_bs.m_size.y = m_bs.m_alignOffset + Math.max(lower, bs.m_size.y - bs.m_alignOffset);
					} else {
						bs.m_align = BlockScreenspace.ALIGN_TEXT_CLEAR;
						bs.m_alignOffset = 0;
						
						m_bs.m_size.y = Math.max(m_bs.m_size.y, bs.m_size.y);
					}
					m_bs.m_size.x += bs.m_size.x;
				}
				
				m_childData.add(bs);
			}
		}
		return new BlockScreenspace(m_bs);
	}

	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.FIXED_SIZE;
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
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		} else {
			return null;
		}
	}

	public Rectangle getBounds() {
		Control c = (Control) m_context.getSWTControl();
		Point	p = c.toDisplay(new Point(m_rect.x, m_rect.y));
		
		return new Rectangle(p.x, p.y, m_rect.width, m_rect.height);
	}
}
