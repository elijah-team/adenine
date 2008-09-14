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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;

import java.util.EventObject;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class BreakElement implements IVisualPart, IBlockGUIHandler {
	Context	m_context;
	int 	m_fontSize;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
	}
	
	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context c) {
		m_context = c;
		
		Resource	resPartData = (Resource) c.getProperty(OzoneConstants.s_partData);
		
		SlideUtilities.recordAmbientProperties(c, source, resPartData);
		
		m_fontSize = SlideUtilities.getAmbientFontSize(c);
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		m_context = null;
	}

	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		return new BlockScreenspace(hintedWidth, m_fontSize, BlockScreenspace.ALIGN_TEXT_CLEAR, 0);
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
	}
		
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.breakElement();
	}
	
	
	/**
	 * Returns the nearest ancestor of this IVisualPart that responds to
	 * mouse clicks.  Returns null if there is no such ancestor.
	 * 
	 * @see IVisualPart#getClickHandler()
	 */
	public IVisualPart getClickHandler() { 
		Object obj = m_context.getLocalProperty(OzoneConstants.s_parentPart);
		if (!(obj instanceof IVisualPart)) return null;
		IVisualPart parent = (IVisualPart)obj;
		return parent.getClickHandler();
	}

	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
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
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
	}

	/**
	 * @see IVisualPart#setFocus()
	 */
	public void setFocus() {
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}
	
	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		} else {
			return null;
		}
	}

	/**
	 * @see IGUIHandler@setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
	}

	/**
	 * @see IGUIHandler#handleGUIEvent(Resource, EventObject)
	 */
	public boolean handleGUIEvent(Resource eventType, EventObject event) {
		return false;
	}	

}
