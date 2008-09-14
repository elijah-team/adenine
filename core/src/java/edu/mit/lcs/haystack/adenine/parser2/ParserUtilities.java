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

import edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.CommentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.FloatToken;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator;
import edu.mit.lcs.haystack.adenine.tokenizer.IndentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken;
import edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.NewLineToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Span;
import edu.mit.lcs.haystack.adenine.tokenizer.StringToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;
import edu.mit.lcs.haystack.adenine.tokenizer.WhitespaceToken;

/**
 * @author David Huynh
 */
final public class ParserUtilities {

	static public void skipAllWhitespaces(ITokenIterator tIterator) {
		while (true) {
			Token token = tIterator.getToken();
			
			if (token instanceof WhitespaceToken ||
				token instanceof NewLineToken ||
				token instanceof IndentToken ||
				token instanceof CommentToken) {
					
				tIterator.swallow();
				continue;
			} 
			break;
		}
	}

	static public Token skipToNextLine(ITokenIterator tIterator) {
		Token unwantedToken = null;
		 
		while (true) {
			Token token = tIterator.getToken();
			
			tIterator.swallow();
			
			if (token == null || token instanceof NewLineToken) {
				break;
			} else if ((unwantedToken == null) && !(token instanceof WhitespaceToken) && !(token instanceof CommentToken)) {
				unwantedToken = token;
			}
		}
		
		return unwantedToken;
	}

	static public void skipWhitespacesAndComments(ITokenIterator tIterator) {
		while (true) {
			Token token = tIterator.getToken();
			
			if (token instanceof WhitespaceToken ||
				token instanceof CommentToken) {
					
				tIterator.swallow();
				continue;
			} 
			break;
		}
	}

	static public void skipWhitespacesCommentsAndNewLines(ITokenIterator tIterator) {
		while (true) {
			Token token = tIterator.getToken();
			
			if (token instanceof WhitespaceToken ||
				token instanceof NewLineToken ||
				token instanceof CommentToken) {
					
				tIterator.swallow();
				continue;
			} 
			break;
		}
	}

	static public GenericToken parseCompoundGenericToken(ITokenIterator tIterator, Token token, boolean swallow) {
		Location 	start = token.getSpan().getStart();
		Location	end = null;
		String		s = "";
		int		i = 0;
		
		while (true) {
			s += (token instanceof SymbolToken) ? ((SymbolToken) token).getSymbol() : ((GenericToken) token).getContent();
			end = token.getSpan().getEnd();
			
			if (swallow) {
				tIterator.swallow();
			} else {
				i++;
			}
			
			token = tIterator.getToken(i);
			
			if (token instanceof SymbolToken) {
				SymbolToken symbolToken = (SymbolToken) token;
				String		s2 = symbolToken.getSymbol();
				
				if (Parser.s_compoundGenericTokenExcepts.indexOf(s2.charAt(0)) >= 0) {
					break;
				}
			} else if (!(token instanceof GenericToken)) {
				break;
			}
		}
		
		return new GenericToken(new Span(start, end), s);
	}

	static public GenericToken parseCompoundGenericToken(ITokenIterator tIterator, Token token) {
		return ParserUtilities.parseCompoundGenericToken(tIterator, token, true);
	}

	static public void parseSeveralAttributesAtLinesEnd(ITokenIterator tIterator, IConstructVisitor visitor) {
		Token token = null;
		
		while (true) {
			skipWhitespacesAndComments(tIterator);
			
			token = tIterator.getToken();
			if (token instanceof SymbolToken) {
				SymbolToken symbolToken = (SymbolToken) token;
				String		s = symbolToken.getSymbol();
				
				if (s.equals(";")) {
					if (Parser.parseAttribute(tIterator, visitor.onAttribute(symbolToken), symbolToken)) {
						continue;
					} else {
						break;
					}
				}
			}
			
			break;
		}
		
		if (token != null) {
			if (token instanceof NewLineToken) {
				tIterator.swallow();
			} else {
				visitor.onException(new SyntaxException("Expected end of line", tIterator.getLocation()));
				skipToNextLine(tIterator);
			}
		}
	}

