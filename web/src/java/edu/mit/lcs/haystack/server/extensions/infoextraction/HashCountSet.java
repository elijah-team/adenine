/*
 * Created on Sep 15, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * @author yks
 * A hashset with a Count of the number of times
 * the hash key has been added.
 */
public class HashCountSet {
    HashMap set = new HashMap();
    
    public int sum() {
        int sum = 0;
        Iterator it = set.values().iterator();
        while (it.hasNext()) {
            Integer i = (Integer)it.next();
            sum += i.intValue();
        }
        return sum;
    }
    
    public Iterator iterator() {
        return set.keySet().iterator();
    }
    
    public boolean add(Object key) {
        //super.add(key);
        Integer count = (Integer)set.get(key);
        if (count != null) {
            count = new Integer(count.intValue()+1);
            set.put(key, count);
            return false;
        } else {
            set.put(key, new Integer(1));
            return true;
        }
    }
    
    public boolean addAll(Collection c) {
        boolean changed = true; //super.addAll(c);
        Iterator it = c.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
        return changed;
    }

    public boolean putAllValues(Map m) {
        boolean changed = true;
        Iterator it = set.values().iterator();
        while (it.hasNext()) {
            add(it.next());
        }
        return changed;
    }

    public void setCount(Object key, int num) {
        Integer count = new Integer(num);
        set.put(key, count);
    }

    public void incrCount(Object key, int num) {
        Integer count = (Integer)set.get(key);
        if (count == null) {
            count = new Integer(num);
        } else {
            count = new Integer(count.intValue() + num);
        }
        set.put(key, count);
    }

    public int getCount(Object key) {
        Integer count = (Integer)set.get(key);
        if (count == null) {
            return 0;
        } else {
            return count.intValue();
        }
    }
}
