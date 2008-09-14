/*
 * Created on Oct 11, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class SourceFactory implements IElementFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public IAdaptable createElement(IMemento memento) {
		String viewInstance = memento.getString("viewInstance");
		return new Source(new Resource(memento.getString("uri")), viewInstance != null ? new Resource(viewInstance) : null);
	}

}
