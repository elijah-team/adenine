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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Methods for creating CSS style sheets for use by the Ozone web interface.
 * 
 * @author Stephen Garland
 * @version 1.0
 */
public class CSSstylesheet {
	
  /**
   * Logger for error messages.
   */
  static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(CSSstylesheet.class);
	
  /**
   * An array for storing the styles described in this stylesheet.
   */
  private CSSstyle[] styles;
 
  /**
   * The number of styles currently in this style sheet.  This number is less than or equal to
   * the allocated size of the <code>styles</code> array.
   */
  private int nStyles;
  
  /**
   * Constructs a new <code>CSSstylesheet</code>.
   */
  public CSSstylesheet() {
    styles = new CSSstyle[10];
    nStyles = 0;
  }

  /* 
   * Prints this stylesheet to a file.
   *
   * @param baseName The complete name (path plus file name) of the HTML file, without
   *                 the suffix <code>.html</code>.
   */
  public void print(String baseName) {
    String pathName = baseName + ".css";	
    PrintWriter pw;
    try {
      File file = new File(pathName);
      FileWriter fw = new FileWriter(file);
      pw = new PrintWriter(fw);
    } catch (Exception e) {
      s_logger.error("Failed to create CSS file " + pathName);
      return;
    }
    CSSstyle.defaultStyle().print(pw, -1);
    for (int index = 0; index < nStyles; index++) styles[index].print(pw, index);
    pw.close();
    s_logger.info("Created CSS file " + pathName + " for HTML rendering");
  }
	
  /**
   * Returns the index of a style in this stylesheet that is equivalent to a given
   * <code>CSSstyle</code>.  Appends the given <code>CSSstyle</code> to this stylesheet
   * if it does not already contain an equivalent style.
   */
  public int find(CSSstyle s) {
    // TODO: (HTML interface) Ensure that this method is not called after this stylesheet is printed.
    int index;
    for (index = 0; index < nStyles; index++)
      if (s.equals(styles[index])) return index;
    if (index >= nStyles) {
      index = nStyles++;
      if (nStyles > styles.length) {
      	CSSstyle[] newStyles = new CSSstyle[styles.length + 10];
      	System.arraycopy(styles, 0, newStyles, 0, styles.length);
      	styles = newStyles;
      }
      styles[index] = s;
    }
    return index;
  }

}
