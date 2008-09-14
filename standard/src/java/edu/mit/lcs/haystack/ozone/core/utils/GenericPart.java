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
 * Created on Jun 13, 2003
 */
package edu.mit.lcs.haystack.ozone.core.utils;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
abstract public class GenericPart implements IPart {
	transient protected IRDFContainer m_source;
	transient protected IRDFContainer m_partDataSource;
	transient protected IRDFContainer m_infoSource;
	protected Context m_context;
	protected Resource m_prescription;
	protected Resource m_resPart;

	/*private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
		try {
			out.defaultWriteObject();
		} catch (java.io.IOException e) {
			System.out.println(">> failed to write out object " + this + " " + e);
			throw e;
		}
	}*/

	protected void setupSources(IRDFContainer source, Context context) {
		m_context = context;
		m_source = source;
		m_infoSource = context.getInformationSource();
		if (m_infoSource == null) m_infoSource = source;

		if (Boolean.TRUE.equals(context.getProperty(OzoneConstants.s_partDataFromInformationSource)))
			m_partDataSource = m_infoSource;
		else m_partDataSource = source;

		m_resPart = (Resource) context.getLocalProperty(OzoneConstants.s_part);
		m_prescription = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		m_context = null;
		m_disposed = true;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		m_source = source;
	
		m_infoSource = m_context.getInformationSource();
	
		if (Boolean.TRUE.equals(m_context.getProperty(OzoneConstants.s_partDataFromInformationSource))) {
			m_partDataSource = m_infoSource;
		} else {
			m_partDataSource = m_source;
		}
	}

	protected boolean m_disposed = false;

	public String toString() {
		return (m_disposed ? "(disposed) " : " ") + m_prescription + "/" + m_resPart;
	}
}
