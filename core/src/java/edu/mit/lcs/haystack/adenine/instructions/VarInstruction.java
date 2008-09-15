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
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class VarInstruction implements IInstructionHandler {
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
		try {
			Set s = m_source.query(new Statement[] {
				new Statement(res, AdenineConstants.var, Utilities.generateWildcardResource(2)),
				new Statement(Utilities.generateWildcardResource(2), Constants.s_rdf_type, AdenineConstants.Identifier),
				new Statement(Utilities.generateWildcardResource(2), AdenineConstants.name, Utilities.generateWildcardResource(1))
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
			
			Iterator i = s.iterator();
			while (i.hasNext()) {
				Literal l = (Literal)((RDFNode[])i.next())[0];
				env.allocateCell(l.getContent());
			}
		} catch (RDFException rdfe) {
			throw new AdenineException("RDF error", rdfe);
		}
		return null;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			ArrayList m_identifiers = new ArrayList();
			IExpression init(Resource res2) throws AdenineException {
				try {
					Set s = m_source.query(new Statement[] {
						new Statement(res2, AdenineConstants.var, Utilities.generateWildcardResource(2)),
						new Statement(Utilities.generateWildcardResource(2), Constants.s_rdf_type, AdenineConstants.Identifier),
						new Statement(Utilities.generateWildcardResource(2), AdenineConstants.name, Utilities.generateWildcardResource(1))
					}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
					
					Iterator i = s.iterator();
					while (i.hasNext()) {
						Literal l = (Literal)((RDFNode[])i.next())[0];
						m_identifiers.add(l.getContent());
					}
				} catch (RDFException rdfe) {
					throw new AdenineException("RDF error", rdfe);
				}
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				frame.m_variables.addAll(m_identifiers);
				buffer.append(targetVar);
				buffer.append(" = null;\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Iterator i = m_identifiers.iterator();
				while (i.hasNext()) {
					env.allocateCell((String)i.next());
				}
				return null;
			}
		}.init(res);
	}

}
