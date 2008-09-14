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

package edu.mit.lcs.haystack.ozone.standard.modeless;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PaintHandler;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.DragAndDropHandler;
import edu.mit.lcs.haystack.ozone.core.utils.KeyHandler;
import edu.mit.lcs.haystack.ozone.core.utils.MouseHandler;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.dnd.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class ModelessPart extends VisualPartBase implements IModelessParent, IModelessCreator, IModelessPart, IBlockGUIHandler {
	protected Shell				m_shell;
	protected IModelessParent	m_parent;
	protected ArrayList			m_children = new ArrayList();
	protected IVisualPart		m_child;
	
	protected Rectangle			m_rectBase;
	protected boolean			m_alignTopOrBottom = false;
	protected Point				m_pointBase;
	
	protected Rectangle			m_childRectBase;
	protected Resource			m_exclusiveChild = null;
	protected ModelessPart		m_exclusiveChildPart = null;
	protected Point				m_mouseDown = new Point(0,0);
	protected TimerTask			m_exclusiveCreateTask = null;
	protected HashSet			m_timerTasks = new HashSet();

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ModelessPart.class);
	public static int s_timerInterval = 250;
	
	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {		
		m_shell.setBounds(r);
		
		IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
		if (blockGUIHandler != null) {
			blockGUIHandler.setBounds(m_shell.getClientArea());
		}
	}

	/**
	 * @see IVisualPart#setFocus()
	 */
	public void setFocus() {
		m_shell.setFocus();
	}

	/**
	 * @see VisualPartBase#onChildResize(ChildPartEvent)
	 */
	protected boolean onChildResize(ChildPartEvent e) {
		s_logger.info("Got child resize event");
		position();
		m_shell.redraw();
		return true;
	}

	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		m_parent = (IModelessParent) context.getProperty(ModelessConstants.MODELESS_PARENT);
		m_parent.registerModelessChild(this);

		super.initialize(source, context);

		m_shell = new Shell(m_parent.getShell(), SWT.NO_TRIM | SWT.POP_UP);
		
		addEventHandlers();
		createChildPart();
		
		m_shell.open();
		m_shell.setFocus();
	}
	
	protected void addEventHandlers() {
		m_shell.addShellListener(new ShellAdapter() {
			public void shellActivated(ShellEvent e) {
				disposeChildren();
			}
		});
		
		new PaintHandler() {
			protected void drawContent(GC gc, Rectangle r) {
				IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
				if (blockGUIHandler != null) {
					blockGUIHandler.draw(gc, r);
				}
			}
			protected void renderHTML(HTMLengine he) {
				IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
				if (blockGUIHandler != null) {
						blockGUIHandler.renderHTML(he);
				}	
			}
		}.initialize(m_shell);
		
		new MouseHandler() {
			protected boolean isDisposed() {
				return m_shell.isDisposed();
			}

			protected boolean handleEvent(Resource eventType, MouseEvent e) {
				if (m_child != null) {
					return m_child.handleEvent(eventType, e);
				}
				return false;
			}

			protected void onMouseDown(MouseEvent me) {
				m_mouseDown.x = me.x;
				m_mouseDown.y = me.y;
				super.onMouseDown(me);
			}

			protected void onMouseHover(MouseEvent me) {
				handleChildRectBase(me);
				super.onMouseHover(me);
			}

			protected void onMouseMove(MouseEvent me) {
				handleChildRectBase(me);
				super.onMouseMove(me);
			}

			protected void onMouseEnter(MouseEvent me) {
				handleChildRectBase(me);
				super.onMouseEnter(me);
			}
		}.initialize(m_shell);
		
		new KeyHandler() {
			protected boolean handleEvent(Resource eventType, KeyEvent e) {
				if (e.character == SWT.ESC && eventType.equals(PartConstants.s_eventKeyPressed)) {
					dispose();
					return true;
				} else if (m_child != null) {
					return m_child.handleEvent(eventType, e);
				} else {
					return false;
				}
			}

			protected boolean isDisposed() {
				return m_shell.isDisposed();
			}
		}.initialize(m_shell);
		
		new DragAndDropHandler() {
			protected Point getMouseDownPoint() {
				return m_mouseDown;
			}

			protected boolean handleDropEvent(
				Resource eventType,
				edu.mit.lcs.haystack.ozone.core.OzoneDropTargetEvent event) {

				if (!eventType.equals(PartConstants.s_eventDrop)) {
					Point pt = m_shell.toControl(new Point(event.m_x, event.m_y));
					event.m_x = pt.x;
					event.m_y = pt.y;
				}			
				if (!handleEvent(eventType, event)) {
					event.m_dropTargetEvent.detail = DND.DROP_NONE;
					return false;
				}
				return true;
			}

			protected boolean handleEvent(
				Resource eventType,
				EventObject event) {
					
				if (m_child != null) {
					return m_child.handleEvent(eventType, event);
				}
				return false;
			}

			protected boolean isDraggable() {
				return !m_shell.isDisposed();
			}

			protected boolean isDroppable() {
				return !m_shell.isDisposed();
			}
		}.initialize(m_shell);
	}
	
	protected void createChildPart() {
		Resource resData = (Resource) m_context.getProperty(OzoneConstants.s_partData);
		Resource resChild = Utilities.getResourceProperty(resData, ModelessConstants.CHILD, m_partDataSource);
		
		try {
			Resource resPart = Ozone.findPart(resChild, m_source, m_partDataSource);
			Class c = Utilities.loadClass(resPart, m_source);
			m_child = (IVisualPart)c.newInstance();

			Context ctx = new Context(m_context);
			ctx.putProperty(ModelessConstants.MODELESS_CREATOR, this);
			ctx.putProperty(ModelessConstants.MODELESS_PARENT, this);
			ctx.putLocalProperty(OzoneConstants.s_partData, resChild);
			ctx.putLocalProperty(OzoneConstants.s_parentPart, this);
			ctx.putLocalProperty(OzoneConstants.s_part, resPart);
			ctx.setSWTControl(m_shell);
			
			m_child.initialize(m_source, ctx);
			
			// Determine position
			
			m_pointBase = (Point) m_context.getLocalProperty(ModelessConstants.BASE_POINT);
			if (m_pointBase == null) {
				m_rectBase = (Rectangle) m_context.getLocalProperty(ModelessConstants.BASE_RECT);
				
				Boolean b = (Boolean) m_context.getLocalProperty(ModelessConstants.BASE_RECT_ALIGN);
				if (b != null) {
					m_alignTopOrBottom = b.booleanValue();
				}
			}
			
			position();
		} catch (Exception e) {
			s_logger.error("Failed to create child part for " + resChild, e);
		}
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		s_logger.info("Dispose");
		
		m_parent.unregisterModelessChild(this);
		
		disposeChildren();
		m_children = null;

		if (m_child != null) {		
			m_child.dispose();
			m_child = null;
		}
		
		if (m_shell != null) {
			m_shell.dispose();
			m_shell = null;		
		}
		
		try {
			m_exclusiveCreateTask.cancel();
		} catch (Exception e) {}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.standard.modeless.IModelessParent#disposeChildren()
	 */
	public void disposeChildren() {
		if (m_children != null) {
			while (m_children.size() > 0) {
				((ModelessPart)m_children.remove(0)).dispose();
			}
		}
	}

	/**
	 * @see see IModeless#asyncDispose()
	 */
	public void asyncDispose() {
		Ozone.idleExecFirst(new IdleRunnable(m_context) {
			public void run() {
				if (m_context != null) {
					dispose();
				}
			}
		});
	}

	/**
	 * @see IModelessParent#getShell()
	 */
	public Shell getShell() {
		return m_shell;
	}

	/**
	 * @see IModelessParent#registerModelessChild(ModelessPart)
	 */
	public void registerModelessChild(ModelessPart mp) {
		if (m_children == null) {
			m_parent.registerModelessChild(mp);
		} else {
			m_children.add(mp);
		}
	}

	/**
	 * @see IModelessParent#unregisterModelessChild(ModelessPart)
	 */
	public void unregisterModelessChild(ModelessPart mp) {
		if (mp == m_exclusiveChildPart) {
			m_exclusiveChildPart = null;
			m_exclusiveChild = null;
			m_childRectBase = null;
		}
		if (m_children == null) {
			m_parent.unregisterModelessChild(mp);
		} else {
			m_children.remove(mp);
		}
	}

	/**
	 * @see IModelessCreator#createModelessPart(Resource, Rectangle, boolean, boolean, Context)
	 */
	public ModelessPart createModelessPart(Resource resData, Rectangle rectBase, boolean alignTopOrBottom, boolean discardWhenOutsideBase, Context context) throws Exception {
		Context ctx = new Context(context);
		
		ctx.putLocalProperty(ModelessConstants.BASE_RECT, rectBase);
		ctx.putLocalProperty(ModelessConstants.BASE_RECT_ALIGN, new Boolean(alignTopOrBottom));
		
		if (discardWhenOutsideBase) {
			Point p = m_shell.toControl(new Point(rectBase.x, rectBase.y));
			Rectangle childRectBase = new Rectangle(p.x, p.y, rectBase.width, rectBase.height);
			if (m_exclusiveChild == null) {
				m_childRectBase = childRectBase;
				m_exclusiveChild = resData;
				return m_exclusiveChildPart = createModelessPart(resData, ctx);
			} else if (m_exclusiveChild.equals(resData) && m_childRectBase.equals(childRectBase)) {
				return m_exclusiveChildPart;
			} else {
				if (m_exclusiveCreateTask != null) {
					try {
						m_exclusiveCreateTask.cancel();
					} catch (Exception e) {}
				}
				Ozone.s_timer.schedule(m_exclusiveCreateTask = new TimerTask() {
					/**
					 * @see java.util.TimerTask#run()
					 */
					public void run() {
						asyncCreateExclusiveModelessPart(m_resData, m_ctx, m_childBase);
					}
					
					Context m_ctx;
					Resource m_resData;
					Rectangle m_childBase;
					
					TimerTask init(Resource resData2, Context ctx2, Rectangle childBase2) {
						m_resData = resData2;
						m_ctx = ctx2;
						m_childBase = childBase2;
						return this;
					}
				}.init(resData, ctx, childRectBase), s_timerInterval);
				return null;
			}
		}
		
		return createModelessPart(resData, ctx);
	}
	
	/**
	 * @see IModelessCreator#createModelessPart(Resource, Point, Context)
	 */
	public ModelessPart createModelessPart(Resource resData, Point pointBase, Context context) throws Exception {
		Context ctx = new Context(context);
		
		ctx.putLocalProperty(ModelessConstants.BASE_POINT, pointBase);
		
		return createModelessPart(resData, ctx);
	}
	
	protected void asyncCreateExclusiveModelessPart(Resource resData, Context ctx, Rectangle childBase) {
		Ozone.idleExec(new IdleRunnable(m_context) {
			public void run() {
				if (!isCursorInside() || m_resData.equals(m_exclusiveChild)) {
					return;
				}
				
				disposeExclusiveChild();
				m_childRectBase = m_childBase;
				m_exclusiveChild = m_resData;
				try {
					m_exclusiveChildPart = createModelessPart(m_resData, m_ctx);
				} catch (Exception e) {
					s_logger.error("Failed to asyncCreateExeclusiveModelessPart", e);
				}
			}

			Context m_ctx;
			Resource m_resData;
			Rectangle m_childBase;

			IdleRunnable init(Resource resData2, Context ctx2, Rectangle childBase2) {
				m_resData = resData2;
				m_ctx = ctx2;
				m_childBase = childBase2;
				return this;
			}
		}.init(resData, ctx, childBase));
	}
	
	protected ModelessPart createModelessPart(Resource resData, Context ctx) throws Exception {
		Resource resPart = Ozone.findPart(resData, m_source, m_partDataSource);
		Class c = Utilities.loadClass(resPart, m_source);
		ModelessPart mp = (ModelessPart)c.newInstance();
		
		ctx.putProperty(ModelessConstants.MODELESS_CREATOR, this);
		ctx.putProperty(ModelessConstants.MODELESS_PARENT, this);
		ctx.putProperty(OzoneConstants.s_partData, resData);
		ctx.putLocalProperty(OzoneConstants.s_parentPart, this);
		ctx.putProperty(OzoneConstants.s_part, resPart);
		ctx.setSWTControl(m_shell);
		
		mp.initialize(m_source, ctx);
		
		return mp;
	}

	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
		if (blockGUIHandler != null) {
			return blockGUIHandler.calculateSize(hintedWidth, hintedHeight);
		} else {
			return null;
		}
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.enter("ModelessPart");
		he.exit("ModelessPart");
	}
	
	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return null;
	}

	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.WIDTH;
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (cls == null || cls.equals(IBlockGUIHandler.class)) {
			return this;
		} else {
			return null;
		}
	}

	protected void position() {
		IBlockGUIHandler blockGUIHandler = (IBlockGUIHandler) m_child.getGUIHandler(IBlockGUIHandler.class);
		if (blockGUIHandler != null) {
			BlockScreenspace bs = null;
			
			switch (blockGUIHandler.getHintedDimensions()) {
			case IBlockGUIHandler.FIXED_SIZE:
				bs = blockGUIHandler.getFixedSize();
				break;
			case IBlockGUIHandler.WIDTH:
				bs = blockGUIHandler.calculateSize(200, -1);
				break;
			case IBlockGUIHandler.HEIGHT:
				bs = blockGUIHandler.calculateSize(-1, 200);
				break;
			case IBlockGUIHandler.BOTH:
				bs = blockGUIHandler.calculateSize(200, 200);
				break;
			}
			
			if (bs != null) {
				position(bs.m_size.x, bs.m_size.y);
			}
		}
	}
		
	protected void position(int width, int height) {
		Rectangle 	screen = m_shell.getDisplay().getClientArea();
		Point		point = new Point(0, 0);
		
		width = Math.min(width, screen.width);
		height = Math.min(height, screen.height);
		
		if (m_pointBase != null) {
			if (m_pointBase.x + width < screen.width) {
				point.x = m_pointBase.x;
			} else {
				point.x = Math.max(m_pointBase.x - width, 0);
			}

			if (m_pointBase.y + height < screen.height) {
				point.y = m_pointBase.y;
			} else {
				point.y = Math.max(m_pointBase.y - height, 0);
			}
		} else {
			if (m_alignTopOrBottom) {
				if (m_rectBase.y + m_rectBase.height + height < screen.height) {
					point.y = m_rectBase.y + m_rectBase.height;
				} else {
					point.y = m_rectBase.y - m_rectBase.height;
				}
				
				if (m_rectBase.x + height < screen.width) {
					point.x = m_rectBase.x;
				} else {
					point.x = m_rectBase.x + m_rectBase.width - width;
				}
			} else {
				if (m_rectBase.x + m_rectBase.width + width < screen.width) {
					point.x = m_rectBase.x + m_rectBase.width;
				} else {
					point.x = m_rectBase.x - width;
				}
				
				if (m_rectBase.y + height < screen.height) {
					point.y = m_rectBase.y;
				} else {
					point.y = m_rectBase.y + m_rectBase.height - height;
				}
			}
		}
		
		setBounds(new Rectangle(point.x, point.y, width, height));
	}
	
	protected void handleChildRectBase(MouseEvent e) {
		if (m_childRectBase != null) {
			if (!m_childRectBase.contains(e.x, e.y)) {
				TimerTask tt = new TimerTask() {
					/**
					 * @see java.util.TimerTask#run()
					 */
					public void run() {
						asyncDisposeChild(m_child);
					}
						
					ModelessPart m_child;
						
					TimerTask init(ModelessPart child2) {
						m_child = child2;
						return this;
					}
				}.init(m_exclusiveChildPart);
				Ozone.s_timer.schedule(tt, s_timerInterval);
				m_timerTasks.add(tt);
			} else {
				resetExclusiveTimer();
			}		
		}
	}

	protected void disposeExclusiveChild() {
		if (m_exclusiveChildPart != null) {
			m_exclusiveChildPart.dispose();
		}
	}

	protected void asyncDisposeChild(ModelessPart child) {
		if (child != null) {
			Ozone.idleExecFirst(new IdleRunnable(m_context) {
				public void run() {
					if (isCursorInside()) {
						m_child.dispose();
					}
				}
				
				ModelessPart m_child;
				
				IdleRunnable init(ModelessPart child2) {
					m_child = child2;
					return this;
				}
			}.init(child));
		}
	}
	
	protected void resetExclusiveTimer() {
		Iterator i = m_timerTasks.iterator();
		while (i.hasNext()) {
			try {
				((TimerTask) i.next()).cancel();
			} catch (Exception e) {
			}
		}
		m_timerTasks.clear();
		try {
			m_exclusiveCreateTask.cancel();
		} catch (Exception e) {
		}
	}
	
	protected boolean isCursorInside() {
		Point pt = Ozone.s_display.getCursorLocation();
		try {
			return getShell().getBounds().contains(pt);
		} catch (Exception e) {
			return false;
		}
	}
}
