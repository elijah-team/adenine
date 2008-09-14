/*
 * Created on Jan 5, 2004
 */
package edu.mit.lcs.haystack.eclipse;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.rdf.IRDFContainer;

/**
 * The Application class provides a <code>run</code> method for running Haystack under Eclipse.
 * This method initializes Haystack, then creates and runs an Eclipse <code>Workbench</code> with 
 * an attached <code>WorkbenchAdvisor</code>.  The <code>Workbench</code> issues callbacks to the
 * methods of the <code>WorkbenchAdvisor</code> to setup the main Eclipse window, then invokes
 * the <code>run</code> method of this class.
 * 
 * @author Dennis Quan
 * @author Stephen Garland
 */
public class Application implements IPlatformRunnable {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Application.class);
	
	/**
	 * Method called by Eclipse to run Haystack as an application plugin.
	 */
	public Object run(Object args) {
		//logDebuggingInfo(args);
		// Start Haystack, extract RDF store
		Haystack hs = Plugin.getHaystack();
		IRDFContainer c = hs.getRootRDFContainer();
		// Create and run workbench
		WorkbenchAdvisor workbenchAdvisor = new WorkbenchAdvisor();
		Display display = Ozone.s_display;
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(display, workbenchAdvisor);
			s_logger.info("Shutting down with return code = " + returnCode);
			return returnCode == PlatformUI.RETURN_RESTART ? IPlatformRunnable.EXIT_RESTART : IPlatformRunnable.EXIT_OK;
		} finally { 
			s_logger.info("Terminating abnormally");
			display.dispose(); 
			}
	}
	
	private void logDebuggingInfo(Object args) {
		s_logger.info("Program arguments");
		String[] argsArray = (String[])args;
		for (int i = 0; i < argsArray.length; i++) s_logger.info("  " + argsArray[i]);
		s_logger.info("System properties");
		SystemProperties.listAll();
	}
	
}