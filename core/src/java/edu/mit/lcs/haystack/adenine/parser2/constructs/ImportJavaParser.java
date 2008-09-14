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

import edu.mit.lcs.haystack.adenine.constructs.IImportJavaVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IConstructParser;
import edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ParserUtilities;
import edu.mit.lcs.haystack.adenine.parser2.SyntaxException;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator;
import edu.mit.lcs.haystack.adenine.tokenizer.IndentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.NewLineToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;

/**
 * @author David Huynh
 */
public class ImportJavaParser implements IConstructParser {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IConstructParser#parseConstruct(edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator, edu.mit.lcs.haystack.adenine.parser2.Parser, edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor)
	 */
	public Location parseConstruct(
		ITokenIterator 		tIterator,
		IConstructVisitor 	constructVisitor, IndentToken indentToken
	) {
		IImportJavaVisitor visitor = 
			(constructVisitor instanceof IImportJavaVisitor) ? 
			(IImportJavaVisitor) constructVisitor : 
			new IImportJavaVisitor() {
				public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
					return new NullAttributeVisitor(m_constructVisitor);
				}

				public void onImportJava(
					GenericToken importjavaKeyword,
					LiteralToken pakkage) {
				}
				
				public void onClass(GenericToken klass) {
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
					
				public IImportJavaVisitor init(IConstructVisitor constructVisitor) {
					m_constructVisitor = constructVisitor;
					return this;
				}
					
				IConstructVisitor m_constructVisitor;
			}.init(constructVisitor);
	
		visitor.start(tIterator.getLocation());
	
		Token 		token = tIterator.getToken();
		Location	endLocation = null;
			
		if (token instanceof GenericToken && ((GenericToken) token).getContent().equals("importjava")) {
			GenericToken importjava = (GenericToken) token;
			
			tIterator.swallow();
			ParserUtilities.skipWhitespacesAndComments(tIterator);
			
			token = tIterator.getToken();
			if (token instanceof LiteralToken) {
				visitor.onImportJava(importjava, (LiteralToken) token);
				
				tIterator.swallow();
				
				while (true) {
					ParserUtilities.skipWhitespacesAndComments(tIterator);
					
					endLocation = tIterator.getLocation();
					
					token = tIterator.getToken();
					if (token == null || token instanceof NewLineToken) {
						tIterator.swallow();
						break;
					} else if (ParserUtilities.isPotentialIdentifier(token)) {
						visitor.onClass(ParserUtilities.parseCompoundGenericToken(tIterator, token));
					} else {
						visitor.onException(new SyntaxException("Expected class", tIterator.getLocation()));
						ParserUtilities.skipToNextLine(tIterator);
						break;
					}
				}
			} else {
				visitor.onException(new SyntaxException("Expected package as literal", tIterator.getLocation()));
				ParserUtilities.skipToNextLine(tIterator);
			}
		} else {
			visitor.onException(new SyntaxException("Expected importjava keyword", tIterator.getLocation()));
			ParserUtilities.skipToNextLine(tIterator);
		}
			
		if (endLocation == null) {
			endLocation = tIterator.getLocation();
		}

		visitor.end(endLocation);
		
		return endLocation;
	}

}
