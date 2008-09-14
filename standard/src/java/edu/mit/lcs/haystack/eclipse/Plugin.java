package edu.mit.lcs.haystack.eclipse;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleContext;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.UnserializableWrapper;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ListDataConsumer;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartCache;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.SystemProperties;

import java.util.*;

/**
 * The main plugin class for Haystack as an Eclipse application.
 */
public class Plugin extends AbstractUIPlugin implements IStartup {
	
	/**
	 * @see #getDefault()
	 */
	private static Plugin plugin;
	
	/**
	 * @see #getResourceBundle()
	 */
	private ResourceBundle resourceBundle;
	
	/**
	 * @see #getBundleContext()
	 */
	private static BundleContext bundleContext;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Plugin.class);
	
	/**
	 * The constructor.
	 */
	public Plugin() {
		super();
		if (plugin == null) plugin = this;
		try { resourceBundle = ResourceBundle.getBundle(SystemProperties.s_PluginResources); }
		catch (MissingResourceException x) { resourceBundle = null; }
	}
	
	/**
	 * Returns the singleton instance of this Haystack plugin for Eclipse.
	 */
	public static Plugin getDefault() {	return plugin; }
	
	/**
	 * Returns the BundleContext passed to the start method of this Haystack plugin.
	 * Used to retrieve list of all installed bundles.
	 */
	public static BundleContext getBundleContext() { return bundleContext; }
	
	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() { return ResourcesPlugin.getWorkspace(); }
	
	/**
	 * Returns the string corresponding to a given key from this plugin's resource bundle.
	 * Returns the key itself if there is no corresponding string.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= Plugin.getDefault().getResourceBundle();
		try { return (bundle != null ? bundle.getString(key) : key); }
		catch (MissingResourceException e) { return key; }
	}
	
	/**
	 * Returns this plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() { return resourceBundle; }
	
	protected Haystack m_haystack;
	protected boolean m_started = false;
	
	public static final Resource s_taskListDataSource = new Resource("http://haystack.lcs.mit.edu/ui/taskPane#taskListDataSource");
	
	protected IDataProvider m_listProvider;
	
	static public Haystack getHaystack() {
		Plugin plugin = getDefault();
		if (plugin.m_haystack == null) plugin.startHaystack();
		return plugin.m_haystack;
	}
	
	/** 
	 * Starts execution of this plugin.  
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		if (bundleContext == null) bundleContext = context;
	}
	
	/**
	 * Starts running Haystack and Ozone under Eclipse.
	 */
	protected void startHaystack() {
		s_logger.info("Starting haystack plugin ...");
		Display d = Display.getDefault(); 
		
		// There was no counterpart for this code when Haystack was run via Ozone.main()
		if (!d.getThread().equals(Thread.currentThread())) {
			s_logger.info("Calling d.asyncExec");
			d.asyncExec(new Runnable() {
				public void run() { startHaystack(); }
			});
			return;
		} else if (m_haystack != null) return;

		try {   // This must be done in two separate steps!
			m_haystack = new Haystack(false); 
			m_haystack.start();
		} catch (Exception e) { s_logger.error("Failed to start haystack", e); }
		
		s_logger.info("Initializing Ozone ...");
		Ozone.s_source = m_haystack.getRootRDFContainer();
		Ozone.s_display = Display.getCurrent();
		Ozone.s_gc = new GC(Ozone.s_display);
		
		// Disable Ozone part cache to avoid caching bugs
		// FIXME: Eliminate part caching bugs, so we can turn it back on
		PartCache.enableCache(false);
		
		Ozone.s_context.putGlobalProperty(OzoneConstants.s_user, m_haystack.m_userIdentity.getResource());
		Ozone.s_context.putGlobalProperty(OzoneConstants.s_identity, m_haystack.m_userIdentity);
		Ozone.s_context.putGlobalProperty(OzoneConstants.s_serviceAccessor, m_haystack.m_serviceManager);
		Ozone.s_context.putProperty(OzoneConstants.s_priority, new Integer(5));

		IRDFContainer infoSource = null;  // used to be Ozone.s_source
		try {
			infoSource = Ozone.getInformationSource(Ozone.s_source, m_haystack.m_userIdentity, m_haystack.m_serviceManager);
		} catch (Exception e) { s_logger.error("Failed to get information source", e); }
		Ozone.s_context.putGlobalProperty(OzoneConstants.s_informationSource, new UnserializableWrapper(infoSource));		
		
		GraphicsManager.initialize();
		
		// Set up task list watch
		Context listContext = new Context(Ozone.s_context);
		listContext.setInformationSource(Ozone.s_source); // TODO:  Investigate what role this plays
		m_listProvider = DataUtilities.createDataProvider(s_taskListDataSource, listContext, Ozone.s_source, Ozone.s_source);
		try {
			s_logger.info("Now catching exception on m_listProvider");
			m_listProvider.registerConsumer(new ListDataConsumer() {
				protected void onElementsAdded(int index, int count) { showTaskListListener(); }
				protected void onElementsChanged(int index, int count) { showTaskListListener(); }
				protected void onElementsChanged(List changedIndices) { showTaskListListener(); }
				protected void onElementsRemoved(int index, int count, List removedElements) { }
				protected void onListCleared() { }
			});
		} catch (Exception e) {
			s_logger.error(e);
		}
		m_started = true;
	}
	
	public void disposeTaskListListener() { 
		try {
			m_listProvider.dispose();
		} catch (Exception e) {
			s_logger.error(e);
		}
	}
	
	protected void showTaskListListener() {
		if (!m_started) return;
		
		Ozone.idleExec(new IdleRunnable() {
			public void run() {
				try {
					IWorkbenchPart wp = (IWorkbenchPart) getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
					if (!(wp instanceof FullScreenView)) {
						getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(EclipseConstants.s_id_TaskPaneView);
					}
				} catch (PartInitException e) { e.printStackTrace(); }
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	public void earlyStartup() { }
}	
