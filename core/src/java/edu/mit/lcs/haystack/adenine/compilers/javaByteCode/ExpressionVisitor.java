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

import org.apache.bcel.generic.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

/**
 * @author David Huynh
 */
public class ExpressionVisitor extends ParserVisitorBase2 implements IExpressionVisitor {
	public ExpressionVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
		super(visitor, codeFrame);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onDereference(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public ISubExpressionVisitor onDereference(SymbolToken periodT) {
		return new SubExpressionVisitor(m_topLevelVisitor, m_codeFrame) {
			boolean m_memberIsIdentifier = false;
			
			public void end(Location endLocation) {
				super.end(endLocation);
				setDereference(m_startLocation, m_memberIsIdentifier);
			}

			protected void onIdentifier(String identifier, GenericToken token) {
				m_memberIsIdentifier = true;
				
				m_codeFrame.getMethodGen().addLineNumber(
					m_codeFrame.getInstructionList().append(new PUSH(m_codeFrame.getConstantPoolGen(), identifier)),
					token.getSpan().getStart().getTrueLine());
			}
		};
	}
	void setDereference(Location location, boolean memberIsIdentifier) {
		/*
		 * The stack should look like this:
		 * 
		 * 		member	<-- top
		 * 		base
		 * 		...
		 */
		
		InstructionList iList = m_codeFrame.getInstructionList();
		
		m_codeFrame.getMethodGen().addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				"edu.mit.lcs.haystack.adenine.instructions.DereferencementInstruction",
				"doDereferencement",
				Type.OBJECT,
				new Type[] { Type.OBJECT, Type.OBJECT },
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			location.getTrueLine());
	}
	
	class InnerExpressionVisitor extends ExpressionVisitor {
		public InnerExpressionVisitor(TopLevelVisitor visitor, CodeFrame codeFrame) {
			super(visitor, codeFrame);
		}
		public void end(Location endLocation) {
			super.end(endLocation);
			ExpressionVisitor.this.setIndexInstruction(endLocation);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLeftBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onLeftBracket(SymbolToken leftBracketT) {
		return new InnerExpressionVisitor(m_topLevelVisitor, m_codeFrame);
	}
	void setIndexInstruction(Location location) {
		/*
		 * The stack should look like this:
		 * 
		 * 		index	<-- top
		 * 		base
		 * 		...
		 */

		InstructionList iList = m_codeFrame.getInstructionList();
		
		m_codeFrame.getMethodGen().addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				"edu.mit.lcs.haystack.adenine.instructions.IndexInstruction",
				"doIndex",
				Type.OBJECT,
				new Type[] { Type.OBJECT, Type.OBJECT },
				org.apache.bcel.Constants.INVOKESTATIC
			)),
			location.getTrueLine());
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
		return new SubExpressionVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onAnonymousModel(Location)
	 */
	public IAnonymousModelVisitor onAnonymousModel(Location location) {
		return new AnonymousModelVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onApply(Location)
	 */
	public IApplyVisitor onApply(Location location) {
		return new ApplyVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onAskModel(Location)
	 */
	public IAskModelVisitor onAskModel(Location location) {
		return new AskModelVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onFloat(edu.mit.lcs.haystack.adenine.tokenizer.FloatToken)
	 */
	public void onFloat(FloatToken floatToken) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = floatToken.getSpan().getStart().getTrueLine();
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeDouble)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(new PUSH(
				m_codeFrame.getConstantPoolGen(), 
				(double) floatToken.getFloat())),
			line);
			
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeDouble.getClassName(),
				"<init>",
				Type.VOID,
				new Type[] { Type.DOUBLE },
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onInteger(edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken)
	 */
	public void onInteger(IntegerToken integerToken) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = integerToken.getSpan().getStart().getTrueLine();
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeInteger)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(new PUSH(
				m_codeFrame.getConstantPoolGen(), 
				integerToken.getInteger())),
			line);
			
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeInteger.getClassName(),
				"<init>",
				Type.VOID,
				new Type[] { Type.INT },
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onList(Location)
	 */
	public IListVisitor onList(Location location) {
		return new ListVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onLiteral(edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken)
	 */
	public void onLiteral(LiteralToken literalToken) {
		InstructionList iList = m_codeFrame.getInstructionList();
		MethodGen		mg = m_codeFrame.getMethodGen();
		int			line = literalToken.getSpan().getStart().getTrueLine();
		
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createNew(JavaByteCodeUtilities.s_typeLiteral)),
			line);
		mg.addLineNumber(
			iList.append(InstructionConstants.DUP),
			line);

		mg.addLineNumber(
			iList.append(new PUSH(
				m_codeFrame.getConstantPoolGen(), 
				literalToken.getContent())),
			line);
			
		mg.addLineNumber(
			iList.append(m_codeFrame.getInstructionFactory().createInvoke(
				JavaByteCodeUtilities.s_typeLiteral.getClassName(),
				"<init>",
				Type.VOID,
				new Type[] { Type.STRING },
				org.apache.bcel.Constants.INVOKESPECIAL
			)),
			line);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onModel(Location)
	 */
	public IModelVisitor onModel(Location location) {
		return new ModelVisitor(m_topLevelVisitor, m_codeFrame);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onString(edu.mit.lcs.haystack.adenine.tokenizer.StringToken)
	 */
	public void onString(StringToken stringToken) {
		InstructionList iList = m_codeFrame.getInstructionList();

		m_codeFrame.getMethodGen().addLineNumber(		
			iList.append(new PUSH(
				m_codeFrame.getConstantPoolGen(), 
				stringToken.getContent())),
			stringToken.getSpan().getStart().getTrueLine());
	}
}
