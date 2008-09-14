package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.w3c.dom.Node;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.DefaultCluster;

/**
 * @author yks
 */
public class Utilities {
    static private HashSet invalidNodes = null;
    static {
        invalidNodes = new HashSet();
        invalidNodes.add("href");
        invalidNodes.add("script");
        invalidNodes.add("style");
        invalidNodes.add("src");
        invalidNodes.add("link");
        invalidNodes.add("meta");
        invalidNodes.add("head");
    }

    public static void abort(String value) {
        System.err.println(value);
        System.exit(1);
    }

    public static void assertion(boolean value, String msg) {
        if (value) {
            Utilities.abort(msg);
        }
    }

    public static double sumDoubleValueVector(Vector v) {
        double sum = 0;
        Iterator it = v.iterator();
        while (it.hasNext()) {
            Double val = (Double) it.next();
            sum += val.doubleValue();
        }
        return sum;
    }

    public static double maxDoubleValueVector(Vector v) {
        double max = Double.MIN_VALUE;
        Iterator it = v.iterator();
        while (it.hasNext()) {
            Double val = (Double) it.next();
            double d = val.doubleValue();

            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    public static double maxIntegerValueVector(Vector v) {
        int max = Integer.MIN_VALUE;
        Iterator it = v.iterator();
        while (it.hasNext()) {
            Integer val = (Integer) it.next();
            int i = val.intValue();
            
            if (i > max) {
                max = i;
            }
        }
        return max;
    }

    public static int sumIntegerValueVector(Vector v) {
        int sum = 0;
        Iterator it = v.iterator();
        while (it.hasNext()) {
            Integer val = (Integer) it.next();
            sum += val.intValue();
        }
        return sum;
    }

    public static String stringArrayToString(String[] array) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < array.length && i + 1 < array.length;) {
            buf.append(array[i++]);
            buf.append(" = ");
            buf.append(array[i++]);
            buf.append("; ");
        }
        return buf.toString();
    }

    /*
     * encodes any non-null string into a rot13 encoded version.
     */
    public static String rot13(String string) {
        if (string != null && string.length() > 0) {
            int len = string.length();
            StringBuffer buf = new StringBuffer(string);
            for (int i = 0; i < len; i++) {
                int abyte = (int) string.charAt(i);
                int cap = abyte & 32;
                abyte &= ~cap;
                abyte = ((abyte >= 'A') && (abyte <= 'Z') ? ((abyte - 'A' + 13) % 26 + 'A') : abyte) | cap;
                buf.setCharAt(i, (char) abyte);
            }
            return buf.toString();
        } else {
            return string;
        }
    }

    public static boolean isTextNode(Node node) {
        switch (node.getNodeType()) {
        case Node.TEXT_NODE:
            return true;
        default:
            return false;
        }
    }

    public static boolean isValidNode(Node node) {
        switch (node.getNodeType()) {
        case Node.TEXT_NODE:
        case Node.COMMENT_NODE:
            return false;
        default:
            String nodeName = node.getNodeName().toLowerCase();
            if (nodeName != null) {
                if (invalidNodes.contains(nodeName)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void printArray(PrintStream out, Object[] array) throws Exception {
        if (array != null && array.length > 0) {
            out.println("{\n");

            for (int i = 0; i < array.length; i++) {
                out.println(array[i].toString() + ((i == array.length - 1) ? "" : ","));
            }

            out.println("}\n");
            out.flush();
        }
    }

    public static String HashMapInOrder(HashMap map, Vector order) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < order.size(); i++) {
            String key = (String) order.get(i);
            buf.append(key + "=" + map.get(key));
            buf.append(" ; ");
        }
        return buf.toString();
    }

    static public void printClusters(PrintStream out, Vector/* DefaultCluster */clusters) {
        Iterator it = clusters.iterator();

        /* iterator thru the clusters */
        int i = 0;
        while (it.hasNext()) {
            DefaultCluster ithItem = (DefaultCluster) it.next();

            /*
             * System.err.println(ithItem.getFirstNode().getNodeName() + " vs. " +
             * ithItem.getSecondNode().getNodeName() + " cost: " +
             * ithItem.getCost());
             */

            out.println("cluster[" + i + "]: " + ithItem.toString());
            i++;
        }
    }

    public static String[] toStringArray(Object[] array) {
        String[] ret = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i].toString();
        }
        return ret;
    }

    public static void debug(Object obj, String mesg) {
        System.err.println(obj.getClass().getName() + ":" + mesg);
    }

    public static Object[] HashMapToArray(HashMap level) {
        Object[] arr = new Object[level.size()];
        Iterator it = level.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            arr[i++] = it.next();
        }
        return arr;
    }

    public static Vector HashMapKeysToVector(HashMap hashmap) {
        Vector vec = new Vector();
        Iterator it = hashmap.keySet().iterator();

        while (it.hasNext()) {
            vec.add(it.next());
        }
        return vec;
    }

    public static Object[] VectorToArray(Vector vec) {
        Object[] arr = null;
        if (vec.size() > 0) {
            arr = new Object[vec.size()];
            Iterator it = vec.iterator();
            int i = 0;
            while (it.hasNext()) {
                arr[i++] = it.next();
            }
        }
        return arr;
    }

    public static String VectorToString(Vector vec) {
        StringBuffer buf = new StringBuffer();
        Iterator it = vec.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }
            buf.append((String) it.next());
        }
        return buf.toString();
    }

    public static LinkedHashSet VectorToHashSet(Vector vec) {
        return new LinkedHashSet(vec);
    }

    public static String[] SetToStringArray(Set vec) {
        String[] arr = null;
        int size = vec.size();
        if (size > 0) {
            arr = new String[size];
            Iterator it = vec.iterator();
            int i = 0;
            while (it.hasNext()) {
                arr[i++] = (String) it.next();
            }
        }
        return arr;
    }

    public static double round(double number, int positions) {
        if (positions != 0) {
            double pow = Math.pow(10, positions);
            return Math.round(number * pow) / pow;
        } else {
            return number;
        }
    }

    public static Vector ArrayToVector(Object[] arr) {
        Vector vec = new Vector();
        for (int i = 0; i < arr.length; i++) {
            vec.add(arr[i]);
        }
        return vec;
    }

    public static int[] IntegerVectorToIntArray(Vector vec) {
        int[] arr = new int[vec.size()];
        Iterator it = vec.iterator();
        int i = 0;
        while (it.hasNext()) {
            arr[i++] = ((Integer) it.next()).intValue();
        }
        return arr;
    }

    public static String serializeMap(TreeMap map) {
        StringBuffer buf = new StringBuffer();
        Iterator it = map.entrySet().iterator();
        boolean first = true;
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next();
            if (!first) {
                buf.append("\n");
            } else {
                first = false;
            }
            buf.append(ent.getKey() + "=" + ent.getValue());
        }
        return buf.toString();
    }

    public static TreeMap deserializeMap(String mapString) {
        if (mapString != null) {
            TreeMap tm = new TreeMap();

            String[] entries = mapString.split("\n");
            for (int i = 0; i < entries.length; i++) {
                String[] kv = entries[i].split("=");
                if (kv != null && kv.length > 1) {
                    tm.put(kv[0], kv[1]);
                }
            }
            return tm;
        } else {
            return null;
        }
    }

    public static HashMap reverseHashMap(HashMap hashMap) {
        HashMap result = new HashMap();
        Iterator it = hashMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }
}