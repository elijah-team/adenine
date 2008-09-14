/*
 * Created on Aug 17, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.ozone.web.IWebBrowserNavigateListener;
import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.TreeFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMElement;

/**
 * @author yks
 */
public class BrowserFrame extends Composite implements IWebBrowserNavigateListener, MouseListener, IHighlighter {
    protected InternetExplorer browser;

    protected String mainURL = "about:blank";

    protected Text locationTextBox;

    /* browsing history */
    protected Vector history = new Vector();

    protected int historyIndex = 0;

    protected Display display;

    protected boolean encodePage = false;

    public InternetExplorer getBrowserWidget() {
        return browser;
    }

    public IHighlighter getHighlighter() {
        return this;
    }

    public void setURL(String url) {
        mainURL = url;
    }

    public void setEncode(boolean value) {
        encodePage = value;
    }

    public String getURL() {
        return mainURL;
    }

    public void back() {
        if (history.size() > 0 && historyIndex > 0) {
            String url = (String) history.elementAt(historyIndex - 1);
            browser.navigate(url);
            historyIndex -= 1;
        }
    }

    public void forward() {
        if (history.size() > 0 && historyIndex < history.size() - 1) {
            String url = (String) history.elementAt(historyIndex + 1);
            browser.navigate(url);
            historyIndex += 1;
        }
    }

    public void navigateTo(String url) {
        browser.navigate(url);
    }

    public void refresh() {
        if (mainURL != null) {
            browser.navigate(mainURL);
        }
    }

