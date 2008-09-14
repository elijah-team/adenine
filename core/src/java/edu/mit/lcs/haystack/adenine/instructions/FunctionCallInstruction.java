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
import edu.mit.lcs.haystack.adenine.SyntaxException;
import edu.mit.lcs.haystack.adenine.functions.NewFunction;
import edu.mit.lcs.haystack.adenine.interpreter.ConstantTable;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Environment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.IExpression;
import edu.mit.lcs.haystack.adenine.interpreter.IInstructionHandler;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.adenine.interpreter.VariableFrame;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class FunctionCallInstruction implements IInstructionHandler {
	protected IRDFContainer m_source;
	protected Interpreter m_interpreter;

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
	
	static public void compileParameters(Resource res2, 
		Resource param, Resource namedParam,
		ArrayList m_parameters,
		ArrayList m_namedParameters, IRDFContainer m_source, 
		Interpreter m_interpreter) throws AdenineException {
		// Retrieve the parameters
		Resource resParameters = Utilities.getResourceProperty(res2, param, m_source);
		Iterator i = ListUtilities.accessDAMLList(resParameters, m_source);
		while (i.hasNext()) {
			m_parameters.add(m_interpreter.compileInstruction((Resource)i.next()));
		}
		
		// Retrieve the named parameters
		Resource[] namedParameters = Utilities.getResourceProperties(res2, namedParam, m_source);
		for (int j = 0; j < namedParameters.length; j++) {
			Resource lhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterName, m_source);
			Resource rhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterVariable, m_source);
			m_namedParameters.add(new Object[] {
				lhs,
				m_interpreter.compileInstruction(rhs)
			});
		}
	}

	static public void getParameters(Resource res, 
		Resource param, Resource namedParam, Vector v, 
		HashMap namedParameterMap, IRDFContainer source, 
		Interpreter interpreter, Environment env, DynamicEnvironment denv) 
		throws AdenineException {
		Resource resParameters = Utilities.getResourceProperty(res, param, source);

		// Retrieve the parameters
		Iterator i = ListUtilities.accessDAMLList(resParameters, source);
		while (i.hasNext()) {
			Object o2 = interpreter.runInstruction((Resource)i.next(), env, denv);
			/*if (o2 instanceof LocalRDFContainer) {
				try {
					System.out.println("rdf: " + Utilities.generateN3((LocalRDFContainer)o2));
				} catch (Exception e){ }
			}*/
			v.add(o2);
		}
		
		// Retrieve the named parameters
		Resource[] namedParameters = Utilities.getResourceProperties(res, namedParam, source);
		for (int j = 0; j < namedParameters.length; j++) {
			Resource lhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterName, source);
			Resource rhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterVariable, source);
			namedParameterMap.put(lhs,
				interpreter.runInstruction(rhs, env, denv));
		}
	}

	/**
	 * @see InstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		Resource resFunction = Utilities.getResourceProperty(res, AdenineConstants.function, m_source);

		// Get the function
		Object o = m_interpreter.runInstruction(resFunction, env, denv);
		if (!(o instanceof ICallable) && !(o instanceof Class) && !(o instanceof Resource)) {
			throw new SyntaxException("Object is not callable: " + o);
		}
		
		Vector v = new Vector();
		HashMap namedParameterMap = new HashMap();
		getParameters(res, AdenineConstants.PARAMETERS, AdenineConstants.namedParameter, v, namedParameterMap, m_source, m_interpreter, env, denv);
		
		if (o instanceof Class) {
			v.insertElementAt(o, 0);
			return new NewFunction().invoke(new Message(v.toArray()), denv).getPrimaryValue();
		}
		
		Object out;
		if (o instanceof Resource) {
			out = m_interpreter.callMethod((Resource)o, new Message(v.toArray(), namedParameterMap), denv).getPrimaryValue();
		} else {
			out = ((ICallable)o).invoke(new Message(v.toArray(), namedParameterMap), denv).getPrimaryValue();
		}
		
		//System.out.println("eval on " + v + " resulted in " + out);
		return out;
	}

	static public final Message doFunctionCall(Object o, Object[] parameters, Map namedParams, DynamicEnvironment denv, Interpreter interpreter, int line) throws AdenineException {
		try {
			if (o instanceof Class) {
				Object[] params2 = new Object[parameters.length + 1];
				System.arraycopy(parameters, 0, params2, 1, parameters.length);
				params2[0] = o;
				return new NewFunction().invoke(new Message(params2, namedParams), denv);
			} else if (o instanceof Resource) {
				return interpreter.callMethod((Resource)o, new Message(parameters, namedParams), denv);
			} else if (o instanceof ICallable) {
				return ((ICallable)o).invoke(new Message(parameters, namedParams), denv);
			} else {
				AdenineException ae = new AdenineException("Object not callable; parameters: " + java.util.Arrays.asList(parameters));
				ae.m_line = line;
				throw ae;
			}
		} catch (AdenineException ae) {
			ae.addToStackTrace("Function call", line);
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
			ArrayList m_parameters = new ArrayList();
			ArrayList m_namedParameters = new ArrayList();
			
			IExpression init(Resource res2) throws AdenineException {
				Resource resFunction = Utilities.getResourceProperty(res2, AdenineConstants.function, m_source);
				m_line = Interpreter.getLineNumber(res2, m_source);

				// Get the function
				m_function = m_interpreter.compileInstruction(resFunction);
				
				// Get the parameters
				compileParameters(res2, AdenineConstants.PARAMETERS, AdenineConstants.namedParameter, m_parameters, m_namedParameters, m_source, m_interpreter);
				
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				boolean hasNamedParameters = !m_namedParameters.isEmpty();
				
				String func = Interpreter.generateIdentifier();
				String vector = Interpreter.generateIdentifier();
				buffer.append("Object[] ");
				buffer.append(vector);
				buffer.append(" = new Object[");
				buffer.append(m_parameters.size());
				buffer.append("];\n");
				
				Iterator i = m_parameters.iterator();
				int i2 = 0;
				while (i.hasNext()) {
					IExpression exp = (IExpression)i.next();
					exp.generateJava(vector + "[" + (i2++) + "]", buffer, frame, ct);
				}
				
				String map = null;
				if (hasNamedParameters) {
					map = Interpreter.generateIdentifier();
					String datum = Interpreter.generateIdentifier();
					buffer.append("HashMap ");
					buffer.append(map);
					buffer.append(" = new HashMap();\nObject ");
					buffer.append(datum);
					buffer.append(" = null;\n");
					
					i = m_namedParameters.iterator();
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
				}
				
				// Can optimize if the function is a resource constant
				if (m_function instanceof ResourceInstruction.ResourceExpression) {
					buffer.append(targetVar);
					buffer.append(" = __interpreter__.callMethod((Resource)");
					buffer.append(ct.getConstantName("new Resource(\"" + Interpreter.escapeString(((ResourceInstruction.ResourceExpression)m_function).getResource().getURI()) + "\")"));
					buffer.append(", new Message(");
					buffer.append(vector);
					if (hasNamedParameters) {
						buffer.append(", ");
						buffer.append(map);
					}
					buffer.append("), __dynamicenvironment__).getPrimaryValue();\n");
				} else {
					buffer.append("Object ");
					buffer.append(func);
					buffer.append(";\n");
					m_function.generateJava(func, buffer, frame, ct);
	
					buffer.append(targetVar);
					buffer.append(" = FunctionCallInstruction.doFunctionCall(");
					buffer.append(func);
					buffer.append(", ");
					buffer.append(vector);
					if (hasNamedParameters) {
						buffer.append(", ");
						buffer.append(map);
					} else {
						buffer.append(", null");
					}
					buffer.append(", __dynamicenvironment__, __interpreter__, ");
					buffer.append(m_line);
					buffer.append(").getPrimaryValue();\n");
				}
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Object o = m_function.evaluate(env, denv);
				
				Object[] parameters = new Object[m_parameters.size()];
				Iterator i = m_parameters.iterator();
				int i2 = 0;
				while (i.hasNext()) {
					parameters[i2++] = ((IExpression)i.next()).evaluate(env, denv);
				}
				
				i = m_namedParameters.iterator();
				HashMap namedParameters = new HashMap();
				while (i.hasNext()) {
					Object[] pair = (Object[]) i.next();
					namedParameters.put(pair[0], ((IExpression) pair[1]).evaluate(env, denv));
				}

				try {
					return doFunctionCall(o, parameters, namedParameters, denv, m_interpreter, m_line).getPrimaryValue();
				} catch (AdenineException ae) {
					throw ae;
				} catch (Exception e) {
					throw new AdenineException("Function call error", e, m_line);
				}
			}
		}.init(res);
	}

}
