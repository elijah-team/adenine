/*
 * Created on Oct 12, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 * @author Stephen Garland
 */
public class View extends ViewPart {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(View.class);
	
	protected ViewHostPart m_pane;
	protected Resource m_slide;

	/**
	 * The constructor.
	 */
	public View(Resource slide) { m_slide = slide; }
	
	protected void createHost(Composite parent) {
		m_pane = new ViewHostPart(parent, this, m_slide);
	}

	/**
	 * This callback creates the viewer and initializes it.
	 */
	public void createPartControl(Composite parent) {
		createHost(parent);
		Context context = new Context(Ozone.s_context);
		Haystack hs = Plugin.getHaystack();
		Resource userResource = hs.m_userIdentity.getResource();
		m_pane.initialize(hs.getRootRDFContainer(), context);
		IWorkbenchWindow win = m_pane.getSite().getWorkbenchWindow();
		if (win.getActivePage().getEditorReferences().length == 0) {
			try { BrowseAction.openEditor(win, new Source(Constants.s_startingPoints)); }
			catch (PartInitException e) { s_logger.error("Failed to open editor");  }
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//FIXME: Does this cause the focus bug?
		//viewer.getControl().setFocus();
	}

}
