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

import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class InformationSourceManagedContainerPart extends SingleChildContainerPartBase {
	transient protected InformationSourceManager m_infoSourceManager;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(InformationSourceManagedContainerPart.class);

	protected boolean isInitialized() {
		return m_child != null;
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		super.dispose();

		if (m_context != null) {
			SlideUtilities.releaseAmbientProperties(m_context);
		}
		
		if (m_infoSourceManager != null) {
			m_infoSourceManager.dispose();
		}
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (isInitialized()) {
			return m_child.getGUIHandler(cls);
		} else {
			return null;
		}
	}

	protected void internalInitialize() {
		super.internalInitialize();
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		setupInformationSourceManager();
		initChild();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.SingleChildContainerPartBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		setupInformationSourceManager();
	}
	
	protected void setupInformationSourceManager() {
		// TODO[dquan]: change m_infoSource to m_source
		m_infoSourceManager = new InformationSourceManager(m_context, m_infoSource, Utilities.getResourceProperty(m_prescription, InformationSourceManager.informationSourceSpecification, m_partDataSource)) {
			/**
			 * @see edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager#detectInformationSources(edu.mit.lcs.haystack.rdf.Resource)
			 */
			protected void detectInformationSources(Resource resource) {
			}

			/**
			 * @see edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager#notifyRefresh()
			 */
			protected void notifyRefresh() {
				/*if (Ozone.isUIThread()) {
					initChild();
				} else*/ {
					Ozone.idleExec(new IdleRunnable(m_context) {
						/**
						 * @see java.lang.Runnable#run()
						 */
						public void run() {
							initChild();
						}
					});
				}
			}
		};
	}
	
	protected void initChild() {
		createChild();
		
		if (!m_initializing) {
			onChildResize(new ChildPartEvent(this));
		}
	}
}
