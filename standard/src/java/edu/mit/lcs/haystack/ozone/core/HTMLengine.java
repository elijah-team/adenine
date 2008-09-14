/*
 * Copyright (c) 2003 Massachusetts Institute of Technology. This code was
 * developed as part of the Haystack research project
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, free of
 * charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package edu.mit.lcs.haystack.ozone.core;

import edu.mit.lcs.haystack.ozone.standard.widgets.parts.EditPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.ImageElement;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Methods for rendering the Ozone UI as an HTML web page with an associated
 * CSS stylesheet.
 * 
 * @see edu.mit.lcs.haystack.server.extensions.http.OzoneServlet
 * 
 * @author Stephen Garland
 * @version 1.0
 */
public class HTMLengine {

	/**
	 * Logger for error messages.
	 */
	static org.apache.log4j.Logger s_logger =
		org.apache.log4j.Logger.getLogger(HTMLengine.class);

	/**
	 * Flag to control level of annotation for this web page.
	 */
	private static boolean verbose = true;

	/**
	 * Base file name for this web page and its associated CSS stylesheet. Does
	 * not contain the directory prefix or the ".html" and ".css" extensions.
	 */
	private String baseName;

	/**
	 * Base path/file name for this web page and its associated CSS stylesheet.
	 * Contains the directory prefix, but not the ".html" or ".css" extensions.
	 */
	private String basePathName;

	/**
	 * <code>PrintWriter</code> for directing output to the file containing
	 * this web page.  Null if none exists.
	 */
	private PrintWriter pw = null;
	
	/**
	 * CSS stylesheet used to render this HTML page.
	 */
	private CSSstylesheet stylesheet;
	
	/**
	 * Number of currently open Ozone/HTML elements in this web page.
	 */
	private int nOpenElements;

	/**
	 * Stack of currently open Ozone/HTML elements in this web page.
	 */
	private PageElement[] openElements;

	/**
	 * First unused unique identifier for  clickable elements on this web page.
	 */
	private int nextId;

	/**
	 * List of clickable elements on this web page.  The allocated length of this
	 * array is always at least <code>nextId</code>.
	 */
	private IVisualPart[] parts;

	/**
	 * Initiates the generation of an HTML page in a specified directory.  
	 * If <code>directory</code> is null, the page will be created in the
	 * temporary files directory.
	 */
	public HTMLengine(File directory) {
		s_logger.info("Creating file for HTML output in " + directory);
		stylesheet = new CSSstylesheet();
		openElements = new PageElement[10];
		nOpenElements = 0;
		parts = new IVisualPart[10];
		nextId = 0;
		pendingTableElement = "";
		try {
			File file = File.createTempFile("Haystack", ".html", directory);
			String fName = file.getPath();

			this.basePathName = fName.substring(0, fName.length() - 5);

			this.baseName =
				basePathName.substring(basePathName.lastIndexOf("Haystack"));
			FileWriter fw = new FileWriter(file);
			this.pw = new PrintWriter(fw);
			s_logger.info("Creating file " + fName + " for HTML rendering");
		} catch (Exception e) {
			s_logger.error("Failed to create file " + baseName + " for HTML output: " + e);
			this.pw = null;
			this.baseName = null;
		}
	}

	/**
	 * Returns the name of the file containing the generated HTML page.
	 */
	public String fileName() {
		return this.baseName == null ? null : this.baseName + ".html";
	}

	/**
	 * Returns an array of the clickable parts on this web page.
	 */
	public IVisualPart[] parts() {
		return this.parts;
	}

