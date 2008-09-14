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
import edu.mit.lcs.haystack.ozone.core.ITextFlowCounter;
import edu.mit.lcs.haystack.ozone.core.ITextSpan;

import java.io.Serializable;
import java.util.*;
import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
class TextFlowCounter implements ITextFlowCounter, Serializable {
	int			m_givenWidth = -1;
	int			m_givenHeight = -1;
	int			m_actualHeight = -1;
	int			m_actualWidth = -1;
	
	int			m_horzAlign;
	int			m_vertAlign;
	
	public Point	m_origin = new Point(0, 0);	// origin of bounding rectangle w.r.t. parent's location
	public Point	m_offset = new Point(0, 0);	// offset due to horz or vert alignment
	
	int			m_currentRowLength;				// accumulated length of segments on current row
	
	int			m_currentRowMaxSpanHeight;
	int			m_rowCount;
	
	int			m_alignOffset;
	
	java.util.List		m_segmentSets;			// all sets of segments
	java.util.List		m_currentSegmentSet;	// set of segments for current client
	java.util.List		m_currentRowSegments;	// set of segments on current row
	
	public TextFlowCounter(int horzAlign, int vertAlign) {
		m_horzAlign = horzAlign;
		m_vertAlign = vertAlign;
	}
	
	public Rectangle translate(Rectangle r) {
		Rectangle r2 = new Rectangle(
			r.x + m_origin.x + m_offset.x,
			r.y + m_origin.y + m_offset.y,
			r.width,
			r.height
		);
		
		return r2;		
	}


	
	public void beginCounter(int width, int height) {
		m_givenWidth = width;
		m_givenHeight = height;
		
		m_actualHeight = 0;
		m_actualWidth = m_givenWidth;
		m_rowCount = 0;
		
		m_segmentSets = new ArrayList();
		
		m_alignOffset = 0;
		
		initNewRow();
	}
	
	public void endCounter() {
		if (m_currentRowLength > 0) {
			addLineBreak(); // takes care of horizontal alignment of last row
		}
		
		switch (m_horzAlign) {
			case SlideConstants.ALIGN_LEFT:
				m_offset.x = 0; break;
			case SlideConstants.ALIGN_CENTER:
				m_offset.x = m_actualWidth / 2; break;
			case SlideConstants.ALIGN_RIGHT:
				m_offset.x = m_actualWidth; break;
		}

		doVertAlign();
	}
	
	public void beginSegmentSet() {
		m_currentSegmentSet = new ArrayList();
	}
	
	public void endSegmentSet() {
		m_segmentSets.add(m_currentSegmentSet);
		m_currentSegmentSet = null;
	}


	
	public void setOrigin(Point origin) {
		m_origin.x = origin.x;
		m_origin.y = origin.y;
	}
	
	public BlockScreenspace getPreferredSize() {
		return new BlockScreenspace(m_actualWidth, m_actualHeight /*Math.max(m_actualHeight, m_givenHeight)*/, BlockScreenspace.ALIGN_TEXT_BASE_LINE, m_alignOffset);
	}
	
	public int getWidth() {
		return m_givenWidth;
	}
	
	public java.util.List getSpanSets() {
		return m_segmentSets;
	}
	
	public void setHeight(int height) {
		if (m_givenHeight != height) {
			m_givenHeight = height;
			doVertAlign();
		}
	}
	
	public int getRemainingHeight() {
		return m_givenHeight - m_actualHeight;
	}
	
	
	
	/**
	 * @see ITextFlowCounter#addSpan(int, int, int, int)
	 */
	public ITextSpan addSpan(int length, int height, int align, int alignOffset) {
		SlideRowSegment segment = new SlideRowSegment(this, align, alignOffset, 
			new Rectangle(m_currentRowLength, 0, length, height));
		
		m_currentRowLength += length;
		m_currentRowMaxSpanHeight = Math.max(m_currentRowMaxSpanHeight, height);
		
		m_currentSegmentSet.add(segment);
		m_currentRowSegments.add(segment);

		return segment;
	}

	/**
	 * @see ITextFlowCounter#addSpan(BlockScreenspace)
	 */
	public ITextSpan addSpan(BlockScreenspace bs) {
		return addSpan(bs.m_size.x, bs.m_size.y, bs.m_align, bs.m_alignOffset);
	}

	/**
	 * @see ITextFlowCounter#getCurrentSpanSet()
	 */
	public List getCurrentSpanSet() {
		return m_currentSegmentSet;
	}

	/**
	 * @see ISlideExtentCounter#getLineLength()
	 */
	public int getLineLength() {
		return m_givenWidth;
	}

	/**
	 * @see ITextFlowCounter#getRemainingLineLength()
	 */
	public int getRemainingLineLength() {
		return m_givenWidth - m_currentRowLength;
	}

