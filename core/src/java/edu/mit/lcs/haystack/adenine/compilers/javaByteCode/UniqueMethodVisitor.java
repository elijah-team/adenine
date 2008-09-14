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
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;
import edu.mit.lcs.haystack.adenine.compilers.utils.TopLevelAttributeVisitor;
import edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.parser2.IAnonymousModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IApplyVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IAskModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ICodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IListVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.IParserVisitor;
import edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAnonymousModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullApplyVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAskModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullAttributeVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullCodeBlockVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullListVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullModelVisitor;
import edu.mit.lcs.haystack.adenine.parser2.NullSubExpressionVisitor;
import edu.mit.lcs.haystack.adenine.parser2.SyntaxException;
import edu.mit.lcs.haystack.adenine.tokenizer.FloatToken;
import edu.mit.lcs.haystack.adenine.tokenizer.GenericToken;
import edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken;
import edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Location;
import edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken;
import edu.mit.lcs.haystack.adenine.tokenizer.StringToken;
import edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken;
import edu.mit.lcs.haystack.adenine.tokenizer.Token;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author David Huynh
 */
public class UniqueMethodVisitor
	extends ConstructVisitorBase
	implements IMethodVisitor {

	Resource			m_methodResource;
	String				m_varName;
	String				m_className;

	List				m_parameters = new LinkedList();
	List				m_namedParameters = new LinkedList();
	List				m_namedParameterURIs = new LinkedList();

	CodeFrameWithMethodGen	m_innerCodeFrame;

	public UniqueMethodVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onMethod(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public IExpressionVisitor onMethod(GenericToken methodKeyword) {
		return new UniqueMethodExpressionVisitor(m_topLevelVisitor);
	}

	class UniqueMethodExpressionVisitor
		extends ParserVisitorBase
		implements IExpressionVisitor {

		public UniqueMethodExpressionVisitor(IParserVisitor visitor) { super(visitor); }
	
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onDereference()
		 */
		public ISubExpressionVisitor onDereference(SymbolToken periodT) {
			return new NullSubExpressionVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLeftBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
		 */
		public IExpressionVisitor onLeftBracket(SymbolToken leftBracketT) {
			onException(new SyntaxException("No dereference allowed here. Expected only 'add'", leftBracketT.getSpan()));
			return new NullExpressionVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onRightBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
		 */
		public void onRightBracket(SymbolToken rightBracketT) {
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onSubExpression(Location)
		 */
		public ISubExpressionVisitor onSubExpression(Location location) {
			return new ISubExpressionVisitor() {
				public void onIdentifier(
					SymbolToken backquoteT,
					Token identifier) {

					if (backquoteT != null) {
						UniqueMethodVisitor.this.onException(new SyntaxException("No backquote allowed here. Expected only 'add'", backquoteT.getSpan()));
					} else {
						m_varName = ((GenericToken) identifier).getContent();
						initMethod();
					}
				}

				public void onResource(ResourceToken resourceToken) {
					UniqueMethodVisitor.this.onException(new SyntaxException("Expected only 'add' here", resourceToken.getSpan()));
				}

				public void start(Location startLocation) {
				}

				public void end(Location endLocation) {
				}

				public void onException(Exception exception) {
					UniqueMethodVisitor.this.onException(exception);
				}
			};
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onApply(Location)
		 */
		public IApplyVisitor onApply(Location location) {
			onException(new SyntaxException("No application allowed here. Expected only 'add'", location));
			return new NullApplyVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onList(Location)
		 */
		public IListVisitor onList(Location location) {
			onException(new SyntaxException("No list allowed here. Expected only 'add'", location));
			return new NullListVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onModel(Location)
		 */
		public IModelVisitor onModel(Location location) {
			onException(new SyntaxException("No model allowed here. Expected only 'add'", location));
			return new NullModelVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onAnonymousModel(Location)
		 */
		public IAnonymousModelVisitor onAnonymousModel(Location location) {
			onException(new SyntaxException("No anonymous model allowed here. Expected only 'add'", location));
			return new NullAnonymousModelVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onAskModel(Location)
		 */
		public IAskModelVisitor onAskModel(Location location) {
			onException(new SyntaxException("No ask model allowed here. Expected only 'add'", location));
			return new NullAskModelVisitor(m_visitor);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLiteral(edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken)
		 */
		public void onLiteral(LiteralToken literalToken) {
			onException(new SyntaxException("No literal allowed here. Expected only 'add'", literalToken.getSpan()));
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onString(edu.mit.lcs.haystack.adenine.tokenizer.StringToken)
		 */
		public void onString(StringToken stringToken) {
			onException(new SyntaxException("No string allowed here. Expected only 'add'", stringToken.getSpan()));
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onInteger(edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken)
		 */
		public void onInteger(IntegerToken integerToken) {
			onException(new SyntaxException("No integer allowed here. Expected only 'add'", integerToken.getSpan()));
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onFloat(edu.mit.lcs.haystack.adenine.tokenizer.FloatToken)
		 */
		public void onFloat(FloatToken floatToken) {
			onException(new SyntaxException("No float allowed here. Expected only 'add'", floatToken.getSpan()));
		}
	}


	protected void initMethod() {
		m_methodResource = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
		
		if (m_varName != null && null == m_codeFrame.resolveVariableName(m_varName)) {
			m_codeFrame.addVariable(m_varName);
		}

		if (m_methodResource != null) {
			m_className = Interpreter.filterSymbols(m_methodResource.getURI());
		
			ClassGen cg = new ClassGen(
				m_className,	 																// class name
				"edu.mit.lcs.haystack.adenine.interpreter.CompiledMethod",						// extends, super class name
				m_topLevelVisitor.getCompiler().getSourceFile() + "<anon:" + m_varName + ">",	// source file
				org.apache.bcel.Constants.ACC_PUBLIC |											// access flags 
				org.apache.bcel.Constants.ACC_SUPER,
				null
			);
		
			m_innerCodeFrame = new CodeFrameWithMethodGen("method" + m_startLocation.getTrueLine() + "_", cg);
			m_innerCodeFrame.m_methodGen = new MethodGen(
				org.apache.bcel.Constants.ACC_PROTECTED,	// access flags
				JavaByteCodeUtilities.s_typeMessage,					// return type
				new Type[] {},								// argument types
				new String[] {},							// argument names
				"doInvoke",									// method name
				m_className,								// class name
				m_innerCodeFrame.getInstructionList(),
				m_innerCodeFrame.getConstantPoolGen()
			);

			IRDFContainer 	target = m_topLevelVisitor.getTarget();
			try {
				target.add(new Statement(m_methodResource, Constants.s_rdf_type, AdenineConstants.Method));
			} catch (RDFException e) {
				onException(e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onParameter(edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onParameter(GenericToken parameter) {
		if (m_methodResource != null) {
			m_parameters.add(parameter);
			m_innerCodeFrame.addVariable(parameter.getContent());
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onNamedParameter(edu.mit.lcs.haystack.adenine.tokenizer.ResourceToken, edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken, edu.mit.lcs.haystack.adenine.tokenizer.GenericToken)
	 */
	public void onNamedParameter(
		ResourceToken name,
		SymbolToken equalT,
		GenericToken parameter) {

		if (m_methodResource != null) {
			m_innerCodeFrame.addVariable(parameter.getContent());

			Resource resolvedName = m_topLevelVisitor.resolveURI(name);
		
			if (resolvedName != null) {
				m_namedParameters.add(parameter.getContent());
				m_namedParameterURIs.add(resolvedName);
			
				IRDFContainer 	target = m_topLevelVisitor.getTarget();
				Resource		param = m_topLevelVisitor.getURIGenerator().generateAnonymousResource();
			
				try {
					target.add(new Statement(param, AdenineConstants.parameterName, resolvedName));
					target.add(new Statement(param, AdenineConstants.parameterVariable, m_topLevelVisitor.generateIdentifierInstruction(parameter)));
				
					target.add(new Statement(m_methodResource, AdenineConstants.namedParameter, param));
				} catch (RDFException e) {
					onException(e);
				}
			} else {
				onException(new SyntaxException("Failed to resolve parameter name (undefined prefix " + name.getPrefix() + ")", name.getSpan()));
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#onBlock()
	 */
	public ICodeBlockVisitor onBlock() {
		if (m_methodResource != null) {
			return new InnerCodeBlockVisitor(m_topLevelVisitor, m_innerCodeFrame);
		} else {
			return new NullCodeBlockVisitor(m_visitor);
		}
	}

	public IAttributeVisitor onAttribute(SymbolToken semicolonT) {
		if (m_methodResource != null) {
			return new TopLevelAttributeVisitor(m_topLevelVisitor, m_methodResource);
		} else {
			return new NullAttributeVisitor(m_visitor);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.constructs.IMethodVisitor#canBeAnonymous()
	 */
	public boolean canBeAnonymous() {
		return true;
	}

	public void start(Location startLocation) {
		super.start(startLocation);
		m_topLevelVisitor.pushMethod();
	}
	
	public void end(Location endLocation) {
		Set 	backquotedIdentifiers = m_topLevelVisitor.popMethod();
		List	orderedBackquotedIdentifiers = new ArrayList(backquotedIdentifiers);
		
		if (m_methodResource != null && !m_innerCodeFrame.getError()) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
	
			try {
				List 	parameterIdentifierInstructions = new ArrayList();
				List	positionalParameters = new ArrayList();
				
				// Generate backquoted parameters
				Iterator i = orderedBackquotedIdentifiers.iterator();
				while (i.hasNext()) {
					String parameterName = (String) i.next();
					
					parameterIdentifierInstructions.add(m_topLevelVisitor.generateIdentifierInstruction(parameterName));
					positionalParameters.add(parameterName);
				}
				
				// Generate parameters
				i = m_parameters.iterator();
				while (i.hasNext()) {
					GenericToken 	token = (GenericToken) i.next();
					String			parameterName = token.getContent();
					
					positionalParameters.add(parameterName);
					parameterIdentifierInstructions.add(
						m_topLevelVisitor.generateIdentifierInstruction(parameterName));
				}
				target.add(new Statement(m_methodResource, AdenineConstants.PARAMETERS, ListUtilities.createDAMLList(parameterIdentifierInstructions.iterator(), target)));
				
				if (JavaByteCodeUtilities.precompileMethod(
						m_innerCodeFrame, 
						positionalParameters, 
						m_namedParameters, 
						m_namedParameterURIs, 
						m_topLevelVisitor,
						endLocation)) {
					
					target.add(new Statement(m_methodResource, Constants.s_haystack_JavaClass, new Literal(m_className)));
					target.add(new Statement(m_methodResource, AdenineConstants.preload, new Literal("true")));
					
					generateMethodConstructionCode(orderedBackquotedIdentifiers);
					
					JavaByteCodeUtilities.writeClass(m_innerCodeFrame.getClassGen(), m_topLevelVisitor.getCompiler());
				} else {
					onException(new AdenineException("Failed to precompile anonymous method at " + m_startLocation));
				}
			} catch (RDFException e) {
				onException(e);
			} catch (AdenineException e) {
				onException(e);
			}
		}
	}
	
	protected void generateMethodConstructionCode(List orderedBackquotedIdentifiers) {
		InstructionList	iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		Iterator 		i = orderedBackquotedIdentifiers.iterator();
		int			line = m_startLocation.getTrueLine();

		/*
		 * 	Construct list of backquoted values
		 */
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeArrayList)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeArrayList.getClassName(),
				"<init>",
				Type.VOID,
				Type.NO_ARGS,
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);

		while (i.hasNext()) {
			String name = (String) i.next();

			mg.addLineNumber(
				iList.append(InstructionConstants.DUP),
				line);
			
			m_codeFrame.generateVariableGet(name, iList, m_codeFrame.getMethodGen(), line);

			mg.addLineNumber(
				iList.append(m_codeFrame.getInstructionFactory().createInvoke(
					JavaByteCodeUtilities.s_typeList.getClassName(),
					"add",
					Type.BOOLEAN,
					new Type[] { Type.OBJECT },
					org.apache.bcel.Constants.INVOKEINTERFACE
				)),
				line);
			mg.addLineNumber(
				iList.append(InstructionConstants.POP),
				line);
		}
		
		/*
		 * 	Push wrapped method URI and dynamic environment
		 */
		mg.addLineNumber(
			iList.append(new PUSH(m_codeFrame.getConstantPoolGen(), m_methodResource.getURI())),
			line);
		
		mg.addLineNumber(
			iList.append(InstructionConstants.THIS),
			line);
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createGetField(
				m_codeFrame.getClassGen().getClassName(), 
				"__dynamicenvironment__", 
				JavaByteCodeUtilities.s_typeDynamicEnvironment)),
			line);

		/*
		 * 	Call makeAnonymousMethod
		 */		

		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				this.getClass().getName(),
				"makeAnonymousMethod",
				JavaByteCodeUtilities.s_typeResource,
				new Type[] { JavaByteCodeUtilities.s_typeList, Type.STRING, JavaByteCodeUtilities.s_typeDynamicEnvironment },
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			line);

		m_codeFrame.generateVariablePut(m_varName, iList, m_codeFrame.getMethodGen(), line);
	}
	
	static public Resource makeAnonymousMethod(
			List 				backquotedValues, 
			String 				wrappedMethodURI, 
			DynamicEnvironment 	denv) throws AdenineException {
				
		try {
			IRDFContainer target = denv.getTarget();

			Resource method = Utilities.generateUniqueResource();
			
			target.add(new Statement(method, Constants.s_rdf_type, AdenineConstants.Method));
			target.add(new Statement(method, Constants.s_haystack_JavaClass, new Literal(AnonymousCompiledMethod.class.getName())));
			target.add(new Statement(method, AdenineConstants.preload, new Literal("true")));
			target.add(new Statement(method, AdenineConstants.function, new Resource(wrappedMethodURI)));
			target.add(new Statement(method, AdenineConstants.BACKQUOTED_PARAMETERS, ListUtilities.createDAMLList(backquotedValues.iterator(), target)));
			
			return method;
		} catch (RDFException rdfe) {
			throw new AdenineException("RDF error", rdfe);
		}
	}
}
