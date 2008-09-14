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

package edu.mit.lcs.haystack;

import edu.mit.lcs.haystack.rdf.*;
import java.io.*;

/**
 * @version 	1.0
 * @author
 */
public class GenerateUserID {

	public static void main(String[] args) {
		Resource userResource = Utilities.generateUniqueResource();
		boolean mustRerun = false;
		try {
			if (!new File("haystack-userid").exists()) {
				FileOutputStream fos = new FileOutputStream("haystack-userid");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
				bw.write(userResource.getURI());
				bw.close();
				fos.close();
				mustRerun = true;
			} else {
				FileInputStream fis = new FileInputStream("haystack-userid");
				BufferedReader br = new BufferedReader(new InputStreamReader(fis));
				userResource = new Resource(br.readLine());
				br.close();
				fis.close();
			}
			
			if (mustRerun || (!new File("haystack-keys").exists())) {
				// Create a keystore
				String basePath = SystemProperties.s_basepath;

				String [] keyargs = new String[] {
				    "-genkey", "-alias", userResource.getURI(),
				    "-keypass", "haystack", "-keystore", 
				    "haystack-keys", "-dname", 
				    "CN=Someone, OU=Haystack, O=Haystack, L=Somewhere, ST=Somewhere, C=US",
				    "-storepass", "haystack" };
				
				sun.security.tools.KeyTool.main(keyargs);


				/*
				System.out.println("p = " + path + " basep = " + basePath);

				String [] envp = new String[] { new String("PATH=" + path) };

				 String command = "keytool -genkey -alias " + userResource.getURI() + " -keypass haystack -keystore haystack-keys -dname \"CN=Someone, OU=Haystack, O=Haystack, L=Somewhere, ST=Somewhere, C=US\" -storepass haystack";
				System.out.println("RUN: " + command);
				Process p = Runtime.getRuntime().exec(command, envp);


				p.waitFor();

				*/
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}



