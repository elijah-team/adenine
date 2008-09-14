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
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ForInInstruction implements IInstructionHandler {
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

	static public final Iterator getIterator(Object o, DynamicEnvironment denv) {
		if (o.getClass().isArray()) {
			return Arrays.asList((Object[])o).iterator();
		}

		if (o instanceof Collection) {
			return ((Collection)o).iterator();
		}
		
		if (o instanceof IRDFContainer) {
			try {
				return ((IRDFContainer)o).iterator();
			} catch (RDFException rdfe) {
				rdfe.printStackTrace();
				return null;
			}
		}

		if (o instanceof Iterator) {
			return (Iterator)o;
		}

		if (o instanceof ConditionSet) {
			return ((ConditionSet)o).iterator();
		}
		
		if (o instanceof Resource) {
			return ListUtilities.accessDAMLList((Resource)o, denv.getSource());
		}
		
		return null;
	}

	/**
	 * @see InstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		Resource resVariable = Utilities.getResourceProperty(res, AdenineConstants.var, m_source);
		Resource resCollection = Utilities.getResourceProperty(res, AdenineConstants.COLLECTION, m_source);
		Resource resBody = Utilities.getResourceProperty(res, AdenineConstants.body, m_source);

		String var = Utilities.getLiteralProperty(resVariable, AdenineConstants.name, m_source);
		Object c = m_interpreter.runInstruction(resCollection, env, denv);
		Iterator i = getIterator(c, denv);
		while (i.hasNext()) {
			Environment env2 = (Environment)env.clone();
			env2.allocateCell(var);
			env2.setValue(var, i.next());
			try {
				m_interpreter.runInstruction(resBody, env2, denv);
			} catch (ContinueException ce) {
			} catch (BreakException be) {
				return null;
			}
		}
		
		return null;	
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			IExpression m_body;
			IExpression m_collection;
			String m_var;
			
			IExpression init(Resource res2) throws AdenineException {
				Resource resVariable = Utilities.getResourceProperty(res2, AdenineConstants.var, m_source);
				Resource resCollection = Utilities.getResourceProperty(res2, AdenineConstants.COLLECTION, m_source);
				Resource resBody = Utilities.getResourceProperty(res2, AdenineConstants.body, m_source);
		
				m_var = Utilities.getLiteralProperty(resVariable, AdenineConstants.name, m_source);
				m_body = m_interpreter.compileInstruction(resBody);
				m_collection = m_interpreter.compileInstruction(resCollection);				
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				m_collection.generateJava(targetVar, buffer, frame, ct);

				String ident = Interpreter.generateIdentifier();
				buffer.append("Iterator ");
				buffer.append(ident);
				buffer.append(" = ");
				buffer.append(ForInInstruction.class.getName());
				buffer.append(".getIterator(");
				buffer.append(targetVar);
				buffer.append(", __dynamicenvironment__);\nwhile (");
				buffer.append(ident);
				buffer.append(".hasNext()) {\n");

				StringBuffer sb = new StringBuffer();				
				VariableFrame vf = Interpreter.generateJavaBlock(null, m_body, sb, frame, m_var, ct, true);
				
				buffer.append("Object ");
				buffer.append(vf.resolveVariableName(m_var));
				buffer.append(" = ");
				buffer.append(ident);
				buffer.append(".next();\n");
				buffer.append(sb);
				buffer.append("}\n");
				buffer.append(targetVar);
				buffer.append(" = null;\n");
			}

			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Object c = m_collection.evaluate(env, denv);
				Iterator i = getIterator(c, denv);
				
				while (i.hasNext()) {
					Environment env2 = (Environment)env.clone();
					env2.allocateCell(m_var);
					env2.setValue(m_var, i.next());
					try {
						m_body.evaluate(env2, denv);
					} catch (ContinueException ce) {
					} catch (BreakException be) {
						return null;
					}
				}
				
				return null;	
			}
		}.init(res);
	}

}
