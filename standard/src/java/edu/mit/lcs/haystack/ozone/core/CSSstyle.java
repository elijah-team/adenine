/* 
 * Copyright (c) 2003 Massachusetts Institute of Technology. 
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
 
import java.io.PrintWriter;
import java.io.Serializable;

import org.eclipse.swt.graphics.Color;

import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideConstants;

/**
 * Methods for creating lists of CSS attribute/value pairs for CSS style sheets used by the Ozone Web 
 * interface.  Following is a typical list of attribute/value pairs.
 * <pre>
     background-color: white;
     color: black;
     text-style: italic;
   </pre>
 * 
 * @author Stephen Garland
 * @version 1.0
 */
public class CSSstyle implements Serializable {
	
  /**
   * Logger for error messages.
   */
  static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(CSSstyle.class);
	  
  /**
   * Names of recognizable CSS attributes.
   */
  static private AttributeInfo[] attributes =
  { new AttributeInfo("background-color", true),
  	new AttributeInfo("background-image", true),
	new AttributeInfo("background-repeat", true),
	new AttributeInfo("border-bottom-color", true),
	new AttributeInfo("border-bottom-width", true),
	new AttributeInfo("border-collapse", false),
	new AttributeInfo("border-left-color", true),
	new AttributeInfo("border-left-width", true),
	new AttributeInfo("border-right-color", true),
	new AttributeInfo("border-right-width",  true),
	new AttributeInfo("border-style", true),
	new AttributeInfo("border-top-color",  true),
	new AttributeInfo("border-top-width", true),
	new AttributeInfo("border-width", true),
	new AttributeInfo("color", true),
	new AttributeInfo("cursor", true),
	new AttributeInfo("font-family", false),
	new AttributeInfo("font-size", false),
	new AttributeInfo("font-style", false),
	new AttributeInfo("font-weight", false),
	new AttributeInfo("height", true),
	new AttributeInfo("margin", true),
	new AttributeInfo("margin-bottom", true),
	new AttributeInfo("margin-left", true),
	new AttributeInfo("margin-right", true),
	new AttributeInfo("margin-top", true),
	new AttributeInfo("min-width", true),
	new AttributeInfo("padding", true),
	new AttributeInfo("padding-bottom", true),
	new AttributeInfo("padding-left", true),
	new AttributeInfo("padding-right", true),
	new AttributeInfo("padding-top", true),
	new AttributeInfo("text-align", false),
	new AttributeInfo("text-indent", false),
	new AttributeInfo("vertical-align", false),
	new AttributeInfo("width", true) };                                                         
  	
  /**
   * The values of the attributes associated with this style.
   */
  private String[] attributeValues;

  /**
   * Cached value for the default CSS style for Ozone.
   */
  private static CSSstyle defaultStyle;
		
  /**
   * Constructs a new <code>CSSstyle</code>.
   */
  public CSSstyle(){
	  /*
  	if (!OzoneServlet.enabled()) return;
  	attributeValues = new String[attributes.length];
  	for (int i = 0; i < attributes.length; i++) attributeValues[i] = "";
  	*/
  }

  /**
   * Returns the default CSS style for Ozone.
   */
  public static CSSstyle defaultStyle() {
    if (defaultStyle == null) {
      defaultStyle = new CSSstyle();
      defaultStyle.setAttribute("border-collapse", "collapse");
	  defaultStyle.setAttribute("border-style", "none");
  	  defaultStyle.setAttribute("border-width", 0);
	  defaultStyle.setAttribute("margin", 0);
      defaultStyle.setAttribute("padding", 0);
      defaultStyle.setAttribute("text-indent", 0);
      defaultStyle.setAttribute("vertical-align", "top");
    }
    return defaultStyle;
  }

  /**
   * Sets the value of an attribute for this style.  Overrides any previous value for that
   * attribute.
   * 
   * @param name    The name of the attribute
   * @param value   Its value
   * 
   * @see #setAttribute(String name, int value)
   * @see #setAttribute(String name, Color c)
   */
  public void setAttribute(String name, String value) {
	  /*
  	if (!OzoneServlet.enabled()) return;
    int i;
    for (i = 0 ; i < attributes.length; i++) if (name.equals(attributes[i].name)) break;
    if (i >= attributes.length) {
      s_logger.error("Ignoring unknown attribute '" + name + "'");
      return;
    }
    attributeValues[i] = value;
    */
  }
	
  /**
   * Sets an integer-valued attribute for this style.
   * 
   * @param name    The name of the attribute
   * @param value   Its value, in pixels
   */
  public void setAttribute(String name, int value) {
	  /*
  	if (!OzoneServlet.enabled()) return;
    setAttribute(name, "" + value + "px");
    */
  }

