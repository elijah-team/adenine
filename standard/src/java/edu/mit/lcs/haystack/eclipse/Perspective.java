/*
 * Created on Oct 11, 2003
 */
package edu.mit.lcs.haystack.eclipse;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Defines the Haystack Tabbed perspective.  This perspective has two tabbed folders. 
 * The folder on the left, which occupies 80% of the screen, contains browser windows.
 * The folder on the right contains the StartingPointsView and the TaskPaneView, which 
 * can be selected by clicking their tabs or using <b>Windows &gt; Show View</b>. 
 * <p>
 * This perspective also displays the Haystack action set, which populates the
 * Haystack menu in the Eclipse menu bar and the Haystack group in the Eclipse tool bar.
 * <p>
 * All perspectives and views are declared and named in plugin.xml.
 * 
 * @author Dennis Quan
 * @author Stephen Garland
 */
public class Perspective implements IPerspectiveFactory {
	
	/* 
	 * Defines the initial layout of the Haystack Tabbed perspective.
	 */
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		IFolderLayout fl = layout.createFolder("right", IPageLayout.RIGHT, 0.8f, layout.getEditorArea());
		fl.addView(EclipseConstants.s_id_StartingPointsView);
		fl.addView(EclipseConstants.s_id_TaskPaneView);
		
		layout.addPerspectiveShortcut(EclipseConstants.s_id_FullScreenPerspective);
		layout.addPerspectiveShortcut(EclipseConstants.s_id_TabbedPerspective);
		layout.addShowViewShortcut(EclipseConstants.s_id_StartingPointsView);
		layout.addShowViewShortcut(EclipseConstants.s_id_TaskPaneView);

		layout.addActionSet("edu.mit.csail.haystack.actionSet");
		layout.addActionSet("org.eclipse.ui.help");
	}

}
