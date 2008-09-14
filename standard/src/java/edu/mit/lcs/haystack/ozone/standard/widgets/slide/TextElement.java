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

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IInlineGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.ITextFlowCounter;
import edu.mit.lcs.haystack.ozone.core.ITextSpan;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.Connector;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHighlightEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.*;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.util.List;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class TextElement extends VisualPartBase implements IInlineGUIHandler, IBlockGUIHandler {
	
	transient Font				m_font;
	transient Color				m_color;
	transient Color				m_shadowColor;
	transient Color				m_hoverColor;
	
	String				m_text;
	String				m_defaultText;
	String				m_parsedText;
	ArrayList			m_fragments = null;
	List				m_spanSet = null;
	Rectangle			m_bounds = null;
	
	int				m_maxLines = -1;
	boolean			m_wrap = true;
	boolean			m_hovered = false;
	int				m_dropShadowThickness;
	boolean			m_highlightBackground = false;
	
	Connector			m_connector;
	
	IDataConsumer		m_dataConsumer;
	IDataProvider		m_dataProvider;
	
	transient IdleRunnable 				m_textChangeRunnable;
	
	static final String	s_space = " ";
	static final String	s_empty = "";
	static final String	s_ellipses = "...";
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TextElement.class);

	public TextElement() {
	}
	
	public TextElement(String text) {
		m_text = text;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_dataProvider != null) {
			m_dataProvider.initializeFromDeserialization(source);
		}
		if (m_connector != null) {
			m_connector.initializeFromDeserialization(source);
		}
		retrieveAmbientProperties();
	}

	public void setText(String text) {
		m_text = text;
		if ((!Ozone.isUIThread() || !m_initializing) && m_textChangeRunnable == null) {
			Ozone.idleExec(m_textChangeRunnable = new IdleRunnable(m_context) {
				public void run() {
					m_textChangeRunnable = null;
					if (m_context != null) {
						onChildResize(new ChildPartEvent(TextElement.this));
					}
				}
				
			});
		}
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		
		if (m_connector != null) {
			m_connector.dispose();
			m_connector = null;
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.dispose();
			m_dataProvider = null;
		}
		m_dataConsumer = null;
		
		GraphicsManager.releaseColor(m_shadowColor);
		
		m_font = null;
		m_color = null;
		m_shadowColor = null;
		m_hoverColor = null;
		m_text = null;
		m_parsedText = null;
		m_fragments = null;

		super.dispose();		
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || 
			cls.equals(IInlineGUIHandler.class) ||
			cls.equals(IBlockGUIHandler.class)) {
			return this;
		}
		return null;
	}

	/**
	 * @see IInlineGUIHandler#calculateTextFlow(ITextFlowCounter)
	 */
	public void calculateTextFlow(ITextFlowCounter tfc) {
		if (m_textChangeRunnable != null) {
			m_textChangeRunnable.expire();
		}
		
		if (m_text == null || m_text.length() == 0) {
			FontMetrics fm = Ozone.getFontMetrics(m_font);
			
			tfc.addSpan(
				0, 
				fm.getHeight(),
				BlockScreenspace.ALIGN_TEXT_BASE_LINE,
				fm.getHeight() - fm.getDescent()
			);
		} else if (m_text.equals(" ")) {
			Font oldFont = Ozone.s_gc.getFont();
			
			Ozone.s_gc.setFont(m_font);
			
			int spaceWidth = Ozone.s_gc.stringExtent(s_space).x;
			
			if (tfc.getRemainingLineLength() < spaceWidth && m_wrap) {
				tfc.addLineBreak();
			} else {
				FontMetrics fm = Ozone.s_gc.getFontMetrics();
				
				tfc.addSpan(
					spaceWidth, 
					fm.getHeight(),
					BlockScreenspace.ALIGN_TEXT_BASE_LINE,
					fm.getHeight() - fm.getDescent()
				);
			}
			
			Ozone.s_gc.setFont(oldFont);
		} else {
			m_fragments = new ArrayList();
			internalCalculateTextFlow(tfc, m_text);
		}
	}

	/**
	 * @see IInlineGUIHandler#draw(GC, List)
	 */
	public void draw(GC gc, java.util.List spanSet) {
		if (m_text != null && m_text.length() > 0 && !m_text.equals(" ")) {
			Font 		oldFont = gc.getFont();
			Color		oldForeground = gc.getForeground();
			int		lineStyle = gc.getLineStyle();
			int		lineWidth = gc.getLineWidth();
			
			Boolean hovered = (Boolean) m_context.getProperty(PartConstants.s_hovered);
			if (hovered == null) {
				hovered = new Boolean(false);
			}
			
			gc.setFont(m_font);
			Color color = m_hovered || hovered.booleanValue() ? m_hoverColor : m_color;
			if (m_hovered || hovered.booleanValue()) {
				gc.setLineStyle(SWT.LINE_SOLID);
				gc.setLineWidth(1);
			}
			
			if (m_highlightBackground) {
				Color background = gc.getBackground();
				gc.setBackground(SlideUtilities.getAmbientHighlightBgcolor(m_context));
				
				for (int i = 0; i < m_fragments.size(); i++) {
					ITextSpan	textSpan = (ITextSpan) spanSet.get(i);
					gc.fillRectangle(textSpan.getArea());
				}
				
				gc.setBackground(background);
			}	
			for (int i = 0; i < m_fragments.size(); i++) {
				String 		str = (String) m_fragments.get(i);
				ITextSpan	textSpan = (ITextSpan) spanSet.get(i);
				Rectangle	r = textSpan.getArea();
				int 		underlineY = r.y + textSpan.getAlignOffset() + 1;
				
				if (m_dropShadowThickness > 0) {
					gc.setForeground(m_shadowColor);
					
					for (int j = 1; j <= m_dropShadowThickness; j++) {
						gc.drawString(str, r.x + j, r.y + j, true);
						if (m_hovered) {
							gc.drawLine(r.x + j, underlineY + j, r.x + r.width + j, underlineY + j);
						}
					}
				}
								
				gc.setForeground(color);
				gc.drawString(str, r.x, r.y, true);
				if (m_hovered) {
					gc.drawLine(r.x, underlineY, r.x + r.width - 1, underlineY);
				}
			}
						
			gc.setFont(oldFont);
			gc.setForeground(oldForeground);
			if (m_hovered || hovered.booleanValue()) {
				gc.setLineStyle(lineStyle);
				gc.setLineWidth(lineWidth);
			}
		}
	}
	
	/**
	 * @see IInlineGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		if (m_text != null && m_text.length() > 0 && !m_text.equals(" ")) {
			retrieveAmbientProperties();
			he.text(m_text, m_CSSstyle, this, m_tooltip, "TextElement");
		}
	}
	
	protected void retrieveAmbientProperties() {
		m_font = SlideUtilities.getAmbientFont(m_context);
		FontData fd = m_font.getFontData()[0];
		int fs = fd.getStyle();
		m_CSSstyle.setAttribute("font-family", fd.getName());
		m_CSSstyle.setAttribute("font-size", fd.getHeight());
		m_CSSstyle.setAttribute("font-style", (fs & SWT.ITALIC) == 0 ? "normal" : "italic");
		m_CSSstyle.setAttribute("font-weight", (fs & SWT.BOLD) == 0 ? "normal" : "bold");
		
		boolean dropShadow = SlideUtilities.getAmbientTextDropShadow(m_context);
		if (dropShadow) {
			m_dropShadowThickness = (fs & SWT.BOLD) != 0 ? 2 : 1;
		}

		if (m_resOnClick != null || m_context.getProperty(PartConstants.s_hovered) != null) {
			m_color = SlideUtilities.getAmbientLinkColor(m_context);
		} else {
			m_color = SlideUtilities.getAmbientColor(m_context);
		}
		m_CSSstyle.setAttribute("color", m_color);
		m_hoverColor = SlideUtilities.getAmbientLinkHoverColor(m_context);
		m_shadowColor = GraphicsManager.acquireColor("80%", SlideUtilities.getAmbientBgcolor(m_context));
	}

	/**
	 * Does the actual initialization work.
	 */	
	boolean m_initializing = true;
	protected void internalInitialize() {
		super.internalInitialize();
		
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		retrieveText();

		retrieveAmbientProperties();
				
		String s;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_maxLines, m_partDataSource);
		if (s != null) {
			m_maxLines = Integer.parseInt(s);
			if (m_maxLines < 1) {
				m_maxLines = -1;
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_wrap, m_partDataSource);
		if (s != null) {
			m_wrap = s.equalsIgnoreCase("true");
		}
		
		Resource resConnector = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_connector, m_partDataSource);
		if (resConnector != null) {
			Context	childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, resConnector);
			
			m_connector = new Connector() {
				public void onChange() {
					handleTextChange();
				}
			};
			m_connector.initialize(m_source, childContext);
			
			handleTextChange();
		}

		m_initializing = false;
	}
	
	protected void retrieveText() {
		String s;
		
		s = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_text, m_partDataSource);
		if (s != null) {
			m_text = s;
		}
		m_defaultText = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_defaultText, m_partDataSource);
		
		if (m_text == null) {
			m_text = m_defaultText;
			
			Resource dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (dataSource == null) {
				return;
			}
			
			m_dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
			if (m_dataProvider == null) {
				return;
			}
			
			m_dataConsumer = new IDataConsumer() {
				public void reset() {
				}

				public void onStatusChanged(Resource status) {
				}

				public void onDataChanged(Resource changeType, Object change)
					throws IllegalArgumentException {
						
					if (
						changeType.equals(DataConstants.STRING_CHANGE) ||
						changeType.equals(DataConstants.LITERAL_CHANGE) ||
						changeType.equals(DataConstants.BOOLEAN_CHANGE) ||
						changeType.equals(DataConstants.INTEGER_CHANGE)) {
							
						onStringChanged(change instanceof Literal ? ((Literal) change).getContent() : change.toString());
					} else if (
						changeType.equals(DataConstants.STRING_DELETION) ||
						changeType.equals(DataConstants.LITERAL_DELETION) ||
						changeType.equals(DataConstants.INTEGER_DELETION)) {
						onStringDeleted();
					} else {
						throw new IllegalArgumentException("Cannot handle change of type " + changeType);
					}
				}

				protected void onStringChanged(String newString) {
					m_text = newString;
					if (!Ozone.isUIThread() || !m_initializing) {
						Ozone.idleExec(new IdleRunnable(m_context) {
							public void run() {
								m_bounds = null;
								onChildResize(new ChildPartEvent(TextElement.this));
							}
							
						});
					}
				}
				
				protected void onStringDeleted() {
					m_text = m_defaultText;
					if (!Ozone.isUIThread() || !m_initializing) {
						Ozone.idleExec(new IdleRunnable(m_context) {
							public void run() {
								m_bounds = null;
								onChildResize(new ChildPartEvent(TextElement.this));
							}
							
						});
					}
				}
			};
			m_dataProvider.registerConsumer(m_dataConsumer);
		}
	}
	
	protected void internalCalculateTextFlow(ITextFlowCounter tfc, String str) {
		Font oldFont = Ozone.s_gc.getFont();
		
		Ozone.s_gc.setFont(m_font);
		
		FontMetrics	fontMetrics = Ozone.s_gc.getFontMetrics();

		int 	spaceWidth = Ozone.s_gc.stringExtent(s_space).x;
		int 	height = fontMetrics.getHeight();
		int 	heightAboveTextBaseLine = fontMetrics.getAscent() + fontMetrics.getLeading(); //height - fontMetrics.getDescent();

		if (m_wrap) {
			// Tokenize the string
			StringTokenizer stLines = new StringTokenizer(str, "\n", true);
			while (stLines.hasMoreTokens()) {
				String			line = stLines.nextToken();
				
				if (line.equals("\n")) {
					tfc.addLineBreak();
					continue;
				}
				
				StringTokenizer	st = new StringTokenizer(line);
				ArrayList		tokens = new ArrayList();
				ArrayList		tokenWidths = new ArrayList();
				
				while (st.hasMoreTokens()) {
					String s = st.nextToken().replace('\b', ' ');
					if (s.length() > 0) {
						tokens.add(s);
						tokenWidths.add(new Integer(Ozone.s_gc.stringExtent(s).x));
					}
				}
		
				while (tokens.size() > 0) {
					String 	currentSegment = "";
					int	currentSegmentWidth = 0;			
					
					while (tokens.size() > 0) {
						int tokenWidth = ((Integer) tokenWidths.get(0)).intValue();

						if (currentSegmentWidth == 0) {
							if (tokenWidth + m_dropShadowThickness > tfc.getRemainingLineLength()) {
								break; // Need to go on to another line
							}
						} else if (currentSegmentWidth + tokenWidth + spaceWidth + m_dropShadowThickness > tfc.getRemainingLineLength()) {
							break; // Need to go on to another line
						}
						
						if (currentSegmentWidth > 0) {
							currentSegmentWidth += spaceWidth;
							currentSegment += s_space;
						}
						currentSegmentWidth += tokenWidth;
						currentSegment += (String) tokens.get(0);
						
						tokens.remove(0);
						tokenWidths.remove(0);
					}
					
					// We can't fit any token on the current line
					if (currentSegmentWidth == 0) {
						// The current line has been used partially, we should start a new line.
						if (tfc.getRemainingLineLength() < tfc.getLineLength()) {
							tfc.addLineBreak();
							continue;
						}
						// The current line is totally new but we can't fit any token.
						// The next token must be really long! We'll put it in anyway.
						else {
							currentSegment = (String) tokens.get(0);
							currentSegmentWidth = ((Integer) tokenWidths.get(0)).intValue();
							
							tokens.remove(0);
							tokenWidths.remove(0);
						}
					}
					// Otherwise, we can fit one or more token on the current line
					
					tfc.addSpan(currentSegmentWidth + m_dropShadowThickness, height + m_dropShadowThickness, BlockScreenspace.ALIGN_TEXT_BASE_LINE, heightAboveTextBaseLine);
					if (tokens.size() > 0) {
						tfc.addLineBreak();
					}
					
					m_fragments.add(currentSegment);
				}
			}
		} else {
			if (m_parsedText == null) {
				m_parsedText = m_text.trim().replace('\b', ' ').replace('\n', ' ');				
			}
			m_fragments.add(m_parsedText);
			tfc.addSpan(Ozone.s_gc.stringExtent(m_parsedText).x + m_dropShadowThickness, height + m_dropShadowThickness, BlockScreenspace.ALIGN_TEXT_BASE_LINE, heightAboveTextBaseLine);
		}
		
		m_spanSet = tfc.getCurrentSpanSet();
		
		Ozone.s_gc.setFont(oldFont);
	}

	protected boolean showContextMenu(MouseEvent e) {
		Font oldFont = Ozone.s_gc.getFont();
		
		Ozone.s_gc.setFont(m_font);
		
		try {
			java.util.List	segmentSet = ((java.util.List) e.data);
			
			if (segmentSet != null) {
				Iterator		i = segmentSet.iterator();
				Iterator		iFragment = m_fragments.iterator();
				String			string = null;
				
				while (i.hasNext()) {
					ITextSpan	textSpan = (ITextSpan) i.next();
					String		fragment = (String) iFragment.next();
					Rectangle	area = textSpan.getArea();
					
					if (e.y - area.y < area.height) {
						StringTokenizer	st = new StringTokenizer(fragment.replace('\b', ' '));
						int 			spaceWidth = Ozone.s_gc.stringExtent(s_space).x;
						int				offset = e.x - area.x;
						
						while (st.hasMoreTokens()) {
							String	token = st.nextToken();
							int 	tokenWidth = Ozone.s_gc.stringExtent(token).x + spaceWidth;
							
							if (offset < tokenWidth) {
								string = token;
								break;
							}
							
							offset -= tokenWidth;
						}
				
						break;
					}
				}
				
				if (string == null) {
					string = m_text;
				} else {
					int j = 0;
					while (j < string.length() && Character.isLetterOrDigit(string.charAt(j)))
					{
						j++;
					}
					string = string.substring(0, j);
				}
				
				Resource word = Utilities.generateUniqueResource();
				
				Context childContext = new Context(m_context);
				
				childContext.putLocalProperty(OzoneConstants.s_underlying, word);
				
				Control control = (Control) m_context.getSWTControl();
				Point	point = control.toDisplay(new Point(e.x, e.y));
				
				PartUtilities.showContextMenu(m_source, childContext, point);
				
				Ozone.s_gc.setFont(oldFont);
				
				return true;
			}
		} catch (Exception ex) {
			Ozone.s_gc.setFont(oldFont);

			s_logger.error("Failed to show context menu", ex);
		}
		
		return super.showContextMenu(e);
	}

	protected boolean onMouseEnter(MouseEvent e) {
		if (m_resOnClick != null) {
			m_hovered = true;
			redraw();
		}
		return super.onMouseEnter(e);
	}

	protected boolean onMouseExit(MouseEvent e) {
		if (m_resOnClick != null) {
			m_hovered = false;
			redraw();
		}
		return super.onMouseExit(e);
	}
	
	protected boolean onContentHighlight(ContentHighlightEvent event) {
		m_highlightBackground = event.m_highlight;
		redraw();
		return true;
	}

	protected void redraw() {
		Control c = (Control) m_context.getSWTControl();
		if (m_spanSet != null) {
			Iterator i = m_spanSet.iterator();
			while (i.hasNext()) {
				ITextSpan	textSpan = (ITextSpan) i.next();
				Rectangle	area = textSpan.getArea();
				
				c.redraw(area.x, area.y, area.width, area.height, true);
			}
		} else if (m_bounds != null) {
			c.redraw(m_bounds.x, m_bounds.y, m_bounds.width, m_bounds.height, true);
		}
	}
	
	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		if (m_textChangeRunnable != null) {
			m_textChangeRunnable.expire();
		}
		
		if (m_text == null) {
			return new BlockScreenspace(BlockScreenspace.ALIGN_TEXT_BASE_LINE);
		} else {
			Font oldFont = Ozone.s_gc.getFont();
			
			Ozone.s_gc.setFont(m_font);
			Point p = Ozone.s_gc.stringExtent(m_text);
			
			FontMetrics fontMetrics = Ozone.s_gc.getFontMetrics();
			
			Ozone.s_gc.setFont(oldFont);
			
			return new BlockScreenspace(p.x + m_dropShadowThickness, p.y + m_dropShadowThickness, 
				BlockScreenspace.ALIGN_TEXT_BASE_LINE, fontMetrics.getHeight() - fontMetrics.getDescent());
		}
	}

	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.FIXED_SIZE;
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
		if (m_textChangeRunnable != null) {
			m_textChangeRunnable.expire();
		}
		
		if (m_text != null && m_text.length() > 0) {
			internalCalculateTextBounds(r);
		}
	}

	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		return null;
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (m_text != null && m_text.length() > 0) {
			internalCalculateTextBounds(r);
			
			Font 		oldFont = gc.getFont();
			Color		oldForeground = gc.getForeground();
			int		lineStyle = gc.getLineStyle();
			int		lineWidth = gc.getLineWidth();
				
			Boolean hovered = (Boolean) m_context.getProperty(PartConstants.s_hovered);
			if (hovered == null) {
				hovered = new Boolean(false);
			}
			
			gc.setFont(m_font);
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setLineWidth(1);
			
			FontMetrics fm = gc.getFontMetrics();
			int 		underlineY = m_bounds.y + fm.getHeight() - fm.getDescent() + 1;
			
			if (m_highlightBackground) {
				Color background = gc.getBackground();
				gc.setBackground(SlideUtilities.getAmbientHighlightBgcolor(m_context));
				
				gc.fillRectangle(m_bounds);
				
				gc.setBackground(background);
			}
			
			if (m_dropShadowThickness > 0) {
				gc.setForeground(m_shadowColor);
				
				for (int j = 1; j <= m_dropShadowThickness; j++) {
					gc.drawString(m_parsedText, m_bounds.x + j, m_bounds.y + j, true);
					if (m_hovered) {
						gc.drawLine(m_bounds.x + j, underlineY + j, m_bounds.x + j + m_bounds.width, underlineY + j);
					}
				}
			}
			
			gc.setForeground(m_hovered || hovered.booleanValue() ? m_hoverColor : m_color);
			gc.drawString(m_parsedText, m_bounds.x, m_bounds.y, true);
			if (m_hovered) {
				gc.drawLine(m_bounds.x, underlineY, m_bounds.x + m_bounds.width - 1, underlineY);
			}
			
			gc.setLineStyle(lineStyle);
			gc.setLineWidth(lineWidth);
			gc.setFont(oldFont);
			gc.setForeground(oldForeground);
		}
	}
	
	protected void handleTextChange() {
		try {
			m_text = ((Literal) m_connector.getContext().getProperty(SlideConstants.s_text)).getContent();
		
			onChildResize(new ChildPartEvent(this));
		} catch (Exception e) {
		}
	}
	
	protected void internalCalculateTextBounds(Rectangle r) {
		if (!r.equals(m_bounds)) {
			String	text = new String(m_text);
			Point	p;
			Font	oldFont = Ozone.s_gc.getFont();
			
			Ozone.s_gc.setFont(m_font);
			p = Ozone.s_gc.stringExtent(text);
			
			if (p.x > r.width) {
				int ellipsesLength = Ozone.s_gc.stringExtent(s_ellipses).x;
				
				while (true && text.length() > 1) {
					text = text.substring(0, text.length() - 2);
					
					p = Ozone.s_gc.stringExtent(text);
					p.x += ellipsesLength;
					
					if (p.x <= r.width) {
						text += s_ellipses;
						break;
					}
				}
			}
			
			Ozone.s_gc.setFont(oldFont);
			
			int 		alignY = SlideUtilities.getAmbientAlignY(m_context);
			int 		alignX = SlideUtilities.getAmbientAlignX(m_context);
			Point		pOrigin = new Point(r.x, r.y);
			
			switch (alignY) {
			case SlideConstants.ALIGN_BOTTOM:
				pOrigin.y = r.y + r.height - p.y; break;
			case SlideConstants.ALIGN_CENTER:
				pOrigin.y = r.y + (r.height - p.y) / 2; break;
			}
			
			switch (alignX) {
			case SlideConstants.ALIGN_RIGHT:
				pOrigin.x = r.x + r.width - p.x; break;
			case SlideConstants.ALIGN_CENTER:
				pOrigin.x = r.x + (r.width - p.x) / 2; break;
			}

			if (m_bounds == null) {
				m_bounds = new Rectangle(0, 0, 0, 0);
			}			
			m_bounds.x = pOrigin.x;
			m_bounds.y = pOrigin.y;
			m_bounds.width = p.x;
			m_bounds.height = p.y;
			
			m_parsedText = text;
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHittest(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onContentHittest(ContentHittestEvent e) {
		return true;
	}
}
