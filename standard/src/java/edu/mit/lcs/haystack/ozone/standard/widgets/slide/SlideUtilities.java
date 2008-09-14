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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.FontDescription;
import edu.mit.lcs.haystack.ozone.core.utils.graphics.GraphicsManager;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

class AmbientColor implements Serializable {
	transient private Color m_color;
	transient private RGB m_rgb;

	AmbientColor(Color c) {
		m_color = c;
	} 
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		if (m_color == null) {
			out.writeObject(m_rgb);
		} else {
			out.writeObject(m_color.getRGB());
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		m_rgb = (RGB) in.readObject();
	}
	
	public Color getColor() {
		if (m_color == null) {
			m_color = GraphicsManager.acquireColorByRGB(m_rgb);
		}
		return m_color;
	}
	
	public void dispose() {
		GraphicsManager.releaseColor(m_color);
		m_color = null;
	}
}

class AmbientFont extends FontDescription {
	transient private Font		m_font;

	public Font getFont() {
		if (m_font == null) {
			acquire();
		}
		return m_font;
	}
	
	public void acquire() {
		m_font = GraphicsManager.acquireFont(this);
	}
		
	public void setFont(Font font) {
		m_font = font;
	}
	
	public void dispose() {
		GraphicsManager.releaseFont(m_font);
	}
}

/**
 * @version 	1.0
 * @author		David Huynh
 * @author		Dennis Quan
 */
public class SlideUtilities {
	/*
	 *	Ambient properties
	 */
	final static public Resource s_ambientFont				= new Resource(SlideConstants.s_namespace + "ambientFont");
	final static public Resource s_ambientColor				= new Resource(SlideConstants.s_namespace + "ambientColor");
	final static public Resource s_ambientBgcolor			= new Resource(SlideConstants.s_namespace + "ambientBgcolor");
	final static public Resource s_ambientHighlightBgcolor	= new Resource(SlideConstants.s_namespace + "ambientHighlightBgcolor");
	
	final static public Resource s_ambientLinkColor					= new Resource(SlideConstants.s_namespace + "ambientLinkColor");
	final static public Resource s_ambientLinkHoverColor				= new Resource(SlideConstants.s_namespace + "ambientLinkHoverColor");
	final static public Resource s_ambientLinkColorDescription		= new Resource(SlideConstants.s_namespace + "ambientLinkColorDescription");
	final static public Resource s_ambientLinkHoverColorDescription	= new Resource(SlideConstants.s_namespace + "ambientLinkHoverColorDescription");

	/*
	 *  Internal slide stuff
	 */
	final static Resource	s_style 		= new Resource(SlideConstants.s_namespace + "style");
	final static Resource	s_styleDomain	= new Resource(SlideConstants.s_namespace + "styleDomain");
	final static String 	s_stylePrefix	= SlideConstants.s_namespace + "stylePrefix:";

	static public void recordAmbientProperties(Context context, IRDFContainer source, Resource resPartData) {
		// Make record first of any style properties
		Resource[] styles = Utilities.getResourceProperties(resPartData, s_style, source);
		for (int i = 0; i < styles.length; i++) {
			Resource style = styles[i];
			Resource styleDomain = Utilities.getResourceProperty(style, s_styleDomain, source);
			if (styleDomain != null) {
				context.putProperty(new Resource(s_stylePrefix + styleDomain.getURI()), style);
			}
		}

		// Inherit properties from style specifications
		String fontFamily = null, fontSize = null, fontBold = null, fontItalic = null;
		String color = null, bgcolor = null, linkColor = null, linkHoverColor = null;
		String alignX = null, alignY = null;
		
		Resource[] types = Utilities.getResourceProperties(resPartData, Constants.s_rdf_type, source);
		for (int i = 0; i < types.length; i++) {
			Resource styleName = new Resource(s_stylePrefix + types[i].getURI());
			Resource style = (Resource)context.getProperty(styleName);
			if (style != null) {
				String str;
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_fontFamily, source);
				if (str != null) {
					fontFamily = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_fontSize, source);
				if (str != null) {
					fontSize = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_fontBold, source);
				if (str != null) {
					fontBold = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_fontItalic, source);
				if (str != null) {
					fontItalic = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_color, source);
				if (str != null) {
					color = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_bgcolor, source);
				if (str != null) {
					bgcolor = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_linkColor, source);
				if (str != null) {
					linkColor = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_linkHoverColor, source);
				if (str != null) {
					linkHoverColor = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_alignX, source);
				if (str != null) {
					alignX = str;
				}
				
				str = Utilities.getLiteralProperty(style, SlideConstants.s_alignY, source);
				if (str != null) {
					alignY = str;
				}
			}
		}
		
