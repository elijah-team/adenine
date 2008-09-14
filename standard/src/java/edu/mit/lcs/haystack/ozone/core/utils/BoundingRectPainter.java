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

package edu.mit.lcs.haystack.ozone.core.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class BoundingRectPainter {
	protected boolean m_boundaryVisible = false;
	protected Rectangle m_lastBoundingRect = null;
	protected Context m_context;
	
	public BoundingRectPainter(Context context) {
		m_context = context;
	}
	
	public void drawBoundingRect(Rectangle r) {
		Control control = (Control) m_context.getSWTControl();
		if (control != null) {
//			System.out.println("*** bounding rect " + r);
			GC 		gc = new GC(control);
			boolean	xor = gc.getXORMode();
			int		lineStyle = gc.getLineStyle();
			int		lineWidth = gc.getLineWidth();
			Color	color = gc.getForeground();
			
			gc.setXORMode(true);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.setLineWidth(1);
			gc.setForeground(SlideUtilities.getAmbientColor(m_context));
			
			gc.drawRectangle(r);

			gc.setXORMode(xor);
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			gc.setForeground(color);
			
			gc.dispose();
		}
	}
	
	public void drawBoundary(boolean visible, Rectangle r) {
		if (m_boundaryVisible && !visible) {
			drawBoundingRect(m_lastBoundingRect);
		} else if (!m_boundaryVisible && visible) {
			drawBoundingRect(r);
			m_lastBoundingRect = r;
		} else if (m_boundaryVisible && visible) {
			if (!r.equals(m_lastBoundingRect)) {
				drawBoundingRect(m_lastBoundingRect);
				drawBoundingRect(r);
				m_lastBoundingRect = r;
			}
		}
		m_boundaryVisible = visible;
	}
}
