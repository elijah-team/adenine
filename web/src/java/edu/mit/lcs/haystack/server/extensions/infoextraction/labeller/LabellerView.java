/*
 * Created on Nov 26, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.labeller;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.BrowserFrame;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Const;
import edu.mit.lcs.haystack.server.extensions.infoextraction.DefaultComposite;
import edu.mit.lcs.haystack.server.extensions.infoextraction.IDocumentFocus;
import edu.mit.lcs.haystack.server.extensions.infoextraction.IDocumentProcessor;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.domnav.DOMNavigator;
import edu.mit.lcs.haystack.server.extensions.infoextraction.domnav.IDOMNavHandler;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 * @author yks
 */
public class LabellerView extends DefaultComposite implements IDocumentProcessor, IDocumentFocus, IDOMNavHandler {

    final static String LABELLED_DATADIR = "labelled_data";

    protected Node curRoot;

    protected List nodeList;

    protected Text output;

    protected List domList;

    protected Group nodeSelection;

    protected LinkedHashMap domListMap;

    protected LinkedHashMap nodeListMap;

    protected Button nextSiblingButton;

    protected Button prevSiblingButton;

    protected Button downTreeButton;

    protected Button upTreeButton;

    protected LabelledDataCache dataCache;

    protected DOMNavigator domNavigator = new DOMNavigator(this);

    /**
     * @param parent
     * @param style
     * @param browserFrame
     */
    public LabellerView(Composite parent, int style, BrowserFrame browserFrame) {
        this(null, parent, style, browserFrame);
    }

