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

package edu.mit.lcs.haystack.adenine.query;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.Resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Dennis Quan
 */
public class Condition implements Serializable {
	protected Resource m_function;
	protected ArrayList m_parameters;
	
	/**
	 * Constructor for Condition.
	 */
	public Condition(Resource function) {
		this(function, new ArrayList());
	}

	/**
	 * Constructor for Condition.
	 */
	public Condition(Object function, ArrayList parameters) {
		this((Resource) function, parameters);
	}

	/**
	 * Constructor for Condition.
	 */
	public Condition(Resource function, ArrayList parameters) {
		m_function = function;
		m_parameters = parameters;
	}

	public Resource getFunction() {
		return m_function;
	}
	
	public Iterator getParameterIterator() {
		return m_parameters.iterator();
	}
	
	public int getParameterCount() {
		return m_parameters.size();
	}
	
	public Object[] getParameters() {
		return m_parameters.toArray();
	}
	
	public Object getParameter(int i) {
		return m_parameters.get(i);
	}

	public int hashCode() {
		// want to do: return m_function.hashCode() + m_parameters.hashCode();
		// problem though is with wildcards, we would really need to take into account the variables/existentials
		//  used, but for now give each wildcard a fixed hashcode, say 20 
		int hashCode = m_function.hashCode();
		
		// for parameters use similar to the default: java.util.hashCode
		Iterator i = m_parameters.iterator();
		while (i.hasNext()) {
			Object obj = i.next();
			int objHashCode = 0;
			if (obj instanceof Resource && (((Resource) obj).getURI().indexOf(Constants.s_wildcard_namespace) == 0)) {
				objHashCode = 20;
			} else {
				objHashCode = (obj==null ? 0 : obj.hashCode());
			}
			hashCode = 31*hashCode + objHashCode;
		}
		return hashCode;	
	}

}