	static protected boolean parseAttributeAtLinesEnd(ITokenIterator tIterator, IAttributeVisitor visitor, SymbolToken semicolonToken) {
		visitor.start(tIterator.getLocation());
	
		tIterator.swallow();
		skipWhitespacesAndComments(tIterator);
		
		Token token = tIterator.getToken();

		boolean r = ParserUtilities.parseExpression(tIterator, visitor.onPredicate(token.getSpan().getStart()), false);
		if (r) {
			skipWhitespacesAndComments(tIterator);
				
			r = ParserUtilities.parseExpression(tIterator, visitor.onObject(token.getSpan().getStart()), false);
		}
						
		visitor.end(tIterator.getLocation());
		
		return r;
	}

	static public boolean isSemicolon(Token token) {
		return (token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals(";"));
	}

	static public boolean isPotentialIdentifier(Token token) {
		return token instanceof SymbolToken || token instanceof GenericToken;
	}

	static public Location parseMethod(ITokenIterator tIterator, IMethodVisitor visitor, GenericToken methodToken, boolean anonymousMethod) {
		visitor.start(methodToken.getSpan().getStart());
		
		tIterator.swallow();
		ParserUtilities.skipWhitespacesAndComments(tIterator);
			
		ParserUtilities.parseExpression(tIterator, visitor.onMethod(methodToken), false); 
		
		Token 		token;/* = tIterator.getToken();*/
		Location	endLocation = null;
		
		/*if ((!anonymousMethod && (token instanceof ResourceToken)) || (anonymousMethod && (token instanceof GenericToken))) {
			visitor.onMethod(methodToken, token);
			
			tIterator.swallow();*/
			
			// parse positional parameters
			
			ParserUtilities.skipWhitespacesAndComments(tIterator);
			token = tIterator.getToken();
	
			while (!ParserUtilities.isSemicolon(token) && ParserUtilities.isPotentialIdentifier(token)) {
				visitor.onParameter(ParserUtilities.parseCompoundGenericToken(tIterator, token, true));
	
				ParserUtilities.skipWhitespacesAndComments(tIterator);
				token = tIterator.getToken();
			}
			
			// parse named parameters
	
			while (token instanceof ResourceToken) {
				ResourceToken name = (ResourceToken) token;
				
				tIterator.swallow();
				ParserUtilities.skipWhitespacesAndComments(tIterator);
				
				token = tIterator.getToken();
				if (token instanceof SymbolToken) {
					SymbolToken equalToken = (SymbolToken) token;
					
					if (equalToken.getSymbol().equals("=")) {
						tIterator.swallow();
						ParserUtilities.skipWhitespacesAndComments(tIterator);
						
						token = tIterator.getToken();
						if (ParserUtilities.isPotentialIdentifier(token)) {
							visitor.onNamedParameter(name, equalToken, ParserUtilities.parseCompoundGenericToken(tIterator, token, true));
							
							ParserUtilities.skipWhitespacesAndComments(tIterator);
							
							token = tIterator.getToken();
							continue;
						}
					}
				}
	
				visitor.onException(new SyntaxException("Named parameters are of form: [name as URI] = [formal parameter variable]", tIterator.getLocation()));
				break;
			}
			
			// parse attributes
			
			ParserUtilities.skipWhitespacesCommentsAndNewLines(tIterator);
			token = tIterator.getToken();
			
			while (true) {
				if (token instanceof SymbolToken) {
					SymbolToken semicolonToken = (SymbolToken) token;
					
					if (semicolonToken.getSymbol().equals(";")) {
						Parser.parseAttribute(tIterator, visitor.onAttribute(semicolonToken), semicolonToken);
						
						ParserUtilities.skipWhitespacesCommentsAndNewLines(tIterator);
						token = tIterator.getToken();
						
						continue;
					}
				}
				break;
			}
			
			if (token != null && token.getSpan().getStart().getColumn() > 0) {
				visitor.onException(new SyntaxException("Unexpected token of type " + token.getType(), token.getSpan()));
				ParserUtilities.skipToNextLine(tIterator);
				token = tIterator.getToken();
			}
			
			if (token instanceof IndentToken) {
				endLocation = ParserUtilities.parseCodeBlock(tIterator, visitor.onBlock(), (IndentToken) token);
				token = tIterator.getToken();
			}
		/*} else {
			if (anonymousMethod) {
				visitor.onException(new SyntaxException("Expected a generic token for method name", tIterator.getLocation()));
			} else {
				visitor.onException(new SyntaxException("Expected a resource URI for method name", tIterator.getLocation()));
			}
			
			ParserUtilities.skipToNextLine(tIterator);
		}*/
	
		if (endLocation == null) {
			endLocation = tIterator.getLocation();
		}

		visitor.end(endLocation);
		
		return endLocation;
	}

