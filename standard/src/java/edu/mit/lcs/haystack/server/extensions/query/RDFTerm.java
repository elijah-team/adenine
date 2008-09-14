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

package edu.mit.lcs.haystack.server.extensions.query;

import edu.mit.lcs.haystack.lucene.index.Term;

import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Represents a term (effectively a word/dimension for text based learning).
 * 
 * @author Vineet Sinha
 */
public class RDFTerm {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(RDFTerm.class);

	protected ResourceArray predicates;
	protected RDFNode object;

	public RDFTerm(ResourceArray predicates, RDFNode object) {
		this.predicates = predicates;
		this.object = object;
	}

	public RDFTerm(Term luceneTerm) {
		this(getResourceArray(luceneTerm.field()), getRDFNode(luceneTerm.text()));
	}

	/**
	 * Constructs the Term, there are old and should be removed 
	 */
	public RDFTerm(Resource predicate, RDFNode object) {
		this(new ResourceArray(predicate), object);
	}

	public RDFTerm(String predicate, String object) {
		this(new Resource(predicate), new Literal(object));
	}

	/*
	static private final Resource s_null_res = new Resource("null");
	public RDFTerm(String word) {
		this(s_null_res, new Literal(word));
	}
	*/

	//public Resource[] getPredicateArray() {
	//	return predicates.getArray();
	//}

	public ResourceArray getPredicates() {
		return predicates;
	}

	public RDFNode getObject() {
		return object;
	}

	public String toString() {
		return predicates.toString() + " " + object.toString();
	}

	public RDFTerm getTerm() {
		return this;
	}

	public boolean equals(Object o) {
		if ((o == null) || !(o instanceof RDFTerm)) {
			return false;
		}

		RDFTerm po = (RDFTerm) o;
		return po.object.equals(this.object) && po.predicates.equals(this.predicates);
	}

	public int hashCode() {
		return predicates.hashCode() + object.hashCode();
	}

	public static ResourceArray getResourceArray(String lucenField) {
		//s_logger.info("in LuceneAgenet.getResourceArray("+lucenField+")");

		int predCnt = 0;
		int[] predNdxArray = new int[10];
		predNdxArray[0] = -2;
		int findNdx = -1;
		do {
			findNdx = lucenField.indexOf(">.<", findNdx + 1);
			predCnt++;
			predNdxArray[predCnt] = findNdx;
			if (predCnt > 5)
				s_logger.info("predCnt>5: " + lucenField);
		} while (findNdx != -1 && predCnt < 10 - 1);
		predNdxArray[predCnt] = lucenField.length() - 1;

		Resource[] resArray = new Resource[predCnt];
		for (int i = 0; i < predCnt; i++) {
			resArray[i] = new Resource(lucenField.substring(predNdxArray[i] + 3, predNdxArray[i + 1]));
		}
		return new ResourceArray(resArray);
	}

	public static RDFNode getRDFNode(String luceneVal) {
		//s_logger.info("in LuceneAgenet.getRDFNode("+value+")");
		String content = null;
		int valLength = luceneVal.length();
		if (luceneVal.charAt(0) == '<' && luceneVal.charAt(valLength - 1) == '>') {
			return new Resource(luceneVal.substring(1, valLength - 1));
		} else {
			return new Literal(luceneVal);
		}
	}
	
	public static boolean isResource(String luceneVal) {
		if (luceneVal.charAt(0) == '<') {
			return true;
		} else {
			return false;
		}
	}

	public static String getLuceneStr(RDFNode rdfNode) {
		return rdfNode.toString();
	}

}
