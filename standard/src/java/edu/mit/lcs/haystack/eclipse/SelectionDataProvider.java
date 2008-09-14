/*
 * Created on Oct 13, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.GenericDataProvider;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class SelectionDataProvider extends GenericDataProvider {
	protected IEclipseSite m_eclipseSite;
	protected Resource m_selection = null;
	protected ISelectionListener m_listener = new ISelectionListener() {
		public void selectionChanged(
			IWorkbenchPart part,
			ISelection selection) {
			if (part instanceof Editor) {
				Editor editor = (Editor) part;
				notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_selection = editor.m_source.m_resource);
			}
		}
	};
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_selection != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_selection);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || DataConstants.RESOURCE.equals(dataType)) {
			return m_selection;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		
		m_eclipseSite = (IEclipseSite) context.getProperty(OzoneConstants.s_browserWindow);
		IWorkbenchPage page = m_eclipseSite.getSite().getPage(); 
		page.addSelectionListener(m_listener);
		m_listener.selectionChanged(page.getActiveEditor(), page.getSelection());
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#dispose()
	 */
	public void dispose() {
		m_eclipseSite.getSite().getPage().removeSelectionListener(m_listener);
	}
	
	static public final Editor.Selection getSelection(Context context) {
		IEclipseSite eclipseSite = (IEclipseSite) context.getProperty(OzoneConstants.s_browserWindow);
		IWorkbenchPage page = eclipseSite.getSite().getPage();
		IEditorPart editor = page.getActiveEditor();
		if (editor instanceof Editor) {
			return ((Editor) editor).getSelection();
		} else {
			return null;
		}
	}
}
