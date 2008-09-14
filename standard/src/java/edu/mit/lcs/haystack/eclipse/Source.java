/*
 * Created on Oct 11, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class Source implements IEditorInput, IPersistableElement {
	protected Resource m_resource;
	protected Resource m_viewInstance = null;
	
	public Source(Resource res) {
		m_resource = res;
	}
	
	public Source(Resource res, Resource viewInstance) {
		m_resource = res;
		m_viewInstance = viewInstance;
	}
	
	public Resource getResource() {
		return m_resource;
	}
	
	public Resource getViewInstance() {
		return m_viewInstance;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return m_resource.getURI();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return this;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return m_resource.getURI();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	public String getFactoryId() {
		return "edu.mit.lcs.haystack.eclipse.SourceFactory";
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		memento.putString("uri", m_resource.getURI());
		if (m_viewInstance != null) {
			memento.putString("viewInstance", m_viewInstance.getURI());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		return arg0 != null && 
			arg0 instanceof Source &&
			((Source) arg0).m_resource.equals(m_resource);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return m_resource.hashCode();
	}
}
