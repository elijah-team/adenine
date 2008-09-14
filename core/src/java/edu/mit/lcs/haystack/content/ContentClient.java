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

package edu.mit.lcs.haystack.content;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ReaderInputStream;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.content.IContentService;
import edu.mit.lcs.haystack.server.core.rdfstore.RemoteRDFContainer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;

//TODO: Recreate lsid capability, which was removed from smallHaystack
//import com.ibm.lsid.LSID;
//import com.ibm.lsid.client.LSIDResolver;

/**
 * @version 	1.1
 * @author		Dennis Quan
 * @author		Enrique A. Munoz Torres
 * 
 * This class abstracts away the handling of various mimetypes within haystack.
 * It will try to detect the encoding of the incoming source and
 * create the appropriate instance
 */
 
// Changes with respect to version 1.0
// Inclusion of getContentAsFile and getContentAsRandomFile methods
//								- Enrique A. Munoz Torres

public class ContentClient {
//TODO: Is the code in the bio plugin sufficient to handle lsids?
//	public static final Resource s_lsidContent = new Resource("urn:lsid:ncbi.nlm.nih.gov.lsid.i3c.org:predicates:content");
//	public static final Resource s_lsidLiteralContent = new Resource("urn:lsid:ncbi.nlm.nih.gov.lsid.i3c.org:predicates:LiteralContent");

	static org.apache.log4j.Logger s_logger =
		org.apache.log4j.Logger.getLogger(ContentClient.class);

	static public ContentClient getContentClient(Resource res, IRDFContainer rdfc, IServiceAccessor sa) {
		return new ContentClient(res, rdfc, sa);
	}

	static public IContentService getContentService(IRDFContainer source, IServiceAccessor sa, Resource user) {
		try {
			Object x = sa.connectToService(Utilities.getResourceProperty(user, Constants.s_config_contentService, source), null);
			if (x instanceof IContentService) {
				return (IContentService) x;
			} else if (x instanceof RemoteRDFContainer) {
				return (IContentService) ((RemoteRDFContainer) x).getRDFStore();
			} else {
				return null;
			}
		} catch (Exception e) {
			s_logger.error("Error while trying to getContentService:", e);
			return null;
		}
	}

	protected IRDFContainer m_source;
	protected Resource m_res;
	protected IServiceAccessor m_serviceAccessor;
	
	protected ContentClient(Resource res, IRDFContainer rdfc, IServiceAccessor sa) {
		m_res = res;
		m_source = rdfc;
		m_serviceAccessor = sa;
	}
	
	public void setContent(InputStream is) throws Exception {
		setContent(is, null);
	}	

	public void setContent(String content) throws Exception {
		setContent(content, null);
	}	

	/**
	 * Sets the content contained by the associated resource.
	 */
	public void setContent(String content, String mimeType) throws Exception {
		
		if (Utilities.isType(m_res, Constants.s_content_LiteralContent, m_source)) {
			
			m_source.replace(m_res, Constants.s_content_content, null, new Literal(content));

		} else if (Utilities.isType(m_res, Constants.s_content_ServiceBackedContent, m_source)) {
		
			Resource service = Utilities.getResourceProperty(m_res, Constants.s_content_service, m_source);
			((IContentService) m_serviceAccessor.connectToService(service, null)).setContent(m_res, new ReaderInputStream(new StringReader(content)), mimeType);
		
		} else if (Utilities.isType(m_res, Constants.s_content_FilesystemContent, m_source) ||
				  (m_res.getURI().toLowerCase().startsWith("file://"))) {
			/* Handle content from file system */
			
			URL url = new URL(m_res.getURI());
			
			// TODO[dquan]: make smarter
			if (!InetAddress.getLocalHost().getHostAddress().equals(InetAddress.getByName(url.getHost()).getHostAddress())) {
				throw new UnsupportedOperationException("Connecting to external hosts not supported: " + url.getHost());
			}

			String str = url.getPath();
			
			// For Windows, must remove forward slash if there is a drive specification
			if ((str.indexOf(':') != -1) && (str.indexOf('/') == 0)) {
				str = str.substring(1);
			}
			
			// write the file on to disk
			FileWriter fw = new FileWriter(str);
			new PrintWriter(fw).print(content);
			fw.close();
			
			if (mimeType != null) {
				m_source.replace(m_res, Constants.s_dc_format, null, new Literal(mimeType));
			}
		
		} else {/*if (Utilities.isType(m_res, Constants.CONTENT_HTTPCONTENT, m_source))*/ 
			
			HttpClient client = new HttpClient();
			URL url = new URL(m_res.getURI());
			client.startSession(url);
			PutMethod put = new PutMethod(url.getPath());
			if (mimeType != null) {
				put.setRequestHeader("Content-Type", mimeType);
			}
			put.setRequestBody(new ReaderInputStream(content));
			client.executeMethod(put);
		}
	}
	
