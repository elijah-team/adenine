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

package edu.mit.lcs.haystack;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.*;

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class URIFinder {
	Composite m_composite;
	Table m_table;
	Text m_text;
	IRDFContainer m_source;
	StyledText m_parent;
	ArrayList m_items;
	
	public URIFinder(IRDFContainer rdfc, Composite parent, Rectangle bounds, StyledText st, Font font) {
		m_parent = st;
		m_source = rdfc;
		m_composite = new Composite(parent, SWT.POP_UP | SWT.BORDER);
		m_table = new Table(m_composite, SWT.SINGLE);
		m_text = new Text(m_composite, SWT.NONE);
		m_composite.setFont(font);
		m_table.setFont(font);
		m_text.setFont(font);
		TableColumn tc;
		tc = new TableColumn(m_table, SWT.LEFT, 0);
		tc.setText("Name");
		tc.setWidth(150);
		tc = new TableColumn(m_table, SWT.LEFT, 1);
		tc.setText("URI");
		tc.setWidth(150);
		m_text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				switch (ke.keyCode) {
					case SWT.ARROW_DOWN:
					case SWT.ARROW_UP:
					{
						Event e = new Event();
						e.keyCode = ke.keyCode;
						m_table.notifyListeners(SWT.KeyDown, e);
					}
					break;
					
					default:
					handleKeyPress(ke);
				}
			}
		});
		m_table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				handleKeyPress(ke);
			}
		});
		m_composite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent ce) {
				Rectangle r = m_composite.getClientArea();
				m_text.setBounds(0, 0, r.width, 20);
				m_table.setBounds(0, 20, r.width, r.height - 20);
			}
		});
		m_composite.setBounds(bounds);
		m_text.setFocus();

		updateMap();		
	}
	
	protected void handleKeyPress(KeyEvent ke) {
		if ((ke.character == '\r') || (ke.character == '\n')) {
			TableItem[] ti = m_table.getSelection();
			if (ti.length > 0) {
				String toInsert = ti[0].getData().toString();
				m_parent.replaceTextRange(m_parent.getCaretOffset(), 0, toInsert);
				m_parent.setCaretOffset(m_parent.getCaretOffset() + toInsert.length());
			}
			m_composite.dispose();
		} else if (ke.character == SWT.ESC) {
			m_composite.dispose();
		} else {
			fillTable();
		}
	}
	
	protected void fillMapWithPredicate(Resource predicate) {
/*		try {
			Set items = m_source.query(new Statement[] {
				new Statement(Utilities.generateWildcardResource(1), predicate, Utilities.generateWildcardResource(2))
			}, Utilities.generateWildcardResourceArray(2), Utilities.generateWildcardResourceArray(2));
			
			Iterator i = items.iterator();
			while (i.hasNext()) {
				RDFNode[] datum = (RDFNode[])i.next();
				Resource res = (Resource)datum[0];
				String title = datum[1].getContent();
				
				m_items.add(new Object[] { res, title });
			}
		} catch(RDFException e) {
		}*/
	}
	
	protected void updateMap() {
		m_items = new ArrayList();

		fillMapWithPredicate(Constants.s_dc_title);
		fillMapWithPredicate(Constants.s_rdfs_comment);
		fillMapWithPredicate(Constants.s_rdfs_label);
		fillMapWithPredicate(Constants.s_dc_description);
	}
	
	protected void fillTable() {
		m_table.removeAll();
		
		String text = m_text.getText().trim();
		if (text.length() == 0) {
			return;
		}
		
		Iterator i = m_items.iterator();
		while (i.hasNext()) {
			Object[] datum = (Object[])i.next();
			Resource res = (Resource)datum[0];
			String text2 = (String)datum[1];
			if (text2.indexOf(text) != -1) {
				TableItem ti = new TableItem(m_table, SWT.NONE);
				ti.setText(0, text2);
				ti.setText(1, res.getContent());
				ti.setData(res);
			}
		}
		
		m_table.setSelection(0);
	}
}
