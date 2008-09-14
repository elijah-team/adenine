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

import java.io.*;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.server.core.content.IContentService;
import edu.mit.lcs.haystack.content.ContentAndMimeType;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.SWT;

/**
 * @author David Huynh
 */
public class GraphicsUtilities {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(GraphicsUtilities.class);

	final static Resource PICTURE_WIDTH = new Resource("http://haystack.lcs.mit.edu/schemata/picture#width");
	final static Resource PICTURE_HEIGHT = new Resource("http://haystack.lcs.mit.edu/schemata/picture#height");

	public static class ImageAndURI {
		public Image		m_image;
		public Resource	m_uri;
		
		public ImageAndURI(Image image, Resource uri) {
			m_image = image;
			m_uri = uri;
		}
	}
	
	static interface Transform {
		public Image transform(Image image);
	}

	public static Image scaleImageToPercents(Image image, int percents) {
		percents = Math.max(1, Math.min(percents, 100));
		
		Rectangle	bounds = image.getBounds();
		ImageData	imageData = image.getImageData();
		ImageData	imageData2 = imageData.scaledTo(bounds.width * percents / 100, bounds.height * percents / 100);
		Image		image2 = new Image(Ozone.s_display, imageData2);
		
		return image2;
	}

	public static Resource scaleImageToPercents(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int percents) {
		ImageAndURI iau = scaleImageToPercents2(imageURI, cs, source, sa, percents);
		
		if (iau != null) {
			iau.m_image.dispose();
			return iau.m_uri;
		} else {
			return null;
		}
	}
	
	public static ImageAndURI scaleImageToPercents2(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int percents) {
		return transform(
			imageURI,
			cs,
			source,
			sa,
			new Transform() {
				public Image transform(Image image) {
					return scaleImageToPercents(image, m_percents);
				}
			
				int m_percents;	
				public Transform init(int percents) {
					m_percents = percents;
					return this;
				}
			}.init(percents)
		);
	}
	
	public static Image scaleImageToFit(Image image, int maxWidth, int maxHeight) {
		Rectangle	bounds = image.getBounds();
		ImageData	imageData = image.getImageData();
		
		int width = maxWidth;
		int height = bounds.height * width / bounds.width;
		if (height > maxHeight) {
			height = maxHeight;
			width = bounds.width * height / bounds.height;
		}
		
		ImageData	imageData2 = imageData.scaledTo(width, height);
		Image		image2 = new Image(Ozone.s_display, imageData2);
		
		return image2;
	}

	public static Resource scaleImageToFit(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int maxWidth, int maxHeight) {
		ImageAndURI iau = scaleImageToFit2(imageURI, cs, source, sa, maxWidth, maxHeight);
		
		if (iau != null) {
			iau.m_image.dispose();
			return iau.m_uri;
		} else {
			return null;
		}
	}
	
	public static ImageAndURI scaleImageToFit2(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int maxWidth, int maxHeight) {
		return transform(
			imageURI,
			cs,
			source,
			sa,
			new Transform() {
				public Image transform(Image image) {
					return scaleImageToFit(image, m_maxWidth, m_maxHeight);
				}
			
				int m_maxWidth;
				int m_maxHeight;
				public Transform init(int maxWidth, int maxHeight) {
					m_maxWidth = maxWidth;
					m_maxHeight = maxHeight;
					return this;
				}
			}.init(maxWidth, maxHeight)
		);
	}

	public static Image rotateImage(Image image, String rotation) {
		Rectangle	bounds = image.getBounds();
		ImageData	imageData = image.getImageData();
		ImageData	imageData2 = null;
		
		if (rotation.equals("90cw")) {
			imageData2 = new ImageData(imageData.height, imageData.width, imageData.depth, imageData.palette);
			
			for (int x = 0; x < imageData.width; x++) {
				for (int y = 0; y < imageData.height; y++) {
					imageData2.setPixel(imageData.height - y - 1, x, imageData.getPixel(x, y));
				}
			}
		} else if (rotation.equals("90ccw")) {
			imageData2 = new ImageData(imageData.height, imageData.width, imageData.depth, imageData.palette);
			
			for (int x = 0; x < imageData.width; x++) {
				for (int y = 0; y < imageData.height; y++) {
					imageData2.setPixel(y, imageData.width - x - 1, imageData.getPixel(x, y));
				}
			}
		} else if (rotation.equals("180")) {
			imageData2 = new ImageData(imageData.width, imageData.height, imageData.depth, imageData.palette);
			
			for (int x = 0; x < imageData.width; x++) {
				for (int y = 0; y < imageData.height; y++) {
					imageData2.setPixel(imageData.width - x - 1, imageData.height - y - 1, imageData.getPixel(x, y));
				}
			}
		} else {
			imageData2 = imageData;
		}

		return new Image(Ozone.s_display, imageData2);
	}

