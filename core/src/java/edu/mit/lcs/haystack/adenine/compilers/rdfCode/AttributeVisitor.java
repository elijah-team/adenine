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

import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 */
public class AttributeVisitor extends ParserVisitorBase implements IAttributeVisitor {
	TopLevelVisitor 	m_topLevelVisitor;
	ExpressionVisitor	m_predicateVisitor;
	ExpressionVisitor	m_objectVisitor;
	
	public AttributeVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor);
		m_topLevelVisitor = topLevelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor#onObject(Location)
	 */
	public IExpressionVisitor onObject(Location location) {
		m_objectVisitor = new ExpressionVisitor(m_topLevelVisitor);
		return m_objectVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor#onPredicate(Location)
	 */
	public IExpressionVisitor onPredicate(Location location) {
		m_predicateVisitor = new ExpressionVisitor(m_topLevelVisitor);
		return m_predicateVisitor;
	}
	
	public Resource getPredicateInstruction() {
		return m_predicateVisitor != null ?
			m_predicateVisitor.getInstructionResource() : null;
	}

	public Resource getObjectInstruction() {
		return m_objectVisitor != null ?
			m_objectVisitor.getInstructionResource() : null;
	}
}
