/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.mit.lcs.haystack.ozone.standard.layout;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IViewContainerFactory;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.MultipleResourceListTransfer;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.VisualPartAwareComposite;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.DragAndDropHandler;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;

import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.events.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.io.Serializable;
import java.util.*;
import java.util.List;

/**
 * @author David Huynh
 */
public class ListViewLayoutManager extends ListLayoutManagerBase implements IBlockGUIHandler {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ListViewLayoutManager.class);
	
	protected class Element implements Serializable {
		public Resource					m_underlying;
		public IntegerValueDataProvider	m_indexDataProvider;
		public BlockViewContainerPart		m_vcp;
		Context m_childContext;
		
		public boolean				m_navigated = false;
		public boolean				m_resized = false;
	}
	
	ArrayList					m_elements = new ArrayList();	// elements which correspond to resources returned from data provider
	HashMap						m_vcpToIndex = new HashMap();	// from a VCP to an element

	ArrayList					m_vcps = new ArrayList();		// all VCPs, including those not associated with any element
	boolean						m_vcpsSorted = true;
	
	IDataProvider				m_selectionDataProvider;
	IDataProvider				m_focusDataProvider;
	SetDataProviderWrapper		m_selectionDataProviderWrapper;
	
	ResourceDataProviderWrapper	m_focusDataProviderWrapper;
	SetDataConsumer				m_selectionDataConsumer;
	ResourceDataConsumer		m_focusDataConsumer;

	FieldDataProvider			m_fieldDataProvider;
	IVisualPart					m_headersVP;
	Rectangle					m_headersRect;
	
	IViewContainerFactory		m_viewContainerFactory;
	
	transient VisualPartAwareComposite	m_innerComposite;
	transient Composite					m_outerComposite;
	transient ScrollBar					m_scrollbar;

	/*
	 * For initialization only: to set selection and focus data
	 */	
	Resource					m_focusedResource;
	HashSet						m_selectedResources = new HashSet();
	
	/*
	 * For optimization
	 */
	BlockViewContainerPart		m_focusedVCP;
	BlockViewContainerPart		m_anchoredVCP;
	List						m_selectedVCPs = new LinkedList();

	int						m_unnavigatedCount = 0;
	
	IdleRunnable				m_runnable;
	boolean					m_disposing = false;
	boolean					m_triggeringNavigations = false;
	boolean					m_refocus = true;
	
	Rectangle					m_insertRect;
	int						m_lastInsertionPoint;
	
	Rectangle					m_rect = new Rectangle(0, 0, 0, 0);
	
	protected boolean	m_multiSelectDrag = false;
	
	class InternalVisualPart implements IVisualPart, IBlockGUIHandler {
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
		 */
		public void initializeFromDeserialization(
			IRDFContainer source) {
			// TODO[dquan]: Anything to do here?
		
		}
		
		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IVisualPart#getGUIHandler(java.lang.Class)
		 */
		public IGUIHandler getGUIHandler(Class cls) {
			if (cls == null || cls.equals(IBlockGUIHandler.class)) {
				return this;
			}
			return null;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
		 */
		public BlockScreenspace calculateSize(
			int hintedWidth,
			int hintedHeight) {
				
			if (hintedWidth < 0) {
				return null;
			} else if (hintedHeight < 0) {
				return new BlockScreenspace(hintedWidth, 200);
			} else {
				return new BlockScreenspace(hintedWidth, hintedHeight);
			}
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
		 */
		public void draw(GC gc, Rectangle r) {
		}
		
	
		/**
		 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
		 */
		public void renderHTML(HTMLengine he) {
			he.enter("ListViewLayoutManager#InternalVisualPart");
			he.exit("ListViewLayoutManager#InternalVisualPart");
		}
		
		/**
		 * Returns the nearest ancestor of this IVisualPart that responds to
		 * mouse clicks.  Returns null if there is no such ancestor.
		 * 
		 * @see IVisualPart#getClickHandler()
		 */
		public IVisualPart getClickHandler() { 
			Object obj = m_context.getLocalProperty(OzoneConstants.s_parentPart);
			if (!(obj instanceof IVisualPart)) return null;
			IVisualPart parent = (IVisualPart)obj;
			return parent.getClickHandler();
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
		 */
		public BlockScreenspace getFixedSize() {
			return null;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getHintedDimensions()
		 */
		public int getHintedDimensions() {
			return IBlockGUIHandler.WIDTH;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
		 */
		public int getTextAlign() {
			return BlockScreenspace.ALIGN_TEXT_CLEAR;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(org.eclipse.swt.graphics.Rectangle)
		 */
		public void setBounds(Rectangle r) {
			Rectangle r2 = m_outerComposite.getBounds();
			if (!r.equals(r2)) {
				m_outerComposite.setBounds(r);
				layout(r, r2);
			}
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
		 */
		public void dispose() {
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IPart#handleEvent(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
		 */
		public boolean handleEvent(Resource eventType, Object event) {
			if (event instanceof EventObject) {
				return internalHandleGUIEvent(eventType, (EventObject) event);
			}
			return false;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
		 */
		public void initialize(IRDFContainer source, Context context) {
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#handleGUIEvent(edu.mit.lcs.haystack.rdf.Resource, java.util.EventObject)
		 */
		public boolean handleGUIEvent(Resource eventType, EventObject event) {
			return internalHandleGUIEvent(eventType, (EventObject) event);
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
			if (m_outerComposite == null) {
				return;
			}
			m_outerComposite.setVisible(visible);
		}
	}
	
	protected InternalVisualPart	m_internalVP = new InternalVisualPart();
	
	synchronized public void dispose() {
		m_disposing = true;
		
		if (m_runnable != null) {
			m_runnable.expire();
			m_runnable = null;
		}
		
		disposeDataConsumers();
		
		if (m_selectionDataProvider != null) {
			m_selectionDataProvider.unregisterConsumer(m_selectionDataConsumer);
			m_selectionDataConsumer = null;
			m_selectionDataProvider = null;
			m_selectionDataProviderWrapper = null;
		}
		if (m_focusDataProvider != null) {
			m_focusDataProvider.unregisterConsumer(m_focusDataConsumer);
			m_focusDataConsumer = null;
			m_focusDataProvider = null;
			m_focusDataProviderWrapper = null;
		}

		for (int i = 0; i < m_elements.size(); i++) {
			Element element = (Element) m_elements.get(i);
			
			element.m_vcp.dispose();
			element.m_indexDataProvider.dispose();
		}
		
		m_elements.clear();
		m_elements = null;
		m_vcps.clear();
		m_vcps = null;
		m_vcpToIndex.clear();
		m_vcpToIndex = null;
		
		m_selectedResources.clear();
		m_selectedVCPs.clear();
		m_selectedResources = null;
		m_selectedVCPs = null;
		
		m_innerComposite.dispose();
		m_outerComposite.dispose();
		m_scrollbar = null;
		m_innerComposite = null;
		m_outerComposite = null;
		
		if (m_headersVP != null) {
			m_headersVP.dispose();
			m_headersVP = null;
			m_headersRect = null;
		}
		
		super.dispose();

		if (m_fieldDataProvider != null) {
			m_fieldDataProvider.dispose();
			m_fieldDataProvider = null;
		}
	}
	
	protected void internalInitialize() {
		super.internalInitialize();
		
		m_viewContainerFactory = new IViewContainerFactory() {
			public IViewContainerPart createViewContainer() {
				BlockViewContainerPart vcp = new BlockViewContainerPart() {
					public void dispose() {
						if (!m_disposing) {
							removeViewContainer(this);
						}
						super.dispose();
					}
					protected void onNavigationComplete() {
						assertFocusSelection(this, m_currentResource);
					}
					protected boolean onMouseUp(MouseEvent e) {
						return onMouseUpOnViewContainer(this, e);
					}
					public void setBounds(Rectangle r) {
						super.setBounds(r);
						m_vcpsSorted = false;
					}
				};
				
				addViewContainer(vcp);
				
				return vcp;
			}
		};
		m_context.setViewContainerFactory(m_viewContainerFactory);
		
		Resource viewPartClass = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_viewPartClass, m_partDataSource);
		if (viewPartClass != null) {
			m_context.putProperty(OzoneConstants.s_viewPartClass, viewPartClass);
		}
		
		Resource dataSource = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_fields, m_partDataSource);
		if (dataSource != null) {
			Context	childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, dataSource);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
			
			m_fieldDataProvider = new FieldDataProvider();
			m_fieldDataProvider.initialize(m_source, childContext);
			
			m_context.putProperty(LayoutConstants.s_fieldsDataProvider, m_fieldDataProvider);
		}

		m_selectionDataProvider = (IDataProvider) m_context.getLocalProperty(LayoutConstants.s_selection);
		if (m_selectionDataProvider != null) {
			m_selectionDataConsumer = new SetDataConsumer() {
				protected void onItemsAdded(Set items) {
					if (Ozone.isUIThread() && m_initializing) {
						m_selectedResources.addAll(items);
					}
				}
				protected void onItemsRemoved(Set items) {
				}
				protected void onSetCleared() {
				}
			};
			m_selectionDataProvider.registerConsumer(m_selectionDataConsumer);
			m_selectionDataProviderWrapper = new SetDataProviderWrapper(m_selectionDataProvider);
		}

		m_focusDataProvider = (IDataProvider) m_context.getLocalProperty(LayoutConstants.s_focus);
		if (m_focusDataProvider != null) {
			m_focusDataConsumer = new ResourceDataConsumer() {
				protected void onResourceChanged(Resource newResource) {
					m_focusedResource = newResource;
					if (!m_initializing) {
						Ozone.idleExec(new IdleRunnable() {
							/* (non-Javadoc)
							 * @see java.lang.Runnable#run()
							 */
							public void run() {
								Iterator i = m_elements.iterator();
								while (i.hasNext()) {
									Element e = (Element) i.next();
									if (m_focusedResource.equals(e.m_underlying)) {
										focusViewContainer(e.m_vcp, true);
										return;
									}
								}
							}
						});
					}
				}
				protected void onResourceDeleted(Resource previousResource) {
				}
			};
			m_focusDataProvider.registerConsumer(m_focusDataConsumer);
			m_focusDataProviderWrapper = new ResourceDataProviderWrapper(m_focusDataProvider);
		}
		
		m_refocus = Utilities.checkBooleanProperty(m_prescription, LayoutConstants.s_refocus, m_partDataSource);
		
		initializeSWTControlsAndHeaders();
	}
	
	protected void initializeSWTControls() {
		Composite c = (Composite) m_context.getSWTControl();
		
		m_outerComposite = new Composite(c, SWT.V_SCROLL | SWT.CLIP_CHILDREN);
		m_outerComposite.setBackground(SlideUtilities.getAmbientBgcolor(m_context));
		
		new DragAndDropHandler() {
			protected boolean handleDropEvent(
				Resource eventType,
				edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
				
				event.m_dropTargetEvent.detail = event.m_dropTargetEvent.operations;
				if (eventType.equals(PartConstants.s_eventDrop)) {
					return ListViewLayoutManager.this.onDrop(event);
				} else {
					return m_supportsListInsertion || m_supportsSetAddition;
				}
			}

			protected boolean isDroppable() {
				return m_supportsListInsertion || m_supportsSetAddition;
			}
		}.initialize(m_outerComposite);
		
		m_outerComposite.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent me) {
				if (m_context != null) {
					if (me.button == 1) {
						clearAllSelections();
					}
				}
			}
			public void mouseUp(MouseEvent me) {
				if (m_context != null) {
					if (me.button == 3) {
						Point point = m_outerComposite.toDisplay(new Point(me.x, me.y));
						
						PartUtilities.showContextMenu(m_source, m_context, point);
					}
				}
			}
		});

		m_scrollbar = m_outerComposite.getVerticalBar();
		m_scrollbar.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onScroll(e);
			}
		});
		
		m_innerComposite = new VisualPartAwareComposite(m_outerComposite, true) {
			protected void drawContent(GC gc, Rectangle r) {
				ListViewLayoutManager.this.drawContent(gc, r);
			}
		};
		m_innerComposite.setVisualPart(m_internalVP);
	}
	
	
	protected void initializeSWTControlsAndHeaders() {
		initializeSWTControls();
				
		Resource headersPartData = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_headers, m_partDataSource);
		/*if ((headersPartData != null) || Utilities.checkBooleanProperty(m_prescription, LayoutConstants.s_hostPartCache, m_partDataSource)) {
			m_context.putProperty(OzoneConstants.s_partCache, new UnserializableWrapper(new PartCache()));
		}*/
		
		if (headersPartData != null) {
			try {
				Resource resPart = Ozone.findPart(headersPartData, m_source, m_partDataSource);
				Class cl = Utilities.loadClass(resPart, m_source);
				
				m_headersVP = (IVisualPart) cl.newInstance();
				if (m_headersVP != null) {
					Context childContext = new Context(m_context);
				
					childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
					childContext.putLocalProperty(OzoneConstants.s_partData, headersPartData);
					childContext.putLocalProperty(OzoneConstants.s_part, resPart);
					
					m_headersVP.initialize(m_source, childContext);
					m_headersRect = new Rectangle(0, 0, 0, 0);
				}
			} catch (Exception e) {
				s_logger.error("Failed to find initialize field header part " + headersPartData, e);
			}
		}
		
		makeDataConsumers();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		if (event instanceof EventObject) {
			return handleGUIEvent(eventType, (EventObject) event);
		}
		return false;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (hintedWidth == -1) {
			return null;
		}
		
		if (m_headersVP != null) {
			IBlockGUIHandler bgh = (IBlockGUIHandler) m_headersVP.getGUIHandler(IBlockGUIHandler.class);
			
			if (bgh != null) {
				BlockScreenspace bs = null;
				
				switch (bgh.getHintedDimensions()) {
				case IBlockGUIHandler.FIXED_SIZE:
					bs = bgh.getFixedSize();
					break;
				case IBlockGUIHandler.WIDTH:
					bs = bgh.calculateSize(hintedWidth, -1);
					break;
				default:
					bs = bgh.calculateSize(hintedWidth, hintedHeight == -1 ? hintedWidth / 3 : Math.min(hintedHeight / 3, hintedWidth / 3));
				}
				
				if (bs != null) {
					BlockScreenspace bs2 = m_internalVP.calculateSize(
						Math.max(hintedWidth, bs.m_size.x), hintedHeight == -1 ? -1 : Math.max(0, hintedHeight - bs.m_size.y));
					
					if (bs2 != null) {
						bs.m_size.x = Math.max(bs.m_size.x, bs2.m_size.x);
						bs.m_size.y += bs2.m_size.y;
						bs.m_align = BlockScreenspace.ALIGN_TEXT_CLEAR;
						bs.m_alignOffset = 0;
						
						return bs;
					}
				}
			}
		}
		
		return m_internalVP.calculateSize(hintedWidth, hintedHeight);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (!r.equals(m_rect)) {
			setBounds(r);
		}
		
		if (m_headersVP != null) {
			IBlockGUIHandler bgh = (IBlockGUIHandler) m_headersVP.getGUIHandler(IBlockGUIHandler.class);
			
			if (bgh != null) {
				bgh.draw(gc, m_headersRect);
			}
		}

	}

	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("ListViewLayoutManager");
		if (m_headersVP != null) {
		  IBlockGUIHandler bgh = (IBlockGUIHandler) m_headersVP.getGUIHandler(IBlockGUIHandler.class);
		  if (bgh != null) bgh.renderHTML(he);
		}
		for (int i = 0; i < m_elements.size(); i++) {
			Element element = (Element) m_elements.get(i);
			element.m_vcp.renderHTML(he);
		}
		he.exit("ListViewLayoutManager"); 
	}	

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.WIDTH;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	boolean m_layingOut = false;
	public void setBounds(Rectangle r) {
		m_layingOut = true;
		{
			if (m_fieldDataProvider != null && m_outerComposite.getBounds().width != r.width) {
				m_fieldDataProvider.setDimension(r.width);
			}
				
			m_rect.x = r.x; m_rect.y = r.y; m_rect.width = r.width; m_rect.height = r.height;
			if (m_headersVP != null) {
				IBlockGUIHandler bgh = (IBlockGUIHandler) m_headersVP.getGUIHandler(IBlockGUIHandler.class);
				
				if (bgh != null) {
					BlockScreenspace bs = null;
					
					switch (bgh.getHintedDimensions()) {
					case IBlockGUIHandler.FIXED_SIZE:
						bs = bgh.getFixedSize();
						break;
					case IBlockGUIHandler.WIDTH:
						bs = bgh.calculateSize(r.width, -1);
						break;
					default:
						bs = bgh.calculateSize(r.width, Math.min(r.height / 3, r.width / 3));
					}
					
					if (bs != null) {
						m_headersRect.x = r.x;
						m_headersRect.y = r.y;
						m_headersRect.width = Math.min(bs.m_size.x, r.width);
						m_headersRect.height = Math.min(bs.m_size.y, r.height);
	
						bgh.setBounds(m_headersRect);
						
						m_internalVP.setBounds(new Rectangle(
							r.x, r.y + m_headersRect.height, 
							r.width, r.height - m_headersRect.height));
							
						m_layingOut = false;
	
						return;
					}
				}
			}
			
			m_internalVP.setBounds(r);
		}
		m_layingOut = false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#handleGUIEvent(Resource, EventObject)
	 */
	public boolean handleGUIEvent(Resource eventType, EventObject event) {
		if (eventType.equals(PartConstants.s_eventChildResize)) {
			return m_layoutPart.handleEvent(PartConstants.s_eventChildResize, event);
		} else if (m_headersVP != null) {
			processChildResizeEvents();
			return m_headersVP.handleEvent(eventType, event);
		} else if (eventType.equals(PartConstants.s_eventDragEnd)) {
			return onDragEnd((DragSourceEvent) event);
		} else if (eventType.equals(PartConstants.s_eventDragSetData)) {
			return onDragSetData((DragSourceEvent) event);
		}
		return false;
	}
	
	/**
	 * @see IGUIHandler@setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		m_internalVP.setVisible(visible);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ILayoutManager#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		}
		return null;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#prepareContextMenu(MouseEvent)
	 */
	protected void prepareContextMenu(MouseEvent e) {
		removeContextOperations();

		for (int i = 0; i < m_elements.size(); i++) {
			Element 	element = (Element) m_elements.get(i);
			Rectangle	r = element.m_vcp.getBounds();
			
			if (e.y >= r.y && e.y < r.y + r.height) {
				if (element.m_vcp.handleEvent(PartConstants.s_eventContentHittest, new ContentHittestEvent(this, e.x, e.y))) {
					createRemoveContextOperation(element.m_underlying, i);
				}
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#hittest
	 * (int, int, boolean)
	 */
	int m_previousHittestElementIndex;
	protected IVisualPart hittest(int x, int y, boolean favorParent) {
		int margin = favorParent ? 3 : 0;
		
		try {
			Element element = (Element) m_elements.get(m_previousHittestElementIndex);
			if (element != null) {
				Rectangle r = element.m_vcp.getBounds();
				
				if (y >= r.y + margin && y < r.y + r.height - margin) {
					return element.m_vcp;
				}
			}
		} catch (Exception ex) {
		}
		
		int min = 0;
		int max = m_elements.size();
		
		while (min < max) {
			int		mid = (min + max) / 2;
			
			if (mid == min) {
				break;
			}
			
			Element 	element = (Element) m_elements.get(mid);
			Rectangle	r = element.m_vcp.getBounds();
			
			if (y < r.y) {
				max = mid;
			} else {
				min = mid;
			}
		}

		if (min < max) {
			Element 	element = (Element) m_elements.get(min);
			Rectangle	r = element.m_vcp.getBounds();
			
			if (y >= r.y + margin && y < r.y + r.height - margin) {
				m_previousHittestElementIndex = min;
				return element.m_vcp;
			}
		}
		
		m_previousHittestElementIndex = -1;
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onKeyPressed(KeyEvent)
	 */
	protected boolean onKeyPressed(KeyEvent e) {
		if (m_elements.size() == 0) {
			return false;
		}
		
		if (e.character == SWT.DEL) {
			Iterator 	i = m_selectedVCPs.iterator();
			ArrayList	fromFinalDataProvider = new ArrayList();
			HashSet		fromOtherDataProvider = new HashSet();
			
			while (i.hasNext()) {
				BlockViewContainerPart	vcp = (BlockViewContainerPart) i.next();
				int 					index = findElementIndexOfViewContainer(vcp);
				
				if (index != -1) {
					fromFinalDataProvider.add(new Integer(index));
				} else {
					Resource underlying = getUnderlying(vcp);
					if (underlying != null) {
						fromOtherDataProvider.add(underlying);
					}
				}
			}

			if (fromFinalDataProvider.size() > 0) {			
				try {
					m_listDataProviderWrapper.requestRemoval(fromFinalDataProvider);
				} catch (Exception ex) {
					s_logger.error("Failed to request list removal at " + fromFinalDataProvider, ex);
				}
			}
			
			if (fromOtherDataProvider.size() > 0) {
				if (m_groupingDataProvider != null) {
					try {
						m_groupingDataProvider.requestChange(DataConstants.SET_REMOVAL, fromOtherDataProvider);
						fromOtherDataProvider = null;
					} catch (Exception ex) {
						s_logger.error("Failed to request set removal of " + fromOtherDataProvider, ex);
					}
				}

				if (fromOtherDataProvider != null && m_dataProvider != null) {
					try {
						m_dataProvider.requestChange(DataConstants.SET_REMOVAL, fromOtherDataProvider);
					} catch (Exception ex) {
						s_logger.error("Failed to request set removal of " + fromOtherDataProvider, ex);
					}
				}
			}
		} else {
			sortViewContainersByLocation();

			int 		focusedIndex = findIndexOfViewContainer(m_focusedVCP);
			Rectangle	clientArea = m_outerComposite.getClientArea();
			boolean	up = true;
			
			switch (e.keyCode) {
			case SWT.ARROW_DOWN:
				up = false;
				if (focusedIndex >= 0) {
					focusedIndex = focusedIndex + 1;
				}
				break;
			case SWT.ARROW_UP:
				if (focusedIndex >= 0) {
					focusedIndex = focusedIndex - 1;
				}
				break;
			case SWT.PAGE_DOWN:
				up = false;
				if (focusedIndex >= 0) {
					int focusedOffset = m_focusedVCP.getBounds().y;
					while (focusedIndex < m_elements.size() - 1) {
						BlockViewContainerPart vcp = (BlockViewContainerPart) m_vcps.get(focusedIndex);
						
						if (vcp.getBounds().y - focusedOffset > clientArea.height) {
							if (vcp != m_focusedVCP) {
								focusedIndex--;
							}
							break;
						}
						
						focusedIndex++;
					}
				}
				break;
			case SWT.PAGE_UP:
				if (focusedIndex >= 0) {
					int focusedOffset = m_focusedVCP.getBounds().y;
					while (focusedIndex >= 0) {
						BlockViewContainerPart vcp = (BlockViewContainerPart) m_vcps.get(focusedIndex);
						
						if (vcp.getBounds().y - focusedOffset > clientArea.height) {
							if (vcp != m_focusedVCP) {
								focusedIndex++;
							}
							break;
						}
						
						focusedIndex--;
					}
				}
				break;
			case SWT.HOME:
				focusedIndex = 0;
				break;
			case SWT.END:
				focusedIndex = m_vcps.size() - 1;
				break;
			default:
				return false;
			}
			
			focusedIndex = Math.min(focusedIndex, m_vcps.size() - 1);
			focusedIndex = Math.max(focusedIndex, 0);
			
			BlockViewContainerPart newFocusedVCP = (BlockViewContainerPart) m_vcps.get(focusedIndex);
			
			if (newFocusedVCP != m_focusedVCP) {
				if ((e.stateMask & SWT.SHIFT) != 0) {
					if (m_anchoredVCP == null){
						m_anchoredVCP = (BlockViewContainerPart) m_vcps.get(0);
					}
					
					clearAllSelections();
					
					addSelections(m_anchoredVCP, newFocusedVCP);
				} else if ((e.stateMask & SWT.CTRL) == 0) {
					clearAllSelections();
					
					addSelections(newFocusedVCP, newFocusedVCP);
					
					m_anchoredVCP = newFocusedVCP;
				}
				
				focusViewContainer(newFocusedVCP, true);
			}
			
			ensureViewContainerVisible(m_focusedVCP, up);
		}

		m_selectedResources.clear();
		//dispatchNavigationRunnable();
		triggerNavigations();
		
		return true;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragExit(DropTargetEvent)
	 */
	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertRect != null) {
			m_innerComposite.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			m_insertRect = null;
		}
		return true;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragHover(DropTargetEvent)
	 */
	protected boolean onDragHover(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		Rectangle r = calcDropRect(event.m_x, event.m_y);
		
		if (r == null || !r.equals(m_insertRect)) {
			if (m_insertRect != null) {
				m_innerComposite.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			}
			if (r != null) {
				m_innerComposite.redraw(r.x, r.y, r.width, r.height, true);
			}
			m_insertRect = r;
		}
		return super.onDragHover(event);
	}
	protected Rectangle calcDropRect(int x, int y) {
		if (m_supportsListInsertion) {
			int min = 0;
			int max = m_elements.size();
	
			while (min < max) {
				int		mid = (min + max) / 2;
	
				if (mid == min) {
					break;
				}
	
				Element 	element = (Element) m_elements.get(mid);
				Rectangle	r = element.m_vcp.getBounds();
	
				if (y < r.y) {
					max = mid;
				} else {
					min = mid;
				}
			}
	
			if (min < max) {
				Element 	element = (Element) m_elements.get(min);
				Rectangle	r = element.m_vcp.getBounds();
	
				if (y < r.y + r.height / 2) {
					m_lastInsertionPoint = min;
					return new Rectangle(r.x + 2, r.y - 2, r.width - 5, 4);
				} else {
					m_lastInsertionPoint = min + 1;
					return new Rectangle(r.x + 2, r.y + r.height - 2, r.width - 5, 4);
				}
			}
		}
		
		m_lastInsertionPoint = -1;
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onChildHasHandledDragAndDropEvent(Resource, DropTargetEvent)
	 */
	protected void onChildHasHandledDragAndDropEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertRect != null) {
			m_innerComposite.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			m_insertRect = null;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDropResourceList(List, DropTargetEvent)
	 */
	protected boolean onDropResourceList(List resourceList, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		unhighlightContent();
		try {
			if (m_lastInsertionPoint != -1 && m_supportsListInsertion) {
				m_listDataProviderWrapper.requestAddition(resourceList.get(0), m_lastInsertionPoint);
			} else if (m_supportsSetAddition) {
				m_listDataProviderWrapper.getDataProvider().requestChange(DataConstants.SET_ADDITION, resourceList.get(0));
			}
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch on drop", e);
		} catch (DataNotAvailableException e) {
			s_logger.error("Data not available on drop", e);
		}
		return false;
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDropResources(java.util.List, DropTargetEvent)
	 */
	protected boolean onDropResources(List resources, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		unhighlightContent();
		try {
			if (m_lastInsertionPoint != -1 && m_supportsListInsertion) {
				m_listDataProviderWrapper.requestAddition(resources, m_lastInsertionPoint);
			} else if (m_supportsSetAddition) {
				HashSet set = new HashSet();

				set.addAll(resources);

				m_listDataProviderWrapper.getDataProvider().requestChange(DataConstants.SET_ADDITION, set);
			}
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch on drop", e);
		} catch (DataNotAvailableException e) {
			s_logger.error("Data not available on drop", e);
		}
		return false;
	}

	protected void unhighlightContent() {
		if (m_insertRect != null) {
			m_innerComposite.redraw(m_insertRect.x, m_insertRect.y, m_insertRect.width, m_insertRect.height, true);
			m_insertRect = null;
		}
	}
	
	protected boolean onMouseUpOnViewContainer(BlockViewContainerPart vcp, MouseEvent e) {
		if ((e.stateMask & SWT.SHIFT) != 0) {
			if ((e.stateMask & SWT.CTRL) == 0) {
				clearAllSelections();
			}
			
			sortViewContainersByLocation();
			
			addSelections(vcp, m_focusedVCP == null ? 
				(BlockViewContainerPart) m_vcps.get(0) : m_focusedVCP);
		} else if ((e.stateMask & SWT.CTRL) != 0) {
			toggleSelection(vcp);
		} else {
			if (vcp != m_focusedVCP || 
				m_selectedVCPs.size() != 1 || 
				((BlockViewContainerPart) m_selectedVCPs.get(0)) != vcp) {
				
				clearAllSelections();
				addSelections(vcp, vcp);
			}
		}
		focusViewContainer(vcp, true);
		m_anchoredVCP = vcp;
		return true;
	}

	protected void processChildResizeEvents() {
		if (m_childrenToResize.size() == 0 || m_triggeringNavigations) {
			return;
		}
		
		Rectangle innerBounds = m_innerComposite.getBounds();
		
		/*
		 * min and max keep track of range of elements whose sizes have changed.
		 */
		int min = m_elements.size();
		int max = -1;
		
		for (int i = 0; i < m_childrenToResize.size(); i++) {
			BlockViewContainerPart vcp = (BlockViewContainerPart) m_childrenToResize.get(i);
			
			Integer index = (Integer) m_vcpToIndex.get(vcp);
			if (index == null) {
				continue;
			}
			
			Element element = (Element) m_elements.get(index.intValue());
			if (element.m_vcp != vcp) {
				s_logger.error("Error during processing child resize events", new Exception());
				continue;
			}
			
			min = Math.min(min, index.intValue());
			max = Math.max(max, index.intValue());
			
			element.m_vcp.update();
			element.m_resized = true;
		}
		
		m_childrenToResize.clear();
		if (m_childResizeRunnable != null) {
			m_childResizeRunnable.expire();
			m_childResizeRunnable = null;
		}
		
		if (min > max) {
			return;
		}
		
		/*
		 * Set bounds to range of elements whose sizes have changed.
		 */
		int shift = 0; // pixels shifted
		{
			Element element = (Element) m_elements.get(min);
			
			BlockScreenspace 	blockScreenspace = calculateElementSize(element.m_vcp, innerBounds.width);
			Rectangle 			r = element.m_vcp.getBounds();
			
			if (blockScreenspace != null) {
				shift += (blockScreenspace.m_size.y - r.height);
				
				r.height = blockScreenspace.m_size.y;
				
				element.m_vcp.setBounds(r);
				redrawInnerComposite(r);
			}
			element.m_resized = false;
			
			int accumulatedOffset = r.y + r.height;
			
			for (int i = min + 1; i <= max; i++) {
				element = (Element) m_elements.get(i);
				r = element.m_vcp.getBounds();
				
				if (element.m_resized || r.y != accumulatedOffset) {
					r.y = accumulatedOffset;

					if (element.m_resized) {
						blockScreenspace = calculateElementSize(element.m_vcp, innerBounds.width);
						if (blockScreenspace != null) {
							shift += (blockScreenspace.m_size.y - r.height);
							
							r.height = blockScreenspace.m_size.y;
						}

						element.m_resized = false;
					}
					
					element.m_vcp.setBounds(r);
					redrawInnerComposite(r);
				}
				
				accumulatedOffset += r.height;
			}
		}
				
		/*
		 * Shift later content and set bounds to any shifted elements.
		 */
		if (shift != 0) {
			if (max < m_elements.size() - 1) {
				Element element = (Element) m_elements.get(max + 1);
				
				shiftContent(element.m_vcp.getBounds().y, shift);
			} else {
				shiftContent(innerBounds.height, shift);
			}

			for (int i = max + 1; i < m_elements.size(); i++) {
				Element 	element = (Element) m_elements.get(i);
				Rectangle	r = element.m_vcp.getBounds();
				
				r.y += shift;
				element.m_vcp.setBounds(r);
			}
		}
	}

	protected boolean onChildResize(ChildPartEvent event) {
		if (!m_layingOut) {
			return super.onChildResize(event);
		}
		return true;
	}

	static int s_defaultHeight = 16; // pixels
	protected void processElementsAdded(int index, List addedElements) {
		int width = m_innerComposite.getBounds().width;
		int addedCount = addedElements.size();
		
		int offset = 0;
		if (index < m_elements.size()) {
			Element elmt = (Element) m_elements.get(index);
			offset = elmt.m_vcp.getBounds().y;
		} else if (m_elements.size() > 0) {
			Element 	elmt = (Element) m_elements.get(m_elements.size() - 1);
			Rectangle	r = elmt.m_vcp.getBounds();
			offset = r.y + r.height;
		}
		
		/*
		 * Shift later elements down.
		 */
		{
			int shift = addedCount * s_defaultHeight;
			for (int i = index; i < m_elements.size(); i++) {
				Element 	element = (Element) m_elements.get(i);
				Rectangle	r = element.m_vcp.getBounds();
				
				r.y += shift;
				
				element.m_vcp.setBounds(r);
				element.m_indexDataProvider.setValue(i + addedCount + 1);
				
				m_vcpToIndex.put(element.m_vcp, new Integer(i + addedCount));
			}
			shiftContent(offset, shift); // shift the bitmap down
		}
		
		for (int i = 0; i < addedElements.size(); i++) {
			Resource	resource = (Resource) addedElements.get(i);
			Element		element = new Element();
			
			element.m_underlying = resource;
			
			element.m_indexDataProvider = new IntegerValueDataProvider(0);
			element.m_indexDataProvider.initialize(m_source, new Context(m_context));
			element.m_indexDataProvider.setValue(index + i + 1);
			
			element.m_vcp = createViewContainer(element);
			element.m_vcp.setBounds(new Rectangle(0, offset + i * s_defaultHeight, width, s_defaultHeight));
			
			m_elements.add(index + i, element);
			
			m_vcpToIndex.put(element.m_vcp, new Integer(index + i));
			
			addViewContainer(element.m_vcp);
		}
		
		m_unnavigatedCount += addedCount;

		//triggerNavigations();
		dispatchNavigationRunnable();
	}
	
	protected void processElementsRemoved(int index, List removedElements) {
		if (m_elements.size() == 0) {
			return;
		}
		
		int width = m_innerComposite.getBounds().width;
		int count = removedElements.size();
		
		int offset = 0;
		int shift = 0;
		{
			Element elmt = (Element) m_elements.get(index);
			offset = elmt.m_vcp.getBounds().y;
		}
		
		/*
		 * Remove the elements.
		 */
		for (int i = 0; i < count; i++) {
			Element elmt = (Element) m_elements.get(index);
			
			shift -= elmt.m_vcp.getBounds().height;
			
			if (!elmt.m_navigated) {
				m_unnavigatedCount--;
			}
			removeViewContainer(elmt.m_vcp);
			
			elmt.m_vcp.dispose();
			elmt.m_indexDataProvider.dispose();
			
			m_elements.remove(index);
			m_vcpToIndex.remove(elmt.m_vcp);
		}

		/*
		 * Shift later elements up.
		 */
		{
			for (int i = index; i < m_elements.size(); i++) {
				Element 	element = (Element) m_elements.get(i);
				Rectangle	r = element.m_vcp.getBounds();
				
				r.y += shift;
				
				element.m_vcp.setBounds(r);
				element.m_indexDataProvider.setValue(i + 1);
				
				m_vcpToIndex.put(element.m_vcp, new Integer(i));
			}
			shiftContent(offset, shift); // shift the bitmap up
		}

		//dispatchNavigationRunnable();
		triggerNavigations();
	}
	protected void processElementsChanged(int index, List changedElements) {
	}
	
	protected void processListCleared() {
		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			Element elmt = (Element) i.next();
			
			removeViewContainer(elmt.m_vcp);
			
			elmt.m_vcp.dispose();
			elmt.m_indexDataProvider.dispose();
		}
		
		m_elements.clear();
		m_vcpToIndex.clear();
		
		m_unnavigatedCount = 0;
		
		redraw();
	}

	
	protected void shiftContent(int offset, int shift) {
		if (shift == 0) {
			return;
		}
		
		Rectangle bounds = m_innerComposite.getBounds();
		if (offset < (-bounds.y)) {
			bounds.y -= shift;
		}
		
		if (shift > 0) {
			m_innerComposite.setBounds(bounds.x, bounds.y, bounds.width, bounds.height + shift);
		}
		
		GC gc = new GC(m_innerComposite);
		gc.copyArea(0, offset, bounds.width, bounds.height - offset, 0, offset + shift);
		gc.dispose();

		if (shift < 0) {
			m_innerComposite.setBounds(bounds.x, bounds.y, bounds.width, bounds.height + shift);
		}
		
		bounds.height += shift;
		
		Rectangle clientArea = m_outerComposite.getClientArea();
		if (bounds.height < clientArea.height) {
			m_innerComposite.setLocation(0, 0);
		} else if (bounds.height + bounds.y < clientArea.height) {
			m_innerComposite.setLocation(0, clientArea.height - bounds.height);
		}
		
		setScrollbars();
	}
	
	protected void setScrollbars() {
		Rectangle inner = m_innerComposite.getBounds();
		Rectangle outer = m_outerComposite.getClientArea();
		
		if (inner.height <= outer.height) {
			m_scrollbar.setEnabled(false);
		} else {
			m_scrollbar.setEnabled(true);
			
			m_scrollbar.setMinimum(0);
			m_scrollbar.setMaximum(inner.height);
			
			m_scrollbar.setIncrement(outer.height / 8);
			m_scrollbar.setPageIncrement(outer.height);
			m_scrollbar.setThumb(outer.height);
			m_scrollbar.setSelection(-inner.y);
		}
	}

	private void onScroll(SelectionEvent e) {
		m_innerComposite.setLocation(0, -m_scrollbar.getSelection());
		dispatchNavigationRunnable();
	}
	
	protected BlockScreenspace calculateElementSize(BlockViewContainerPart vcp, int width) {
		switch (vcp.getHintedDimensions()) {
			case IBlockGUIHandler.FIXED_SIZE:
				return vcp.getFixedSize();
			case IBlockGUIHandler.WIDTH:
				return vcp.calculateSize(width, -1);
			default:
				return vcp.calculateSize(width, 20); // TODO[dfhuynh]: what do we do?
		}
	}
	
	protected BlockViewContainerPart createViewContainer(Element element) {
		BlockViewContainerPart vcp = new BlockViewContainerPart() {
			public void dispose() {
				if (!m_disposing) {
					removeViewContainer(this);
				}
				super.dispose();
			}
			protected void onNavigationComplete() {
				assertFocusSelection(this, m_currentResource);
				update();
			}
			protected boolean onMouseUp(MouseEvent e) {
				return onMouseUpOnViewContainer(this, e);
			}
			public void setBounds(Rectangle r) {
				super.setBounds(r);
				m_vcpsSorted = false;
			}
		};
		vcp.setNestingRelation(m_nestingRelation);
		
		element.m_childContext = new Context(m_context);

		element.m_childContext.setSWTControl(m_innerComposite);
		element.m_childContext.putProperty(LayoutConstants.s_itemIndex, element.m_indexDataProvider);
		element.m_childContext.putLocalProperty(OzoneConstants.s_parentPart, m_internalVP);
		
		vcp.initialize(m_source, element.m_childContext);
		
		return vcp;
	}
	
	protected void dispatchNavigationRunnable() {
		if (m_runnable == null) {
			Ozone.idleExec(new IdleRunnable(m_context) {
				public void run() {
					m_runnable = null;
					triggerNavigations();
				}
			});
		}
	}
	
	static int s_navigateBatch = 10;
	protected void triggerNavigations() {
		if (m_unnavigatedCount == 0) {
			return;
		}
		
		m_triggeringNavigations = true;
		
		boolean triggerAgain = false;
		
		int i;
		int startIndex;
		int endIndex;
			
		int navigateCount = 0;
		int maxNavigate = Math.min(s_navigateBatch, m_unnavigatedCount);
		
		sortViewContainersByLocation();
		
		Rectangle	innerBounds = m_innerComposite.getBounds();
		Rectangle	visibleArea = m_outerComposite.getClientArea();
		
		visibleArea.y = -innerBounds.y;
		
		/*
		 * Navigate visible items and nearby items.
		 */
		{
			int	startY = Math.max(0, visibleArea.y - visibleArea.height / 2);
			int	endY = Math.min(innerBounds.height, startY + visibleArea.height * 3 /2);
			int[]	a = findStartEndElementIndices(startY, endY);
			
			startIndex = a[0];
			endIndex = Math.min(m_elements.size() - 1, startIndex + Math.max(maxNavigate, a[1] - startIndex));
			
			for (i = startIndex; i <= endIndex; i++) {
				Element element = (Element) m_elements.get(i);
				
				if (!element.m_navigated) {
					element.m_vcp.navigate(element.m_underlying);
					element.m_navigated = true;
					navigateCount++;
				}
			}
			
			if (i <= endIndex) {
				triggerAgain = true;
			}
		}

		/*
		 * Navigate around the focused item.
		 */
		long x = System.currentTimeMillis();
		if (m_focusedVCP != null) {
			Rectangle	r = m_focusedVCP.getBounds();
			int		startY = Math.max(0, r.y - visibleArea.height / 2);
			int		endY = Math.min(innerBounds.height, startY + 2 * visibleArea.height);
			int[]		a = findStartEndElementIndices(startY, endY);
			
			startIndex = a[0];
			endIndex = Math.min(m_elements.size() - 1, startIndex + Math.max(maxNavigate, a[1] - startIndex));
			
			for (i = startIndex; i <= endIndex; i++) {
				Element element = (Element) m_elements.get(i);
				
				if (!element.m_navigated) {
					element.m_vcp.navigate(element.m_underlying);
					element.m_navigated = true;
					navigateCount++;
				}
			}

			if (i <= endIndex) {
				triggerAgain = true;
			}
		}
		
		m_unnavigatedCount -= navigateCount;
		if (m_unnavigatedCount > 0 && triggerAgain) {
			dispatchNavigationRunnable();
		}
		
		m_triggeringNavigations = false;
		
		processChildResizeEvents();
	}
	
	protected int[] findStartEndElementIndices(int startY, int endY) {
		int[] result = new int[2];
		
		int lo = 0;
		int hi = m_elements.size() - 1;
		while (lo < hi - 1) {
			int mid = (lo + hi) / 2;
			
			Element element = (Element) m_elements.get(mid);
			if (element.m_vcp.getBounds().y < startY) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		
		result[0] = lo;
		
		hi = m_elements.size() - 1;
		while (lo < hi - 1) {
			int mid = (lo + hi) / 2;
			
			Element element = (Element) m_elements.get(mid);
			if (element.m_vcp.getBounds().y > endY) {
				hi = mid;
			} else {
				lo = mid;
			}
		}
		
		result[1] = hi;
		
		return result;
	}
	
	protected void layout(Rectangle bounds, Rectangle oldBounds) {
		Rectangle clientArea = m_outerComposite.getClientArea();
		
		/*
		 * Needs to re-layout everything
		 */
		if (oldBounds.width != bounds.width) {
			int accumulatedOffset = 0;
			
			for (int i = 0; i < m_elements.size(); i++) {
				Element 			element = (Element) m_elements.get(i);
				BlockScreenspace	bs = calculateElementSize(element.m_vcp, clientArea.width);
				int				height = bs != null ? bs.m_size.y : s_defaultHeight;
				Rectangle			r = new Rectangle(0, accumulatedOffset, clientArea.width, height);
				
				element.m_vcp.setBounds(r);
				
				accumulatedOffset += height;
			}
						
			Rectangle innerBounds = m_innerComposite.getBounds();
			
			innerBounds.height = accumulatedOffset;
			innerBounds.width = clientArea.width;
			
			if (innerBounds.height < clientArea.height) {
				innerBounds.y = 0;
			} else if (innerBounds.height + innerBounds.y < clientArea.height) {
				innerBounds.y = clientArea.height - innerBounds.height;
			}
			
			if (!m_innerComposite.getBounds().equals(innerBounds)) {
				m_innerComposite.setBounds(innerBounds);
				m_innerComposite.redraw();
			}
		} else {
			Rectangle	innerBounds = m_innerComposite.getBounds();
			Point		location = m_innerComposite.getLocation();
			
			if (innerBounds.height < clientArea.height) {
				location.y = 0;
			} else if (innerBounds.height + innerBounds.y < clientArea.height) {
				location.y = clientArea.height - innerBounds.height;
			}

			if (!m_innerComposite.getLocation().equals(location)) {
				m_innerComposite.setLocation(location);
			}
		}

		setScrollbars();
		
		dispatchNavigationRunnable();
	}
	
	protected void drawContent(GC gc, Rectangle r) {
		Region region = new Region();
		gc.getClipping(region);
		
		Rectangle clipping = gc.getClipping();
		
		Rectangle visibleArea = m_outerComposite.getClientArea();
		visibleArea.y = -m_innerComposite.getLocation().y;
		
		Rectangle drawingArea = clipping.intersection(visibleArea);
		
		int startY = drawingArea.y;
		int endY = drawingArea.y + drawingArea.height;
		
		int lo = 0;
		int hi = m_elements.size() - 1;
		while (lo < hi - 1) {
			int mid = (lo + hi) / 2;
			
			if (mid == lo) {
				break;
			}
			
			Element element = (Element) m_elements.get(mid);
			if (element.m_vcp.getBounds().y < startY) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		
		while (lo < m_elements.size()) {
			Element element = (Element) m_elements.get(lo);
			if (element.m_vcp.getBounds().y > endY) {
				break;
			}
			
			Rectangle r2 = element.m_vcp.getBounds();
			if (region.intersects(r2)) {
				element.m_vcp.draw(gc, r2);
			}
			lo++;
		}

		if (m_insertRect != null) {
			boolean	xor = gc.getXORMode();
			int		lineStyle = gc.getLineStyle();
			int		lineWidth = gc.getLineWidth();
			Color		color = gc.getForeground();

			gc.setXORMode(true);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.setLineWidth(1);
			gc.setForeground(SlideUtilities.getAmbientColor(m_context));

			gc.drawRectangle(m_insertRect.x, m_insertRect.y, m_insertRect.width - 1, m_insertRect.height - 1);

			gc.setXORMode(xor);
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			gc.setForeground(color);
		}
		
		region.dispose();
	}
	
	protected void clearAllSelections() {
		int width = m_innerComposite.getBounds().width;
		
		Iterator i = m_selectedVCPs.iterator();
		while (i.hasNext()) {
			BlockViewContainerPart vcp = (BlockViewContainerPart) i.next();

			vcp.setSelected(false);
			redrawInnerComposite(vcp.getBounds());
			
			i.remove();
		}
		
		if (m_selectionDataProviderWrapper != null) {
			try {
				m_selectionDataProviderWrapper.requestClear();
			} catch (DataMismatchException e) {
				s_logger.error(e);
			}
		}
	}
	
	protected Resource getUnderlying(BlockViewContainerPart vcp) {
		Resource underlying = vcp == null ? null : vcp.getCurrentResource();
		if (vcp != null && underlying == null) {
			Integer i = (Integer) m_vcpToIndex.get(vcp);
			
			if (i != null) {
				underlying = ((Element) m_elements.get(i.intValue())).m_underlying;
			}
		}
		return underlying;
	}
	
	protected void addSelections(BlockViewContainerPart vcp1, BlockViewContainerPart vcp2) {
		int start = findIndexOfViewContainer(vcp1);
		int end = vcp1 == vcp2 ? start : findIndexOfViewContainer(vcp2);

		if (start > end) {
			int temp = start; start = end; end = temp;
		}
		
		if (start < 0 || end >= m_vcps.size()) {
			return;
		}
		
		HashSet	resources = new HashSet();
		
		for (int j = start; j <= end; j++) {
			BlockViewContainerPart vcp = (BlockViewContainerPart) m_vcps.get(j);
			
			if (!vcp.getSelected()) {
				Resource underlying = getUnderlying(vcp);
				if (underlying != null) {
					resources.add(underlying);
				}
				
				vcp.setSelected(true);
				m_selectedVCPs.add(vcp);
				
				redrawInnerComposite(vcp.getBounds());
			}
		}
		
		if (m_selectionDataProviderWrapper != null) {
			try {
				m_selectionDataProviderWrapper.requestAddition(resources);
			} catch (DataMismatchException e) {
				s_logger.error(e);
			}
		}
	}
	
	protected void toggleSelection(BlockViewContainerPart vcp) {
		if (vcp == null) {
			return;
		}
		
		Resource underlying = getUnderlying(vcp);

		if (vcp.getSelected()) {
			vcp.setSelected(false);
			m_selectedVCPs.remove(vcp);

			if (m_selectionDataProviderWrapper != null && underlying != null) {
				try {
					m_selectionDataProviderWrapper.requestRemoval(underlying);
				} catch (DataMismatchException e) {
					s_logger.error(e);
				}
			}
		} else {
			vcp.setSelected(true);
			m_selectedVCPs.add(vcp);

			if (m_selectionDataProviderWrapper != null && underlying != null) {
				try {
					m_selectionDataProviderWrapper.requestAddition(underlying);
				} catch (DataMismatchException e) {
					s_logger.error(e);
				}
			}
		}
		redrawInnerComposite(vcp.getBounds());
	}
	
	transient protected TimerTask			m_exclusiveCreateTask = null;

	protected void asyncUpdateFocusDataProvider() {
		if (m_focusDataProviderWrapper != null) {
			Ozone.idleExec(new IdleRunnable(m_context) {
				/* (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					updateFocusDataProvider();
				}
	
			});
		}
	}
	
	protected void updateFocusDataProvider() {
		if (m_focusDataProviderWrapper != null) {
			Resource underlying;
			if (m_focusedVCP != null && (underlying = getUnderlying(m_focusedVCP)) != null) {
				try {
					m_focusDataProviderWrapper.requestResourceSet(underlying);
				} catch (DataMismatchException e) {
					s_logger.error(e);
				}
			} else {
				try {
					m_focusDataProviderWrapper.requestResourceDeletion();
				} catch (DataMismatchException e) {
					s_logger.error(e);
				}
			}
		}
	}

	protected void delayedUpdateFocusDataProvider() {
		if (m_focusDataProviderWrapper != null) {
			try {
				m_exclusiveCreateTask.cancel();
			} catch (Exception e) {
			}
	
			Ozone.s_timer.schedule(m_exclusiveCreateTask = new TimerTask() {
				/**
				 * @see java.util.TimerTask#run()
				 */
				public void run() {
					asyncUpdateFocusDataProvider();
				}
			}, 400);
		}
	}
	
	protected void focusViewContainer(BlockViewContainerPart vcp, boolean repaintOldFocus) {
		if (vcp != m_focusedVCP) {
			if (m_focusedVCP != null && repaintOldFocus) {
				m_focusedVCP.setFocused(false);
				redrawInnerComposite(m_focusedVCP.getBounds());
			}

			if (vcp != null) {
				Resource underlying = getUnderlying(vcp);
				
				vcp.setFocused(true);
				
				redrawInnerComposite(vcp.getBounds());
			}
			
			m_focusedVCP = vcp;
			delayedUpdateFocusDataProvider();
		}
		m_focusedResource = null;
	}
	
	protected void ensureViewContainerVisible(BlockViewContainerPart vcp, boolean up) {
		if (vcp == null) {
			return;
		}
		
		Rectangle visibleArea = m_outerComposite.getClientArea();
		visibleArea.y = -m_innerComposite.getLocation().y;
		
		int y = visibleArea.y;
		
		Rectangle vcpBounds = vcp.getBounds();
		
		if (vcpBounds.y < visibleArea.y) {
			y = vcpBounds.y;
		} else if (vcpBounds.y + vcpBounds.height > visibleArea.y + visibleArea.height) {
			y = (vcpBounds.y + vcpBounds.height) - visibleArea.height;
		}
		
		y = Math.min(y, m_innerComposite.getClientArea().height - visibleArea.height);
		y = Math.max(y, 0);
		
		if (y != visibleArea.y) {
			m_innerComposite.setLocation(0, -y);
			setScrollbars();
		}
	}
	
	protected void redrawInnerComposite(Rectangle r) {
		m_innerComposite.redraw(r.x, r.y, r.width, r.height, false);
	}
	
	protected int findElementIndexOfViewContainer(BlockViewContainerPart vcp) {
		int min = 0;
		int max = m_elements.size();
		int y = vcp.getBounds().y;
		
		while (min < max) {
			int mid = (min + max) / 2;
			
			if (mid == min) {
				break;
			}
			
			Element	element = (Element) m_elements.get(mid);
			int	y2 = element.m_vcp.getBounds().y;
			
			if (y2 <= y) {
				min = mid;
			} else {
				max = mid;
			}
		}
		
		if (min >= 0 && min < m_elements.size()) {
			Element	element = (Element) m_elements.get(min);
			
			if (element.m_vcp == vcp) {
				return min;
			}
		}
		return -1;
	}
	
	protected int findIndexOfViewContainer(BlockViewContainerPart vcp) {
		if (vcp == null) {
			return -1;
		}
		
		int min = 0;
		int max = m_vcps.size();
		int y = vcp.getBounds().y;
		
		while (min < max) {
			int mid = (min + max) / 2;
			
			if (mid == min) {
				break;
			}
			
			BlockViewContainerPart	vcp2 = (BlockViewContainerPart) m_vcps.get(mid);
			int					y2 = vcp2.getBounds().y;
			
			if (y2 < y) {
				min = mid;
			} else {
				max = mid;
			}
		}
		
		for (int i = min; i < m_vcps.size(); i++) {
			BlockViewContainerPart	vcp2 = (BlockViewContainerPart) m_vcps.get(i);
			int					y2 = vcp2.getBounds().y;
			
			if (y2 > y) {
				return -1;
			} else if (vcp2 == vcp) {
				return i;
			}
		}
		return -1;
	}
	
	protected void sortViewContainersByLocation() {
		if (!m_vcpsSorted) {
			internalMergeSort(m_vcps, 0, m_vcps.size() - 1);
			m_vcpsSorted = true;
		}
	}
	
	/*
	 * Adapted from http://www.cs.ubc.ca/spider/harrison/Java/MergeSortAlgorithm.java.html
	 */
	protected void internalMergeSort(List toSortItems, int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;

		if (lo >= hi) {
		    return;
		}
		int mid = (lo + hi) / 2;
	
        internalMergeSort(toSortItems, lo, mid);
        internalMergeSort(toSortItems, mid + 1, hi);
	
		int 					end_lo = mid;
        int 					start_hi = mid + 1;
		BlockViewContainerPart	vcp1, vcp2;
		Rectangle				r1, r2;
		
		while ((lo <= end_lo) && (start_hi <= hi)) {
			vcp1 = (BlockViewContainerPart) toSortItems.get(lo);
			vcp2 = (BlockViewContainerPart) toSortItems.get(start_hi);
			
			r1 = vcp1.getBounds();
			r2 = vcp2.getBounds();
			
            if (r1.y < r2.y || (r1.y == r2.y && r1.x < r2.x)) {
                lo++;
            } else {
                /*  
                 *  a[lo] >= a[start_hi]
                 *  The next element comes from the second list, 
                 *  move the toSortItems[start_hi] element into the next 
                 *  position and shuffle all the other elements up.
                 */
				vcp1 = (BlockViewContainerPart) toSortItems.get(start_hi);
                for (int k = start_hi - 1; k >= lo; k--) {
                	toSortItems.set(k+1, toSortItems.get(k));
                }
                toSortItems.set(lo, vcp1);
                lo++;
                end_lo++;
                start_hi++;
	        }
	    }
	}

	protected void addViewContainer(BlockViewContainerPart vcp) {
		if (vcp != null) {
			m_vcps.add(vcp);
			m_vcpsSorted = false;
			
			assertFocusSelection(vcp, getUnderlying(vcp));
		}
	}
	
	protected void removeViewContainer(BlockViewContainerPart vcp) {
		if (vcp != null) {
			boolean needToReassignSelection = false;
			if (m_selectedVCPs.remove(vcp)) {
				Resource underlying = getUnderlying(vcp);
				if (m_selectionDataProviderWrapper != null && underlying != null) {
					try {
						m_selectionDataProviderWrapper.requestRemoval(underlying);
					} catch (DataMismatchException e) {
						s_logger.error(e);
					}
				}
				needToReassignSelection = true;
			}
			
			if (vcp == m_focusedVCP) {
				sortViewContainersByLocation();
				
				int index = findIndexOfViewContainer(vcp);
				m_vcps.remove(index);
				
				index = Math.min(Math.max(index, 0), m_vcps.size() - 1);
				
				if (index >= 0 && m_refocus) {
					focusViewContainer((BlockViewContainerPart) m_vcps.get(index), false);
				} else {
					focusViewContainer(null, false);
				}
			} else {
				m_vcps.remove(vcp);
			}
			
			if (needToReassignSelection && m_focusedVCP != null) {
				addSelections(m_focusedVCP, m_focusedVCP);
			}
		}
	}
	
	protected void assertFocusSelection(BlockViewContainerPart vcp, Resource underlying) {
		if (vcp != null && underlying != null) {
			if (m_selectedResources.remove(underlying)) {
				vcp.setSelected(true);
				if (m_selectionDataProviderWrapper != null) {
					try {
						m_selectionDataProviderWrapper.requestAddition(underlying);
					} catch (DataMismatchException e) {
						s_logger.error(e);
					}
				}
			}
			
			if (underlying.equals(m_focusedResource) && m_focusedVCP == null) {
				focusViewContainer(vcp, true);
			}
		}
	}

	protected boolean onGotInputFocus(FocusEvent e) {
		boolean r = super.onGotInputFocus(e);
		
		if (!r) {
			m_innerComposite.setFocus();
			r = true;
		}
		return r;
	}

	protected boolean onLostInputFocus(FocusEvent e) {
		boolean r = super.onLostInputFocus(e);

		return true;
	}

	protected java.util.List getFocusableChildParts() {
		List l = new LinkedList();
		
		if (m_focusedVCP != null) {
			l.add(m_focusedVCP);
		}
		
		return l;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#onDrag(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	protected boolean onDrag(DragSourceEvent e) {
		m_multiSelectDrag = false;
		if (m_selectedVCPs.size() > 1) {
			Point 		p = (Point) e.data;
			IVisualPart	vp = hittest(p.x, p.y, false);

			if (m_selectedVCPs.contains(vp)) {
				e.data = this;
				m_multiSelectDrag = true;
				return true;
			}
		}
		
		return super.onDrag(e);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#onDragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	protected boolean onDragSetData(DragSourceEvent e) {
		if (m_multiSelectDrag && MultipleResourceListTransfer.getInstance().isSupportedType(e.dataType)) {
			java.util.List data = new ArrayList();
			Iterator i = m_selectedVCPs.iterator();
			while (i.hasNext()) {
				IViewContainerPart vcp = (IViewContainerPart) i.next();
				data.add(vcp.getCurrentResource());
			}
			e.data = data;
			return true;
		}
	
		return super.onDragSetData(e);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		initializeSWTControls();
		
		/*if (m_headersVP != null) {
			m_context.putProperty(OzoneConstants.s_partCache, new UnserializableWrapper(new PartCache()));
		}*/

		super.initializeFromDeserialization(source);

		if (m_headersVP != null) {
			m_headersVP.initializeFromDeserialization(source);
		}
		if (m_fieldDataProvider != null) {
			m_fieldDataProvider.initializeFromDeserialization(source);
		}

		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element) i.next();
			if (e.m_vcp != null) {
				e.m_childContext.setSWTControl(m_innerComposite);
				e.m_vcp.initializeFromDeserialization(source);
			}
		}
	}
}
