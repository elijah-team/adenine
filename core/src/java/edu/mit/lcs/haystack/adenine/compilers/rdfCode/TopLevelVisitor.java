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

package edu.mit.lcs.haystack.adenine.compilers.rdfCode;

import java.util.*;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.constructs.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.*;

/**
 * @author David Huynh
 */
public class TopLevelVisitor extends TopLevelVisitorBase {
	protected Resource m_package;
	
	public ConstructVisitorBase getConstruct(String s, List instructionList) {
		if (s.equals("method")) {
			return new MethodVisitor(this, instructionList);
		} else if (s.equals("=")) {
			return new AssignmentVisitor(this, instructionList);
		} else if (s.equals("break")) {
			return new BreakVisitor(this, instructionList);
		} else if (s.equals("block")) {
			return new BlockVisitor(this, instructionList);
		} else if (s.equals("call")) {
			return new CallVisitor(this, instructionList);
		} else if (s.equals("continue")) {
			return new ContinueVisitor(this, instructionList);
		} else if (s.equals("for")) {
			return new ForVisitor(this, instructionList);
		} else if (s.equals("function")) {
			return new FunctionVisitor(this, instructionList);
		} else if (s.equals("if")) {
			return new IfVisitor(this, instructionList);
		} else if (s.equals("importjava")) {
			return new ImportJavaVisitor(this, instructionList);
		} else if (s.equals("return")) {
			return new ReturnVisitor(this, instructionList);
		} else if (s.equals("skipBlock")) {
			return new SkipBlockVisitor(this, instructionList);
		} else if (s.equals("uniqueMethod")) {
			return new MethodVisitor(this, instructionList);
		} else if (s.equals("var")) {
			return new VarVisitor(this, instructionList);
		} else if (s.equals("while")) {
			return new WhileVisitor(this, instructionList);
		} else if (s.equals("with")) {
			return new WithVisitor(this, instructionList);
		}
		return null;
	}
	
	public TopLevelVisitor(IParserVisitor visitor, Resource pakkage, IRDFContainer target) {
		super(visitor, target);
		m_package = pakkage;
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
		Resource r = resolveURI(token);
		
		if (r != null) {
			return generateResourceInstruction(r, token.getSpan());
		} else {
			onException(new SyntaxException("Unknown prefix " + token.getPrefix() + ":", token.getSpan()));
			return null;
		}
	}

	public Resource generateResourceInstruction(Resource resource, Span span) {
		Resource r = generateInstruction(AdenineConstants.Resource, span.getStart());
		
		try {
			m_target.add(new Statement(r, AdenineConstants.resource, resource));
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

class TopLevelMethodVisitor extends ParserVisitorBase implements IMethodVisitor {
	TopLevelVisitor 	m_topLevelVisitor;
	List				m_parameters = new LinkedList();
	InnerCodeBlockVisitor	m_innerBlockVisitor;
	TopLevelExpressionVisitor m_methodResourceVisitor;
	
	public TopLevelMethodVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor; 
	}

	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		if (getMethodResource() != null) {
			return new TopLevelAttributeVisitor(m_topLevelVisitor, getMethodResource());
		} else {
			return new NullAttributeVisitor(m_visitor);
		}
	}

	public ICodeBlockVisitor onBlock() {
		IRDFContainer 	target = m_topLevelVisitor.getTarget();
		try {
			target.add(new Statement(getMethodResource(), Constants.s_rdf_type, AdenineConstants.Method));
		} catch (RDFException e) {
			onException(e);
		}

		m_innerBlockVisitor = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_innerBlockVisitor;
	}

	public IExpressionVisitor onMethod(GenericToken methodKeyword) {
		return m_methodResourceVisitor = new TopLevelExpressionVisitor(m_topLevelVisitor, true);
	}

	public void onNamedParameter(
		ResourceToken name,
		SymbolToken equalT,
		GenericToken parameter) {

		if (getMethodResource() != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
			Resource		param = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
			
			try {
				target.add(new Statement(param, AdenineConstants.parameterName, m_topLevelVisitor.resolveURI(name)));
				target.add(new Statement(param, AdenineConstants.parameterVariable, m_topLevelVisitor.generateIdentifierInstruction(parameter)));
				
				target.add(new Statement(getMethodResource(), AdenineConstants.namedParameter, param));
			} catch (RDFException e) {
				onException(e);
			}
		}
	}

	public void onParameter(GenericToken parameter) {
		if (getMethodResource() != null) {
			m_parameters.add(m_topLevelVisitor.generateIdentifierInstruction(parameter));
		}
	}

	public void end(Location endLocation) {
		if (getMethodResource() != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
	
			try {
				target.add(new Statement(getMethodResource(), AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(m_parameters.iterator(), target)));
				
				Resource firstInstruction = (m_innerBlockVisitor != null) ? m_innerBlockVisitor.getFirstInstruction() : null;
				
				if (firstInstruction != null) {
					target.add(new Statement(getMethodResource(), AdenineConstants.start, firstInstruction));
				}
			} catch (RDFException e) {
				onException(e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#canBeAnonymous()
	 */
	public boolean canBeAnonymous() {
		return false;
	}

	Resource getMethodResource() {
		return m_methodResourceVisitor.getResource();
	}

}


class MainVisitor extends ParserVisitorBase implements IMainVisitor {
	TopLevelVisitor 	m_topLevelVisitor;
	Resource			m_methodResource;
	InnerCodeBlockVisitor	m_innerBlockVisitor;
	
	public MainVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor; 

		IRDFContainer target = m_topLevelVisitor.getTarget();
		try {
			m_methodResource = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
		
			target.add(new Statement(m_methodResource, Constants.s_rdf_type, AdenineConstants.Method));
		} catch (RDFException e) {
			onException(e);
		}
	}

	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		return new TopLevelAttributeVisitor(m_topLevelVisitor, m_methodResource);
	}

	public ICodeBlockVisitor onMain(GenericToken mainKeyword) {
		m_innerBlockVisitor = new InnerCodeBlockVisitor(m_topLevelVisitor);
		return m_innerBlockVisitor;
	}

	public void end(Location endLocation) {
		if (m_innerBlockVisitor != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
	
			try {
				Resource firstInstruction = (m_innerBlockVisitor != null) ? m_innerBlockVisitor.getFirstInstruction() : null;
				if (firstInstruction != null) {
					target.add(new Statement(m_methodResource, AdenineConstants.start, firstInstruction));
				}
				
				Resource pakkage = m_topLevelVisitor.getPackage();
				if (pakkage != null) {
					target.replace(pakkage, AdenineConstants.main, null, m_methodResource);
				}
			} catch (RDFException e) {
				onException(e);
			}
		}
	}
}
