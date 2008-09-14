/*
 * Created on Dec 16, 2003
 */

package edu.mit.lcs.haystack.eclipse;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class TaskPaneView extends View {
	public static final Resource s_taskPaneView = new Resource("http://haystack.lcs.mit.edu/ui/taskPane#taskPaneView");

	public TaskPaneView() {
		super(s_taskPaneView);
	}
}
