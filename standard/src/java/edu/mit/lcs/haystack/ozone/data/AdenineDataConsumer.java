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

package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * @author David Huynh
 */
public class AdenineDataConsumer implements IDataConsumer {
	transient protected IRDFContainer	m_source;
	protected Context			m_context;
	
	protected Resource		m_methodOnDataChanged;
	protected Resource		m_methodOnStatusChanged;
	protected Resource		m_methodReset;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineDataConsumer.class);
	
	public AdenineDataConsumer(
		IRDFContainer	source,
		Context			context,
		Resource		methodOnDataChanged,
		Resource		methodOnStatusChanged,
		Resource		methodReset
	) {
		m_source = source;
		m_context = context;
		
		m_methodOnDataChanged = methodOnDataChanged;
		m_methodOnStatusChanged = methodOnStatusChanged;
		m_methodReset = methodReset;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onDataChanged(Resource, Object)
	 */
	public void onDataChanged(Resource changeType, Object change)
		throws IllegalArgumentException {
			
		if (m_methodOnDataChanged != null) {
			try {
				callMethod(m_methodOnDataChanged, new Object[] { this, changeType, change });
			} catch (Exception e) {
				s_logger.error("Error calling method onDataChanged " + m_methodOnDataChanged, e);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#onStatusChanged(Resource)
	 */
	public void onStatusChanged(Resource status) {
		if (m_methodOnStatusChanged != null) {
			try {
				callMethod(m_methodOnStatusChanged, new Object[] { this, status });
			} catch (Exception e) {
				s_logger.error("Error calling method onStatusChanged " + m_methodOnStatusChanged, e);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataConsumer#reset()
	 */
	public void reset() {
		if (m_methodReset != null) {
			try {
				callMethod(m_methodReset, new Object[] { this });
			} catch (Exception e) {
				s_logger.error("Error calling method reset " + m_methodReset, e);
			}
		}
	}

	protected DynamicEnvironment makeDynamicEnvironment() {
		DynamicEnvironment	denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(denv, m_context);
		
		return denv;
	}
	
	protected Object callMethod(Resource method, Object[] parameters) throws AdenineException {
		Interpreter 		interpreter = Ozone.getInterpreter();
		DynamicEnvironment	denv = makeDynamicEnvironment();
		
		return interpreter.callMethod(method, parameters, denv);
	}
	
	public void initializeFromDeserialization(IRDFContainer source) {
		m_source = source;
	}
}
