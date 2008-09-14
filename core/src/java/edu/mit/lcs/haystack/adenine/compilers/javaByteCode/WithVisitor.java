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

import edu.mit.lcs.haystack.adenine.constructs.IWithVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.JSR;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Huynh
 */
public class WithVisitor
	extends ConstructVisitorBase
	implements IWithVisitor {

	protected GenericToken		m_variable;
	protected CodeFrame			m_innerCodeFrame;
	protected List				m_cleanupBranchHandles = new ArrayList();
	protected int					m_savedValueIndex;
	protected int					m_isBoundIndex;
		
	public WithVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IWithVisitor#onLHS(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onLHS(GenericToken withKeyword, GenericToken variable) {
		m_variable = variable;
		
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = withKeyword.getSpan().getStart().getTrueLine();
		
		LocalVariableGen lg;
		
		lg = mg.addLocalVariable("savedValue", Type.OBJECT, null, null);
		m_savedValueIndex = lg.getIndex(); 

		lg = mg.addLocalVariable("isBound", Type.BOOLEAN, null, null);
		m_isBoundIndex = lg.getIndex(); 
		
		generateStoringOldValue(line);
	}
		
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IWithVisitor#onRHS(Location)
	 */
	public IExpressionVisitor onRHS(Location location) {
		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IForVisitor#onBody()
	 */
	public ICodeBlockVisitor onBody() {
		m_innerCodeFrame = new CodeFrame(
			"with" + m_startLocation.getTrueLine() + "_", 
			m_codeFrame) {
				
			protected void addCleanupCode(MethodGen mg, InstructionList iList, int line) {
				BranchHandle ih = iList.append(new JSR(null));
				
				mg.addLineNumber(ih, line);
				
				m_cleanupBranchHandles.add(ih);
			}
		};
		
		return new InnerCodeBlockVisitor(m_topLevelVisitor, m_innerCodeFrame);
	}

	public void end(Location endLocation) {
		if (m_variable != null && m_innerCodeFrame != null) {
			InstructionList iList = m_codeFrame.getInstructionList();
			MethodGen		mg = m_codeFrame.getMethodGen();
			int			line = endLocation.getTrueLine();
			
			generateStoringNewValue(m_startLocation.getTrueLine());
			
			InstructionHandle ihInnerCodeStart = m_innerCodeFrame.appendToInstructionList(iList);	// append body's code
			InstructionHandle ihInnerCodeEnd = iList.append(InstructionConstants.NOP); mg.addLineNumber(ihInnerCodeEnd, line);
			
			InstructionHandle[] ihs = JavaByteCodeUtilities.generateTryFinally(
				m_codeFrame.getClassGen(),
				mg,
				iList,
				m_codeFrame.getConstantPoolGen(),
				new CodeGenerator() {
					int m_line;
					public void generate(
						ClassGen cg,
						MethodGen mg,
						InstructionList iList,
						ConstantPoolGen cpg) {
							
						generateRestoringOldValue(iList, m_line);
					}
					public CodeGenerator init(int line) {
						m_line = line;
						return this;
					}
				}.init(line),
				line
			);
			InstructionHandle ihCatchAllClause = ihs[0];
			InstructionHandle ihFinallyClause = ihs[1];

			JavaByteCodeUtilities.setTargetOfBranches(m_cleanupBranchHandles, ihFinallyClause);
			
			mg.addExceptionHandler(ihInnerCodeStart, ihInnerCodeEnd, ihCatchAllClause, Type.THROWABLE);
		}
		super.end(endLocation);
	}
	
	protected void generateStoringOldValue(int line) {
		MethodGen		mg = m_codeFrame.getMethodGen();
		InstructionList iList = m_codeFrame.getInstructionList();

		mg.addLineNumber(
			iList.append(InstructionConstants.ACONST_NULL),
			line);
		mg.addLineNumber(
			iList.append(new ASTORE(m_savedValueIndex)),
			line);
		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),		// load this
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
			line);
			
		mg.addLineNumber(
			iList.append(new PUSH(							// push variable name as string
				m_codeFrame.getConstantPoolGen(), 
				m_variable.getContent())),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeDynamicEnvironment.getClassName(), 
				"isBound", 
				Type.BOOLEAN, 
				new Type[] { Type.STRING }, 
				org.apache.bcel.Constants.INVOKEVIRTUAL)),	// call __dynamicenvironment__.isBound(varName)
			line);

		mg.addLineNumber(
			iList.append(InstructionConstants.DUP), 		// duplicate the isBound result
			line);
		mg.addLineNumber(
			iList.append(new ISTORE(m_isBoundIndex)),
			line);
			
		
		/*
		 * 	Stack should look like this
		 * 		isBound result
		 * 		null
		 * 		...
		 */		

		
		BranchHandle bhNotBound = iList.append(new IFEQ(null));
															// if not bound, branch away

		mg.addLineNumber(bhNotBound, line);
		
		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),		// load this
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
			line);
			
		mg.addLineNumber(
			iList.append(new PUSH(							// push variable name as string
				m_codeFrame.getConstantPoolGen(), 
				m_variable.getContent())),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeDynamicEnvironment.getClassName(), 
				"getValue", 
				Type.OBJECT, 
				new Type[] { Type.STRING }, 
				org.apache.bcel.Constants.INVOKEVIRTUAL)),	// call __dynamicenvironment__.getValue(varName)
			line);
		mg.addLineNumber(
			iList.append(new ASTORE(m_savedValueIndex)),
			line);
		
		InstructionHandle ih = iList.append(InstructionConstants.NOP);
		 
		mg.addLineNumber(ih, line);
		
		bhNotBound.setTarget(ih);

		/*
		 * 	Stack should be empty
		 */
	}

	protected void generateRestoringOldValue(InstructionList iList, int line) {
		MethodGen		mg = m_codeFrame.getMethodGen();

		mg.addLineNumber(
			iList.append(new ILOAD(m_isBoundIndex)),
			line);
		
		BranchHandle bhNotBound = iList.append(new IFEQ(null));
															// if not bound, branch away
		mg.addLineNumber(bhNotBound, line);

		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),		// load this
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
			line);
			
		mg.addLineNumber(
			iList.append(new PUSH(							// push variable name as string
				m_codeFrame.getConstantPoolGen(), 
				m_variable.getContent())),
			line);

		mg.addLineNumber(
			iList.append(new ALOAD(m_savedValueIndex)),
			line);
				
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeDynamicEnvironment.getClassName(), 
				"setValue", 
				Type.VOID, 
				new Type[] { Type.STRING, Type.OBJECT }, 
				org.apache.bcel.Constants.INVOKEVIRTUAL)),	// call __dynamicenvironment__.getValue(varName)
			line);

		InstructionHandle ih = iList.append(InstructionConstants.NOP);
			 
		mg.addLineNumber(ih, line);
							
		bhNotBound.setTarget(ih);
	}
	
	protected void generateStoringNewValue(int line) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		
		/*
		 * We assume that the rhs expression has generated code
		 * that pushes the value of the expression onto the stack.
		 * Now we need to assign it.
		 */
			 
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),		// load this
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.SWAP),		// swap so that expression value is first again
			line);
			
		mg.addLineNumber(
			iList.append(new PUSH(							// push variable name as string
				m_codeFrame.getConstantPoolGen(), 
				m_variable.getContent())),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.SWAP),		// swap so that expression value is first again
			line);
				
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeDynamicEnvironment.getClassName(), 
				"setValue", 
				Type.VOID, 
				new Type[] { Type.STRING, Type.OBJECT }, 
				org.apache.bcel.Constants.INVOKEVIRTUAL)),	// call __dynamicenvironment__.getValue(varName)
			line);
	}
	
}