	public static Resource rotateImage(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, String rotation) {
		ImageAndURI iau = rotateImage2(imageURI, cs, source, sa, rotation);
		
		if (iau != null) {
			iau.m_image.dispose();
			return iau.m_uri;
		} else {
			return null;
		}
	}
	
	public static ImageAndURI rotateImage2(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, String rotation) {
		return transform(
			imageURI,
			cs,
			source,
			sa,
			new Transform() {
				public Image transform(Image image) {
					return rotateImage(image, m_rotation);
				}
			
				String m_rotation;	
				public Transform init(String rotation) {
					m_rotation = rotation;
					return this;
				}
			}.init(rotation)
		);
	}

	public static Image cropImage(Image image, int left, int right, int top, int bottom) {
		Rectangle	bounds = image.getBounds();
		
		left = Math.min(bounds.width, Math.max(left, 0));
		top = Math.min(bounds.height, Math.max(top, 0));
		
		right = Math.min(bounds.width - left, Math.max(right, 0));
		bottom = Math.min(bounds.height - top, Math.max(bottom, 0));
		
		int width = bounds.width - left - right;
		int height = bounds.height - top - bottom;
		
		ImageData	imageData = image.getImageData();
		ImageData	imageData2 = new ImageData(width, height, imageData.depth, imageData.palette);
		Image		image2 = new Image(Ozone.s_display, imageData2);
		GC			gc = new GC(image2);
		
		gc.drawImage(image, left, top, width, height, 0, 0, width, height);
		gc.dispose();
		
		return image2;
	}

	public static Resource cropImage(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int left, int right, int top, int bottom) {
		ImageAndURI iau = cropImage2(imageURI, cs, source, sa, left, right, top, bottom);
		
		if (iau != null) {
			iau.m_image.dispose();
			return iau.m_uri;
		} else {
			return null;
		}
	}
	
	public static ImageAndURI cropImage2(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int left, int right, int top, int bottom) {
		return transform(
			imageURI,
			cs,
			source,
			sa,
			new Transform() {
				public Image transform(Image image) {
					return cropImage(image, m_left, m_right, m_top, m_bottom);
				}
			
				int m_left;
				int m_right;
				int m_top;
				int m_bottom;
				public Transform init(int left, int right, int top, int bottom) {
					m_left = left;
					m_right = right;
					m_top = top;
					m_bottom = bottom;
					return this;
				}
			}.init(left, right, top, bottom)
		);
	}
	
