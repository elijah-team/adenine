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

package edu.mit.lcs.haystack.proxy;

import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.security.Identity;
import edu.mit.lcs.haystack.security.IdentityManager;

/**
 * Manages proxies to local and remote services.
 * @author Dennis Quan
 */
public class ProxyManager implements IServiceAccessor {
	protected IRDFContainer m_root;
	protected IdentityManager m_identityManager;

	public ProxyManager(IRDFContainer rdfc, IdentityManager im) {
		m_root = rdfc;
		m_identityManager = im;
	}
	
	public IRDFContainer getRootRDFContainer() {
		return m_root;
	}
	
	public void setRootRDFContainer(IRDFContainer root) {
		this.m_root = root;
	}
	
	public Resource getResource() {
		return null;
	}
		
	public Object connectToService(Resource res, Identity id) throws Exception {
		Set protocols = m_root.query(new Statement[] {
			new Statement(res, Constants.s_wsdl_port, Utilities.generateWildcardResource(4)),
			new Statement(Utilities.generateWildcardResource(4), Constants.s_wsdl_binding, Utilities.generateWildcardResource(2)),
			new Statement(Utilities.generateWildcardResource(2), Constants.s_rdf_type, Utilities.generateWildcardResource(3)),
			new Statement(Utilities.generateWildcardResource(1), Constants.s_config_bindingDomain, Utilities.generateWildcardResource(3))
		}, new Resource[] { Utilities.generateWildcardResource(1), Utilities.generateWildcardResource(3) }, Utilities.generateWildcardResourceArray(4) );
		
		Iterator i = protocols.iterator();
		while (i.hasNext()) {
			Resource protocol = (Resource)((RDFNode[])i.next())[0];

			Class c = Utilities.loadClass(protocol, m_root);
			IProtocol p = (IProtocol)c.newInstance();
			Object o = p.connectToService(this, m_root, res, id);
			if (o != null) {
				return o;
			}
		}
				
		return null;
	}

	public IRDFContainer discoverServices(Resource protocol, Resource endpoint) throws Exception {
		Class c = Utilities.loadClass(protocol, m_root);
		IProtocol p = (IProtocol)c.newInstance();
		return p.discoverServices(this, m_root, endpoint);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.proxy.IServiceAccessor#isShuttingDown()
	 */
	public boolean isShuttingDown() {
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.proxy.IServiceAccessor#getIdentityManager()
	 */
	public IdentityManager getIdentityManager() {
		return m_identityManager;
	}

}

