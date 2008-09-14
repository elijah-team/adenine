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

import edu.mit.lcs.haystack.adenine.constructs.IIfVisitor;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author David Huynh
 */
public class IfVisitor
	extends ConstructVisitorBase
	implements IIfVisitor {
	
	protected List			m_doneBranchHandles = new ArrayList();
	protected BranchHandle	m_previousFalseBranchHandle;
		
	public IfVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	public void end(Location endLocation) {
		MethodGen 	mg = m_codeFrame.getMethodGen();
		int		line = endLocation.getTrueLine();
		
		hookupPreviousFalseBranch(line);
		
		InstructionHandle ihDone = m_codeFrame.getInstructionList().append(InstructionConstants.NOP);
		mg.addLineNumber(ihDone, line);
		
		Iterator i = m_doneBranchHandles.iterator();
		while (i.hasNext()) {
			((BranchHandle) i.next()).setTarget(ihDone);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onIf(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onIf(GenericToken ifKeyword) {
		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			public void end(Location endLocation) {
				onDoneTestExpression(endLocation);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onIfBody(Location)
	 */
	public ICodeBlockVisitor onIfBody(Location location) {
		return new InnerCodeBlockVisitor(
				m_topLevelVisitor, 
				new CodeFrame("if" + location.getTrueLine() + "_", m_codeFrame)) {
					
			public void end(Location endLocation) {
				super.end(endLocation);
				onDoneBody(this.m_codeFrame, endLocation);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onElseIf(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onElseIf(GenericToken elseIfKeyword) {
		onBeforeAnotherBranch(elseIfKeyword.getSpan().getStart());
		
		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			public void end(Location endLocation) {
				onDoneTestExpression(endLocation);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onElseIfBody(Location)
	 */
	public ICodeBlockVisitor onElseIfBody(Location location) {
		return new InnerCodeBlockVisitor(
				m_topLevelVisitor, 
				new CodeFrame("elseIf" + location.getTrueLine() + "_", m_codeFrame)) {
					
			public void end(Location endLocation) {
				super.end(endLocation);
				onDoneBody(this.m_codeFrame, endLocation);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IIfVisitor#onElse(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public ICodeBlockVisitor onElse(GenericToken elseKeyword) {
		onBeforeAnotherBranch(elseKeyword.getSpan().getStart());

		return new InnerCodeBlockVisitor(
				m_topLevelVisitor, 
				new CodeFrame("else" + elseKeyword.getSpan().getStart().getTrueLine() + "_", m_codeFrame)) {
					
			public void end(Location endLocation) {
				super.end(endLocation);
				onDoneBody(this.m_codeFrame, endLocation);
			}
		};
	}
	
	protected void onBeforeAnotherBranch(Location location) {
		MethodGen 	mg = m_codeFrame.getMethodGen();
		int		line = location.getTrueLine();
		
		BranchHandle bhDone = m_codeFrame.getInstructionList().append(new GOTO(null));
		mg.addLineNumber(bhDone, line);
		
		m_doneBranchHandles.add(bhDone);
		
		hookupPreviousFalseBranch(line);
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

		MethodGen 	mg = m_codeFrame.getMethodGen();
		int		line = location.getTrueLine();
		
		mg.addLineNumber(
			m_codeFrame.getInstructionList().append(m_codeFrame.getInstructionFactory().createInvoke(
				Interpreter.class.getName(),
				"isTrue",
				Type.BOOLEAN,
				new Type[] { Type.OBJECT },
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);

		m_previousFalseBranchHandle = m_codeFrame.getInstructionList().append(new IFEQ(null));
		mg.addLineNumber(m_previousFalseBranchHandle, line);
	}
	
	protected void onDoneBody(CodeFrame codeFrame, Location location) {
		codeFrame.appendToInstructionList(m_codeFrame.getInstructionList());
	}
	
	protected void hookupPreviousFalseBranch(int line) {
		MethodGen mg = m_codeFrame.getMethodGen();
		
		if (m_previousFalseBranchHandle != null) {
			InstructionHandle ih = m_codeFrame.getInstructionList().append(InstructionConstants.NOP);
			
			mg.addLineNumber(ih, line);
			
			m_previousFalseBranchHandle.setTarget(ih);
			
			m_previousFalseBranchHandle = null;
		}
	}
}
