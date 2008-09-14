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

import java.util.Date;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.ControlPart;
import edu.mit.lcs.haystack.ozone.core.utils.DragAndDropHandler;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.SmartDateDataProvider;
import edu.mit.lcs.haystack.ozone.data.StringDataConsumer;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class LiteralEditorPart extends ControlPart
	implements IValueEditorPart {
	transient protected Composite 	m_parent;
	transient protected StyledText 	m_text;
	transient protected Font		m_oldFont;
	protected int					m_height;
	protected boolean				m_multiline;
	protected boolean				m_editable;
	protected boolean				m_isDate = false; 
	protected Resource				m_property;
	protected String				m_textString; // for Web interface
	
	transient protected IdleRunnable	m_saveRunnable;
	protected String 					m_lastSaved = null;

	protected IDataProvider		m_dataProvider = null;
	protected boolean			m_ownsDataProvider = false;
	
	transient protected TimerTask	m_changeTask = null;
	protected boolean				m_disallowBlanks = false;

	public static final Resource	s_literalEditor = new Resource("http://haystack.lcs.mit.edu/ui/ozoneeditor#literalEditor");
	public static final Resource	s_literalEditorPart = new Resource("http://haystack.lcs.mit.edu/ui/ozoneeditor#literalEditorPart");
	public static final Resource	s_literalEditorPartData = new Resource("http://haystack.lcs.mit.edu/ui/ozoneeditor#literalEditorPartData");

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(LiteralEditorPart.class);
	
	protected void changeTextAsync(String newString) {
		Ozone.idleExec(new IdleRunnable(m_context) {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				setValue(new Literal(m_newString));
			}
						
			String m_newString;
			IdleRunnable init(String str) {
				m_newString = str;
				return this;
			}
		}.init(newString));
	}

	protected void cancelDelayedTextChange() {
		if (m_changeTask != null) {
			try {
				m_changeTask.cancel();
			} catch (Exception e) {}
		}
	}

	protected void changeTextDelayed(String str) {
		cancelDelayedTextChange();
		Ozone.s_timer.schedule(m_changeTask = new TimerTask() {
			/**
			 * @see java.util.TimerTask#run()
			 */
			public void run() {
				changeTextAsync(m_str);
			}

			String m_str;					
					
			TimerTask init(String str) {
				m_str = str;
				return this;
			}
		}.init(str), 500);
	}
	
	protected void saveTextAsync() {
		Ozone.idleExec(m_saveRunnable);
	}

	protected void cancelDelayedTextSave() {
		if (m_changeTask != null) {
			try {
				m_changeTask.cancel();
			} catch (Exception e) {}
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
		}, 100);
	}
	
	/**
	 * @see IPart#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		m_control.setBounds(new Rectangle(r.x + 1, r.y + 1, r.width - 2, r.height - 2));
	}

	public void save() {
		if (!m_editable) {
			return;
		}
				
		try {
			String text = m_text.getText();
			if (!text.equals(m_lastSaved)) { 
				cancelDelayedTextChange();
				if (m_dataProvider != null) {
					try {
						m_dataProvider.requestChange((text == null || (m_disallowBlanks && (text.length() == 0))) ? DataConstants.STRING_DELETION : DataConstants.STRING_CHANGE, text);
					} catch (DataMismatchException e) {
						s_logger.error("Failed to request change", e);
					}
				}
				m_lastSaved = text;
			}
		} catch (Exception e) {
			s_logger.error("Failed to save content", e);
		}
	}

	protected void initializeSWTPart() {
		m_parent = (Composite) m_context.getSWTControl();
		m_text = new StyledText(m_parent, SWT.MULTI | SWT.FLAT | SWT.NO_BACKGROUND | (m_multiline ? SWT.WRAP | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL : 0));
		m_text.setEditable(m_editable = false);
		m_text.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent me) {
				if (me.button == 3) {
					if (!PartUtilities.showContextMenu(m_text, me, m_source, m_context)) {
						Point	point = m_text.toDisplay(new Point(me.x, me.y));
						PartUtilities.showContextMenu(m_source, m_context, point);
					}
				}
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
		m_oldFont = m_text.getFont();
		Font font = SlideUtilities.getAmbientFont(m_context);
		FontMetrics fm = Ozone.getFontMetrics(font);
		m_height = (fm.getHeight() + 4) * (m_multiline ? 4 : 1);
		m_text.setFont(font);
		m_text.setForeground(SlideUtilities.getAmbientColor(m_context));
		m_control = m_text;
		
		m_property = (Resource) m_context.getDescendantLocalProperty(OzoneConstants.s_aspect);
		
		/* this shows the text in the same color as the background for hiding the
		 * password 
		 */
		if (Utilities.isType(m_property, Constants.s_haystack_PasswordProperty, m_source)) {
			Color bgcolor = m_text.getBackground();
			m_text.setForeground(bgcolor);
			m_text.setSelectionForeground(bgcolor);
			m_text.setSelectionBackground(bgcolor);
		}
		
		if (m_property != null) {
			try {
				m_isDate = m_source.contains(new Statement(m_property, Constants.s_rdfs_range, Constants.s_xsd_dateTime));
			} catch (RDFException e) {
			}
		}
		
		String text = Utilities.getLiteralProperty(m_prescription, PartConstants.s_text, m_partDataSource);
		if (text != null) {
			if (!m_text.getText().equals(m_lastSaved = text)) {
				m_text.setText(text);
				m_textString = text;
			}
		}
		
		m_saveRunnable = new IdleRunnable(m_context) {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				save();
			}
		};

		m_text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent me) {
				saveTextDelayed();
			}
		});
		
		m_text.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent fe) {
			}

			public void focusGained(FocusEvent fe) {
			}
		});
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
	 * @see IBlockGUIHandler#calculateSize(int,int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		Point size = new Point(200, m_height);
