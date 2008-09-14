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

import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.rdf.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class IndexInstruction implements IInstructionHandler {
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
	
	public static Object doIndex(Object base, Object index) throws AdenineException {
		if (base == null) {
			throw new AdenineException("Base is null");
		}

		if (base instanceof Collection) {
			Collection c = (Collection)base;
			
			if (index instanceof Number) {
				int i = ((Number)index).intValue();
				if (i < 0) {
					throw new SyntaxException("Index out of bounds: " + index);
				}
				Object[] o = c.toArray();
				if (i > (o.length - 1)) {
					throw new SyntaxException("Index out of bounds: " + index);
				}
				return o[i];
			}
		} else if (base instanceof Map) {
			Map m = (Map)base;
			return m.get(index);
		} else if (base.getClass().isArray()) {
			if (index instanceof Number) {
				int i = ((Number)index).intValue();
				if (i < 0) {
					throw new SyntaxException("Index out of bounds: " + index);
				}
				if (i > (Array.getLength(base) - 1)) {
					throw new SyntaxException("Index out of bounds: " + index);
				}
				return Array.get(base, i);
			}
		} else if (base instanceof ConditionSet) {
			ConditionSet cs = (ConditionSet)base;
			if (index instanceof Number) {
				int i = ((Number)index).intValue();
				if (i < 0) {
					throw new SyntaxException("Index out of bounds: " + index);
				}
				if (i > (cs.count() - 1)) {
					throw new SyntaxException("Index out of bounds: " + index);
				}
				return cs.get(i);
			}
		}
		
		throw new SyntaxException("Invalid index " + index + " on collection " + base);
	}

	/**
	 * @see InstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		Resource resBase = Utilities.getResourceProperty(res, AdenineConstants.base, m_source);
		Resource resIndex = Utilities.getResourceProperty(res, AdenineConstants.index, m_source);
		
		// Get base and index
		Object base = m_interpreter.runInstruction(resBase, env, denv);
		Object index = m_interpreter.runInstruction(resIndex, env, denv);
		return doIndex(base, index);		
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			IExpression m_base;
			IExpression m_index;
			IExpression init(Resource res2) throws AdenineException {
				Resource resBase = Utilities.getResourceProperty(res2, AdenineConstants.base, m_source);
				Resource resIndex = Utilities.getResourceProperty(res2, AdenineConstants.index, m_source);
				
				// Get base and index
				m_base = m_interpreter.compileInstruction(resBase);
				m_index = m_interpreter.compileInstruction(resIndex);
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				String index = Interpreter.generateIdentifier();
				buffer.append("Object ");
				buffer.append(index);
				buffer.append(";\n");
				m_index.generateJava(index, buffer, frame, ct);
				m_base.generateJava(targetVar, buffer, frame, ct);
				buffer.append(targetVar);
				buffer.append(" = IndexInstruction.doIndex(");
				buffer.append(targetVar);
				buffer.append(",");
				buffer.append(index);
				buffer.append(");");
			}			
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				// Get base and index
				Object base = m_base.evaluate(env, denv);
				Object index = m_index.evaluate(env, denv);

				return doIndex(base, index);
			}
		}.init(res);
	}

}
