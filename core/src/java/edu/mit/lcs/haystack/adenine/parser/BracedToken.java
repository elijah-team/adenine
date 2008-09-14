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

package edu.mit.lcs.haystack.adenine.parser;

import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class BracedToken extends ContainerToken {
	public BracedToken(int line) {
		m_line = line;
		m_percent = false;
		m_dollar = false;
	}
	
	public boolean m_dollar;
	public boolean m_percent;
	
	public String toString() {
		return (m_dollar ? "$" : "") + (m_percent ? "%" : "") + "{" + super.toString() + "}";
	}
	
	public String prettyPrint(int tablevel) {
		StringBuffer sb = new StringBuffer(m_dollar ? "${" : (m_percent ? "%{" : "{"));
		Iterator i = m_tokens.iterator();
		StringBuffer space = new StringBuffer(" ");
		boolean longString = false;
		if (toString().length() > 60) {
			++tablevel;
			longString = true;
			space = new StringBuffer("\n");
			addTabs(space, tablevel);
		}
		int c = m_dollar ? 1 : 3;
		while (i.hasNext()) {
			PrettyPrintable pp = (PrettyPrintable)i.next();
			if (c == 3) {
				sb.append(space);
				c = 0;
			} else {
				sb.append(" ");
			}
			sb.append(pp.prettyPrint(tablevel));
			++c;
		}
		
		if (longString) {
			sb.append("\n");
			addTabs(sb, tablevel - 1);
			sb.append("}");
		} else {
			sb.append(" }");
		}
		return sb.toString();
	}
}

