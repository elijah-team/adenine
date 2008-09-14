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

/**
 * @author David Huynh
 */
abstract public class ParentChildDropHandler implements Serializable {
	public boolean letChildrenHandleEvent(Resource eventType, edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {
		IVisualPart	previousChildPart = getPreviousChildPart();
		boolean r = false;
		
		if (eventType.equals(PartConstants.s_eventDragExit)) {
			if (previousChildPart != null) {
				previousChildPart.handleEvent(eventType, event); // ignore result
				onChildExit(previousChildPart, event);
				setPreviousChildPart(null);
			}
		} else {
			IVisualPart	visualChildPart = hittest(event.m_x, event.m_y);
			
			if (eventType.equals(PartConstants.s_eventFakeDrop)) {
				if (visualChildPart != null) {
					return visualChildPart.handleEvent(PartConstants.s_eventFakeDrop, event);
				}
				return false;
			}

			if (eventType.equals(PartConstants.s_eventDragEnter)) {
				if (visualChildPart != null) {
					r = visualChildPart.handleEvent(PartConstants.s_eventDragEnter, event);
					onChildEnter(visualChildPart, event);
				}			
			} else if (eventType.equals(PartConstants.s_eventDragHover)) {
				if (previousChildPart != null) {
					if (previousChildPart != visualChildPart) {
						previousChildPart.handleEvent(PartConstants.s_eventDragExit, event);
						
						if (visualChildPart != null) {
							r = visualChildPart.handleEvent(PartConstants.s_eventDragEnter, event);
							onChildChange(previousChildPart, visualChildPart, event);
						} else {
							onChildExit(previousChildPart, event);
						}
					} else if (visualChildPart != null) {
						r = visualChildPart.handleEvent(PartConstants.s_eventDragHover, event);
					}
				} else if (visualChildPart != null) {
					r = visualChildPart.handleEvent(PartConstants.s_eventDragEnter, event);
					onChildEnter(visualChildPart, event);
				}
				
			} else {
				if (previousChildPart != null) {
					if (previousChildPart != visualChildPart) {
						previousChildPart.handleEvent(PartConstants.s_eventDragExit, event);
						
						if (visualChildPart != null) {
							visualChildPart.handleEvent(PartConstants.s_eventDragEnter, event);
							onChildChange(previousChildPart, visualChildPart, event);
						} else {
							onChildExit(previousChildPart, event);
						}
					}
				} else if (visualChildPart != null) {
					visualChildPart.handleEvent(PartConstants.s_eventDragEnter, event);
					onChildEnter(visualChildPart, event);
				}
				
				if (visualChildPart != null) {
					r = visualChildPart.handleEvent(eventType, event);
				}
			}
			
			setPreviousChildPart(visualChildPart);
		}
		
		return r;
	}

	protected abstract IVisualPart hittest(int x, int y);
	protected abstract IVisualPart getPreviousChildPart();
	protected abstract void setPreviousChildPart(IVisualPart vp);
	
	protected void onChildExit(IVisualPart previousChildPart, OzoneDropTargetEvent e) {
	}
	
	protected void onChildEnter(IVisualPart childPart, OzoneDropTargetEvent e) {
	}
	
	protected void onChildChange(IVisualPart previousChildPart, IVisualPart currentChildPart, OzoneDropTargetEvent e) {
	}
}
