package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;


/**
 * @author yks
 * implemented by classes that fragmentize a 
 * web page (the manner of fragmentation is left to the implementation)
 */
public interface IFeatureSet {
    public String getFeatureName();
    
    public Node getRoot();
        
    /**
     * adds features to the feature set.
     * automatically (usually using data structures available
     * to the implementation
     */
    public void addFeatures();

    /**
     * adds one feature, with accompanying information to
     * this feature set.
     * @param feature - Feature (subclassed from AbstractFeature)
     * @param info - any information attached to that feature
     *             - can be a weight.
     */
    public void addFeature(AbstractFeature feature, Object info);
    public void addFeature(AbstractFeature feature);
    
    /**
     * does a set similarity measure
     * @return a double between 0.0 and 1.0
     */
    public double similarity(IFeatureSet set);

    /**
     * does a weighted set similarity measure
     * @return a double between 0.0 and 1.0
     */
    public double weightedSimilarity(IFeatureSet set);
    
    /**
     * Finds the set of features that are in the current
     * set but not in the given set.
     */
    public Set difference(IFeatureSet set);
    
    /**
     * calculates the size of the feature set i.e.
     * number of distince features stored in this set.
     * @return
     */
    public int size();

    /**
     * returns a mapping of features to values stored per feature 
     **/
    public Map getFeatureMap();
    /**
     * @return an array of the features in this feature set.
     */
    public AbstractFeature[] getFeatures();
    /**
     * @return the first n features (order is non-deterministic)
     */
    public AbstractFeature[] getFeatures(int n);
    
    /**
     * For display purposes
     * returns the features as sorted by frequency
     */
    public AbstractFeature[] getFeaturesSorted();

    /**
     * returns the max n features, (where
     * max is sorted by frequency).
     * @param n
     * @return
     */
    public AbstractFeature[] maxFrequency(int n);

    /**
     * get the iterator for the feature set.
     * @return
     */
    public Iterator iterator();
    
}