    public LabellerView(Display display, Composite parent, int style, BrowserFrame browserFrame) {
        super(display, parent, style, browserFrame);
        this.setLayout(new GridLayout());
        
        /* This is essential for the browser to notify the
         * widget of changes
         */
        browserFrame.addDocumentProcessor(this);
        
        int width = 5;
        dataCache = new LabelledDataCache(LABELLED_DATADIR, this.getBrowser());

        SashForm sash = new SashForm(this, SWT.VERTICAL | SWT.NULL);
        GridLayout sashgl = new GridLayout();
        sashgl.numColumns = 1;
        sash.setLayout(sashgl);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        /**
         * list of urls to select from
         */
        Group cacheUrlsGroup = makeGroup(sash, SWT.NONE, "URLs");

        cachedUrls = new List(cacheUrlsGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        cachedUrls.setToolTipText("click on a url");
        cachedUrls.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH));
        cachedUrls.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String url = getSelection(cachedUrls);
                if (url != null) {
                    processSelection(url, cachedUrls.getSelectionIndex());
                }
            }
        });

        /*
         * currently selected nodes
         */
        nodeSelection = makeGroup(sash, SWT.NONE, "Nodes");
        nodeList = new List(nodeSelection, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        nodeList.setLayoutData(new GridData(GridData.FILL_BOTH));
        nodeList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                processNodeSelection();
            }
        });

        /*
         * node selection UI
         */
        Group labellerControl = makeGroup(sash, SWT.NONE, "Operations");
        labellerControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout();
        gl.numColumns = width;
        labellerControl.setLayout(gl);

        Button addNode = new Button(labellerControl, SWT.NONE);
        addNode.setText("Add Node");
        addNode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addNode.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String selection[] = domList.getSelection();
                if (selection != null && selection.length > 0) {
                    INode node = (INode) domListMap.get(selection[0]);
                    System.err.println("Add Node: " + node.getNodeID().toString());
                    addToNodeList(node);
                }
            }
        });

        Button removeNode = new Button(labellerControl, SWT.NONE);
        removeNode.setText("Remove Node");
        removeNode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeNode.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String selection[] = nodeList.getSelection();
                if (selection != null && selection.length > 0) {
                    dataCache.removeEntry(getCurSelected(), selection[0]);
                    populateNodeList();
                }
            }
        });

        Button quickPopulate = new Button(labellerControl, SWT.NONE);
        quickPopulate.setText("Quick Populate");
        quickPopulate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        quickPopulate.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                System.err.println("Quick Populate clicked");
                quickPopulate();
            }
        });

        Button saveLabelling = new Button(labellerControl, SWT.NONE);
        saveLabelling.setText("Save");
        saveLabelling.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        saveLabelling.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                System.err.println("SAVE clicked");
                dataCache.saveToFile();
                populateNodeList();
            }
        });

        Button deleteLabelling = new Button(labellerControl, SWT.NONE);
        deleteLabelling.setText("Delete");
        deleteLabelling.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        deleteLabelling.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (getCurSelected() != null) {
                    System.err.println("DELETE clicked");
                    dataCache.removeEntry(getCurSelected());
                    populateNodeList();
                }
            }
        });

        domList = new List(labellerControl, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = width;
        domList.setLayoutData(gd);
        domList.setToolTipText("click on an item to see its info");

        domList.addSelectionListener(new SelectionAdapter() {
            // Sent when selection occurs in the control.
            public void widgetSelected(SelectionEvent e) {
                String selection[] = domList.getSelection();
                if (selection != null && selection.length > 0) {
                    INode node = (INode) domListMap.get(selection[0]);
                    getHighlighter().clearHighlighted();
                    getHighlighter().highlightNodeByNodeID(node.getNodeID(), Const.DEFAULT_BGCOLOR, Const.DEFAULT_FGCOLOR);
                    printNodeToOutput(node);
                }
            }
        });

        /*
         * DOM down sibling button (navigates to the url in question
         */
        prevSiblingButton = new Button(labellerControl, SWT.PUSH | SWT.ARROW_LEFT);
        prevSiblingButton.setText("Prev Sib");
        prevSiblingButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        prevSiblingButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                domNavigator.nextSibling();
            }
        });

        /*
         * DOM up sibling button (navigates to the url in question
         */
        nextSiblingButton = new Button(labellerControl, SWT.PUSH | SWT.ARROW_RIGHT);
        nextSiblingButton.setText("Next Sib");
        nextSiblingButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nextSiblingButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                domNavigator.prevSibling();
            }
        });

        /*
         * DOM up button (navigates to the url in question
         */
        upTreeButton = new Button(labellerControl, SWT.PUSH | SWT.ARROW_UP);
        upTreeButton.setText("Up Level");
        upTreeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        upTreeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                domNavigator.moveDownTree();
            }
        });

        /*
         * DOM down button (navigates to the url in question
         */
        downTreeButton = new Button(labellerControl, SWT.PUSH | SWT.ARROW_DOWN);
        downTreeButton.setText("Down Level");
        downTreeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        downTreeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                domNavigator.moveUpTree();
            }
        });

        /*
         * text outputting containing stats about the currently selected node
         */
        Group outputGroup = makeGroup(sash, SWT.NONE, "Output");
        GridData outputGroupGD = new GridData(GridData.FILL_BOTH);
        outputGroupGD.grabExcessVerticalSpace = true;
        outputGroup.setLayoutData(outputGroupGD);
        outputGroup.setLayout(new GridLayout());

        this.output = new Text(outputGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
        output.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
        output.setEditable(false);
        output.setText("");
        output.setSize(80, 100);
    }

    public void processNodeSelection() {
        String selections[] = nodeList.getSelection();
        if (selections != null && selections[0] != null) {
            System.err.println("SELECTED: node[" + selections[0] + "]");

            INode node = (INode) nodeListMap.get(selections[0]);
            getHighlighter().clearHighlighted();
            getHighlighter().highlightNodeByNodeID(node.getNodeID(), Const.DEFAULT_BGCOLOR, Const.DEFAULT_FGCOLOR);

            printNodeToOutput(node);
        }
    }

    private void addToNodeList(INode node) {
        if (nodeListMap == null) {
            nodeListMap = new LinkedHashMap();
        }

        String nodeID = node.getNodeID().toString();
        if (!nodeListMap.containsKey(nodeID)) {
            nodeListMap.put(nodeID, node);
            dataCache.addEntry(getCurSelected(), nodeID);
            System.err.println("addToNodeList(): " + nodeListMap.size());
            System.err.println(Utilities.VectorToString(dataCache.getEntries(getCurSelected())));

            drawNodeList();
        }
    }

    private void setNodeListLabel() {
        String defaultLabel = "Nodes: ";
        nodeSelection.setText(defaultLabel + nodeList.getItemCount());
    }

    private void drawNodeList() {
        int size = nodeListMap.size();
        if (size > 0) {
            System.err.println("drawNodeList(): " + size);

            String[] items = new String[size];
            Iterator it = nodeListMap.keySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                String s = (String) it.next();
                items[i++] = s;
            }

            nodeList.setItems(items);
        } else {
            System.err.println("drawNodeList().clear(): " + size);
            clearNodeList();
        }
        setNodeListLabel();
    }

    private void clearNodeList() {
        nodeList.setItems(new String[0]);
    }

    private void populateNodeList() {
        Vector list = dataCache.getEntries(this.getCurSelected());
        System.err.println("populateNodeList(): curSelected " + this.getCurSelected());
        System.err.println("populateNodeList(): " + Utilities.VectorToString(list));
        System.err.println("populateNodeList(): " + this.getCurSelected());
        nodeListMap = new LinkedHashMap();

        Iterator it = list.iterator();
        while (it.hasNext()) {
            /* resolve the node */
            String s = (String) it.next();
            NodeID nodeID = NodeID.fromString(s);
            INode[] nodes = nodeID.getNodes((INode) this.getDOM());

            if (nodes != null && nodes.length > 0) {
                nodeListMap.put(s, nodes[0]);
            } else {
                System.err.println("populateNodeList(): Can't resolve " + s);
            }
        }
        drawNodeList();
    }

    /**
     * prints the contents of the given node into the output buffer.
     * 
     * @param node
     */
    public void printNodeToOutput(INode node) {
        if (node != null) {
            StringBuffer buf = new StringBuffer();
            buf.append(node.toString(0, "\t"));
            buf.append("\n");
            output.setText(buf.toString());
        }
    }

    /**
     * called when a url is selected in the UI. This method will cause the
     * browserFrame to navigate to the selected url. and reinitialize current
     * state variables.
     *  
     */
    private void processSelection(String url, int index) {
        curRoot = this.getDocumentRoot(url);

        setCurSelected(url);

        if (!browserFrame.getURL().equals(url)) {
            browserFrame.navigateTo(url);
        }

        clearDOMList();
        populateNodeList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IDocumentProcessor#process(edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser,
     *      java.lang.String)
     */
    public void process(IDOMBrowser browser, String url) {
        System.err.println("== LabellerView.process() == START");

        String[] items = this.cachedUrls.getItems();
        for (int i = 0; i < items.length; i++) {
            if (url.equals(items[i])) {
                cachedUrls.select(i);
                setCurSelected(url);
                processSelection(url, i);
            }
        }
        System.err.println("== LabellerView.process() == END");

    }

    private LinkedHashMap buildPathTrace(NodeID nodeID, INode root) {
        LinkedHashMap lhm = new LinkedHashMap();
        buildPathTraceRecur(lhm, nodeID, root, nodeID.getLength());
        return lhm;
    }

    private void buildPathTraceRecur(LinkedHashMap lhm, NodeID nodeID, INode root, int len) {
        System.err.println("LABELLER: " + nodeID.toString() + "; root " + root);
        if (len <= 0) {
            return;
        }

        INode nodes[] = nodeID.getNodes(root);
        if (nodes != null && nodes.length > 0) {
            INode cur = nodes[0];

            INode parent = (INode) cur.getParentNode();
            lhm.put(cur.getNodeID().toString(), cur);

            if (parent != null && cur != parent) {
                NodeID parentID = parent.getNodeID();
                if (parentID != null) {
                    buildPathTraceRecur(lhm, parentID, root, len - 1);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.IDocumentFocus#setFocus(edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode)
     */
    public void setFocus(INode node) {
        NodeID nodeID = node.getNodeID();
        /*
         * generate a list of node ids from the root of the tree to the bottom.
         */
        domListMap = buildPathTrace(nodeID, (INode) this.getDOM());

        domNavigator.setCurNodeID(nodeID);

        drawDOMList();

        System.err.println("FOCUS set on : " + nodeID.toString());
        NodeID n = NodeID.fromString(nodeID.toString());
        INode nodes[] = n.getNodes((INode) this.getDOM());

        System.err.println("FOUND: " + nodes.length);
        printNodeToOutput(node);
    }

    private void clearDOMList() {
        domList.setItems(new String[0]);
    }

    private Vector getMatchingChildren(INode node, INode matching) {
        Vector /* INode */candidates = new Vector();

        NodeList nl = node.getChildNodes();
        String curName = matching.getNodeName();

        for (int i = 0; i < nl.getLength(); i++) {
            INode ithChild = (INode) nl.item(i);

            String ithChildName = ithChild.getNodeName();

            if (ithChild == matching) {
                /* skip self */
                continue;
            }

            if (ithChildName != null && ithChildName.equalsIgnoreCase(curName)) {
                candidates.add(ithChild);
            }

        }
        return candidates;
    }

    private void quickPopulate() {

        NodeID nodeID = domNavigator.getCurNodeID();
        INode[] nodes = nodeID.getNodes((INode) this.getDOM());
        Vector candidates = null;

        if (nodes != null && nodes.length > 0) {
            INode curNode = nodes[0];
            String curName = curNode.getNodeName();
            if (curName == null) {
                System.err.println("QuickPopulate(): node with no name");
                return;
            }

            INode parent = (INode) curNode.getParentNode();
            if (parent != null) {
                candidates = getMatchingChildren(parent, curNode);
                /*
                 * heuristic for nodes that may mismatch siblings by one level
                 */
                if (candidates == null || candidates.size() == 0) {
                    System.err.println("no candidates found for parent");
                    parent = (INode) parent.getParentNode();
                    candidates = getMatchingChildren(parent, curNode);
                }
            }

            /* add self first */
            addToNodeList(curNode);
        }

        /* now populate the node list */
        if (candidates != null) {
            Iterator it = candidates.iterator();
            while (it.hasNext()) {
                INode n = (INode) it.next();
                System.err.println("QuickPopulate(): adding " + ((INode) n).toString());
                addToNodeList(n);
            }
        }
    }

    private void drawDOMList() {
        int size = domListMap.keySet().size();
        if (size > 0) {
            String[] trace = new String[size];
            System.arraycopy(domListMap.keySet().toArray(), 0, trace, 0, size);
            domList.setItems(trace);
        } else {
            clearDOMList();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.domnav.IDOMNavHandler#runAction(edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode)
     */
    public void runAction(INode node) {
        this.setFocus(node);
        NodeID curNodeID = node.getNodeID();
        getHighlighter().highlightNodeByNodeID(curNodeID, Const.DEFAULT_BGCOLOR, Const.DEFAULT_FGCOLOR);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.domnav.IDOMNavHandler#getDOM()
     */
    public INode getDOM() {
        if (this.getCurSelected() != null) {
            return (INode)this.getDocumentRoot(this.getCurSelected());
        } else {
            IDOMBrowser browser = this.getBrowser();
            if (browser != null && browser.getDocument() != null && browser.getDocument().getDocumentElement() != null) {
                return (INode) browser.getDocument().getDocumentElement();
            } else {
                return null;
            }
        }
    }
}