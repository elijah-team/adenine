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

package edu.mit.lcs.haystack.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class DefaultMarshaller implements IMarshaller {

	static public StringBuffer stringify(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			String str = Byte.toString(b[i]);
			int c = 5 - str.length();
			for (int j = 0; j < c; j++) {
				str = str + " ";
			}
			sb.append(str);
		}
		return sb;
	}
	
	static public byte[] destringify(String s) {
		byte[] out = new byte[s.length() / 5];
		for (int i = 0; i < s.length() / 5; i++) {
			String str = s.substring(i * 5, (i + 1) * 5);
			str = str.substring(0, str.indexOf(" "));
			out[i] = Byte.parseByte(str);
		}
		return out;
	}

	/**
	 * @see IMarshaller#encode(Object)
	 */
	public String encode(Object o) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			return stringify(baos.toByteArray()).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @see IMarshaller#decode(String)
	 */
	public Object decode(String str) {
		try {
			Object o = new ObjectInputStream(new ByteArrayInputStream(destringify(str))).readObject();
			return o;
		} catch (Exception e) {
			return str;
		}
	}

}
