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

package edu.mit.lcs.haystack.ozone.standard.editor;

import java.util.TimerTask;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.dnd.*;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.ControlPart;
import edu.mit.lcs.haystack.ozone.core.utils.DragAndDropHandler;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;

/**
 * Implements a text editor capable of editing content:Content resources.
 * @author Dennis Quan
 */
public class TextEditorPart extends ControlPart implements IViewPart {
	public final static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TextEditorPart.class);
	
	transient protected Font			m_oldFont;
	transient protected StyledText		m_text;
	transient protected TimerTask		m_changeTask = null;
	transient protected boolean			m_initializing = true;
	protected Resource			m_underlying = null;
	protected boolean           m_isEditable = true;
	
	public void dispose() {
		try {
			m_changeTask.cancel();
		} catch (Exception e) {}
		if (!m_text.isDisposed()) {
			saveNow();
			m_text.setFont(m_oldFont);
		}
		SlideUtilities.releaseAmbientProperties(m_context);
		super.dispose();
	}

	protected void saveTextAsync() {
		if (Ozone.isUIThread()) {
			saveNow();
		} else {
			if (m_context == null) {
				return;
			}
			Ozone.idleExec(new IdleRunnable(m_context) {
				/**
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					saveNow();
				}
			});
		}
	}

	protected void saveNow() {
/*		if (m_initializing) {
			return;
		}

		try {
			ContentClient cc = ContentClient.getContentClient(m_underlying, m_infoSource, m_context.getServiceAccessor());
			cc.setContent(new ByteArrayInputStream(m_text.getText().getBytes("UTF-8")));
		} catch (Exception e) {
			s_logger.error("Failed to save content for " + m_underlying, e);
		}*/
	}

	protected void cancelDelayedTextSave() {
		if (m_changeTask != null) {
			try {
				m_changeTask.cancel();
			} catch (Exception e) {}
			m_changeTask = null;
		}
	}

	protected void saveTextDelayed() {
		cancelDelayedTextSave();
		Ozone.s_timer.schedule(m_changeTask = new TimerTask() {
			/**
			 * @see java.util.TimerTask#run()
			 */
			public void run() {
				saveTextAsync();
			}
		}, 200);
	}
	
	protected void initializeViewPart() {
		try {
			ContentClient cc = ContentClient.getContentClient(m_underlying, m_infoSource, m_context.getServiceAccessor());
			String text = cc.getContentAsString();
			m_text.setText(text);
			
		} catch (Exception e) {
			s_logger.error("Failed to get content client for " + m_underlying, e);
		}
	}

	/**
	 * @see IVisualPart#draw()
	 */
	public void draw() {
	}

	/**
	 * @see IVisualPart#handleMouseEvent(int, MouseEvent)
	 */
	public boolean handleMouseEvent(int eventType, MouseEvent e) {
		return false;
	}
	
	protected void setupTextWidget() {
		Composite parent = (Composite) m_context.getSWTControl();
		m_text = new StyledText(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
		m_control = m_text;
		
		if (m_isEditable) {
			m_text.setEditable(true);
		} else {
			m_text.setEditable(false);
		}
		
		m_oldFont = m_text.getFont();
		m_text.setFont(SlideUtilities.getAmbientFont(m_context));
		m_text.setForeground(SlideUtilities.getAmbientColor(m_context));

		m_text.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent me) {
				if (me.button == 3) {
					if (!PartUtilities.showContextMenu(m_text, me, m_source, m_context)) {
						TextEditorPart.super.showContextMenu(me);
					}
				}
			}
		});
		
		m_text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent fe) {
				saveTextAsync();
			}
		});

		m_text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent me) {
				saveTextDelayed();
			}
		});
		new DragAndDropHandler() {
			protected boolean handleDropEvent(
				Resource eventType,
				edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {

				if (eventType.equals(PartConstants.s_eventDrop)) {
					boolean r = PartUtilities.performDrop(event, m_source, m_context);
					return r;
				} else {
					event.m_dropTargetEvent.detail = DND.DROP_LINK;
					return true;
				}
			}

			protected boolean isDroppable() {
				return true;
			}
		}.initialize(m_text);
	}

	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		super.initialize(source, context);

		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		setupTextWidget();
		
		m_underlying = (Resource) m_context.getProperty(OzoneConstants.s_underlying);
		initializeViewPart();
		m_initializing = false;

		Resource viewPart = (Resource)m_context.getProperty(OzoneConstants.s_ViewPart);
		/*
		String val = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_editable, m_infoSource);
		System.err.println("prescription: " + m_prescription);
		System.err.println("SlideConstants.s" + SlideConstants.s_editable);
		System.err.println("resPart" + m_resPart);
		System.err.println("partDataSource:"+m_partDataSource);
		System.err.println("context:"+m_context);
		System.err.println("got val: " + val);
		
		if (val != null) {
			if (val.equals("true")) {
				m_isEditable = true;
			} else if (val.equals("false")) {
				m_isEditable = false;
			}
		}
		*/
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		setupTextWidget();
		initializeViewPart();
		m_initializing = false;
	}
}
