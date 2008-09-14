/*
 * Created on Jan 27, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;

/**
 * @author yks
 */
public class DistanceMatrixOpt {
    private static int DEFAULT_SIZE = 1024;

    final private static int DOUBLING_LIMIT = 4096;

    final private static int INCREMENT = 1024;

    final private static double NOT_VALID = -1;

    public static void setDefaultSize(int size) {
        DEFAULT_SIZE = size;
    }
    
    public static boolean isValid(double v) {
        return v != NOT_VALID;
    }
    
    private int width;

    private int height;

    private double[][] distances;

    private LinkedHashMap rowIndex;

    private LinkedHashMap colIndex;

    private Vector freeRowIndex = new Vector();

    private Vector freeColIndex = new Vector();

    /*
     * when entries are deleted, the matrix has holes in the rows or columns, if
     * the matrix is holeless, matrix copies canbe more efficient.
     */
    private boolean hasHoles = false;

    private int getNextFreeRowIndex() {
        if (freeRowIndex.isEmpty()) {
            return rowIndex.size();
        } else {
            Integer i = (Integer) freeRowIndex.get(0);
            freeRowIndex.remove(0);
            return i.intValue();
        }
    }

    private int getNextFreeColIndex() {
        if (freeColIndex.isEmpty()) {
            return colIndex.size();
        } else {
            Integer i = (Integer) freeColIndex.get(0);
            freeColIndex.remove(0);
            return i.intValue();
        }
    }

    public boolean hasRow(IPoint p) {
        return rowIndex.containsKey(p);
    }

    public boolean hasCol(IPoint p) {
        return colIndex.containsKey(p);
    }

    /**
     * expand the current matrix by a doubling algorithm and copy the old matrix
     * contents into the new expanded matrix
     */
    private void expandMatrix() {

        int newWidth, newHeight;

        if (width > DOUBLING_LIMIT) {
            newWidth = width + INCREMENT;
        } else {
            newWidth = width * 2;
        }
        if (height > DOUBLING_LIMIT) {
            newHeight = height + INCREMENT;
        } else {
            /* double height */
            newHeight = height * 2;
        }

        System.err.print("expandMatrix: " + width + "x" + height);
        System.err.println(" -> " + newWidth + "x" + newHeight);

        if (hasHoles) {
            fullCopy(this, this, newWidth, newHeight);
        } else {
            optimizedCopy(this, this, newWidth, newHeight);
        }
    }

    /**
     * optimizedCopy: Done when there are no holes in the row or colIndexes. so
     * we can do a direct matrix copy; NOTE, old and dm can be the same matrix;
     * coders beware!
     */
    private void optimizedCopy(DistanceMatrixOpt old, DistanceMatrixOpt dm, int newWidth, int newHeight) {
        
        double[][] old_distances = old.distances;
        LinkedHashMap old_rowIndex = old.rowIndex;
        LinkedHashMap old_colIndex = old.colIndex;
        int oldWidth = old.width;
        int oldHeight = old.height;

        dm.width = newWidth;
        dm.height = newHeight;
        dm.distances = new double[newWidth][newHeight];
        dm.rowIndex = (LinkedHashMap) old_rowIndex.clone();
        dm.colIndex = (LinkedHashMap) old_colIndex.clone();
        dm.freeRowIndex = new Vector();
        dm.freeColIndex = new Vector();
        dm.hasHoles = false;

        /* shortcuts */
        LinkedHashMap rowIndex = dm.rowIndex;
        LinkedHashMap colIndex = dm.colIndex;
        double[][] distances = dm.distances;

        int i;
        for (i = 0; i < oldWidth; i++) {
            int j;
            for (j = 0; j < oldHeight; j++) {
                distances[i][j] = old_distances[i][j];
            }
            for (; j < newHeight; j++) {
                distances[i][j] = NOT_VALID;
            }
        }

        /* initialize the rest as invalid */
        for (; i < newWidth; i++) {
            for (int j = 0; j < newHeight; j++) {
                distances[i][j] = NOT_VALID;
            }
        }
    }

