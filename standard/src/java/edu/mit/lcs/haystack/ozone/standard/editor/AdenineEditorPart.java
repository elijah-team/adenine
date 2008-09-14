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

import java.io.*;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.parser.Parser;
import edu.mit.lcs.haystack.adenine.compilers.javaByteCode.JavaByteCodeCompiler;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.URIFinder;

/**
 * Supports editing Adenine files located on HTTP servers.
 * @author Dennis Quan
 */
public class AdenineEditorPart extends TextEditorPart {
	public final static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineEditorPart.class);
	
	protected StyledText		m_results;
	protected SashForm			m_sashForm;
	protected ToolBar			m_toolbar;
	protected Composite			m_composite;
	protected AdenineLineStyler m_lineStyler = new AdenineLineStyler();

	protected void uninstall() {
		try {
			Utilities.uninstallPackage(m_underlying, m_source);
			m_results.setText("Uninstall successful.");
		} catch(RDFException e) {
			s_logger.error("Failed to uninstall package " + m_underlying, e);
		}
	}		

	protected void compile() {
		try {
			Utilities.uninstallPackage(m_underlying, m_source);
			
			PackageFilterRDFContainer 	rdfc = new PackageFilterRDFContainer(m_source, m_underlying);
			java.util.List 				errors = new JavaByteCodeCompiler(rdfc).compile(
				null, 
				new StringReader(m_text.getText()), 
				"adenine file object: " + m_underlying.getURI(), 
				null,
				null
			);
			
			if (errors.isEmpty()) {
				m_results.setText("Compilation successful.");
			} else {
				StringWriter sw = new StringWriter();
				
				java.util.Iterator i = errors.iterator();
				
				while (i.hasNext()) {
					((Exception) i.next()).printStackTrace(new PrintWriter(sw));
				}

				m_results.setText(sw.getBuffer().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void prettyPrint() {
		try {
			try {
				m_text.setText(Parser.blockify(Parser.tokenize(new StringReader(m_text.getText()))).prettyPrint(-1));
				m_results.setText("Pretty print successful.");
			} catch (AdenineException ae) {
				StringWriter sw = new StringWriter();
				ae.printStackTrace(new PrintWriter(sw));
				m_results.setText(sw.getBuffer().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void insertURI() {
		new URIFinder(m_source, m_text, new Rectangle(100, 100, 300, 300), m_text, SlideUtilities.getAmbientFont(m_context));
	}
	
	protected void generateURI() {
		String toInsert = Utilities.generateUniqueResource().toString();
		m_text.replaceTextRange(m_text.getCaretOffset(), 0, toInsert);
		m_text.setCaretOffset(m_text.getCaretOffset() + toInsert.length());
	}

	protected void setupTextWidget() {
		Composite parent = (Composite) m_context.getSWTControl();
		m_control = m_composite = new Composite(parent, 0);
		m_sashForm = new SashForm(m_composite, SWT.VERTICAL);
		m_toolbar = new ToolBar(m_composite, SWT.FLAT);
		m_composite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent ce) {
				Point tb = m_toolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				Rectangle r = m_composite.getClientArea();
				m_toolbar.setBounds(0, 0, r.width, tb.y);
				m_sashForm.setBounds(0, tb.y, r.width, r.height - tb.y);
			}
		});
		createTool("send.gif", "Compile (F7)").addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				compile();
			}
		});

		createTool("send.gif", "Uninstall (F3)").addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				uninstall();
			}
		});

		createTool("send.gif", "Pretty Print (F8)").addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				prettyPrint();
			}
		});

		createTool("send.gif", "Generate URI (F4)").addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				generateURI();
			}
		});

		createTool("send.gif", "Insert URI (F2)").addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				insertURI();
			}
		});

		m_text = new StyledText(m_sashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		m_results = new StyledText(m_sashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		m_sashForm.setWeights(new int[] { 75, 25 });
		m_text.addLineStyleListener(m_lineStyler);

		m_text.addDisposeListener(new DisposeListener() {  
			public void widgetDisposed (DisposeEvent event) {
				m_text.removeLineStyleListener(m_lineStyler);
				m_text.removeDisposeListener(this);
			}
		});
		
		m_text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				if (ke.character == '\r') {
					int offset = m_text.getCaretOffset();
					String str = m_text.getText(0, offset - 2);
					StringBuffer sb = new StringBuffer();
					int i = str.length() - 2;
					while (i >= 0) {
						char ch = str.charAt(i);
						if (ch == ' ' || ch == '\t') {
							sb.insert(0, ch);
						} else if (ch == '\n' || ch == '\r') {
							m_text.insert(sb.toString());
							m_text.setCaretOffset(offset + sb.length());
							return;
						} else {
							sb = new StringBuffer();
						}
						--i;
					}
				} else if (ke.keyCode == SWT.F2) {
					insertURI();
				} else if (ke.keyCode == SWT.F7) {
					compile();
				} else if (ke.keyCode == SWT.F8) {
					prettyPrint();
				} else if (ke.keyCode == SWT.F4) {
					generateURI();
				} else if (ke.keyCode == SWT.F3) {
					uninstall();
				}
			}
		});
	}

	protected ToolItem createTool(String bitmap, String text) {
		try {
			InputStream is = getClass().getResourceAsStream(bitmap);
			//Image image = new Image(m_toolbar.getDisplay(), m_imageloader.load(is)[0]);
			ToolItem ti = new ToolItem(m_toolbar, 0);
			//ti.setImage(image);
			ti.setText(text);
			ti.setToolTipText(text);
			return ti;
		} catch (Exception e) {
			s_logger.error("Failed to create tool with " + bitmap + " and " + text, e);
			return null;
		}
	}
}

