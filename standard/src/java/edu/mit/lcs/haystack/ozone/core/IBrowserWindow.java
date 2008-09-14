/*
 * Created on Oct 13, 2003
 */
package edu.mit.lcs.haystack.ozone.core;

import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @see edu.mit.lcs.haystack.ozone.core.IViewNavigator
 * @author Dennis Quan
 */
public interface IBrowserWindow {
	public IBrowserWindow navigate(Resource res, Resource viewInstance, boolean newWindow);
	public IBrowserWindow navigate(Resource res);
	public void setRedirectToNewWindow(boolean b);
	public boolean isRedirectToNewWindow();
	public Resource getCurrentResource();  // Also provided by IViewNavigator
	public void close();

	/**
	 * The usual browser-like navigation features.  Also provided by IViewNavigator.
	 */	
	public void refresh();
	public void back();
	public void forward();
	public void home();
	
	/**
	 * Returns a data provider.  Also provided by IViewNavigator.
	 */
	public IDataProvider getNavigationDataProvider();
}
