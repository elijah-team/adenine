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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

import java.util.*;

/**
 * @author David Huynh
 */
public class TopLevelListVisitor extends ParserVisitorBase implements IListVisitor {
	TopLevelVisitorBase m_topLevelVisitor;
	Resource			m_listResource;
	List				m_items = new LinkedList();
	boolean			m_error = false;
	
	public TopLevelListVisitor(TopLevelVisitorBase topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onItem(Location)
	 */
	public IExpressionVisitor onItem(Location location) {
		return new TopLevelExpressionVisitor(m_topLevelVisitor, false) {
			public void end(Location endLocation) {
				RDFNode node = getRDFNode();
				
				if (node != null) {
					m_items.add(0, node);
				} else {
					m_error = true;
				}
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onLeftParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onLeftParenthesis(
		SymbolToken atSignT,
		SymbolToken leftParenthesisT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onRightParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onRightParenthesis(SymbolToken rightParenthesisT) {
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IParserVisitor#end(edu.mit.lcs.haystack.adenine.tokenizer.Location)
	 */
	public void end(Location endLocation) {
		super.end(endLocation);
		
		if (!m_error) {
			if (m_items.size() == 0) {
				m_listResource = Constants.s_daml_nil;
			} else {
				IRDFContainer	target = m_topLevelVisitor.getTarget();
				Resource 		lastNode = Constants.s_daml_nil;
				
				while (m_items.size() > 0) {
					RDFNode item = (RDFNode) m_items.remove(0);
					
					m_listResource = Utilities.generateUniqueResource();
					
					try {
						target.add(new Statement(m_listResource, Constants.s_rdf_type, Constants.s_daml_List));
						target.add(new Statement(m_listResource, Constants.s_daml_rest, lastNode));
						target.add(new Statement(m_listResource, Constants.s_daml_first, item));
					} catch (RDFException e) {
						onException(e);
					}
					
					lastNode = m_listResource;
				}
			}
		}
	}

	public Resource getListResource() {
		return m_listResource;
	}
}

