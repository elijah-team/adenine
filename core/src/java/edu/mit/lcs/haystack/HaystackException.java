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

package edu.mit.lcs.haystack;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Base class for all Haystack exceptions.
 * @author Dennis Quan
 */
public class HaystackException extends Exception {
	protected Throwable chain = null;
	
	public HaystackException(String message) {
		super(message);
	}

	public HaystackException(String message, Throwable e) {
		super(message);
		chain = e;
	}
	
	public void printStackTrace() {
		super.printStackTrace();
		if (chain != null) {
			System.err.println("stemming from exception:");
			chain.printStackTrace();
		}
	}

	public void printStackTrace(PrintWriter s) {
		super.printStackTrace(s);
		if (chain != null) {
			s.println("stemming from exception:");
			chain.printStackTrace(s);
		}
	}

	public void printStackTrace(PrintStream s) {
		super.printStackTrace(s);
		if (chain != null) {
			s.println("stemming from exception:");
			chain.printStackTrace(s);
		}
	}
	
	public String getMessage() {
		// chain message as well when displaying!
		if (chain==null) {
			return getThisMessage();
		} else {
			return getThisMessage() + ": " + chain.getMessage();
		}
	}
	
	protected String getThisMessage() {
		return super.getMessage();
	}
	
	/**
	 * Prints a stack trace for an exception.  Primarily used to replace the contents of
	 * auto-generated catch blocks.
	 * <p>
	 * TODO [sjg]: Develop caller-specific alternatives to invoking this method.
	 */
	static public void uncaught(Exception e) {
		e.printStackTrace();
	}
	
}

