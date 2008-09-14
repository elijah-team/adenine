/*
 * Created on Nov 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.HashMap;

/**
 * @author yks
 */
public class IntHashMap extends HashMap {
    final static int NOT_VALID = -1;
    
    static public boolean isValid(int val) {
        return (val != NOT_VALID);
    }
    
    public void putInt(Object key, int val) {
        this.put(key, new Integer(val));
    }
    
    public int getInt(Object key) {
        Integer val = (Integer)this.get(key);
        if (val == null) {
            return NOT_VALID;
        } else {
            return val.intValue();
        }
    }
}
