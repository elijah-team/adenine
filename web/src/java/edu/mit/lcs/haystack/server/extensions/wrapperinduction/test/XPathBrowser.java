package edu.mit.lcs.haystack.server.extensions.wrapperinduction.test;

import java.util.ArrayList;

import org.apache.xpath.XPathAPI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEW3CElement;


/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class XPathBrowser {

    protected static final String RDF_STORE_FILE = "ergo_store.rdf";
    
    protected Display display;
    protected Shell shell;
    protected Shell comparisonDialog;
    
    protected IEXPathBrowser browser;
    protected Text xPathText;

    protected ArrayList highlightedElements;

    public XPathBrowser(Display _display) {
	this.display = _display;
	this.highlightedElements = new ArrayList();
    }
    
    public static void main(String [] args) throws Exception {
	try {
	    Display display = new Display();
	    XPathBrowser browser = new XPathBrowser(display);
	    Shell xPathShell = browser.open();
	    
	    while (!xPathShell.isDisposed()) {
		if (!display.readAndDispatch()) display.sleep();
	    }
	    browser.close();
	    display.dispose();
	}
	catch (Throwable e) {
	    System.out.println("Exception in main: " + e);
	    e.printStackTrace();
	}
    }

    public Shell open() throws Exception {
	shell = new Shell(display, SWT.SHELL_TRIM);
	shell.setMaximized(true);
	shell.setText("Tree Edit Browser");
	
	try {
	    GridLayout gl = new GridLayout();
	    gl.numColumns = 3;
	    shell.setLayout(gl);

	    Label xPathLabel = new Label(shell, SWT.LEFT);
	    xPathLabel.setText("XPath:");
	    xPathLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
						  

	    xPathText = new Text(shell, SWT.LEFT | SWT.BORDER);
	    xPathText.setText("/html/body/div/p/a");
	    xPathText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL |
						 GridData.FILL_HORIZONTAL));
	    

	    Button findButton = new Button(shell, SWT.LEFT);
	    GridData findButtonGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING |
							  GridData.VERTICAL_ALIGN_BEGINNING);
	    findButton.setLayoutData(findButtonGridData);
	    findButton.setText("Find");
	    findButton.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent e) {
			try {
			    unhighlightHighlightedElements();
			    
			    Document doc = browser.getDocument();
			    NodeIterator iter = XPathAPI.selectNodeIterator(doc,
									    xPathText.getText());
			    Node n;
			    while ((n = iter.nextNode()) != null) {
				System.out.println("Highlighting element " + n.getNodeName());
				highlightElement((IEW3CElement)n);
			    }
			}
			catch (Exception ex) {
			    System.out.println("Exception: " + ex);
			    ex.printStackTrace();
			}
		    }
		});
	    


	    browser = new IEXPathBrowser(shell, SWT.CENTER, "http://www.google.com/search?q=haystack");
	    GridData browserGridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER |
						    GridData.GRAB_HORIZONTAL |
						    GridData.GRAB_VERTICAL | 
						    GridData.FILL_BOTH);
	    browserGridData.widthHint = 700;
	    browserGridData.heightHint = 250;
	    browserGridData.horizontalSpan = 3;
	    browser.setLayoutData(browserGridData);


	    shell.pack();
	    shell.open();
	}
	catch (Exception e) {
	    System.out.println("Exception while opening TreeEditBrowser: " + e);
	    e.printStackTrace();
	}

	return shell;
    }

    public void close() {
	if (this.browser != null) this.browser.dispose();
    }

    protected void highlightElement(IEW3CElement toHighlight) {
	toHighlight.highlight();
	this.highlightedElements.add(toHighlight);
    }

    protected void highlightElements(IEW3CElement[] toHighlight) {
	for (int i = 0; i < toHighlight.length; i++) {
	    highlightElement(toHighlight[i]);
	}
    }

    protected void unhighlightHighlightedElements() {
	for (int i = 0; i < highlightedElements.size(); i++) {
	    ((IEW3CElement)highlightedElements.get(i)).unhighlight();
	}
	highlightedElements.clear();
    }
}


