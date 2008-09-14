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

package edu.mit.lcs.haystack.adenine.parser2.constructs;

import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

/**
 * @author David Huynh
 */
public class WithParser implements IConstructParser {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IConstructParser#parseConstruct(edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator, edu.mit.lcs.haystack.adenine.parser2.Parser, edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor)
	 */
	public Location parseConstruct(
		ITokenIterator 		tIterator,
		IConstructVisitor 	constructVisitor, IndentToken indentToken
	) {
		IWithVisitor visitor =
			(constructVisitor instanceof IWithVisitor) ? 
			(IWithVisitor) constructVisitor :
			new IWithVisitor() {
				public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
					return new NullAttributeVisitor(m_constructVisitor);
				}

				public void onLHS(GenericToken withKeyword, GenericToken variable) {
				}

				public IExpressionVisitor onRHS(Location location) {
					return new NullExpressionVisitor(m_constructVisitor);
				}

				public ICodeBlockVisitor onBody() {
					return new NullCodeBlockVisitor(m_constructVisitor);
				}

				public void start(Location startLocation) {
					m_constructVisitor.start(startLocation);
				}
		
				public void end(Location endLocation) {
					m_constructVisitor.end(endLocation);
				}
		
				public void onException(Exception exception) {
					m_constructVisitor.onException(exception);
				}
						
				public IWithVisitor init(IConstructVisitor constructVisitor) {
					m_constructVisitor = constructVisitor;
					return this;
				}
						
				IConstructVisitor m_constructVisitor;
			}.init(constructVisitor);
		
		visitor.start(tIterator.getLocation());
		
		Token 		token = tIterator.getToken();
		Location	endLocation = null;
				
		if (token instanceof GenericToken && ((GenericToken) token).getContent().equals("with")) {
			GenericToken with = (GenericToken) token;
			
			tIterator.swallow();
			ParserUtilities.skipWhitespacesAndComments(tIterator);
			
			token = tIterator.getToken();
			if (ParserUtilities.isPotentialIdentifier(token)) {
				GenericToken variable = ParserUtilities.parseCompoundGenericToken(tIterator, token, true);
				
				visitor.onLHS(with, variable);
			} else {
				visitor.onException(new SyntaxException("Unexpected variable name after end of with", token.getSpan()));
			}

			ParserUtilities.skipWhitespacesAndComments(tIterator);
			
			ParserUtilities.parseExpression(tIterator, visitor.onRHS(tIterator.getLocation()), false);
			
			endLocation = tIterator.getLocation();

			token = ParserUtilities.skipToNextLine(tIterator);
			if (token != null) { 
				visitor.onException(new SyntaxException("Unexpected code after end of with", token.getSpan()));
			}

			ParserUtilities.skipWhitespacesCommentsAndNewLines(tIterator);
					
			token = tIterator.getToken();
			if (token instanceof IndentToken && indentToken.isOuterLevelOf((IndentToken) token)) {
				endLocation = ParserUtilities.parseCodeBlock(tIterator, visitor.onBody(), (IndentToken) token);
			}
		} else {
			visitor.onException(new SyntaxException("Expected while keyword", tIterator.getLocation()));
			ParserUtilities.skipToNextLine(tIterator);
		}
				
		if (endLocation == null) {
			endLocation = tIterator.getLocation();
		}

		visitor.end(endLocation);
		
		return endLocation;
	}

}