	static public boolean parseExpression(ITokenIterator tIterator, IExpressionVisitor visitor, boolean allowEmptyExpression) {
		visitor.start(tIterator.getLocation());
		
		boolean	processed = false;
		boolean	swallow = true;
		Token 		token = tIterator.getToken();
		
		if (token instanceof IntegerToken) {
			visitor.onInteger((IntegerToken) token);
			processed = true;
		} else if (token instanceof FloatToken) {
			visitor.onFloat((FloatToken) token);
			processed = true;
		} else if (token instanceof StringToken) {
			visitor.onString((StringToken) token);
			processed = true;
		} else if (token instanceof LiteralToken) {
			visitor.onLiteral((LiteralToken) token);
			processed = true;
		} else if (token instanceof SymbolToken) {
			SymbolToken symbolToken = (SymbolToken) token;
			String		s = symbolToken.getSymbol();
			
			swallow = false;
			processed = true;
			
			Location l = token.getSpan().getStart();
			
			if (s.equals("@")) {
				ParserUtilities.parseList(tIterator, visitor.onList(l), symbolToken);
			} else if (s.equals("{")) {
				ParserUtilities.parseModel(tIterator, visitor.onModel(l), symbolToken);
			} else if (s.equals("$")) {
				ParserUtilities.parseAnonymousModel(tIterator, visitor.onAnonymousModel(l), symbolToken);
			} else if (s.equals("%")) {
				ParserUtilities.parseAskModel(tIterator, visitor.onAskModel(l), symbolToken);
			} else if (s.equals("(")) {
				ParserUtilities.parseApply(tIterator, visitor.onApply(l), symbolToken);
			} else {
				processed = false;
			}
		}
		
		if (processed) {
			if (swallow) {
				tIterator.swallow();
			}
		} else if (token != null) {
			processed = ParserUtilities.parseSubExpression(tIterator, visitor.onSubExpression(token.getSpan().getStart()), allowEmptyExpression);
		}
		
		if (processed) {
			// look ahead parse dereference and brackets
			while (true) {
				boolean foundBracket = false;
				boolean foundPeriod = false;
				
				int i = 0;
				while (true) {
					token = tIterator.getToken(i);
					
					if (token instanceof WhitespaceToken || token instanceof CommentToken) {
						i++;
						continue;
					}
					
					if (token instanceof SymbolToken) {
						String s = ((SymbolToken) token).getSymbol();
						
						if (s.equals(".")) {
							foundPeriod = true;
						} else if (s.equals("[")) {
							foundBracket = true;
						}
					}
					break;
				}
				
				if (foundBracket) {
					tIterator.swallow(i+1);
					ParserUtilities.skipWhitespacesAndComments(tIterator);
					
					processed = parseExpression(tIterator, visitor.onLeftBracket((SymbolToken) token), false);
					if (processed) {
						ParserUtilities.skipWhitespacesAndComments(tIterator);
						
						token = tIterator.getToken();
						if (token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals("]")) {
							visitor.onRightBracket((SymbolToken) token);
							tIterator.swallow();
						} else {
							visitor.onException(new SyntaxException("Expected ]", tIterator.getLocation()));
							processed = false;
						}
					}
				} else if (foundPeriod) {
					tIterator.swallow(i+1);
					ParserUtilities.skipWhitespacesAndComments(tIterator);
					
					processed = ParserUtilities.parseSubExpression(tIterator, visitor.onDereference((SymbolToken) token), false);
				} else {
					break;
				}
			}
		} else if (!allowEmptyExpression) {
			visitor.onException(new SyntaxException("Empty expression not allowed here", tIterator.getLocation()));
		}
		
		visitor.end(tIterator.getLocation());
		
		return processed;
	}

