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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IViewNavigator;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.ScrollableComposite;
import edu.mit.lcs.haystack.ozone.core.VisualPartAwareComposite;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * The standard implementation of IViewContainerPart.
 * 
 * @author David Huynh
 * @author Dennis Quan
 */

public class ScrollableViewContainerPart extends ViewContainerPart implements IBlockGUIHandler {
	transient Composite			m_parentFrame;
	transient ScrollableComposite	m_contentFrame;
	transient LinkedList			m_cache;
	transient IViewNavigator		m_viewNavigator;
	boolean			m_routeNavigationNotifications;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ScrollableViewContainerPart.class);
	
	class CacheEntry {
		public VisualPartAwareComposite	m_composite;
		public Resource					m_underlying;
		public Resource					m_viewInstance;
		public Resource					m_part;
		public Class						m_class;
		public IVisualPart					m_visualPart;

		public String toString() { return "[" + m_composite + ";" + m_underlying + ";" + m_viewInstance + ";" + m_part + ";" + m_class + ";" + m_visualPart + "]"; }
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.GenericPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		m_cache = new LinkedList();		
		m_parentFrame = (Composite) m_context.getSWTControl();
		m_contentFrame = new ScrollableComposite(m_parentFrame);

		if (m_child != null) {
			m_childContext.setSWTControl(m_contentFrame.getInnerControl());
		}

		super.initializeFromDeserialization(source);

		m_viewNavigator = (IViewNavigator) m_context.getProperty(OzoneConstants.s_viewNavigator);
		if (m_viewNavigator != null) {
			m_viewNavigator.registerViewContainer(this);
		}
	}
	
	protected void internalInitialize() {
		m_cache = new LinkedList();		
		m_parentFrame = (Composite) m_context.getSWTControl();
		m_contentFrame = new ScrollableComposite(m_parentFrame);
		
		m_synchronous = false;
		
		super.internalInitialize();
		
		m_viewNavigator = (IViewNavigator) m_context.getProperty(OzoneConstants.s_viewNavigator);
		if (m_viewNavigator != null) {
			m_viewNavigator.registerViewContainer(this);
		}

		m_routeNavigationNotifications = Utilities.checkBooleanProperty(m_prescription, PartConstants.s_routeNavigationNotifications, m_partDataSource);
	}
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_viewNavigator != null) {
			m_viewNavigator.unregisterViewContainer(this);
			m_viewNavigator = null;
		}

		if (m_cache != null) {
			for (int i = 0; i < m_cache.size(); i++) {
				CacheEntry entry = (CacheEntry) m_cache.get(i);
				if (entry.m_visualPart != null) {
					entry.m_visualPart.dispose();
				}
				entry.m_composite.setVisualPart(null);
				
				if (!entry.m_composite.isDisposed()) {
					entry.m_composite.dispose();
				}
			}
			m_cache.clear();
		}
		m_cache = null;
		
		m_child = null;
		m_currentResource = null;
		m_currentViewInstance = null;
		
		m_contentFrame.setVisualPart(null);
		m_contentFrame.setInnerControl(null);
		if (!m_contentFrame.isDisposed()) {
			m_contentFrame.dispose();
		}
		m_contentFrame = null;
		
		m_parentFrame = null;
		
		super.dispose();
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		if (eventType.equals(PartConstants.s_eventChildResize)) {
			if (((ChildPartEvent) event).m_childPart == m_child) {
				m_contentFrame.layout(true);
			}
		}
		return true;
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		} else {
			return null;
		}
	}

	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.BOTH;
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		m_contentFrame.setBounds(r);
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (hintedWidth == -1 || hintedHeight == -1) {
			if (m_child != null) {
				IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
				if (blockGUIHandler != null) {
					return blockGUIHandler.calculateSize(hintedWidth, hintedHeight);
				}
			}
			
			return new BlockScreenspace(0, 0, BlockScreenspace.ALIGN_TEXT_CLEAR, 0);
		} else {
			return new BlockScreenspace(hintedWidth, hintedHeight, BlockScreenspace.ALIGN_TEXT_CLEAR, 0);
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
		he.enter("ScrollableViewContainer");
		if (m_child != null) {
			IGUIHandler h = m_child.getGUIHandler(null);
			if (h instanceof IBlockGUIHandler) {
				((IBlockGUIHandler)  h).renderHTML(he);
			} else if (h instanceof IInlineGUIHandler) {
				((IInlineGUIHandler) h).renderHTML(he);
			}
		}
		he.exit("ScrollableViewContainer");
	}
	
	/**
	 * @see IViewContainerPart#refresh()
	 */
	public void refresh() {
		synchronized (this) {
			if (m_currentResource == null) {
				return;
			}
			
			if (m_runnable != null) {
				m_runnable.expire();
				m_runnable = null;
			}
			
			if (m_synchronous && Ozone.isUIThread()) {
				internalRefresh();
			} else {
				m_runnable = new IdleRunnable(m_context) {
					public void run() {
						if (m_context != null) {
							internalRefresh();
						}
					}
				};
			}
		}
		scheduleRunnable();
		/* SJG
		HTMLengine he = new HTMLengine();
		he.startPage("ScrollableViewContainerPart");
		renderHTML(he);
		he.endPage();
		*/
	}
	
	protected void scheduleRunnable() {
		Ozone.idleExecFirst(m_runnable);
	}
	
	protected void internalRefresh() {
		if (m_child != null) {
			m_child.dispose();
			m_child = null;
		}
		initializeChildPart();
		
		((CacheEntry) m_cache.get(0)).m_visualPart = m_child;
	}
	
	static int s_cacheCount = 10;
	
	synchronized protected void initializeChild() {
		CacheEntry entry;

		Iterator i = m_cache.iterator();
		while (i.hasNext()) {
			entry = (CacheEntry) i.next();
			
			if (entry.m_underlying.equals(m_currentResource) && 
				entry.m_viewInstance.equals(m_currentViewInstance) &&
				entry.m_part.equals(m_currentPart) &&
				entry.m_class.equals(m_currentClass)) {
				i.remove();
				m_cache.add(0, entry);
				
				m_child = entry.m_visualPart;
				
				m_contentFrame.setInnerControl(entry.m_composite);
				m_contentFrame.setVisualPart(entry.m_visualPart);
			
				setHostingProperties();
				
				return;
			}
		}
		
		if (m_cache.size() == s_cacheCount) { // reuse
			entry = (CacheEntry) m_cache.remove(s_cacheCount - 1);
			
			if (entry.m_visualPart != null) {
				entry.m_visualPart.dispose();
				entry.m_visualPart = null;
			}
		} else {
			entry = new CacheEntry();
			entry.m_composite = new VisualPartAwareComposite(m_contentFrame, true); // do we need to pass key events to the parent frame?
		}
		m_contentFrame.setInnerControl(entry.m_composite);

		initializeChildPart();

		entry.m_underlying = m_currentResource;
		entry.m_viewInstance = m_currentViewInstance;
		entry.m_part = m_currentPart;
		entry.m_class = m_currentClass;
		entry.m_visualPart = m_child;
		
		m_cache.add(0, entry);
	}
	
	protected void initializeChildPart() {
		if (isOverNested()) {
			return;
		}
		
		m_childContext = new Context(m_context);
		
		m_childContext.setInformationSource(m_childInfoSource);
		m_childContext.putLocalProperty(OzoneConstants.s_part, m_currentPart);
		m_childContext.putLocalProperty(OzoneConstants.s_underlying, m_currentResource);
		m_childContext.putLocalProperty(OzoneConstants.s_viewInstance, m_currentViewInstance);
		m_childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		m_childContext.putProperty(OzoneConstants.s_viewContainer, this);
		m_childContext.setSWTControl(m_contentFrame.getInnerControl());
		
		try {
			m_child = (IVisualPart) m_currentClass.newInstance();
			
			setHostingProperties();				
			m_child.initialize(m_source, m_childContext);
		} catch (Exception e) {
			s_logger.error("Failed to initialize child part " + m_currentClass.getName() + " for " + m_currentResource, e);
			m_child = null;
		}

		m_contentFrame.setVisualPart(m_child);
	}
	
	synchronized public void onNavigateComplete(Resource resource, IPart childPart) {
		Iterator i = m_cache.iterator();
		while (i.hasNext()) {
			CacheEntry entry = (CacheEntry) i.next();
			
			if (entry.m_visualPart == childPart) {
				entry.m_underlying = resource;
				break;
			}
		}
				
		if (childPart == m_child && m_viewNavigator != null) {
			m_viewNavigator.notifyNavigation(resource);
		}
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (m_contentFrame != null) {
			m_contentFrame.setVisible(visible);
		}
	}
}
