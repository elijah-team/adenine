/*
 * Created on Nov 26, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.labeller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cache.DocumentCache;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;

/**
 * @author yks
 */
public class LabelledDataCache {

    final static String META_FILE = "metafile";

    LinkedHashMap entries = new LinkedHashMap();

    IDOMBrowser browser;

    File dirFile;

    public LinkedHashSet getURLs() {
        return new LinkedHashSet(entries.keySet());
    }
    
    public LabelledDataCache(String dirName) {
        this(dirName, null);
    }
    
    public LabelledDataCache(String dirName, IDOMBrowser browser) {
        this.browser = browser;
        try {
            File file = new File(dirName);
            if (file.exists()) {
                if (file.isDirectory()) {
                    dirFile = file;
                } else {
                    throw new Exception(dirName + " is not a directory");
                }
            } else {
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        initFromFile();
    }

    public Vector/* String */getEntries(String url) {
        Vector/* String */vec = new Vector();
        LabelledEntry le = (LabelledEntry) entries.get(url);
        
        if (le != null) {
            Iterator it = le.getNodeIDs().iterator();
            while (it.hasNext()) {
                String entry = (String) it.next();
                vec.add(entry);
            }
        }
        return vec;
    }

    public String getPage(String url) {
        LabelledEntry le = (LabelledEntry) entries.get(url);
        if (le != null) {
            return le.getPage();
        }
        return null;
    }
    
    public void addEntry(String url, String nodeID) {
        LabelledEntry e;
        
        if (url != null) {
            e = (LabelledEntry) entries.get(url);
            if (e == null) {
                String page = DocumentCache.getPage(browser, url);
                e = new LabelledEntry(url, page);
                entries.put(url, e);
            }
            e.addNodeID(nodeID);
        }
    }

    public void removeEntry(String url, String nodeID) {
        LabelledEntry e = (LabelledEntry) entries.get(url);
        if (url != null) {
            e = (LabelledEntry) entries.get(url);
            e.removeNodeID(nodeID);
        }

    }

    public void removeEntry(String url) {
        entries.remove(url);
    }

    public void saveToFile() {
        try {

            /**
             * iterate through each LabelledEntry and write each into a file.
             */
            Iterator it = entries.values().iterator();
            TreeMap vec = new TreeMap();
            while (it.hasNext()) {
                LabelledEntry entry = (LabelledEntry) it.next();

                String fileName = String.valueOf(entry.getID());
                File file = new File(dirFile, fileName);
                FileOutputStream fout = new FileOutputStream(file);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout));
                bw.write(entry.serialize());
                bw.close();
                fout.close();

                vec.put(new Integer(entry.getID()), entry.getKey());
            }

            /**
             * write the meta data into a separate META_FILE.
             */
            File file = new File(dirFile, META_FILE);
            FileOutputStream fout = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout));
            bw.write(Utilities.serializeMap(vec));
            bw.close();
            fout.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void initFromFile() {
        TreeMap map = null;
        try {
            /**
             * read in the META_FILE
             */
            File file = new File(dirFile, META_FILE);
            FileInputStream fin = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));
            StringBuffer buf = new StringBuffer();
            String line;
            while (br.ready()) {
                line = br.readLine();
                // skip commented lines
                if (! line.startsWith("#")) {
                    buf.append(line+"\n");
                }
            }
            
            map = Utilities.deserializeMap(buf.toString());
            br.close();
            fin.close();
        } catch (FileNotFoundException fnfe) {
            /*
             * meta file not found then just assume empty meta file.
             */
            System.err.println("\"" + dirFile + "/" + META_FILE + "\" not found, assuming working from scratch");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (map != null) {
            /*
             * for each entry in the meta file read in the actual entry records.
             */
            Iterator it = map.keySet().iterator();
            int max = -1;
            while (it.hasNext()) {
                String key = (String) it.next();

                System.err.println("READING: " + key);
                try {
                    File file = new File(dirFile, key);
                    FileInputStream fout = new FileInputStream(file);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fout));

                    StringBuffer buf = new StringBuffer();
                    while (br.ready()) {
                        buf.append(br.readLine()+"\n");
                    }

                    LabelledEntry lbe = LabelledEntry.deserialize(buf.toString());
                    entries.put(lbe.getKey(), lbe);
                    br.close();
                    fout.close();

                    int idVal = Integer.parseInt(key);
                    if (idVal > max) {
                        max = idVal;
                    }

                } catch (FileNotFoundException fnfe) {
                    /* file for entry not found */
                    System.err.println("File: " + dirFile + "/" + key + " not found, deleting entry");
                    map.remove(key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            LabelledEntry.setNextID(max + 1);
        }
    }
}