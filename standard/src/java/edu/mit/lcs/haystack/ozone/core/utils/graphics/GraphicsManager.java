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

package edu.mit.lcs.haystack.ozone.core.utils.graphics;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.internal.Compatibility;

import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.http.OzoneServlet;

/**
 * @author David Huynh
 */
public class GraphicsManager {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(GraphicsManager.class);

	static Hashtable	s_fontsByFamily = new Hashtable();
	static Hashtable	s_fontsByObject = new Hashtable();
	
	static Hashtable	s_colors 		= new Hashtable();
	static Hashtable	s_defaultColors = new Hashtable();
	static Hashtable	s_luminantColors = new Hashtable();
	
	static Hashtable	s_imagesByURI	= new Hashtable();
	static Hashtable	s_imagesByObject = new Hashtable();
	
	static int s_cachedImages = 0;
	
	
	public static final Color	s_red   		= new Color(Ozone.s_display, new RGB(255,0,0));
	public static final Color	s_green 		= new Color(Ozone.s_display, new RGB(0,255,0));
	public static final Color	s_blue  		= new Color(Ozone.s_display, new RGB(0,0,255));
	
	public static final Color	s_white 		= new Color(Ozone.s_display, new RGB(0xff,0xff,0xff));
	public static final Color	s_lightestGray 	= new Color(Ozone.s_display, new RGB(0xee,0xee,0xee));
	public static final Color	s_lighterGray 	= new Color(Ozone.s_display, new RGB(0xcc,0xcc,0xcc));
	public static final Color	s_lightGray 	= new Color(Ozone.s_display, new RGB(0xaa,0xaa,0xaa));
	public static final Color	s_gray  		= new Color(Ozone.s_display, new RGB(0x88,0x88,0x88));
	public static final Color	s_darkGray  	= new Color(Ozone.s_display, new RGB(0x66,0x66,0x66));
	public static final Color	s_darkerGray  	= new Color(Ozone.s_display, new RGB(0x44,0x44,0x44));
	public static final Color	s_darkestGray  	= new Color(Ozone.s_display, new RGB(0x22,0x22,0x22));
	public static final Color	s_black 		= new Color(Ozone.s_display, new RGB(0,0,0));

	public static final Color	s_tan   		= new Color(Ozone.s_display, new RGB(255,255,0xdd));
	
	public static final Cursor 	s_arrowCursor	= new Cursor(Ozone.s_display, SWT.CURSOR_ARROW);
	public static final Cursor 	s_handCursor	= new Cursor(Ozone.s_display, SWT.CURSOR_HAND);
	public static final Cursor 	s_noCursor		= new Cursor(Ozone.s_display, SWT.CURSOR_NO);
	public static final Cursor 	s_horzResizeCursor	= new Cursor(Ozone.s_display, SWT.CURSOR_SIZEWE);
	public static final Cursor 	s_vertResizeCursor	= new Cursor(Ozone.s_display, SWT.CURSOR_SIZENS);
	public static final Cursor 	s_nwseResizeCursor	= new Cursor(Ozone.s_display, SWT.CURSOR_SIZENWSE);
	public static final Cursor 	s_neswResizeCursor	= new Cursor(Ozone.s_display, SWT.CURSOR_SIZENESW);
	
	final static Resource LUMINANT				= new Resource(OzoneConstants.s_namespace + "luminant");

	final static Resource PICTURE_WIDTH = new Resource("http://haystack.lcs.mit.edu/schemata/picture#width");
	final static Resource PICTURE_HEIGHT = new Resource("http://haystack.lcs.mit.edu/schemata/picture#height");

