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

package edu.mit.lcs.haystack.ozone.core.utils;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

/**
 * @author David Huynh
 */
abstract public class MouseHandler {
	protected Control m_control;
	
	public void initialize(Control control) {
		m_control = control;
		
		control.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent me) {
				if (!isDisposed()) {
					onMouseUp(me);
				}
			}

			public void mouseDown(MouseEvent me) {
				if (!isDisposed()) {
					onMouseDown(me);
				}
			}

			public void mouseDoubleClick(MouseEvent me) {
				if (!isDisposed()) {
					onMouseDoubleClick(me);
				}
			}
		});
		
		control.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent me) {
				if (!isDisposed()) {
					onMouseEnter(me);
				}
			}

			public void mouseExit(MouseEvent me) {
				if (!isDisposed()) {
					onMouseExit(me);
				}
			}

			public void mouseHover(MouseEvent me) {
				if (!isDisposed()) {
					onMouseHover(me);
				}
			}
		});

		control.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent me) {
				if (!isDisposed()) {
					onMouseMove(me);
				}
			}
		});
	}

	protected void onMouseUp(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseUp, me);
	}
	protected void onMouseDown(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseDown, me);
	}
	protected void onMouseDoubleClick(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseDoubleClick, me);
	}
	protected void onMouseEnter(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseEnter, me);
	}
	protected void onMouseExit(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseExit, me);
	}
	protected void onMouseHover(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseHover, me);
	}
	protected void onMouseMove(MouseEvent me) {
		handleEvent(PartConstants.s_eventMouseMove, me);
	}
	
	abstract protected boolean isDisposed();
	abstract protected boolean handleEvent(Resource eventType, MouseEvent e);
}
