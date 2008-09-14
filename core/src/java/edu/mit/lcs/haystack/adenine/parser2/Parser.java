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

import edu.mit.lcs.haystack.adenine.parser2.constructs.AssignmentParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.BlockParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.BreakParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.CallParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.ContinueParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.ForParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.FunctionParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.IfParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.ImportJavaParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.MainParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.MethodParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.ReturnParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.SkipBlockParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.VarParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.WhileParser;
import edu.mit.lcs.haystack.adenine.parser2.constructs.WithParser;
import edu.mit.lcs.haystack.adenine.tokenizer.CommentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ErrorToken;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.IScannerVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator;
import edu.mit.lcs.haystack.adenine.tokenizer.IndentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.NewLineToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;
import edu.mit.lcs.haystack.adenine.tokenizer.Tokenizer;
import edu.mit.lcs.haystack.adenine.tokenizer.WhitespaceToken;

import java.io.Reader;
import java.util.HashMap;

/**
 * @author David Huynh
 */
final public class Parser {
	static protected HashMap s_constructParsers = new HashMap();
	static {
		s_constructParsers.put("=", 			new AssignmentParser());
		s_constructParsers.put("block",			new BlockParser());
		s_constructParsers.put("break", 		new BreakParser());
		s_constructParsers.put("call",			new CallParser());
		s_constructParsers.put("continue", 		new ContinueParser());
		s_constructParsers.put("for", 			new ForParser());
		s_constructParsers.put("function",		new FunctionParser());
		s_constructParsers.put("if", 			new IfParser());
		s_constructParsers.put("importjava", 	new ImportJavaParser());
		s_constructParsers.put("method", 		new MethodParser());
		s_constructParsers.put("main",	 		new MainParser());
		s_constructParsers.put("skipBlock",		new SkipBlockParser());
		s_constructParsers.put("return", 		new ReturnParser());
		s_constructParsers.put("uniqueMethod", 	new MethodParser());
		s_constructParsers.put("var", 			new VarParser());
		s_constructParsers.put("while", 		new WhileParser());
		s_constructParsers.put("with", 			new WithParser());
	}
	
	static public void parse(IScannerVisitor sVisitor, ITopLevelVisitor pVisitor, Reader reader) {
		if (sVisitor == null) {
			throw new NullPointerException("sVisitor is null");
		}
		if (pVisitor == null) {
			throw new NullPointerException("pVisitor is null");
		}
		if (reader == null) {
			throw new NullPointerException("reader is null");
		}
					
		parse(new Tokenizer(reader, sVisitor), pVisitor);
	}

	static public void parse(ITokenIterator tokenIterator, ITopLevelVisitor pVisitor) {
		if (tokenIterator == null) {
			throw new NullPointerException("tokenIterator is null");
		}
		if (pVisitor == null) {
			throw new NullPointerException("pVisitor is null");
		}
					
		parseTopLevel(tokenIterator, pVisitor);
	}
	
	static protected void parseTopLevel(ITokenIterator tIterator, ITopLevelVisitor visitor) {
		visitor.start(tIterator.getLocation());
		
		while (true) { 
			Token token = tIterator.getToken();

			if (token == null) {
				break;
			} else if (token instanceof IndentToken) {
				visitor.onException(new SyntaxException("Indentation not allowed at top level", token.getSpan()));

				ITokenIterator tIterator2 = new BlockFilterTokenIterator(tIterator);
				while (tIterator2.getToken() != null) {
					tIterator2.swallow();
				}

				continue;
			} else if (
				token instanceof CommentToken ||
				token instanceof WhitespaceToken ||
				token instanceof NewLineToken) {

				tIterator.swallow();
				continue;
			} else if (token instanceof SymbolToken) {
				SymbolToken symbolToken = (SymbolToken) token;
				String		s = symbolToken.getSymbol();
				
				if (s.equals("@")) {
					parsePragma(tIterator, visitor, symbolToken);
					continue;
				}
			} else if (token instanceof ErrorToken) {
				visitor.onException(new SyntaxException("Bad token", token.getSpan()));
				ParserUtilities.skipToNextLine(tIterator);
				continue;
			}
			
			parseConstruct(tIterator, visitor, null, true);
		}
		
		visitor.end(tIterator.getLocation());
	}