	static public void parseApply(ITokenIterator tIterator, IApplyVisitor visitor, SymbolToken leftParenthesisToken) {
		visitor.start(tIterator.getLocation());
		
		if (leftParenthesisToken != null) {
			visitor.onLeftParenthesis(leftParenthesisToken);
			
			tIterator.swallow(); // swallow (
			ParserUtilities.skipAllWhitespaces(tIterator);
		}
		
		boolean done = false;
		
		if (ParserUtilities.parseExpression(tIterator, visitor.onCallable(tIterator.getLocation()), false)) {
			// parse positional arguments
			
			while (true) {
				if (leftParenthesisToken == null) {
					ParserUtilities.skipWhitespacesAndComments(tIterator);
				} else {
					ParserUtilities.skipAllWhitespaces(tIterator);
				}
				
				Token token = tIterator.getToken();
				if (token instanceof ResourceToken) {
					int 		i = 1;
					boolean 	foundNamedParameters = false;
					
					while (true) {
						Token token2 = tIterator.getToken(i);
						
						if (token2 instanceof WhitespaceToken || 
							token2 instanceof CommentToken ||
							(leftParenthesisToken != null && 
								(token2 instanceof NewLineToken || token2 instanceof IndentToken))) {
							i++;
							continue;
						}
						
						if (token2 instanceof SymbolToken && ((SymbolToken) token2).getSymbol().equals("=")) {
							foundNamedParameters = true;
						}
						
						break;
					}
					
					if (foundNamedParameters) {
						break;
					}
				} else if (token == null || token instanceof NewLineToken) {
					if (leftParenthesisToken != null) {
						visitor.onException(new SyntaxException("Expected )", tIterator.getLocation()));
					}
					done = true;
					break;
				} else if (leftParenthesisToken != null && token instanceof SymbolToken) {
					SymbolToken symbolToken = (SymbolToken) token;
					
					if (symbolToken.getSymbol().equals(")")) {
						visitor.onRightParenthesis(symbolToken);
						tIterator.swallow();
						done = true;
						break;
					}
				}
				
				if (!ParserUtilities.parseExpression(tIterator, visitor.onArgument(token.getSpan().getStart()), false)) {
					break;
				}
			}
			
			// parse named arguments
			
			if (!done) {
				while (true) {
					if (leftParenthesisToken == null) {
						ParserUtilities.skipWhitespacesAndComments(tIterator);
					} else {
						ParserUtilities.skipAllWhitespaces(tIterator);
					}
				
					Token token = tIterator.getToken();
					
					if (token instanceof ResourceToken) {
						tIterator.swallow();
						if (leftParenthesisToken == null) {
							ParserUtilities.skipWhitespacesAndComments(tIterator);
						} else {
							ParserUtilities.skipAllWhitespaces(tIterator);
						}
						
						Token token2 = tIterator.getToken();
						if (token2 instanceof SymbolToken) {
							SymbolToken symbolToken = (SymbolToken) token2;
							String		s = symbolToken.getSymbol();
							
							if (s.equals("=")) {
								tIterator.swallow();
								if (leftParenthesisToken == null) {
									ParserUtilities.skipWhitespacesAndComments(tIterator);
								} else {
									ParserUtilities.skipAllWhitespaces(tIterator);
								}
								
								if (ParserUtilities.parseExpression(tIterator, visitor.onNamedArgument((ResourceToken) token, symbolToken), false)) {
									continue;
								} else {
									break;
								}
							}
						}
					} else if (token == null || (leftParenthesisToken == null && token instanceof NewLineToken)) {
						if (leftParenthesisToken != null) {
							visitor.onException(new SyntaxException("Expected )", tIterator.getLocation()));
						}
						done = true;
						break;
					} else if (leftParenthesisToken != null && token instanceof SymbolToken) {
						SymbolToken symbolToken = (SymbolToken) token;
					
						if (symbolToken.getSymbol().equals(")")) {
							visitor.onRightParenthesis(symbolToken);
							tIterator.swallow();
							done = true;
							break;
						}
					}
		
					visitor.onException(new SyntaxException("Expected named argument in form: [name as URI] = [value expression]", tIterator.getLocation()));
					break;
				}
			}
		}
				
		if (!done && leftParenthesisToken != null) {
			visitor.onException(new SyntaxException("Expected )", tIterator.getLocation()));
	
			// try to search for closing ) or new line
			while (true) {
				Token token = tIterator.getToken();
				
				if (token == null) {
					break;
				}
				
				tIterator.swallow();
				if (token instanceof NewLineToken) {
					break;
				} else if (token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals(")")) {
					break;
				}
			}
		}
		
		visitor.end(tIterator.getLocation());
	}

