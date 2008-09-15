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

import edu.mit.lcs.haystack.Constants;
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
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class Method2Instruction implements IInstructionHandler {

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

	static Resource duplicateParameterList(Resource list, IRDFContainer source, IRDFContainer target) throws RDFException {
		ArrayList newParams = new ArrayList();
		Iterator i = ListUtilities.accessDAMLList(list, source);
		while (i.hasNext()) {
			Resource param = (Resource)i.next();
			Resource newParam = Utilities.generateUniqueResource();
			target.add(new Statement(newParam, Constants.s_rdf_type, AdenineConstants.Identifier));
			target.add(new Statement(newParam, AdenineConstants.name, Utilities.getProperty(param, AdenineConstants.name, source)));
			newParams.add(newParam);
		}
		
		return ListUtilities.createDAMLList(newParams.iterator(), target);
	}

	/**
	 * @see IInstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource instruction, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		
		IExpression e = generateExpression(instruction);
		return e.evaluate(env, denv);
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			Resource	m_instruction;
			Literal		m_varName;
			List		m_parameters;
			List		m_backquotedParameters;
			Resource	m_wrappedMethod;

			IExpression init(Resource instruction) throws AdenineException {
				m_instruction = instruction;
				
				m_wrappedMethod = Utilities.getResourceProperty(m_instruction, AdenineConstants.function, m_source);
				
				Resource	resParameters = Utilities.getResourceProperty(m_wrappedMethod, AdenineConstants.PARAMETERS, m_source);
				Resource	resBackquotedParameters = Utilities.getResourceProperty(m_instruction, AdenineConstants.BACKQUOTED_PARAMETERS, m_source);

				Iterator	iParameters = ListUtilities.accessDAMLList(resParameters, m_source);
				Iterator	iBackquotedParameters = ListUtilities.accessDAMLList(resBackquotedParameters, m_source);

				m_parameters = new ArrayList();
				while (iParameters.hasNext()) {
					m_parameters.add(iParameters.next());
				}

				m_backquotedParameters = new ArrayList();
				while (iBackquotedParameters.hasNext()) {
					m_backquotedParameters.add(iBackquotedParameters.next());
				}
				
				m_varName = (Literal) Utilities.getIndirectProperty(m_instruction, AdenineConstants.var, AdenineConstants.name, m_source);
				
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Resource	method = m_wrappedMethod;
				Iterator	iParameters = m_parameters.iterator();
				Iterator	iBackquotedParameters = m_backquotedParameters.iterator();

				if (iBackquotedParameters.hasNext()) {
					try {
						IRDFContainer target = denv.getTarget();
				
						LinkedList arguments = new LinkedList();
						LinkedList parameters = new LinkedList();
				
						while (iBackquotedParameters.hasNext()) {
							Literal parameter = (Literal) iBackquotedParameters.next();
							iParameters.next();
					
							Object value = env.getValue(parameter.getContent());

							if (value instanceof Resource) {
								Resource r = Utilities.generateUniqueResource();
						
								target.add(new Statement(r, Constants.s_rdf_type, AdenineConstants.Resource));
								target.add(new Statement(r, AdenineConstants.resource, (Resource) value));
						
								arguments.add(r);
							} else if (value instanceof Literal) {
								Resource r = Utilities.generateUniqueResource();
						
								target.add(new Statement(r, Constants.s_rdf_type, AdenineConstants.Literal));
								target.add(new Statement(r, AdenineConstants.literal, (Literal) value));
						
								arguments.add(r);
							} else if (value instanceof String) {
								Resource r = Utilities.generateUniqueResource();
						
								target.add(new Statement(r, Constants.s_rdf_type, AdenineConstants.Literal));
								target.add(new Statement(r, AdenineConstants.literal, new Literal((String) value)));
						
								arguments.add(r);
							} else if (value instanceof Integer || value instanceof Float) {
								Resource r = Utilities.generateUniqueResource();
						
								target.add(new Statement(r, Constants.s_rdf_type, AdenineConstants.Identifier));
								target.add(new Statement(r, AdenineConstants.name, new Literal(value.toString())));
						
								arguments.add(r);
							} else {
								Resource r = Utilities.generateUniqueResource();
						
								target.add(new Statement(r, Constants.s_rdf_type, AdenineConstants.Literal));
								target.add(new Statement(r, AdenineConstants.literal, new Literal(value.toString())));
						
								arguments.add(r);
							}
						}
						while (iParameters.hasNext()) {
							Resource parameter = (Resource) iParameters.next();
					
							parameters.add(parameter);
							arguments.add(parameter);
						}
				
				
						method = Utilities.generateUniqueResource();
						Resource callReturnInstruction = Utilities.generateUniqueResource();
				
						target.add(new Statement(callReturnInstruction, Constants.s_rdf_type, AdenineConstants.CallReturn));
						{
							Resource resourceInstruction = Utilities.generateUniqueResource();
					
							target.add(new Statement(resourceInstruction, Constants.s_rdf_type, AdenineConstants.Resource));
							target.add(new Statement(resourceInstruction, AdenineConstants.resource, m_wrappedMethod));
					
							target.add(new Statement(callReturnInstruction, AdenineConstants.function, resourceInstruction));
						}
				
						target.add(new Statement(method, Constants.s_rdf_type, AdenineConstants.Method));
						target.add(new Statement(method, AdenineConstants.start, callReturnInstruction));
				
						Resource[] namedParameters = Utilities.getResourceProperties(m_wrappedMethod, AdenineConstants.namedParameter, m_source);
						for (int j = 0; j < namedParameters.length; j++) {
							Resource namedParameter = namedParameters[j];
					
							target.add(new Statement(method, AdenineConstants.namedParameter, namedParameter));
							target.add(new Statement(callReturnInstruction, AdenineConstants.namedParameter, namedParameter));
						}
				
						target.add(new Statement(method, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(parameters.iterator(), m_source)));
						target.add(new Statement(callReturnInstruction, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(arguments.iterator(), m_source)));
					} catch (RDFException rdfe) {
						throw new AdenineException("RDF error", rdfe);
					}
				}
				
				if (m_varName != null) {
					env.setValue(m_varName.getContent(), method);
				}
		
				return method;
			}
		}.init(res);
	}

}
