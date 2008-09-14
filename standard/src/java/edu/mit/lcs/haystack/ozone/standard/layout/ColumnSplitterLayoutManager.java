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

import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;

import org.eclipse.swt.graphics.*;

/**
 * @author David Huynh
 */
public class ColumnSplitterLayoutManager extends SplitterLayoutManagerBase {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ColumnSplitterLayoutManager.class);

	protected boolean	m_alignText = false;
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.initialize(source, context);
		
		m_resizeCursor = GraphicsManager.s_horzResizeCursor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.SplitterLayoutManagerBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		m_resizeCursor = GraphicsManager.s_horzResizeCursor;
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
		return m_calculatedBS.m_align;
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		internalSetBounds(r, null);
	}

	protected void internalSetBounds(Rectangle r, Region region) {
		boolean sizeChanged = r.width != m_bounds.width;
		
		m_bounds.x = r.x; m_bounds.y = r.y; m_bounds.width = r.width; m_bounds.height = r.height;
		
		if (r.width != m_calculatedBS.m_size.x) {
			internalCalculateSize(m_bounds.width, m_bounds.height, true);
		}
		
		int[] widths = new int[m_elements.size()];
		
		/*	Decide which width to use for each element.
		 */
		int accumulatedWidth = 0;
		for (int i = 0; i < m_elements.size(); i++) {
			Element elmt = (Element) m_elements.get(i);

			if (elmt.m_initializedSize && elementResizable(elmt) && !sizeChanged) {
				widths[i] = elmt.m_bounds.width;
			} else if (elmt.m_blockScreenspace != null) {
				widths[i] = elmt.m_blockScreenspace.m_size.x;
			} else {
				widths[i] = 0;
			}
			
			accumulatedWidth += widths[i];
		}
		// Crop the fill element if necessary
		if (m_fillElement > -1) {
			widths[m_fillElement] = Math.max(widths[m_fillElement] - (accumulatedWidth - r.width), 0);
		}
		
		Rectangle bounds = new Rectangle(0, 0, 0, 0);
		
		/*	Actually set the bounds for each element if its bounds have changed.
		 */
		accumulatedWidth = 0;
		for (int i = 0; i < m_elements.size(); i++) {
			Element elmt = (Element) m_elements.get(i);
			
			bounds.x = r.x + Math.min(accumulatedWidth, r.width);
			bounds.y = r.y;
			bounds.height = r.height;
			bounds.width = Math.min(widths[i], Math.max((r.width - accumulatedWidth), 0));
			
			IVisualPart 		vp = (IVisualPart) m_visualChildParts.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			boolean				childResized = m_childrenToResize.contains(vp);
			
			if (blockGUIHandler != null && 
				(!elmt.m_initializedSize || !elmt.m_bounds.equals(bounds) || childResized)) {
					
				elmt.m_bounds.x = bounds.x; elmt.m_bounds.y = bounds.y; elmt.m_bounds.width = bounds.width; elmt.m_bounds.height = bounds.height;
				elmt.m_initializedSize = (elmt.m_blockScreenspace != null);
				
				blockGUIHandler.setBounds(elmt.m_bounds);
				
				if (region != null) {					
					region.add(bounds);
				}
			}
			
			accumulatedWidth += elmt.m_bounds.width;
		}
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (hintedWidth < 0) {
			return null;
		}
		
		if (hintedWidth != m_calculatedBS.m_size.x || 
			(hintedHeight != -1 && hintedHeight != m_calculatedBS.m_size.y)) {
			internalCalculateSize(hintedWidth, hintedHeight, true);
		}
		
		return m_calculatedBS;
	}

	/**
	 * @see SplitterLayoutManagerBase#redistributeElements()
	 */
	protected void redistributeElements() {
		internalCalculateSize(m_bounds.width, m_bounds.height, false);
	}

	protected BlockScreenspace internalCalculateSize(int hintedWidth, int hintedHeight, boolean forceRedistribute) {
		int i;
		int	accumulatedWidth = 0;

		/*	Calculate size for all elements if required except for the fill element.
		 */
		for (i = 0; i < m_elements.size(); i++) {
			if (i == m_fillElement) {
				continue;
			}
			
			Element 			elmt = (Element) m_elements.get(i);
			IVisualPart			vp = (IVisualPart) m_visualChildParts.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (blockGUIHandler == null) {
				continue;
			}
			
			int width = hintedWidth / m_elements.size();
			if (elmt.m_constraint != null) {
				if (elmt.m_constraint.m_ratio < 0) {
					width = elmt.m_constraint.m_pixels;
				} else {
					width = (int) (hintedWidth * elmt.m_constraint.m_ratio);
				}
			}

			if (forceRedistribute || elmt.m_needsRecalculation || !elmt.m_initializedSize || !elementResizable(elmt)) {
				elmt.m_blockScreenspace = calculateElementSize(blockGUIHandler, width, hintedHeight);
				elmt.m_needsRecalculation = false;
			}
			
			if (elmt.m_initializedSize && elementResizable(elmt)) {
				if (forceRedistribute) {
					accumulatedWidth += elmt.m_blockScreenspace.m_size.x;
				} else {
					accumulatedWidth += elmt.m_bounds.width;
				}
			} else if (elmt.m_blockScreenspace != null) {
				accumulatedWidth += elmt.m_blockScreenspace.m_size.x;
			}
		}
		
		/*	Calculate size of fill element.
		 */
		if (m_fillElement >= 0) {
			Element 			elmt = (Element) m_elements.get(m_fillElement);
			IVisualPart			vp = (IVisualPart) m_visualChildParts.get(m_fillElement);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (blockGUIHandler != null) {
				int width = hintedWidth - accumulatedWidth;
			
				elmt.m_blockScreenspace = calculateElementSize(blockGUIHandler, width, hintedHeight);
				elmt.m_needsRecalculation = false;
			}
		}
		
		/*	Calculate total space.
		 */
		m_calculatedBS.m_size.x = 0;
		m_calculatedBS.m_size.y = 0;
		
		if (m_alignText) {
			int	aboveBaseline = 0;
			int belowBaseline = 0;
			
			for (i = 0; i < m_elements.size(); i++) {
				Element 			elmt = (Element) m_elements.get(i);
				BlockScreenspace	bs = elmt.m_blockScreenspace;
				
				if (bs != null) {
					if (elmt.m_initializedSize || elementResizable(elmt)) {
						if (forceRedistribute) {
							m_calculatedBS.m_size.x += bs.m_size.x;
						} else {
							m_calculatedBS.m_size.x += elmt.m_bounds.width;
						}
					} else {
						m_calculatedBS.m_size.x += bs.m_size.x;
					}
					
					int height = 0;
					if (elmt.m_initializedSize || elementResizable(elmt)) {
						if (forceRedistribute) {
							height = bs.m_size.y;
						} else {
							height = elmt.m_bounds.height;
						}
					} else {
						height = bs.m_size.y;
					}
					if (bs.m_align == BlockScreenspace.ALIGN_TEXT_BASE_LINE) {
						aboveBaseline = Math.max(aboveBaseline, bs.m_alignOffset);
						belowBaseline = Math.max(belowBaseline, height - bs.m_alignOffset);
					} else {
						m_calculatedBS.m_size.y = Math.max(m_calculatedBS.m_size.y, height);
					}
				}
			}
			
			m_calculatedBS.m_size.y = Math.max(m_calculatedBS.m_size.y, aboveBaseline + belowBaseline);
			m_calculatedBS.m_align = BlockScreenspace.ALIGN_TEXT_BASE_LINE;
			m_calculatedBS.m_alignOffset = aboveBaseline;
		} else {
			for (i = 0; i < m_elements.size(); i++) {
				Element elmt = (Element) m_elements.get(i);
				
				if (elmt.m_initializedSize || elementResizable(elmt)) {
					if (forceRedistribute) {
						BlockScreenspace	bs = elmt.m_blockScreenspace;
						if (bs != null) {
							m_calculatedBS.m_size.x += elmt.m_blockScreenspace.m_size.x;
							m_calculatedBS.m_size.y = Math.max(m_calculatedBS.m_size.y, elmt.m_blockScreenspace.m_size.y);
						}
					} else {
						m_calculatedBS.m_size.x += elmt.m_bounds.width;
						m_calculatedBS.m_size.y = Math.max(m_calculatedBS.m_size.y, elmt.m_bounds.height);
					}
				} else {
					BlockScreenspace	bs = elmt.m_blockScreenspace;
					if (bs != null) {
						m_calculatedBS.m_size.x += bs.m_size.x;
						m_calculatedBS.m_size.y = Math.max(m_calculatedBS.m_size.y, bs.m_size.y);
					}
				}
			}
			m_calculatedBS.m_align = BlockScreenspace.ALIGN_TEXT_CLEAR;
		}
		
		return m_calculatedBS;
	}
	
	protected BlockScreenspace calculateElementSize(IBlockGUIHandler blockGUIHandler, int hintedWidth, int hintedHeight) {
		BlockScreenspace bs;
		switch (blockGUIHandler.getHintedDimensions()) {
		case IBlockGUIHandler.FIXED_SIZE:
			bs = blockGUIHandler.getFixedSize();
			break;
		case IBlockGUIHandler.HEIGHT:
			bs = blockGUIHandler.calculateSize(hintedWidth, hintedHeight < 0 ? hintedWidth : hintedHeight);
			break;
		default:
			bs = blockGUIHandler.calculateSize(hintedWidth, hintedHeight);
		}
		return bs;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.SplitterLayoutManagerBase#hittestResizeElement(int, int)
	 */
	protected int hittestResizeElement(int x, int y) {
		int result = -1;
		int resizables = 0;
		boolean	nearBorder = false;
		for (int i = 0; i < m_elements.size(); i++) {
			Element elmt = (Element) m_elements.get(i);
			
			if (elmt.m_constraint == null || elmt.m_constraint.m_resizable) {
				if (nearBorder) {
					resizables++;
				} else {
					result = i;
				}
			}
			if (Math.abs(elmt.m_bounds.x + elmt.m_bounds.width - x) < s_resizeHotspotWidth) {
				nearBorder = true;
			}
		}
		return nearBorder && resizables > 0 ? result : -1;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.SplitterLayoutManagerBase#onStartResizing()
	 */
	protected int m_widthBeforeResizing;
	protected int m_resizeElement2;
	protected void onStartResizing() {
		Element elmt = (Element) m_elements.get(m_resizeElement);
		
		m_widthBeforeResizing = elmt.m_bounds.width;

		for (int i = m_resizeElement + 1; i < m_elements.size(); i++) {
			elmt = (Element) m_elements.get(i);
			if (elmt.m_constraint == null || elmt.m_constraint.m_resizable) {
				m_resizeElement2 = i;
				break;
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.SplitterLayoutManagerBase#onResizing(int, int)
	 */
	protected void onResizing(int x, int y) {
		if (m_resizeElement < 0) {
			return;
		}
		
		Element elmt = (Element) m_elements.get(m_resizeElement);
		Element elmt2 = (Element) m_elements.get(m_resizeElement2);
		
		int originalDiff = x - m_resizeInitialPoint.x;
		int newWidth = Math.max(2 * s_resizeHotspotWidth, m_widthBeforeResizing + originalDiff);
		int currentDiff = newWidth - elmt.m_bounds.width;
		
		elmt2.m_bounds.width -= currentDiff;
		if (elmt2.m_blockScreenspace != null) {
			elmt2.m_blockScreenspace.m_size.x = elmt2.m_bounds.width;
		}
		
		elmt.m_bounds.width = newWidth;
		if (elmt.m_blockScreenspace != null) {
			elmt.m_blockScreenspace.m_size.x = elmt.m_bounds.width;
		}

		for (int i = m_resizeElement + 1; i <= m_resizeElement2; i++) {
			Element elmt3 = (Element) m_elements.get(i);
			elmt3.m_bounds.x += currentDiff;
		}
				
		for (int i = m_resizeElement; i <= m_resizeElement2; i++) {
			IVisualPart 		vp = (IVisualPart) m_visualChildParts.get(i);
			IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
			
			if (blockGUIHandler != null) {
				Element elmt3 = (Element) m_elements.get(i);
				
				blockGUIHandler.setBounds(elmt3.m_bounds);
			}
		}
		
		Rectangle r = elmt.m_bounds.union(elmt2.m_bounds);

		m_parent.redraw(r.x, r.y, r.width, r.height, true);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.SplitterLayoutManagerBase#onEndResizing()
	 */
	protected void onEndResizing() {
		for (int i = 0; i < m_elements.size(); i++) {
			Element		element = (Element) m_elements.get(i);
			Constraint	constraint = element.m_constraint;
			
			if (constraint != null && constraint.m_persistent) {
				if (constraint.m_ratio < 0) {
					constraint.m_pixels = element.m_bounds.width;
				} else {
					constraint.m_ratio = ((float) element.m_bounds.width) / m_bounds.width;
				}
			}
		}
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 * 
	 * SJG: Need to use frames for scrolling, handle m_alignText
	 */
	public void renderHTML(HTMLengine he) {
		he.columnSetStart("ColumnSplitterLayoutManager");
		for (int i = 0; i < m_visualChildParts.size(); i++) {
		  IVisualPart 		vp = (IVisualPart) m_visualChildParts.get(i);
		  Element			element = (Element) m_elements.get(i);
		  IBlockGUIHandler	blockGUIHandler = (IBlockGUIHandler) vp.getGUIHandler(IBlockGUIHandler.class);
		  if (blockGUIHandler != null) {
			  he.columnStart("ColumnSplitterLayoutManager");
			  blockGUIHandler.renderHTML(he);
			  he.columnEnd("ColumnSplitterLayoutManager");
		  }
		}
		he.columnSetEnd("ColumnSplitterLayoutManager");
	  }


}
