/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
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

package edu.mit.lcs.haystack.adenine.interpreter;

import java.util.*;
import java.io.*;

/**
 * Manages precompiled Adenine code
 * @version 	1.0
 * @author		Janis Sermulins
 */

public class AdenineClassLoaderManager {
	HashMap m_classNameToClassLoader = new HashMap();
	HashSet m_excludes = new HashSet();
	AdenineClassLoader m_currentLoader = new AdenineClassLoader(getClass().getClassLoader());
	String m_precompilePath;

	private static AdenineClassLoaderManager s_instance = null;

	private AdenineClassLoaderManager() {
		m_precompilePath =
			new File(System.getProperty("edu.mit.csail.haystack.precompile", "."))
				.getAbsolutePath();
	}

	public static AdenineClassLoaderManager getInstance() {
		if (s_instance == null) {
			s_instance = new AdenineClassLoaderManager();
		}

		return s_instance;
	}

	public void updateClass(File file, String className) {
		throw new RuntimeException("This method is deprecated");
	}

	public ClassLoader updateClass(byte[] bytes, String className) {
		if (m_currentLoader.hasClass(className)) {
			m_currentLoader = new AdenineClassLoader(getClass().getClassLoader());
		}

		m_classNameToClassLoader.put(className, m_currentLoader);
		m_currentLoader.loadDefineClass(bytes, className);

		try {
			FileOutputStream fos =
				new FileOutputStream(makeFileFromClassName(className));

			fos.write(bytes);
			fos.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return m_currentLoader;
	}

	synchronized public Class getClass(String className) {
		Class klass = null;
		ClassLoader loader =
			(ClassLoader) m_classNameToClassLoader.get(className);

		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}

		try {
			klass = loader.loadClass(className);
		} catch (ClassNotFoundException ex) {
			if (!m_excludes.contains(className)) {
				try {
					File f = makeFileFromClassName(className);
					FileInputStream fis = new FileInputStream(f);
					int length = (int) f.length();
					byte[] bytes = new byte[length];
					int i = 0;
					int j = 0;

					while (i < length && j != -1) {
						j = fis.read(bytes, i, length - i);
						i += j;
					}

					fis.close();

					m_classNameToClassLoader.put(className, m_currentLoader);
					m_currentLoader.loadDefineClass(bytes, className);

					klass = m_currentLoader.loadClass(className);
				} catch (Exception e) {
					e.printStackTrace();
					m_excludes.add(className);
				}
			}
		}

		return klass;
	}

	File makeFileFromClassName(String className) throws IOException {
		return new File(m_precompilePath, className + ".class");
	}
}
