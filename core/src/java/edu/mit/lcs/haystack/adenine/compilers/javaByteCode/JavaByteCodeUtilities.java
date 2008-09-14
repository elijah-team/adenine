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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.AdenineClassLoaderManager;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.*;

/**
 * @author David Huynh
 */
public class JavaByteCodeUtilities {
	static public ObjectType 	s_typeClass 				= new ObjectType("java.lang.Class");
	
	static public ObjectType 	s_typeMessage 				= new ObjectType("edu.mit.lcs.haystack.adenine.interpreter.Message");
	static public ObjectType 	s_typeEnvironment 			= new ObjectType("edu.mit.lcs.haystack.adenine.interpreter.Environment");
	static public ObjectType 	s_typeDynamicEnvironment 	= new ObjectType("edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment");
	static public ObjectType 	s_typeInterpreter 			= new ObjectType("edu.mit.lcs.haystack.adenine.interpreter.Interpreter");
	static public ObjectType 	s_typeCallable 				= new ObjectType("edu.mit.lcs.haystack.adenine.interpreter.ICallable");
	
	static public ObjectType 	s_typeRDFNode 				= new ObjectType("edu.mit.lcs.haystack.rdf.RDFNode");
	static public ObjectType 	s_typeResource 				= new ObjectType("edu.mit.lcs.haystack.rdf.Resource");
	static public ObjectType 	s_typeLiteral 				= new ObjectType("edu.mit.lcs.haystack.rdf.Literal");
	static public ObjectType 	s_typeStatement 			= new ObjectType("edu.mit.lcs.haystack.rdf.Statement");
	static public ObjectType 	s_typeIRDFContainer 		= new ObjectType("edu.mit.lcs.haystack.rdf.IRDFContainer");
	static public ObjectType 	s_typeLocalRDFContainer 	= new ObjectType("edu.mit.lcs.haystack.rdf.LocalRDFContainer");
	
	static public ObjectType 	s_typeDouble 				= new ObjectType("java.lang.Double");
	static public ObjectType 	s_typeInteger 				= new ObjectType("java.lang.Integer");
	static public ObjectType 	s_typeBoolean 				= new ObjectType("java.lang.Boolean");
	
	static public ObjectType 	s_typeList 					= new ObjectType("java.util.List");
	static public ObjectType 	s_typeArrayList 			= new ObjectType("java.util.ArrayList");
	static public ObjectType 	s_typeLinkedList 			= new ObjectType("java.util.LinkedList");
	static public ObjectType 	s_typeIterator 				= new ObjectType("java.util.Iterator");
	static public ObjectType 	s_typeMap 					= new ObjectType("java.util.Map");
	static public ObjectType 	s_typeHashMap 				= new ObjectType("java.util.HashMap");
	
	static public ArrayType 	s_typeObjectArray 			= new ArrayType("java.lang.Object", 1);

