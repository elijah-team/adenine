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

import java.util.*;

import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ReturnInstruction implements IInstructionHandler {
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
		Vector v = new Vector();
		HashMap namedParameterMap = new HashMap();
		FunctionCallInstruction.getParameters(res, AdenineConstants.RETURN_PARAMETERS, AdenineConstants.NAMED_RETURN_PARAMETER, v, namedParameterMap, m_source, m_interpreter, env, denv);

		if (v.isEmpty()) {
			v.add(null);
		}
		
		throw new ReturnException(new Message(v.toArray(), namedParameterMap));
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			ArrayList m_parameters = new ArrayList();
			ArrayList m_namedParameters = new ArrayList();
			int m_line;

			IExpression init(Resource res2) throws AdenineException {
				m_line = Interpreter.getLineNumber(res2, m_source);

				// Get the parameters
				FunctionCallInstruction.compileParameters(res2, AdenineConstants.RETURN_PARAMETERS, AdenineConstants.NAMED_RETURN_PARAMETER, m_parameters, m_namedParameters, m_source, m_interpreter);

				return this;
			}
			
			public void generateJava(String target, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				String map = null;
				boolean hasNamedParameters = !m_namedParameters.isEmpty();
				if (hasNamedParameters) {
					map = Interpreter.generateIdentifier();
					String datum = Interpreter.generateIdentifier();
					buffer.append("HashMap ");
					buffer.append(map);
					buffer.append(" = new HashMap();\nObject ");
					buffer.append(datum);
					buffer.append(" = null;\n");
					
					Iterator i = m_namedParameters.iterator();
					HashMap namedParameters = new HashMap();
					while (i.hasNext()) {
						Object[] pair = (Object[]) i.next();
						((IExpression) pair[1]).generateJava(datum, buffer, frame, ct);
						buffer.append(map);
						buffer.append(".put(");
						buffer.append(ct.getConstantName("new Resource(\"" + Interpreter.escapeString(((Resource) pair[0]).getURI()) + "\")"));
						buffer.append(", ");
						buffer.append(datum);
						buffer.append(");\n");
					}
				} else if (m_parameters.size() == 0) {
					buffer.append("if (true) return new Message();\n");
					return;
				}

				String ident = Interpreter.generateIdentifier();
				buffer.append("Object[] ");
				buffer.append(ident);
				buffer.append(" = new Object[");
				buffer.append(m_parameters.size());
				buffer.append("];\n");
				
				Iterator i = m_parameters.iterator();
				int i2 = 0;
				while (i.hasNext()) {
					IExpression exp = (IExpression)i.next();
					exp.generateJava(ident + "[" + (i2++) + "]", buffer, frame, ct);
				}

				buffer.append("if (true) return new Message(");
				buffer.append(ident);
				if (hasNamedParameters) {
					buffer.append(", ");
					buffer.append(map);
				}
				buffer.append(");\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Vector v = new Vector();
				Iterator i = m_parameters.iterator();
				while (i.hasNext()) {
					v.add(((IExpression)i.next()).evaluate(env, denv));
				}
				
				i = m_namedParameters.iterator();
				HashMap namedParameters = new HashMap();
				while (i.hasNext()) {
					IExpression[] pair = (IExpression[])i.next();
					namedParameters.put(pair[0].evaluate(env, denv), pair[1].evaluate(env, denv));
				}
				
				if (v.isEmpty()) {
					v.add(null);
				}

				throw new ReturnException(new Message(v.toArray(), namedParameters));
			}
		}.init(res);
	}

}
