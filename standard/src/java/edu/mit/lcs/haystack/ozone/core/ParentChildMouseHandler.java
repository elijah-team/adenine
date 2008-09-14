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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.*;

import org.eclipse.swt.events.*;

/**
 * @author David Huynh
 */
abstract public class ParentChildMouseHandler implements Serializable {
	public boolean letChildrenHandleEvent(Resource eventType, MouseEvent me) {
		IVisualPart	previousChildPart = getPreviousChildPart();
		boolean 	r = false;

		if (eventType.equals(PartConstants.s_eventMouseExit)) {
			if (previousChildPart != null) {
				previousChildPart.handleEvent(eventType, me); // ignore result
				onChildExit(previousChildPart, me);
				setPreviousChildPart(null);
			}
		} else {
			IVisualPart	visualChildPart = hittest(me.x, me.y);
			
			if (eventType.equals(PartConstants.s_eventMouseEnter)) {
				if (visualChildPart != null) {
					r = visualChildPart.handleEvent(PartConstants.s_eventMouseEnter, me);
					onChildEnter(visualChildPart, me);
				}			
			} else if (eventType.equals(PartConstants.s_eventMouseHover) ||
						eventType.equals(PartConstants.s_eventMouseMove)) {
							
				if (previousChildPart != null) {
					if (previousChildPart != visualChildPart) {
						previousChildPart.handleEvent(PartConstants.s_eventMouseExit, me);
						
						if (visualChildPart != null) {
							r = visualChildPart.handleEvent(PartConstants.s_eventMouseEnter, me);
							onChildChange(previousChildPart, visualChildPart, me);
						} else {
							onChildExit(previousChildPart, me);
						}
					} else if (visualChildPart != null) {
						r = visualChildPart.handleEvent(PartConstants.s_eventMouseHover, me);
					}
				} else if (visualChildPart != null) {
					r = visualChildPart.handleEvent(PartConstants.s_eventMouseEnter, me);
					onChildEnter(visualChildPart, me);
				}
			} else {
				if (previousChildPart != null) {
					if (previousChildPart != visualChildPart) {
						previousChildPart.handleEvent(PartConstants.s_eventMouseExit, me);
						
						if (visualChildPart != null) {
							visualChildPart.handleEvent(PartConstants.s_eventMouseEnter, me);
							onChildChange(previousChildPart, visualChildPart, me);
						} else {
							onChildExit(previousChildPart, me);
						}
					}
				} else if (visualChildPart != null) {
					visualChildPart.handleEvent(PartConstants.s_eventMouseEnter, me);
					onChildEnter(visualChildPart, me);
				}
				
				if (visualChildPart != null) {
					r = visualChildPart.handleEvent(eventType, me);
				}
			}
			
			setPreviousChildPart(visualChildPart);
		}
		
		return r;
	}
	
	protected abstract IVisualPart hittest(int x, int y);
	protected abstract IVisualPart getPreviousChildPart();
	protected abstract void setPreviousChildPart(IVisualPart vp);
	
	protected void onChildExit(IVisualPart previousChildPart, MouseEvent e) {
	}
	
	protected void onChildEnter(IVisualPart childPart, MouseEvent e) {
	}
	
	protected void onChildChange(IVisualPart previousChildPart, IVisualPart currentChildPart, MouseEvent e) {
	}
}
