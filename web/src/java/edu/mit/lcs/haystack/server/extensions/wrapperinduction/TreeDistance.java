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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * Represents the edit distance between two trees, where the edit operations are
 * required to preserve parent-child operations and the depth of nodes in the
 * tree.
 * 
 * @author Andrew Hogue
 */
public class TreeDistance implements Runnable {

    protected INode root1;

    protected INode root2;

    protected IProgressMonitor progressMonitor;

    protected double cutoffPercentage;

    protected Mapping mapping;

    protected int helperCalls = 0;

    protected int helperExecutes = 0;

    public TreeDistance(INode root1, INode root2, double cutoffPercentage) {
        this.root1 = root1;
        this.root2 = root2;
        this.cutoffPercentage = cutoffPercentage;
    }

    public TreeDistance(INode root1, INode root2, double cutoffPercentage,
            IProgressMonitor progress) {
        this.root1 = root1;
        this.root2 = root2;
        this.cutoffPercentage = cutoffPercentage;
        this.progressMonitor = progress;
    }

    public void run() {
        // 	this.mapping = this.getMapping();
    }

    public Mapping getMapping() {
        //	System.out.println("Total size: " + root1.getSize() + " + " +
        // root2.getSize());
        if (progressMonitor != null) {
            String nodeName = root1.getNodeName();
            progressMonitor.beginTask((nodeName==null)?"getMapping":nodeName, 
                    (int) (cutoffPercentage * root1.getSize()* root2.getSize() / 5));
            //	    System.out.println("set max to " +
            // (int)(cutoffPercentage*root1.getSize()*root2.getSize()/5));
        }
        this.mapping = subtreeDistanceHelper(root1, root2);
        if (progressMonitor != null) {
            progressMonitor.done();
        }
        // System.out.println("root1: " + root1.getSize() + "\nroot2: " +
        // root2.getSize() + "\ncalls: " + helperCalls + "\nexecutes: " +
        // helperExecutes);
        return this.mapping;
    }

