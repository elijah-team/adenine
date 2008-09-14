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

package edu.mit.lcs.haystack.adenine.compilers.javaByteCode;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.compilers.utils.TopLevelVisitorBase;
import edu.mit.lcs.haystack.adenine.parser2.IConstructVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IParserVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullConstructVisitor;
import edu.mit.lcs.haystack.adenine.parser2.SyntaxException;
import edu.mit.lcs.haystack.adenine.tokenizer.FloatToken;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.IndentToken;
import edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.StringToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author David Huynh
 */
public class TopLevelVisitor extends TopLevelVisitorBase {
	protected Resource 				m_package;
	protected JavaByteCodeCompiler	m_compiler;
	
	public ConstructVisitorBase getConstruct(String s, CodeFrame codeFrame) {
		if (s.equals("method")) {
			return new MethodVisitor(this, codeFrame);
		} else if (s.equals("=")) {
			return new AssignmentVisitor(this, codeFrame);
		} else if (s.equals("block")) {
			return new BlockVisitor(this, codeFrame);
		} else if (s.equals("break")) {
			return new BreakVisitor(this, codeFrame);
		} else if (s.equals("call")) {
			return new CallVisitor(this, codeFrame);
		} else if (s.equals("continue")) {
			return new ContinueVisitor(this, codeFrame);
		} else if (s.equals("for")) {
			return new ForVisitor(this, codeFrame);
		} else if (s.equals("function")) {
			return new FunctionVisitor(this, codeFrame);
		} else if (s.equals("if")) {
			return new IfVisitor(this, codeFrame);
		} else if (s.equals("importjava")) {
			return new ImportJavaVisitor(this, codeFrame);
		} else if (s.equals("skipBlock")) {
			return new SkipBlockVisitor(this, codeFrame);
		} else if (s.equals("return")) {
			return new ReturnVisitor(this, codeFrame);
		} else if (s.equals("uniqueMethod")) {
			return new UniqueMethodVisitor(this, codeFrame);
		} else if (s.equals("var")) {
			return new VarVisitor(this, codeFrame);
		} else if (s.equals("while")) {
			return new WhileVisitor(this, codeFrame);
		} else if (s.equals("with")) {
			return new WithVisitor(this, codeFrame);
		}
		return null;
	}
	
	public TopLevelVisitor(IParserVisitor visitor, Resource pakkage, IRDFContainer target, JavaByteCodeCompiler compiler) {
		super(visitor, target);
		m_package = pakkage;
		m_compiler = compiler;
	}
	
	public JavaByteCodeCompiler getCompiler() {
		return m_compiler;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IBlockVisitor#onConstruct(Location, String)
	 */
	public IConstructVisitor onConstruct(Location location, String construct) {
		if (construct.equals("method")) {
			return new TopLevelMethodVisitor(this);
		} else if (construct.equals("main")) {
			return new MainVisitor(this);
		} else if (construct.equals("call")) {
			return new TopLevelCallVisitor(this, null);
		} else {
			return new NullConstructVisitor(this) {
				public void start(Location startLocation) {
					onException(new SyntaxException("Unrecognized construct at top level", startLocation));
				}
			};
		}
	}
	
	public Resource generateInstruction(Resource type, Location location) {
		Resource r = getURIGenerator().generateAnonymousResource();
		
		makeInstruction(r, type, location);
		
		return r;
	}

	public void makeInstruction(Resource instruction, Resource type, Location location) {
		try {
			m_target.add(new Statement(instruction, Constants.s_rdf_type, type));
			m_target.add(new Statement(instruction, AdenineConstants.line, new Literal(Integer.toString(location.getLine() + 1))));
		} catch (RDFException e) {
			onException(e);
		}
	}
	
	public Resource generateIdentifierInstruction(GenericToken token) {
		Resource r = generateInstruction(AdenineConstants.Identifier, token.getSpan().getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.name, new Literal(token.getContent())));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	public Resource generateIdentifierInstruction(String name) {
		Resource r = getURIGenerator().generateAnonymousResource();
		try {
			m_target.add(new Statement(r, Constants.s_rdf_type, AdenineConstants.Identifier));
			m_target.add(new Statement(r, AdenineConstants.name, new Literal(name)));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	public Resource generateLiteralInstruction(Token t, String s) {
		Resource r = generateInstruction(AdenineConstants.Literal, t.getSpan().getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.literal, new Literal(s)));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	public Resource generateResourceInstruction(ResourceToken token) {
		Resource r = generateInstruction(AdenineConstants.Resource, token.getSpan().getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.resource, resolveURI(token)));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	public Resource generateStringInstruction(StringToken token) {
		Resource r = generateInstruction(AdenineConstants.String, token.getSpan().getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.string, new Literal(token.getContent())));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	public Resource generateIntegerInstruction(IntegerToken token) {
		Resource r = generateInstruction(AdenineConstants.Identifier, token.getSpan().getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.name, new Literal(token.getContent())));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	public Resource generateFloatInstruction(FloatToken token) {
		Resource r = generateInstruction(AdenineConstants.Identifier, token.getSpan().getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.name, new Literal(token.getContent())));
		} catch (RDFException e) {
			onException(e);
		}
		return r;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IBlockVisitor#setFirstIndent(edu.mit.lcs.haystack.adenine.tokenizer.IndentToken)
	 */
	public void setFirstIndent(IndentToken ident) {
	}
	
	public Resource getPackage() {
		return m_package;
	}
	
	/*
	 * Handles backquoted identifiers
	 */
	protected LinkedList m_backquotedIdentifierLists = new LinkedList();
	
	public void pushMethod() {
		m_backquotedIdentifierLists.add(0, new HashSet());
	}
	
	public Set popMethod() {
		return (Set) m_backquotedIdentifierLists.remove(0);
	}
	
	public void addBackquotedIdentifier(String identifier) {
		Set set = (Set) m_backquotedIdentifierLists.get(0);
		
		set.add(identifier);
	}
}

