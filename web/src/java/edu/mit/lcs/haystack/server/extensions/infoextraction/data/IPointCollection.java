/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.rec.PointFactory;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;

/**
 * @author yks
 * represents a collection of indexed points
 */
public interface IPointCollection {
    void addPoint( IPoint point );
    void removePoint( IPoint point );
    IPointCollection copy();
    
    boolean hasDistance(IPoint pointA, IPoint pointB);
    void setDistance(IPoint pointA, IPoint pointB, double dist);
    
    double getDistance(IPoint pointA, IPoint pointB);
    public int size();
    public Iterator iterator();
    public boolean hasPoint(IPoint p);
    public IPoint getNearestNeighbour(IPoint point);
    public Vector/*IPoint*/ getNearestNeighbours(IPoint point);

    public Vector/*IPoint*/ getClosestPoints();
    public IPoint getPoint(int index);
    
    public void clearAssociations();
    public void initialize();
    public void setProgressMonitor(IProgressMonitor ipm);
    public void setPointFactory(PointFactory pf);
    public PointFactory getPointFactory();
}
