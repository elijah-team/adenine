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
public class VarParser implements IConstructParser {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IConstructParser#parseConstruct(edu.mit.lcs.haystack.adenine.tokenizer.ITokenIterator, edu.mit.lcs.haystack.adenine.parser2.Parser, edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor)
	 */
	public Location parseConstruct(
		ITokenIterator 		tIterator,
		IConstructVisitor 	constructVisitor, IndentToken indentToken
	) {
		IVarVisitor visitor = 
			(constructVisitor instanceof IVarVisitor) ? 
			(IVarVisitor) constructVisitor : 
			new IVarVisitor() {
				public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
					return new NullAttributeVisitor(m_constructVisitor);
				}

				public void onVar(GenericToken varKeyword) {
				}

				public void onVariableName(GenericToken variable) {
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
					
				public IVarVisitor init(IConstructVisitor constructVisitor) {
					m_constructVisitor = constructVisitor;
					return this;
				}
					
				IConstructVisitor m_constructVisitor;
			}.init(constructVisitor);
	
		visitor.start(tIterator.getLocation());
	
		Token 		token = tIterator.getToken();
		Location	endLocation = null;
			
		if (token instanceof GenericToken && ((GenericToken) token).getContent().equals("var")) {
			visitor.onVar((GenericToken) token);
			
			tIterator.swallow();
			ParserUtilities.skipWhitespacesAndComments(tIterator);
				
			while (true) {
				endLocation = tIterator.getLocation();
				
				token = tIterator.getToken();
				if (token == null) {
					break;
				} else if (token instanceof NewLineToken) {
					tIterator.swallow();
					break;
				} else if (token instanceof GenericToken || token instanceof SymbolToken) {
					visitor.onVariableName(ParserUtilities.parseCompoundGenericToken(tIterator, token));
					ParserUtilities.skipWhitespacesAndComments(tIterator);
				} else {
					visitor.onException(new SyntaxException("Expected variable name", tIterator.getLocation()));
					ParserUtilities.skipToNextLine(tIterator);
					break;
				}
			}
		}
	
		if (endLocation == null) {
			endLocation = tIterator.getLocation();
		}

		visitor.end(endLocation);
		
		return endLocation;
	}

}
