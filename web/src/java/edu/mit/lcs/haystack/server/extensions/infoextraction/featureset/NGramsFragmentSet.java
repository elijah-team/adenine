package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.FixedSizeQueue;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;

/**
 * @author yks
 */
public class NGramsFragmentSet extends DefaultFeatureSet {
    public static final String NAME = "N-Gram";

    FixedSizeQueue window = new FixedSizeQueue(3);

    public NGramsFragmentSet(Node root) {
        super(root);
    }

    public void addFeatures() {
        try {
            Utilities.debug(this, "addFeatures() - START");
            window.clear();
            addFragments(root);
            Utilities.debug(this, "addFeatures() - DONE");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test() {
        try {
            test(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test(Node node) throws Exception {
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        //System.err.println("test(" + node.getNodeName() + ").getChildNodes().getLength():"
        //        + len);
        //System.err.flush();
        
        if (len == 0) {
            return;
        }

        int cur = 0;
        while (cur < len) {
            cur = filterNodes(children, cur);
            if (cur >= 0) {
                IAugmentedNode child = (IAugmentedNode) children.item(cur);
                AbstractFeature[] afs = child.getFeatures(10);
                
                if (afs != null) {
                    Utilities.printArray(System.err, afs);                    System.err.println("}");
                }
                test((Node) child);
            } else {
                break;
            }
            cur++;
        }

    }

    private void addFragments(Node node) {
        NodeList children = node.getChildNodes();
        int len = children.getLength();

        int cur = 0;
        while (cur < len) {
            cur = filterNodes(children, cur);
            if (cur >= 0) {
                Node child = (Node) children.item(cur);
                window.add(child, true);
                //System.err.println("adding: " + child.nodeName());
                window.feedConsumers();
                addFragments(child);
                window.add(child, false);
                window.feedConsumers();
            } else {
                break;
            }
            cur++;
        }
    }
    
    public String getFeatureName() {
        return NAME;
    }
}