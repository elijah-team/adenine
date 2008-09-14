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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.utils.ControlPart;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.ozone.data.SetDataConsumer;
import edu.mit.lcs.haystack.ozone.data.SetDataProviderWrapper;
import edu.mit.lcs.haystack.ozone.data.StringDataConsumer;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class TitleBasedResourceEditorPart extends ControlPart {
	interface IValue {
		public String getValue();
		public void dispose();
		public void setTableItem(TableItem ti);
	}
	
	protected class FixedValue implements IValue {
		String m_value;
		TableItem m_ti;
		
		protected FixedValue(String s) {
			m_value = s;
		}
		
		public void dispose() {
		}
		
		public String getValue() {
			return m_value;
		}

		public void setTableItem(TableItem ti) {
			m_ti = ti;
			ti.setText(0, m_value);
		}
	}
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(TitleBasedResourceEditorPart.class);
	
	class DynamicValue extends StringDataConsumer implements IValue {
		Resource m_item;
		String m_value = null;
		IDataProvider m_dataProvider = null;
		TableItem m_ti;

		protected DynamicValue(Resource item) {
			m_item = item;

			if (m_dataProvider == null) {
				// Use the dc:title or rdfs:label
				m_value = Utilities.getLiteralProperty(item, Constants.s_dc_title, m_infoSource);
				if (m_value == null) {
					m_value = Utilities.getLabel(item, m_infoSource);
				}
				
				// Update immediately
				if (m_ti != null && !m_ti.isDisposed() && m_value != null) {
					updateTableItem(m_ti, m_value, m_item);
				} else {
					m_ti = updateTableItem(null, m_value, m_item);
				}
			} else {
				Context	childContext = new Context(m_context);
				childContext.putLocalProperty(DataConstants.RESOURCE, item);
				m_dataProvider = DataUtilities.createDataProvider2(m_titleDataSource, childContext, m_source, m_partDataSource);
				m_dataProvider.registerConsumer(this);
			}
		}

		public String getValue() {
			return m_value;
		}

		protected void onStringChanged(String newString) {
			synchronized (m_item) {
				m_value = newString;
			}
			asyncUpdate();
		}

		protected void onStringDeleted(String previousString) {
			synchronized (m_item) {
				m_value = null;
			}
			asyncUpdate();
		}
		
		void asyncUpdate() {
			Ozone.idleExec(new IdleRunnable(m_context) {
				/**
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					synchronized (m_item) {
						if (m_ti != null && !m_ti.isDisposed() && m_value != null) {
							updateTableItem(m_ti, m_value, m_item);
						} else {
							m_ti = updateTableItem(null, m_value, m_item);
						}
					}
				}
			});
		}
		
		public void dispose() {
			if (m_dataProvider != null) {
				m_dataProvider.dispose();
				m_dataProvider = null;
			}
		}

		public void setTableItem(TableItem ti) {
			m_ti = ti;
			if (m_value != null) {
				ti.setText(0, m_value);
			}
		}
	}
	
	protected StyledText				m_text;
	protected IdleRunnable				m_saveRunnable;
	protected IdleRunnable				m_updateRunnable;
	protected String					m_lastSaved = "";
	protected SetDataConsumer			m_dataConsumer;
	protected Composite					m_popup;
	protected Table						m_table;
	protected SetDataProviderWrapper	m_dataProviderWrapper;
	protected boolean					m_showPopup = false;
	protected Resource					m_currentResource = null;
	protected HashMap					m_possibleValues = new HashMap();
	protected Resource					m_onValueSet = null;
	protected Resource					m_titleDataSource = null;
	protected IDataProvider				m_dataProvider = null;
	protected boolean					m_ownsDataProvider = false;
	protected Font						m_oldFont;
		
	public void save() {
		try {
			String text = m_text.getText();
			if ((m_lastSaved == null) || !text.equals(m_lastSaved)) {
				m_currentResource = null;
				m_lastSaved = text;
				fillTable();
				showPopup(text.length() > 0);
			}
		} catch (Exception e) {
			s_logger.error("Failed to save", e);
		}
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		if (m_text != null) {
			m_text.setBounds(new Rectangle(r.x + 1, r.y + 1, r.width - 2, r.height - 2));
			repositionPopup();
		}
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
	 * @see IBlockGUIHandler#calculateSize(int,int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		Point size = new Point(200, 18);
		/*if (hintedHeight != -1) {
			size.y = hintedHeight;
		}*/
		if (hintedWidth != -1) {
			size.x = hintedWidth;
		}
		return new BlockScreenspace(size);
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		Rectangle r2 = m_text.getBounds();
		--r2.x;
		--r2.y;
		++r2.width;
		++r2.height;
		gc.setForeground(SlideUtilities.getAmbientColor(m_context));
		gc.drawRectangle(r2);
	}
	
	public void renderHTML(HTMLengine he) {
		he.enter("TitleBasedResourceEditorPart");
		he.exit("TitleBasedResourceEditorPart");
	}

	public String getContent() {
		return m_text.getText();
	}

	/**
	 * @see IValueEditorPart#getValue()
	 */
	public RDFNode getValue() {
		return m_currentResource;
	}

	protected String getTitle(Resource res) {
		Context	childContext = new Context(m_context);
		childContext.putLocalProperty(DataConstants.RESOURCE, res);
		IDataProvider dataProvider = DataUtilities.createDataProvider2(m_titleDataSource, childContext, m_source, m_partDataSource);
		try {
			return (String)dataProvider.getData(DataConstants.STRING, null);
		} catch (DataNotAvailableException e) {
			return res.toString();
		} finally {
			dataProvider.dispose();
		}
	}

	/**
	 * @see IValueEditorPart#setValue(RDFNode)
	 */
	public void setValue(RDFNode rdfn) {
		m_currentResource = (Resource) rdfn;
		m_text.setText(m_lastSaved = getTitle(m_currentResource));
		showPopup(false);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#internalInitialize()
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		SlideUtilities.recordAmbientProperties(m_context, m_source, m_prescription);
		
		m_onValueSet = Utilities.getResourceProperty(m_prescription, Constants.s_editor_onValueSet, m_partDataSource);
		m_titleDataSource = Utilities.getResourceProperty(m_prescription, Constants.s_editor_titleSource, m_partDataSource);
		
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			Resource dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (dataSource != null) {
				m_dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
				m_ownsDataProvider = true;
			}
		}

		if (m_dataProvider != null) {
			m_dataProvider.registerConsumer(new ResourceDataConsumer() {
				/**
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceChanged(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceChanged(Resource newResource) {
					setValue(newResource);
				}
				/**
				 * @see edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer#onResourceDeleted(edu.mit.lcs.haystack.rdf.Resource)
				 */
				protected void onResourceDeleted(Resource previousResource) {
				}
			});
		}
		
		// Set up text box
		Composite parent = (Composite) m_context.getSWTControl();
		m_text = new StyledText(parent, SWT.SINGLE | SWT.FLAT | SWT.NO_BACKGROUND);
		Font font = SlideUtilities.getAmbientFont(m_context);
		m_oldFont = m_text.getFont();
		m_text.setFont(font);
		
		m_saveRunnable = new IdleRunnable(m_context) {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				save();
			}
		};

		m_updateRunnable = new IdleRunnable(m_context) {
			/**
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				fillTable();
				showPopup(m_text.getText().length() > 0);
			}
		};

		m_text.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent me) {
				if (me.button == 3) {
					if (!PartUtilities.showContextMenu(m_text, me, m_source, m_context)) {
						Point point = m_text.toDisplay(new Point(me.x, me.y));
						PartUtilities.showContextMenu(m_source, m_context, point);
					}
				}
			}
		});
		m_text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent me) {
				Ozone.idleExec(m_saveRunnable);
			}
		});

		m_text.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent fe) {
			}

			public void focusGained(FocusEvent fe) {
			}
		});

		m_text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				switch (ke.keyCode) {
					case SWT.ARROW_DOWN:
					{
						int i = m_table.getSelectionIndex() + 1;
						if (i != m_table.getItemCount()) {
							m_table.setSelection(i);
						}
						break;
					}
					
					case SWT.ARROW_UP:
					{
						int i = m_table.getSelectionIndex() - 1;
						if (i == -2) {
							i = 0;
						}
						if (i >= 0) {
							m_table.setSelection(i);
						}
						break;
					}

					default:
					handleKeyPress(ke);
				}
			}
		});

		m_control = m_text;

		// Set up floating assistance window
		m_popup = new Composite(parent, SWT.BORDER);
		m_table = new Table(m_popup, SWT.SINGLE);
		m_table.setFont(font);
		m_popup.setVisible(false);
		TableColumn tc;
		tc = new TableColumn(m_table, SWT.LEFT, 0);
		tc.setText("Name");
		tc.setWidth(250);
		tc = new TableColumn(m_table, SWT.LEFT, 1);
		tc.setText("URI");
		tc.setWidth(200);

		m_table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				handleKeyPress(ke);
			}
		});
		
		m_table.addMouseListener(new MouseAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.MouseAdapter#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
			 */
			public void mouseDoubleClick(MouseEvent arg0) {
				accept();
			}
		});

		m_popup.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent ce) {
				Rectangle r = m_popup.getClientArea();
				m_table.setBounds(r);
			}
		});

		retrievePossibleValues();

		m_text.setFocus();
	}

	protected void retrievePossibleValues() {
		// Set up data source
		Resource dataSource = Utilities.getResourceProperty(m_prescription, Constants.s_editor_valuesSource, m_partDataSource);
		if (dataSource != null) {
			IDataProvider dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
			if (dataProvider != null) {
				m_dataProviderWrapper = new SetDataProviderWrapper(dataProvider);
				m_dataConsumer = new SetDataConsumer() {
					/**
					 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsAdded(java.util.Set)
					 */
					protected void onItemsAdded(Set items) {
						synchronized (m_possibleValues) {
							Iterator i = items.iterator();
							while (i.hasNext()) {
								Resource res = (Resource) i.next();
								m_possibleValues.put(res, new DynamicValue(res));
							}
						}
						Ozone.idleExec(m_updateRunnable);
					}

					/**
					 * @see edu.mit.lcs.haystack.ozone.data.SetDataConsumer#onItemsRemoved(java.util.Set)
					 */
					protected void onItemsRemoved(Set items) {
						synchronized (m_possibleValues) {
							Iterator i = items.iterator();
							while (i.hasNext()) {
								IValue v = (IValue) m_possibleValues.remove(i.next());
								if (v != null) {
									v.dispose();
								}
							}
						}
						Ozone.idleExec(m_updateRunnable);
					}

					/**
					 * @see edu.mit.lcs.haystack.ozone.data.
					 * SetDataConsumer#onSetCleared()
					 */
					protected void onSetCleared() {
						synchronized (m_possibleValues) {
							m_possibleValues.clear();
						}
						Ozone.idleExec(m_updateRunnable);
					}
				};
				dataProvider.registerConsumer(m_dataConsumer);
			}
		}

		if (m_dataProviderWrapper == null) {
			// Look for everything with a DC_TITLE
			/*try {
				Iterator i = m_infoSource.query(new Statement(Utilities.generateWildcardResource(1), Constants.s_dc_title, Utilities.generateWildcardResource(2)), Utilities.generateWildcardResourceArray(2)).iterator();
				while (i.hasNext()) {
					RDFNode[] datum = (RDFNode[]) i.next();
					m_possibleValues.put(datum[0], new DynamicValue((Resource) datum[0]));
				}
			} catch (RDFException e) {
				s_logger.error("Failed while looking for things with titles", e);
			}*/
		}
	}

	public void accept() {
		TableItem[] ti = m_table.getSelection();
		if (ti.length > 0) {
			m_currentResource = (Resource) ti[0].getData();
			m_text.setText(m_lastSaved = ti[0].getText(0));
		} else {
			m_currentResource = null;
		}
		callOnValueSet();
		showPopup(false);
	}

	protected void handleKeyPress(KeyEvent ke) {
		if ((ke.character == '\r') || (ke.character == '\n')) {
			accept();
		} else if (ke.character == SWT.ESC) {
			showPopup(false);
		}
	}
	
	protected void showPopup(boolean b) {
		if (b == m_showPopup) {
			return;
		}
		
		m_popup.setVisible(m_showPopup = b);
		if (b) {
			repositionPopup();
		}
	}
	
	protected void repositionPopup() {
		Rectangle r = m_text.getBounds();
		m_popup.setBounds(r.x, r.y + r.height, r.width, 200);
		m_popup.moveAbove(null);
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		m_text.setFont(m_oldFont);
		SlideUtilities.releaseAmbientProperties(m_context);
		
		super.dispose();
		m_popup.dispose();
		
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.dispose();
		}

		Iterator i = m_possibleValues.keySet().iterator();
		while (i.hasNext()) {
			IValue v = (IValue) m_possibleValues.get(i.next());
			if (v != null) {
				v.dispose();
			}
		}
		
		if (m_ownsDataProvider && m_dataProvider != null) {
			m_dataProvider.dispose();
		}
		m_dataProvider = null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IGUIHandler#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		m_popup.setVisible(visible && m_showPopup);
	}

	protected void fillTable() {
		m_table.removeAll();

		String text = m_text.getText().trim();
		if (text.length() == 0) {
			return;
		}

		Iterator i = m_possibleValues.keySet().iterator();
		while (i.hasNext()) {
			Resource res = (Resource) i.next();
			IValue v = (IValue) m_possibleValues.get(res);
			String text2 = v.getValue();
			if (text2 != null && text2.toLowerCase().indexOf(text.toLowerCase()) != -1) {
				TableItem ti = new TableItem(m_table, SWT.NONE);
				ti.setText(0, text2);
				ti.setText(1, res.getURI());
				v.setTableItem(ti);
				ti.setData(res);
			}
		}

		m_table.setSelection(0);
	}
	
	protected TableItem updateTableItem(TableItem ti, String str, Resource res) {
		String text = m_text.getText().trim();
		if (text.length() > 0 && text.toLowerCase().indexOf(str.toLowerCase()) != -1) {
			if (ti == null) {
				ti = new TableItem(m_table, SWT.NONE);
				ti.setText(1, res.getURI());
			}
			ti.setText(0, str);
			ti.setData(res);
		}
		
		return ti;
	}
	
	protected Resource resolveResource() {
		if (m_currentResource != null) {
			return m_currentResource;
		}
		
		String text = m_text.getText();

		// Hunt for a match, if possible
		Iterator i = m_possibleValues.keySet().iterator();
		HashSet matches = new HashSet();
		while (i.hasNext()) {
			Resource res = (Resource) i.next();
			IValue value = (IValue) m_possibleValues.get(res);
			
			// TODO[dquan]: wait for string to become available, if necessary
			String text2 = value.getValue();
			if (text.equals(text2)) {
				matches.add(res);
			}
		}

		if (matches.size() != 1) {
			// Recognize <> notation
			if (text.length() >= 2 && text.charAt(0) == '<' && text.charAt(text.length() - 1) == '>') {
				m_currentResource = new Resource(text.substring(1, text.length() - 1));
			}
		} else {
			m_currentResource = (Resource) matches.iterator().next();
		}

		if (m_currentResource == null) {
			// TODO[dquan]:
			m_currentResource = Utilities.generateUnknownResource();
			try {
				m_infoSource.add(new Statement(m_currentResource, Constants.s_dc_title, new Literal(m_text.getText())));
			} catch (RDFException e) {
				s_logger.error("Failed to add title", e);
			}

			if (m_dataProviderWrapper != null) {
				HashSet set = new HashSet();
				set.add(m_currentResource);
				try {
					m_dataProviderWrapper.requestAddition(set);
				} catch (Exception e) {
					s_logger.error("Failed to request set addition", e);
				}
			}
		}
		
		return m_currentResource;
	}
	
	protected void callOnValueSet() {
		Resource res = resolveResource();
				
		Interpreter i = Ozone.getInterpreter();
		DynamicEnvironment denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(denv, m_context);

		try {
			i.callMethod(m_onValueSet, new Object[] { res, this }, denv);
		} catch (Exception e) {
			s_logger.error("Failed to call on value set", e);
		}
		
		if (m_dataProvider != null) {
			try {
				m_dataProvider.requestChange(res == null ? DataConstants.RESOURCE_DELETION : DataConstants.RESOURCE_CHANGE, res);
			} catch (DataMismatchException e) {
				s_logger.error("Failed to request resource deletion", e);
			}
		}
	}
}
