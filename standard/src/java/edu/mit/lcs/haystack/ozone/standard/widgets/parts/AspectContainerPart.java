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

import java.util.Iterator;
import java.util.List;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AspectContainerPart extends SingleChildContainerPartBase {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PartContainerPart.class);
	
	protected IDataProvider m_dataProvider = null;
	protected boolean m_ownsDataProvider = false;
	protected Resource m_partClass;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.SingleChildContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_ownsDataProvider) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}
	
	protected void internalInitialize() {
		m_partClass = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_partClass, m_partDataSource);
		
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource chainedDataSource = (Resource) Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (chainedDataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(chainedDataSource, m_context, m_partDataSource, m_partDataSource);
				m_ownsDataProvider = m_dataProvider != null;
			}
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(new ResourceDataConsumer() {
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceChanged(Resource newResource) {
					initializeChild(newResource);
				}
				
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceDeleted(Resource previousResource) {
					initializeChild(null);
				}
			});
		}
	}

	static public IVisualPart instantiateAspect(Resource aspect, IRDFContainer source, IRDFContainer partDataSource, Context childContext, Resource partClass) throws Exception {
		Resource[]	types = Utilities.getResourceProperties(aspect, Constants.s_rdf_type, partDataSource);
		List typePriorityList = Utilities.getTypePriorityList(types, source);
		typePriorityList.add(Constants.s_rdf_Property);
		Iterator i = typePriorityList.iterator();
		while (i.hasNext()) {
			Resource type = (Resource) i.next();
			RDFNode[] part = null;
			
			if (partClass != null) {
				part = source.queryExtract(new Statement[] {
					new Statement(Utilities.generateWildcardResource(1), OzoneConstants.s_dataDomain, type),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, partClass),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, Constants.s_lensui_LensPart),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, OzoneConstants.s_SWTPart)
				}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
			}
			if (part == null) {
				part = source.queryExtract(new Statement[] {
					new Statement(Utilities.generateWildcardResource(1), OzoneConstants.s_dataDomain, type),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, Constants.s_lensui_LensPart),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, OzoneConstants.s_SWTPart)
				}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
			}
			if (part == null) {
				continue;
			}
					
			Class c = Utilities.loadClass((Resource) part[0], source);
			if (c == null || !IVisualPart.class.isAssignableFrom(c)) {
				continue;
			}
				
			childContext.putLocalProperty(OzoneConstants.s_part, part[0]);
			childContext.putLocalProperty(OzoneConstants.s_partData, aspect);
			childContext.putLocalProperty(OzoneConstants.s_aspect, aspect);
			return (IVisualPart) c.newInstance();
		}
		
		return null;
	}

	synchronized protected void initializeChild(Resource child) {
		if (m_child != null) {
			m_child.dispose();
		}

		if (child != null) {
			try {
				Context childContext = new Context(m_context);
				m_child = (IVisualPart) instantiateAspect(child, m_source, m_partDataSource, childContext, m_partClass);
			
				childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
				m_child.initialize(m_source, childContext);
				onChildResize(new ChildPartEvent(this));
				
				return;
			} catch (Exception e) {
				s_logger.error("Failed to initialize child part " + child, e);
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
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		
		if (m_ownsDataProvider) {
			m_dataProvider.dispose();
			m_ownsDataProvider = false;
		}
	}

}