	/*
	 * 	Returns the token following the block, if any.
	 */
	static public Location parseCodeBlock(ITokenIterator tIterator, ICodeBlockVisitor visitor, IndentToken indentToken) {
		visitor.start(tIterator.getLocation());
		visitor.setFirstIndent(indentToken);
		
		ITokenIterator 	tIterator2 = new BlockFilterTokenIterator(tIterator);
		Location		returnLocation = tIterator.getLocation();
		
		while (true) {
			Token token = tIterator2.getToken();
			
			if (token == null) {
				break;
			} else if (token instanceof IndentToken) {
				if (((IndentToken) token).isIdenticalTo(indentToken)) {
					tIterator2.swallow();
					
					returnLocation = Parser.parseConstruct(tIterator2, visitor, (IndentToken) token, false);
				} else {
					break;
				}
			} else {
				tIterator2.swallow();
			}
		}
		
		visitor.end(returnLocation);
		
		return returnLocation;
	}

	static protected void parseModel(ITokenIterator tIterator, IModelVisitor visitor, SymbolToken leftBraceToken) {
		Token 		token = null;
		boolean	skipToEnd = false;
	
		visitor.start(leftBraceToken.getSpan().getStart());
		visitor.onModelStart(leftBraceToken);
		tIterator.swallow(); // swallow {
	
		while (true) {
			ParserUtilities.skipAllWhitespaces(tIterator);
			
			token = tIterator.getToken();
			
			if (token == null) {
				break;
			} else {
				if (token instanceof SymbolToken) {
					SymbolToken symbolToken = (SymbolToken) token;
					String		s = symbolToken.getSymbol();
					
					if (s.equals("}")) {
						visitor.onModelEnd(symbolToken);
						tIterator.swallow();
						break;						
					}
				}
				 
				if (ParserUtilities.parseExpression(tIterator, visitor.onSubject(token.getSpan().getStart()), false)) {
					if (!Parser.parseSeveralAttributes(tIterator, visitor)) {
						skipToEnd = true;
						break;
					}
				} else {
					skipToEnd = true;
					break;
				}
			}
		}
		
		if (skipToEnd) {
			SymbolToken symbolToken = skipToClosingBrace(tIterator);
			if (symbolToken != null) {
				visitor.onModelEnd(symbolToken);
				tIterator.swallow();
			}
		}
		
		visitor.end(tIterator.getLocation());
	}
	
