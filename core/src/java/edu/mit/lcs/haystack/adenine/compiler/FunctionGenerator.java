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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.parser.*;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class FunctionGenerator implements IInstructionGenerator {

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
		ExistentialExpression res = compiler.generateInstruction(AdenineConstants.Function, token.m_line);
		
		// Obtain function name
		ITemplateExpression resFunction = compiler.compileToken((Token)j.next(), prefixes);
		res.add(AdenineConstants.function, resFunction);
		
		// Obtain parameters
		ExistentialExpression resLast2 = null;
		ExistentialExpression resFirst2 = new ExistentialExpression();
		ExistentialExpression resNext = resFirst2;
		while (j.hasNext()) {
			Token t = (Token)j.next();
			if (t instanceof SemicolonToken) {
				compiler.processAttributes(res, j, i, token.m_line, prefixes);
				break;
			}
			
			ITemplateExpression res1 = compiler.compileToken(t, prefixes);
			resNext.add(Constants.s_rdf_type, Constants.s_daml_List);
			resNext.add(Constants.s_daml_first, res1);
			
			if (resLast2 != null) {
				resLast2.add(Constants.s_daml_rest, resNext);
			}
			resLast2 = resNext;
			resNext = new ExistentialExpression();
		}
		
		if (resLast2 == null) {
			res.add(AdenineConstants.PARAMETERS, Constants.s_daml_nil);
		} else {
			res.add(AdenineConstants.PARAMETERS, resFirst2);
			resLast2.add(Constants.s_daml_rest, Constants.s_daml_nil);
		}
		
		// Process body
		if (!i.hasNext()) {
			throw new SyntaxException("Unexpected end of file", token.m_line);
		}
		Object o2 = i.next();
		if (o2 instanceof Block) {
			res.add(AdenineConstants.body, compiler.compileBlock((Block)o2, prefixes));
		} else {
			throw new SyntaxException("function needs body block", token.m_line);
		}
		
		return res;
	}

}
