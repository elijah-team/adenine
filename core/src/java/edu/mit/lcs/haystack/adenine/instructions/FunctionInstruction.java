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
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class FunctionInstruction implements IInstructionHandler {
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
		Resource resFunction = Utilities.getResourceProperty(res, AdenineConstants.function, m_source);
		Resource resParameters = Utilities.getResourceProperty(res, AdenineConstants.PARAMETERS, m_source);
		Resource resBody = Utilities.getResourceProperty(res, AdenineConstants.body, m_source);
		
		// Get the function name
		String name = Utilities.getLiteralProperty(resFunction, AdenineConstants.name, m_source);
		
		// Create binding in environment so that function can call itself
		env.allocateCell(name);
		
		// Create closure
		Closure c = new Closure(env, resBody, resParameters, m_interpreter);
		
		// Bind to active environment
		env.setValue(name, c);
		
		return c;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			String m_name;
			Resource m_resParameters;
			IExpression m_body;
			Resource m_res;
			
			IExpression init(Resource res2) throws AdenineException {
				Resource resFunction = Utilities.getResourceProperty(res2, AdenineConstants.function, m_source);
				m_resParameters = Utilities.getResourceProperty(res2, AdenineConstants.PARAMETERS, m_source);
				Resource resBody = Utilities.getResourceProperty(res2, AdenineConstants.body, m_source);
				m_res = res2;
			
				m_body = m_interpreter.compileInstruction(resBody);
				
				// Get the function name
				m_name = Utilities.getLiteralProperty(resFunction, AdenineConstants.name, m_source);
				
				return this;
			}

			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame0, ConstantTable ct) throws AdenineException {
				frame0.m_variables.add(m_name);
				
				// Map named parameters
				Resource[] namedParameters = Utilities.getResourceProperties(m_res, AdenineConstants.namedParameter, m_source);
				if (namedParameters.length > 0) {
					throw new UnsupportedOperationException("Named parameters not supported with Adenine compilation.");
				}
				
				String denvvar = Interpreter.generateIdentifier();
				String ivar = Interpreter.generateIdentifier();
		
				// Map parameters
				buffer.append(frame0.resolveVariableName(m_name));
				buffer.append(" = new ICallable() {\n");
				buffer.append("public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {\n");
				buffer.append("DynamicEnvironment ");
				buffer.append(denvvar);
				buffer.append(" = __dynamicenvironment__;\n__dynamicenvironment__ = denv;\n");
				buffer.append("try {");
				buffer.append("int ");
				buffer.append(ivar);
				buffer.append(" = 0;\n");
				VariableFrame frame = new VariableFrame();
				frame.m_parentFrame = frame0;
				StringBuffer sb2 = new StringBuffer();
				Iterator i = ListUtilities.accessDAMLList(m_resParameters, m_source);
				while (i.hasNext()) {
					Resource resVarName = (Resource)i.next();
					String varName = Utilities.getLiteralProperty(resVarName, AdenineConstants.name, m_source);
					frame.m_variables.add(varName);
					buffer.append("if (");
					buffer.append(ivar);
					buffer.append(" < message.m_values.length) {\n");
					buffer.append(frame.resolveVariableName(varName));
					buffer.append(" = message.m_values[");
					buffer.append(ivar);
					buffer.append("++];\n}\n");
					sb2.append("Object ");
					sb2.append(frame.resolveVariableName(varName));
					sb2.append(";\n");
				}
		
				buffer.append("\n");
		
				StringBuffer sb3 = new StringBuffer();
				String out = Interpreter.generateIdentifier();
				VariableFrame vf2 = Interpreter.generateJavaBlock(out, m_body, sb3, frame, null, ct, false);
		
				buffer.append("Object ");
				buffer.append(out);
				buffer.append(";\n");
				buffer.append(sb3);
				buffer.append("__dynamicenvironment__ = ");
				buffer.append(denvvar);
				buffer.append(";\n");
				buffer.append("return new Message(");
				buffer.append(out);
				buffer.append(");\n} catch (AdenineException ae) { throw ae; } catch (Exception e) { throw new AdenineException(\"\", e); } }\n");
				buffer.append(sb2);
		
				// Generate variable declarations
				i = vf2.m_variables.iterator();
				while (i.hasNext()) {
					String ident2 = (String)i.next();
					buffer.append("Object ");
					buffer.append(vf2.resolveVariableName(ident2));
					buffer.append(" = null;\n");
				}
				buffer.append("};\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				// Create binding in environment so that function can call itself
				env.allocateCell(m_name);
				
				// Create closure
				Closure c = new Closure(env, m_body, m_resParameters, m_interpreter);
				
				// Bind to active environment
				env.setValue(m_name, c);
				
				return c;
			}
		}.init(res);
	}

}
