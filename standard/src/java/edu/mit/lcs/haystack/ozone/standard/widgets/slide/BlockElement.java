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

import java.io.IOException;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Control;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class BlockElement extends SingleChildContainerPartBase implements IBlockGUIHandler {
	/*
	 *	Dimensions
	 */
	protected int					m_width					= -1;
	protected int					m_height				= -1;
	protected int					m_minWidth				= -1;
	protected int					m_minHeight				= -1;
	protected int					m_maxWidth				= -1;
	protected int					m_maxHeight				= -1;
	protected boolean				m_fillParentWidth		= false;
	protected boolean				m_fillParentHeight		= false;
	protected boolean				m_cropChildWidth		= false;
	protected boolean				m_cropChildHeight		= false;
	protected boolean				m_stretchChildWidth		= false;
	protected boolean				m_stretchChildHeight	= false;
	
	/*
	 *	Background
	 */
	transient protected Image		m_image = null;
	transient protected Color 		m_bgcolor = null;
	protected boolean				m_repeatX = true;
	protected boolean				m_repeatY = true;
	protected int					m_bgAlignX = SlideConstants.ALIGN_LEFT;
	protected int					m_bgAlignY = SlideConstants.ALIGN_TOP;
	protected boolean				m_bgcolorSpecified;
	
	/*
	 *	Margins
	 */
	protected int					m_marginLeft;
	protected int					m_marginRight;
	protected int					m_marginTop;
	protected int					m_marginBottom;
	
	/*
	 *	Clearance
	 */
	protected int					m_clearanceLeft;
	protected int					m_clearanceRight;
	protected int					m_clearanceTop;
	protected int					m_clearanceBottom;

	/*
	 *	Borders
	 */	
	protected int					m_borderLeftWidth;
	transient protected Color		m_borderLeftColor;
	protected int					m_borderRightWidth;
	transient protected Color		m_borderRightColor;
	protected int					m_borderTopWidth;
	transient protected Color		m_borderTopColor;
	protected int					m_borderBottomWidth;
	transient protected Color		m_borderBottomColor;
	
	protected boolean				m_borderLeftShadow;
	protected boolean				m_borderRightShadow;
	protected boolean				m_borderTopShadow;
	protected boolean				m_borderBottomShadow;
	transient protected Color[]		m_borderShadowColors;
	
	protected boolean				m_dropShadow;
	transient protected Color[]		m_dropShadowColors;
	
	/*
	 * Miscellany
	 */
	
	transient protected Color		m_color;
	protected int					m_textAlign = -1;
	
	protected Point					m_queriedSize;
	protected BlockScreenspace		m_queriedBS;
	protected BlockScreenspace		m_queriedChildBS;
	
	protected Rectangle				m_rect = new Rectangle(0, 0, 0, 0);
	protected Rectangle				m_childRect = new Rectangle(0, 0, 0, 0);
	
	protected boolean				m_highlighted = false;

	static protected boolean		s_fillParent = true;
	static protected boolean		s_cropChild = false;
	static protected boolean		s_stretchChild = true;
	static protected int			s_dropShadowThickness = 3;
	static protected int			s_borderShadowThickness = 5;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(BlockElement.class);

	public static void writeColor(java.io.ObjectOutputStream out, Color color) throws IOException {
		out.writeObject(color == null ? null : color.getRGB());
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		writeColor(out, m_bgcolor);
		writeColor(out, m_borderLeftColor);
		writeColor(out, m_borderRightColor);
		writeColor(out, m_borderTopColor);
		writeColor(out, m_borderBottomColor);

		if (m_borderShadowColors == null) {
			out.writeInt(0);
		} else {
			out.writeInt(m_borderShadowColors.length);
			for (int i = 0; i < m_borderShadowColors.length; i++) {
				writeColor(out, m_borderShadowColors[i]);
			}
		}

		if (m_dropShadowColors == null) {
			out.writeInt(0);
		} else {
			out.writeInt(m_dropShadowColors.length);
			for (int i = 0; i < m_dropShadowColors.length; i++) {
				writeColor(out, m_dropShadowColors[i]);
			}
		}
	}

	transient private RGB m_bgcolorRGB;
	transient private RGB m_borderLeftColorRGB;
	transient private RGB m_borderRightColorRGB;
	transient private RGB m_borderTopColorRGB;
	transient private RGB m_borderBottomColorRGB;
	transient private RGB[] m_borderShadowColorRGBs;
	transient private RGB[] m_dropShadowColorRGBs;
	protected Resource m_resBack;

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		m_bgcolorRGB = (RGB) in.readObject();
		m_borderLeftColorRGB = (RGB) in.readObject();
		m_borderRightColorRGB = (RGB) in.readObject();
		m_borderTopColorRGB = (RGB) in.readObject();
		m_borderBottomColorRGB = (RGB) in.readObject();
		
		int c = in.readInt();
		if (c > 0) {
			m_borderShadowColorRGBs = new RGB[c];
			for (int i = 0; i < c; i++) {
				m_borderShadowColorRGBs[i] = (RGB) in.readObject();
			}
		}
		
		c = in.readInt();
		if (c > 0) {
			m_dropShadowColorRGBs = new RGB[c];
			for (int i = 0; i < c; i++) {
				m_dropShadowColorRGBs[i] = (RGB) in.readObject();
			}
		}
	}

	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_bgcolorRGB != null) {		
			m_bgcolor = GraphicsManager.acquireColorByRGB(m_bgcolorRGB);
		}
		
		if (m_borderLeftColorRGB != null) {		
			m_borderLeftColor = GraphicsManager.acquireColorByRGB(m_borderLeftColorRGB);
		}
		
		if (m_borderRightColorRGB != null) {		
			m_borderRightColor = GraphicsManager.acquireColorByRGB(m_borderRightColorRGB);
		}
		
		if (m_borderTopColorRGB != null) {		
			m_borderTopColor = GraphicsManager.acquireColorByRGB(m_borderTopColorRGB);
		}
		
		if (m_borderBottomColorRGB != null) {		
			m_borderBottomColor = GraphicsManager.acquireColorByRGB(m_borderBottomColorRGB);
		}
		
		if (m_borderShadowColorRGBs != null) {		
			m_borderShadowColors = new Color[m_borderShadowColorRGBs.length];
			for (int i = 0; i < m_borderShadowColorRGBs.length; i++) {
				m_borderShadowColors[i] = GraphicsManager.acquireColorByRGB(m_borderShadowColorRGBs[i]);
			}
		}

		if (m_dropShadowColorRGBs != null) {		
			m_dropShadowColors = new Color[m_dropShadowColorRGBs.length];
			for (int i = 0; i < m_dropShadowColorRGBs.length; i++) {
				m_dropShadowColors[i] = GraphicsManager.acquireColorByRGB(m_dropShadowColorRGBs[i]);
			}
		}

		if (m_resBack == null) {
			return;
		}
		
		try {
			m_image = new Image(Ozone.s_display, ContentClient.getContentClient(m_resBack, m_partDataSource, m_context.getServiceAccessor()).getContent());
		} catch (Exception e) {
			s_logger.error("Failed to load background image " + m_resBack.getURI());
		}
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		
		GraphicsManager.releaseColor(m_bgcolor);
		GraphicsManager.releaseColor(m_borderLeftColor);
		GraphicsManager.releaseColor(m_borderRightColor);
		GraphicsManager.releaseColor(m_borderTopColor);
		GraphicsManager.releaseColor(m_borderBottomColor);
		
		if (m_dropShadowColors != null) {
			for (int i = 0; i < s_dropShadowThickness; i++) {
				GraphicsManager.releaseColor(m_dropShadowColors[i]);
				m_dropShadowColors[i] = null;
			}
			m_dropShadowColors = null;
		}

		if (m_borderShadowColors != null) {
			for (int i = 0; i < s_borderShadowThickness; i++) {
				GraphicsManager.releaseColor(m_borderShadowColors[i]);
				m_borderShadowColors[i] = null;
			}
			m_borderShadowColors = null;
		}

		m_bgcolor = null;
		m_borderLeftColor = null;
		m_borderRightColor = null;
		m_borderTopColor = null;
		m_borderBottomColor = null;
		
		if (m_image != null) {
			m_image.dispose();
			m_image = null;
		}
		
		super.dispose();
	}
	
	protected void internalInitialize() {
		super.internalInitialize();

		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		getMarginProperties();
		getClearanceProperties();
		getDimensionProperties();
		getBackgroundProperties();
		getBorderProperties();
		getOtherProperties();
		initializeChild();
	}
	
	protected void getMarginProperties() {
		String	s;
		int	i;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_margin, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginLeft = i;
				m_marginRight = i;
				m_marginTop = i;
				m_marginBottom = i;
				m_CSSstyle.setAttribute("margin", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginX, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginLeft = i;
				m_marginRight = i;
				m_CSSstyle.setAttribute("margin-left", i);
				m_CSSstyle.setAttribute("margin-right", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginY, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginTop = i;
				m_marginBottom = i;
				m_CSSstyle.setAttribute("margin-top", i);
				m_CSSstyle.setAttribute("margin-bottom", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginLeft, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginLeft = i;
				m_CSSstyle.setAttribute("margin-left", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginRight, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginRight = i;
				m_CSSstyle.setAttribute("margin-right", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginTop, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginTop = i;
				m_CSSstyle.setAttribute("margin-top", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_marginBottom, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_marginBottom = i;
				m_CSSstyle.setAttribute("margin-bottom", i);
			}
		}
	}

	protected void getClearanceProperties() {
		String	s;
		int	i;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearance, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceLeft = i;
				m_clearanceRight = i;
				m_clearanceTop = i;
				m_clearanceBottom = i;
				m_CSSstyle.setAttribute("padding", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearanceX, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceLeft = i;
				m_clearanceRight = i;
				m_CSSstyle.setAttribute("padding-left", i);
				m_CSSstyle.setAttribute("padding-left", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearanceY, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceTop = i;
				m_clearanceBottom = i;
				m_CSSstyle.setAttribute("padding-top", i);
				m_CSSstyle.setAttribute("padding-bottom", i);
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearanceLeft, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceLeft = i;
				m_CSSstyle.setAttribute("padding-left", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearanceRight, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceRight = i;
				m_CSSstyle.setAttribute("padding-right", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearanceTop, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceTop = i;
				m_CSSstyle.setAttribute("padding-top", i);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_clearanceBottom, m_partDataSource);
		if (s != null) {
			i = Integer.parseInt(s);
			if (i > 0) {
				m_clearanceBottom = i;
				m_CSSstyle.setAttribute("padding-bottom", i);
			}
		}
	}

	protected void getDimensionProperties() {
		String 		s;
		boolean	b;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_width, m_partDataSource);
		if (s != null) {
			m_width = Math.max(-1, Integer.parseInt(s));
			m_CSSstyle.setAttribute("width", m_width);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_height, m_partDataSource);
		if (s != null) {
			m_height = Math.max(-1, Integer.parseInt(s));
			m_CSSstyle.setAttribute("height", m_height);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_minWidth, m_partDataSource);
		if (s != null) {
			m_minWidth = Math.max(-1, Integer.parseInt(s));
			m_CSSstyle.setAttribute("min-width", m_minWidth);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_minHeight, m_partDataSource);
		if (s != null) {
			m_minHeight = Math.max(-1, Integer.parseInt(s));
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_maxWidth, m_partDataSource);
		if (s != null) {
			m_maxWidth = Math.max(-1, Integer.parseInt(s));
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_maxHeight, m_partDataSource);
		if (s != null) {
			m_maxHeight = Math.max(-1, Integer.parseInt(s));
		}

		m_fillParentWidth = m_fillParentHeight = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_fillParent, m_partDataSource, s_fillParent);
		m_fillParentWidth = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_fillParentWidth, m_partDataSource, m_fillParentWidth);
		m_fillParentHeight = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_fillParentHeight, m_partDataSource, m_fillParentHeight);
		
		m_cropChildWidth = m_cropChildHeight = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_cropChild, m_partDataSource, s_cropChild);
		m_cropChildWidth = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_cropChildWidth, m_partDataSource, m_cropChildWidth);
		m_cropChildHeight = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_cropChildHeight, m_partDataSource, m_cropChildHeight);
		
		m_stretchChildWidth = m_stretchChildHeight = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_stretchChild, m_partDataSource, s_stretchChild);
		m_stretchChildWidth = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_stretchChildWidth, m_partDataSource, m_stretchChildWidth);
		m_stretchChildHeight = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_stretchChildHeight, m_partDataSource, m_stretchChildHeight);
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_textAlign, m_partDataSource);
		if (s != null) {
			m_CSSstyle.setAttribute("text-align", s);
			if (s.equalsIgnoreCase("top")) {
				m_textAlign = BlockScreenspace.ALIGN_LINE_TOP;
			} else if (s.equalsIgnoreCase("bottom")) {
				m_textAlign = BlockScreenspace.ALIGN_LINE_BOTTOM;
			} else if (s.equalsIgnoreCase("baseline")) {
				m_textAlign = BlockScreenspace.ALIGN_TEXT_BASE_LINE;
			} 
			// other values are "center" in ui/collectionView.ad, ui/im.ad, ui/taskPane.ad
			//                  "false" in ui/viewNavigator.ad
		}
	}
	
	protected void getBorderProperties() {
		String	s;
		int	borderWidth;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderWidth, m_partDataSource);
		if (s != null) {
			borderWidth = Integer.parseInt(s);
			
			if (borderWidth > 0) {
				m_borderLeftWidth = borderWidth;
				m_borderRightWidth = borderWidth;
				m_borderTopWidth = borderWidth;
				m_borderBottomWidth = borderWidth;
				m_CSSstyle.setAttribute("border-width", borderWidth);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderXWidth, m_partDataSource);
		if (s != null) {
			borderWidth = Integer.parseInt(s);
			
			if (borderWidth > 0) {
				m_borderLeftWidth = borderWidth;
				m_borderRightWidth = borderWidth;
				m_CSSstyle.setAttribute("border-left-width", borderWidth);
				m_CSSstyle.setAttribute("border-right-width", borderWidth);
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderYWidth, m_partDataSource);
		if (s != null) {
			borderWidth = Integer.parseInt(s);
			
			if (borderWidth > 0) {
				m_borderTopWidth = borderWidth;
				m_borderBottomWidth = borderWidth;
				m_CSSstyle.setAttribute("border-top-width", borderWidth);
				m_CSSstyle.setAttribute("border-bottom-width", borderWidth);
			}
		}

		String	commonColor = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderColor, m_partDataSource);
		Color	ambientColor = SlideUtilities.getAmbientColor(m_context);
		
		String colorX = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderXColor, m_partDataSource);
		if (colorX == null) {
			colorX = commonColor;
		}
		String colorY = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderYColor, m_partDataSource);
		if (colorY == null) {
			colorY = commonColor;
		}
		
		String color;

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderLeftWidth, m_partDataSource);
		if (s != null) {
			m_borderLeftWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-left-width", m_borderLeftWidth);
		}
		if (m_borderLeftWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderLeftColor, m_partDataSource);
			m_borderLeftColor = GraphicsManager.acquireColor(color != null ? color : colorX, ambientColor);
			m_CSSstyle.setAttribute("border-left-color", m_borderLeftColor);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderRightWidth, m_partDataSource);
		if (s != null) {
			m_borderRightWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-right-width", m_borderRightWidth);
		}
		if (m_borderRightWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderRightColor, m_partDataSource);
			m_borderRightColor = GraphicsManager.acquireColor(color != null ? color : colorX, ambientColor);
			m_CSSstyle.setAttribute("border-right-color", m_borderRightColor);
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderTopWidth, m_partDataSource);
		if (s != null) {
			m_borderTopWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-top-width", m_borderTopWidth);
		}
		if (m_borderTopWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderTopColor, m_partDataSource);
			m_borderTopColor = GraphicsManager.acquireColor(color != null ? color : colorY, ambientColor);
			m_CSSstyle.setAttribute("border-top-color", m_borderTopColor);
			
		}

		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderBottomWidth, m_partDataSource);
		if (s != null) {
			m_borderBottomWidth = Math.max(0, Integer.parseInt(s));
			m_CSSstyle.setAttribute("border-bottom-width", m_borderBottomWidth);
		}
		if (m_borderBottomWidth > 0) {
			color = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderBottomColor, m_partDataSource);
			m_borderBottomColor = GraphicsManager.acquireColor(color != null ? color : colorY, ambientColor);
			m_CSSstyle.setAttribute("border-bottom-color", m_borderBottomColor);
		}
		
		if (m_borderLeftWidth > 0 || m_borderRightWidth > 0 ||m_borderTopWidth > 0 || m_borderBottomWidth > 0)
			m_CSSstyle.setAttribute("border-style", "solid");
		
		boolean b = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderShadow, m_partDataSource);
		m_borderLeftShadow = m_borderRightShadow = m_borderTopShadow = m_borderBottomShadow = b;
		
		m_borderLeftShadow = m_borderRightShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderXShadow, m_partDataSource, m_borderLeftShadow);
		m_borderLeftShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderLeftShadow, m_partDataSource, m_borderLeftShadow);
		m_borderRightShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderRightShadow, m_partDataSource, m_borderRightShadow);

		m_borderTopShadow = m_borderBottomShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderXShadow, m_partDataSource, m_borderTopShadow);
		m_borderTopShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderTopShadow, m_partDataSource, m_borderTopShadow);
		m_borderBottomShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_borderBottomShadow, m_partDataSource, m_borderBottomShadow);
		
		if (m_borderLeftShadow || m_borderRightShadow || m_borderTopShadow || m_borderBottomShadow) {
			Color c = SlideUtilities.getAmbientBgcolor(m_context);
			
			m_borderShadowColors = new Color[s_borderShadowThickness];
			for (int i = 0; i < s_borderShadowThickness; i++) {
				double scale = 1 - ((s_borderShadowThickness - i) * 0.3 / s_borderShadowThickness);
				
				m_borderShadowColors[i] = GraphicsManager.acquireColorByRGB(new RGB(
					(int) (c.getRed() * scale),
					(int) (c.getGreen() * scale),
					(int) (c.getBlue() * scale)
				));
			}
		}

		m_dropShadow = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_dropShadow, m_partDataSource);
		if (m_dropShadow) {
			Color c = SlideUtilities.getAmbientBgcolor(m_context.getParentContext());
			
			m_dropShadowColors = new Color[s_dropShadowThickness];
			for (int i = 0; i < s_dropShadowThickness; i++) {
				double scale = 1 - ((s_dropShadowThickness - i) * 0.2 / s_dropShadowThickness);
				
				m_dropShadowColors[i] = GraphicsManager.acquireColorByRGB(new RGB(
					(int) (c.getRed() * scale),
					(int) (c.getGreen() * scale),
					(int) (c.getBlue() * scale)
				));
			}
		}
	}
	
	protected void getBackgroundProperties() {
		String s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_bgcolor, m_partDataSource);
		if (s != null) {
			m_bgcolor = GraphicsManager.acquireColor(s, SlideUtilities.getAmbientBgcolor(m_context));
		} else {
			m_bgcolor = GraphicsManager.acquireColorBySample((Color) m_context.getUnchainedProperty(SlideUtilities.s_ambientBgcolor));
			m_bgcolorSpecified = true;
		}
		if (m_bgcolor != null)
			m_CSSstyle.setAttribute("background-color", m_bgcolor);
		m_resBack = Utilities.getResourceProperty(m_prescription, SlideConstants.s_background, m_partDataSource);
		
		if (m_resBack == null) {
			return;
		}
		// TODO: Use GraphicsManager to cache background image as for other images
		m_CSSstyle.setAttribute("background-image", "url(\"" + m_resBack.getURI().replaceAll("http://haystack.lcs.mit.edu/", "") + "\")");
		
		try {
			m_image = new Image(Ozone.s_display, ContentClient.getContentClient(m_resBack, m_partDataSource, m_context.getServiceAccessor()).getContent());
		} catch (Exception e) {
			s_logger.error("Failed to load background image " + m_resBack.getURI());
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_backgroundRepeat, m_partDataSource);
		if (s != null) {
			if (s.equalsIgnoreCase("x")) {
				m_repeatY = false;
				m_CSSstyle.setAttribute("background-repeat", "repeat-x");
			} else if (s.equalsIgnoreCase("y")) {
				m_repeatX = false;
				m_CSSstyle.setAttribute("background-repeat", "repeat-y");
			} else if (s.equalsIgnoreCase("none")) {
				m_repeatX = false;
				m_repeatY = false;
				m_CSSstyle.setAttribute("background-repeat", "no-repeat");
			} else {
				m_CSSstyle.setAttribute("background-repeat", "repeat");
			}
		}
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_backgroundAlignX, m_partDataSource);
		if (s != null) {
			if (s.equalsIgnoreCase("right")) {
				m_bgAlignX = SlideConstants.ALIGN_RIGHT;
			} else if (s.equalsIgnoreCase("center")) {
				m_bgAlignX = SlideConstants.ALIGN_CENTER;
				m_repeatX = false;
			}
		}
				
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_backgroundAlignY, m_partDataSource);
		if (s != null) {
			if (s.equalsIgnoreCase("bottom")) {
				m_bgAlignY = SlideConstants.ALIGN_BOTTOM;
			} else if (s.equalsIgnoreCase("center")) {
				m_bgAlignY = SlideConstants.ALIGN_CENTER;
				m_repeatY = false;
			}
		}
	}
	
	protected void getOtherProperties() {
		String s;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_borderLeftColor, m_partDataSource);
		Color	ambientColor = SlideUtilities.getAmbientColor(m_context);
		m_color = GraphicsManager.acquireColor(s, ambientColor);
		m_CSSstyle.setAttribute("color", m_color);
	}
	
	protected void initializeChild() {
		createChild();
		if (m_child != null) {
			m_gh = m_child.getGUIHandler(IBlockGUIHandler.class);
		}
	}
	
	protected boolean internalContentHittest(ContentHittestEvent e) {
		return
			(
				m_borderLeftWidth > 0 && 
				m_borderRightWidth > 0 && 
				m_borderTopWidth > 0 && 
				m_borderBottomWidth > 0
			) ||
			m_bgcolorSpecified;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHighlight(EventObject, boolean)
	 */
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		if (internalContentHittest(null)) {
			m_highlighted = event.m_highlight;
			PartUtilities.repaint(m_rect, m_context);
			return true;
		} else if (m_child != null) {
			return m_child.handleEvent(PartConstants.s_eventContentHighlight, event);
		}
		return false;
	}
	
	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle rect) {
		if (!rect.equals(m_rect)) {
			setBounds(rect);
		}
		
		drawBackgroundColor(gc);
		drawBackgroundImage(gc);
		drawBorders(gc);

		if (m_gh instanceof IBlockGUIHandler) {
			((IBlockGUIHandler) m_gh).draw(gc, m_childRect);
		}
		
		drawBoundingRect(gc);
	}

	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enterSpan("BlockElement", m_CSSstyle, this, m_tooltip);
		if (m_gh instanceof IBlockGUIHandler) ((IBlockGUIHandler)m_gh).renderHTML(he);
		he.exitSpan("BlockElement");
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (m_queriedSize != null && m_queriedSize.x == hintedWidth && m_queriedSize.y == hintedHeight) {
			return new BlockScreenspace(m_queriedBS);
		} else {
			m_queriedSize = new Point(hintedWidth, hintedHeight);
			
			BlockScreenspace bs = null;
			
			if (m_gh instanceof IBlockGUIHandler) {
				IBlockGUIHandler bgh = (IBlockGUIHandler) m_gh;
				
				if (bgh.getHintedDimensions() == IBlockGUIHandler.FIXED_SIZE) {
					bs = new BlockScreenspace(bgh.getFixedSize());
				} else {
					Point p = calculateQuery(hintedWidth, hintedHeight);
					bs = bgh.calculateSize(p.x, p.y);
				}
			}
			
			if (bs == null) {
				bs = new BlockScreenspace();
			}
			
			m_queriedChildBS = new BlockScreenspace(bs);
			
			adjustQueriedBS(bs, hintedWidth, hintedHeight);
			
			m_queriedBS = new BlockScreenspace(bs);
			
			return bs;
		}
	}

	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		if (m_queriedBS != null) {
			return new BlockScreenspace(m_queriedBS);
		} else {
			BlockScreenspace bs = null;
			
			if (m_gh instanceof IBlockGUIHandler) {
				bs = ((IBlockGUIHandler) m_gh).getFixedSize();
			}
			
			if (bs == null) {
				bs = new BlockScreenspace();
			}
			
			m_queriedChildBS = new BlockScreenspace(bs);

			adjustQueriedBS(bs, -1, -1);
		
			m_queriedBS = new BlockScreenspace(bs);
			
			return bs;
		}
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		if (getHintedDimensions() == IBlockGUIHandler.FIXED_SIZE) {
			if (m_queriedChildBS == null) {
				getFixedSize();
			}
		} else {
			if (m_queriedSize == null || 
				(m_queriedSize.x >= 0 && m_queriedSize.x != r.width) ||
				(m_queriedSize.y >= 0 && m_queriedSize.y != r.height)) {
					
				calculateSize(r.width, r.height);
			}
		}
		
		m_rect.x = r.x;
		m_rect.y = r.y;
		
		m_rect.width = r.width; //Math.min(r.width, m_queriedBS.m_size.x);
		m_rect.height = r.height; //Math.min(r.height, m_queriedBS.m_size.y);
		
		m_childRect.x = m_rect.x + m_marginLeft;
		m_childRect.y = m_rect.y + m_marginTop;
		m_childRect.width = m_rect.width - m_marginLeft - m_marginRight -  (m_dropShadow ? s_dropShadowThickness : 0);
		m_childRect.height = m_rect.height - m_marginTop - m_marginBottom -  (m_dropShadow ? s_dropShadowThickness : 0);
		
		if (!m_stretchChildWidth) {
			m_childRect.width = Math.min(m_childRect.width, m_queriedChildBS.m_size.x);
		}
		if (!m_stretchChildHeight) {
			m_childRect.height = Math.min(m_childRect.height, m_queriedChildBS.m_size.y);
		}

		if (m_gh instanceof IBlockGUIHandler) {		
			IBlockGUIHandler bgh = (IBlockGUIHandler) m_gh;
			
			bgh.setBounds(m_childRect);
		}
	}

	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		int r = IBlockGUIHandler.FIXED_SIZE;
		
		if (m_gh instanceof IBlockGUIHandler) {
			r = ((IBlockGUIHandler) m_gh).getHintedDimensions();
		}
		return r;
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		if (m_gh instanceof IBlockGUIHandler) {
			return ((IBlockGUIHandler) m_gh).getTextAlign();
		}
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
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
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onChildResize(edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		m_queriedSize = null;
		m_queriedBS = null;
		m_queriedChildBS = null;
		return super.onChildResize(e);
	}

	protected void drawBackgroundColor(GC gc) {
		if (m_bgcolor != null) {
			Color background = gc.getBackground();
		
			gc.setBackground(m_bgcolor);
			gc.fillRectangle(
				m_rect.x, m_rect.y,
				m_rect.width - (m_dropShadow ? s_dropShadowThickness : 0),
				m_rect.height - (m_dropShadow ? s_dropShadowThickness : 0)
			);
			
			gc.setBackground(background);
		}
	}
	
	protected void drawBackgroundImage(GC gc) {
		if (m_image == null) {
			return;
		}
		
		Region 		region = new Region();
		gc.getClipping(region);
		
		Rectangle	imageSize = m_image.getBounds();
		Rectangle	regionBounds = region.getBounds().intersection(m_rect);
		Rectangle	tile = new Rectangle(0, 0, 0, 0);
		Rectangle	cropped;
		int		xStart;
		int		xEnd;
		int		xChange;
		int		yStart;
		int		yEnd;
		int		yChange;
		
		if (m_bgAlignX == SlideConstants.ALIGN_LEFT) {
			xStart = m_rect.x;
			xChange = 1;
			xEnd = m_rect.x + m_rect.width - (m_dropShadow ? s_dropShadowThickness : 0);
		} else if (m_bgAlignX == SlideConstants.ALIGN_RIGHT) {
			xStart = m_rect.x + m_rect.width - imageSize.width - (m_dropShadow ? s_dropShadowThickness : 0);
			xChange = -1;
			xEnd = m_rect.x - imageSize.width;
		} else {
			xStart = m_rect.x + (m_rect.width - imageSize.width) / 2;
			xChange = 1;
			xEnd = xStart;
		}
		
		if (m_bgAlignY == SlideConstants.ALIGN_TOP) {
			yStart = m_rect.y;
			yChange = 1;
			yEnd = m_rect.y + m_rect.height - (m_dropShadow ? s_dropShadowThickness : 0);
		} else if (m_bgAlignY == SlideConstants.ALIGN_RIGHT) {
			yStart = m_rect.y + m_rect.height - imageSize.height - (m_dropShadow ? s_dropShadowThickness : 0);
			yChange = -1;
			yEnd = m_rect.y - imageSize.height;
		} else {
			yStart = m_rect.y + (m_rect.height - imageSize.height) / 2;
			yChange = 1;
			yEnd = yStart;
		}
		
		tile.width = imageSize.width; tile.height = imageSize.height;
		
		tile.x = xStart;
		while (true) {
			tile.y = yStart;
				
			while (true) {
				cropped = tile.intersection(regionBounds);
				
				if (cropped.width > 0 && cropped.height > 0) {
					try {
						gc.drawImage(
							m_image,
							cropped.x - tile.x,
							cropped.y - tile.y,
							cropped.width,
							cropped.height,
							cropped.x,
							cropped.y,
							cropped.width,
							cropped.height
						);
					} catch (Exception e) {
					}
				}
				
				if (!m_repeatY) {
					break;
				}
				
				tile.y = tile.y + yChange * imageSize.height;
				if ((yEnd - tile.y) * yChange < 0) {
					break;
				}
			}
			
			if (!m_repeatX) {
				break;
			}
			
			tile.x = tile.x + xChange * imageSize.width;
			if ((xEnd - tile.x) * xChange < 0) {
				break;
			}
		}
		
		region.dispose();
	}
	
	protected void drawBorders(GC gc) {
		Rectangle rect = new Rectangle(
			m_rect.x, m_rect.y,
			m_rect.width - (m_dropShadow ? s_dropShadowThickness : 0),
			m_rect.height - (m_dropShadow ? s_dropShadowThickness : 0)
		);
		
		Color	bkColor = gc.getBackground();
		int 	lineStyle = gc.getLineStyle();
		int 	lineWidth = gc.getLineWidth();
		Color	foreground = gc.getForeground();
		
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		
		if (m_borderLeftWidth > 0) {
			gc.setBackground(m_borderLeftColor);
			gc.fillRectangle(rect.x, rect.y, m_borderLeftWidth, rect.height);
		}
		if (m_borderRightWidth > 0) {
			gc.setBackground(m_borderRightColor);
			gc.fillRectangle(rect.x + rect.width - m_borderRightWidth, rect.y, m_borderRightWidth, rect.height);
		}
		if (m_borderTopWidth > 0) {
			gc.setBackground(m_borderTopColor);
			gc.fillRectangle(rect.x, rect.y, rect.width, m_borderTopWidth);
		}
		if (m_borderBottomWidth > 0) {
			gc.setBackground(m_borderBottomColor);
			gc.fillRectangle(rect.x, rect.y + rect.height - m_borderBottomWidth, rect.width, m_borderBottomWidth);
		}
		
		if (m_dropShadow) {
			for (int i = 0; i < s_dropShadowThickness; i++) {
				int x = m_rect.x + m_rect.width - s_dropShadowThickness + i;
				int y = m_rect.y + m_rect.height - s_dropShadowThickness + i;
				
				gc.setForeground(m_dropShadowColors[i]);
				
				gc.drawLine(m_rect.x + 1 + i, y, x, y);
				gc.drawLine(x, m_rect.y + 1 + i, x, y);
			}
		}
		
		if (m_borderTopShadow) {
			for (int i = 0; i < s_borderShadowThickness; i++) {
				int x = m_rect.x + (m_borderLeftShadow ? i : 0);
				int x2 = m_rect.x + m_rect.width - 1 - (m_borderRightShadow ? i : 0);
				int y = m_rect.y + i;
				
				gc.setForeground(m_borderShadowColors[i]);
				
				gc.drawLine(x, y, x2, y);
			}
		}
		
		if (m_borderBottomShadow) {
			for (int i = 0; i < s_borderShadowThickness; i++) {
				int x = m_rect.x + (m_borderLeftShadow ? i : 0);
				int x2 = m_rect.x + m_rect.width - 1 - (m_borderRightShadow ? i : 0);
				int y = m_rect.y + m_rect.height - 1 - i;
				
				gc.setForeground(m_borderShadowColors[i]);
				
				gc.drawLine(x, y, x2, y);
			}
		}
			
		if (m_borderLeftShadow) {
			for (int i = 0; i < s_borderShadowThickness; i++) {
				int x = m_rect.x + i;
				int y = m_rect.y + (m_borderTopShadow ? i : 0);
				int y2 = m_rect.y + m_rect.height - 1 - (m_borderTopShadow ? i : 0);
				
				gc.setForeground(m_borderShadowColors[i]);
				
				gc.drawLine(x, y, x, y2);
			}
		}
		
		if (m_borderRightShadow) {
			for (int i = 0; i < s_borderShadowThickness; i++) {
				int x = m_rect.x + m_rect.width - 1 - i;
				int y = m_rect.y + (m_borderTopShadow ? i : 0);
				int y2 = m_rect.y + m_rect.height - 1 - (m_borderTopShadow ? i : 0);
				
				gc.setForeground(m_borderShadowColors[i]);
				
				gc.drawLine(x, y, x, y2);
			}
		}
		
		gc.setLineStyle(lineStyle);
		gc.setLineWidth(lineWidth);
		gc.setForeground(foreground);
		gc.setBackground(bkColor);
	}
	
	protected void cacheBounds(Rectangle r) {
		m_rect.x = r.x; m_rect.y = r.y; m_rect.width = r.width; m_rect.height = r.height;
	}
	
	protected Point calculateQuery(int hintedWidth, int hintedHeight) {
		if (! (m_gh instanceof IBlockGUIHandler)) {
			return new Point(0, 0);
		}
		
		if (m_width != -1) {
			hintedWidth = m_width;
		} else {
			hintedWidth = Math.max(hintedWidth, m_minWidth);
		}

		if (m_height != -1) {
			hintedHeight = m_height;
		} else {
			hintedHeight = Math.max(hintedHeight, m_minHeight);
		}

		if (hintedWidth >= 0) {
			hintedWidth -= m_marginLeft + m_marginRight + (m_dropShadow ? s_dropShadowThickness : 0);
		}
		if (hintedHeight >= 0) {
			hintedHeight -= m_marginTop + m_marginBottom + (m_dropShadow ? s_dropShadowThickness : 0);
		}
		
		return new Point(hintedWidth, hintedHeight);
	}
	
	protected void adjustQueriedBS(BlockScreenspace bs, int hintedWidth, int hintedHeight) {
		bs.m_size.x += m_marginLeft + m_marginRight + (m_dropShadow ? s_dropShadowThickness : 0);
		bs.m_size.y += m_marginTop + m_marginBottom + (m_dropShadow ? s_dropShadowThickness : 0);
		bs.m_alignOffset += m_marginTop;
		
		if (m_width != -1) {
			bs.m_size.x = m_width;
		} else {
			if (m_fillParentWidth && hintedWidth >= 0) {
				bs.m_size.x = Math.max(bs.m_size.x, hintedWidth);
			}
			if (m_cropChildWidth && hintedWidth >= 0) {
				bs.m_size.x = Math.min(bs.m_size.x, hintedWidth);
			}
			if (m_maxWidth != -1 && bs.m_size.x > m_maxWidth) {
				bs.m_size.x = m_maxWidth;
			}
		}
		
		if (m_height != -1) {
			bs.m_size.y = m_height;
		} else {
			if (m_fillParentHeight && hintedHeight >= 0) {
				bs.m_size.y = Math.max(bs.m_size.y, hintedHeight);
			}
			if (m_cropChildHeight && hintedHeight >= 0) {
				bs.m_size.y = Math.min(bs.m_size.y, hintedHeight);
			}
			if (m_maxHeight != -1 && bs.m_size.y > m_maxHeight) {
				bs.m_size.y = m_maxHeight;
			}
		}
		
		bs.m_clearanceLeft 		= m_marginLeft > 0 		? m_clearanceLeft 	: Math.max(bs.m_clearanceLeft, m_clearanceLeft);
		bs.m_clearanceRight 	= m_marginRight > 0 	? m_clearanceRight 	: Math.max(bs.m_clearanceRight, m_clearanceRight);
		bs.m_clearanceTop 		= m_marginTop > 0 		? m_clearanceTop	: Math.max(bs.m_clearanceTop, m_clearanceTop);
		bs.m_clearanceBottom 	= m_marginBottom > 0 	? m_clearanceBottom	: Math.max(bs.m_clearanceBottom, m_clearanceBottom);
		
		if (m_textAlign != -1) {
			bs.m_align = m_textAlign;
		}
	}
	
	protected void drawBoundingRect(GC gc) {
		if (m_highlighted) {
			Rectangle r;
			
			if (m_dropShadow) {
				r = new Rectangle(m_rect.x, m_rect.y, m_rect.width - s_borderShadowThickness + 1, m_rect.height - s_borderShadowThickness + 1);
			} else {
				r = new Rectangle(m_rect.x, m_rect.y, m_rect.width - 1, m_rect.height - 1);
			}
			
			boolean	xor = gc.getXORMode();
			int		lineStyle = gc.getLineStyle();
			int		lineWidth = gc.getLineWidth();
			Color		color = gc.getForeground();

			gc.setXORMode(true);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.setLineWidth(1);
			gc.setForeground(SlideUtilities.getAmbientColor(m_context));

			gc.drawRectangle(r.x, r.y, r.width - 1, r.height - 1);

			gc.setXORMode(xor);
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			gc.setForeground(color);
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase#hittestChild(int, int)
	 */
	protected boolean hittestChild(int x, int y) {
		return m_childRect.contains(x, y);
	}

	public Rectangle getBounds() {
		Control c = (Control) m_context.getSWTControl();
		Point	p = c.toDisplay(new Point(m_rect.x, m_rect.y));
		
		return new Rectangle(p.x, p.y, m_rect.width, m_rect.height);
	}
}