	static protected void parsePragma(ITokenIterator tIterator, ITopLevelVisitor visitor, SymbolToken atSignToken) {
		tIterator.swallow(); // swallow @
		
		Token token = tIterator.getToken();
		if (token instanceof GenericToken) {
			GenericToken 	genericToken = (GenericToken) token;
			String			s = genericToken.getContent();
			
			if (s.equals("base")) {
				boolean error = true;
				
				tIterator.swallow(); // swallow base
				ParserUtilities.skipWhitespacesAndComments(tIterator);
				
				token = tIterator.getToken();
				if (token instanceof ResourceToken) {
					visitor.onBase(atSignToken, genericToken, (ResourceToken) token);
					
					tIterator.swallow(); // swallow base URI
					ParserUtilities.skipWhitespacesAndComments(tIterator);
					
					token = tIterator.getToken();
					
					if (token instanceof NewLineToken) {
						tIterator.swallow();
						error = false;
					}
				}
				
				if (error) {
					visitor.onException(new SyntaxException("@base declaration must be of form: @base [URI] ", tIterator.getLocation()));
					ParserUtilities.skipToNextLine(tIterator);
				}
			} else if (s.equals("prefix")) {
				boolean error = true;
				
				tIterator.swallow(); // swallow prefix
				ParserUtilities.skipWhitespacesAndComments(tIterator);
				
				token = tIterator.getToken();
				if (token instanceof ResourceToken) {
					ResourceToken prefix = (ResourceToken) token;
					
					if (prefix.isPrefixed() && prefix.getSuffix().length() == 0) {
						tIterator.swallow(); // swallow prefix URI
						ParserUtilities.skipWhitespacesAndComments(tIterator);
						
						token = tIterator.getToken();
						if (token instanceof ResourceToken) {
							visitor.onPrefix(atSignToken, genericToken, prefix, (ResourceToken) token);

							tIterator.swallow(); // swallow expansion
							ParserUtilities.skipWhitespacesAndComments(tIterator);

							token = tIterator.getToken();
					
							if (token instanceof NewLineToken) {
								tIterator.swallow();
								error = false;
							}
						}
					}
				}					
					
				if (error) {
					visitor.onException(new SyntaxException("@prefix declaration must be of form: @prefix [prefix:] [expanded URI]", tIterator.getLocation()));
					ParserUtilities.skipToNextLine(tIterator);
				}
			} else {
				visitor.onException(new SyntaxException("Unrecognized pragma keyword '" + s + "'", genericToken.getSpan()));
				ParserUtilities.skipToNextLine(tIterator);
			}
		} else {
			visitor.onException(new SyntaxException("Expected pragma keyword after @. Use @base or @prefix", atSignToken.getSpan()));
			ParserUtilities.skipToNextLine(tIterator);
		}
	}
	
	static protected boolean parseSeveralAttributes(ITokenIterator tIterator, IModelVisitor visitor) {
		ParserUtilities.skipAllWhitespaces(tIterator);
		
		if (!parseAttribute(tIterator, visitor.onAttribute(null), null)) {
			return false;
		}
		
		while (true) {
			ParserUtilities.skipAllWhitespaces(tIterator);
			
			Token token = tIterator.getToken();
			
			if (token instanceof SymbolToken) {
				SymbolToken symbolToken = (SymbolToken) token;
				String		s = symbolToken.getSymbol();
				
				if (s.equals(";")) {
					if (!parseAttribute(tIterator, visitor.onAttribute(symbolToken), symbolToken)) {
						return false;
					}
					continue;
				}
			}
			
			break;
		}
		
		return true;
	}
	static protected void parseSeveralAttributes(ITokenIterator tIterator, IAnonymousModelVisitor visitor) {
		parseAttribute(tIterator, visitor.onAttribute(null), null);
		
		while (true) {
			ParserUtilities.skipAllWhitespaces(tIterator);
			
			Token token = tIterator.getToken();
			
			if (token instanceof SymbolToken) {
				SymbolToken symbolToken = (SymbolToken) token;
				String		s = symbolToken.getSymbol();
				
				if (s.equals(";")) {
					parseAttribute(tIterator, visitor.onAttribute(symbolToken), symbolToken);
					continue;
				}
			}
			
			break;
		}
	}
	public static boolean parseAttribute(ITokenIterator tIterator, IAttributeVisitor visitor, SymbolToken semicolonToken) {
		boolean r = false;
		
		visitor.start(tIterator.getLocation());

		if (semicolonToken != null) {
			tIterator.swallow();
			ParserUtilities.skipAllWhitespaces(tIterator);
		}
		
		Token token = tIterator.getToken();
		if (token != null && !(token instanceof SymbolToken && ((SymbolToken) token).getSymbol().equals("}"))) {
			r = ParserUtilities.parseExpression(tIterator, visitor.onPredicate(token.getSpan().getStart()), false);
			
			if (r) {
				ParserUtilities.skipAllWhitespaces(tIterator);
			
				r = ParserUtilities.parseExpression(tIterator, visitor.onObject(token.getSpan().getStart()), false);
			}
		}
				
		visitor.end(tIterator.getLocation());
		
		return r;
	}
	static Location parseConstruct(ITokenIterator tIterator, ICodeBlockVisitor visitor, IndentToken indentToken, boolean allowAdd) {
		Token 		token = tIterator.getToken();
		Location	endLocation = null;

		if (ParserUtilities.isPotentialIdentifier(token)) {
			GenericToken 	token2 = ParserUtilities.parseCompoundGenericToken(tIterator, token, false);
			String			s = token2.getContent();
			
			IConstructParser parser = (IConstructParser) s_constructParsers.get(s);
			if (parser == null) {
				parser = new CallParser();
				s = "call";
			}

			endLocation = parser.parseConstruct(tIterator, visitor.onConstruct(token2.getSpan().getStart(), s), indentToken);
		} else if (token instanceof ResourceToken) {
			new CallParser().parseConstruct(tIterator, visitor.onConstruct(token.getSpan().getStart(), "call"), indentToken);
		}
		
		return endLocation != null ? endLocation : tIterator.getLocation();
	}
	
	public static String s_compoundGenericTokenExcepts = "<>()[]{}.,;?@$%";
}