	static public InstructionHandle[] generateTryFinally(
		ClassGen			cg,
		MethodGen			mg,
		InstructionList		iList,
		ConstantPoolGen		cpg,
		CodeGenerator 		generator,
		int				line
	) {
		/*
		 * 	This is the generated code 
		 * 
		 * 	jsr <finally clause>
		 * 	goto <after finally>
		 * 
		 * 	astore <throwable>					<-- beginning of catch all
		 * 	jsr <finally clause>
		 * 	aload <throwable>
		 * 	athrow
		 * 
		 * 	astore <return address>				<-- beginning of finally clause
		 * 	<code body of finally clause>
		 * 	ret <return address>
		 * 
		 * 	nop									<-- after finally clause
		 */
		 
		BranchHandle	bhEndOfNormalCodeToFinally = iList.append(new JSR(null)); mg.addLineNumber(bhEndOfNormalCodeToFinally, line);
		BranchHandle	bhEndOfNormalCodeToAfterFinally = iList.append(new GOTO(null)); mg.addLineNumber(bhEndOfNormalCodeToAfterFinally, line);
		
		/*
		 * 	Catch all clause
		 */
		
		LocalVariableGen 	lvg1 = mg.addLocalVariable("throwable", Type.THROWABLE, null, null);
		int				throwableIndex = lvg1.getIndex();
		
		InstructionHandle	ihStartOfCatchAll = iList.append(new ASTORE(throwableIndex)); 	mg.addLineNumber(ihStartOfCatchAll, line);
		BranchHandle		bhCatchAllToFinally  = iList.append(new JSR(null)); 			mg.addLineNumber(bhCatchAllToFinally, line);
		InstructionHandle	ihLoadThrowable = iList.append(new ALOAD(throwableIndex)); 	mg.addLineNumber(ihLoadThrowable, line);
		InstructionHandle	ihThrow = iList.append(new ATHROW());							mg.addLineNumber(ihThrow, line);
		
		lvg1.setStart(ihStartOfCatchAll);
		lvg1.setEnd(ihThrow);
		
		/*
		 * 	Finally clause
		 */
		 
		LocalVariableGen 	lvg2 = mg.addLocalVariable("returnAddress", Type.THROWABLE, null, null);
		int				returnAddressIndex = lvg2.getIndex();
		
		InstructionHandle	ihStoreReturnAddress = iList.append(new ASTORE(returnAddressIndex));	mg.addLineNumber(ihStoreReturnAddress, line);
		
		generator.generate(cg, mg, iList, cpg);
		
		InstructionHandle	ihReturn = iList.append(new RET(returnAddressIndex));					mg.addLineNumber(ihReturn, line);
		
		lvg2.setStart(ihStoreReturnAddress);
		lvg2.setEnd(ihReturn);
		
		/*
		 * 	Wire up branches
		 */
		InstructionHandle	ihStartOfFinally = ihStoreReturnAddress;
		{ 
			InstructionHandle	ihAfterFinally = iList.append(InstructionConstants.NOP); mg.addLineNumber(ihAfterFinally, line);
			
			bhEndOfNormalCodeToFinally.setTarget(ihStartOfFinally);
			bhEndOfNormalCodeToAfterFinally.setTarget(ihAfterFinally);
			
			bhCatchAllToFinally.setTarget(ihStartOfFinally);
		}	
		
		return new InstructionHandle[] { ihStartOfCatchAll, ihStartOfFinally };
	}
	
	static public void setTargetOfBranches(List branchHandles, InstructionHandle target) {
		Iterator i = branchHandles.iterator();
		
		while (i.hasNext()) {
			BranchHandle bh = (BranchHandle) i.next();
			
			bh.setTarget(target);
		}
	}

	static public void writeClass(ClassGen cg, JavaByteCodeCompiler compiler) throws AdenineException {
		AdenineClassLoaderManager.getInstance().updateClass(
			cg.getJavaClass().getBytes(), 
			cg.getClassName()
		);
	}

