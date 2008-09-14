/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;

/**
 * @author yks
 * represents a single clusterable point
 */
public interface IPoint {
    
    public void clearAssociations();
    public void setAssociation(Object object);
    public Object getAssociation();
    public boolean hasAssociation();
    
    public double distance(IPoint p);
    /**
     * an IPoint implementation-specific function that
     * returns the centroid of a collection of points
     * of the same class
     */
    public double internalDistance(ICluster c);
    public IPoint centroid(ICluster c);
    public Object getData();
    public boolean dataEquals(IPoint b);
    
    public String getUniqueID();
    public boolean isCentroid();
    public void setPointCollection(IPointCollection ipc);
    public IPointCollection getPointCollection();
}
