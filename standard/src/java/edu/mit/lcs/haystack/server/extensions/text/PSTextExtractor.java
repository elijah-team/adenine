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
import java.io.InputStream;

import edu.mit.lcs.haystack.SystemProperties;

/**
 * @version 	1.0
 * @author      Janis Sermulins
 */

public class PSTextExtractor implements ITextExtractor {

	/**
	 * @see ITextExtractor#convertToText(InputStream)
	 */
	public InputStream convertToText(InputStream is) {

		InputStream result;

		try {
			File temp1 = File.createTempFile("test", ".ps");
			File temp2 = File.createTempFile("test", ".txt");

			PSUtils.dumpToFile(temp1, is);

			is.close();

			String cmd[] = new String[3];

			String haystackPath = SystemProperties.s_basepath;

			if (haystackPath.indexOf('/') == -1) {
				cmd[0] = "ps2ascii.bat";
			} else {
				cmd[0] = "ps2ascii";
			}

			cmd[1] = temp1.getAbsolutePath();
			cmd[2] = temp2.getAbsolutePath();

			PSUtils.execute(cmd);

			result = new FileInputStream(temp2);

			temp1.delete();
			temp2.delete();

		} catch (Exception ex) {

			result = null;
		}

		return result;
	}
}
