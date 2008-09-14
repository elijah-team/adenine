package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;

/**
 * @author yks
 *
 * fragments a web page based on its node path. 
 */
public class TagPathSet extends DefaultFeatureSet {
    static final public String NAME = "Tag-Path";
    
    public TagPathSet(Node root) {
        super(root);
        addFeatures();
    }
    
    /** 
     * populates with path fragments
     */
    public void addFeatures() {
        Utilities.debug(this, "addFeatures()");
        addFeaturesRecursive(root, new TreeFeature() );
    }

    private void addFeaturesRecursive(Node node, TreeFeature frag) {
        NodeList children = node.getChildNodes();
        int len = children.getLength();

        TreeFeature myFrag = new TreeFeature(frag, false);
        myFrag.addPathComponent(node.getNodeName());

        int cur = 0;
        while ( cur < len ) {
            cur = filterNodes(children, cur);
            if (cur >= 0) {
                Node child = (Node)children.item(cur);
                addFeaturesRecursive(child, myFrag);    
            } else {
                break;
            }
            cur++;
        }
        storeFeatureAndIncrementCount(myFrag);
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.IFeatureSet#addFeature(edu.mit.lcs.haystack.server.infoextraction.AbstractFeature, java.lang.Object)
     */
    public void addFeature(AbstractFeature feature, Object info) {
        this.storeFeatureAndInfo(feature, info);
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.IFeatureSet#getFeatureName()
     */
    public String getFeatureName() {
        return NAME;
    }
}
