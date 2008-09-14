/*
 * Created on Dec 22, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;

/**
 * @author yks
 */
public class ObjectMatrix implements IObjectMatrix, ILimited {

    private final static Object NOT_VALID = null;

    public static boolean isValid(Object o) {
        return o != NOT_VALID;
    }
    
    private LinkedHashMap rows = new LinkedHashMap();

    private Vector rowKeys = new Vector();

    private Vector colKeys = new Vector();

    private int size = 0;
    
    public int size() {
        return size;
    }

    private int limit = Integer.MAX_VALUE;
    
    public int limit() { return limit; }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public boolean isFull() {
        return size >= limit;
    }
    
    public int getRowOffset(IPoint pointRef, IPoint pointOff) {
        return this.getOffset(rowKeys, pointRef, pointOff);
    }

    public int getColOffset(IPoint pointRef, IPoint pointOff) {
        return this.getOffset(colKeys, pointRef, pointOff);
    }

    /**
     * if pointRef is null then offset is calculated from the lastPoint to the
     * pointOff in points
     * 
     * if pointOff is null then offset is calculated from the pointRef to the
     * first element
     * 
     * @param pointStart
     * @param pointEnd
     * @return the index difference between two points in the matrix indices
     */
    private int getOffset(Vector points, IPoint pointStart, IPoint pointEnd) {

        int startIndex, endIndex;

        if (pointStart != null) {
            startIndex = points.indexOf(pointStart);
        } else {
            startIndex = -1;
        }

        if (pointEnd != null) {
            endIndex = points.indexOf(pointEnd);
        } else {
            endIndex = -1;
        }
        //        System.err.println("#points: " + points.size());
        //        System.err.println("start: " + startIndex + " end: " + endIndex);
        if (startIndex >= 0 && endIndex >= 0) {

            return startIndex - endIndex;

        } else if (startIndex >= 0 || endIndex >= 0) {

            if (startIndex >= 0) {
                /* endIndex < 0 */
                return startIndex + 1;
            } else {
                /* startIndex < 0 */
                return points.size() - endIndex;
            }

        } else {
            return 0;
        }
    }

    public IPoint getRowPointByOffset(IPoint point, int offset) {
        return getPointByOffset(this.rowKeys, point, offset);
    }

    public IPoint getColPointByOffset(IPoint point, int offset) {
        return getPointByOffset(this.colKeys, point, offset);
    }

    private IPoint getPoint(Vector points, int offset) {
        if (points != null && offset >= 0) {
            return (IPoint) points.get(offset);
        } else {
            return null;
        }
    }

