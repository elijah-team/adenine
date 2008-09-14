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

package edu.mit.lcs.haystack.eclipse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.javaByteCode.JavaByteCodeCompiler;
import edu.mit.lcs.haystack.adenine.compilers.rdfCode.RDFCodeCompiler;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * @author Dennis Quan
 */
public class SetupAgent extends GenericService {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SetupAgent.class);
	
	protected Resource m_defaultPackage;
	
	public void init(String basePath, ServiceManager manager, Resource res) throws ServiceException {
		super.init(basePath, manager, res);
		// Set up RDF container
		IRDFContainer source = m_serviceManager.getRootRDFContainer();
		// Load RDF in /bootstrap/packages.ad for Haystack Standard Functionality plugin
		ICompiler rdfCompiler = new RDFCodeCompiler(source); 
		try {
			InputStream is = CoreLoader.getResourceAsStream("/bootstrap/packages.ad");
			rdfCompiler.compile(null, new InputStreamReader(is), null, null, null);
			is.close();
			s_logger.info("Compiled /bootstrap/packages.ad"); 
		} catch (Exception e) { s_logger.error("Failed to compile /bootstrap/packages.ad.", e); }
		// Load RDF in /bootstrap/pluginName.ad for other plugins
		URL[] boot = CoreLoader.getBootstrapURLs();
		for (int i = 0; i < boot.length; i++) {
			try { 
				InputStream is = boot[i].openStream();
				rdfCompiler.compile(null, new InputStreamReader(is), null, null, null);
				is.close();
				s_logger.info("Compiled Adenine file " + boot[i]);
			} catch (Exception e) { s_logger.info("Failed to compile Adenine file " + boot[i], e); }
		}
		
		Vector/*<Resource>*/ packages = new Vector();
		packages.addElement(new Resource(SystemProperties.s_packageSet));
		// Install default package set
		
		// Load /bootstrap/pluginName.ad file for other plugins
		for (int i = 0; i < boot.length; i++) {
			// Install package set /bootstrap/pluginName.ad
			String urlString = boot[i].toString();
			String fileName = urlString.substring(urlString.indexOf("/bootstrap"), urlString.length() - 3);
			packages.addElement(new Resource("http://haystack.lcs.mit.edu" + fileName));
		}
		
		PackageFilterRDFContainer packageFilterRDFC = new PackageFilterRDFContainer(source, null);
		IRDFContainer authoringRDFC = null;
		if (source.supportsAuthoring()) authoringRDFC = (IRDFContainer)source;
		PackageInstallDisplay pid = new PackageInstallDisplay(
				m_serviceManager.getResource(), 
				m_serviceManager.getIdentityManager().getUnauthenticatedIdentity(m_userResource), 
				authoringRDFC, 
				packages,
				packageFilterRDFC, 
				source, 
				new JavaByteCodeCompiler(packageFilterRDFC), 
				m_serviceManager
		);
		try { new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, pid); }
		catch (InvocationTargetException e1) { e1.printStackTrace(); }
		catch (InterruptedException e1) { e1.printStackTrace(); }
		
		// Iterate through data, finding paths (mainly) to images
		Resource[] data = Utilities.getResourceSubjects(Constants.s_rdf_type, Constants.s_config_OntologyData, source);
		for (int j = 0; j < data.length; j++) {
			String resourcePath = Utilities.getLiteralProperty(data[j], Constants.s_content_path, source);
			try {
				source.add(new Statement(data[j], Constants.s_rdf_type, Constants.s_content_JavaClasspathContent));
				source.add(new Statement(data[j], Constants.s_content_path, new Literal(resourcePath)));
			} catch (RDFException e) { s_logger.error("RDF exception: ", e); }
		}
	}
	
	public static String computeMD5(InputStream is) {
		if (is == null) return null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String str;
			StringBuffer sb = new StringBuffer();
			while ((str = reader.readLine()) != null) sb.append(str + "\n");
			String byteChars = "0123456789abcdef";
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] bytes = md.digest(sb.toString().getBytes());
			sb = new StringBuffer();
			for (int i = 0; i < bytes.length; i++) {
				int loNibble = bytes[i] & 0xf;
				int hiNibble = (bytes[i] >> 4) & 0xf;
				sb.append(byteChars.charAt(hiNibble));
				sb.append(byteChars.charAt(loNibble));
			}
			return sb.toString();
		} 
		catch(NoSuchAlgorithmException e) { s_logger.error("Could not find MD5 digest algorithm."); }
		catch(IOException e) { s_logger.error("Exception: ", e);}
		return null;
	}
	
	
	
}
