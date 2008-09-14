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
public class CallParser implements IConstructParser {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IConstructParser#parseConstruct(edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator, edu.mit.lcs.haystack.adenine.parser2.Parser, edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor)
	 */
	public Location parseConstruct(
		ITokenIterator 		tIterator,
		IConstructVisitor 	constructVisitor, IndentToken indentToken
	) {
		ICallVisitor visitor = 
			(constructVisitor instanceof ICallVisitor) ? 
			(ICallVisitor) constructVisitor : 
			new ICallVisitor() {
				public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
					return new NullAttributeVisitor(m_constructVisitor);
				}

				public void onCall(GenericToken callKeyword) {
				}

				public IExpressionVisitor onCallable(Location location) {
					return new NullExpressionVisitor(m_constructVisitor);
				}

				public IExpressionVisitor onArgument(Location location) {
					return new NullExpressionVisitor(m_constructVisitor);
				}

				public IExpressionVisitor onNamedArgument(
					ResourceToken name,
					SymbolToken equalT) {
						
					return new NullExpressionVisitor(m_constructVisitor);
				}

				public void onComma(SymbolToken commaT) {
				}

				public void onResult(GenericToken variable) {
				}

				public void onNamedResult(
					ResourceToken name,
					SymbolToken equalT,
					GenericToken variable) {
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

				public ICallVisitor init(IConstructVisitor constructVisitor) {
					m_constructVisitor = constructVisitor;
					return this;
				}
				
				IConstructVisitor m_constructVisitor;
			}.init(constructVisitor);

		visitor.start(tIterator.getLocation());
		
		Token token = tIterator.getToken();
		if ((token instanceof GenericToken) && ((GenericToken) token).getContent().equals("call")) {
			visitor.onCall((GenericToken) token);
			tIterator.swallow();
			ParserUtilities.skipWhitespacesAndComments(tIterator);
		}
			
		IApplyVisitor visitor2 = new IApplyVisitor() {
			public void onLeftParenthesis(SymbolToken leftParenthesisT) {
			}

			public IExpressionVisitor onCallable(Location location) {
				return m_callVisitor.onCallable(location);
			}

			public IExpressionVisitor onArgument(Location location) {
				return m_callVisitor.onArgument(location);
			}

			public IExpressionVisitor onNamedArgument(
				ResourceToken name,
				SymbolToken equalT) {
					
				return m_callVisitor.onNamedArgument(name, equalT);
			}

			public void onRightParenthesis(SymbolToken rightParenthesisT) {
			}

			public void start(Location startLocation) {
			}

			public void end(Location endLocation) {
			}

			public void onException(Exception exception) {
				m_callVisitor.onException(exception);
			}

			public IApplyVisitor init(ICallVisitor callVisitor) {
				m_callVisitor = callVisitor;
				return this;
			}
			
			ICallVisitor m_callVisitor;
		}.init(visitor);
		
		ParserUtilities.parseApply(tIterator, visitor2, null);
		
		ParserUtilities.skipWhitespacesAndComments(tIterator);
		
		token = tIterator.getToken();
		if ((token instanceof SymbolToken) && ((SymbolToken) token).getSymbol().equals(",")) {
			visitor.onComma((SymbolToken) token);
			
			tIterator.swallow();
			
			int count = 0;
			
			while (true) {
				ParserUtilities.skipWhitespacesAndComments(tIterator);

				token = tIterator.getToken();
				if (token instanceof GenericToken || token instanceof SymbolToken) {
					visitor.onResult(ParserUtilities.parseCompoundGenericToken(tIterator, token));
					count++;
				} else if (token instanceof ResourceToken) {
					ResourceToken name = (ResourceToken) token;
					
					tIterator.swallow();
					ParserUtilities.skipWhitespacesAndComments(tIterator);
					
					token = tIterator.getToken();
					if (!(token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals("="))) {
						visitor.onException(new SyntaxException("Expected = after named result's resource URI", token.getSpan()));
						break;
					}
					
					SymbolToken equalT = (SymbolToken) token;
					
					tIterator.swallow();
					ParserUtilities.skipWhitespacesAndComments(tIterator);
					
					token = tIterator.getToken();
					if (token instanceof GenericToken || token instanceof SymbolToken) {
						visitor.onNamedResult(
							name,
							equalT,
							ParserUtilities.parseCompoundGenericToken(tIterator, token)
						);
						count++;
					}					
				} else {
					break;
				}
			}
			
			if (count == 0) {
				visitor.onException(new SyntaxException("Expected one or more result variables", tIterator.getLocation()));
			}
		}
		
		Location endLocation = tIterator.getLocation();
		
		token = ParserUtilities.skipToNextLine(tIterator);
		if (token != null) {
			visitor.onException(new SyntaxException("Expected one or more result variables", token.getSpan()));
		}

		visitor.end(endLocation);
		
		return endLocation;
	}

}
