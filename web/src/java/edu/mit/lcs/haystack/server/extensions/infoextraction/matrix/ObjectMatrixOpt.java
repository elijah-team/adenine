/*
 * Created on Jan 29, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;

/**
 * @author yks
 */
public class ObjectMatrixOpt implements IObjectMatrix, ILimited {
    private static int DEFAULT_SIZE = 256;

    final private static int DOUBLING_LIMIT = 2048;

    final private static int INCREMENT = 512;

    final private static Object NOT_VALID = null;

    public static void setDefaultSize(int size) {
        DEFAULT_SIZE = size;
    }
    
    public static boolean isValid(Object o) {
        return o != NOT_VALID;
    }

    private int width;

    private int height;

    private Object[][] matrix;

    private LinkedHashMap rowIndex;

    private LinkedHashMap colIndex;

    private Vector freeRowIndex = new Vector();

    private Vector freeColIndex = new Vector();

    private int size;

    private int limit;

    /* ILimited interface methods */
    public int limit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int size() {
        return size;
    }

    public String infoString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ObjectMatrixOpt.limit(): " + limit() + "\n");
        buf.append("ObjectMatrixOpt.size(): " + size() + "\n");
        return buf.toString();
    }
    
    public boolean isFull() {
        if (limit == 0) {
            return false;
        } else {
            return size >= limit;
        }
    }

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

        System.err.print("expandMatrix: " + width + "x" + height + " -> ");
        System.err.println(newWidth + "x" + newHeight);

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
    private void optimizedCopy(ObjectMatrixOpt old, ObjectMatrixOpt dm, int newWidth, int newHeight) {
        Object[][] old_distances = old.matrix;
        LinkedHashMap old_rowIndex = old.rowIndex;
        LinkedHashMap old_colIndex = old.colIndex;
        int oldWidth = old.width;
        int oldHeight = old.height;

        dm.size = old.size;
        dm.limit = old.limit;

        dm.width = newWidth;
        dm.height = newHeight;
        dm.matrix = new Object[newWidth][newHeight];
        dm.rowIndex = (LinkedHashMap) old_rowIndex.clone();
        dm.colIndex = (LinkedHashMap) old_colIndex.clone();
        dm.freeRowIndex = new Vector();
        dm.freeColIndex = new Vector();
        dm.hasHoles = false;
        /* shortcuts */
        LinkedHashMap rowIndex = dm.rowIndex;
        LinkedHashMap colIndex = dm.colIndex;
        Object[][] distances = dm.matrix;

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
    static private void fullCopy(ObjectMatrixOpt old, ObjectMatrixOpt dm, int newWidth, int newHeight) {
        /**
         * copying the old matrix into the new matrix parameters
         */
        Object[][] old_matrix = old.matrix;
        LinkedHashMap old_rowIndex = old.rowIndex;
        LinkedHashMap old_colIndex = old.colIndex;

        dm.width = newWidth;
        dm.height = newHeight;
        dm.matrix = new Object[newWidth][newHeight];
        dm.rowIndex = new LinkedHashMap(newWidth);
        dm.colIndex = new LinkedHashMap(newHeight);
        invalidateAllEntries(dm);
        dm.freeRowIndex = new Vector();
        dm.freeColIndex = new Vector();
        dm.hasHoles = false;

        /* shortcuts */
        LinkedHashMap rowIndex = dm.rowIndex;
        LinkedHashMap colIndex = dm.colIndex;
        Object[][] matrix = dm.matrix;

        Iterator rowIt = old_rowIndex.keySet().iterator();
        int i = 0;
        int size = 0;
        while (rowIt.hasNext()) {
            IPoint rowP = (IPoint) rowIt.next();
            Iterator colIt = old_colIndex.keySet().iterator();

            rowIndex.put(rowP, new Integer(i));

            int j = 0;
            while (colIt.hasNext()) {
                IPoint colP = (IPoint) colIt.next();

                Integer row = (Integer) old_rowIndex.get(rowP);
                Integer col = (Integer) old_colIndex.get(colP);

                Object val = matrix[i][j] = old_matrix[row.intValue()][col.intValue()];

                if (val != NOT_VALID) {
                    size++;
                }

                if (i == 0) {
                    colIndex.put(colP, new Integer(j));
                }
                j++;
            }
            i++;
        }

        dm.size = size;
        dm.limit = old.limit;

        old_matrix = null;
        old_rowIndex = null;
        old_colIndex = null;
    }

    private static void invalidateAllEntries(ObjectMatrixOpt dm) {
        Object[][] matrix = dm.matrix;
        for (int i = 0; i < dm.width; i++) {
            for (int j = 0; j < dm.height; j++) {
                matrix[i][j] = NOT_VALID;
            }
        }
    }

    public IObjectMatrix copy() {
        /*
         * copy uses a special constructor which doesn't allocate memory
         * full/optimized copy will do that
         */
        ObjectMatrixOpt dmCopy = new ObjectMatrixOpt(this.width, this.height);

        if (hasHoles) {
            fullCopy(this, dmCopy, this.width, this.height);
        } else {
            optimizedCopy(this, dmCopy, this.width, this.height);
        }
        return dmCopy;
    }

    public ObjectMatrixOpt() {
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
    private ObjectMatrixOpt(int widthHint, int heightHint) {
        if (widthHint > 0 && heightHint > 0) {
            width = widthHint;
            height = heightHint;
        } else {
            width = DEFAULT_SIZE;
            height = DEFAULT_SIZE;
        }
    }

    private void init() {
        if (matrix == null) {
            matrix = new Object[width][height];
            rowIndex = new LinkedHashMap(width);
            colIndex = new LinkedHashMap(height);
            freeRowIndex = new Vector();
            freeColIndex = new Vector();
            size = 0;
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
            if (matrix[row.intValue()][col.intValue()] != NOT_VALID) {
                return true;
            }
        }
        return false;
    }

    public Object getEntry(IPoint i, IPoint j) {
        Integer row = (Integer) rowIndex.get(i);
        Integer col = (Integer) colIndex.get(j);

        if (row != null && col != null) {
            Object d = matrix[row.intValue()][col.intValue()];
            if (d != NOT_VALID) {
                return d;
            }
        }
        return NOT_VALID;
    }

    private void invalidateRow(int i) {
        int ncols = numCols();
        for (int j = 0; j < ncols; j++) {
            if (matrix[i][j] != NOT_VALID) {
                matrix[i][j] = NOT_VALID;
                size--;
            }
        }
    }

    private void invalidateCol(int j) {
        int nrows = numRows();
        for (int i = 0; i < nrows; i++) {
            if (matrix[i][j] != NOT_VALID) {
                matrix[i][j] = NOT_VALID;
                size--;
            }
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
                    Object d = this.getEntry(rp, cp);
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
            int rint = row.intValue();
            int cint = col.intValue();
            if (matrix[rint][cint] != NOT_VALID) {
                matrix[rint][cint] = NOT_VALID;
                size--;
            }
        }
    }

    public void putEntrySymmetric(IPoint i, IPoint j, Object val) {
        putEntry(i, j, val);
        putEntry(j, i, val);
    }

    private boolean hasFreeSpace() {
        if (rowIndex.size() < width && colIndex.size() < height)
            return true;
        else
            return false;
    }

    public void putEntry(IPoint i, IPoint j, Object val) {

        Integer row = (Integer) rowIndex.get(i);
        Integer col = (Integer) colIndex.get(j);

        if (row != null && col != null) {
            int r = row.intValue();
            int c = col.intValue();

            if (matrix[r][c] == NOT_VALID && val != NOT_VALID) {
                size++;
            }
            matrix[r][c] = val;
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

                int r = row.intValue();
                int c = col.intValue();

                if (matrix[r][c] == NOT_VALID && val != NOT_VALID) {
                    size++;
                }
                matrix[r][c] = val;

            } else {
                expandMatrix(); /* increase the space */
                putEntry(i, j, val);
            }
        }
    }

    public String toString() {
        int limit = 10;
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

                Object d = getEntry(rowP, colP);
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