	static protected SymbolToken skipToClosingBrace(ITokenIterator tIterator) {
		Token token = null;
		
		while ((token = tIterator.getToken()) != null) {
			if (token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals("}")) {
				break;
			}
			if (!ParserUtilities.parseExpression(tIterator, new NullExpressionVisitor(), false)) {
				tIterator.swallow();
			}
		}
		return (SymbolToken) token;
	}

	static protected void parseAnonymousModel(ITokenIterator tIterator, IAnonymousModelVisitor visitor, SymbolToken dollarSignToken) {
		visitor.start(dollarSignToken.getSpan().getStart());
		tIterator.swallow(); // swallow $
	
		Token token = tIterator.getToken();
		if (token instanceof SymbolToken) {
			SymbolToken leftBraceToken = (SymbolToken) token;
	
			if (leftBraceToken.getSymbol().equals("{")) {
				visitor.onModelStart(dollarSignToken, leftBraceToken);
				tIterator.swallow();
				
				ParserUtilities.skipAllWhitespaces(tIterator);
				
				Parser.parseSeveralAttributes(tIterator, visitor);
					
				ParserUtilities.skipAllWhitespaces(tIterator);
				
				token = tIterator.getToken();
				if (token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals("}")) {
					visitor.onModelEnd((SymbolToken) token);
					tIterator.swallow();
				} else {
					visitor.onException(new SyntaxException("Expected }", tIterator.getLocation()));
				}
			} else {
				visitor.onException(new SyntaxException("Expected ${", token.getSpan()));
			}
		} else {
			visitor.onException(new SyntaxException("Expected ${", token.getSpan()));
		}
	
		visitor.end(tIterator.getLocation());
	}

	static protected void parseAskModel(ITokenIterator tIterator, IAskModelVisitor visitor, SymbolToken percentSignToken) {
		visitor.start(percentSignToken.getSpan().getStart());
		tIterator.swallow(); // swallow %
	
		Token token = tIterator.getToken();
		if (token instanceof SymbolToken) {
			SymbolToken leftBraceToken = (SymbolToken) token;
	
			if (leftBraceToken.getSymbol().equals("{")) {
				visitor.onModelStart(percentSignToken, leftBraceToken);
				tIterator.swallow();
				
				boolean foundClosingBrace = false;
				
				parseCondition(tIterator, visitor, null);
				
				while (true) {
					ParserUtilities.skipAllWhitespaces(tIterator);
					
					token = tIterator.getToken();
			
					if (token == null) {
						break;
					} else {
						if (token instanceof SymbolToken) {
							SymbolToken symbolToken = (SymbolToken) token;
							String		s = symbolToken.getSymbol();
					
							if (s.equals("}")) {
								visitor.onModelEnd(symbolToken);
								tIterator.swallow();
								foundClosingBrace = true;
								break;						
							} else if (s.equals(",")) {
								tIterator.swallow();
								parseCondition(tIterator, visitor, symbolToken);
								continue;
							}
						}
					}
				}
				
				if (!foundClosingBrace) {
					visitor.onException(new SyntaxException("Expected }", tIterator.getLocation()));
				}
			} else {
				visitor.onException(new SyntaxException("Expected %{", tIterator.getLocation()));
			}
		} else {
			visitor.onException(new SyntaxException("Expected %{", tIterator.getLocation()));
		}
	
		visitor.end(tIterator.getLocation());
	}

	static protected void parseCondition(ITokenIterator tIterator, IAskModelVisitor visitor, SymbolToken commaToken) {
		ParserUtilities.skipAllWhitespaces(tIterator);
		
		int 		termCount = 0;
		Location	endingLocation = tIterator.getLocation();
		
		visitor.onConditionStart(commaToken);
		
		while (true) {
			ParserUtilities.skipAllWhitespaces(tIterator);
	
			Token token = tIterator.getToken();
			if (token instanceof SymbolToken) {
				SymbolToken symbolToken = (SymbolToken) token;
				String		s = symbolToken.getSymbol();
				
				if (s.equals("}") || s.equals(",")) {
					break;
				}
			}
	
			if (!ParserUtilities.parseExpression(tIterator, visitor.onConditionTerm(token.getSpan().getStart()), false)) {
				break;
			}
			endingLocation = tIterator.getLocation();
			
			termCount++;
		}
		
		visitor.onConditionEnd(endingLocation);
		
		if (commaToken != null && termCount == 0) {
			visitor.onException(new SyntaxException("Expected condition", tIterator.getLocation()));
		}
	}