    private IPoint getPointByOffset(Vector points, IPoint point, int offset) {

        if (point == null) {
            return getPoint(points, offset);
        }

        int row = points.indexOf(point);
        if (row >= 0) { /* found */
            if (row + offset >= 0) {
                if (points.size() < row + offset) {
                    System.err.println("getPointByOffset: " + points.size() + "/" + offset + "/r=" + row);
                }
                return (IPoint) points.get(row + offset);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean hasEntry(IPoint i, IPoint j) {
        LinkedHashMap dhm = getRow(i);
        if (dhm == null) {
            return false;
        } else {
            Object d = dhm.get(j);
            if (d == NOT_VALID) {
                return false;
            } else {
                return true;
            }
        }
    }

    public Object getEntry(IPoint i, IPoint j) {
        LinkedHashMap dhm = getRow(i);
        if (dhm == null) {
            return NOT_VALID;
        } else {
            return dhm.get(j);
        }
    }

    public LinkedHashMap getRow(IPoint i) {
        return (LinkedHashMap) rows.get(i);
    }

    public void removeRow(IPoint iKey) {
        if (rows.containsKey(iKey)) {
            size -= getRow(iKey).size();
        }
        rows.remove(iKey);
        rowKeys.remove(iKey);
    }

    public void removeAll(IPoint iKey) {
        removeRow(iKey);
        Iterator it = rows.values().iterator();
        while (it.hasNext()) {
            LinkedHashMap row = (LinkedHashMap) it.next();
            size -= 1;
            row.remove(iKey);
        }
    }

    public void removeEntry(IPoint i, IPoint j) {
        HashMap dhm = getRow(i);
        if (dhm != null) {
            dhm.remove(j);
            size--;
        }
        if (colKeys != null) {
            colKeys.remove(j);
        }
    }

    public void putEntrySymmetric(IPoint i, IPoint j, Object val) {
        putEntry(i, j, val);
        putEntry(j, i, val);
    }

    public void putEntry(IPoint i, IPoint j, Object val) {
        // make the ObjectMatrix size limited
        // for memory concerns
        if (this.isFull()) {
            return;
        }

        LinkedHashMap dhm = getRow(i);
        if (dhm == null) {
            dhm = new LinkedHashMap();
        }

        if (!rowKeys.contains(i)) {
            rowKeys.add(i);
        }
        if (!colKeys.contains(j)) {
            colKeys.add(j);
        }

        if (!dhm.containsKey(j)) {
            size++;
        }
        
        dhm.put(j, val);

        rows.put(i, dhm);
    }

    public String rowKeys2String() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < rowKeys.size(); i++) {
            IPoint p = (IPoint) rowKeys.get(i);
            buf.append((i == 0 ? "" : " ") + p.getUniqueID());
        }
        return buf.toString();
    }

    public String colKeys2String() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < colKeys.size(); i++) {
            IPoint p = (IPoint) colKeys.get(i);
            buf.append((i == 0 ? "" : " ") + p.getUniqueID());
        }
        return buf.toString();
    }

    public IObjectMatrix copy() {

        ObjectMatrix dmCopy = new ObjectMatrix();
        LinkedHashMap newRow = new LinkedHashMap(rows.size());
        
        Iterator rowIt = rows.entrySet().iterator();
        while (rowIt.hasNext()) {
            Map.Entry e = (Map.Entry) rowIt.next();
            Object key = e.getKey();
            LinkedHashMap row = (LinkedHashMap)e.getValue();
            
            Object rowCopy = row.clone();
            newRow.put(key, row);
        }
        dmCopy.rows = newRow;
        
        dmCopy.rowKeys = (Vector)rowKeys.clone();
        dmCopy.colKeys = (Vector)colKeys.clone();
        dmCopy.limit = this.limit;
        dmCopy.size = this.size;
        return dmCopy;
    }
    
    public String toString() {
        int limit = 10;
        int i = 0;
        StringBuffer buf = new StringBuffer();

        for (i = 0; i < rowKeys.size(); i++) {
            LinkedHashMap dhm = (LinkedHashMap) rows.get(rowKeys.get(i));
            buf.append(rowKeys.get(i) + ":");
            buf.append("[");

            for (int j = 0; j < colKeys.size(); j++) {
                Object val = dhm.get(colKeys.get(j));
                buf.append(val.toString());
                if (j != colKeys.size() - 1) {
                    buf.append(" ");
                }
            }
            buf.append("]");
            buf.append("\n");
        }
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.IObjectMatrix#removeCol(edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint)
     */
    public void removeCol(IPoint col) {
        Iterator it = rows.values().iterator();
        while (it.hasNext()) {
            LinkedHashMap row = (LinkedHashMap) it.next();
            size -= 1;
            row.remove(col);
        }
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.IObjectMatrix#numRows()
     */
    public int numRows() {
        return rows.size();
    }

    /* (non-Javadoc)
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.matrix.IObjectMatrix#numCols()
     */
    public int numCols() {
        if (rows.size() > 0) {
            Iterator it = rows.keySet().iterator();
            LinkedHashMap row = (LinkedHashMap)it.next();
            return row.size();
        }
        return 0;
    }
}