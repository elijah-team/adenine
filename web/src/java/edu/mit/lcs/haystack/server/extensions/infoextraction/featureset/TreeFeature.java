package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 * 
 * representation of feature derived from the DOM tree.
 */
public class TreeFeature extends DefaultFeature {
    
    Vector pathComponents;
    Vector leafComponents;

    final static String pathSeparator = "/";

    final static String leafSeparator = ".";

    public TreeFeature(String strFeature) {
        this();
        if (strFeature != null) {
            int index = 0;
            String pathComps[] = strFeature.split(pathSeparator);
            if (pathComps.length > 0) {
                int i;
                if (pathComps[0].equals("")) {
                    i = 1;
                } else {
                    i = 0;
                }
                for (i = 1; i < pathComps.length - 1; i++) {
                    this.addPathComponent(pathComps[i]);
                }
                if (pathComps[pathComps.length - 1] != null) {
                    String comp = pathComps[pathComps.length - 1];
                    String leafComps[] = comp.split("\\"+leafSeparator);
                    if (leafComps != null && leafComps.length > 0) {
                        this.addPathComponent(leafComps[0]);
                        for (int j = 1; j < leafComps.length; j++) {
                            this.addLeafComponent(leafComps[j]);
                        }
                    }
                }
            }
        }
    }
    
    public TreeFeature() {
        pathComponents = new Vector();
        leafComponents = new Vector();
    }

    public TreeFeature(TreeFeature feature, boolean copyLeaf) {
        pathComponents = (Vector) feature.pathComponents.clone();
        if (copyLeaf) {
            leafComponents = (Vector) feature.leafComponents.clone();
        } else {
            leafComponents = new Vector();
        }
    }

    public void addPathComponent(String string) {
        pathComponents.add(string);
    }

    public void addLeafComponent(String string) {
        leafComponents.add(string);
    }

    public Vector/* INode */getNodes(INode node) {
        return getNodes(new INode[] { node }, 1);
    }

    
    /**
     * recursively returns the node which matches this particular 
     * tree feature,
     * @param root
     * @return a vector containing matching INodes
     */
    protected Vector/* INode */getNodes(INode[] nodes, int depth) {
        Vector matches = new Vector();
        
        if (depth < pathComponents.size()) {
            for (int i = 0; i < nodes.length; i++) {
                INode curNode = nodes[i];
                String compName = (String) pathComponents.get(depth);
                INode[] children = curNode.getChildren(compName);
                if (children != null && children.length > 0) {
                    Vector results = getNodes(children, depth + 1);
                    if (results.size() > 0) {
                        matches.addAll(results);
                    }
                }
            }

        } else {
            /* handle leaf matching */
            for (int i = 0; i < nodes.length; i++) {
                INode curNode = nodes[i];
        
                boolean isAMatch = true;
                for (int j = 0; j < leafComponents.size(); j++) {
                    String compName = (String) leafComponents.elementAt(j);

                    INode[] children = curNode.getChildren(compName);
                    if (children == null || children.length == 0) {
                        isAMatch = false;
                        break;
                    }
                }
                //only return the match if all leafs match
                if (isAMatch) {
                    matches.add(curNode);
                }
            }
        }

        return matches;
    }

    static public TreeFeature fromString(String strFeature) {
        return new TreeFeature(strFeature);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < pathComponents.size(); i++) {
            buf.append(pathSeparator + pathComponents.elementAt(i));
        }
        for (int i = 0; i < leafComponents.size(); i++) {
            buf.append(leafSeparator + leafComponents.elementAt(i));
        }
        return buf.toString();
    }
}