	public static void initialize() {
		makeDefaultColor("red", 			s_red);
		makeDefaultColor("green", 			s_green);
		makeDefaultColor("blue", 			s_blue);
		
		makeDefaultColor("white", 			s_white);
		makeDefaultColor("lightestGray", 	s_lightestGray);
		makeDefaultColor("lighterGray", 	s_lighterGray);
		makeDefaultColor("lightGray", 		s_lightGray);
		makeDefaultColor("gray", 			s_gray);
		makeDefaultColor("darkGray", 		s_darkGray);
		makeDefaultColor("darkerGray", 		s_darkerGray);
		makeDefaultColor("darkestGray", 	s_darkestGray);
		makeDefaultColor("black", 			s_black);
		
		makeDefaultColor("tan", 			s_tan);
	}
	
	public static synchronized void uninitialize() {
	}

	public static synchronized Font acquireFont(FontDescription description) {
		String fontFamily = null;
		
		if (description.m_family != null) {
			fontFamily = description.m_family;
		} 
		else if (description.m_families != null) {
		    Iterator i = description.m_families.iterator();
		    
		    /*	Find the first font family that is installed 
			on the system.
		     */
		    while (i.hasNext()) {
			String 		family = (String) i.next();
			FontData[]	fontData = Ozone.s_display.getFontList(family, true);
					
			if (fontData.length > 0 && fontData[0] != null && fontData[0].getName().equalsIgnoreCase(family)) {
			    fontFamily = family;
			    break;
			}
		    }
		} 


		if (fontFamily == null) {
			fontFamily = Ozone.s_gc.getFont().getFontData()[0].getName();
//		    fontFamily = Ozone.s_display.getSystemFont().getFontData()[0].getName();
		}
		
		//here's the problem. fontFamily == null
		List entryList = (List) s_fontsByFamily.get(fontFamily);
				    

		/*	Try looking up for an existing entry.
		 */
		if (entryList != null) {
			Iterator iEntry = entryList.iterator();
			
			while (iEntry.hasNext()) {
				FontEntry entry = (FontEntry) iEntry.next();
				
				if (entry.m_size == description.m_size &&
					entry.m_bold == description.m_bold &&
					entry.m_italic == description.m_italic) {
						
					entry.m_use++;
					return entry.m_font;
				}
			}
		}
		
		/*	Create a new entry.
		 */
		if (entryList == null) {
			entryList = new ArrayList();
			
			s_fontsByFamily.put(fontFamily, entryList);
		}
		
		FontEntry entry = new FontEntry();
		{
			entry.m_size = description.m_size;
			entry.m_bold = description.m_bold;
			entry.m_italic = description.m_italic;
			
			int style = SWT.NORMAL;
			
			if (description.m_bold) 	style |= SWT.BOLD;
			if (description.m_italic) 	style |= SWT.ITALIC;

			int size = description.m_size;
			entry.m_font = new Font(
				Ozone.s_display,
				fontFamily,
				size,
				style
			);
			
			entry.m_use = 1;
		}
		
		entryList.add(entry);
		s_fontsByObject.put(entry.m_font, entryList);
		
		return entry.m_font;
	}
	
	public static synchronized void releaseFont(Font font) {
		if (font != null) {
			List entryList = (List) s_fontsByObject.get(font);
			
			if (entryList != null) {
				Iterator i = entryList.iterator();
				
				while (i.hasNext()) {
					FontEntry entry = (FontEntry) i.next();
					
					if (entry.m_font == font) {
						entry.m_use--;
						
						if (entry.m_use == 0) {
							entry.m_font.dispose();
							
							entryList.remove(entry);
							s_fontsByObject.remove(entry.m_font);
						}
						break;
					}
				}
				
				if (entryList.size() == 0) {
					s_fontsByObject.remove(font);
				}
			}
		}
	}
	
	public static synchronized Color acquireColor(String description, Color referenceColor) {
		if (description == null) {
			return acquireColorByRGB(referenceColor.getRGB());
		} else if (description.startsWith("#")) {
			if (description.length() == 7) {
				return acquireColorByRGB(
					new RGB(
						Integer.parseInt(description.substring(1, 3), 16),
						Integer.parseInt(description.substring(3, 5), 16),
						Integer.parseInt(description.substring(5, 7), 16)
					)
				);
			} else {
				return null;
			}
		} else if (description.endsWith("%")) {
			return acquireColorByLuminance(
				Integer.parseInt(description.substring(0, description.length() - 1)),
				referenceColor
			);
		} else {
			return acquireColorByName(description);
		}
	}
	
