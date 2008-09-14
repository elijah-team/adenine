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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.ScrollableComposite;
import edu.mit.lcs.haystack.ozone.core.VisualPartAwareComposite;
import edu.mit.lcs.haystack.ozone.core.utils.BoundingRectPainter;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataProviderWrapper;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.ViewContainerPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class MultiColumnStackerLayoutManager
	extends ListLayoutManagerBase
	implements IBlockGUIHandler, IVisualPart {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(MultiColumnStackerLayoutManager.class);
		
	class Element {
		Resource m_underlying;
		int m_height = 0;
		IViewContainerPart m_part;
		
		Element(Resource res) {
			m_underlying = res;
			
			// Create the part
			m_part = createViewContainer(res);
			
			m_childParts.put(m_part, this);
			m_visualChildParts.add(m_part);
		}
		
		void draw(GC gc, int x, int y) {
			IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
			if (bguih != null) {
				bguih.draw(gc, new Rectangle(x, y, m_columnWidth, m_height));
			}

			if (m_underlying.equals(m_focusedResource)) {
				int old = gc.getLineStyle();
				gc.setLineStyle(SWT.LINE_DOT);
				gc.setLineWidth(1);
				gc.setForeground(SlideUtilities.getAmbientColor(m_context));
				gc.drawRectangle(new Rectangle(x - 2, y - 2, m_columnWidth + 4, m_height + 4));
				gc.setLineStyle(old);
			}
		}
	

	  void renderHTML(HTMLengine he) {
	    he.enter("MultiColumnStackerLayoutManager#Element");
	    IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
	    if (bguih != null) bguih.renderHTML(he);
	    he.exit("MultiColumnStackerLayoutManager#Element");
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
	  
		void layout(int x, int y) {
			IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
			if (bguih != null) {
				bguih.setBounds(new Rectangle(x, y, m_columnWidth, m_height));
			}
		}
		
		void setVisible(boolean b) {
			IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
			if (bguih != null) {
				bguih.setVisible(b);
			}
		}
		
		int recalcHeight() {
			IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
			if (bguih != null) {
				BlockScreenspace bs = bguih.calculateSize(m_columnWidth, -1);
				return m_height = bs.m_size.y;
			} else {
				return m_height = 0;
			}
		}
		
		void dispose() {
			m_part.dispose();
			m_childParts.remove(m_part);
			m_visualChildParts.remove(m_part);
		}
		
		public String toString() {
			return "[Element: " + m_underlying + "; " + m_part.getCurrentResource() + "]";
		}
	}
		
	class Column {
		ArrayList m_elements = new ArrayList();
		
		public String toString() {
			return "[Column: " + m_elements + "]";
		}

		void dispose() {
			Iterator i = m_elements.iterator();
			while (i.hasNext()) {
				((Element) i.next()).dispose();
			}
			m_elements.clear();
		}
	}
	
	class ScrollSizeHandler implements IVisualPart, IBlockGUIHandler {
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
		 */
		public void initializeFromDeserialization(
			IRDFContainer source) {
			// TODO[dquan]: Anything to do here?
		
		}
		
		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IVisualPart#getGUIHandler(Class)
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
			if (m_rect != null && m_innerHeight != hintedHeight) {
				reshuffle(0, hintedHeight);
			}
			return new BlockScreenspace((m_horizontalMargin * 2 + m_columnWidth) * m_columns.size(), hintedHeight);
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
			he.enter("MultiColumnStackerLayoutManager#ScrollSizeHandler");
			he.exit("MultiColumnStackerLayoutManager#ScrollSizeHandler");
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
			return IBlockGUIHandler.BOTH;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
		 */
		public int getTextAlign() {
			return 0;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
		 */
		public void setBounds(Rectangle r) {
			m_innerHeight = r.height;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
		 */
		public void dispose() {
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IPart#handleEvent(Resource, Object)
		 */
		public boolean handleEvent(Resource eventType, Object event) {
			return false;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
		 */
		public void initialize(IRDFContainer source, Context context) {
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#handleGUIEvent(Resource, EventObject)
		 */
		public boolean handleGUIEvent(Resource eventType, EventObject event) {
			return false;
		}

		/**
		 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
		 */
		public void setVisible(boolean visible) {
		}
	}
	
	ArrayList m_columns = new ArrayList();
	protected int m_columnWidth = 300;
	protected boolean m_drawSeparatorLine = false;
	int m_horizontalMargin = 10;
	int m_verticalMargin = 10;
	int m_innerHeight = 0;
	BoundingRectPainter m_boundingRectPainter;
	Rectangle m_rect = null;
	ScrollableComposite m_scrollableComposite;
	VisualPartAwareComposite m_canvas;
	HashMap m_childParts = new HashMap();
	
	IDataProvider				m_focusDataProvider;
	ResourceDataProviderWrapper	m_focusDataProviderWrapper;
	ResourceDataConsumer		m_focusDataConsumer;
	Resource					m_focusedResource;

	protected void asyncRedraw() {
		Ozone.idleExec(new IdleRunnable(m_context) {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				layout();
				m_canvas.redraw();
			}
		});
	}

	protected void asyncUnfocus() {
		Ozone.idleExec(new IdleRunnable(m_context) {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				focusElement(null);
			}
		});
	}

	protected IViewContainerPart createViewContainer(Resource underlying) {
		IViewContainerPart vcp = /*m_viewContainerFactory != null ?
			m_viewContainerFactory.createViewContainer() :*/
			new ViewContainerPart(true);
			
		if (vcp instanceof ViewContainerPart) {
			((ViewContainerPart) vcp).setNestingRelation(m_nestingRelation);
		}
		
		Context	childContext = new Context(m_context);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		childContext.setSWTControl(m_canvas);
			
		vcp.initialize(m_source, childContext);
		vcp.navigate(underlying);
		
		return vcp;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#hittest
	 * (int, int, boolean)
	 */
	protected IVisualPart hittest(int x, int y, boolean favorParent) {
		int netColumnWidth = m_columnWidth + (2 * m_horizontalMargin);
		int column = x / netColumnWidth;
		if (column > m_columns.size()) {
			return null;
		}
		
		int xInColumn = x % netColumnWidth;
		if (xInColumn < m_horizontalMargin || xInColumn > (m_horizontalMargin + m_columnWidth)) {
			return null;
		}
		
		Column c = (Column)m_columns.get(column);
		int y0 = 0;
		Iterator i = c.m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element)i.next();
			y0 += m_verticalMargin;
			if (y < y0) {
				return null;
			}
			y0 += e.m_height;
			if (y < y0) {
				return e.m_part;
			}
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.BOTH;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return 0;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		return new BlockScreenspace(hintedWidth, hintedHeight);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		m_scrollableComposite.setBounds(r);
		Rectangle oldRect = m_rect;
		m_rect = new Rectangle(r.x, r.y, r.width, r.height);

		if (oldRect == null && m_innerHeight != 0) {
			reshuffle(0, m_innerHeight);
		}
		layout();

		m_canvas.redraw();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		Color oldBackground = gc.getBackground(); 
		gc.setBackground(SlideUtilities.getAmbientBgcolor(m_context));
		gc.fillRectangle(r);
		gc.setBackground(oldBackground);

		int netColumnWidth = m_columnWidth + (2 * m_horizontalMargin);
		Iterator i = m_columns.iterator();
		int x = r.x;
		while (i.hasNext()) {
			Column c = (Column)i.next();
			int y = r.y;
			
			if (x != r.x && m_drawSeparatorLine) {
				gc.setForeground(GraphicsManager.s_gray);
				gc.drawRectangle(x, m_verticalMargin, 0, m_innerHeight - 2 * m_verticalMargin);
			}
			
			Iterator i2 = c.m_elements.iterator();
			while (i2.hasNext()) {
				Element e = (Element)i2.next();
				y += m_verticalMargin;
				e.draw(gc, x + m_horizontalMargin, y);
				y += e.m_height;
			}
			x += netColumnWidth;
		}
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("MultiColumnStackerLayoutManager");
		Iterator i = m_columns.iterator();
		while (i.hasNext()) {
			Column c = (Column)i.next();
			Iterator i2 = c.m_elements.iterator();
			while (i2.hasNext()) {
				Element e = (Element)i2.next();
				e.renderHTML(he);
			}
		}
		he.exit("MultiColumnStackerLayoutManager");
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
		
	public void layout() {
		int netColumnWidth = m_columnWidth + (2 * m_horizontalMargin);
		Iterator i = m_columns.iterator();
		int x = 0;
		while (i.hasNext()) {
			Column c = (Column)i.next();
			int y = 0;
			Iterator i2 = c.m_elements.iterator();
			while (i2.hasNext()) {
				Element e = (Element)i2.next();
				y += m_verticalMargin;
				e.layout(x + m_horizontalMargin, y);
				y += e.m_height;
			}
			x += netColumnWidth;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#handleGUIEvent(Resource, EventObject)
	 */
	public boolean handleGUIEvent(Resource eventType, EventObject event) {
		return internalHandleGUIEvent(eventType, event);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		/*Iterator i = m_columns.iterator();
		while (i.hasNext()) {
			Column c = (Column)i.next();
			Iterator i2 = c.m_elements.iterator();
			while (i2.hasNext()) {
				Element e = (Element)i2.next();
				e.setVisible(visible);
			}
		}*/
		m_scrollableComposite.setVisible(visible);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ILayoutManager#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (IBlockGUIHandler.class.equals(cls) || cls == null) {
			return this;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsAdded(int, List)
	 */
	protected void processElementsAdded(int index, List addedElements) {
		int[] position = translateIndex(index);
		Iterator i = addedElements.iterator();
		Column c;
		if (position != null) {
			c = (Column)m_columns.get(position[0]);
		} else {
			c = new Column();
			m_columns.add(c);
			position = new int[] { 0, 0 };
		}
		
		while (i.hasNext()) {
			Resource res = (Resource)i.next();
			c.m_elements.add(position[1]++, new Element(res));
		}
		reshuffle(position[0], m_innerHeight);
		asyncRedraw();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsChanged(int, List)
	 */
	protected void processElementsChanged(int index, List changedElements) {
		super.processElementsChanged(index, changedElements);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsRemoved(int, List)
	 */
	protected void processElementsRemoved(int index, List removedElements) {
		int[] position = translateIndex(index);
		int originalColumn = position[0];
		Iterator i = removedElements.iterator();
		Column c = (Column) m_columns.get(position[0]);
		while (i.hasNext()) {
			Resource res = (Resource) i.next();
			if (position[1] >= c.m_elements.size()) {
				++position[0];
				position[1] = 0;
				c = (Column) m_columns.get(position[0]);
			}
			Element e = (Element) c.m_elements.remove(position[1]);
			if (!e.m_underlying.equals(res)) {
				s_logger.warn(">> Warning!! " + res + " != " + e.m_underlying);
			}
			if (m_focusedResource == e.m_underlying) {
				asyncUnfocus();
			}
			e.dispose();
		}
		reshuffle(originalColumn, m_innerHeight);
		asyncRedraw();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.ListLayoutManagerBase#processListCleared()
	 */
	protected void processListCleared() {
		int[] position = new int[] { 0, 0 };
		int originalColumn = position[0];
		Column c = (Column) m_columns.get(position[0]);
		while (true) {
			if (position[1] >= c.m_elements.size()) {
				++position[0];
				position[1] = 0;
				if (m_columns.size() == position[0]) {
					break;
				}
				c = (Column) m_columns.get(position[0]);
			}
			Element e = (Element) c.m_elements.remove(position[1]);
			if (m_focusedResource == e.m_underlying) {
				asyncUnfocus();
			}
			e.dispose();
		}
		reshuffle(originalColumn, m_innerHeight);
		asyncRedraw();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		if (m_focusDataProvider != null) {
			m_focusDataProvider.unregisterConsumer(m_focusDataConsumer);
			m_focusDataConsumer = null;
			m_focusDataProvider = null;
			m_focusDataProviderWrapper = null;
		}

		Iterator i = m_columns.iterator();
		while (i.hasNext()) {
			((Column) i.next()).dispose();
		}
		m_columns.clear();

		disposeDataConsumers();
		m_scrollableComposite.dispose();
		m_canvas.dispose();

		super.dispose();
	}

	protected void reshuffle(int column, int totalHeight) {
		if (totalHeight == 0) {
			return;
		}
		
		int oldColumnCount = m_columns.size();
		
mainloop:
		while (column < m_columns.size()) {
//			System.out.println(">> processing column " + column);
			Column c = (Column)m_columns.get(column);
			int height = m_verticalMargin;
			
			if (c.m_elements.size() == 0) {
//				System.out.println(">> removing empty column " + column);
				// Remove this column
				m_columns.remove(column);
				continue;
			}
			
			for (int i = 0; i < c.m_elements.size(); i++) {
//				System.out.println(">> processing element " + i + " of " + c.m_elements.size());

				height += m_verticalMargin;
				Element e = (Element)c.m_elements.get(i);
				height += e.recalcHeight();
				if (height > totalHeight) {
					e.m_height -= (totalHeight - height);

//					System.out.println(">> height " + height + " exceeded total height " + totalHeight); 
					if (c.m_elements.size() == 1) {
						// Nothing we can do
						++column;
						continue mainloop;
					}
					
					// Overflow condition; push onto the next column
					Column nextColumn = null;
					if (column == (m_columns.size() - 1)) {
//						System.out.println(">> out of columns; creating a new one");
						nextColumn = new Column();
						m_columns.add(nextColumn);
					} else {
						nextColumn = (Column)m_columns.get(column + 1);
					}
					
					int limit = i;
					if (i == 0) {
						limit = 1;
					}
					for (int k = c.m_elements.size() - 1; k >= limit; k--) {
						nextColumn.m_elements.add(0, c.m_elements.remove(k));
					}

					++column;
					continue mainloop;
				}
			}
			
			// Is there extra space in this column?
			while (column < (m_columns.size() - 1)) {
//				System.out.println(">> checking column " + (column + 1) + " for extra space");
				Column nextColumn = (Column)m_columns.get(column + 1);
				if (nextColumn.m_elements.size() == 0) {
//					System.out.println(">> no elements in column " + (column + 1) + "; removing");
					m_columns.remove(column + 1);
					continue;
				}
				
				Element e = (Element)nextColumn.m_elements.get(0);
				e.recalcHeight();
				if (e.m_height + m_verticalMargin + height < totalHeight) {
					height += e.m_height + m_verticalMargin;
//					System.out.println(">> moving from next column to this column; height now " + height);
					c.m_elements.add(e);
					nextColumn.m_elements.remove(0);
				} else {
//					System.out.println(">> finished; height now " + height);
					break;
				}
			}
			
			++column;
		}
		
		if (oldColumnCount != m_columns.size()) {
			m_scrollableComposite.layout(false);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		String columnWidth = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_columnWidth, m_partDataSource);
		if (columnWidth != null) {
			m_columnWidth = Integer.parseInt(columnWidth);
		}
		String verticalMargin = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_verticalMargin, m_partDataSource);
		if (verticalMargin != null) {
			m_verticalMargin = Integer.parseInt(verticalMargin);
		}
		m_drawSeparatorLine = Utilities.checkBooleanProperty(m_prescription, LayoutConstants.s_drawSeparatorLine, m_partDataSource);
		
		Composite parent = (Composite)m_context.getSWTControl();
		m_scrollableComposite = new ScrollableComposite(parent);
		m_scrollableComposite.setBackground(SlideUtilities.getAmbientBgcolor(m_context));
		m_canvas = new VisualPartAwareComposite(m_scrollableComposite, true);
		m_canvas.setVisualPart(this);
		m_scrollableComposite.setVisualPart(new ScrollSizeHandler());
		m_scrollableComposite.setInnerControl(m_canvas);
		
		m_scrollableComposite.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent me) {
				if (m_context != null) {
					if (me.button == 1) {
						focusElement(null);
					}
				}
			}
			public void mouseUp(MouseEvent me) {
				if (m_context != null) {
					if (me.button == 3) {
						Point point = m_scrollableComposite.toDisplay(new Point(me.x, me.y));
						
						PartUtilities.showContextMenu(m_source, m_context, point);
					}
				}
			}
		});

		m_boundingRectPainter = new BoundingRectPainter(m_context);
		
		m_focusDataProvider = (IDataProvider) m_context.getLocalProperty(LayoutConstants.s_focus);
		if (m_focusDataProvider != null) {
			m_focusDataConsumer = new ResourceDataConsumer() {
				protected void onResourceChanged(Resource newResource) {
					if (Ozone.isUIThread() && m_initializing) {
						m_focusedResource = newResource;
					}
				}
				protected void onResourceDeleted(Resource previousResource) {
				}
			};
			m_focusDataProvider.registerConsumer(m_focusDataConsumer);
			m_focusDataProviderWrapper = new ResourceDataProviderWrapper(m_focusDataProvider);
		}

		makeDataConsumers();
	}

	protected int[] translateIndex(int i) {
		int column = 0, index = 0;
		if (m_columns.size() == 0) {
			return null;
		}
		Column c = (Column)m_columns.get(column);
		for (int j = 0; j < i; j++) {
			if (index == c.m_elements.size()) {
				index = 0;
				c = (Column)m_columns.get(++column);
			} else {
				++index;
			}
		}
		return new int[] { column, index };
	}
	
	protected int translateIndex(int column, int index) {
		int i = 0;
		for (int j = 0; j < column; j++) {
			Column c = (Column)m_columns.get(j);
			i += c.m_elements.size();
		}
		return i + index;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#passChildResizeToParent()
	 */
	protected boolean passChildResizeToParent() {
		Iterator iChildPart = m_childrenToResize.iterator();
		
		while (iChildPart.hasNext()) {
			IPart	childPart = (IPart) iChildPart.next();
			Element e = (Element)m_childParts.get(childPart);
			if (e != null) {
				e.recalcHeight();
			}
		}

		reshuffle(0, m_innerHeight);
		layout();
		m_scrollableComposite.layout(false);
		m_canvas.redraw();

		return false;
	}
	
	protected void prepareContextMenu(MouseEvent e) {
		removeContextOperations();

		IPart part = (IPart)hittest(e.x, e.y, false);
		if (part != null) {
			Element e2 = (Element) m_childParts.get(part);
			if (e2 != null) {
				// Find the column
				Iterator i = m_columns.iterator();
				int index = 0;
				while (i.hasNext()) {
					Column c = (Column) i.next();
					Iterator j = c.m_elements.iterator();
					while (j.hasNext()) {
						Element e3 = (Element) j.next();
						if (e3 == e2) {
							createRemoveContextOperation(e2.m_underlying, index);
							return;
						}
						++index;
					}
				}
			}
		}
	}

	protected void focusElement(Element e) {
		Resource underlying = e == null ? null : e.m_underlying;
		if (m_focusedResource != underlying) {
			if (m_focusDataProviderWrapper != null && underlying != null) {
				try {
					m_focusDataProviderWrapper.requestResourceSet(underlying);
				} catch (DataMismatchException e2) {
					s_logger.error(e2);
				}
			} else if (m_focusDataProviderWrapper != null) {
				try {
					m_focusDataProviderWrapper.requestResourceDeletion();
				} catch (DataMismatchException e2) {
					s_logger.error(e2);
				}
			}

			m_focusedResource = underlying;
			
			m_canvas.redraw();
		}
	}
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onMouseEvent(edu.mit.lcs.haystack.rdf.Resource, org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onMouseEvent(Resource eventType, MouseEvent event) {
		if (eventType.equals(PartConstants.s_eventMouseDown)) {
			IVisualPart part = hittest(event.x, event.y, false);
			if (part != null) {
				Element e = (Element) m_childParts.get(part);
				if (e != null) {
					focusElement(e);
				} else {
					focusElement(null);
				}
			} else {
				focusElement(null);
			}
		}
		return super.onMouseEvent(eventType, event);
	}

	protected boolean onMouseUp(MouseEvent e) {
		if (e.button == 3) {
			if (hittest(e.x, e.y, false) == null) {
				Point	point = m_canvas.toDisplay(new Point(e.x, e.y));
				
				PartUtilities.showContextMenu(m_source, m_context, point);
				
				return true;
			}
		}
		
		return super.onMouseUp(e);
	}
}
