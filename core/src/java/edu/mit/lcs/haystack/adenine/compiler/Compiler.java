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

package edu.mit.lcs.haystack.adenine.compiler;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.parser.*;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.*;

import java.io.*;
import java.util.*;

import org.apache.log4j.Category;

/**
 * Adenine compiler
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Compiler implements ICompiler {
	public IRDFContainer m_target;
	HashMap m_instructionGenerators = new HashMap();
	Resource m_sourceResource;
	boolean m_precompile = false;
	String m_outPath = "";
	Interpreter m_interpreter = null;
	static public final Category s_logCategory = Category.getInstance(Compiler.class);
	public static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Compiler.class);

	// TODO: Resolve duplication in AdenineSourceAgent.java
	static public final String NAMESPACE = "http://haystack.lcs.mit.edu/agents/adenine#";
	static public final Resource PRECOMPILE_TIME = new Resource(NAMESPACE + "precompileTime");
	
	LocalRDFContainer m_tempStorage;
	
	public Compiler(IRDFContainer target) {
		m_target = target;
		
		m_instructionGenerators.put("method", new MethodGenerator());
		m_instructionGenerators.put("uniqueMethod", new MethodGenerator());
		m_instructionGenerators.put("function", new FunctionGenerator());
		m_instructionGenerators.put("call", new CallGenerator());
		m_instructionGenerators.put("return", new ReturnGenerator());
		m_instructionGenerators.put("break", new BreakGenerator());
		m_instructionGenerators.put("continue", new ContinueGenerator());
		m_instructionGenerators.put("for", new ForGenerator());
		m_instructionGenerators.put("if", new IfGenerator());
		m_instructionGenerators.put("while", new WhileGenerator());
		m_instructionGenerators.put("var", new VarGenerator());
		m_instructionGenerators.put("with", new WithGenerator());
		m_instructionGenerators.put("import", new ImportGenerator());
		m_instructionGenerators.put("importjava", new ImportJavaGenerator());
		m_instructionGenerators.put("=", new AssignmentGenerator());
	}
	
	public Compiler(IRDFContainer target, boolean precompile, File outDir) throws AdenineException, RDFException {
		this(target);
		m_outPath = outDir.getAbsolutePath();
		if (precompile) {
			m_tempStorage = new LocalRDFContainer();
			m_target = m_tempStorage;
			InputStreamReader inp = new InputStreamReader(CoreLoader.getResourceAsStream("/schemata/adenine.ad"));
			compile(null, inp, null, null, null);
			m_precompile = true;
			m_target = target;
		}
	}
	
	public static void main(String args[]) {
		try {
			LocalRDFContainer rdfc = new LocalRDFContainer();
			new Compiler(rdfc).compile(null, new FileReader(args[0]), null, null, null);
			System.out.println(Utilities.generateN3(rdfc));
		} catch (Exception e) { HaystackException.uncaught(e); }
	}
	
	void processPrefix(int line, Iterator i, HashMap prefixes) throws AdenineException {
		try {
			IdentifierToken t2 = (IdentifierToken)i.next();
			URIToken t3 = (URIToken)i.next();
			if (t2.m_token.charAt(t2.m_token.length() - 1) != ':') {
				throw new SyntaxException("Prefix must end with :", line);
			}
			prefixes.put(t2.m_token.substring(0, t2.m_token.length() - 1), t3.m_token);
		} catch (Exception e) {
			throw new SyntaxException("Invalid @prefix", line);
		}
	}
	
	public java.util.List compile(Resource pakkage, Reader input, String sourceFile, IRDFContainer source, IServiceAccessor sa) {
		List 	errors = new LinkedList();
		String 	str;
		
		if (input == null) {
			m_sourceResource = pakkage;
			try {
				str = ContentClient.getContentClient(pakkage, source, sa).getContentAsString();
			} catch (Exception e) {
				errors.add(new AdenineException("Failed to compile " + input, e));
				return errors;
			}
		} else {
			BufferedReader 	reader = new BufferedReader(input);
			StringWriter 	sw = new StringWriter();
			PrintWriter 	pw = new PrintWriter(sw);
			String 			line;
		
			try {
				while ((line = reader.readLine()) != null) {
					pw.println(line);
				}
			} catch (IOException ioe) {
				errors.add(new AdenineException("Failed to read Adenine source file", ioe));
				return errors;
			}

			// Store source code
			m_sourceResource = Utilities.generateUniqueResource();
			try {
				m_target.add(new Statement(m_sourceResource, Constants.s_content_content, new Literal(sw.getBuffer().toString())));
				m_target.add(new Statement(m_sourceResource, Constants.s_rdf_type, Constants.s_content_LiteralContent));
			} catch (RDFException e) {
				errors.add(e);
				return errors;
			}
			
			str = sw.getBuffer().toString();
		}
		return doCompile(str, errors, pakkage);
	}
	
	protected List doCompile(String input, List errors, Resource pakkage) { 
		try {
			// Tokenize and blockify
			Block b = Parser.blockify(Parser.tokenize(new StringReader(input)));
		
			// Generate metainstructions
			HashMap prefixes = new HashMap();
			prefixes.put("random", Utilities.generateUniqueResource().getURI() + ":");
			prefixes.put("adenine", AdenineConstants.NAMESPACE);
			prefixes.put("rdf", Constants.s_rdf_namespace);
			prefixes.put("rdfs", Constants.s_rdfs_namespace);
			prefixes.put("daml", Constants.s_daml_namespace);
			prefixes.put("xsd", Constants.s_xsd_namespace);
			prefixes.put("", Utilities.generateUniqueResource().getURI() + ":");
			prefixes.put("@urigenerator", new URIGenerator());

			Resource main = compileTopLevel(prefixes, b);
		
			// Record compilation metadata
			String base = (String)prefixes.get("@base");
			if (base != null) {
				Resource resBase = new Resource(base);
				m_target.add(new Statement(resBase, AdenineConstants.compileTime, new Literal(new Date().toString())));
			}
			
			if (main != null && pakkage != null) {
				m_target.add(new Statement(pakkage, AdenineConstants.main, main));
			}
		} catch (AdenineException e) {
			errors.add(e);
		} catch (RDFException e) {
			errors.add(e);
		}
		
		return errors; // supposedly of errors
	}

	public ExistentialExpression compileBlock(Block b, HashMap prefixes) throws AdenineException, RDFException {
		ListIterator k = b.m_items.listIterator();
		ExistentialExpression eeLast = null, eeFirst = null;
		while (k.hasNext()) {
			Object o = k.next();
			if (o instanceof Block) {
				throw new SyntaxException("Block not valid here", ((Block)o).m_startline);
			}
			Line line = (Line)o;
			ListIterator i = line.m_tokens.listIterator();
			while (i.hasNext()) {
				Token t = (Token)i.next();
				ExistentialExpression ee1 = null;
				boolean breakAfter = false;
				if (t instanceof IdentifierToken) {
					if (t.m_token.equals("@prefix")) {
						// Process prefix
						processPrefix(t.m_line, i, prefixes);
						continue;
					} else {
						// Find appropriate desugarer
						IInstructionGenerator ig = (IInstructionGenerator)m_instructionGenerators.get(t.m_token);
						if (ig != null) {
							ee1 = ig.generateInstruction(this, t, prefixes, k, i);
						}
					}
				}
				
				if (ee1 == null) {
					// Process as function call
					ee1 = compileFunctionCall(t.m_line, line.m_tokens, k, prefixes);
					breakAfter = true;
				}
				
				if (eeFirst == null) {
					eeFirst = ee1;
				} 
				if (eeLast != null) {
					eeLast.add(AdenineConstants.next, ee1);
				}

				eeLast = ee1;
				if (breakAfter) {
					break;
				}
			}
		}
		
		if (eeFirst == null) {
			return null;
		}
		
		return eeFirst;
	}

	public ExistentialExpression generateInstruction(Resource type, int line) throws RDFException {
		ExistentialExpression ee = new ExistentialExpression();
		ee.add(Constants.s_rdf_type, type);
		ee.add(AdenineConstants.line, new LiteralExpression(Integer.toString(line)));
		if (m_sourceResource != null) {
			ee.add(AdenineConstants.source, m_sourceResource);
		}
		return ee;
	}

	public void processAttributes(ExistentialExpression ee, Iterator i, Iterator k, int line, HashMap prefixes) throws AdenineException, RDFException {
		if (!i.hasNext()) {
			if (!k.hasNext()) {
				throw new SyntaxException("Unexpected end of file processing ;", line);
			}
			Object o = k.next();
			if (o instanceof Line) {
				i = ((Line)o).m_tokens.iterator();
			}
		}
		while (i.hasNext()) {
			ITemplateExpression[] r = processPredicateObject(i, prefixes, line);
			ee.add(r[0], r[1]);
			if (i.hasNext()) {
				Token t = (Token)i.next();
				if (!(t instanceof SemicolonToken)) {
					throw new SyntaxException("Semicolon expected", line);
				}
				if (!i.hasNext()) {
					if (!k.hasNext()) {
						throw new SyntaxException("Unexpected end of file processing ;", line);
					}
					Object o = k.next();
					if (o instanceof Line) {
						i = ((Line)o).m_tokens.iterator();
					}
				}
			}
		}
	}

	ExistentialExpression processDollarBrace(int line, Iterator i, HashMap prefixes) throws AdenineException, RDFException {
		ExistentialExpression resSubject = new ExistentialExpression();
		while (i.hasNext()) {
			ITemplateExpression[] a = processPredicateObject(i, prefixes, line);
			resSubject.add(a[0], a[1]);

			if (i.hasNext()) {
				Token t = (Token)i.next();
				if (!(t instanceof SemicolonToken)) {
					throw new SyntaxException("Semicolon expected", line);
				}
			}
		}
		return resSubject;
	}
	
	ITemplateExpression processObject(Token t3, HashMap prefixes, int line) throws AdenineException, RDFException {
		ITemplateExpression resObject = null;
		if (t3 instanceof IdentifierToken) {
			resObject = new ResourceExpression(identifierToResource(t3.m_line, t3.m_token, prefixes));
		} else if (t3 instanceof SemicolonToken) {
			throw new SyntaxException("; cannot be used as object", t3.m_line);
		} else if (t3 instanceof URIToken) {
			resObject = new ResourceExpression(t3.m_token);
		} else if (t3 instanceof ParenthesizedToken) {
			resObject = processList(t3.m_line, ((ParenthesizedToken)t3).m_tokens.iterator(), prefixes);
		} else if (t3 instanceof LiteralToken) {
			resObject = new LiteralExpression(t3.m_token);
		} else if (t3 instanceof BracedToken) {
			BracedToken bt = (BracedToken)t3;
			if (bt.m_percent) {
				resObject = processQuery(bt, prefixes, t3.m_line);
			} else if (!bt.m_dollar) {
				throw new SyntaxException("{} cannot be used in this context", t3.m_line);
			} else {
				resObject = processDollarBrace(t3.m_line, bt.m_tokens.iterator(), prefixes);
			}
		} else {
			throw new SyntaxException("Invalid token", t3.m_line);
		}
		
		return resObject;
	}
	
	ITemplateExpression processList(int line, Iterator i, HashMap prefixes) throws AdenineException, RDFException {
		if (!i.hasNext()) {
			return new ResourceExpression(Constants.s_daml_nil);
		}
		
		ExistentialExpression resSubject = new ExistentialExpression();
		ExistentialExpression resFirst = resSubject;
		ExistentialExpression resLast = null;
		
		while (i.hasNext()) {
			ITemplateExpression o = processObject((Token) i.next(), prefixes, line);
			if (resLast != null) {
				resLast.add(Constants.s_daml_rest, resSubject);
			}
			resSubject.add(Constants.s_daml_first, o);
			resLast = resSubject;
			resSubject = new ExistentialExpression();
		}
		
		resLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		
		return resFirst;
	}
	
	ITemplateExpression processQuery(BracedToken token, HashMap prefixes, int line) throws AdenineException, RDFException {
		ExistentialExpression res = new ExistentialExpression();
		res.add(Constants.s_rdf_type, AdenineConstants.ConditionSet);
		ListIterator i = token.m_tokens.listIterator();
		ArrayList currentCondition = new ArrayList();
		ArrayList conditions = new ArrayList();
		while (i.hasNext()) {
			Token t1 = (Token)i.next();
			if ((t1 instanceof IdentifierToken) && t1.m_token.equals(",")) {
				if (currentCondition.isEmpty()) {
					throw new SyntaxException("Misplaced ,", t1.m_line);
				} else {
					conditions.add(makeList(currentCondition.iterator()));
					currentCondition = new ArrayList();
				}
			} else if (!currentCondition.isEmpty() && ((t1 instanceof IdentifierToken) || (t1 instanceof URIToken) || (t1 instanceof LiteralToken))) {
				// TODO[dquan]: handle case where the token is an identifier but not a URI
				Object o = compileToken(t1, prefixes);
//				System.out.println(t1);
//				System.out.println(o);
				currentCondition.add(o);
			} else {
				i.previous();
				currentCondition.add(processObject((Token) i.next(), prefixes, line));
			}
		}

		if (!currentCondition.isEmpty()) {
			conditions.add(makeList(currentCondition.iterator()));
			currentCondition = new ArrayList();
		}
		res.add(AdenineConstants.conditions, makeList(conditions.iterator()));

		return res;
	}
	
	ITemplateExpression makeList(Iterator i) throws AdenineException, RDFException {
		if (!i.hasNext()) {
			return new ResourceExpression(Constants.s_daml_nil);
		}
		
		ExistentialExpression resSubject = new ExistentialExpression();
		ExistentialExpression resFirst = resSubject;
		ExistentialExpression resLast = null;
		
		while (i.hasNext()) {
			ITemplateExpression o = (ITemplateExpression)i.next();
			if (resLast != null) {
				resLast.add(Constants.s_daml_rest, resSubject);
			}
			resSubject.add(Constants.s_daml_first, o);
			resLast = resSubject;
			resSubject = new ExistentialExpression();
		}
		
		resLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		
		return resFirst;
	}
	
	ExistentialExpression compileFunctionCall(int line, ArrayList tokens, Iterator k, HashMap prefixes) throws AdenineException, RDFException {
		ExistentialExpression ee = generateInstruction(AdenineConstants.FunctionCall, line);
		ListIterator i = tokens.listIterator();
		if (!i.hasNext()) {
			throw new SyntaxException("Invalid function call", line);
		}
		
		// Obtain function name
		ITemplateExpression resFunction = compileToken((Token)i.next(), prefixes);
		ee.add(AdenineConstants.function, resFunction);
		
		// Obtain parameters
		ExistentialExpression eeLast = null;
		ExistentialExpression eeFirst = new ExistentialExpression();
		ExistentialExpression eeNext = eeFirst;
		while (i.hasNext()) {
			Token t = (Token)i.next();
			if ((k != null) && (t instanceof SemicolonToken)) {
				processAttributes(ee, i, k, line, prefixes);
				break;
			}
			
			// Check for named parameter
			if (i.hasNext()) {
				Token t2 = (Token)i.next();
				if ((t2 instanceof IdentifierToken) && (t2.m_token.equals("="))) {
					if (!i.hasNext()) {
						throw new SyntaxException("Incomplete named parameter", line);
					}
					Token t3 = (Token)i.next();
					
					// Enter named parameter
					ITemplateExpression te1 = processObject(t, prefixes, t.m_line);
					ITemplateExpression te2 = compileToken(t3, prefixes);
					ExistentialExpression eeParam = new ExistentialExpression();
					eeParam.add(AdenineConstants.parameterName, te1);
					eeParam.add(AdenineConstants.parameterVariable, te2);
					ee.add(AdenineConstants.namedParameter, eeParam);
					continue;
				}
				
				i.previous();
			}
			
			ITemplateExpression ee1 = compileToken(t, prefixes);
			eeNext.add(Constants.s_rdf_type, Constants.s_daml_List);
			eeNext.add(Constants.s_daml_first, ee1);
			
			if (eeLast != null) {
				eeLast.add(Constants.s_daml_rest, eeNext);
			}
			eeLast = eeNext;
			eeNext = new ExistentialExpression();
		}
		
		if (eeLast == null) {
			ee.add(AdenineConstants.PARAMETERS, Constants.s_daml_nil);
		} else {
			ee.add(AdenineConstants.PARAMETERS, eeFirst);
			eeLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		}
		
		return ee;
	}
	
	ExistentialExpression compileList(int line, ArrayList tokens, HashMap prefixes) throws AdenineException, RDFException {
		ExistentialExpression ee = generateInstruction(AdenineConstants.FunctionCall, line);

		// Hard code function name
		ITemplateExpression resFunction = compileIdentifier("List", line);
		ee.add(AdenineConstants.function, resFunction);
		
		// Obtain parameters
		ExistentialExpression eeLast = null;
		ExistentialExpression eeFirst = new ExistentialExpression();
		ExistentialExpression eeNext = eeFirst;
		Iterator i = tokens.iterator();
		while (i.hasNext()) {
			Token t = (Token)i.next();
			if (t instanceof SemicolonToken) {
				throw new SyntaxException("; invalid here", line);
			}
			
			ITemplateExpression ee1 = compileToken(t, prefixes);
			eeNext.add(Constants.s_rdf_type, Constants.s_daml_List);
			eeNext.add(Constants.s_daml_first, ee1);
			
			if (eeLast != null) {
				eeLast.add(Constants.s_daml_rest, eeNext);
			}
			eeLast = eeNext;
			eeNext = new ExistentialExpression();
		}
		
		if (eeLast == null) {
			ee.add(AdenineConstants.PARAMETERS, Constants.s_daml_nil);
		} else {
			ee.add(AdenineConstants.PARAMETERS, eeFirst);
			eeLast.add(Constants.s_daml_rest, Constants.s_daml_nil);
		}
		
		return ee;
	}
	
	boolean isURIIdentifier(String str) {
		return (str.indexOf(":") != -1) || (str.indexOf("?") == 0);
	}
	
	public Resource identifierToResource(int line, String str, HashMap prefixes) throws AdenineException {
		if ((str.length() >= 2) && (str.charAt(0) == '?')) {
			return new Resource(Constants.s_wildcard_namespace + str.substring(1));
		}
		
		if (str.equals("^")) {
			String s = (String)prefixes.get("@base");
			if (s == null) {
				throw new SyntaxException("Cannot use ^ without declaring a @base", line);
			}
			return new Resource(s);
		}
		
		int i = str.indexOf(":");
		if (i == -1) {
			throw new SyntaxException("Identifier must be of the form ?name or prefix:name: " + str, line);
		}
		
		String prefix = str.substring(0, i);
		String base = (String)prefixes.get(prefix);
		if (base == null) {
			throw new SyntaxException("Unknown prefix: " + prefix, line);
		}
		
		return new Resource(base + str.substring(i + 1));
	}
	
	ITemplateExpression compileObject(Iterator i, HashMap prefixes, int line) throws AdenineException, RDFException {
		if (!i.hasNext()) {
			throw new SyntaxException("Unexpected EOF", line);
		}

		Token t3 = (Token)i.next();
		return compileToken(t3, prefixes);
	}
	
	ITemplateExpression[] compilePredicateObject(Iterator i, HashMap prefixes, int line) throws AdenineException, RDFException {
		if (!i.hasNext()) {
			throw new SyntaxException("Unexpected EOF", line);
		}

		ITemplateExpression resPredicate = null;
		Token t2 = (Token)i.next();
		resPredicate = compileToken(t2, prefixes);
		
		ITemplateExpression[] a = new ITemplateExpression[2];
		a[0] = resPredicate;
		a[1] = compileObject(i, prefixes, line);
		
		return a;
	}
	
	ITemplateExpression[] processPredicateObject(Iterator i, HashMap prefixes, int line) throws AdenineException, RDFException {
		if (!i.hasNext()) {
			throw new SyntaxException("Unexpected EOF", line);
		}

		ITemplateExpression resPredicate = null;
		Token t2 = (Token)i.next();
		if (t2 instanceof IdentifierToken) {
			resPredicate = new ResourceExpression(identifierToResource(t2.m_line, t2.m_token, prefixes));
		} else if (t2 instanceof SemicolonToken) {
			throw new SyntaxException("; cannot be used as predicate", t2.m_line);
		} else if (t2 instanceof URIToken) {
			resPredicate = new ResourceExpression(t2.m_token);
		} else if (t2 instanceof ParenthesizedToken) {
			resPredicate = processList(t2.m_line, ((ParenthesizedToken)t2).m_tokens.iterator(), prefixes);
		} else if (t2 instanceof LiteralToken) {
			throw new SyntaxException("Predicate cannot be literal", t2.m_line);
		} else if (t2 instanceof BracedToken) {
			BracedToken bt = (BracedToken)t2;
			if (!bt.m_dollar) {
				throw new SyntaxException("{} cannot be used in this context", t2.m_line);
			}
			resPredicate = processDollarBrace(t2.m_line, bt.m_tokens.iterator(), prefixes);
		} else {
			throw new SyntaxException("Invalid token", t2.m_line);
		}
		
		ITemplateExpression[] a = new ITemplateExpression[2];
		a[0] = resPredicate;
		if (!i.hasNext()) {
			throw new SyntaxException("predicate missing object", t2.m_line);
		}
		a[1] = processObject((Token) i.next(), prefixes, t2.m_line);
		
		return a;
	}
	
	public ITemplateExpression compileToken(Token token, HashMap prefixes) throws AdenineException, RDFException {
		try {
			if (token instanceof URIToken) {
				return compileResource(new Resource(token.m_token));
			} else if (token instanceof IdentifierToken) {
				if (isURIIdentifier(token.m_token)) {
					return compileResource(identifierToResource(token.m_line, token.m_token,prefixes));
				} else {
					return compileIdentifier(token.m_token, token.m_line);
				}
			} else if (token instanceof LiteralToken) {
				return compileLiteral(token.m_token);
			} else if (token instanceof StringToken) {
				return compileString(token.m_token);
			} else if (token instanceof ParenthesizedToken) {
				ParenthesizedToken pt = (ParenthesizedToken)token;
				if (pt.m_prefix == '@') {
					return compileList(token.m_line, pt.m_tokens, prefixes);
				} else {
					return compileFunctionCall(token.m_line, pt.m_tokens, null, prefixes);
				}
			} else if (token instanceof BackQuoteToken) {
				// Generate backquote directive
				BackQuoteToken bqt = (BackQuoteToken)token;
				BackQuoteExpression bqe = new BackQuoteExpression(compileToken(bqt.m_token, prefixes), token.m_line);
				return bqe;
			} else if (token instanceof IndexToken) {
				// Generate index directive
				IndexToken it = (IndexToken)token;
				ExistentialExpression ee = new ExistentialExpression();
				ee.add(Constants.s_rdf_type, AdenineConstants.Index);
				ee.add(AdenineConstants.base, compileToken(it.m_base, prefixes));
				ee.add(AdenineConstants.index, compileToken(it.m_index, prefixes));
				return ee;
			} else if (token instanceof DereferencementToken) {
				// Generate dereferencement directive
				DereferencementToken it = (DereferencementToken)token;
				ExistentialExpression ee = new ExistentialExpression();
				ee.add(Constants.s_rdf_type, AdenineConstants.Dereferencement);
				ee.add(AdenineConstants.base, compileToken(it.m_base, prefixes));
				ee.add(AdenineConstants.member, compileToken(it.m_member, prefixes));
				return ee;
			} else if (token instanceof BracedToken) {
				BracedToken bt = (BracedToken)token;
				if (bt.m_dollar) {
					return compileDollarBrace(token.m_line, bt.m_tokens.iterator(), prefixes);
				} else if (bt.m_percent) {
					return compileQuery((BracedToken)bt, prefixes, token.m_line);
				} else {
					// Generate RDF container constructor
					return compileModel((BracedToken)bt, prefixes, token.m_line);
				}
			} else {
				throw new SyntaxException("Unexpected token: " + token, token == null ? -1 : token.m_line);
			}
		} catch (AdenineException ae) {
			if (ae.m_line == -1 && token != null) {
				ae.m_line = token.m_line;
			}
			throw ae;
		}
	}
	
	public ExistentialExpression compileIdentifier(String str, int line) throws RDFException {
		ExistentialExpression ee = generateInstruction(AdenineConstants.Identifier, line);
		ee.add(AdenineConstants.name, new LiteralExpression(str));
		return ee;
	}
	
	ExistentialExpression compileLiteral(String str) {
		ExistentialExpression ee = new ExistentialExpression();
		ee.add(Constants.s_rdf_type, AdenineConstants.Literal);
		ee.add(AdenineConstants.literal, new LiteralExpression(str));
		return ee;
	}
	
	ExistentialExpression compileString(String str) {
		ExistentialExpression ee = new ExistentialExpression();
		ee.add(Constants.s_rdf_type, AdenineConstants.String);
		ee.add(AdenineConstants.string, new LiteralExpression(str));
		return ee;
	}
	
	ExistentialExpression compileResource(Resource res) {
		ExistentialExpression ee = new ExistentialExpression();
		ee.add(Constants.s_rdf_type, AdenineConstants.Resource);
		ee.add(AdenineConstants.resource, res);
		return ee;
	}
	
	ExistentialExpression compileStatement(ITemplateExpression subject, ITemplateExpression predicate, ITemplateExpression object) throws AdenineException, RDFException {
		ExistentialExpression ee = new ExistentialExpression();
		ee.add(Constants.s_rdf_type, AdenineConstants.Statement);
		if (subject != null) {
			ee.add(AdenineConstants.subject, subject);
		}
		ee.add(AdenineConstants.predicate, predicate);
		ee.add(AdenineConstants.object, object);
		return ee;
	}
	
	ExistentialExpression compileDollarBrace(int line, Iterator i, HashMap prefixes) throws AdenineException, RDFException {
		ExistentialExpression ee = generateInstruction(AdenineConstants.BNode, line);
		while (i.hasNext()) {
			ITemplateExpression[] a = compilePredicateObject(i, prefixes, line);
			ExistentialExpression r = compileStatement(null, a[0], a[1]);
			ee.add(AdenineConstants.statement, r);

			if (i.hasNext()) {
				Token t = (Token)i.next();
				if (!(t instanceof SemicolonToken)) {
					throw new SyntaxException("Semicolon expected", line);
				}
			}
		}
		return ee;
	}
	
	ITemplateExpression compileModel(BracedToken token, HashMap prefixes, int line) throws AdenineException, RDFException {
		ExistentialExpression res = generateInstruction(AdenineConstants.Model, line);
		ITemplateExpression lastRes = null;
		Iterator i = token.m_tokens.iterator();
		while (i.hasNext()) {
			ITemplateExpression resSubject = null;
			Token t1 = (Token)i.next();
			if (t1 instanceof SemicolonToken) {
				if (lastRes != null) {
					resSubject = lastRes;
				} else {
					throw new SyntaxException("Misplaced ;", t1.m_line);
				}
			} else {
				resSubject = compileToken(t1, prefixes);
			}
			
			lastRes = resSubject;
			
			if (!i.hasNext()) {
				s_logCategory.warn("Extraneous semicolon on line " + line);
				break;
				//throw new SyntaxException("Unexpected EOF", line);
			}

			ITemplateExpression[] rest = compilePredicateObject(i, prefixes, line);
			ITemplateExpression resStatement = compileStatement(resSubject, rest[0], rest[1]);
			res.add(AdenineConstants.statement, resStatement);
		}
		return res;
	}
	
	ITemplateExpression compileQuery(BracedToken token, HashMap prefixes, int line) throws AdenineException, RDFException {
		ExistentialExpression res = generateInstruction(AdenineConstants.Query, line);
		Iterator i = token.m_tokens.iterator();
		ArrayList currentCondition = new ArrayList();
		ArrayList conditions = new ArrayList();
		while (i.hasNext()) {
			Token t1 = (Token)i.next();
			if ((t1 instanceof IdentifierToken) && t1.m_token.equals(",")) {
				if (currentCondition.isEmpty()) {
					throw new SyntaxException("Misplaced ,", t1.m_line);
				} else {
					conditions.add(makeList(currentCondition.iterator()));
					currentCondition = new ArrayList();
				}
			} else {
				currentCondition.add(compileToken(t1, prefixes));
			}
		}

		if (!currentCondition.isEmpty()) {
			conditions.add(makeList(currentCondition.iterator()));
			currentCondition = new ArrayList();
		}

		res.add(AdenineConstants.queryConditions, makeList(conditions.iterator()));
		return res;
	}
	
	void compileMethod(int line, ListIterator i, Iterator k, HashMap prefixes, HashMap precompiledMethodData) throws AdenineException, RDFException {
		IRDFContainer source;
		IRDFContainer precompileStorage = null;
		
		if (m_precompile) {
			precompileStorage = new LocalRDFContainer();
			source = precompileStorage;
		} else {
			source = m_target;
		}

		// Parse function name
		Token t = (Token)i.next();
		Resource resName;
		if (t instanceof IdentifierToken) {
			resName = identifierToResource(line, t.m_token, prefixes);
		} else if (t instanceof URIToken) {
			resName = new Resource(t.m_token);
		} else {
			throw new SyntaxException("Token not appropriate in service name specification: " + t, line);
		}
		
		source.add(new Statement(resName, Constants.s_rdf_type, AdenineConstants.Method));
		String base = (String)prefixes.get("@base");
		if (base != null) {
			Resource resBase = new Resource(base);
			m_target.add(new Statement(resName, Constants.s_rdfs_isDefinedBy, resBase));
		}
		m_target.add(new Statement(resName, AdenineConstants.compileTime, new Literal(new Date().toString())));
		
		// Parse parameters
		Resource resLast = null;
		Resource resFirst = ((URIGenerator)prefixes.get("@urigenerator")).generateAnonymousResource();
		Resource resNew = resFirst;
		while (i.hasNext()) {
			t = (Token)i.next();
			if (t instanceof SemicolonToken) {
				ExistentialExpression ee = new ExistentialExpression(resName);
				processAttributes(ee, i, k, line, prefixes);
				ee.generate((URIGenerator)prefixes.get("@urigenerator"), m_target);
				break;
			} else if (t instanceof IdentifierToken) {
				// Check for named parameter
				if (i.hasNext()) {
					Token t2 = (Token)i.next();
					if ((t2 instanceof IdentifierToken) && (t2.m_token.equals("="))) {
						if (!i.hasNext()) {
							throw new SyntaxException("Incomplete named parameter", line);
						}
						Token t3 = (Token)i.next();
						
						// Enter named parameter
						ITemplateExpression te1 = processObject(t, prefixes, t.m_line);
						ITemplateExpression te2 = compileToken(t3, prefixes);
						ExistentialExpression eeParam = new ExistentialExpression();
						eeParam.add(AdenineConstants.parameterName, te1);
						eeParam.add(AdenineConstants.parameterVariable, te2);
						source.add(new Statement(resName, AdenineConstants.namedParameter, eeParam.generate((URIGenerator)prefixes.get("@urigenerator"), source)));
						continue;
					}
					
					i.previous();
				}

				if (isURIIdentifier(t.m_token)) {
					throw new SyntaxException("Parameter identifier cannot be a URI", line);
				}

				ExistentialExpression resIdent = compileIdentifier(t.m_token, t.m_line);
				source.add(new Statement(resNew, Constants.s_rdf_type, Constants.s_daml_List));
				source.add(new Statement(resNew, Constants.s_daml_first, resIdent.generate((URIGenerator)prefixes.get("@urigenerator"), source)));
				if (resLast != null) {
					source.add(new Statement(resLast, Constants.s_daml_rest, resNew));
				}
				resLast = resNew;
				resNew = ((URIGenerator)prefixes.get("@urigenerator")).generateAnonymousResource();
			} else {
				throw new SyntaxException("Token not appropriate in method specification: " + t, line);
			}
		}
		
		if (resLast == null) {
			source.add(new Statement(resName, AdenineConstants.PARAMETERS, Constants.s_daml_nil));
		} else {
			source.add(new Statement(resName, AdenineConstants.PARAMETERS, resFirst));
			source.add(new Statement(resLast, Constants.s_daml_rest, Constants.s_daml_nil));
		}
				
		// Parse block
		if (!k.hasNext()) {
			throw new SyntaxException("Expected method body", line + 1);
		}
		Object o = k.next();
		if (!(o instanceof Block)) {
			throw new SyntaxException("Expected method body", line + 1);
		}

		if (m_precompile) {
			m_target.add(precompileStorage);
		}
		
		ExistentialExpression resBlock = compileBlock((Block)o, prefixes);
		source.add(new Statement(resName, AdenineConstants.start, resBlock.generate((URIGenerator)prefixes.get("@urigenerator"), source)));
		
		if (m_precompile) {
			precompiledMethodData.put(resName, source);
		}
	}

	void compileLibrary(int line, Iterator i, Iterator k, HashMap prefixes) throws AdenineException, RDFException {
		IRDFContainer source = m_target;

		// Parse library name
		Token t = (Token)i.next();
		Resource resName;
		if (t instanceof IdentifierToken) {
			resName = identifierToResource(line, t.m_token, prefixes);
		} else if (t instanceof URIToken) {
			resName = new Resource(t.m_token);
		} else {
			throw new SyntaxException("Token not appropriate in library name specification: " + t, line);
		}
		
		source.add(new Statement(resName, Constants.s_rdf_type, AdenineConstants.LIBRARY));
		
		// Parse block
		Object o = k.next();
		if (!(o instanceof Block)) {
			throw new SyntaxException("Expected library body", line + 1);
		}
		
		ExistentialExpression resBlock = compileBlock((Block)o, prefixes);
		source.add(new Statement(resName, AdenineConstants.start, resBlock.generate((URIGenerator)prefixes.get("@urigenerator"), source)));
	}

	Resource compileMain(int line, Iterator i, Iterator k, HashMap prefixes) throws AdenineException, RDFException {
		IRDFContainer source = m_target;

		Resource resName = ((URIGenerator)prefixes.get("@urigenerator")).generateAnonymousResource();
		
		// Parse parameters
		Resource resLast = null;
		Resource resFirst = ((URIGenerator)prefixes.get("@urigenerator")).generateAnonymousResource();
		Resource resNew = resFirst;
		while (i.hasNext()) {
			Token t = (Token)i.next();
			if (t instanceof SemicolonToken) {
				ExistentialExpression ee = new ExistentialExpression(resName);
				processAttributes(ee, i, k, line, prefixes);
				ee.generate((URIGenerator)prefixes.get("@urigenerator"), m_target);
				break;
			} else if (t instanceof IdentifierToken) {
				if (isURIIdentifier(t.m_token)) {
					throw new SyntaxException("Parameter identifier cannot be a URI", line);
				}

				ExistentialExpression resIdent = compileIdentifier(t.m_token, t.m_line);
				source.add(new Statement(resNew, Constants.s_rdf_type, Constants.s_daml_List));
				source.add(new Statement(resNew, Constants.s_daml_first, resIdent.generate((URIGenerator)prefixes.get("@urigenerator"), source)));
				if (resLast != null) {
					source.add(new Statement(resLast, Constants.s_daml_rest, resNew));
				}
				resLast = resNew;
				resNew = ((URIGenerator)prefixes.get("@urigenerator")).generateAnonymousResource();
			} else {
				throw new SyntaxException("Token not appropriate in main method specification: " + t, line);
			}
		}
		
		if (resLast == null) {
			source.add(new Statement(resName, AdenineConstants.PARAMETERS, Constants.s_daml_nil));
		} else {
			source.add(new Statement(resName, AdenineConstants.PARAMETERS, resFirst));
			source.add(new Statement(resLast, Constants.s_daml_rest, Constants.s_daml_nil));
		}
				
		// Parse block
		Object o = k.next();
		if (!(o instanceof Block)) {
			throw new SyntaxException("Expected main body", line + 1);
		}
		
		ExistentialExpression resBlock = compileBlock((Block)o, prefixes);
		source.add(new Statement(resName, AdenineConstants.start, resBlock.generate((URIGenerator)prefixes.get("@urigenerator"), source)));
		
		return resName;
	}

	Resource compileTopLevel(HashMap prefixes, Block b) throws AdenineException, RDFException {
		HashMap precompiledMethodData = new HashMap();
		IRDFContainer source = m_target;
		Iterator k = b.m_items.iterator();
		Resource lastRes = null;
		Resource mainRes = null;
		while (k.hasNext()) {
			Object o = k.next();
			if (o instanceof Block) {
				throw new SyntaxException("Block not valid here", ((Block)o).m_startline);
			}
			Line line = (Line)o;
			ListIterator i = line.m_tokens.listIterator();
			while (i.hasNext()) {
				Resource resSubject = null;
				Token t1 = (Token)i.next();
				if (t1 instanceof IdentifierToken) {
					if (t1.m_token.equals("method")) {
						// Compile method
						compileMethod(t1.m_line, i, k, prefixes, precompiledMethodData);
						lastRes = null;
						break;
					} else if (t1.m_token.equals("library")) {
						// Compile library
						compileLibrary(t1.m_line, i, k, prefixes);
						lastRes = null;
						break;
					} else if (t1.m_token.equals("add")) {
						// Compile add
						Token t2 = (Token)i.next();
						if (t2 instanceof BracedToken) {
							compileAdd(((BracedToken)t2).m_tokens.iterator(), prefixes);
						} else {
							throw new SyntaxException("add requires a model parameter", t1.m_line);
						}
						lastRes = null;
						break;
					} else if (t1.m_token.equals("main")) {
						if (mainRes != null) {
							throw new SyntaxException("main service already defined", t1.m_line);
						}
						
						// Compile main
						mainRes = compileMain(t1.m_line, i, k, prefixes);
						lastRes = null;
						break;
					} else if (t1.m_token.equals("@base")) {
						URIToken t3 = (URIToken)i.next();
						prefixes.put("@base", t3.m_token);
						prefixes.put("", t3.m_token + ":");
						prefixes.put("@urigenerator", new URIGenerator(t3.m_token));
						lastRes = null;
						continue;
					} else if (t1.m_token.equals("@prefix")) {
						// Process prefix
						processPrefix(t1.m_line, i, prefixes);
						lastRes = null;
						continue;
					}
					
					resSubject = identifierToResource(t1.m_line, t1.m_token, prefixes);
				} else if (t1 instanceof SemicolonToken) {
					if (lastRes != null) {
						resSubject = lastRes;
					} else {
						throw new SyntaxException("Misplaced ;", t1.m_line);
					}
				} else if (t1 instanceof URIToken) {
					resSubject = new Resource(t1.m_token);
				} else if (t1 instanceof ParenthesizedToken) {
					resSubject = (Resource)processList(t1.m_line, ((ParenthesizedToken)t1).m_tokens.iterator(), prefixes).generate((URIGenerator)prefixes.get("@urigenerator"), source);
				} else if (t1 instanceof LiteralToken) {
					throw new SyntaxException("Subject cannot be literal", t1.m_line);
				} else if (t1 instanceof BracedToken) {
					BracedToken bt = (BracedToken)t1;
					if (!bt.m_dollar) {
						throw new SyntaxException("{} cannot be used in this context", t1.m_line);
					}
					resSubject = (Resource)processDollarBrace(t1.m_line, bt.m_tokens.iterator(), prefixes).generate((URIGenerator)prefixes.get("@urigenerator"), source);
				} else {
					throw new SyntaxException("Invalid token", t1.m_line);
				}
				
				lastRes = resSubject;
				
				if (!i.hasNext()) {
					s_logCategory.warn("Extraneous semicolon on line " + line);
					break;
					//throw new SyntaxException("Unexpected EOF");
				}
	
				ITemplateExpression[] rest = processPredicateObject(i, prefixes, line.m_lineno);
				source.add(new Statement(resSubject, (Resource)rest[0].generate((URIGenerator)prefixes.get("@urigenerator"), source), rest[1].generate((URIGenerator)prefixes.get("@urigenerator"), source)));
			}
		}

		if (m_precompile) {
			// Precompile methods
			Iterator j = precompiledMethodData.keySet().iterator();
			ArrayList methodsToCompile = new ArrayList();
			LocalRDFContainer tempStorage = new LocalRDFContainer();
			tempStorage.add(m_tempStorage);
			while (j.hasNext()) {
				// Attempt to precompile
				Object key = j.next();
				methodsToCompile.add(key);
				tempStorage.add((IRDFContainer) precompiledMethodData.get(key));
			}
			
			m_interpreter = new Interpreter(tempStorage);
			Collection successfulMethods = m_interpreter.compileMethodsToJava(methodsToCompile, m_outPath);
			j = precompiledMethodData.keySet().iterator();
			while (j.hasNext()) {			
				Resource resName = (Resource) j.next();
				if (successfulMethods.contains(resName)) {
					m_target.add(new Statement(resName, Constants.s_haystack_JavaClass, tempStorage.extract(resName, Constants.s_haystack_JavaClass, null)));
					m_target.add(new Statement(resName, AdenineConstants.precompile, new Literal("true")));
					m_target.add(new Statement(resName, AdenineConstants.preload, new Literal("true")));
					s_logCategory.info(resName + " precompiled");
				} else {
					m_target.add((IRDFContainer) precompiledMethodData.get(resName));
					s_logCategory.info(resName + " could not be precompiled");
				}
				m_target.add(new Statement(resName, PRECOMPILE_TIME, new Literal(new Date().toString())));
			}
		}
		
		return mainRes;
	}

	void compileAdd(Iterator i, HashMap prefixes) throws AdenineException, RDFException {
		IRDFContainer source = m_target;
		Resource lastRes = null;
		while (i.hasNext()) {
			Resource resSubject = null;
			Token t1 = (Token)i.next();
			if (t1 instanceof IdentifierToken) {
				resSubject = identifierToResource(t1.m_line, t1.m_token, prefixes);
			} else if (t1 instanceof SemicolonToken) {
				if (lastRes != null) {
					resSubject = lastRes;
				} else {
					throw new SyntaxException("Misplaced ;", t1.m_line);
				}
			} else if (t1 instanceof URIToken) {
				resSubject = new Resource(t1.m_token);
			} else if (t1 instanceof ParenthesizedToken) {
				resSubject = (Resource)processList(t1.m_line, ((ParenthesizedToken)t1).m_tokens.iterator(), prefixes).generate((URIGenerator)prefixes.get("@urigenerator"), source);
			} else if (t1 instanceof LiteralToken) {
				throw new SyntaxException("Subject cannot be literal", t1.m_line);
			} else if (t1 instanceof BracedToken) {
				BracedToken bt = (BracedToken)t1;
				if (!bt.m_dollar) {
					throw new SyntaxException("{} cannot be used in this context", t1.m_line);
				}
				resSubject = (Resource)processDollarBrace(t1.m_line, bt.m_tokens.iterator(), prefixes).generate((URIGenerator)prefixes.get("@urigenerator"), source);
			} else {
				throw new SyntaxException("Invalid token", t1.m_line);
			}
			
			lastRes = resSubject;
			
			if (!i.hasNext()) {
				s_logCategory.warn("Extraneous semicolon on line " + t1.m_line);
				break;
				//throw new SyntaxException("Unexpected EOF", t1.m_line);
			}

			ITemplateExpression[] rest = processPredicateObject(i, prefixes, t1.m_line);
			source.add(new Statement(resSubject, (Resource)rest[0].generate((URIGenerator)prefixes.get("@urigenerator"), source), rest[1].generate((URIGenerator)prefixes.get("@urigenerator"), source)));
		}
	}
}
