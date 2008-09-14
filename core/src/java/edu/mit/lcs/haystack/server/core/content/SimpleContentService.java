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

/*
 * Created on Jun 22, 2003
 */
package edu.mit.lcs.haystack.server.core.content;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.content.ContentAndMimeType;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import org.apache.log4j.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Dennis Quan
 */
public class SimpleContentService
	extends GenericService
	implements IContentService {
		
	final static public Category s_logger = Category.getInstance(SimpleContentService.class);

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#allocateContent()
	 */
	public Resource allocateContent() {
		Resource res = Utilities.generateUniqueResource();
		try {
			m_infoSource.add(new Statement(res, Constants.s_rdf_type, Constants.s_content_ServiceBackedContent));
			m_infoSource.add(new Statement(res, Constants.s_content_service, m_serviceResource));
			if (m_userResource != null) {
				m_infoSource.add(new Statement(res, Constants.s_dc_creator, m_userResource));
			}
		} catch (RDFException e) {
			s_logger.error("Failed to allocate content", e);
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#allocateContent(java.lang.String)
	 */
	public Resource allocateContent(String suffix) {
		Resource res = new Resource(Utilities.generateUniqueResource().getURI() + suffix);
		try {
			m_infoSource.add(new Statement(res, Constants.s_rdf_type, Constants.s_content_ServiceBackedContent));
			m_infoSource.add(new Statement(res, Constants.s_content_service, m_serviceResource));
			if (m_userResource != null) {
				m_infoSource.add(new Statement(res, Constants.s_dc_creator, m_userResource));
			}
		} catch (RDFException e) {
			s_logger.error("Failed to allocate content", e);
		}
		return res;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContent(edu.mit.lcs.haystack.rdf.Resource)
	 */
	synchronized public ContentAndMimeType getContent(Resource res) throws IOException {
		File file = new File(new File(m_basePath), Interpreter.filterSymbols(res.getURI()));
		if (!file.exists()) {
			return null;
		}
		
		ContentAndMimeType camt = new ContentAndMimeType();
		camt.m_mimeType = Utilities.getLiteralProperty(res, Constants.s_dc_format, m_infoSource);
		camt.m_content = new FileInputStream(file);
		return camt;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#setContent(edu.mit.lcs.haystack.rdf.Resource, java.io.InputStream, java.lang.String)
	 */
	synchronized public void setContent(Resource res, InputStream is, String mimeType) throws IOException {
		File file = new File(new File(m_basePath), Interpreter.filterSymbols(res.getURI()));
		if (mimeType != null) {
			try {
				m_infoSource.replace(res, Constants.s_dc_format, null, new Literal(mimeType));
			} catch (RDFException e) {
				s_logger.error("Failed to record MIME type", e);
			}
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		byte[] buffer = new byte[65536];
		int c;
		while ((c = is.read(buffer)) > 0) {
			fos.write(buffer, 0, c);
		}
		fos.close();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContentAsFile(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public File getContentAsFile(Resource res)
		throws IOException, UnsupportedOperationException {
		File file = new File(new File(m_basePath), Interpreter.filterSymbols(res.getURI()));
		if (!file.exists()) {
			return null;
		}
		return file;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContentSize(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public long getContentSize(Resource res) throws IOException {
		return getContentAsFile(res).length();
	}
}
