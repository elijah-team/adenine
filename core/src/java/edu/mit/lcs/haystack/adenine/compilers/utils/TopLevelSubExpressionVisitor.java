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

package edu.mit.lcs.haystack.adenine.compilers.utils;

import edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.SyntaxException;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;
import edu.mit.lcs.haystack.rdf.RDFNode;

/**
 * @author David Huynh
 */
public class TopLevelSubExpressionVisitor extends ParserVisitorBase implements ISubExpressionVisitor {
	TopLevelVisitorBase 				m_topLevelVisitor;

	RDFNode								m_result;
	
	public TopLevelSubExpressionVisitor(TopLevelVisitorBase topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onIdentifier(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.Token)
	 */
	public void onIdentifier(SymbolToken backquoteT, Token identifier) {
		if (identifier instanceof GenericToken && ((GenericToken) identifier).getContent().equals("^")) {
			m_result = m_topLevelVisitor.getBase();
			if (backquoteT != null) {
				onException(new SyntaxException("Backquote not allowed in top level model", backquoteT.getSpan()));
			}
		} else {
			onException(new SyntaxException("Identifier not allowed in top level model", identifier.getSpan()));
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onResource(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken)
	 */
	public void onResource(ResourceToken resourceToken) {
		m_result = m_topLevelVisitor.resolveURI(resourceToken);
		if (m_result == null) {
			onException(new SyntaxException("Unknown prefix " + resourceToken.getPrefix(), resourceToken.getSpan()));
		}
	}

	public RDFNode getRDFNode() {
		if (m_result != null) {
			return m_result;
		} else {
			return null;
		}
	}
}
