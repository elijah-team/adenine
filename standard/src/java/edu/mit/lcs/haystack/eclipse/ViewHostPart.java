/*
 * Created on Oct 12, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlidePart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class ViewHostPart extends EmbeddedPart {
	protected View m_view;
	protected Resource m_slide;
	
	/**
	 * @param parent
	 */
	public ViewHostPart(Composite parent, View view, Resource slide) {
		super(parent, view.getSite().getWorkbenchWindow().getShell());
		m_view = view;
		m_slide = slide;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#onNavigationRequested(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, boolean, boolean)
	 */
	protected IBrowserWindow onNavigationRequested(
		Resource res,
		Resource viewInstance,
		boolean alreadyNavigated,
		boolean newWindow) {
		try {
			return BrowseAction.openEditor(m_view.getViewSite().getWorkbenchWindow(), new Source(res)).m_embeddedFrame.m_hostParts[0];
		} catch (PartInitException e) {
			e.printStackTrace();
			return null;
		}		
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#createChild()
	 */
	protected void createChild() {
		Context childContext = new Context(m_context);

		setupChildContext(childContext);
				
		m_child = new SlidePart();
		m_child.initialize(m_source, childContext);
	}
	
	protected void setupChildContext(Context childContext) {
		childContext.putLocalProperty(OzoneConstants.s_part, new Resource(SlideConstants.s_namespace + "SlidePart"));
		childContext.putLocalProperty(OzoneConstants.s_partData, m_slide);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#isRedirectToNewWindow()
	 */
	public boolean isRedirectToNewWindow() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#close()
	 */
	public void close() {
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#setRedirectToNewWindow(boolean)
	 */
	public void setRedirectToNewWindow(boolean b) {
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.eclipse.IEclipseSite#getSite()
	 */
	public IWorkbenchPartSite getSite() {
		return m_view.getSite();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#refresh()
	 */
	public void refresh() {

	}
	
	public void generateHTML() {
	// SJG: Need to implement this
	}
	
	public IRDFContainer getPartDataSource() {
		return m_partDataSource;
	}
	
	public Context getContext() {
		return m_context;
	}

}
