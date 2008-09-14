/*
 * Created on Jul 15, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.web.WebViewPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.query.LuceneAgent;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.rdfstore.Cholesterol3RDFStoreService;

/**
 * @author yks
 *  
 */
public class PageAnnotator {
    static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger
            .getLogger(PageAnnotator.class);

    public PageAnnotator() {
    }

    public static Context updateContext(IRDFContainer rdfc,
            IRDFContainer infoSource, IDOMElement clicked, Context context,
            WebViewPart webView) {

        NodeID clickedID = clicked.getNodeID();

        Context resultContext = context;

        System.err.println("clickedID: " + clicked.getNodeID());
        IDOMDocument doc = webView.getDOMBrowser().getDocument();

        System.err.println("Name of node: " + clicked.getNodeName());
        System.err.println("InnerHTML of node: " + clicked.getInnerHTML());
        System.err.println("type of node: " + clicked.getNodeType());
        System.err.println("node text: " + clicked.getNodeText());

        String focusText = clicked.getNodeText();

        LuceneAgent la = LuceneAgent.getLuceneAgent(rdfc, context);
        Set results = la.query(focusText);
        Iterator it = results.iterator();
        while (it.hasNext()) {
            RDFNode node = (RDFNode) it.next();
            System.err.println("lucene index: " + node.toString());
        }

        if (rdfc instanceof Cholesterol3RDFStoreService) {
            System.err.println("We have a cholesterol3rdfstore service!!!");
            Cholesterol3RDFStoreService cholesterol = (Cholesterol3RDFStoreService) rdfc;
            List res = cholesterol.searchLiterals(focusText);
            boolean found = false;
            int count = 0;
            HashMap duplicate = new HashMap();
            if (res != null && !res.isEmpty()) {
                Iterator iter = res.iterator();
                while (iter.hasNext()) {
                    Literal lit = (Literal) iter.next();
                    System.err.println("Literal: " + lit);
                    Set queryResults = null;

                    //= results ${ rdf:type hs:List ; dc:title "Results" }
                    //        		= resultList @()
                    //        		for x in literals
                    //        			for y in (query { ?x dc:title x })
                    //        				if (! (resultList.contains y[0]))
                    //                           resultList.add y[0]
                    try {
                        queryResults = rdfc.query(new Statement(Utilities
                                .generateWildcardResource(1),
                                Constants.s_dc_title, lit),
                                new Resource[] { Utilities
                                        .generateWildcardResource(1) });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (!queryResults.isEmpty()) {
                        Iterator queryResultsIter = queryResults.iterator();
                        while (queryResultsIter.hasNext()) {
                            RDFNode[] nodes = (RDFNode[]) queryResultsIter
                                    .next();
                            Resource r = (Resource) nodes[0];
                            System.err.println("From search: " + r);

                            if (duplicate.get(r) != null) {
                                continue;
                            } else {
                                duplicate.put(r, new Boolean(true));
                                /*
                                 * Context(parent) constructor will build a
                                 * chain of contexts
                                 */

                                resultContext = new Context(resultContext);
                                resultContext.putLocalProperty(
                                        OzoneConstants.s_underlying, r);
                                if (count >= 3) {
                                    found = true;
                                    break;
                                } else {
                                    count++;
                                }
                            }
                        }
                    } else {
                        System.err.println("No matches");
                    }
                    if (found) { /*
                                  * exit the outer loop when 1 good match is
                                  * found
                                  */
                        break;
                    }
                }
            }
        }
        return resultContext;
    }
}