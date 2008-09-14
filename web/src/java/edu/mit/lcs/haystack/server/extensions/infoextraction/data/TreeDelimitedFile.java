/*
 * Created on Jan 14, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.AugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class TreeDelimitedFile {

    IRelatablePoint root;

    private TreeDelimitedFile(IRelatablePoint root) {
        this.root = root;
    }

    public TreeDelimitedFile(String file) {
        try {
            readFromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IRelatablePoint getContent() {
        return root;
    }

    public void reset() {
        root = null;
    }

    private String toString(int indent, IRelatablePoint node) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent; i++)
            sb.append("\t");
        sb.append(((INode) node.getData()).getNodeName());
        sb.append("\n");
        Vector children = node.getChildren();

        if (!children.isEmpty()) {
            for (int j = 0; j < children.size(); j++) {
                IRelatablePoint child = (IRelatablePoint) children.get(j);
                sb.append(toString(indent + 1, child));
            }
        }
        return sb.toString();
    }

    public String toString() {
        if (root != null) {
            return toString(0, root);
        } else {
            return "";
        }
    }

    public void toFile(String filename) throws FileNotFoundException, IOException {
        File file = new File(filename);
        FileOutputStream fout = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout));
        bw.write(toString());
        bw.close();
        fout.close();
    }

    public IRelatablePoint readFromFile(String filename) throws FileNotFoundException, IOException {
        File file = new File(filename);
        FileInputStream fin = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fin));

        IRelatablePoint curRoot = null;
        int numTabs = 0;
        String line;
        ArrayList /* IRelatablePoint */stack = new ArrayList();
        while (null != (line = br.readLine())) {
            /* skip over blank lines. */
            if (line.length() == 0) {
                continue;
            }
            /* skip over comments */
            if (line.startsWith("#")) {
                continue;
            }

            String tabs[] = line.split("\t");
            int curDepth = tabs.length - 1;
            String data = line.trim();
            INode node = new AugmentedNode(INode.ELEMENT_NODE, data);
            IRelatablePoint p = new TreeAlignmentPoint(node);
            if (stack.size() > curDepth) {
                stack.set(curDepth, p);
            } else {
                stack.add(p);
            }

            //System.err.println(p.getUniqueID());

            if (curDepth > 0) {
                curRoot = (IRelatablePoint) stack.get(curDepth - 1);
            } else {
                curRoot = null;
            }

            if (curRoot == null) {
                root = p;
            } else {
                curRoot.addChild(p);
                INode pnode = (INode) curRoot.getData();
                INode cnode = (INode) p.getData();
                pnode.appendChild(cnode);
            }
        }
        return root;
    }

    final private static String NODENAME_PREFIX = "N";

    final private static int DEFAULT_BRANCHING_FACTOR = 10;

    private static int NODEID = 0;

    final private static int NUM_NODES = 1000;

    final private static int MAX_DEPTH = 10;

    private static int randomNumChildren(int branchingFactor) {
        return (int) Math.ceil(Math.random() * (double) branchingFactor);
    }

    private static int branchingFactor(int depth) {
        if (depth > 0 && depth < MAX_DEPTH) {
            return (int) Math.floor((double) DEFAULT_BRANCHING_FACTOR / (double) Math.sqrt(depth + 1));
        } else if (depth >= MAX_DEPTH) {
            return 0;
        } else {
            return DEFAULT_BRANCHING_FACTOR;
        }
    }

    private static IRelatablePoint randomTree(int depth) {
        if (NODEID < NUM_NODES) {
            String nodeName = NODENAME_PREFIX + NODEID;
            INode node = new AugmentedNode(INode.ELEMENT_NODE, nodeName);
            IRelatablePoint p = new TreeAlignmentPoint(node);
            NODEID++;

            if (NODEID % 100 == 0) {
                System.err.println("generated: " + NODEID + " nodes");
            }
            int numChildren = randomNumChildren(branchingFactor(depth));
            for (int i = 0; i < numChildren; i++) {
                IRelatablePoint c = randomTree(depth + 1);
                if (c != null) {
                    p.addChild(c);
                }
            }
            return p;
        }
        return null;
    }

    public static TreeDelimitedFile generateRandomTree() {
        IRelatablePoint randTree = randomTree(0);
        TreeDelimitedFile tdf = new TreeDelimitedFile(randTree);
        return tdf;
    }

    public static void main(String[] args) {
        TreeDelimitedFile tdf = generateRandomTree();
        String filename = "random.tree";
        try {
            tdf.toFile(filename);
        } catch (Exception e) {
            System.err.println("File output failed: " + filename);
            e.printStackTrace();
        }
    }
}