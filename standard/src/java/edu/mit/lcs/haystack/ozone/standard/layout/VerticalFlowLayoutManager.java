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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.CSSstyle;
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
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.BooleanDataConsumer;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataProviderWrapper;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.ViewContainerPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class VerticalFlowLayoutManager
	extends ListLayoutManagerBase
	implements IBlockGUIHandler, IVisualPart {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(VerticalFlowLayoutManager.class);

	class Element implements Serializable {
		Resource m_underlying;
		int m_height = 0;
		boolean m_allColumns = false;
		IViewContainerPart m_part;
		SpansAllColumnsDataConsumer m_dataConsumer;
		Context m_childContext;
		
		void initializeFromDeserialization(IRDFContainer source) {
			if (m_dataConsumer != null) {
				m_dataConsumer.m_dataProvider.initializeFromDeserialization(source);
			}
			if (m_part != null) {
				m_part.initializeFromDeserialization(source);
			}
		}
		
		class SpansAllColumnsDataConsumer extends BooleanDataConsumer implements IDataProvider {
			IDataProvider m_dataProvider;
			SpansAllColumnsDataConsumer() {
				Context	childContext = new Context(m_context);
				childContext.putLocalProperty(OzoneConstants.s_dataProvider, this);
				m_dataProvider = DataUtilities.createDataProvider2(m_spansAllColumnsDataSource, childContext, m_source, m_partDataSource);
				m_dataProvider.registerConsumer(this);
			}
			
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
			 */
			public void initializeFromDeserialization(
				IRDFContainer source) {
			}
		
			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
			 */
			public Object getData(Resource dataType, Object specifications)
				throws DataNotAvailableException {
				return m_underlying;
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
			 */
			public Resource getStatus() {
				return null;
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.IPart#handleEvent(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
			 */
			public boolean handleEvent(Resource eventType, Object event) {
				return false;
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.BooleanDataConsumer#onBooleanChanged(java.lang.Boolean)
			 */
			protected void onBooleanChanged(Boolean b) {
				if (b.booleanValue() != m_allColumns) {
					m_allColumns = b.booleanValue();
					asyncReshuffle();
				}
			}
			
			public void dispose() {
				m_dataProvider.dispose();
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
			 */
			public void registerConsumer(IDataConsumer dataConsumer) {
				dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_underlying);
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
			 */
			public void requestChange(Resource changeType, Object change)
				throws UnsupportedOperationException, DataMismatchException {
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(edu.mit.lcs.haystack.rdf.Resource)
			 */
			public boolean supportsChange(Resource changeType) {
				return false;
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
			 */
			public void unregisterConsumer(IDataConsumer dataConsumer) {
			}

			/* (non-Javadoc)
			 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
			 */
			public void initialize(IRDFContainer source, Context context) {
			}

		}

		protected void createViewContainer() {
			IViewContainerPart vcp = new ViewContainerPart(true);

			if (vcp instanceof ViewContainerPart) {
				((ViewContainerPart) vcp).setNestingRelation(m_nestingRelation);
			}

			m_childContext = new Context(m_context);
			m_childContext.putLocalProperty(OzoneConstants.s_parentPart, VerticalFlowLayoutManager.this);
			m_childContext.putLocalProperty(OzoneConstants.s_part, OzoneConstants.s_viewContainerPart);
			m_childContext.setSWTControl(m_canvas);

			Resource vcPartData = Utilities.generateUniqueResource();
			m_childContext.putLocalProperty(OzoneConstants.s_partData, vcPartData);
		
			Interpreter 		i = Ozone.getInterpreter();
			DynamicEnvironment 	denv = new DynamicEnvironment(m_source);
			Resource			dataSource = (Resource) m_context.getProperty(OzoneConstants.s_dataSource);

			Ozone.initializeDynamicEnvironment(denv, m_context);

			try {
				i.callMethod(SETUP_SPAN_ALL_COLUMNS_OPERATION, new Message(new Object[] { m_prescription, m_context.getLocalProperty(OzoneConstants.s_layoutInstance), m_underlying, vcPartData }), denv);
			} catch (AdenineException e2) {
				s_logger.error("Error invoking " + SETUP_SPAN_ALL_COLUMNS_OPERATION, e2);
			}

			vcp.initialize(m_source, m_childContext);
			vcp.navigate(m_underlying);

			m_part = vcp;
		}

		Element(Resource res) {
			m_underlying = res;

			// Create the part
			createViewContainer();

			m_childParts.put(m_part, this);
			m_elementMap.put(res, this);
			
			// Check for a full row specification
			try {
				RDFNode[] rdfn = m_partDataSource.queryExtract(new Statement[] {
					new Statement(m_prescription, LayoutConstants.s_constraint, Utilities.generateWildcardResource(2)),
					new Statement(Utilities.generateWildcardResource(2), LayoutConstants.s_element, res),
					new Statement(Utilities.generateWildcardResource(2), LayoutConstants.s_spansAllColumns, Utilities.generateWildcardResource(1))
				}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
				if (rdfn != null && (rdfn[0].getContent().compareToIgnoreCase("true") == 0)) {
					m_allColumns = true;
				} 
			} catch (RDFException rdfe) {
			}
			
			if (m_spansAllColumnsDataSource != null) {
				m_dataConsumer = new SpansAllColumnsDataConsumer();
			}
		}
		
		int getTrueWidth() {
			return m_allColumns ? m_innerWidth - 2 * m_horizontalMargin : m_columnWidth;
		}

		void draw(GC gc, int x, int y) {
			IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
			if (bguih != null) {
				bguih.draw(gc, new Rectangle(x, y, getTrueWidth(), m_height));
			}

			if (m_underlying.equals(m_focusedResource) && m_focusDataProvider != null) {
				int old = gc.getLineStyle();
				gc.setLineStyle(SWT.LINE_DOT);
				gc.setLineWidth(1);
				gc.setForeground(SlideUtilities.getAmbientColor(m_context));
				gc.drawRectangle(new Rectangle(x - 2, y - 2, getTrueWidth() + 4, m_height + 4));
				gc.setLineStyle(old);
			}
		}

	  void renderHTML(HTMLengine he) {
	    he.enter("VerticalFlowLayoutManager#Element");
	    IBlockGUIHandler bguih = (IBlockGUIHandler)m_part.getGUIHandler(IBlockGUIHandler.class);
	    if (bguih != null) bguih.renderHTML(he);
	    he.exit("VerticalFlowLayoutManager#Element");
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
				bguih.setBounds(new Rectangle(x, y, getTrueWidth(), m_height));
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
				BlockScreenspace bs = bguih.calculateSize(getTrueWidth(), -1);
				return m_height = bs.m_size.y;
			} else {
				return m_height = 0;
			}
		}

		void dispose() {
			m_part.dispose();
			m_childParts.remove(m_part);
			m_elementMap.remove(m_underlying);
			if (m_dataConsumer != null) {
				m_dataConsumer.dispose();
			}
		}

		public String toString() {
			return "[Element: " + m_underlying + "; " + m_part.getCurrentResource() + "]";
		}
	}

	class Column implements Serializable {
		ArrayList m_elements = new ArrayList();

		public String toString() {
			return "[Column: " + m_elements + "]";
		}
		
		public void add(Element e) {
			m_elements.add(e);
			if (m_elements.size() > 1) {
				m_height += m_verticalMargin;
			}
			m_height += e.m_height;
		}
		
		int m_height = 0;
	}
	
	abstract class LayoutItem implements Serializable {
		abstract int getHeight();
	}
	
	class MultiColumnLayoutItem extends LayoutItem {
		int m_height = 0;
		ArrayList m_columns = new ArrayList();

		int getHeight() { 
			return m_height;
		}
		
		public String toString() {
			return "[MultiColumnLayoutItem: " + m_columns + "; height=" + m_height + "]";
		}
				
		MultiColumnLayoutItem(ArrayList elements) {
			// Tell all elements to recalc heights
			Iterator i = elements.iterator();
			while (i.hasNext()) {
				Element e = (Element) i.next();
				e.recalcHeight();
			}
			
			// If there aren't enough columns, then just assign immediately
			if (elements.size() <= VerticalFlowLayoutManager.this.m_columns) {
				i = elements.iterator();
				while (i.hasNext()) {
					Element e = (Element) i.next();
					Column c = new Column();
					c.add(e);
					m_columns.add(c);
					if (c.m_height > m_height) {
						m_height = c.m_height;
					}
				}
				
				while (m_columns.size() < VerticalFlowLayoutManager.this.m_columns) {
					m_columns.add(new Column());
				}
			} else {
				for (int j = 0; j < VerticalFlowLayoutManager.this.m_columns; j++) {
					m_columns.add(new Column());
				}
				
				i = elements.iterator();
				int column = 0;
				int[] divisions = new int[VerticalFlowLayoutManager.this.m_columns];
				divisions = optimize(elements, divisions, 0);
				int j = 0;
				while (i.hasNext()) {
					Element e = (Element) i.next();
					Column c = (Column) m_columns.get(column);
					c.add(e);
					if (c.m_height > m_height) {
						m_height = c.m_height;
					}
					if (column < (divisions.length - 1) && divisions[column] == j) {
						++column;
					}
					++j;
				}
			}
		}
		
		// TODO[dquan]: memoize
		int[] optimize(ArrayList elements, int[] divisions, int position) {
			int[] newDivisions = new int[divisions.length];
			System.arraycopy(divisions, 0, newDivisions, 0, divisions.length);
			if (position >= (divisions.length - 1)) {
				return newDivisions;
			}
			
			int size = elements.size();
			int start = 0;
			if (position > 0) {
				start = divisions[position - 1] + 1;
			}
			
			int[] bestSet = null;
			int bestScore = 0;
			for (int i = start; i < (size - 1); i++) {
				newDivisions[position] = i;
				int[] x = optimize(elements, newDivisions, position + 1);
				int score = computeScore(elements, x);
				if (bestSet == null || score < bestScore) {
					bestScore = score;
					bestSet = x;
				}
			}
			
			if (bestSet == null) {
				return newDivisions;
			}
			return bestSet;
		}
		
		int computeScore(ArrayList elements, int[] divisions) {
			Iterator i = elements.iterator();
			int n = 0;
			int total = 0;
			int j = 0;
			int[] heights = new int[divisions.length];
			while (i.hasNext()) {
				Element e = (Element) i.next();
				heights[n] += e.m_height + m_verticalMargin;
				total += e.m_height + m_verticalMargin;
				if (divisions[n] == j) {
					++n;
				}
				++j;
			}
			int score = 0;
			int average = total / heights.length; 
			for (int k = 0; k < heights.length; k++) {
				int diff = heights[k] - average;
				score += (diff > 0) ? diff : -diff;
			}
			return score;
		}
	}
	
	class SingleElementLayoutItem extends LayoutItem {
		Element m_element = null;

		public String toString() {
			return "[SingleElementLayoutItem: " + m_element + "; height=" + m_element.m_height + "]";
		}

		int getHeight() { 
			return m_element.m_height;
		}
	}

	class ScrollSizeHandler implements IVisualPart, IBlockGUIHandler {
		/* (non-Javadoc)
		 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
		 */
		public void initializeFromDeserialization(
			IRDFContainer source) {
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
			m_actualInnerWidth = hintedWidth;
			m_innerWidth = m_actualInnerWidth;
			if (m_maxWidth != -1 && m_innerWidth > m_maxWidth) {
				m_innerWidth = m_maxWidth;
			}
			if (m_rect != null) {
				reshuffle(m_innerWidth);
			}
			return new BlockScreenspace(hintedWidth, m_layout.size() == 0 ? hintedHeight : m_totalHeight);
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
			Iterator i = m_layout.iterator();
			int x = r.x;
			int cumulativeY = r.y + m_verticalMargin;
			
			if (!i.hasNext() && m_emptyMessage != null) {
				Font font = gc.getFont();
				gc.setFont(SlideUtilities.getAmbientFont(m_context));
				Color oldForeground = gc.getForeground(); 
				gc.setForeground(SlideUtilities.getAmbientColor(m_context));
				gc.drawText(m_emptyMessage, r.x + m_horizontalMargin, r.y + m_verticalMargin, true);
				gc.setFont(font);
				gc.setForeground(oldForeground);
				return;
			}
			
			while (i.hasNext()) {
				LayoutItem li = (LayoutItem) i.next();
			
				if (li instanceof SingleElementLayoutItem) {
					Element e = ((SingleElementLayoutItem) li).m_element;
					e.draw(gc, x + m_horizontalMargin, cumulativeY);
				} else {			
					MultiColumnLayoutItem mcli = (MultiColumnLayoutItem) li;
					Iterator i1 = mcli.m_columns.iterator();

					while (i1.hasNext()) {
						Column c = (Column) i1.next();
	
						int y = cumulativeY;
						if (x != r.x && m_drawSeparatorLine) {
							gc.setForeground(GraphicsManager.s_gray);
							gc.drawRectangle(x, cumulativeY, 0, mcli.m_height);
						}

						Iterator i2 = c.m_elements.iterator();
						while (i2.hasNext()) {
							Element e = (Element)i2.next();
							e.draw(gc, x + m_horizontalMargin, y);
							if (i2.hasNext()) {
								y += m_verticalMargin;
							}							
							y += e.m_height;
						}
						x += netColumnWidth;
					}
					x = r.x;
				}
			
				cumulativeY += m_verticalMargin + li.getHeight();
			}

			if (m_insertionRectangle != null) {
				boolean	xor = gc.getXORMode();
				int		lineStyle = gc.getLineStyle();
				int		lineWidth = gc.getLineWidth();
				Color		color = gc.getBackground();

				//gc.setXORMode(true);
				//gc.setLineStyle(SWT.LINE_DOT);
				//gc.setLineWidth(1);
				//gc.setForeground(SlideUtilities.getAmbientColor(m_context));
				gc.setBackground(SlideUtilities.getAmbientColor(m_context));
				gc.fillRectangle(m_insertionRectangle.x, m_insertionRectangle.y, m_insertionRectangle.width - 1, m_insertionRectangle.height - 1);
			
				gc.setXORMode(xor);
				gc.setLineStyle(lineStyle);
				gc.setLineWidth(lineWidth);
				gc.setBackground(color);
			}
		}
		
		/**
		 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
		 */
		public void renderHTML(HTMLengine he) {
			Iterator i = m_layout.iterator();
			if (!i.hasNext() && m_emptyMessage != null) {
				he.text(m_emptyMessage, m_CSSstyle, null, null, "VerticalFlowLayoutManager#ScrollSizeHandler");
				return;
			}
			he.rowSetStart("VerticalFlowLayoutManager");
			while (i.hasNext()) {
				LayoutItem li = (LayoutItem) i.next();
			
				if (li instanceof SingleElementLayoutItem) {
					Element e = ((SingleElementLayoutItem) li).m_element;
					he.rowStart("VerticalFlowLayoutManager");
					e.renderHTML(he);
					he.rowEnd("VerticalFlowLayoutManager");
				} else {			
					MultiColumnLayoutItem mcli = (MultiColumnLayoutItem) li;
					Iterator i1 = mcli.m_columns.iterator();

					while (i1.hasNext()) {
						Column c = (Column) i1.next();
						Iterator i2 = c.m_elements.iterator();
						while (i2.hasNext()) {
							Element e = (Element)i2.next();
							he.rowStart("VerticalFlowLayoutManager");
							e.renderHTML(he);
							he.rowEnd("VerticalFlowLayoutManager");
						}
					}
				}
			}
			he.rowSetEnd("VerticalFlowLayoutManager");
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
			return VerticalFlowLayoutManager.this.handleEvent(eventType, event);
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

	protected ArrayList m_elements = new ArrayList();
	protected HashMap m_elementMap = new HashMap();
	protected ArrayList m_layout = new ArrayList();
	protected String m_emptyMessage = "Drag and drop items here or add items using the context menu.";
	protected int m_columnWidth = 250;
	protected boolean m_drawSeparatorLine = false;
	protected int m_horizontalMargin = 10;
	protected int m_verticalMargin = 10;
	protected int m_innerWidth = 0;
	protected int m_actualInnerWidth = 0;
	protected int m_totalHeight = 0;
	protected int m_columns = 1;
	protected int m_minColumnWidth = 250;
	protected int m_maxWidth = -1;
	protected Rectangle m_rect = null;
	protected HashMap m_childParts = new HashMap();
	
	protected CSSstyle m_CSSstyle;

	transient protected BoundingRectPainter m_boundingRectPainter;
	transient protected ScrollableComposite m_scrollableComposite;
	transient protected VisualPartAwareComposite m_canvas;
	
	protected IDataProvider					m_focusDataProvider;
	protected ResourceDataProviderWrapper	m_focusDataProviderWrapper;
	protected ResourceDataConsumer			m_focusDataConsumer;
	protected Resource						m_focusedResource;
	protected Resource						m_spansAllColumnsDataSource;
	
	/*protected int m_insertionColumn = -1;
	protected int m_insertionRowInColumn = -1;
	protected int m_insertionLayoutIndex = -1;*/
	protected int m_insertionIndex = -1;
	protected boolean m_insertionSpanAllColumns = false;
	protected Rectangle m_insertionRectangle = null;

	protected void asyncRedraw() {
		Ozone.idleExecOnce(m_redrawRunnable);
	}
	
	transient IdleRunnable m_redrawRunnable;
	transient IdleRunnable m_reshuffleRunnable; 
	
	protected void asyncReshuffle() {
		Ozone.idleExecOnce(m_reshuffleRunnable);
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

	public void setSpanOneColumn(Resource res) {
		Element element = (Element) m_elementMap.get(res);
		if (element != null) {
			element.m_allColumns = false;
			try {
				element.m_dataConsumer.m_dataProvider.requestChange(DataConstants.BOOLEAN_CHANGE, Boolean.FALSE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			asyncReshuffle();
		}
	}

	public void setSpanAllColumns(Resource res) {
		Element element = (Element) m_elementMap.get(res);
		if (element != null) {
			try {
				element.m_dataConsumer.m_dataProvider.requestChange(DataConstants.BOOLEAN_CHANGE, Boolean.TRUE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			element.m_allColumns = true;
			asyncReshuffle();
		}
	}

	static final Resource SETUP_SPAN_ALL_COLUMNS_OPERATION = new Resource("http://haystack.lcs.mit.edu/schemata/layout#setupSpanAllColumnsOperations");

	protected void hittestInsertionPoint(int x, int y) {
		/*m_insertionColumn = -1;
		m_insertionLayoutIndex = -1;
		m_insertionRowInColumn = -1;*/
		m_insertionSpanAllColumns = false;
		m_insertionIndex = -1;
		m_insertionRectangle = null;
		
		int topMargin = m_verticalMargin / 2;
		int bottomMargin = m_verticalMargin - topMargin;
		
		if (m_rect == null) {
			return;
		}

// Coming from our own scrollable view composite
//		x -= m_rect.x;
//		y -= m_rect.y;
		
		int netColumnWidth = m_columnWidth + (2 * m_horizontalMargin);
		int column = x / netColumnWidth;
		if (column > m_columns) {
			return;
		}
		
		Iterator i = m_layout.iterator();
		if (!i.hasNext() || (y < bottomMargin)) {
			//m_insertionLayoutIndex = 0;
			m_insertionIndex = 0;
			m_insertionRectangle = new Rectangle(m_horizontalMargin, bottomMargin - 2, m_innerWidth - m_horizontalMargin * 2, 3);
			return;
		}

		if (y > (m_totalHeight - topMargin)) {
			//m_insertionLayoutIndex = m_layout.size();
			m_insertionIndex = m_elements.size();
			m_insertionRectangle = new Rectangle(m_horizontalMargin, m_totalHeight - topMargin - 2, m_innerWidth - m_horizontalMargin * 2, 3);
			return;
		}
		
		// Find the right layout item
		int insertionLayoutIndex = 0;
		LayoutItem li = null;
		int cumulativeY = bottomMargin;
		boolean inBottom = false;
		while (i.hasNext()) {
			li = (LayoutItem) i.next();
			if (y >= cumulativeY && y < (cumulativeY + li.getHeight() + topMargin)) {
				inBottom = false;
				break;
			} 
			++insertionLayoutIndex;
			cumulativeY += li.getHeight() + m_verticalMargin;
			if (y >= (cumulativeY - bottomMargin) && (y < cumulativeY)) {
				inBottom = true;
				break;
			}
		}

		if (li instanceof SingleElementLayoutItem) {
			//m_insertionLayoutIndex = insertionLayoutIndex;
			m_insertionIndex = m_elements.indexOf(((SingleElementLayoutItem) li).m_element) + (inBottom ? 1 : 0);
			m_insertionSpanAllColumns = true;
			m_insertionRectangle = new Rectangle(m_horizontalMargin, cumulativeY - 2, m_innerWidth - m_horizontalMargin * 2, 3);
			return;
		}

		// Find the right column
		MultiColumnLayoutItem mcli = (MultiColumnLayoutItem) li;
		int xInColumn = x % netColumnWidth;
/*		if (xInColumn < m_horizontalMargin || xInColumn > (m_horizontalMargin + m_columnWidth)) {
			return;
		}*/

		Column c = (Column) mcli.m_columns.get(column);
		i = c.m_elements.iterator();
		int insertionRowInColumn = 0;
		if (!i.hasNext()) {
/*			m_insertionColumn = column;
			m_insertionRowInColumn = insertionRowInColumn;
			m_insertionLayoutIndex = insertionLayoutIndex;*/
			// Find index of last item in this multi column set
			Iterator k = mcli.m_columns.iterator();
			Element e2 = null;
			while (k.hasNext()) {
				Column c2 = (Column) k.next();
				if (c2.m_elements.size() > 0) {
					e2 = (Element) c2.m_elements.get(c2.m_elements.size() - 1);
				}
			}
			m_insertionIndex = m_elements.indexOf(e2) + 1;
			m_insertionRectangle = new Rectangle(m_horizontalMargin + column * netColumnWidth, cumulativeY - 2, m_columnWidth, 3);
			return;
		}
		while (i.hasNext()) {
			Element e = (Element) i.next();
			if (y < (cumulativeY + topMargin + e.m_height)) {
/*				m_insertionColumn = column;
				m_insertionRowInColumn = insertionRowInColumn;
				m_insertionLayoutIndex = insertionLayoutIndex;*/
				m_insertionIndex = m_elements.indexOf(e);
				m_insertionRectangle = new Rectangle(m_horizontalMargin + column * netColumnWidth, cumulativeY - 2, m_columnWidth, 3);
				return;
			}
			cumulativeY += m_verticalMargin + e.m_height;
			++insertionRowInColumn;
			if (!i.hasNext() || (y < cumulativeY)) {
				/*m_insertionColumn = column;
				m_insertionRowInColumn = insertionRowInColumn;
				m_insertionLayoutIndex = insertionLayoutIndex;*/
				m_insertionIndex = m_elements.indexOf(e) + 1;
				m_insertionRectangle = new Rectangle(m_horizontalMargin + column * netColumnWidth, cumulativeY - 2, m_columnWidth, 3);
				return;
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#hittest
	 * (int, int, boolean)
	 */
	protected IVisualPart hittest(int x, int y, boolean favorParent) {
		if (m_rect == null) {
			return null;
		}

// Coming from our own scrollable view composite
//		x -= m_rect.x;
//		y -= m_rect.y;
		
		int netColumnWidth = m_columnWidth + (2 * m_horizontalMargin);
		int column = x / netColumnWidth;
		if (column >= m_columns) {
			return null;
		}
		if (y > m_totalHeight || y < m_verticalMargin) {
			return null;
		}
		
		// Find the right layout item
		Iterator i = m_layout.iterator();
		LayoutItem li = null;
		int cumulativeY = m_verticalMargin;
		while (i.hasNext()) {
			li = (LayoutItem) i.next();
			if (y >= cumulativeY && y < (cumulativeY + li.getHeight())) {
				break;
			}
			cumulativeY += li.getHeight() + m_verticalMargin;
			li = null; 
		}
		if (li == null) {
			return null;
		}

		if (li instanceof SingleElementLayoutItem) {
			return ((SingleElementLayoutItem) li).m_element.m_part;
		}

		// Find the right column
		MultiColumnLayoutItem mcli = (MultiColumnLayoutItem) li;
		int xInColumn = x % netColumnWidth;
		if (xInColumn < m_horizontalMargin || xInColumn > (m_horizontalMargin + m_columnWidth)) {
			return null;
		}

		Column c = (Column) mcli.m_columns.get(column);
		i = c.m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element)i.next();
			cumulativeY += e.m_height;
			if (y < cumulativeY) {
				return e.m_part;
			}
			cumulativeY += m_verticalMargin;
			if (y < cumulativeY) {
				return null;
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
		m_rect = new Rectangle(r.x, r.y, r.width, r.height);

		layout();
		m_canvas.redraw();
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
		he.enter("VerticalFlowLayoutManager");
		((ScrollSizeHandler) m_scrollableComposite.getVisualPart()).renderHTML(he);
		he.exit("VerticalFlowLayoutManager");
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
		Iterator i = m_layout.iterator();
		int x = 0;
		int cumulativeY = m_verticalMargin;
		while (i.hasNext()) {
			LayoutItem li = (LayoutItem) i.next();

			if (li instanceof SingleElementLayoutItem) {
				Element e = ((SingleElementLayoutItem) li).m_element;
				e.layout(x + m_horizontalMargin, cumulativeY);
			} else {
				MultiColumnLayoutItem mcli = (MultiColumnLayoutItem) li;
				Iterator i1 = mcli.m_columns.iterator();

				while (i1.hasNext()) {
					Column c = (Column) i1.next();
					int y = cumulativeY;

					Iterator i2 = c.m_elements.iterator();
					while (i2.hasNext()) {
						Element e = (Element)i2.next();
						e.layout(x + m_horizontalMargin, y);
						if (i2.hasNext()) {
							y += m_verticalMargin;
						}
						y += e.m_height;
					}
					x += netColumnWidth;
				}
				x = 0;
			}

			cumulativeY += m_verticalMargin + li.getHeight();
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
		/*Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element) i.next();
			e.setVisible(visible);
		}*/
		m_scrollableComposite.setVisible(visible);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.ILayoutManager#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || IBlockGUIHandler.class.equals(cls)) {
			return this;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.ListLayoutManagerBase#processElementsAdded(int, List)
	 */
	protected void processElementsAdded(int index, List addedElements) {
		Iterator i = addedElements.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (o instanceof Resource) {
				Resource res = (Resource) o;
				Element e = new Element(res);
				m_elements.add(index++, e);
			}
		}
		asyncReshuffle();
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
		Iterator i = removedElements.iterator();
		while (i.hasNext()) {
			Resource res = (Resource) i.next();
			Element e = (Element) m_elements.remove(index);
			if (res == null || !res.equals(e.m_underlying)) {
				s_logger.warn(">> Warning!! " + res + " != " + e.m_underlying);
			}
			e.dispose();
		}
		reshuffle(m_innerWidth);
		asyncRedraw();
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.ListLayoutManagerBase#processListCleared()
	 */
	protected void processListCleared() {
		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element) i.next();
			e.dispose();
		}
		m_elements.clear();
		reshuffle(m_innerWidth);
		asyncRedraw();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		disposeDataConsumers();

		if (m_focusDataProvider != null) {
			m_focusDataProvider.unregisterConsumer(m_focusDataConsumer);
			m_focusDataConsumer = null;
			m_focusDataProvider = null;
			m_focusDataProviderWrapper = null;
		}

		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			((Element) i.next()).dispose();
		}
		m_elements.clear();

		m_scrollableComposite.dispose();
		m_canvas.dispose();
		super.dispose();
	}

	protected void reshuffle(int totalWidth) {
		int totalHeight = m_totalHeight;
		
		m_layout = new ArrayList();
		m_totalHeight = m_verticalMargin;
		if (totalWidth != 0) {
			m_columns = totalWidth / m_minColumnWidth;
			if (m_columns < 1) {
				m_columns = 1;
			}
			m_columnWidth = totalWidth / m_columns - m_verticalMargin * 2;
	
			ArrayList buffer = new ArrayList();		
			Iterator i = m_elements.iterator();
			while (i.hasNext()) {
				Element e = (Element) i.next();
				
				if (e.m_allColumns) {
					if (!buffer.isEmpty()) {
						MultiColumnLayoutItem mcli = new MultiColumnLayoutItem(buffer);
						buffer.clear();
						m_layout.add(mcli);
						m_totalHeight += mcli.m_height + m_verticalMargin;
					}
					
					SingleElementLayoutItem seli = new SingleElementLayoutItem();
					seli.m_element = e;
					m_layout.add(seli);
					m_totalHeight += e.recalcHeight() + m_verticalMargin;
				} else {
					buffer.add(e);
				}
			}
	
			if (!buffer.isEmpty()) {
				MultiColumnLayoutItem mcli = new MultiColumnLayoutItem(buffer);
				buffer.clear();
				m_layout.add(mcli);
				m_totalHeight += mcli.m_height + m_verticalMargin;
			}
		}
		
		if (totalHeight != m_totalHeight) {
			m_scrollableComposite.layout(false);
			Ozone.idleExec(new IdleRunnable(m_context) {
				public void run() {
					ChildPartEvent event = new ChildPartEvent(VerticalFlowLayoutManager.this);
					m_layoutPart.handleEvent(PartConstants.s_eventChildResize, event);
				}
			});
		}
	}
	
	protected void setupSWTControls() {
		Composite parent = (Composite)m_context.getSWTControl();
		m_scrollableComposite = new ScrollableComposite(parent);
		m_scrollableComposite.setBackground(SlideUtilities.getAmbientBgcolor(m_context));
		m_canvas = new VisualPartAwareComposite(m_scrollableComposite, true);
		ScrollSizeHandler ssh = new ScrollSizeHandler();
		m_canvas.setVisualPart(ssh);
		m_scrollableComposite.setVisualPart(ssh);
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
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		String minColumnWidth = Utilities.getLiteralProperty(m_prescription, LayoutConstants.s_minColumnWidth, m_partDataSource);
		if (minColumnWidth != null) {
			m_minColumnWidth = Integer.parseInt(minColumnWidth);
		}
		String maxWidth = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_maxWidth, m_partDataSource);
		if (maxWidth != null) {
			m_maxWidth = Integer.parseInt(maxWidth);
		}
		m_drawSeparatorLine = Utilities.checkBooleanProperty(m_prescription, LayoutConstants.s_drawSeparatorLine, m_partDataSource);
		
		setupSWTControls();

		m_spansAllColumnsDataSource = Utilities.getResourceProperty(m_prescription, LayoutConstants.s_spansAllColumnsDataSource, m_partDataSource);
		
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
		
		m_CSSstyle = new CSSstyle();
		FontData fd = SlideUtilities.getAmbientFont(m_context).getFontData()[0];
		int fs = fd.getStyle();
		m_CSSstyle.setAttribute("font-family", fd.getName());
		m_CSSstyle.setAttribute("font-size", fd.getHeight());
		m_CSSstyle.setAttribute("font-style", (fs & SWT.ITALIC) == 0 ? "normal" : "italic");
		m_CSSstyle.setAttribute("font-weight", (fs & SWT.BOLD) == 0 ? "normal" : "bold");
		m_CSSstyle.setAttribute("color", SlideUtilities.getAmbientColor(m_context));	
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

//		reshuffle(m_innerWidth);
//		layout();
//		m_scrollableComposite.layout(false);
//		m_canvas.redraw();
		
//		asyncRedraw();
		asyncReshuffle();
		
		return false;
	}

	protected void prepareContextMenu(MouseEvent e) {
		removeContextOperations();

		IPart part = (IPart)hittest(e.x, e.y, false);
		if (part != null) {
			Element e2 = (Element) m_childParts.get(part);
			if (e2 != null) {
				int i = m_elements.indexOf(e2);
				createRemoveContextOperation(e2.m_underlying, i);
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

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragExit(DropTargetEvent)
	 */
	protected boolean onDragExit(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertionRectangle != null) {
			PartUtilities.repaint(m_insertionRectangle, m_context);
			m_insertionRectangle = null;
		}
		return true;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDragEnter(DropTargetEvent)
	 */
	protected boolean onDragHover(edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		Rectangle oldRect = m_insertionRectangle;
		hittestInsertionPoint(event.m_x, event.m_y);
		//System.out.println(">> " + m_insertionLayoutIndex + " " + m_insertionColumn + " " + m_insertionRowInColumn);
		if (m_insertionRectangle == null || !m_insertionRectangle.equals(oldRect)) {
			if (oldRect != null) {
				PartUtilities.repaint(oldRect, m_context);
			}
			if (m_insertionRectangle != null) {
				PartUtilities.repaint(m_insertionRectangle, m_context);
			}
		}
		return super.onDragEnter(event);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onChildHasHandledDragAndDropEvent(Resource, DropTargetEvent)
	 */
	protected void onChildHasHandledDragAndDropEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		if (m_insertionRectangle != null) {
			PartUtilities.repaint(m_insertionRectangle, m_context);
			m_insertionRectangle = null;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDropResourceList(List, DropTargetEvent)
	 */
	protected boolean onDropResourceList(List resourceList, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		m_insertionRectangle = null;
		try {
			Resource resourceToDrop = (Resource) resourceList.get(0);
			
			if (m_supportsListInsertion) {
				for (int i = 0; i < m_elements.size(); i++) {
					Element element = (Element) m_elements.get(i);
					
					if (element.m_underlying.equals(resourceToDrop)) {
						if (m_insertionIndex == i) {
							return true;
						} else {
							/*m_listDataProviderWrapper.requestRemoval(i, 1);
							if (i < m_insertionIndex) {
								m_insertionIndex--;
							}
							break;*/
						}
					}
				}
			}
						
			if (m_insertionIndex != -1 && m_supportsListInsertion) {
				m_listDataProviderWrapper.requestAddition(resourceToDrop, m_insertionIndex);
			} else if (m_supportsSetAddition) {
				m_listDataProviderWrapper.getDataProvider().requestChange(DataConstants.SET_ADDITION, resourceToDrop);
			}
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch in drop", e);
		} catch (DataNotAvailableException e) {
			s_logger.error("Data not available in drop", e);
		}
		return false;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.LayoutManagerBase#onDropResources(java.util.List, DropTargetEvent)
	 */
	protected boolean onDropResources(List resources, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		m_insertionRectangle = null;
		try {
			if (m_insertionIndex != -1 && m_supportsListInsertion) {
				m_listDataProviderWrapper.requestAdditions(resources, m_insertionIndex);
			} else if (m_supportsSetAddition) {
				HashSet set = new HashSet();
				
				set.addAll(resources);
				
				m_listDataProviderWrapper.getDataProvider().requestChange(DataConstants.SET_ADDITION, set);
			}
			return true;
		} catch (DataMismatchException e) {
			s_logger.error("Data mismatch in drop", e);
		} catch (DataNotAvailableException e) {
			s_logger.error("Data not available in drop", e);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		
		setupSWTControls();
		
		Iterator i = m_elements.iterator();
		while (i.hasNext()) {
			Element e = (Element) i.next();
			if (e.m_part != null) {
				e.m_childContext.setSWTControl(m_canvas);
				e.initializeFromDeserialization(source);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.parts.layout.LayoutManagerBase#onChildResize(edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent event) {
		asyncReshuffle();
		return true;
	}
	
	public VerticalFlowLayoutManager(){
		m_redrawRunnable = new IdleRunnable() {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				layout();
				m_canvas.redraw();
			}
		};

		m_reshuffleRunnable = new IdleRunnable() {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				reshuffle(m_innerWidth);
				layout();
				asyncRedraw();
			}
		}; 
	}
}
