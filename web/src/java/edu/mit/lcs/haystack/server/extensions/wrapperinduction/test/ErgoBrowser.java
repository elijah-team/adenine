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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.mit.lcs.haystack.ozone.web.IWebBrowserNavigateListener;
import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.ozone.web.WebViewPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Pattern;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;

/**
 * @version 1.0
 * @author Andrew Hogue
 */
public class ErgoBrowser implements IWebBrowserNavigateListener, MouseListener {

    //    protected static final String DEFAULT_URL =
    // "file:///c:/cygwin/home/ahogue/projects/haystack/test.html";
    //    protected static final String DEFAULT_URL =
    // "file:///c:/cygwin/home/ahogue/projects/haystack/htmltest/nytimespartial.html";
    //    protected static final String DEFAULT_URL = "http://nytimes.com";
    //    protected static final String DEFAULT_URL = "http://gizmodo.com";
    //    protected static final String DEFAULT_URL = "http://slashdot.org";
    //    protected static final String DEFAULT_URL =
    // "http://www.google.com/search?q=haystack";
    protected static final String DEFAULT_URL = "http://www.imdb.com/title/tt0033467/";

    //    protected static final String DEFAULT_URL =
    // "http://www.csail.mit.edu/biographies/PI/biolist.php";

    protected static final String RDF_STORE_FILE = "ergo_store.rdf";

    protected WrapperManager ergo;

    protected WebViewPart webView;

    protected Display display;

    protected Shell shell;

    protected InternetExplorer browser;

    protected String url;

    protected IRDFContainer rdfc;

    protected Pattern currentPattern;

    protected Text locationText;

    protected Text maxExampleSize;

    protected Text minRepeatSize;

    protected Text costLimit;

    protected Text classLabel;

    protected Text propertyLabel;

    protected Text patternText;

    protected Text domainText;

    protected Text pageText;

    protected Text patternNameText;

    public ErgoBrowser(Display _display) {
        this(_display, null);
    }

    public ErgoBrowser(Display _display, String _url) {
        this.display = _display;
        this.url = (_url == null) ? DEFAULT_URL : _url;
        this.webView = new WebViewPart();
    }

    public static void main(String[] args) throws Exception {
        try {
            Display display = new Display();
            ErgoBrowser ergoBrowser = new ErgoBrowser(display);
            Shell ergoShell = ergoBrowser.open();

            while (!ergoShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
            ergoBrowser.close();
            display.dispose();
        } catch (Throwable e) {
            Thread.sleep(1000);
            e.printStackTrace();
        }
    }

    public Shell open() throws Exception {
        shell = new Shell(display, SWT.SHELL_TRIM);
        shell.setMaximized(true);

        try {
            GridLayout gl = new GridLayout();
            gl.numColumns = 4;
            shell.setLayout(gl);

            Label location = new Label(shell, SWT.CENTER);
            location.setText("Location:");
            location.setLayoutData(new GridData(
                    GridData.HORIZONTAL_ALIGN_CENTER));

            locationText = new Text(shell, SWT.SINGLE | SWT.BORDER);
            GridData locationGridData = new GridData(GridData.GRAB_HORIZONTAL
                    | GridData.FILL_HORIZONTAL);
            locationGridData.grabExcessHorizontalSpace = true;
            locationGridData.horizontalSpan = 2;
            locationText.setLayoutData(locationGridData);
            locationText.setText(this.url);

            Button go = new Button(shell, SWT.PUSH);
            go.setText("Go");
            go.setLayoutData(new GridData());
            go.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (locationText.getText() != null) {
                        webView.navigate(new Resource(locationText.getText()));
                    }
                }
            });

            Button addActiveNode = new Button(shell, SWT.PUSH);
            addActiveNode.setText("Positive Example");
            GridData addActiveNodeGridData = new GridData(
                    GridData.VERTICAL_ALIGN_BEGINNING);
            addActiveNodeGridData.horizontalSpan = 2;
            addActiveNode.setLayoutData(addActiveNodeGridData);
            addActiveNode.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    WrapperManager.clearHighlightedElements(webView);

                    try {
                        if (currentPattern == null) {
                            currentPattern = Pattern.fromResource(WrapperManager
                                    .createPattern(getRDFContainer(), webView,
                                            new Resource(classLabel.getText()),
                                            new Resource(classLabel.getText()),
                                            maxExampleSize.getText(), costLimit
                                                    .getText()),
                                    getRDFContainer());
                        } else {
                            currentPattern = Pattern
                                    .fromResource(
                                            WrapperManager
                                                    .addPositiveExample(
                                                            getRDFContainer(),
                                                            webView,
                                                            currentPattern
                                                                    .makeResource(getRDFContainer()),
                                                            costLimit.getText()),
                                            getRDFContainer());
                        }
                    } catch (RDFException ee) {
                        ee.printStackTrace();
                    }

