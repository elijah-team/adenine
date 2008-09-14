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

/*
 * Created on Apr 25, 2003
 */
package edu.mit.lcs.haystack.ozone.core;

import java.util.EventObject;

import org.eclipse.swt.dnd.DropTargetEvent;

/**
 * @author Dennis
 */
public class OzoneDropTargetEvent extends EventObject {
	/**
	 * @param arg0
	 */
	public OzoneDropTargetEvent(Object arg0, int x, int y) {
		super(arg0);
		m_data = arg0;
		this.m_x = x;
		this.m_y = y;
	}

	/**
	 * @param arg0
	 */
	public OzoneDropTargetEvent(DropTargetEvent arg0) {
		super(arg0);
		m_dropTargetEvent = arg0;
		m_x = arg0.x;
		m_y = arg0.y;
	}

	public DropTargetEvent m_dropTargetEvent;
	public Object m_data;
	public int m_x;
	public int m_y;
}
