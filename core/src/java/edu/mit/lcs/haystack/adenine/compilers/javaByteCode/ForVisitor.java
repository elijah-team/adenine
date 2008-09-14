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

import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.instructions.ForInInstruction;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import org.apache.bcel.generic.*;

/**
 * @author David Huynh
 */
public class ForVisitor
	extends ConstructVisitorBase
	implements IForVisitor {
		
	protected String						m_identifier;
	protected CodeFrameWithBreakContinue	m_innerCodeFrame;
		
	public ForVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	public void end(Location endLocation) {
		if (m_innerCodeFrame != null) {
			int line = endLocation.getTrueLine();
			
			m_innerCodeFrame.addContinueBranch(line); // loop back up
			
			m_innerCodeFrame.resolveBreakBranchHandles(line);
			m_innerCodeFrame.appendToInstructionList(m_codeFrame.getInstructionList());
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IForVisitor#onBody()
	 */
	public ICodeBlockVisitor onBody() {
		return new InnerCodeBlockVisitor(m_topLevelVisitor, m_innerCodeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IForVisitor#onForIn(edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.Token, edu.mit.lcs.haystack.adenine.tokenizer.Token)
	 */
	public IExpressionVisitor onForIn(
		GenericToken forKeyword,
		GenericToken iterator,
		GenericToken inKeyword) {
			
		m_identifier = iterator.getContent();
		
		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			public void end(Location endLocation) {
				onDoneExpression(endLocation);
			}
		};
	}

	protected void onDoneExpression(Location location) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = location.getTrueLine();
		
		LocalVariableGen	lvgIterator = mg.addLocalVariable("iterator", JavaByteCodeUtilities.s_typeIterator, null, null);
		int				iteratorIndex = lvgIterator.getIndex();

		/*
		 * 	Stack should look like
		 * 
		 * 		collection to iterate
		 * 		...
		 */
		 
		/*
		 * 	Push __dynamicenvironment__ and call ForInInstruction.getIterator
		 */
		 
		InstructionHandle ihStart = iList.append(InstructionConstants.THIS); 
	 
	 	mg.addLineNumber(
			ihStart,		// load this
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
			line);

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				ForInInstruction.class.getName(),
				"getIterator",
				JavaByteCodeUtilities.s_typeIterator,
				new Type[] { 
					Type.OBJECT,
					JavaByteCodeUtilities.s_typeDynamicEnvironment
				},
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);
			
		mg.addLineNumber(
			iList.append(new ASTORE(iteratorIndex)),
			line);

		/*
		 * 	Generate test code: iterator.hasNext()
		 */
		 
		InstructionHandle ihTest = iList.append(new ALOAD(iteratorIndex));
		
		mg.addLineNumber(ihTest, line);
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeIterator.getClassName(),
				"hasNext",
				Type.BOOLEAN,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKEINTERFACE
			)),
			line);
		
		m_innerCodeFrame = new CodeFrameWithBreakContinue(
			"for" + m_startLocation.getTrueLine() + "_",
			m_codeFrame,
			ihTest
		);
		
		InstructionList iList2 = m_innerCodeFrame.getInstructionList();
		
		/*
		 * 	Add break if test fails
		 */
		 
		BranchHandle ihTestFail = iList2.append(new IFEQ(null));
		
		mg.addLineNumber(ihTestFail, line);
		
		m_innerCodeFrame.addBreakBranch(ihTestFail, line);
		
		/*
		 * 	Retrieve iterator.next()
		 */
		
		mg.addLineNumber(
			iList2.append(new ALOAD(iteratorIndex)),
			line);

		mg.addLineNumber(
			iList2.append(m_innerCodeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeIterator.getClassName(),
				"next",
				Type.OBJECT,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKEINTERFACE
			)),
			line);

		m_innerCodeFrame.generateNewVariablePut(m_identifier, iList2, m_innerCodeFrame.getMethodGen(), line);
		
		InstructionHandle ihEnd = iList2.append(InstructionConstants.NOP);
		
		mg.addLineNumber(ihEnd, line);
		
		lvgIterator.setStart(ihStart);
		lvgIterator.setEnd(ihEnd);
	}
}
