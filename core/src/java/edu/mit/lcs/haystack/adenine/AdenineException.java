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

package edu.mit.lcs.haystack.adenine;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AdenineException extends HaystackException {
	public AdenineException(String str) {
		super(str);
	}
	
	public AdenineException(String str, Throwable e) {
		super(str, e);
	}
	
	public AdenineException(String str, int line) {
		super(str);
		m_line = line;
	}
	
	public AdenineException(String str, Throwable e, int line) {
		super(str, e);
		m_line = line;
	}
	
	public int m_line = -1;
	public ArrayList m_stackTrace = new ArrayList();
		
	/**
	 * @see Throwable#getMessage()
	 */
	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(super.getMessage());

		if (m_line != -1) {
			sb.append(" occurred at line ");
			sb.append(m_line);
			sb.append(": ");
		}

		sb.append("\n");
		Iterator i = m_stackTrace.iterator();
		while (i.hasNext()) {
			sb.append("\t");
			sb.append((String)i.next());
			sb.append("\n");
		}
		
		return sb.toString();
	}

	public void addToStackTrace(String operation, int line) {
		m_stackTrace.add(operation + " at line " + line);
	}
	
	public void addToStackTrace(Resource resFunction) {
		m_stackTrace.add("in method " + resFunction);
	}
	
	static public String describeInstruction(Resource resInstruction, IRDFContainer rdfc) {
		RDFNode operation = Utilities.getIndirectProperty(resInstruction, Constants.s_rdf_type, Constants.s_rdfs_label, rdfc);
		StringBuffer sb = new StringBuffer();
		if ((operation != null) && (operation instanceof Literal)) {
			sb.append(operation.getContent());
		} else {
			sb.append("Unknown operation");
		}
		
		sb.append(" at ");
		
		String strLine = Utilities.getLiteralProperty(resInstruction, AdenineConstants.line, rdfc);
		if (strLine == null) {
			sb.append("unknown line");
		} else {
			sb.append("line ");
			sb.append(strLine);
		}

		return sb.toString();
	}

	public void addToStackTrace(Resource resInstruction, IRDFContainer rdfc) {
		m_stackTrace.add(describeInstruction(resInstruction, rdfc));
		
		/*if (m_line == -1) {
			if (strLine != null) {
				try {
					m_line = Integer.parseInt(strLine);
				} catch (NumberFormatException nfe) {
				}
			}
		}*/
	}

	public void printStackTrace() {
		printStackTrace(System.err);
	}
	
	protected void printStackTrace0(PrintStream ps) {
		if (chain instanceof AdenineException) {
			((AdenineException)chain).printStackTrace0(ps);
		} else if (chain != null) {
			ps.print("Java exception: ");
			chain.printStackTrace(ps);
		}
		ps.println(getMessage());
	}

	protected void printStackTrace0(PrintWriter ps) {
		if (chain instanceof AdenineException) {
			((AdenineException)chain).printStackTrace0(ps);
		} else if (chain != null) {
			ps.print("Java exception: ");
			chain.printStackTrace(ps);
		}
		ps.println(getMessage());
	}

	public void printStackTrace(PrintWriter s) {
		s.println("An Adenine exception has occurred.");
		printStackTrace0(s);
	}

	public void printStackTrace(PrintStream s) {
		s.println("An Adenine exception has occurred.");
		printStackTrace0(s);
	}
}
