/*
 * Created on Nov 24, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 * 
 * A PCP representation is where a tree is represented by a collection of
 * Parent-Child-Pair rules.
 */
public class PCPPoint extends WeightedVector {
    public PCPPoint() {
        super(null);
    }

    public PCPPoint(Object arg) {
        super(arg);
        if (arg != null) {
            generateVector((Node) arg);
            this.normalize();
        }
    }

    public void addProduction(Production prod) {
        super.add(prod.toString());
    }

    private void generateVector(Node node) {
        if (Utilities.isValidNode(node)) {
            String tagName = node.getNodeName();

            /* recursively add child nodes */
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {

                if (tagName != null) {
                    Node child = children.item(i);
                    if (Utilities.isTextNode(child) || Utilities.isValidNode(child)) {
                        String childName = child.getNodeName();
                        if (childName != null) {
                            Production p = new Production(tagName, childName);
                            this.addProduction(p);
                        }
                    }
                }
                INode cINode = (INode) children.item(i);
                generateVector(cINode);
            }
        }
    }
    
    class Production {
        String leftSide;

        String rightSide;

        public Production(String leftSide, String rightSide) {
            if (leftSide != null) {
                this.leftSide = leftSide.toLowerCase();
            } else {
                this.leftSide = null;
            }
            if (rightSide != null) {
                this.rightSide = rightSide.toLowerCase();
            } else {
                this.rightSide = null;
            }
        }

        public String toString() {
            return leftSide + "=>" + rightSide;
        }
    }
}