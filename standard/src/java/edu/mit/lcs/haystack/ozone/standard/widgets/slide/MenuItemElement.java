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

import java.io.IOException;

import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.modeless.IModelessCreator;
import edu.mit.lcs.haystack.ozone.standard.modeless.ModelessConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class MenuItemElement extends BlockElement {
	transient protected Color		m_backgroundHighlightColor;
	transient protected Color		m_backgroundNormalColor;
	Resource	m_submenu;
	boolean	m_mouseHovering = false;
	boolean	m_autoshowSubmenu = true;
	
	final static Resource SUBMENU = new Resource(SlideConstants.s_namespace + "submenu");
	final static Resource AUTOSHOW_SUBMENU = new Resource(SlideConstants.s_namespace + "autoshowSubmenu");
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		BlockElement.writeColor(out, m_backgroundHighlightColor);
		BlockElement.writeColor(out, m_backgroundNormalColor);
	}

	private transient RGB m_backgroundHighlightRGB;
	private transient RGB m_backgroundNormalRGB;

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		m_backgroundHighlightRGB = (RGB) in.readObject();
		m_backgroundNormalRGB = (RGB) in.readObject();
	}

	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_backgroundHighlightRGB != null) {
			m_backgroundHighlightColor = GraphicsManager.acquireColorByRGB(m_backgroundHighlightRGB);
		}
		
		if (m_backgroundNormalRGB != null) {
			m_backgroundNormalColor = GraphicsManager.acquireColorByRGB(m_backgroundNormalRGB);
		}
	}

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
		
		m_submenu = Utilities.getResourceProperty(m_prescription, SUBMENU, m_partDataSource);
		
		m_autoshowSubmenu = Utilities.checkBooleanProperty(m_prescription, AUTOSHOW_SUBMENU, m_partDataSource, true);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseEnter(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onMouseEnter(MouseEvent e) {
		m_mouseHovering = true;
		m_bgcolor = m_backgroundHighlightColor;
		repaint(m_rect);
		
		if (m_submenu != null) {
			Control c = (Control) m_context.getSWTControl();
			c.setCursor(GraphicsManager.s_handCursor);

			if (m_autoshowSubmenu) {
				onClick(e);
			}
			return true;
		} else {
			return super.onMouseEnter(e);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseExit(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onMouseExit(MouseEvent e) {
		m_mouseHovering = false;
		m_bgcolor = m_backgroundNormalColor;
		repaint(m_rect);
		
		if (m_submenu != null && !m_autoshowSubmenu) {
			Control c = (Control) m_context.getSWTControl();

			c.setCursor(null);
			
			return true;
		} else {
			return super.onMouseEnter(e);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onClick(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onClick(MouseEvent e) {
		if (m_submenu != null) {
			Control c = (Control) m_context.getSWTControl();
			IModelessCreator mc = (IModelessCreator) m_context.getProperty(ModelessConstants.MODELESS_CREATOR);
			try {
				Point p = c.toDisplay(new Point(m_rect.x, m_rect.y));
				mc.createModelessPart(m_submenu, new Rectangle(p.x, p.y, m_rect.width, m_rect.height), false, m_autoshowSubmenu, m_context);
			} catch (Exception ex) {
			}
			return true;
		} else {
			return super.onClick(e);
		}
	}
}
