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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ExistentialExpression implements ITemplateExpression {
	ArrayList m_data = new ArrayList();
	
	Resource m_subject = null;
	
	public ExistentialExpression() {
	}
	
	public String toString() {
		StringBuffer out = new StringBuffer();
		Iterator i = m_data.iterator();
		while (i.hasNext()) {
			Object[] o = (Object[])i.next();
			out.append(o[0]);
			out.append(",");
			out.append(o[1]);
			out.append(" ");
		}
		return "[ee: " + m_subject + " " + out + "]";
	}
	
	public ExistentialExpression(Resource res) {
		m_subject = res;
	}
	
	public void add(ITemplateExpression predicate, ITemplateExpression object) {
		if ((predicate == null) || (object == null)) {
			throw new IllegalArgumentException();
		}
		m_data.add(new ITemplateExpression[] { predicate, object });
	}

	public void add(Resource predicate, Resource object) {
		if ((predicate == null) || (object == null)) {
			throw new IllegalArgumentException();
		}
		m_data.add(new ITemplateExpression[] { new ResourceExpression(predicate), new ResourceExpression(object) });
	}

	public void add(Resource predicate, ITemplateExpression object) {
		if ((predicate == null) || (object == null)) {
			throw new IllegalArgumentException();
		}
		m_data.add(new ITemplateExpression[] { new ResourceExpression(predicate), object });
	}

	/**
	 * @see ITemplateExpression#generate(URIGenerator, IRDFContainer)
	 */
	public RDFNode generate(URIGenerator urig, IRDFContainer target) throws RDFException, AdenineException {
		Resource res = m_subject == null ? urig.generateAnonymousResource() : m_subject;
		
		Iterator i = m_data.iterator();
		while (i.hasNext()) {
			ITemplateExpression[] datum = (ITemplateExpression[])i.next();
			target.add(new Statement(res, (Resource)datum[0].generate(urig, target), datum[1].generate(urig, target)));
		}
		
		return res;
	}

	/**
	 * @see ITemplateExpression#generateIndirect()
	 */
	public ITemplateExpression generateIndirect() {
		ExistentialExpression ee = new ExistentialExpression();
		
		if (m_subject == null) {
			ee.add(Constants.s_rdf_type, AdenineConstants.BNode);
	
			Iterator i = m_data.iterator();
			while (i.hasNext()) {
				ITemplateExpression[] datum = (ITemplateExpression[])i.next();
				ExistentialExpression ee1 = new ExistentialExpression();
				ee1.add(Constants.s_rdf_type, AdenineConstants.Statement);
				ee1.add(AdenineConstants.predicate, datum[0].generateIndirect());
				ee1.add(AdenineConstants.object, datum[1].generateIndirect());
				ee.add(AdenineConstants.statement, ee1);
			}
		} else {
			ee.add(Constants.s_rdf_type, AdenineConstants.Model);
			
			ITemplateExpression s = new ResourceExpression(m_subject).generateIndirect();
	
			Iterator i = m_data.iterator();
			while (i.hasNext()) {
				ITemplateExpression[] datum = (ITemplateExpression[])i.next();
				ExistentialExpression ee1 = new ExistentialExpression();
				ee1.add(Constants.s_rdf_type, AdenineConstants.Statement);
				ee1.add(AdenineConstants.subject, s);
				ee1.add(AdenineConstants.predicate, datum[0].generateIndirect());
				ee1.add(AdenineConstants.object, datum[1].generateIndirect());
				ee.add(AdenineConstants.statement, ee1);
			}
			
			// TODO[dquan]: add function call to add
		}
		
		return ee;
	}

}
