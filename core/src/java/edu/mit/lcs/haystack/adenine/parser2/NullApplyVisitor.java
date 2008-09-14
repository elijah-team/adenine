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

package edu.mit.lcs.haystack.adenine.parser2;

import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;

/**
 * @author David Huynh
 */
public class NullApplyVisitor
	extends NullParserVisitor
	implements IApplyVisitor {

	public NullApplyVisitor() {};
	public NullApplyVisitor(IParserVisitor visitor) { super(visitor); }

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onLeftParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onLeftParenthesis(SymbolToken leftParenthesisT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onCallable()
	 */
	public IExpressionVisitor onCallable(Location location) {
		return new NullExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onArgument()
	 */
	public IExpressionVisitor onArgument(Location location) {
		return new NullExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onNamedArgument(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onNamedArgument(ResourceToken name, SymbolToken equalT) {
		return new NullExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onRightParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onRightParenthesis(SymbolToken rightParenthesisT) {
	}

}
