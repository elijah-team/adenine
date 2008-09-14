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

/*
 * Created on Jul 9, 2003
 */
package edu.mit.lcs.haystack.server.extensions.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.content.ContentAndMimeType;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.content.IContentService;
import edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * @author Dennis Quan
 */
public class FilesystemAgent
	extends GenericService
	implements IContentService, IRDFListener {
	static HashMap s_mimeTypes = new HashMap();
	static {
		s_mimeTypes.put("gif", "image/gif");
		s_mimeTypes.put("png", "image/png");
		s_mimeTypes.put("bmp", "image/x-bitmap");
		s_mimeTypes.put("pict", "image/pict");
		s_mimeTypes.put("pct", "image/pict");
		s_mimeTypes.put("tiff", "image/tiff");
		s_mimeTypes.put("jpg", "image/jpeg");
		s_mimeTypes.put("jpeg", "image/jpeg");
		s_mimeTypes.put("txt", "text/plain");
		s_mimeTypes.put("xml", "text/xml");
		s_mimeTypes.put("htm", "text/html");
		s_mimeTypes.put("html", "text/html");
		s_mimeTypes.put("shtml", "text/html");
		s_mimeTypes.put("css", "text/css");
		s_mimeTypes.put("rdf", "text/xml");
		s_mimeTypes.put("bib", "application/x-bibtex");
		s_mimeTypes.put("pdb", "application/x-pdb");
		s_mimeTypes.put("java", "text/plain");
		s_mimeTypes.put("ad", "application/x-adenine");
		s_mimeTypes.put("n3", "application/n3");
		s_mimeTypes.put("z", "application/x-compress");
		s_mimeTypes.put("zip", "application/x-zip-compressed");
		s_mimeTypes.put("tar", "application/x-tar");
		s_mimeTypes.put("jar", "application/java-archive");
		s_mimeTypes.put("js", "application/x-javascript");
		s_mimeTypes.put("ps", "application/postscript");
		s_mimeTypes.put("pdf", "application/pdf");
		s_mimeTypes.put("rtf", "application/rtf");
		s_mimeTypes.put("mpg", "application/x-mpeg");
		s_mimeTypes.put("mpeg", "application/x-mpeg");
		s_mimeTypes.put("swf", "application/x-shockwave-flash");
		s_mimeTypes.put("doc", "application/msword");
		s_mimeTypes.put("xls", "application/vnd.ms-excel");
		s_mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
		s_mimeTypes.put("wma", "audio/x-ms-wma");
		s_mimeTypes.put("mp3", "audio/mpeg");
		s_mimeTypes.put("mp2", "audio/mpeg");
		s_mimeTypes.put("mp1", "audio/mpeg");
		s_mimeTypes.put("mp", "audio/mpeg");
		s_mimeTypes.put("mid", "audio/midi");
		s_mimeTypes.put("midi", "audio/midi");		
		s_mimeTypes.put("wav", "audio/x-wav");
		s_mimeTypes.put("vcf", "text/directory");
		s_mimeTypes.put("ics", "text/calendar");
		s_mimeTypes.put("au", "audio/basic");		
	}
	
	//final static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FilesystemAgent.class);
	final static String s_namespace = "http://haystack.lcs.mit.edu/agents/fs#";
	final static Resource s_underlyingPath = new Resource(s_namespace + "underlyingPath");
	final static Resource s_lastModifiedTime = new Resource(s_namespace + "lastModifiedTime");
	final static Resource s_lastSynchronizedTime = new Resource(s_namespace + "lastSynchronizedTime");
	final static Resource s_size = new Resource(s_namespace + "size");
	final static Resource s_filename = new Resource(s_namespace + "filename");
	final static Resource s_mountedPath = new Resource(s_namespace + "mountedPath");
	final static Resource s_mountedDirectory = new Resource(s_namespace + "mountedDirectory");
	final static Resource s_File = new Resource(s_namespace + "File");
	final static Resource s_Directory = new Resource(s_namespace + "Directory");
	final static Resource s_child = new Resource(s_namespace + "child");
	final static Resource s_fsui_genericDirectoryTreeView = new Resource("http://haystack.lcs.mit.edu/ui/fs#genericDirectoryTreeView");
	
	protected IRDFContainer m_source;
	protected Resource m_mountedPathCookie;
	protected HashSet m_mountedPaths = new HashSet();

	protected Resource mount(File file) throws IOException, RDFException {
		HashSet processedFiles = new HashSet();
		ArrayList filesToProcess = new ArrayList();
		filesToProcess.add(new Object[] { null, Collections.singletonList(file) });
		Resource baseRes = null;

		while (true) {
			Iterator i = filesToProcess.iterator();
			if (!i.hasNext()) {
				break;
			}
			
			ArrayList newFilesToProcess = new ArrayList();
			
			while (i.hasNext()) {
				Object[] o = (Object[]) i.next();
				Resource parentFile = (Resource) o[0];
				List files = (List) o[1];
				Iterator j = files.iterator();
				Resource res = null;
				while (j.hasNext()) {
					File f = (File) j.next();
					ArrayList filesToRecurseInto = new ArrayList();
					res = mount(f, processedFiles, parentFile, filesToRecurseInto);
					if (res == null || filesToRecurseInto.isEmpty()) {
						continue;
					}
					newFilesToProcess.add(new Object[] { res, filesToRecurseInto });
				}
				if (parentFile == null) {
					m_source.add(new Statement(getServiceResource(), s_mountedDirectory, baseRes = res));
					m_infoSource.add(new Statement(baseRes, Constants.s_haystack_view, s_fsui_genericDirectoryTreeView));
					m_infoSource.replace(baseRes, Constants.s_dc_title, null, new Literal(file.getAbsolutePath()));
				}					
			} 
			
			filesToProcess = newFilesToProcess;
		}
		
		return baseRes;
	}

	protected Resource mount(File file, Set processedFiles, Resource parentFile, Collection filesToRecurseInto) throws IOException, RDFException {
		file = file.getCanonicalFile();
		if (processedFiles.contains(file) || !file.exists() || file.isHidden()) {
			return null;
		}
		
		Literal path = new Literal(file.toString());
		RDFNode[] datum = m_infoSource.queryExtract(new Statement[] {
			new Statement(Utilities.generateWildcardResource(1), s_underlyingPath, path),  
			new Statement(Utilities.generateWildcardResource(1), Constants.s_content_service, getServiceResource())
		}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
		Resource resFile;
		if (datum == null) {
			resFile = Utilities.generateUniqueResource();
			m_infoSource.add(new Statement(resFile, s_underlyingPath, path));
			m_infoSource.add(new Statement(resFile, Constants.s_content_service, getServiceResource()));
			
			if (parentFile != null) {
				m_infoSource.add(new Statement(parentFile, s_child, resFile));
			}
			
			if (file.isDirectory()) {
				m_infoSource.add(new Statement(resFile, Constants.s_rdf_type, s_Directory));
			} else {
				m_infoSource.add(new Statement(resFile, Constants.s_rdf_type, s_File));
				m_infoSource.add(new Statement(resFile, Constants.s_rdf_type, Constants.s_content_ServiceBackedContent));
			}				
		} else {
			resFile = (Resource) datum[0];
		}

		updateFileMetadata(resFile, file);		
		
		processedFiles.add(file);

		File[] subfiles = file.listFiles();
		if (subfiles != null) {
			for (int i = 0; i < subfiles.length; i++) {
				filesToRecurseInto.add(subfiles[i]);
			}
		}
		
		return resFile;
	}
	
	protected void updateFileMetadata(Resource resFile, File file) throws IOException, RDFException {
		//s_logger.info("Updating " + file);
		
		m_infoSource.replace(resFile, s_size, null, new Literal(Long.toString(file.length())));
		m_infoSource.replace(resFile, s_filename, null, new Literal(file.getName()));
		m_infoSource.replace(resFile, s_lastModifiedTime, null, new Literal(new Date(file.lastModified()).toString()));
		m_infoSource.replace(resFile, s_lastSynchronizedTime, null, new Literal(new Date().toString()));
		
		if (Utilities.getLiteralProperty(resFile, Constants.s_dc_title, m_infoSource) == null) {
			String name = file.getName();
			if (name.length() == 0) {
				name = file.toString();
			}
			m_infoSource.replace(resFile, Constants.s_dc_title, null, new Literal(name));
		}

		if (Utilities.getLiteralProperty(resFile, Constants.s_dc_format, m_infoSource) == null) {
			String filename = file.getName(); 
			int i = filename.lastIndexOf('.');
			if (i != -1) {
				String extension = filename.substring(i + 1).toLowerCase();
				String mimeType = (String) s_mimeTypes.get(extension);
				if (mimeType != null) {
					m_infoSource.replace(resFile, Constants.s_dc_format, null, new Literal(mimeType));
				}
			} 
		}
	}		
	
	protected File getFileFromResource(Resource res) throws Exception {
		return new File(Utilities.getLiteralProperty(res, s_underlyingPath, m_infoSource)).getCanonicalFile();		
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#allocateContent()
	 */
	public Resource allocateContent() {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContent(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public ContentAndMimeType getContent(Resource res) throws IOException {
		try {
			File file = getFileFromResource(res);
			if (file != null) {
				ContentAndMimeType camt = new ContentAndMimeType();
				camt.m_content = new FileInputStream(file);
				camt.m_mimeType = Utilities.getLiteralProperty(res, Constants.s_dc_format, m_infoSource);
				return camt;
			}
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception e) {
			//s_logger.error("Could not get content for " + res, e);
		} 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#setContent(edu.mit.lcs.haystack.rdf.Resource, java.io.InputStream, java.lang.String)
	 */
	public void setContent(Resource res, InputStream is, String mimeType)
		throws IOException {
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.service.GenericService#init(java.lang.String, edu.mit.lcs.haystack.server.service.ServiceManager, edu.mit.lcs.haystack.rdf.Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
		super.init(basePath, manager, res);
		
		m_source = manager.getRootRDFContainer();

		m_mountedPathCookie = Utilities.generateUniqueResource();
		try {
			((IRDFEventSource) m_source).addRDFListener(getServiceResource(), getServiceResource(), s_mountedPath, null, m_mountedPathCookie);
		} catch (Exception e) {
			//s_logger.error("Unable to monitor for added mounted paths", e);
		}
	}

	synchronized protected void remountPaths() {
		RDFNode[] mountedPaths = Utilities.getProperties(getServiceResource(), s_mountedPath, m_source);
		for (int i = 0; i < mountedPaths.length; i++) {
			if (m_mountedPaths.contains(mountedPaths[i].getContent())) {
				continue;
			}
			
			File file = new File(mountedPaths[i].getContent());
			if (file.exists()) {
				try {
					Resource resFile = mount(file);
					if (resFile != null) {
						m_mountedPaths.add(file);
					}
				} catch (Exception e) {
					//s_logger.error("Unable to mount path " + file, e);
				}
				m_mountedPaths.add(mountedPaths[i].getContent());
			}
		}
	}

	public void refresh() {
		Thread t = new Thread() {
			/* (non-Javadoc)
			 * @see java.lang.Thread#run()
			 */
			public void run() {
				remountPaths();
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	} 
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.rdfstore.IRDFListener#statementAdded(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Statement)
	 */
	public void statementAdded(Resource cookie, Statement s) {
		if (m_mountedPathCookie.equals(cookie)) {
			refresh();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.rdfstore.IRDFListener#statementRemoved(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Statement)
	 */
	public void statementRemoved(Resource cookie, Statement s) {
		if (m_mountedPathCookie.equals(cookie)) {
			refresh();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContentAsFile(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public File getContentAsFile(Resource res)
		throws IOException, UnsupportedOperationException {
		try {
			return getFileFromResource(res);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			//s_logger.error("Error getting filename from resource " + res, e);
			throw new UnsupportedOperationException();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#allocateContent(java.lang.String)
	 */
	public Resource allocateContent(String suffix) {	
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContentSize(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public long getContentSize(Resource res) throws IOException {
		return 0;
	}
}
