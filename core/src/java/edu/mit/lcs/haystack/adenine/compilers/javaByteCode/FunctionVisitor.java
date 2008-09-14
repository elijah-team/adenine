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

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.bcel.generic.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class FunctionVisitor
	extends ConstructVisitorBase
	implements IFunctionVisitor {

	CodeFrameWithMethodGen		m_innerCodeFrame;
	int						m_iIndex;
	int						m_parametersIndex;
	int						m_oldDenvIndex;
	InstructionHandle			m_startOfBody;
	List						m_cleanupBranchHandles = new ArrayList();

	public FunctionVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IFunctionVisitor#onFunction(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onFunction(GenericToken functionKeyword, GenericToken name) {
		int 	line = functionKeyword.getSpan().getStart().getTrueLine();
		String	functionName = name.getContent();
		String	methodName = "func_" + functionName + "_" + line;
			
		m_codeFrame.addVariable(functionName);
		
		String	varName = m_codeFrame.resolveVariableName(functionName);

		/*
		 * 	For the method that will implement the function
		 */
		{
			m_innerCodeFrame = new CodeFrameWithMethodGen(
				"func" + m_startLocation.getTrueLine() + "_", 
				m_codeFrame
			) {
				protected void addCleanupCode(MethodGen mg, InstructionList iList, int line) {
					BranchHandle ih = iList.append(new JSR(null));
				
					mg.addLineNumber(ih, line);
				
					m_cleanupBranchHandles.add(ih);
				}
			};
			
			m_innerCodeFrame.m_methodGen = new MethodGen(
				org.apache.bcel.Constants.ACC_PUBLIC,			// access flags
				JavaByteCodeUtilities.s_typeMessage,						// return type
				new Type[] {
					JavaByteCodeUtilities.s_typeMessage ,
					JavaByteCodeUtilities.s_typeDynamicEnvironment
				},												// argument types
				new String[] { 
					"message",
					"denv"
				},												// argument names
				methodName,										// method name
				m_innerCodeFrame.getClassGen().getClassName(),	// class name
				m_innerCodeFrame.getInstructionList(),
				m_innerCodeFrame.getConstantPoolGen()
			);
			
			generateStoringOldDenv(line);
			generateParameterInitialization(line);
		}
		
		generateConstructionOfFunctionCallable(varName, methodName, line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onParameter(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onParameter(GenericToken parameter) {
		String	varName = parameter.getContent();
		
		m_innerCodeFrame.addVariable(varName);
		
		String 			resolvedVarName = m_innerCodeFrame.resolveVariableName(varName);
		InstructionList iList = m_innerCodeFrame.getInstructionList();
		MethodGen		mg = m_innerCodeFrame.getMethodGen();
		int			line = parameter.getSpan().getStart().getTrueLine();

		/*
		 * 	Insert code for each parameter:
		 * 
		 * 		if (i < parameters.length) {
		 * 			<class-member-corresponding-to-parameter> = parameters[i++];
		 * 		}
		 */
		 
		mg.addLineNumber(
			iList.append(new ILOAD(m_iIndex)),
			line);
		mg.addLineNumber(
			iList.append(new ALOAD(m_parametersIndex)),
			line);
		mg.addLineNumber(
			iList.append(new ARRAYLENGTH()),
			line);
		
		BranchHandle bh = iList.append(new IF_ICMPGE(null));
		
		mg.addLineNumber(bh, line);
		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(new ALOAD(m_parametersIndex)),	// load parameters
			line);
		mg.addLineNumber(
			iList.append(new ILOAD(m_iIndex)),				// load i
			line);
		mg.addLineNumber(
			iList.append(new IINC(m_iIndex, 1)),			// i = i + 1
			line);
		mg.addLineNumber(
			iList.append(new AALOAD()),					// load element of parameters at pushed i
			line);
		mg.addLineNumber(
			iList.append(m_innerCodeFrame.getInstructionFactory().createPutField(
				m_innerCodeFrame.getClassGen().getClassName(),
				resolvedVarName,
				Type.OBJECT
			)),												// store
			line);
		
		InstructionHandle ih = iList.append(new NOP());
		 
		mg.addLineNumber(ih, line);
		
		bh.setTarget(ih);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IFunctionVisitor#onBody()
	 */
	public ICodeBlockVisitor onBody() {
		m_startOfBody = m_innerCodeFrame.getInstructionList().append(InstructionConstants.NOP);
		
		m_innerCodeFrame.getMethodGen().addLineNumber(m_startOfBody, m_startLocation.getTrueLine());
		
		return new InnerCodeBlockVisitor(m_topLevelVisitor, m_innerCodeFrame);
	}

	public void end(Location endLocation) {
		InstructionList 	iList = m_innerCodeFrame.getInstructionList();
		MethodGen			mg = m_innerCodeFrame.getMethodGen();
		int				line = endLocation.getTrueLine();
		
		InstructionHandle	endOfBody = iList.append(InstructionConstants.NOP); mg.addLineNumber(m_startOfBody, line);
		
		InstructionHandle[] ihs = JavaByteCodeUtilities.generateTryFinally(
			m_innerCodeFrame.getClassGen(),
			mg,
			iList,
			m_innerCodeFrame.getConstantPoolGen(),
			new CodeGenerator() {
				int m_line;
				public void generate(
					ClassGen cg,
					MethodGen mg,
					InstructionList iList,
					ConstantPoolGen cpg) {
							
					generateRestoringOldDenv(iList, m_line);
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

		mg.addExceptionHandler(m_startOfBody, endOfBody, ihCatchAllClause, Type.THROWABLE);
		
		/*
		 * 	Construct a Message object to return
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeMessage)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeMessage.getClassName(),
				"<init>",
				Type.VOID,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
		mg.addLineNumber(
			iList.append(new ARETURN()),
			line);
		
		m_innerCodeFrame.m_methodGen.setMaxStack();
		m_innerCodeFrame.m_methodGen.setMaxLocals();
		m_innerCodeFrame.getClassGen().addMethod(m_innerCodeFrame.m_methodGen.getMethod());
	}
	
	protected void generateStoringOldDenv(int line) {
		InstructionList 	iList = m_innerCodeFrame.getInstructionList();
		MethodGen			mg = m_innerCodeFrame.getMethodGen();
		LocalVariableGen	lvg = mg.addLocalVariable("oldDenv", JavaByteCodeUtilities.s_typeDynamicEnvironment, null, null);
		
		m_oldDenvIndex = lvg.getIndex();

		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS), 					// load this
			line);
		mg.addLineNumber(
			iList.append(m_innerCodeFrame.getInstructionFactory().createGetField(
				m_innerCodeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),		// get __dynamicenvironment__
			line);
		mg.addLineNumber(
			iList.append(new ASTORE(m_oldDenvIndex)),
			line);

			
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.ALOAD_2),
			line);
		mg.addLineNumber(
			iList.append(m_innerCodeFrame.getInstructionFactory().createPutField(
				m_innerCodeFrame.getClassGen().getClassName(),
				"__dynamicenvironment__",
				JavaByteCodeUtilities.s_typeDynamicEnvironment
			)),
			line);
	}
	
	protected void generateRestoringOldDenv(InstructionList iList, int line) {
		MethodGen mg = m_innerCodeFrame.getMethodGen();
		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(new ALOAD(m_oldDenvIndex)),
			line);
		mg.addLineNumber(
			iList.append(m_innerCodeFrame.getInstructionFactory().createPutField(
				m_innerCodeFrame.getClassGen().getClassName(),
				"__dynamicenvironment__",
				JavaByteCodeUtilities.s_typeDynamicEnvironment
			)),
			line);
	}
	
	protected void generateParameterInitialization(int line) {
		InstructionList 	iList = m_innerCodeFrame.getInstructionList();
		MethodGen			mg = m_innerCodeFrame.m_methodGen;
		LocalVariableGen 	lg;
		
		/*
		 * 	Insert code:
		 * 		int i = 0;
		 */
			
		mg.addLineNumber(
			iList.append(new ICONST(0)),
			line);

		lg = mg.addLocalVariable("i", Type.INT, null, null);
		m_iIndex = lg.getIndex();

		mg.addLineNumber(
			iList.append(new ISTORE(m_iIndex)),
			line);
		
		/*
		 * 	Insert code:
		 * 		Object[] parameters = message.m_values;
		 */

		mg.addLineNumber(
			iList.append(new ALOAD(1)), // load message
			line);
		mg.addLineNumber(
			iList.append(m_innerCodeFrame.getInstructionFactory().createGetField(
				JavaByteCodeUtilities.s_typeMessage.getClassName(), "m_values", JavaByteCodeUtilities.s_typeObjectArray)),
			line);
				
		lg = mg.addLocalVariable("parameters", JavaByteCodeUtilities.s_typeObjectArray, null, null);
		m_parametersIndex = lg.getIndex();
			
		mg.addLineNumber(
			iList.append(new ASTORE(m_parametersIndex)),
			line);
	}
	
	protected void generateConstructionOfFunctionCallable(String varName, String methodName, int line) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
			
		/*
		 * 	Make a callable
		 */
			
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), methodName)),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				this.getClass().getName(),					// class name
				"makeCallable",								// constructor
				JavaByteCodeUtilities.s_typeCallable,					// return type
				new Type[] { Type.OBJECT, Type.STRING },	// argument types
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);
			
		/*
		 * 	Assign it to a member variable
		 */

		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.SWAP),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createPutField(
				m_codeFrame.getClassGen().getClassName(),
				varName,
				Type.OBJECT
			)),
			line);
	}
	
	static public ICallable makeCallable(Object methodObject, String methodName) { 
		return new ICallable() {
			Method 	m_function;
			Object	m_methodObject;
			
			public Message invoke(
				Message				message,
				DynamicEnvironment	denv
			) throws AdenineException {
				try {
					return (Message) m_function.invoke(m_methodObject, new Object[] { message, denv });
				} catch (IllegalArgumentException e) {
					throw new AdenineException("", e);
				} catch (IllegalAccessException e) {
					throw new AdenineException("", e);
				} catch (InvocationTargetException e) {
					throw new AdenineException("", e);
				} catch (Exception e) {
					throw new AdenineException("", e);
				}
			}
			
			public ICallable init(Object methodObject, String methodName) {
				try {
					m_function = methodObject.getClass().getMethod(methodName, new Class[] { Message.class, DynamicEnvironment.class });
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}

				m_methodObject = methodObject;
				return this;
			}
		}.init(methodObject, methodName);
	}
}
