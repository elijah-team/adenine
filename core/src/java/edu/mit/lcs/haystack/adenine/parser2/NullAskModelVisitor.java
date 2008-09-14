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

/*
 * Created on Mar 11, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package edu.mit.lcs.haystack.adenine.parser2;

import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;

/**
 * @author David Huynh
 */
public class NullAskModelVisitor
	extends NullParserVisitor
	implements IAskModelVisitor {

	public NullAskModelVisitor() {};
	public NullAskModelVisitor(IParserVisitor visitor) { super(visitor); }

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelStart(SymbolToken percentT, SymbolToken leftBraceT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onConditionStart(SymbolToken commaT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionTerm(Location)
	 */
	public IExpressionVisitor onConditionTerm(Location location) {
		return new NullExpressionVisitor(m_visitor);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionEnd(Location)
	 */
	public void onConditionEnd(Location location) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onModelEnd(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelEnd(SymbolToken rightBraceT) {
	}

}
