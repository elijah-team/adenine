/*
 * Created on Nov 7, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;

/**
 * @author yks converts a tree into a vector of tags.
 */
public class BagOfTags extends WeightedVector {

    public BagOfTags() {
    } /* empty weightedVector */

    public BagOfTags(Object arg) {
        super(arg);
        if (arg != null) {
            generateVector(this.node);
            this.normalize();
        }
    }

    private void generateVector(Node node) {
        /* generate features for a vector */ 
        if (Utilities.isTextNode(node) || Utilities.isValidNode(node)) {
            String tagName = node.getNodeName();

            if (tagName != null) {
                this.add(tagName);
            }

            /* recursively add child nodes */
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = (Node) children.item(i);
                generateVector(child);
            }
        }
    }
}