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

import java.util.Arrays;
import java.util.List;

import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.Literal;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class PlusFunction implements ICallable {
	static public Number parse(Object o) throws AdenineException {
		String str = null;
		if (o == null) {
			throw new AdenineException("Null is not a number");
		}
		
		if (o instanceof Number) {
			return (Number)o;
		} else if (o instanceof Literal) {
			str = ((Literal)o).getContent();
		} else if (!(o instanceof String)) {
			str = o.toString();
		} else {
			str = (String)o;
		}
		
		try {
			if (str.indexOf('.') != -1) {
				return new Double(Double.parseDouble(str));
			} else {
				return new Integer(Integer.parseInt(str));
			}
		} catch (Exception e) {
			throw new AdenineException("Number in invalid format: " + o, e);
		}
	}
	
	static public Object[] upgradeNumberList(Object[] numbers) {
		List params = Interpreter.upgradeNumberCollection(Arrays.asList(numbers));
		return params.toArray();
	}

	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		Object[] numbers = upgradeNumberList(parameters);
		if ((numbers.length > 0) && (numbers[0] instanceof Double)) {
			double n = 0;
			for (int i = 0; i < numbers.length; i++) {
				n += ((Double)numbers[i]).doubleValue();
			}
			return new Message(new Double(n));
		} else if ((numbers.length > 0) && (numbers[0] instanceof Long)) {
			long n = 0;
			for (int i = 0; i < numbers.length; i++) {
				n += ((Long)numbers[i]).longValue();
			}
			return new Message(new Long(n));
		} else if ((numbers.length > 0) && (numbers[0] instanceof Float)) {
			float n = 0;
			for (int i = 0; i < numbers.length; i++) {
				n += ((Float)numbers[i]).longValue();
			}
			return new Message(new Float(n));
		} else {
			int n = 0;
			for (int i = 0; i < numbers.length; i++) {
				n += ((Integer)numbers[i]).intValue();
			}
			return new Message(new Integer(n));
		}
	}

}
