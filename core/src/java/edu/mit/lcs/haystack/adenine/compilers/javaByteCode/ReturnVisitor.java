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
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import org.apache.bcel.generic.*;

/**
 * @author David Huynh
 */
public class ReturnVisitor
	extends ConstructVisitorBase
	implements IReturnVisitor {
		
	protected int	m_resultCount = 0;
	protected int	m_namedResultCount = 0;
	
	public ReturnVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	public void end(Location endLocation) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = endLocation.getTrueLine();
		
		if (m_namedResultCount == 0) {
			if (m_resultCount == 0) {
				pushResultList(endLocation);
			}
			pushNamedResultMap(endLocation);
		}
		
		/*
		 * 	Stack should look like:
		 * 
		 * 		named result map
		 * 		result list
		 * 		message, uninitialized
		 * 		message, uninitialized
		 * 		...
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeMessage.getClassName(),
				"<init>",
				Type.VOID,
				new Type[] { JavaByteCodeUtilities.s_typeList, JavaByteCodeUtilities.s_typeMap },
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
		
		m_codeFrame.addReturnBranch(line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IReturnVisitor#onResult(Location)
	 */
	public IExpressionVisitor onResult(Location location) {
		InstructionList iList = m_codeFrame.getInstructionList();

		if (m_resultCount == 0) {
			pushResultList(location);
		}
		m_resultCount++;
		
		m_codeFrame.getMethodGen().addLineNumber(
			iList.append(InstructionConstants.DUP), // duplicate list
			location.getTrueLine());
		
		/*
		 * 	Stack should look like:
		 * 
		 * 		list
		 * 		list
		 * 		message, uninitialized
		 * 		message, uninitialized
		 * 		...
		 */

		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			public void end(Location endLocation) {
				onDoneResult(endLocation);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IReturnVisitor#onNamedResult(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onNamedResult(
		ResourceToken name,
		SymbolToken equalT) {

		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		Location		location = name.getSpan().getStart();
		int			line = location.getTrueLine();
		
		if (m_namedResultCount == 0) {
			if (m_resultCount == 0) {
				pushResultList(location);
			}
			pushNamedResultMap(location);
		}
		m_namedResultCount++;

		mg.addLineNumber(
			iList.append(InstructionConstants.DUP), 			// duplicate map
			line);
		
		/*
		 * 	Construct a Resource of the result name
		 */

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeResource)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);
		mg.addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), m_topLevelVisitor.resolveURI(name).getURI())),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeResource.getClassName(),
				"<init>",
				Type.VOID,
				new Type[] { Type.STRING },
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
		
		/*
		 * 	Stack should look like:
		 * 
		 * 		result URI
		 * 		map
		 * 		map
		 * 		list
		 * 		message, uninitialized
		 * 		message, uninitialized
		 * 		...
		 */
		
		return new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			public void end(Location endLocation) {
				onDoneNamedResult(endLocation);
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IReturnVisitor#onReturn(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onReturn(GenericToken returnKeyword) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = returnKeyword.getSpan().getStart().getTrueLine();

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeMessage)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);
	}

	protected void pushResultList(Location location) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = location.getTrueLine();

		/*
		 * 	Construct an ArrayList and push it on the stack
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
			line
		);
	}

	protected void pushNamedResultMap(Location location) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = location.getTrueLine();

		/*
		 * 	Construct a HashMap and push it on the stack
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeHashMap)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeHashMap.getClassName(),
				"<init>",
				Type.VOID,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
	}
	
	protected void onDoneResult(Location location) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = location.getTrueLine();

		/*
		 * 	Stack should look like:
		 *
		 * 		result value 
		 * 		list
		 * 		list
		 * 		message, uninitialized
		 * 		message, uninitialized
		 * 		...
		 */
		 
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeList.getClassName(),
				"add",
				Type.BOOLEAN,
				new Type[] { Type.OBJECT },
				org.apache.bcel.Constants.INVOKEINTERFACE
			)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.POP),
			line);
	}

	protected void onDoneNamedResult(Location location) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = location.getTrueLine();

		/*
		 * 	Stack should look like:
		 *
		 * 		result value 
		 * 		result URI
		 * 		map
		 * 		map
		 * 		list
		 * 		message, uninitialized
		 * 		message, uninitialized
		 * 		...
		 */
		 
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeMap.getClassName(),
				"put",
				Type.OBJECT,
				new Type[] { Type.OBJECT, Type.OBJECT },
				org.apache.bcel.Constants.INVOKEINTERFACE
			)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.POP),
			line);
	}
}
