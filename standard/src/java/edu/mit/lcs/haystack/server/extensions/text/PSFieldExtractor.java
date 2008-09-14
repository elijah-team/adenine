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
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;

public class PSFieldExtractor {

	public static void extractPSFields(
		Resource res,
		IRDFContainer source,
		IRDFContainer target,
		IServiceAccessor sa)
		throws Exception {

		String haystackPath = SystemProperties.s_basepath;
		String cmd[] = new String[3];
		String str;

		InputStream input = ContentClient.getContentClient(res, source, sa).getContent();

		// check if we are on windows
		if (haystackPath.indexOf('/') == -1) {
			haystackPath += "\\lib\\win32\\psextract";
			cmd[0] = haystackPath + "\\extract.bat";
		} else {
			haystackPath += "/lib/linux/psextract";
			cmd[0] = haystackPath + "/extract.sh";
		}

		File temp1 = File.createTempFile("test", ".ps");
		File temp2 = File.createTempFile("test", ".txt");

		PSUtils.dumpToFile(temp1, input);

		input.close();

		cmd[1] = temp1.getAbsolutePath();
		cmd[2] = temp2.getAbsolutePath();

		PSUtils.executeInDir(cmd, new File(haystackPath));

		InputStream inp = new FileInputStream(temp2);

		Vector lines = extractLines(inp);

		inp.close();

		// line 1

		str = (String) lines.elementAt(0);

		if (str.length() < 5 || str.substring(0, 6).equals("REJECT")) {
			return;
		}

		// line 2	

		str = ((String) lines.elementAt(1)).trim();

		target.add(new Statement(res, Constants.s_dc_title, new Literal(str)));

		//System.out.println("Title: ["+str+"]");

		// line 3

		str = ((String) lines.elementAt(2)).trim();

		int lastpos = 0, pos = 0;

		for (;;) {
			pos = str.indexOf(" ANDALSOSOMEONECALLED ", lastpos);

			if (pos == -1)
				break;

			target.add(
				new Statement(
					res,
					Constants.s_dc_creator,
					new Literal(str.substring(lastpos, pos))));

			//System.out.println("Author: ["+str.substring(lastpos, pos)+"]");

			lastpos = pos + 22;
		}

		target.add(
			new Statement(
				res,
				Constants.s_dc_creator,
				new Literal(str.substring(lastpos, str.length()))));

		//System.out.println("Author: ["+str.substring(lastpos, str.length())+"]");

		// line 4

		str = ((String) lines.elementAt(3)).trim();

		//System.out.println("Abstarct: ["+str+"]");

		Resource resText = StorageUtilities.enterStorage(str, source);
		source.add(new Statement(res, Constants.s_text_summary, resText));
		source.add(
			new Statement(
				resText,
				Constants.s_rdf_type,
				Constants.s_content_HttpContent));
		source.add(
			new Statement(resText, Constants.s_dc_format, new Literal("text/plain")));
	}

	public static Vector extractLines(InputStream input) {

		Vector result = new Vector();

		StringBuffer line;
		int data = 0;

		line = new StringBuffer();

		for (;;) {
			try {
				data = input.read();
			} catch (IOException ex) {
				break;
			}

			if (data == -1)
				break;

			if (data > 20)
				line.append((char) data);
			else {
				if (line.length() > 0) {
					result.addElement(line.toString());
					line = new StringBuffer();
				}
			}
		}

		result.addElement(line.toString());

		return result;
	}

}
