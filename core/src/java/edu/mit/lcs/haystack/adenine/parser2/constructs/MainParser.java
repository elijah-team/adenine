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

import edu.mit.lcs.haystack.adenine.constructs.IMainVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IConstructParser;
import edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullCodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.Parser;
import edu.mit.lcs.haystack.adenine.parser2.ParserUtilities;
import edu.mit.lcs.haystack.adenine.parser2.SyntaxException;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator;
import edu.mit.lcs.haystack.adenine.tokenizer.IndentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;

/**
 * @author David Huynh
 */
public class MainParser implements IConstructParser {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IConstructParser#parseConstruct(edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator, edu.mit.lcs.haystack.adenine.parser2.Parser, edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor)
	 */
	public Location parseConstruct(
		ITokenIterator 		tIterator,
		IConstructVisitor 	constructVisitor,
		IndentToken 		indentToken
	) {
		IMainVisitor visitor = 
			(constructVisitor instanceof IMainVisitor) ? 
			(IMainVisitor) constructVisitor :
			new IMainVisitor() {
				public ICodeBlockVisitor onMain(GenericToken mainKeyword) {
					return new NullCodeBlockVisitor(m_constructVisitor);
				}

				public void start(Location startLocation) {
					m_constructVisitor.start(startLocation);
				}

				public void end(Location endLocation) {
					m_constructVisitor.end(endLocation);
				}

				public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
					return new NullAttributeVisitor(m_constructVisitor);
				}

				public void onException(Exception exception) {
					m_constructVisitor.onException(exception);
				}
				
				public IMainVisitor init(IConstructVisitor constructVisitor) {
					m_constructVisitor = constructVisitor;
					return this;
				}
				
				IConstructVisitor m_constructVisitor;
			}.init(constructVisitor);
		
		GenericToken 	mainKeyword = (GenericToken) tIterator.getToken();
		Location		endLocation = null;
		
		visitor.start(mainKeyword.getSpan().getStart());
		
		tIterator.swallow();
		ParserUtilities.skipWhitespacesCommentsAndNewLines(tIterator);
		
		// parse attributes
		
		Token token = tIterator.getToken();
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
			visitor.onException(new SyntaxException("No more code is allowed after 'main' keyword", token.getSpan()));
			ParserUtilities.skipToNextLine(tIterator);
			token = tIterator.getToken();
		}
		
		if (token instanceof IndentToken) {
			endLocation = ParserUtilities.parseCodeBlock(tIterator, visitor.onMain(mainKeyword), (IndentToken) token);
		}

		if (endLocation == null) {
			endLocation = tIterator.getLocation();
		}

		visitor.end(endLocation);
		
		return endLocation;
	}
}
