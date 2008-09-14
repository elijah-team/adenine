/*
 * Created on Oct 11, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import java.util.LinkedList;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.StringDataConsumer;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.ScrollableViewContainerPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlidePart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;

/**
 * Ozone editor host part.
 * @author	Dennis Quan
 */
public class EditorHostPart extends VisualPartBase {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(EditorHostPart.class);
	
	protected HostPart[] m_hostParts;
	protected int m_visibleIndex = 0;
	protected int m_selectedIndex = 0;
	protected int m_sidebarWidth = 160;
	protected SidebarPart m_sidebarPart;
	protected Resource m_editorID = Utilities.generateUniqueResource();
	
	class SidebarPart extends EmbeddedPart {
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#onNavigationRequested(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, boolean, boolean)
		 */
		protected IBrowserWindow onNavigationRequested(
				Resource res,
				Resource viewInstance,
				boolean alreadyNavigated,
				boolean newWindow) {
			return EditorHostPart.this.onNavigationRequested(null, res, viewInstance, alreadyNavigated, newWindow);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#createChild()
		 */
		protected void createChild() {
			Context childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_part, new Resource(SlideConstants.s_namespace + "SlidePart"));
			childContext.putLocalProperty(OzoneConstants.s_partData, EclipseConstants.s_viewSelectorView);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			m_child = new SlidePart();
			m_child.initialize(m_source, childContext);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#isRedirectToNewWindow()
		 */
		public boolean isRedirectToNewWindow() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#close()
		 */
		public void close() {
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#setRedirectToNewWindow(boolean)
		 */
		public void setRedirectToNewWindow(boolean b) {
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.IEclipseSite#getSite()
		 */
		public IWorkbenchPartSite getSite() {
			return m_editor.getSite();
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#refresh()
		 */
		public void refresh() {

		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
		 */
		public void initialize(IRDFContainer source, Context context) {
			m_parent = new Composite(EditorHostPart.this.m_composite, 0);
			super.initialize(source, context);
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#dispose()
		 */
		public void dispose() {
			super.dispose();
			m_parent.dispose();
		}
		
		public SidebarPart() {
			super(null, m_editor.getSite().getWorkbenchWindow().getShell());
		}
		
		Resource m_currentResource;
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#onNavigationComplete(edu.mit.lcs.haystack.rdf.Resource)
		 */
		protected void onNavigationComplete(Resource newResource) {
			m_currentResource = newResource;
			super.onNavigationComplete(newResource);
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#getCurrentResource()
		 */
		public Resource getCurrentResource() {
			return m_currentResource;
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#saveState()
		 */
		public void saveState() {
			super.saveState();
			
			// Also save main pane's state
			getActiveHostPart().saveState();
		}
	}
	
	class HostPart extends EmbeddedPart {
		IDataProvider				m_titleProvider;
		ScrollableViewContainerPart	m_viewContainer;
		Resource					m_currentResource;
		String						m_title;

		class ViewContainerPart extends ScrollableViewContainerPart {
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.IViewContainerPart#onNavigateComplete(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.ozone.IPart)
			 */
			public void onNavigateComplete(Resource resource, IPart childPart) {
				super.onNavigateComplete(resource, childPart);
				HostPart.this.m_currentResource = resource;
				onNavigationRequested(resource, null, true, false);
			}
		}
		
		public HostPart() {
			super(null, m_editor.getSite().getWorkbenchWindow().getShell());
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#createChild()
		 */
		protected void createChild() {
			Context childContext = new Context(m_context);
			
			//childContext.putLocalProperty(OzoneConstants.s_part, new Resource(SlideConstants.s_namespace + "SlidePart"));
			//childContext.putLocalProperty(OzoneConstants.s_partData, EclipseConstants.s_eclipseEditor);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
				
			m_child = new ViewContainerPart();//SlidePart(); //
			m_child.initialize(m_source, childContext);

			m_viewContainer = (ScrollableViewContainerPart) m_child;//m_context.getProperty(EclipseConstants.s_viewContainer);//
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#onNavigationRequested(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, boolean, boolean)
		 */
		protected IBrowserWindow onNavigationRequested(
			Resource res,
			Resource viewInstance,
			boolean alreadyNavigated,
			boolean newWindow) {
			return EditorHostPart.this.onNavigationRequested(this, res, viewInstance, alreadyNavigated, newWindow);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#refresh()
		 */
		public void refresh() {
			m_viewContainer.refresh();
		}
		
		public void generateHTML() {
		// SJG: Need to implement this
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#close()
		 */
		public void close() {
			if (m_hostParts.length == 1) {
				m_editor.getSite().getWorkbenchWindow().getActivePage().closeEditor(m_editor, true);
			}
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#isRedirectToNewWindow()
		 */
		public boolean isRedirectToNewWindow() {
			return m_redirect && (m_hostParts.length == 1);
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#setRedirectToNewWindow(boolean)
		 */
		public void setRedirectToNewWindow(boolean b) {
			m_redirect = b;
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.IEclipseSite#getSite()
		 */
		public IWorkbenchPartSite getSite() {
			return m_editor.getSite();
		}

		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
		 */
		public void initialize(IRDFContainer source, Context context) {
			m_parent = new Composite(EditorHostPart.this.m_composite, 0);
			super.initialize(source, context);

			m_titleProvider = DataUtilities.createDataProvider(EclipseConstants.s_titleSource, m_context, m_partDataSource, m_partDataSource);
			m_titleProvider.registerConsumer(new StringDataConsumer() {
				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.StringDataConsumer#onStringChanged(java.lang.String)
				 */
				protected void onStringChanged(String newString) {
					onTitleChanged(HostPart.this, m_title = newString);
				}

				/* (non-Javadoc)
				 * @see edu.mit.lcs.haystack.ozone.data.StringDataConsumer#onStringDeleted(java.lang.String)
				 */
				protected void onStringDeleted(String previousString) {
				}
			});
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#back()
		 */
		public void back() {
			EditorHostPart.this.back();
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#forward()
		 */
		public void forward() {
			EditorHostPart.this.forward();
		}
		
		public void navigate(Resource res, Resource viewInstance) {
			saveState();
			if (viewInstance != null) {
				s_logger.info("Navigating to " + res + " with view instance " + viewInstance);
				m_viewContainer.navigate(res, viewInstance);
			} else {
				s_logger.info("Navigating to " + res + " with null view instance");
				m_viewContainer.navigate(res);
			}
			m_currentResource = res;
			onNavigationComplete(res);
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#getCurrentResource()
		 */
		public Resource getCurrentResource() {
			return m_currentResource;
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#dispose()
		 */
		public void dispose() {
			super.dispose();
			m_parent.dispose();
			m_titleProvider.dispose();
		}
	}
	
	protected LinkedList	m_backResources = new LinkedList();
	protected LinkedList	m_forwardResources = new LinkedList();
	protected Editor		m_editor;
	protected boolean		m_redirect = false;
	protected Composite		m_parentComposite;
	protected Composite		m_composite;
	protected Rectangle		m_rect;
	protected int			m_headerHeight = 20;
	protected int			m_horzMargin = 3;

	public EditorHostPart(Composite parent, Editor editor) {
		m_parentComposite = parent;
		m_editor = editor;
		
		m_hostParts = new HostPart[] { new HostPart()/*, new HostPart(), new HostPart()*/ };
		m_sidebarPart = new SidebarPart();
	}
	
	protected int findHostPart(HostPart hp) {
		for (int i = 0; i < m_hostParts.length; i++) {
			if (m_hostParts[i] == hp) {
				return i;
			}
		}
		
		return -1;
	}

	protected IBrowserWindow onNavigationRequested(
		HostPart hp,
		Resource res,
		Resource viewInstance,
		boolean alreadyNavigated,
		boolean newWindow) {
		if (newWindow) {
			try {
				return BrowseAction.openEditor(m_editor.getSite().getWorkbenchWindow(), new Source(res)).m_embeddedFrame.m_hostParts[0];
			} catch (PartInitException e) {
				e.printStackTrace();
				return null;
			}		
		} else {
			if (hp == null) {
				hp = m_hostParts[m_selectedIndex % m_hostParts.length];
			}
			
			if (!alreadyNavigated) {
				hp.saveState();
				
				// Find where we are now
				int i = findHostPart(hp);
				int c = (m_visibleIndex % m_hostParts.length);
				if (i == c) {
					// No view containers in front of us; make record of current resource to go back 
					m_backResources.add(hp.m_currentResource);

					if (m_forwardResources.size() > 0) {
						Resource res2 = (Resource) m_forwardResources.removeLast();
						if (!res2.equals(res)) {
							m_forwardResources.clear();
						}
					}
				} else {
					m_forwardResources.clear();
					boolean first = true;
					while (i != c) {
						i = ((i + 1) % m_hostParts.length);
						if (first) {
							first = false;
						} else {
							m_backResources.removeLast();
						}
						--m_visibleIndex;
					}
				}

				// At the frontier; push into the next host part
				m_selectedIndex = ++m_visibleIndex;
				int i2 = m_visibleIndex % m_hostParts.length;
				HostPart hp2 = m_hostParts[i2];
				hp2.navigate(res, viewInstance);
				onResize();
				redrawTitleRegion();
				notifyEditorOfChangedPane();
				m_editor.onNavigationRequested(res, viewInstance, true);
				return hp2;
			} else {
				m_backResources.add(hp.m_currentResource);

				if (m_forwardResources.size() > 0) {
					Resource res2 = (Resource) m_forwardResources.removeLast();
					if (!res2.equals(res)) {
						m_forwardResources.clear();
					}
				}
				m_editor.onNavigationRequested(res, viewInstance, alreadyNavigated);
				notifyEditorOfChangedPane();
				hp.onNavigationComplete(res);
				return hp;
			}
		}
	}
	
	public void navigate(Resource res, Resource viewInstance) {
		m_backResources.clear();
		m_forwardResources.clear();
		m_selectedIndex = m_visibleIndex = 0;
		m_hostParts[0].navigate(res, viewInstance);
		m_sidebarPart.onNavigationComplete(res);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.initialize(source, context);
		
		m_context.putProperty(OzoneConstants.s_browserID, m_editorID);

		m_composite = new Composite(m_parentComposite, 0);
		m_composite.addPaintListener(new PaintListener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
			 */
			public void paintControl(PaintEvent e) {
				onPaint(e.gc);
			}
		});
		m_composite.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				if (m_rect == null || !m_rect.equals(m_composite.getClientArea())) {
					m_rect = m_composite.getClientArea();
					onResize();
				}
			}
		});
		m_composite.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event e) {
				onMouseDown(e.x, e.y);
			}
		});
		
		for (int i = 0; i < m_hostParts.length; i++) {
			m_hostParts[i].initialize(source, new Context(context));
		}
		
		m_sidebarPart.initialize(source, new Context(context));
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.eclipse.EmbeddedPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		
		for (int i = 0; i < m_hostParts.length; i++) {
			m_hostParts[i].dispose();
		}
		
		m_sidebarPart.dispose();
		
		m_composite.dispose();
	}
	
	public void back() {
		if (m_backResources.size() == 0) {
			return;
		}
		
		HostPart hp = m_hostParts[m_visibleIndex % m_hostParts.length];;
		
		if (m_hostParts.length == 1) {
			m_forwardResources.add(hp.m_currentResource);
			Resource res = (Resource) m_backResources.removeLast();
			hp.navigate(res, null);
			notifyEditorOfChangedPane();
		} else if (m_visibleIndex > 0) {
			m_forwardResources.add(hp.m_currentResource);
			m_selectedIndex = --m_visibleIndex;
			
			// May have to tell the recently cleared host part to show the right resource
			if (m_backResources.size() >= m_hostParts.length) {
				hp.navigate((Resource) m_backResources.get(m_backResources.size() - m_hostParts.length), null);
			}
			Resource res = (Resource) m_backResources.removeLast();
			
			onResize();
			redrawTitleRegion();
			notifyEditorOfChangedPane();
		}
	}

	public void forward() {
		if (m_forwardResources.size() == 0) {
			return;
		}
		
		HostPart hp = getActiveHostPart();
		
		if (m_hostParts.length == 1) {
			m_backResources.add(hp.m_currentResource);
			Resource res = (Resource) m_forwardResources.removeLast();
			hp.navigate(res, null);
			notifyEditorOfChangedPane();
		} else {
			onNavigationRequested(hp, (Resource) m_forwardResources.getLast(), null, false, false);
		}
	}

	protected void onResize() {
		Rectangle r = m_composite.getClientArea();
		r.width -= m_sidebarWidth;
		r.x += m_sidebarWidth;
		int visibleHosts = (m_visibleIndex + 1) >= m_hostParts.length ? m_hostParts.length : m_visibleIndex + 1;
		int width = r.width / m_hostParts.length;
		int headerArea = m_hostParts.length > 1 ? 1 + m_headerHeight : 0;
		int horzMargin = m_hostParts.length > 1 ? m_horzMargin : 0;
		int vertMargin = m_hostParts.length > 1 ? 1 : 0;
		int offset = (visibleHosts < m_hostParts.length ? 0 : ((m_visibleIndex + 1) % m_hostParts.length));
		for (int i = 0; i < m_hostParts.length; i++) {
			HostPart hp = m_hostParts[i];
			int x = ((i - offset + m_hostParts.length) % m_hostParts.length) * width + horzMargin + r.x;
			hp.m_parent.setBounds(x, headerArea, width - horzMargin * 2, r.height - headerArea - vertMargin);
			hp.m_parent.setVisible(i < visibleHosts);
		}
		m_sidebarPart.m_parent.setBounds(0, 0, m_sidebarWidth, r.height);
	}
	
	protected void onPaint(GC gc) {
		if (m_hostParts.length == 1) {
			// No headers to paint when there is only one pane
			return;
		}

		// NB: this layout code mirrors onResize()
		Rectangle r = m_composite.getClientArea();
		r.width -= m_sidebarWidth;
		r.x += m_sidebarWidth;
		int visibleHosts = (m_visibleIndex + 1) >= m_hostParts.length ? m_hostParts.length : m_visibleIndex + 1;
		int width = r.width / m_hostParts.length;
		int headerArea = m_headerHeight;
		int horzMargin = m_horzMargin;
		int offset = (visibleHosts < m_hostParts.length ? 0 : ((m_visibleIndex + 1) % m_hostParts.length));
		for (int i = 0; i < m_hostParts.length; i++) {
			HostPart hp = m_hostParts[i];
			int x = ((i - offset + m_hostParts.length) % m_hostParts.length) * width + horzMargin + r.x;
			if (i < visibleHosts && hp.m_title != null) {
				gc.drawString(hp.m_title, x + 3, 3);
				gc.drawRectangle(x - 1, 20, width - horzMargin * 2 + 2, r.height - 21);
			}
			if ((m_selectedIndex % m_hostParts.length) == i) {
				int old = gc.getLineStyle();
				gc.setLineStyle(SWT.LINE_DOT);
				gc.setLineWidth(1);
				gc.drawRectangle(x, 1, width - horzMargin * 2, headerArea - 3);
				gc.setLineStyle(old);
			}
		}
	}
	
	protected void onMouseDown(int x, int y) {
		if (m_hostParts.length == 1) {
			// No headers
			return;
		}

		// NB: this layout code mirrors onResize()
		Rectangle r = m_composite.getClientArea();
		r.width -= m_sidebarWidth;
		int visibleHosts = (m_visibleIndex + 1) >= m_hostParts.length ? m_hostParts.length : m_visibleIndex + 1;
		int width = r.width / m_hostParts.length;
		int headerArea = m_headerHeight;
		int offset = (visibleHosts < m_hostParts.length ? 0 : ((m_visibleIndex + 1) % m_hostParts.length));
		
		int n = (x - m_sidebarWidth) / width;
		if (n >= visibleHosts) {
			return;
		}
		m_selectedIndex = (n + offset) + Math.max(0, m_visibleIndex - (m_visibleIndex % m_hostParts.length) - m_hostParts.length);
		redrawTitleRegion();
		notifyEditorOfChangedPane();
	}
	
	protected void notifyEditorOfChangedPane() {
		HostPart hp = getActiveHostPart();
		Resource res = hp.getCurrentResource();
		m_editor.onNavigationRequested(res, null, true);
		m_sidebarPart.onNavigationComplete(res);
		if (hp.m_title != null) {
			m_editor.onTitleChanged(hp.m_title);
		}
	}
	
	protected void onTitleChanged(HostPart hp, String title) {
		if ((m_selectedIndex % m_hostParts.length) == findHostPart(hp)) {
			m_editor.onTitleChanged(title);
		}
		
		// Invalidate title areas
		redrawTitleRegion();
	}
	
	protected void redrawTitleRegion() {
		if (m_hostParts.length > 1) {
			m_composite.redraw();
		}
	}
	
	public void changeView(Resource viewInstance) {
		int i = m_selectedIndex % m_hostParts.length;
		m_hostParts[i].navigate(m_hostParts[i].getCurrentResource(), viewInstance);
	}
	
	public HostPart getActiveHostPart() {
		return m_hostParts[m_selectedIndex % m_hostParts.length];
	}
	
	public void setPaneCount(int n) {
		HostPart hp = getActiveHostPart();
		HostPart[] x = new HostPart[n];
		x[0] = hp;
		m_visibleIndex = m_selectedIndex = 0;
		for (int i = 1; i < n; i++) {
			x[i] = new HostPart();
			x[i].initialize(m_source, new Context(m_context));
		}
		for (int i = 0; i < m_hostParts.length; i++) {
			if (m_hostParts[i] != hp) {
				m_hostParts[i].dispose();
			}
		}
		m_hostParts = x;
		onResize();
		redrawTitleRegion();
	}
}
