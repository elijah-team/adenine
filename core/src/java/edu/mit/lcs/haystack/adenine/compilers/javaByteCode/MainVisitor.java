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
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;
import edu.mit.lcs.haystack.adenine.compilers.utils.TopLevelAttributeVisitor;
import edu.mit.lcs.haystack.adenine.constructs.IMainVisitor;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAttributeVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import java.util.ArrayList;

/**
 * @author David Huynh
 */
class MainVisitor extends ParserVisitorBase implements IMainVisitor {
	TopLevelVisitor 		m_topLevelVisitor;
	
	Resource				m_methodResource;
	String					m_className;
	
	InnerCodeBlockVisitor		m_innerBlockVisitor;
	
	CodeFrameWithMethodGen	m_codeFrame;
	
	public MainVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
	}

	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		if (m_methodResource != null) {
			return new TopLevelAttributeVisitor(m_topLevelVisitor, m_methodResource);
		} else {
			return new NullAttributeVisitor(m_visitor);
		}
	}

	public ICodeBlockVisitor onMain(GenericToken mainKeyword) {
		m_methodResource = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
		m_className = Interpreter.filterSymbols(m_methodResource.getURI());
			
		ClassGen cg = new ClassGen(
			m_className,	 											// class name
			"edu.mit.lcs.haystack.adenine.interpreter.CompiledMethod",	// extends, super class name
			m_topLevelVisitor.getCompiler().getSourceFile() + "<main>",	// source file
			org.apache.bcel.Constants.ACC_PUBLIC |						// access flags 
			org.apache.bcel.Constants.ACC_SUPER,
			null
		);
			
		m_codeFrame = new CodeFrameWithMethodGen("main" + m_startLocation.getTrueLine() + "_", cg);
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
			target.add(new Statement(m_methodResource, Constants.s_rdf_type, AdenineConstants.Method));
		} catch (RDFException e) {
			onException(e);
		}


		m_innerBlockVisitor = new InnerCodeBlockVisitor(m_topLevelVisitor, m_codeFrame);
		return m_innerBlockVisitor;
	}

	public void end(Location endLocation) {
		if (m_methodResource != null && !m_codeFrame.m_error) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
	
			try {
				if (JavaByteCodeUtilities.precompileMethod(
						m_codeFrame, 
						new ArrayList(), 
						new ArrayList(), 
						new ArrayList(), 
						m_topLevelVisitor,
						endLocation)) {
					
					target.add(new Statement(m_methodResource, Constants.s_haystack_JavaClass, new Literal(m_className)));
					target.add(new Statement(m_methodResource, AdenineConstants.preload, new Literal("true")));

					JavaByteCodeUtilities.writeClass(m_codeFrame.getClassGen(), m_topLevelVisitor.getCompiler());
					
					Resource pakkage = m_topLevelVisitor.getPackage();
					if (pakkage != null) {
						target.replace(pakkage, AdenineConstants.main, null, m_methodResource);
					}
				} else {
					onException(new AdenineException("Failed to precompile main method at " + m_startLocation));
				}
			} catch (AdenineException e) {
				onException(e);
			} catch (RDFException e) {
				onException(e);
			}
		}
	}
}
