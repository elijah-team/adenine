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
abstract public class KeyHandler {
	public void initialize(Control control) {
		control.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (!isDisposed()) {
					onKeyPressed(e);
				}
			}

			public void keyReleased(KeyEvent e) {
				if (!isDisposed()) {
					onKeyReleased(e);
				}
			}
		});
	}

	protected void onKeyPressed(KeyEvent e) {
		handleEvent(PartConstants.s_eventKeyPressed, e);
	}
	protected void onKeyReleased(KeyEvent e) {
		handleEvent(PartConstants.s_eventKeyReleased, e);
	}

	abstract protected boolean isDisposed();
	abstract protected boolean handleEvent(Resource eventType, KeyEvent e);
}
