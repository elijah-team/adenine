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
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.SyntaxException;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.URIGenerator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class BackQuoteExpression implements ITemplateExpression {
	ITemplateExpression m_exp;
	int m_line;
	
	public BackQuoteExpression(ITemplateExpression exp, int line) {
		m_exp = exp;
		m_line = line;
	}

	/**
	 * @see ITemplateExpression#generate(URIGenerator, IRDFContainer)
	 */
	public RDFNode generate(URIGenerator urig, IRDFContainer target) throws AdenineException {
		throw new SyntaxException("No context into which to backquote", m_line);
	}

	/**
	 * @see ITemplateExpression#generateIndirect()
	 */
	public ITemplateExpression generateIndirect() {
		return new ITemplateExpression() {
			public RDFNode generate(URIGenerator urig, IRDFContainer target) throws RDFException, AdenineException {
				Resource res = urig.generateAnonymousResource();
				Resource res1 = urig.generateAnonymousResource();
				Resource res2 = urig.generateAnonymousResource();
				
				ExistentialExpression rdfTypeEE = new ExistentialExpression();
				rdfTypeEE.add(Constants.s_rdf_type, AdenineConstants.Resource);
				rdfTypeEE.add(AdenineConstants.resource, Constants.s_rdf_type);
				
				ExistentialExpression adenineRDFNodeEE = new ExistentialExpression();
				adenineRDFNodeEE.add(Constants.s_rdf_type, AdenineConstants.Resource);
				adenineRDFNodeEE.add(AdenineConstants.resource, AdenineConstants.RDFNode);
				
				ExistentialExpression adenineRDFNode_EE = new ExistentialExpression();
				adenineRDFNode_EE.add(Constants.s_rdf_type, AdenineConstants.Resource);
				adenineRDFNode_EE.add(AdenineConstants.resource, AdenineConstants.rdfNode);
				
				target.add(new Statement(res, Constants.s_rdf_type, AdenineConstants.BNode));
				target.add(new Statement(res, AdenineConstants.statement, res1));
				target.add(new Statement(res, AdenineConstants.statement, res2));
				target.add(new Statement(res1, AdenineConstants.predicate, rdfTypeEE.generate(urig, target)));
				target.add(new Statement(res1, AdenineConstants.object, adenineRDFNodeEE.generate(urig, target)));
				target.add(new Statement(res2, AdenineConstants.predicate, adenineRDFNode_EE.generate(urig, target)));
				target.add(new Statement(res2, AdenineConstants.object, m_exp.generate(urig, target)));
				
				return res;
			}

			public ITemplateExpression generateIndirect() {
				// TODO[dquan]: implement generateIndirect for backquote expression
				return m_exp;
			}
		};
	}

}
