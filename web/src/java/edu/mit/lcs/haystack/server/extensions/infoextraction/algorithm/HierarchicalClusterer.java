/*
 * Created on Nov 7, 2004
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
 * performs hierarchical clustering on a set of features
 */
public class HierarchicalClusterer implements IClusterAlgorithm {
    IPointCollection points;
    
    private Vector/*ICluster*/clusters;
    
    public static double DEFAULT_THRESHOLD = 0.1;
    private double threshold = DEFAULT_THRESHOLD;
    private IProgressMonitor ipm;
    
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#setData(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection)
     */
    public void setPointCollection(IPointCollection pointCollection) {
        this.points = pointCollection.copy();
    }

    public void setProgressMonitor(IProgressMonitor ipm) {
        this.ipm = ipm;
        if (points != null) {
            this.points.setProgressMonitor( ipm );
        }
        System.err.println("ipm " + this.ipm);
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.IClusterAlgorithm#run()
     */
    public void run() {
        
        int lastSize = points.size();
        IPointCollection curPoints = points;
        int i = 0;
        while (true) {
            Timer.printTimeElapsed("Hierarchical-iterate " + i + " size: " + curPoints.size());

            curPoints = iterate(curPoints, threshold);
        
            if (curPoints.size() == 1 ||
                curPoints.size() == lastSize) {
                /* we've reached the end */
                break;
            } else {
            }
            i++;
            lastSize = curPoints.size();
        }
        
        clusters = new Vector();
        Iterator it = curPoints.iterator();
        while (it.hasNext()) {
            IPoint p = (IPoint)it.next();
            ICluster c = (ICluster)p.getAssociation();

            /* singleton points get their
             * own clusters 
             */
            if (c == null) {
                c = new DefaultCluster();
                c.addMember(p);
                p.setAssociation(c);
            }
            
            clusters.add(c);
        }
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.IClusterAlgorithm#getClusters()
     */
    public Vector getClusters() {
        return clusters;
    }

    /**
     * Take a set of points, and merge all the clusters
     * associated with those set of points, creates a new cluster
     * that encapsulates all these points.
     * @return the centroid of the new cluster.
     */
    public IPoint mergePoints(IPointCollection pc, Vector /* IPoint */ vpoints) {
        ICluster newCluster = new DefaultCluster();
        
        /**
         * merge all clusters associated with each
         * point in vpoints.
         */
        for (int i = 0; i < vpoints.size(); i++) {
            IPoint p = (IPoint)vpoints.get(i);
            if (p.hasAssociation()) {
        
                ICluster c = (ICluster) p.getAssociation();
                Iterator it = c.getMembers().iterator();
                while (it.hasNext()) {
                    IPoint curP = (IPoint) it.next();
                    newCluster.addMember(curP);
                    curP.setAssociation(newCluster);
                }
                
            } else {
                newCluster.addMember(p);
                p.setAssociation(newCluster);
            }
        }

        IPoint centroid = (IPoint)pc.getPointFactory().makeCentroid(newCluster);
        centroid.setAssociation( newCluster ); //only set association not membership
        return centroid;
    }
    
    /**
     * For each iteration, we take two nearest points,
     * and merge the clusters that are associated with those
     * two points.
     */
    private IPointCollection iterate(IPointCollection oldColl, double threshold) {

        Vector/*IPoint*/ nearest = oldColl.getClosestPoints();
        
        IPoint a = (IPoint)nearest.get(0);
        IPoint b = (IPoint)nearest.get(1);
        double dist = oldColl.getDistance(a,b);

        if ( dist > threshold ) {
            System.err.println("dist: " + dist + " > threshold: " + threshold);
            return oldColl;
        }

        System.err.print("nearest: " + nearest.size());
        for (int i = 0; i < nearest.size() && i < 2; i++) {
            System.err.print(((IPoint)nearest.get(i)).getUniqueID() + "\t");
        }
        System.err.println(" dist: " + dist);
        IPoint newCentroid = mergePoints(oldColl, nearest);

        /* remove all old points from the cluster */
        for (int j = 0; j < nearest.size(); j++) {
            IPoint remove = (IPoint)nearest.get(j); 
            oldColl.removePoint( remove );
        }
        
        /* add the newly formed cluster */
        ICluster c = (ICluster) newCentroid.getAssociation();
        oldColl.addPoint( newCentroid );
        
        return oldColl;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Hierarchical Agg. Clustering: Threshold = " + this.threshold );
        return buf.toString();
    }
}
