/*
 * Created on Oct 12, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IBrowserWindow;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartAwareComposite;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.GenericDataProvider;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.standard.modeless.IModelessCreator;
import edu.mit.lcs.haystack.ozone.standard.modeless.IModelessParent;
import edu.mit.lcs.haystack.ozone.standard.modeless.ModelessConstants;
import edu.mit.lcs.haystack.ozone.standard.modeless.ModelessPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * Frame part.
 * @author	Dennis Quan
 * @author	David Huynh
 */
abstract public class EmbeddedPart extends VisualPartBase implements IModelessCreator, IModelessParent, IBrowserWindow, IEclipseSite {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(EmbeddedPart.class);
	
	IVisualPart					m_child;
	VisualPartAwareComposite	m_composite;
	ArrayList					m_modelessChildren = new ArrayList();
	Composite					m_parent;
	Shell						m_shell;
	ShellListener				m_shellListener;
	
	Rectangle					m_rect;
	Rectangle					m_childRectBase;

	public EmbeddedPart(Composite parent, Shell shell) {
		m_parent = parent;
		m_shell = shell;
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		m_context.removeProperty(OzoneConstants.s_browserWindow);
		m_shell.removeShellListener(m_shellListener);
		disposeChildren();
	}
	
	public void internalDispose() {
		disposeChildren();
		
		m_composite.setVisualPart(null);

		m_child.dispose();
		m_composite.dispose();
		
		m_child = null;
		m_composite = null;
		
		super.dispose();
	}

	/**
	 * @see VisualPartBase#onChildResize(ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		m_composite.layout();
		return true;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.modeless.IModelessParent#disposeChildren()
	 */
	public void disposeChildren() {
		while (m_modelessChildren.size() > 0) {
			((ModelessPart) m_modelessChildren.remove(0)).dispose();
		}
	}

	/**
	 * Does the actual initialization work.
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		m_context.putProperty(OzoneConstants.s_browserWindow, this);
		m_context.putProperty(OzoneConstants.s_frame, this);		
		m_context.putProperty(ModelessConstants.MODELESS_CREATOR, this);
		m_context.putProperty(ModelessConstants.MODELESS_PARENT, this);
		m_context.putProperty(OzoneConstants.s_navigationDataProvider, m_dataProvider);

		m_parent.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				if (m_rect == null || !m_rect.equals(m_parent.getClientArea())) {
					m_rect = m_parent.getClientArea();
					onResize(e);
				}
			}
		});

		{
			m_composite = new VisualPartAwareComposite(m_parent, true);
			
			m_composite.addMouseTrackListener(new MouseTrackAdapter() {
				public void mouseEnter(MouseEvent me) {
					handleChildRectBase(me);
				}
				public void mouseHover(MouseEvent me) {
					handleChildRectBase(me);
				}
			});
			m_composite.addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(MouseEvent me) {
					handleChildRectBase(me);
				}
			});

			m_context.setSWTControl(m_composite);
			refreshFrame();	
		}
		
		m_shell.addShellListener(m_shellListener = new ShellAdapter() {
			public void shellActivated(ShellEvent e) { 
				disposeChildren();
			}
		});		
		
		m_composite.setVisible(true);
	}
	
	public boolean isDisposed() {
		return m_parent.isDisposed();
	}
	
	protected void refreshFrame() {
		boolean hasOld = false;
		if (m_child != null) {
			m_child.dispose();
			m_child = null;
			hasOld = true;
		}

		createChild();
			
		m_composite.setVisualPart(m_child);
		
		if (hasOld) {
			m_composite.layout();
			m_composite.redraw();
		}
	}

	/**
	 * @see IModelessCreator#createModelessPart(Resource, Rectangle, boolean, boolean, Context)
	 */
	public ModelessPart createModelessPart(Resource resData, Rectangle rectBase, boolean alignTopOrBottom, boolean discardWhenOutsideBase, Context context) throws Exception {
		Context ctx = new Context(context);
		
		ctx.putLocalProperty(ModelessConstants.BASE_RECT, rectBase);
		ctx.putLocalProperty(ModelessConstants.BASE_RECT_ALIGN, new Boolean(alignTopOrBottom));
		
		if (m_childRectBase != null) {
			disposeChildren();
			m_childRectBase = null;
		}
		
		if (discardWhenOutsideBase) {
			Point p = m_composite.toControl(new Point(rectBase.x, rectBase.y));
			m_childRectBase = new Rectangle(p.x, p.y, rectBase.width, rectBase.height);
		}
		
		return createModelessPart(resData, ctx);
	}

