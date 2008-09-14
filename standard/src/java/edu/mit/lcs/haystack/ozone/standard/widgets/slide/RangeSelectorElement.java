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

package edu.mit.lcs.haystack.ozone.standard.widgets.slide;

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import java.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author		Vineet Sinha
 */
public class RangeSelectorElement extends VisualPartBase implements IBlockGUIHandler {
	Color m_color = null;
	int m_fontSize = -1;
	Resource m_resColl = null;
	Resource m_resPredicate = null;

	int m_numEntries = 0;
	MapEntry[] m_dataEntries = null;

	protected Color m_thumbColor = null;
	Rectangle m_bounds = new Rectangle(0, 0, 0, 0);

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(RangeSelectorElement.class);

	interface IConverter {
		public Object literalToObj(Literal lit);
		public long objToLong(Object obj);
		public Object longToObj(long relVal);
	}

	IConverter m_converter = null;

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);

		GraphicsManager.releaseColor(m_thumbColor);
		m_thumbColor = null;

		m_color = null;
		super.dispose();
	}

	/**
	 * Retrieves part data and then foreground color of line.
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		m_color = SlideUtilities.getAmbientColor(m_context);
		m_fontSize = SlideUtilities.getAmbientFontSize(m_context);

		m_thumbColor = GraphicsManager.acquireColor("65%", SlideUtilities.getAmbientColor(m_context));

		m_resColl = Utilities.getResourceProperty(m_prescription, DataConstants.SUBJECT, m_partDataSource);
		m_resPredicate = Utilities.getResourceProperty(m_prescription, DataConstants.PREDICATE, m_partDataSource);
		Resource resType =
			Utilities.getResourceProperty(
				m_prescription,
				new Resource("http://haystack.lcs.mit.edu/ui/navigationView#rangeType"),
				m_partDataSource);

		if (resType == null) {
			s_logger.error("No type");
		} else if (resType.equals(Constants.s_xsd_dateTime)) {
			s_logger.info("Using date converter!");
			m_converter = new IConverter() {
				public Object literalToObj(Literal lit) {
					return Utilities.parseDateTime(lit);
				}
				public long objToLong(Object obj) {
					return ((Date) obj).getTime();
				}
				public Object longToObj(long relVal) {
					return new Date(relVal);
				}
			};
		} else if (resType.equals(Constants.s_xsd_int)) {
			s_logger.info("Using int converter!");
			m_converter = new IConverter() {
				public Object literalToObj(Literal lit) {
					return new Integer(lit.getContent());
				}
				public long objToLong(Object obj) {
					return ((Integer) obj).longValue();
				}
				public Object longToObj(long relVal) {
					return new Integer((int) relVal);
				}
			};
		} else {
			s_logger.error("Unknown type: " + resType);
		}

		// we need to do m_resColl hs:member ?x ?x m_resPredicate ?y @(?y)
		//m_dataStrings = Utilities.getProperties(m_resColl, m_resPredicate, m_infoSource);
		try {
			Set results =
				m_infoSource.query(
					new Statement[] {
						new Statement(m_resColl, Constants.s_haystack_member, Utilities.generateWildcardResource(1)),
						new Statement(
							Utilities.generateWildcardResource(1),
							m_resPredicate,
							Utilities.generateWildcardResource(2))},
					new Resource[] { Utilities.generateWildcardResource(2)},
					Utilities.generateWildcardResourceArray(2));

			m_dataEntries = new MapEntry[results.size() + 2];
			Iterator i = results.iterator();
			int j = 0;
			while (i.hasNext()) {
				Literal curStr = (Literal) ((RDFNode[]) i.next())[0];
				m_dataEntries[j++] = new MapEntry(curStr, m_converter.literalToObj(curStr));
			}

			long maxVal = m_converter.objToLong(m_dataEntries[0].getValue());
			long minVal = m_converter.objToLong(m_dataEntries[0].getValue());
			for (int k = 1; k < j; k++) {
				long curVal = m_converter.objToLong(m_dataEntries[k].getValue());
				if (minVal > curVal) {
					minVal = curVal;
				}
				if (maxVal < curVal) {
					maxVal = curVal;
				}
			}
			m_dataEntries[j++] = new MapEntry(new Literal("minStr"), m_converter.longToObj((long) (minVal - (maxVal - minVal) * 0.01)));
			m_dataEntries[j++] = new MapEntry(new Literal("maxStr"), m_converter.longToObj((long) (maxVal + (maxVal - minVal) * 0.01)));

		} catch (Exception e) {
			m_dataEntries = new MapEntry[0];
			s_logger.error("Exception while trying to get range values!", e);
		}

		Arrays.sort(m_dataEntries, MapEntry.getValueComparator());
		m_numEntries = m_dataEntries.length;
	}

	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int xConstraint, int yConstraint) {
		return new BlockScreenspace(xConstraint, m_fontSize, BlockScreenspace.ALIGN_TEXT_CLEAR, 0);
	}

	private int[] getThumbPoly(int deltaX, int deltaY, int side) {
		deltaX += side / 2; // so that none of the values below are negative

		int thumb[] = new int[5 * 2];
		thumb[0 * 2 + 0] = 0;
		thumb[0 * 2 + 1] = 0;
		thumb[1 * 2 + 0] = side / 2;
		thumb[1 * 2 + 1] = side / 2;
		thumb[2 * 2 + 0] = side / 2;
		thumb[2 * 2 + 1] = side;
		thumb[3 * 2 + 0] = -side / 2;
		thumb[3 * 2 + 1] = side;
		thumb[4 * 2 + 0] = -side / 2;
		thumb[4 * 2 + 1] = side / 2;

		for (int p = 0; p < thumb.length; p += 2) {
			thumb[p + 0] += deltaX;
		}

		for (int p = 0; p < thumb.length; p += 2) {
			thumb[p + 1] += deltaY;
		}

		return thumb;
	}

	private Rectangle getThumbRect(int deltaX, int deltaY, int side) {
		return new Rectangle(deltaX, deltaY, side, side);
	}

	// relative co-ordinate to m_bounds
	int m_leftSliderPos = 0;
	int m_rghtSliderPos = -1;

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (!r.equals(m_bounds)) {
			m_bounds.x = r.x;
			m_bounds.y = r.y;
			m_bounds.width = r.width;
			m_bounds.height = r.height;
		}

		int style = gc.getLineStyle();
		int width = gc.getLineWidth();
		Color color = gc.getForeground();
		Color background = gc.getBackground();

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setForeground(m_color);
		gc.setBackground(m_thumbColor);

		// possibly move lower by r.height/5
		// possibly move right
		gc.drawLine(m_bounds.x, m_bounds.y + 1, m_bounds.x + m_bounds.width, m_bounds.y + 1);

		if (m_dataEntries != null && m_numEntries > 0) {
			long start = m_converter.objToLong(m_dataEntries[0].getValue());
			long lineGap = m_converter.objToLong(m_dataEntries[m_numEntries - 1].getValue()) - start;
			for (int i = 0; i < m_numEntries; i++) {
				long cur = m_converter.objToLong(m_dataEntries[i].getValue());
				int pos = (int) (m_bounds.width * (cur - start) / lineGap);
				gc.drawLine(m_bounds.x + pos, m_bounds.y, m_bounds.x + pos, m_bounds.y + m_bounds.height / 2);
			}
		}

		int thumb[] = getThumbPoly(m_bounds.x + m_leftSliderPos, m_bounds.y, m_bounds.height);
		gc.fillPolygon(thumb);

		int thumb2[] = getThumbPoly(m_bounds.x + m_rghtSliderPos, m_bounds.y, m_bounds.height);
		gc.fillPolygon(thumb2);

		gc.setLineStyle(style);
		gc.setLineWidth(width);
		gc.setForeground(color);
		gc.setBackground(background);
	}

	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.unimplemented("Range selector ");
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
		return IBlockGUIHandler.WIDTH;
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		if (m_rghtSliderPos == -1) {
			m_rghtSliderPos = r.width / 2;
		} else {
			m_rghtSliderPos = r.width * m_rghtSliderPos / m_bounds.width;
		}

		m_bounds.x = r.x;
		m_bounds.y = r.y;
		m_bounds.width = r.width;
		m_bounds.height = r.height;
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

	protected int getIndex(int sliderPos) {
		long start = m_converter.objToLong(m_dataEntries[0].getValue());
		long end = m_converter.objToLong(m_dataEntries[m_numEntries - 1].getValue());
		long relVal = start + (end - start) * sliderPos / m_bounds.width;
		int ndx =
			Arrays.binarySearch(
				m_dataEntries,
				new MapEntry(null, m_converter.longToObj(relVal)),
				MapEntry.getValueComparator());
		if (ndx < 0) {
			ndx = (-1 * ndx) - 1;
		}
		return ndx;
	}

	protected int m_mouseDownX;

	protected void moveThumb(MouseEvent e, boolean left) {
		Control control = (Control) m_context.getSWTControl();

		MouseMoveListener mml = new MouseMoveListener() {
			Control m_control;
			boolean m_left;

			public MouseMoveListener initialize(Control control, boolean left) {
				m_control = control;
				m_left = left;
				return this;
			}

			public void mouseMove(MouseEvent e) {
				/*
				s_logger.info(
					"m_leftSliderPos: "
						+ m_leftSliderPos
						+ " m_rghtSliderPos: "
						+ m_rghtSliderPos
						+ " m_mouseDownX: "
						+ m_mouseDownX);
				*/
				int newLeftSliderPos = m_leftSliderPos;
				int newRghtSliderPos = m_rghtSliderPos;
				if (m_left) {
					newLeftSliderPos += e.x - m_mouseDownX;
				} else {
					newRghtSliderPos += e.x - m_mouseDownX;
				}

				// check invalid options
				if (newLeftSliderPos >= newRghtSliderPos - m_bounds.height) {
					return;
				}
				if (newLeftSliderPos < 0) {
					return;
				}
				if (newRghtSliderPos > m_bounds.width) {
					return;
				}

				// only update here
				m_leftSliderPos = newLeftSliderPos;
				m_rghtSliderPos = newRghtSliderPos;
				m_mouseDownX = e.x;
				m_control.redraw(m_bounds.x, m_bounds.y, m_bounds.width, m_bounds.height, /*all*/
				true);
				//s_log.info("current range: " + m_dataStrings[getIndex(m_leftSliderPos)+1] + " to " + m_dataStrings[getIndex(m_rghtSliderPos)]);
				int leftNdx = getIndex(m_leftSliderPos)-1;
				int rghtNdx = getIndex(m_rghtSliderPos)-1;
				try {
					m_partDataSource.replace(
						m_prescription,
						new Resource("http://haystack.lcs.mit.edu/ui/navigationView#leftItem"),
						null,
						(Literal) m_dataEntries[leftNdx].getKey());
					m_partDataSource.replace(
						m_prescription,
						new Resource("http://haystack.lcs.mit.edu/ui/navigationView#rightItem"),
						null,
						(Literal) m_dataEntries[rghtNdx].getKey());
					m_partDataSource.replace(
						m_prescription,
						new Resource("http://haystack.lcs.mit.edu/ui/navigationView#count"),
						null,
						new Literal((new Integer(rghtNdx - leftNdx)).toString()));
				} catch (RDFException ex) {
					s_logger.error("Unexpected exception while writing rdf", ex);
				}
			}
		}
		.initialize(control, left);

		MouseListener ml = new MouseAdapter() {
			Control m_control;
			MouseMoveListener m_mml;

			MouseListener initialize(Control control, MouseMoveListener mml) {
				m_control = control;
				m_mml = mml;
				return this;
			}

			public void mouseUp(MouseEvent me) {
				m_control.setCapture(false);
				m_control.removeMouseListener(this);
				m_control.removeMouseMoveListener(m_mml);
				//PartUtilities.setContainerControlDraggable(true, m_context);
			}
		}
		.initialize(control, mml);

		PartUtilities.setContainerControlDraggable(false, m_context);
		m_mouseDownX = e.x;

		control.setCapture(true);
		control.addMouseMoveListener(mml);
		control.addMouseListener(ml);

	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onMouseDown(MouseEvent)
	 */
	protected boolean onMouseDown(MouseEvent e) {

		// left thumb		
		if (getThumbRect(m_bounds.x + m_leftSliderPos, m_bounds.y /*2*r.height*/
			, m_bounds.height).contains(e.x, e.y)) {
			moveThumb(e, /*left*/
			true);
			return true;
		}

		// right thumb		
		if (getThumbRect(m_bounds.x + m_rghtSliderPos, m_bounds.y /*2*r.height*/
			, m_bounds.height).contains(e.x, e.y)) {
			moveThumb(e, /*left*/
			false);
			return true;
		}

		return super.onMouseDown(e);
	}

}
