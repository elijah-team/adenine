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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class DeserializeFunction implements ICallable {

	/**
	 * @see edu.mit.lcs.haystack.adenine.interpreter.ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv)
		throws AdenineException {
		Object o = message.getPrimaryValue();
		if (o instanceof Literal) {
			return new Message(((Literal)o).getContent());
		}
		try {
			RDFNode[] datum = denv.getSource().queryExtract(new Statement[] {
				new Statement((Resource)o, Constants.s_rdf_type, Utilities.generateWildcardResource(3)),
				new Statement(Utilities.generateWildcardResource(2), AdenineConstants.conversionDomain, Utilities.generateWildcardResource(3)),
				new Statement(Utilities.generateWildcardResource(2), AdenineConstants.converter, Utilities.generateWildcardResource(1)),
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(3));
			if (datum != null) {
				Interpreter i = new Interpreter(denv.getSource());
				return new Message(i.callMethod((Resource)datum[0], new Object[] { message.getPrimaryValue() }, denv));
			}
		} catch (RDFException rdfe) {
			throw new AdenineException("", rdfe);
		}
		return new Message();
	}

	static public Object deserialize(RDFNode res, IRDFContainer source) {
		if (res instanceof Literal) {
			return res.getContent();
		}
		try {
			RDFNode[] datum = source.queryExtract(new Statement[] {
				new Statement((Resource)res, Constants.s_rdf_type, Utilities.generateWildcardResource(3)),
				new Statement(Utilities.generateWildcardResource(2), AdenineConstants.conversionDomain, Utilities.generateWildcardResource(3)),
				new Statement(Utilities.generateWildcardResource(2), AdenineConstants.converter, Utilities.generateWildcardResource(1)),
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(3));
			if (datum != null) {
				Interpreter i = new Interpreter(source);
				return i.callMethod((Resource)datum[0], new Object[] { res });
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
}
