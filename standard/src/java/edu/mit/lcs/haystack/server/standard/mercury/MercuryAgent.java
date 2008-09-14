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

package edu.mit.lcs.haystack.server.standard.mercury;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;

/**
 * @author Dennis Quan
 */
public class MercuryAgent extends GenericService {
	public static final String s_namespace = "http://haystack.lcs.mit.edu/agents/mercury#";
	public static final Resource s_mercuryAgent = new Resource(s_namespace + "MercuryAgent");
	
	static public MercuryAgent getMercuryAgent(Resource user, IServiceAccessor sa) {
		IRDFContainer root = sa.getRootRDFContainer();
		try {
			RDFNode[] datum = root.queryExtract(new Statement[] {
				new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, s_mercuryAgent),
				new Statement(Utilities.generateWildcardResource(1), Constants.s_haystack_user, user)
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
			if (datum != null) {
				return (MercuryAgent) sa.connectToService((Resource) datum[0], null);
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public void incorporate(IRDFContainer data, boolean convertOntologies, boolean autoCorrect, Resource packageName) throws ServiceException {
		try {
			Set results = data.query(new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, Utilities.generateWildcardResource(2)), Utilities.generateWildcardResourceArray(2));
			HashSet unknowns = new HashSet();
			Iterator i = results.iterator();
			while (i.hasNext()) {
				RDFNode[] datum = (RDFNode[]) i.next();
				Resource subject = (Resource) datum[0];
				if (subject.getURI().indexOf("urn:unknown:") == 0) {
					unknowns.add(subject);
				}
			}
			
			IRDFContainer target;
			if (packageName == null) {
				target = m_infoSource;
			} else {
				target = new PackageFilterRDFContainer(m_infoSource, packageName);
			}
			
			i = unknowns.iterator();
			while (i.hasNext()) {
				Resource subject = (Resource) i.next();
				
				// Resolve this URI
				ArrayList al = new ArrayList();
				results = data.query(new Statement(subject, Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2)), Utilities.generateWildcardResourceArray(2));
				Iterator j = results.iterator();
				while (j.hasNext()) {
					RDFNode[] datum = (RDFNode[]) j.next();
					al.add(new Statement(subject, (Resource) datum[0], datum[1]));
				}
				
				Statement[] statements = new Statement[al.size()];
				al.toArray(statements);
				Set possibilities = m_infoSource.query(statements, new Resource[] { subject }, new Resource[] { subject });
				if (possibilities.size() == 0) {					
					target.add(new LocalRDFContainer(statements));
				} else if (possibilities.size() == 1) {
					Resource newSubject = (Resource) ((RDFNode[]) possibilities.iterator().next())[0];

					// Cache and remove [subject] <> <>
					Set results0 = data.query(new Statement(subject, Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2)), Utilities.generateWildcardResourceArray(2));
					data.remove(new Statement(subject, Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2)), Utilities.generateWildcardResourceArray(2));
					
					// Replace in subject field
					j = results0.iterator();
					while (j.hasNext()) {
						RDFNode[] datum = (RDFNode[]) j.next();
						data.add(new Statement(newSubject, (Resource) datum[0], datum[1]));
					}
					
					// Cache and remove <> <> [subject]
					Set results1 = data.query(new Statement(Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2), subject), Utilities.generateWildcardResourceArray(2));
					data.remove(new Statement(Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(2), subject), Utilities.generateWildcardResourceArray(2));
					
					// Replace in object field
					j = results1.iterator();
					while (j.hasNext()) {
						RDFNode[] datum = (RDFNode[]) j.next();
						data.add(new Statement((Resource) datum[0], (Resource) datum[1], newSubject));
					}
				}
			}
			
			target.add(data);
		} catch (RDFException e) {
			throw new ServiceException("RDF error", e);
		}
	}
	
	public Resource hasMatch(IRDFContainer data, Resource res) throws ServiceException {
		try {
			Statement[] s = new Statement[data.size()];
			Iterator i = data.iterator();
			int j = 0;
			while (i.hasNext()) {
				s[j++] = (Statement) i.next();
			}
			RDFNode[] datum = m_infoSource.queryExtract(s, new Resource[] { res }, new Resource[] { res });
			if (datum != null) {
				return (Resource) datum[0];
			} else {
				return null;
			}
		} catch (RDFException e) {
			throw new ServiceException("RDF error", e);
		}
	}
}