	public static Image adjustBrightnessContrast(Image image, int brightness, int contrast) {
		Rectangle	bounds = image.getBounds();
		ImageData	imageData = image.getImageData();
		PaletteData	paletteData = imageData.palette;
		ImageData	imageData2 = (ImageData) imageData.clone();
		
		brightness = Math.max(Math.min(brightness, 100), -100);
		contrast = Math.max(Math.min(contrast, 100), -100);
		
		int	brightnessShift = brightness * 256 / 100;
		int	contrastShift = contrast * 128 / 100;
		int	r;
		int	g;
		int	b;
		int	p;
		
		if (paletteData.isDirect) {
			
			for (int x = 0; x < bounds.width; x++) {
				for (int y = 0; y < bounds.height; y++) {
					p = imageData.getPixel(x, y);
					
					if (paletteData.redShift > 0) {
						r = (p & paletteData.redMask) << paletteData.redShift;
					} else {
						r = (p & paletteData.redMask) >> -paletteData.redShift;
					}

					if (paletteData.greenShift > 0) {
						g = (p & paletteData.greenMask) << paletteData.greenShift;
					} else {
						g = (p & paletteData.greenMask) >> -paletteData.greenShift;
					}

					if (paletteData.blueShift > 0) {
						b = (p & paletteData.blueMask) << paletteData.blueShift;
					} else {
						b = (p & paletteData.blueMask) >> -paletteData.blueShift;
					}

					{
						r += brightnessShift;
						g += brightnessShift;
						b += brightnessShift;
						
						if (r > 127) {
							r += contrastShift;
						} else {
							r -= contrastShift;
						}
						if (g > 127) {
							g += contrastShift;
						} else {
							g -= contrastShift;
						}
						if (b > 127) {
							b += contrastShift;
						} else {
							b -= contrastShift;
						}
						
						r = Math.max(0, Math.min(r, 255));
						g = Math.max(0, Math.min(g, 255));
						b = Math.max(0, Math.min(b, 255));
					}
					
					p = 0;
					if (paletteData.redShift > 0) {
						p = p | ((r >> paletteData.redShift) & paletteData.redMask);
					} else {
						p = p | ((r << -paletteData.redShift) & paletteData.redMask);
					}
					if (paletteData.greenShift > 0) {
						p = p | ((g >> paletteData.greenShift) & paletteData.greenMask);
					} else {
						p = p | ((g << -paletteData.greenShift) & paletteData.greenMask);
					}
					if (paletteData.blueShift > 0) {
						p = p | ((b >> paletteData.blueShift) & paletteData.blueMask);
					} else {
						p = p | ((b << -paletteData.blueShift) & paletteData.blueMask);
					}
					
					imageData2.setPixel(x, y, p);
				}
			}
		} else {
			RGB[] colors = imageData2.palette.colors;
			
			for (int i = 0; i < colors.length; i++) {
				r = colors[i].red;
				g = colors[i].green;
				b = colors[i].blue;

				{
					r += brightnessShift;
					g += brightnessShift;
					b += brightnessShift;
					
					if (r > 127) {
						r += contrastShift;
					} else {
						r -= contrastShift;
					}
					if (g > 127) {
						g += contrastShift;
					} else {
						g -= contrastShift;
					}
					if (b > 127) {
						b += contrastShift;
					} else {
						b -= contrastShift;
					}
					
					r = Math.max(0, Math.min(r, 255));
					g = Math.max(0, Math.min(g, 255));
					b = Math.max(0, Math.min(b, 255));
				}
				
				colors[i].red = r;
				colors[i].green = g;
				colors[i].blue = b;
			}
		}
		
		return new Image(Ozone.s_display, imageData2);
	}

	public static Resource adjustBrightnessContrast(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int brightness, int contrast) {
		ImageAndURI iau = adjustBrightnessContrast2(imageURI, cs, source, sa, brightness, contrast);
		
		if (iau != null) {
			iau.m_image.dispose();
			return iau.m_uri;
		} else {
			return null;
		}
	}
	
	public static ImageAndURI adjustBrightnessContrast2(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, int brightness, int contrast) {
		return transform(
			imageURI,
			cs,
			source,
			sa,
			new Transform() {
				public Image transform(Image image) {
					return adjustBrightnessContrast(image, m_brightness, m_contrast);
				}
			
				int m_brightness;
				int m_contrast;
				public Transform init(int brightness, int contrast) {
					m_brightness = brightness;
					m_contrast = contrast;
					return this;
				}
			}.init(brightness, contrast)
		);
	}
	
	static ImageAndURI transform(Resource imageURI, IContentService cs, IRDFContainer source, IServiceAccessor sa, Transform t) {
		try {
			ContentClient		cc = ContentClient.getContentClient(imageURI, source, sa);
			ContentAndMimeType	cmt = cc.getContentAndMimeType();
			
			Image		image = new Image(Ozone.s_display, cmt.m_content);
			Image		image2 = t.transform(image);
			Resource	image2URI = cs.allocateContent();
			ImageLoader	il = new ImageLoader();
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			il.data = new ImageData[] { image2.getImageData() };
			cc = ContentClient.getContentClient(image2URI, source, sa);
			
			if ("image/gif".equals(cmt.m_mimeType)) {
				il.save(baos, SWT.IMAGE_GIF);
				cc.setContent(new ByteArrayInputStream(baos.toByteArray()), cmt.m_mimeType);
			} else if ("image/jpeg".equals(cmt.m_mimeType)) {
				il.save(baos, SWT.IMAGE_JPEG);
				cc.setContent(new ByteArrayInputStream(baos.toByteArray()), cmt.m_mimeType);
			} else {
				il.save(baos, SWT.IMAGE_JPEG);
				cc.setContent(new ByteArrayInputStream(baos.toByteArray()), "image/jpeg");
			}
			
			image.dispose();
			
			source.replace(image2URI, PICTURE_WIDTH, null, new Literal(Integer.toString(image2.getBounds().width)));
			source.replace(image2URI, PICTURE_HEIGHT, null, new Literal(Integer.toString(image2.getBounds().height)));
			
			return new ImageAndURI(image2, image2URI);
		} catch (Exception e) {
			s_logger.error("Failed to transofrm image " + imageURI, e);
			return null;
		}
	}
}
