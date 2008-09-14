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

import org.apache.bcel.generic.*;
import java.util.*;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;

/**
 * @author David Huynh
 */
public class CodeFrame {
	protected CodeFrame 			m_parentFrame;
	protected List				m_childFrames = new ArrayList();
	
	protected String 				m_frameName;

	protected ClassGen			m_classGen;
	protected ConstantPoolGen		m_constantPoolGen;
	protected InstructionFactory	m_iFactory;
	
	protected InstructionList		m_iList;
	
	protected Set 				m_variables = new HashSet();
	protected Set 				m_defaultEnvironmentVariables;
	
	protected boolean				m_error = false;

	public CodeFrame(String frameName, ClassGen classGen) {
		this(frameName, classGen, new HashSet());
	}
	public CodeFrame(String frameName, CodeFrame parentFrame) {
		this(frameName, parentFrame, true, true);
	}
	public CodeFrame(String frameName, CodeFrame parentFrame, boolean hookParent, boolean hookChild) {
		this(frameName, parentFrame.getClassGen(), parentFrame.getDefaultEnvironmentVariables());
		
		if (hookParent) {
			m_parentFrame = parentFrame;
		}
		if (hookChild) {
			parentFrame.m_childFrames.add(this);
		}
	}

	protected CodeFrame(String frameName, ClassGen classGen, Set defaultEnvironmentVariables) {
		m_classGen = classGen;
		m_frameName = frameName;
		
		m_constantPoolGen = classGen.getConstantPool();
		
		m_iList = new InstructionList();
		m_iFactory = new InstructionFactory(classGen);
		
		m_defaultEnvironmentVariables = defaultEnvironmentVariables;
	}
	
	public ClassGen 			getClassGen() { return m_classGen; }
	public MethodGen 			getMethodGen() { return m_parentFrame != null ? m_parentFrame.getMethodGen() : null; }
	
	public InstructionList 	getInstructionList() { return m_iList; }
	public InstructionFactory 	getInstructionFactory() { return m_iFactory; }
	public ConstantPoolGen 	getConstantPoolGen() { return m_constantPoolGen; }
	
	public boolean 			getError() { return m_error; }
	public void 				setError() { m_error = true; if (m_parentFrame != null) m_parentFrame.setError(); }


	/*
	 * 	Adds a break branch
	 */
	public boolean addBreakBranch(int line) {
		return internalAddBreakBranch(getMethodGen(), m_iList, line);
	}
	public boolean addBreakBranch(BranchHandle handle, int line) {
		return internalAddBreakBranch(getMethodGen(), m_iList, handle, line);
	}
	protected boolean internalAddBreakBranch(MethodGen mg, InstructionList iList, BranchHandle handle, int line) {
		{
			InstructionList iList2 = new InstructionList();
	
			addCleanupCode(mg, iList2, line);
			
			iList.insert(handle, iList);
		}
		if (m_parentFrame != null) {
			return m_parentFrame.internalAddBreakBranch(mg, iList, handle, line);
		} else {
			return false;
		}
	}
	protected boolean internalAddBreakBranch(MethodGen mg, InstructionList iList, int line) {
		addCleanupCode(mg, iList, line);
		if (m_parentFrame != null) {
			return m_parentFrame.internalAddBreakBranch(mg, iList, line);
		} else {
			return false;
		}
	}
	
	/*
	 * 	Adds a continue branch
	 */
	public boolean addContinueBranch(int line) {
		return internalAddContinueBranch(getMethodGen(), m_iList, line);
	}
	protected boolean internalAddContinueBranch(MethodGen mg, InstructionList iList, int line) {
		addCleanupCode(mg, iList, line);
		if (m_parentFrame != null) {
			return m_parentFrame.internalAddContinueBranch(mg, iList, line);
		} else {
			return false;
		}
	}
	
	/*
	 * 	Adds a return branch
	 */
	public void addReturnBranch(int line) {
		MethodGen			mg = getMethodGen();
		LocalVariableGen	lvg = mg.addLocalVariable("result", JavaByteCodeUtilities.s_typeMessage, null, null);
		int			 	index = lvg.getIndex();
		
		InstructionHandle	ihStart = m_iList.append(new ASTORE(index)); mg.addLineNumber(ihStart, line);
		
		recursiveAddCleanupCode(mg, m_iList, line);

		mg.addLineNumber(m_iList.append(new ALOAD(index)), line);
		
		InstructionHandle	ihEnd = m_iList.append(InstructionConstants.ARETURN);
		 
		mg.addLineNumber(ihEnd, line);
		
		lvg.setStart(ihStart);
		lvg.setEnd(ihEnd);
	}
	protected void recursiveAddCleanupCode(MethodGen mg, InstructionList iList, int line) {
		addCleanupCode(mg, iList, line);
		if (m_parentFrame != null) {
			m_parentFrame.recursiveAddCleanupCode(mg, iList, line);
		}
	}

	protected void addCleanupCode(MethodGen mg, InstructionList iList, int line) {
	}
	
	final public InstructionHandle appendToInstructionList(InstructionList iList) {
		return iList.append(m_iList);
	}
	
	public void addVariable(String name) {
		m_variables.add(name);
	}
	public void addTopVariable(String name) {
		CodeFrame cf = this;
		
		while (cf.m_parentFrame != null) {
			cf = cf.m_parentFrame;
		}
		cf.m_variables.add(name);
	}
	public String resolveVariableName(String name) {
		if (m_variables.contains(name)) {
			return resolveLocalVariable(name);
		} else if (m_parentFrame != null) {
			return m_parentFrame.resolveVariableName(name);
		} else if (Interpreter.s_defaultEnvironment.isBound(name)) {
			return resolveDefaultEnvironmentVariable(name);
		} else {
			return null;
		}
	}
	public Set getDefaultEnvironmentVariables() {
		return m_defaultEnvironmentVariables;
	}
	