	/**
	 * Prints the header for this HTML page.
	 */
	public void startPage() {
		println(0, "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		println(0, "<HTML>");
		println(0, "<HEAD>");
		println(0, "  <TITLE>HTML Rendering of Screen " + baseName + ".html</TITLE>");
		println(0, "  <META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
		println(0, "  <LINK HREF=\"" + baseName + ".css\" REL=\"stylesheet\" TYPE=\"text/css\">");
		println(0, "   <SCRIPT LANGUAGE=\"javascript\" TYPE=\"text/javascript\">");
		println(0, "   <!--");
		println(0, "      var done = false;");
		println(0, "      function send(id){");
		println(0, "        if (done) return;");
		println(0, "        var s = 'servlet/haystack?file=" + baseName + ".html';");
		println(0, "        s += '&id=' + id;");
		println(0, "        var t = document.getElementsByTagName('input');");
		println(0, "        var i = 0;");
		println(0, "        for (i = 0; i < t.length; i++)");
		
		/* yks: java.net.URLEncoder.encode(t[i].value) doesn't seem to be recognized by javascript */
		println(0, "        	s += '&text' + t[i].id + '=' + escape(t[i].value)"); /*java.net.URLEncoder.encode(t[i].value);"); */
		println(0, "        location.replace(s);");
		println(0, "        done = true;");
		println(0, "      }");
		println(0, "   //-->");
		println(0, "   </SCRIPT>");
		println(0, "</HEAD>");
		println(0, "<BODY>");
	}

	/**
	 * Completes the generation of this HTML page.  Prints its associated stylesheet.
	 */
	public void endPage() {
		println(0, "</BODY>");
		println(0, "</HTML>");
		pw.close();
		stylesheet.print(basePathName);
	}

	/**
	 * Stub for use in yet-to-be-implemented renderHTML() methods.
	 * 
	 * @param className    		Name of class containing renderHTML() method
	 */
	public void enter(String className) {
		openElement("", null, null, "", className);
		printOpening();
	}
	
	/**
	 * Stub for use in yet-to-be-implemented renderHTML() methods.
	 * 
	 * @param className 		Name of class containing renderHTML() method
	 */
	public void exit(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		closeElement(className);
	}
	
	/**
	 * Stub for use in yet-to-be-implemented renderHTML() methods.
	 * 
	 * @param description		Description of missing part
	 */
	public void unimplemented(String description) {
		println(nOpenElements, description + " not yet available");
	}
	
	/**
	 * Creates a new Ozone/HTML element for this web page.  Defers printing the HTML HTMLkeyword for 
	 * that element.
	 * 
	 * @param HTMLkeyword	HTML keyword for element; empty if the element is an Ozone element
	 * @param s				Style for printing the created element; null if none specified.
	 * 						This method modifies <code>s</code> by inheriting undefined
	 * 						attributes from the enclosing element on this web page.
	 * @param vp			The Ozone part that handles mouse events for this element; null if none.
	 * @param tooltip		String to print as a tooltip when mouse hovers over this element.
	 * @param className		Name of class with renderHTML() method
	 * 
	 * @see #printOpening()
	 * @see #closeElement(String className)
	 */
	private void openElement(String HTMLkeyword, CSSstyle s, IVisualPart vp, String tooltip, String className) {
		if (nOpenElements >= openElements.length) {
			int n = openElements.length;
			PageElement[] newElements = new PageElement[n + 10];
			System.arraycopy(openElements, 0, newElements, 0, n);
			openElements = newElements;
		}
		if (s == null) s = new CSSstyle(); // TODO: (HTML interface) Optimize this.
		if (nOpenElements > 0) {
			PageElement pe = openElements[nOpenElements-1];
			s = s.inheritFrom(pe.style, pe.HTMLkeyword);
		} else s = s.inheritFrom(null, "");
		openElements[nOpenElements++] = new PageElement(HTMLkeyword, s, vp, tooltip, className);
	}

	/**
	 * Prints yet-to-be-printed openings for elements in <code>openElements</code>.   Includes an
	 * <code>onClick</code> attribute in an HTML element if it is clickable.  Enters the visual 
	 * part that responds to clicks and/or entries in input elements for future reference (when 
	 * clicks occur) in the array <code>IVisualPart[] parts</code>.  Also includes a 
	 * <code>TITLE</code> attribute in an HTML element if it has an associated tooltip.  Prints 
	 * at most a comment for non-HTML elements.
	 */
	private void printOpening() {
		int i = nOpenElements - 1;
		while (i >= 0 && !openElements[i].opened) i--;
		i++; // points to first opening that must be printed
		while (i < nOpenElements) {
			PageElement pe = openElements[i++];
			pe.opened = true;
			StringBuffer sb = new StringBuffer();
			if (!pe.HTMLkeyword.equals("")) {
				sb.append("<" + pe.HTMLkeyword + " class=\"class" + stylesheet.find(pe.style) + "\"");
				if (pe.vp != null) {
					IVisualPart vp = pe.vp.getClickHandler();
					if (vp != null) {
						pe.style.setAttribute("cursor", "pointer");
						addPart(vp);
						sb.append(" onClick=\"javascript:send(" + nextId + ");\"");
					}
					else if (pe.vp instanceof EditPart) {
						addPart(pe.vp);
						sb.append(" ID=" + nextId);
					}
				}
				if (pe.tooltip != null && !pe.tooltip.equals(""))
					sb.append(" TITLE=\"" + pe.tooltip + "\"");
				sb.append(">");
				if (verbose) sb.append("  ");
				//else if (!verbose) continue;
				//if (verbose) {
				sb.append("<!-- " + i + "- " + pe.creatingClass);
				if (pe.HTMLkeyword.equals("")) sb.append(" class" + stylesheet.find(pe.style));
				sb.append(" -->");
				//}
				println(i, sb.toString());
			}
		}
	}
	
	/**
	 * Enters a visual part in the array <code>IVisualPart[] parts</code>.  
	 */
	private void addPart(IVisualPart vp) {
		nextId++;
		if (nextId >= parts.length) {
			IVisualPart[] newParts = new IVisualPart[parts.length + 10];
			System.arraycopy(parts, 0, newParts, 0, parts.length);
			parts = newParts;
		}
		parts[nextId] = vp;
	}
	
	/**
	 * HTML table element that will be generated to display a set of rows or columns.
	 * Generation of the <code>TABLE</code>, <code>TR</code>, <code>TD</code> element
	 * is delayed until its style can be determined by an Ozone block/slide/train
	 * element nested within.  Null if there is no pending table element to be generated.
	 */
	private String pendingTableElement;
	
	/**
	 * Prints the HTML HTMLkeyword for the <code>pendingTableElement</code>, if it is
	 * not the empty string.  Resets the <code>pendingTableElement</code> to the empty 
	 * string, unless it is currently <code>TR</code>, in which case it becomes 
	 * <code>TD</code>.
	 * 
	 * @param s				Style for the element; null if none specified.
	 * 						This method modifies <code>s</code> by inheriting undefined
	 * 						attributes from the enclosing element on this web page.
	 * @param vp			The Ozone part that handles mouse events for this element; null if none.
	 * @param tooltip		String to print as a tooltip when mouse hovers over this element.
	 * @param className		Name of class with renderHTML() method
	 * @return				True if an HTML element was printed.
	 */
	private boolean outputPendingTableElement(CSSstyle s, IVisualPart vp, String tooltip, 
											  String className) {
		if (pendingTableElement.equals("")) return false;
		openElement(pendingTableElement, s, vp, tooltip, className);
		printOpening();
		pendingTableElement = (pendingTableElement.equals("TR") ? "TD" : "");
		return true;
	}
	
	/**
	 * Closes the innermost open element on this web page.  Assumes the innermost element
	 * is an HTML element.  Prints the closing HTML element.
	 * 
	 * @param className     Name of class with renderHTML() method
	 */
	private void closeElement(String className) {
		if (nOpenElements <= 0) {
			s_logger.error("Attempt to close more elements than were opened");
			return;
		}
		PageElement prev = openElements[nOpenElements - 1];
		String tail = verbose ? "<!-- -" + nOpenElements + " " + className + " -->" : "";
		if (prev.HTMLkeyword.equals("")) {
			if (verbose) println(nOpenElements, tail);
		} else println(nOpenElements, "</" + prev.HTMLkeyword + ">" + (verbose ? "  " + tail : ""));
		nOpenElements--;
	}
	
	/**
	 * Initiates the creation of a table to display a set of rows.  Forces generation of
	 * any pending HTML table elements.  Defers printing the HTML <code>&gt;TABLE&lt;</code>
	 * element for this table until its style can be determined by a contained block.  Each 
	 * row in the table is initiated by an explicit call to <code>rowStart</code> and 
	 * finished by an explicit call to <code>rowEnd</code>.
	 * 
	 * @param className     Name of class with renderHTML() method
	 * 
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.RowSplitterLayoutManager#renderHTML(HTMLengine he)
	 * @see edu.mit.lcs.haystack.ozone.standard.widgets.slide.RowSetElement#renderHTML(HTMLengine he)
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.VerticalFlowLayoutManager.ScrollSizeHandler#renderHTML(HTMLengine he)
	 */
	public void rowSetStart(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		pendingTableElement = "TABLE";
	}

	/**
	 * Finishes the creation of a table to display a set of rows.  Assumes all rows in the
	 * table have been finished.
	 * 
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.RowSplitterLayoutManager#renderHTML(HTMLengine he)
	 * @see edu.mit.lcs.haystack.ozone.standard.widgets.slide.RowSetElement#renderHTML(HTMLengine he)
	 * @see edu.mit.lcs.haystack.ozone.standard.layout.VerticalFlowLayoutManager.ScrollSizeHandler#renderHTML(HTMLengine he)
	 */
	public void rowSetEnd(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		closeElement(className);
	}

	/**
	 * Initiates the display of a row in a table of rows.  Forces generation of the
	 * enclosing HTML <code>&gt;TABLE&lt;</code> element, if necessary.  Assumes the
	 * pending HTML element is <code>TABLE</code> or the empty string.  Defers printing
	 * the HTML <code>&gt;TR&lt;</code> element until the style of the row can be
	 * determined by a contained block.  When the <code>&gt;TR&lt;</code> element is
	 * printed, printing of a subsequent <code>&gt;TD&lt;</code> is deferred until its
	 * style can be determined.
	 * 
	 * @see parts.layout.RowSplitterLayoutManager#renderHTML(HTMLengine he)
	 * @see parts.slide.RowSetElement#renderHTML(HTMLengine he)
	 * @see parts.layout.VerticalFlowLayoutManager.ScrollSizeHandler#renderHTML(HTMLengine he)
	 */
	public void rowStart(String className) {
		outputPendingTableElement(null, null, "", className);
		pendingTableElement = "TR";
	}

	/**
	 * Finishes the display of a row in a table or rows.
	 * 
	 * @see parts.layout.RowSplitterLayoutManager#renderHTML(HTMLengine he)
	 * @see parts.slide.RowSetElement#renderHTML(HTMLengine he)
	 */
	public void rowEnd(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		closeElement("TD");
		closeElement(className);
	}

	/**
	 * Initiates the creation of a table to display a set of columns.  Forces generation of
	 * any pending HTML table elements.  Prints the HTML <code>&gt;TABLE&lt;</code> element
	 * for this table, but defers printing the <code>&gt;TR&lt;</code> element until its 
	 * style can be determined by a contained block.  Each column in the table is initiated
	 * by an explicit call to <code>columnStart</code> and finished by an explicit call to 
	 * <code>columnEnd</code>.
	 * 
	 * @param className     Name of class with renderHTML() method
	 * 
	 * @see parts.layout.ColumnerLayoutManager#renderHTML(HTMLengine he)
	 * @see parts.slide.ColumnElement#renderHTML(HTMLengine he)
	 */
	public void columnSetStart(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		openElement("TABLE", null, null, "", className);
		printOpening();
		pendingTableElement = "TR";
	}

	/**
	 * Finishes the creation of a table to display a set of columns.  Assumes all columns
	 * in the table have been finished.
	 * 
	 * @see parts.layout.ColumnerLayoutManager#renderHTML(HTMLengine he)
	 * @see parts.slide.ColumnElement#renderHTML(HTMLengine he)
	 */
	public void columnSetEnd(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		closeElement("TR");
		closeElement(className);
	}

	/**
	 * Initiates the display of a column in a table of columns.  Forces generation of the
	 * enclosing HTML <code>&gt;TR&lt;</code> element, if necessary.  Assumes the
	 * pending HTML element is <code>TR</code> or the empty string.  Defers printing
	 * the HTML <code>&gt;TD&lt;</code> element until the style of the column can be
	 * determined by a contained block.  
	 *
	 * @see parts.layout.ColumnerLayoutManager#renderHTML(HTMLengine he)
	 * @see parts.slide.ColumnElement#renderHTML(HTMLengine he)
	 */
	public void columnStart(String className) {
		outputPendingTableElement(null, null, "", className);
		pendingTableElement = "TD";
	}

	/**
	 * Finishes the display of a column in a table of columns.
	 * 
	 * @see parts.layout.ColumnerLayoutManager#renderHTML(HTMLengine he)
	 * @see parts.slide.ColumnElement#renderHTML(HTMLengine he)
	 */
	public void columnEnd(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		closeElement("TD");
	}

	/**
	 * Opens an Ozone block/span/train element in this HTML page.
	 * 
	 * @param className   	Name of class containing the renderHTML method
	 * @param s 			Style used to render this HTML element
	 * @param vp			Ozone visual part that handles mouse actions for this element
	 * @param tooltip		String to display when the mouse moves over this element
	 * 
	 * @see parts.slide.BlockElement
	 * @see parts.slide.SpanElement
	 * @see parts.slide.TrainElement
	 */
	public void enterSpan(String className, CSSstyle s, IVisualPart vp, String tooltip) {
		if (outputPendingTableElement(s, vp, tooltip, className)) {
			// Span was absorbed in enclosing table element
			// Create dummy element for subsequent invocation of exitSpan
			openElement("", s, vp, tooltip, className);
		} else {
			// Treat unabsorbed span as an actual HTML element
			openElement("SPAN", s, vp, tooltip, className);
			printOpening();
		}
	}

	/**
	 * Generates the closing for a SPAN element in this HTML page.
	 */
	public void exitSpan(String className) {
		while (outputPendingTableElement(null, null, "", className)) {}
		closeElement(className);
	}

	/**
	 * @param s				Style for printing the paragraph; null if none specified.
	 * 						This method modifies <code>s</code> by inheriting undefined
	 * 						attributes from the enclosing element on this web page.
	 * @param vp			The Ozone part that handles mouse events for this element; null if none.
	 * @param tooltip		String to print as a tooltip when mouse hovers over this element.
	 * @param className		Name of class with renderHTML() method
	 * @see parts.slide.ParagraphElement#renderHTML(HTMLengine he)
	 */
	public void paragraphStart(CSSstyle s, IVisualPart vp, String tooltip, String className) {
		while (outputPendingTableElement(s, vp, tooltip, className));
		enter(className);
	}

	/*
	 * @see parts.slide.ParagraphElement#renderHTML(HTMLengine he)
	 */
	public void paragraphEnd(String className) {
		println(nOpenElements, "<P>");
		exit(className);
	}

	/**
	 * @see parts.slide.BreakElement#renderHTML(HTMLengine he)
	 */
	public void breakElement() {
		while (outputPendingTableElement(null, null, "", "Break"));
		println(nOpenElements, "<BR>");
	}

	/**
	 * @see parts.slide.LineElement#renderHTML(HTMLengine he)
	 */
	public void lineElement() {
		while (outputPendingTableElement(null, null, "", "Line"));
		println(nOpenElements, "<HR>");
	}

	/**
	 * @see parts.slide.EditPart#renderHTML(HTMLengine he)
	 */
	public void editor(String text, CSSstyle style, IVisualPart vp, String className) {
		while (outputPendingTableElement(null, null, "", "Editor"));
		openElement("INPUT TYPE=\"text\"", style, vp, "", className);
		printOpening();
		nOpenElements--;
	}

	/**
	 * @see parts.slide.TextElement#renderHTML(HTMLengine he)
	 * @see parts.QuickPropertyTextPart#renderHTML(HTMLengine he)
	 * @see parts.layout.VerticalFlowLayoutManager.ScrollSizeHandler#renderHTML(HTMLengine he)
	 */
	public void text(String s, CSSstyle style, IVisualPart vp, String tooltip, String className) {
		if (s == null) return;
		if (s.length() > 0) {
			// Translate characters with special meaning in HTML, Ozone
			StringBuffer sb = null;
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (sb == null) {
					if ("<>&\"\b\n".indexOf(c) < 0)  continue;
					sb = new StringBuffer(s.length());
					for (int j = 0; j < i; j++) sb.append(s.charAt(j));
				}
				switch (c) {
				// HTML translations
				case '<' :
					sb.append("&lt;");
					break;
				case '>' :
					sb.append("&gt;");
					break;
				case '&' :
					sb.append("&amp;");
					break;
				case '"' :
					sb.append("&quot;");
					break;
					// Ozone translations
				case '\n' :
					sb.append("<BR>");
					break;
				case '\b' :
					if (i < s.length() && s.charAt(i) == '\b') {
						sb.append("<P>");
						i++;
					}
					break;
				default :
					sb.append(c);
				    break;
				}
			}
			if (sb != null) {
				s_logger.info("Translated '" + s + "' to'" + sb.toString() + "'");
				s = sb.toString();
			}
		}
		while (outputPendingTableElement(null, null, "", "Text"));
		openElement("SPAN", style, vp, tooltip, className);
		printOpening();
		println(nOpenElements, s);
		closeElement(className);
	}

	/**
	 * @see parts.slide.ImageElement#renderHTML(HTMLengine he)

	 */
	public void image(CSSstyle style, String uri, String tooltip, ImageElement ie, String className) {
		while (outputPendingTableElement(null, null, "", "Image"));
		// TODO: (HTML interface) Modularize code better to eliminate duplicate code
		if (style == null) style = new CSSstyle(); // TODO: (HTML interface) Optimize this.
		if (nOpenElements > 0) {
			PageElement pe = openElements[nOpenElements-1];
			style.inheritFrom(pe.style, pe.HTMLkeyword);
		}

		String s = "<IMG class=\"class" + stylesheet.find(style) + "\"";
		if (ie != null) {
			IVisualPart vp = ie.getClickHandler();
			if (vp != null) {				
				nextId++;
				if (nextId >= parts.length) {
					IVisualPart[] newParts = new IVisualPart[parts.length + 10];
					System.arraycopy(parts, 0, newParts, 0, parts.length);
					parts = newParts;
				}
				parts[nextId] = vp;
				s += "\" onClick=\"javascript:send(" + nextId + ");\"";
			}
		}
		println(nOpenElements, s + "  SRC=\"" + uri + "\">");
		/*
		if (tooltip == null || tooltip.equals(""))
			tooltip = "Image: " + uri.substring(uri.lastIndexOf("/") + 1);
		println(nOpenElements, "     ALT=\"" + tooltip + "\"");      // used as tooltip for Internet Explorer
		println(nOpenElements, "     TITLE=\"" + tooltip + "\">");   // used as tooltip for Netscape
		*/
	}
	
	/**
	 * Indents and appends a line of text to this HTML page.
	 */
	private void println(int indentation, String s) {
		if (pw != null) pw.println(indent(indentation) + s);
	}
	
	/**
	 * Cached string for use in generating indentation.
	 */
	private static String spaces =
		"                                                  ";
	
	/**
	 * Returns a string containing a specified number of spaces.
	 */
	private static String indent(int n) {
		while (n > spaces.length()) spaces += spaces;
		return spaces.substring(0, n);
	}
	
	/*
	 * Facilities for tracing.
	 */

	private static StackTraceElement[] previousTrace = {};

	public static void printStackTrace() {
		Exception e = new Exception();
		StackTraceElement[] currentTrace = e.getStackTrace();
		// Find the length of the common prefix of the current and previous traces
		int nSame = 0;
		while (nSame < currentTrace.length
			&& nSame < previousTrace.length
			&& currentTrace[currentTrace.length - nSame - 1]
				== previousTrace[previousTrace.length - nSame - 1])
			nSame--;
		// Print what follows the common prefix in the current trace
		for (int i = 0; i < currentTrace.length - nSame; i++)
			s_logger.info(currentTrace[i].toString());
		// Print the last line of the common prefix
		if (nSame > 0) s_logger.info(currentTrace[currentTrace.length - nSame]);
		if (nSame > 1) s_logger.info("...");
	}

}

/**
 * Information about elements on a web page.
  */
class PageElement {
	
