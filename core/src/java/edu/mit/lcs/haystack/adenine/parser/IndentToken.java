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

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class IndentToken extends Token {
	public IndentToken(int line) {
		m_line = line;
	}
	
	public String processIndent(String s) {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		while (i < s.length()) {
			char ch = s.charAt(i);
			if ((ch != ' ') && (ch != '\t')) {
				break;
			} 
			sb.append(ch);
			++i;
		}
		m_token = sb.toString();
		return s.substring(i);
	}
	
	public boolean sameIndentation(IndentToken it) {
		return it.m_token.equals(m_token);
	}
	
	public String toString() {
		return "\n" + m_token;
	}
}

