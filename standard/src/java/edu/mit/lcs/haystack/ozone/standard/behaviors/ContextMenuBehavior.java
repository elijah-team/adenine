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

import org.eclipse.swt.graphics.Point;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBehavior;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;

/**
 * @version 	1.0
 * @author		David Huynh
 * @author		Dennis Quan
 */
public class ContextMenuBehavior extends GenericPart implements IBehavior {
	final public static Resource s_objectOnly = new Resource(OzoneConstants.s_namespace + "objectOnly");
	
	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
	}

	/**
	 * @see IBehavior#activate(Resource, IPart, EventObject)
	 */
	public void activate(Resource resElement, IPart part, EventObject event) {
		Resource resUnderlying = (Resource) Utilities.getResourceProperty(m_prescription, OzoneConstants.s_underlying, m_partDataSource);
		if (resUnderlying != null) {
			m_context.putLocalProperty(OzoneConstants.s_underlying, resUnderlying);
			Point pt = Ozone.s_display.getCursorLocation();
			
			int maxCount = 3;
			if (Utilities.checkBooleanProperty(m_prescription, s_objectOnly, m_partDataSource)) {
				maxCount = 1;
			}
			
			PartUtilities.showContextMenu(m_source, m_context, pt, maxCount);
		}
	}
}