	/**
	 * Sets the content contained by the associated resource.
	 */
	public void setContent(InputStream is, String mimeType) throws Exception {
		if (Utilities.isType(m_res, Constants.s_content_LiteralContent, m_source)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			char[] buffer = new char[65536];
			int cch;
			StringBuffer sb = new StringBuffer();
			while ((cch = reader.read(buffer, 0, 65536)) > 0) {
				sb.append(buffer, 0, cch);
			}
			
			m_source.replace(m_res, Constants.s_content_content, null, new Literal(sb.toString()));
		
		} else if (Utilities.isType(m_res, Constants.s_content_ServiceBackedContent, m_source)) {
		
			Resource service = Utilities.getResourceProperty(m_res, Constants.s_content_service, m_source);
			((IContentService) m_serviceAccessor.connectToService(service, null)).setContent(m_res, is, mimeType);
		
		} else if (Utilities.isType(m_res, Constants.s_content_FilesystemContent, m_source) ||
		        	(m_res.getURI().toLowerCase().startsWith("file://"))) {
			URL url = new URL(m_res.getURI());
			// TODO[dquan]: make smarter
			if (!InetAddress.getLocalHost().getHostAddress().equals(InetAddress.getByName(url.getHost()).getHostAddress())) {
				throw new UnsupportedOperationException("Connecting to external hosts not supported: " + url.getHost());
			}

			String str = url.getPath();
			
			// For Windows, must remove forward slash if there is a drive specification
			if ((str.indexOf(':') != -1) && (str.indexOf('/') == 0)) {
				str = str.substring(1);
			}
						
			FileOutputStream fw = new FileOutputStream(str);
			byte[] buffer = new byte[65536];
			int cch;
			while ((cch = is.read(buffer, 0, 65536)) > 0) {
				fw.write(buffer, 0, cch);
			}
			fw.close();
			
			if (mimeType != null) {
				m_source.replace(m_res, Constants.s_dc_format, null, new Literal(mimeType));
			}
			
		} else {/*if (Utilities.isType(m_res, Constants.CONTENT_HTTPCONTENT, m_source))*/ 
			HttpClient client = new HttpClient();
			URL url = new URL(m_res.getURI());
			client.startSession(url);
			PutMethod put = new PutMethod(url.getPath());
			if (mimeType != null) {
				put.setRequestHeader("Content-Type", mimeType);
			}
			put.setRequestBody(is);
			client.executeMethod(put);
		}
	}
	
