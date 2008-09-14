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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.Pair;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		David Huynh
 * @author		Dennis Quan
 */
public class TemplatePartContainerPart extends SingleChildContainerPartBase {
	final static Resource s_template = new Resource(OzoneConstants.s_namespace + "template");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TemplatePartContainerPart.class);
	
	protected Context m_childContext;
	protected Resource m_cacheStyle;
	
	protected void internalInitialize() {
		m_context.putLocalProperty(PartConstants.s_templatePartData, m_prescription);

		m_cacheStyle = Utilities.getResourceProperty(m_resPart, PartConstants.s_cacheStyle, m_partDataSource);
		if (m_cacheStyle == null) {
			m_cacheStyle = PartConstants.s_cacheAcrossInstances;
		}
		
		Resource childPrescription = Utilities.getResourceProperty(m_resPart, s_template, m_partDataSource);
		if (childPrescription != null) {
			if (PartConstants.s_cacheAcrossInstances.equals(m_cacheStyle)) {
				m_child = (IVisualPart) PartCache.deserialize(childPrescription, this, m_context, m_source);
				if (m_child != null) {
					return;
				}
			} else if (PartConstants.s_cachePerResource.equals(m_cacheStyle)) {
				m_child = (IVisualPart) PartCache.deserialize(new Pair(childPrescription, m_resUnderlying == null ? m_prescription : m_resUnderlying), this, m_context, m_source);
				if (m_child != null) {
					return;
				}
			}
		
			try {
				Resource childPart = Ozone.findPart(childPrescription, m_source, m_partDataSource);
				Class c = Utilities.loadClass(childPart, m_source);
				
				if (IVisualPart.class.isAssignableFrom(c)) {
					m_childContext = createChildContext(childPrescription, childPart);
					m_child = (IVisualPart) c.newInstance();
					m_child.initialize(m_source, m_childContext);
					
					if (PartConstants.s_cacheAcrossInstances.equals(m_cacheStyle)) {
						PartCache.serialize(childPrescription, this, m_context, m_child, m_childContext);
					} else if (PartConstants.s_cachePerResource.equals(m_cacheStyle)) {
						PartCache.serialize(new Pair(childPrescription, m_resUnderlying == null ? m_prescription : m_resUnderlying), this, m_context, m_child, m_childContext);
					}
				}
			} catch (Exception e) {
				s_logger.error("Failed to initialize child part " + childPrescription, e);
			}
		}
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (m_child != null) {
			return m_child.getGUIHandler(cls);
		} else {
			return null;
		}
	}
}