  /**
   * Sets a color-valued attribute for this style.  Represents the common colors red, white, blue, 
   * black, and green by their names.  Represents other colors in hexadecimal.
   * 
   * @param name    The name of the attribute
   * @param c       Its value
   */
  public void setAttribute(String name, Color c) {
	  /*
  	if (!OzoneServlet.enabled()) return;
    if (c == null) return;
    String value = (c.equals(GraphicsManager.s_black) ? "black" :
		    c.equals(GraphicsManager.s_blue) ? "blue" :
		    c.equals(GraphicsManager.s_green) ? "green" :
		    c.equals(GraphicsManager.s_red) ? "red" :
		    c.equals(GraphicsManager.s_white) ? "white" :
		    "#" + toHex(c.getRed()) + toHex(c.getGreen()) + toHex(c.getBlue()));
    setAttribute(name, value);
    */
  }
	
  /**
   * Converts an integer to a hexadecimal representating containing at least two digits.  Prepends
   * a zero to representations that would otherwise contain a single digit.
   * 
   * @param n  The integer
   */
  private static String toHex(int n) {
    return (n < 16 ? "0" : "") + Integer.toHexString(n);
  }
	
  /**
   * Returns a new style that inherits non-null valued attributes from this style and other
   * attributes from another style.  
   */
  public CSSstyle inheritFrom(CSSstyle parentStyle, String parentHTMLkeyword) {
	  /*
  	if (!OzoneServlet.enabled()) return this;
  	CSSstyle result = new CSSstyle();
  	boolean fromTable = parentHTMLkeyword.equals("TABLE") || parentHTMLkeyword.equals("TR") || 
						parentHTMLkeyword.equals("TD");
    for (int i = 0; i < attributes.length; i++) {
    	String v = attributeValues[i];
    	result.attributeValues[i] = v;
    	/* Try doing without inheritance -- it may not be appropriate given other recent changes
    		(v.equals("") && parentStyle != null && !(fromTable && attributes[i].tableProperty)
    				? parentStyle.attributeValues[i] : v);
    				*/
	  /*
    }
    return result;
	*/
	return this;
  }
  
  /**
   * Tests whether this style has the same attribute values as another.  Note, however, 
   * that two styles with different attribute values may produce the same effect.  For
   * example, CSS (but not this method) treats the single attribute/value pair
   * <code>margin: 2px</code> as equivalent to the list
   * <pre>
       margin-left: 2px;
       margin-right: 2px;
       margin-top: 2px;
       margin-bottom: 2px;
   </pre>
   * of four attribute/value pairs.
   *
   * @param s  The other style
   * @return   <code>true</code> if the two styles are the same
   */
  public boolean equals(CSSstyle s) {
	  /*
  	if (!OzoneServlet.enabled()) return true;
    for (int i = 0; i < attributeValues.length; i++)
      if (!attributeValues[i].equals(s.attributeValues[i])) return false;
      */
    return true;
  }
	
  /**
   * Prints the definition for this style.  The definition has the form
   * <pre>
      *.class<index> {
        <list of attribute/value pairs>
        }
   * </pre>
   * Does not print attributes that have not been assigned a value.  Does not qualify the 
   * definition by a class name if <code>index<code> is negative.
   *
   * @param pw      PrintWriter to which the definition is directed
   * @param index   Index used to generate the name of the class for this definition
   *                Negative to print a style that applies to all elements
   */
  public void print(PrintWriter pw, int index) {
	  /*
  	if (!OzoneServlet.enabled()) return;
    if (index < 0) pw.println("* {");
    else pw.println("*.class" + index + " {");
    for (int i = 0; i < attributes.length; i++)
      if (!attributeValues[i].equals(""))
	pw.println("  " + attributes[i].name + ": " + attributeValues[i] + ";");
    pw.println("}");
    pw.println();	
    */
  }
	
  /**
   * Returns a string representing the attributes in this style.  Useful for debugging.
   */
  public String toString() {
	  /*
  	if (!OzoneServlet.enabled()) return "";
    String result = "";
    for (int i = 0; i < attributes.length; i++)
      if (!attributeValues[i].equals(""))
	result += "  " + attributes[i].name + ": " + attributeValues[i] + "\n";
    return result;
    */
	  return "";
  }

  
  static public String int2AlignX(int n) {
  	switch (n) {
  	case SlideConstants.ALIGN_LEFT: return "left";
  	case SlideConstants.ALIGN_RIGHT: return "right";
  	case SlideConstants.ALIGN_CENTER: return "center";
  	default: s_logger.error("Illegal value for horizontal alignment: " + n);
  			 return "";
  	}
   }
  
  static public String int2AlignY(int n) {
  	switch (n) {
  	case SlideConstants.ALIGN_TOP: return "top";
  	case SlideConstants.ALIGN_BOTTOM: return "bottom";
  	case SlideConstants.ALIGN_CENTER: return "middle";
  	default: s_logger.error("Illegal value for vertical alignment: " + n);
  			 return "";
  	}
  }
  
}
  
  class AttributeInfo {
  	String name;
  	boolean tableProperty;
  	AttributeInfo(String name, boolean tableProperty) {
  		this.name = name;
  		this.tableProperty = tableProperty;
  	}
 
 
}
