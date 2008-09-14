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

package edu.mit.lcs.haystack.adenine.compilers.javaByteCode;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.*;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;

import java.util.*;
import org.apache.bcel.generic.*;

/**
 * @author David Huynh
 */
class TopLevelMethodVisitor extends ParserVisitorBase implements IMethodVisitor {
	TopLevelVisitor 	m_topLevelVisitor;
	
	TopLevelExpressionVisitor m_methodResourceVisitor;
	String				m_className;
	
	List				m_parameters = new LinkedList();
	List				m_namedParameters = new LinkedList();
	List				m_namedParameterURIs = new LinkedList();
	
	CodeFrameWithMethodGen	m_codeFrame;
	
	public TopLevelMethodVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
		
	}

	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		if (getMethodResource() != null) {
			return new TopLevelAttributeVisitor(m_topLevelVisitor, getMethodResource());
		} else {
			return new NullAttributeVisitor(m_visitor);
		}
	}

	public ICodeBlockVisitor onBlock() {
		if (getMethodResource() != null) {
			return new InnerCodeBlockVisitor(m_topLevelVisitor, m_codeFrame);
		} else {
			return new NullCodeBlockVisitor(m_visitor);
		}
	}
	
	public IExpressionVisitor onMethod(GenericToken methodKeyword) {
		return m_methodResourceVisitor = new TopLevelExpressionVisitor(m_topLevelVisitor, true) {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase#end(edu.mit.lcs.haystack.adenine.tokenizer.Location)
			 */
			public void end(Location endLocation) {
				super.end(endLocation);

				m_className = Interpreter.filterSymbols(getMethodResource().getURI());
			
				ClassGen cg = new ClassGen(
					m_className,	 											// class name
					"edu.mit.lcs.haystack.adenine.interpreter.CompiledMethod",	// extends, super class name
					m_topLevelVisitor.getCompiler().getSourceFile(),			// source file
					org.apache.bcel.Constants.ACC_PUBLIC |						// access flags 
					org.apache.bcel.Constants.ACC_SUPER,
					null
				);
			
				m_codeFrame = new CodeFrameWithMethodGen("", cg);
				m_codeFrame.m_methodGen = new MethodGen(
					org.apache.bcel.Constants.ACC_PROTECTED,	// access flags
					JavaByteCodeUtilities.s_typeMessage,					// return type
					new Type[] {},								// argument types
					new String[] {},							// argument names
					"doInvoke",									// method name
					m_className,								// class name
					m_codeFrame.getInstructionList(),
					m_codeFrame.getConstantPoolGen()
				);

				IRDFContainer 	target = m_topLevelVisitor.getTarget();
				try {
					target.add(new Statement(getMethodResource(), Constants.s_rdf_type, AdenineConstants.Method));
				} catch (RDFException e) {
					onException(e);
				}
			}
		};
	}

	public void onNamedParameter(
		ResourceToken name,
		SymbolToken equalT,
		GenericToken parameter) {

		if (getMethodResource() != null) {
			Resource resolvedName = m_topLevelVisitor.resolveURI(name);
			
			m_namedParameters.add(parameter.getContent());
			m_namedParameterURIs.add(resolvedName);
			
			m_codeFrame.addVariable(parameter.getContent());
			
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
			Resource		param = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
			
			try {
				target.add(new Statement(param, AdenineConstants.parameterName, resolvedName));
				target.add(new Statement(param, AdenineConstants.parameterVariable, m_topLevelVisitor.generateIdentifierInstruction(parameter)));
				
				target.add(new Statement(getMethodResource(), AdenineConstants.namedParameter, param));
			} catch (RDFException e) {
				onException(e);
			}
		}
	}

	public void onParameter(GenericToken parameter) {
		if (getMethodResource() != null) {
			m_parameters.add(parameter);
			m_codeFrame.addVariable(parameter.getContent());
		}
	}

	public void end(Location endLocation) {
		if (getMethodResource() != null && !m_codeFrame.m_error) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
	
			try {
				// Generate parameters
				List		parameterIdentifierInstructions = new ArrayList();
				Iterator 	i = m_parameters.iterator();
				while (i.hasNext()) {
					parameterIdentifierInstructions.add(
						m_topLevelVisitor.generateIdentifierInstruction((GenericToken) i.next()));
				}
				target.add(new Statement(getMethodResource(), AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(parameterIdentifierInstructions.iterator(), target)));
				
				if (JavaByteCodeUtilities.precompileMethod(
						m_codeFrame, 
						m_parameters, 
						m_namedParameters, 
						m_namedParameterURIs, 
						m_topLevelVisitor,
						endLocation)) {
					
					JavaByteCodeUtilities.writeClass(m_codeFrame.getClassGen(), m_topLevelVisitor.getCompiler());
					
					target.add(new Statement(getMethodResource(), Constants.s_haystack_JavaClass, new Literal(m_className)));
					target.add(new Statement(getMethodResource(), AdenineConstants.preload, new Literal("true")));
				}
			} catch (RDFException e) {
				onException(e);
			} catch (AdenineException e) {
				onException(e);
			} catch (Exception e) {
				onException(new AdenineException("Failed to precompile method " + getMethodResource(), e));
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#canBeAnonymous()
	 */
	public boolean canBeAnonymous() {
		return false;
	}

	Resource getMethodResource() {
		return m_methodResourceVisitor.getResource();
	}
	
}

