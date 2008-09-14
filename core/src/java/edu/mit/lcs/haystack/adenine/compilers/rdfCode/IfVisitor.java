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
import edu.mit.lcs.haystack.adenine.constructs.IIfVisitor;
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
public class IfVisitor
	extends ConstructVisitorBase
	implements IIfVisitor {
		
	protected ExpressionVisitor	m_condition;
	protected InnerCodeBlockVisitor	m_blockIf;
	protected InnerCodeBlockVisitor	m_blockElse;
	protected Location			m_startLocation;
		
	public IfVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	public void end(Location endLocation) {
		if (m_condition != null) {
			Resource condition = m_condition.getInstructionResource();
			
			if (condition != null) {
				IRDFContainer target = m_topLevelVisitor.getTarget();

				m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.If, m_startLocation);

				try {
					target.add(new Statement(m_instructionResource, AdenineConstants.CONDITION, condition));
					
					if (m_blockIf != null) {
						Resource block = m_blockIf.getFirstInstruction();
						if (block != null) {
							target.add(new Statement(m_instructionResource, AdenineConstants.body, block));
						}
					}
					
					if (m_blockElse != null) {
						Resource block = m_blockElse.getFirstInstruction();
						if (block != null) {
							target.add(new Statement(m_instructionResource, AdenineConstants.ELSEBODY, block));
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
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onIf(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onIf(GenericToken ifKeyword) {
		m_startLocation = ifKeyword.getSpan().getStart();
		m_condition = new ExpressionVisitor(m_topLevelVisitor);
		return m_condition;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onIfBody(Location)
	 */
	public ICodeBlockVisitor onIfBody(Location location) {
		m_blockIf = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_blockIf;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onElseIf(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onElseIf(GenericToken ifKeyword) {
		m_startLocation = ifKeyword.getSpan().getStart();
		m_condition = new ExpressionVisitor(m_topLevelVisitor);
		return m_condition;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onElseIfBody(Location)
	 */
	public ICodeBlockVisitor onElseIfBody(Location location) {
		m_blockIf = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_blockIf;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onElse(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public ICodeBlockVisitor onElse(GenericToken elseKeyword) {
		m_blockElse = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_blockElse;
	}
}
