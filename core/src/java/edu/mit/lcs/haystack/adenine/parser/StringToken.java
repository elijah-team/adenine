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

import edu.mit.lcs.haystack.adenine.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class StringToken extends Token {
	public StringToken(int line) {
		m_line = line;
	}
	
	public int processToken(String str, int i) throws AdenineException {
		while (i < str.length()) {
			char ch = str.charAt(i++);
			if (ch == '\'') {
				return i;
			} else if (ch == '\\') {
				if (i == str.length()) {
					throw new SyntaxException("Unfinished escape sequence", m_line);
				}
				ch = LiteralToken.processEscapeSequence(str.charAt(i++));	
			}
			m_token = m_token + ch;
		}
		throw new SyntaxException("Unclosed quotation", m_line);
	}
	
	public String toString() {
		return "\'" + m_token + "\'";
	}

	public String prettyPrint(int tablevel) {
		return "\'" + LiteralToken.unescapeString(m_token) + "\'";
	}
}

