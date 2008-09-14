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

/*
 * Created on May 10, 2003
 */
package edu.mit.lcs.haystack.adenine.functions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Message;

/**
 * @author Dennis Quan
 */
public class SortFunction implements ICallable {
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.interpreter.ICallable#invoke(edu.mit.lcs.haystack.adenine.interpreter.Message, edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv)
		throws AdenineException {
		Object[] params = message.getOrderedValues();
		if (params.length < 2) {
			Collections.sort((List) params[0]);
		} else {
			Collections.sort((List) params[0], new Comparator() {
				ICallable m_function;
				DynamicEnvironment m_denv;
				
				Comparator init(ICallable function, DynamicEnvironment denv) {
					m_function = function;
					m_denv = denv;
					return this;
				}
				
				/* (non-Javadoc)
				 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
				 */
				public int compare(Object arg0, Object arg1) {
					try {
						return ((Integer) (m_function.invoke(new Message(new Object[] { arg0, arg1 }), m_denv)).getPrimaryValue()).intValue();
					} catch (AdenineException e) { 
						HaystackException.uncaught(e); 
						return 0;
					}
				}

			}.init((ICallable) params[1], denv));
		}
		return new Message();
	}
}
