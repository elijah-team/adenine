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

import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IListVisitor;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;

/**
 * @author David Huynh
 */
public class ListVisitor extends ParserVisitorBase2 implements IListVisitor {
	protected ApplyVisitor	m_applyVisitor;
	
	public ListVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
		m_applyVisitor = new ApplyVisitor(visitor, codeFrame);
	}
	
	public void start(Location startLocation) {
		m_applyVisitor.start(startLocation);
	}
	
	public void end(Location endLocation) {
		m_applyVisitor.end(endLocation);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onItem(Location)
	 */
	public IExpressionVisitor onItem(Location location) {
		return m_applyVisitor.onArgument(location);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IListVisitor#onLeftParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onLeftParenthesis(
		SymbolToken atSignT,
		SymbolToken leftParenthesisT) {
			
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = atSignT.getSpan().getStart().getTrueLine();
			
		m_applyVisitor.onCallable(atSignT.getSpan().getStart());
		
		/*
		 * 	Fake a push of type List
		 */
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetStatic(
				JavaByteCodeUtilities.s_typeInterpreter.getClassName(), "s_defaultEnvironment", JavaByteCodeUtilities.s_typeEnvironment)),
			line);
		
		mg.addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), "List")),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeEnvironment.getClassName(), "getValue", Type.OBJECT, new Type[] { Type.STRING }, org.apache.bcel.Constants.INVOKEVIRTUAL)),
			line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor#onRightParenthesis(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public void onRightParenthesis(SymbolToken rightParenthesisT) {
	}
}
