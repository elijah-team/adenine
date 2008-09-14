/*
 * Created on Aug 16, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.Iterator;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 * @author yks
 */
public interface IHighlighter {
    public void clearHighlighted();
    public void highlightNodesByFeature(String feature, String bgcolor, String fgcolor);
    public void highlightNodeByNodeID(NodeID id, String bgcolor, String fgcolor);
    public void highlightNodeByNodeID(NodeID id);
    public void highlightNodes(Iterator/*Node*/ nodeIt, String bgcolor, String fgcolor);
    public void highlightNodes(Iterator/*Node*/ nodeIt);
}
