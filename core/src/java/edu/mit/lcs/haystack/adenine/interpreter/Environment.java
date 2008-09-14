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

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Environment implements Cloneable {
	HashMap m_bindings = new HashMap(100);
	
	public Environment() {
	}
	
	public Object getValue(String name) {
		return getCell(name).getValue();
	}

	public Object getValueChecked(String name) throws UnboundIdentifierException {
		if (!isBound(name)) {
			throw new UnboundIdentifierException(name);
		}
		return getCell(name).getValue();
	}

	public Cell getCell(String name) {
		return (Cell)m_bindings.get(name);
	}
	
	public void setCell(String name, Cell c) {
		m_bindings.put(name, c);
	}
	
	public void setValue(String name, Object o) {
		if (!isBound(name)) {
			setCell(name, new Cell(o));
		} else {
			Cell c = getCell(name);
			c.setValue(o);
		} 
	}
	
	public void unbind(String name) {
		m_bindings.remove(name);
	}
	
	public boolean isBound(String name) {
		return m_bindings.containsKey(name);
	}
	
	public void allocateCell(String name) {
		m_bindings.put(name, new Cell());
	}
	
	/**
	 * @see Object#clone()
	 */
	public Object clone() {
		Environment e = new Environment();
		e.m_bindings = (HashMap)m_bindings.clone();
		return e;
	}
}
