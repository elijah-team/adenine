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

package edu.mit.lcs.haystack.ozone.core;

import java.io.Serializable;

import org.eclipse.swt.graphics.Point;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class BlockScreenspace implements Serializable {
	public static final int ALIGN_TEXT_CLEAR = 0; // avoid text on same line, go to new line if necessary
	public static final int ALIGN_TEXT_BASE_LINE = 1;
	public static final int ALIGN_TEXT_CENTER = 2;
	public static final int ALIGN_LINE_TOP = 3;
	public static final int ALIGN_LINE_BOTTOM = 4;
	public static final int ALIGN_LINE_CENTER = 5;
	// public static final int ALIGN_TEXT_TOP = 6;
	// public static final int ALIGN_TEXT_BOTTOM = 7;
	// public static final int ALIGN_TEXT_CENTER = 8;

	public int	m_align;
	public int	m_alignOffset;	// height in pixel above text base line
	public Point	m_size;
	
	public int	m_clearanceLeft;
	public int	m_clearanceRight;
	public int	m_clearanceTop;
	public int	m_clearanceBottom;
	
	public BlockScreenspace() {
		m_align = ALIGN_TEXT_CLEAR;
		m_size = new Point(0, 0);
	}
	
	public BlockScreenspace(int align) {
		m_align = align;
		m_size = new Point(0, 0);
	}
	
	public BlockScreenspace(int width, int height) {
		m_align = ALIGN_TEXT_CLEAR;
		m_alignOffset = 0;
		m_size = new Point(width, height);
	}
	
	public BlockScreenspace(Point p) {
		m_align = ALIGN_TEXT_CLEAR;
		m_size = new Point(p.x, p.y);
	}
	
	public BlockScreenspace(int width, int height, int align, int alignOffset) {
		m_align = align;
		m_alignOffset = alignOffset;
		m_size = new Point(width, height);
	}

	public BlockScreenspace(Point p, int align, int alignOffset) {
		m_align = align;
		m_alignOffset = alignOffset;
		m_size = new Point(p.x, p.y);
	}

	public BlockScreenspace(BlockScreenspace bs) {
		m_align = bs.m_align;
		m_alignOffset = bs.m_alignOffset;
		m_size = new Point(bs.m_size.x, bs.m_size.y);
		
		m_clearanceLeft = bs.m_clearanceLeft;
		m_clearanceRight = bs.m_clearanceRight;
		m_clearanceTop = bs.m_clearanceTop;
		m_clearanceBottom = bs.m_clearanceBottom;
	}
	
	/**
	 * @see java.lang.Object#equals(Object)
	 */
	public boolean equals(Object o) {
		if (o != null && o instanceof BlockScreenspace) {
			BlockScreenspace bs = (BlockScreenspace) o;
			
			return bs.m_align == m_align && bs.m_alignOffset == m_align && bs.m_size == m_size;
		} else {
			return false;
		}
	}

}
