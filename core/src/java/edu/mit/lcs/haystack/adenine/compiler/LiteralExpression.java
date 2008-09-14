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

package edu.mit.lcs.haystack.adenine.compiler;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.URIGenerator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class LiteralExpression implements ITemplateExpression {
	Literal m_l;
	
	public LiteralExpression(Literal l) {
		m_l = l;
	}
	
	public LiteralExpression(String l) {
		m_l = new Literal(l);
	}
	
	/**
	 * @see ITemplateExpression#generate(URIGenerator,IRDFContainer)
	 */
	public RDFNode generate(URIGenerator urig, IRDFContainer target) throws RDFException {
		return m_l;
	}

	/**
	 * @see ITemplateExpression#generateIndirect()
	 */
	public ITemplateExpression generateIndirect() {
		ExistentialExpression ee = new ExistentialExpression();
		ee.add(Constants.s_rdf_type, AdenineConstants.Literal);
		ee.add(new ResourceExpression(AdenineConstants.literal), new LiteralExpression(m_l));
		return ee;
	}

}
