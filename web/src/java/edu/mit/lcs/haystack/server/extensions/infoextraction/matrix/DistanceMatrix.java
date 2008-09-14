/*
 * Created on Nov 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.rec.DoubleHashMap;

/**
 * @author yks
 */
public class DistanceMatrix implements IDoubleMatrix {
    private final static double NOT_VALID = -1.0;
    
    public static boolean isValid(double v) {
        return v != NOT_VALID;
    }
    
    LinkedHashMap rows = new LinkedHashMap();
        
    public boolean hasRow(IPoint p) {
        return rows.containsKey(p.getUniqueID());
    }
    
    public boolean hasCol(IPoint p) {
        if (rows.size() > 0) {
            Iterator it = rows.values().iterator();
            DoubleHashMap firstRow = (DoubleHashMap)it.next();
            return firstRow.containsKey(p);
        } else {
            return false;
        }
    }
    
    public int numRows() {
        return rows.size();
    }
    
    public int numCols() {
        if (numRows() > 0) {
            Iterator rowit = rows.values().iterator();
            if (rowit.hasNext()) {
                DoubleHashMap dhm = (DoubleHashMap)rowit.next();
                return (dhm.size());
            }
        }
        return 0;
    }
        
    public IDoubleMatrix copy() {
        DistanceMatrix dmCopy = new DistanceMatrix();
        LinkedHashMap newRow = new LinkedHashMap(rows.size());
        
        Iterator rowIt = rows.entrySet().iterator();
        while (rowIt.hasNext()) {
            Map.Entry e = (Map.Entry) rowIt.next();
            Object key = e.getKey();
            DoubleHashMap row = (DoubleHashMap)e.getValue();
            
            DoubleHashMap rowCopy = (DoubleHashMap)row.clone();
            newRow.put(key, row);
        }
        dmCopy.rows = newRow;
        return dmCopy;
    }

    public boolean hasEntry(IPoint i, IPoint j) {
        DoubleHashMap dhm = getRow(i);
        if (dhm == null) {
            return false;
        } else {
            double d = dhm.getDouble(j.getUniqueID());
            if (d == NOT_VALID) {
                return false;
            } else {
                return true; 
            }
        }
    }
    
    public double getEntry(IPoint i, IPoint j) {
        DoubleHashMap dhm = getRow(i);
        if (dhm == null) {
            return NOT_VALID;
        } else {
            return dhm.getDouble(j);
        }
    }
    
    public DoubleHashMap getRow(IPoint i) {
        return (DoubleHashMap)rows.get(i);
    }
    
    public void removeRow(IPoint i) {
        rows.remove(i);
    }
        
    public void removeCol(IPoint i) {
        Iterator it = rows.values().iterator();
        while (it.hasNext()) {
            DoubleHashMap row = (DoubleHashMap)it.next();
            row.remove(i);
        }
    }

    public void removeAll(IPoint i) {
        removeRow(i);
        Iterator it = rows.values().iterator();
        while (it.hasNext()) {
            DoubleHashMap row = (DoubleHashMap)it.next();
            row.remove(i);
        }
    }
    
    public void removeEntry(IPoint i, IPoint j) {
        DoubleHashMap dhm = getRow(i);
        if (dhm != null) {
            dhm.remove(j);
        }
    }

    public void putEntrySymmetric(IPoint i, IPoint j, double val) {
        putEntry(i, j, val);
        putEntry(j, i, val);
    }

    public void putEntry(IPoint i, IPoint j, double val) {
        DoubleHashMap dhm = getRow(i);
        if (dhm == null) {
            dhm = new DoubleHashMap();
        }
        dhm.putDouble(j, val);
        rows.put(i, dhm);            
    }
    
    public String toString() {
        int limit = 10;
        int i = 0;
        StringBuffer buf = new StringBuffer();
        Vector ordered = new Vector();
        Iterator it = rows.keySet().iterator();
       
        while (it.hasNext() && i < limit) {
            Object cur = it.next();
            ordered.add(cur);
            i++;
        }

        if (ordered.size() > 0) {
            DoubleHashMap dhm = (DoubleHashMap)rows.get( ordered.get(0));

            Iterator keysIt = dhm.keySet().iterator();
            i = 0;
            buf.append("\\:[");

            while (keysIt.hasNext() && i < limit) {
                if (i > 0) {
                    buf.append(" ");
                }
                buf.append(keysIt.next());
                i++;
            }
            buf.append("]\n");
        }
        
        for (i = 0; i < ordered.size(); i++) {
            DoubleHashMap dhm = (DoubleHashMap)rows.get( ordered.get(i));
            String aStr = (String)ordered.get(i);
            buf.append(aStr + ":");
            buf.append("[");
            
            Iterator keyIt = dhm.keySet().iterator();
            int j = 0;
            while (keyIt.hasNext()) {
                Object key = keyIt.next();
                double val = dhm.getDouble( key );
                if (j > 0) {
                    buf.append(" ");
                }
                buf.append( val );
                j++;
            }
            buf.append("]");
            buf.append("\n");
        }
        return buf.toString();
    }
}
