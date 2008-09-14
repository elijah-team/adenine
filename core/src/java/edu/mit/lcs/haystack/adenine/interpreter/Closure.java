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

package edu.mit.lcs.haystack.adenine.interpreter;

import java.util.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.instructions.*;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Closure implements ICallable {
	Environment m_env;
	Resource m_resBody;
	IExpression m_body = null;
	ArrayList m_parameters = new ArrayList();
	Interpreter m_interpreter;

	public Closure(Environment env, Resource resBody, Resource resParameters, Interpreter i) {
		m_env = (Environment)env.clone();
		m_resBody = resBody;
		m_interpreter = i;
		
		IRDFContainer source = i.getRootRDFContainer();
		Iterator j = ListUtilities.accessDAMLList(resParameters, source);
		while (j.hasNext()) {
			Resource resVarName = (Resource)j.next();
			String varName = Utilities.getLiteralProperty(resVarName, AdenineConstants.name, source);
			m_parameters.add(varName);
		}
	}

	public Closure(Environment env, IExpression body, Resource resParameters, Interpreter i) {
		m_env = (Environment)env.clone();
		m_body = body;
		m_interpreter = i;
		
		IRDFContainer source = i.getRootRDFContainer();
		Iterator j = ListUtilities.accessDAMLList(resParameters, source);
		while (j.hasNext()) {
			Resource resVarName = (Resource)j.next();
			String varName = Utilities.getLiteralProperty(resVarName, AdenineConstants.name, source);
			m_parameters.add(varName);
		}
	}

	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Environment env = (Environment)m_env.clone();
		int j = 0;
		Iterator i = m_parameters.iterator();
		Object[] parameters = message.m_values;
		while (i.hasNext()) {
			String varName = (String)i.next();
			env.allocateCell(varName);
			if (j < parameters.length) {
				env.setValue(varName, parameters[j]);
			} else {
				env.setValue(varName, null);
			}
			++j;
		}

		if (j < parameters.length) {
			// TODO[dquan]: Too many arguments
		}
		
		Object ret;
		Object o = denv.getMessageIfAny();
		denv.setMessage(message);
		try {
			if (m_body != null) {
				ret = m_body.evaluate(env, denv);
			} else {
				ret = m_interpreter.runInstruction(m_resBody, env, denv);
			}
		} catch (ReturnException re) {
			return re.m_retVal;
		} finally {
			denv.setMessageIfAny((Message)o);
		}
		
		return new Message(ret);
	}

}
