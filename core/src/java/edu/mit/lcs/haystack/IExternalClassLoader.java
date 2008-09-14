/*
 * Created on Jan 31, 2004
 */
package edu.mit.lcs.haystack;

import java.io.InputStream;

/**
 * @author Dennis Quan
 */
public interface IExternalClassLoader {
	public Class loadClass(String className, String pluginName) throws Exception;
	public InputStream getResourceAsStream(String resourceName, String pluginName);
}
