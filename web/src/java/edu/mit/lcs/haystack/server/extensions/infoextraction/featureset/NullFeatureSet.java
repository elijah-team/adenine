/*
 * Created on Nov 28, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

/**
 * @author yks
 */
public class NullFeatureSet implements IFeatureSet {
    private Node root;
    
    public final static String NAME = "null";
    
    NullFeatureSet(Node root) {
        this.root = root;
    }
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getFeatureName()
     */
    public String getFeatureName() {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getRoot()
     */
    public Node getRoot() {
        return this.root;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#addFeatures()
     */
    public void addFeatures() { }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#addFeature(edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature, java.lang.Object)
     */
    public void addFeature(AbstractFeature feature, Object info) { }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#addFeature(edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature)
     */
    public void addFeature(AbstractFeature feature) { }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#similarity(edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet)
     */
    public double similarity(IFeatureSet set) {
        return 0;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#weightedSimilarity(edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet)
     */
    public double weightedSimilarity(IFeatureSet set) {
        return 0;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#difference(edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet)
     */
    public Set difference(IFeatureSet set) {
		return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#size()
     */
    public int size() {
        return 0;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getFeatureMap()
     */
    public Map getFeatureMap() {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getFeatures()
     */
    public AbstractFeature[] getFeatures() {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getFeatures(int)
     */
    public AbstractFeature[] getFeatures(int n) {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getFeaturesSorted()
     */
    public AbstractFeature[] getFeaturesSorted() {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#maxFrequency(int)
     */
    public AbstractFeature[] maxFrequency(int n) {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#iterator()
     */
    public Iterator iterator() {
        return null;
    }

}
