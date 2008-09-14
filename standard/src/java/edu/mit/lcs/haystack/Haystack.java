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

import java.io.*;
import java.net.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.rdfCode.RDFCodeCompiler;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.converters.AdenineConverter;
import edu.mit.lcs.haystack.security.Identity;
import edu.mit.lcs.haystack.security.IdentityManager;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Main class for starting up Haystack server and Ozone UI.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class Haystack {
	
	public static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Haystack.class);
	
	static {
		try { PropertyConfigurator.configure(Haystack.class.getResource("log4j.properties")); }
		catch (Exception e) {
			s_logger.error("Could not locate log4j.properties in edu/mit/lcs/haystack classpath", e);
		}
	}
	
	/**
	 * Constructs a new Haystack, with the option of starting it now or later.
	 */
	public Haystack(boolean startNow) { if (startNow) start(); }
	
	/**
	 * Constructs and starts a new Haystack.
	 */
	public Haystack() { this(true); }
	
	/**
	 * Starts Haystack.
	 * 
	 * @see SystemProperties
	 */
	public void start() {
		try {
			s_logger.info("Starting Haystack");
			
			// Begin initializations
			initializePaths();
			initializeKeys();
			
			// Set up identity manager
			m_identityManager = new IdentityManager(m_keyFile.toString());
			if (!m_identityManager.containsIdentity(m_userResource)) {
				s_logger.error("haystack-keys file does not contain user's keys");
				return;
			}
			m_userIdentity = m_identityManager.authenticate(m_userResource, "haystack");
			
			String localRDF = SystemProperties.s_bootstrap;
			
			// Create bootstrap.rdf if necessary
			File bootstrap = new File(localRDF);
			if (!bootstrap.exists()) createBootstrapRDF(bootstrap);
			else { s_logger.info("found bootstrap file: " + bootstrap); }
			
			// "Mount" the root RDF source and start the server
			IRDFContainer source = new LocalRDFContainer();
			String basePath = m_baseUserPath.toString() + File.separatorChar;
			
			try {
				new AdenineConverter().parse(new URL("file", "", localRDF.replace(File.separatorChar, '/')), source);
			} catch (Exception e) {
				s_logger.error("An error occurred reading local RDF file " + localRDF, e);
				throw new RuntimeException();
			}
			
			Resource baseRes;
			try {
				baseRes = (Resource)source.extract(null, Constants.s_rdf_type, Constants.s_config_HaystackServer);
			} catch (Exception e) {
				s_logger.error("Bootstrap file " + localRDF + " missing config:HaystackServer reference", e);
				throw new RuntimeException();
			}
					
			// Create a service manager
			m_serviceManager = new ServiceManager(basePath, source, baseRes, m_identityManager);
			try { m_serviceManager.start(); }
			catch (Exception e) {
				s_logger.error("An error occurred starting the service manager with bootstrap file " + localRDF + ".", e);
			}
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					s_logger.info("Shutting down the service manager.");
					try { m_serviceManager.stop(); }
					catch(Exception e) {
						s_logger.error("An error occurred while shutting down the system", e);
					}
					s_logger.info("Shutting down the system.");
				}
			});
		} catch (Exception e) { s_logger.error("System error", e); }
	} 
	
	private File m_baseSystemPath;
	private File m_baseUserPath;
	private File m_identityFile;
	private File m_keyFile;
	private Resource m_userResource;
	private boolean m_debug;
	public IdentityManager m_identityManager;
	public ServiceManager m_serviceManager;
	public Identity m_userIdentity;
	
	public IRDFContainer getRootRDFContainer(){
		return m_serviceManager.getRootRDFContainer();
	}
	
	public void toggleDebug() {
		m_debug = !m_debug;
		s_logger.info("Set debug to " + m_debug);
	}
	
	public boolean isDebug() {
		return m_debug;
	}
	
	public static void main(String[] args) {
		Haystack haystack = new Haystack();
		try {
			Display display = Display.getDefault();
			Shell s = new Shell();
			s.setText("Adenine Console");
			s.setLayout(new FillLayout());
			SWTConsole c = new SWTConsole(s, haystack.m_serviceManager.getRootRDFContainer());
			c.setEnvironmentValue("__identity__", haystack.m_userIdentity);
//			c.setEnvironmentValue("__infosource__", edu.mit.lcs.haystack.ozone.core.Ozone.getInformationSource(haystack.m_serviceManager.getRootRDFContainer(), haystack.m_userIdentity, haystack.m_serviceManager));
			c.setServiceAccessor(haystack.m_serviceManager);
			s.open();
			while (!s.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			} 
			display.dispose();
			System.exit(0);
		} catch (Exception e) {
			new Console(haystack.m_serviceManager.getRootRDFContainer()).run();
			System.exit(0);
		}
	}
	
	private void initializePaths() {
		m_baseSystemPath = new File(SystemProperties.s_basepath);
		if (!m_baseSystemPath.exists()
				&& !m_baseSystemPath.mkdirs()
				&& !m_baseSystemPath.canWrite()) {
			s_logger.error("Couldn't access base directory " + m_baseSystemPath);
			PlatformUI.getWorkbench().close();
			//System.exit(-1);
		}
		if (SystemProperties.s_purgeRDF) {
			s_logger.warn("Purging the entire Haystack database");
			deleteDirectoryContents(m_baseSystemPath);
		}
		m_baseUserPath = new File(SystemProperties.s_userpath);
		if (!m_baseUserPath.exists()
				&& !m_baseUserPath.mkdirs()
				&& !m_baseUserPath.canWrite()) {
			s_logger.error("Couldn't access user directory " + m_baseUserPath);
			throw new RuntimeException();
		}
		
		File precompile = new File(m_baseUserPath, "precompile");
		if (!precompile.exists()
				&& !precompile.mkdirs()
				&& !precompile.canWrite()) {
			s_logger.error("Couldn't access adenine precompile directory " + m_baseUserPath);
			throw new RuntimeException();
		}
		System.setProperty("edu.mit.csail.haystack.precompile", precompile.getAbsolutePath());
		
		s_logger.info("Set up base path:       " + m_baseSystemPath);
		s_logger.info("Set up user path:       " + m_baseUserPath);
		s_logger.info("Set up precompile path: " + precompile);
	}
	
	public static boolean deleteDirectoryContents(File dir) {
		// Do not delete symbolic links
		try { if (!dir.getCanonicalFile().equals(dir.getAbsoluteFile())) return false; }
		catch (IOException e) { return false; }
		boolean success = true;
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) success &= deleteDirectoryContents(file);
				success &= file.delete();
			}
		}
		return success;
	}
	
	private Resource initializeKeys() {
		m_identityFile = new File(m_baseUserPath, "haystack-userid");
		m_keyFile = new File(m_baseUserPath, "haystack-keys");
		boolean rebuildKeys = false;
		
		try {
			if (!m_identityFile.exists()) {
				m_userResource = Utilities.generateUniqueResource();
				BufferedWriter bw =
					new BufferedWriter(
							new OutputStreamWriter(
									new FileOutputStream(m_identityFile)));
				bw.write(m_userResource.getURI());
				bw.close();
				s_logger.info("Created new user id: " + m_userResource);
				rebuildKeys = true;
			} else {
				BufferedReader br =
					new BufferedReader(
							new InputStreamReader(
									new FileInputStream(m_identityFile)));
				m_userResource = new Resource(br.readLine());
				br.close();
				s_logger.info("Found user id: " + m_userResource);
			}
		} catch (IOException e) {
			s_logger.error("Couldn't access user file " + m_identityFile, e);
			throw new RuntimeException();
		}
		
		if (rebuildKeys || !m_keyFile.exists()) {
			// Create a keystore
			s_logger.info("Creating user keys at: " + m_keyFile);
			String[] keyargs =
				new String[] {
					"-genkey",
					"-alias",
					m_userResource.getURI(),
					"-keypass",
					"haystack",
					"-keystore",
					m_keyFile.toString(),
					"-dname",
					"CN=Someone, OU=Haystack, O=Haystack, L=Somewhere, ST=Somewhere, C=US",
					"-storepass",
			"haystack" };
			sun.security.tools.KeyTool.main(keyargs);
		}
		
		return m_userResource;
	}
	
	/**
	 * Creates bootstrap.rdf.
	 */
	private void createBootstrapRDF(File bootstrap) {
		String boot = SystemProperties.s_bootstrapTemplate;
		
		// FIXME: Ugly hack to get it to work with MacOS X property list
		if (boot.startsWith("/")) boot = "file:" + boot;
		
		try {
			URL template = new URL(boot);
			s_logger.info("Trying to make bootstrap RDF at: " + bootstrap);
			s_logger.info("Using template: " + template);
			LocalRDFContainer 	rdfc = new LocalRDFContainer();
			Resource			pakkage = Utilities.generateUniqueResource();
			
			ICompiler compiler = new RDFCodeCompiler(rdfc);
			InputStreamReader inp = new InputStreamReader(CoreLoader.getResourceAsStream("/schemata/adenine.ad"));
			compiler.compile(pakkage, inp, "/schemata/adenine.ad", null, null);
			
			java.util.List errors =
				compiler.compile(pakkage, new InputStreamReader(template.openStream()), null, null, null);
			
			if (errors.isEmpty()) {
				LocalRDFContainer	rdfc2 = new LocalRDFContainer();
				Resource			main = Utilities.getResourceProperty(pakkage, AdenineConstants.main, rdfc);
				if (main != null) {
					Interpreter i = new Interpreter(rdfc);
					DynamicEnvironment denv = new DynamicEnvironment();
					denv.setTarget(rdfc2);
					denv.setIdentity(m_userIdentity);
					i.callMethod(main, new Object[] {}, denv);
				}
				
				Writer w = new BufferedWriter(new FileWriter(bootstrap));
				Utilities.generateAdenine(rdfc2, w);
				w.close();
			} else throw (Exception) errors.get(0);
		} catch (Exception e) {
			s_logger.error("Couldn't create bootstrap file " + bootstrap, e);
			throw new RuntimeException();
		}
	}
}
