/*
 * Created on Dec 22, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.HashMap;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks a sequence mapping between two sequence of tree nodes
 */
public class TreeMapping {

    static public IRelatablePoint gap = null;

    static public boolean isGap(IRelatablePoint p) {
        return gap == p;
    }

    static private boolean debug_score = false;

    private Vector/* TreeMapping */mappings;

    private String _description;

    private double _cached_score = -1;

    private double _node_score = 0;

    private int _cached_size = -1;

    private boolean approximate = false;

    static public int numAllocated = 0;

    static public int numCalled = 0;

    static public int numCached = 0;

    static public int numCacheHit = 0;

    static public int numOptimized = 0;

    static public boolean optimization = false;

    final static double optimization_threshold = 1.0;

    final static public int CACHE_LIMIT = 1000000;

    static HashMap treeMappingCache;

    static {
        clearCache();
    }

    static public void clearCache() {
        treeMappingCache = new HashMap();
        numCalled = 0;
        numAllocated = 0;
        numCached = 0;
        numCacheHit = 0;
    }

    static private String getNodeKey(IRelatablePoint a, IRelatablePoint b) {
        StringBuffer desc = new StringBuffer();
        String aKey, bKey;
        if (a != null) {
            INode aINode = (INode) a.getData();
            aKey = aINode.getNodeName();
        } else {
            aKey = "_G";
        }
        if (b != null) {
            INode bINode = (INode) b.getData();
            bKey = bINode.getNodeName();
        } else {
            bKey = "_G";
        }
        if (aKey.compareTo(bKey) <= 0) {
            desc.append(aKey);
            desc.append("-");
            desc.append(bKey);
        } else {
            desc.append(aKey);
            desc.append("-");
            desc.append(bKey);
        }
        desc.append("-" + getNodeScore(a, b));
        return desc.toString();
    }

    static public TreeMapping getMapping(IRelatablePoint a, IRelatablePoint b) {
        String key;
        numCalled++;

        if (numCached > CACHE_LIMIT) {
            numAllocated++;
            return new TreeMapping(a, b);
        }

        if (isGap(a) || isGap(b) || a.getChildren().size() == 0 || b.getChildren().size() == 0) {

            key = getNodeKey(a, b);

            if (treeMappingCache.containsKey(key)) {
                numCacheHit++;
                return (TreeMapping) treeMappingCache.get(key);
            } else {
                numAllocated++;
                numCached++;
                TreeMapping tm = new TreeMapping(a, b);
                treeMappingCache.put(key, tm);
                return tm;
            }

        } else {
            numAllocated++;
            return new TreeMapping(a, b);
        }
    }

    public TreeMapping(IRelatablePoint a, IRelatablePoint b) {
        boolean done = false;
        if (TreeMapping.optimization) {

            int aSize = isGap(a) ? 0 : a.getSize();
            int bSize = isGap(b) ? 0 : b.getSize();
            int total = aSize + bSize;

            double ratio;
            double estimate;

            if (aSize > 0 && bSize > 0) {
                if (aSize < bSize) {
                    estimate = (double) aSize / (double) total;
                    ratio = (double) aSize / (double) bSize;
                } else {
                    estimate = (double) bSize / (double) total;
                    ratio = (double) bSize / (double) aSize;
                }

                if (ratio < TreeMapping.optimization_threshold) {

                    approximate = true;
                    _cached_score = estimate * _cached_size;
                    _description = getNodeDescription(a, b);
                    numOptimized++;
                    done = true;
                } 
            }
        }
        
        if (! done) {
            _node_score = getNodeScore(a, b);
            _description = getNodeDescription(a, b);
            _cached_size = getNodeSize(a, b);
        }
        mappings = new Vector();
        
    }

    static int getNodeSize(IRelatablePoint a, IRelatablePoint b) {
        int size = 0;
        if (!isGap(a)) {
            size = a.getSize();
        }
        if (!isGap(b)) {
            size += b.getSize();
        }
        return size;
    }

    static private String getNodeDescription(IRelatablePoint a, IRelatablePoint b) {
        String desc;
        if (a != null) {
            INode aINode = (INode) a.getData();
            desc = aINode.getNodeName() + "[" + a.getUniqueID() + "]";
        } else {
            desc = "GAP";
        }
        desc += " - ";
        if (b != null) {
            INode bINode = (INode) b.getData();
            desc += bINode.getNodeName() + "[" + b.getUniqueID() + "]";
        } else {
            desc += "GAP";
        }
        return desc;
    }

    static private double getNodeScore(IRelatablePoint a, IRelatablePoint b) {
        double nscore = 0;
        if (isGap(a) || isGap(b)) {
            nscore = 0;
        } else {
            INode aINode = (INode) a.getData();
            INode bINode = (INode) b.getData();

            if (aINode.getNodeName().equals(bINode.getNodeName())) {
                nscore = 2;
            } else {
                nscore = 0;
            }
        }
        return nscore;
    }

    public double getScore() {
        if (_cached_score > 0) {
            return _cached_score;
        }

        _cached_score = _node_score;
        if (debug_score) {
            System.err.println("node score:" + _node_score);
        }

        double seqScore = 0;
        double curPairScore = 0;
        for (int i = 0; i < mappings.size(); i++) {
            curPairScore = 0;

            TreeMapping tm = (TreeMapping) mappings.get(i);
            curPairScore += tm.getScore();

            if (debug_score) {
                System.err.println("pair-score: " + tm._description + curPairScore);
            }
            seqScore += curPairScore;
        }
        _cached_score += seqScore;

        if (debug_score) {
            System.err.println("sequence: " + this.toString());
            System.err.println("final score: " + _cached_score);
        }

        return _cached_score;
    }

    public double getNormalizedScore() {
        int totalSize = getSize();
        double score = getScore();
        return score / (double) totalSize;
    }

    public int getSize() {
        return _cached_size;
    }

    public void prependMapping(TreeMapping mapping) {
        mappings.add(mapping);
        _cached_score = -1;
    }

    /*
     * we prepend more than append so store list internally as a reverse list
     * for more efficiency
     */
    public void appendMapping(TreeMapping mapping) {
        mappings.insertElementAt(mapping, 0);
        _cached_score = -1;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("TOP: " + _description + "\n");
        buf.append("SEQUENCE: \n");

        for (int i = mappings.size() - 1; i >= 0; i--) {
            TreeMapping tm = (TreeMapping) mappings.get(i);

            buf.append("\t" + tm._description);
            buf.append("\n");
        }
        return buf.toString();
    }

    public static String infoString() {
        StringBuffer buf = new StringBuffer();

        buf.append("TreeMapping.allocated: " + numAllocated + "\n");
        buf.append("TreeMapping.called: " + numCalled + "\n");
        buf.append("TreeMapping.cached: " + numCached + "\n");
        buf.append("TreeMapping.numCacheHit: " + numCacheHit + "\n");
        buf.append("TreeMapping.numOptimized: " + numOptimized + "\n");
        return buf.toString();
    }
}