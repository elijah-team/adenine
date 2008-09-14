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
import edu.mit.lcs.haystack.adenine.compilers.utils.TopLevelExpressionVisitor;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class MethodVisitor
	extends ConstructVisitorBase
	implements IMethodVisitor {

	List				m_parameters = new LinkedList();
	InnerCodeBlockVisitor	m_innerBlockVisitor;
	TopLevelExpressionVisitor m_methodResourceVisitor;
	
	public MethodVisitor(TopLevelVisitor visitor, List instructionList) {
		super(visitor, instructionList);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onMethod(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onMethod(GenericToken methodKeyword) {
/*		if (name instanceof GenericToken) {
			m_topLevelVisitor.makeInstruction(m_instructionResource, AdenineConstants.MethodDef2, name.getSpan().getStart());
			
			Resource var = m_topLevelVisitor.generateIdentifierInstruction((GenericToken) name);
			
			setMethodResource(m_topLevelVisitor.getURIGenerator().generateAnonymousResource());
			
			try {
				m_topLevelVisitor.getTarget().add(new Statement(getMethodResource(), Constants.s_rdf_type, AdenineConstants.Method));
				 
				m_topLevelVisitor.getTarget().add(new Statement(m_instructionResource, AdenineConstants.var, var));
				m_topLevelVisitor.getTarget().add(new Statement(m_instructionResource, AdenineConstants.function, getMethodResource())); 
			} catch (RDFException e) {
				onException(e);
			}
		} else {
			onException(new SyntaxException("Expected a generic token for anonymous method name", name.getSpan()));
		}*/
		// TODO: Implement method
		return null;
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
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onNamedParameter(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onNamedParameter(
		ResourceToken name,
		SymbolToken equalT,
		GenericToken parameter) {

		IRDFContainer 	target = m_topLevelVisitor.getTarget();
		Resource		param = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
	
		try {
			target.add(new Statement(param, AdenineConstants.parameterName, m_topLevelVisitor.resolveURI(name)));
			target.add(new Statement(param, AdenineConstants.parameterVariable, m_topLevelVisitor.generateIdentifierInstruction(parameter)));
		
			target.add(new Statement(getMethodResource(), AdenineConstants.namedParameter, param));
		} catch (RDFException e) {
			onException(e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onBlock()
	 */
	public ICodeBlockVisitor onBlock() {
		m_innerBlockVisitor = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_innerBlockVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#canBeAnonymous()
	 */
	public boolean canBeAnonymous() {
		return true;
	}

	public void start(Location startLocation) {
		super.start(startLocation);
		m_topLevelVisitor.pushMethod();
	}
	
	public void end(Location endLocation) {
		Set backquotedIdentifiers = m_topLevelVisitor.popMethod();
		
		if (m_instructionResource != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
			ArrayList		orderedBackquotedIdentifiers = new ArrayList(backquotedIdentifiers);
			ArrayList		orderedBackquotedIdentifiersAsLiterals = new ArrayList();
	
			try {
				for (int i = orderedBackquotedIdentifiers.size() - 1; i >= 0; i--) {
					Resource 	p = m_topLevelVisitor.generateInstruction(AdenineConstants.Identifier, m_startLocation);
					Literal		identifier = new Literal((String) orderedBackquotedIdentifiers.get(i));

					target.add(new Statement(p, AdenineConstants.name, identifier));

					m_parameters.add(0, p);
					orderedBackquotedIdentifiersAsLiterals.add(0, identifier);
				}
				
				target.add(new Statement(getMethodResource(), AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(m_parameters.iterator(), target)));
				target.add(new Statement(m_instructionResource, AdenineConstants.BACKQUOTED_PARAMETERS, ListUtilities.createDAMLList(orderedBackquotedIdentifiersAsLiterals.iterator(), target)));
				
				Resource firstInstruction = (m_innerBlockVisitor != null) ? m_innerBlockVisitor.getFirstInstruction() : null;
				if (firstInstruction != null) {
					target.add(new Statement(getMethodResource(), AdenineConstants.start, firstInstruction));
				}
			} catch (RDFException e) {
				onException(e);
			}
		}
		super.end(endLocation);
	}

	Resource getMethodResource() {
		return m_methodResourceVisitor.getResource();
	}
}
