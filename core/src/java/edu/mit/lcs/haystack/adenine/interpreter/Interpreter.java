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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.compiler.Compiler;
import edu.mit.lcs.haystack.adenine.compiler.ExistentialExpression;

import edu.mit.lcs.haystack.adenine.functions.AddFunction;
import edu.mit.lcs.haystack.adenine.functions.AndFunction;
import edu.mit.lcs.haystack.adenine.functions.AppendFunction;
import edu.mit.lcs.haystack.adenine.functions.ApplyFunction;
import edu.mit.lcs.haystack.adenine.functions.AskFunction;
import edu.mit.lcs.haystack.adenine.functions.BitAndFunction;
import edu.mit.lcs.haystack.adenine.functions.ConnectFunction;
import edu.mit.lcs.haystack.adenine.functions.ContainsFunction;
import edu.mit.lcs.haystack.adenine.functions.DeserializeFunction;
import edu.mit.lcs.haystack.adenine.functions.DivideFunction;
import edu.mit.lcs.haystack.adenine.functions.EqualityFunction;
import edu.mit.lcs.haystack.adenine.functions.ExtractFunction;
import edu.mit.lcs.haystack.adenine.functions.FederatingQueryEngineFunction;
import edu.mit.lcs.haystack.adenine.functions.ForkFunction;
import edu.mit.lcs.haystack.adenine.functions.GtFunction;
import edu.mit.lcs.haystack.adenine.functions.InequalityFunction;
import edu.mit.lcs.haystack.adenine.functions.InstanceOfFunction;
import edu.mit.lcs.haystack.adenine.functions.LengthFunction;
import edu.mit.lcs.haystack.adenine.functions.ListFunction;
import edu.mit.lcs.haystack.adenine.functions.LtFunction;
import edu.mit.lcs.haystack.adenine.functions.MinusFunction;
import edu.mit.lcs.haystack.adenine.functions.MultiplyFunction;
import edu.mit.lcs.haystack.adenine.functions.NewFunction;
import edu.mit.lcs.haystack.adenine.functions.NotFunction;
import edu.mit.lcs.haystack.adenine.functions.OrFunction;
import edu.mit.lcs.haystack.adenine.functions.PlusFunction;
import edu.mit.lcs.haystack.adenine.functions.PrintDataFunction;
import edu.mit.lcs.haystack.adenine.functions.PrintFunction;
import edu.mit.lcs.haystack.adenine.functions.PrintListFunction;
import edu.mit.lcs.haystack.adenine.functions.PrintSetFunction;
import edu.mit.lcs.haystack.adenine.functions.QueryExtractFunction;
import edu.mit.lcs.haystack.adenine.functions.QueryFunction;
import edu.mit.lcs.haystack.adenine.functions.QuerySizeFunction;
import edu.mit.lcs.haystack.adenine.functions.ReifyFunction;
import edu.mit.lcs.haystack.adenine.functions.RemoveFunction;
import edu.mit.lcs.haystack.adenine.functions.ReplaceFunction;
import edu.mit.lcs.haystack.adenine.functions.SetFunction;
import edu.mit.lcs.haystack.adenine.functions.SortFunction;
import edu.mit.lcs.haystack.adenine.functions.XMLDOMFunction;

import edu.mit.lcs.haystack.adenine.instructions.ReturnException;
import edu.mit.lcs.haystack.adenine.parser.Block;
import edu.mit.lcs.haystack.adenine.parser.Parser;
import edu.mit.lcs.haystack.adenine.query.DefaultQueryEngine;

import edu.mit.lcs.haystack.core.CoreLoader;

import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.proxy.ProxyManager;

import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.URIGenerator;
import edu.mit.lcs.haystack.rdf.Utilities;

