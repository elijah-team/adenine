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

import edu.mit.lcs.haystack.ozone.core.utils.graphics.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;

/**
 * @author David Huynh
 */
public class ScrollableComposite extends Composite {
	IVisualPart					m_visualPart;
	ScrollBar					m_horizontal;
	ScrollBar					m_vertical;
	VisualPartAwareComposite	m_child;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ScrollableComposite.class);

	public ScrollableComposite(Composite parent) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		this.setBackground(GraphicsManager.s_white);
		
		addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				internalLayout();
			}
		});
		
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				handleKeyPressed(e);
			}
		});
		
		m_horizontal = this.getHorizontalBar();
		m_vertical = this.getVerticalBar();
		
		m_horizontal.setVisible(false);
		m_vertical.setVisible(false);

		m_horizontal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onScroll(e);
			}
		});			
		m_vertical.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onScroll(e);
			}
		});			
	}
	
	public Control getInnerControl() {
		return m_child;
	}
	
	public void setInnerControl(VisualPartAwareComposite child) {
		if (child != null && child.getParent() != this) {
			s_logger.error("Provided control is not child of this Composite", new Exception());
			return;
		}
		
		if (m_child != null && !m_child.isDisposed()) {
			m_child.setVisible(false);
			m_child.setEnabled(false);
		}
		
		m_child = child;
		if (m_child != null) {
			m_child.setVisible(true);
			m_child.setEnabled(true);
		}
	}
	
	public void layout(boolean b) {
		internalLayout();
	}
	
	public void setVisualPart(IVisualPart visualPart) {
		m_visualPart = visualPart;
		if (m_child != null && !m_child.isDisposed()) {
			m_child.setVisualPart(visualPart);
			m_child.setSize(0, 0);
		}

		internalLayout();
	}
	public IVisualPart getVisualPart() {
		return m_visualPart;
	}
	
	boolean m_inResize = false;
	private void internalLayout() {
		if (m_inResize || m_visualPart == null || m_child == null) {
			return;
		}
		
		IBlockGUIHandler sn = (IBlockGUIHandler) 
			m_visualPart.getGUIHandler(IBlockGUIHandler.class);
			
		if (sn == null) {
			s_logger.info("Embedded child part specifies null gui handler " + m_visualPart);
			return;
		}
		
		m_inResize = true;
		m_horizontal.setVisible(false);
		m_vertical.setVisible(false);
		
		Rectangle			r = getClientArea();
		BlockScreenspace 	bs = null;
		
		switch (sn.getHintedDimensions()) {
		case IBlockGUIHandler.FIXED_SIZE:
			bs = sn.getFixedSize();
			break;
			
		default:
			bs = sn.calculateSize(r.width, r.height);
			break;				
		}
		
		if (bs == null) {
			s_logger.error("Embedded child part specifies null block screenspace", new Exception());
			m_child.setSize(0, 0);
			setScrollbars();
		} else {
			if (sn.getHintedDimensions() != IBlockGUIHandler.FIXED_SIZE &&
				(bs.m_size.x > r.width || bs.m_size.y > r.height)) {
					
				if (bs.m_size.y > r.height && bs.m_size.x <= r.width) {
					m_vertical.setVisible(true);
					r = getClientArea();
					bs = sn.calculateSize(r.width, r.height);
				} else if (bs.m_size.x > r.width && bs.m_size.y <= r.height) {
					m_horizontal.setVisible(true);
					r = getClientArea();
					bs = sn.calculateSize(r.width, r.height);
				}
				
				if (bs.m_size.x > r.width && bs.m_size.y > r.height) {
					m_horizontal.setVisible(true);
					m_vertical.setVisible(true);
				}
			}
		
			if (bs.m_size.equals(m_child.getSize())) {
				sn.setBounds(m_child.getBounds());
				m_child.redraw();
			} else {
				m_child.setSize(bs.m_size);
				sn.setBounds(m_child.getBounds());
			}
			setScrollbars();
		}
		m_inResize = false;
	}
	
	private void setScrollbars() {
		Rectangle rInner = m_child.getBounds();
		Rectangle rOuter = getClientArea();
		
		if (m_vertical.getVisible()) {
			m_vertical.setMinimum(0);
			m_vertical.setMaximum(rInner.height);
			
			m_vertical.setIncrement(rOuter.height / 8);
			m_vertical.setPageIncrement(rOuter.height);
			m_vertical.setThumb(rOuter.height);
		} else {
			m_vertical.setSelection(0);
		}
		if (m_horizontal.getVisible()) {
			m_horizontal.setMinimum(0);
			m_horizontal.setMaximum(rInner.width);
			
			m_horizontal.setIncrement(rOuter.width / 8);
			m_horizontal.setPageIncrement(rOuter.width);
			m_horizontal.setThumb(rOuter.width);
		} else {
			m_horizontal.setSelection(0);
		}
		
		onScroll(null);
	}
	
	protected void onScroll(SelectionEvent e) {
		if (m_child != null) {
			m_child.setLocation(
				-m_horizontal.getSelection(),
				m_vertical.getVisible() ? -m_vertical.getSelection() : 0
			);
		}
	}
	
	public boolean handleKeyPressed(KeyEvent e) {
		if (m_child != null) {
			switch (e.keyCode) {
			case SWT.ARROW_DOWN:
				if (m_vertical.getVisible()) {
					m_vertical.setSelection(
						Math.min(m_vertical.getSelection() + m_vertical.getIncrement(), m_vertical.getMaximum())
					);
				}
				break;
			case SWT.ARROW_UP:
				if (m_vertical.getVisible()) {
					m_vertical.setSelection(
						Math.max(m_vertical.getSelection() - m_vertical.getIncrement(), m_vertical.getMinimum())
					);
				}
				break;
			case SWT.PAGE_DOWN:
				if (m_vertical.getVisible()) {
					m_vertical.setSelection(
						Math.min(m_vertical.getSelection() + m_vertical.getPageIncrement(), m_vertical.getMaximum())
					);
				}
				break;
			case SWT.PAGE_UP:
				if (m_vertical.getVisible()) {
					m_vertical.setSelection(
						Math.max(m_vertical.getSelection() - m_vertical.getPageIncrement(), m_vertical.getMinimum())
					);
				}
				break;
				
			case SWT.ARROW_RIGHT:
				if (m_horizontal.getVisible()) {
					m_horizontal.setSelection(
						Math.min(m_horizontal.getSelection() + m_horizontal.getIncrement(), m_horizontal.getMaximum())
					);
				}
				break;
			case SWT.ARROW_LEFT:
				if (m_horizontal.getVisible()) {
					m_horizontal.setSelection(
						Math.max(m_horizontal.getSelection() - m_horizontal.getIncrement(), m_horizontal.getMinimum())
					);
				}
				break;

			case SWT.HOME:
				if (m_vertical.getVisible()) {
					m_vertical.setSelection(m_vertical.getMinimum());
				}
				break;
			case SWT.END:
				if (m_vertical.getVisible()) {
					m_vertical.setSelection(m_vertical.getMaximum());
				}
				break;
			default:
				return false;
			}
			
			onScroll(null);
			return true;
		}
		return false;
	}
}