	static protected boolean parseSubExpression(ITokenIterator tIterator, ISubExpressionVisitor visitor, boolean allowEmptyExpression) {
		visitor.start(tIterator.getLocation());
		
		boolean	processed = false;
		boolean	swallow = true;
		Token 		token = tIterator.getToken();
		
		if (token instanceof SymbolToken) {
			SymbolToken backquoteT = null;
			
			if (((SymbolToken) token).getSymbol().equals("`")) {
				backquoteT = (SymbolToken) token;
				
				tIterator.swallow();
				token = tIterator.getToken();
			}
			
			if (token instanceof GenericToken) {
				visitor.onIdentifier(backquoteT, ParserUtilities.parseCompoundGenericToken(tIterator, token, true));
	
				swallow = false;
				processed = true;
			} else if (token instanceof SymbolToken) {
				swallow = false;
				
				if ("@$%{}()[];".indexOf(((SymbolToken) token).getSymbol()) < 0) {
					visitor.onIdentifier(backquoteT, ParserUtilities.parseCompoundGenericToken(tIterator, token, true));
					
					processed = true;
				}
			}
		} else if (token instanceof ResourceToken) {
			visitor.onResource((ResourceToken) token);
			processed = true;
		} else if (token instanceof GenericToken) {
			visitor.onIdentifier(null, ParserUtilities.parseCompoundGenericToken(tIterator, token, true));
			swallow = false;
			processed = true;
		}
		
		if (processed) {
			if (swallow) {
				tIterator.swallow();
			}
		} else if (!allowEmptyExpression) {
			visitor.onException(new SyntaxException("Expected identifier or resource, possibly backquoted", tIterator.getLocation()));
		}
	
		visitor.end(tIterator.getLocation());
		
		return processed;
	}

	static protected void parseList(ITokenIterator tIterator, IListVisitor visitor, SymbolToken atSignToken) {
		visitor.start(atSignToken.getSpan().getStart());
		tIterator.swallow(); // swallow @
		
		Token token = tIterator.getToken();
		if (token instanceof SymbolToken) {
			SymbolToken leftParenthesisToken = (SymbolToken) token;
	
			if (leftParenthesisToken.getSymbol().equals("(")) {
				visitor.onLeftParenthesis(atSignToken, leftParenthesisToken);
				tIterator.swallow();
				
				boolean foundRightParenthesis = false;
				
				while (true) {
					ParserUtilities.skipAllWhitespaces(tIterator);
					
					token = tIterator.getToken();
			
					if (token == null) {
						break;
					} else {
						if (token instanceof SymbolToken) {
							SymbolToken symbolToken = (SymbolToken) token;
							String		s = symbolToken.getSymbol();
					
							if (s.equals(")")) {
								visitor.onRightParenthesis(symbolToken);
								tIterator.swallow();
								foundRightParenthesis = true;
								break;						
							}
						}
				
						if (!ParserUtilities.parseExpression(tIterator, visitor.onItem(token.getSpan().getStart()), false)) {
							break;
						}
					}
				}
				
				if (!foundRightParenthesis) {
					visitor.onException(new SyntaxException("Expected )", tIterator.getLocation()));
				}
			} else {
				visitor.onException(new SyntaxException("Expected @(", tIterator.getLocation()));
			}
		} else {
			visitor.onException(new SyntaxException("Expected @(", tIterator.getLocation()));
		}
	
		visitor.end(tIterator.getLocation());
	}	
}

