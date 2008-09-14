/*
 * Created on August 9, 2005.
 */
package edu.mit.lcs.haystack.eclipse;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import edu.mit.lcs.haystack.SystemProperties;

/**
 * Haystack-specific advisor for configuring the Eclipse workbench.
 * 
 * @author Stephen Garland
 */
public class WorkbenchAdvisor extends org.eclipse.ui.application.WorkbenchAdvisor {
		
	/**
	 * Returns the id of the perspective to use for the initial workbench window.  Required to provide
	 * an implementation of abstract method in the superclass.  
	 */
	public String getInitialWindowPerspectiveId() { 
		return SystemProperties.s_initialPerspective; 
		}
	
	/**
	 * Creates a Haystack-specific advisor for configuring the (single) workbench window 
	 * associated with the Eclipse workbench.
	 * Overrides the default Eclipse 3.1 implementation of this method, which exists to provide
	 * (unused) backwards compatability with Eclipse 3.0.
	 */
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new WindowAdvisor(configurer);
	}
	
}