		String str;
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_fontFamily, source);
		if (str != null) {
			fontFamily = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_fontSize, source);
		if (str != null) {
			fontSize = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_fontBold, source);
		if (str != null) {
			fontBold = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_fontItalic, source);
		if (str != null) {
			fontItalic = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_color, source);
		if (str != null) {
			color = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_bgcolor, source);
		if (str != null) {
			bgcolor = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_linkColor, source);
		if (str != null) {
			linkColor = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_linkHoverColor, source);
		if (str != null) {
			linkHoverColor = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_alignX, source);
		if (str != null) {
			alignX = str;
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_alignY, source);
		if (str != null) {
			alignY = str;
		}

		if (fontFamily != null ||
			fontSize != null ||
			fontBold != null ||
			fontItalic != null) {
				
			AmbientFont newAmbientFont = new AmbientFont();
			AmbientFont ambientFont = getOrCreateAmbientFont(context);
			
			if (fontSize == null) {
				newAmbientFont.m_size = ambientFont.m_size;
			} else if (fontSize.endsWith("%")){
				newAmbientFont.m_size = (int) Float.parseFloat(fontSize.substring(0, fontSize.length() - 1)) * ambientFont.m_size / 100;
			} else {
				newAmbientFont.m_size = SystemProperties.s_isMacOSX 
					? (int) Math.round(1.40 * Integer.parseInt(fontSize))
					: Integer.parseInt(fontSize);
			}
			
			if (fontBold == null) {
				newAmbientFont.m_bold = ambientFont.m_bold;
			} else {
				newAmbientFont.m_bold = fontBold.equalsIgnoreCase("true");
			}
		
			if (fontItalic == null) {
				newAmbientFont.m_italic = ambientFont.m_italic;
			} else {
				newAmbientFont.m_italic = fontItalic.equalsIgnoreCase("true");
			}
			
			if (fontFamily != null && fontFamily.length() > 0) {
				if (fontFamily.indexOf(',') < 0) {
					newAmbientFont.m_family = fontFamily;
				} else {
					StringTokenizer tokenizer = new StringTokenizer(fontFamily, ",");
					
					newAmbientFont.m_families = new ArrayList();
					while (tokenizer.hasMoreTokens()) {
						newAmbientFont.m_families.add(tokenizer.nextToken().trim());
					}
				}
			} else {
				newAmbientFont.m_family = ambientFont.m_family;
				
				if (ambientFont.m_families != null) {
					newAmbientFont.m_families = new ArrayList();
					newAmbientFont.m_families.addAll(ambientFont.m_families);
				}
			}
			
			newAmbientFont.acquire();
						
			context.putProperty(s_ambientFont, newAmbientFont);
		}
		
		if (bgcolor != null) {
			Color c = GraphicsManager.acquireColor(bgcolor, getAmbientBgcolor(context));
			if (c != null) {
				context.putProperty(s_ambientBgcolor, new AmbientColor(c));
				context.putProperty(s_ambientHighlightBgcolor, new AmbientColor(GraphicsManager.acquireColor("90%", c)));
			}
		}
		if (color != null) {
			Color c = GraphicsManager.acquireColor(color, getAmbientColor(context));
			if (c != null) {
				context.putProperty(s_ambientColor, new AmbientColor(c));
			}
		}
		if (color != null || linkColor != null) {
			if (linkColor != null) {
				context.putProperty(s_ambientLinkColorDescription, linkColor);
			} else {
				linkColor = getAmbientLinkColorDescription(context);
			}
			
			Color c = GraphicsManager.acquireColor(linkColor, getAmbientColor(context));
			if (c != null) {
				context.putProperty(s_ambientLinkColor, new AmbientColor(c));
			}
		}
		if (color != null || linkColor != null || linkHoverColor != null) {
			if (linkHoverColor != null) {
				context.putProperty(s_ambientLinkHoverColorDescription, linkHoverColor);
			} else {
				linkHoverColor = getAmbientLinkHoverColorDescription(context);
			}
			
			Color c = GraphicsManager.acquireColor(linkHoverColor, getAmbientLinkColor(context));
			if (c != null) {
				context.putProperty(s_ambientLinkHoverColor, new AmbientColor(c));
			}
		}
		
		if (alignX != null) {
			int alignXValue = SlideConstants.ALIGN_LEFT;
			
			if (alignX.equalsIgnoreCase("right")) {
				alignXValue = SlideConstants.ALIGN_RIGHT;
			} else if (alignX.equalsIgnoreCase("center")) {
				alignXValue = SlideConstants.ALIGN_CENTER;
			}
				
			
			context.putProperty(SlideConstants.s_alignX, new Integer(alignXValue));
		}

		if (alignY != null) {
			int alignYValue = SlideConstants.ALIGN_TOP;
			
			if (alignY.equalsIgnoreCase("bottom")) {
				alignYValue = SlideConstants.ALIGN_BOTTOM;
			} else if (alignY.equalsIgnoreCase("center")) {
				alignYValue = SlideConstants.ALIGN_CENTER;
			}
			
			context.putProperty(SlideConstants.s_alignY, new Integer(alignYValue));
		}
		
		str = Utilities.getLiteralProperty(resPartData, SlideConstants.s_textDropShadow, source);
		if (str != null) {
			Boolean b = new Boolean(str.equalsIgnoreCase("true"));
			context.putProperty(SlideConstants.s_textDropShadow, b);
		}
	}
	
	static public void releaseAmbientProperties(Context context) {
		AmbientColor c;

		if (context == null) {
			return;
		}
		
		c = (AmbientColor) context.getUnchainedProperty(s_ambientBgcolor);
		if (c != null) {
			c.dispose();
		}

		c = (AmbientColor) context.getUnchainedProperty(s_ambientHighlightBgcolor);
		if (c != null) {
			c.dispose();
		}
		
		c = (AmbientColor) context.getUnchainedProperty(s_ambientColor);
		if (c != null) {
			c.dispose();
		}

		c = (AmbientColor) context.getUnchainedProperty(s_ambientLinkColor);
		if (c != null) {
			c.dispose();
		}
		
		c = (AmbientColor) context.getUnchainedProperty(s_ambientLinkHoverColor);
		if (c != null) {
			c.dispose();
		}
		
		AmbientFont ambientFont = (AmbientFont) context.getUnchainedProperty(s_ambientFont);
		if (ambientFont != null) {
			ambientFont.dispose();
			ambientFont.m_family = null;
			
			if (ambientFont.m_families != null) {
				ambientFont.m_families.clear();
				ambientFont.m_families = null;
			}
		}
	}
	
	static public Font getAmbientFont(Context context) {
		AmbientFont ambientFont = getOrCreateAmbientFont(context);
		
		return ambientFont.getFont();
	}
	
	static public int getAmbientFontSize(Context context) {
		AmbientFont ambientFont = getOrCreateAmbientFont(context);
		
		return ambientFont.m_size;
	}
	
	static public Color getAmbientColor(Context context) {
		AmbientColor color = (AmbientColor) context.getProperty(s_ambientColor);
		
		if (color == null) {
			return GraphicsManager.s_black;
		} else {
			return color.getColor();
		}
	}
	
	static public Color getAmbientBgcolor(Context context) {
		AmbientColor color = (AmbientColor) context.getProperty(s_ambientBgcolor);
		
		if (color == null) {
			return GraphicsManager.s_white;
		} else {
			return color.getColor();
		}
	}

	static public Color getAmbientHighlightBgcolor(Context context) {
		AmbientColor color = (AmbientColor) context.getProperty(s_ambientHighlightBgcolor);
		
		if (color == null) {
			return GraphicsManager.s_lightestGray;
		} else {
			return color.getColor();
		}
	}
	
	static public Color getAmbientLinkColor(Context context) {
		AmbientColor color = (AmbientColor) context.getProperty(s_ambientLinkColor);
		
		if (color == null) {
			return GraphicsManager.s_blue;
		} else {
			return color.getColor();
		}
	}

	static public Color getAmbientLinkHoverColor(Context context) {
		AmbientColor color = (AmbientColor) context.getProperty(s_ambientLinkHoverColor);
		
		if (color == null) {
			return GraphicsManager.s_blue;
		} else {
			return color.getColor();
		}
	}
	
	static public String getAmbientLinkColorDescription(Context context) {
		String s = (String) context.getProperty(s_ambientLinkColorDescription);
		
		if (s == null) {
			return "100%";
		} else {
			return s;
		}
	}

	static public String getAmbientLinkHoverColorDescription(Context context) {
		String s = (String) context.getProperty(s_ambientLinkHoverColorDescription);
		
		if (s == null) {
			return "120%";
		} else {
			return s;
		}
	}
	
	static public int getAmbientAlignX(Context context) {
		Integer alignX = (Integer) context.getProperty(SlideConstants.s_alignX);
		
		if (alignX == null) {
			return SlideConstants.ALIGN_LEFT;
		} else {
			return alignX.intValue();
		}
	}
	
	static public int getAmbientAlignY(Context context) {
		Integer alignY = (Integer) context.getProperty(SlideConstants.s_alignY);
		
		if (alignY == null) {
			return SlideConstants.ALIGN_TOP;
		} else {
			return alignY.intValue();
		}
	}
	
	static public boolean getAmbientTextDropShadow(Context context) {
		Boolean b = (Boolean) context.getProperty(SlideConstants.s_textDropShadow);
		
		if (b == null) {
			return false;
		} else {
			return b.booleanValue();
		}
	}
	
	static final AmbientFont s_defaultAmbientFont = new AmbientFont();
	static {
		Font 		font = Ozone.s_display.getSystemFont();
		FontData	fontData = font.getFontData()[0];
		int			fontStyle = fontData.getStyle();
		
		s_defaultAmbientFont.m_family = fontData.getName();
		s_defaultAmbientFont.m_size = fontData.getHeight();
		s_defaultAmbientFont.m_bold = (fontStyle & SWT.BOLD) != 0;
		s_defaultAmbientFont.m_italic = (fontStyle & SWT.ITALIC) != 0;
	}
	
	static private AmbientFont getOrCreateAmbientFont(Context context) {
		AmbientFont ambientFont = (AmbientFont) context.getProperty(s_ambientFont);
		
		if (ambientFont == null) {
			return s_defaultAmbientFont;
		}
		
		return ambientFont;
	}
	
	static public String makeOrderFromNumber(int n) {
		switch (n % 10) {
			case 1: return n + "st" ;
			case 2: return n + "nd" ;
			case 3: return n + "rd" ;
			default: return n + "th" ;
		}
	}
}
