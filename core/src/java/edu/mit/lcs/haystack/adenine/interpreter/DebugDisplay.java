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

package edu.mit.lcs.haystack.adenine.interpreter;

import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.SWTConsole;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Adenine interactive debugger display.
 * @version 	1.0
 * @author		Dennis Quan
 */
class DebugDisplay {
	Interpreter m_interpreter;
	Shell m_shell;
	SashForm m_sashForm;
	ToolBar m_toolbar;
	SWTConsole m_console;
	Text m_status;
	StyledText m_source;
	
	class Event extends RuntimeException {
		Event(int s) { 
			m_status = s; 
		}
		
		int m_status;
	}
	
	DebugDisplay(Interpreter i) {
		m_interpreter = i;
		Display display = Display.getDefault();
		m_shell = new Shell(display);
		m_shell.setText("Adenine Debugger");
		m_sashForm = new SashForm(m_shell, SWT.VERTICAL);
		m_toolbar = new ToolBar(m_shell, SWT.FLAT);
		m_shell.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent ce) {
				Point tb = m_toolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				Rectangle r = m_shell.getClientArea();
				m_toolbar.setBounds(0, 0, r.width, tb.y);
				m_sashForm.setBounds(0, tb.y, r.width, r.height - tb.y);
			}
		});
		m_status = new Text(m_sashForm, SWT.V_SCROLL | SWT.MULTI);
		m_status.setEditable(false);
		m_source = new StyledText(m_sashForm, SWT.V_SCROLL | SWT.MULTI);
		m_source.setEditable(false);
		Font font = new Font(display, "Courier New", 8, SWT.NORMAL);
		m_source.setFont(font);
		m_console = new SWTConsole(m_sashForm, i.getRootRDFContainer());

		ToolItem ti;
		ti = new ToolItem(m_toolbar, 0);
		ti.setText("Step Into");
		ti.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				throw new Event(STEP_INTO);
			}
		});

		ti = new ToolItem(m_toolbar, 0);
		ti.setText("Step Over");
		ti.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				throw new Event(STEP_OVER);
			}
		});

		ti = new ToolItem(m_toolbar, 0);
		ti.setText("Stop Debugging");
		ti.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				throw new Event(STOP_DEBUGGING);
			}
		});

		ti = new ToolItem(m_toolbar, 0);
		ti.setText("Unmark As Debuggable");
		ti.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				throw new Event(UNMARK);
			}
		});
	}
	
	static final int STEP_INTO = 0;
	static final int STEP_OVER = 1;
	static final int STEP_OUT = 2;
	static final int CONTINUE = 3;
	static final int STOP_DEBUGGING = 4;
	static final int UNMARK = 5;
	
	void dumpEnvironment(Environment env, StringBuffer sb) {
		Iterator i = env.m_bindings.keySet().iterator();
		while (i.hasNext()) {
			String key = (String)i.next();
			Object value = env.getValue(key);
			
			sb.append("\t[");
			sb.append(key);
			sb.append("] ");
			sb.append(value);
			sb.append("\n");
		}
	}
	
	void dumpStackTrace(ArrayList stack, StringBuffer sb) {
		ListIterator i = stack.listIterator(stack.size());
		while (i.hasPrevious()) {
			Resource res = (Resource)i.previous();
			String description = AdenineException.describeInstruction(res, m_interpreter.getRootRDFContainer());
			sb.append("\t");
			sb.append(description);
			sb.append("\n");
		}
	}
	
	int displayStatus(ArrayList stack, Environment env, DynamicEnvironment denv, AdenineException ae) {
		IRDFContainer rdfc = m_interpreter.getRootRDFContainer();
		
		m_console.setEnvironment(env);
		m_console.setDynamicEnvironment(denv);
		m_shell.open();
		
		StringBuffer sb = new StringBuffer();
		sb.append("Current method:\n\t");
		sb.append(m_interpreter.m_currentMethod);
		sb.append("\n\nStack trace:\n");
		dumpStackTrace(stack, sb);
		sb.append("\nEnvironment:\n");
		dumpEnvironment(env, sb);
		sb.append("\nDynamic environment:\n");
		dumpEnvironment(denv, sb);
		
		m_status.setText(sb.toString());
		
		// Find source code, if available
		Resource resTop = (Resource)stack.get(stack.size() - 1);
		String source = null;
		try {
			source = ContentClient.getContentClient(Utilities.getResourceProperty(resTop, AdenineConstants.source, rdfc), rdfc, m_interpreter.m_sa).getContentAsString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (source != null) {
			m_source.setText(source);
			
			int line = Interpreter.getLineNumber(resTop, rdfc);
			if (line != -1) {
				--line;
				int offset = m_source.getOffsetAtLine(line);
				int endOffset = (line >= (m_source.getLineCount() - 1) ? m_source.getCharCount() : m_source.getOffsetAtLine(line + 1));
				m_source.setSelection(offset, endOffset);
				m_source.showSelection();
			}
		} else {
			m_source.setText("No source available");
		}
	
		try {
			Display d = m_shell.getDisplay();
	        while (!m_shell.isDisposed()) {
	        	if (!d.readAndDispatch()) {
					d.sleep();
	        	}
	        }
		} catch (Event e) {
			m_shell.setVisible(false);
			return e.m_status;
		}
		
		return STOP_DEBUGGING;
	}
	
	void dispose() {
		if (!m_shell.isDisposed()) {
			m_shell.dispose();
		}
	}
}
