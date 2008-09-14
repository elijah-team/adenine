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

package edu.mit.lcs.haystack.adenine.compilers.utils;

import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

/**
 * @author David Huynh
 */
public class TopLevelAttributeVisitor extends ParserVisitorBase implements IAttributeVisitor {
	TopLevelVisitorBase 		m_topLevelVisitor;
	Resource					m_subject;
	TopLevelExpressionVisitor	m_predicateVisitor;
	TopLevelExpressionVisitor	m_objectVisitor;
	
	public TopLevelAttributeVisitor(TopLevelVisitorBase topLevelVisitor, Resource subject) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
		m_subject = subject;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor#onObject(Location)
	 */
	public IExpressionVisitor onObject(Location location) {
		m_objectVisitor = new TopLevelExpressionVisitor(m_topLevelVisitor, false);
		return m_objectVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor#onPredicate(Location)
	 */
	public IExpressionVisitor onPredicate(Location location) {
		m_predicateVisitor = new TopLevelExpressionVisitor(m_topLevelVisitor, false);
		return m_predicateVisitor;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IParserVisitor#end(edu.mit.lcs.haystack.adenine.tokenizer.Location)
	 */
	public void end(Location endLocation) {
		super.end(endLocation);
		
		if (m_predicateVisitor != null && m_objectVisitor != null) {
			Resource	predicate = (Resource) m_predicateVisitor.getRDFNode();
			RDFNode		object = m_objectVisitor.getRDFNode();
			
			if (predicate != null && object != null && m_subject != null) {
				try {
					m_topLevelVisitor.getTarget().add(new Statement(m_subject, predicate, object));
				} catch (RDFException e) {
					onException(e);
				}
			}
		}
	}
}