	public ContentAndMimeType getContentAndMimeType() throws Exception {
	
		ContentAndMimeType cmt = new ContentAndMimeType();
		
		if (Utilities.isType(m_res, Constants.s_content_LiteralContent, m_source)) {
			cmt.m_mimeType = "text/html" ;
			cmt.m_content = new ByteArrayInputStream(Utilities.getLiteralProperty(m_res, Constants.s_content_content, m_source).getBytes("UTF-8"));
		
		} else if (Utilities.isType(m_res, Constants.s_content_ServiceBackedContent, m_source)) {
			Resource service = Utilities.getResourceProperty(m_res, Constants.s_content_service, m_source);
			return ((IContentService) m_serviceAccessor.connectToService(service, null)).getContent(m_res);
//TODO: Is the code in the bio plugin sufficient to handle lsids?	
//		} else if (Utilities.isType(m_res, s_lsidLiteralContent, m_source)) {
//			cmt.m_mimeType = "text/html" ;
//			cmt.m_content = new ByteArrayInputStream(Utilities.getLiteralProperty(m_res, s_lsidContent, m_source).getBytes("UTF-8"));
//		
		} else if (Utilities.isType(m_res, Constants.s_content_JavaClasspathContent, m_source)) {
			String path = Utilities.getLiteralProperty(m_res, Constants.s_content_path, m_source);
			cmt.m_mimeType = "text/html" ;
			String pluginName =
				Utilities.getLiteralProperty(
						m_res,
						Constants.s_haystack_pluginName,
						m_source);
			if (pluginName != null) {
				cmt.m_content = CoreLoader.getResourceAsStream(path, pluginName);
			} else {
				cmt.m_content = CoreLoader.getResourceAsStream(path);
			}
		} else if (Utilities.isType(m_res, Constants.s_content_FilesystemContent, m_source) ||
			(m_res.getURI().toLowerCase().startsWith("file://"))) {
			URL url = new URL(m_res.getURI());
			// TODO[dquan]: make smarter
			if (!InetAddress.getLocalHost().equals(InetAddress.getByName(url.getHost()))
					&& !InetAddress.getByName(url.getHost()).isLoopbackAddress()) {
				throw new UnsupportedOperationException("Connecting to external hosts not supported: " + url.getHost());
			}
			
			String str = url.getPath();
			
			// For Windows, must remove forward slash if there is a drive specification
			if ((str.indexOf(':') != -1) && (str.indexOf('/') == 0)) {
				str = str.substring(1);
			}
						
			FileInputStream fis = new FileInputStream(str);
			cmt.m_content = fis;
			
			String s = m_res.getURI().toLowerCase();
			if (s.endsWith(".gif")) {
				cmt.m_mimeType = "image/gif";
			} else if (s.endsWith("jpg") || s.endsWith(".jpeg")) {
				cmt.m_mimeType = "image/jpeg";
			} else {
				cmt.m_mimeType = "text/html" ;
			}
		
		} else if (Utilities.isType(m_res, Constants.s_content_NullContent, m_source)) {
//TODO: Is the code in the bio plugin sufficient to handle lsids?
//		} else if (m_res.getURI().toLowerCase().startsWith("urn:lsid:")) {
//			LSIDResolver resolver = new LSIDResolver(new LSID(m_res.getURI()));
//			cmt.m_content = resolver.getData();
//			cmt.m_mimeType = null;
		
		} else /*if (Utilities.isType(m_res, Constants.CONTENT_HTTPCONTENT, m_source))*/ {
			HttpClient	client = new HttpClient();
			URL 		url = new URL(m_res.getURI());
			
			client.startSession(url);
			GetMethod get = new GetMethod(url.getPath());
			if (url.getQuery() != null) {
				get.setQueryString(url.getQuery());
			}
			
			client.executeMethod(get);
			cmt.m_content = get.getResponseBodyAsStream();
			try {
				cmt.m_mimeType = get.getResponseHeader("Content-Type").getValue();
			} catch (Exception e) {
				cmt.m_mimeType = null;
			}

			client.endSession();
		}
		return cmt;
	}
	
	public long getContentSize() throws Exception {
		if (Utilities.isType(m_res, Constants.s_content_ServiceBackedContent, m_source)) {
			Resource service = Utilities.getResourceProperty(m_res, Constants.s_content_service, m_source);
			return ((IContentService) m_serviceAccessor.connectToService(service, null)).getContentSize(m_res);
		} else if (Utilities.isType(m_res, Constants.s_content_FilesystemContent, m_source) ||
			(m_res.getURI().toLowerCase().indexOf("file://") == 0)) {
			URL url = new URL(m_res.getURI());
			// TODO[dquan]: make smarter
			if (!InetAddress.getLocalHost().equals(InetAddress.getByName(url.getHost()))
					&& !InetAddress.getByName(url.getHost()).isLoopbackAddress()) {
				throw new UnsupportedOperationException("Connecting to external hosts not supported: " + url.getHost());
			}
			
			String str = url.getPath();
			
			// For Windows, must remove forward slash if there is a drive specification
			if ((str.indexOf(':') != -1) && (str.indexOf('/') == 0)) {
				str = str.substring(1);
			}
						
			File file = new File(str);
			return file.length();
		} else if (Utilities.isType(m_res, Constants.s_content_NullContent, m_source)) {
			return 0;
		} else /*if (Utilities.isType(m_res, Constants.CONTENT_HTTPCONTENT, m_source))*/ {
			HttpClient	client = new HttpClient();
			URL 		url = new URL(m_res.getURI());
			
			client.startSession(url);
			HeadMethod head = new HeadMethod(url.getPath());
			if (url.getQuery() != null) {
				head.setQueryString(url.getQuery());
			}
			
			client.executeMethod(head);
			
			long size = 0;
			try {
				size = Long.parseLong(head.getResponseHeader("Content-Length").getValue());
			} catch (Exception e) {
			}

			client.endSession();
			return size;
		}
	}
	
