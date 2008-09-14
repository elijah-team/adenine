/*
 * Created on Oct 14, 2003
 */
package edu.mit.lcs.haystack.eclipse;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class EclipseConstants {
	
	/**
	 * Prefix for constants used in src/adenine/ui/eclipse.ad.
	 * 
	 * @see WorkspaceSynchronizationAgent#s_namespace
	 */
	public static final String s_namespace = "http://haystack.lcs.mit.edu/ui/eclipse#";
	
	public static final Resource s_startingPointsView   = new Resource(s_namespace + "startingPointsView");
	public static final Resource s_contextsView         = new Resource(s_namespace + "contextsView");
	public static final Resource s_commandView          = new Resource(s_namespace + "commandView");
	public static final Resource s_titleSource          = new Resource(s_namespace + "titleSource");
	public static final Resource s_eclipseEditor        = new Resource(s_namespace + "eclipseEditor");
	public static final Resource s_perspective          = new Resource(s_namespace + "perspective");
	public static final Resource s_viewContainer        = new Resource(s_namespace + "viewContainer");
	public static final Resource s_viewSelectorView      = new Resource(s_namespace + "viewSelectorView");
	public static final Resource s_availableStoresSource = new Resource(s_namespace + "availableStoresSource");

	/**
	 * Prefix for identifiers in plugin.xml.
	 */	
	public static final String s_id = "edu.mit.csail.haystack.eclipse";

	public static final String s_id_FullScreenPerspective = s_id + ".FullScreenPerspective";
	public static final String s_id_FullScreenView        = s_id + ".FullScreenView";
	public static final String s_id_StartingPointsView    = s_id + ".StartingPointsView";
	public static final String s_id_TabbedPerspective     = s_id + ".Perspective";
	public static final String s_id_TaskPaneView          = s_id + ".TaskPaneView";
	public static final String s_id_Editor                = s_id + ".Editor";
	public static final String s_id_BackAction            = s_id + ".BackAction";
	public static final String s_id_ForwardAction         = s_id + ".ForwardAction";
	public static final String s_id_HomeAction            = s_id + ".HomeAction";
	public static final String s_id_RefreshAction         = s_id + ".RefreshAction";
	public static final String s_id_SinglePaneAction      = s_id + ".SinglePaneAction";
	public static final String s_id_DoublePaneAction      = s_id + ".DoublePaneAction";
	public static final String s_id_TriplePaneAction      = s_id + ".TriplePaneAction";
	public static final String s_id_UpdateHaystack        = s_id + ".UpdateHaystack";

}
