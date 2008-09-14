/*
 * Created on Nov 7, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;

/**
 * @author yks Same as HashCountSet. Keeps track of the frequency that add is
 *         called for each key. Optionally, the count can be set manually by
 *         calling setCount;
 */
public class WeightedVector extends DefaultPoint { //HashSet implements IPoint {
    private HashMap set = new HashMap();
    private HashSet hashSet = new HashSet();
    
    double total = 0;

    boolean normalized = false;

    public Iterator iterator() {
        return set.keySet().iterator();
    }

    public WeightedVector() {
        super(null);
    }

    public WeightedVector(Object arg) {
        super(arg);
    }
    
    public boolean add(Object key) {
        hashSet.add(key);
        normalized = false;

        WeightInfo wi = (WeightInfo) set.get(key);
        total++;

        if (wi != null) {
            wi.count += 1;
            set.put(key, wi);
            return false;
        } else {
            set.put(key, new WeightInfo(1));
            return true;
        }
    }

    /**
     * normalizes the vector counts such that all the weights add up to 1
     */
    public void normalize() {
        if (!normalized) {
            Iterator it = this.iterator();
            double length = 0;
            while (it.hasNext()) {
                Object key = it.next();
                WeightInfo wi = (WeightInfo) set.get(key);
                length += wi.count * wi.count;
            }
            length = Math.sqrt(length);
            
            it = this.iterator();
            while (it.hasNext()) {
                Object key = it.next();
                WeightInfo wi = (WeightInfo) set.get(key);
                wi.weight = (double) wi.count / (double) length;
            }
            normalized = true;
        }
    }

    /**
     * Add a bunch of items into this WeightVector
     */
    public boolean addAll(Collection c) {
        boolean changed = hashSet.addAll(c);
        Iterator it = c.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
        return changed;
    }

    /**
     * increment the count of a given key 
     * by num
     * @param key
     * @param num
     */
    public void incrCount(Object key, double num) {
        normalized = false;
        WeightInfo wi = (WeightInfo) set.get(key);
        if (wi != null) {
            wi.count += num;
        } else {
            wi = new WeightInfo(num);
        }

        /*
         * update total by changing by the difference of the new count and old
         * count
         */
        total += num;
        set.put(key, wi);
    }

    /**
     * Set the weight of a given key NOTE:
     * 
     * @param key
     * @param num
     */
    public void setCount(Object key, double num) {
        normalized = false;
        WeightInfo wi = (WeightInfo) set.get(key);
        double old = 0;
        if (wi != null) {
            old = wi.count;
            wi.count = num;
        } else {
            wi = new WeightInfo(num);
        }

        /*
         * update total by changing by the difference of the new count and old
         * count
         */
        total += num - old;
        set.put(key, wi);
    }

    public double getWeight(Object key) {
        normalize();
        WeightInfo wi = (WeightInfo) set.get(key);
        if (wi == null) {
            return 0;
        } else {
            return wi.weight;
        }
    }

    public double getCount(Object key) {
        WeightInfo wi = (WeightInfo) set.get(key);
        if (wi == null) {
            return 0;
        } else {
            return wi.count;
        }
    }

    public void addVector(WeightedVector wc) {
        Iterator terms = wc.iterator();
        while (terms.hasNext()) {
            String key = (String)terms.next();
            double count = wc.getCount(key);
            this.incrCount(key, count);
        }
    }
    
    /* generate a centroid vector */
    public IPoint centroid(ICluster c) {
        Vector /*IPoint*/ points = c.getMembers();
        Iterator it = points.iterator();
        WeightedVector centroid = new WeightedVector();
        
        while (it.hasNext()) {
            WeightedVector cur = (WeightedVector) it.next();
            centroid.addVector( cur );
        }
        
        int numVectors = points.size();
        it = centroid.iterator();
        while (it.hasNext()) {
            Object key = it.next();
            double count = centroid.getCount(key);
            centroid.setCount(key, count / (double)numVectors);
        }
        
        centroid.normalize();
        return centroid;
    }

    /**
     * for debugging
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        Iterator it = this.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            double w = getWeight(key);
            double c = getCount(key);
            buf.append(key + ":" + c + ":" + (Math.round(w*1000)/1000) + "; ");
        }
        
        return buf.toString();
    }
    
    public double distance(IPoint wv) {
        double dist = 1 - dotProduct((WeightedVector)wv);
        if (dist < 0 || dist < 1e-15) { dist = 0;} 
        //HACK to solve numerical precision errors.
        return dist;
    }
    
    public double vectorLength() {
        
        double squares = 0;
        Iterator it = this.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            double w = this.getWeight(key);
            squares += w * w;
        }
        return Math.sqrt(squares);
    }
    
    public int size() {
        return hashSet.size();
    }
    
    public double dotProduct(WeightedVector wv) {
        double sim = 0;
        /*
         * normalize vectors to be sure
         */
        wv.normalize();
        this.normalize();

        WeightedVector here;
        WeightedVector there;

        /*
         * for efficiency, we iterate over the vector with the least number
         * elements
         */
        if (hashSet.size() < wv.size()) {
            here = this;
            there = wv;
        } else {
            here = wv;
            there = this;
        }

        Iterator it = here.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            double thereWeight = there.getWeight(key);
            double hereWeight = here.getWeight(key);

            if (thereWeight != 0 && hereWeight != 0) {
                sim += thereWeight * hereWeight;
            }
        }

        return sim;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.DefaultPoint#setPointCollection(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection)
     */
    public void setPointCollection(IPointCollection ipc) {
        this.pointCollection = ipc;
    }

    class WeightInfo {
        public double count;

        public double weight;

        public WeightInfo(double count) {
            this.count = count;
        }
    }
}
