/*
 * Created on Feb 10, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 * Representation of Points as parent child child pairs with
 * start and end symbols.
 */
public class PCCTPoint extends WeightedVector {
    
    public PCCTPoint() {
        super(null);
    }

    public PCCTPoint(Object arg) {
        super(arg);
        if (arg != null) {
            generateVector((Node) arg);
            this.normalize();
        }
    }
    
    public void addTriplet(Triplet triplet) {
        super.add(triplet.toString());
    }

    private void generateVector(Node node) {
        if (Utilities.isValidNode(node)) {
            String tagName = node.getNodeName();

            /* recursively add child nodes */
            NodeList children = node.getChildNodes();
            String lastChildName = null;
            for (int i = 0; i < children.getLength(); i++) {

                if (tagName != null) {
                    Node child = children.item(i);
                    if (Utilities.isTextNode(child) || Utilities.isValidNode(child)) {
                        String childName = child.getNodeName();
                        if (childName != null) {
                            
                            Triplet p = new Triplet(tagName, lastChildName, childName);
                            addTriplet(p);
                            lastChildName = childName;
                        }
                    }
                }
                
                
                INode cINode = (INode) children.item(i);
                generateVector(cINode);
            }

            if (lastChildName != null) {
                Triplet p = new Triplet(tagName, lastChildName, null);
                addTriplet(p); 
            }
        }
    }

    class Triplet {
        final static private String TERMINATOR = "$";
        private String parent;
        private String leftChild;
        private String rightChild;

        public Triplet(String parent, String leftSide, String rightSide) {
            if (parent != null) {
                this.parent = parent.toLowerCase();
            }
            if (leftSide != null) {
                this.leftChild = leftSide.toLowerCase();
            } else {
                this.leftChild = TERMINATOR;
            }
            if (rightSide != null) {
                this.rightChild = rightSide.toLowerCase();
            } else {
                this.rightChild = TERMINATOR;
            }
        }

        public String toString() {
            return parent + "=>" + leftChild + " " + rightChild;
        }
    }
}

