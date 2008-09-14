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
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Message;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class PrintSetFunction implements ICallable {
	protected PrintWriter _pw;
	/**
	 * sets the print writer for this print function 
	 * @param pw
	 */
	public PrintSetFunction(PrintWriter pw) {
		_pw = pw;
	}
	/**
	 * default constructor, output determined by __output__ in
	 * environment
	 */
	public PrintSetFunction() {
		_pw = null;
	}
	
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		if (message.m_values.length != 1) {
			throw new AdenineException("printset expects one parameter");
		}
		Iterator i = ((Collection)message.m_values[0]).iterator();
		StringBuffer sb = new StringBuffer();
		while (i.hasNext()) {
			Object o = i.next();
			if (o == null) {
				sb.append("null");
			} else if (o.getClass().isArray()) {
				int c = Array.getLength(o);
				for (int j = 0; j < c; j++) {
					Object o2 = Array.get(o, j);
					if (o2 == null) {
						sb.append("null");
					} else {
						sb.append(o2.toString());
					}
					sb.append("\t");
				}
			} else {
				sb.append(o.toString());
			}
			sb.append("\n");
		}
		if (_pw == null) {
		  _pw = denv.getOutput();
		}
		
		_pw.print(sb.toString());
		_pw.flush();
		return new Message();
	}
}
