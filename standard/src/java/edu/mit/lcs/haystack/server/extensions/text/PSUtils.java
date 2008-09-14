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

package edu.mit.lcs.haystack.server.extensions.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PSUtils {

	public static boolean isUnix() {
		return false;
	}

	public static void dumpToFile(File f, InputStream input) throws IOException {

		OutputStream output = new FileOutputStream(f);

		byte data[] = new byte[1024];
		int res;

		for (;;) {
			res = input.read(data);
			if (res == -1)
				break;
			output.write(data, 0, res);
		}

		output.close();
	}

	public static String fileToString(File f) throws FileNotFoundException, IOException {

		InputStream input = new FileInputStream(f);

		StringBuffer buf = new StringBuffer(1024);

		byte data[] = new byte[1024];

		int res;

		for (;;) {
			res = input.read(data);
			if (res == -1)
				break;

			for (int t = 0; t < res; t++)
				buf.append((char) data[t]);
		}

		input.close();

		return buf.toString();

	}

	public static void execute(String command[]) throws IOException {

		Runtime runtime = Runtime.getRuntime();
		Process process;

		process = runtime.exec(command);

		for (;;) {

			try {
				process.waitFor();
				break;
			} catch (InterruptedException ex) {
			}

		}

		return;

	}

	public static void executeInDir(String command[], File dir) throws IOException {

		Runtime runtime = Runtime.getRuntime();
		Process process;

		process = runtime.exec(command, null, dir);

		for (;;) {

			try {
				process.waitFor();
				break;
			} catch (InterruptedException ex) {
			}

		}

		return;

	}

	public static InputStream executeGetInputStream(String command[], InputStream input)
		throws IOException {

		OutputStream output = null;

		Runtime runtime = Runtime.getRuntime();
		Process process;

		process = runtime.exec(command);

		output = process.getOutputStream();

		streamToStream(input, output);

		for (;;) {

			try {
				process.waitFor();
				break;
			} catch (InterruptedException ex) {
			}

		}

		return process.getInputStream();

	}

	public static InputStream executeGetInputStream(
		String command[],
		InputStream input,
		File dir)
		throws IOException {

		OutputStream output = null;

		Runtime runtime = Runtime.getRuntime();
		Process process;

		process = runtime.exec(command, null, dir);

		output = process.getOutputStream();

		streamToStream(input, output);

		for (;;) {

			try {
				process.waitFor();
				break;
			} catch (InterruptedException ex) {
			}

		}

		return process.getInputStream();

	}

	public static void streamToStream(InputStream input, OutputStream output)
		throws IOException {

		int data;

		for (;;) {
			data = input.read();
			if (data == -1)
				break;

			try {
				output.write(data);
			} catch (IOException ex) {
				break;
			}
		}

		output.close();
	}

	public static String streamToString(InputStream stream) throws IOException {

		StringBuffer buf = new StringBuffer(1024);
		int data;

		for (;;) {
			data = stream.read();
			if (data == -1)
				break;
			buf.append((char) data);
		}

		return buf.toString();
	}

}