    /**
     * fullCopy: Done when there are holes in the row or col index. we need to
     * do a fullCopy (i.e. examine each key and copy each entry individuallly
     */
    static private void fullCopy(DistanceMatrixOpt old, DistanceMatrixOpt dm, int newWidth, int newHeight) {
        /**
         * copying the old matrix into the new matrix parameters
         */
        double[][] old_distances = old.distances;
        LinkedHashMap old_rowIndex = old.rowIndex;
        LinkedHashMap old_colIndex = old.colIndex;

        dm.width = newWidth;
        dm.height = newHeight;
        dm.distances = new double[newWidth][newHeight];
        
        for (int i = 0; i < newWidth; i++) {
            for (int j = 0; j < newHeight; j++) {
                dm.distances[i][j] = NOT_VALID;
            }
        }
        
        dm.rowIndex = new LinkedHashMap(newWidth);
        dm.colIndex = new LinkedHashMap(newHeight);
        invalidateAllEntries(dm);
        dm.freeRowIndex = new Vector();
        dm.freeColIndex = new Vector();
        dm.hasHoles = false;

        /* shortcuts */
        LinkedHashMap rowIndex = dm.rowIndex;
        LinkedHashMap colIndex = dm.colIndex;
        double[][] distances = dm.distances;

        Iterator rowIt = old_rowIndex.keySet().iterator();
        int i = 0;

        while (rowIt.hasNext()) {
            IPoint rowP = (IPoint) rowIt.next();
            Iterator colIt = old_colIndex.keySet().iterator();

            rowIndex.put(rowP, new Integer(i));

            int j = 0;
            while (colIt.hasNext()) {
                IPoint colP = (IPoint) colIt.next();

                Integer row = (Integer) old_rowIndex.get(rowP);
                Integer col = (Integer) old_colIndex.get(colP);

                distances[i][j] = old_distances[row.intValue()][col.intValue()];

                if (i == 0) {
                    colIndex.put(colP, new Integer(j));
                }
                j++;
            }
            i++;
        }

        old_distances = null;
        old_rowIndex = null;
        old_colIndex = null;
    }

    private static void invalidateAllEntries(DistanceMatrixOpt dm) {
        double[][] distances = dm.distances;
        for (int i = 0; i < dm.width; i++) {
            for (int j = 0; j < dm.height; j++) {
                distances[i][j] = NOT_VALID;
            }
        }
    }

    public DistanceMatrixOpt copy() {
        /*
         * copy uses a special constructor which doesn't allocate memory
         * full/optimized copy will do that
         */
        DistanceMatrixOpt dmCopy = new DistanceMatrixOpt(this.width, this.height);

        if (hasHoles) {
            fullCopy(this, dmCopy, this.width, this.height);
        } else {
            optimizedCopy(this, dmCopy, this.width, this.height);
        }
        return dmCopy;
    }

    public DistanceMatrixOpt() {
        width = DEFAULT_SIZE;
        height = DEFAULT_SIZE;
        init();
    }

    /**
     * this constructor simply sets the width and height
     * 
     * @param widthHint
     * @param heightHint
     */
    private DistanceMatrixOpt(int widthHint, int heightHint) {
        if (widthHint > 0 && heightHint > 0) {
            width = widthHint;
            height = heightHint;
        } else {
            width = DEFAULT_SIZE;
            height = DEFAULT_SIZE;
        }
    }

