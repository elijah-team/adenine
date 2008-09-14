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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContainerPartBase;
import org.eclipse.swt.graphics.*;

import java.util.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class RowSetElement extends ContainerPartBase implements IBlockGUIHandler {
	boolean			m_packTop = true;
	BlockScreenspace	m_bs;
	ArrayList			m_rects = new ArrayList();

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(RowSetElement.class);

	/**
	 * @see IPart#dispose()
	 */	
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);

		super.dispose();		
	}
	
	protected void getInitializationData() {
		super.getInitializationData();
		
		String s;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_pack, m_partDataSource);
		if (s != null && s.equalsIgnoreCase("bottom")) {
			m_packTop = false;
		}
	}

	protected void internalInitialize() {
		super.internalInitialize();
		
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		Iterator i = SlidePart.getChildren(m_partDataSource, m_prescription);
		while (i.hasNext()) {
			try {
				Resource	childPartData = (Resource) i.next();
				Resource	childPart = Ozone.findPart(childPartData, m_source, m_partDataSource);
				Class		c = Utilities.loadClass(childPart, m_source);
				IPart		p = (IPart) c.newInstance();
				
				if (p != null) {
					Context childContext = new Context(m_context);
				
					childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
					childContext.putLocalProperty(OzoneConstants.s_partData, childPartData);
					childContext.putLocalProperty(OzoneConstants.s_part, childPart);
					
					p.initialize(m_source, childContext);
					
					if (p instanceof IVisualPart) {
						m_childParts.add(p);
						m_childData.add(null);
						m_rects.add(new Rectangle(0, 0, 0, 0));
					} else {
						m_otherChildParts.add(p);
					}
				}
			} catch (Exception e) {
				s_logger.error("Failed to initialize child part", e);
			}
		}
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
		try {
			IVisualPart 		vp = (IVisualPart) m_childParts.get(0);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			return blockGUIHandler.getTextAlign();
		} catch (Exception e) {
			return BlockScreenspace.ALIGN_TEXT_CLEAR;
		}
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

	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (m_bs == null || 
			(hintedWidth != m_bs.m_size.x) ||
			(hintedHeight != m_bs.m_size.y)) {
				
			m_bs = new BlockScreenspace(BlockScreenspace.ALIGN_TEXT_BASE_LINE);
			
			if (m_packTop) {
				int 		previousBottomClearance = 0;
				boolean	first = true;
			
				for (int i = 0; i < m_childParts.size(); i++) {
					IVisualPart 		vp = (IVisualPart) m_childParts.get(i);
					IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
					BlockScreenspace	bs = null;
					
					if (bgh != null) {
						switch (bgh.getHintedDimensions()) {
						case IBlockGUIHandler.FIXED_SIZE:
							bs = bgh.getFixedSize();
							break;
						case IBlockGUIHandler.WIDTH:
							bs = bgh.calculateSize(
								hintedWidth >= 0 ? hintedWidth : 100,
								hintedHeight >= 0
									? (i == m_childParts.size() - 1 ? hintedHeight - m_bs.m_size.y : -1)
									: -1
							);
							break;
						case IBlockGUIHandler.HEIGHT:
							bs = bgh.calculateSize(
								hintedWidth, 
								(i == m_childParts.size() - 1)
									? (hintedHeight >= 0 ? hintedHeight - m_bs.m_size.y : 100)
									: (hintedHeight >= 0 ? hintedHeight / m_childParts.size() : 100)
							);
							break;
						case IBlockGUIHandler.BOTH:
							bs = bgh.calculateSize(
								hintedWidth >= 0 ? hintedWidth : 100,
								(i == m_childParts.size() - 1)
									? (hintedHeight >= 0 ? hintedHeight - m_bs.m_size.y : 100)
									: (hintedHeight >= 0 ? hintedHeight / m_childParts.size() : 100)
							);
							break;
						}
					}
					
					if (bs != null) {
						int gap = first ? 0 : Math.max(previousBottomClearance, bs.m_clearanceTop);
						
						if (i == m_childParts.size() - 1 && hintedHeight >= 0) {
							bs.m_size.y = hintedHeight - gap - m_bs.m_size.y;
						}
						
						m_bs.m_size.x = Math.max(m_bs.m_size.x, bs.m_size.x);
						m_bs.m_size.y += gap + bs.m_size.y;
						m_bs.m_clearanceLeft = Math.max(m_bs.m_clearanceLeft, bs.m_clearanceLeft);
						m_bs.m_clearanceRight = Math.max(m_bs.m_clearanceRight, bs.m_clearanceRight);
						
						if (first && bs.m_align == BlockScreenspace.ALIGN_TEXT_BASE_LINE) {
							m_bs.m_alignOffset = bs.m_alignOffset;
							m_bs.m_clearanceTop = bs.m_clearanceTop;
						}
						
						first = false;
					}
					
					m_childData.set(i, bs);
				}
				
				m_bs.m_clearanceBottom = previousBottomClearance;
			} else {
				int		previousTopClearance = 0;
				boolean	first = true;
				
				for (int i = m_childParts.size() - 1; i >= 0; i--) {
					IVisualPart 		vp = (IVisualPart) m_childParts.get(i);
					IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
					BlockScreenspace	bs = null;
					
					if (bgh != null) {
						switch (bgh.getHintedDimensions()) {
						case IBlockGUIHandler.FIXED_SIZE:
							bs = bgh.getFixedSize();
							break;
						case IBlockGUIHandler.WIDTH:
							bs = bgh.calculateSize(
								hintedWidth >= 0 ? hintedWidth : 100,
								hintedHeight >= 0
									? (i == 0 ? hintedHeight - m_bs.m_size.y : -1)
									: -1
							);
							break;
						case IBlockGUIHandler.HEIGHT:
							bs = bgh.calculateSize(
								hintedWidth, 
								(i == 0)
									? (hintedHeight >= 0 ? hintedHeight - m_bs.m_size.y : 100)
									: (hintedHeight >= 0 ? hintedHeight / m_childParts.size() : 100)
							);
							break;
						case IBlockGUIHandler.BOTH:
							bs = bgh.calculateSize(
								hintedWidth >= 0 ? hintedWidth : 100,
								(i == 0)
									? (hintedHeight >= 0 ? hintedHeight - m_bs.m_size.y : 100)
									: (hintedHeight >= 0 ? hintedHeight / m_childParts.size() : 100)
							);
							break;
						}
					}
					
					if (bs != null) {
						int gap = first ? 0 : Math.max(previousTopClearance, bs.m_clearanceBottom);
						
						if (i == 0 && hintedHeight >= 0) {
							bs.m_size.y = hintedHeight - m_bs.m_size.y - gap;
						}
						
						m_bs.m_size.x = Math.max(m_bs.m_size.x, bs.m_size.x);
						m_bs.m_size.y += gap + bs.m_size.y;
						m_bs.m_clearanceLeft = Math.max(m_bs.m_clearanceLeft, bs.m_clearanceLeft);
						m_bs.m_clearanceRight = Math.max(m_bs.m_clearanceRight, bs.m_clearanceRight);
						
						if (bs.m_align == BlockScreenspace.ALIGN_TEXT_BASE_LINE) {
							m_bs.m_alignOffset = bs.m_alignOffset;
						} else {
							m_bs.m_alignOffset = 0;
						}
						
						if (first) {
							m_bs.m_clearanceBottom = bs.m_clearanceBottom;
						}
						
						first = false;
					}
					
					m_childData.set(i, bs);
				}
			
				m_bs.m_clearanceTop = previousTopClearance;
			}
		}
		
		return new BlockScreenspace(m_bs);
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		calculateSize(r.width, r.height);
		cacheBounds(r);
		
		int 		offset = 0;
		int 		previousBottomClearance = 0;
		boolean	first = true;
		
		for (int i = 0; i < m_childParts.size(); i++) {
			IVisualPart 		vp = (IVisualPart) m_childParts.get(i);
			IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			BlockScreenspace	bs = (BlockScreenspace) m_childData.get(i);
			
			if (bs != null && bgh != null) {
				if (bs.m_size.x < r.width) {
					BlockScreenspace bs2 = bgh.calculateSize(r.width, bs.m_size.y);
					if (bs2 != null) {
						bs = bs2;
						m_childData.set(i, bs);
					}
				}

				int gap = first ? 0 : Math.max(previousBottomClearance, bs.m_clearanceTop);				
				Rectangle r2 = new Rectangle(
					r.x, r.y + offset + gap, Math.min(r.width, bs.m_size.x), Math.min(r.height - offset, bs.m_size.y)
				);
				Rectangle r3 = (Rectangle) m_rects.get(i);
				
				/*if (!r3.equals(r2))*/ {
					r3.x = r2.x; r3.y = r2.y; r3.width = r2.width; r3.height = r2.height;
					bgh.setBounds(r2);
				}
				
				offset = Math.min(offset + bs.m_size.y + gap, r.height);
				first = false;
			}
		}
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		setBounds(r);
		
		Region 		region = new Region();
		Rectangle	clipping = gc.getClipping();

		gc.getClipping(region);
		
		for (int i = 0; i < m_childParts.size(); i++) {
			IVisualPart 		vp = (IVisualPart) m_childParts.get(i);
			IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			BlockScreenspace	bs = (BlockScreenspace) m_childData.get(i);
			Rectangle			r2 = (Rectangle) m_rects.get(i);
			
			if (bs != null && bgh != null) {
				if (region.intersects(r2)) {
					gc.setClipping(r2.intersection(clipping));
					bgh.draw(gc, r2);
				}
			}
		}
		
		gc.setClipping(region);
		region.dispose();
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 * 
	 * SJG: need to handle m_packTop
	 */
	public void renderHTML(HTMLengine he) {
	  he.rowSetStart("RowSetElement");
	  for (int i = 0; i < m_childParts.size(); i++) {
	    IVisualPart 		vp = (IVisualPart) m_childParts.get(i);
	    IBlockGUIHandler	bgh = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
	    BlockScreenspace	bs = (BlockScreenspace) m_childData.get(i);
	    if (bs != null && bgh != null) {
	    	he.rowStart("RowSetElement");
	    	bgh.renderHTML(he);
	    	he.rowEnd("RowSetElement");
	    }
	  }
	  he.rowSetEnd("RowSetElement");
	}
	

	protected IVisualPart hittest(int x, int y) {
		for (int i = 0; i < m_childParts.size(); i++) {
			Rectangle r = (Rectangle) m_rects.get(i);
				
			if (r.contains(x, y)) {
				return (IVisualPart) m_childParts.get(i);
			}
		}
		
		return null;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onChildResize(edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		m_bs = null;
		return super.onChildResize(e);
	}

}

