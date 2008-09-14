/*
 * Created on Aug 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import org.w3c.dom.Node;

import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;

public class FeatureVector {

    private String nodeName;

    private int nodeSize;
    
    public int getNodeSize() {
        return nodeSize;
    }

    public String nodeName() {
        return nodeName;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public String toString() {
        return (nodeName + ":" + Integer.toString(nodeSize));
    }

    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

    public FeatureVector(Node node) {
        IAugmentedNode ian = (IAugmentedNode) node;
        nodeName = ian.nodeName();
        nodeSize = ian.numDescendants();
    }
}