/*
 * Created on Nov 26, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.labeller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Vector;

/**
 * @author yks
 */
public class LabelledEntry {
    private String url;

    private LinkedHashSet /* NodeID */nodeIDs = new LinkedHashSet();

    private String page;

    final private static String separator = "\n=========\n";

    final private static String fieldSep = ":";

    final private static String recordSep = "\n";

    final private static String listSep = ",";

    final private static String ID = "ID";

    final private static String URL = "URL";

    final private static String NID = "NODEIDS";

    final private static HashMap isList = new HashMap();

    private int id;

    private static int counter = 0;

    static {
        isList.put(NID, null);
    }

    static public void setNextID(int nextID) {
        counter = nextID;
    }

    static public boolean isList(String fieldName) {
        return isList.containsKey(fieldName);
    }

    public String getKey() {
        return url;
    }

    public String getPage() {
        return page;
    }
    
    public int getID() {
        return id;
    }

    private int nextID() {
        return counter++;
    }

    public LabelledEntry() {
    }

    public LabelledEntry(String url, String page) {
        this.id = nextID();
        this.url = url;
        this.page = page;
    }

    public void addNodeID(Object nid) {
        nodeIDs.add(nid);
    }

    public void removeNodeID(Object nid) {
        nodeIDs.remove(nid);
    }

    public Vector getNodeIDs() {
        return new Vector(nodeIDs);
    }

    static public LinkedHashSet hashSetDeserialize(String str) {
        LinkedHashSet lhs = new LinkedHashSet();
        String[] vals = str.split(listSep);
        for (int i = 0; i < vals.length; i++) {
            lhs.add(vals[i]);
        }
        return lhs;
    }

    /* outputs the NodeIDs, in this collection */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{\n");
        buf.append( hashSetSerialize(this.nodeIDs));
        buf.append("}\n");
        return buf.toString();
    }
    
    public String hashSetSerialize(LinkedHashSet lhs) {
        StringBuffer buf = new StringBuffer();
        Iterator it = lhs.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                buf.append(listSep);
            } else {
                first = false;
            }
            buf.append(it.next());
            
        }
        return buf.toString();
    }

    public String serialize() {
        StringBuffer buf = new StringBuffer();
        buf.append(URL + fieldSep + url + recordSep);
        buf.append(NID + fieldSep + hashSetSerialize(nodeIDs) + recordSep);
        buf.append(ID + fieldSep + String.valueOf(id) + recordSep);

        buf.append(separator + page);
        return buf.toString();
    }

    static public HashMap parseHeaders(String str) {
        HashMap map = new HashMap();
        String[] headers = str.split(recordSep);
        for (int i = 0; i < headers.length; i++) {
            String[] keyval = headers[i].split(fieldSep, 2);
            String key = keyval[0];
            String val = keyval[1];

            if (isList(key)) {
                LinkedHashSet lhs = hashSetDeserialize(val);
                map.put(key, lhs);
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    static public LabelledEntry deserialize(String str) {
        LabelledEntry le = new LabelledEntry();
        String[] headersAndBody = str.split(separator, 2);
        if (headersAndBody.length > 0) {
            String headers = headersAndBody[0];
            if (headersAndBody.length > 1) {
                le.page = headersAndBody[1];
            } else {
                le.page = null;
            }

            HashMap headerMap = parseHeaders(headers);

            le.url = (String) headerMap.get(URL);
            le.nodeIDs = (LinkedHashSet) headerMap.get(NID);
            le.id = Integer.parseInt((String) headerMap.get(ID));
        }
        return le;
    }
}