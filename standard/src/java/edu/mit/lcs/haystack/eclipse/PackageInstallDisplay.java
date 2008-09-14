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
import java.io.IOException;
import java.io.InputStream;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.FederationRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.security.Identity;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class PackageInstallDisplay implements IRunnableWithProgress {
	private Resource m_resServer;
	private Identity m_id;
	private IRDFContainer m_authoringRDFC;
	private PackageFilterRDFContainer m_packageFilterRDFC;
	private IRDFContainer m_source;
	private ICompiler m_compiler;
	private IServiceAccessor m_sa;

	private HashMap	m_uriToInfo;
	private Set	m_packagesToInstall;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PackageInstallDisplay.class);

	public PackageInstallDisplay(Resource resServer, Identity id, IRDFContainer authoringRDFC, Vector packages,
		PackageFilterRDFContainer packageFilterRDFC, IRDFContainer source, ICompiler compiler, IServiceAccessor sa) {
		m_resServer = resServer;
		m_id = id;
		m_authoringRDFC = authoringRDFC;
		m_packageFilterRDFC = packageFilterRDFC;
		m_source = source;
		m_compiler = compiler;
		m_sa = sa;
		m_uriToInfo = new HashMap();
		m_packagesToInstall = new TreeSet(new ExternalPropertyComparator(m_uriToInfo));
		for (Iterator i = packages.iterator(); i.hasNext(); ) {
			Resource r = (Resource)i.next();
			m_uriToInfo.put(r, new PackageInfo(r, m_source));
			m_packagesToInstall.add(r);
		}
	}	
	
	protected void showErrorInMessageBox(String description, Exception e) {
		Display.getDefault().syncExec(new Runnable() {
			String 		m_s;
			Exception 	m_e;
				
			public void run() {
				MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), SWT.OK);
				if (m_e != null) {
					StringWriter sw = new StringWriter();
					m_e.printStackTrace(new PrintWriter(sw));
					mb.setMessage(m_s + ":\n\n" + sw.getBuffer());
				} else mb.setMessage(m_s);
				mb.setText("Haystack Setup");
				mb.open();
			}
			public Runnable init(String s, Exception e) {
				m_s = s;
				m_e = e;
				return this;
			}
		}.init(description, e));
	}

	/*----------------------------------------------------------------*/
	static public IRDFContainer getInformationSource(IRDFContainer source, Resource userResource, 
													 IServiceAccessor sa) throws Exception {
		FederationRDFContainer infoSource = new FederationRDFContainer();
		infoSource.addSource(source, 1);
		
		/*Resource resInfoSource = Utilities.getResourceProperty
			(userResource, Constants.s_config_defaultInformationSource, source);
		if (resInfoSource != null) {
			IRDFContainer rdfc2 = (IRDFContainer)sa.connectToService(resInfoSource, null);
			infoSource.addSource(rdfc2, 0);
		}

		Resource[] resInfoSources = Utilities.getResourceProperties(userResource, Constants.s_config_secondaryInformationSource, source);
		if (resInfoSources.length != 0) {
			for (int i = 0; i < resInfoSources.length; i++) {
				IRDFContainer rdfc2 = (IRDFContainer)sa.connectToService(resInfoSources[i], null);
				infoSource.addSource(rdfc2, 2);
			}
		}*/
		return infoSource;
	}

	/*----------------------------------------------------------------*/
	class PackageInfo {
		public PackageInfo(Resource uri, IRDFContainer source) {
			m_uri = uri;
			m_description = Utilities.getLiteralProperty(uri, Constants.s_dc_description, source);
			if (m_description == null) m_description = m_uri.toString();
		}
		public Resource	m_uri;
		public Resource	m_mainURI;
		public String m_description;
	}
	
	class ExternalPropertyComparator implements Comparator {
		Map m_map;
		
		public ExternalPropertyComparator(Map map) { m_map = map; }
		
		public int compare(Object arg0, Object arg1) {
			PackageInfo info0 = (PackageInfo) m_map.get(arg0);
			PackageInfo info1 = (PackageInfo) m_map.get(arg1);
			if (info0 == null) {
				//FIXME: Fix problem, which is caused by forgetting about the installation of /data/frame and 
				// /data/data in core haystack before installing /programs/addressBook in a separate plugin
				s_logger.error("  No PackageInfo for " + arg0 + " to compare with " + info1.m_description);
				throw new InternalError();
				//return -1;
			}
			return info0.m_description.compareTo(info1.m_description);
		}
	}
	
	final static Resource s_packageInitializer = new Resource(Constants.s_config_namespace + "packageInitializer");


	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask("Installing Haystack Packages", IProgressMonitor.UNKNOWN);  // unknown amount of work

		Interpreter interpreter = null;
		Resource 	resUser = m_id.getResource();
		HashMap		dependencies = new HashMap();
		Set		 	installedPackages = new TreeSet(new ExternalPropertyComparator(m_uriToInfo));

		/*
		 * Un/Installation pass, dependency detection
		 */
		while (m_packagesToInstall.size() > 0) {
			Iterator	pi = m_packagesToInstall.iterator();
			Resource	currentPackage = (Resource) pi.next();
			PackageInfo info = (PackageInfo) m_uriToInfo.get(currentPackage);
	
			pi.remove();
			
			try { compilePackage(monitor, currentPackage, info); } 
			catch (RDFException e) { s_logger.error("Error installing package " + currentPackage, e); }
			catch (Exception e) { s_logger.error("Fatal error installing package " + currentPackage, e); }
			
			installedPackages.add(currentPackage);
			
			Resource[] includes = Utilities.getResourceProperties(currentPackage, Constants.s_config_includes, m_source);
			for (int i = 0; i < includes.length; i++) {
				Resource 	package2 = includes[i];
				PackageInfo	info2 = (PackageInfo) m_uriToInfo.get(package2);
				if (info2 == null) {
					m_uriToInfo.put(package2, new PackageInfo(package2, m_source));
					m_packagesToInstall.add(package2);
				}
			}

			Resource[] dependsOn = Utilities.getResourceProperties(currentPackage, Constants.s_config_dependsOn, m_source);
			if (dependsOn.length > 0) {
				Set dependSet = new HashSet();
				dependencies.put(currentPackage, dependSet);
				for (int i = 0; i < dependsOn.length; i++) dependSet.add(dependsOn[i]);
			}
		}
		
		/*
		 * Run initialization methods (main) according to dependency graph
		 */
