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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

import java.util.*;

/**
 * @author David Huynh
 */
public class FunctionVisitor
	extends ConstructVisitorBase
	implements IFunctionVisitor {

	List				m_parameters = new LinkedList();
	InnerCodeBlockVisitor	m_innerBlockVisitor;
	
	public FunctionVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IFunctionVisitor#onFunction(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onFunction(GenericToken functionKeyword, GenericToken name) {
		if (name instanceof GenericToken) {
			m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.Function, name.getSpan().getStart());
			
			Resource function = m_topLevelVisitor.generateIdentifierInstruction((GenericToken) name);
			
			try {
				m_topLevelVisitor.getTarget().add(new Statement(m_instructionResource, AdenineConstants.function, function));
			} catch (RDFException e) {
				onException(e);
			}
		} else {
			onException(new SyntaxException("Expected a generic token for anonymous function name", name.getSpan()));
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onParameter(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onParameter(GenericToken parameter) {
		if (m_instructionResource != null) {
			m_parameters.add(m_topLevelVisitor.generateIdentifierInstruction(parameter));
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IFunctionVisitor#onBody()
	 */
	public ICodeBlockVisitor onBody() {
		m_innerBlockVisitor = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_innerBlockVisitor;
	}

	public void start(Location startLocation) {
		super.start(startLocation);
		m_topLevelVisitor.pushMethod();
	}
	
	public void end(Location endLocation) {
		if (m_instructionResource != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
	
			try {
				target.add(new Statement(m_instructionResource, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(m_parameters.iterator(), target)));
				
				Resource firstInstruction = (m_innerBlockVisitor != null) ? m_innerBlockVisitor.getFirstInstruction() : null;
				if (firstInstruction != null) {
					target.add(new Statement(m_instructionResource, AdenineConstants.body, firstInstruction));
				}
			} catch (RDFException e) {
				onException(e);
			}
		}
		super.end(endLocation);
	}

}
