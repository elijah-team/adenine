/*
 * Created on Nov 29, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.DistanceMatrix;
import edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.ObjectMatrix;
import edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.ObjectMatrixOpt;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class SequenceAlignerOld {
    double gapPenalty = 0;

    Vector row;

    Vector column;

    static IRelatablePoint gapPoint = new TreeDistancePoint(null);

    static ObjectMatrixOpt cache;

    static {
        clearCache();
    }

    static public void clearCache() {
        cache = new ObjectMatrixOpt();
    }

    static int gcCounter = 0;

    boolean debug_trace = false;

    public SequenceAlignerOld() {
    }

    public double getGapPenalty(int span) {
        return span * gapPenalty;
    }

    /**
     * Given a vector of points describing the column
     * 
     * @param col
     * @param start
     * @param end
     * @param row
     * @param alignmentMatrix
     * @return
     */
    int maxColumn(Vector col, int start, int end, IRelatablePoint row, DistanceMatrix alignmentMatrix) {
        double max = 0;
        int maxIndex = start;
        for (int i = start; i < end; i++) {
            IRelatablePoint c = (IRelatablePoint) col.get(i);
            double cost = alignmentMatrix.getEntry(row, c);
            double score = cost - getGapPenalty(end - i);
            if (score > max) {
                max = score;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    int maxRow(Vector row, int start, int end, IRelatablePoint col, DistanceMatrix alignmentMatrix) {
        double max = 0;
        int maxIndex = start;
        for (int j = start; j < end; j++) {
            IRelatablePoint r = (IRelatablePoint) row.get(j);
            double cost = alignmentMatrix.getEntry(r, col);
            double score = cost - getGapPenalty(end - j);
            if (score > max) {
                max = score;
                maxIndex = j;
            }
        }
        return maxIndex;
    }
    

    /**
     * find the first max value from an array
     * 
     * @param arr
     * @return
     */
    int getMaxIndex(double[] arr) {
        if (arr != null && arr.length > 0) {
            int maxI = 0;
            double max = arr[maxI];
            for (int i = 1; i < arr.length; i++) {
                if (arr[i] > max) {
                    max = arr[i];
                    maxI = i;
                }
            }
            return maxI;
        } else {
            return 0;
        }
    }

    /**
     * assume that vector a and b are already padded with a 0th element with the
     * a gap element
     * 
     * @param a
     * @param b
     * @param traceBackMat
     * @return
     */
    public DistanceMatrix buildMatrix(Vector a, Vector b, ObjectMatrix traceBackMat) {
        DistanceMatrix scoreMat = new DistanceMatrix();

        Vector aPoints = new Vector(a);
        aPoints.insertElementAt(gapPoint, 0);

        Vector bPoints = new Vector(b);
        bPoints.insertElementAt(gapPoint, 0);

        /** initialize the gap penalties */
        for (int i = 0; i < aPoints.size(); i++) {
            IRelatablePoint ap = (IRelatablePoint) aPoints.get(i);
            scoreMat.putEntry(ap, gapPoint, this.gapPenalty * i);
        }

        for (int j = 0; j < bPoints.size(); j++) {
            IRelatablePoint bp = (IRelatablePoint) bPoints.get(j);
            scoreMat.putEntry(gapPoint, bp, this.gapPenalty * j);
        }

        for (int ai = 1; ai < aPoints.size(); ai++) {
            IRelatablePoint aip = (IRelatablePoint) aPoints.get(ai);
            for (int bi = 1; bi < bPoints.size(); bi++) {
                IRelatablePoint bip = (IRelatablePoint) bPoints.get(bi);

                double maxes[] = new double[4];
                Object traces[] = new Object[4];
                /* diagonal */
                IRelatablePoint am1 = (IRelatablePoint) aPoints.get(ai - 1);
                IRelatablePoint bm1 = (IRelatablePoint) bPoints.get(bi - 1);

                /* Cost from the diagonal */
                double h_am1_bm1 = scoreMat.getEntry(am1, bm1);

                double alignScore = 0;
                TreeMapping mapping = this.getBestAlignment(aip, bip);
                alignScore = mapping.getScore();

                INode aipn = (INode) aip.getData();
                INode bipn = (INode) bip.getData();

                maxes[0] = h_am1_bm1 + alignScore;
                /* point to the diagonal */
                traces[0] = traceBackMat.getEntry(am1, bm1);

                /* Maximum in the current row */
                //int aMaxIndex = maxRow(aPoints, 0, ai, bip, scoreMat);
                int aMaxIndex = ai - 1;
                IRelatablePoint aMax = (IRelatablePoint) aPoints.get(aMaxIndex);
                double h_am1 = scoreMat.getEntry(aMax, bip);
                maxes[1] = h_am1;
                traces[1] = traceBackMat.getEntry(aMax, bip);

                /* Maximum in the current column */
                //int bMaxIndex = maxColumn(bPoints, 0, bi, aip, scoreMat);
                int bMaxIndex = bi - 1;
                IRelatablePoint bMax = (IRelatablePoint) bPoints.get(bMaxIndex);
                double h_bm1 = scoreMat.getEntry(aip, bMax);
                maxes[2] = h_bm1;
                traces[2] = traceBackMat.getEntry(aip, bMax);

                /* no matches */
                maxes[3] = 0;
                traces[3] = null;

                int maxIndex = getMaxIndex(maxes);

                traceBackMat.putEntry(aip, bip, new Trace(aip, bip, (Trace) traces[maxIndex]));

                scoreMat.putEntry(aip, bip, maxes[maxIndex]);
            }
        }
        return scoreMat;
    }


    public TreeMapping getBestAlignment(IRelatablePoint node1, IRelatablePoint node2) {

        if (cache.hasEntry(node1, node2)) {
            return (TreeMapping) cache.getEntry(node1, node2);
        }

        TreeMapping map = generateMapping(node1, node2);

        if (!cache.isFull()) {
            cache.putEntrySymmetric(node1, node2, map);
        }
        return map;
    }

    private int numRowGaps(IRelatablePoint cur, IRelatablePoint last, ObjectMatrix matrix) {
        return matrix.getRowOffset(cur, last);
    }

    private int numColGaps(IRelatablePoint cur, IRelatablePoint last, ObjectMatrix matrix) {
        return matrix.getColOffset(cur, last);
    }

    private IRelatablePoint getRowPoint(IRelatablePoint p, int offset, ObjectMatrix matrix) {
        return (IRelatablePoint) matrix.getRowPointByOffset(p, offset);
    }

    private IRelatablePoint getColPoint(IRelatablePoint p, int offset, ObjectMatrix matrix) {
        return (IRelatablePoint) matrix.getColPointByOffset(p, offset);
    }

    

    private TreeMapping generateMapping(IRelatablePoint node1, IRelatablePoint node2) {
        Vector seq1 = node1.getChildren();
        Vector seq2 = node2.getChildren();

        int seq1Size = seq1.size();
        int seq2Size = seq2.size();

        TreeMapping map = TreeMapping.getMapping(node1, node2);

        if (seq1Size == 0 && seq2Size == 0) {
            /* leaf nodes */
            return map;

        } else if (seq1Size > 0 && seq2Size == 0) {
            /* node1 is non-leaf; node2 is a leaf */
            return map;

        } else if (seq2Size > 0 && seq1Size == 0) {
            /* node1 is leaf; node2 is a non-leaf */
            return map;

        } else if (!node1.dataEquals(node2)) {
            /*
             * treat the unalign data case as special
             */
            return map;
        } else {
            /* both nodes are non-leaf and are equal to each other */
            ObjectMatrix traceBackMat = new ObjectMatrix();
            DistanceMatrix alignmentMatrix = buildMatrix(seq1, seq2, traceBackMat);

            if (debug_trace) {
                System.err.print("MATRIX: \n" + alignmentMatrix.toString());
            }

            Coord maxCoord = findMax(seq1, seq2, alignmentMatrix);

            IRelatablePoint nextA, nextB;
            Trace trace = (Trace) traceBackMat.getEntry(maxCoord.getA(), maxCoord.getB());
            IRelatablePoint curA = null, curB = null;
            if (true)
            /** follow the traceback */
            {
                while (true) {

                    if (trace != null) {
                        nextA = trace.getA();
                        nextB = trace.getB();
                    } else {
                        nextA = null;
                        nextB = null;
                    }

                    if (debug_trace) {
                        if (nextA != null) {
                            System.err.println("TRACE: " + nextA.getData() + "/" + nextA.getUniqueID());
                            System.err.println("\t" + nextB.getData() + "/" + nextB.getUniqueID());
                        } else {
                            System.err.println("TRACE: " + nextA);
                            System.err.println("\t" + nextB);
                        }
                    }

                    int numAGaps = numRowGaps(curA, nextA, traceBackMat);
                    int numBGaps = numColGaps(curB, nextB, traceBackMat);

                    if (numAGaps > 0 && numAGaps == 0) {

                        for (int i = numAGaps - 1; i >= 0; i--) {
                            IRelatablePoint curAP = getRowPoint(nextA, i, traceBackMat);
                            map.prependMapping(TreeMapping.getMapping(curAP, TreeMapping.gap));
                        }

                    } else if (numBGaps > 0 && numAGaps == 0) {

                        for (int i = numBGaps - 1; i >= 0; i--) {
                            IRelatablePoint curBP = getColPoint(nextB, i, traceBackMat);
                            map.prependMapping(TreeMapping.getMapping(TreeMapping.gap, curBP));
                        }

                    } else {
                        if (curA != null && curB != null) {
                            map.prependMapping(getBestAlignment(curA, curB));
                        }
                    }

                    curA = nextA;
                    curB = nextB;

                    if (trace == null) {
                        break;
                    } else {
                        trace = trace.followTrace();
                    }
                }
            }

            /** fill in the gaps at the beginning */
            if (true) {
                int numAGaps = numRowGaps(curA, null, traceBackMat);
                int numBGaps = numColGaps(curB, null, traceBackMat);

                if (numAGaps > 0 && numBGaps == 0) {

                    for (int i = numAGaps - 1; i >= 0; i--) {
                        IRelatablePoint curAP = getRowPoint(null, i, traceBackMat);
                        map.prependMapping(TreeMapping.getMapping(curAP, TreeMapping.gap));
                    }
                } else if (numBGaps > 0 && numAGaps == 0) {
                    for (int i = numBGaps - 1; i >= 0; i--) {
                        IRelatablePoint curBP = getColPoint(null, i, traceBackMat);
                        map.prependMapping(TreeMapping.getMapping(TreeMapping.gap, curBP));
                    }
                } else {
                    if (curA != null && curB != null) {
                        /** recursion */
                        map.prependMapping(getBestAlignment(curA, curB));
                    }
                }
            }

            /* clean up the matrices */
            traceBackMat = null;
            alignmentMatrix = null;

            gcCounter++;
            if (gcCounter % 10000 == 0) {
                System.gc();
                System.err.println("GARBAGE COLLECTION at counter: " + gcCounter);
                System.err.flush();
            }
            if (gcCounter % 1000 == 0) {
                System.err.println("MAP_CACHE: SIZE/LIMIT " + cache.size() + "/" + cache.limit());
                System.err.println("TREE_MAP: ALLOC/CALLED/CACHE_HIT/CACHED: " + TreeMapping.numAllocated + "/" + TreeMapping.numCalled + "/" + TreeMapping.numCacheHit + "/" + TreeMapping.numCached);
                System.err.flush();
            }
            return map;
        }
    }

    /**
     * returns the largest value in the last row and column of the matrix
     * 
     * @param aVec
     * @param bVec
     * @param matrix
     * @return
     */
    private Coord findMax(Vector aVec, Vector bVec, DistanceMatrix matrix) {
        Coord maxCoord = null;

        Vector aPoints = new Vector(aVec);
        aPoints.insertElementAt(gapPoint, 0);

        Vector bPoints = new Vector(bVec);
        bPoints.insertElementAt(gapPoint, 0);

        double max = Double.NEGATIVE_INFINITY;
        for (int a = aPoints.size() - 1; a >= 0; a--) {
            IRelatablePoint aip = (IRelatablePoint) aPoints.get(a);
            int b = bPoints.size() - 1;
            IRelatablePoint bip = (IRelatablePoint) bPoints.get(b);
            double score = matrix.getEntry(aip, bip);

            if (score > max) {
                max = score;
                maxCoord = new Coord(aip, bip);
            }

        }

        int a = aPoints.size() - 1;
        IRelatablePoint aip = (IRelatablePoint) aPoints.get(a);
        for (int b = bPoints.size() - 1; b >= 0; b--) {
            IRelatablePoint bip = (IRelatablePoint) bPoints.get(b);

            double score = matrix.getEntry(aip, bip);

            if (score > max) {
                max = score;
                maxCoord = new Coord(aip, bip);
            }
        }

        return maxCoord;
    }
    
    class Trace {
        private Trace next;

        private IRelatablePoint a;

        private IRelatablePoint b;

        public IRelatablePoint getA() {
            return a;
        }

        public IRelatablePoint getB() {
            return b;
        }

        public Trace(IRelatablePoint aip, IRelatablePoint bip, Trace next) {
            this.next = next;
            this.a = aip;
            this.b = bip;
        }

        public Trace followTrace() {
            return next;
        }
    }

    class Coord {
        private IRelatablePoint a;

        private IRelatablePoint b;

        public IRelatablePoint getA() {
            return a;
        }

        public IRelatablePoint getB() {
            return b;
        }

        public Coord(IRelatablePoint a, IRelatablePoint b) {
            this.a = a;
            this.b = b;
        }
    }
}