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

import java.io.*;
import java.util.*;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.rdfCode.RDFCodeCompiler;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.*;

/**
 * Displays an Adenine console using standard I/O ports.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Console {
	HashMap prefixes;
	Environment env;
	Interpreter in;
	DynamicEnvironment denv;
	String m_lastCommand = "";
	private static final String font = Constants.isWindows ? "Tahoma" : "Sans";
	static public final String s_prompt = "\nadenine> ";
	static public final String s_inputPrompt = "\ninput> ";
	String m_prompt = s_prompt;
	boolean m_inInput = false;
	String m_input;

	class QuitException extends AdenineException {
		/**
		 * @param str
		 */
		public QuitException() {
			super("");
		}
	}

	public Console(IRDFContainer rdfc) {
		in = new Interpreter(rdfc);
		env = in.createInitialEnvironment();
		denv = new DynamicEnvironment(rdfc, null);
		denv.setInstructionSource(rdfc);
		
		env.setValue("quit", new ICallable() { public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException { throw new QuitException(); }});
		prefixes = new HashMap();
		prefixes.put("adenine", AdenineConstants.NAMESPACE);
		prefixes.put("rdf", Constants.s_rdf_namespace);
		prefixes.put("rdfs", Constants.s_rdfs_namespace);
		prefixes.put("daml", Constants.s_daml_namespace);
		prefixes.put("xsd", Constants.s_xsd_namespace);
		prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
		prefixes.put("hs", "http://haystack.lcs.mit.edu/schemata/haystack#");
		prefixes.put("config", "http://haystack.lcs.mit.edu/schemata/config#");
		prefixes.put("mail", "http://haystack.lcs.mit.edu/schemata/mail#"); 
		prefixes.put("content", "http://haystack.lcs.mit.edu/schemata/content#"); 
		prefixes.put("ozone", "http://haystack.lcs.mit.edu/schemata/ozone#");
		prefixes.put("slide", "http://haystack.lcs.mit.edu/schemata/ozoneslide#");
		prefixes.put("source", "http://haystack.lcs.mit.edu/agents/adenine#");
		prefixes.put("op", "http://haystack.lcs.mit.edu/schemata/operation#");
		prefixes.put("opui", "http://haystack.lcs.mit.edu/ui/operation#");
		env.setValue("compile", new Compile());
		env.setValue("help", new Help());
		
	    System.out.print("Haystack Adenine Console\nVersion 1.0\nCopyright (c) Massachusetts Institute of Technology, 2001-2002.\n");
	}

	public void setServiceAccessor(IServiceAccessor sa) {
		denv.setServiceAccessor(sa);
	}

	public void setEnvironmentValue(String name, Object o) {
		env.setValue(name, o);
	}
	
	public void setDynamicEnvironmentValue(String name, Object o) {
		denv.setValue(name, o);
	}
	
	public void setEnvironment(Environment env) {
		this.env = env;
	}
	
	public void setDynamicEnvironment(DynamicEnvironment denv) {
		this.denv = denv;
	}
	
	class Help implements ICallable {
		/**
		 * @see ICallable#invoke(Message, DynamicEnvironment)
		 */
		public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
			StringBuffer sb = new StringBuffer();
	        java.lang.reflect.Method[] methods = message.m_values[0].getClass().getMethods();
	        if (methods.length > 0) {
	            sb.append("Methods:\n");
	            for (int i = 0; i < methods.length; i++) {
	                java.lang.reflect.Method m = methods[i];
	                try {
	                    Object.class.getMethod(m.getName(), m.getParameterTypes());
	                } catch (Exception e) {
	                    sb.append(m.getReturnType().getName() + " " + m.getName() + "(");
	                    Class[] params = m.getParameterTypes();
	                    for (int j = 0; j < params.length; j++) {
	                        sb.append(params[j].getName());
	                        if (j != (params.length - 1)) {
	                            sb.append(", ");
	                        }
	                    }
	                    sb.append(")\n");
	                }
	            }
	        }
	
	        java.lang.reflect.Field[] fields = message.m_values[0].getClass().getFields();
	        if (fields.length > 0) {
	            sb.append("Fields:\n");
	            for (int i = 0; i < fields.length; i++) {
	                java.lang.reflect.Field f = fields[i];
	                sb.append(f.getType());
	                sb.append(" ");
	                sb.append(f.getName());
	                sb.append("\n");
	            }
	            sb.append("\n");
	        }
	        
	        System.out.print(sb.toString());
	        
	        return new Message();
		}
	}

	class Compile implements ICallable {
		/**
		 * @see ICallable#invoke(Message, DynamicEnvironment)
		 */
		public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
			try {
				in.compileMethodToJava((Resource)message.m_values[0], (String)message.m_values[1]);
				return new Message();
			} catch (Exception e) {
				HaystackException.uncaught(e);
				return new Message();
			}
		}
	}
	
	public void run() {
		PrintWriter oldWriter = denv.getOutput();
		BufferedReader oldReader = denv.getInput();
		while (true) {
			try {
				System.out.print(m_prompt);
				String str = oldReader.readLine();
				Object o = in.eval(str, prefixes, env, denv);
				System.out.print("Result: ");
				System.out.println(o == null ? "null" : o.toString());
			} catch (QuitException qe) {
				return;
			} catch (Exception e) { HaystackException.uncaught(e); }
		}
	}

	/**
	 * Displays an Adenine console in an SWT frame window.
	 */
	public static void main(String[] args) {
		try {
			LocalRDFContainer 	rdfc = new LocalRDFContainer();
			ICompiler 			compiler = new RDFCodeCompiler(rdfc);
			
			compiler.compile(
				null,
				new InputStreamReader(CoreLoader.getResourceAsStream("/schemata/adenine.ad")),
//				new InputStreamReader(Interpreter.class.getResourceAsStream("/schemata/adenine.ad")),
				"/schemata/adenine.ad",
				null,
				null
			);
			new Console(rdfc).run();
		}
		catch (Exception e) { HaystackException.uncaught(e); }
	}
}
