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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;
import edu.mit.lcs.haystack.adenine.parser2.IAnonymousModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author David Huynh
 */
public class AnonymousModelVisitor extends ParserVisitorBase implements IAnonymousModelVisitor {
	protected TopLevelVisitor 	m_topLevelVisitor;
	protected LinkedList			m_attributeVisitors = new LinkedList();
	protected Resource			m_instructionResource;
	
	public AnonymousModelVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor);
		m_topLevelVisitor = topLevelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onAttribute(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		AttributeVisitor av = new AttributeVisitor(m_topLevelVisitor);
		
		m_attributeVisitors.add(av);
		
		return av;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelEnd(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelEnd(SymbolToken rightBraceT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelStart(SymbolToken dollarSignT, SymbolToken leftBraceT) {
	}
	
	public void end(Location endLocation) {
		IRDFContainer target = m_topLevelVisitor.getTarget();

		m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.BNode, m_startLocation);
		
		Iterator v = m_attributeVisitors.iterator();
		while (v.hasNext()) {
			AttributeVisitor 	visitor = (AttributeVisitor) v.next();
			Resource			predicateInstruction = visitor.getPredicateInstruction();
			Resource			objectInstruction = visitor.getObjectInstruction();
			
			if (predicateInstruction != null && objectInstruction != null) {
				Resource			statement = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
				
				try {
					target.add(new Statement(statement, Constants.s_rdf_type, AdenineConstants.Statement));
					
					target.add(new Statement(statement, AdenineConstants.predicate, predicateInstruction));
					target.add(new Statement(statement, AdenineConstants.object, objectInstruction));
					
					target.add(new Statement(m_instructionResource, AdenineConstants.statement, statement));
				} catch (RDFException e) {
					onException(e);
				}
			}
		}
	}
}
