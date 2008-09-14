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

package edu.mit.lcs.haystack.adenine.tokenizer;

import java.util.Map;

/**
 * @author David Huynh
 */
public class ResourceToken extends Token {
	final String	m_prefix;
	final String	m_suffix;
	
	public ResourceToken(Span span, String prefix, String suffix) {
		super(span);
		
		m_prefix = prefix;
		m_suffix = suffix;
	}
	
	final public String resolveURI(Map prefixes) {
		if (m_prefix == null) {
			return m_suffix;
		} else {
			String prefixExpanded = (String) prefixes.get(m_prefix);
			
			if (prefixExpanded != null) {
				return prefixExpanded + m_suffix;
			} else {
				return null;
			}
		}
	}
	
	final public String getPrefix() {
		return m_prefix;
	}
	
	final public String getSuffix() {
		return m_suffix;
	}
	
	final public boolean isPrefixed() {
		return m_prefix != null;
	}

	public String getType() {
		return "Resource";
	}
}
