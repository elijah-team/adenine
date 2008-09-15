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
import edu.mit.lcs.haystack.adenine.query.Condition;
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class QueryInstruction implements IInstructionHandler {
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
		ConditionSet cs = new ConditionSet();
		Iterator i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(res, AdenineConstants.queryConditions, m_source), m_source);
		while (i.hasNext()) {
			Resource resCondition = (Resource)i.next();
			Resource resFunction = null;
			ArrayList parameters = new ArrayList();
			Iterator i2 = ListUtilities.accessDAMLList(resCondition, m_interpreter.getRootRDFContainer());
			while (i2.hasNext()) {
				Object o = m_interpreter.runInstruction((Resource)i2.next(), env, denv);
				if (resFunction == null) {
					resFunction = (Resource)o;
				} else {
					parameters.add(o);
				}
			}
			cs.add(new Condition(resFunction, parameters));
		}
		return cs;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			ArrayList m_al = new ArrayList();
			int m_line = -1;
			IExpression init(Resource res2) throws AdenineException {
				String strLine = Utilities.getLiteralProperty(res2, AdenineConstants.line, m_source);
				if (strLine != null) {
					try {
						m_line = Integer.parseInt(strLine);
					} catch (NumberFormatException nfe) {
					}
				}
				Iterator i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(res2, AdenineConstants.queryConditions, m_source), m_source);
				while (i.hasNext()) {
					Resource resCondition = (Resource)i.next();
					IExpression function = null;
					ArrayList parameters = new ArrayList();
					Iterator i2 = ListUtilities.accessDAMLList(resCondition, m_interpreter.getRootRDFContainer());
					while (i2.hasNext()) {
						IExpression exp = m_interpreter.compileInstruction((Resource)i2.next());
						if (function == null) {
							function = exp;
						} else {
							parameters.add(exp);
						}
					}
					m_al.add(new Object[] { function, parameters });
				}
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				throw new UnsupportedOperationException("%{} not supported with precompilation");
/*				buffer.append(targetVar);
				buffer.append(" = new ConditionSet();\n");
				
				String ident0 = Interpreter.generateIdentifier();
				String ident1 = Interpreter.generateIdentifier();
				String ident2 = Interpreter.generateIdentifier();
				buffer.append("Object ");
				buffer.append(ident0);
				buffer.append(", ");
				buffer.append(ident1);
				buffer.append(", ");
				buffer.append(ident2);
				buffer.append(";\n");
				
				Iterator i = m_al.iterator();
				while (i.hasNext()) {
					IExpression[] pair = (IExpression[])i.next();
					pair[0].generateJava(ident0, buffer, frame);
					pair[1].generateJava(ident1, buffer, frame);
					pair[2].generateJava(ident2, buffer, frame);
					buffer.append("((IRDFContainer)");
					buffer.append(targetVar);
					buffer.append(").add(new Statement(");
					buffer.append("(Resource)ModelInstruction.extractNode(");
					buffer.append(ident0);
					buffer.append(",__dynamicenvironment__.getTarget()),\n(Resource)ModelInstruction.extractNode(");
					buffer.append(ident1);
					buffer.append(",__dynamicenvironment__.getTarget()),\nModelInstruction.extractNode(");
					buffer.append(ident2);
					buffer.append(",__dynamicenvironment__.getTarget())));");
				}
*/			}

			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				try {
					Iterator i = m_al.iterator();
					ConditionSet cs = new ConditionSet();
					while (i.hasNext()) {
						Object[] pair = (Object[])i.next();
						Resource function = (Resource)((IExpression)pair[0]).evaluate(env, denv);
						Iterator i2 = ((ArrayList)pair[1]).iterator();
						ArrayList parameters = new ArrayList();
						while (i2.hasNext()) {
							parameters.add(((IExpression)i2.next()).evaluate(env, denv));
						}
						cs.add(new Condition(function, parameters));
					}
					return cs;
				} catch (ClassCastException cce) {
					throw new AdenineException("Type error", cce, m_line);
				}
			}
		}.init(res);
	}

}
