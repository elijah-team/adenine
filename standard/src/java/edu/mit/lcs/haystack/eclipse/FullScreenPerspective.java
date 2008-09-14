/*
 * Created on May 12, 2004
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Defines the Haystack FullScreen perspective.  This perspective has a single folder,
 * which contains the FullScreenView. 
 * <p>
 * All perspectives and views are declared and named in plugin.xml.
 * 
 * @author Dennis Quan
 * @author Stephen Garland
 */
public class FullScreenPerspective implements IPerspectiveFactory {
	
	/* 
	 * Defines the Haystack FullScreen perspective.
	 */
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
		layout.addStandaloneView(EclipseConstants.s_id_FullScreenView, false, IPageLayout.RIGHT, 1f, layout.getEditorArea());
		
		layout.addPerspectiveShortcut(EclipseConstants.s_id_FullScreenPerspective);
		layout.addPerspectiveShortcut(EclipseConstants.s_id_TabbedPerspective);
		layout.addShowViewShortcut(EclipseConstants.s_id_FullScreenView);
	}

}
