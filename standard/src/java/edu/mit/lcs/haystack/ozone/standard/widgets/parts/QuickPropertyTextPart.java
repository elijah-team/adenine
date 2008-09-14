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
 * Created on May 13, 2003
 */
package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.UnderlyingResourceDataProvider;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class QuickPropertyTextPart extends VisualPartBase implements IBlockGUIHandler {
	protected int m_height = -1;
	protected String m_string = "";
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		
		String s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_height, m_partDataSource);
		if (s != null) {
			m_height = Math.max(-1, Integer.parseInt(s));
		}
		
		Resource property = Utilities.getResourceProperty(m_prescription, DataConstants.PREDICATE, m_partDataSource);
		Resource subject = UnderlyingResourceDataProvider.getUnderlyingResource(m_context, 1);
		if (subject != null && property != null) {
			m_string = Utilities.getLiteralProperty(subject, property, m_infoSource);
			if (m_string == null) {
				m_string = "";
			}
		}
		
		FontData fd = SlideUtilities.getAmbientFont(m_context).getFontData()[0];
		int fs = fd.getStyle();
		m_CSSstyle.setAttribute("font-family", fd.getName());
		m_CSSstyle.setAttribute("font-size", fd.getHeight());
		m_CSSstyle.setAttribute("font-style", (fs & SWT.ITALIC) == 0 ? "normal" : "italic");
		m_CSSstyle.setAttribute("font-weight", (fs & SWT.BOLD) == 0 ? "normal" : "bold");
		m_CSSstyle.setAttribute("color", SlideUtilities.getAmbientColor(m_context));
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#dispose()
	 */
	public void dispose() {
		super.dispose();
		
		SlideUtilities.releaseAmbientProperties(m_context);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#getGUIHandler(java.lang.Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		} else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		return new BlockScreenspace(hintedWidth, m_height != -1 ? m_height : hintedHeight);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBlockGUIHandler#draw(org.eclipse.swt.graphics.GC, org.eclipse.swt.graphics.Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		Font oldFont = gc.getFont();
		Color oldColor = gc.getForeground();
		
		gc.setFont(SlideUtilities.getAmbientFont(m_context));
		gc.setForeground(SlideUtilities.getAmbientColor(m_context));
		
		gc.drawText(m_string, r.x + 2, r.y + 2);
		
		gc.setFont(oldFont);
		gc.setForeground(oldColor);

	}

	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.text(m_string, m_CSSstyle, this, m_tooltip, "QuickPropertyTextPart");
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.WIDTH | (m_height == -1 ? IBlockGUIHandler.HEIGHT : 0);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBlockGUIHandler#setBounds(org.eclipse.swt.graphics.Rectangle)
	 */
	public void setBounds(Rectangle r) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
	}
}

