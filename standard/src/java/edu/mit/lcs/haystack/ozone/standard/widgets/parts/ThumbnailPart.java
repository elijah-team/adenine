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

import edu.mit.lcs.haystack.content.ContentClient;
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
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.ContentHittestEvent;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsUtilities;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.server.core.content.IContentService;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;

import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class ThumbnailPart extends VisualPartBase implements IBlockGUIHandler {
	Resource		m_picture;
	int			m_maxDimension = 100;
	Point			m_size;
	Image			m_image;
	RDFListener		m_rdfListener;
	
	final static Resource PICTURE = new Resource(PartConstants.s_namespace + "picture");
	final static Resource MAX_DIMENSION = new Resource(PartConstants.s_namespace + "maxDimension");
	
	final static Resource THUMBNAIL = new Resource("http://haystack.lcs.mit.edu/schemata/picture#thumbnail");
	final static Resource CONTENT = new Resource("http://haystack.lcs.mit.edu/schemata/picture#content");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ThumbnailPart.class);
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_image != null) {
			m_image.dispose();
			m_image = null;
		}
		m_picture = null;
		m_size = null;
		
		m_rdfListener.stop();
		m_rdfListener = null;
		
		super.dispose();		
	}
	
	/**
	 * Retrieves the part data resource and any image source.
	 */
	protected void internalInitialize() {
		super.internalInitialize();
		
		m_picture = Utilities.getResourceProperty(m_prescription, PICTURE, m_partDataSource);
		if (m_picture == null) {
			Context context = m_context;
			while (context != null) {
				m_picture = (Resource) context.getLocalProperty(OzoneConstants.s_underlying);
			
				if (m_picture != null) {
					break;
				}
			
				context = context.getParentContext();
			}
		}
		
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource) m_infoSource) {
			public void statementRemoved(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					public void run() {
						getThumbnail();
						onChildResize(new ChildPartEvent(ThumbnailPart.this));
					}
				});
			}
		};
		m_rdfListener.start();
		try {
			m_rdfListener.addPattern(m_picture, THUMBNAIL, null);
		} catch (RDFException e) {
			s_logger.error("Failed to watch for thumbnail for " + m_picture, e);
		}
		
		String s = Utilities.getLiteralProperty(m_prescription, MAX_DIMENSION, m_partDataSource);
		if (s != null) {
			m_maxDimension = Math.max(1000, Integer.parseInt(s));
		}
		
		getThumbnail();
	}
	
	protected void getThumbnail() {
		if (m_image != null) {
			m_image.dispose();
			m_image = null;
		}

		Resource thumbnail = null;
		try {
			thumbnail = Utilities.getResourceProperty(m_picture, THUMBNAIL, m_infoSource);
			if (thumbnail != null) {
				m_image = new Image(
					Ozone.s_display, 
					ContentClient.getContentClient(
						Utilities.getResourceProperty(thumbnail, CONTENT, m_infoSource), 
						m_source,
						m_context.getServiceAccessor()
					).getContent()
				);
				Rectangle r = m_image.getBounds();
				m_size = new Point(r.width, r.height);
			}
		} catch (Exception e) {
			s_logger.error("Failed to retrieve existing thumbnail for " + m_picture, e);
			thumbnail = null;
		}

		try {
			if (thumbnail == null) {
				IContentService cs = ContentClient.getContentService(m_source, m_context.getServiceAccessor(), m_context.getUserIdentity().getResource());
								
				GraphicsUtilities.ImageAndURI iau = GraphicsUtilities.scaleImageToFit2(
					Utilities.getResourceProperty(m_picture, CONTENT, m_infoSource),
					cs,
					m_infoSource,
					m_context.getServiceAccessor(),
					m_maxDimension,
					m_maxDimension
				);
				
				m_image = iau.m_image;
				Rectangle r = m_image.getBounds();
				m_size = new Point(r.width, r.height);

				thumbnail = Utilities.generateUniqueResource();
				
				m_infoSource.replace(m_picture, THUMBNAIL, null, thumbnail);
				m_infoSource.add(new Statement(thumbnail, CONTENT, iau.m_uri));
			}
		} catch (Exception e) {
			s_logger.error("Failed to generate thumbnail for " + m_picture, e);
			m_size = new Point(0, 0);
		}
	}
	
	/**
	 * @see IBlockGUIHandler#calculateSize(int, int)
	 */
	public BlockScreenspace calculateSize(int hintedWidth, int hintedHeight) {
		return null;
	}

	/**
	 * @see IBlockGUIHandler#getFixedSize()
	 */
	public BlockScreenspace getFixedSize() {
		return new BlockScreenspace(m_size);
	}

	/**
	 * @see IBlockGUIHandler#draw(GC, Rectangle)
	 */
	public void draw(GC gc, Rectangle r) {
		if (m_image != null) {
			gc.drawImage(m_image, r.x, r.y);
		}
	}

	
	/**
	 * @see IBlockGUIHandler#renderHTML(HTMLengine he)
	 */
	public void renderHTML(HTMLengine he) {
		if (m_image != null) he.unimplemented("Thumbnail");
	}
	
	/**
	 * @see IBlockGUIHandler#getHintedDimensions()
	 */
	public int getHintedDimensions() {
		return IBlockGUIHandler.FIXED_SIZE;
	}

	/**
	 * @see IBlockGUIHandler#getTextAlign()
	 */
	public int getTextAlign() {
		return BlockScreenspace.ALIGN_TEXT_CLEAR;
	}

	/**
	 * @see IBlockGUIHandler#setBounds(Rectangle)
	 */
	public void setBounds(Rectangle r) {
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

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.VisualPartBase#onContentHittest(org.eclipse.swt.events.MouseEvent)
	 */
	protected boolean onContentHittest(ContentHittestEvent e) {
		return true;
	}
}
