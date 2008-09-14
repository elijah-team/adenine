package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;

/**
 * @author yks
 *
 * Keeps track of the frequency of tags at specific levels
 * in the tree
 **/
public class TagLevelFrequency extends DefaultFeatureSet {
    public static final String NAME = "Tag-Path-with-PCCF";

    public TagLevelFrequency(IDOMElement root) {
        super(root);
        addFeatures();
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.IFeatureSet#addFeatures()
     */
    public void addFeatures() {
        Utilities.debug(this, "addFeatures()");
    }
    
    public String getFeatureName() {
        return NAME;
    }
}
