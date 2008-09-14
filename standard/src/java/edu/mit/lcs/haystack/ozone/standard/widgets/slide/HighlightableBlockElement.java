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

import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class HighlightableBlockElement extends BlockElement {
	transient Color		m_backgroundHighlightColor;
	transient Color		m_backgroundNormalColor;
	boolean	m_mouseHovering = false;
	
	boolean	m_highlightBorder = false;
	int		m_cachedBorderLeftWidth = 0;
	int		m_cachedBorderRightWidth = 0;
	int		m_cachedBorderTopWidth = 0;
	int		m_cachedBorderBottomWidth = 0;
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		GraphicsManager.releaseColor(m_backgroundHighlightColor);
		m_backgroundHighlightColor = null;
		
		m_bgcolor = m_backgroundNormalColor; // parent will dispose this color
		super.dispose();
	}

	/**
	 * @see VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		String s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_backgroundHighlight, m_partDataSource);
		if (s != null) {
			m_backgroundHighlightColor = GraphicsManager.acquireColor(s, SlideUtilities.getAmbientBgcolor(m_context));
		}
		m_backgroundNormalColor = m_bgcolor;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_highlightBorder, m_partDataSource);
		if (s != null && s.equalsIgnoreCase("true")) {
			m_highlightBorder = true;
		}
		
		if (m_highlightBorder) {
			m_cachedBorderLeftWidth = m_borderLeftWidth;
			m_cachedBorderRightWidth = m_borderRightWidth;
			m_cachedBorderTopWidth = m_borderTopWidth;
			m_cachedBorderBottomWidth = m_borderBottomWidth;			
			
			removeBorders();
		}		
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
		Control c = (Control) m_context.getSWTControl();
		if (m_resOnClick != null) {
			m_mouseHovering = true;
			
			if (m_highlightBorder) {
				insertBorders();
			}
			m_bgcolor = m_backgroundHighlightColor;
			
			repaint(m_rect);
		}
	}

	protected void handleMouseExit(MouseEvent e) {
		Control c = (Control) m_context.getSWTControl();
		if (m_resOnClick != null) {
			m_mouseHovering = false;

			if (m_highlightBorder) {
				removeBorders();
			}
			m_bgcolor = m_backgroundNormalColor;
			
			repaint(m_rect);
		}
	}
	
	protected void removeBorders() {
		m_borderLeftWidth = 0;
		m_borderRightWidth = 0;
		m_borderTopWidth = 0;
		m_borderBottomWidth = 0;
	}
	
	protected void insertBorders() {
		m_borderLeftWidth = m_cachedBorderLeftWidth;
		m_borderRightWidth = m_cachedBorderRightWidth;
		m_borderTopWidth = m_cachedBorderTopWidth;
		m_borderBottomWidth = m_cachedBorderBottomWidth;			
	}	
}
