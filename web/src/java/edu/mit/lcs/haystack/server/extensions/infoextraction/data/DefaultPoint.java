/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public abstract class DefaultPoint implements IRelatablePoint  {
    IPointCollection pointCollection = null;
    
    IAugmentedNode node = null;
    int index;
    Object association = null;
    
    IRelatablePoint parent = null;
    Vector/*IPoint*/children = new Vector();
    
    static int nextUniqueID = 0;
    
    int uniqueID;
    
    public int hashCode() {
        return uniqueID;
    }
    
    public DefaultPoint(Object arg) {
        this.node = (IAugmentedNode) arg;
        this.uniqueID = nextUniqueID++;
    }

    private int cachedSize;
    
    public int getSize() {
        if (cachedSize > 0) {
            return cachedSize;
        } else {
            int size = 1;
            for (int i = 0; i < children.size(); i++) {
                IRelatablePoint child = (IRelatablePoint)children.get(i);
                size += child.getSize();
            }
            cachedSize = size;
            return size;
        }
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getUniqueID() {
        return Integer.toString(this.uniqueID);
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint#clearAssociations()
     */
    public void clearAssociations() {
        association = null;
    }

    public Object getAssociation() {
        return association;
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint#setAssociation(java.lang.Object)
     */
    public void setAssociation(Object object) {
        association = object;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint#hasAssociation()
     */
    public boolean hasAssociation() {
        return (association != null);
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint#getData()
     */
    public final Object getData() {
        return node;
    }

    public boolean dataEquals(IPoint p) {
        Object a = p.getData();
        Object b = getData();
        if (a != null && b != null) 
            return ((INode)a).getNodeName().equals(((INode)b).getNodeName());
        else
            return a == b;
    }

    public void setParent(IPoint p) {
    }
        
    public Vector/*IPoint*/ getChildren() {
        return children;
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IRelatablePoint#getParent()
     */
    public IRelatablePoint getParent() {
        return parent;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IRelatablePoint#setParent(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IRelatablePoint)
     */
    public void setParent(IRelatablePoint p) {
        parent = p;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IRelatablePoint#addChild(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IRelatablePoint)
     */
    public void addChild(IRelatablePoint p) {
        children.add(p);
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint#similarity(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint)
     */
    abstract public double distance(IPoint p);

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint#centroid(java.util.Vector)
     */
    abstract public IPoint centroid(ICluster c);

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint#internalDistance(edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster)
     */
    public double internalDistance(ICluster c) {
        Vector members = c.getMembers();
        /* distance between self and self is 0.0
         * so we can ignore them in sums 
         */
        double total = 0;
        int numMem = members.size();
        
        if (numMem == 0) {
            return 0.0;
        }
        
        for (int i = 0; i < numMem; i++) {
            for (int j = i+1; j < numMem; j++) {
                
                IPoint pi = (IPoint)members.get(i);
                IPoint pj = (IPoint)members.get(j);

                IPointCollection ipc = pi.getPointCollection();
                
                double d;
                if (! this.pointCollection.hasDistance(pi, pj)) {
                    d = pi.distance(pj);
                    System.err.println("Not cached: pointSize: " + ipc.size());
                    
                } else {
                    d = this.pointCollection.getDistance(pi, pj);
                }
                total += 2*d;
                
            }
            
        }
        double internalDist = total / (double) (numMem*numMem);
        return internalDist;
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint#setPointCollection(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection)
     */
    abstract public void setPointCollection(IPointCollection ipc);
    
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint#getPointCollection()
     */
    public IPointCollection getPointCollection() {
        return this.pointCollection;
    }
    
    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint#isCentroid()
     */
    public boolean isCentroid() {
        return false;
    }
    
    /* for displaying a tree of IRelatablePoints */
    public String toString(int depth, String indent) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < depth; i++) {
            out.append(indent);
        }
        out.append(this.getData().toString());
        out.append("\n");
        Vector children = this.getChildren();
        
        for (int i = 0; i < children.size(); i++) {
            out.append(((IRelatablePoint) children.get(i)).toString(depth + 1,
                    indent));
        }
        return out.toString();
    }
}
