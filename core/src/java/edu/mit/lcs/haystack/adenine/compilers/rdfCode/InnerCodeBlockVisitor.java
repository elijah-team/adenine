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
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.parser2.*;
import edu.mit.lcs.haystack.adenine.compilers.utils.*;
import edu.mit.lcs.haystack.adenine.tokenizer.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author David Huynh
 */
public class InnerCodeBlockVisitor extends ParserVisitorBase implements ICodeBlockVisitor {
	protected TopLevelVisitor	m_topLevelVisitor;
	protected List			m_instructions = new ArrayList();
	
	public InnerCodeBlockVisitor(TopLevelVisitor topLevelVisitor) {
		super(topLevelVisitor.getChainedVisitor());
		m_topLevelVisitor = topLevelVisitor;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IBlockVisitor#onConstruct(Location, String)
	 */
	public IConstructVisitor onConstruct(Location location, String construct) {
		return m_topLevelVisitor.getConstruct(construct, m_instructions);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IBlockVisitor#setFirstIndent(edu.mit.lcs.haystack.adenine.tokenizer.IndentToken)
	 */
	public void setFirstIndent(IndentToken ident) {
	}

	public Resource getFirstInstruction() {
		if (m_instructions.size() > 0) {
			return (Resource) m_instructions.get(0);
		} else {
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.adenine.parser2.IParserVisitor#end(edu.mit.lcs.haystack.adenine.tokenizer.Location)
	 */
	public void end(Location endLocation) {
		Iterator 		i = m_instructions.iterator();
		Resource		previous = null;
		IRDFContainer	target = m_topLevelVisitor.getTarget();
		
		try {
			while (i.hasNext()) {
				Resource current = (Resource) i.next();
				
				if (previous != null) {
					target.add(new Statement(previous, AdenineConstants.next, current));
				}
				
				previous = current;
			}
		} catch (RDFException e) {
			onException(e);
		}
		
		super.end(endLocation);
	}

}
