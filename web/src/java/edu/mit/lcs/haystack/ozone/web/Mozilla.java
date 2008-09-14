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

package edu.mit.lcs.haystack.ozone.web;

import java.util.*;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.internal.mozilla.nsIWebBrowser;
import org.eclipse.swt.browser.*;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.mozilla.MozDOMDocument;

/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class Mozilla implements IWebBrowser, IDOMBrowser {

    protected Browser m_browser;

    protected HashSet m_navigateListeners = new HashSet();
    
    protected HashSet m_webOperationListeners = new HashSet();
        
    protected IDOMDocument m_document;

    public Mozilla(Composite parent, Listener contextMenuListener) {
	m_browser = new Browser(parent, 0);
	
	m_browser.addListener(SWT.MenuDetect, contextMenuListener);
	
	m_browser.addLocationListener(new LocationListener() {
		public void changed(LocationEvent event) {
		    //		    System.out.println(">>>>> got LocationEvent.changed() for " + event.location);
		    Iterator i = m_navigateListeners.iterator();
		    while (i.hasNext()) {
			((IWebBrowserNavigateListener)i.next()).beforeNavigate(event.location);
		    }
		}
		/* (non-Javadoc)
		 * @see org.eclipse.swt.browser.LocationListener#changing(org.eclipse.swt.browser.LocationEvent)
		 */
		public void changing(LocationEvent event) { }
	    });

	m_browser.addProgressListener(new ProgressListener() {
		public void changed(ProgressEvent event) {
		    //		    System.out.println(">>>>> got ProgressEvent.changed for " + event.current + "/" + event.total);
		    Iterator i = m_navigateListeners.iterator();
		    while (i.hasNext()) {
			((IWebBrowserNavigateListener)i.next()).progressChange(event.current, event.total);
		    }
		}
		public void completed(ProgressEvent event) {
		    //		    System.out.println(">>>>> got ProgressEvent.completed for " + event.current + "/" + event.total);
		    try {
		    	Iterator i = m_navigateListeners.iterator();
		    	while (i.hasNext()) {
		    		IWebBrowserNavigateListener wbnl = (IWebBrowserNavigateListener)i.next();
		    		m_document = null;
		    		wbnl.documentComplete(getLocationURL());
		    		wbnl.navigateComplete();
		    	}
		    }
			catch(RuntimeException e) {
				e.printStackTrace();
			}
		}}
	    );

	m_browser.addStatusTextListener(new StatusTextListener() {
		public void changed(StatusTextEvent event) {
		    //		    System.out.println(">>>>> got StatusTextEvent.changed for " + event.text);
		    Iterator i = m_navigateListeners.iterator();
		    while (i.hasNext()) {
			((IWebBrowserNavigateListener)i.next()).statusTextChange(event.text);
		    }
		}
	    });
    }
	
    /**
     * @see IWebBrowser#navigate(String)
     */
    public void navigate(String url) {
	m_browser.setUrl(url);
    }
	
    public String getLocationName() {
    	return this.getDocument().getTitle();
	}

    public String getLocationURL() {
	return this.getURL();
    }

    /**
     * @see IWebBrowser#getControl()
     */
    public Control getControl() {
	return m_browser;
    }

    /**
     * @see IWebBrowser#addNavigateListener(IWebBrowserNavigateListener)
     */
    public void addNavigateListener(IWebBrowserNavigateListener wbnl) {
	m_navigateListeners.add(wbnl);	
    }

    /**
     * @see IWebBrowser#removeNavigateListener(IWebBrowserNavigateListener)
     */
    public void removeNavigateListener(IWebBrowserNavigateListener wbnl) {
	m_navigateListeners.remove(wbnl);
    }


    public static void main(String[] args) throws Exception {
	/*try {
	    Display display = new Display();
	    
	    Shell shell = new Shell(display, SWT.SHELL_TRIM);
	    shell.setText("Mozilla Test");
	    
	    GridLayout gl = new GridLayout();
	    gl.numColumns = 1;
	    shell.setLayout(gl);
	    Mozilla moz = new Mozilla(shell);

	    GridData gd = new GridData();
	    gd.horizontalAlignment = GridData.FILL;
	    gd.verticalAlignment = GridData.FILL;
	    gd.grabExcessHorizontalSpace = true;
	    gd.grabExcessVerticalSpace = true;
	    gd.widthHint = 800;
	    gd.heightHint = 600;
	    moz.getControl().setLayoutData(gd);

	    shell.pack();
	    shell.open();

	    moz.navigate("http://www.google.com/");

	    while (!shell.isDisposed()) {
		if (!display.readAndDispatch()) display.sleep();
	    }
	    display.dispose();
	}
	catch (Throwable e) {
	    System.out.println("Excepion in Mozilla.main: " + e);
	    e.printStackTrace();
	}*/
    }

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMBrowser#getDocument()
	 */
	public IDOMDocument getDocument() {
		if(m_document == null) {
			nsIWebBrowser webBrowser = m_browser.getNSBrowser();
			m_document = new MozDOMDocument(webBrowser);
		}
		return m_document;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.IDOMBrowser#getURL()
	 */
	public String getURL() {
		return this.getDocument().getURL();
	}

	/*
	 * @see IWebBrowser#addWebOperationListener(IWebOperationListener)
	 */
	public void addWebOperationListener(IWebOperationListener wol) {
		m_webOperationListeners.add(wol);
	}

	/* 
	 * @see IWebBrowser#removeWebOperationListener(IWebOperationListener)
	 */
	public void removeWebOperationListener(IWebOperationListener wol) {
		m_webOperationListeners.remove(wol);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.standard.widgets.IWebBrowser#navigate(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void navigate(String url, String headers, String postData) {
		throw new RuntimeException("Unimplemented method in Mozilla");
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IWebBrowser#getWebOpOccurred()
	 */
	public boolean getWebOpOccurred() {
		throw new RuntimeException("Unimplemented method in Mozilla");
	}
}
