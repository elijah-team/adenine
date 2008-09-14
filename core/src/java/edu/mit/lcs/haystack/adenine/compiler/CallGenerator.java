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
public class CallGenerator implements IInstructionGenerator {

	/**
	 * @see IInstructionGenerator#generateInstruction(Compiler, Token, HashMap, ListIterator, ListIterator)
	 */
	public ExistentialExpression generateInstruction(
		Compiler compiler,
		Token token,
		HashMap prefixes,
		ListIterator k,
		ListIterator i)
		throws RDFException, AdenineException {
		ExistentialExpression ee = compiler.generateInstruction(AdenineConstants.Call, token.m_line);
		
		// Obtain function name
		ITemplateExpression resFunction = compiler.compileToken((Token)i.next(), prefixes);
		ee.add(AdenineConstants.function, resFunction);
		
		// Obtain parameters
		ExistentialExpression eeLast = null;
		ExistentialExpression eeFirst = new ExistentialExpression();
		ExistentialExpression eeNext = eeFirst;
		while (i.hasNext()) {
			Token t = (Token)i.next();
			if (t instanceof SemicolonToken) {
				throw new SyntaxException("Invalid semicolon", t.m_line);
			}
			
			if ((t instanceof IdentifierToken) && t.m_token.equals(",")) {
				break;
			}
			
			// Check for named parameter
			if (i.hasNext()) {
				Token t2 = (Token)i.next();
				if ((t2 instanceof IdentifierToken) && (t2.m_token.equals("="))) {
					if (!i.hasNext()) {
						throw new SyntaxException("Incomplete named parameter", t2.m_line);
					}
					Token t3 = (Token)i.next();
					
					// Enter named parameter
					ITemplateExpression te1 = compiler.processObject(t, prefixes, t.m_line);
					ITemplateExpression te2 = compiler.compileToken(t3, prefixes);
					ExistentialExpression eeParam = new ExistentialExpression();
					eeParam.add(AdenineConstants.parameterName, te1);
					eeParam.add(AdenineConstants.parameterVariable, te2);
					ee.add(AdenineConstants.namedParameter, eeParam);
					continue;
				}
				
				i.previous();
			}
			
			ITemplateExpression ee1 = compiler.compileToken(t, prefixes);
			eeNext.add(Constants.s_rdf_type, Constants.s_daml_List);
			eeNext.add(Constants.s_daml_first, ee1);
			
			if (eeLast != null) {
				eeLast.add(Constants.s_daml_rest, eeNext);
			}
			eeLast = eeNext;
			eeNext = new ExistentialExpression();
		}
		
		if (eeLast == null) {
			ee.add(AdenineConstants.PARAMETERS, Constants.s_daml_nil);
		} else {
			ee.add(AdenineConstants.PARAMETERS, eeFirst);
			eeLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		}

		// Obtain return parameters
		eeLast = null;
		eeFirst = new ExistentialExpression();
		eeNext = eeFirst;
		while (i.hasNext()) {
			Token t = (Token)i.next();
			if (t instanceof SemicolonToken) {
				compiler.processAttributes(ee, i, k, t.m_line, prefixes);
				break;
			}
			
			// Check for named parameter
			if (i.hasNext()) {
				Token t2 = (Token)i.next();
				if ((t2 instanceof IdentifierToken) && (t2.m_token.equals("="))) {
					if (!i.hasNext()) {
						throw new SyntaxException("Incomplete named parameter", t2.m_line);
					}
					Token t3 = (Token)i.next();
					
					// Enter named parameter
					ITemplateExpression te1 = compiler.processObject(t, prefixes, t.m_line);
					ITemplateExpression te2 = compiler.compileToken(t3, prefixes);
					ExistentialExpression eeParam = new ExistentialExpression();
					eeParam.add(AdenineConstants.parameterName, te1);
					eeParam.add(AdenineConstants.parameterVariable, te2);
					ee.add(AdenineConstants.NAMED_RETURN_PARAMETER, eeParam);
					continue;
				}
				
				i.previous();
			}
			
			ITemplateExpression ee1 = compiler.compileToken(t, prefixes);
			eeNext.add(Constants.s_rdf_type, Constants.s_daml_List);
			eeNext.add(Constants.s_daml_first, ee1);
			
			if (eeLast != null) {
				eeLast.add(Constants.s_daml_rest, eeNext);
			}
			eeLast = eeNext;
			eeNext = new ExistentialExpression();
		}
		
		if (eeLast == null) {
			ee.add(AdenineConstants.RETURN_PARAMETERS, Constants.s_daml_nil);
		} else {
			ee.add(AdenineConstants.RETURN_PARAMETERS, eeFirst);
			eeLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		}
		
		return ee;
	}
}

