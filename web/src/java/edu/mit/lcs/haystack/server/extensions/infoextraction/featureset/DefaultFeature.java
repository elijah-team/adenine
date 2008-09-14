package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;


/**
 * @author yks
 *
 * Basic implementation of a feature
 */
public class DefaultFeature extends AbstractFeature {
    String feature;
    int frequency = 1;
    Object info = null;
    
    public DefaultFeature(String feature) {
        this.feature = new String(feature);
    }
    
    public DefaultFeature() {
        this.feature = "";
    }
    
    public String toString() {
        return this.feature;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.AbstractFeature#increment()
     */
    public void increment() {
        frequency++;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.AbstractFeature#getFrequency()
     */
    public int getFrequency() {
        return frequency;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.AbstractFeature#setInfo(java.lang.Object)
     */
    public void setInfo(Object info) {
        this.info = info;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.AbstractFeature#getInfo(java.lang.Object)
     */
    public Object getInfo(Object getInfo) {
        return this.info;
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.infoextraction.featureset.IFeatureSet#getFeatureName()
     */
    public String getFeatureName() {
        return "Default-Feature";
    }

}
