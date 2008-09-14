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
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

/**
 * @author David Huynh
 */
public class CodeFrameWithMethodGen extends CodeFrame {
	public MethodGen 	m_methodGen;
		
	protected CodeFrameWithMethodGen(String frameName, ClassGen cg) {
		super(frameName, cg);
	}
	protected CodeFrameWithMethodGen(String frameName, CodeFrame codeFrame) {
		super(frameName, codeFrame, true, true);
	}

	public MethodGen getMethodGen() {
		return m_methodGen;
	}

	protected boolean internalAddBreakBranch(MethodGen mg, InstructionList iList, BranchHandle handle, int line) {
		InstructionList iList2 = new InstructionList();
	
		addCleanupCode(mg, iList2, line);
			
		iList.insert(handle, iList);

		return true;
	}
	
	protected boolean internalAddBreakBranch(MethodGen mg, InstructionList iList, int line) {
		addCleanupCode(mg, iList, line);
		return true;
	}
	
	protected boolean internalAddContinueBranch(MethodGen mg, InstructionList iList, int line) {
		addCleanupCode(mg, iList, line);
		return true;
	}
	
	protected void recursiveAddCleanupCode(MethodGen mg, InstructionList iList, int line) {
		addCleanupCode(mg, iList, line);
		// do not branch to parent
	}
}
