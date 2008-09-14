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

package edu.mit.lcs.haystack.adenine.compilers.javaByteCode;

import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

/**
 * @author David Huynh
 */
public class TopLevelCallVisitor
	extends ConstructVisitorBase
	implements ICallVisitor {
		
	public TopLevelCallVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onArgument(Location)
	 */
	public IExpressionVisitor onArgument(Location location) {
		return new TopLevelExpressionVisitor(m_topLevelVisitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onCall(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onCall(GenericToken callKeyword) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onCallable(Location)
	 */
	public IExpressionVisitor onCallable(Location location) {
		return new NullExpressionVisitor(m_topLevelVisitor.getChainedVisitor());
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onComma(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onComma(SymbolToken commaT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onNamedArgument(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onNamedArgument(
		ResourceToken name,
		SymbolToken equalT) {

		onException(new SyntaxException("No named argument allowed for top level add", name.getSpan().getStart()));
		return new NullExpressionVisitor(m_topLevelVisitor.getChainedVisitor());
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onResult(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onResult(GenericToken variable) {
		onException(new SyntaxException("No result allowed for top level add", variable.getSpan().getStart()));
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.ICallVisitor#onNamedResult(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onNamedResult(
		ResourceToken name,
		SymbolToken equalT,
		GenericToken variable) {

		onException(new SyntaxException("No named result allowed for top level add", name.getSpan().getStart()));
	}
}
