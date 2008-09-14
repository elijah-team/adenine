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

import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.SyntaxException;
import edu.mit.lcs.haystack.adenine.parser.Block;
import edu.mit.lcs.haystack.adenine.parser.IdentifierToken;
import edu.mit.lcs.haystack.adenine.parser.SemicolonToken;
import edu.mit.lcs.haystack.adenine.parser.Token;
import edu.mit.lcs.haystack.rdf.RDFException;

import java.util.HashMap;
import java.util.ListIterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ForGenerator implements IInstructionGenerator {

	/**
	 * @see IInstructionGenerator#generateInstruction(Compiler, Token, HashMap, ListIterator, ListIterator)
	 */
	public ExistentialExpression generateInstruction(
		Compiler compiler,
		Token token,
		HashMap prefixes,
		ListIterator i,
		ListIterator j)
		throws RDFException, AdenineException {
		ExistentialExpression res = compiler.generateInstruction(AdenineConstants.ForIn, token.m_line);
		
		// Extract variable name
		if (!j.hasNext()) {
			throw new SyntaxException("for requires variable name and collection", token.m_line);
		}
		
		res.add(AdenineConstants.var, compiler.compileToken((Token)j.next(), prefixes));
		
		// Extract "in"
		if (!j.hasNext()) {
			throw new SyntaxException("for requires variable name and collection", token.m_line);
		}
		
		Token t1 = (Token)j.next();
		if (!(t1 instanceof IdentifierToken) || !t1.m_token.equals("in")) {
			throw new SyntaxException("for ... in ... misformed", token.m_line);
		}
		
		// Extract collection
		if (!j.hasNext()) {
			throw new SyntaxException("for requires variable name and collection", token.m_line);
		}
		
		res.add(AdenineConstants.COLLECTION, compiler.compileToken((Token)j.next(), prefixes));
		
		if (j.hasNext()) {
			Token t = (Token)j.next();
			if (t instanceof SemicolonToken) {
				compiler.processAttributes(res, j, i, t.m_line, prefixes);
			} else {
				throw new SyntaxException("for misformed", token.m_line);
			}
		}
		
		// Process body
		if (!i.hasNext()) {
			throw new SyntaxException("Unexpected end of file", token.m_line);
		}
		Object o2 = i.next();
		if (o2 instanceof Block) {
			res.add(AdenineConstants.body, compiler.compileBlock((Block)o2, prefixes));
		} else {
			throw new SyntaxException("for needs body block", token.m_line);
		}
		
		return res;
	}

}

