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

/**
 * @author David Huynh
 */
public class SpanTextFlowCounter implements ITextFlowCounter, Serializable { 
	ITextFlowCounter	m_parent;

	int				m_marginLeft;
	int				m_marginRight;
	int				m_marginTop;
	int				m_marginBottom;
	
	ArrayList			m_spanSets = new ArrayList();
	ArrayList			m_currentSpanSet;
	ITextSpan			m_firstTextSpan;	// corresponds to left margin
	ITextSpan			m_lastTextSpan;		// corresponds to right margin
	
	public SpanTextFlowCounter(int marginLeft, int marginRight, int marginTop, int marginBottom) {
		m_marginLeft = marginLeft;
		m_marginRight = marginRight;
		m_marginTop = marginTop;
		m_marginBottom = marginBottom;
	}
	
	public void beginCounter(ITextFlowCounter parent) {
		m_parent = parent;

		if (m_marginLeft > m_parent.getRemainingLineLength()) {
			m_parent.addLineBreak();
		}
		m_firstTextSpan = m_parent.addSpan(m_marginLeft, 0, BlockScreenspace.ALIGN_TEXT_BASE_LINE, 0);
		m_spanSets.clear();
	}
	
	public void endCounter() {
		m_parent.addSpan(m_marginRight, 0, BlockScreenspace.ALIGN_TEXT_BASE_LINE, 0);
	}
	
	public void beginSegmentSet() {
		m_currentSpanSet = new ArrayList();
	}
	
	public void endSegmentSet() {
		m_spanSets.add(m_currentSpanSet);
		m_currentSpanSet = null;
	}
	
	
	
	public int getMarginTop() {
		return m_marginTop;
	}
	public int getMarginBottom() {
		return m_marginBottom;
	}
	
	public ITextSpan getFirstSegment() {
		return m_firstTextSpan;
	}
	public ITextSpan getLastSegment() {
		return m_lastTextSpan;
	}
	public ArrayList getSpanSets() {
		return m_spanSets;
	}
	
	
	/**
	 * @see ITextFlowCounter#getCurrentSpanSet()
	 */
	public List getCurrentSpanSet() {
		return m_currentSpanSet;
	}

	/**
	 * @see ITextFlowCounter#getRemainingLineLength()
	 */
	public int getRemainingLineLength() {
		return m_parent.getRemainingLineLength() - m_marginRight;
	}

	/**
	 * @see ITextFlowCounter#getLineLength()
	 */
	public int getLineLength() {
		return m_parent.getLineLength() - m_marginRight;
	}
	
	/**
	 * @see ITextFlowCounter#addSpan(int, int, int, int)
	 */
	public ITextSpan addSpan(int length, int height, int align, int alignOffset) {
		SpanTextSpan sts = new SpanTextSpan(this, m_parent.addSpan(
			length,
			height + m_marginTop + m_marginBottom,
			align,
			alignOffset + m_marginTop
		));
		
		m_currentSpanSet.add(sts);
		return sts;
	}

	/**
	 * @see ITextFlowCounter#addSpan(BlockScreenspace)
	 */
	public ITextSpan addSpan(BlockScreenspace bs) {
		return addSpan(bs.m_size.x, bs.m_size.y, bs.m_align, bs.m_alignOffset);
	}

	/**
	 * @see ITextFlowCounter#addLineBreak()
	 */
	public void addLineBreak() {
		m_parent.addLineBreak();
	}

	/**
	 * @see ITextFlowCounter#getAverageLineHeight()
	 */
	public int getAverageLineHeight() {
		return m_parent.getAverageLineHeight() - m_marginTop - m_marginBottom;
	}
}
