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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.utils.Connector;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class ConnectorPart extends Connector {
	protected Resource	m_resOnChange;
	transient protected Interpreter m_interpreter;
	transient protected DynamicEnvironment m_denv;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ConnectorPart.class);
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		m_interpreter = Ozone.getInterpreter();
		m_denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(m_denv, m_context);
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		m_resOnChange = null;
		
		super.dispose();
	}

	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.initialize(source, context);
		
		m_resOnChange = Utilities.getResourceProperty(m_resPartData, PartConstants.s_onChange, m_source);

		m_interpreter = Ozone.getInterpreter();
		m_denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(m_denv, m_context);
		
		onChange();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}	

	/**
	 * @see Connector#onChange()
	 */
	public void onChange() {
		try {
			Resource resResult = (Resource) m_interpreter.callMethod(m_resOnChange, new Object[] { this }, m_denv);
		} catch(AdenineException e) {
			s_logger.error("Failed to execute onChange handler " + m_resOnChange, e);
		}
	}
}