    /**
     * This method generates a browser frame, containing the general controls
     * for the browser.
     * 
     * @param parent
     * @return
     */
    public BrowserFrame(Display display, Composite parent, int style) {
        super(parent, style);
        this.display = display;
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        this.setLayout(gl);

        /*
         * url location textbox
         */
        locationTextBox = new Text(this, SWT.SINGLE | SWT.BORDER);
        GridData locationTextGD = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        locationTextGD.grabExcessHorizontalSpace = true;
        locationTextBox.setLayoutData(locationTextGD);
        locationTextBox.setText(mainURL);
        locationTextBox.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                //Sent when a key is pressed on the system keyboard.
                if (e.character == SWT.CR) {
                    browser.navigate(locationTextBox.getText());
                }
            }
        });

        /*
         * Go button (navigates to the url in question
         */
        Button goButton = new Button(this, SWT.PUSH);
        goButton.setText("Go");

        goButton.setLayoutData(new GridData());
        goButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                if (locationTextBox.getText() != null) {
                    browser.navigate(locationTextBox.getText());
                }
            }
        });

        // browserwindow
        this.browser = new InternetExplorer(this, this);

        GridData browserGD = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.FILL_BOTH);
        browserGD.horizontalSpan = 2;
        browserGD.verticalSpan = 1;
        browserGD.widthHint = 600;
        browserGD.heightHint = 800;
        browser.getControl().setLayoutData(browserGD);
        browser.addNavigateListener(this);
    }

    /**
     * IWebBrowserNavigateListener interface
     */
    public void beforeNavigate(String url) {
    }

    public void navigateComplete() {
        locationTextBox.setText(getBrowserWidget().getLocationURL());
        mainURL = getBrowserWidget().getLocationURL();
    }

    /**
     * recursively traverse the tree, creating a clone of that tree in the
     * process
     */
    protected INode cloneTree(INode tree) {

        NodeList children = tree.getChildNodes();
        INode clone;
        Vector clonedChildren = new Vector();

        for (int i = 0; i < children.getLength(); i++) {
            INode child = (INode) children.item(i);
            String nodeName = child.getNodeName();
            if (!nodeName.equalsIgnoreCase("STYLE") && !nodeName.equalsIgnoreCase("SCRIPT")) {
                INode childClone = cloneTree(child);
                clonedChildren.add(childClone);
            }
        }
        clone = (INode) tree.cloneNode(false);
        for (int i = 0; i < clonedChildren.size(); i++) {
            INode childClone = (INode) clonedChildren.get(i);
            clone.appendChild(childClone);
            childClone.setParent(clone);
        }
        return clone;
    }

    /*
     * returns a rot13 tree
     */
    protected void rot13Tree(INode tree) {

        NodeList children = tree.getChildNodes();
        INode clone;
        Vector clonedChildren = new Vector();
        for (int i = 0; i < children.getLength(); i++) {
            INode child = (INode) children.item(i);
            String nodeName = child.getNodeName();
            if (!nodeName.equalsIgnoreCase("STYLE") && !nodeName.equalsIgnoreCase("SCRIPT")) {
                rot13Tree(child);
                if (child.getNodeType() == INode.TEXT_NODE) {
                    String value = ((IEDOMElement) child).getNodeText();
                    String rot13 = Utilities.rot13(value);
                    ((IEDOMElement) child).setNodeText(rot13);
                }
            }
        }
    }

    /*
     * returns a rot13 tree
     */
    protected INode cloneRot13Tree(INode tree) {

        NodeList children = tree.getChildNodes();
        INode clone;
        Vector clonedChildren = new Vector();
        for (int i = 0; i < children.getLength(); i++) {
            INode child = (INode) children.item(i);
            String nodeName = child.getNodeName();
            if (!nodeName.equalsIgnoreCase("STYLE") && !nodeName.equalsIgnoreCase("SCRIPT")) {
                INode childClone = cloneRot13Tree(child);
                if (childClone.getNodeType() == INode.TEXT_NODE) {
                    ((IEDOMElement) childClone).setNodeText(Utilities.rot13(childClone.getNodeValue()));
                }

                clonedChildren.add(childClone);
            }
        }
        clone = (INode) tree.cloneNode(false);
        for (int i = 0; i < clonedChildren.size(); i++) {
            INode childClone = (INode) clonedChildren.get(i);
            clone.appendChild(childClone);
            childClone.setParent(clone);
        }
        return clone;
    }

    private Vector documentProcessors = new Vector();

    public void addDocumentProcessor(IDocumentProcessor dp) {
        documentProcessors.add(dp);
    }

    public void notifyDocumentProcessors(String url) {
        System.err.println("documentProcessors.size()" + documentProcessors.size());
        Iterator it = documentProcessors.iterator();
        while (it.hasNext()) {
            System.err.println("documentComplete(): calling process");
            IDocumentProcessor idp = (IDocumentProcessor) it.next();
            idp.process(getBrowserWidget(), url);
        }
    }

    private Vector documentFocus = new Vector();

    public void addDocumentFocus(IDocumentFocus dp) {
        documentFocus.add(dp);
    }

    /*
     * called when the document completes loading
     */
    public void documentComplete(String url) {

        if (this.encodePage) {
            this.encodePage();
        }

        if (url.equals(getBrowserWidget().getLocationURL())) {
            if (historyIndex <= history.size()) {

                history.add(url);
                historyIndex = history.size() - 1;

            } else {
                String tmp = (String) history.elementAt(historyIndex);

                if (tmp != null && tmp.equals(url)) {
                    /* nothing...this is the url we want */
                    System.err.println("revisit history: " + url);

                } else {
                    /*
                     * the user went to a different url so we truncate the
                     * history.
                     */
                    history.setSize(historyIndex);
                    history.add(url);
                    historyIndex = history.size() - 1;
                    System.err.println("replace history: " + url);
                }
            }

            notifyDocumentProcessors(url);

            /*
             * INode copy = cloneTree((IDOMElement) getBrowser().getDocument()
             * .getDocumentElement()); System.err.println("copyHeight: " +
             * copy.getHeight()); storeURL(url, copy);
             */
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.ozone.widgets.IWebBrowserNavigateListener#statusTextChange(java.lang.String)
     */
    public void statusTextChange(String status) { }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.ozone.widgets.IWebBrowserNavigateListener#progressChange(int,
     *      int)
     */
    public void progressChange(int progress, int progressMax) { }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
     */
    public void mouseDoubleClick(MouseEvent e) {
        System.err.println("mouseDoubleClick");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
     */
    public void mouseDown(MouseEvent e) {

        System.err.println("mouseDown:" + e.button);
        if (e.button == 3) {
            System.err.println("button " + e.button + " clicked at x:" + e.x + " y:" + e.y);

            Point b = getBrowserWidget().getControl().toControl(new Point(e.x, e.y));
            IAugmentedNode clicked = (IAugmentedNode) ((IDOMBrowser) getBrowserWidget()).getDocument().getElementAtPoint(b.x, b.y);
            INode clickedINode = (INode) clicked;
            INode browserRoot = (INode) getBrowserWidget().getDocument().getDocumentElement();

            if (clicked != null) {
                AbstractFeature[] features = null;

                NodeID nodeID = clickedINode.getNodeID();

                System.err.println("NodeID: " + nodeID.toString());

                Iterator it = documentFocus.iterator();
                while (it.hasNext()) {
                    IDocumentFocus idp = (IDocumentFocus) it.next();
                    idp.setFocus(clickedINode);
                }

                //TODO: must register a list of event listeners maybe?

                /*
                 * IFeatureSet f = this.getFragStore().get(
                 * this.locationTextBox.getText()); // if (f instanceof
                 * NGramsFragmentSet) { // ((NGramsFragmentSet)f).test(); // }
                 * 
                 * INode dataTree = (INode) f.getRoot();
                 * 
                 * INode[] targetNodes = nodeID.getNodes(dataTree);
                 * 
                 * if (targetNodes != null && targetNodes.length > 0) { INode t =
                 * targetNodes[0]; System.err.println("Matched: " +
                 * t.getNodeName()); IAugmentedNode targetNode =
                 * (IAugmentedNode) targetNodes[0]; features =
                 * targetNode.getFeatures(); } else { System.err.println("Node
                 * not found!"); }
                 * 
                 * if (features != null) { for (int i = 0; i < features.length;
                 * i++) { System.err.println("f[" + i + "]: " +
                 * features[i].toString()); } } else { System.err.println("NO
                 * features"); }
                 */} else {
                System.err.println("I detect no click!!");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
     */
    public void mouseUp(MouseEvent e) {
        System.err.println("mouseUp:" + e.button);
    }

    public void clearHighlighted() {
        IDOMDocument doc = getBrowserWidget().getDocument();

        IDOMElement[] highlightedElements = doc.getHighlightedElements();
        for (int i = 0; i < highlightedElements.length; i++) {
            highlightedElements[i].unhighlight();
        }
        doc.clearHighlightedElements();
    }

    /**
     * Given a tree feature, a tag path, hilit all nodes in the dom shares that
     * tag path
     * 
     * @param feature -
     *            string representation of the tree feature
     */
    public void highlightNodesByFeature(String feature, String bgcolor, String fgcolor) {
        IDOMDocument doc = getBrowserWidget().getDocument();

        TreeFeature fragInstance = TreeFeature.fromString(feature);
        System.err.println("Fragment:" + fragInstance.toString());
        Vector toHighlight = fragInstance.getNodes((INode) (doc.getDocumentElement()));

        clearHighlighted();

        for (int i = 0; i < toHighlight.size(); i++) {
            IDOMElement victim = (IDOMElement) toHighlight.get(i);
            System.err.println("victim[" + i + "]: " + victim.getNodeName());
            IDOMElement highlighted = ((IDOMElement) victim).highlight(bgcolor, fgcolor);
            getBrowserWidget().getDocument().addHighlightedElement(highlighted);
        }

    }

    /**
     * given a tree feature, a tag path, hilit all nodes in the dom shares that
     * tag path
     * 
     * @param feature -
     *            string representation of the tree feature
     */
    public void highlightNodeByNodeID(NodeID nodeID, String bgcolor, String fgcolor) {
        IDOMDocument doc = getBrowserWidget().getDocument();
        INode[] toHighlights = nodeID.getNodes(doc);

        clearHighlighted();

        if (toHighlights != null && toHighlights.length > 0) {
            IDOMElement victim = (IDOMElement) toHighlights[0];
            IDOMElement highlighted = ((IDOMElement) victim).highlight(bgcolor, fgcolor);
            getBrowserWidget().getDocument().addHighlightedElement(highlighted);
        }
    }

    /**
     * highlight nodes in the given vector of INode instances
     */
    public void highlightNodes(Iterator/* <INode> */nodeIt, String bgcolor, String fgcolor) {
        IDOMDocument doc = getBrowserWidget().getDocument();

        clearHighlighted();

        while (nodeIt.hasNext()) {
            INode node = (INode) nodeIt.next();
            INode[] toHighlights = node.getNodeID().getNodes(doc);

            if (toHighlights != null && toHighlights.length > 0) {
                IDOMElement victim = (IDOMElement) toHighlights[0];
                IDOMElement highlighted = ((IDOMElement) victim).highlight(bgcolor, fgcolor);
                getBrowserWidget().getDocument().addHighlightedElement(highlighted);
            }
        }
    }

    /**
     * highlight nodes by nodeID.
     */
    public void highlightNodeByNodeID(NodeID id) {
        highlightNodeByNodeID(id, Const.DEFAULT_BGCOLOR, Const.DEFAULT_FGCOLOR);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IHighlighter#highlightNodes(java.util.Vector)
     */
    public void highlightNodes(Iterator nodesIt) {
        highlightNodes(nodesIt, Const.DEFAULT_BGCOLOR, Const.DEFAULT_FGCOLOR);
    }

    /* encode the entire page in rot13 */
    public void encodePage() {
        System.err.println("entering encode");
        IDOMDocument doc = getBrowserWidget().getDocument();
        INode root = (INode) doc.getDocumentElement();
        this.rot13Tree(root);
        System.err.println("leaving encode");
    }
}