	/**
	 * @see IModelessCreator#createModelessPart(Resource, Point, Context)
	 */
	public ModelessPart createModelessPart(Resource resData, Point pointBase, Context context) throws Exception {
		Context ctx = new Context(context);
		
		ctx.putLocalProperty(ModelessConstants.BASE_POINT, pointBase);
		
		return createModelessPart(resData, ctx);
	}
	
	protected ModelessPart createModelessPart(Resource resData, Context ctx) throws Exception {
		Resource resPart = Ozone.findPart(resData, m_source, m_partDataSource);
		Class c = Utilities.loadClass(resPart, m_source);
		ModelessPart mp = (ModelessPart)c.newInstance();
		
		ctx.putProperty(ModelessConstants.MODELESS_CREATOR, this);
		ctx.putProperty(ModelessConstants.MODELESS_PARENT, this);
		ctx.putProperty(OzoneConstants.s_partData, resData);
		ctx.putProperty(OzoneConstants.s_part, resPart);
		ctx.setSWTControl(m_composite);
		
		mp.initialize(m_source, ctx);
		return mp;
	}

	/**
	 * @see IModelessParent#getShell()
	 */
	public Shell getShell() {
		return null;
	}

	/**
	 * @see IModelessParent#registerModelessChild(ModelessPart)
	 */
	public void registerModelessChild(ModelessPart mp) {
		m_modelessChildren.add(mp);
	}

	/**
	 * @see IModelessParent#unregisterModelessChild(ModelessPart)
	 */
	public void unregisterModelessChild(ModelessPart mp) {
		m_modelessChildren.remove(mp);
	}

	protected void onResize(Event e) {
		Rectangle r = m_parent.getClientArea();
		if (r.width > 0 && r.height > 0 && !r.equals(m_composite.getBounds())) {
			m_composite.setBounds(r);
			
			IBlockGUIHandler bgh = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
			if (bgh != null) {
				bgh.setBounds(r);
			}
		}
	}

	protected void handleChildRectBase(MouseEvent e) {
		if (m_childRectBase != null && !m_childRectBase.contains(e.x, e.y)) {
			disposeChildren();
			m_childRectBase = null;
		}
	}
	
	abstract protected void createChild();
	abstract protected IBrowserWindow onNavigationRequested(Resource res, Resource viewInstance, boolean alreadyNavigated, boolean newWindow);

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#back()
	 */
	public void back() { }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#forward()
	 */
	public void forward() { }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#getCurrentResource()
	 */
	public Resource getCurrentResource() {
		return null;
	}
	
	class NavigationDataProvider extends GenericDataProvider {
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#dispose()
		 */
		public void dispose() {
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
		 */
		public Object getData(Resource dataType, Object specifications)
			throws DataNotAvailableException {
			if (DataConstants.RESOURCE.equals(dataType) || dataType == null) {
				return getCurrentResource();
			}
			return null;
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#handleEvent(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
		 */
		public boolean handleEvent(Resource eventType, Object event) {
			return false;
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
		 */
		public void initialize(IRDFContainer source, Context context) {
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
		 */
		public void initializeFromDeserialization(IRDFContainer source) {
		}
		
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
		 */
		protected void onConsumerAdded(IDataConsumer dataConsumer) {
			Resource res = getCurrentResource();
			if (res != null) {
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, res);
			}
		}
		
		void notifyListeners(Resource res) {
			notifyDataConsumers(DataConstants.RESOURCE_CHANGE, res);
		}
	}
	
	NavigationDataProvider m_dataProvider = new NavigationDataProvider();
	
	protected void onNavigationComplete(Resource newResource) {
		m_dataProvider.notifyListeners(newResource);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#getNavigationDataProvider()
	 */
	public IDataProvider getNavigationDataProvider() {
        return m_dataProvider;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#home()
	 */
	public void home() { }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewNavigator#notifyNavigation(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public void notifyNavigation(Resource underlying) {
		onNavigationRequested(underlying, null, true, false);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) { }
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#navigate(edu.mit.lcs.haystack.rdf.Resource)
	 */
	public IBrowserWindow navigate(Resource res) {
		return navigate(res, null, false);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IBrowserWindow#navigate(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Resource, boolean)
	 */
	public IBrowserWindow navigate(
		Resource res,
		Resource viewInstance,
		boolean newWindow) {
		return onNavigationRequested(res, viewInstance, false, newWindow);
	}
	
	public void saveState() {
		m_child.handleEvent(PartConstants.s_eventSaveState, null);
	}
}
