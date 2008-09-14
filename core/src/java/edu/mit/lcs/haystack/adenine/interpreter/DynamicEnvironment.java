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

package edu.mit.lcs.haystack.adenine.interpreter;

import java.io.*;
import java.util.HashMap;

import edu.mit.lcs.haystack.adenine.query.DefaultQueryEngine;
import edu.mit.lcs.haystack.adenine.query.IQueryEngine;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.security.Identity;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class DynamicEnvironment extends Environment {
	public DynamicEnvironment() {
		setOutput(new PrintWriter(System.out, true));
		setInput(new BufferedReader(new InputStreamReader(System.in)));
		setQueryEngine(new DefaultQueryEngine());
		setIdentity(null);
	}

	public DynamicEnvironment(IRDFContainer rdfc) {
		this();
		setSource(rdfc);
		setTarget(rdfc);
	}

	public DynamicEnvironment(IRDFContainer rdfc, IServiceAccessor sa) {
		this(rdfc);
		setServiceAccessor(sa);
	}

	/**
	 * @see Object#clone()
	 */
	public Object clone() {
		DynamicEnvironment e = new DynamicEnvironment();
		e.m_bindings = (HashMap)m_bindings.clone();
		return e;
	}
	
	public Message getMessageIfAny() {
		if (isBound("__message__")) {
			return (Message)getValue("__message__");
		} else {
			return null;
		}
	}

	public IQueryEngine getQueryEngine() {
		return (IQueryEngine)getValue("__queryengine__");
	}

	public IRDFContainer getSource() {
		return (IRDFContainer)getValue("__source__");
	}

	public IRDFContainer getTarget() {
		return (IRDFContainer)getValue("__target__");
	}
	
	public IRDFContainer getInstructionSource() {
		return (IRDFContainer)getValue("__instructionsource__");
	}
	
	public IServiceAccessor getServiceAccessor() {
		return (IServiceAccessor)getValue("__serviceaccessor__");
	}

	public PrintWriter getOutput() {
		return (PrintWriter)getValue("__output__");
	}

	public BufferedReader getInput() {
		return (BufferedReader)getValue("__input__");
	}

	public Identity getIdentity() {
		return (Identity)getValue("__identity__");
	}

	public void setQueryEngine(IQueryEngine qe) {
		setCell("__queryengine__", new Cell(qe));
	}

	public void setTarget(IRDFContainer rdfc) {
		setCell("__target__", new Cell(rdfc));
	}

	public void setSource(IRDFContainer rdfc) {
		setCell("__source__", new Cell(rdfc));
	}

	public void setInstructionSource(IRDFContainer rdfc) {
		setCell("__instructionsource__", new Cell(rdfc));
	}

	public void setInput(BufferedReader input) {
		setCell("__input__", new Cell(input));
	}

	public void setOutput(PrintWriter output) {
		setCell("__output__", new Cell(output));
	}

	public void setServiceAccessor(IServiceAccessor sa) {
		setCell("__serviceaccessor__", new Cell(sa));
	}

	public void setIdentity(Identity id) {
		setCell("__identity__", new Cell(id));
	}

	public void setMessage(Message msg) {
		setCell("__message__", new Cell(msg));
	}

	public void setMessageIfAny(Message msg) {
		if (msg != null) {
			setMessage(msg);
		}
	}
}
