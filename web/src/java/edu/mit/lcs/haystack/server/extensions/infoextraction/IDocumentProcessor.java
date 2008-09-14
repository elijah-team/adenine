/*
 * Created on Aug 17, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;

/**
 * @author yks
 */
public interface IDocumentProcessor {
    public void process(IDOMBrowser browser, String url);
}
