/*
 * Created on Aug 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.io.PrintStream;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.IClusterAlgorithm;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPointCollection;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;

/**
 * @author Yuan; enumerates all possible subtrees of a given tree and calculates
 *         the cost of mapping any two given subtrees DEF: a subtree is defined
 *         here as a node and all its descendants therefore each node represents
 *         a unique subtree.
 */
public class RecordDetector {
    public final static int KMEANS = 1;

    public final static int THRESHOLD = 2;

    protected IAugmentedNode root;

    protected IPointCollection pointCollection;

    protected Vector/* ICluster */clusters;

    protected IClusterAlgorithm algo;

    protected PointFactory pointFactory;

    protected PrintStream out;

    private IProgressMonitor ipm;

    public RecordDetector(IAugmentedNode root) {
        this.root = root;
    }

    public void setProgressMonitor(IProgressMonitor ipm) {
        this.ipm = ipm;
        if (this.pointCollection != null) {
            this.pointCollection.setProgressMonitor(ipm);
        }
        if (this.algo != null) {
            this.algo.setProgressMonitor(ipm);
        }
    }

    public void setAlgorithm(IClusterAlgorithm algorithm) {
        this.algo = algorithm;
        init();
    }

    public int numPoints() {
        if (this.pointCollection != null) {
            return this.pointCollection.size();
        } else {
            return 0;
        }
    }
    
    public void setPointFactory(PointFactory pf) {
        this.pointFactory = pf;
        pointCollection = null;
        init();
    }

    public IClusterAlgorithm getAlgorithm() {
        return this.algo;
    }

    public IPointCollection getCollection() {
        return pointCollection;
    }

    public void init() {
        if (this.pointCollection == null && pointFactory != null && root != null) {
            this.pointCollection = pointFactory.makePointCollection(root);
        }
        if (this.algo != null && pointCollection != null) {
            this.algo.setPointCollection(pointCollection);
        }
        if (this.pointCollection != null) {
            this.pointCollection.setProgressMonitor(ipm);
        }
    }

    public String resultInfo(Vector result) {
        int num = 0;
        StringBuffer buf = new StringBuffer();
        buf.append("# clusters: " + result.size());
        
        if (result.size() >= 1) {
            for (int i = 0; i < result.size() && i < num; i++) {
                ICluster clu = (ICluster)result.get(i);
                buf.append("; c["+i+"]: ");
                if (clu!=null) {
                    buf.append( clu.numMembers() );
                    buf.append( clu.description() );
                } else {
                    buf.append( "NULL");
                }
            }
        }
        return buf.toString();
    }
    
    public void run() {
        algo.run();
        Vector/* ICluster */result = algo.getClusters();
        System.err.println(resultInfo(result));
        
        result = ClusterFilterer.filterHomogeneousClusters(result);
        System.err.println(resultInfo(result));

        result = ClusterFilterer.filterSingletonClusters(result);
        System.err.println(resultInfo(result));

        result = ClusterRelationResolver.resolveClusterRelationships(result);
        System.err.println(resultInfo(result));
        
        result = ClusterRanker.scoreByClusterEntropy(result);
        System.err.println(resultInfo(result));
        
        result = ClusterRanker.sortByClusterScore(result);
        System.err.println(resultInfo(result));
        
        ClusterSerializer.serialize(result);
        
        this.setClusters(result);
    }

    public void setOutput(PrintStream stream) {
        out = stream;
    }

    /**
     * sets the instance clusters variable
     * 
     * @param clusters
     */
    private void setClusters(Vector clusters) {
        this.clusters = clusters;
    }

    /**
     * return the vector of clusters at the current point in processing.
     * 
     * @return an empty Vector if no cluster has been generated
     */
    public Vector getClusters() {
        if (clusters == null) {
            return new Vector();
        } else {
            return clusters;
        }
    }
}