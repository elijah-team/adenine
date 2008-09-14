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

package edu.mit.lcs.haystack.adenine.compiler;

import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.parser.*;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ImportGenerator implements IInstructionGenerator {

	/**
	 * @see IInstructionGenerator#generateInstruction(Compiler, Token, HashMap, Iterator, Iterator)
	 */
	public ExistentialExpression generateInstruction(
		Compiler compiler,
		Token token,
		HashMap prefixes,
		ListIterator i,
		ListIterator j)
		throws RDFException, AdenineException {
		ExistentialExpression res = compiler.generateInstruction(AdenineConstants.IMPORT, token.m_line);
		
		if (!j.hasNext()) {
			throw new SyntaxException("Expected library name after import", token.m_line);
		}
		Token t = (Token)j.next();
		ITemplateExpression resName;
		if (t instanceof IdentifierToken) {
			resName = new ResourceExpression(compiler.identifierToResource(t.m_line, t.m_token, prefixes));
		} else if (t instanceof URIToken) {
			resName = new ResourceExpression(t.m_token);
		} else {
			throw new SyntaxException("Token not appropriate in import specification: " + t, t.m_line);
		}
		
		res.add(AdenineConstants.LIBRARY_, resName);
		
		if (j.hasNext()) {
			t = (Token)j.next();
			if (t instanceof SemicolonToken) {
				compiler.processAttributes(res, i, j, token.m_line, prefixes);
			} else {
				throw new SyntaxException("import only takes one parameter", t.m_line);
			}
		}
		
		return res;
	}

}
