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
import edu.mit.lcs.haystack.adenine.interpreter.IExpression;
import edu.mit.lcs.haystack.adenine.interpreter.IInstructionHandler;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.VariableFrame;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class WithInstruction implements IInstructionHandler {
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
	 * @see IInstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		Resource resLHS = Utilities.getResourceProperty(res, AdenineConstants.LHS, m_source);
		Resource resRHS = Utilities.getResourceProperty(res, AdenineConstants.RHS, m_source);
		Resource resBody = Utilities.getResourceProperty(res, AdenineConstants.body, m_source);
		
		if (Utilities.isType(resLHS, AdenineConstants.Identifier, m_source)) {
			Object old = null;
			boolean wasBound = false;
			String var = null;
			try {
				Object o = m_interpreter.runInstruction(resRHS, env, denv);
				var = Utilities.getLiteralProperty(resLHS, AdenineConstants.name, m_source);
				if (denv.isBound(var)) {
					old = denv.getValue(var);
					wasBound = true;
				}
				denv.setValue(var, o);
				Environment env2 = (Environment)env.clone();
				return m_interpreter.runInstruction(resBody, env2, denv);
			} catch (AdenineException ae) {
				ae.addToStackTrace("with", Interpreter.getLineNumber(res, m_source));
				throw ae;
			} catch (NullPointerException npe) {
				throw new AdenineException("Null pointer exception", npe, Interpreter.getLineNumber(res, m_source));
			} finally {
				if (var != null) {
					if (wasBound) {
						denv.setValue(var, old);
					} else {
						denv.unbind(var);
					}
				}
			}
		} else {
			throw new AdenineException("Invalid l-value in with statement", Interpreter.getLineNumber(res, m_source));
		}
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			IExpression m_body;
			IExpression m_rhs;
			String m_var;
			int m_line;
			
			IExpression init(Resource res2) throws AdenineException {
				Resource resBody = Utilities.getResourceProperty(res2, AdenineConstants.body, m_source);
		
				m_body = m_interpreter.compileInstruction(resBody);
				m_line = Interpreter.getLineNumber(res2, m_source);

				Resource resLHS = Utilities.getResourceProperty(res2, AdenineConstants.LHS, m_source);
				Resource resRHS = Utilities.getResourceProperty(res2, AdenineConstants.RHS, m_source);
				
				if (Utilities.isType(resLHS, AdenineConstants.Identifier, m_source)) {
					m_rhs = m_interpreter.compileInstruction(resRHS);
				} else {
					throw new AdenineException("Invalid l-value in with statement", m_line);
				}
				m_var = Utilities.getLiteralProperty(resLHS, AdenineConstants.name, m_source);
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				String ident = Interpreter.generateIdentifier();
				String ident2 = Interpreter.generateIdentifier();
				String ident3 = Interpreter.generateIdentifier();
				buffer.append("boolean ");
				buffer.append(ident);
				buffer.append(" = __dynamicenvironment__.isBound(\"");
				buffer.append(m_var);
				buffer.append("\");\n");
				buffer.append("Object ");
				buffer.append(ident2);
				buffer.append(" = ");
				buffer.append(ident);
				buffer.append(" ? __dynamicenvironment__.getValue(\"");
				buffer.append(m_var);
				buffer.append("\") : null;\n");
				buffer.append("Object ");
				buffer.append(ident3);
				buffer.append(" = null;\n");
				m_rhs.generateJava(ident3, buffer, frame, ct);
				buffer.append("__dynamicenvironment__.setValue(\"");
				buffer.append(m_var);
				buffer.append("\", ");
				buffer.append(ident3);
				buffer.append(");\n");
				buffer.append("try {\n");
				Interpreter.generateJavaBlock(m_body, buffer, frame, ct);
				buffer.append("} finally {\n");
				buffer.append("if (");
				buffer.append(ident);
				buffer.append(") {\n");
				buffer.append("__dynamicenvironment__.setValue(\"");
				buffer.append(m_var);
				buffer.append("\", ");
				buffer.append(ident2);
				buffer.append(");\n");
				buffer.append("}\n");
				buffer.append("}\n");
				buffer.append(targetVar);
				buffer.append(" = null;\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Object old = null;
				boolean wasBound = false;
				try {
					Object o = m_rhs.evaluate(env, denv);
					if (denv.isBound(m_var)) {
						old = denv.getValue(m_var);
						wasBound = true;
					}
					denv.setValue(m_var, o);
					
					return m_body.evaluate(env, denv);
				} catch (AdenineException ae) {
					ae.addToStackTrace("with", m_line);
					throw ae;
				} catch (NullPointerException npe) {
					throw new AdenineException("Null pointer exception", npe, m_line);
				} finally {
					if (wasBound) {
						denv.setValue(m_var, old);
					} else {
						denv.unbind(m_var);
					}
				}
			}
		}.init(res);
	}

}
