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

package edu.mit.lcs.haystack.adenine.compilers.rdfCode;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.content.ContentClient;

/**
 * @author David Huynh
 */
public class RDFCodeCompiler implements ICompiler {
	protected IRDFContainer 	m_target;
	protected IParserVisitor	m_visitor = new IParserVisitor() {
		public void start(Location startLocation) {
		}

		public void end(Location endLocation) {
		}

		public void onException(Exception exception) {
			m_errors.add(exception);
		}
	};
	protected List m_errors;
	
	static final org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(RDFCodeCompiler.class);

	public RDFCodeCompiler(IRDFContainer target) {
		m_target = target;		
	}
	
	public java.util.List compile(Resource pakkage, Reader input, String sourceFile, IRDFContainer source, IServiceAccessor sa) {
			String str = null;
		if (input == null) {
			
			try {
				str = ContentClient.getContentClient(pakkage, source, sa).getContentAsString();
			} catch (Exception e) {
				List errors = new LinkedList();
				
				errors.add(e);
				
				return errors;
			}
			input = new StringReader(str); 
		}
		
		return doCompile(
				 str,
			input, 
			new TopLevelVisitor(
				new IParserVisitor() {
					public void start(Location startLocation) {
					}
					
					public void end(Location endLocation) {
					}
					
					public void onException(Exception exception) {
						addException(exception);
					}
				},
				pakkage,
				m_target
			)
		);
	}
	
	protected List doCompile(String str, Reader input, TopLevelVisitor visitor) {
		//s_logger.info("Compiling..." + str);
		
		m_errors = new LinkedList();
		
		Parser.parse(
			new IScannerVisitor() {
				public void onToken(Token token) {
					if (token instanceof ErrorToken) {
						addException(new SyntaxException("Error token of type " + token.getType(), token.getSpan()));
					}
				}
			},
			visitor,
			input
		);
		//s_logger.info("Done compiling with " + m_errors.size() + " error(s).");
		
		return m_errors;
	}
	
	public IRDFContainer getTarget() {
		return m_target;
	}
	public org.apache.log4j.Logger getLogger() {
		return s_logger;
	}
	public void addException(Exception e) {
		if (m_errors.isEmpty()) HaystackException.uncaught(e);

		m_errors.add(e);
		s_logger.error(e);
	}
}


