/*
 * Created on Aug 23, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Node;

import edu.mit.lcs.haystack.server.extensions.infoextraction.BrowserFrame;
import edu.mit.lcs.haystack.server.extensions.infoextraction.DefaultComposite;
import edu.mit.lcs.haystack.server.extensions.infoextraction.IDocumentProcessor;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.TreeAlignmentPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.TreeMapping;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class RecordDetectionView extends DefaultComposite implements IProgressMonitor, IDocumentProcessor {
    protected Text output;

    protected RecordDetector recDetector;

    protected List clusterList;

    protected List nodeList;

    protected IAugmentedNode curRoot;

    protected AlgorithmFactory paramSetter = new AlgorithmFactory();

    protected Combo pointSelection;

    /* Current cluster API */
    protected Vector/* ICluster */curClusters = null;

    void setCurrentClusters(Vector v) {
        curClusters = v;
    }

    Vector/* ICluster */getCurrentClusters() {
        return curClusters;
    }

    ICluster getCluster(int i) {
        if (curClusters != null && i >= 0 && i < curClusters.size()) {
            return (ICluster) curClusters.get(i);
        }
        return null;
    }

    /* Current node set API */
    protected Vector/* IPoint */curNodes = null;

    void setCurrentNodes(Vector/* IPoint */v) {
        curNodes = v;
    }

    Vector/* IPoint */getCurrentNodes() {
        return curNodes;
    }

    protected Display display;

    protected ProgressBar progressBar;

    protected IPoint curPoint;

    protected IPoint lastPoint;

    protected HashMap subTreeStore = new HashMap();

    /**
     * called when a url is selected in the UI. This method will cause the
     * browserFrame to navigate to the selected url. and reinitialize current
     * state variables.
     *  
     */
    private void processSelection(String url, int index) {
        System.err.println("RecordDetectionView.processSelection().start");
        curRoot = getDocumentRoot(url);

        setCurSelected(url);

        if (!browserFrame.getURL().equals(url)) {
            browserFrame.navigateTo(url);
        }

        recDetector = null;
        curClusters = null;
        curNodes = null;

        recDetector = (RecordDetector) subTreeStore.get(url);
        if (recDetector != null) {
            setCurrentClusters(recDetector.getClusters());
        }

        if (getCurrentClusters() == null) {
            setCurrentClusters(new Vector());
        }

        populateClusterList(getCurrentClusters());
        clearNodeList();
        clearOutput();
        progressBar.setSelection(0);
        System.err.println("RecordDetectionView.processSelection().end");

    }

    /**
     * Utilities
     */
    private INode getINode(IPoint p) {
        return (INode) p.getData();
    }

    private Vector/* Node */extractNodesFromPointVector(Vector/* IPoint */v) {

        Vector nodes = new Vector();
        Iterator it = curNodes.iterator();
        while (it.hasNext()) {
            IPoint point = (IPoint) it.next();
            Node n = (Node) point.getData();
            nodes.add(n);
        }
        return nodes;
    }

    /**
     * called when a cluster is selected in the cluster list this method will 1.
     * find all the nodes in the given cluster, and highlight them
     *  
     */
    private void processClusterSelection() {
        int selection = clusterList.getSelectionIndex();
        if (selection >= 0) {
            System.err.println("SELECTED: cluster[" + selection + "]");
            ICluster curCluster = (ICluster) getCluster(selection);
            curNodes = curCluster.getMembers();
            if (curCluster != null) {
                populateNodeList(curNodes);
            }

            /* copy nodes into a vector for highlighting */
            Vector hilitNodes = extractNodesFromPointVector(curNodes);

            getHighlighter().clearHighlighted();
            getHighlighter().highlightNodes(hilitNodes.iterator());
        }
    }

    /**
     * called when a specific node is selected. 1. highlight the selected node
     * in the browser 2. dump the text representation of the subtree of the
     * current node 3. diff the currently selected node with a previously
     * selected node and output the treemapping cost.
     */
    private void processNodeSelection() {
        int selection = nodeList.getSelectionIndex();
        if (selection >= 0 && curNodes != null) {
            System.err.println("SELECTED: node[" + selection + "]");

            curPoint = (IPoint) curNodes.get(selection);
            INode curINode = getINode(curPoint);

            getHighlighter().clearHighlighted();
            getHighlighter().highlightNodeByNodeID(curINode.getNodeID());

            StringBuffer buf = new StringBuffer();

            if (lastPoint != null) {
                buf.append("DISTANCE: " + this.recDetector.getCollection().getDistance(curPoint, lastPoint) + "\n");
                if ((curPoint instanceof TreeAlignmentPoint) && (lastPoint instanceof TreeAlignmentPoint)) {
                    TreeAlignmentPoint a = (TreeAlignmentPoint) curPoint;
                    TreeAlignmentPoint b = (TreeAlignmentPoint) lastPoint;
                    buf.append("a.p2pSimilarity(b): " + a.p2pSimilarity(b) +"\n");
                }
            }

            buf.append("-- curNode.toString(0,...) ---------------------\n");
            buf.append(curINode.toString(0, "\t"));
            buf.append("\n");

            buf.append("-- curNode.toString() ---------\n");
            buf.append(curPoint);
            buf.append("\n");

            buf.append("-- curNode.contentSize() ----------\n");
            IAugmentedNode curIAN = (IAugmentedNode) curPoint.getData();
            buf.append(curIAN.contentSize());
            buf.append("\n");

            buf.append("-- curNode.textSize() ----------\n");
            buf.append(curIAN.textSize());
            buf.append("\n");

            buf.append("-- curNode.size() ----------\n");
            buf.append(curIAN.getSize());
            buf.append("\n");

            if (lastPoint != null) {
                INode lastINode = getINode(lastPoint);

                buf.append("-- lastNode.toString(0,...) ---------------------\n");
                buf.append(lastINode.toString(0, "\t"));
                buf.append("\n");

                buf.append("-- lastNode.toString() ---------\n");
                buf.append(lastPoint);
                buf.append("\n");

                buf.append("-- lastNode.contentSize() ----------\n");
                IAugmentedNode lastIAN = (IAugmentedNode) lastPoint.getData();
                buf.append(lastIAN.contentSize());
                buf.append("\n");

                buf.append("-- lastNode.textSize() ----------\n");
                buf.append(lastIAN.textSize());
                buf.append("\n");

                buf.append("-- lastNode.size() ----------\n");
                buf.append(lastIAN.getSize());
                buf.append("\n");

            }
            output.setText(buf.toString());

            lastPoint = curPoint;
        }
    }

    /**
     * populates the nodeList component in the UI.
     * 
     * @param vec
     */
    private void populateNodeList(Vector/* IPoint */vec) {
        String[] items = new String[vec.size()];

        for (int i = 0; i < items.length; i++) {
            IAugmentedNode ian = (IAugmentedNode) ((IPoint) vec.get(i)).getData();
            items[i] = "Nd[" + i + "]: " + ian.toString();
        }
        nodeList.setItems(items);
    }

    private void clearNodeList() {
        nodeList.setItems(new String[0]);
    }

    /**
     * populates the clusterList component in the UI.
     * 
     * @param vec
     */
    private void populateClusterList(Vector/* ICluster */vec) {
        if (vec.size() > 0) {
            String[] items = new String[vec.size()];

            for (int i = 0; i < items.length; i++) {
                items[i] = "Grp[" + i + "]: " + ((ICluster) vec.get(i)).description();
            }
            clusterList.setItems(items);
        } else {
            System.err.println("ERROR: no cluster list in populateClusterList()");
        }
    }

    private void clearClusterList() {
        clusterList.setItems(new String[0]);
    }

    private void clearOutput() {
        output.setText("");
    }

    public RecordDetector getRecordDetector(String url) {
        RecordDetector stg = (RecordDetector) subTreeStore.get(url);
        if (stg == null) {
            stg = new RecordDetector(curRoot);
            subTreeStore.put(url, stg);
        }
        return stg;
    }

    /**
     * Called when a "run" is pressed. 1. finds and fetches the subTreeMapper
     * corresponding to this url. 2. run the series of filtering/clustering
     * algorithms to come out with a set of clusters ranked from most to least
     * likely to be records.
     */
    private void runAlgorithm(String url, int index) {
        if (curRoot == null) {
            processSelection(url, index);
        }

        if (curRoot != null) {
            System.err.println("runAlgorithm: " + url + index);
            try {
                // clear the tree mapping cache/
                // HACK
                TreeMapping.clearCache();

                recDetector = getRecordDetector(url);

                /* set algorithmic parameters */
                recDetector.setAlgorithm(paramSetter.makeAlgorithm());

                /* set the type of point to use */
                int sel = pointSelection.getSelectionIndex();
                String selStr = null;
                if (sel >= 0) {
                    selStr = pointSelection.getItem(sel);
                }
                recDetector.setPointFactory(new PointFactory(selStr));

                recDetector.setProgressMonitor(this);

                PrintStream out = new PrintStream(new FileOutputStream("infoextraction.txt"));

                recDetector.setOutput(out);
                recDetector.run();

                /* retrieve the clusters */
                setCurrentClusters(recDetector.getClusters());
                populateClusterList(getCurrentClusters());

                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public RecordDetectionView(Composite parent, int style, BrowserFrame browserFrame) {
        this(null, parent, style, browserFrame);
    }

    public RecordDetectionView(Display display, Composite parent, int style, BrowserFrame browserFrame) {

        super(display, parent, style, browserFrame);
        this.setLayout(new GridLayout());

        browserFrame.addDocumentProcessor(this);

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

        /**
         * control for initiating processing of the currently selected url.
         */
        Group algorithmParams = makeGroup(sash, SWT.NONE, "Algorithm");
        GridData gd = (GridData) algorithmParams.getLayoutData();
        gd.heightHint = 500;
        gd.grabExcessVerticalSpace = true;
        gd.verticalSpan = 2;
        algorithmParams.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH));

        GridLayout gl = new GridLayout();
        gl.numColumns = 3;
        algorithmParams.setLayout(gl);

        Composite params = paramSetter.makeControls(algorithmParams);

        GridData pgd = new GridData(GridData.FILL_BOTH);
        pgd.horizontalSpan = 3;
        params.setLayoutData(pgd);

        pointSelection = PointFactory.makePointTypeSelector(algorithmParams);
        pointSelection.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_BOTH));

        Button button = new Button(algorithmParams, SWT.NONE);
        button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        button.setText("Run");
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String url = getSelection(cachedUrls);
                System.err.println("selection:" + url);
                if (url != null) {
                    runAlgorithm(url, cachedUrls.getSelectionIndex());
                }
            }
        });

        progressBar = new ProgressBar(algorithmParams, SWT.HORIZONTAL);
        GridData pbld = new GridData(GridData.FILL_HORIZONTAL);
        progressBar.setLayoutData(pbld);

        /*
         * cluster selection UI
         */
        Group clusterSelection = makeGroup(sash, SWT.NONE, "Groups");

        clusterList = new List(clusterSelection, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        clusterList.setLayoutData(new GridData(GridData.FILL_BOTH));
        clusterList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                processClusterSelection();
            }
        });

        /*
         * node selection UI
         */
        Group nodeSelection = makeGroup(sash, SWT.NONE, "Nodes");
        nodeList = new List(nodeSelection, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        nodeList.setLayoutData(new GridData(GridData.FILL_BOTH));
        nodeList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                processNodeSelection();
            }
        });

        /*
         * labels containing stats about the current page
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

    private boolean done = false;

    private boolean cancelled = false;

    ////////////////////////////////
    /// IProgressMonitor methods ///
    ////////////////////////////////
    public void beginTask(String name, int totalWork) {
        progressBar.setMaximum(totalWork);
    }

    public void done() {
        progressBar.setSelection(progressBar.getMaximum());
        this.done = true;
    }

    public boolean isCanceled() {

        return this.cancelled;
    }

    public void setCanceled(boolean value) {

        this.cancelled = value;
    }

    public void worked(int work) {
        progressBar.setSelection(work);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IDocumentProcessor#process(edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser,
     *      java.lang.String)
     */
    public void process(IDOMBrowser browser, String url) {
        System.err.println("RecordDetectionView.process() -- START");
        String[] items = this.cachedUrls.getItems();
        for (int i = 0; i < items.length; i++) {
            if (url.equals(items[i])) {
                cachedUrls.select(i);
                setCurSelected(url);
                processSelection(url, i);
            }
        }
        System.err.println("RecordDetectionView.process() -- END");
    }
}