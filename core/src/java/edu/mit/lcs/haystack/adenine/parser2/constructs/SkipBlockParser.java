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

import edu.mit.lcs.haystack.adenine.constructs.ISkipBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.BlockFilterTokenIterator;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IConstructParser;
import edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAttributeVisitor;
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
public class SkipBlockParser implements IConstructParser {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IConstructParser#parseConstruct(edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator, edu.mit.lcs.haystack.adenine.parser2.Parser, edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor)
	 */
	public Location parseConstruct(
		ITokenIterator 		tIterator,
		IConstructVisitor 	constructVisitor,
		IndentToken 		indentToken
	) {
		ISkipBlockVisitor visitor = 
			(constructVisitor instanceof ISkipBlockVisitor) ? 
			(ISkipBlockVisitor) constructVisitor : 
			new ISkipBlockVisitor() {
				public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
					return new NullAttributeVisitor(m_constructVisitor);
				}

				public void onSkipBlock(GenericToken blockKeyword, GenericToken identifier) {
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
					
				public ISkipBlockVisitor init(IConstructVisitor constructVisitor) {
					m_constructVisitor = constructVisitor;
					return this;
				}
					
				IConstructVisitor m_constructVisitor;
			}.init(constructVisitor);
	
		visitor.start(tIterator.getLocation());
	
		Token 			token = tIterator.getToken();
		Location		endLocation = null;
			
		if (token instanceof GenericToken && ((GenericToken) token).getContent().equals("skipBlock")) {
			GenericToken	skipBlockKeyword = (GenericToken) token;
			GenericToken	identifier = null;
		
			tIterator.swallow();
			ParserUtilities.skipWhitespacesAndComments(tIterator);

			token = tIterator.getToken();
			if (ParserUtilities.isPotentialIdentifier(token)) {
				identifier = ParserUtilities.parseCompoundGenericToken(tIterator, token, true);
			}
			
			visitor.onSkipBlock(skipBlockKeyword, identifier);
			
			endLocation = tIterator.getLocation(); 

			token = ParserUtilities.skipToNextLine(tIterator);
			if (token != null) { 
				visitor.onException(new SyntaxException("Unexpected code after skipBlock keyword and optional identifier", token.getSpan()));
			}
			
			token = tIterator.getToken();
			if (token instanceof IndentToken && (indentToken.isOuterLevelOf((IndentToken) token))) {
				ITokenIterator 	tIterator2 = new BlockFilterTokenIterator(tIterator);
		
				while (tIterator2.getToken() != null) {
					endLocation = tIterator.getLocation();
					tIterator2.swallow();
				}				
			}
		} else {
			visitor.onException(new SyntaxException("Expected skipBlock keyword", tIterator.getLocation()));
			ParserUtilities.skipToNextLine(tIterator);
		}
			
		if (endLocation == null) {
			endLocation = tIterator.getLocation();
		}

		visitor.end(endLocation);
		
		return endLocation;
	}
}
