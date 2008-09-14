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

package edu.mit.lcs.haystack.security;

import java.io.*;
import java.security.*;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class IdentityManager {
	protected KeyStore m_keystore;
	protected String m_filename;
	char[] s_password = new char[] { 'h', 'a', 'y', 's', 't', 'a', 'c', 'k' };
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(IdentityManager.class);

	public IdentityManager(String filename) throws Exception {
		m_keystore = KeyStore.getInstance("jks");
		m_filename = filename;
		
		File file = new File(filename);
		if (file.exists()) {
			FileInputStream fis = new FileInputStream(filename);
			m_keystore.load(fis, s_password);
		} else {
			m_keystore.load(null, s_password);
			save();
		}
	}

	protected void save() {
		try {
			FileOutputStream fos = new FileOutputStream(m_filename);
			m_keystore.store(fos, s_password);
		} catch (Exception e) {
			s_logger.error("Could not save keystore", e);
		}
	}
	
	/**
	 * @see IIdentityManager#authenticate(Resource, String)
	 */
	public Identity authenticate(Resource id, String password) {
		if (!containsIdentity(id)) {
			return null;
		}

		String uri = id.getURI();
		char[] ach = password.toCharArray();

		try {
			PrivateKey privateKey = (PrivateKey)m_keystore.getKey(uri, ach);
			Certificate[] c = m_keystore.getCertificateChain(uri);
			return new Identity(this, id, c[0].getPublicKey(), privateKey);
		} catch(KeyStoreException e) {
			s_logger.error("Could not authenticate identity " + id, e);
		} catch(NoSuchAlgorithmException e) {
			s_logger.error("Could not authenticate identity " + id, e);
		} catch(UnrecoverableKeyException e) {
			s_logger.error("Could not authenticate identity " + id, e);
		}
		
		return null;
	}
	
	public Identity getUnauthenticatedIdentity(Resource id) {
		return new Identity(this, id, null, null);
	}

	/**
	 * @see IIdentityManager#containsIdentity(Resource)
	 */
	public boolean containsIdentity(Resource id) {
		try {
			return m_keystore.containsAlias(id.getURI());
		} catch(KeyStoreException e) {
			s_logger.error("Could not check for identity " + id, e);
			return false;
		}
	}
}
