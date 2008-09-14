/*
 * Created on Nov 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.LinkedHashMap;

/**
 * @author yks
 */
public class DoubleHashMap extends LinkedHashMap {
    final static double NOT_VALID = -1.0;
    
    static public boolean isValid(double val) {
        return (val != NOT_VALID);
    }
    
    public void putDouble(Object key, double val) {
        this.put(key, new Double(val));
    }
    
    public double getDouble(Object key) {
        Double val = (Double)this.get(key);
        if (val == null) {
            return NOT_VALID;
        } else {
            return val.doubleValue();
        }
    }
}
