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
 * Created on Apr 22, 2003
 */
package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class DataProviderHostPart extends SingleChildContainerPartBase {
	IDataProvider m_dataProvider;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		m_dataProvider = DataUtilities.createDataProvider(Utilities.getResourceProperty(m_prescription, PartConstants.s_hostedDataProvider, m_partDataSource), m_context, m_source, m_partDataSource);
		Resource[] propertyNames = Utilities.getResourceProperties(m_prescription, PartConstants.s_propertyName, m_partDataSource);
		Resource child = Utilities.getResourceProperty(m_prescription, SlideConstants.s_child, m_partDataSource);
		
		try {
			Resource part = Ozone.findPart(child, m_source, m_partDataSource);
			Class c = Utilities.loadClass(part, m_source);
				
			if (IVisualPart.class.isAssignableFrom(c)) {
				Context childContext = new Context(m_context);
					
				childContext.putLocalProperty(OzoneConstants.s_part, part);
				childContext.putLocalProperty(OzoneConstants.s_partData, child);
				childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
				for (int i = 0; i < propertyNames.length; i++) {
					childContext.putProperty(propertyNames[i], m_dataProvider);
				}
					
				m_child = (IVisualPart) c.newInstance();
				m_child.initialize(m_source, childContext);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		
		if (m_dataProvider != null) {
			m_dataProvider.dispose();
			m_dataProvider = null;
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.SingleChildContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		if (m_dataProvider != null) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}
}
