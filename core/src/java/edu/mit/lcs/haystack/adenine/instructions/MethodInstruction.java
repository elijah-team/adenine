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
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class MethodInstruction implements IInstructionHandler {

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
	 * @see InstructionHandler#evaluate(Resource, Environment, DynamicEnvironment)
	 */
	public Object evaluate(Resource res, Environment env, DynamicEnvironment denv)
		throws AdenineException {
		Resource resStart = Utilities.getResourceProperty(res, AdenineConstants.start, m_source);
		Resource resParameters = Utilities.getResourceProperty(res, AdenineConstants.PARAMETERS, m_source);
		Resource name = Utilities.getResourceProperty(res, AdenineConstants.name, m_source);
		Resource resName;
		if (name == null) {
			resName = Utilities.generateUniqueResource();
		} else {
			resName = (Resource)m_interpreter.runInstruction(name, env, denv);
		}
		
		try {
			// Duplicate the parameter list
			IRDFContainer target = denv.getTarget();
			resParameters = duplicateParameterList(resParameters, m_source, target);
			
			target.add(new Statement(resName, Constants.s_rdf_type, AdenineConstants.Method));
			target.add(new Statement(resName, AdenineConstants.start, (Resource)m_interpreter.runInstruction(resStart, env, denv)));
			target.add(new Statement(resName, AdenineConstants.PARAMETERS, resParameters));
			target.add(new Statement(resName, AdenineConstants.compileTime, new Literal(new Date().toString())));
			
			Resource[] namedParameters = Utilities.getResourceProperties(res, AdenineConstants.namedParameter, m_source);
			for (int j = 0; j < namedParameters.length; j++) {
				target.add(new Statement(resName, AdenineConstants.namedParameter, namedParameters[j]));
			} 
		} catch (RDFException rdfe) {
			throw new AdenineException("RDF error", rdfe);
		}
		
		RDFNode varName = Utilities.getIndirectProperty(res, AdenineConstants.var, AdenineConstants.name, m_source);
		if ((varName != null) && (varName instanceof Literal)) {
			env.setValue(varName.getContent(), resName);
		}
		
		return resName;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			IExpression m_name;
			IExpression m_start;
			String m_varName;
			Resource m_resParameters;
			Resource[] m_namedParameters;
			IExpression init(Resource res2) throws AdenineException {
				m_resParameters = Utilities.getResourceProperty(res2, AdenineConstants.PARAMETERS, m_source);
				Resource resStart = Utilities.getResourceProperty(res2, AdenineConstants.start, m_source);
				m_namedParameters = Utilities.getResourceProperties(res2, AdenineConstants.namedParameter, m_source);
				Resource name = Utilities.getResourceProperty(res2, AdenineConstants.name, m_source);
				if (name == null) {
					m_name = null;
				} else {
					m_name = m_interpreter.compileInstruction(name);
				}
				m_start = m_interpreter.compileInstruction(resStart);
				RDFNode varName = Utilities.getIndirectProperty(res2, AdenineConstants.var, AdenineConstants.name, m_source);
				if ((varName != null) && (varName instanceof Literal)) {
					m_varName = varName.getContent();
				} else {
					m_varName = null;
				}
				
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				if (m_name != null) {
					m_name.generateJava(targetVar, buffer, frame, ct);
				} else {
					buffer.append(targetVar);
					buffer.append(" = ");
					buffer.append("Utilities.generateUniqueResource();\n");
				}
				
				String ident0 = Interpreter.generateIdentifier();
				String ident1 = Interpreter.generateIdentifier();
				String ident2 = Interpreter.generateIdentifier();
				buffer.append("Object ");
				buffer.append(ident0);
				buffer.append(";\n");
				
				buffer.append("__dynamicenvironment__.getTarget().add(new Statement((Resource)");
				buffer.append(targetVar);
				buffer.append(", Constants.s_rdf_type, AdenineConstants.Method));\n");
				
				m_start.generateJava(ident0, buffer, frame, ct);
				buffer.append("__dynamicenvironment__.getTarget().add(new Statement((Resource)");
				buffer.append(targetVar);
				buffer.append(", AdenineConstants.start, (Resource) ");
				buffer.append(ident0);
				buffer.append("));\n");
				
				buffer.append("__dynamicenvironment__.getTarget().add(new Statement((Resource)");
				buffer.append(targetVar);
				buffer.append(", AdenineConstants.compileTime, new Literal(new Date().toString())));\n");
				
				buffer.append("Resource ");
				buffer.append(ident2);
				buffer.append(";\nArrayList ");
				buffer.append(ident1);
				buffer.append(" = new ArrayList();\n");
				
				Iterator i = ListUtilities.accessDAMLList(m_resParameters, m_source);
				while (i.hasNext()) {
					Resource param = (Resource)i.next();
					buffer.append(ident2);
					buffer.append(" = Utilities.generateUniqueResource();\n");
					buffer.append("__dynamicenvironment__.getTarget().add(new Statement(");
					buffer.append(ident2);
					buffer.append(", Constants.s_rdf_type, AdenineConstants.Identifier));\n");
					buffer.append("__dynamicenvironment__.getTarget().add(new Statement(");
					buffer.append(ident2);
					buffer.append(", AdenineConstants.name, (Literal)");
					buffer.append(ct.getConstantName("new Literal(\"" + Interpreter.escapeString(Utilities.getLiteralProperty(param, AdenineConstants.name, m_source)) + "\")"));
					buffer.append("));\n");
					buffer.append(ident1);
					buffer.append(".add(");
					buffer.append(ident2);
					buffer.append(");\n");
				}

				buffer.append("__dynamicenvironment__.getTarget().add(new Statement((Resource)");
				buffer.append(targetVar);
				buffer.append(", AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(");
				buffer.append(ident1);
				buffer.append(".iterator(), __dynamicenvironment__.getTarget())));\n");
				
				for (int j = 0; j < m_namedParameters.length; j++) {
					buffer.append("__dynamicenvironment__.getTarget().add(new Statement((Resource)");
					buffer.append(targetVar);
					buffer.append(", AdenineConstants.namedParameter, "); 
					buffer.append(ct.getConstantName("new Resource(\"" + Interpreter.escapeString(m_namedParameters[j].getURI()) + "\")"));
					buffer.append("));\n");
				}

				if (m_varName != null) {
					buffer.append(frame.generateVariableName(m_varName));
					buffer.append(" = ");
					buffer.append(targetVar);
					buffer.append(";\n");
				}
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Resource resName = m_name == null ? Utilities.generateUniqueResource() : (Resource)m_name.evaluate(env, denv);
				IRDFContainer target = denv.getTarget();
				try {
					target.add(new Statement(resName, Constants.s_rdf_type, AdenineConstants.Method));
					target.add(new Statement(resName, AdenineConstants.start, (Resource)m_start.evaluate(env, denv)));
					target.add(new Statement(resName, AdenineConstants.PARAMETERS, duplicateParameterList(m_resParameters, m_source, denv.getTarget())));
					target.add(new Statement(resName, AdenineConstants.compileTime, new Literal(new Date().toString())));
					for (int j = 0; j < m_namedParameters.length; j++) {
						target.add(new Statement(resName, AdenineConstants.namedParameter, m_namedParameters[j]));
					}
				} catch (RDFException rdfe) {
					throw new AdenineException("RDF error", rdfe);
				}
				
				if (m_varName != null) {
					env.setValue(m_varName, resName);
				}
				
				return resName;
			}
		}.init(res);
	}

}