	/**
	 * Returns an InputStream providing access to the content of the associated resource.
	 */
	public InputStream getContent() throws Exception {
		return getContentAndMimeType().m_content;
	}
	
	public String getContentAsString() throws Exception {
		if (Utilities.isType(m_res, Constants.s_content_LiteralContent, m_source)) {
			return Utilities.getLiteralProperty(m_res, Constants.s_content_content, m_source);
		} else {
			InputStream is = getContent();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[65536];
			int c;
			while ((c = is.read(buffer)) > 0) {
				baos.write(buffer, 0, c);
			}
			try {
				return new String(baos.toByteArray(), "UTF-8");
			} catch (Exception e) {
				return new String(baos.toByteArray());
			}
		}
	}

	public Reader getContentAsReader() throws Exception {
		return new InputStreamReader(getContent());
	}

	/**
	 * Returns the Date of the associated resource.
	 */
	public Date getDate() throws Exception {
		if (Utilities.isType(m_res, Constants.s_content_LiteralContent, m_source)) {
			return null;
		} else if (Utilities.isType(m_res, Constants.s_content_JavaClasspathContent, m_source)) {
			return null;
		} else if (Utilities.isType(m_res, Constants.s_content_FilesystemContent, m_source) ||
			(m_res.getURI().toLowerCase().indexOf("file://") == 0)) {
			URL url = new URL(m_res.getURI());
			// TODO[dquan]: make smarter
			if (!InetAddress.getLocalHost().getHostAddress().equals(InetAddress.getByName(url.getHost()).getHostAddress())) {
				throw new UnsupportedOperationException("Connecting to external hosts not supported: " + url.getHost());
			}
			
			String str = url.getPath();
			
			// For Windows, must remove forward slash if there is a drive specification
			if ((str.indexOf(':') != -1) && (str.indexOf('/') == 0)) {
				str = str.substring(1);
			}
						
			return new Date(new File(str).lastModified());
		} else /*if (Utilities.isType(m_res, Constants.CONTENT_HTTPCONTENT, m_source))*/ {
			URL url = new URL(m_res.getURI());
			HttpURLConnection urlc = (HttpURLConnection)(url.openConnection());
			urlc.setRequestMethod("HEAD");
			urlc.connect();
			return new Date(urlc.getLastModified());
		} /*else {
			return null;
		}*/
	}

	public RandomAccessFile getContentAsRandomFile() throws Exception {
		return (new RandomAccessFile(this.getContentAsFile(),"r"));
	}
	
	public File getContentAsFile() throws Exception {
		if (Utilities.isType(m_res, Constants.s_content_FilesystemContent, m_source) || (m_res.getURI().toLowerCase().indexOf("file://") == 0)) {
			URL url = new URL(m_res.getURI());
			// TODO[dquan]: make smarter
			if (!InetAddress.getLocalHost().getHostAddress().equals(InetAddress.getByName(url.getHost()).getHostAddress())) {
				throw new UnsupportedOperationException("Connecting to external hosts not supported: " + url.getHost());
			}
			
			String str = url.getPath();
			
			// For Windows, must remove forward slash if there is a drive specification
			if ((str.indexOf(':') != -1) && (str.indexOf('/') == 0)) {
				str = str.substring(1);
			}
						
			File fis = new File(str);
			return fis;
		} else if (Utilities.isType(m_res, Constants.s_content_ServiceBackedContent, m_source)) {
			Resource service = Utilities.getResourceProperty(m_res, Constants.s_content_service, m_source);
			try {
				return ((IContentService) m_serviceAccessor.connectToService(service, null)).getContentAsFile(m_res);
			} catch (UnsupportedOperationException uoe) {}
		}

		InputStream is = this.getContent();
		File file = File.createTempFile("ngk", "tmp");
		OutputStream os = new FileOutputStream(file);
		byte[] data = new byte[65536];
		int c;
		while ((c = is.read(data)) > 0) {
			os.write(data, 0, c);
		}
		os.close();
		is.close();
		return file;
	}
	
}
