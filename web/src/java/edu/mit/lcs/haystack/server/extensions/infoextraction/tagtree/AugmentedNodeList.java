/*
 * Created on Aug 13, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree;

import java.util.Vector;

import org.w3c.dom.Node;

/**
 * @author yks
 */
public class AugmentedNodeList implements org.w3c.dom.NodeList {
    Vector /* <AugmentedNode> */ nodes;
        
    public AugmentedNodeList() {
        nodes = new Vector();
    }
    
    public AugmentedNodeList(Vector list) {
        nodes = (Vector)list.clone();
    }
    
    public Node addNode(Node node) {
        nodes.add(node);
        return node;
    }
    
    public Node removeNode(Node node) {
        nodes.remove(node);
        return node;
    }
    
    /* (non-Javadoc)
     * @see org.w3c.dom.NodeList#item(int)
     */
    public Node item(int index) {
        if (index >=0 && index < nodes.size()) {
            return (Node)nodes.get(index);
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.w3c.dom.NodeList#getLength()
     */
    public int getLength() {
        return nodes.size();
    }
}
