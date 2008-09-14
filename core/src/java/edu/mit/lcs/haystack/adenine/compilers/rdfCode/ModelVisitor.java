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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;

import java.util.*;

class Section {
	public ExpressionVisitor	m_subjectVisitor;
	public LinkedList			m_attributeVisitors = new LinkedList();
}

/**
 * @author David Huynh
 */
public class ModelVisitor extends ParserVisitorBase implements IModelVisitor {
	protected TopLevelVisitor 	m_topLevelVisitor;
	protected LinkedList			m_sections = new LinkedList();
	protected Section				m_section;
	protected Resource			m_instructionResource;
	
	public ModelVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor);
		m_topLevelVisitor = topLevelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onSubject(Location)
	 */
	public IExpressionVisitor onSubject(Location location) {
		m_section = new Section();
		m_section.m_subjectVisitor = new ExpressionVisitor(m_topLevelVisitor);
		
		m_sections.add(0, m_section);
		
		return m_section.m_subjectVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onAttribute(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		if (m_section != null && 
			m_section.m_subjectVisitor != null && 
			m_section.m_subjectVisitor.getInstructionResource() instanceof Resource) {
				
			AttributeVisitor av = new AttributeVisitor(m_topLevelVisitor);
			
			m_section.m_attributeVisitors.add(av);
			
			return av;
		} else {
			return new NullAttributeVisitor(m_visitor);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelEnd(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelEnd(SymbolToken rightBraceT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelStart(SymbolToken leftBraceT) {
	}
	
	public void end(Location endLocation) {
		IRDFContainer target = m_topLevelVisitor.getTarget();

		m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.Model, m_startLocation);
		
		Iterator s = m_sections.iterator();
		
		while (s.hasNext()) {
			Section 		section = (Section) s.next();
			Resource		subjectInstruction = section.m_subjectVisitor.getInstructionResource();
			
			if (subjectInstruction != null) {
				Iterator		v = section.m_attributeVisitors.iterator();
				
				while (v.hasNext()) {
					AttributeVisitor 	visitor = (AttributeVisitor) v.next();
					Resource			predicateInstruction = visitor.getPredicateInstruction();
					Resource			objectInstruction = visitor.getObjectInstruction();
					
					Resource			statement = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
					
					try {
						target.add(new Statement(statement, Constants.s_rdf_type, AdenineConstants.Statement));
						
						target.add(new Statement(statement, AdenineConstants.subject, subjectInstruction));
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
}
