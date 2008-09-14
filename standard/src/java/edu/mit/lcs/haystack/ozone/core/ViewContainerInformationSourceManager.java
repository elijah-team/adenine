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

package edu.mit.lcs.haystack.ozone.core;

import java.util.Arrays;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class ViewContainerInformationSourceManager
	extends InformationSourceManager {
	protected IViewContainerPart m_vcp;
	
	public ViewContainerInformationSourceManager(Context context, IRDFContainer source, Resource infoSourceSpec, IViewContainerPart vcp) {
		super(context, source, infoSourceSpec);
		m_vcp = vcp;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager#notifyRefresh()
	 */
	protected void notifyRefresh() {
		m_vcp.refresh();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager#detectInformationSources(edu.mit.lcs.haystack.rdf.Resource)
	 */
	protected void detectInformationSources(Resource resource) {
		// Check for info:knowsAbout
		Resource[] resKnowsAbout = Utilities.getResourceSubjects(Constants.s_info_knowsAbout, resource, m_source);
		m_detectedInformationSources.addAll(Arrays.asList(resKnowsAbout));
		if (m_infoSourceSpec != null) {
			for (int i = 0; i < resKnowsAbout.length; i++) {
				try {
					m_source.add(new Statement(m_infoSourceSpec, detectedInformationSource, resKnowsAbout[i]));			
				} catch (RDFException e) {
				}
			}
		}
	}

}