	public static synchronized Color acquireColorByLuminance(int luminance, Color referenceColor) {
		RGB 				referenceRGB = referenceColor.getRGB();
		Hashtable			luminantTable = (Hashtable) s_luminantColors.get(referenceRGB);
		RGB					rgb = null;
		
		if (luminantTable != null) {
			rgb = (RGB) luminantTable.get(new Integer(luminance));
		}
		
		if (rgb == null) {
			int	red 		= referenceColor.getRed();
			int	green 		= referenceColor.getGreen();
			int	blue 		= referenceColor.getBlue();
			
			int	redBase		= red < 128 ? 255 : 0;
			int	greenBase	= green < 128 ? 255 : 0;
			int	blueBase	= blue < 128 ? 255 : 0;
			
			int	redScalar	= red < 128 ? (red - 255) : red;
			int	greenScalar = green < 128 ? (green - 255) : green;
			int	blueScalar	= blue < 128 ? (blue - 255) : blue;
			
			int	luminance2 = Math.max(0, luminance);
			
			rgb = new RGB(
				Math.max(0, Math.min(255, redBase + (luminance2 * redScalar / 100))),
				Math.max(0, Math.min(255, greenBase + (luminance2 * greenScalar / 100))),
				Math.max(0, Math.min(255, blueBase + (luminance2 * blueScalar / 100)))
			);
			
			if (luminantTable == null) {
				luminantTable = new Hashtable();
				s_luminantColors.put(referenceRGB, luminantTable);
			}
			
			luminantTable.put(new Integer(luminance), rgb);
		}
				
		return acquireColorByRGB(rgb);
	}
	
	public static synchronized Color acquireColorByRGB(RGB rgb) {
		ColorEntry entry = (ColorEntry) s_colors.get(rgb);
		
		if (entry != null) {
			entry.m_use++;
		} else {
			entry = new ColorEntry();
			entry.m_color = new Color(Ozone.s_display, rgb);
			entry.m_use = 1;
			
			s_colors.put(rgb, entry);
		}
		
		return entry.m_color;
	}
	
	public static synchronized Color acquireColorBySample(Color c) {
		return c == null ? null : acquireColorByRGB(c.getRGB());
	}
	
	public static synchronized Color acquireColorByName(String name) {
		ColorEntry entry = (ColorEntry) s_defaultColors.get(name);
		
		if (entry != null) {
			entry.m_use++;
			
			return entry.m_color;
		} else {
			return null;
		}
	}
	
	public static synchronized void releaseColor(Color color) {
		if (color != null && !color.isDisposed()) {
			ColorEntry entry = (ColorEntry) s_colors.get(color.getRGB());
			
			if (entry != null) {
				entry.m_use--;
				
				if (entry.m_use == 0) {
					s_colors.remove(color.getRGB());
					
					entry.m_color.dispose();
				}
			} else {
				color.dispose();
			}
		}
	}
	
	public static synchronized boolean isColorDisposed(Color color) {
		if (color == null) return false;
		ColorEntry entry = (ColorEntry) s_colors.get(color.getRGB());
		return (entry != null && entry.m_use <= 0);
	}
	
