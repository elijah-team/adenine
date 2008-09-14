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

import java.io.File;
import java.io.InputStream;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.content.IContentService;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;

/**
 * Text extraction agent.
 * @version 	1.0
 * @author		Dennis Quan
 * @version 	1.1
 * @author		Vineet Sinha
 * @change      Bug fixes and support for a seperate content resource
 */

public class TextExtractionAgent
	extends GenericService
	implements IService, ITextExtractionAgent {

	static org.apache.log4j.Logger s_logger =
		org.apache.log4j.Logger.getLogger(TextExtractionAgent.class);

	/**
	 * Supports extraction of content using a different resource
	 * @param res - main resource of object  
	 * @param resContent - resource of content
	 * @return Resource - resource of extracted text
	 * @throws ServiceException
	 */
	public Resource extractText(Resource res, Resource resContent)
		throws ServiceException {
		try {
			IRDFContainer source = m_serviceManager.getRootRDFContainer();
			InputStream content =
				ContentClient.getContentClient(resContent, source, m_serviceManager).getContent();

			ITextExtractor te;
			/*
			if (source
				.contains(
					new Statement(
						res,
						Constants.s_dc_format,
						new Literal("application/pdf")))) {
				te = new PDFTextExtractor();
			} else 
			*/
			if (
				source.contains(
					new Statement(
						res,
						Constants.s_dc_format,
						new Literal("application/postscript")))) {
				te = new PSTextExtractor();
			} else {
				te = new HtmlTextExtractor();
			}

			InputStream convTextStr = te.convertToText(content);
			Resource resText = enterStorage(convTextStr, source);
			source.add(new Statement(res, Constants.s_text_extractedText, resText));
			source.add(
				new Statement(
					resText,
					Constants.s_rdf_type,
					Constants.s_text_extractedText));
			source.add(
				new Statement(
					resText,
					Constants.s_rdf_type,
					Constants.s_content_HttpContent));
			source.add(
				new Statement(resText, Constants.s_dc_format, new Literal("text/plain")));
			return resText;
		} catch (Exception e) {
			s_logger.error("Exception while extracting text: ", e);
			return null;
		}
	}

	/**
	 * Utility function: Extracts text to a file
	 * @param res - main resource of object  
	 * @param outfilName - output file name
	 * @throws ServiceException
	 */
	public void extractTextToFile(Resource res, String outfilName)
		throws ServiceException {
		try {
			IRDFContainer source = m_serviceManager.getRootRDFContainer();
			InputStream content =
				ContentClient.getContentClient(res, source, m_serviceManager).getContent();

			ITextExtractor te;
			/*
			if (source
				.contains(
					new Statement(
						res,
						Constants.s_dc_format,
						new Literal("application/pdf")))) {
				te = new PDFTextExtractor();
			} else 
			*/
			if (
				source.contains(
					new Statement(
						res,
						Constants.s_dc_format,
						new Literal("application/postscript")))) {
				te = new PSTextExtractor();
			} else {
				te = new HtmlTextExtractor();
			}

			InputStream convTextStr = te.convertToText(content);
			
			File f = new File(outfilName);
			PSUtils.dumpToFile(f, convTextStr);
		} catch (Exception e) {
			s_logger.error("Exception while extracting text: ", e);
		}
	}


	/**
	 * @see ITextExtractionAgent#extractText(Resource)
	 */
	public Resource extractText(Resource res) throws ServiceException {
		return extractText(res, res);
	}

	/**
	 * @param str
	 * @param source
	 * @return Resource
	 */
	private Resource enterStorage(InputStream is, IRDFContainer source)
		throws Exception {

		// Connect to a storage
		Resource resStorage =
			Utilities.getResourceProperty(
				m_serviceResource,
				Constants.s_text_contentService,
				source);
		IContentService cs;
		if (resStorage == null) {
			cs =
				ContentClient.getContentService(source, m_serviceManager, m_userResource);
		} else {
			cs = (IContentService) m_serviceManager.connectToService(resStorage, null);
		}
		Resource resBody = cs.allocateContent();

		ContentClient cc = ContentClient.getContentClient(resBody, m_infoSource, m_serviceManager);
		cc.setContent(is);

		return resBody;
	}

}