	/**
	 * HTML keyword for opening and closing this element.  The empty string if this element is not
	 * an HTML element (i.e., it corresponds to an Ozone element used to inherit styles or to a
	 * yet-to-be-implemented Ozone element).
	 */
	String HTMLkeyword;
	
	/**
	 * The style associated with this element.
	 */
	CSSstyle style;

	/**
	 * The visual part that responds to clicks on this element.  Null if there is no such part.
	 */
	IVisualPart vp;
	
	/**
	 * A tooltip to display when the mouse hovers over this element.  Null or empty if there is
	 * no tooltip.
	 */
	String tooltip;
	
	/**
	 * Name of the class containing the <code>renderHTML</code> method responsible for this
	 * element.  Used for annotation and debugging.
	 */
	String creatingClass;
	
	/**
	 * Flag that indicates whether an opening HTML element or comment has been printed for this
	 * element.
	 */
	boolean opened;
	
	/**
	 * Creates a new <code>PageElement</code>.  
	 */
	PageElement(String HTMLkeyword, CSSstyle style, IVisualPart vp, String tooltip, String creatingClass) {
		this.HTMLkeyword = HTMLkeyword;
		this.style = style;
		this.creatingClass = creatingClass;
		this.vp = vp;
		this.tooltip = tooltip;
		this.opened = false;
	}
}
