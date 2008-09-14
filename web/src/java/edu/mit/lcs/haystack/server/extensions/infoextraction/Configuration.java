/*
 * Created on Jun 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * @author yks Simple configuration Class, (reads a configuration file, stores
 *         the contents in a 2 level hash
 */
public class Configuration {

	LinkedHashMap/* <String,Vector> */configEntries;
	LinkedHashMap/* <String,HashMap <String,Vector>> */hashEntries;
	String fileName;
	String COMMENTS = "# This configuration file can contain\n"
			+ "# <key>=<value>  pairs\n"
			+ "# or \n"
			+ "# [section]\n"
			+ "# <key1>=<value1>\n"
			+ "# \n"
			+ "# section style key-value pairs.\n"
			+ "# section key/value pairs are accessed via get(key,subkey)\n"
			+ "# interface\n"
			+ "# Having multiple <key> values will result in a list of <values>\n\n";

	/* 
	 * return an iterator on the names of the Sections
	 */
	public Iterator getSectionKeys() {
		return hashEntries.keySet().iterator();
	}
	
	public void put(String key, String value) {

		Vector v = (Vector) configEntries.get(key);
		if (v == null) {
			v = new Vector();
			configEntries.put(key, v);
		}
		v.add(value);

	}

	public void put(String key, String subkey, String value) {

		HashMap h = (HashMap) hashEntries.get(key);
		if (h == null) {
			h = new HashMap();
			hashEntries.put(key, h);
		}
		Vector v = (Vector) configEntries.get(key);
		if (v == null) {
			v = new Vector();
			h.put(subkey, v);
		}
		v.add(value);
	}

	public String get(String key) {

		Vector v = getList(key);
		if (v != null) {
			return (String) v.get(0);
		} else {
			return null;
		}
	}

	public Vector/* String */getList(String key) {

		return (Vector) configEntries.get(key);
	}

	public String get(String key, String subkey) {

		Vector v = getList(key, subkey);
		if (v != null) {
			if (v.size() > 0) {
				return (String) v.get(0);
			}
		}
		return null;
	}

	public Vector/* String */getList(String key, String subkey) {

		HashMap h = (HashMap) hashEntries.get(key);
		if (h != null) {
			Vector v = (Vector) h.get(subkey);
			return v;
		}
		return null;
	}

	public HashMap getHash(String key) {
		return (HashMap) hashEntries.get(key);
	}

	/*
	 * Resets the configuration, empties all entries
	 */
	private void reset() {

		/* resets the hash tables */
		configEntries = new LinkedHashMap();
		hashEntries = new LinkedHashMap();
	}

	public Configuration(String filename) {

		this.fileName = new String(filename);
		configEntries = new LinkedHashMap();
		hashEntries = new LinkedHashMap();
	}

	/*
	 * Write the configuration into the internal filename
	 */

	public void saveToFile() throws IOException {

		saveToFile(fileName);
	}

	/*
	 * Writes the configuration into a file
	 */
	public void saveToFile(String fileName) throws IOException {

		File file = new File(fileName);
		FileOutputStream fout = new FileOutputStream(file);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout));
		bw.write(COMMENTS);
		bw.write(toSerializedString());
		bw.close();
		fout.close();
	}

	/*
	 * Returns the contents of the configuration as a string
	 */
	public String toSerializedString() {

		StringBuffer sb = new StringBuffer();
		Iterator it = configEntries.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector vec = (Vector) configEntries.get(key);
			for (int i = 0; i < vec.size(); i++) {
				sb.append(key + "=" + (String) vec.get(i) + "\n");
			}
		}
		it = hashEntries.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			HashMap hash = (HashMap) hashEntries.get(key);
			sb.append("[" + key + "]\n");
			Iterator subit = hash.keySet().iterator();
			while (subit.hasNext()) {
				String subkey = (String) subit.next();
				Vector vec = (Vector) hash.get(subkey);
				for (int i = 0; i < vec.size(); i++) {
					sb.append(subkey + "=" + (String) vec.get(i) + "\n");
				}
			}
			sb.append("\n"); /* blank line to separate sections */
		}
		return sb.toString();
	}

	/*
	 * Reads the configuration from the filename specified by the
	 * constructor
	 */
	public void readFromFile() throws IOException, FileNotFoundException {

		readFromFile(fileName);
	}

	/*
	 * Reads the configuration from a file, resets previous contents
	 * of this configuration.
	 */
	public void readFromFile(String filename) throws IOException,
			FileNotFoundException {

		reset();
		File file = new File(filename);
		FileInputStream fin = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fin));
		String line;
		String sectionName = null;
		while (null != (line = br.readLine())) {
			line = line.trim();
			/* skip over blank lines. */
			if (line.length() == 0) {
				sectionName = null;
				continue;
			}
			/* skip over comments */
			if (line.startsWith("#")) {
				continue;
			}
			line.split("=");
			if (line.startsWith("[") && line.endsWith("]")) {
				sectionName = line.substring(1, line.length() - 1);
				continue;
			}
			if (line.indexOf("=") >= 0) {
				String values[] = line.split("=", 2);
				String key = null;
				String value = null;
				if (values.length >= 1) {
					key = values[0];
					if (values.length == 1) {
						value = "";
					} else {
						value = values[1];
					}
				}
				
				if ( key != null && value != null ) {
					key = key.trim();
					value = value.trim();

					if (sectionName != null) {
						put(sectionName, key, value);
					} else {
						put(key, value);
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		Configuration config = new Configuration("config_test");
		config.put("hello", "world");
		config.put("hello", "world2");
		config.put("section", "jobs", "welldone");
		try {
			System.err.println("deserialized Config file looks like: ");
			System.err.println(config.toSerializedString());
			config.saveToFile();
			config.readFromFile();
			System.err.println("deserialized Config file looks like: ");
			System.err.println(config.toSerializedString());
			System.err.println("get(hello): " + config.get("hello"));
			System.err.println("get(section,jobs): "
					+ config.get("section", "jobs"));
			config = new Configuration("configuration.cfg");
			config.readFromFile();
			System.err.println(config.toSerializedString());
		} catch (Exception e) {
			System.err.println("Error: " + e);
		}
	}
}