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
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class ApplyFunction implements ICallable {
	/**
	 * @see edu.mit.lcs.haystack.adenine.interpreter.ICallable#invoke(edu.mit.lcs.haystack.adenine.interpreter.Message, edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv)
		throws AdenineException {
		Interpreter i = new Interpreter(denv.getInstructionSource());
		Object o = message.getPrimaryValue();
		Message msg = (Message) message.getOrderedValues()[1]; 

		// TODO[dquan]: support Class		
		/*if (o instanceof Class) {
			v.insertElementAt(o, 0);
			Message msg = new NewFunction().invoke(new Message(v.toArray(), namedParams), denv);
			return msg.getPrimaryValue();
		} else */
		if (o instanceof Resource) {
			return i.callMethod((Resource) o, msg, denv);
		} else if (o instanceof ICallable) {
			return ((ICallable) o).invoke(msg, denv);
		}
		
		throw new AdenineException("apply: object not callable");
	}
}
