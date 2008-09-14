/*
 * Created on Oct 18, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.IDE;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.content.ContentAndMimeType;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.content.IContentService;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

/**
 * @author Dennis Quan
 */
public class WorkspaceSynchronizationAgent extends GenericService implements IContentService, IResourceChangeListener {
	protected IWorkspace m_workspace;
	protected IRDFContainer m_infoSource;
	
	static WorkspaceSynchronizationAgent s_theAgent;
	
	static public WorkspaceSynchronizationAgent getDefault() { return s_theAgent; }
	
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
		s_mimeTypes.put("rdf", "application/rdf+xml");
		s_mimeTypes.put("bib", "application/x-bibtex");
		s_mimeTypes.put("pdb", "application/x-pdb");
		s_mimeTypes.put("java", "text/plain");
		s_mimeTypes.put("ad", "application/x-adenine");
		s_mimeTypes.put("rss", "text/xml");
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
	
	final static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(WorkspaceSynchronizationAgent.class);
	final static String s_namespace = EclipseConstants.s_namespace;  // "http://haystack.lcs.mit.edu/ui/eclipse#"
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
	
	protected IProject m_myFiles;
	protected IFolder m_unclassifiedFiles;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.service.GenericService#init(java.lang.String, edu.mit.lcs.haystack.server.service.ServiceManager, edu.mit.lcs.haystack.rdf.Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res) throws ServiceException {
		super.init(basePath, manager, res);
		s_logger.info("Initializing ...");
		s_theAgent = this;
		m_workspace = Plugin.getWorkspace();
		m_infoSource = Plugin.getHaystack().getRootRDFContainer();
		
		// Create "My files" project if it does not already exist
		m_myFiles = m_workspace.getRoot().getProject("My files");
		if (!m_myFiles.exists()) {
			try {
				m_myFiles.create(null);
				m_myFiles.open(null);
				s_logger.info("Created project " + m_myFiles);
			} catch (CoreException e) { s_logger.error("Could not create project 'My files'."); }
		}
		m_unclassifiedFiles = m_myFiles.getFolder("Unclassified files");
		if (!m_unclassifiedFiles.exists()) {
			try { 
				m_unclassifiedFiles.create(false, true, null); 
				s_logger.info("Created folder " + m_unclassifiedFiles);
			}
			catch (CoreException e) { s_logger.error("Could not create folder 'Unclassified files'."); }
		}
		
		// Do initial scan
		scanWithProgress(m_workspace.getRoot());
		
		// Set up to listen for changes to the workspace
		m_workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		s_logger.info("Done initializing");
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#allocateContent(java.lang.String)
	 */
	public Resource allocateContent(String suffix) {
		return allocateContentInternal(Utilities.generateUniqueIdentifier() + " - " + suffix, m_unclassifiedFiles);
	}
	