	public static synchronized Image acquireImage(Resource uri, Color referenceColor, int luminance, IRDFContainer source, IServiceAccessor sa) {
		OriginalImageEntry originalEntry = (OriginalImageEntry) s_imagesByURI.get(uri);
		
		if (originalEntry == null) {
			Image image = null;
			
			try {
				image = new Image(Ozone.s_display, ContentClient.getContentClient(uri, source, sa).getContent());
			} catch(MalformedURLException e) {
				s_logger.error("Malformed image URI " + uri.getURI(), e);
				return null;
			} catch(IOException e) {
				s_logger.error("I/O exception while loading image at " + uri.getURI(), e);
				return null;
			} catch(Exception e) {
				s_logger.error("Exception while loading image at " + uri.getURI(), e);
				return null;
			}
			
			try {
				source.replace(uri, PICTURE_WIDTH, null, new Literal(Integer.toString(image.getBounds().width)));
				source.replace(uri, PICTURE_HEIGHT, null, new Literal(Integer.toString(image.getBounds().height)));
			} catch (RDFException e) {
			}
			
			originalEntry = new OriginalImageEntry();
			
			originalEntry.m_referencedImages = new Hashtable();
			originalEntry.m_uri = uri;
			originalEntry.m_luminant = Utilities.checkBooleanProperty(uri, LUMINANT, source);
			originalEntry.m_image = image;
			originalEntry.m_use = 0;
			
			s_imagesByURI.put(uri, originalEntry);
			s_imagesByObject.put(originalEntry.m_image, originalEntry);
		}
		
		if (!originalEntry.m_luminant) {
			originalEntry.m_use++;
			return originalEntry.m_image;
		}
		
		Hashtable byRGB = (Hashtable) originalEntry.m_referencedImages.get(referenceColor.getRGB());
		if (byRGB == null) {
			byRGB = new Hashtable();
			originalEntry.m_referencedImages.put(referenceColor.getRGB(), byRGB);
		}
		
		LuminantImageEntry entry = (LuminantImageEntry) byRGB.get(new Integer(luminance));
		if (entry == null) {
			ImageData	imageData = originalEntry.m_image.getImageData();
			PaletteData	palette = imageData.palette;
			RGB[]		rgbs = palette.colors;
			
			int		red 	= referenceColor.getRed();
			int		green 	= referenceColor.getGreen();
			int		blue 	= referenceColor.getBlue();
			
			int		redBase 	= red < 128 ? 255 : 0;
			int		greenBase 	= green < 128 ? 255 : 0;
			int		blueBase 	= blue < 128 ? 255 : 0;
			
			int		redScalar 	= red < 128 ? (red - 255) : red;
			int		greenScalar = green < 128 ? (green - 255) : green;
			int		blueScalar 	= blue < 128 ? (blue - 255) : blue;
			
			if (rgbs != null) {		
				for (int i = 0; i < rgbs.length; i++) {
					if (imageData.transparentPixel != i) {
						rgbs[i].red = Math.max(0, Math.min(255, redBase + ((255 - rgbs[i].red) * luminance / 100) * redScalar / 255));
						rgbs[i].green = Math.max(0, Math.min(255, greenBase + ((255 - rgbs[i].green) * luminance / 100) * greenScalar / 255));
						rgbs[i].blue = Math.max(0, Math.min(255, blueBase + ((255 - rgbs[i].blue) * luminance / 100) * blueScalar / 255));
					}
				}
			}
			
			ImageData imageDataNew = new ImageData(imageData.width, imageData.height, imageData.depth, palette, imageData.scanlinePad, imageData.data);
			imageDataNew.transparentPixel = imageData.transparentPixel;
			
			entry = new LuminantImageEntry();
			entry.m_image = new Image(Ozone.s_display, imageDataNew);
			entry.m_rgb = referenceColor.getRGB();
			entry.m_luminance = luminance;
			entry.m_originalEntry = originalEntry;
			entry.m_use = 0;

			byRGB.put(new Integer(luminance), entry);			
			s_imagesByObject.put(entry.m_image, entry);
		}

		entry.m_use++;
		originalEntry.m_use++;
		return entry.m_image;
	}
	
