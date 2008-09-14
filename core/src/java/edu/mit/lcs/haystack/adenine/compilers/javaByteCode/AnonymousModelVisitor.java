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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.instructions.ModelInstruction;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;

import org.apache.bcel.generic.*;

/**
 * @author David Huynh
 */
public class AnonymousModelVisitor extends ParserVisitorBase2 implements IAnonymousModelVisitor {
	protected int m_subjectIndex; 
	
	public AnonymousModelVisitor(TopLevelVisitor topLevelVisitor, CodeFrame codeFrame) {
		super(topLevelVisitor, codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onAttribute(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		/*
		 * 	The stack should look like this:
		 * 
		 * 		target		<-- top
		 * 		local rdfc
		 * 		target
		 * 		local rdfc
		 * 		...
		 */
		
		return new AttributeVisitor(m_topLevelVisitor, m_codeFrame) {
			boolean m_hasAttribute = false;
			public IExpressionVisitor onPredicate(Location location) {
				AnonymousModelVisitor.this.m_codeFrame.getMethodGen().addLineNumber(
					AnonymousModelVisitor.this.m_codeFrame.getInstructionList().append(InstructionConstants.DUP2),
					m_startLocation.getTrueLine());
		
				m_hasAttribute = true;
				
				return super.onPredicate(location);
			}
			public void end(Location endLocation) {
				if (m_hasAttribute) {
					onDoneOneStatement(endLocation);
				}
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelEnd(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelEnd(SymbolToken rightBraceT) {
		// add local rdf container to target then load subject back on stack
		
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = rightBraceT.getSpan().getStart().getTrueLine(); 
		
		mg.addLineNumber(
			iList.append(InstructionConstants.SWAP),
			line);
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeIRDFContainer.getClassName(),
				"add",
				Type.VOID,
				new Type[] { JavaByteCodeUtilities.s_typeIRDFContainer },
				org.apache.bcel.Constants.INVOKEINTERFACE
			)),
			line);

		mg.addLineNumber(
			iList.append(new ALOAD(m_subjectIndex)),
			line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelStart(SymbolToken dollarSignT, SymbolToken leftBraceT) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = dollarSignT.getSpan().getStart().getTrueLine(); 

		/*
		 * 	Construct a local RDF container and push it on the stack
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeLocalRDFContainer)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeLocalRDFContainer.getClassName(),
				"<init>",
				Type.VOID,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
		
		/*
		 * 	Push __dynamicenvironment__ and a null subject
		 */
		mg.addLineNumber(
			iList.append(new ALOAD(0)), 					// load this
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
				line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeDynamicEnvironment.getClassName(),
				"getTarget",
				JavaByteCodeUtilities.s_typeIRDFContainer,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKEVIRTUAL
			)),												// call __dynamicenvironment__.getTarget()
			line);
			
		m_subjectIndex = m_codeFrame.getMethodGen().addLocalVariable(
			Interpreter.generateIdentifier(), 
			JavaByteCodeUtilities.s_typeResource,
			null,
			null
		).getIndex();

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				"edu.mit.lcs.haystack.adenine.compilers.javaByteCode.AnonymousModelVisitor",
				"generateSubject",
				JavaByteCodeUtilities.s_typeResource,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);
		
		mg.addLineNumber(
			iList.append(new ASTORE(m_subjectIndex)),
			line);
	}
	
	protected void onDoneOneStatement(Location endLocation) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = endLocation.getTrueLine(); 

		/*
		 * 	The stack should look like this:
		 * 
		 * 		object		<-- top
		 * 		predicate
		 * 		target
		 * 		local rdfc
		 * 		target
		 * 		local rdfc
		 * 		...
		 */

		mg.addLineNumber(
			iList.append(new ALOAD(m_subjectIndex)), // push resolved subject
			line);
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				"edu.mit.lcs.haystack.adenine.compilers.javaByteCode.ModelVisitor",
				"addStatement",
				Type.VOID,
				new Type[] { 
					JavaByteCodeUtilities.s_typeLocalRDFContainer, 
					JavaByteCodeUtilities.s_typeIRDFContainer, 
					Type.OBJECT, 
					Type.OBJECT, 
					JavaByteCodeUtilities.s_typeResource 
				},
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);

		/*
		 * 	The stack should look like this:
		 * 
		 * 		target		<-- top
		 * 		local rdfc
		 * 		...
		 */
	}
	
	public static void addStatement(
		LocalRDFContainer 	rdfc, 
		IRDFContainer		target,
		Object 				predicate, 
		Object 				object,
		Resource			subject
	) throws AdenineException, RDFException {
		
		rdfc.add(new Statement(
			subject,
			(Resource) ModelInstruction.extractNode(predicate, target),
			ModelInstruction.extractNode(object, target)
		));
	}

	public static Resource generateSubject() throws AdenineException, RDFException {
		return Utilities.generateUniqueResource();
	}
}
