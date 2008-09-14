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

package edu.mit.lcs.haystack.server.core.rdfstore;

import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.EventListenerService;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Convenience base class for RDF event listeners.
 * @version 	1.0
 * @author		David Huynh
 * @author		Dennis Quan
 */
public class RDFListener extends EventListenerService implements IRDFListener {
	IRDFEventSource 	m_store;
	Map					m_cookies = new HashMap();

	/**
	 * Constructor for RDFListener.
	 * @param sm
	 */
	public RDFListener(ServiceManager sm, IRDFEventSource store) {
		super(sm);
		m_store = store;
	}

	/**
	 * @see IRDFListener#statementAdded(Resource, Statement)
	 */
	public void statementAdded(Resource cookie, Statement s) {
	}

	/**
	 * @see IRDFListener#statementRemoved(Resource, Statement)
	 */
	public void statementRemoved(Resource cookie, Statement s) {
	}

	/**
	 * @see EventListenerService#getInterfaceName()
	 */
	protected String getInterfaceName() {
		return IRDFListener.class.getName();
	}

	/**
	 * @see EventListenerService#stop()
	 */
	public void stop() {
		super.stop();
		Iterator i = m_cookies.keySet().iterator();
		while (i.hasNext()) {
			try {
				m_store.removeRDFListener((Resource)i.next());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public Resource addPattern(Resource s, Resource p, RDFNode o) throws RDFException {
		Resource cookie = Utilities.generateUniqueResource();
		m_store.addRDFListener(getServiceResource(), s, p, o, cookie);
		
		m_cookies.put(cookie, new Object[] { s, p, o });
		
		return cookie;
	}

	public void removePattern(Resource s, Resource p, RDFNode o) {
		Iterator i = m_cookies.keySet().iterator();
		while (i.hasNext()) {
			Object key = i.next();
			Object[] pattern = (Object[])m_cookies.get(key);
			if (((s != null && s.equals(pattern[0])) || (s == null && pattern[0] == null)) &&
				((p != null && p.equals(pattern[1])) || (p == null && pattern[1] == null)) &&	
				((o != null && o.equals(pattern[2])) || (o == null && pattern[2] == null))) {
				removePattern((Resource)key);
			}
		}
	}

	public void removePattern(Resource cookie) {
		if (m_cookies.remove(cookie) != null) {
			try {
				m_store.removeRDFListener(cookie);
			} catch (RDFException e) {
			}
		}
	}
}
