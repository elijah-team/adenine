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

import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author David Huynh
 */
public class CodeFrameWithBreakContinue extends CodeFrame {
	protected List				m_breakBranchHandles = new ArrayList();
	protected InstructionHandle	m_continueTarget;
	
	public CodeFrameWithBreakContinue(String frameName, CodeFrame parentFrame, InstructionHandle continueTarget) {
		super(frameName, parentFrame);
		
		m_continueTarget = continueTarget;
	}
	
	protected boolean internalAddBreakBranch(MethodGen mg, InstructionList iList, BranchHandle handle, int line) {
		m_breakBranchHandles.add(handle);
		return true;
	}
	protected boolean internalAddBreakBranch(MethodGen mg, InstructionList iList, int line) {
		BranchHandle handle = iList.append(new GOTO(null));
			
		mg.addLineNumber(handle, line);
			
		m_breakBranchHandles.add(handle);
			
		return true;
	}

	protected boolean internalAddContinueBranch(MethodGen mg, InstructionList iList, int line) {
		mg.addLineNumber(
			iList.append(new GOTO(m_continueTarget)),
			line);
			
		return true;
	}

	public void resolveBreakBranchHandles(int line) {
		InstructionHandle ih = m_iList.append(new NOP());
		
		getMethodGen().addLineNumber(ih, line);
		
		Iterator i = m_breakBranchHandles.iterator();
		while (i.hasNext()) {
			((BranchHandle) i.next()).setTarget(ih);
		}
	}
}
