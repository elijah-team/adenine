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

package edu.mit.lcs.haystack.adenine.compilers.javaByteCode;

import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.apache.bcel.generic.*;

/**
 * @author David Huynh
 */
public class SubExpressionVisitor extends ParserVisitorBase2 implements ISubExpressionVisitor {
	public SubExpressionVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onIdentifier(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.Token)
	 */
	public void onIdentifier(SymbolToken backquoteT, Token identifier) {
		if (identifier instanceof GenericToken && ((GenericToken) identifier).getContent().equals("^")) {
			if (backquoteT != null) {
				onException(new SyntaxException("Backquote not allowed after ^", backquoteT.getSpan()));
			}
			
			Resource r = m_topLevelVisitor.getBase();
			if (r != null) {
				addResource(r, identifier.getSpan());
			} else {
				onException(new SyntaxException("Base ^ as not been defined", identifier.getSpan()));
			}
		} else {
			GenericToken 	i = (GenericToken) identifier;
			String			name = i.getContent();
			
			if (backquoteT != null) {
				m_topLevelVisitor.addBackquotedIdentifier(name);
				m_codeFrame.addTopVariable(name);
			}
			
			onIdentifier(name, i);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onResource(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken)
	 */
	public void onResource(ResourceToken resourceToken) {
		Resource r = m_topLevelVisitor.resolveURI(resourceToken);
		
		if (r != null) {
			addResource(r, resourceToken.getSpan());
		} else {
			onException(new SyntaxException("Unknown prefix " + resourceToken.getPrefix(), resourceToken.getSpan()));
		} 
	}
	
	protected void addResource(Resource resource, Span span) {
		InstructionList	iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = span.getStart().getTrueLine();
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeResource)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(new PUSH(							// push variable name as string
				m_codeFrame.getConstantPoolGen(), 
				resource.getURI())),
			line);
			
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeResource.getClassName(),
				"<init>",
				Type.VOID,
				new Type[] { Type.STRING },
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
	}

	protected void onIdentifier(String identifier, GenericToken token) {
		m_codeFrame.generateVariableGet(
			identifier, 
			m_codeFrame.getInstructionList(), 
			m_codeFrame.getMethodGen(), 
			token.getSpan().getStart().getTrueLine());
	}
}
