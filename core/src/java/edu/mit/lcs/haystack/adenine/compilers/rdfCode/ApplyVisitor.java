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

package edu.mit.lcs.haystack.adenine.compilers.rdfCode;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;

import java.util.*;

/**
 * @author David Huynh
 */
public class ApplyVisitor extends ParserVisitorBase implements IApplyVisitor {
	protected TopLevelVisitor	m_topLevelVisitor;
	protected Resource		m_instructionResource;
		
	protected List				m_arguments = new ArrayList();
	protected List				m_argumentNames = new ArrayList();
	protected List				m_namedArguments = new ArrayList();

	protected ExpressionVisitor	m_callable;
		
	public ApplyVisitor(TopLevelVisitor visitor) {
		super(visitor);
		m_topLevelVisitor = visitor;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onArgument()
	 */
	public IExpressionVisitor onArgument(Location location) {
		ExpressionVisitor ev = new ExpressionVisitor(m_topLevelVisitor);
		m_arguments.add(ev);
		
		return ev;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onCallable()
	 */
	public IExpressionVisitor onCallable(Location location) {
		m_callable = new ExpressionVisitor(m_topLevelVisitor);
		return m_callable;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onNamedArgument(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onNamedArgument(
		ResourceToken name,
		SymbolToken equalT) {

		ExpressionVisitor ev = new ExpressionVisitor(m_topLevelVisitor);
		m_namedArguments.add(ev);
		
		m_argumentNames.add(name);
		
		return ev;
	}

	public void end(Location endLocation) {
		if (m_callable != null) {
			Resource callable = m_callable.getInstructionResource();
			
			if (callable != null) {
				IRDFContainer target = m_topLevelVisitor.getTarget();

				m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.FunctionCall, m_startLocation);
				
				List arguments = new ArrayList();
				for (int i = 0; i < m_arguments.size(); i++) {
					Resource r = (((ExpressionVisitor) m_arguments.get(i)).getInstructionResource());
					if (r != null) {
						arguments.add(r);
					}
				}
				
				for (int i = 0; i < m_namedArguments.size(); i++) {
					Resource a = (((ExpressionVisitor) m_namedArguments.get(i)).getInstructionResource());
					Resource n = m_topLevelVisitor.resolveURI((ResourceToken) m_argumentNames.get(i));
					if (a != null && n != null) {
						Resource r = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
						
						try {
							target.add(new Statement(r, AdenineConstants.parameterName, n));
							target.add(new Statement(r, AdenineConstants.parameterVariable, a));
							
							target.add(new Statement(m_instructionResource, AdenineConstants.namedParameter, r));
						} catch (RDFException e) {
							onException(e);
						}
					}
				}
				
				try {
					target.add(new Statement(m_instructionResource, AdenineConstants.function, m_callable.getInstructionResource()));
					target.add(new Statement(m_instructionResource, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(arguments.iterator(), target)));
				} catch (RDFException e) {
					onException(e);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onLeftParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onLeftParenthesis(SymbolToken leftParenthesisT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onRightParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onRightParenthesis(SymbolToken rightParenthesisT) {
	}
}
