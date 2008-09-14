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
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Environment;
import edu.mit.lcs.haystack.adenine.interpreter.IExpression;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AssignmentInstruction implements IInstructionHandler {
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
		// TODO[dquan]: support other types of l-values
		Resource resLHS = Utilities.getResourceProperty(res, AdenineConstants.LHS, m_source);
		Resource resRHS = Utilities.getResourceProperty(res, AdenineConstants.RHS, m_source);
		if (Utilities.isType(resLHS, AdenineConstants.Identifier, m_source)) {
			Object o = m_interpreter.runInstruction(resRHS, env, denv);
			String var = Utilities.getLiteralProperty(resLHS, AdenineConstants.name, m_source);
			env.setValue(var, o);
			return o;
		} else {
			throw new SyntaxException("Invalid l-value: " + resLHS);
		}
	}
	
	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			String m_var;
			IExpression m_rhs;
			
			IExpression init(Resource res2) throws AdenineException {
				Resource resLHS = Utilities.getResourceProperty(res2, AdenineConstants.LHS, m_source);
				Resource resRHS = Utilities.getResourceProperty(res2, AdenineConstants.RHS, m_source);
				if (!Utilities.isType(resLHS, AdenineConstants.Identifier, m_source)) {
					throw new SyntaxException("Invalid l-value: " + resLHS);
				}
				m_rhs = m_interpreter.compileInstruction(resRHS);
				m_var = Utilities.getLiteralProperty(resLHS, AdenineConstants.name, m_source);
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				String temp = Interpreter.generateIdentifier();
				buffer.append("Object ");
				buffer.append(temp);
				buffer.append(";\n");
				m_rhs.generateJava(temp, buffer, frame, ct);
				buffer.append(frame.generateVariableName(m_var));
				buffer.append(" = ");
				buffer.append(temp);
				buffer.append(";\n");
				buffer.append(targetVar);
				buffer.append(" = null;\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Object o = m_rhs.evaluate(env, denv);
				env.setValue(m_var, o);
				return o;
			}
		}.init(res);
	}

}
