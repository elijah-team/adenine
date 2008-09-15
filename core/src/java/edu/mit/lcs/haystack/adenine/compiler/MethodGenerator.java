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
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.SyntaxException;
import edu.mit.lcs.haystack.adenine.parser.Block;
import edu.mit.lcs.haystack.adenine.parser.IdentifierToken;
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
public class MethodGenerator implements IInstructionGenerator {

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
		int line = token.m_line;
		
		Token t;
		ExistentialExpression resName = compiler.generateInstruction(AdenineConstants.MethodDef, line);
		if (token.m_token.equals("method")) {	
			// Parse function name
			t = (Token)i.next();
			ITemplateExpression resName2 = compiler.compileToken(t, prefixes);
			
			resName.add(AdenineConstants.name, resName2);
		} else {
			// Parse variable name
			t = (Token)i.next();
			ITemplateExpression resName2 = compiler.compileToken(t, prefixes);
			
			resName.add(AdenineConstants.var, resName2);
		}
		
		// Parse parameters
		ExistentialExpression resLast = null;
		ExistentialExpression resFirst = new ExistentialExpression();
		ExistentialExpression resNew = resFirst;
		while (i.hasNext()) {
			t = (Token)i.next();
			if (t instanceof SemicolonToken) {
				compiler.processAttributes(resName, i, k, line, prefixes);
				break;
			} else if (t instanceof IdentifierToken) {
				// Check for named parameter
				if (i.hasNext()) {
					Token t2 = (Token)i.next();
					if ((t2 instanceof IdentifierToken) && (t2.m_token.equals("="))) {
						if (!i.hasNext()) {
							throw new SyntaxException("Incomplete named parameter", line);
						}
						Token t3 = (Token)i.next();
						
						// Enter named parameter
						ITemplateExpression te1 = compiler.processObject(t, prefixes, t.m_line);
						ITemplateExpression te2 = compiler.compileToken(t3, prefixes);
						ExistentialExpression eeParam = new ExistentialExpression();
						eeParam.add(AdenineConstants.parameterName, te1);
						eeParam.add(AdenineConstants.parameterVariable, te2);
						resName.add(AdenineConstants.namedParameter, eeParam);
						continue;
					}
					
					i.previous();
				}

				if (compiler.isURIIdentifier(t.m_token)) {
					throw new SyntaxException("Parameter identifier cannot be a URI", line);
				}

				ExistentialExpression resIdent = compiler.compileIdentifier(t.m_token, t.m_line);
				resNew.add(Constants.s_rdf_type, Constants.s_daml_List);
				resNew.add(Constants.s_daml_first, resIdent);
				if (resLast != null) {
					resLast.add(Constants.s_daml_rest, resNew);
				}
				resLast = resNew;
				resNew = new ExistentialExpression();
			} else {
				throw new SyntaxException("Token not appropriate in method specification: " + t, line);
			}
		}
		
		if (resLast == null) {
			resName.add(AdenineConstants.PARAMETERS, Constants.s_daml_nil);
		} else {
			resName.add(AdenineConstants.PARAMETERS, resFirst);
			resLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		}
				
		// Parse block
		Object o = k.next();
		if (!(o instanceof Block)) {
			throw new SyntaxException("Expected method body", line + 1);
		}
		
		ExistentialExpression resBlock = compiler.compileBlock((Block)o, prefixes);
		resName.add(AdenineConstants.start, resBlock.generateIndirect());
		
		return resName;
	}

}