    private void init() {
        if (distances == null) {
            distances = new double[width][height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    distances[i][j] = NOT_VALID;
                }
            }
            rowIndex = new LinkedHashMap(width);
            colIndex = new LinkedHashMap(height);
            freeRowIndex = new Vector();
            freeColIndex = new Vector();
        }
    }

    public int numRows() {
        return rowIndex.size();
    }

    public int numCols() {
        return colIndex.size();
    }

    public boolean hasEntry(IPoint i, IPoint j) {
        Integer row = (Integer) rowIndex.get(i);
        Integer col = (Integer) colIndex.get(j);

        if (row != null && col != null) {
            if (distances[row.intValue()][col.intValue()] != NOT_VALID) {
                return true;
            }
        }
        return false;
    }

    public double getEntry(IPoint i, IPoint j) {
        Integer row = (Integer) rowIndex.get(i);
        Integer col = (Integer) colIndex.get(j);

        if (row != null && col != null) {
            double d = distances[row.intValue()][col.intValue()];
            if (d != NOT_VALID) {
                return d;
            }
        }
        return NOT_VALID;
    }

    private void invalidateRow(int i) {
        int ncols = numCols();
        for (int j = 0; j < ncols; j++) {
            distances[i][j] = NOT_VALID;
        }
    }

    private void invalidateCol(int j) {
        int nrows = numRows();
        for (int i = 0; i < nrows; i++) {
            distances[i][j] = NOT_VALID;
        }
    }

    public void removeRow(IPoint i) {
        Integer row = (Integer) rowIndex.get(i);
        if (row != null) {
            freeRowIndex.add(row);
            rowIndex.remove(i);
            invalidateRow(row.intValue());
        }
        hasHoles = true;
    }

    public void removeCol(IPoint j) {
        Integer col = (Integer) colIndex.get(j);
        if (col != null) {
            freeColIndex.add(col);
            colIndex.remove(j);
            invalidateCol(col.intValue());
        }
        hasHoles = true;
    }

    /**
     * sanity check routine to make sure matrix contains only valid entries.
     * 
     * @return
     */
    public boolean checkSanity() {
        Iterator rit = rowIndex.keySet().iterator();
        while (rit.hasNext()) {
            IPoint rp = (IPoint) rit.next();

            Iterator cit = colIndex.keySet().iterator();
            while (cit.hasNext()) {
                IPoint cp = (IPoint) cit.next();
                if (cp != rp) {
                    double d = this.getEntry(rp, cp);
                    if (d == NOT_VALID) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void removeAll(IPoint i) {
        removeRow(i);
        removeCol(i);
        hasHoles = true;
    }

    public void removeEntry(IPoint i, IPoint j) {
        Integer row = (Integer) rowIndex.get(i);
        Integer col = (Integer) colIndex.get(j);

        if (row != null && col != null) {
            distances[row.intValue()][col.intValue()] = NOT_VALID;
        }
    }

    public void putEntrySymmetric(IPoint i, IPoint j, double val) {
        putEntry(i, j, val);
        putEntry(j, i, val);
    }

    private boolean hasFreeSpace() {
        if (rowIndex.size() < width && colIndex.size() < height)
            return true;
        else
            return false;
    }

    public void putEntry(IPoint i, IPoint j, double val) {

        Integer row = (Integer) rowIndex.get(i);
        Integer col = (Integer) colIndex.get(j);

        if (row != null && col != null) {
            distances[row.intValue()][col.intValue()] = val;
        } else {
            if (hasFreeSpace()) {

                if (row == null) {
                    row = new Integer(getNextFreeRowIndex());
                    rowIndex.put(i, row);
                }

                if (col == null) {
                    col = new Integer(getNextFreeColIndex());
                    colIndex.put(j, col);
                }

                distances[row.intValue()][col.intValue()] = val;

            } else {
                expandMatrix(); /* increase the space */
                putEntry(i, j, val);
            }
        }
    }

    public void dump(PrintStream out) {
        out.print( toString(Integer.MAX_VALUE) );
    }
    
    public String toString() {
        return toString(10);
    }
    
    public String toString(int limit) {
        int i = 0;
        StringBuffer buf = new StringBuffer();

        if (rowIndex.size() > 0) {
            Iterator keysIt = rowIndex.keySet().iterator();
            i = 0;
            buf.append("\\:[");

            while (keysIt.hasNext() && i < limit) {
                if (i > 0) {
                    buf.append("\t");
                }
                buf.append(((IPoint) keysIt.next()).getUniqueID());
                i++;
            }
            buf.append("]\n");
        }

        Iterator rowIt = rowIndex.keySet().iterator();
        i = 0;
        while (rowIt.hasNext() && i < limit) {
            IPoint rowP = (IPoint) rowIt.next();

            buf.append(rowP.getUniqueID() + ":");
            buf.append("[");

            Iterator colIt = colIndex.keySet().iterator();
            int j = 0;
            while (colIt.hasNext() && j < limit) {
                IPoint colP = (IPoint) colIt.next();

                double d = getEntry(rowP, colP);
                if (j > 0) {
                    buf.append("\t");
                }
                buf.append(d);
                j++;
            }
            buf.append("]");
            buf.append("\n");
            i++;
        }
        return buf.toString();
    }
}