//		if (false)
		while (!installedPackages.isEmpty()) {
			Set packagesWithDependencies = new HashSet();
			Set packagesToInitialize = new TreeSet(new ExternalPropertyComparator(m_uriToInfo));
			
			packagesWithDependencies.addAll(dependencies.keySet());
			
			packagesToInitialize.addAll(installedPackages);
			packagesToInitialize.removeAll(packagesWithDependencies);
			
			if (!packagesToInitialize.isEmpty()) {
				Iterator i = packagesToInitialize.iterator();
				while (i.hasNext()) {
					Resource	p = (Resource) i.next();
					PackageInfo	info = (PackageInfo) m_uriToInfo.get(p);
					
					if (info.m_mainURI != null) {
						if (interpreter == null) interpreter = new Interpreter(m_source);
						try {
							DynamicEnvironment denv = new DynamicEnvironment(m_source, m_sa);
							denv.setValue("__infosource__", getInformationSource(m_source, resUser, m_sa));
							denv.setServiceAccessor(m_sa);
							denv.setIdentity(m_id);
							
							monitor.subTask("Initializing package " + info.m_description);
							s_logger.info("Initializing " + info.m_description + "...");
							
							interpreter.callMethod(info.m_mainURI, new Object[] { m_resServer, resUser }, denv);
							
							m_packageFilterRDFC.remove(new Statement(p, s_packageInitializer, info.m_mainURI), new Resource[] {});
						} catch (Exception e) { s_logger.error("Error initializing package " + p, e); }
					}
				}
				
				/*
				 * Remove dependencies
				 */
				i = packagesWithDependencies.iterator();
				while (i.hasNext()) {
					Resource 	p = (Resource) i.next();
					Set 		dependSet = (Set) dependencies.get(p);
					dependSet.removeAll(packagesToInitialize);
					if (dependSet.size() == 0) dependencies.remove(p);
				}
				installedPackages.removeAll(packagesToInitialize);
			} else {
				if (!installedPackages.isEmpty()) {
					String 		s = "";
					Iterator	i = packagesWithDependencies.iterator();
					while (i.hasNext()) {
						Resource p = (Resource) i.next();
						s += "    Package " + p + " depends on\n";
						Set dependSet = (Set) dependencies.get(p);
						Iterator j = dependSet.iterator();
						while (j.hasNext()) {
							Resource p2 = (Resource) j.next();
							if (m_uriToInfo.containsKey(p2)) s += "      " + p2 + "\n";
							else s += "      " + p2 + " (not installed)\n";
						}
					}
					s_logger.error("Package dependency problem detected:\n" + s);
				}
				break;
			}
		}
	}

	private boolean compilePackage(IProgressMonitor monitor, Resource currentPackage, PackageInfo info) throws RDFException {
		boolean 		packageInstallSuccess = true;
		ContentClient 	cc = ContentClient.getContentClient(currentPackage, m_source, m_sa);
		String 			md5 = null;
		
		try {
			InputStream is = cc.getContent();
			
			if (is != null) {
				md5 = SetupAgent.computeMD5(is);
				if (md5 == null) {
					s_logger.error("Failed to compute MD5 of content of package " + currentPackage);
					return false;
				}
			} else {
				s_logger.info("Package " + info.m_description + " has no content.");
				return true;
			}
		} catch (Exception e) {
			s_logger.error("Could not find Setup package " + currentPackage, e);
			return false;
		}

		if (m_authoringRDFC != null) {
			String md5Old = Utilities.getLiteralProperty(currentPackage, Constants.s_haystack_md5, m_authoringRDFC);
			if (md5Old != null) {
				if (!md5.equals(md5Old)) {
					monitor.subTask("Uninstalling old package " + info.m_description + " ...");
					s_logger.info("Uninstalling old package " + info.m_description + " ...");
					Utilities.uninstallPackage(currentPackage, m_authoringRDFC);
				} else {
					s_logger.info("Package " + info.m_description + " is up to date.");
					info.m_mainURI = Utilities.getResourceProperty(currentPackage, s_packageInitializer, m_packageFilterRDFC);
					return true;
				}
			}
		}
			
		s_logger.info("Installing " + info.m_description + " ...");
		monitor.subTask("Installing " + info.m_description + " ...");
			
		m_packageFilterRDFC.changePackage(currentPackage);
			
		// Assert package
		m_source.add(new Statement(currentPackage, Constants.s_rdf_type, Constants.s_haystack_Package));
		m_source.add(new Statement(currentPackage, Constants.s_dc_title, new Literal(info.m_description)));
//		m_source.add(new Statement(currentPackage, Constants.s_haystack_PackageFilename, new Literal(resourcePath)));

		// TODO[dquan]: generalize
		String filename = Utilities.getLiteralProperty(currentPackage, Constants.s_content_path, m_source);
		if (filename == null) {
			s_logger.error("Couldn't find filename for " + currentPackage);
		}
		String extension = filename.substring(filename.lastIndexOf('.') + 1);
		if (extension.equals("rdf") || extension.equals("daml")) {
			try {
				Utilities.parseRDF(cc.getContent(), m_packageFilterRDFC);
			} catch (Exception e) {
				showErrorInMessageBox("An error occurred compiling " + info.m_description, e);
				s_logger.error("An error occurred compiling RDF package " + currentPackage, e);
				packageInstallSuccess = false;
			}
		} else if (extension.equals("n3")) {
			try { Utilities.parseN3(cc.getContent(), m_packageFilterRDFC); }
			catch (Exception e) {
				showErrorInMessageBox("An error occurred compiling " + info.m_description, e);
				s_logger.error("An error occurred compiling Notation3 package " + currentPackage, e);
				packageInstallSuccess = false;
			}
		} else if (extension.equals("ad")) {
			InputStream is = null;
			Reader reader = null;
			try {
				//m_packageFilterRDFC.add(new Statement(currentPackage, Constants.s_rdf_type, Constants.s_content_JavaClasspathContent));
				//m_packageFilterRDFC.add(new Statement(currentPackage, Constants.s_content_path, new Literal(Utilities.getLiteralProperty(currentPackage, Constants.s_content_path, m_packageFilterRDFC))));
				
				List errors = m_compiler.compile(currentPackage, null, filename, m_packageFilterRDFC, m_sa);
				
				if (errors.isEmpty()) {
					Resource main = Utilities.getResourceProperty(currentPackage, AdenineConstants.main, m_packageFilterRDFC);
					if (main != null) {
						m_packageFilterRDFC.add(new Statement(currentPackage, s_packageInitializer, main));
						info.m_mainURI = main; 
					}
				} else {
					StringBuffer buffer = new StringBuffer();

					Iterator j = errors.iterator();
					while (j.hasNext()) {
						Exception e = (Exception) j.next();
						s_logger.error("An error occurred compiling Adenine package " + currentPackage, e);
						buffer.append(e.getMessage());
						buffer.append("\n");
					}

					showErrorInMessageBox("An error occurred installing " + info.m_description + "\r\n" + buffer.toString(), null);
					packageInstallSuccess = false;
				}
			} catch (Exception ae) {
				showErrorInMessageBox("An error occurred compiling Adenine package " + info.m_description, ae);
				s_logger.error("An error occurred compiling Adenine package " + currentPackage, ae);
				packageInstallSuccess = false;
			} finally {
				if (reader != null) {
					try { reader.close(); }
					catch(IOException e) { }
				}
				if (is != null) {
					try { is.close(); }
					catch(IOException e) { }
				}
			}
		} 
		
		if (packageInstallSuccess) 
			m_source.replace(currentPackage, Constants.s_haystack_md5, null, new Literal(md5));
		
		return packageInstallSuccess;
	}
	
}
