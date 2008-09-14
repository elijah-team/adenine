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
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

import java.util.*;

/**
 * @author David Huynh
 */
public class ReturnVisitor
	extends ConstructVisitorBase
	implements IReturnVisitor {
		
	protected List				m_results = new ArrayList();
	protected List				m_resultNames = new ArrayList();
	protected List				m_namedResults = new ArrayList();
		
	public ReturnVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	public void start(Location startLocation) {
		m_startLocation = startLocation;
	}
	
	public void end(Location endLocation) {
		IRDFContainer target = m_topLevelVisitor.getTarget();

		m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.Return, m_startLocation);
		
		List results = new ArrayList();
		for (int i = 0; i < m_results.size(); i++) {
			Resource r = (((ExpressionVisitor) m_results.get(i)).getInstructionResource());
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
			target.add(new Statement(m_instructionResource, AdenineConstants.RETURN_PARAMETERS, ListUtilities.createDAMLList(results.iterator(), target)));
		} catch (RDFException e) {
			onException(e);
		}
		super.end(endLocation);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IReturnVisitor#onNamedResult(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onNamedResult(
		ResourceToken name,
		SymbolToken equalT) {
		
		ExpressionVisitor ev = new ExpressionVisitor(m_topLevelVisitor);
		m_namedResults.add(ev);
		m_resultNames.add(name);
		
		return ev;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IReturnVisitor#onResult(Location)
	 */
	public IExpressionVisitor onResult(Location location) {
		ExpressionVisitor ev = new ExpressionVisitor(m_topLevelVisitor);
		m_results.add(ev);
		
		return ev;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IReturnVisitor#onReturn(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onReturn(GenericToken returnKeyword) {
		m_startLocation = returnKeyword.getSpan().getStart();
	}
}
