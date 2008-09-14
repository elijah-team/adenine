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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.parser2.IParserVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ITopLevelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.SyntaxException;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.URIGenerator;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.HashMap;

/**
 * @author David Huynh
 */
abstract public class TopLevelVisitorBase extends ParserVisitorBase implements ITopLevelVisitor {
	protected IRDFContainer	m_target;
	protected Resource 		m_base;
	protected HashMap			m_prefixes = new HashMap();
	
	public TopLevelVisitorBase(IParserVisitor visitor, IRDFContainer target) {
		super(visitor);
		m_target = target;

		m_prefixes.put("random", Utilities.generateUniqueResource().getURI() + ":");
		m_prefixes.put("adenine", AdenineConstants.NAMESPACE);
		m_prefixes.put("rdf", Constants.s_rdf_namespace);
		m_prefixes.put("rdfs", Constants.s_rdfs_namespace);
		m_prefixes.put("daml", Constants.s_daml_namespace);
		m_prefixes.put("xsd", Constants.s_xsd_namespace);
		m_prefixes.put("", Utilities.generateUniqueResource().getURI() + ":");
		m_prefixes.put("@urigenerator", new URIGenerator());
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ITopLevelVisitor#onBase(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken)
	 */
	public void onBase(
		SymbolToken atSignT,
		Token baseKeyword,
		ResourceToken baseURI) {

		Resource r = resolveURI(baseURI);
		
		if (r != null) {
			setBase(r);
		} else {
			onException(new SyntaxException("Base URI cannot be resolved", baseURI.getSpan()));
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ITopLevelVisitor#onPrefix(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken)
	 */
	public void onPrefix(
		SymbolToken atSignT,
		Token prefixKeyword,
		ResourceToken prefix,
		ResourceToken expansion) {

		Resource r = resolveURI(expansion);
		
		if (r != null) {
			setPrefix(prefix.getPrefix(), r.getContent());
		} else {
			onException(new SyntaxException("Prefix expansion cannot be resolved", expansion.getSpan()));
		}
	}


	public Resource getBase() {
		return m_base;
	}
	protected void setBase(Resource base) {
		m_prefixes.put("@urigenerator", new URIGenerator(base.getContent()));
		m_base = base;
	}
	
	protected void setPrefix(String prefix, String expansion) {
		m_prefixes.put(prefix, expansion);
	}
	
	public Resource resolveURI(ResourceToken r) {
		String s = r.resolveURI(m_prefixes);
		
		if (s == null) {
			return null;
		} else {
			return new Resource(s);
		}
	}
	
	public IRDFContainer getTarget() {
		return m_target;
	}
	
	public URIGenerator getURIGenerator() {
		return (URIGenerator) m_prefixes.get("@urigenerator");
	}
}