	public static synchronized String getImageURI(Image image) {
		if (image == null) return null;
		Object o = s_imagesByObject.get(image);
		if (o instanceof OriginalImageEntry) return ((OriginalImageEntry) o).m_uri.getURI().replaceAll("http://haystack.lcs.mit.edu/", "");
		if (o instanceof LuminantImageEntry) {
			LuminantImageEntry entry = (LuminantImageEntry) o;
			if (entry.m_uriString == null) {
				entry.m_uriString = "Image" + (s_cachedImages++) + ".gif";
				ImageLoader loader = new ImageLoader();
				loader.data = new ImageData[1];
				loader.data[0] = entry.m_image.getImageData();
				String fName = OzoneServlet.directory().getPath() + File.separator + entry.m_uriString;
				// TODO: (HTML interface) Find better way to avoid exception in loader.save when Web interface is not running
				try { Compatibility.newFileOutputStream(fName); } 
				catch (IOException e) {	return null; }
				s_logger.info("Caching " + fName);
				loader.save(fName, SWT.IMAGE_GIF);
			}
			return entry.m_uriString;
		}
		return null;
	}
	
	
	public static synchronized void releaseImage(Image image) {
		if (image != null) {
			Object o = s_imagesByObject.get(image);
			
			if (o != null) {
				OriginalImageEntry originalEntry = null;
				
				if (o instanceof OriginalImageEntry) {
					originalEntry = (OriginalImageEntry) o;
				} else if (o instanceof LuminantImageEntry) {
					LuminantImageEntry luminantEntry = (LuminantImageEntry) o;
					
					originalEntry = luminantEntry.m_originalEntry;
					
					luminantEntry.m_use--;
					if (luminantEntry.m_use == 0) {						
						Hashtable byRGB = (Hashtable) originalEntry.m_referencedImages.get(luminantEntry.m_rgb);
						
						byRGB.remove(new Integer(luminantEntry.m_luminance));
						if (byRGB.isEmpty()) {
							originalEntry.m_referencedImages.remove(luminantEntry.m_rgb);
						}
						
						s_imagesByObject.remove(image);
	
						luminantEntry.m_image.dispose();
						luminantEntry.m_image = null;
						luminantEntry.m_originalEntry = null;
						luminantEntry.m_rgb = null;
						if (luminantEntry.m_uriString != null) {
							// TODO: (HTML interface) Remove cached image from Ozone web interface directory
						}
					}
				}
				
				originalEntry.m_use--;
				if (originalEntry.m_use == 0) {
					s_imagesByObject.remove(originalEntry.m_image);
					s_imagesByURI.remove(originalEntry.m_uri);
					
					originalEntry.m_image.dispose();
					originalEntry.m_image = null;
					originalEntry.m_referencedImages.clear();
					originalEntry.m_referencedImages = null;
					originalEntry.m_uri = null;
				}
			}
		}
	}

	private static void makeDefaultColor(String name, Color color) {
		ColorEntry entry = makeColorEntry(color);
		
		s_defaultColors.put(name, entry);
		s_colors.put(color.getRGB(), entry);
	}
	
	private static ColorEntry makeColorEntry(Color color) {
		ColorEntry entry = new ColorEntry();
		
		entry.m_color = color;
		entry.m_use = 1;
		
		return entry;
	}
	
}

class FontEntry {
	public int		m_size;
	
	public boolean	m_bold;
	public boolean	m_italic;
	
	public Font		m_font;
	
	public int		m_use;
}

class ColorEntry {
	public Color		m_color;
	public int		m_use;
	public List		m_list = new ArrayList();
}

class OriginalImageEntry {
	public Resource	m_uri;
	
	public boolean	m_luminant;
	
	public Image		m_image;
	public Hashtable	m_referencedImages; // by reference rgb
	
	public int		m_use;
}

class LuminantImageEntry {
	public Image				m_image;
	public String				m_uriString;    // of cached image for Ozone web interface
	
	public OriginalImageEntry	m_originalEntry;
	public RGB					m_rgb;
	public int				m_luminance;
	
	public int				m_use;
}

