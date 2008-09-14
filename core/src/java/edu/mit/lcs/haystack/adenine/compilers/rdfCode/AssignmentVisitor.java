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
public class AssignmentVisitor
	extends ConstructVisitorBase
	implements IAssignmentVisitor {
		
	protected Resource 			m_lhs;
	protected ExpressionVisitor 	m_rhs;

	public AssignmentVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IAssignmentVisitor#onAssignment(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onLHS(SymbolToken equalT, GenericToken identifier) {
		m_lhs = m_topLevelVisitor.generateIdentifierInstruction(identifier);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IAssignmentVisitor#onRHS(Location)
	 */
	public IExpressionVisitor onRHS(Location location) {
		m_rhs = new ExpressionVisitor(m_topLevelVisitor);
		return m_rhs;
	}

	public void end(Location endLocation) {
		if (m_lhs != null && m_rhs != null) {
			Resource lhs = m_lhs;
			Resource rhs = m_rhs.getInstructionResource();
			
			if (lhs != null && rhs != null) {
				IRDFContainer	target = m_topLevelVisitor.getTarget();
				
				try {
					m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.Assignment, endLocation);
					
					target.add(new Statement(m_instructionResource, AdenineConstants.LHS, lhs));
					target.add(new Statement(m_instructionResource, AdenineConstants.RHS, rhs));
					
					super.end(endLocation);
					return;
				} catch (RDFException e) {
					onException(e);
				}
			}
		}
		m_instructionResource = null;
		super.end(endLocation);
	}
}
