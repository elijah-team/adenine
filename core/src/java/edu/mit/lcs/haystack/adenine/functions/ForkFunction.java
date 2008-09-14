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
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Starts an Adenine service in a new thread.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ForkFunction implements ICallable {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ForkFunction.class);

	/**
	 * @see ICallable#invoke(Message, DynamicEnvironment)
	 */
	public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] parameters = message.m_values;
		
		ForkThread ft;
		
		int priority = ForkThread.NORM_PRIORITY;
			
		if (parameters[0] instanceof Resource) {
			ft = new ForkThread((Resource) parameters[0]);
			ft.m_parameters = new Object[parameters.length - 1];
			System.arraycopy(parameters, 1, ft.m_parameters, 0, parameters.length - 1);
		} else {
			ft = new ForkThread((Resource) parameters[1]);
			if (parameters[0] instanceof Integer) {
				priority = Math.max(
					ForkThread.MIN_PRIORITY,
					Math.min(
						ForkThread.MAX_PRIORITY,
						priority + ((Integer) parameters[0]).intValue()
					)
				);
			} else if (parameters[0] instanceof String) {
				if (parameters[0].equals("min")) {
					priority = ForkThread.MIN_PRIORITY;
				} else if (parameters[0].equals("max")) {
					priority = ForkThread.MAX_PRIORITY;
				}
			}
			ft.m_parameters = new Object[parameters.length - 2];
			System.arraycopy(parameters, 2, ft.m_parameters, 0, parameters.length - 2);
		}
		
		ft.m_priority = priority;
		ft.m_denv = (DynamicEnvironment)denv.clone();
		ft.m_source = denv.getSource(); // TODO[dquan]: use interpreter's source
		
		ft.start();
		return new Message();
	}

	class ForkThread extends Thread {
		DynamicEnvironment 	m_denv;
		IRDFContainer 		m_source;
		Resource 			m_service;
		Object[] 			m_parameters;
		int					m_priority;
		
		public ForkThread(Resource service) {
			super("Adenine fork " + service.getURI());
			m_service = service;
		}
		
		public void run() {
			setPriority(m_priority);
			
			{
				StringBuffer s = new StringBuffer(256);
				s.append("\r\n    ");
				s.append(this);
				s.append(" is starting at priority ");
				s.append(getPriority());
				s.append(" with parameters");
				
				for (int i = 0; i < m_parameters.length; i++) {
					s.append("\r\n      ");
					s.append(m_parameters[i]);
				}
				s_logger.info(s);
			}
			
			Interpreter i = new Interpreter(m_source);
			try { i.callMethod(m_service, m_parameters, m_denv); }
			catch (AdenineException ae) { HaystackException.uncaught(ae); }


			{
				StringBuffer s = new StringBuffer(256);
				s.append("\r\n    ");
				s.append(this);
				s.append(" finished");
				s_logger.info(s);
			}
		}
	}
}
