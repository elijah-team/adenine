package edu.mit.lcs.haystack.eclipse;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.jface.dialogs.InputDialog;

import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Implementation of an action delegate for a browse action.  
 * <p>
 * The workbench maintains a proxy for this action, but does not load this class 
 * until the user performs a browse action.  Afterwards, the workbench forwards
 * all browse actions through the proxy action to this action delegate, which does 
 * the real work. 
 *
 * @see IWorkbenchWindowActionDelegate
 */
public class BrowseAction implements IWorkbenchWindowActionDelegate {
	
	private IWorkbenchWindow m_window;
	
	/**
	 * The constructor.
	 */
	public BrowseAction() { }

	/**
	 * Performs an action delegated by the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		InputDialog id = new InputDialog(m_window.getShell(),
			"Haystack", "Enter the URI to navigate to:", "", null);
		id.open();
		if (id.getReturnCode() == InputDialog.OK) {
			Resource res = new Resource(id.getValue());
			try { openEditor(m_window, new Source(res)); }
			catch (PartInitException e) { HaystackException.uncaught(e); }
		}
	}
	
	public static Editor openEditor(IWorkbenchWindow window, Source src) throws PartInitException {
		return (Editor) window.getActivePage().openEditor(src, EclipseConstants.s_id_Editor);		
	}

	/**
	 * Selection in the workbench has been changed.  We can change the state of the 'real' action 
	 * here if we want, but this can only happen after the delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) { }

	/**
	 * We can use this method to dispose of any system resources we previously allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() { }

	/**
	 * Caches a m_window object to provide a parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.m_window = window;
	}
}