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
import edu.mit.lcs.haystack.adenine.constructs.IForVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.List;

/**
 * @author David Huynh
 */
public class ForVisitor
	extends ConstructVisitorBase
	implements IForVisitor {
		
	protected ExpressionVisitor	m_collection;
	protected InnerCodeBlockVisitor	m_block;
	protected Location			m_startLocation;
	protected GenericToken		m_var;
		
	public ForVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	public void end(Location endLocation) {
		if (m_collection != null) {
			Resource collection = m_collection.getInstructionResource();
			
			if (collection != null) {
				IRDFContainer target = m_topLevelVisitor.getTarget();

				m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.ForIn, m_startLocation);

				try {
					target.add(new Statement(m_instructionResource, AdenineConstants.var, m_topLevelVisitor.generateIdentifierInstruction(m_var)));
					target.add(new Statement(m_instructionResource, AdenineConstants.COLLECTION, collection));
					
					if (m_block != null) {
						Resource block = m_block.getFirstInstruction();

						if (block != null) {
							target.add(new Statement(m_instructionResource, AdenineConstants.body, block));
						}
					}
					
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
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IForVisitor#onBody()
	 */
	public ICodeBlockVisitor onBody() {
		m_block = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_block;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IForVisitor#onForIn(edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.Token)
	 */
	public IExpressionVisitor onForIn(
		GenericToken forKeyword,
		GenericToken iterator,
		GenericToken inKeyword) {
			
		m_startLocation = forKeyword.getSpan().getStart();
		m_var = iterator;
		m_collection = new ExpressionVisitor(m_topLevelVisitor);
		return m_collection;
	}

}
