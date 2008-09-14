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

import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import org.apache.bcel.generic.*;

/**
 * @author David Huynh
 */
public class AskModelVisitor extends ParserVisitorBase2 implements IAskModelVisitor {
	protected boolean m_firstTerm;
	
	static public ObjectType 	s_typeConditionSet = new ObjectType("edu.mit.lcs.haystack.adenine.query.ConditionSet");
	static public ObjectType 	s_typeCondition = new ObjectType("edu.mit.lcs.haystack.adenine.query.Condition");


	public AskModelVisitor(TopLevelVisitor topLevelVisitor, CodeFrame codeFrame) {
		super(topLevelVisitor, codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelEnd(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelEnd(SymbolToken rightBraceT) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IModelVisitor#onModelStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onModelStart(SymbolToken percentSignT, SymbolToken leftBraceT) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = percentSignT.getSpan().getStart().getTrueLine();

		/*
		 * 	Construct a ConditionSet and push it on the stack
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(s_typeConditionSet)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				s_typeConditionSet.getClassName(),
				"<init>",
				Type.VOID,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionStart(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onConditionStart(SymbolToken commaT) {
		m_firstTerm = true;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionEnd(Location)
	 */
	public void onConditionEnd(Location location) {
		if (!m_firstTerm) {
			InstructionList iList = m_codeFrame.getInstructionList();
			MethodGen		mg = m_codeFrame.getMethodGen();
			int			line = location.getTrueLine();
	
			/*
			 * 	Stack should look like this:
			 *
			 * 		parameter list				<-- top
			 * 		function resource 
			 * 		condition not initialized
			 * 		condition not initialized
			 * 		condition set
			 * 		condition set
			 * 		...
			 */
	
			/*
			 * 	Construct the Condition object
			 */
			mg.addLineNumber(
				iList.append(m_codeFrame.getInstructionFactory().createInvoke(
					s_typeCondition.getClassName(),
					"<init>",
					Type.VOID,
					new Type[] { Type.OBJECT, JavaByteCodeUtilities.s_typeArrayList },
					org.apache.bcel.Constants.INVOKESPECIAL
				)),
				line);
	
	
			/*
			 * 	Add the Condition to the Condition Set
			 */
			mg.addLineNumber(
				iList.append(m_codeFrame.getInstructionFactory().createInvoke(
					s_typeConditionSet.getClassName(),
					"add",
					Type.VOID,
					new Type[] { s_typeCondition },
					org.apache.bcel.Constants.INVOKEVIRTUAL
				)),
				line);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor#onConditionTerm(Location)
	 */
	public IExpressionVisitor onConditionTerm(Location location) {
		if (m_firstTerm) {
			InstructionList iList = m_codeFrame.getInstructionList();
			MethodGen		mg = m_codeFrame.getMethodGen();
			int			line = location.getTrueLine();
		
			mg.addLineNumber(
				iList.append(InstructionConstants.DUP),	// dup the condition set
				line);

			/*
			 * 	Allocate a Condition and push it on the stack and dup it
			 */
		
			mg.addLineNumber(
				iList.append(m_codeFrame.getInstructionFactory().createNew(s_typeCondition)),
				line);
			mg.addLineNumber(
				iList.append(InstructionConstants.DUP),
				line);

			/*
			 * 	Stack should look like this:
			 * 
			 * 		condition not initialized	<-- top
			 * 		condition not initialized
			 * 		condition set
			 * 		condition set
			 * 		...
			 */
		 
			m_firstTerm = false;
			return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
				public void end(Location endLocation) {
					onDoneFirstTerm();
				}
			};
		} else {
			InstructionList iList = m_codeFrame.getInstructionList();
			
			m_codeFrame.getMethodGen().addLineNumber(
				iList.append(InstructionConstants.DUP), // dup the parameter list
				m_startLocation.getTrueLine());
			
			/*
			 * 	Stack should look like this:
			 *
			 *		parameter list
			 *		parameter list
			 * 		function resource 
			 * 		condition not initialized
			 * 		condition not initialized
			 * 		condition set
			 * 		condition set
			 * 		...
			 */
			 
			return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
				public void end(Location endLocation) {
					onDoneTerm();
				}
			};
		}
	}

	protected void onDoneFirstTerm() {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = m_startLocation.getTrueLine();

		/*
		 * 	Stack should look like this:
		 *
		 * 		function resource 
		 * 		condition not initialized
		 * 		condition not initialized
		 * 		condition set
		 * 		condition set
		 * 		...
		 */
		 
		/*
		 * 	Create parameter list
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeArrayList)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeArrayList.getClassName(),
				"<init>",
				Type.VOID,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);

		/*
		 * 	Stack should look like this:
		 *
		 *		parameter list
		 * 		function resource 
		 * 		condition not initialized
		 * 		condition not initialized
		 * 		condition set
		 * 		condition set
		 * 		...
		 */
	}
	
	protected void onDoneTerm() {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = m_startLocation.getTrueLine();

		/*
		 * 	Stack should look like this:
		 *
		 *		term
		 *		parameter list
		 *		parameter list
		 * 		function resource 
		 * 		condition not initialized
		 * 		condition not initialized
		 * 		condition set
		 * 		condition set
		 * 		...
		 */
		 
		/*
		 * 	Add term to parameter list
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeArrayList.getClassName(),
				"add",
				Type.BOOLEAN,
				new Type[] { Type.OBJECT },
				org.apache.bcel.Constants.INVOKEVIRTUAL
			)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.POP),
			line);

		/*
		 * 	Stack should look like this:
		 *
		 *		parameter list
		 * 		function resource 
		 * 		condition not initialized
		 * 		condition not initialized
		 * 		condition set
		 * 		condition set
		 * 		...
		 */
	}
}
