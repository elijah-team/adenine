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

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.ReaderInputStream;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.Resource;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class XMLDOMFunction implements ICallable {
	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		if (parameters.length != 1) {
			throw new SyntaxException("XMLDOM expects one argument.");
		}
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);

		if (parameters[0] instanceof Resource) {
			// Parse the DOM located at this URL
			Resource res = (Resource)parameters[0];
			boolean start = true;
			ContentClient cc = ContentClient.getContentClient(res, denv.getSource(), denv.getServiceAccessor());
			try {
				StringBuffer sb = new StringBuffer();
				String str = cc.getContentAsString();
				for (int i = 0; i < str.length(); i++) {
					char ch = str.charAt(i);
					if (start) {
						if (ch == '<') {
							start = false;
						} else {
							continue;
						}
					}
					if (ch < 9) {
						continue;
					}
					sb.append(ch);
				}
				try {
					String s = sb.toString();
					ReaderInputStream is = new ReaderInputStream(s);
					Object out = dbf.newDocumentBuilder().parse(is);
					return new Message(out);
				} catch (Exception e) {
					HaystackException.uncaught(e);
					return new Message(dbf.newDocumentBuilder().parse(cc.getContent()));
				}
			} catch (Exception e) {
				throw new AdenineException("Error occurred parsing XML document " + res + ".", e);
			}
		} else if (parameters[0] instanceof String) {
			// Parse the DOM located at this URL
			try {
				return new Message(dbf.newDocumentBuilder().parse((String)parameters[0]));
			} catch (Exception e) {
				throw new AdenineException("XML parsing error.", e);
			}
		} else {
			throw new SyntaxException("XMLDOM expects a string or a resource.");
		}
	}

}
