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

import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

/**
 * @author David Huynh
 */
public class TopLevelAddExpressionVisitor
	extends ParserVisitorBase
	implements IExpressionVisitor {

	public TopLevelAddExpressionVisitor(IParserVisitor visitor) { super(visitor); }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onDereference()
	 */
	public ISubExpressionVisitor onDereference(SymbolToken periodT) {
		return new NullSubExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLeftBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onLeftBracket(SymbolToken leftBracketT) {
		onException(new SyntaxException("No dereference allowed here. Expected only 'add'", leftBracketT.getSpan()));
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
		return new ISubExpressionVisitor() {
			public void onIdentifier(
				SymbolToken backquoteT,
				Token identifier) {

				if (backquoteT != null) {
					TopLevelAddExpressionVisitor.this.onException(new SyntaxException("No backquote allowed here. Expected only 'add'", backquoteT.getSpan()));
				} else if (!"add".equals(((GenericToken) identifier).getContent())) {
					TopLevelAddExpressionVisitor.this.onException(new SyntaxException("Expected only 'add' here", identifier.getSpan()));
				}
			}

			public void onResource(ResourceToken resourceToken) {
				TopLevelAddExpressionVisitor.this.onException(new SyntaxException("Expected only 'add' here", resourceToken.getSpan()));
			}

			public void start(Location startLocation) {
			}

			public void end(Location endLocation) {
			}

			public void onException(Exception exception) {
				TopLevelAddExpressionVisitor.this.onException(exception);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onApply(Location)
	 */
	public IApplyVisitor onApply(Location location) {
		onException(new SyntaxException("No application allowed here. Expected only 'add'", location));
		return new NullApplyVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onList(Location)
	 */
	public IListVisitor onList(Location location) {
		onException(new SyntaxException("No list allowed here. Expected only 'add'", location));
		return new NullListVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onModel(Location)
	 */
	public IModelVisitor onModel(Location location) {
		onException(new SyntaxException("No model allowed here. Expected only 'add'", location));
		return new NullModelVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onAnonymousModel(Location)
	 */
	public IAnonymousModelVisitor onAnonymousModel(Location location) {
		onException(new SyntaxException("No anonymous model allowed here. Expected only 'add'", location));
		return new NullAnonymousModelVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onAskModel(Location)
	 */
	public IAskModelVisitor onAskModel(Location location) {
		onException(new SyntaxException("No ask model allowed here. Expected only 'add'", location));
		return new NullAskModelVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLiteral(edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken)
	 */
	public void onLiteral(LiteralToken literalToken) {
		onException(new SyntaxException("No literal allowed here. Expected only 'add'", literalToken.getSpan()));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onString(edu.mit.lcs.haystack.adenine.tokenizer.StringToken)
	 */
	public void onString(StringToken stringToken) {
		onException(new SyntaxException("No string allowed here. Expected only 'add'", stringToken.getSpan()));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onInteger(edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken)
	 */
	public void onInteger(IntegerToken integerToken) {
		onException(new SyntaxException("No integer allowed here. Expected only 'add'", integerToken.getSpan()));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onFloat(edu.mit.lcs.haystack.adenine.tokenizer.FloatToken)
	 */
	public void onFloat(FloatToken floatToken) {
		onException(new SyntaxException("No float allowed here. Expected only 'add'", floatToken.getSpan()));
	}
}
