package edu.mit.lcs.haystack.eclipse;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import edu.mit.lcs.haystack.Haystack;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.UnserializableWrapper;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;

public class Editor extends EditorPart implements IReusableEditor {
	protected Source m_source;
	protected EditorHostPart m_embeddedFrame = null;
	protected String m_title = "";
	
	public class Selection implements ISelection {
		Resource m_resource;
		Object m_host;
		
		Selection(Resource res, Object host) {
			m_resource = res;
			m_host = host;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelection#isEmpty()
		 */
		public boolean isEmpty() {
			return m_resource == null;
		}
		
		public Resource getResource() {
			return m_resource;
		}
		
		public Object getHost() {
			return m_host;
		}
	}
	
	protected Selection m_selection = new Selection(null, null);
	
	public Selection getSelection() {
		return m_selection;
	}
	
	protected ISelectionProvider m_selectionProvider = new ISelectionProvider() {
		protected HashSet m_listeners = new HashSet();

		public ISelection getSelection() {
			return m_selection;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			m_listeners.add(listener);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			m_listeners.remove(listener);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
		 */
		public void setSelection(ISelection selection) {
			if (selection instanceof Selection) {
				m_selection = (Selection) selection;
				
				Iterator i = m_listeners.iterator();
				while (i.hasNext()) {
					ISelectionChangedListener l = (ISelectionChangedListener) i.next();
					l.selectionChanged(new SelectionChangedEvent(this, selection));
				}
			}
		}
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorPart#gotoMarker(org.eclipse.core.resources.IMarker)
	 */
	public void gotoMarker(IMarker marker) { }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		setSite(site);
		setInput(input);
		
		getSite().setSelectionProvider(m_selectionProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		if (m_embeddedFrame != null) {
			m_embeddedFrame.getActiveHostPart().saveState();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		m_embeddedFrame = new EditorHostPart(parent, this);
		Context context = new Context(Ozone.s_context);
		
		Haystack hs = Plugin.getHaystack();
		Resource userResource = hs.m_userIdentity.getResource();
	    
		IRDFContainer infoSource = hs.getRootRDFContainer();
		try {
			infoSource = Ozone.getInformationSource(infoSource, hs.m_userIdentity, hs.m_serviceManager);
		} catch (Exception e) {
			//s_logger.error("Failed to get information source", e);
		}

		context.putProperty(OzoneConstants.s_informationSource, new UnserializableWrapper(infoSource));		
	    
		m_embeddedFrame.initialize(hs.getRootRDFContainer(), context);
		setInput(m_source);
		m_selectionProvider.setSelection(new Selection(m_source.m_resource, m_embeddedFrame));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IReusableEditor#setInput(org.eclipse.ui.IEditorInput)
	 */
	public void setInput(IEditorInput newInput) {
		setInput((Source) newInput, false);
	}
	
	protected void setInput(Source source, boolean alreadyNavigated) {
		m_source = source;
		
		if (m_embeddedFrame != null) {
			if (!alreadyNavigated) {
				m_embeddedFrame.navigate(m_source.getResource(), m_source.getViewInstance());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitle()
	 */
	public String getTitle() {
		return m_title;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorPart#getEditorInput()
	 */
	public IEditorInput getEditorInput() {
		return m_source;
	}
	
	public void onNavigationRequested(Resource res, Resource viewInstance, boolean alreadyNavigated) {
		setInput(new Source(res, viewInstance), alreadyNavigated);
		m_selectionProvider.setSelection(new Selection(res, m_embeddedFrame));
	}
	
	void onTitleChanged(String title) {
		synchronized (this) {
			m_title = title;
		}
		Ozone.idleExec(new IdleRunnable() {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		});
	}
}