	static public boolean precompileMethod(
		CodeFrameWithMethodGen 	methodCodeFrame, 
		List 					positionalParameters, 
		List 					namedParameters, 
		List 					namedParameterURIs, 
		TopLevelVisitor 		visitor,
		Location				endLocation
	) {
		if (insertMemberVariables(
				methodCodeFrame, 
				visitor) &&
			insertInitializeParametersMethod(
				methodCodeFrame, 
				positionalParameters,
				namedParameters,
				namedParameterURIs,
				visitor) && 
			insertDefaultEnvironmentMappings(
				methodCodeFrame, 
				visitor)) {
				
			InstructionList iList = methodCodeFrame.getInstructionList();
			MethodGen		mg = methodCodeFrame.getMethodGen();
			int			line = endLocation.getTrueLine();
				
			mg.addLineNumber(
				iList.append(methodCodeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeMessage)),
				line);
			mg.addLineNumber(
				iList.append(InstructionConstants.DUP),
				line);
			mg.addLineNumber(
				iList.append(methodCodeFrame.getInstructionFactory().createInvoke(
					JavaByteCodeUtilities.s_typeMessage.getClassName(),
					"<init>",
					Type.VOID,
					Type.NO_ARGS,
					Constants.INVOKESPECIAL
				)),
				line);
			mg.addLineNumber(iList.append(new ARETURN()), line);
			
			methodCodeFrame.getMethodGen().setMaxStack();
			methodCodeFrame.getMethodGen().setMaxLocals();
			
			methodCodeFrame.getClassGen().addMethod(methodCodeFrame.getMethodGen().getMethod());
			
			methodCodeFrame.getClassGen().addEmptyConstructor(Constants.ACC_PUBLIC);
			
			return true;
		}
		return false;
	}

	static public boolean insertMemberVariables(
		CodeFrame 		methodCodeFrame, 
		TopLevelVisitor	visitor
	) {
		List	names = new ArrayList();
		List	resolvedNames = new ArrayList();
		
		methodCodeFrame.resolveAllLocalVariables(names, resolvedNames);
		
		Iterator j = resolvedNames.iterator();
		
		while (j.hasNext()) {
			String resolvedName = (String) j.next();
			
			methodCodeFrame.getClassGen().addField(new FieldGen(
				Constants.ACC_PRIVATE,
				Type.OBJECT,
				resolvedName,
				methodCodeFrame.getConstantPoolGen()
			).getField());
		}
		
		return true;
	}

	static public boolean insertInitializeParametersMethod(
		CodeFrame 		methodCodeFrame, 
		List 			positionalParameters,
		List			namedParameters,
		List			namedParameterURIs,
		TopLevelVisitor	visitor
	) {
		InstructionList iList = new InstructionList();
		MethodGen 		mg = new MethodGen(
			Constants.ACC_PROTECTED,		// access flags
			Type.VOID,										// return type
			new Type[] { JavaByteCodeUtilities.s_typeMessage },		// argument types
			new String[] { "msg"},							// argument names
			"initializeParameters",							// method name
			methodCodeFrame.getClassGen().getClassName(),	// class name
			iList,
			methodCodeFrame.getConstantPoolGen()
		);
		
		LocalVariableGen lg;
		
		/*
		 * 	Insert code:
		 * 		int i = 0;
		 */
		int iIndex;
	
		iList.append(new ICONST(0));		
		lg = mg.addLocalVariable("i", Type.INT, null, null);
		iIndex = lg.getIndex();
		iList.append(new ISTORE(iIndex));
		
		/*
		 * 	Insert code:
		 * 		Object[] parameters = msg.m_values;
		 */
	
		int parametersIndex;
		iList.append(new ALOAD(1)); // load msg
		iList.append(methodCodeFrame.getInstructionFactory().createGetField(
			JavaByteCodeUtilities.s_typeMessage.getClassName(), "m_values", JavaByteCodeUtilities.s_typeObjectArray));
		lg = mg.addLocalVariable("parameters", JavaByteCodeUtilities.s_typeObjectArray, null, null);
		parametersIndex = lg.getIndex();
		iList.append(new ASTORE(parametersIndex));
		
		Iterator i, j;
		
		/*
		 * Handles positional parameters
		 */
		i = positionalParameters.iterator();
		while (i.hasNext()) {
			Object	o = i.next();
			String	varName = o instanceof GenericToken ? ((GenericToken) o).getContent() : (String) o;
			String	resolvedVarName = methodCodeFrame.resolveVariableName(varName);
	
			/*
			 * 	Insert code for each parameter:
			 * 
			 * 		if (i < parameters.length) {
			 * 			<class-member-corresponding-to-parameter> = parameters[i++];
			 * 		}
			 */
			
			if (resolvedVarName != null) {
				iList.append(new ILOAD(iIndex));
				iList.append(new ALOAD(parametersIndex));
				iList.append(new ARRAYLENGTH());
				
				BranchHandle bh = iList.append(new IF_ICMPGE(null));
				
				iList.append(new ALOAD(0));				// load this
				iList.append(new ALOAD(parametersIndex));	// load parameters
				iList.append(new ILOAD(iIndex));			// load i
				iList.append(new IINC(iIndex, 1));			// i = i + 1
				iList.append(new AALOAD());				// load element of parameters at pushed i
				
				iList.append(methodCodeFrame.getInstructionFactory().createPutField(
					methodCodeFrame.getClassGen().getClassName(), resolvedVarName, Type.OBJECT));	// store
				
				bh.setTarget(iList.append(new NOP()));
			} else {
				visitor.onException(new AdenineException("Failed to resolve var name " + varName));
				return false;
			}
		}
	
		/*
		 * Handles named parameters
		 */
		if (namedParameters != null && namedParameterURIs != null) {
			i = namedParameters.iterator();
			j = namedParameterURIs.iterator();
			
			while (i.hasNext()) {
				Object		o = i.next();
				String		varName = o instanceof GenericToken ? ((GenericToken) o).getContent() : (String) o;
				String		varURI = ((Resource) j.next()).getURI();
				String		resolvedVarName = methodCodeFrame.resolveVariableName(varName);
				
				/*
				 * 	Insert code:
				 * 		<class-member-corresponding-to-parameter> = msg.getNamedValue(
				 * 			new Resource(<varURI>));
				 */
				 
				if (resolvedVarName != null) {
					iList.append(new ALOAD(0)); // load this
					iList.append(new ALOAD(1)); // load msg
					
					iList.append(methodCodeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeResource.getClassName()));
					iList.append(InstructionConstants.DUP);
					iList.append(new PUSH(methodCodeFrame.getConstantPoolGen(), varURI));
					iList.append(methodCodeFrame.getInstructionFactory().createInvoke(
						JavaByteCodeUtilities.s_typeResource.getClassName(),	// class name
						"<init>",									// constructor
						Type.VOID,									// return type
						new Type[] { Type.STRING },				// argument types
						Constants.INVOKESPECIAL
					));
					
					iList.append(methodCodeFrame.getInstructionFactory().createInvoke(
						JavaByteCodeUtilities.s_typeMessage.getClassName(),		// class name
						"getNamedValue",							// method name
						Type.OBJECT,								// return type
						new Type[] { JavaByteCodeUtilities.s_typeResource },
						Constants.INVOKEVIRTUAL
					));
		
					iList.append(methodCodeFrame.getInstructionFactory().createPutField(
						methodCodeFrame.getClassGen().getClassName(), resolvedVarName, Type.OBJECT));	// store
				} else {
					visitor.onException(new AdenineException("Failed to resolve var name " + varName));
					return false;
				}
			}
		}
	
		iList.append(new RETURN());
		
		setLineNumbersEqualIndices(iList, mg);
	
		mg.setMaxStack();
		mg.setMaxLocals();
		methodCodeFrame.getClassGen().addMethod(mg.getMethod());
		
		return true;
	}

	static public boolean insertDefaultEnvironmentMappings(CodeFrame methodCodeFrame, TopLevelVisitor visitor) {
		InstructionList iList = new InstructionList();
		
		MethodGen mg = new MethodGen(
			Constants.ACC_STATIC,				// access flags
			Type.VOID,											// return type
			Type.NO_ARGS,										// argument types
			null,												// argument names
			Constants.STATIC_INITIALIZER_NAME,	// method name
			methodCodeFrame.getClassGen().getClassName(),		// class name
			iList,
			methodCodeFrame.getConstantPoolGen()
		);
		
		
		/*
		 * 	Insert code:
		 * 		Object defaultEnvironment = 0;
		 */
	
		LocalVariableGen 	lg;
		int 				defaultEnvironmentIndex;
		
		iList.append(methodCodeFrame.getInstructionFactory().createGetStatic(
			JavaByteCodeUtilities.s_typeInterpreter.getClassName(), "s_defaultEnvironment", JavaByteCodeUtilities.s_typeEnvironment));
	
		lg = mg.addLocalVariable("defaultEnvironment", JavaByteCodeUtilities.s_typeEnvironment, null, null);
		defaultEnvironmentIndex = lg.getIndex();
		iList.append(new ASTORE(defaultEnvironmentIndex));
		
		
		List names = new ArrayList();
		List resolvedNames = new ArrayList();
		
		methodCodeFrame.resolveAllDefaultEnvironmentVariables(names, resolvedNames);
	
		Iterator i = names.iterator();
		Iterator j = resolvedNames.iterator();
		
		while (i.hasNext()) {
			String name = (String) i.next();
			String resolvedName = (String) j.next();
			
			methodCodeFrame.getClassGen().addField(new FieldGen(
				Constants.ACC_PRIVATE | Constants.ACC_STATIC,
				Type.OBJECT,
				resolvedName,
				methodCodeFrame.getConstantPoolGen()
			).getField());
			
			iList.append(new ALOAD(defaultEnvironmentIndex));
			iList.append(new PUSH(methodCodeFrame.getConstantPoolGen(), name));
			iList.append(methodCodeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeEnvironment.getClassName(), "getValue", Type.OBJECT, new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
				
			iList.append(methodCodeFrame.getInstructionFactory().createPutStatic(
				methodCodeFrame.getClassGen().getClassName(), resolvedName, Type.OBJECT));
		}
		
		iList.append(new RETURN());
		
		setLineNumbersEqualIndices(iList, mg);
	
		mg.setMaxStack();
		mg.setMaxLocals();
		methodCodeFrame.getClassGen().addMethod(mg.getMethod());
		
		return true;
	}

	static protected void setLineNumbersEqualIndices(InstructionList iList, MethodGen mg) {
		int l = 1;
		
		iList.setPositions();
		
		Iterator k = iList.iterator();
		while (k.hasNext()) {
			InstructionHandle ih = (InstructionHandle) k.next();
			mg.addLineNumber(ih, l++ /*ih.getPosition()*/);
		}
	}
}
