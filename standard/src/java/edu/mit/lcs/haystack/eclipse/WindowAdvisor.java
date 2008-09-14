package edu.mit.lcs.haystack.eclipse;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * Haystack-specific advisor for configuring a single workbench window for the Eclipse workbench.
 * 
 * @author Stephen Garland
 */
public class WindowAdvisor extends WorkbenchWindowAdvisor {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(WindowAdvisor.class);
	
	public WindowAdvisor(IWorkbenchWindowConfigurer windowConfigurer) { super(windowConfigurer); }
	
	/**
	 * Called before the window's controls are created.  
	 */
	public void preWindowOpen() { 
		super.preWindowOpen(); 
		IWorkbenchWindowConfigurer conf = getWindowConfigurer();
		
		String initialPerspective = getInitialPerspective();
		s_logger.info("Initial perspective = " + initialPerspective);
		
		boolean fullScreen = initialPerspective.equals(EclipseConstants.s_id_FullScreenPerspective);
		conf.setShowCoolBar(!fullScreen);   
		conf.setShowMenuBar(!fullScreen); 
		conf.setShowPerspectiveBar(false);
		conf.setShowStatusLine(!fullScreen);
		conf.setTitle("Haystack");
	}
	
	/**
	 * Returns the initial perspective for the workbench.  If this perspective is not specified as a
	 * system property, it is determined by querying the Haystack RDF store for the eclipse:perspective 
	 * property of the server, which is defined in src/adenine/bootstrap/bootstrapEclipse.ad.  
	 * 
	 * @see SystemProperties#s_initialPerspective
	 */
	public String getInitialPerspective() {
		String initialPerspective = SystemProperties.s_initialPerspective;
		if (initialPerspective == null) {
			Haystack hs = Plugin.getHaystack();
			IRDFContainer c = hs.getRootRDFContainer();
			initialPerspective = Utilities.getLiteralProperty(hs.m_serviceManager.getResource(), EclipseConstants.s_perspective, c);
			//PlatformUI.getPreferenceStore().setDefault(IWorkbenchPreferenceConstants.DEFAULT_PERSPECTIVE_ID, s_initialPerspective);
		}
		return initialPerspective;
	}
	
	/**
	 * Overrides method that displays the Eclipse Welcome page.
	 * <p>
	 * TODO: Provide a Haystack welcome page.  Or try making IWorkbenchPreferences.SHOW_INTRO false.
	 */
	public void openIntro() { }
	
	/**
	 * Called after the window has been opened.  Used to adjust the window.
	 */
	public void postWindowOpen() {
		super.postWindowOpen();
	}
	
}
