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
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IListVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Huynh
 */
public class ListVisitor extends ParserVisitorBase implements IListVisitor {
	protected TopLevelVisitor	m_topLevelVisitor;
	protected Resource		m_instructionResource;
		
	protected List			m_arguments = new ArrayList();
		
	public ListVisitor(TopLevelVisitor visitor) {
		super(visitor);
		m_topLevelVisitor = visitor;
	}
	
	public void end(Location endLocation) {
		IRDFContainer target = m_topLevelVisitor.getTarget();

		m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.FunctionCall, m_startLocation);
		
		List arguments = new ArrayList();
		for (int i = 0; i < m_arguments.size(); i++) {
			Resource r = (((ExpressionVisitor) m_arguments.get(i)).getInstructionResource());
			if (r != null) {
				arguments.add(r);
			}
		}
		
		try {
			Resource identifier = m_topLevelVisitor.generateInstruction(AdenineConstants.Identifier, m_startLocation);
			
			target.add(new Statement(identifier, AdenineConstants.name, new Literal("List")));
			
			target.add(new Statement(m_instructionResource, AdenineConstants.function, identifier));
			target.add(new Statement(m_instructionResource, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(arguments.iterator(), target)));
		} catch (RDFException e) {
			onException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onItem(Location)
	 */
	public IExpressionVisitor onItem(Location location) {
		ExpressionVisitor ev = new ExpressionVisitor(m_topLevelVisitor);
		m_arguments.add(ev);
		
		return ev;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onLeftParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onLeftParenthesis(
		SymbolToken atSignT,
		SymbolToken leftParenthesisT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onRightParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onRightParenthesis(SymbolToken rightParenthesisT) {
	}
}
