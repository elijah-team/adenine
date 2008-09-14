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

package edu.mit.lcs.haystack.ozone.standard.widgets.slide;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import edu.mit.lcs.haystack.ozone.core.BlockScreenspace;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IBlockGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.ozone.data.DataUtilities;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.ozone.data.ResourceDataConsumer;
import edu.mit.lcs.haystack.ozone.data.ResourceDataProviderWrapper;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class ImageElement extends VisualPartBase implements IBlockGUIHandler {
	transient Image			m_image;
	transient Image			m_hoverImage;
	Resource		m_resSource;
	int				m_textAlign;
	int				m_baseLineOffset = 0;
	boolean			m_hovered = false;
	boolean			m_scaleToFit = false;
	Rectangle		m_rect;
	String			m_textAlignString;
	String			m_baseLineOffsetString;
	protected int		m_width = -1;
	protected int		m_height = -1;
	String 			m_imageURI;
		
	ResourceDataConsumer		m_dataConsumer;
	ResourceDataProviderWrapper	m_dataProviderWrapper;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().initializeFromDeserialization(source);
		}
		retrieveImageData(true);
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		SlideUtilities.releaseAmbientProperties(m_context);
		
		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().unregisterConsumer(m_dataConsumer);
			m_dataProviderWrapper.dispose();
			m_dataProviderWrapper = null;
		}
		m_dataConsumer = null;
		
		GraphicsManager.releaseImage(m_image);
		m_image = null;
		GraphicsManager.releaseImage(m_hoverImage);
		m_hoverImage = null;
		
		m_resSource = null;
		
		super.dispose();		
	}
	
	/**
	 * Retrieves the part data resource and any image source.
	 */
	boolean m_initializing = true;
	protected void internalInitialize() {
		super.internalInitialize();
		
		SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);

		m_textAlignString = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_textAlign, m_partDataSource);
		if (m_textAlignString != null) m_CSSstyle.setAttribute("text-align", m_textAlignString);

		m_baseLineOffsetString = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_baseLineOffset, m_partDataSource);
		m_scaleToFit = Utilities.checkBooleanProperty(m_prescription, SlideConstants.s_scaleToFit, m_partDataSource);
		
		String x;
		x = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_width, m_partDataSource);
		if (x != null) {
			m_width = Integer.parseInt(x); 
			m_CSSstyle.setAttribute("width", x);
		}
		x = Utilities.getLiteralProperty(m_prescription, SlideConstants.s_height, m_partDataSource);
		if (x != null) {
			m_height = Integer.parseInt(x); 
			m_CSSstyle.setAttribute("height", x);
		}
		
		initializeSources();
		
		m_initializing = false;
	}
	
	protected void initializeSources() {
		m_resSource = Utilities.getResourceProperty(m_prescription, SlideConstants.s_source, m_partDataSource);
		if (m_resSource != null) {
			retrieveImageData(false);
		} else {
			Resource dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (dataSource == null) {
				return;
			}
			
			IDataProvider dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
			if (dataProvider == null) {
				return;
			}
			
			m_dataProviderWrapper = new ResourceDataProviderWrapper(dataProvider);
			m_dataConsumer = new ResourceDataConsumer() {
				protected void onResourceChanged(Resource newResource) {
					m_resSource = newResource;
					if (Ozone.isUIThread() && m_initializing) {
						retrieveImageData(true);
					} else {
						Ozone.idleExec(new IdleRunnable(m_context) {
							public void run() {
								retrieveImageData(true);
							}
						});
					}
				}
				
				protected void onResourceDeleted(Resource previousResource) {
					m_resSource = null;
					if (Ozone.isUIThread() && m_initializing) {
						retrieveImageData(true);
					} else {
						Ozone.idleExec(new IdleRunnable(m_context) {
							public void run() {
								retrieveImageData(true);
							}
						});
					}
				}
			};
			dataProvider.registerConsumer(m_dataConsumer);
		}		
	}
	
	int m_calculatedWidth;
	int m_calculatedHeight;
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		Rectangle r = m_image.getBounds();
		if (hintedWidth == -1 && hintedHeight == -1) {
			hintedWidth = r.width;
			hintedHeight = r.height;
		} else if (hintedWidth == -1) {
			hintedWidth = hintedHeight * r.width / r.height;
		} else if (hintedHeight == -1) {
			hintedHeight = hintedWidth * r.height / r.width;
		} else if ((hintedHeight * r.width / r.height) > hintedWidth) {
			hintedHeight = hintedWidth * r.height / r.width;
		} else {
			hintedWidth = hintedHeight * r.width / r.height;
		}
		return new BlockScreenspace(m_calculatedWidth = hintedWidth, m_calculatedHeight = hintedHeight);
	}

	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		if (m_image != null) {
			Rectangle r = m_image.getBounds();
			int width = m_width;
			int height = m_height;
			if (width == -1) {
				width = r.width;
			}
			if (height == -1) {
				height = r.height;
			}
			return new BlockScreenspace(width, height, m_textAlign, m_baseLineOffset);
		} else {
			return new BlockScreenspace(BlockScreenspace.ALIGN_TEXT_BASE_LINE);
		}
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (m_image != null) {
			Boolean hovered = (Boolean) m_context.getProperty(PartConstants.s_hovered);
			if (hovered == null) {
				hovered = new Boolean(false);
			}

			if (m_scaleToFit) {
				Rectangle r2 = m_image.getBounds();
				gc.drawImage((m_hovered || hovered.booleanValue()) && m_hoverImage != null ? m_hoverImage : m_image, 0, 0, r2.width, r2.height, r.x, r.y, m_calculatedWidth, m_calculatedHeight);
			} else {
				Rectangle r2 = m_image.getBounds();
				if ((m_width != -1 && r2.width != m_width) || (m_height != -1 && r2.height != m_height)) {
					gc.drawImage((m_hovered || hovered.booleanValue()) && m_hoverImage != null ? m_hoverImage : m_image, 0, 0, r2.width, r2.height, r.x, r.y, m_width == -1 ? r2.width : m_width, m_height == -1 ? r2.height : m_height);
				} else {
					gc.drawImage((m_hovered || hovered.booleanValue()) && m_hoverImage != null ? m_hoverImage : m_image, r.x, r.y);
				}
			}
		}
	}
	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		he.image(m_CSSstyle, m_imageURI, m_tooltip, this, "ImageElement");
	}
	
	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return m_scaleToFit ? IBlockGUIHandler.WIDTH : IBlockGUIHandler.FIXED_SIZE;
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		if (m_image != null) {
			return m_textAlign;
		} else {
			return BlockScreenspace.ALIGN_TEXT_BASE_LINE;
		}
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
		m_rect = new Rectangle(r.x, r.y, r.width, r.height);
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
	
	protected boolean onMouseEnter(MouseEvent e) {
		Control c = (Control) m_context.getSWTControl();
		if (m_resOnClick != null) {
			m_hovered = true;
			if (m_rect != null) {
				repaint(m_rect);
			}
		}
		return super.onMouseEnter(e);
	}

	protected boolean onMouseExit(MouseEvent e) {
		Control c = (Control) m_context.getSWTControl();
		if (m_resOnClick != null) {
			m_hovered = false;
			if (m_rect != null) {
				repaint(m_rect);
			}
		}
		return super.onMouseExit(e);
	}
	
	protected void retrieveImageData(boolean signalResize) {
		String s;
		
		GraphicsManager.releaseImage(m_image);
		m_image = null;
		GraphicsManager.releaseImage(m_hoverImage);
		m_hoverImage = null;
		if (m_resSource == null) return;
		
		m_textAlign = BlockScreenspace.ALIGN_TEXT_CENTER;
		
		if (m_textAlignString != null) {
			if (m_textAlignString.equalsIgnoreCase("top")) {
				m_textAlign = BlockScreenspace.ALIGN_LINE_TOP;
			} else if (m_textAlignString.equalsIgnoreCase("bottom")) {
				m_textAlign = BlockScreenspace.ALIGN_LINE_BOTTOM;
			} else if (m_textAlignString.equalsIgnoreCase("baseline")) {
				m_textAlign = BlockScreenspace.ALIGN_TEXT_BASE_LINE;
			}
		}
		
		if (m_resOnClick != null || m_context.getProperty(PartConstants.s_hovered) != null) {
			m_image = GraphicsManager.acquireImage(m_resSource, SlideUtilities.getAmbientLinkColor(m_context), 100, m_infoSource, m_context.getServiceAccessor());
			m_hoverImage = GraphicsManager.acquireImage(m_resSource, SlideUtilities.getAmbientLinkHoverColor(m_context), 100, m_infoSource, m_context.getServiceAccessor());
		} else {
			m_image = GraphicsManager.acquireImage(m_resSource, SlideUtilities.getAmbientColor(m_context), 100, m_infoSource, m_context.getServiceAccessor());
		}
		//if (OzoneServlet.enabled()) m_imageURI = GraphicsManager.getImageURI(m_image);
		
		Rectangle originalSize = m_image.getBounds();
		int height = m_height == -1 ?  originalSize.height : m_height;
		
		if (m_textAlign == BlockScreenspace.ALIGN_TEXT_BASE_LINE) {
			if (m_baseLineOffsetString != null) {
				m_baseLineOffset = Integer.parseInt(m_baseLineOffsetString);
			}
			m_baseLineOffset += height;
		} else if (m_textAlign == BlockScreenspace.ALIGN_TEXT_CENTER) {
			FontMetrics fontMetrics = Ozone.getFontMetrics(SlideUtilities.getAmbientFont(m_context));
			
			m_baseLineOffset = (fontMetrics.getAscent() + height) / 2;
			
			m_textAlign = BlockScreenspace.ALIGN_TEXT_BASE_LINE;
		} else {
			m_baseLineOffset = 0;
		}
		
		if (signalResize) {
			onChildResize(new ChildPartEvent(this));
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHittest(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onContentHittest(ContentHittestEvent e) {
		return true;
	}

}
