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
import edu.mit.lcs.haystack.adenine.parser.Line;
import edu.mit.lcs.haystack.adenine.parser.SemicolonToken;
import edu.mit.lcs.haystack.adenine.parser.Token;
import edu.mit.lcs.haystack.rdf.RDFException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class IfGenerator implements IInstructionGenerator {

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
		ExistentialExpression res = compiler.generateInstruction(AdenineConstants.If, token.m_line);
		
		// Extract condition
		if (!j.hasNext()) {
			throw new SyntaxException("if expects condition", token.m_line);
		}
		
		res.add(AdenineConstants.CONDITION, compiler.compileToken((Token)j.next(), prefixes));
		
		if (j.hasNext()) {
			Token t = (Token)j.next();
			if (t instanceof SemicolonToken) {
				compiler.processAttributes(res, j, i, t.m_line, prefixes);
			} else {
				throw new SyntaxException("if has only one parameter", token.m_line);
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
			throw new SyntaxException("if needs body block", token.m_line);
		}
		
		// Check for else
		if (i.hasNext() && (i instanceof ListIterator)) {
			ListIterator li = (ListIterator)i;
			Object o3 = li.next();
			if (o3 instanceof Line) {
				Line l = (Line)o3;
				Iterator k = l.m_tokens.iterator();
				if (k.hasNext()) {
					Object o6 = k.next();
					if (o6 instanceof IdentifierToken) {
						IdentifierToken it = (IdentifierToken)o6;
						if (it.m_token.equals("else")) {
							// Process body
							if (!i.hasNext()) {
								throw new SyntaxException("Unexpected end of file", it.m_line);
							}
							Object o4 = i.next();
							if (o4 instanceof Block) {
								res.add(AdenineConstants.ELSEBODY, compiler.compileBlock((Block)o4, prefixes));
							} else {
								throw new SyntaxException("else needs body block", it.m_line);
							}
						} else {
							li.previous();
						}
					} else {
						li.previous();
					}
				} else {
					li.previous();
				}
			} else {
				// Backtrack
				li.previous();
			}
		}
		
		return res;
	}

}
