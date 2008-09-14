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

package edu.mit.lcs.haystack.ozone.standard.behaviors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EventObject;

import org.eclipse.jface.dialogs.MessageDialog;

import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBehavior;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;

/**
 * Invokes an Adenine method.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AdenineBehavior implements IBehavior {
	public static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineBehavior.class);

	IRDFContainer	m_source;
	Context 		m_context;
	
	Resource		m_resPartData;
	
	Interpreter		m_interpreter;
	DynamicEnvironment m_denv;

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		m_source = source;
	}
	
	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context c) {
		m_source = source;
		m_context = c;

		getInitializationData();
		internalInitialize();		
	}

	/**
	 * @see IBehavior#activate(Resource, IPart, EventObject)
	 */
	public void activate(Resource resElement, IPart part, EventObject event) {
		try {
			m_interpreter.callMethod(m_resPartData, new Object[] { resElement, m_context, part, event }, m_denv);
		} catch (Exception e) {
			s_logger.error("An error occurred executing behavior " + m_resPartData, e);

			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String error = sw.toString();
			MessageDialog.openInformation(Ozone.s_display.getActiveShell(), "Error", error);
		}
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}	

	/**
	 * Retrieves resources.
	 */
	private void getInitializationData() {
		m_resPartData = (Resource) m_context.getProperty(OzoneConstants.s_partData);
	}

	/**
	 * Does the actual initialization work.
	 */
	private void internalInitialize() {
		m_interpreter = new Interpreter(m_source);
		m_denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(m_denv, m_context);
	}
}