                    refreshPattern(currentPattern);
                    System.out
                            .println("exiting addActiveNode.widgetSelected()");
                }
            });

            // browser
            this.browser = new InternetExplorer(shell, this);
            this.webView.setWebBrowser(this.browser);
            //	    this.ergo = new WrapperManager(browser);
            GridData browserGridData = new GridData(
                    GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL
                            | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
            browserGridData.widthHint = 700;
            browserGridData.heightHint = 500;
            browserGridData.horizontalSpan = 2;
            browserGridData.verticalSpan = 7;
            browser.getControl().setLayoutData(browserGridData);
            browser.addNavigateListener(this);

            Button addProperty = new Button(shell, SWT.PUSH);
            addProperty.setText("Add Property");
            GridData addPropertyGridData = new GridData(
                    GridData.VERTICAL_ALIGN_BEGINNING);
            addProperty.setLayoutData(addPropertyGridData);
            addProperty.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    if (currentPattern == null)
                        return;
                    WrapperManager.clearHighlightedElements(webView);

                    try {
                        RDFNode[] context = WrapperManager.getPatternContext(
                                getRDFContainer(), webView);
                        currentPattern = Pattern.fromResource(WrapperManager
                                .updatePatternProperty(getRDFContainer(),
                                        webView, (Resource) context[0],
                                        (Resource) context[1], new Resource(
                                                propertyLabel.getText()),
                                        context[2], context[3]),
                                getRDFContainer());
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }

                    WrapperManager.highlightPattern(currentPattern, webView);
                    refreshPattern(currentPattern);
                }
            });

            propertyLabel = new Text(shell, SWT.RIGHT);
            propertyLabel.setText("dc:title");
            propertyLabel.setLayoutData(new GridData(
                    GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END
                            | GridData.GRAB_HORIZONTAL
                            | GridData.FILL_HORIZONTAL));

            Button removeWrappers = new Button(shell, SWT.PUSH);
            removeWrappers.setText("Remove Wrappers");
            GridData removeWrappersGridData = new GridData(
                    GridData.VERTICAL_ALIGN_BEGINNING);
            removeWrappersGridData.horizontalSpan = 2;
            removeWrappers.setLayoutData(removeWrappersGridData);
            removeWrappers.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    try {
                        WrapperManager.removeWrappers(getRDFContainer(),
                                webView, new Resource(browser.getDocument()
                                        .getURL()));
                    } catch (RDFException ee) {
                        ee.printStackTrace();
                    }
                }
            });

            Label maxExampleSizeLabel = new Label(shell, SWT.LEFT);
            maxExampleSizeLabel.setText("Max Example Size:");
            maxExampleSizeLabel.setLayoutData(new GridData(
                    GridData.HORIZONTAL_ALIGN_BEGINNING
                            | GridData.VERTICAL_ALIGN_END));

            maxExampleSize = new Text(shell, SWT.RIGHT);
            maxExampleSize.setText("150");
            maxExampleSize.setLayoutData(new GridData(
                    GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END
                            | GridData.GRAB_HORIZONTAL
                            | GridData.FILL_HORIZONTAL));

            Label costLimitLabel = new Label(shell, SWT.LEFT);
            costLimitLabel.setText("Cost Limit:");
            costLimitLabel.setLayoutData(new GridData(
                    GridData.HORIZONTAL_ALIGN_BEGINNING
                            | GridData.VERTICAL_ALIGN_END));

            costLimit = new Text(shell, SWT.RIGHT);
            costLimit.setText(".5");
            costLimit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END
                    | GridData.VERTICAL_ALIGN_END | GridData.GRAB_HORIZONTAL
                    | GridData.FILL_HORIZONTAL));

            Label classLabelLabel = new Label(shell, SWT.LEFT);
            classLabelLabel.setText("Semantic Class:");
            classLabelLabel.setLayoutData(new GridData(
                    GridData.HORIZONTAL_ALIGN_BEGINNING
                            | GridData.VERTICAL_ALIGN_END));

            classLabel = new Text(shell, SWT.RIGHT);
            classLabel.setText("Search Result");
            classLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END
                    | GridData.VERTICAL_ALIGN_END | GridData.GRAB_HORIZONTAL
                    | GridData.FILL_HORIZONTAL));

            patternText = new Text(shell, SWT.MULTI | SWT.READ_ONLY | SWT.LEFT);
            GridData patternTextGridData = new GridData(
                    GridData.HORIZONTAL_ALIGN_BEGINNING
                            | GridData.VERTICAL_ALIGN_BEGINNING
                            | GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL
                            | GridData.GRAB_VERTICAL);
            patternTextGridData.grabExcessHorizontalSpace = true;
            patternTextGridData.grabExcessVerticalSpace = true;
            patternTextGridData.heightHint = 300;
            patternTextGridData.widthHint = 150;
            patternTextGridData.horizontalSpan = 2;
            patternText.setLayoutData(patternTextGridData);

            webView.navigate(new Resource(url));

            shell.pack();
            shell.open();
        } catch (Exception e) {
            System.out.println("Exception while opening ErgoBrowser: " + e);
        }

        return shell;
    }

    /**
     * Refreshes the Pattern list.
     */
    //     public void refreshPatternList() {
    // 	try {
    // 	    patternsList.removeAll();
    // 	    Resource[] patterns = ergo.getPatterns(browser.getDocument().getDomain(),
    // 						   getPathname());
    // 	    if (patterns == null || patterns.length <= 0) {
    // 		patternsList.add("No Patterns...");
    // 	    }
    // 	    else {
    // 		for (int i = 0; i < patterns.length; i++) {
    // 		    patternsList.add(ergo.getPatternString(patterns[i]));
    // 		}
    // 	    }
    // 	}
    // 	catch (Exception e) {
    // 	    System.out.println("Exception while refreshing pattern list: " + e);
    // 	    e.printStackTrace();
    // 	}
    //     }
    /**
     * Refreshes the text of the current pattern
     */
    public void refreshPattern(Pattern p) {
        if (p == null || p.getRoot() == null)
            patternText.setText("");
        else
            patternText.setText(p.getRoot().toString(0, "      "));
    }

    /**
     * Closes this browser, writing the current store to RDF_STORE_FILE.
     */
    public void close() throws RDFException {
        //  	boolean retVal = Utilities.generateRDF(getRDFContainer(), new
        // File(RDF_STORE_FILE));
        //  	if (!retVal) System.out.println("Warning: ErgoBrowser.close() failed
        // to write RDF to file");
    }

    /**
     * Either loads the RDF container stored on disk at RDF_STORE_FILE or
     * creates a blank one.
     */
    protected IRDFContainer getRDFContainer() throws RDFException {
        if (this.rdfc == null) {
            try {
                this.rdfc = new LocalRDFContainer();
                if (new File(RDF_STORE_FILE).exists()) {
                    Utilities.parseRDF(new URL("file", "", RDF_STORE_FILE
                            .replace(File.separatorChar, '/')), this.rdfc);
                }
            } catch (MalformedURLException e) {
                System.out.println("Exception getting RDF Container:\n");
                e.printStackTrace();
            }
        }
        return this.rdfc;
    }

    public void beforeNavigate(String _url) {
    }

    public void navigateComplete() {
        shell.setText("Ergo Browser - " + browser.getDocument().getTitle());
        currentPattern = null;
        //	refreshPatternList();
        refreshPattern(currentPattern);
        locationText.setText(browser.getLocationURL());
    }

    public void documentComplete(String url) {
        // documentComplete() seems to get called 4+ times for every
        // load, don't want to repeat the pattern highlighting...
        // TODO: this will break user refreshes...
        if (browser.getDocument().getURL().equalsIgnoreCase(this.url))
            return;
        System.out.println("documentComplete()");
        this.url = browser.getDocument().getURL();
        try {
            Resource[] patternRes = WrapperManager.getPatternResources(this
                    .getRDFContainer(), browser.getDocument().getURL());
            for (int i = 0; i < patternRes.length; i++) {
                WrapperManager.highlightPattern(Pattern.fromResource(patternRes[i], this
                        .getRDFContainer()), this.webView);
            }
        } catch (RDFException e) {
            System.out
                    .println("Error highlighting patterns in ErgoBrowser.navigateComplete():\n");
            e.printStackTrace();
        }
    }

    public void statusTextChange(String status) {
    }

    public void progressChange(int progress, int progressMax) {
    }

    /**
     * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(MouseEvent)
     */
    public void mouseDoubleClick(MouseEvent e) {
    }

    /**
     * Implemented to receive context menu requests from browser widget.
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseDown(MouseEvent)
     */
    public void mouseDown(MouseEvent e) {
        // handle right clicks only.
        if (e.button == 3) {
            Point b = browser.getControl().toControl(new Point(e.x, e.y));
            System.out.println("Mouse click at {" + e.x + ", " + e.y + "}");
            IDOMElement clicked = browser.getDocument().getElementAtPoint(b.x,
                    b.y);
            if (clicked == null)
                System.out.println("got null element from getElementAtPoint()");
            else
                patternText.setText(clicked.toString(1, "  "));
        }
    }

    /**
     * @see org.eclipse.swt.events.MouseListener#mouseUp(MouseEvent)
     */
    public void mouseUp(MouseEvent e) {
    }

}