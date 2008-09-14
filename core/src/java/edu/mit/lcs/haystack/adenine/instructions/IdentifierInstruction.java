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
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class IdentifierInstruction implements IInstructionHandler {
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
		String str = Utilities.getLiteralProperty(res, AdenineConstants.name, m_source);
		
		// TODO[dquan]: handle numbers separately
		try {
			if (str.indexOf('.') != -1) {
				return new Double(Double.parseDouble(str));
			} else {
				int i = Integer.parseInt(str);
				return new Integer(i);
			}
		} catch (Exception e) {
			// Not a number...
		}
		
		if (!env.isBound(str)) {
			if (!denv.isBound(str)) {
				throw new UnboundIdentifierException(str);
			} else {
				return denv.getValue(str);
			}
		}
		return env.getValue(str);
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			Integer m_integer = null;
			Double m_double = null;
			String m_str;
			IExpression init(Resource res2) {
				m_str = Utilities.getLiteralProperty(res2, AdenineConstants.name, m_source);
				
				// TODO[dquan]: handle numbers separately
				try {
					if (m_str.indexOf('.') != -1) {
						m_double = new Double(Double.parseDouble(m_str));
					} else {
						int i = Integer.parseInt(m_str);
						m_integer = new Integer(i);
					}
				} catch (Exception e) {
					// Not a number...
				}
				
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				buffer.append(targetVar);
				buffer.append(" = ");
				if (m_integer != null) {
					buffer.append(ct.getConstantName("new Integer(" + m_integer + ")"));
				} else if (m_double != null) {
						buffer.append(ct.getConstantName("new Double(" + m_double + ")"));
				} else {
					try {
						buffer.append(frame.resolveVariableName(m_str));
					} catch (UnboundIdentifierException uie) {
						// Assume it is a dynamic variable
						buffer.append("__dynamicenvironment__.getValueChecked(\"");
						buffer.append(m_str);
						buffer.append("\")");
					}
				}
				buffer.append(";\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				if (m_integer != null) {
					return m_integer;
				}
				
				if (m_double != null) {
					return m_double;
				}
				
				if (!env.isBound(m_str)) {
					if (!denv.isBound(m_str)) {
						throw new UnboundIdentifierException(m_str);
					} else {
						return denv.getValue(m_str);
					}
				}
				return env.getValue(m_str);
			}
		}.init(res);
	}

}
