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

package edu.mit.lcs.haystack.adenine.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.functions.DeserializeFunction;
import edu.mit.lcs.haystack.adenine.interpreter.ISerializable;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class ConditionSet implements ISerializable, Serializable {
	protected ArrayList m_conditions = new ArrayList();
	
	public ConditionSet() {
	}
	
	public ConditionSet(Resource res, IRDFContainer source) {
		Resource conditions = Utilities.getResourceProperty(res, AdenineConstants.conditions, source);
		if (conditions != null) {
			Iterator i = ListUtilities.accessDAMLList(conditions, source);
			while (i.hasNext()) {
				Resource condition = (Resource)i.next();
				ArrayList al = new ArrayList();
				Iterator j = ListUtilities.accessDAMLList(condition, source);
				Resource function = (Resource)j.next();
				while (j.hasNext()) {
					Object o = j.next();
					al.add(DeserializeFunction.deserialize((Resource)o, source));
				}
				Condition c = new Condition(function, al);
				m_conditions.add(c);
			}
		}
	}
	
	public Condition get(int x) {
		return (Condition)m_conditions.get(x);
	}
	
	public int count() {
		return m_conditions.size();
	}
	
	public Iterator iterator() {
		return m_conditions.iterator();
	}
	
	public void addAll(ConditionSet cs) {
		m_conditions.addAll(cs.m_conditions);
	}
	
	public void add(Condition c) {
		m_conditions.add(c);
	}
	
	public void remove(Condition c) {
		m_conditions.remove(c);
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("%{ ");
		Iterator i = m_conditions.iterator();
		while (i.hasNext()) {
			Condition c = (Condition)i.next();
			sb.append(c.getFunction());
			sb.append(" ");
			
			Iterator j = c.getParameterIterator();
			while (j.hasNext()) {
				Object o = j.next();
				if (o instanceof Resource) {
					String uri = ((Resource)o).getURI();
					if (uri.indexOf(Constants.s_wildcard_namespace) == 0) {
						sb.append("?");
						sb.append(uri.substring(Constants.s_wildcard_namespace.length()));
					} else {
						sb.append(o);
					}
				} else {
					sb.append(o);
				}
				sb.append(" ");
			}
			
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	public int hashCode() {
		int retVal = 0;
		Iterator i = m_conditions.iterator();
		while (i.hasNext()) {
			// order should not really matter, so just add the hashCodes
			retVal += i.next().hashCode();
		}
		return retVal;	
	}

	/**
	 * @see edu.mit.lcs.haystack.adenine.interpreter.ISerializable#serialize(IRDFContainer)
	 */
	public RDFNode serialize(IRDFContainer rdfc) throws RDFException, AdenineException {
		Resource res = Utilities.generateUniqueResource();
		rdfc.add(new Statement(res, Constants.s_rdf_type, AdenineConstants.ConditionSet));
		Iterator i = m_conditions.iterator();
		ArrayList al = new ArrayList();
		while (i.hasNext()) {
			Condition c = (Condition)i.next();
			ArrayList al2 = new ArrayList();
			al2.add(c.getFunction());
			Iterator j = c.getParameterIterator();
			while (j.hasNext()) {
				al2.add(extractNode(j.next(), rdfc));
			}
			al.add(ListUtilities.createDAMLList(al2.iterator(), rdfc));
		}
		rdfc.add(new Statement(res, AdenineConstants.conditions, ListUtilities.createDAMLList(al.iterator(), rdfc)));
		return res;
	}

	public static RDFNode extractNode(Object o, IRDFContainer rdfc) throws AdenineException, RDFException {
		try {
			if (o instanceof Collection) {
				return ListUtilities.createDAMLList(((Collection)o).iterator(), rdfc);
			} else if (o instanceof Literal) {
				Resource res = Utilities.generateUniqueResource();
				rdfc.add(new Statement(res, Constants.s_rdf_type, AdenineConstants.Literal));
				rdfc.add(new Statement(res, AdenineConstants.literal, (Literal)o));
				return res;
			} else if (o instanceof Resource) {
				Resource res = Utilities.generateUniqueResource();
				rdfc.add(new Statement(res, Constants.s_rdf_type, AdenineConstants.Resource));
				rdfc.add(new Statement(res, AdenineConstants.resource, (Resource)o));
				return res;
			} else if (o instanceof String) {
				return new Literal((String)o);
			} else if (o == null) {
				return new Literal("null");
			} else if (o instanceof ISerializable) {
				return ((ISerializable)o).serialize(rdfc);
			} else {
				return new Literal(o.toString());
			}
		} catch (ClassCastException cce) {
			throw new AdenineException("Type error", cce);
		}
	}

}
