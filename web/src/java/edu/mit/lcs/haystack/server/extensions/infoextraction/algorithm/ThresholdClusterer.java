/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm;

import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.DefaultCluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPointCollection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;

/**
 * @author yks
 */
public class ThresholdClusterer implements IClusterAlgorithm {
    public final static double DEFAULT_THRESHOLD = 0.25;

    public final static int NON_STRICT = 1;

    public final static int STRICT = 2;

    public final static int MAJORITY = 3;

    private IPointCollection points;

    private Vector/* ICluster */clusters;

    private IProgressMonitor ipm;
    
    protected static double threshold = DEFAULT_THRESHOLD;

    protected static int mergeMode = MAJORITY;

    public void setThreshold(double threshold) {
        ThresholdClusterer.threshold = threshold;
    }

    public void setMergeMode(int mergeMode) {
        ThresholdClusterer.mergeMode = mergeMode;
    }

    public ThresholdClusterer() {
    }

    public void setProgressMonitor(IProgressMonitor ipm) {
        this.ipm = ipm;
        if (points != null) {
            this.points.setProgressMonitor( ipm );
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#run()
     */
    public void run() {
        this.clusters = generateInitialClusters();
        mergeClusters(this.clusters);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#setData(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection)
     */
    public void setPointCollection(IPointCollection pointCollection) {
        this.points = pointCollection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#getClusters()
     */
    public Vector getClusters() {
        return clusters;
    }

    public void setClusters(Vector clusters) {
        this.clusters = clusters;
    }

    /**
     * Given all node to node mapping costs, for each node, cluster it with a
     * node that has the lowest cost relative to it.
     * 
     * @return
     */
    private Vector generateInitialClusters() {
        Timer.printTimeElapsed("generateInitialClusters() -- START: threshold:" + threshold);

        Vector/* ICluster */clusters = new Vector();

        points.clearAssociations();

        Iterator it = points.iterator();
        while (it.hasNext()) {
            IPoint point = (IPoint) it.next();

            /* cluster nodes that haven't been clusters */
            if (!point.hasAssociation()) {

                IPoint neighbour = points.getNearestNeighbour(point);
                double distance = points.getDistance(point, neighbour);

                if (distance <= threshold) {

                    if (neighbour.hasAssociation()) {

                        ICluster cluster = (ICluster) neighbour.getAssociation();
                        point.setAssociation(cluster);
                        cluster.addMember(point);

                    } else {

                        ICluster cluster = new DefaultCluster();
                        clusters.add(cluster);
                        cluster.addMember(point);
                        cluster.addMember(neighbour);
                        point.setAssociation(cluster);
                        neighbour.setAssociation(cluster);
                    }
                }
            }
        }
        Timer.printTimeElapsed("generateInitialClusters() -- END");
        return clusters;
    }

    /**
     * compare all the members of the two clusters and if all the pairwise costs
     * are above the threshold then return true.
     * 
     * @param clusterA
     * @param clusterB
     * @return true if all pairwise costs between the two clusters are below a
     *         certain threshold, and they have the same node name.
     */

    private boolean shouldMergeStrict(ICluster clusterA, ICluster clusterB) {

        Iterator aIt = clusterA.iterator();
        while (aIt.hasNext()) {
            IPoint aMember = (IPoint) aIt.next();

            Iterator bIt = clusterB.iterator();
            while (bIt.hasNext()) {
                IPoint bMember = (IPoint) bIt.next();

                double cost = points.getDistance(bMember, aMember);
                if (cost > threshold) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean shouldMergeMajority(ICluster clusterA, ICluster clusterB) {

        Iterator aIt = clusterA.iterator();
        int numA = clusterA.numMembers();
        boolean[] votes = new boolean[numA];

        int cur = 0;
        while (aIt.hasNext()) {
            IPoint aMember = (IPoint) aIt.next();

            votes[cur] = true;

            Iterator bIt = clusterB.iterator();
            while (bIt.hasNext()) {
                IPoint bMember = (IPoint) bIt.next();

                double cost = points.getDistance(aMember, bMember);
                if (cost > threshold) {
                    votes[cur] = false;
                    continue;
                }
            }

            cur++;
        }

        int count = 0;
        for (int i = 0; i < numA; i++) {
            if (votes[i]) {
                count++;
            }
        }

        if (count >= (numA / 2))
            return true;
        else
            return false;
    }

    private boolean shouldMergeNonStrict(ICluster clusterA, ICluster clusterB) {

        Iterator aIt = clusterA.iterator();
        while (aIt.hasNext()) {
            IPoint aMember = (IPoint) aIt.next();

            Iterator bIt = clusterB.iterator();
            while (bIt.hasNext()) {
                IPoint bMember = (IPoint) bIt.next();

                double cost = points.getDistance(aMember, bMember);
                if (cost <= threshold) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldMerge(ICluster clusterA, ICluster clusterB) {
        switch (mergeMode) {
        case STRICT:
            return shouldMergeStrict(clusterA, clusterB);
        case NON_STRICT:
            return shouldMergeNonStrict(clusterA, clusterB);
        case MAJORITY:
            return shouldMergeMajority(clusterA, clusterB);
        }
        return false;
    }

    /**
     * given a first pass at grouping nodes, now operate on the given clusters
     * and merge clusters together.
     * 
     * @param vec
     * @return
     */
    public void mergeClusters(Vector/* ICluster */clusters) {
        Timer.printTimeElapsed("mergeClusters() -- START:");

        Iterator it = clusters.iterator();

        ICluster last = null;

        for (int i = 0; i < clusters.size(); i++) {

            ICluster cluster = (ICluster) clusters.get(i);

            /* compare with other clusters larger in the list */
            for (int j = i + 1; j < clusters.size(); j++) {
                ICluster target = (ICluster) clusters.get(j);
                if (shouldMerge(cluster, target)) {
                    cluster.join(target);
                    clusters.remove(target);
                }
            }
        }
        Timer.printTimeElapsed("mergeClusters() -- END:");
    }
}