	public Resource allocateContent(String suffix, IFolder folder) {
		return allocateContentInternal(Utilities.generateUniqueIdentifier() + " - " + suffix, folder);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#allocateContent()
	 */
	public Resource allocateContent() {
		return allocateContentInternal(Utilities.generateUniqueIdentifier(), m_unclassifiedFiles);
	}
	
	protected Resource allocateContentInternal(String name, IFolder container) {
		IFile file = container.getFile(name);
		try { file.create(new ByteArrayInputStream(new byte[0]), false, null); }
		catch (CoreException e) { 
			s_logger.error("Content allocation failed for file " + file + " named " + name + " in container " + container);
		}
		return findURI(file, null);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContent(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public ContentAndMimeType getContent(Resource res)  {
		IFile file = getFile(res);
		if (file != null) {
			try {
				return new ContentAndMimeType(file.getContents(), filenameToMimeType(file.getName()));
			} catch (CoreException e) { s_logger.error("Failed to get input stream from file " + file.getName()); }
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContentAsFile(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public File getContentAsFile(Resource res) throws IOException, UnsupportedOperationException {
		IFile file = getFile(res);
		if (file == null) return null;
		return new File(file.getRawLocation().toString());
	}
	
	public IFolder getFolder(Resource res) throws Exception {
		return m_workspace.getRoot().getFolder(getPathFromResource(res));
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#setContent(edu.mit.lcs.haystack.rdf.Resource, java.io.InputStream, java.lang.String)
	 */
	public void setContent(Resource res, InputStream is, String mimeType) throws IOException {
		IFile file = getFile(res);
		if (file == null) s_logger.error("Cannot access file " + file.getName());
		else {
			try { file.setContents(is, false, false, null); }
			catch (CoreException e) {  s_logger.error("Failed to set content of file " + file.getName()); }
		}
	}
	
	protected IFile getFile(Resource res) {
		IPath p = getPathFromResource(res);
		if (p == null) return null;
		IFile f = m_workspace.getRoot().getFile(p);
		return f.exists() ? f : null;
	}
	
	protected Path getPathFromResource(Resource res) {
		String s = Utilities.getLiteralProperty(res, s_underlyingPath, m_infoSource);
		if (s == null) return null;
		return new Path(s);		
	}
	
	class ScanRunnable implements Runnable, IRunnableWithProgress {
		IResource m_res;
		
		ScanRunnable(IResource res) { m_res = res; }
		
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			scan(m_res, monitor, true, null);
			monitor.done();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				//m_plugin.getWorkbench().getActiveWorkbenchWindow().
				new ProgressMonitorDialog(Display.getDefault().getActiveShell()).
				run(true, false, this);
			} catch (InvocationTargetException e) { e.printStackTrace(); }
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
	
	protected void scanWithProgress(IResource res) {
		Display d = Display.getDefault();
		if (d.getThread().equals(Thread.currentThread())) new ScanRunnable(res).run(); 
		else d.asyncExec(new ScanRunnable(res));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResource res = event.getResource();
		switch (event.getType()) {
		case IResourceChangeEvent.POST_CHANGE :
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					public boolean visit(IResourceDelta delta) throws CoreException {
						IResource res = delta.getResource();
						int flags = delta.getFlags();
						switch (delta.getKind()) {
						case IResourceDelta.ADDED :
							if ((flags & IResourceDelta.MOVED_FROM) == 0) {
								scan(res, null, false, res.getParent() == null ? null : findURI(res.getParent(), null));
							} else {
								try { updateFileMetadata(findURI(res, null), res); }
								catch (RDFException e) { s_logger.error("RDF exception: ", e); }
							}
							break;
							
						case IResourceDelta.REMOVED :
							/*System.out.print("Resource ");
							 System.out.print(res.getFullPath());
							 System.out.println(" was removed.");*/
							if ((flags & IResourceDelta.MOVED_TO) != 0) {
								Resource resFile = findURI(res, null);
								if (resFile != null) {
									try {
										m_infoSource.replace(resFile, s_underlyingPath, null, new Literal(delta.getMovedToPath().toString()));
									} catch (RDFException e) { s_logger.error("RDF exception: ", e); }
								}
							}
							break;
							
						case IResourceDelta.CHANGED :
							/*System.out.print("Resource ");
							 System.out.print(res.getFullPath());
							 System.out.println(" has changed.");*/
							break;
						}
						return true; // visit the children
					}
				});
			} catch (CoreException e) { s_logger.error("Exception: ", e); }
			break;
		}
	}
	
	/*protected IMarker getMarker(IResource res) {
	 try {
	 IMarker[] markers = res.findMarkers("edu.mit.lcs.haystack.eclipse.marker", false, IResource.DEPTH_ZERO);
	 if (markers.length > 0) return markers[0];
	 } catch (CoreException e) { e.printStackTrace(); }
	 return null;
	 }*/
	
	protected Resource findURI(IResource res, Resource parent) {
		Resource resFile = null;
		try {
			Literal path = new Literal(res.getFullPath().toString());
			RDFNode[] datum = m_infoSource.queryExtract(new Statement[] {
					new Statement(Utilities.generateWildcardResource(1), s_underlyingPath, path),  
					new Statement(Utilities.generateWildcardResource(1), Constants.s_content_service, getServiceResource())
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
			if (datum == null) {
				resFile = Utilities.generateUniqueResource();
				m_infoSource.add(new Statement(resFile, s_underlyingPath, path));
				m_infoSource.add(new Statement(resFile, Constants.s_content_service, getServiceResource()));
				if (parent != null) m_infoSource.add(new Statement(parent, s_child, resFile));
			} else resFile = (Resource) datum[0];
			
		} catch (RDFException e) { s_logger.error("RDF exception: ", e); }
		return resFile;
	}
	
	protected void scan(IResource res, IProgressMonitor monitor, boolean recurse, Resource parent) {
		String uri;
		if (monitor != null) monitor.beginTask("Scanning " + res.getFullPath(), IProgressMonitor.UNKNOWN);
		try {
			/*if (marker == null) {
			 marker = res.createMarker("edu.mit.lcs.haystack.eclipse.marker");
			 marker.setAttribute("uri", uri = "urn:haystack-eclispe:" + Utilities.generateUniqueIdentifier());
			 } else {
			 uri = (String) marker.getAttribute("uri");
			 }*/
			Resource resFile = findURI(res, parent);
			updateFileMetadata(resFile, res);
			if (res instanceof IContainer) {
				m_infoSource.add(new Statement(resFile, Constants.s_rdf_type, s_Directory));
				if (recurse) {
					IContainer container = (IContainer) res;
					IResource[] members = container.members();
					for (int i = 0; i < members.length; i++) scan(members[i], monitor, true, resFile);
				}
			} else {
				m_infoSource.add(new Statement(resFile, Constants.s_rdf_type, s_File));
				m_infoSource.add(new Statement(resFile, Constants.s_rdf_type, Constants.s_content_ServiceBackedContent));
			}
		} catch (RDFException e) { s_logger.error("RDF exception: ", e); }
		catch (CoreException e) { s_logger.error("Could not access members of container " + res + ": ", e); }
	}
	
	protected void updateFileMetadata(Resource resFile, IResource file) throws RDFException {
		s_logger.info("Updating metadata for " + file);
		//m_infoSource.replace(resFile, s_size, null, new Literal(Long.toString(file.length())));
		m_infoSource.replace(resFile, s_filename, null, new Literal(file.getName()));
		m_infoSource.replace(resFile, s_lastModifiedTime, null, new Literal(new Date(file.getLocalTimeStamp()).toString()));
		//m_infoSource.replace(resFile, s_lastSynchronizedTime, null, new Literal(new Date().toString()));
		if (Utilities.getLiteralProperty(resFile, Constants.s_dc_title, m_infoSource) == null) {
			String name = file.getName();
			if (name.length() == 0) name = file.toString();
			m_infoSource.replace(resFile, Constants.s_dc_title, null, new Literal(name));
		}
		if (Utilities.getLiteralProperty(resFile, Constants.s_dc_format, m_infoSource) == null) {
			String mimeType = filenameToMimeType(file.getName());
			if (mimeType != null) m_infoSource.replace(resFile, Constants.s_dc_format, null, new Literal(mimeType));
		}
	}
	
	public String filenameToMimeType(String filename) {
		int i = filename.lastIndexOf('.');
		if (i == -1) return null;
		String extension = filename.substring(i + 1).toLowerCase();
		String mimeType = (String) s_mimeTypes.get(extension);
		if (mimeType != null) return mimeType;
		else return "application/x-" + extension;
	}
	
	public IEditorPart openEditor(Resource res) throws Exception {
		IFile file = m_workspace.getRoot().getFile(getPathFromResource(res));
		return IDE.openEditor(Plugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
	}
	
	public boolean allocateContent(String requestedPath, Resource resFile) {
		IFile file = m_workspace.getRoot().getFile(new Path(requestedPath));
		try {
			m_infoSource.add(new Statement(resFile, s_underlyingPath, new Literal(requestedPath)));
			m_infoSource.add(new Statement(resFile, Constants.s_content_service, getServiceResource()));
			Resource parent = file.getParent() == null ? null : findURI(file.getParent(), null);
			if (parent != null) m_infoSource.add(new Statement(parent, s_child, resFile));
		} catch (RDFException e) { s_logger.error("RDF exception: ", e); }
		try {
			file.create(new ByteArrayInputStream(new byte[0]), false, null);
			updateFileMetadata(resFile, file);
			return true;
		} catch (RDFException e) { s_logger.error("RDF exception: ", e); }
		catch (CoreException e) { s_logger.error("Exception: ", e); }
		return false;
	}
	
	public IPath getBaseFolderPath() { return m_myFiles.getFullPath(); }
	
	public IContainer getBaseFolder() { return m_myFiles; }
	
	public IWorkspace getWorkspace() { return m_workspace; }
	
	public Resource getBaseFolderResource() { return findURI(getBaseFolder(), null); }
	
	/**
	 * Helper function that attaches a filesystem folder to workspace.
	 */
	public Resource mountFolder(String path) {
		File file = new File(path);
		String baseName = file.getName();
		String name = baseName;
		int i = 1;
		while (m_myFiles.getFolder(name).exists()) name = baseName + (i++);
		IFolder folder = m_myFiles.getFolder(name);
		try { folder.createLink(new Path(path), 0, null); }
		catch (CoreException e) {
			s_logger.error("Exception: ", e);
			return null;
		}
		return findURI(folder, null);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.content.IContentService#getContentSize(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public long getContentSize(Resource res) throws IOException {
		return getContentAsFile(res).length();
	}
	
}
