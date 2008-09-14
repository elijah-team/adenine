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
import edu.mit.lcs.haystack.adenine.interpreter.ISerializable;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.VariableFrame;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ModelInstruction implements IInstructionHandler {
	public static RDFNode extractNode(Object o, IRDFContainer rdfc) throws AdenineException, RDFException {
		try {
			if (o instanceof Collection) {
				return ListUtilities.createDAMLList(((Collection)o).iterator(), rdfc);
			} else if (o instanceof RDFNode) {
				return (RDFNode)o;
			} else if (o instanceof String) {
				return new Literal((String)o);
			} else if (o == null) {
				return new Literal("null");
			} else if (o instanceof ISerializable) {
				return ((ISerializable)o).serialize(rdfc);
			} else {
				return new Literal(o.toString());
			}
		} catch (ClassCastException cce) {
			throw new AdenineException("Type error", cce);
		}
	}

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
		denv = (DynamicEnvironment)denv.clone();
		LocalRDFContainer rdfc = new LocalRDFContainer();
		denv.setTarget(rdfc);
		Object subject = null, predicate = null;
		try {
			Resource[] statements = Utilities.getResourceProperties(res, AdenineConstants.statement, m_source);
			for (int i = 0; i < statements.length; i++) {
				Resource resStatement = statements[i];
				subject = extractNode(m_interpreter.runInstruction((Resource)m_source.extract(resStatement, AdenineConstants.subject, null), env, denv), rdfc);
				predicate = extractNode(m_interpreter.runInstruction((Resource)m_source.extract(resStatement, AdenineConstants.predicate, null), env, denv), rdfc);
				Resource resSubject = (Resource)subject;
				Resource resPredicate = (Resource)predicate;
				rdfc.add(new Statement(resSubject, resPredicate,
					extractNode(m_interpreter.runInstruction((Resource)m_source.extract(resStatement, AdenineConstants.object, null), env, denv), rdfc)));
			}
		} catch (RDFException rdfe) {
			throw new AdenineException("Unknown error", rdfe);
		} catch (ClassCastException cce) {
			if (!(subject instanceof Resource)) {
				throw new AdenineException("Subject in model is not of type resource: " + subject);
			} else if (!(predicate instanceof Resource)) {
				throw new AdenineException("Predicate in model is not of type resource: " + predicate);
			} else {
				throw new AdenineException("Unknown error", cce);
			}
		}
		return rdfc;
	}

	/**
	 * @see IInstructionHandler#generateExpression(Resource)
	 */
	public IExpression generateExpression(Resource res) throws AdenineException {
		return new IExpression() {
			ArrayList m_al = new ArrayList();
			int m_line = -1;
			Resource m_res;
			IExpression init(Resource res2) throws AdenineException {
				m_res = res2;
				String strLine = Utilities.getLiteralProperty(res2, AdenineConstants.line, m_source);
				if (strLine != null) {
					try {
						m_line = Integer.parseInt(strLine);
					} catch (NumberFormatException nfe) {
					}
				}
				Resource[] statements = Utilities.getResourceProperties(res2, AdenineConstants.statement, m_source);
				for (int i = 0; i < statements.length; i++) {
					Resource r = statements[i];
					IExpression subj = m_interpreter.compileInstruction(Utilities.getResourceProperty(r, AdenineConstants.subject, m_source));
					IExpression pred = m_interpreter.compileInstruction(Utilities.getResourceProperty(r, AdenineConstants.predicate, m_source));
					IExpression obj = m_interpreter.compileInstruction(Utilities.getResourceProperty(r, AdenineConstants.object, m_source));
					m_al.add(new IExpression[] { subj, pred, obj });
				}
				return this;
			}
			
			public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
				buffer.append(targetVar);
				buffer.append(" = new LocalRDFContainer();\n");
				
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
					pair[0].generateJava(ident0, buffer, frame, ct);
					pair[1].generateJava(ident1, buffer, frame, ct);
					pair[2].generateJava(ident2, buffer, frame, ct);
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
			}

			public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
				denv = (DynamicEnvironment)denv.clone();
				LocalRDFContainer rdfc = new LocalRDFContainer();
				denv.setTarget(rdfc);
				try {
					Iterator i = m_al.iterator();
					while (i.hasNext()) {
						IExpression[] pair = (IExpression[])i.next();
						Object subj = pair[0].evaluate(env, denv);
						Object pred = pair[1].evaluate(env, denv);
						Object obj = pair[2].evaluate(env, denv);
						rdfc.add(new Statement((Resource)ModelInstruction.extractNode(subj, rdfc),
							(Resource)ModelInstruction.extractNode(pred, rdfc),
							ModelInstruction.extractNode(obj, rdfc)));
					}
					return rdfc;
				} catch (RDFException rdfe) {
					throw new AdenineException("Unknown error", rdfe, m_line);
				} catch (ClassCastException cce) {
					throw new AdenineException("Type error " + m_res, cce, m_line);
				}
			}
		}.init(res);
	}

}
