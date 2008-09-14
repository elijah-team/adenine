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
 * Created on Jun 30, 2003
 */
package edu.mit.lcs.haystack.ozone.standard.layout;

import java.util.List;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.AspectContainerPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartCache;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class AspectFieldSetPart extends FieldSetPart {
	protected void processElementsAdded(int index, List addedElements) {
		boolean postLayout = false;
		
		for (int i = 0; i < addedElements.size(); i++) {
			try {
				FieldData	fieldData = (FieldData) addedElements.get(i);
				IVisualPart visualPart;		
				if ((m_cachedFieldParts != null) && m_cachedFieldParts.containsKey(fieldData.m_fieldID)) {
					visualPart = (IVisualPart) m_cachedFieldParts.remove(fieldData.m_fieldID);
					visualPart.initializeFromDeserialization(m_source); 
				} else {
					Resource	childPartData = fieldData.m_fieldID;
					visualPart = (IVisualPart) PartCache.deserialize(childPartData, this, m_context, m_source);
					if (visualPart == null) {
						Context		childContext = new Context(m_context);
						visualPart = AspectContainerPart.instantiateAspect(childPartData, m_source, m_partDataSource, childContext, m_partClass);
					
						childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
						visualPart.initialize(m_source, childContext);
						PartCache.serialize(childPartData, this, m_context, visualPart, childContext);
					}
				}
				
				m_childParts.add(index + i, visualPart);
				m_fields.add(index + i, fieldData);
				m_childData.add(index + i, new FieldLocation(fieldData.getDimension(), calculateChildPartSize(visualPart, fieldData.getDimension()), fieldData.isResizable()));
			} catch (Exception e) {
				s_logger.error("Failed to process newly added element", e);
			}
		}
		
		if (!m_initializing) {
			layoutChildParts(postLayout, true);
		}
	}
	
	protected Resource m_partClass;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.FieldSetPart#internalInitialize()
	 */
	protected void internalInitialize() {
		m_partClass = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_partClass, m_partDataSource);
		
		super.internalInitialize();
	}
}
