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

package edu.mit.lcs.haystack.adenine.parser2;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.Span;

/**
 * @author David Huynh
 */
public class SyntaxException extends HaystackException {
	protected Location	m_location;
	protected Span		m_span;
	
	public SyntaxException(String message, Location location) {
		super(message);
		m_location = location;
	}

	public SyntaxException(String message, Location location, Throwable e) {
		super(message, e);
		m_location = location;
	}

	public SyntaxException(String message, Span span) {
		super(message);
		m_span = span;
	}

	public SyntaxException(String message, Span span, Throwable e) {
		super(message, e);
		m_span = span;
	}
	
	public Span getSpan() {
		return m_span;
	}
	public Location getLocation() {
		return m_location;
	}

	protected String getThisMessage() {
//		m_exception.printStackTrace();

		if (m_span != null) {
			return super.getThisMessage() + " " + m_span;
		} else if (m_location != null) {
			return super.getThisMessage() + " " + m_location;
		} else {
			return super.getThisMessage();
		}
	}
	
	public String getMessageWithoutDetail() {
		return super.getThisMessage();
	}
}
