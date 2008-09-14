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

package edu.mit.lcs.haystack.adenine.interpreter;

import edu.mit.lcs.haystack.rdf.Resource;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Message implements Serializable {
	public Message() {
		m_values = new Object[] { null };
	}
	
	public Message(Object value) {
		m_values = new Object[] { value };
	}
	
	public Message(Object[] values) {
		if (values == null) {
			throw new IllegalArgumentException();
		}
		m_values = values;
	}
	
	public Message(Object[] values, Map namedValues) {
		if (values == null) {
			throw new IllegalArgumentException();
		}
		m_values = values;
		m_namedValues = namedValues;
	}

	public Message(java.util.List values, Map namedValues) {
		if (values == null) {
			throw new IllegalArgumentException();
		}
		m_values = values.toArray();

		m_namedValues = namedValues;
	}
	
	public Object[] m_values;
	public Map m_namedValues = null;
	
	public void setNamedValue(Resource name, Object value) {
		if (m_namedValues == null) {
			m_namedValues = new HashMap();
		}
		
		m_namedValues.put(name, value);
	}
	
	public Object getPrimaryValue() {
		return m_values[0];
	}

	public Object getPrimaryValueChecked() {
		return m_values.length > 0 ? m_values[0] : null;
	}
	
	public Object[] getOrderedValues() {
		return m_values;
	}
	
	public Map getNamedValues() {
		return m_namedValues;
	}
	
	public Object getNamedValue(Resource name) {
		if ((m_namedValues != null) && m_namedValues.containsKey(name)) {
			return m_namedValues.get(name);
		} else {
			return null;
		}
	}
}
