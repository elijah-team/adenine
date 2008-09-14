/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.DistanceMatrixOpt;
import edu.mit.lcs.haystack.server.extensions.infoextraction.rec.PointFactory;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks representation of a collection of points (for clustering)
 */
public class DefaultPointCollection implements IPointCollection {
    protected Vector /* IPoint */points = new Vector();

    protected Vector /* IPoint */needsUpdate = new Vector();

    protected DistanceMatrixOpt distances;

    protected IProgressMonitor ipm;

    protected PointFactory pointFactory;

    public void setProgressMonitor(IProgressMonitor ipm) {
        this.ipm = ipm;
    }

    Vector mappingPairs;

    double threshold;

    boolean useOptimization = true;

    public DefaultPointCollection() {
    }

    public DefaultPointCollection(IAugmentedNode root, PointFactory pf) {
        setPointFactory(pf);
        this.generateSubTreesRecursive(root, null);
    }

    public void setPointFactory(PointFactory pf) {
        this.pointFactory = pf;
    }

    public PointFactory getPointFactory() {
        return this.pointFactory;
    }

    public void useOptimization(boolean yes) {
        useOptimization = yes;
    }

    /**
     * clear all node to cluster associations.
     */
    public void clearAssociations() {
        for (int i = 0; i < points.size(); i++) {
            IPoint p = (IPoint) points.get(i);
            p.clearAssociations();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#addPoint(edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPoint)
     */
    public void addPoint(IPoint point) {
        points.add(point);
        point.setPointCollection(this);
        needsUpdate.add(point);
        if (distances != null) {
            generatePartialMappings();
        }
    }

    /**
     * does a post-order traversal of (children visited after the node) the tree
     * and enumerate all the subtrees. (Some irrevalent nodes are filtered.)
     * 
     * @param root
     */
    private void generateSubTreesRecursive(IAugmentedNode root, IRelatablePoint parent) {
        if (Utilities.isValidNode((Node) root)) {
            IRelatablePoint thisPoint = pointFactory.makePoint(root);

            /* estabilish parent child relationships */
            if (parent != null) {
                parent.addChild(thisPoint);
                thisPoint.setParent(parent);
            }

            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                IAugmentedNode child = (IAugmentedNode) children.item(i);
                generateSubTreesRecursive(child, thisPoint);
            }

            /* add children first before add this point */
            addPoint(thisPoint);
        }
    }

    /**
     * a less costly mapping generating
     *  
     */
    private void generatePartialMappings() {

        if (distances == null) {
            generateAllMappings();
            return;
        }

        double dist;
        int end;

        for (int i = needsUpdate.size() - 1; i >= 0; i--) {

            IPoint pointA = (IPoint) needsUpdate.get(i);

            for (int j = points.size() - 1; j >= 0; j--) {

                IPoint pointB = (IPoint) points.get(j);

                if (pointB != pointA) {
                    if (!distances.hasEntry(pointA, pointB)) {
                        dist = pointA.distance(pointB);
                        distances.putEntrySymmetric(pointA, pointB, dist);
                    }
                }
            }
        }

        needsUpdate.clear();

        if (!distances.checkSanity()) {
            Exception e = new Exception();
            e.printStackTrace();
        }
    }

    /**
     * generate all possible mappings of all node pairs in the given tree
     * 
     * @param ipm
     */
    public void generateAllMappings() {
        Timer.printTimeElapsed("generateAllMappings() -- START");
        int dimension = points.size();

        if (distances == null) {
            distances = new DistanceMatrixOpt();
        }

        /* progress bar */
        if (ipm != null) {
            ipm.beginTask("generateAllMappings", dimension);
        }

        int comparisons = 0;
        int total = 0;
        int max = dimension * (dimension - 1) / 2;

        long performance[][] = new long[dimension][dimension];
        long maxTime = 0L;
        int slowest_i = 0, slowest_j = 0;
        long totalTime = 0L;

        for (int i = 0; i < dimension; i++) {
            for (int j = i + 1; j < dimension; j++) {

                Timer.printTimeElapsed(null);
                IPoint pointA = (IPoint) points.get(i);
                IPoint pointB = (IPoint) points.get(j);

                if (!distances.hasEntry(pointA, pointB)) {
                    double dist = pointA.distance(pointB);
                    distances.putEntrySymmetric(pointA, pointB, dist);
                    comparisons++;
                }

                total++;

                long elapsed = Timer.printTimeElapsed(null);
                performance[i][j] = elapsed;
                totalTime += elapsed;
                if (elapsed > maxTime) {
                    slowest_i = i;
                    slowest_j = j;
                    maxTime = elapsed;
                }
            }

            if (ipm != null) {
                ipm.worked(i + 1);
            }
            System.err.println("generateAllMappings() -- ACTUAL/ITERATION/MAX: " + comparisons + "/" + total + "/" + max);
        }

        IRelatablePoint pointA = (IRelatablePoint) points.get(slowest_i);
        IRelatablePoint pointB = (IRelatablePoint) points.get(slowest_j);

        System.err.println("SLOWEST time: " + performance[slowest_i][slowest_j]);
        System.err.println("A: " + pointA.getUniqueID() + "/" + ((INode) pointA.getData()).getNodeName() + "/c=" + pointA.getChildren().size() + "/s=" + pointA.getSize());
        System.err.println("B: " + pointB.getUniqueID() + "/" + ((INode) pointB.getData()).getNodeName() + "/c=" + pointB.getChildren().size() + "/s=" + pointB.getSize());
        System.err.println("TOTAL time: " + totalTime);

        if (ipm != null) {
            ipm.done();
        }
        Timer.printTimeElapsed("generateAllMappings() -- END - comparisons: " + comparisons + "/" + total + ": " + ((double) comparisons / (double) total));

        if (!distances.checkSanity()) {
            Exception e = new Exception();
            e.printStackTrace();
        }

        needsUpdate.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#getDistance(int,
     *      int)
     */
    public double getDistance(IPoint pointA, IPoint pointB) {
        if (distances == null) {
            generateAllMappings();
        }
        return distances.getEntry(pointA, pointB);
    }

    public boolean hasDistance(IPoint pointA, IPoint pointB) {
        if (distances == null) {
            generateAllMappings();
        }
        return distances.hasEntry(pointA, pointB);
    }

    /**
     * dump all the mapping costs to a given output stream
     * 
     * @param out
     */
    private void dumpMappingCosts(PrintStream out) {
        int dimension = size();
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                IPoint a = (IPoint) points.get(i);
                IPoint b = (IPoint) points.get(j);
                out.print(getDistance(a, b));
                if (j < dimension - 1) {
                    out.print(",");
                }
            }
            out.println();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#size()
     */
    public int size() {
        return points.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#iterator()
     */
    public Iterator iterator() {
        return this.points.iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#getPoint(int)
     */
    public IPoint getIthPoint(int index) {
        if (index < 0 || index >= size()) {
            return null;
        } else {
            return (IPoint) this.points.get(index);
        }
    }

    /**
     * find the point that has the lowest distance compared to the given node
     * (index)
     * 
     * @param i
     * @return
     */

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#getNearestNeighbour()
     */
    public IPoint getNearestNeighbour(IPoint p) {
        if (distances == null) {
            generateAllMappings();
        }

        double min = Double.MAX_VALUE;

        IPoint candidate = null;
        Iterator it = points.iterator();
        while (it.hasNext()) {
            IPoint cur = (IPoint) it.next();
            if (cur == p) {
                /* skip comparisons with self */
                continue;
            }

            double dist = distances.getEntry(p, cur);
            if (dist < min) {
                if (dist == -1.0) {
                    System.err.println("getNearest: " + p.getUniqueID() + "/" + cur.getUniqueID());
                }
                min = dist;
                candidate = cur;
            }
        }
        return candidate;
    }

    /**
     * recursively traverse the tree, and generates a feature vector for each
     * node;
     */
    public void generateFeatureVectorsRecursive(Node node) {

        if (Utilities.isValidNode(node)) {
            IAugmentedNode ian = (IAugmentedNode) node;

            BagOfTags bot = new BagOfTags(node);
            bot.normalize();
            points.add(bot);

            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                generateFeatureVectorsRecursive(child);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#getClosestPoints()
     */
    public Vector getClosestPoints() {
        if (distances == null) {
            generateAllMappings();
        }

        IPoint pa = null;
        IPoint pb = null;
        double min = Double.MAX_VALUE;

        Iterator it = points.iterator();
        while (it.hasNext()) {
            IPoint p = (IPoint) it.next();
            IPoint nearest = getNearestNeighbour(p);

            double dist = getDistance(p, nearest);
            if (dist < min) {
                min = dist;
                pa = p;
                pb = nearest;
            }
        }

        Vector closest = new Vector();
        closest.add(pa);
        closest.add(pb);

        if (min == 0) {
            Iterator it2 = points.iterator();
            while (it2.hasNext()) {
                IPoint p = (IPoint) it2.next();
                if (p == pa || p == pb) {
                    continue;
                }
                double dist = getDistance(pa, p);
                if (dist == min) {
                    closest.add(p);
                }
            }
        }
        return closest;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#removePoint(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint)
     */
    public void removePoint(IPoint point) {
        points.remove(point);
        if (distances != null) {
            distances.removeAll(point);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#copy()
     */
    public IPointCollection copy() {

        DefaultPointCollection copy = new DefaultPointCollection();
        copy.setPointFactory(getPointFactory());
        copy.points = new Vector(points);
        copy.setProgressMonitor(ipm);
        if (distances != null) {
            copy.distances = distances.copy();
        }
        return copy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#hasPoint(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint)
     */
    public boolean hasPoint(IPoint p) {
        return points.contains(p);
    }

    public IPoint getPoint(int index) {
        return (IPoint) this.points.get(index);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        for (int i = 0; i < points.size(); i++) {
            IPoint p = (IPoint) points.get(i);
            buf.append(p.getUniqueID() + ",");
        }
        buf.append(")");
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#setDistance(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint,
     *      edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint,
     *      double)
     */
    public void setDistance(IPoint pointA, IPoint pointB, double dist) {
        if (distances != null) {
            distances.putEntrySymmetric(pointA, pointB, dist);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IPointCollection#getNearestNeighbours(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint)
     */
    public Vector getNearestNeighbours(IPoint point) {
        IPoint candidate = this.getNearestNeighbour(point);
        double min = distances.getEntry(candidate, point);
        Vector candidates = new Vector();

        Iterator it = points.iterator();
        while (it.hasNext()) {
            IPoint cur = (IPoint) it.next();
            if (cur == candidate) {
                /* skip comparisons with self */
                continue;
            }

            double dist = distances.getEntry(candidate, cur);
            if (dist == min) {
                candidates.add(cur);
            }
        }
        return candidates;
    }

    public void initialize() {
        generateAllMappings();
    }
}