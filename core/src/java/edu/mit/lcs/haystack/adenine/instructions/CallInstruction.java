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
import edu.mit.lcs.haystack.adenine.functions.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class CallInstruction implements IInstructionHandler {
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
	
	static public Object mapMessageToIdentifiers(Message msg,
		Vector v, HashMap map, Environment env) {
		int c = v.size();
		for (int i = 0; i < c; i++) {
			if (i < msg.m_values.length) {
				env.setValue((String)v.elementAt(i), msg.m_values[i]);
			} else {
				env.setValue((String)v.elementAt(i), null);
			}
		}
		
		Iterator j = map.keySet().iterator();
		while (j.hasNext()) {
			Resource res = (Resource)j.next();
			String name = (String)map.get(res);
			if (msg.m_namedValues != null && res != null && msg.m_namedValues.containsKey(res)) {
				env.setValue(name, msg.m_namedValues.get(res));
			} else {
				env.setValue(name, null);
			}
		}
		
		return c == 0 ? null : v.elementAt(0);
	}
	
	static public void getReturnParameters(Resource res, Vector v, 
		HashMap namedParameterMap, IRDFContainer source, 
		Interpreter interpreter, Environment env, DynamicEnvironment denv) 
		throws AdenineException {
		Resource resParameters = Utilities.getResourceProperty(res, AdenineConstants.RETURN_PARAMETERS, source);

		// Retrieve the parameters
		Iterator i = ListUtilities.accessDAMLList(resParameters, source);
		while (i.hasNext()) {
			String name = Utilities.getLiteralProperty((Resource)i.next(), AdenineConstants.name, source);
			v.add(name);
		}
		
		// Retrieve the named parameters
		Resource[] namedParameters = Utilities.getResourceProperties(res, AdenineConstants.NAMED_RETURN_PARAMETER, source);
		for (int j = 0; j < namedParameters.length; j++) {
			Resource lhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterName, source);
			Resource rhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterVariable, source);
			namedParameterMap.put(lhs, Utilities.getLiteralProperty(rhs, AdenineConstants.name, source));
		}
	}

	/**
	 * @see InstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
			
		Vector v2 = new Vector();
		HashMap namedParameterMap2 = new HashMap();
		getReturnParameters(res, v2, namedParameterMap2, m_source, m_interpreter, env, denv);

		return mapMessageToIdentifiers(internalEvaluate(res, env, denv), v2, namedParameterMap2, env);
	}
	
	protected Message internalEvaluate(Resource res, Environment env, DynamicEnvironment denv) 
		throws AdenineException {
		Resource resFunction = Utilities.getResourceProperty(res, AdenineConstants.function, m_source);

		// Get the function
		Object o = m_interpreter.runInstruction(resFunction, env, denv);
		if (!(o instanceof ICallable) && !(o instanceof Class) && !(o instanceof Resource)) {
			throw new SyntaxException("Object is not callable: " + o);
		}
		
		Vector v = new Vector();
		HashMap namedParameterMap = new HashMap();
		FunctionCallInstruction.getParameters(res, AdenineConstants.PARAMETERS, AdenineConstants.namedParameter, v, namedParameterMap, m_source, m_interpreter, env, denv);
		
		
		if (o instanceof Class) {
			v.insertElementAt(o, 0);
			Message msg = new NewFunction().invoke(new Message(v.toArray()), denv);
			return msg;
		}
		
		Message out;
		if (o instanceof Resource) {
			out = m_interpreter.callMethod((Resource)o, new Message(v.toArray(), namedParameterMap), denv);
		} else {
			out = ((ICallable)o).invoke(new Message(v.toArray()), denv);
		}
		
		return out;
	}

	static public Object doFunctionCall(Object o, Vector v, Map namedParams, Vector v2, HashMap namedParams2, DynamicEnvironment denv, Interpreter interpreter, int line, Environment env) throws AdenineException {
		try {
			if (o instanceof Class) {
				v.insertElementAt(o, 0);
				Message msg = new NewFunction().invoke(new Message(v.toArray(), namedParams), denv);
				return msg.getPrimaryValue();
			} else if (o instanceof Resource) {
				Message msg = interpreter.callMethod((Resource)o, new Message(v.toArray(), namedParams), denv);
				return mapMessageToIdentifiers(msg, v2, namedParams2, env);
			} else if (o instanceof ICallable) {
				Message msg = ((ICallable)o).invoke(new Message(v.toArray(), namedParams), denv);
				return mapMessageToIdentifiers(msg, v2, namedParams2, env);
			} else {
				AdenineException ae = new AdenineException("Object not callable");
				ae.m_line = line;
				throw ae;
			}
		} catch (AdenineException ae) {
			ae.addToStackTrace("[function call]", line);
			throw ae;
		}
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			IExpression m_function;
			int m_line;
			Resource m_res;
			ArrayList m_parameters = new ArrayList();
			ArrayList m_namedParameters = new ArrayList();
			
			IExpression init(Resource res2) throws AdenineException {
				m_res = res2;
				Resource resFunction = Utilities.getResourceProperty(res2, AdenineConstants.function, m_source);
				m_line = Interpreter.getLineNumber(res2, m_source);

				// Get the function
				m_function = m_interpreter.compileInstruction(resFunction);
				
				// Get the parameters
				FunctionCallInstruction.compileParameters(res2, AdenineConstants.PARAMETERS, AdenineConstants.namedParameter, m_parameters, m_namedParameters, m_source, m_interpreter);
				
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				throw new UnsupportedOperationException();
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Object o = m_function.evaluate(env, denv);
				
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

				Vector returnParameters = new Vector();
				HashMap namedReturnParameters = new HashMap();
					
				// Get the return parameters
				getReturnParameters(m_res, returnParameters, namedReturnParameters, m_source, m_interpreter, env, denv);

				try {
					return doFunctionCall(o, v, namedParameters, returnParameters, namedReturnParameters, denv, m_interpreter, m_line, env);
				} catch (Exception e) {
					throw new AdenineException("Function call error", e, m_line);
				}
			}
		}.init(res);
	}

}
