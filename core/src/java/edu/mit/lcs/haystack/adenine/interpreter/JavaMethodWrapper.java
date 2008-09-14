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

import java.lang.reflect.*;
import java.util.*;

import edu.mit.lcs.haystack.adenine.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class JavaMethodWrapper implements ICallable {
	Object m_targetObject;
	Class m_targetClass;
	String m_methodName;
	ArrayList m_methods = new ArrayList();
	
	public JavaMethodWrapper(Object o, String methodName, ArrayList al) {
		if (o instanceof Class) {
			m_targetObject = null;
			m_targetClass = (Class)o;
		} else {			
			m_targetObject = o;
			m_targetClass = o.getClass();
		}
		m_methodName = methodName;
		m_methods = al;
	}
	
	public JavaMethodWrapper(Object o, String methodName) {
		if (o instanceof Class) {
			m_targetObject = null;
			m_targetClass = (Class)o;
		} else {			
			m_targetObject = o;
			m_targetClass = o.getClass();
		}
		m_methodName = methodName;
		
		Method[] ms = m_targetClass.getMethods();
		for (int i = 0; i < ms.length; i++) {
			Method m = ms[i];
			if (m.getName().equals(m_methodName) &&
				((m.getModifiers() & Modifier.PUBLIC) != 0)) {
				m_methods.add(m);
			}
		}
	}
	
	public static Object convertToArray(Class c, Collection coll) {
		Object o = Array.newInstance(c.getComponentType(), coll.size());
		Iterator i = coll.iterator();
		int j = 0;
		while (i.hasNext()) {
			Array.set(o, j, i.next());
			++j;
		}
		return o;
	}
	
	static public String prepareParameters(Class[] cs, Object[] parameters, Object[] p2, int offset) {
		String mismatch = null;
		boolean ok = true;
		for (int j = 0; ok && (j < cs.length); j++) {
			if (parameters[j + offset] != null) {
				if (cs[j].isArray() && parameters[j + offset] instanceof Collection) {
					p2[j] = convertToArray(cs[j], (Collection)parameters[j + offset]);
				} else if (!cs[j].isAssignableFrom(parameters[j + offset].getClass())) {
					// TODO[dquan]: make more elegant
					if ((parameters[j + offset].getClass() == Boolean.class) &&
						(cs[j].toString().equals("boolean"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Integer.class) &&
						(cs[j].toString().equals("int"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Long.class) &&
						(cs[j].toString().equals("long"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Double.class) &&
						(cs[j].toString().equals("double"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Float.class) &&
						(cs[j].toString().equals("float"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Character.class) &&
						(cs[j].toString().equals("char"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Byte.class) &&
						(cs[j].toString().equals("byte"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else if ((parameters[j + offset].getClass() == Short.class) &&
						(cs[j].toString().equals("short"))) {
						ok = true;
						p2[j] = parameters[j + offset];
					} else {
						mismatch = ", parameter " + j + " has class " + parameters[j + offset].getClass().getName() + " when expected " + cs[j].toString();
						ok = false;
					}
				} else {
					p2[j] = parameters[j + offset];
				}
			} else {
				p2[j] = null;
			}
		}
		
		if (ok) {
			return null;
		} else {
			return mismatch;
		}
	}
	
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		//System.out.println("Invoking " + m_methodName + " on " + m_targetObject + " from class " + m_targetClass + " with # of params " + parameters.length);
		//System.out.println(m_methodName);
		String mismatch = null;
		Object[] parameters = message.m_values;
		try {
			Iterator i = m_methods.iterator();
			while (i.hasNext()) {
				Method m = (Method)i.next();
				Class[] cs = m.getParameterTypes();
				if (cs.length == parameters.length) {
					Object[] p2 = new Object[parameters.length];
					mismatch = prepareParameters(cs, parameters, p2, 0);
					if (mismatch == null) {
						m.setAccessible(true);
						Object ret = m.invoke(m_targetObject, p2);
						return new Message(ret);
					}
				}
			}
		} catch (InvocationTargetException ite) {
			throw new AdenineException("Invocation error: " + m_methodName, ite.getTargetException());
		} catch (IllegalAccessException iae) {
			throw new AdenineException("Illegal access to method: " + m_methodName, iae);
		}
		throw new AdenineException("Parameter mismatch: " + m_methodName + mismatch);
	}
}