import org.apache.log4j.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Interprets Adenine code.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Interpreter {
	HashMap m_instructionHandlers = new HashMap(100);
	IRDFContainer m_source;
	IRDFContainer m_instructionSource;
	IServiceAccessor m_sa;
	IRDFContainer m_target = null;
	ArrayList m_callStack = new ArrayList();
	DebugDisplay m_debugDisplay = null;
	Resource m_currentMethod = null;
	int m_stepDepth = Integer.MAX_VALUE;
	static AdenineClassLoaderManager s_loaderManager;

	static public final Category s_logger = Category.getInstance(Interpreter.class);	
	public static Environment s_defaultEnvironment = new Environment();

	static {
		// Initialize default environment
		s_defaultEnvironment.setValue("add", new AddFunction());
		s_defaultEnvironment.setValue("ask", new AskFunction());
		s_defaultEnvironment.setValue("query", new QueryFunction());
		s_defaultEnvironment.setValue("remove", new RemoveFunction());
		s_defaultEnvironment.setValue("replace", new ReplaceFunction());
		s_defaultEnvironment.setValue("deserialize", new DeserializeFunction());
		s_defaultEnvironment.setValue("extract", new ExtractFunction());
		s_defaultEnvironment.setValue("apply", new ApplyFunction());
		s_defaultEnvironment.setValue("length", new LengthFunction());
		s_defaultEnvironment.setValue("queryExtract", new QueryExtractFunction());
		s_defaultEnvironment.setValue("querySize", new QuerySizeFunction());
		s_defaultEnvironment.setValue("connect", new ConnectFunction());
		s_defaultEnvironment.setValue("UniqueResource", new JavaMethodWrapper(Utilities.class, "generateUniqueResource"));
		s_defaultEnvironment.setValue("UnknownResource", new JavaMethodWrapper(Utilities.class, "generateUnknownResource"));
		s_defaultEnvironment.setValue("PackageFilterRDFContainer", PackageFilterRDFContainer.class);
		s_defaultEnvironment.setValue("==", new EqualityFunction());
		s_defaultEnvironment.setValue("!=", new InequalityFunction());
		s_defaultEnvironment.setValue("!", new NotFunction());
		s_defaultEnvironment.setValue("append", new AppendFunction());
		s_defaultEnvironment.setValue("String", new AppendFunction());
		s_defaultEnvironment.setValue("contains", new ContainsFunction());
		s_defaultEnvironment.setValue("+", new PlusFunction());
		s_defaultEnvironment.setValue("*", new MultiplyFunction());
		s_defaultEnvironment.setValue("&", new BitAndFunction());
		s_defaultEnvironment.setValue("-", new MinusFunction());
		s_defaultEnvironment.setValue("/", new DivideFunction());
		s_defaultEnvironment.setValue("lt", new LtFunction());
		s_defaultEnvironment.setValue("gt", new GtFunction());
		s_defaultEnvironment.setValue("List", new ListFunction());
		s_defaultEnvironment.setValue("Set", new SetFunction());
		s_defaultEnvironment.setValue("Map", HashMap.class);
		s_defaultEnvironment.setValue("fork", new ForkFunction());
		s_defaultEnvironment.setValue("new", new NewFunction());
		s_defaultEnvironment.setValue("null", null);
		s_defaultEnvironment.setValue("and", new AndFunction());
		s_defaultEnvironment.setValue("or", new OrFunction());
		s_defaultEnvironment.setValue("true", new Boolean(true));
		s_defaultEnvironment.setValue("false", new Boolean(false));
		s_defaultEnvironment.setValue("instanceOf", new InstanceOfFunction());
		s_defaultEnvironment.setValue("print", new PrintFunction());	
		s_defaultEnvironment.setValue("warn", new PrintFunction(new PrintWriter(System.err)));
		s_defaultEnvironment.setValue("sort", new SortFunction());
		//s_defaultEnvironment.setValue("input", new InputFunction());
		s_defaultEnvironment.setValue("reify", new ReifyFunction());
		s_defaultEnvironment.setValue("Literal", Literal.class);
		s_defaultEnvironment.setValue("Integer", Integer.class);
		s_defaultEnvironment.setValue("Resource", Resource.class);
		s_defaultEnvironment.setValue("Message", Message.class);
		s_defaultEnvironment.setValue("Statement", Statement.class);
		s_defaultEnvironment.setValue("LocalRDFContainer", LocalRDFContainer.class);
		s_defaultEnvironment.setValue("DefaultQueryEngine", DefaultQueryEngine.class);
		s_defaultEnvironment.setValue("FederatingQueryEngine", new FederatingQueryEngineFunction());
		s_defaultEnvironment.setValue("XMLDOM", new XMLDOMFunction());
		s_defaultEnvironment.setValue("printset", new PrintSetFunction());
		s_defaultEnvironment.setValue("printlist", new PrintListFunction());
		s_defaultEnvironment.setValue("printdata", new PrintDataFunction());

		s_defaultEnvironment.setValue("warnset", new PrintSetFunction(new PrintWriter(System.err)));
		s_defaultEnvironment.setValue("warnlist", new PrintListFunction(new PrintWriter(System.err)));
		s_defaultEnvironment.setValue("warndata", new PrintDataFunction(new PrintWriter(System.err)));

	}
	
	static ArrayList s_numberClassOrder = new ArrayList();
	static { 
		s_numberClassOrder.add(Byte.class);
		s_numberClassOrder.add(Short.class);
		s_numberClassOrder.add(Integer.class);
		s_numberClassOrder.add(Long.class);
		s_numberClassOrder.add(Float.class);
		s_numberClassOrder.add(Double.class);
	}
	
	interface INumberConverter {
		Object convertNumber(Number n);
	}
	
	static ArrayList s_numberConverters = new ArrayList();
	static {
		s_numberConverters.add(new INumberConverter() {
			public Object convertNumber(Number n) {
				return new Byte(n.byteValue());
			}
		});
		s_numberConverters.add(new INumberConverter() {
			public Object convertNumber(Number n) {
				return new Short(n.shortValue());
			}
		});
		s_numberConverters.add(new INumberConverter() {
			public Object convertNumber(Number n) {
				return new Integer(n.intValue());
			}
		});
		s_numberConverters.add(new INumberConverter() {
			public Object convertNumber(Number n) {
				return new Long(n.longValue());
			}
		});
		s_numberConverters.add(new INumberConverter() {
			public Object convertNumber(Number n) {
				return new Float(n.floatValue());
			}
		});
		s_numberConverters.add(new INumberConverter() {
			public Object convertNumber(Number n) {
				return new Double(n.doubleValue());
			}
		});
	}
	
	static public java.util.List upgradeNumberCollection(java.util.List l) {
		// Check for the most precise class present
		Iterator i = l.iterator();
		int max = -1;
		while (i.hasNext()) {
			Object o = i.next();
			if (o == null) {
				return l;
			}
			Class c = o.getClass();
			int n = s_numberClassOrder.indexOf(c);
			if (n == -1) {
				return l;
			}
			if (n > max) {
				max = n;
			}
		}
		
		if (max == -1) {
			return l;
		}
		
		ArrayList newList = new ArrayList();
		i = l.iterator();
		INumberConverter nc = (INumberConverter)s_numberConverters.get(max);
		while (i.hasNext()) {
			newList.add(nc.convertNumber((Number)i.next()));
		}
		
		return newList;
	}
	
	static public int getLineNumber(Resource res, IRDFContainer source) {
		String strLine = Utilities.getLiteralProperty(res, AdenineConstants.line, source);
		if (strLine != null) {
			try {
				return Integer.parseInt(strLine);
			} catch (NumberFormatException nfe) {
			}
		}
		return -1;
	}

	static public VariableFrame generateJavaBlock(IExpression body, StringBuffer buffer, VariableFrame parentFrame, ConstantTable ct) throws AdenineException {
		return generateJavaBlock(null, body, buffer, parentFrame, null, ct, true);
	}

	static public VariableFrame generateJavaBlock(String ident, IExpression body, StringBuffer buffer, VariableFrame parentFrame, String varName, ConstantTable ct, boolean generateVariables) throws AdenineException {
		StringBuffer buffer2 = new StringBuffer();
		VariableFrame frame = new VariableFrame();
		frame.m_parentFrame = parentFrame;
		if (varName != null) {
			frame.m_variables.add(varName);
		}
		if (ident == null) {
			ident = generateIdentifier();
			buffer.append("Object ");
			buffer.append(ident);
			buffer.append(";\n");
		}
		body.generateJava(ident, buffer2, frame, ct);
		
		if (generateVariables) {
			Iterator i = frame.m_variables.iterator();
			while (i.hasNext()) {
				String ident2 = (String)i.next();
				if (!ident2.equals(varName)) {
					buffer.append("Object ");
					buffer.append(frame.resolveVariableName(ident2));
					buffer.append(" = null;\n");
				}
			}
		}
				
		buffer.append(buffer2);
		
		return frame;
	}

	static public String generateIdentifier() {
		return "__anon" + Utilities.generateUniqueIdentifier() + "__";
	}

	/**
	 * Determines if an expression is considered to be "true".
	 */
	static public final boolean isTrue(Object condition) {
		return ((condition != null) &&
			(((condition instanceof Literal) && (((Literal) condition).getContent().compareToIgnoreCase("true") != 0)) ||
			((condition instanceof Number) && (((Number)condition).intValue() != 0)) || 
			((condition instanceof Boolean) && (((Boolean)condition).booleanValue())) || 
			(!(condition instanceof Number) && !(condition instanceof Boolean))));
	}
	
	public void setTarget(IRDFContainer rdfc) {
		m_target = rdfc;
	}
	
	public void setServiceAccessor(IServiceAccessor sa) {
		m_sa = sa;
	}
	
	public Interpreter(IRDFContainer rdfc) {
		m_source = rdfc;
		m_instructionSource = rdfc;
		m_sa = new ProxyManager(m_source, null);
		s_loaderManager = AdenineClassLoaderManager.getInstance();

		try {
			Set s = m_instructionSource.query(new Statement[] {
			    new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, AdenineConstants.INSTRUCTION_HANDLER),
				new Statement(Utilities.generateWildcardResource(1), Constants.s_haystack_javaImplementation, Utilities.generateWildcardResource(2)),
				new Statement(Utilities.generateWildcardResource(1), AdenineConstants.INSTRUCTION_DOMAIN, Utilities.generateWildcardResource(4)),
				new Statement(Utilities.generateWildcardResource(2), Constants.s_haystack_className, Utilities.generateWildcardResource(3))
			}, new Resource[] { Utilities.generateWildcardResource(3), Utilities.generateWildcardResource(4) }, Utilities.generateWildcardResourceArray(4));
			
			Iterator i = s.iterator();
			while (i.hasNext()) {
				RDFNode[] a = (RDFNode[])i.next();
				Literal l = (Literal)a[0];
				Resource res = (Resource)a[1];
				String className = l.getContent();
				try {
					IInstructionHandler ih = (IInstructionHandler)CoreLoader.loadClass(l.getContent()).newInstance();
					ih.initialize(this);
					addInstructionHandler(res, ih);
				} catch (Exception e) {
					// Skip
					//e.printStackTrace();
				}
			}
		} catch (Exception e) {
			s_logger.error("Unknown error", e);
		}
	}
	
	public IRDFContainer getRootRDFContainer() {
		return m_instructionSource;
	}
	
	public void addInstructionHandler(Resource resInstructionHandler, IInstructionHandler handler) {
		m_instructionHandlers.put(resInstructionHandler, handler);
	}
	
	public IInstructionHandler determineInstructionHandler(Resource resInstruction) throws AdenineException {
		try {
			Resource res = (Resource)m_instructionSource.queryExtract(new Statement[] {
				new Statement(resInstruction, Constants.s_rdf_type, Utilities.generateWildcardResource(1)),
//				new Statement(Utilities.generateWildcardResource(2), Constants.RDF_TYPE, AdenineConstants.INSTRUCTIONHANDLER),
				new Statement(Utilities.generateWildcardResource(2), AdenineConstants.INSTRUCTION_DOMAIN, Utilities.generateWildcardResource(1))
			}, Utilities.generateWildcardResourceArray(2), Utilities.generateWildcardResourceArray(1))[0];
			
//			Resource res = (Resource)m_source.extract(resInstruction, Constants.RDF_TYPE, null);
			IInstructionHandler ih = (IInstructionHandler)m_instructionHandlers.get(res);
			if (ih == null) {
				throw new UnknownInstructionException("ins " + resInstruction + " " + res + " " + res.toString());
			}
			return ih;
		} catch (UnknownInstructionException uie) {
			throw uie;
		} catch (Exception e) {
			Resource res = Utilities.getResourceProperty(resInstruction, Constants.s_rdf_type, m_instructionSource);
			s_logger.error("Unknown error", e);
			throw new UnknownInstructionException(res == null ? "uri " + resInstruction.toString() : res.toString(), e);
		}
	}
	
	public IExpression compileInstruction(Resource resInstruction) throws AdenineException {
		ArrayList al = new ArrayList();
		while (true) {
			IInstructionHandler ih = determineInstructionHandler(resInstruction);
			try {
				al.add(ih.generateExpression(resInstruction));
			} catch (AdenineException ae) {
				if (ae.m_line == -1) {
					String strLine = Utilities.getLiteralProperty(resInstruction, AdenineConstants.line, m_source);
					if (strLine != null) {
						try {
							ae.m_line = Integer.parseInt(strLine);
						} catch (NumberFormatException nfe) {
						}
					}
				}
				throw ae;
			}

			resInstruction = Utilities.getResourceProperty(resInstruction, AdenineConstants.next, m_source);
			if (resInstruction == null) {
				if (al.size() == 1) {
					return (IExpression)al.get(0);
				} else {
					return new IExpression() {
						ArrayList m_al;
						IExpression init(ArrayList al2) {
							m_al = al2;
							return this;
						}
						
						public void generateJava(String targetVar, StringBuffer buffer, VariableFrame frame, ConstantTable ct) throws AdenineException {
							Iterator i = m_al.iterator();
							while (i.hasNext()) {
								IExpression exp = (IExpression)i.next();
								exp.generateJava(targetVar, buffer, frame, ct);
							}
						}
						
						public Object evaluate(Environment env, DynamicEnvironment denv) throws AdenineException {
							Iterator i = m_al.iterator();
							Object o = null;
							while (i.hasNext()) {
								o = ((IExpression) i.next()).evaluate(env, denv);
							}
							return o;
						}
					}.init(al);
				}
			}
		}
	}
	
	public Object runInstruction(Resource resInstruction, Environment env, DynamicEnvironment denv) throws AdenineException {
		while (true) {
			Object o;
			
			ArrayList oldCallStack = (ArrayList)m_callStack.clone();
			m_callStack.add(resInstruction);
			
			IInstructionHandler ih = determineInstructionHandler(resInstruction);
			
			if ((m_debugDisplay != null) && !ih.isConstantExpression()) {
				if (m_stepDepth > m_callStack.size()) {
					int choice = m_debugDisplay.displayStatus(m_callStack, env, denv, null);
					switch (choice) {
					case DebugDisplay.UNMARK:
						try {
							m_instructionSource.remove(new Statement(m_currentMethod, AdenineConstants.debug, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
						} catch (RDFException e) {
						}
						
					case DebugDisplay.STOP_DEBUGGING:
						m_debugDisplay.dispose();
						m_debugDisplay = null;
						break;
						
					case DebugDisplay.STEP_OVER:
						m_stepDepth = m_callStack.size() + 1;
						break;
						
					case DebugDisplay.STEP_INTO:
						m_stepDepth = Integer.MAX_VALUE;
						break;
					}
				}
			}
			
			try {
				o = ih.evaluate(resInstruction, env, denv);
			} catch (ContinuationException ce) {
				throw ce;
			} catch (AdenineException ae) {
				ae.addToStackTrace(resInstruction, m_instructionSource);
				m_callStack = oldCallStack;
				throw ae;
			} catch (NullPointerException npe) {
				AdenineException ae = new AdenineException("Null pointer exception", npe);
				ae.addToStackTrace(resInstruction, m_instructionSource);
				throw ae;
			} catch (Exception e) {
				AdenineException ae = new AdenineException("Unknown exception", e);
				ae.addToStackTrace(resInstruction, m_instructionSource);
				throw ae;
			}

			m_callStack = oldCallStack;

			resInstruction = Utilities.getResourceProperty(resInstruction, AdenineConstants.next, m_instructionSource);
			if (resInstruction == null) {
				return o;
			}
		}
	}

	/**
	 * Binds standard functions in the given environment.
	 */
	public Environment createInitialEnvironment() {
		return (Environment)s_defaultEnvironment.clone();
	}
	
	static public String filterSymbols(String str) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
			case '=':
				sb.append("_eq");
				break;

			case '+':
				sb.append("_pl");
				break;

			case '&':
				sb.append("_an");
				break;

			case '-':
				sb.append("_mi");
				break;

			case '*':
				sb.append("_ti");
				break;

			case '!':
				sb.append("_ba");
				break;
				
			case '_':
				sb.append("_un");
				break;
				
			case ':':
				sb.append("_co");
				break;
				
			case '/':
				sb.append("_sl");
				break;
				
			case '\\':
				sb.append("_bs");
				break;
				
			case '.':
				sb.append("_do");
				break;
				
			case '?':
				sb.append("_qu");
				break;
				
			case '#':
				sb.append("_ha");
				break;
				
			default:
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	static public String escapeString(String str) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
			case '\n':
				sb.append("\\n");
				break;

			case '\"':
				sb.append("\\\"");
				break;

			case '\'':
				sb.append("\\\'");
				break;

			case '\r':
				sb.append("\\r");
				break;

			case '\t':
				sb.append("\\t");
				break;
				
			default:
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	static int compile(String[] args) throws Exception {
		// TODO: remove; from old compiler support 
		return 0;
		//com.sun.tools.javac.Main.compile(args);
	}

	/**
	 * Compiles an Adenine method to Java source code.
	 * @param resMethod The method to compile.
	 * @param path      The location to which to save the source file.
	 */
	public void compileMethodToJava(Resource resMethod, String path) throws AdenineException {
		try {
			String[] output = convertMethodToJava(resMethod);
			
			File file = new File(path, output[0] + ".java");
			FileOutputStream fos = new FileOutputStream(file);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
			
			pw.write(output[1]);
			
			pw.close();
			fos.close();
			
			String args[] = { 
				"-sourcepath",
				path,
				"-d",
				path,
				"-classpath", 
				System.getProperty("java.class.path"), 
				"-nowarn", 
				file.getAbsolutePath()
			}; 
			
			if (compile(args) == 0) {
				m_instructionSource.add(new Statement(resMethod, Constants.s_haystack_JavaClass, new Literal(output[0])));
				s_loaderManager.updateClass(new File(path, output[0] + ".class"), output[0]);
    
				for (int t = 1;;t++) {

					File subclass = new File(path, output[0] + "$" + t + ".class");
					if (!subclass.exists()) break;

					//s_logCategory.info("loading subclass" + output[0] + "$" + t);

					s_loaderManager.updateClass(subclass, output[0] + "$" + t);

					//s_logCategory.info("loading subclass" + output[0] + "$" + t + " DONE");
				}
			} else {
				new File(path, output[0] + ".class").delete();
			}
		} catch (Exception e) {
			throw new AdenineException("Compilation error", e);
		}
	}
	
	/**
	 * Batch compiles Adenine methods to Java source code.
	 */
	public Collection compileMethodsToJava(Collection methods, String path) {
		ArrayList params = new ArrayList();
		ArrayList successful = new ArrayList();
		ArrayList successfulFiles = new ArrayList();
		params.add("-sourcepath");
		params.add(path);
		params.add("-d");
		params.add(path);
		params.add("-classpath");
		params.add(System.getProperty("java.class.path"));
		params.add("-nowarn");
		Iterator i = methods.iterator();
		while (i.hasNext()) {
			Resource resMethod = (Resource) i.next();
			try {
				String[] output = convertMethodToJava(resMethod);
				
				File file = new File(path, output[0] + ".java");
				FileOutputStream fos = new FileOutputStream(file);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
				
				pw.write(output[1]);
				
				pw.close();
				fos.close();
				
				params.add(file.getAbsolutePath());
				successful.add(resMethod);
				successfulFiles.add(output[0]);
			} catch (Exception e) {
				s_logger.info("Could not compile method " + resMethod + " to Java", e);
			}
		}

		if (params.size() <= 7) {
			return new HashSet();
		}

		String args[] = new String[params.size()];
		params.toArray(args);

		try {			
			if (compile(args) == 0) {
				i = successful.iterator();
				Iterator i2 = successfulFiles.iterator();
				while (i.hasNext()) {
					Resource resMethod = (Resource) i.next();
					String filename = (String) i2.next();
					m_instructionSource.add(new Statement(resMethod, Constants.s_haystack_JavaClass, new Literal(filename)));
					
					s_loaderManager.updateClass(new File(path, filename + ".class"), filename);
	    
					for (int t = 1;;t++) {
	
						File subclass = new File(path, filename + "$" + t + ".class");
						if (!subclass.exists()) break;
	
						//s_logCategory.info("loading subclass" + output[0] + "$" + t);
	
						s_loaderManager.updateClass(subclass, filename + "$" + t);
	
						//s_logCategory.info("loading subclass" + output[0] + "$" + t + " DONE");
					}
				}
				return successful;
			} else {
				Iterator i2 = successfulFiles.iterator();
				while (i2.hasNext()) {
					String filename = (String) i2.next();
					new File(path, filename + ".class").delete();
				}
			}
		} catch (Exception e) {
			s_logger.info("Error invoking Java compiler", e);
		}
		return new HashSet();
	}
	
	/**
	 * Converts a service to Java.
	 */
	public String[] convertMethodToJava(Resource resMethod) throws AdenineException {
		// Convert name
		String methodName = filterSymbols(resMethod.getURI());
		
		// Print header
		StringBuffer sb = new StringBuffer();
		sb.append("import edu.mit.lcs.haystack.rdf.*;\nimport edu.mit.lcs.haystack.adenine.*;\n");		
		sb.append("import edu.mit.lcs.haystack.adenine.interpreter.*;\nimport edu.mit.lcs.haystack.adenine.instructions.*;\n");		
		sb.append("import edu.mit.lcs.haystack.adenine.query.*;\n");		
		sb.append("import edu.mit.lcs.haystack.core.CoreLoader;\n");	
		sb.append("import java.util.*;\n");		
		sb.append("import edu.mit.lcs.haystack.Constants;\n");		

		Resource resStart = Utilities.getResourceProperty(resMethod, AdenineConstants.start, m_instructionSource);
		IExpression exp = compileInstruction(resStart);
		
		// Map parameters
		sb.append("public class ");
		sb.append(methodName);
		sb.append(" extends CompiledMethod {\npublic void initializeParameters(Message msg) {\nint i = 0;\nObject[] parameters = msg.m_values;\n");
		VariableFrame frame = new VariableFrame();
		StringBuffer sb2 = new StringBuffer();
		Iterator i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(resMethod, AdenineConstants.PARAMETERS, m_instructionSource), m_instructionSource);
		while (i.hasNext()) {
			Resource resVarName = (Resource)i.next();
			String varName = Utilities.getLiteralProperty(resVarName, AdenineConstants.name, m_instructionSource);
			frame.m_variables.add(varName);
			sb.append("if (i < parameters.length) {\n");
			sb.append(frame.resolveVariableName(varName));
			sb.append(" = parameters[i++];\n}\n");
			sb2.append("Object ");
			sb2.append(frame.resolveVariableName(varName));
			sb2.append(";\n");
		}

		// Map named parameters
		Resource[] namedParameters = Utilities.getResourceProperties(resMethod, AdenineConstants.namedParameter, m_instructionSource);
		for (int j = 0; j < namedParameters.length; j++) {
			Resource paramName = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterName, m_instructionSource);
			String varName = Utilities.getIndirectProperty(namedParameters[j], AdenineConstants.parameterVariable, AdenineConstants.name, m_instructionSource).getContent();
			frame.m_variables.add(varName);
			sb.append(frame.resolveVariableName(varName));
			sb.append(" = msg.getNamedValue(new Resource(\"" + paramName.getURI() + "\"));\n");
			sb2.append("Object ");
			sb2.append(frame.resolveVariableName(varName));
			sb2.append(";\n");
		}

		sb.append("}\n");
		sb.append(sb2);
		
		sb.append("protected Message doInvoke() throws RDFException, AdenineException {\n");
		
		StringBuffer sb3 = new StringBuffer();
		ConstantTable ct = new ConstantTable();
		VariableFrame vf2 = generateJavaBlock("out", exp, sb3, frame, null, ct, false);

		sb.append("Object out;\n");
		sb.append(sb3);
		sb.append("return new Message(out);\n}\n");

		// Map default environment over
		i = frame.m_defaultEnvironmentVariables.iterator();
		while (i.hasNext()) {
			String name = (String)i.next();
			Object function = s_defaultEnvironment.m_bindings.get(name);
			frame.m_variables.add(name);
			sb.append("static Object ");
			sb.append(frame.resolveVariableName(name));
			sb.append(" = Interpreter.s_defaultEnvironment.getValue(\"");
			sb.append(name);
			sb.append("\");\n");
		}
		
		// Generate variable declarations
		i = vf2.m_variables.iterator();
		while (i.hasNext()) {
			String ident2 = (String)i.next();
			sb.append("Object ");
			sb.append(vf2.resolveVariableName(ident2));
			sb.append(" = null;\n");
		}

		sb.append(ct.generateConstantTable());
		sb.append("}\n");
		return new String[] { methodName, sb.toString() };
	}

	/**
	 * Invokes a service.
	 */
	public Object callMethod(Resource resMethod, Object[] arguments) throws AdenineException {
		// Initialize default identifiers
		DynamicEnvironment denv = new DynamicEnvironment();

		IRDFContainer rdfcOut;
		if (m_target == null) {
			rdfcOut = m_source;
		} else {
			rdfcOut = m_target;
		}

		denv.setSource(m_source);
		denv.setTarget(rdfcOut);
		denv.setServiceAccessor(m_sa);

		return callMethod(resMethod, arguments, denv);
	}		
	
	/**
	 * Invokes a service.
	 */
	public Message callMethod(Resource resMethod, Message msg) throws AdenineException {
		// Initialize default identifiers
		DynamicEnvironment denv = new DynamicEnvironment();

		IRDFContainer rdfcOut;
		if (m_target == null) {
			rdfcOut = m_source;
		} else {
			rdfcOut = m_target;
		}

		denv.setSource(m_source);
		denv.setTarget(rdfcOut);
		denv.setServiceAccessor(m_sa);

		return callMethod(resMethod, msg, denv);
	}

	/**
	 * Invokes a service.
	 */
	public Object callMethod(Resource resMethod, Object[] arguments, DynamicEnvironment denv) throws AdenineException {
		return callMethod(resMethod, new Message(arguments), denv).getPrimaryValueChecked();
	}
	
	/**
	 * Invokes a service.
	 */
	public Message callMethod(Resource resMethod, Message message, DynamicEnvironment denv) throws AdenineException {
		Object[] arguments = message.m_values;
		
		boolean preload = Utilities.checkBooleanProperty(resMethod, AdenineConstants.preload, m_instructionSource);
		if (m_callStack.isEmpty()) {
			boolean debug = Utilities.checkBooleanProperty(resMethod, AdenineConstants.debug, m_instructionSource);
			if (debug) {
				m_debugDisplay = new DebugDisplay(this);
			} else {
				m_debugDisplay = null;
			}
		}

		if (m_debugDisplay != null) {
			preload = false;
		}
		
		Resource oldMethod = m_currentMethod;
		m_currentMethod = resMethod;
		
		// Set the instruction source
		denv.setInstructionSource(m_instructionSource);

		try {
			if (preload) {		
				// Check for precompiled class
				String className = Utilities.getLiteralProperty(resMethod, Constants.s_haystack_JavaClass, m_instructionSource);
				if (className != null) {
					CompiledMethod cs = null;
					try {
					    cs = (CompiledMethod)((className.startsWith("edu.mit.lcs.haystack.adenine") ? CoreLoader.loadClass(className) : s_loaderManager.getClass(className)).newInstance());
					} catch (Exception e) {
						
					}
					
					if (cs != null) {
						try {
							//s_logCategory.info("Using Java compiled method for " + resMethod);
							return cs.invoke(this, denv, message, resMethod);
						} catch (AdenineException ae) {
							ae.addToStackTrace(resMethod);
							throw ae;
						}
					}
					s_logger.warn("Java compiled method failed; resorting to interprer.");
				} 
			}
			
			// Map parameters
			Environment params = createInitialEnvironment();
			
			Iterator i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(resMethod, AdenineConstants.PARAMETERS, m_instructionSource), m_instructionSource);
			int j = 0;
			while (i.hasNext()) {
				Resource resVarName = (Resource)i.next();
				String varName = Utilities.getLiteralProperty(resVarName, AdenineConstants.name, m_instructionSource);
				if (j < arguments.length) {
					params.setValue(varName, arguments[j]);
				} else {
					params.setValue(varName, null);
				}
				++j;
			}
	
			if (j < arguments.length) {
				// TODO[dquan]: Too many arguments
			}
			
			// Map named parameters
			Resource[] namedParameters = Utilities.getResourceProperties(resMethod, AdenineConstants.namedParameter, m_instructionSource);
			for (j = 0; j < namedParameters.length; j++) {
				Resource paramName = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterName, m_instructionSource);
				Resource rhs = Utilities.getResourceProperty(namedParameters[j], AdenineConstants.parameterVariable, m_instructionSource);
				
				String varName = Utilities.getLiteralProperty(rhs, AdenineConstants.name, m_instructionSource);
				
				Object o = null;
				if ((message.m_namedValues != null) && message.m_namedValues.containsKey(paramName)) {
					o = message.m_namedValues.get(paramName);
				}
				
				params.setValue(varName, o);
			}
	
			// Get starting instruction and run
			Resource resStart = Utilities.getResourceProperty(resMethod, AdenineConstants.start, m_instructionSource);
			if (resStart == null) {
				throw new AdenineException("Method " + resMethod + " missing entry point.");
			}
	
			// Run service
			Object ret = null;
			Object o = denv.getMessageIfAny();
			denv.setMessage(message);
			try {
				if (preload) {
					ret = compileInstruction(resStart).evaluate(params, denv);
				} else {
					ret = runInstruction(resStart, params, denv);
				}
			} catch (ReturnException re) {
				return re.m_retVal;
			} catch (AdenineException ae) {
				ae.addToStackTrace(resMethod);
				throw ae;
			} catch (NullPointerException npe) {
				AdenineException ae = new AdenineException("Null pointer exception", npe);
				ae.addToStackTrace(resMethod);
				throw ae;
			} finally {
				denv.setMessageIfAny((Message)o);
			}
			
			return new Message(new Object[] { ret });
		} finally {
			if (m_callStack.isEmpty() && (m_debugDisplay != null)) {
				m_debugDisplay.dispose();
				m_debugDisplay = null;
			}
			m_currentMethod = oldMethod;
		}
	}

	/**
	 * Evaluates a single Adenine command.
	 */
	public Object eval(String str, HashMap prefixes, Environment env, DynamicEnvironment denv) throws Exception {
		Block b = Parser.blockify(Parser.tokenize(new StringReader(str)));
		Compiler compiler = new Compiler(m_source); // TODO[dfhuynh]: replace old compiler
		ExistentialExpression ee = compiler.compileBlock(b, prefixes);
		if (ee == null) {
			return null;
		}
		
		Resource res = (Resource) ee.generate(new URIGenerator(), m_instructionSource);
		if (res == null) {
			return null;
		}
		return runInstruction(res, env, denv);
	}
}