	public void resolveAllLocalVariables(List names, List resolvedNames) {
		Iterator i = m_variables.iterator();
		
		while (i.hasNext()) {
			String name = (String) i.next();
			String resolvedName = resolveLocalVariable(name);
			
			names.add(name);
			resolvedNames.add(resolvedName);
		}
		
		i = m_childFrames.iterator();
		
		while (i.hasNext()) {
			((CodeFrame) i.next()).resolveAllLocalVariables(names, resolvedNames);
		}
	}
	public void resolveAllDefaultEnvironmentVariables(List names, List resolvedNames) {
		Iterator i = m_defaultEnvironmentVariables.iterator();
		
		while (i.hasNext()) {
			String name = (String) i.next();
			String resolvedName = resolveDefaultEnvironmentVariable(name);
			
			names.add(name);
			resolvedNames.add(resolvedName);
		}
	}
		
	public InstructionHandle generateVariableGet(String name, InstructionList iList, MethodGen mg, int line) {
		if (m_variables.contains(name)) {
			String resolvedMemberVariableName = resolveLocalVariable(name);

			InstructionHandle 	ih = iList.append(InstructionConstants.THIS);
			
			mg.addLineNumber(ih, line);
			mg.addLineNumber( 
				iList.append(m_iFactory.createGetField(
					m_classGen.getClassName(),
					resolvedMemberVariableName,
					Type.OBJECT
				)),
				line);
			
			return ih;
		} else if (m_parentFrame != null) {
			return m_parentFrame.generateVariableGet(name, iList, mg, line);
		} else if (Interpreter.s_defaultEnvironment.isBound(name)) {
			String resolvedMemberVariableName = resolveDefaultEnvironmentVariable(name);

			m_defaultEnvironmentVariables.add(name);

			InstructionHandle ih = iList.append(m_iFactory.createGetStatic(
				m_classGen.getClassName(),
				resolvedMemberVariableName,
				Type.OBJECT
			));
			
			mg.addLineNumber(ih, line);
			
			return ih;
		} else {
			InstructionHandle 	ih = iList.append(InstructionConstants.THIS);
			
			mg.addLineNumber(ih, line);
			mg.addLineNumber(
				iList.append(m_iFactory.createGetField(
					m_classGen.getClassName(), 
					"__dynamicenvironment__",
					JavaByteCodeUtilities.s_typeDynamicEnvironment
				)),
				line);
			mg.addLineNumber(
				iList.append(new PUSH(m_constantPoolGen, name)),
				line);
			mg.addLineNumber(
				iList.append(m_iFactory.createInvoke(
					JavaByteCodeUtilities.s_typeDynamicEnvironment.getClassName(), 
					"getValueChecked", 
					Type.OBJECT, 
					new Type[] { Type.STRING }, 
					org.apache.bcel.Constants.INVOKEVIRTUAL
				)),
				line);
			
			return ih;
		}
	}

	public InstructionHandle generateNewVariablePut(String name, InstructionList iList, MethodGen mg, int line) {
		if (!m_variables.contains(name)) {
			m_variables.add(name);
		}
		return generateLocalVariablePut(name, iList, mg, line);
	}
	
	public InstructionHandle generateVariablePut(String name, InstructionList iList, MethodGen mg, int line) {
		if (m_variables.contains(name)) {
			return generateLocalVariablePut(name, iList, mg, line);
		} else if (m_parentFrame != null) {
			return m_parentFrame.generateVariablePut(name, iList, mg, line);
		} else if (Interpreter.s_defaultEnvironment.isBound(name)) {
			String resolvedMemberVariableName = resolveDefaultEnvironmentVariable(name);

			m_defaultEnvironmentVariables.add(name);

			InstructionHandle ih = iList.append(m_iFactory.createPutStatic(
				m_classGen.getClassName(),
				resolvedMemberVariableName,
				Type.OBJECT
			));
			
			mg.addLineNumber(ih, line);
			
			return ih;
		} else {
			m_variables.add(name);
			return generateLocalVariablePut(name, iList, mg, line);
		}
	}
	protected InstructionHandle generateLocalVariablePut(String name, InstructionList iList, MethodGen mg, int line) {
		String resolvedMemberVariableName = resolveLocalVariable(name);

		InstructionHandle 	ih = iList.append(InstructionConstants.THIS);
		
		mg.addLineNumber(ih, line);
		mg.addLineNumber(
			iList.append(InstructionConstants.SWAP),
			line);
		mg.addLineNumber(
			iList.append(m_iFactory.createPutField(
				m_classGen.getClassName(),
				resolvedMemberVariableName,
				Type.OBJECT
			)),
			line);
			
		return ih;
	}
	
	protected String resolveLocalVariable(String name) {
		return "m_" + m_frameName + Interpreter.filterSymbols(name);
	}
	protected String resolveDefaultEnvironmentVariable(String name) {
		return "s_" + Interpreter.filterSymbols(name);
	}
	
	
	static public void printlnLastObject(InstructionList iList, InstructionFactory iFactory) {
		iList.append(InstructionConstants.DUP);
		iList.append(iFactory.createInvoke(
			CodeFrame.class.getName(),
			"println",
			Type.VOID,
			new Type[] { Type.OBJECT },
			org.apache.bcel.Constants.INVOKESTATIC
		));
	}
	
	static public void println(Object o) {
		System.err.println(o);
	}
}
