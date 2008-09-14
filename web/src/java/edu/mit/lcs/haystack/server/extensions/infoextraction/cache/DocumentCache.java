/*
 * Created on Nov 26, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.cache;

import java.util.HashMap;

import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMElement;

/**
 * @author yks
 * an in memory page cache;
 */
public class DocumentCache {

    static HashMap cache = new HashMap();
    
    static public boolean isCached(String url) {
        return (null != cache.get(url));
    }
    
    static public void storePage(String url) {
        IDOMDocument doc = InternetExplorer.parseURL(url);
        String contents = ((IEDOMElement)doc.getDocumentElement()).getOuterHTML();
        cache.put(url, contents);
    }
    
    static public void storePage(IDOMBrowser browser, String url) {
        if (browser == null) {
            storePage(url);
        } else {
            String contents = ((IEDOMElement) browser.getDocument().getDocumentElement()).getOuterHTML();
            cache.put(url, contents);
        }
    }
    
    static public String getPage(String url) {
        return getPage(null, url);
    }
    
    static public String getPage(IDOMBrowser browser, String url) {
        String contents = (String)cache.get(url);
        if (contents == null) {
            storePage(browser, url);
            contents = (String)cache.get(url);
        }
        return contents;
    }
}
