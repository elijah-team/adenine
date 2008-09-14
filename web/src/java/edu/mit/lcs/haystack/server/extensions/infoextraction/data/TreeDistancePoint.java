/*
 * Created on Nov 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Mapping;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.TreeDistance;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks 
 * 
 * keep the representation of each tree as a tree. similarity
 * between two trees is the tree edit distance between two trees
 * using ahogue's TreeMapping code.
 */
public class TreeDistancePoint extends DefaultPoint {
    
    public TreeDistancePoint() {
        super(null);
    }

    public TreeDistancePoint(Object arg) {
        super(arg);
    }

    boolean useOptimization = true;

    double threshold = 0.5;

    boolean membersEqual(ICluster A, ICluster B) {
        if (A.numMembers() == B.numMembers()) {

            Iterator aIt = A.iterator();
            Vector mB = B.getMembers();

            while (aIt.hasNext()) {
                IPoint pa = (IPoint) aIt.next();
                if (!mB.contains(pa)) {
                    return false;
                }
            }
            return true;

        }
        return false;
    }

    /**
     * compares the similarity of a point with a cluster. returns the average
     * similarity between this point and the cluster
     */
    public double p2cDistance(ICluster B) {
        Iterator it = B.getMembers().iterator();
        double distSum = 0;
        int count = 0;
        while (it.hasNext()) {
            IPoint p = (IPoint) it.next();
            if (p == this) {
                continue;
            }
            distSum += this.p2pDistance(p);
            count++;
        }

        if (count == 0) {
            return 0;
        } else {
            return distSum / (double) count;
        }
    }

    /**
     * compares the similarity of a point with a cluster. returns the average
     * similarity between this point and the cluster
     */
    public double c2pDistance(IPoint B) {
        ICluster A = (ICluster)this.getAssociation();
        Iterator it = A.iterator();
        double distSum = 0;
        int count = 0;
        while (it.hasNext()) {
            TreeDistancePoint p = (TreeDistancePoint) it.next();
            if (p == this) {
                continue;
            }
            distSum += p.p2pDistance(B);
            count++;
        }

        if (count == 0) {
            return 0;
        } else {
            return distSum / (double) count;
        }
    }

    /**
     * compares two virtual centroid points.
     */
    public double c2cDistance(ICluster B) {
        double distSum = 0;
        int count = 0;

        ICluster A = (ICluster) this.getAssociation();

        if (membersEqual(A, B)) {
            return 1.0;
        }

        Iterator itA = A.iterator();

        while (itA.hasNext()) {
            TreeDistancePoint pa = (TreeDistancePoint) itA.next();
            if (pa == this) {
                continue;
            }

            Iterator itB = B.iterator();

            while (itB.hasNext()) {
                IPoint pb = (IPoint) itB.next();
                if (pb == this) {
                    continue;
                }

                distSum += pa.p2pDistance(pb);
                count++;
            }
        }

        if (count == 0) {
            return 0;
        } else {
            return distSum / (double) count;
        }
    }

    public double p2pDistance(IPoint B) {
        if (this.pointCollection != null) {
            if (this.pointCollection.hasDistance((IPoint)this, B)) {
                return this.pointCollection.getDistance(this, B);
            } else {
            }
        }

        double dist = 1 - this.p2pSimilarity(B);
        
        if (dist < 0 || dist < 1e-15) {
            dist = 0; //HACK around numerical imprecision
        }
        if (this.pointCollection != null) {
            this.pointCollection.setDistance(this, B, dist);
        }
        return dist;
    }
    
    public double p2pSimilarity(IPoint B) {
        double cutoffPercentage = 1.0;
        double distance = 1.0;

        //System.err.println("p2p: " + this.getUniqueID() + " vs " + B.getUniqueID() + " START");
        IPoint A = this;

        INode root1 = (INode) (A.getData());
        INode root2 = (INode) (B.getData());

        double root1Size = root1.getSize();
        double root2Size = root2.getSize();
        Mapping mapping = null;

        if (root1Size > 0 && root2Size > 0) {           
            if (useOptimization) {

                double ratio;
                if (root1Size < root2Size)
                    ratio = root1Size / root2Size;
                else
                    ratio = root2Size / root1Size;

                if ((1 - ratio) > this.threshold) {
                    distance = 1.0;
                } else {
                    mapping = new TreeDistance(root1, root2, cutoffPercentage).getMapping();
                    distance = mapping.getNormalizedCost();
                }

            } else {
                mapping = new TreeDistance(root1, root2, cutoffPercentage).getMapping();
                distance = mapping.getNormalizedCost();
            }
        } else {
            distance = 1.0;
        }

        return (1.0-distance);
    }

    public double distance(IPoint B) {
        if (B.hasAssociation() && B.isCentroid()) {
            if (this.hasAssociation() && this.isCentroid()) {
                return c2cDistance((ICluster) B.getAssociation());
            } else {
                return p2cDistance((ICluster) B.getAssociation());
            }
        } else {
            if (this.hasAssociation() && this.isCentroid()) {
                return c2pDistance(B); 
            } else {
                return p2pDistance(B);
            }
        }
    }

    public boolean isCentroid() {
        return virtualPoint;
    }

    private boolean virtualPoint = false;

    /**
     * for tree similarity we will use a virtual representative
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint#centroid(java.util.Vector)
     */
    public IPoint centroid(ICluster cluster) {
        TreeDistancePoint centroid = new TreeDistancePoint();
        centroid.virtualPoint = true;
        centroid.setAssociation(cluster);
        return centroid;
    }

    public double internalDistance(ICluster c) {
        Iterator itA = c.iterator();
        double total = 0;
        double numMembers = (double) c.numMembers();

        if (numMembers > 0) {
            while (itA.hasNext()) {
                TreeDistancePoint pa = (TreeDistancePoint) itA.next();
                if (pa == this) {
                    continue;
                }

                Iterator itB = c.iterator();
                while (itB.hasNext()) {
                    IPoint pb = (IPoint) itB.next();
                    if (pb == this) {
                        continue;
                    }

                    total += 2 * (1 - pa.p2pSimilarity(pb));
                }
            }
            return total / (numMembers * numMembers);

        }
        return 1.0;

    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.DefaultPoint#setPointCollection(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection)
     */
    public void setPointCollection(IPointCollection ipc) {
        this.pointCollection = ipc;
    }
}