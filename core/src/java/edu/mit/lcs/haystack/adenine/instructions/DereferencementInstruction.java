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

package edu.mit.lcs.haystack.adenine.instructions;

import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.ConstantTable;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Environment;
import edu.mit.lcs.haystack.adenine.interpreter.IDereferenceable;
import edu.mit.lcs.haystack.adenine.interpreter.IExpression;
import edu.mit.lcs.haystack.adenine.interpreter.IInstructionHandler;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.InvalidMemberException;
import edu.mit.lcs.haystack.adenine.interpreter.JavaMethodWrapper;
import edu.mit.lcs.haystack.adenine.interpreter.VariableFrame;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class DereferencementInstruction implements IInstructionHandler {
	IRDFContainer m_source;
	Interpreter m_interpreter;

	/**
	 * @see IInstructionHandler#initialize(Interpreter)
	 */
	public void initialize(Interpreter interpreter) {
		m_interpreter = interpreter;
		m_source = interpreter.getRootRDFContainer();
	}

	/**
	 * @see IInstructionHandler#isConstantExpression()
	 */
	public boolean isConstantExpression() {
		return false;
	}

	/**
	 * @see InstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		Resource resBase = Utilities.getResourceProperty(res, AdenineConstants.base, m_source);
		Resource resMember = Utilities.getResourceProperty(res, AdenineConstants.member, m_source);
		
		// Verify member is an identifier
		Object member;
		if (Utilities.isType(resMember, AdenineConstants.Identifier, m_source)) {
			member = Utilities.getLiteralProperty(resMember, AdenineConstants.name, m_source);
		} else if (Utilities.isType(resMember, AdenineConstants.Resource, m_source)) {
			member = Utilities.getResourceProperty(resMember, AdenineConstants.resource, m_source);
		} else {
			throw new InvalidMemberException(resMember);
		}
		
		// Get base
		Object base = m_interpreter.runInstruction(resBase, env, denv);
		return doDereferencement(base, member);		
	}

	static public final Object doDereferencement(Object base, Object member) throws AdenineException {
		if (base == null) {
			throw new AdenineException("Attempt to dereference " + member + " from null");
		}
		
		if (base instanceof IDereferenceable) {
			return ((IDereferenceable)base).getMember(member);
		} else {
			// Find the member
			if (base instanceof Class) {
				Object o = findMember((Class)base, (String)member, base);
				if (o != null) {
					return o;
				}
			}
			return findMember(base.getClass(), (String)member, base);
		}
	}
	
/*	static public final Object doDereferencement(Object base, Resource member) throws AdenineException {
		return doDereferencement(base, 
			Interpreter.escapeString(member.getURI()));
	}
*/
	static final Object findMember(Class c, String member, Object base) throws AdenineException {
		Method[] m = c.getMethods();
		ArrayList methods = new ArrayList();
		for (int i = 0; i < m.length; i++) {
			if (m[i].getName().equals(member)) {
				methods.add(m[i]);
			}
		}
		if (methods.size() > 0) {
			return new JavaMethodWrapper(base, member, methods);
		}
		
		Field[] f = c.getFields();
		for (int i = 0; i < f.length; i++) {
			if (f[i].getName().equals(member)) {
				try {
					return f[i].get(base);
				} catch (IllegalAccessException iae) {
					throw new AdenineException("Cannot access field " + member, iae);
				}
			}
		}
		
		return null;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			Object m_member;
			IExpression m_base;
			boolean m_identifierBase;
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				String ident = Interpreter.generateIdentifier();
				buffer.append("Object ");
				buffer.append(ident);
				buffer.append(";\n");
				
				m_base.generateJava(ident, buffer, frame, ct);
				buffer.append(targetVar);
				buffer.append(" = DereferencementInstruction.doDereferencement(");
				buffer.append(ident);
				if (m_identifierBase) {
					buffer.append(",\"");
					buffer.append(m_member);
					buffer.append("\");\n");
				} else {
					buffer.append(",new Resource(\"");
					buffer.append(Interpreter.escapeString(((Resource) m_member).getURI()));
					buffer.append("\"));\n");
				}
			}			
			
			IExpression init(Resource res2) throws AdenineException {
				Resource resBase = Utilities.getResourceProperty(res2, AdenineConstants.base, m_source);
				Resource resMember = Utilities.getResourceProperty(res2, AdenineConstants.member, m_source);
				
				// Verify member is an identifier
				if (Utilities.isType(resMember, AdenineConstants.Identifier, m_source)) {
					m_member = Utilities.getLiteralProperty(resMember, AdenineConstants.name, m_source);
					m_identifierBase = true;
				} else if (Utilities.isType(resMember, AdenineConstants.Resource, m_source)) {
					m_member = Utilities.getResourceProperty(resMember, AdenineConstants.resource, m_source);
					m_identifierBase = false;
				} else {
					throw new InvalidMemberException(resMember);
				}
				
				// Get base
				m_base = m_interpreter.compileInstruction(resBase);
				
				return this;
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Object base = m_base.evaluate(env, denv);
				return doDereferencement(base, m_member);				
			}
		}.init(res);
	}

}
