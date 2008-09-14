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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import edu.mit.lcs.haystack.adenine.AdenineException;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class VariableFrame {
	public String m_frameName = Interpreter.generateIdentifier();
	public HashSet m_variables = new HashSet();
	public VariableFrame m_parentFrame = null;
	public HashSet m_defaultEnvironmentVariables = new HashSet();
	
	public String resolveVariableName(String name) throws UnboundIdentifierException {
		if (m_variables.contains(name)) {
			return Interpreter.filterSymbols(m_frameName + "_" + name);
		} else if (m_parentFrame != null) {
			return m_parentFrame.resolveVariableName(name);
		} else if (Interpreter.s_defaultEnvironment.isBound(name)) {
			m_defaultEnvironmentVariables.add(name);
			return Interpreter.filterSymbols(m_frameName + "_" + name);
		} else {
			throw new UnboundIdentifierException(name);
		}
	}
	
	public String generateVariableName(String name) {
		try {
			return resolveVariableName(name);
		} catch (AdenineException ae) {
			m_variables.add(name);
			return Interpreter.filterSymbols(m_frameName + "_" + name);
		}
	}
	
	public void mapVariables(HashMap variables) {
		if (m_parentFrame != null) {
			m_parentFrame.mapVariables(variables);
		}
		
		Iterator i = m_variables.iterator();
		while (i.hasNext()) {
			String var = (String)i.next();
			variables.put(var, m_frameName);
		}
	} 
	
	public VariableFrame flatten(String target, StringBuffer buffer) throws AdenineException {
		VariableFrame frame = new VariableFrame();
		HashMap variables = new HashMap();
		mapVariables(variables);
		
		Iterator i = variables.keySet().iterator();
		while (i.hasNext()) {
			String var = (String)i.next();
			
			buffer.append(target);
			buffer.append(".");
			buffer.append(frame.m_frameName);
			buffer.append("_");
			buffer.append(Interpreter.filterSymbols(var));
			buffer.append(" = ");
			buffer.append((String)variables.get(var));
			buffer.append("_");
			buffer.append(Interpreter.filterSymbols(var));
			buffer.append(";\n");
		}
		
		return frame;
	}
}
