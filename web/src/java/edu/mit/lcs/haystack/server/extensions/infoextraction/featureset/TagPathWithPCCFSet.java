package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;

/**
 * @author yks
 */
public class TagPathWithPCCFSet extends DefaultFeatureSet {
    static final public String NAME = "Tag-Path-with-PCCF";
    
    TagPathWithPCCFSet(Node root) {
        super(root);
        addFeatures();
    }
    
    /**
     * IFragmentSet interface
     */
    public void addFeatures() {
        Utilities.debug(this, "addFeatures()");
        addFeaturesRecursive(this.root, new TreeFeature());
    }
    
    /**
     * builds a trigram at the target <code>node</code>
     * and along with it, also the path from the root of the
     * tree to that node
     * @param node
     * @param path
     */
    private void addFeaturesRecursive(Node node, TreeFeature frag) {
        NodeList children = node.getChildNodes();
        int numChildren = children.getLength();

        if (numChildren > 0) {
            String nodeName = node.getNodeName();
            if (nodeName == null) {
                return;
            }
            
            int leftChildIndex = 0;
            int rightChildIndex = 0;
            Node leftChild;
            Node rightChild;

            while (leftChildIndex < numChildren) {
                TreeFeature myFrag = new TreeFeature(frag, false);
                myFrag.addPathComponent( nodeName );
                
                leftChildIndex = filterNodes(children, leftChildIndex);
                if (leftChildIndex >= 0) {
                    leftChild = children.item(leftChildIndex);
                } else {
                    leftChild = null;
                }

                if (leftChild != null && leftChild.getNodeName()!= null) {
                    myFrag.addLeafComponent( leftChild.getNodeName() );
                } else {
                    break; /* no more nodes to form triplets */
                }

                rightChildIndex = filterNodes(children, leftChildIndex+1);
                if (rightChildIndex >= 0) {
                    rightChild = children.item(rightChildIndex);
                } else {
                    rightChild = null;
                }

                if (rightChild != null && rightChild.getNodeName() != null) {
                    myFrag.addLeafComponent( rightChild.getNodeName() );
                }                

                storeFeatureAndIncrementCount(myFrag);

                if (leftChild != null) {
                    addFeaturesRecursive((Node)leftChild, myFrag);
                }
                
                if (rightChildIndex >= 0) {
                    leftChildIndex = rightChildIndex;
                } else {
                    break;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.IFeatureSet#addFeature(edu.mit.lcs.haystack.server.infoextraction.AbstractFeature, java.lang.Object)
     */
    public void addFeature(AbstractFeature feature, Object info) {
        storeFeatureAndInfo(feature, info);
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.IFeatureSet#getFeatureName()
     */
    public String getFeatureName() {
        return NAME;
    }
}
