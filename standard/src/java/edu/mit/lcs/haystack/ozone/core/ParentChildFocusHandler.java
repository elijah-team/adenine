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

package edu.mit.lcs.haystack.ozone.core;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.events.FocusEvent;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.*;

/**
 * @author David Huynh
 */
abstract public class ParentChildFocusHandler implements Serializable {
	protected IVisualPart	m_lastChildPart;
	
	public boolean letChildHandleGotFocusEvent(FocusEvent e) {
		if (m_lastChildPart != null) {
			try {
				boolean r = m_lastChildPart.handleEvent(PartConstants.s_eventGotInputFocus, e);
				
				if (r) {
					return r;
				}
			} catch (Exception ex) {
			}
		}
		
		try {
			Iterator i = getChildParts().iterator();
			while (i.hasNext()) {
				IVisualPart vp = (IVisualPart) i.next();
				boolean 	r = vp.handleEvent(PartConstants.s_eventGotInputFocus, e);
				
				if (r) {
					m_lastChildPart = vp;
					return true;
				}
			}
		} catch (Exception ex) {
		}
		
		m_lastChildPart = null;
		
		return false;
	}
	
	public boolean letChildHandleLostFocusEvent(FocusEvent e) {
		if (m_lastChildPart != null) {
			try {
				return m_lastChildPart.handleEvent(PartConstants.s_eventLostInputFocus, e);
			} catch (Exception ex) {
			}
		}
		
		return false;
	}
	
	public void removeChildPart(IVisualPart vp) {
		if (m_lastChildPart == vp) {
			m_lastChildPart = null;
		}
	}
	
	protected abstract List getChildParts();
}
