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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

import java.util.*;

import org.apache.bcel.generic.*;

/**
 * @author Dennis Quan
 */
public class MethodVisitor
	extends UniqueMethodVisitor {
	ExpressionVisitor m_methodNameVisitor;

	public MethodVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onMethod(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onMethod(GenericToken methodKeyword) {
		return m_methodNameVisitor = new ExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase#end(edu.mit.lcs.haystack.adenine.tokenizer.Location)
			 */
			public void end(Location endLocation) {		
				super.end(endLocation);
				initMethod();
			}
		};
	}
	
	protected void generateMethodConstructionCode(List orderedBackquotedIdentifiers) {
		InstructionList	iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		Iterator 		i = orderedBackquotedIdentifiers.iterator();
		int			line = m_startLocation.getTrueLine();

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createCast(Type.OBJECT, JavaByteCodeUtilities.s_typeResource)),
			line);

		/*
		 * 	Construct list of backquoted values
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
			line);

		while (i.hasNext()) {
			String name = (String) i.next();

			mg.addLineNumber(
				iList.append(InstructionConstants.DUP),
				line);
			
			m_codeFrame.generateVariableGet(name, iList, m_codeFrame.getMethodGen(), line);

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
		
		/*
		 * 	Push wrapped method URI and dynamic environment
		 */
		mg.addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), m_methodResource.getURI())),
			line);
		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),
			line);

		/*
		 * 	Call makeAnonymousMethod
		 */		

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				this.getClass().getName(),
				"makeMethod",
				Type.VOID,
				new Type[] { JavaByteCodeUtilities.s_typeResource, JavaByteCodeUtilities.s_typeList, Type.STRING, JavaByteCodeUtilities.s_typeDynamicEnvironment },
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);
	}
	
	static public void makeMethod(
			Resource			method, 
			List 				backquotedValues, 
			String 				wrappedMethodURI, 
			DynamicEnvironment 	denv) throws AdenineException {
				
		try {
			IRDFContainer target = denv.getTarget();

			target.add(new Statement(method, Constants.s_rdf_type, AdenineConstants.Method));
			target.add(new Statement(method, Constants.s_haystack_JavaClass, new Literal(AnonymousCompiledMethod.class.getName())));
			target.add(new Statement(method, AdenineConstants.preload, new Literal("true")));
			target.add(new Statement(method, AdenineConstants.function, new Resource(wrappedMethodURI)));
			target.add(new Statement(method, AdenineConstants.BACKQUOTED_PARAMETERS, ListUtilities.createDAMLList(backquotedValues.iterator(), target)));
		} catch (RDFException rdfe) {
			throw new AdenineException("RDF error", rdfe);
		}
	}
}