	/**
	 * @see ITextFlowCounter#addLineBreak()
	 */
	public void addLineBreak() {
		if (m_currentRowLength == 0) {
			return;
		}
		
		int aboveBaseLine = 0;
		int belowBaseLine = 0;
		int rowHeight = 0;
		int	topBleed = 0;
		int	bottomBleed = 0;
		
		boolean	topAligned = false;
		boolean	bottomAligned = false;
		
		Iterator i = m_currentRowSegments.iterator();		
		while (i.hasNext()) {
			SlideRowSegment segment = (SlideRowSegment) i.next();
			
			switch (segment.m_align) {
				case BlockScreenspace.ALIGN_TEXT_BASE_LINE:
					aboveBaseLine = Math.max(aboveBaseLine, segment.m_alignOffset);
					belowBaseLine = Math.max(belowBaseLine, segment.m_r.height - segment.m_alignOffset);
					break;
				case BlockScreenspace.ALIGN_LINE_TOP:
					topAligned = true;
					rowHeight = Math.max(rowHeight, segment.m_r.height);
					break;
				case BlockScreenspace.ALIGN_LINE_BOTTOM:
					bottomAligned = true;
					rowHeight = Math.max(rowHeight, segment.m_r.height);
					break;
				default:
					rowHeight = Math.max(rowHeight, segment.m_r.height);
			}
		}
		
		rowHeight = Math.max(rowHeight, aboveBaseLine + belowBaseLine);
		int offset = rowHeight - (aboveBaseLine + belowBaseLine);
		if (topAligned && bottomAligned) {			
			aboveBaseLine += offset / 2;
		} else if (bottomAligned) {
			aboveBaseLine += offset;
		}
		
		int horzOffsetDueToAlignment = 0;
		switch (m_horzAlign) {
			case SlideConstants.ALIGN_CENTER:
				horzOffsetDueToAlignment = - m_currentRowLength / 2; break;
			case SlideConstants.ALIGN_RIGHT:
				horzOffsetDueToAlignment = - m_currentRowLength; break;				
		}
		
		i = m_currentRowSegments.iterator();
		while (i.hasNext()) {
			SlideRowSegment segment = (SlideRowSegment) i.next();
			
			switch (segment.m_align) {
				case BlockScreenspace.ALIGN_TEXT_BASE_LINE:
					segment.m_r.y = m_actualHeight + aboveBaseLine - segment.m_alignOffset; break;
				case BlockScreenspace.ALIGN_LINE_TOP:
					segment.m_r.y = m_actualHeight; break;
				case BlockScreenspace.ALIGN_LINE_BOTTOM:
					segment.m_r.y = m_actualHeight + rowHeight - segment.m_r.height; break;
				default:
					segment.m_r.y = m_actualHeight;
			}
			
			segment.m_r.x += horzOffsetDueToAlignment;
		}
		
		if (m_rowCount == 0) {
			m_alignOffset = aboveBaseLine;
		}
		
		m_actualHeight += rowHeight;
		m_actualWidth = Math.max(m_actualWidth, m_currentRowLength);
		m_rowCount++;
		
		initNewRow();
	}
	
	/**
	 * @see ITextFlowCounter#getAverageLineHeight()
	 */
	public int getAverageLineHeight() {
		if (m_rowCount == 0) {
			return m_currentRowMaxSpanHeight;
		} else {
			return m_actualHeight / m_rowCount;
		}
	}

	
	
	private void doVertAlign() {
		switch (m_vertAlign) {
			case SlideConstants.ALIGN_TOP:
				m_offset.y = 0; break;
			case SlideConstants.ALIGN_CENTER:
				m_offset.y = (m_givenHeight - m_actualHeight) / 2; break;
			case SlideConstants.ALIGN_BOTTOM:
				m_offset.y = m_givenHeight - m_actualHeight; break;
		}
	}
	
	private void initNewRow() {
		m_currentRowLength = 0;
		m_currentRowSegments = new ArrayList();
		m_currentRowMaxSpanHeight = 0;
	}
}

class SlideRowSegment implements ITextSpan, Serializable {
	TextFlowCounter	m_counter;
	
	public Rectangle	m_r;
	
	public int			m_align;
	public int			m_alignOffset;
	
	public SlideRowSegment(
		TextFlowCounter	counter, 
		int 				align,
		int 				alignOffset,
		Rectangle			r
	) {
		m_counter = counter;
		m_align = align;
		m_alignOffset = alignOffset;
		m_r = r;
	}
	
	/**
	 * @see ITextSpan#getArea()
	 */
	public Rectangle getArea() {
		return m_counter.translate(m_r);
	}

	/**
	 * @see ITextSpan#getAlign()
	 */
	public int getAlign() {
		return m_align;
	}

	/**
	 * @see ITextSpan#getAlignOffset()
	 */
	public int getAlignOffset() {
		return m_alignOffset;
	}

}
