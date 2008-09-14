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

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ControlPart;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * Standard text editing part.
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class EditPart extends ControlPart {
	transient Text 		m_text;
	String 				m_textString;
	transient Color		m_color;
	Resource	m_resOnChange;
	transient Font		m_oldFont;
	
	boolean		m_assertTextOnChange = true;
	
	int			m_width = -1;
	int			m_height = -1;
	int			m_heightAboveBaseLine = -1;
	
	static final int s_margin = 2;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(EditPart.class);

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		m_runnable = new OnChangeRunnable();
		initializeWidget();
	}
	
	class OnChangeRunnable extends IdleRunnable {
		OnChangeRunnable() {
			super(m_context);
		}
		
		boolean	m_dispatched = false;
		
		public synchronized void run() {
			m_dispatched = false;
			if (!hasExpired()) {
				Interpreter i = Ozone.getInterpreter();
				DynamicEnvironment denv = new DynamicEnvironment(m_source);
				Ozone.initializeDynamicEnvironment(denv, m_context);

				try {
					i.callMethod(m_resOnChange, new Object[] { EditPart.this }, denv);
				} catch (Exception e) {
					s_logger.error("Error calling method onChange " + m_resOnChange, e);
				}
			}
		}
		
		public synchronized void onChange() {
			if (!m_dispatched) {
				Ozone.idleExec(this);
				m_dispatched = true;
			}
		}
	}
	
	transient OnChangeRunnable m_runnable = new OnChangeRunnable();
	
	protected boolean m_multiline;
	protected boolean m_wrap;
	/**
	 * @see ControlPart#dispose()
	 */
	public void dispose() {
		m_text.setFont(m_oldFont);
		SlideUtilities.releaseAmbientProperties(m_context);
		
		m_text = null;
		m_color = null;
		
		m_runnable.expire();
		m_runnable = null;
		
		m_resOnChange = null;
		
		super.dispose();
	}

	/**
	 * @see VisualPartBase#getInitializationData()
	 */
	protected void getInitializationData() {
		super.getInitializationData();
		
		m_resOnChange = Utilities.getResourceProperty(m_prescription, PartConstants.s_onChange, m_partDataSource);
	}
	
	protected void initializeWidget() {
		Composite parent = (Composite) m_context.getSWTControl();
	
		Font font = SlideUtilities.getAmbientFont(m_context);
		FontData fd = font.getFontData()[0];
		int fs = fd.getStyle();
		m_CSSstyle.setAttribute("font-family", fd.getName());
		m_CSSstyle.setAttribute("font-size", fd.getHeight());
		m_CSSstyle.setAttribute("font-style", (fs & SWT.ITALIC) == 0 ? "normal" : "italic");
		m_CSSstyle.setAttribute("font-weight", (fs & SWT.BOLD) == 0 ? "normal" : "bold");
		
		m_text = new Text(parent, SWT.FLAT | SWT.NO_BACKGROUND | (m_multiline ? SWT.MULTI : 0) | (m_wrap ? SWT.WRAP | SWT.V_SCROLL : 0));
		m_oldFont = m_text.getFont();
		m_text.setFont(font);
		m_textString = "";
		
		m_color = SlideUtilities.getAmbientColor(m_context);
		//m_CSSstyle.setAttribute("color", m_color);
		//Hack for now
		m_CSSstyle.setAttribute("color", "black");
		s_logger.info("Setting INPUT color to black");
		m_text.setForeground(m_color);
			
		FontMetrics	fontMetrics = Ozone.getFontMetrics(font);
		
		m_height = m_text.computeSize(-1, -1).y + 2 * s_margin; // fontMetrics.getHeight() + 2 * s_margin;
		m_heightAboveBaseLine = (m_height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent() + fontMetrics.getLeading();
		
		m_control = m_text;

		m_text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent me) {
				try {
					m_textString = m_text.getText();

					m_source.replace(
						m_prescription,
						m_assertTextOnChange ? PartConstants.s_text : PartConstants.s_dynamicText,
						null,
						new Literal(m_textString)
					);
					
					if (m_resOnChange != null) {
						m_runnable.onChange();
					}
				} catch (Exception e) {
					s_logger.error("Error processing text modify event", e);
				}
			}
		});
		
		if (m_resOnEnterPressed != null) {
			m_text.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					if (ke.character == '\r') {
						onEnterPressed();
					}
				}
			});
		}
	}

	/**
	 * @see VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		m_multiline = Utilities.checkBooleanProperty(m_prescription, PartConstants.s_multiline, m_partDataSource);
		m_wrap = Utilities.checkBooleanProperty(m_prescription, PartConstants.s_wrap, m_partDataSource);

		initializeWidget();
		
		String s;
		{
			s = Utilities.getLiteralProperty(m_prescription, PartConstants.s_text, m_source);
			if (s == null) {
				s = Ozone.getIndirectLiteralProperty(m_prescription, PartConstants.s_text, m_source, m_context);
				m_assertTextOnChange = false;
				if (s != null) {
					try {
						m_source.replace(
							m_prescription,
							m_assertTextOnChange ? PartConstants.s_text : PartConstants.s_dynamicText,
							null,
							new Literal(s)
						);
					} catch(RDFException e) {
					}
				}
			}
			if (s != null) {
				m_text.setText(s);
				m_textString = s;
			}
		}
		s = Utilities.getLiteralProperty(m_prescription, PartConstants.s_protected, m_partDataSource);
		if (s != null && s.equalsIgnoreCase("true")) {
			m_text.setEchoChar('*');
		}
		s = Utilities.getLiteralProperty(m_prescription, PartConstants.s_width, m_partDataSource);
		if (s != null) {
			m_width = Integer.parseInt(s);
		}
		s = Utilities.getLiteralProperty(m_prescription, PartConstants.s_height, m_partDataSource);
		if (s != null) {
			m_height = Integer.parseInt(s);
		}
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		m_text.setBounds(
			r.x + s_margin, 
			r.y + s_margin, 
			(m_width != -1 ? m_width : r.width) - 2 * s_margin, 
			Math.min(m_height, r.height) - 2 * s_margin
		);
		m_text.setVisible(true);
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (!r.equals(m_text.getBounds())) {
			m_text.setBounds(new Rectangle(r.x + s_margin, r.y + s_margin, (m_width != -1 ? m_width : r.width) - 2 * s_margin, r.height - 2 * s_margin));
		}
		m_text.setVisible(true);
		
		Color	background = gc.getBackground();
		Color	foreground = gc.getForeground();
		int	lineStyle = gc.getLineStyle();
		int	lineWidth = gc.getLineWidth();
		
		int width = (m_width != -1 ? m_width : r.width) - 1;
		int height = Math.min(m_height, r.height) - 1;
		
		gc.setForeground(m_text.getBackground());
		gc.setBackground(m_text.getBackground());
		gc.fillRectangle(r.x + 1, r.y + 1, width - 1, height - 1);
		
		gc.setForeground(m_color);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.drawRectangle(r.x, r.y, width, height);
		
		gc.setLineStyle(lineStyle);
		gc.setLineWidth(lineWidth);
		gc.setBackground(background);
		gc.setForeground(foreground);
	}

	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine)
	 */
	public void renderHTML(HTMLengine he) {
		he.editor(m_textString, m_CSSstyle, this, "EditPart");
	}
	
	public String getContent() {
		return m_text.getText();
	}

	public void setContent(String str) {
		m_textString = str;
		m_text.setText(str);
	}
	
	public void setContentAsync(String str) {
		if (Ozone.isUIThread()) {
			setContent(str);
		} else {
			if (m_context == null) return;
			Ozone.idleExec(new setContentRunnable(str));
		}
	}
	
	class setContentRunnable extends IdleRunnable {
		String m_str;
		setContentRunnable(String str) {
			super(m_context);
			m_str = str;
		}
		public void run() {	setContent(m_str); }
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		if (m_width != -1) {
			hintedWidth = m_width;
		} else if (hintedWidth == -1) {
			hintedWidth = 200;
		}
		
		return new BlockScreenspace(hintedWidth, m_height, BlockScreenspace.ALIGN_TEXT_BASE_LINE, m_heightAboveBaseLine);
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_BASE_LINE;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#onGotInputFocus(org.eclipse.swt.events.FocusEvent)
	 */
	protected boolean onGotInputFocus(FocusEvent e) {
		if (m_text != null && !m_text.isDisposed()) {
			m_text.setFocus();
		} 
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.VisualPartBase#onLostInputFocus(org.eclipse.swt.events.FocusEvent)
	 */
	protected boolean onLostInputFocus(FocusEvent e) {
		return true;
	}

}
