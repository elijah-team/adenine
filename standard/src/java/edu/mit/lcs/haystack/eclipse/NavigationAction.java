/*
 * Created on Oct 19, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;

import edu.mit.lcs.haystack.Constants;
/**
 * Implementation of an action delegate for a browse action.  
 * <p>
 * The workbench maintains a proxy for this action, but does not load this class 
 * until the user performs a browse action.  Afterwards, the workbench forwards
 * all browse actions through the proxy action to this action delegate, which does 
 * the real work. 
 *
 * @see IWorkbenchWindowActionDelegate
 * @author Dennis Quan
 */
public class NavigationAction implements IWorkbenchWindowActionDelegate {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(NavigationAction.class);

	protected IWorkbenchWindow m_window;
	protected Editor.Selection m_selection;
	
	public void dispose() {	}

	public void init(IWorkbenchWindow window) { m_window = window; }

	public void run(IAction action) {
		if ((m_selection == null) || !(m_selection.m_host instanceof EditorHostPart)) {
			if (action.getId().equals(EclipseConstants.s_id_HomeAction)) {
				try { BrowseAction.openEditor(m_window, new Source(Constants.s_startingPoints)); } 
				catch (PartInitException e) {	}
			}
			return;
		}
		
		EditorHostPart ehp = (EditorHostPart) m_selection.m_host;
		
		String id = action.getId();
		if (id.equals(EclipseConstants.s_id_BackAction))            ehp.back();
		else if (id.equals(EclipseConstants.s_id_ForwardAction))    ehp.forward();
		else if (id.equals(EclipseConstants.s_id_RefreshAction))    ehp.getActiveHostPart().refresh();
		else if (id.equals(EclipseConstants.s_id_HomeAction))       ehp.getActiveHostPart().navigate(Constants.s_startingPoints);
		else if (id.equals(EclipseConstants.s_id_SinglePaneAction)) ehp.setPaneCount(1);
		else if (id.equals(EclipseConstants.s_id_DoublePaneAction)) ehp.setPaneCount(2);
		else if (id.equals(EclipseConstants.s_id_TriplePaneAction)) ehp.setPaneCount(3);
		else s_logger.error("Unknown action");
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof Editor.Selection) m_selection = (Editor.Selection) selection;
	}

}
