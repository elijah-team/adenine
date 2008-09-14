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

package edu.mit.lcs.haystack.adenine.functions;

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.SyntaxException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.JavaMethodWrapper;
import edu.mit.lcs.haystack.adenine.interpreter.Message;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class NewFunction implements ICallable {
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		if (parameters.length < 1) {
			throw new SyntaxException("new requires at least one parameter");
		}
		String mismatch = null;
		try {
			Object klass = parameters[0];
			if (klass instanceof Class) {
				Class c = (Class)klass;
				Constructor[] a = c.getConstructors();
	
				for (int i = 0; i < a.length; i++) {
					Constructor m = a[i];
					if ((m.getModifiers() & Modifier.PUBLIC) != 0) {
						Class[] cs = m.getParameterTypes();
						if ((parameters.length - 1) == cs.length) {
							Object[] p2 = new Object[parameters.length - 1];
							mismatch = JavaMethodWrapper.prepareParameters(cs, parameters, p2, 1);
							if (mismatch == null) {
								m.setAccessible(true);
								Object ret = m.newInstance(p2);
								return new Message(ret);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new SyntaxException("Failed to create object", e);
		}
		throw new AdenineException("Parameter mismatch in construction: " + mismatch);
	}

}
