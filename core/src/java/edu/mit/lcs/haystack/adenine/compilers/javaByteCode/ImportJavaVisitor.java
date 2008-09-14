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
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

/**
 * @author David Huynh
 */
public class ImportJavaVisitor
	extends ConstructVisitorBase
	implements IImportJavaVisitor {
		
	public ImportJavaVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IImportJavaVisitor#onClass(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onClass(GenericToken klass) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = klass.getSpan().getStart().getTrueLine();
		
		/*
		 * 	Resolve the package + class into a Class object
		 */
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP), // dup package
			line);
		mg.addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), klass.getContent())),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				this.getClass().getName(), 
				"resolveClass", 
				JavaByteCodeUtilities.s_typeClass, 
				new Type[] { Type.STRING, Type.STRING }, 
				org.apache.bcel.Constants.INVOKESTATIC)),
			line);
			
		m_codeFrame.generateNewVariablePut(klass.getContent(), iList, m_codeFrame.getMethodGen(), line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IImportJavaVisitor#onImportJava(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken, edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken)
	 */
	public void onImportJava(
		GenericToken importjavaKeyword,
		LiteralToken pakkage) {

		InstructionList iList = m_codeFrame.getInstructionList();
		
		/*
		 * 	Push package as string
		 */
		 
		m_codeFrame.getMethodGen().addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), pakkage.getContent())),
			pakkage.getSpan().getStart().getTrueLine());
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IParserVisitor#end(edu.mit.lcs.haystack.adenine.tokenizer.Location)
	 */
	public void end(Location endLocation) {
		super.end(endLocation);
		
		m_codeFrame.getMethodGen().addLineNumber(
			m_codeFrame.getInstructionList().append(InstructionConstants.POP), // pop the package name
			endLocation.getTrueLine());
	}

	static public Class resolveClass(String pakkage, String klass) throws AdenineException {
		String className = pakkage + "." + klass;
		Class result = CoreLoader.loadClass(className);
		if (result == null)
			throw new AdenineException("importjava specifies unknown class " + className);
		return result;
	}
	
}
