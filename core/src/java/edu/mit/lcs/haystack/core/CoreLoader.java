/* 
 * Copyright (c) 2005 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.mit.lcs.haystack.core;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Loads classes and resources from Haystack plugins.
 * 
 * @version 1.0
 * @author Stephen Garland
 */
public class CoreLoader {
	
	public static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(CoreLoader.class);

	/**
	 * Maps Haystack plugin names to bundles containing plugins.
	 */
	static private Hashtable/*<String,Bundle>*/ bales = null;
	
	/**
	 * Initializes the collection of Haystack plugins.
	 */
	private static void init() {
		bales = new Hashtable();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("edu.mit.csail.haystack.core.bales");
		if (point == null) {
			s_logger.error("Missing extension point edu.mit.csail.haystack.core.bales");
			return;
		}
		s_logger.info("Finding Haystack plugins");
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			String name = extensions[i].getNamespace();
			Bundle b = Platform.getBundle(name);
			if (b == null) s_logger.error("   Missing " + name);
			else s_logger.info("   Found " + name);
			bales.put(name, Platform.getBundle(name));
		}
	}
		
	/**
	 * Loads a class using a specified plugin's class loader.
	 */
	public static Class loadClass(String className, String pluginName) throws Exception {
		return Platform.getBundle(pluginName).loadClass(className);
	}
	
	/**
	 * Loads a class using from some (unsecified) haystack plugin.
	 */
	public static Class loadClass(String className) {
		if (bales == null) init();
		for (Enumeration eb = bales.elements(); eb.hasMoreElements(); ) {
			Bundle b = (Bundle)eb.nextElement();
			// TODO: Load class only at leaves of plugin tree, which have maximal classpaths.
			try { return b.loadClass(className); }
			catch (Exception e) { }
		}
		s_logger.error("Could not load " + className);
		return null;
	}
	
	/**
	 * Retrives a resource using a specified plugin's class loader.
	 */
	public static InputStream getResourceAsStream(String resourceName, String pluginName) {
		try {
			return Platform.getBundle(pluginName).getResource(resourceName).openStream();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Retrives a resource using from some (unsecified) haystack plugin.
	 */
	public static InputStream getResourceAsStream(String resourceName) {
		if (bales == null) init();
		for (Enumeration eb = bales.elements(); eb.hasMoreElements(); ) {
			Bundle b = (Bundle)eb.nextElement();
			// TODO: Retrieve resource only at leaves of plugin tree.
			try { return b.getResource(resourceName).openStream(); }
			catch (Exception e) { }
		}
		s_logger.error("Failed to open " + resourceName);
		return null;	
	}
	
	/**
	 * Returns an array of URLs for Adenine bootstrap files.  The bootstrap file for the
	 * Haystack plugin <code>edu.mit.csail.haystack.pluginName</code> should be on the Java 
	 * classpath for the plugin and have the name <code>/bootstrap/pluginName.ad</code>.
	 */
	public static URL[] getBootstrapURLs() {
		String prefix = "edu.mit.csail.haystack.";
		s_logger.info("Finding Adenine bootstrap files");
		Vector list = new Vector();
		for (Enumeration e = bales.keys(); e.hasMoreElements(); ) {
			String bundleId = (String)e.nextElement();
			String pluginName = bundleId.substring(prefix.length());
			String resourceId = "/bootstrap/" + pluginName;
			String resourceId1 = resourceId + ".ad";
			URL url = ((Bundle)bales.get(bundleId)).getResource(resourceId1);
			if (url == null)
				s_logger.info("   Missing " + resourceId1 + " for " + bundleId);
			else {
				s_logger.info("   Found " + resourceId1 + " for " + bundleId);
				list.addElement(url);
			}
		  }
		URL[] result = new URL[list.size()];
		for (int i = 0; i < list.size(); i++) result[i] = (URL)list.elementAt(i);
		return result;
		
	}
	
}
