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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

/**
 * @author David Huynh
 */
public class TopLevelExpressionVisitor extends ParserVisitorBase implements IExpressionVisitor {
	TopLevelVisitorBase 			m_topLevelVisitor;
	boolean						m_expectsResource;

	TopLevelSubExpressionVisitor		m_subExpressionVisitor;
	TopLevelListVisitor					m_listVisitor;
	TopLevelModelVisitor				m_modelVisitor;
	TopLevelAnonymousModelVisitor		m_anonymousModelVisitor;
	RDFNode								m_result;
	
	public TopLevelExpressionVisitor(TopLevelVisitorBase topLevelVisitor, boolean expectsResource) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
		m_expectsResource = expectsResource;
	}

	public TopLevelExpressionVisitor(TopLevelVisitorBase topLevelVisitor) {
		this(topLevelVisitor, false);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onDereference(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public ISubExpressionVisitor onDereference(SymbolToken periodT) {
		onException(new SyntaxException("No dereferencement allowed at top level", periodT.getSpan()));
		return new NullSubExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLeftBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onLeftBracket(SymbolToken leftBracketT) {
		onException(new SyntaxException("No index allowed at top level", leftBracketT.getSpan()));
		return new NullExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onRightBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onRightBracket(SymbolToken rightBracketT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onSubExpression(Location)
	 */
	public ISubExpressionVisitor onSubExpression(Location location) {
		m_subExpressionVisitor = new TopLevelSubExpressionVisitor(m_topLevelVisitor);
		return m_subExpressionVisitor;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onAnonymousModel(Location)
	 */
	public IAnonymousModelVisitor onAnonymousModel(Location location) {
		m_anonymousModelVisitor = new TopLevelAnonymousModelVisitor(m_topLevelVisitor);
		return m_anonymousModelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onApply(Location)
	 */
	public IApplyVisitor onApply(Location location) {
		return new NullApplyVisitor(m_visitor) {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onLeftParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
			 */
			public void onLeftParenthesis(SymbolToken leftParenthesisT) {
				onException(new SyntaxException("Method application not allowed in top level model", leftParenthesisT.getSpan()));
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onAskModel(Location)
	 */
	public IAskModelVisitor onAskModel(Location location) {
		return new NullAskModelVisitor(m_visitor) {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
			 */
			public void onModelStart(SymbolToken percentT, SymbolToken leftBraceT) {
				onException(new SyntaxException("Ask model not allowed in top level model", percentT.getSpan()));
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onFloat(edu.mit.lcs.haystack.adenine.tokenizer.FloatToken)
	 */
	public void onFloat(FloatToken floatToken) {
		m_result = new Literal(floatToken.getContent());
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onInteger(edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken)
	 */
	public void onInteger(IntegerToken integerToken) {
		m_result = new Literal(integerToken.getContent());
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onList(Location)
	 */
	public IListVisitor onList(Location location) {
		m_listVisitor = new TopLevelListVisitor(m_topLevelVisitor);
		return m_listVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onLiteral(edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken)
	 */
	public void onLiteral(LiteralToken literalToken) {
		m_result = new Literal(literalToken.getContent());
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onModel(Location)
	 */
	public IModelVisitor onModel(Location location) {
		m_modelVisitor = new TopLevelModelVisitor(m_topLevelVisitor);
		return m_modelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onString(edu.mit.lcs.haystack.adenine.tokenizer.StringToken)
	 */
	public void onString(StringToken stringToken) {
		m_result = new Literal(stringToken.getContent());
	}

	public RDFNode getRDFNode() {
		if (m_result != null) {
			return m_result;
		} else if (m_listVisitor != null) {
			return m_listVisitor.getListResource();
		} else if (m_anonymousModelVisitor != null) {
			return m_anonymousModelVisitor.getAnonymousResource();
		} else if (m_modelVisitor != null) {
			return m_modelVisitor.getLastResource();
		} else {
			try {
				return m_subExpressionVisitor.getRDFNode();
			} catch (Exception e) {
				return null;
			}
		}
	}
	public Resource getResource() {
		return (Resource) getRDFNode();
	}
}
