/*
 * Created on Feb 10, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;

/**
 * @author yks
 * 
 * Simplest of all representation. Each tree as the size of
 * its self.
 */
public class TreeSizePoint extends DefaultPoint {

    double size;
    
    public TreeSizePoint() {
        super(null);
        size = 0;
    }

    public TreeSizePoint(Object arg) {
        super(arg);

        if (this.node != null) {
            this.size = this.node.getSize();
        }
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.DefaultPoint#distance(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint)
     */
    public double distance(IPoint p) {
        TreeSizePoint b = (TreeSizePoint)p;
        
        if (this.size > b.size) {
            return this.size - b.size;
        } else {
            return b.size - this.size;
        }
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.DefaultPoint#centroid(edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster)
     */
    public IPoint centroid(ICluster c) {
        Vector /*IPoint*/ points = c.getMembers();
        int numMembers = c.numMembers();

        Iterator it = points.iterator();        
        int sum = 0;
        while (it.hasNext()) {
            TreeSizePoint cur = (TreeSizePoint) it.next();
            sum += cur.size;
        }
        
        TreeSizePoint centroid = new TreeSizePoint();
        centroid.size = sum / numMembers;
        return centroid;
    }

    private String _string = null;

    public String toString() {
        if (_string != null) {
            return _string;
        } else {
            _string = new String("TreeSize: " + size);
        }
        return _string;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.DefaultPoint#setPointCollection(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPointCollection)
     */
    public void setPointCollection(IPointCollection ipc) {
        this.pointCollection = ipc;
    }

}
