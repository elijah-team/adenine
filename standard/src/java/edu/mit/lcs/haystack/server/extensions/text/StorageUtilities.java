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

package edu.mit.lcs.haystack.server.extensions.text;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ReaderInputStream;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class StorageUtilities {
	
	/*
	 * Vineet: seems like old code, see TextExtractionAgent.enterStorage
	 * to possibly refactor here
	 */
	 
	static public Resource enterStorage(String str, IRDFContainer source) {
		return enterStorage(new ReaderInputStream(str), source);
	}
	
	static public Resource enterStorage(InputStream is, IRDFContainer source) {
		try {
			Resource resStorage = Utilities.getResourceSubjects(Constants.s_rdf_type, Constants.s_haystack_Storage, source)[0];
			HttpClient client = new HttpClient();
			URL url = new URL(resStorage.getURI());
			client.startSession(url);
			String id = Utilities.generateUniqueIdentifier();
			PutMethod put = new PutMethod(url.getPath() + id);
			System.out.println("putting into " + url.getPath() + id);
			put.setRequestBody(is);
			System.out.println(client.executeMethod(put));
			System.out.println(put.getStatusText());
			return new Resource(url.toString() + id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
