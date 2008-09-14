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

package edu.mit.lcs.haystack.ozone.standard.layout;

import java.io.Serializable;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 */
public class FieldData implements Serializable {
	protected Resource	m_fieldData;
	protected Resource	m_fieldID;
	protected int			m_dimension;
	protected boolean		m_resizable;
	
	protected FieldData() {}
	
	public FieldData(int dimension, boolean resizable) {
		m_fieldData = null;
		m_fieldID = null;
		m_dimension = dimension;
		m_resizable = resizable;
	}
	
	public FieldData(Resource fieldData, Resource fieldID, int dimension, boolean resizable) {
		m_fieldData = fieldData;
		m_fieldID = fieldID;
		m_dimension = dimension;
		m_resizable = resizable;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		if (arg0 == null) {
			return false;
		}
		
		FieldData fd = (FieldData) arg0;
		
		if ((m_dimension != fd.m_dimension) || (m_resizable != fd.m_resizable)) {
			return false;
		}
		
		if (((m_fieldData == null) && (fd.m_fieldData != null)) ||
			((m_fieldData != null) && (fd.m_fieldData == null)) ||
			((m_fieldID == null) && (fd.m_fieldID != null)) ||
			((m_fieldID != null) && (fd.m_fieldID == null))) {
			return false;
		}
		
		if ((m_fieldData == fd.m_fieldData) || (m_fieldID == fd.m_fieldID)) {
			return true;
		} 
		
		return m_fieldData.equals(fd.m_fieldData) && m_fieldID.equals(fd.m_fieldID);
	} 
	
	public Resource getFieldData() {
		return m_fieldData;
	}
	
	public Resource getFieldID() {
		return m_fieldID;
	}
	
	public int getDimension() {
		return m_dimension;
	}
	
	public boolean isResizable() {
		return m_resizable;
	}
}
