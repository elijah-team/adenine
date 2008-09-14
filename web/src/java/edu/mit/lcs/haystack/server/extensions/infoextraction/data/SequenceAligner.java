/*
 * Created on Feb 8, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.ObjectMatrixOpt;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Mapping;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.TreeDistance;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class SequenceAligner {
        
    private double gapPenalty = 0;

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

    public SequenceAligner() {
    }

    public double getGapPenalty(int span) {
        return span * gapPenalty;
    }

    int maxA(int start, int end, int b, TraceCell[][] matrix) {
        double max = 0;
        int maxIndex = start;
        for (int a = end; a >= start; a--) {
            double score = matrix[a][b].getScore();
            if (score > max) {
                max = score;
                maxIndex = a;
            }
        }
        return maxIndex;    
    }

    int maxB(int start, int end, int a, TraceCell[][] matrix) {
        double max = 0;
        int maxIndex = start;
        for (int b = end; b >= start; b--) {
            double score = matrix[a][b].getScore();
            if (score > max) {
                max = score;
                maxIndex = b;
            }
        }
        return maxIndex;            
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
    public TraceCell[][] buildMatrix(Vector a, Vector b) {
        int aSize = a.size();
        int bSize = b.size();
        TraceCell[][] scoreMat = new TraceCell[aSize + 1][bSize + 1];

        /** initialize the gap penalties */

        for (int i = 0; i < aSize + 1; i++) {
            scoreMat[i][0] = new TraceCell(i, 0, 0.0 /* gapscore */, null);
        }

        for (int j = 1; j < bSize + 1; j++) {
            scoreMat[0][j] = new TraceCell(0, j, 0.0 /* gapscore */, null);
        }

        double[] scores = new double[3];
        int[] aTrace = new int[3];
        int[] bTrace = new int[3];

        for (int ai = 1; ai < aSize + 1; ai++) {
            /*
             * indices are off by 1 because of gap points
             */
            IRelatablePoint aip = (IRelatablePoint) a.get(ai - 1);
            for (int bi = 1; bi < bSize + 1; bi++) {
                IRelatablePoint bip = (IRelatablePoint) b.get(bi - 1);

                /* diagonal */
                double h_am1_bm1 = scoreMat[ai - 1][bi - 1].getScore();
                TreeMapping mapping = this.getBestAlignment(aip, bip);
                double alignScore = mapping.getScore();
                scores[0] = h_am1_bm1 + alignScore;
                aTrace[0] = ai - 1;
                bTrace[0] = bi - 1;

                /* maximum in the current row */
                int aMaxIndex = maxA(0, ai-1, bi, scoreMat);
                double h_am1 = scoreMat[aMaxIndex][bi].getScore();
                scores[1] = h_am1;
                aTrace[1] = aMaxIndex;
                bTrace[1] = bi;

                /* Maximum in the current column */
                int bMaxIndex = maxB(0, bi-1, ai, scoreMat);
                //int bMaxIndex = bi - 1;
                double h_bm1 = scoreMat[ai][bMaxIndex].getScore();
                scores[2] = h_bm1;
                aTrace[2] = ai;
                bTrace[2] = bMaxIndex;

                /* find the max score */
                int maxIndex;
                if (scores[0] >= scores[1] && scores[0] >= scores[2]) {
                    maxIndex = 0;
                } else if (scores[1] >= scores[0] && scores[1] >= scores[2]) {
                    maxIndex = 1;
                } else {
                    maxIndex = 2;
                }

                scoreMat[ai][bi] = new TraceCell(ai, bi, scores[maxIndex], scoreMat[aTrace[maxIndex]][bTrace[maxIndex]]);
            }
        }
        return scoreMat;
    }
    
    TreeMapping generateFakeMap(IRelatablePoint a, IRelatablePoint b) {
        return null;
    }
    
    public TreeMapping getBestAlignment(IRelatablePoint nodeA, IRelatablePoint nodeB) {
        TreeMapping map = null;
        
        if (cache.hasEntry(nodeA, nodeB)) {
            return (TreeMapping) cache.getEntry(nodeA, nodeB);
        }
        
        map = generateMapping(nodeA, nodeB);
        
        if (!cache.isFull()) {
            cache.putEntrySymmetric(nodeA, nodeB, map);
        }
        return map;
    }
    
    private TreeMapping generateMapping(IRelatablePoint nodeA, IRelatablePoint nodeB) {
        Vector seqA = nodeA.getChildren();
        Vector seqB = nodeB.getChildren();

        int seqASize = seqA.size();
        int seqBSize = seqB.size();

        /* size optimization saves us from comparing
         * trees that have a certain size difference
         * instead we assigned the mapping an optimistic bound
         * namely the size of the smaller tree.
         */
        
        TreeMapping map = TreeMapping.getMapping(nodeA, nodeB);

        if (seqASize == 0 && seqBSize == 0) {
            /* leaf nodes */
            return map;

        } else if (seqASize > 0 && seqBSize == 0) {
            /* node1 is non-leaf; node2 is a leaf */
            return map;

        } else if (seqBSize > 0 && seqASize == 0) {
            /* node1 is leaf; node2 is a non-leaf */
            return map;

//        } else if (!nodeA.dataEquals(nodeB)) {
//            /*
//             * treat the unalign data case as special
//             */
//            return map;
        } else {
            /* both nodes are non-leaf and are equal to each other */
            
            TraceCell[][] alignmentMatrix = buildMatrix(seqA, seqB);
            //debug_trace = true;

            if (debug_trace) {
                System.err.print("sequenceA: ");
                for (int i = 0; i < seqA.size(); i++) {
                    IRelatablePoint irp = (IRelatablePoint) seqA.get(i);
                    INode n = (INode) irp.getData();
                    System.err.print(n.getNodeName() + "\t");
                }
                System.err.println();
                System.err.print("sequenceB: ");
                for (int i = 0; i < seqB.size(); i++) {
                    IRelatablePoint irp = (IRelatablePoint) seqB.get(i);
                    INode n = (INode) irp.getData();
                    System.err.print(n.getNodeName() + "\t");
                }
                System.err.println();

                System.err.println("MATRIX: ");

                System.err.println(traceMatrix2String(alignmentMatrix, 10));
            }

            TraceCell cur = findMax2(alignmentMatrix);
            TraceCell next;

            if (debug_trace) {
                System.err.println("max: " + cur.getA() + "/" + cur.getB());
            }
            int nextA, nextB;
            int curA, curB;
            /** follow the traceback */
            while (true) {
                curA = cur.getA();
                curB = cur.getB();

                next = cur.followTrace();
                if (next != null) {
                    nextA = next.getA();
                    nextB = next.getB();
                } else {
                    nextA = 0;
                    nextB = 0;
                }

                if (debug_trace) {
                    System.err.println("TRACE: (" + curA + "/" + curB + ") => (" + nextA + "/" + nextB + ")");
                }

                int numAGaps = curA - nextA;
                int numBGaps = curB - nextB;

                if (numAGaps > 0 && numBGaps == 0) {
                    /* there are gaps in B */
                    for (int i = numAGaps; i >= 1; i--) {
                        /*
                         * seqA.get(i - 1) because indices are off by one because of the
                         * gap nodes in the matrix indices
                         */
                        IRelatablePoint curAP = (IRelatablePoint) seqA.get(i - 1);
                        map.prependMapping(TreeMapping.getMapping(curAP, TreeMapping.gap));
                    }

                } else if (numBGaps > 0 && numAGaps == 0) {
                    /* there are gaps in A */
                    for (int i = numBGaps; i >= 1; i--) {
                        IRelatablePoint curBP = (IRelatablePoint) seqB.get(i - 1);
                        map.prependMapping(TreeMapping.getMapping(TreeMapping.gap, curBP));
                    }

                } else if (curA != 0 && curB != 0) {
                    IRelatablePoint curAP = (IRelatablePoint) seqA.get(cur.getA() - 1);
                    IRelatablePoint curBP = (IRelatablePoint) seqB.get(cur.getB() - 1);
                    map.prependMapping(getBestAlignment(curAP, curBP));
                }

                if (next == null) {
                    break;
                } else {
                    cur = cur.followTrace();
                }
            }
            /* clean up the matrices */
            alignmentMatrix = null;

            if (gcCounter % 1000 == 0) {
                System.err.print(cache.infoString());
                System.err.print(TreeMapping.infoString());
            }
            gcCounter++;

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
    private TraceCell findMax2(TraceCell[][] matrix) {
        TraceCell maxCoord = null;

        double max = Double.NEGATIVE_INFINITY;
        int aSize = matrix.length;
        int bSize = matrix[0].length;
        int maxA;
        int maxB;

        //System.err.println("findMax2: a:" + aSize + " b:" + bSize);
        int a, b;
        b = bSize - 1;
        for (a = aSize - 1; a >= 0; a--) {
            double score = matrix[a][b].getScore();

            if (score > max) {
                max = score;
                maxCoord = matrix[a][b];
            }
        }

        a = aSize - 1;
        for (b = bSize - 2 /* already did bSize -1 */; b >= 0; b--) {
            double score = matrix[a][b].getScore();
            if (score > max) {
                max = score;
                maxCoord = matrix[a][b];
            }
        }

        return maxCoord;
    }

    static String traceMatrix2String(TraceCell[][] matrix, int limit) {
        StringBuffer sb = new StringBuffer();
        int r = matrix.length;
        int c = matrix[0].length;

        for (int i = 0; i < r && i < limit; i++) {
            sb.append("[");
            for (int j = 0; j < c && j < limit; j++) {
                if (j > 0) {
                    sb.append("\t");
                }

                //sb.append(matrix[i][j].getA() + "," + matrix[i][j].getB());
                TraceCell t = matrix[i][j];
                if (t != null && t.followTrace() != null) {
                    TraceCell next = t.followTrace();
                    sb.append(next.getA() + "," + next.getB());
                } else {
                    sb.append("nil");
                }

            }
            sb.append("]");
            sb.append("\n");
        }
        return sb.toString();
    }
    
    static private long treeAlignTask(IRelatablePoint aTree, IRelatablePoint bTree) {
        TreeMapping.clearCache();
        SequenceAligner sa = new SequenceAligner();

        System.err.println("---------------------------------");

        Timer.printTimeElapsed(null);
        Timer.printTimeElapsed("TreeAlignmentTask - START");
        TreeMapping ma = sa.getBestAlignment(aTree, bTree);
        long elapsed = Timer.printTimeElapsed("TreeAlignmentTask - END");


        System.err.println("TreeAlignment.alignment: \n" + ma.toString());
        System.err.println("TreeAlignment.getSize(): " + ma.getSize());
        System.err.println("TreeAlignment.getScore(): " + ma.getScore());

        System.err.println("TreeAlignement.getNormalizedScore(): " + ma.getNormalizedScore());
        System.err.println("SequenceAligner.cached.size(): " + SequenceAligner.cache.size());

        System.err.print("ALLOC/HIT/CALLED/CACHED: ");
        System.err.print(TreeMapping.numAllocated);
        System.err.print("/" + TreeMapping.numCacheHit);
        System.err.print("/" + TreeMapping.numCalled);
        System.err.print("/" + TreeMapping.numCached);
        System.err.println();

        //        System.err.println("CACHE KEYS: ");
        //        Iterator it = TreeMapping.treeMappingCache.keySet().iterator();
        //        while (it.hasNext()) {
        //            String key = (String) it.next();
        //            System.err.println(key);
        //        }
        return elapsed;
    }

    static private long treeDistTask(IRelatablePoint aTree, IRelatablePoint bTree) {
        Timer.printTimeElapsed(null);
        Timer.printTimeElapsed("TreeDistanceTask - START");
        TreeDistance td = new TreeDistance((INode) aTree.getData(), (INode) bTree.getData(), 1.0);
        Mapping mapping = td.getMapping();
        long elapsed = Timer.printTimeElapsed("TreeDistanceTask -- END");

        System.err.println("TreeDistance.getCost(): " + mapping.getCost());
        System.err.println("TreeDistance.getNormalizedCost(): " + mapping.getNormalizedCost());
        return elapsed;
    }

    static String scoreMatrix2String(TraceCell[][] matrix, int limit) {
        StringBuffer sb = new StringBuffer();
        int r = matrix.length;
        int c = matrix[0].length;

        for (int i = 0; i < r && i < limit; i++) {
            sb.append("[");
            for (int j = 0; j < c && j < limit; j++) {
                if (j > 0) {
                    sb.append("\t");
                }

                sb.append(matrix[i][j].getScore());
            }
            sb.append("]");
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        TreeDelimitedFile fileA = new TreeDelimitedFile("a.tree");
        IRelatablePoint aTree = fileA.getContent();

        System.err.println(aTree.toString(0, "\t"));

        TreeDelimitedFile fileB = new TreeDelimitedFile("b.tree");
        IRelatablePoint bTree = fileB.getContent();

        System.err.println(bTree.toString(0, "\t"));

        long tat = 0;
        long tdt = 0;
        int numtests = 10;

        for (int i = 0; i < numtests; i++) {
            tdt += treeDistTask(aTree, bTree);
            tat += treeAlignTask(aTree, bTree);
            System.gc();

        }

        double tatavg = (double) tat / (double) numtests;
        double tdtavg = (double) tdt / (double) numtests;

        System.err.println("Tree-Alignment Average: " + tatavg);
        System.err.println("Tree-Distance Average: " + tdtavg);
    }
    
    class TraceCell {
        private TraceCell next;

        private int a;

        private int b;

        private double score;

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }

        public TraceCell(int a, int b, double score, TraceCell next) {
            this.next = next;
            this.a = a;
            this.b = b;
            this.score = score;
        }

        public TraceCell followTrace() {
            return next;
        }
    }

    class Coordinate {
        private int a;

        private int b;

        public Coordinate(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }
    }
}