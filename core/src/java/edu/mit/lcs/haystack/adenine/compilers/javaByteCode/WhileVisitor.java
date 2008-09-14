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

import edu.mit.lcs.haystack.adenine.constructs.IWhileVisitor;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.Type;

/**
 * @author David Huynh
 */
public class WhileVisitor
	extends ConstructVisitorBase
	implements IWhileVisitor {
	
	protected CodeFrameWithBreakContinue	m_innerCodeFrame;
	protected InstructionHandle			m_continueTarget;
			
	public WhileVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	public void end(Location endLocation) {
		if (m_innerCodeFrame != null) {
			m_innerCodeFrame.addContinueBranch(endLocation.getTrueLine()); // loop back up
			
			m_innerCodeFrame.resolveBreakBranchHandles(endLocation.getTrueLine());
			m_innerCodeFrame.appendToInstructionList(m_codeFrame.getInstructionList());
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IForVisitor#onBody()
	 */
	public ICodeBlockVisitor onBody() {
		m_innerCodeFrame = new CodeFrameWithBreakContinue( 
			"while" + m_startLocation.getTrueLine() + "_", 
			m_codeFrame,
			m_continueTarget
		);
		
		BranchHandle bh = m_innerCodeFrame.getInstructionList().append(new IFEQ(null));
		
		m_innerCodeFrame.getMethodGen().addLineNumber(bh, m_startLocation.getTrueLine());
		
		m_innerCodeFrame.addBreakBranch(bh, m_startLocation.getTrueLine()); // this is for testing the expression
		
		return new InnerCodeBlockVisitor(m_topLevelVisitor, m_innerCodeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IWhileVisitor#onWhile(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onWhile(GenericToken whileKeyword) {
		m_continueTarget = m_codeFrame.getInstructionList().append(InstructionConstants.NOP);
		
		m_codeFrame.getMethodGen().addLineNumber(m_continueTarget, whileKeyword.getSpan().getStart().getTrueLine());
		
		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			public void end(Location endLocation) {
				onDoneTestExpression(endLocation);
			}
		};
	}
	
	protected void onDoneTestExpression(Location location) {
		/*
		 *  Stack should look like:
		 * 
		 * 		test expression value
		 * 		...
		 * 
		 * 	We need to turn the value into 1 for true and 0 for false
		 */

		m_codeFrame.getMethodGen().addLineNumber(
			m_codeFrame.getInstructionList().append(m_codeFrame.getInstructionFactory().createInvoke(
				Interpreter.class.getName(),
				"isTrue",
				Type.BOOLEAN,
				new Type[] { Type.OBJECT },
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			location.getTrueLine());
	}
}
