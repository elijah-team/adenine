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
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ImportJavaInstruction implements IInstructionHandler {
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
		try {
			String packageName = Utilities.getLiteralProperty(res, AdenineConstants.PACKAGE, m_source);
			Set s = m_source.query(new Statement[] {
				new Statement(res, AdenineConstants.name, Utilities.generateWildcardResource(1))
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
			
			Iterator i = s.iterator();
			while (i.hasNext()) {
				Literal l = (Literal)((RDFNode[])i.next())[0];
				String className = packageName + "." + l.getContent().replace('-', '$');
				Class c = CoreLoader.loadClass(className);
				if (c == null)
					throw new AdenineException("Could not load class " + className);
				env.setValue(l.getContent(), c);
			}
		} catch (RDFException rdfe) {
			throw new AdenineException("Unknown error", rdfe);
		}
		return null;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			Resource m_res;
			String m_packageName;
			ArrayList m_classes = new ArrayList();
			IExpression init(Resource res2) throws AdenineException {
				m_res = res2;
				m_packageName = Utilities.getLiteralProperty(res2, AdenineConstants.PACKAGE, m_source);
				Set s;
				try {
					s = m_source.query(new Statement[] {
						new Statement(
							res2,
							AdenineConstants.name,
							Utilities.generateWildcardResource(1)),
						},
						Utilities.generateWildcardResourceArray(1),
						Utilities.generateWildcardResourceArray(1));
				} catch (RDFException e) {
					throw new AdenineException("Unknown error", e);
				}
				
				Iterator i = s.iterator();
				while (i.hasNext()) {
					Literal l = (Literal)((RDFNode[])i.next())[0];
					m_classes.add(l.getContent());
				}
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				frame.m_variables.addAll(m_classes);
				buffer.append(targetVar);
				buffer.append(" = null;\n");
				buffer.append("try {\n");
				Iterator i = m_classes.iterator();
				while (i.hasNext()) {
					String s = (String)i.next();
					buffer.append(frame.generateVariableName(s));
					buffer.append(" = CoreLoader.loadClass(\"");
					buffer.append(m_packageName);
					buffer.append(".");
					buffer.append(s);
					buffer.append("\");\n");
				}
				buffer.append("} catch (ClassNotFoundException cnfe) {\n");
				buffer.append("throw new AdenineException(\"\", cnfe);\n");
				buffer.append("}\n");
			}
			
			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				Iterator i = m_classes.iterator();
				while (i.hasNext()) {
					String s = (String)i.next();
					String className = m_packageName + "." + s;
					Class c = CoreLoader.loadClass(className);
					if (c == null)
						throw new AdenineException("Could not load " + className);
					env.setValue(s, c);					
				}
				return null;
			}
		}.init(res);
	}

}
