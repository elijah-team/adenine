package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks abstract implementation of IFragmentSet, contains implementation
 *         of "similarity" measure
 */
abstract public class DefaultFeatureSet implements IFeatureSet {
    public static String NAME = "Default";
    
    TreeMap/* <String,FeatureContainer> */features;

    public final static String KEY_VALUE_SEPARATOR = "     ";

    Node root;

    /**
     * a comparator that compares two FeatureContainer objects by "frequency"
     */
    private class ValueComparator implements Comparator {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            return ((AbstractFeature) o1).getFrequency()
                    - ((AbstractFeature) o2).getFrequency();
        }

    }

    /**
     * constructor
     * 
     * @param root
     *            of the dom
     */
    public DefaultFeatureSet(Node root) {
        this.root = root;
        this.features = new TreeMap(/* new ValueComparator() */);
    }

    public Node getRoot() {
        return this.root;
    }
    /**
     * default addFragments implementation does not do anything
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#addFeatures()
     */
    public void addFeatures() {
        /* nothing */
    }

    public void addFeature(AbstractFeature feature, Object info) {
        // need to figure out the position stuff
        storeFeatureAndInfo(feature, info);
    }

    public void addFeature(AbstractFeature feature) {
        // need to figure out the position stuff
        storeFeatureAndIncrementCount(feature);
    }

    public AbstractFeature[] getFeaturesSorted() {
        AbstractFeature frags[] = this.getFeatures();
        Arrays.sort(frags, new ValueComparator());
        return frags;
    }

    public AbstractFeature[] maxFrequency(int n) {
        int i = 0;
        AbstractFeature[] frags = getFeaturesSorted();
        Vector v = new Vector();
        while (i < n && i < frags.length) {
            v.add(frags[i]);
        }
        return (AbstractFeature[]) (v.toArray());
    }

    /**
     * adds a fragment to the set, and increments the fragment frequency count
     * 
     * @param feat
     */
    protected void storeFeatureAndIncrementCount(AbstractFeature feat) {
        String featString = feat.toString();
        AbstractFeature val = (AbstractFeature) features.get(featString);
        if (val != null) {
            val.increment();
        } else {
            val = feat;
        }
        features.put(featString, val);
    }

    /**
     * adds a fragment to the set, and increments the fragment frequency count
     * 
     * @param frag
     */
    protected void storeFeatureAndInfo(AbstractFeature frag, Object info) {
        String fragString = frag.toString();
        AbstractFeature instance = (AbstractFeature) features.get(fragString);
        if (instance != null) {
            instance.setInfo(info);
            features.put(fragString, instance);
        } else {
            frag.setInfo(info);
            features.put(fragString, frag);
        }
    }

    /**
     * returns the set representing the symmetric difference between two sets
     * 
     * @param set
     *            the target set to do the difference on
     * @return set containing the difference between the larger set and the
     *         smaller set
     */
    public Set difference(IFeatureSet set) {
        Set sourceDiff = new TreeSet(this.getFeatureMap().keySet());
        Set targetDiff = new TreeSet(set.getFeatureMap().keySet());

        if (sourceDiff.size() > targetDiff.size()) {
            sourceDiff.removeAll(targetDiff);
            return sourceDiff;
        } else {
            targetDiff.removeAll(sourceDiff);
            return targetDiff;
        }
    }

    /**
     * find the ratio of intersecting fragments over the the union of fragment
     * between this and another FragmentSet. returns a double between 0.0, and
     * 1.0 All repetitive occurrence of a fragment is counted as one instance
     * 
     * @param set
     *            target set to be compared against
     * @return a double between 0.0 to 1.0 0.0 means not similar at all to 1.0
     *         the two sets are the same
     */
    public double similarity(IFeatureSet set) {
        /* find the intersection divide it by the union */
        Set intersection = this.getFeatureMap().keySet();
        Set union = new TreeSet(this.getFeatureMap().keySet());

        Set target = set.getFeatureMap().keySet();

        /* finds the intersection */
        intersection.retainAll(target);
        union.addAll(target);

        if (union.size() > 0) {
            return (double) intersection.size() / (double) union.size();
        }
        return 1.0;
    }

    /**
     * find the ratio of intersecting fragments over the the union of fragment
     * between this and another FragmentSet. returns a double between 0.0, and
     * 1.0 All repetitive occurrence are accounted for during union.
     * 
     * @param set
     *            target set to be compared against
     * @return a double between 0.0 to 1.0; 0.0 means not similar at all to 1.0
     *         the two sets are the same
     */
    public double weightedSimilarity(IFeatureSet set) {
        /* find the intersection divide it by the union */
        Set intersection = new TreeSet(this.getFeatureMap().keySet());
        Set union = new TreeSet(this.getFeatureMap().keySet());

        Set target = new TreeSet(set.getFeatureMap().keySet());

        /* finds the intersection */
        intersection.retainAll(target);
        union.addAll(target);

        int intersectionSize = 0;
        int unionSize = 0;

        Map self = this.getFeatureMap();
        Map other = set.getFeatureMap();

        Iterator it = intersection.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            AbstractFeature countSrc = (AbstractFeature) self.get(key);
            AbstractFeature countTgt = (AbstractFeature) other.get(key);
            int weight = Math.min(countSrc.getFrequency(), countTgt.getFrequency());
            intersectionSize += weight;
        }

        it = union.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            AbstractFeature countSrc = (AbstractFeature) self.get(key);
            AbstractFeature countTgt = (AbstractFeature) other.get(key);
            if (countSrc == null && countTgt == null) {
                int weight = Math.max(countSrc.getFrequency(), countTgt.getFrequency());
                unionSize += weight;
            }
        }

        if (unionSize > 0) {
            return (double) intersectionSize / (double) unionSize;
        }
        return 1.0;
    }

    /**
     * @return the Map containing fragment => fragment count
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#getFeatureMap()
     */
    public Map getFeatureMap() {
        return features;
    }

    /**
     * returns all the features in the set as an array
     */
    public AbstractFeature[] getFeatures() {
        if (features != null && features.size() > 0) {
            int n = features.size();
            return getFeatures(n);
        }
        return null;
    }

    /**
     * returns the first n features in the set as an array
     */
    public AbstractFeature[] getFeatures(int n) {
        System.err.println("getFeatures!!!");
        System.err.flush();
        AbstractFeature[] ret = null;
        if (features != null && features.size() > 0) {
            if (features.size() < n) {
                n = features.size();
            }
            System.err.println("getFeatures(" + n + ")");
            System.err.println("numFeatures: " + features.size());
            ret = new AbstractFeature[n];
            Iterator it = features.values().iterator();
            int i = 0;
            while (it.hasNext() && i < n) {
                ret[i++] = (AbstractFeature) it.next();
            }
        }
        return ret;
    }

    /**
     * returns the fragments and the values (information) stored with each
     * fragment
     */
    public String[] getFeatureAndFrequency() {

        String[] ret = null;
        if (features != null && features.size() > 0) {
            ret = new String[features.size()];
            Iterator it = features.entrySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                Map.Entry entrySet = (Map.Entry) it.next();
                ret[i++] = entrySet.getKey() + KEY_VALUE_SEPARATOR
                        + entrySet.getValue();
            }
        }
        return ret;
    }

    /**
     * @return iterator instance on the set of fragments
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet#iterator()
     */
    public Iterator iterator() {
        return features.keySet().iterator();
    }

    /**
     * returns the size of this set
     */
    public int size() {
        return features.keySet().size();
    }

    /**
     * For debugging purposes, we want a print out of all the fragments of a
     * page and their respective counts
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();

        Iterator it = features.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            buf.append(key);
            buf.append("\t" + features.get(key).toString() + "\n");
        }
        return buf.toString();
    }

    /**
     * Filters a set of nodes starting from <code>starting</code> and returns
     * the next index that fulfills the default filter criteria.
     * 
     * @param nodes
     * @param starting
     *            a integer >= 0 representing a starting point in nodes to
     *            search
     * @return -1 if no nodes could be found.
     */
    private static HashMap ignoreTags;
    static {
        ignoreTags = new HashMap();
        Object dummy = new Object();
        ignoreTags.put("script", dummy);
        ignoreTags.put("href", dummy);
        ignoreTags.put("src", dummy);
        ignoreTags.put("style", dummy);
    }

    protected int filterNodes(NodeList nodes, int starting) {
        int len = nodes.getLength();

        if (starting >= len) {
            return -1;
        } else {
            Node node;
            int i = starting;
            while (i < len) {
                node = nodes.item(i);
                if (ignoreTags.get(node.getNodeName().toLowerCase()) != null
                        || node.getNodeType() == INode.COMMENT_NODE) {
                    i++;
                } else {
                    break;
                }
            }
            if (i < len) {
                return i;
            } else {
                return -1;
            }
        }
    }
}

/**
 * a container class that stores the instance of the feature, the
 * count/frequency of that instance, and other info as an Object.
 * 
 * @author yks
 */

class FeatureContainer {
    AbstractFeature feature;

    int frequency;

    Object info;

    /**
     * increments the frequency count
     */
    void increment() {
        frequency++;
    }

    public FeatureContainer(AbstractFeature feature, int frequency, Object info) {
        this.feature = feature;
        this.frequency = frequency;
        this.info = info;
    }

    public FeatureContainer(AbstractFeature feature, int frequency) {
        this.feature = feature;
        this.frequency = frequency;
    }

    public FeatureContainer(AbstractFeature feature, Object info) {
        this.feature = feature;
        this.frequency = 1;
        this.info = info;
    }
}