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

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBehavior;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;

import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class SlidePart extends VisualPartBase implements IViewPart {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SlidePart.class);
	
	BlockElement	m_block = new BlockElement();
	
	final static Resource s_slide = new Resource(SlideConstants.s_namespace + "Slide");
	
	/**
	 * Does the actual initialization work.
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		if (m_prescription == null) {
			m_context.putProperty(OzoneConstants.s_partDataFromInformationSource, Boolean.TRUE);
			m_partDataSource = m_infoSource;
			if (Utilities.isType(m_resViewInstance, s_slide, m_infoSource)) {
				m_context.putLocalProperty(OzoneConstants.s_partData, m_prescription = m_resViewInstance);
			} else {
				m_context.putLocalProperty(OzoneConstants.s_partData, m_prescription = m_resUnderlying);
			}
		}
		
		{
			Context childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, m_prescription);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			m_block.initialize(m_source, childContext);
		}
		
		try {
			Resource resBehavior = Utilities.getResourceProperty(m_prescription, SlideConstants.s_onLoad, m_partDataSource);
			if (resBehavior != null) {
				Resource resPart = Ozone.findPart(resBehavior, m_source, m_partDataSource);
				if (resPart != null) {
					IBehavior	sb = (IBehavior)Utilities.loadClass(resPart, m_source).newInstance();
					Context		childContext = new Context(m_context);
					
					childContext.putLocalProperty(OzoneConstants.s_part, resPart);
					childContext.putLocalProperty(OzoneConstants.s_partData, resBehavior);
					
					sb.initialize(m_source, childContext);
					sb.activate(m_prescription, this, null);
				}
			}
		} catch (Exception e) {
			s_logger.error("onLoad", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		m_block.initializeFromDeserialization(source);
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_block != null) {
			m_block.dispose();
		}
		m_block = null;
		
		super.dispose();
	}
	
	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		return m_block.getGUIHandler(cls);
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object e) {
		if (m_block == null) {
			return false;
		}
		
		if (!eventType.equals(PartConstants.s_eventChildResize)) {
			return m_block.handleEvent(eventType, e);
		}
		return super.handleEvent(eventType, e);
	}

	static public Iterator getChildren(IRDFContainer source, Resource resData) {
		try {
			return ListUtilities.accessDAMLList(Utilities.getResourceProperty(resData, SlideConstants.s_children, source), source);
		} catch (Exception e) {
			return null;
		}
	}
	
}
