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

import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.constructs.ICallVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Huynh
 */
public class CallVisitor
	extends ConstructVisitorBase
	implements ICallVisitor {
		
	protected List				m_arguments = new ArrayList();
	protected List				m_argumentNames = new ArrayList();
	protected List				m_namedArguments = new ArrayList();

	protected List				m_results = new ArrayList();
	protected List				m_resultNames = new ArrayList();
	protected List				m_namedResults = new ArrayList();
	
	protected ExpressionVisitor	m_callable;
	protected Location			m_startLocation;
		
	public CallVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	public void start(Location startLocation) {
		m_startLocation = startLocation;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onArgument(Location)
	 */
	public IExpressionVisitor onArgument(Location location) {
		ExpressionVisitor ev = new ExpressionVisitor(m_topLevelVisitor);
		m_arguments.add(ev);
		
		return ev;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onCall(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onCall(GenericToken callKeyword) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onCallable(Location)
	 */
	public IExpressionVisitor onCallable(Location location) {
		m_callable = new ExpressionVisitor(m_topLevelVisitor);
		return m_callable;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onComma(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onComma(SymbolToken commaT) {
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

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onResult(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onResult(GenericToken variable) {
		m_results.add(variable);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onNamedResult(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onNamedResult(
		ResourceToken name,
		SymbolToken equalT,
		GenericToken variable) {

		m_namedResults.add(variable);
		m_resultNames.add(name);
	}

	public void end(Location endLocation) {
		if (m_callable != null) {
			Resource callable = m_callable.getInstructionResource();
			
			if (callable != null) {
				IRDFContainer target = m_topLevelVisitor.getTarget();

				m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.Call, m_startLocation);
				
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
				
				List results = new ArrayList();
				for (int i = 0; i < m_results.size(); i++) {
					Resource r = m_topLevelVisitor.generateIdentifierInstruction((GenericToken) m_results.get(i));
					if (r != null) {
						results.add(r);
					}
				}
				
				for (int i = 0; i < m_namedResults.size(); i++) {
					Resource a = (((ExpressionVisitor) m_namedResults.get(i)).getInstructionResource());
					Resource n = m_topLevelVisitor.resolveURI((ResourceToken) m_resultNames.get(i));
					if (a != null && n != null) {
						Resource r = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
						
						try {
							target.add(new Statement(r, AdenineConstants.parameterName, n));
							target.add(new Statement(r, AdenineConstants.parameterVariable, a));

							target.add(new Statement(m_instructionResource, AdenineConstants.NAMED_RETURN_PARAMETER, r));
						} catch (RDFException e) {
							onException(e);
						}
					}
				}

				try {
					target.add(new Statement(m_instructionResource, AdenineConstants.function, m_callable.getInstructionResource()));
					target.add(new Statement(m_instructionResource, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(arguments.iterator(), target)));
					target.add(new Statement(m_instructionResource, AdenineConstants.RETURN_PARAMETERS, ListUtilities.createDAMLList(results.iterator(), target)));
				} catch (RDFException e) {
					onException(e);
				}
				
				super.end(endLocation);
				return;
			}
		}
		
		m_instructionResource = null;
		super.end(endLocation);
	}
}
