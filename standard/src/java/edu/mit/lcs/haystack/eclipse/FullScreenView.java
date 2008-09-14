/*
 * Created on May 12, 2004
 */

package edu.mit.lcs.haystack.eclipse;

import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 * @author Stephen Garland
 */
public class FullScreenView extends View {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FullScreenView.class);
	
	public FullScreenView() {
		super(Utilities.getResourceProperty(Plugin.getHaystack().m_userIdentity.getResource(), Ozone.s_defaultFrame, Plugin.getHaystack().getRootRDFContainer()));
	}
	
	/**
	 * @see edu.mit.lcs.haystack.eclipse.View#createHost(org.eclipse.swt.widgets.Composite)
	 */
	protected void createHost(Composite parent) {
		m_pane = new FullScreenHostPart(parent, this, m_slide);
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

		IRDFContainer infoSource = hs.getRootRDFContainer();
		Resource resFrame = Utilities.getResourceProperty(userResource, Ozone.s_defaultFrame, infoSource);
		if (resFrame == null) s_logger.error("No default frame was found.");
	    context.putLocalProperty(OzoneConstants.s_partData, resFrame); 

		/*
		// Adds view-specific actions to the view's menu and toolbar.  The UI is less cluttered,
		// however, if we provide access to these actions within Haystack.
		// see also getViewSite().getActionBars().setGlobalActionHandler
		IWorkbenchWindow win = m_pane.getSite().getWorkbenchWindow();
		Action action = new SampleAction(win);
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager dropDownMenu = actionBars.getMenuManager();
		IToolBarManager toolBar = actionBars.getToolBarManager();
		dropDownMenu.add(action);
		toolBar.add(action);
		*/

		Resource r = Constants.s_startingPoints;
		Resource home = Utilities.getResourceProperty(context.getUserResource(), r, infoSource);
		((FullScreenHostPart)m_pane).requestViewing(r);
		

	}
	
	/*
	public class SampleAction extends Action {
		protected IWorkbenchWindow m_window;
		public void run() { 
			IWorkbench workbench = PlatformUI.getWorkbench();
			try { workbench.showPerspective(Application.TABBED_PERSPECTIVE_ID, 
					workbench.getActiveWorkbenchWindow()); }
			catch (WorkbenchException e) { s_logger.error("Failed to switch perspective."); }
		}
		public SampleAction(IWorkbenchWindow window) { 
			m_window = window; 
			setText("Haystack Tabbed Perspective");
			setToolTipText("Click here to switch to a tabbed Haystack/Eclipse perspective");
			}
	}
	*/
	
}

