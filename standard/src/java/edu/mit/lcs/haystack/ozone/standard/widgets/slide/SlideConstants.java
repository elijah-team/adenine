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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.Constants;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public final class SlideConstants {
	final public static String s_namespace = Constants.s_slide_namespace;
	
	/*
	 * Alignment constants
	 */
	 
	public static final int	ALIGN_TOP = 0;
	public static final int	ALIGN_BOTTOM = 1;
	public static final int	ALIGN_CENTER = 2;
	public static final int	ALIGN_LEFT = 0;
	public static final int	ALIGN_RIGHT = 1;	

	/*
	 * Misc.
	 */
	
	final public static Resource s_children 		= new Resource(s_namespace + "children");
	final public static Resource s_child			= new Resource(s_namespace + "child");

	/*
	 *  Ambient (inherited) properties
	 */
	final public static Resource s_fontFamily	= new Resource(s_namespace + "fontFamily");
	final public static Resource s_fontSize		= new Resource(s_namespace + "fontSize");
	final public static Resource s_fontBold		= new Resource(s_namespace + "fontBold");
	final public static Resource s_fontItalic	= new Resource(s_namespace + "fontItalic");
	final public static Resource s_color			= new Resource(s_namespace + "color");
	final public static Resource s_bgcolor 		= new Resource(s_namespace + "bgcolor");
	final public static Resource s_linkColor		= new Resource(s_namespace + "linkColor");
	final public static Resource s_linkHoverColor = new Resource(s_namespace + "linkHoverColor");
	
	/*
	 *  Common properties (applicable to more than one type of element)
	 */
	final public static Resource s_width 		= new Resource(s_namespace + "width");
	final public static Resource s_height		= new Resource(s_namespace + "height");		
	final public static Resource s_minWidth		= new Resource(s_namespace + "minWidth");
	final public static Resource s_minHeight	= new Resource(s_namespace + "minHeight");
	final public static Resource s_maxWidth		= new Resource(s_namespace + "maxWidth");
	final public static Resource s_maxHeight	= new Resource(s_namespace + "maxHeight");
	

	/*
	 *  Slide element properties
	 */
	final public static Resource s_onLoad		= new Resource(s_namespace + "onLoad");

	/*
	 *  Text element properties
	 */
	final public static Resource s_text 			= new Resource(s_namespace + "text");
	final public static Resource s_defaultText	= new Resource(s_namespace + "defaultText");
	final public static Resource s_maxLines		= new Resource(s_namespace + "maxLines"); // integer=-1
	final public static Resource s_wrap			= new Resource(s_namespace + "wrap"); // [TRUE|false]
	final public static Resource s_textDropShadow = new Resource(s_namespace + "textDropShadow"); // [FALSE, true]
    /* attribute to make the element editable true or false */
	final public static Resource s_editable      = new Resource(s_namespace + "editable"); //[TRUE|False]

	/*
	 *  Image element properties
	 */
	final public static Resource s_source 			= new Resource(s_namespace + "source");
	final public static Resource s_scaleToFit		= new Resource(s_namespace + "scaleToFit");
	final public static Resource s_textAlign 		= new Resource(s_namespace + "textAlign"); // [top|bottom|CENTER|baseline]
	final public static Resource s_baseLineOffset	= new Resource(s_namespace + "baseLineOffset");
	
	/*
	 *  RowSet, ColumnSet elements' properties
	 */
	final public static Resource s_scale		= new Resource(s_namespace + "scale"); // [PIXEL|percent]
	final public static Resource s_pack		= new Resource(s_namespace + "pack"); // [LEFT|right|TOP|bottom]
	
	/*
	 *  Paragraph element properties
	 */
	final public static Resource s_alignX	= new Resource(s_namespace + "alignX"); // [LEFT|right|center]
	final public static Resource s_alignY	= new Resource(s_namespace + "alignY"); // [TOP|bottom|center]
	
	/*
	 *	Block element properties	 */

	final public static Resource s_fillParent			= new Resource(s_namespace + "fillParent");		// [TRUE|false]
	final public static Resource s_fillParentWidth		= new Resource(s_namespace + "fillParentWidth");	// [TRUE|false]
	final public static Resource s_fillParentHeight		= new Resource(s_namespace + "fillParentHeight");	// [TRUE|false]
	
	final public static Resource s_cropChild				= new Resource(s_namespace + "cropChild"); 		// [true|FALSE]
	final public static Resource s_cropChildWidth		= new Resource(s_namespace + "cropChildWidth");	// [true|FALSE]
	final public static Resource s_cropChildHeight		= new Resource(s_namespace + "cropChildHeight");	// [true|FALSE]
	
	final public static Resource s_stretchChild			= new Resource(s_namespace + "stretchChild"); 			// [TRUE|false]
	final public static Resource s_stretchChildWidth		= new Resource(s_namespace + "stretchChildWidth");		// [TRUE|false]
	final public static Resource s_stretchChildHeight	= new Resource(s_namespace + "stretchChildHeight");	// [TRUE|false]
	
	final public static Resource s_background 			= new Resource(s_namespace + "background");
	final public static Resource s_backgroundRepeat		= new Resource(s_namespace + "backgroundRepeat"); // [x|y|BOTH|none]
	final public static Resource s_backgroundAlignX		= new Resource(s_namespace + "backgroundAlignX"); // [LEFT|right]
	final public static Resource s_backgroundAlignY		= new Resource(s_namespace + "backgroundAlignY"); // [TOP|bottom]
	
	final public static Resource s_margin				= new Resource(s_namespace + "margin"); // in pixel, default = 0
	final public static Resource s_marginX				= new Resource(s_namespace + "marginX"); // in pixel, default = 0
	final public static Resource s_marginY				= new Resource(s_namespace + "marginY"); // in pixel, default = 0
	final public static Resource s_marginLeft			= new Resource(s_namespace + "marginLeft"); // in pixel, default = 0
	final public static Resource s_marginRight			= new Resource(s_namespace + "marginRight"); // in pixel, default = 0
	final public static Resource s_marginTop				= new Resource(s_namespace + "marginTop"); // in pixel, default = 0
	final public static Resource s_marginBottom			= new Resource(s_namespace + "marginBottom"); // in pixel, default = 0
	
	final public static Resource s_clearance				= new Resource(s_namespace + "clearance"); // in pixel, default = 0
	final public static Resource s_clearanceX			= new Resource(s_namespace + "clearanceX"); // in pixel, default = 0
	final public static Resource s_clearanceY			= new Resource(s_namespace + "clearanceY"); // in pixel, default = 0
	final public static Resource s_clearanceLeft			= new Resource(s_namespace + "clearanceLeft"); // in pixel, default = 0
	final public static Resource s_clearanceRight		= new Resource(s_namespace + "clearanceRight"); // in pixel, default = 0
	final public static Resource s_clearanceTop			= new Resource(s_namespace + "clearanceTop"); // in pixel, default = 0
	final public static Resource s_clearanceBottom		= new Resource(s_namespace + "clearanceBottom"); // in pixel, default = 0
	
	final public static Resource s_borderWidth			= new Resource(s_namespace + "borderWidth");
	final public static Resource s_borderColor			= new Resource(s_namespace + "borderColor");
	final public static Resource s_borderXWidth			= new Resource(s_namespace + "borderXWidth");
	final public static Resource s_borderXColor			= new Resource(s_namespace + "borderXColor");
	final public static Resource s_borderYWidth			= new Resource(s_namespace + "borderYWidth");
	final public static Resource s_borderYColor			= new Resource(s_namespace + "borderYColor");
	final public static Resource s_borderLeftWidth		= new Resource(s_namespace + "borderLeftWidth");
	final public static Resource s_borderLeftColor		= new Resource(s_namespace + "borderLeftColor");
	final public static Resource s_borderRightWidth		= new Resource(s_namespace + "borderRightWidth");
	final public static Resource s_borderRightColor		= new Resource(s_namespace + "borderRightColor");
	final public static Resource s_borderTopWidth		= new Resource(s_namespace + "borderTopWidth");
	final public static Resource s_borderTopColor		= new Resource(s_namespace + "borderTopColor");
	final public static Resource s_borderBottomWidth		= new Resource(s_namespace + "borderBottomWidth");
	final public static Resource s_borderBottomColor		= new Resource(s_namespace + "borderBottomColor");

	final public static Resource s_dropShadow			= new Resource(s_namespace + "dropShadow"); // [FALSE, true]

	final public static Resource s_borderShadow			= new Resource(s_namespace + "borderShadow"); // [FALSE, true]
	final public static Resource s_borderXShadow			= new Resource(s_namespace + "borderXShadow"); // [FALSE, true]
	final public static Resource s_borderYShadow			= new Resource(s_namespace + "borderYShadow"); // [FALSE, true]
	final public static Resource s_borderTopShadow		= new Resource(s_namespace + "borderTopShadow"); // [FALSE, true]
	final public static Resource s_borderBottomShadow	= new Resource(s_namespace + "borderBottomShadow"); // [FALSE, true]
	final public static Resource s_borderLeftShadow		= new Resource(s_namespace + "borderLeftShadow"); // [FALSE, true]
	final public static Resource s_borderRightShadow		= new Resource(s_namespace + "borderRightShadow"); // [FALSE, true]

	/*
	 * Highlightable element properties
	 */	
	final public static Resource s_backgroundHighlight	= new Resource(s_namespace + "backgroundHighlight");
	final public static Resource s_highlightBorder		= new Resource(s_namespace + "highlightBorder");
}
