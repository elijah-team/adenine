/*
 * Created on Oct 18, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class PopupMenuActionDelegate implements IObjectActionDelegate {
	IWorkbenchPart m_part;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		m_part = targetPart;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if ("edu.mit.csail.haystack.eclipse.browseTo".equals(action.getId())) {
			IResource file = (IResource) ((IStructuredSelection) m_selection).getFirstElement();
			Resource res = WorkspaceSynchronizationAgent.getDefault().findURI(file, null);
			try {
				BrowseAction.openEditor(m_part.getSite().getWorkbenchWindow(), new Source(res));
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
	}
	
	ISelection m_selection;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		m_selection = selection;
		System.out.println(m_selection.getClass());
	}

}
