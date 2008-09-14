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

import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataProviderWrapper;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Karun Bakshi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DynamicViewContainerPart extends ViewContainerPart {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(DynamicViewContainerPart.class);

	final public static Resource s_viewPartClassSource	= new Resource(OzoneConstants.s_namespace + "viewPartClassSource");
	
	protected ResourceDataConsumer			m_viewPartClassDataConsumer;
	protected ResourceDataProviderWrapper	m_viewPartClassDataProviderWrapper;

	
	protected void registerViewPartClass() {
		Resource dataSource = Utilities.getResourceProperty(m_prescription, s_viewPartClassSource, m_source);
		
		if (dataSource != null) {
			IDataProvider dp = DataUtilities.createDataProvider(dataSource, m_context, m_source);
			if (dp != null) {
				m_viewPartClassDataProviderWrapper = 
					new ResourceDataProviderWrapper(dp);
				
				m_viewPartClassDataConsumer = new ResourceDataConsumer() {
					protected void onResourceChanged(Resource newResource) {
						m_context.putProperty(OzoneConstants.s_viewPartClass, newResource);
						
						if (m_resUnderlying != null) {
							navigate(m_resUnderlying, m_resViewInstance);
						}
					}

					protected void onResourceDeleted(Resource previousResource) {
					}
				};
				
				dp.registerConsumer(m_viewPartClassDataConsumer);
			}
		}
	}
	
	public void dispose() {
		if (m_viewPartClassDataProviderWrapper != null) {
			m_viewPartClassDataProviderWrapper.getDataProvider().unregisterConsumer(
				m_viewPartClassDataConsumer);
			m_viewPartClassDataProviderWrapper.dispose();
			m_viewPartClassDataProviderWrapper = null;
			
			m_viewPartClassDataConsumer = null;
		}
		super.dispose();
	}
}
