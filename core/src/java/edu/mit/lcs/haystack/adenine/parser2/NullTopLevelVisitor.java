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

import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;

/**
 * @author David Huynh
 */
public class NullTopLevelVisitor
	extends NullCodeBlockVisitor
	implements ITopLevelVisitor {
		
	protected boolean m_interpretive;

	public NullTopLevelVisitor(boolean interpretive) {
		m_interpretive = interpretive;		
	}
	public NullTopLevelVisitor(IParserVisitor visitor, boolean interpretive) {
		super(visitor);
		m_interpretive = interpretive;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ITopLevelVisitor#onBase(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken)
	 */
	public void onBase(
		SymbolToken atSignT,
		Token baseKeyword,
		ResourceToken baseURI) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ITopLevelVisitor#onPrefix(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken)
	 */
	public void onPrefix(
		SymbolToken atSignT,
		Token prefixKeyword,
		ResourceToken prefix,
		ResourceToken expansion) {
	}

}
