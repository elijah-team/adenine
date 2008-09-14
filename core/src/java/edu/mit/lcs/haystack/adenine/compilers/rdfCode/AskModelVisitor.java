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
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;

import java.util.*;

/**
 * @author David Huynh
 */
public class AskModelVisitor extends ParserVisitorBase implements IAskModelVisitor {
	protected TopLevelVisitor 	m_topLevelVisitor;
	protected LinkedList			m_conditions = new LinkedList();
	protected LinkedList			m_condition;
	protected Resource			m_instructionResource;
	
	public AskModelVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor);
		m_topLevelVisitor = topLevelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelEnd(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelEnd(SymbolToken rightBraceT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelStart(SymbolToken percentSignT, SymbolToken leftBraceT) {
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onConditionStart(SymbolToken commaT) {
		m_condition = new LinkedList();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionEnd(Location)
	 */
	public void onConditionEnd(Location location) {
		if (m_condition != null) {
			m_conditions.add(m_condition);
			m_condition = null;
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionTerm(Location)
	 */
	public IExpressionVisitor onConditionTerm(Location location) {
		if (m_condition != null) {
			ExpressionVisitor v = new ExpressionVisitor(m_topLevelVisitor);
			
			m_condition.add(v);
			
			return v;
		} else {
			return new NullExpressionVisitor(m_topLevelVisitor.getChainedVisitor());
		}
	}

	public void end(Location endLocation) {
		IRDFContainer 	target = m_topLevelVisitor.getTarget();
		LinkedList		conditionResources = new LinkedList();

		m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.Query, m_startLocation);
		
		try {
			Iterator c = m_conditions.iterator();
			while (c.hasNext()) {
				LinkedList	terms = (LinkedList) c.next();
				Iterator	v = terms.iterator();
				LinkedList	instructionResources = new LinkedList();
				
				while (v.hasNext()) {
					ExpressionVisitor visitor = (ExpressionVisitor) v.next();
					
					instructionResources.add(visitor.getInstructionResource());
				}
				
				conditionResources.add(ListUtilities.createDAMLList(instructionResources.iterator(), target));
			}
			
			target.add(new Statement(m_instructionResource, AdenineConstants.conditions, ListUtilities.createDAMLList(conditionResources.iterator(), target)));
		} catch (RDFException e) {
			onException(e);
		}
	}
}