/*		if (hintedHeight != -1) {
			size.y = hintedHeight;
		}
*/		if (hintedWidth != -1) {
			size.x = hintedWidth;
		}
		return new BlockScreenspace(size);
	}

	public String getContent() {
		return m_text.getText();
	}

	/**
	 * @see ControlPart#dispose()
	 */
	public void dispose() {
		if (!m_text.isDisposed()) {
			m_text.setFont(m_oldFont);	
		}
		SlideUtilities.releaseAmbientProperties(m_context);

		super.dispose();
	
		if (m_ownsDataProvider && m_dataProvider != null) {
			m_dataProvider.dispose();
		}

		try {
			m_changeTask.cancel();
		} catch (Exception e) {}
	}

	/**
	 * @see IVisualPart#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (m_text.getEditable()) {
			r = m_text.getBounds();
			--r.x;
			--r.y;
			++r.width;
			++r.height;
			gc.setForeground(SlideUtilities.getAmbientColor(m_context));
			gc.drawRectangle(r);
		}
	}
	
	public void renderHTML(HTMLengine he) {
		// TODO: (HTML interface) Enable editing in LiteralEditorPart
		he.text(m_textString, m_CSSstyle, this, m_tooltip, "LiteralEditorPart");
	}

	/**
	 * @see IPropertyEditorPart#onSelection(boolean)
	 */
	public void onSelection(boolean selected) {
	}

	/**
	 * @see IValueEditorPart#getValue()
	 */
	public RDFNode getValue() {
		return new Literal(m_text.getText());
	}

	/**
	 * @see IValueEditorPart#setValue(RDFNode)
	 */
	public void setValue(RDFNode rdfn) {
		String str = rdfn.getContent();
		
		if (!m_editable && m_isDate) {
			Date d = Utilities.parseDateTime(str);
			if (d != null) {
				str = SmartDateDataProvider.makeSmartDate(d);
			}
		} 
		
		if (!m_text.getText().equals(str)) {
			m_text.setText(m_lastSaved = str);
			m_textString = str;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();

		// Check for multiline property
		Resource predicate = (Resource) m_context.getDescendantLocalProperty(OzoneConstants.s_aspect);
		m_multiline = false;
		if (predicate != null) {
			m_multiline = Utilities.checkBooleanProperty(predicate, Constants.s_editor_multiline, m_source);
		} else {
			m_multiline = Utilities.checkBooleanProperty(m_prescription, Constants.s_editor_multiline, m_partDataSource);
		}

		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		initializeSWTPart();
		
		if (m_prescription == null) {
			m_prescription = s_literalEditorPartData;
			m_context.putLocalProperty(OzoneConstants.s_partData, m_prescription);
		}
		if (m_resPart == null) {
			m_resPart = s_literalEditorPart;
			m_context.putLocalProperty(OzoneConstants.s_part, m_resPart);
		}
		m_context.putProperty(s_literalEditor, this);

		RDFNode object = (RDFNode) m_context.getProperty(MetadataEditorConstants.object);
		if (object != null) {
			setValue(object);
			m_lastSaved = object.getContent();
		}
		
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (dataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
				m_ownsDataProvider = true;
			}
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(new StringDataConsumer() {
				/**
				 * @see edu.mit.lcs.haystack.ozone.data.StringDataConsumer#onStringChanged(java.lang.String)
				 */
				protected void onStringChanged(String newString) {
					changeTextDelayed(newString);
				}

				/**
				 * @see edu.mit.lcs.haystack.ozone.data.StringDataConsumer#onStringDeleted(java.lang.String)
				 */
				protected void onStringDeleted(String previousString) {
					changeTextDelayed("");
				}
			});
			
			m_text.setEditable(m_editable = true);
		} else {
			m_text.setBackground(SlideUtilities.getAmbientBgcolor(m_context));
		}
	}

	public void enableBlankSupport() {
		m_disallowBlanks = true;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		initializeSWTPart();
	}
}

