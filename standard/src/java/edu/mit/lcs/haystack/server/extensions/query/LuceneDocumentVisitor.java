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
 * Created on Jul 21, 2003
 */

package edu.mit.lcs.haystack.server.extensions.query;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import edu.mit.lcs.haystack.lucene.document.Document;
import edu.mit.lcs.haystack.lucene.document.Field;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author vineet
 * @author yks
 *
 * implements interfaces to add literals or literal properties of resources when traversing the
 * literal graph   
 */
public class LuceneDocumentVisitor extends StackBasedPrefixDocumentVisitorBase {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(LuceneDocumentVisitor.class);

	private final LuceneAgent luceneAgent;
	private Document luceneDoc;

	/**
	 * @param LuceneAgent
	 */
	LuceneDocumentVisitor(LuceneAgent agent, Document luceneDoc) {
		this.luceneAgent = agent;
		this.luceneDoc = luceneDoc;
	}

	/**
	 * Given a field, and resource, this function extracts the dc:title (or rdfs:label if former doesn't exist)
	 * and adds the literal value of the property, to the lucene "globalDocText" field of the internal
	 * lucene document (luceneDoc). 
	 */

	public void visitResource(String field, Resource resVal) {
		luceneDoc.add(Field.Keyword(getFieldPrefix() + field, RDFTerm.getLuceneStr(resVal)));

		try {
			RDFNode resText = getRDFContainer().extract(resVal, Constants.s_dc_title, null);
			if (resText == null) {
				resText = getRDFContainer().extract(resVal, Constants.s_rdfs_label, null);
			}
			if (resText != null) {
				luceneDoc.add(Field.Text(LuceneAgent.s_str_lucene_globalDocText, resText.getContent()));
			}
		} catch (RDFException e) {
			s_logger.error("Could not add resource into global content for search!", e);
		}
	}
	
	/**
	 * Given a field, and resource, this function extracts the dc:title (or rdfs:label if former doesn't exist)
	 * and adds the literal value of the property, to the lucene "globalDocText" field of the internal
	 * lucene document (luceneDoc).  This method differs from "visitResource" in that a reverse extension is
	 * added prepended to the field name. 
	 */
	public void visitReverseResource(String field, Resource resVal) {
		luceneDoc.add(Field.Keyword(getFieldPrefix() + getReverseExt() + field, RDFTerm.getLuceneStr(resVal)));

		try {
			RDFNode resText = getRDFContainer().extract(resVal, Constants.s_dc_title, null);
			if (resText == null) {
				resText = getRDFContainer().extract(resVal, Constants.s_rdfs_label, null);
			}
			if (resText != null) {
				luceneDoc.add(Field.Text(LuceneAgent.s_str_lucene_globalDocText, resText.getContent()));
			}
		} catch (RDFException e) {
			s_logger.error("Could not add resource into global content for search!", e);
		}
	}

	/**
	 * adds the field, value pair to the internal luceneDoc.  Called in the
	 * case when the visited node is a literal.
	 */
	public void visitText(String field, String value) {
		luceneDoc.add(Field.Text(getFieldPrefix() + field, value));
		luceneDoc.add(Field.Text(LuceneAgent.s_str_lucene_globalDocText, value));
	}

	/**
	 * Called when the visited node has a content type (and has data stored
	 * externally).
	 */
	public void visitContent(String field, ContentClient cc) {
		try {
			Reader reader;

			// add in lucene for field
			reader = new BufferedReader(new InputStreamReader(cc.getContent()));
			luceneDoc.add(Field.Text(getFieldPrefix() + field, reader));

			// add in globally
			reader = new BufferedReader(new InputStreamReader(cc.getContent()));
			luceneDoc.add(Field.Text(LuceneAgent.s_str_lucene_globalDocText, reader));

		} catch (Exception e) {
			s_logger.error("Unexpected exception", e);
		}

	}
	
	/*
	 * we don't support visitText(String field, Reader reader) because then we would need two copies of the
	 * reader
	 */

	/**
	 * Returns the service accessor of the associated lucene agent
	 */
	public IServiceAccessor getSA() {
		return this.luceneAgent.m_sm;
	}

	/**
	 * returns the RDF container of associated the lucene agent
	 */
	public IRDFContainer getRDFContainer() {
		return this.luceneAgent.m_sm.getRootRDFContainer();
	}
}