/*
 * Created on Nov 21, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.DefaultCluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPointCollection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;

/**
 * @author yks
 */
public class KMeans implements IClusterAlgorithm {
    IPointCollection points;
    private IProgressMonitor ipm;

    public final static int INIT_UNIFORM = 1;
    public final static int INIT_RANDOM = 2;

    public final static int DEFAULT_K = 6;
    public final static int DEFAULT_ITERATIONS = 10;

    int K = DEFAULT_K;
    int iterations = DEFAULT_ITERATIONS;
    int initialization = INIT_RANDOM;
    
    protected Vector/*ICluster*/ clusters = new Vector();
    protected Vector/*IPoint*/ centroids = new Vector();
    
    public KMeans() {
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("KMeans: K = " + K + " ; iterations = " + iterations);
        buf.append(" initialization: " + ((initialization==INIT_RANDOM)? "random" : "uniform" ) );
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#setData(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection)
     */
    public void setPointCollection(IPointCollection pointCollection) {
        this.points = pointCollection;
        
        System.err.println("KMeans: initialized with: " + points.size() + " points");
        for (int j = 0; j < K; j++) {
            clusters.add( new DefaultCluster() );
        }
    }

    public void setProgressMonitor(IProgressMonitor ipm) {
        this.ipm = ipm;
        if (points != null) {
            this.points.setProgressMonitor( ipm );
        }
    }
    
    public void setIterations(int iter) {
        iterations = iter;
    }
    
    public void setK(int k) {
        this.K = k;
    }
    
    public void initCentroids() {
        switch(initialization) {
        case INIT_UNIFORM:
            initCentroidsUniform();
            break;
        case INIT_RANDOM:
            initCentroidsRandom();
            break;
        default:
            initCentroidsRandom();
        }
        this.points.initialize();
//        System.err.println("KMeans.initialCentroids: " + 
//                ClusterSerializer.prettyResult(this.clusters, null, null));
    }
    
    /**
     * Split data points into K partitions and initialize centroids based on
     */
    public void initCentroidsUniform() {
        for (int c = 0; c < K; c++) {
            clusters.add( new DefaultCluster() );
            centroids.add( c, null);
        }
    
        for (int i = 0; i < points.size(); i++) {
            int k = i % K;
            IPoint p = points.getPoint(i);
            ICluster cluster = (ICluster) clusters.get(k);
            cluster.addMember(p);
            p.setAssociation(cluster);
        }
    }
    
    public void initCentroidsRandom() {
        for (int c = 0; c < K; c++) {
            clusters.add( new DefaultCluster() );
            centroids.add( c, null);
        }

        for (int i = 0; i < points.size(); i++) {
            int k = (int) Math.floor(Math.random() * K);
            IPoint p = points.getPoint(i);
            
            //System.err.println(p.getUniqueID() + ": " +(INode) p.getData() );
            
            DefaultCluster cluster = (DefaultCluster) clusters.get(k);
            cluster.addMember(p);
            p.setAssociation(cluster);
        }
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#run()
     */
    public void run() {
        initCentroids();
        iterate();
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IClusterAlgorithm#getClusters()
     */
    public Vector getClusters() {
        return clusters;
    }
        
    public void maximization() {
        /* initialized the new clusters */
        Vector/*ICluster*/ newClusters = new Vector();
        
        for (int j = 0; j < K; j++) {
            newClusters.add( new DefaultCluster() );
        }

        for (int i = 0; i < points.size(); i++) {
            IPoint p = (IPoint)points.getPoint(i);

            double min = Double.MAX_VALUE;
            int pi = 0;
 
            for (int j = 0; j < K; j++) {
                ICluster cluster = (ICluster) clusters.get(j);
                IPoint centroid = (IPoint) centroids.get(j);
         
                double dist = p.distance(centroid);
                
                if (dist < min) {
                    min = dist;
                    pi = j;
                }
            }

            ICluster associateTo = (ICluster)newClusters.get(pi);
            associateTo.addMember(p);
            p.setAssociation(associateTo);
            
        }
        /* set to the new clusters */
        clusters = newClusters;
    }

    public double expectation() {
        double diff = 0;
        Vector newCentroids = new Vector();
        for (int c = 0; c < K; c++) {
            ICluster cluster = (ICluster)clusters.get(c);
            
            IPoint newCentroid = points.getPointFactory().makeCentroid(cluster);
            IPoint oldCentroid = (IPoint)centroids.get(c);
            
            double dist = 0.0;
            if (oldCentroid != null) {
                dist = oldCentroid.distance( newCentroid );
            } else {
                dist = 1.0;
            }
            
            newCentroids.add(c, newCentroid );
            
            diff += dist;
        }
        centroids = newCentroids;
        return diff;
    }

    /**
     * run one cycle of K-Means
     *  
     */
    public int iterate() {
        double last = -1;
        int i;
        for (i = 0; i < iterations; i++) {
            double sum = expectation();

            System.err.println("iteration: " + i + " sum: "+ sum);

            if (sum == 0 || sum < 1e-10) {
                System.err.println("Stopping criterion reached after " + i + " iterations.");
                break;
            }
            maximization();
            last = sum;
        }
        return i;
    }
}
