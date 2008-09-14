/*
 * Created on Jan 29, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IRelatablePoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.TreeDelimitedFile;

/**
 * @author yks
 */
public class MatrixTest {

    public static void postTraverseTree(Vector dm, IRelatablePoint irp) {
        if (irp != null) {
            Vector children = irp.getChildren();
            if (!children.isEmpty()) {
                for (int i = 0; i < children.size(); i++) {
                    IRelatablePoint child = (IRelatablePoint) children.get(i);
                    postTraverseTree(dm, child);
                }
            }
            dm.add(irp);
        }
    }

    public static void testDistanceMatrixOpt(Vector nodes) {
        System.err.println();
        System.err.println("testDistanceMatrixOpt");
        
        DistanceMatrixOpt dm = new DistanceMatrixOpt();

        Timer.printTimeElapsed(null);

        int iCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                dm.putEntry(pi, pj, (double) (i + j));
                iCount++;
            }
        }
        long insertTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        DistanceMatrixOpt dm2 = dm.copy();
        long copyTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        int rCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                double d = dm2.getEntry(pi, pj);
                if (d != (double) (i + j)) {
                    System.err.println("Error! " + i + "/" + j + ": " + d);
                }
                rCount++;
            }
        }
        long readTime = Timer.printTimeElapsed(null);

        String code = "DistanceMatrixOpt";
        System.err.println(code + ".insert-time: " + insertTime);
        System.err.println(code + ".copy-time: " + copyTime);
        System.err.println(code + ".retrieve-time: " + readTime);
        System.err.println(code + ".insert-time-avg: " + (double) insertTime / (double) iCount);
        System.err.println(code + ".retrieve-time-avg: " + (double) readTime / (double) rCount);
        System.err.println(code + ".copy-time-avg: " + copyTime / (double) iCount);

    }

    public static void testDistanceMatrix(Vector nodes) {
        System.err.println();
        System.err.println("testDistanceMatrix");
        
        DistanceMatrix dm = new DistanceMatrix();

        Timer.printTimeElapsed(null);

        int iCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                dm.putEntry(pi, pj, (double) (i + j));
                iCount++;
            }
        }
        long insertTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        DistanceMatrix dm2 = (DistanceMatrix)dm.copy();
        long copyTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        int rCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                double d = dm2.getEntry(pi, pj);
                if (d != (double) (i + j)) {
                    System.err.println("Error! " + i + "/" + j + ": " + d);
                }
                rCount++;
            }
        }
        long readTime = Timer.printTimeElapsed(null);

        String code = "DistanceMatrix";
        System.err.println(code + ".insert-time: " + insertTime);
        System.err.println(code + ".copy-time: " + copyTime);
        System.err.println(code + ".retrieve-time: " + readTime);
        System.err.println(code + ".insert-time-avg: " + (double) insertTime / (double) iCount);
        System.err.println(code + ".retrieve-time-avg: " + (double) readTime / (double) rCount);
        System.err.println(code + ".copy-time-avg: " + copyTime / (double) iCount);

    }

    public static void testDistanceMatrixOptDelete(Vector nodes) {
        System.err.println();
        System.err.println("testDistanceMatrixOptDelete");
        
        DistanceMatrixOpt dm = new DistanceMatrixOpt();

        Timer.printTimeElapsed(null);

        int iCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                dm.putEntry(pi, pj, (double) (i + j));
                iCount++;
            }
        }
        long insertTime = Timer.printTimeElapsed(null);

        int dCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                if (i % 7 == 0) {
                    dm.removeRow(pi);
                } else if (j % 13 == 0) {
                    dm.removeCol(pj);
                }
                dCount++;
            }
        }
        long deleteTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        DistanceMatrixOpt dm2 = dm.copy();
        long copyTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        int rCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                double d = dm2.getEntry(pi, pj);

                boolean pass = false;
                if (i % 7 == 0) {
                    if (!DistanceMatrixOpt.isValid(d)) {
                        pass = true;
                    }
                } else if (j % 13 == 0) {
                    if (!DistanceMatrixOpt.isValid(d)) {
                        pass = true;
                    }
                } else if (d == (double) (i + j)) {
                    pass = true;
                }

                if (!pass) {
                    System.err.println("Error! " + i + "/" + j + ": " + d);
                }
                rCount++;
            }
        }
        long readTime = Timer.printTimeElapsed(null);

        String code = "ObjectMatrix";
        System.err.println(code + ".insert-time: " + insertTime);
        System.err.println(code + ".copy-time: " + copyTime);
        System.err.println(code + ".retrieve-time: " + readTime);
        System.err.println(code + ".insert-time-avg: " + (double) insertTime / (double) iCount);
        System.err.println(code + ".retrieve-time-avg: " + (double) readTime / (double) rCount);
        System.err.println(code + ".copy-time-avg: " + copyTime / (double) iCount);
    }

    public static void testObjectMatrixOpt(Vector nodes) {
        System.err.println();
        System.err.println("testObjectMatrixOpt");
        
        ObjectMatrixOpt dm = new ObjectMatrixOpt();

        Timer.printTimeElapsed(null);

        int iCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                dm.putEntry(pi, pj, new Integer(i+j));
                iCount++;
            }
        }
        long insertTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        ObjectMatrixOpt dm2 = (ObjectMatrixOpt)dm.copy();
        long copyTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        int rCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                Object d = dm2.getEntry(pi, pj);
                if (d == null || ((Integer)d).intValue() != i + j) {
                    System.err.println("Error! " + i + "/" + j + ": " + d);
                }
                rCount++;
            }
        }
        long readTime = Timer.printTimeElapsed(null);

        String code = "ObjectMatrixOpt";
        System.err.println(code + ".insert-time: " + insertTime);
        System.err.println(code + ".copy-time: " + copyTime);
        System.err.println(code + ".retrieve-time: " + readTime);
        System.err.println(code + ".insert-time-avg: " + (double) insertTime / (double) iCount);
        System.err.println(code + ".retrieve-time-avg: " + (double) readTime / (double) rCount);
        System.err.println(code + ".copy-time-avg: " + copyTime / (double) iCount);
        System.err.println(code + ".size: " +dm2.size());

    }

    public static void testObjectMatrix(Vector nodes) {
        System.err.println();
        System.err.println("testObjectMatrix");
        
        ObjectMatrix dm = new ObjectMatrix();

        Timer.printTimeElapsed(null);

        int iCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                dm.putEntry(pi, pj, new Integer(i + j));
                iCount++;
            }
        }
        long insertTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        ObjectMatrix dm2 = (ObjectMatrix)dm.copy();
        long copyTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        int rCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                Object d = dm2.getEntry(pi, pj);
                if (d == null || ((Integer)d).intValue() != (i + j)) {
                    System.err.println("Error! " + i + "/" + j + ": " + d);
                }
                rCount++;
            }
        }
        long readTime = Timer.printTimeElapsed(null);

        String code = "ObjectMatrix";
        System.err.println(code + ".insert-time: " + insertTime);
        System.err.println(code + ".copy-time: " + copyTime);
        System.err.println(code + ".retrieve-time: " + readTime);
        System.err.println(code + ".insert-time-avg: " + (double) insertTime / (double) iCount);
        System.err.println(code + ".retrieve-time-avg: " + (double) readTime / (double) rCount);
        System.err.println(code + ".copy-time-avg: " + copyTime / (double) iCount);
        System.err.println(code + ".size: " +dm2.size());
        
    }

    public static void testObjectMatrixOptDelete(Vector nodes) {
        System.err.println();
        System.err.println("testObjectMatrixOptDelete");
        
        ObjectMatrixOpt dm = new ObjectMatrixOpt();

        Timer.printTimeElapsed(null);

        int iCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                dm.putEntry(pi, pj, new Integer(i + j));
                iCount++;
            }
        }
        long insertTime = Timer.printTimeElapsed(null);

        int dCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                if (i % 7 == 0) {
                    dm.removeRow(pi);
                } else if (j % 13 == 0) {
                    dm.removeCol(pj);
                }
                dCount++;
            }
        }
        long deleteTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        ObjectMatrixOpt dm2 = (ObjectMatrixOpt)dm.copy();
        long copyTime = Timer.printTimeElapsed(null);

        Timer.printTimeElapsed(null);
        int rCount = 0;
        for (int i = 0; i < nodes.size(); i++) {
            IRelatablePoint pi = (IRelatablePoint) nodes.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                IRelatablePoint pj = (IRelatablePoint) nodes.get(j);
                Object d = dm2.getEntry(pi, pj);

                boolean pass = false;
                if (i % 7 == 0) {
                    if (!ObjectMatrixOpt.isValid(d)) {
                        pass = true;
                    }
                } else if (j % 13 == 0) {
                    if (!ObjectMatrixOpt.isValid(d)) {
                        pass = true;
                    }
                } else if (d != null && ((Integer)d).intValue() == (i + j)) {
                    pass = true;
                }

                if (!pass) {
                    System.err.println("Error! " + i + "/" + j + ": " + d);
                }
                rCount++;
            }
        }
        long readTime = Timer.printTimeElapsed(null);

        String code = "ObjectMatrixOpt";
        System.err.println(code + ".insert-time: " + insertTime);
        System.err.println(code + ".copy-time: " + copyTime);
        System.err.println(code + ".retrieve-time: " + readTime);
        System.err.println(code + ".delete-time: " + deleteTime);
        System.err.println(code + ".insert-time-avg: " + (double) insertTime / (double) iCount);
        System.err.println(code + ".retrieve-time-avg: " + (double) readTime / (double) rCount);
        System.err.println(code + ".copy-time-avg: " + copyTime / (double) iCount);
        System.err.println(code + ".size: " +dm2.size());

    }

    public static void main(String[] args) {
        TreeDelimitedFile fileA = new TreeDelimitedFile("random.tree");
        IRelatablePoint aTree = fileA.getContent();
        Vector nodes = new Vector();
        postTraverseTree(nodes, aTree);
        DistanceMatrixOpt.setDefaultSize(2);
        ObjectMatrixOpt.setDefaultSize(2);
        
        int size = nodes.size();
        System.err.println("Expected Matrix size: " + size + "x" + size + " (" + (size*size) + ")");
        testDistanceMatrix(nodes);
        testDistanceMatrixOpt(nodes);
        testDistanceMatrixOptDelete(nodes);

        testObjectMatrix(nodes);
        testObjectMatrixOpt(nodes);
        testObjectMatrixOptDelete(nodes);
    }
}
