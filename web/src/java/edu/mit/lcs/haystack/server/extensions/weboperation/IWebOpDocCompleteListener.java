/*
 * Created on Nov 10, 2004
 */
package edu.mit.lcs.haystack.server.extensions.weboperation;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;

/**
 * @author Ryan Manuel
 */
public interface IWebOpDocCompleteListener {
	public void documentComplete(IDOMDocument doc, String url);
}
