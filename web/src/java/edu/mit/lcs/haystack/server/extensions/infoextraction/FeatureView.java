/*
 * Created on Aug 17, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.DefaultFeatureSet;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.FeatureStore;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.TreeFeature;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 * @author yks
 */
public class FeatureView extends DefaultComposite {
    protected Combo featureCombo = null;

    protected List featureList = null;

    protected List traceList = null;

    protected Text output;

    protected LinkedHashMap traceListMap;

    /**
     * generates a mapping of NodeID string of the form <siblingno>.
     * <siblingno>... to the actual node at that nodeID.
     * 
     * the keys of the mapping is all the subpaths from the root of the given
     * nodeId
     * 
     * e.g. input 1.1.3.4.2 outputs 1 1.1 1.1.3 1.1.3.4 1.1.3.4.2
     * 
     * @param nodeId
     * @return a sorted map (keys sorted by node-paths by increasing path
     *         length) and the values are the Nodes from the given browser/DOM.
     */
    static public LinkedHashMap/* String,INode */buildPathTrace(Vector nodeId,
            IDOMBrowser browser) {
        LinkedHashMap lhm = new LinkedHashMap();

        if (nodeId.size() > 0) {
            int[] nodeNum = new int[nodeId.size()];

            String nodeIdString = new String();

            for (int i = 0; i < nodeId.size(); i++) {
                nodeNum[i] = ((Integer) nodeId.get(i)).intValue();

                nodeIdString += "." + nodeNum[i];

                try {
                    /*
                     * make a copy off the nodeId, but could use
                     * System.arrayCopy
                     */

                    int nodeIdCopy[] = new int[i + 1];
                    System.arraycopy(nodeNum, 0, nodeIdCopy, 0, i + 1);

                    NodeID targetNode = new NodeID(nodeIdCopy);

                    INode[] targetNodes = targetNode.getNodes((INode)browser
                            .getDocument().getDocumentElement());

                    if (targetNodes != null && targetNodes.length > 0) {
                        String key = targetNodes[0].getNodeID().toString();
                        lhm.put(key, targetNodes[0]);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return lhm;
    }

    /**
     * For a given set of features. 1. find nodes (and their node paths) that
     * match said features 2. generate a by-tree-height histogram of the
     * frequencies by which particular nodes appear in node-paths found in 1. 3.
     * return a node path that has the highest frequencies from step 2.
     * 
     * @param features
     * @return vector of sibling numbers.
     */
    public Vector runBestGuess(AbstractFeature[] items) {
        int threshold = 7;
        boolean DEBUG = true;

        /* all matching nodes */
        Vector matches = new Vector();

        IDOMElement root = (IDOMElement) getBrowser().getDocument()
                .getDocumentElement();

        /*
         * 1. get all the nodes that match the features in the given <code>
         * items </code> set.
         */
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof TreeFeature) {
                TreeFeature frag = (TreeFeature) items[i];
                Vector nodes = frag.getNodes(root);
                matches.addAll(nodes);
            }
        }

        Vector treeHeight = new Vector();

        /*
         * Iterate thru all matching nodes, and at each level of all nodes
         * visited, keep count the frequency of occurrence.
         * 
         * we get a vector [treeHeight] that contains at each level, the counts
         * of each sibling.
         */
        Iterator it = matches.iterator();
        while (it.hasNext()) {
            INode node = (INode) it.next();
            NodeID id = node.getNodeID();

            for (int j = 0; j < id.getLength(); j++) {
                int siblingno = id.getSiblingNo(j);
                if (siblingno < 0) {
                    continue;
                }

                HashMap map = null;
                if (j < treeHeight.size()) {
                    map = (HashMap) treeHeight.get(j);
                }

                if (map == null) {
                    map = new HashMap();
                    treeHeight.add(j, map);
                }

                Integer key = new Integer(siblingno);
                Integer val = (Integer) map.get(key);
                if (val == null) {
                    val = new Integer(1);
                } else {
                    /* val++ */
                    val = new Integer(val.intValue() + 1);
                }
                map.put(key, val);
            }
        }

        /*
         * 2. now that we have a vector (size = height of the deepest node we
         * sort the sibling numbers at each level by their frequency and obtain
         * a sequence of sibling numbers that has highest frequency at each
         * level.
         */
        Vector maxFreqSibNo = new Vector();
        Iterator level = treeHeight.iterator();

        while (level.hasNext()) {
            /* iterate thru each level of the tree */

            HashMap map = (HashMap) level.next();
            int maxValue = 0;
            Integer maxKey;

            /*
             * we want to sort all the sibling values at each level by frequency
             */
            TreeMap sortedByValue = new TreeMap(new Comparator() {
                Map map;

                public Comparator init(Map map) {
                    this.map = map;
                    return this;
                }

                public int compare(Object a, Object b) {
                    return ((Integer) (map.get(b))).compareTo((Integer) (map
                            .get(a)));
                }
            }.init(map));

            Iterator mapit = map.entrySet().iterator();

            while (mapit.hasNext()) {
                Entry ent = (Entry) mapit.next();
                sortedByValue.put(ent.getKey(), ent.getValue());
            }

            it = sortedByValue.entrySet().iterator();
            Integer maxFreq = (Integer) sortedByValue.firstKey();

            maxFreqSibNo.add(maxFreq);

            if (DEBUG) {
                while (it.hasNext()) {
                    Entry ent = (Entry) it.next();
                    System.err.print("\t" + ent.getKey() + "=>"
                            + ent.getValue());
                }
                System.err.println();
            }
        }

        return maxFreqSibNo;
    }

    /**
     * shows the list of best nodes for the given feature
     */
    private void processSelection() {
        String curSelected = getSelection(cachedUrls);

        if (curSelected != null) {
            IFeatureSet curSet = getFeatureStore().get(curSelected);
            if (curSet != null) {
                output.setText(curSelected + "\n\n" + curSet.toString());

                AbstractFeature[] items = curSet.getFeaturesSorted();

                if (items != null) {
                    featureList.setItems(Utilities.toStringArray(items));

                    Vector bestNodePath = this.runBestGuess(items);
                    traceListMap = buildPathTrace(bestNodePath, getBrowser());

                    int size = traceListMap.keySet().size();
                    String trace[] = new String[size];
                    System.arraycopy(traceListMap.keySet().toArray(), 0, trace,
                            0, size);

                    if (trace != null && trace.length > 0) {
                        traceList.setItems(trace);
                    }
                }
            }
        }
    }

    public FeatureView(Display display, Composite parent, int style, BrowserFrame browserFrame) {
        super(display, parent, style, browserFrame);
        this.setLayout(new GridLayout());
        /*
         * List of urls with fragstats
         */

        Group storeEntryGroup = new Group(this, SWT.NONE);
        storeEntryGroup.setText("URLs");
        storeEntryGroup.setLayout(new GridLayout());
        storeEntryGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        cachedUrls = new List(storeEntryGroup, SWT.MULTI | SWT.V_SCROLL
                | SWT.H_SCROLL | SWT.BORDER);
        cachedUrls
                .setToolTipText("click on a selection to see its fragment stats");
        cachedUrls.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.FILL_HORIZONTAL));

        cachedUrls.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                processSelection();
            }
        });

        Group featureGroup = new Group(this, SWT.NONE);
        featureGroup.setText("Features");
        featureGroup.setLayout(new GridLayout());
        featureGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        /*
         * list of features doing clustering on
         */
        this.featureCombo = new Combo(featureGroup, SWT.SINGLE | SWT.BORDER
                | SWT.DROP_DOWN | SWT.READ_ONLY);
        featureCombo.select(FeatureStore.DEFAULT_TYPE);
        featureCombo.setItems(FeatureStore.TYPE_NAMES);
        featureCombo.setLayoutData(new GridData());
        featureCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (getFeatureStore().getType() != featureCombo
                        .getSelectionIndex()) {
                    getFeatureStore().setType(featureCombo.getSelectionIndex());
                    processSelection();
                }
            }
        });

        /*
         * labels containing stats about the current page
         */
        Group featureListGroup = new Group(this, SWT.NONE);
        featureListGroup.setText("Feature List");
        featureListGroup.setLayout(new GridLayout());
        featureListGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        featureList = new List(featureListGroup, SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
        featureList.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
        featureList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String selection[] = featureList.getSelection();
                for (int i = 0; i < selection.length; i++) {
                    System.err.println("S:" + selection[i]);
                    if (selection[i] != null) {
                        String parts[] = selection[i]
                                .split(DefaultFeatureSet.KEY_VALUE_SEPARATOR);
                        if (parts != null & parts.length > 0) {
                            getHighlighter().highlightNodesByFeature(parts[0],
                                    Const.DEFAULT_BGCOLOR,
                                    Const.DEFAULT_FGCOLOR);
                        }
                    }
                }
            }
        });

        Group traceListGroup = new Group(this, SWT.NONE);
        traceListGroup.setText("Trace List");
        traceListGroup.setLayout(new GridLayout());
        traceListGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        traceList = new List(traceListGroup, SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.BORDER | SWT.READ_ONLY);
        traceList.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        traceList.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                String selection[] = traceList.getSelection();
                if (selection != null && selection.length > 0) {

                    INode node = (INode) traceListMap.get(selection[0]);

                    getHighlighter().clearHighlighted();
                    getHighlighter().highlightNodeByNodeID(node.getNodeID(), Const.DEFAULT_BGCOLOR, Const.DEFAULT_FGCOLOR);
                    
                }

            }
        });

        /*
         * labels containing stats about the current page
         */
        Group outputGroup = new Group(this, SWT.NONE);
        outputGroup.setText("Output");
        GridData outputGroupGD = new GridData(GridData.FILL_BOTH);
        outputGroupGD.grabExcessVerticalSpace = true;
        outputGroup.setLayoutData(outputGroupGD);
        outputGroup.setLayout(new GridLayout());
        this.output = new Text(outputGroup, SWT.V_SCROLL | SWT.H_SCROLL
                | SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
        output.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
        output.setEditable(false);
        output.setText("");
        output.setSize(80, 100);
    }

}