    protected Mapping subtreeDistanceHelper(INode node1, INode node2) {
        this.helperCalls++;
        if (progressMonitor != null) {
            progressMonitor.worked(this.helperCalls);
        }
        // 	if (node1.toString().equals("A") && node2.toString().equals("*")) {
        // 	    System.out.println("node1.equals(node2) = " + node1.equals(node2));
        // 	    System.out.println("node1.isWildcard() = " +
        // ((PatternNode)node1).isWildcard() + ", node2.isWildcard() = " +
        // ((PatternNode)node2).isWildcard());
        // 	}

        // handle base case, comparing node1 to node2
        if (!node1.equals(node2)) {
            Mapping mapping = new Mapping();
            mapping.add(node1, node2, node1.getChangeCost(node2));
            // 	    if (node1.toString().equals("A") && node2.toString().equals("*"))
            // 		System.out.println("adding A->* mapping of cost " +
            // mapping.getCost());
            return mapping;
        }

        this.helperExecutes++;

        NodeList children1 = node1.getChildNodes();
        NodeList children2 = node2.getChildNodes();

        // if no children, return a quick mapping
        if (children1.getLength() == 0) {
            if (children2.getLength() == 0) {
                Mapping mapping = new Mapping();
                mapping.add(node1, node2, 0);
                return mapping;
            } else {
                Mapping mapping = new Mapping();
                for (int i = 0; i < children2.getLength(); i++) {
                    mapping.add(null, (INode) children2.item(i),
                            ((INode) children2.item(i)).getInsertCost());
                }
                return mapping;
            }
        } else if (children2.getLength() == 0) {
            Mapping mapping = new Mapping();
            for (int i = 0; i < children1.getLength(); i++) {
                mapping.add((INode) children1.item(i), null, ((INode) children1
                        .item(i)).getDeleteCost());
            }
            return mapping;
        }

        /* neither node's children is empty */
        int m = children1.getLength();
        int n = children2.getLength();
        Mapping[][] c = new Mapping[m + 1][n + 1];
        //	boolean[][] b = new boolean[m+1][n+1];

        Mapping currMapping = null;
        int cost1, cost2, cost3;

        c[0][0] = new Mapping();

        // first do delete/insert costs
        
        /*
         * cost if tree2 has no children(i)
         * i.e. cost to delete from tree1, children(i)
         */
        for (int i = 1; i <= m; i++) {
            c[i][0] = c[i - 1][0].cloneMapping();
            c[i][0].add((INode) children1.item(i - 1), null, ((INode) children1
                    .item(i - 1)).getDeleteCost());
            //	    b[i][0] = true;
        }
        
        /*
         * cost if tree1 has no children i
         * i.e. cost to insert into tree1 children(i) of tree2
         */
        for (int j = 1; j <= n; j++) {
            c[0][j] = c[0][j - 1].cloneMapping();
            c[0][j].add(null, (INode) children2.item(j - 1), ((INode) children2
                    .item(j - 1)).getInsertCost());
            //	    b[0][j] = true;
        }

        int prevMax = 1;
        int height = c.length;
        int width = c[0].length;

        // now fill in the rest
        // m is the tree1's number of children
        // n is the tree2's number of children
        // width = n + 1
        // height = m + 1
        // i == tree1
        // j == tree2
        for (int i = 1; i <= m; i++) {
            int min, max;
            if (m == n) {
                /* mid == i */
                int mid = (int) (Math.floor(width / height) * i);
                
                min = mid - (int) Math.ceil((int) (cutoffPercentage * .5 * width));
                min = (min <= 0) ? 1 : min;
                min = (min <= prevMax) ? min : prevMax;
                max = mid + (int) Math.ceil((int) (cutoffPercentage * .5 * width))+ 1;
                max = (max >= width) ? width - 1 : max;
                prevMax = max;
            } else {
                /* if the number of children of the two trees are different
                 * then compare each possible paring of children
                 */
                min = 1;
                max = n;
            }

            for (int j = min; j <= max; j++) {
                /*
                 * recursion: c[i,j] = min{  c[i,j-1]+cost(x_i->null),
                 *                           c[i-1,j]+cost(null->y_j), 
                 *                           c[i-1,j-1]+cost(x_i -> y_j)}
                 */

                //  		boolean DEBUG =
                // (((INode)children1.item(i-1)).toString().equals("A") &&
                // 				 ((INode)children2.item(j-1)).toString().equals("*") &&
                // 				 ((INode)children2.item(j-1)).getSiblingNo() == 12);
                currMapping = subtreeDistanceHelper((INode) children1.item(i - 1), 
                                                    (INode) children2.item(j - 1));

                /* cost 1 */
                if (c[i - 1][j] != null) {
                    cost1 =  c[i - 1][j].getCost() + ((INode) children1.item(i - 1)).getDeleteCost();
                } else {
                    cost1 = Integer.MAX_VALUE;
                }
                /* cost 2 */
                if (c[i][j - 1] != null) {
                    cost2 = c[i][j - 1].getCost() + ((INode) children2.item(j - 1)).getInsertCost();
                } else {
                    cost2 = Integer.MAX_VALUE;
                }
                
                if (c[i - 1][j - 1] != null) {
                    cost3 = c[i - 1][j - 1].getCost() + currMapping.getCost();
                } else {
                    cost3 = Integer.MAX_VALUE;
                }

                //  		if (DEBUG) System.out.println("cost1=" + cost1 + ", cost2=" +
                // cost2 + ", cost3=" + cost3 + "\n" + currMapping);

                if (cost1 < cost2 && cost1 < cost3) {
                    /* use cost 1*/
                    c[i][j] = c[i - 1][j].cloneMapping();
                    c[i][j].add((INode) children1.item(i - 1), null,
                            ((INode) children1.item(i - 1)).getDeleteCost());
                } else if (cost2 < cost3) {
                    /* use cost 2 */
                    c[i][j] = c[i][j - 1].cloneMapping();
                    c[i][j].add(null, (INode) children2.item(j - 1),
                            ((INode) children2.item(j - 1)).getInsertCost());
                } else {
                    /* use cost 3 */
                    c[i][j] = Mapping.merge(c[i - 1][j - 1], currMapping);
                }

                //		b[i][j] = true;
            }
        }

        //	printBools(b);
        c[m][n].add(node1, node2, 0);
        return c[m][n];
    }

    protected void printBools(boolean[][] b) {
        System.out.println("=========");
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[i].length; j++) {
                if (b[i][j])
                    System.out.print("X ");
                else
                    System.out.print("0 ");
            }
            System.out.println();
        }
        System.out.println("=========");
    }

    /**
     * Adds deletions to the given Mapping for all children of the given INode.
     * (Assumes the given INode has already been "taken care of" in the Mapping)
     */
    protected void addRecursiveDeletions(INode n, Mapping mapping) {
        NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            mapping.add((INode) children.item(i), null, ((INode) children
                    .item(i)).getDeleteCost());
            addRecursiveDeletions((INode) children.item(i), mapping);
        }
    }

    /**
     * Adds insertions to the given Mapping for all children of the given INode.
     * (Assumes the given INode has already been "taken care of" in the Mapping)
     */
    protected void addRecursiveInsertions(INode n, Mapping mapping) {
        NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            mapping.add(null, (INode) children.item(i), ((INode) children
                    .item(i)).getInsertCost());
            addRecursiveInsertions((INode) children.item(i), mapping);
        }
    }

}