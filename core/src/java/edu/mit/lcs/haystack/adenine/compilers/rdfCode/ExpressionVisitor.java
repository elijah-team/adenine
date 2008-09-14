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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.ParserVisitorBase;

/**
 * @author David Huynh
 */
public class ExpressionVisitor extends ParserVisitorBase implements IExpressionVisitor {
	protected TopLevelVisitor	m_topLevelVisitor;
	protected Resource		m_instructionResource;
	
	public ExpressionVisitor(TopLevelVisitor visitor) {
		super(visitor);
		m_topLevelVisitor = visitor;
	}
	
	public Resource getInstructionResource() {
		return m_instructionResource;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onDereference(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public ISubExpressionVisitor onDereference(SymbolToken periodT) {
		if (m_instructionResource != null) {
			return new SubExpressionVisitor(m_topLevelVisitor) {
				Location m_location;
				public void end(Location endLocation) {
					super.end(endLocation);
					setDereference(this.m_instructionResource, m_location);
				}
				public SubExpressionVisitor init(Location location) {
					m_location = location;
					return this;
				}
			}.init(periodT.getSpan().getStart());
		} else {
			return new NullSubExpressionVisitor(m_topLevelVisitor.getChainedVisitor());
		}
	}
	void setDereference(Resource ref, Location location) {
		if (m_instructionResource != null && ref != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
			Resource		base = m_instructionResource;
			
			m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.Dereferencement, location);
			
			try {
				target.add(new Statement(m_instructionResource, AdenineConstants.base, base));
				target.add(new Statement(m_instructionResource, AdenineConstants.member, ref));
			} catch (RDFException e) {
				onException(e);
			}
		}
	}
	
	class InnerExpressionVisitor extends ExpressionVisitor {
		Location m_location;
		
		public InnerExpressionVisitor(TopLevelVisitor visitor, Location location) {
			super(visitor);
			m_location = location;
		}
		public void end(Location endLocation) {
			super.end(endLocation);
			ExpressionVisitor.this.setIndexInstruction(this.getInstructionResource(), m_location);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IExpressionVisitor#onLeftBracket(edu.mit.lcs.haystack.adenine.tokenizer.SymbolToken)
	 */
	public IExpressionVisitor onLeftBracket(SymbolToken leftBracketT) {
		if (m_instructionResource != null) {
			return new InnerExpressionVisitor(m_topLevelVisitor, leftBracketT.getSpan().getStart());
		} else {
			return new NullExpressionVisitor(m_topLevelVisitor.getChainedVisitor());
		}
	}
	void setIndexInstruction(Resource index, Location location) {
		if (m_instructionResource != null && index != null) {
			IRDFContainer 	target = m_topLevelVisitor.getTarget();
			Resource		base = m_instructionResource;
			
			m_instructionResource = m_topLevelVisitor.generateInstruction(AdenineConstants.Index, location);
			
			try {
				target.add(new Statement(m_instructionResource, AdenineConstants.base, base));
				target.add(new Statement(m_instructionResource, AdenineConstants.index, index));
			} catch (RDFException e) {
				onException(e);
			}
		}
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
		return new SubExpressionVisitor(m_topLevelVisitor) {
			public void end(Location endLocation) {
				super.end(endLocation);
				
				ExpressionVisitor.this.m_instructionResource = this.m_instructionResource;
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onAnonymousModel(Location)
	 */
	public IAnonymousModelVisitor onAnonymousModel(Location location) {
		return new AnonymousModelVisitor(m_topLevelVisitor) {
			public void end(Location endLocation) {
				super.end(endLocation);
				
				ExpressionVisitor.this.m_instructionResource = this.m_instructionResource;
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onApply(Location)
	 */
	public IApplyVisitor onApply(Location location) {
		return new ApplyVisitor(m_topLevelVisitor) {
			public void end(Location endLocation) {
				super.end(endLocation);
				
				ExpressionVisitor.this.m_instructionResource = this.m_instructionResource;
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onAskModel(Location)
	 */
	public IAskModelVisitor onAskModel(Location location) {
		return new AskModelVisitor(m_topLevelVisitor) {
			public void end(Location endLocation) {
				super.end(endLocation);
				
				ExpressionVisitor.this.m_instructionResource = this.m_instructionResource;
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onFloat(edu.mit.lcs.haystack.adenine.tokenizer.FloatToken)
	 */
	public void onFloat(FloatToken floatToken) {
		m_instructionResource = m_topLevelVisitor.generateFloatInstruction(floatToken);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onInteger(edu.mit.lcs.haystack.adenine.tokenizer.IntegerToken)
	 */
	public void onInteger(IntegerToken integerToken) {
		m_instructionResource = m_topLevelVisitor.generateIntegerInstruction(integerToken);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onList(Location)
	 */
	public IListVisitor onList(Location location) {
		return new ListVisitor(m_topLevelVisitor) {
			public void end(Location endLocation) {
				super.end(endLocation);
				
				ExpressionVisitor.this.m_instructionResource = this.m_instructionResource;
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onLiteral(edu.mit.lcs.haystack.adenine.tokenizer.LiteralToken)
	 */
	public void onLiteral(LiteralToken literalToken) {
		m_instructionResource = m_topLevelVisitor.generateLiteralInstruction(literalToken, literalToken.getContent());
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onModel(Location)
	 */
	public IModelVisitor onModel(Location location) {
		return new ModelVisitor(m_topLevelVisitor) {
			public void end(Location endLocation) {
				super.end(endLocation);
				
				ExpressionVisitor.this.m_instructionResource = this.m_instructionResource;
			}
		};
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.ISubExpressionVisitor#onString(edu.mit.lcs.haystack.adenine.tokenizer.StringToken)
	 */
	public void onString(StringToken stringToken) {
		m_instructionResource = m_topLevelVisitor.generateStringInstruction(stringToken);
	}

}
