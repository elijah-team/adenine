
package edu.mit.lcs.haystack.eclipse;

import java.util.Hashtable;

import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.ozone.core.Context; 
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.INavigationMaster;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewNavigator;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.Resource;

public class FullScreenHostPart extends ViewHostPart implements INavigationMaster {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(FullScreenHostPart.class);
	
	Hashtable m_viewNavigators = new Hashtable();
	
	public FullScreenHostPart(Composite parent, View view, Resource slide) {
		super(parent, view, slide);
		Ozone.s_context.putGlobalProperty(OzoneConstants.s_frame, this);	
	}
	
	/* 
	 * @see edu.mit.lcs.haystack.eclipse.ViewHostPart#setupChildContext(edu.mit.lcs.haystack.ozone.Context)
	 */
	protected void setupChildContext(Context childContext) {
		super.setupChildContext(childContext);
		childContext.getParentContext().putProperty(OzoneConstants.s_navigationMaster, this);
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		m_context.removeProperty(OzoneConstants.s_navigationMaster);
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.INavigationMaster#requestViewing(Resource)
	 */
	public void requestViewing(Resource res) {
		if (res != null && !m_viewNavigators.isEmpty())
			((IViewNavigator) m_viewNavigators.values().toArray()[0]).requestNavigation(res, null, null);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.INavigationMaster#requestViewing(Resource, Resource)
	 */
	public void requestViewing(Resource res, Resource viewInstance) {
		if (res != null && !m_viewNavigators.isEmpty())
			((IViewNavigator)m_viewNavigators.values().toArray()[0]).requestNavigation(res, null, viewInstance);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.INavigationMaster#registerViewNavigator(Resource, IViewNavigator)
	 */
	public Object registerViewNavigator(Resource id, IViewNavigator vn) {
		if (id != null && vn != null) {
			m_viewNavigators.put(id, vn);
			return id;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.INavigationMaster#unregisterViewNavigator(Object)
	 */
	public void unregisterViewNavigator(Object cookie) {
		if (cookie != null && m_viewNavigators.containsKey(cookie))
			m_viewNavigators.remove(cookie);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.INavigationMaster#getViewNavigator(Resource)
	 */
	public IViewNavigator getViewNavigator(Resource id) {
		if (id != null) return (IViewNavigator) m_viewNavigators.get(id);
		if (m_viewNavigators.size() > 0) return (IViewNavigator)m_viewNavigators.values().iterator().next();
dispose();
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.eclipse.ViewHostPart#onNavigationRequested(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, boolean, boolean)
	 */
	protected IBrowserWindow onNavigationRequested(Resource res, Resource viewInstance, 
			boolean alreadyNavigated, boolean newWindow) {
		if (alreadyNavigated) return null;
		// Commented out 2005/12/13 to prevent creation of new tabs in full screen perspective
		//if (newWindow) return super.onNavigationRequested(res, viewInstance, alreadyNavigated, newWindow);
		((IViewNavigator) m_viewNavigators.values().iterator().next()).requestNavigation(res, OzoneConstants.s_InteractiveViewPart, viewInstance);
		return null;
	}
	
	public Composite getComposite() { return m_composite; }
	
	public void renderHTML(HTMLengine he) {
		m_composite.renderHTML(he);
	}
}


