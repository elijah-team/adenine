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

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.adenine.SWTConsole;
import edu.mit.lcs.haystack.adenine.interpreter.JavaMethodWrapper;
import edu.mit.lcs.haystack.ozone.core.IViewPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.utils.ControlPart;

/**
 * Provides a console for typing Adenine commands.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AdenineConsolePart extends ControlPart implements IViewPart {

	protected void internalInitialize() {
		super.internalInitialize();
		
		Composite control = (Composite) m_context.getSWTControl();
		
		SWTConsole c = new SWTConsole(control, m_source);
		m_control = c.getControl();
		
		c.setServiceAccessor(m_context.getServiceAccessor());
		c.setEnvironmentValue("navigate", new JavaMethodWrapper(m_context.getProperty(OzoneConstants.s_navigationMaster), "navigate"));
		c.setEnvironmentValue("context", m_context);
	}
	
	/**
	 * @see IVisualPart#getPreferredSize(int, int)
	 */
	public Point getPreferredSize(int x, int y) {
		if (x == -1) {
			x = 200;
		}
		if (y == -1) {
			y = 300;
		}
		return new Point(x,y);
	}
}
