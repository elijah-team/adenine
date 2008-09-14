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

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.events.*;

/**
 * @author David Huynh
 */
abstract public class PaintHandler {
	protected Composite	m_composite;
	protected Image		m_image;

	public void initialize(Composite composite) {
		m_composite = composite;
		
		composite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (m_composite != null && !m_composite.isDisposed()) {
					onPaint(e);
				}
			}
		});
		
		composite.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (m_image != null) {
					m_image.dispose();
					m_image = null;
				}
				m_composite = null;
			}
		});
	}

	protected void onPaint(PaintEvent e) {
		Rectangle	r = m_composite.getClientArea();
		Rectangle	clipping = e.gc.getClipping();
		
		if (clipping.width * clipping.height > 1000 && r.width * r.height < 1000000) {
			if (m_image != null && !m_image.getBounds().equals(r)) {
				m_image.dispose();
				m_image = null;
			}
			if (m_image == null) {
				m_image = new Image(Ozone.s_display, r.width, r.height);
			}
			
			GC gc;
			try {
				gc = new GC(m_image);
			} catch (Exception e1) {
				try {
					m_image.dispose();
				} catch (Exception e2) {
				}
				m_image = new Image(Ozone.s_display, r.width, r.height);
				gc = new GC(m_image);
			}
			
			Region region = new Region();
			
			e.gc.getClipping(region);
			gc.setClipping(region);
			
			region.dispose();
			region = null;
			
			gc.setBackground(m_composite.getBackground());
			gc.fillRectangle(clipping);
			
			drawContent(gc, r);
			
			e.gc.drawImage(
				m_image,
				clipping.x, clipping.y, clipping.width, clipping.height,
				clipping.x, clipping.y, clipping.width, clipping.height
			);
					
			gc.dispose();
			gc = null;
		} else {
			e.gc.fillRectangle(clipping);
			drawContent(e.gc, r);
		}
	}
	
	abstract protected void drawContent(GC gc, Rectangle r);
}
