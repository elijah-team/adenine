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

package edu.mit.lcs.haystack.server.extensions.http;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Event;

import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.eclipse.FullScreenHostPart;
import edu.mit.lcs.haystack.ozone.core.HTMLengine;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.EditPart;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;


/**
 * Servlet for rendering and serving the Ozone interface as a web page.
 * <p>
 * To use this servlet, perform the following steps:
 * <ul>
 * <li>
 * Execute the commands
 * <pre>
 *   cd haystack/tomcat/webapps/content
 *   (cd ../../../src/resource; tar cf - data) | tar xf -
 * </pre>
 * to copy the directory containing the images for web pages into the directory used
 * by the HTTP server.
 * </li><li>
 * Start the standalone version of Haystack.
 * </li><li>
 * Start the HTTP server by navigating from "Starting Points" to "Configure Haystack"
 * to "Turn on web server"
  * </li><li>
 * Browse to http://hostname:8100/servlet/haystack.
 * </li></ul>
 * <p>
 * <h1>Status as of January 26, 2004</h1>
 * The following improvements are needed to make the Web interface work useful.
 * <ul>
 * <li>
  * The interface needs to respond to right clicks.  The HTTP query generated in
 * response to a click should contain details about the mouse event (e.g., which
 * button was pressed).  Need to consider whether the pop-up menu for right clicks
 * should be presented as a separate pop-up web page or as part of an entirely
 * redrawn page.
 * </li><li>
 * Some conventions need to be established as a replacement for drag and drop.
 * </li><li>
 * The refresh button in the top tool bar should work properly.  At present, it 
 * seems to be relying incorrectly on parts of the display being cached.
 * </li><li>
 * The delay between activating an element in response to a click and re-rendering
 * the web page may need to be increased to allow other threads time to finish.
 * Alternatively, the web page should preriodically auto-refresh itself.
 * </li>
 * </ul>
 * The following changes are needed to improve the appearance of the generated web
 * pages.
 * <ul>
 * <li>
 * Color correction needs to be applied to images that will be displayed on dark
 * backgrounds.  Consider whether it suffices to do this once and for all, by
 * making a parallel, altered copy of the tomcat/webapps/content/data directory.
 * </li><li>
 * Breaks need to be prevented in some elements (e.g., in the Starting Points
 * element in the top tool bar when the window is resized).
 * </li><li>
 * Consideration should be given to the treatment of :doubleSpace and
 * :nonwrappingSpace.  Why is :doubleSpace defined to be "\b\b"?
 * </li>
 * </ul>
 * 
 * @author Stephen Garland
 */
public class OzoneServlet extends HttpServlet {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(OzoneServlet.class);
	
	// TODO Find string for constructing URL from an instance of HTTPservice
	static String s_URLprefix = "http://" + SystemProperties.s_hostname + ":8100/";
	
	static File s_dir = new File(SystemProperties.s_catalinaHome + File.separator + "webapps" + File.separator + 
			"content");
	
	public static File directory() { return s_dir; }
	
	/*
	 *Returns true if information should be generated for the Ozone web interface.
	 *
	 * TODO: (HTML interface) Replace static control by a settable system property.
	 */
	static public boolean enabled() { return false; }
		
	static Hashtable/*[String, IVisualPart[]]*/ pages2VisualParts = new Hashtable();

	/**
	 * Handle HTTP get for http://localhost:8100/servlet/edu.mit.lcs.haystack.server.http.OzoneServlet,
	 * as well as to the abbreviation http://localhost:8100/servlet/haystack defined in tomcat/conf/web.xml
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse resp)
		throws ServletException, IOException {
		FullScreenHostPart frame = (FullScreenHostPart)Ozone.s_context.getProperty(OzoneConstants.s_frame);
		if (frame == null) {
			badRequest(resp, "HTTP rendering available only for standalone Haystack configuration");
			return;
		}
		if (!takeAction(frame, request, resp)) return;
		// Give action that occurred a chance to complete
		// TODO: Find a better way for servlet to wait.  
		// Yielding or sleeping 100ms is not enough.  Auto-refreshing the web page is not a good 
		// solution, because it negates user actions (scrolling, entering text, ...).
		try { Thread.sleep(200); }
		catch (Exception ex) {}
		// Regenerate display
		HTMLengine he = new HTMLengine(s_dir);
		String fileName = he.fileName();
		if (fileName == null) return;  // failed to create file for HTML page
		he.startPage();
		frame.renderHTML(he);
		he.endPage();
		pages2VisualParts.put(he.fileName(), he.parts());
		s_logger.info("Redirecting user's browser to " + s_URLprefix +fileName);
		resp.sendRedirect(s_URLprefix + fileName);
	}
	
	protected boolean takeAction(FullScreenHostPart frame, HttpServletRequest request, HttpServletResponse resp) {
		// Determine what was clicked to generate web request
		String prevFilename = request.getParameter("file");
		if (prevFilename == null) // render current page
			return true;
		IVisualPart[] vp = (IVisualPart[])pages2VisualParts.get(prevFilename);
		if (vp == null) 
			return badRequest(resp, "unknown value (" + prevFilename + ") for file attribute");
		String linkString = request.getParameter("id");
		IVisualPart p = getVisualPart(prevFilename, linkString, vp, resp);
		if (p == null) return false;
		s_logger.info("User clicked on element " + linkString + " on page " + prevFilename);	
		
		IVisualPart ch = p.getClickHandler();
		if (ch == null)
			return badRequest(resp, "no click handler for value " + linkString + " of id attribute in " + prevFilename);
		for (Enumeration pNames = request.getParameterNames(); pNames.hasMoreElements(); ) {
			String pName = (String)pNames.nextElement();
			if (!pName.startsWith("text")) continue;
			IVisualPart pp = getVisualPart(prevFilename, pName.substring(4), vp, resp);
			if (pp == null) continue;
			if (pp instanceof EditPart) {
				try {
					((EditPart)pp).setContentAsync(URLDecoder.decode(request.getParameter(pName), "UTF-8"));
				} catch (Exception e) {
					// We should not get an UnsupportedEncodingException here.
				}
			}
		}
		Event e = new Event();
		e.button = 1;
		e.widget = frame.getComposite();
		MouseEvent me = new MouseEvent(e);
		if (ch.handleGUIEvent(PartConstants.s_eventServletRequest, me)) return true;
		s_logger.info("No action taken in response to GUI event");
		return false;
	}
	
	private IVisualPart getVisualPart(String prevFilename, String name, IVisualPart vp[],
																		HttpServletResponse resp) {
		String problem = null;
		int index = 0;
		if (name == null) problem = "missing id for HTML element";
		else {
			try { 
					index = Integer.parseInt(name); 
					if (index >= vp.length) 
						problem = "HTML element id " + index + " too large";
					else if (vp[index] == null) 
						problem = "no visual part for id " + index + " of HTML element";
			}	
			catch (NumberFormatException e) { 
				problem = "non-integer value for id of HTML element"; 
			}
		}
		if (problem != null) {
			badRequest(resp, problem + " in " + prevFilename);
			return null;
		}
		return vp[index];
	}
		
	protected boolean badRequest(HttpServletResponse r, String s) {
		try { r.sendError(0, s); }
		catch (Exception e) {}
		s_logger.warn(s);
		return false;
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
			doGet(request, response);
		}

}
