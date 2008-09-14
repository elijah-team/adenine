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

package edu.mit.lcs.haystack.ozone.standard.behaviors;

import java.util.EventObject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBehavior;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.INavigationMaster;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;
import edu.mit.lcs.haystack.ozone.data.*;

/**
 * Causes navigation to a specific resource.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class HyperlinkBehavior extends GenericPart implements IBehavior {
	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context c) {
		setupSources(source, c);
	}

	/**
	 * @see IBehavior#activate(Resource, IPart, EventObject)
	 */
	public void activate(Resource resElement, IPart part, EventObject event) {
		Resource res = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_target, m_source);
		if (res != null) {
			navigate(res, event instanceof MouseEvent ? (MouseEvent) event : null);
		} else {
			Resource 		dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_source);
			IDataProvider 	dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_source);
			
			if (dataProvider != null) {
				dataProvider.registerConsumer(new ResourceDataConsumer() {
					protected void onResourceChanged(Resource newResource) {
						navigate(newResource, m_mouseEvent);
					}
					protected void onResourceDeleted(Resource previousResource) {
					}
					MouseEvent m_mouseEvent;
					ResourceDataConsumer init(MouseEvent me) {
						m_mouseEvent = me;
						return this;
					}
				}.init(event instanceof MouseEvent ? (MouseEvent) event : null));
				dataProvider.dispose();
			}
		}
	}

	void navigate(Resource res, MouseEvent me) {
		if (me != null && ((me.stateMask & SWT.SHIFT) != 0)) {
			IBrowserWindow bw = (IBrowserWindow) m_context.getProperty(OzoneConstants.s_browserWindow);
			if (bw != null) {
				bw.navigate(res, null, true);
				return;
			}
		}
		
		IBrowserWindow bw = (IBrowserWindow) m_context.getProperty(OzoneConstants.s_browserWindow);
		if (bw != null) {
			bw.navigate(res, null, false);
			return;
		} else {
			((INavigationMaster) m_context.getProperty(OzoneConstants.s_navigationMaster)).requestViewing(res);
		}
	}
}
