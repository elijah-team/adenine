package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;

/**
 * @author yks
 * 
 * Stores an HTML page as a (unordered) bag of fragments Definition: Fragment is
 * the concatenation, a node with two of its children
 */
public class ParentChildChildFragmentSet extends DefaultFeatureSet {
    public static final String NAME = "Parent-Child-Child-Fragment (PCCF)";

    public ParentChildChildFragmentSet(Node root) {
        super(root);
        addFeatures();
    }

    /**
     * IFragmentSet interface
     */
    public void addFeatures() {
        Utilities.debug(this, "addFeatures");
        addFeaturesRecursive(this.root);
    }

    /**
     * Extracts all the fragments from the web page, and populates the fragments
     * map.
     */
    private void addFeaturesRecursive(Node node) {
        NodeList children = node.getChildNodes();
        int numChildren = children.getLength();

        if (numChildren > 0) {
            String nodeName = node.getNodeName();

            int leftChildIndex = 0;
            int rightChildIndex = 0;
            Node leftChild;
            Node rightChild;

            while (leftChildIndex < numChildren) {
                TreeFeature frag = new TreeFeature();
                frag.addPathComponent(nodeName);

                leftChildIndex = filterNodes(children, leftChildIndex);
                if (leftChildIndex >= 0) {
                    leftChild = children.item(leftChildIndex);
                } else {
                    leftChild = null;
                }

                if (leftChild != null) {
                    frag.addLeafComponent(leftChild.getNodeName());
                } else {
                    break; /* no more nodes to form triplets */
                }

                rightChildIndex = filterNodes(children, leftChildIndex + 1);
                if (rightChildIndex >= 0) {
                    rightChild = children.item(rightChildIndex);
                } else {
                    rightChild = null;
                }

                if (rightChild != null) {
                    frag.addLeafComponent(rightChild.getNodeName());
                }

                storeFeatureAndIncrementCount(frag);

                if (leftChild != null) {
                    addFeaturesRecursive((Node) leftChild);
                }

                if (rightChildIndex >= 0) {
                    leftChildIndex = rightChildIndex;
                } else {
                    break;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IFeatureSet#addFeature(edu.mit.lcs.haystack.server.infoextraction.AbstractFeature,
     *      java.lang.Object)
     */
    public void addFeature(AbstractFeature feature, Object info) {
        storeFeatureAndInfo(feature, info);
    }

    public String getFeatureName() {
        return NAME;
    }
}