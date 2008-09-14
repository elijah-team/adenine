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

import edu.mit.lcs.haystack.adenine.tokenizer.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class BlockFilterTokenIterator implements ITokenIterator {
	ITokenIterator 	m_tokenIterator;
	IndentToken		m_indentToken;
	LinkedList		m_tokens = new LinkedList();
	boolean		m_eof = false;
	boolean		m_newLine = true;
	
	public BlockFilterTokenIterator(ITokenIterator tokenIterator) {
		m_tokenIterator = tokenIterator;
		
		Token token = m_tokenIterator.getToken();
		
		if (token instanceof IndentToken) {
			m_indentToken = (IndentToken) token;
		} else {
			Location	l = (Location) token.getSpan().getStart();
			Span 		span = new Span(l, l);
			
			m_indentToken = new IndentToken(span);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#getToken()
	 */
	public Token getToken() {
		return getToken(0);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#getToken(int)
	 */
	public Token getToken(int ahead) {
		fetchSeveralTokens(ahead + 1);
		
		if (ahead < m_tokens.size()) {
			return (Token) m_tokens.get(ahead);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#swallow()
	 */
	public void swallow() {
		swallow(1);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#swallow(int)
	 */
	public void swallow(int count) {
		fetchSeveralTokens(count);
		
		int actualCount = Math.min(count, m_tokens.size());
		
		while (actualCount > 0) {
			m_tokens.remove(0);
			m_tokenIterator.swallow();
			actualCount--;
		}
	}

	protected void fetchSeveralTokens(int count) {
		if (!m_eof && count > m_tokens.size()) {
			while (m_tokens.size() < count) {
				Token token = m_tokenIterator.getToken(m_tokens.size());
				
				if (m_newLine &&
					!(token instanceof WhitespaceToken ||
						token instanceof NewLineToken ||
						token instanceof CommentToken
					)
				) {
					if (!((token instanceof IndentToken) &&
							m_indentToken.isIdenticalOrOuterLevelOf((IndentToken) token)
						 )
						) {
							
						m_eof = true;
						break;
					} 
				}

				m_newLine = token instanceof NewLineToken;
				
				m_tokens.add(token);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator#getLocation()
	 */
	public Location getLocation() {
		return m_tokenIterator.getLocation();
	}
}