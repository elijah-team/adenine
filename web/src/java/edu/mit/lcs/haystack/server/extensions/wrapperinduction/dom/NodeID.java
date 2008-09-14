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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ArrayUtils;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;

/**
 * Represents the location of an INode (or a range of sibling INodes) within a
 * tree.
 * 
 * @version 1.0
 * @author Andrew Hogue
 */
public class NodeID {

    // the siblingsNos representing the path from the root to the
    // parent of the element
    protected int[] siblingNos;

    // The range of siblings of the actual nodes represented by this
    // NodeID at the lowest level.
    protected Range range;

    public NodeID(int[] siblingNos, Range range) throws NodeIDException {
        if (range == null || range.getSize() <= 0) {
            if (siblingNos.length == 0) {
                throw new NodeIDException("can't create empty NodeID");
            } else {
                this.siblingNos = new int[siblingNos.length - 1];
                for (int i = 0; i < this.siblingNos.length; i++) {
                    this.siblingNos[i] = siblingNos[i];
                }
                this.range = new Range(siblingNos[siblingNos.length - 1], siblingNos[siblingNos.length - 1]);
            }
        } else {
            this.siblingNos = siblingNos;
            this.range = range;
        }
    }

    /**
     * Creates a length-one NodeID
     */
    public NodeID(int siblingNo) {
        this.siblingNos = new int[0];
        this.range = new Range(siblingNo, siblingNo);
    }

    public NodeID(int[] siblingNos) throws NodeIDException {
        if (siblingNos.length == 0) {
            throw new NodeIDException("can't create empty NodeID");
        }

        this.siblingNos = new int[siblingNos.length - 1];
        for (int i = 0; i < this.siblingNos.length; i++) {
            this.siblingNos[i] = siblingNos[i];
        }
        this.range = new Range(siblingNos[siblingNos.length - 1], siblingNos[siblingNos.length - 1]);
    }

    /**
     * Returns the last element of this NodeID.
     */
    public Range getRange() {
        return this.range;
    }

    public int getLength() {
        return this.siblingNos.length + 1; // add one for the range
    }

    public int getSiblingNo(int index) {
        if (index < 0 || index >= this.siblingNos.length)
            return -1;
        return this.siblingNos[index];
    }

    /**
     * Creates a child node ID with this NodeID as the parent. If this NodeID
     * has a range (i.e. more than one element) at its lowest level, throws an
     * exception.
     */
    public NodeID makeChildNodeID(int siblingNo) throws NodeIDException {
        if (this.range.getSize() > 1)
            throw new NodeIDException("Cannot call makeChildNodeID on a NodeID representing a range!");
        return new NodeID(ArrayUtils.push(this.siblingNos, this.range.start), new Range(siblingNo, siblingNo));
    }

    /**
     * Creates a child NodeID with a range at its lowest level, with this NodeID
     * as the parent. If this NodeID has a range (i.e. more than one element) at
     * its lowest level, throws an exception.
     */
    public NodeID makeChildNodeID(Range range) throws NodeIDException {
        if (this.range.getSize() > 1)
            throw new NodeIDException("Cannot call makeChildNodeID on a NodeID representing a range!");
        return new NodeID(ArrayUtils.push(this.siblingNos, this.range.start), range);
    }

    /**
     * Creates a new NodeID with this NodeID as the child of the given node.
     */
    public NodeID makeParentNodeID(int siblingNo) throws NodeIDException {
        int[] returnInt = ArrayUtils.shift(this.siblingNos, siblingNo);
        return new NodeID(returnInt, this.range);
    }

    public int[] getSiblingNos() {
        return this.siblingNos;
    }

    public String toString() {
        return ArrayUtils.join(".", this.siblingNos) + ":" + this.range;
    }

    public static NodeID fromString(String nodeIDString) {
        NodeID id = null;
        String sibAndRange[] = nodeIDString.split(":");

        if (sibAndRange.length > 0) {
            /*
             * "." matches any character in regex so we need to escape it here
             */
            String arr[] = sibAndRange[0].split("\\.");

            int[] siblingNos = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                try {
                    siblingNos[i] = Integer.parseInt(arr[i]);
                } catch (NumberFormatException nfe) {
                    siblingNos[i] = 0;
                }
            }
            int range = 0;
            try {
                if (sibAndRange.length > 1) {
                    id = new NodeID(siblingNos, Range.fromString(sibAndRange[1]));
                } else {
                    id = new NodeID(siblingNos, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    /**
     * Retreives the INodes with this NodeID relative to the given root.
     */
    public INode[] getNodes(ITree tree) {
        return this.getNodes((INode) tree.getDocumentElement());
    }

    /**
     * Retreives the INodes with this NodeID relative to the given root.
     */
    public INode[] getNodes(INode root) {
        if (this.siblingNos.length == 0) {
            if (this.range.getSize() == 1 && this.range.start == root.getSiblingNo()) {
                return new INode[] { root };
            } else {
                return new INode[0];
            }
        } else {
            return this.getNodes(root, 1);
        }
    }

    protected INode[] getNodes(INode curr, int depth) {
        if (depth < this.siblingNos.length) {
            INode child = curr.getChild(this.siblingNos[depth]);
            if (child == null) {
                return new INode[0];
            } else {
                return this.getNodes(child, depth + 1);
            }
        } else {
            return this.range.getChildRange(curr);
        }
    }

    /**
     * Returns the number of nodes in this selection and its subtrees.
     */
    public int getNodeSize(ITree tree) {
        return getNodeSize((INode) tree.getDocumentElement());
    }

    /**
     * Returns the number of nodes in this selection and its subtrees.
     */
    public int getNodeSize(INode root) {
        INode[] nodes = this.getNodes(root);
        int size = 0;
        for (int i = 0; i < nodes.length; i++) {
            size += nodes[i].getSize();
        }
        return size;
    }

    /**
     * Returns the text of all nodes in the document matching this NodeID (most
     * useful if this NodeID matches a Range).
     */
    public String getText(IDOMDocument doc) {
        INode[] matchedNodes = this.getNodes(doc);
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < matchedNodes.length; i++) {
            out.append(((IDOMElement) matchedNodes[i]).getNodeText());
        }
        return out.toString();
    }

    /**
     * A NodeID is said to equal another if the sequences of siblingNos and the
     * ranges in the two objects are the same.
     */
    public boolean equals(Object o) {
        if (!(o instanceof NodeID))
            return false;

        int[] oSiblingNos = ((NodeID) o).siblingNos;
        if (this.siblingNos.length != oSiblingNos.length)
            return false;
        for (int i = 0; i < this.siblingNos.length; i++) {
            if (this.siblingNos[i] != oSiblingNos[i])
                return false;
        }

        return this.range.equals(((NodeID) o).range);
    }

    /**
     * Tests whether the entirety this NodeID matches just the non-range part of
     * the given NodeID
     */
    public boolean equalsIgnoreRange(NodeID o) {
        if (this.range.getSize() > 1)
            return false;
        if (this.siblingNos.length != o.siblingNos.length - 1)
            return false;
        for (int i = 0; i < this.siblingNos.length; i++) {
            if (this.siblingNos[i] != o.siblingNos[i])
                return false;
        }
        if (this.range.start != o.siblingNos[o.siblingNos.length - 1])
            return false;

        return true;
    }

    /**
     * Returns true if the given NodeID is "within" this one (that is, if the
     * nodes represented by the given NodeID are completely within the subtree
     * or range represented by this NodeID).
     */
    public boolean contains(NodeID other) {
        if (other == null) {
            System.out.println("NodeID.contains() got null NodeID");
            return false;
        }
        if (other.siblingNos.length < this.siblingNos.length)
            return false;
        for (int i = 0; i < this.siblingNos.length; i++)
            if (other.siblingNos[i] != this.siblingNos[i])
                return false;

        if (other.siblingNos.length == this.siblingNos.length)
            return this.range.contains(other.range);
        else
            return this.range.contains(other.siblingNos[this.siblingNos.length]);
    }

    /**
     * Given another NodeID that is contained within this one, creates a NodeID
     * relative to the node represented by this one. If this node does not
     * contain the given node, or if this node represents a range, returns null.
     */
    public NodeID makeRelativeID(NodeID containedID) throws NodeIDException {
        if (!this.contains(containedID)) {
            //	    System.out.println("makeRelativeID() returning null");
            return null;
        }
        // essentially, we just "chop off" the top part of containedID
        // that is identical to this ID, and change the root siblingNo
        // to 0
        int[] newSiblingNos = new int[containedID.siblingNos.length - this.siblingNos.length];
        Range newRange = new Range(0, 0);
        if (newSiblingNos.length > 0) {
            newSiblingNos[0] = 0;
            for (int i = 1; i < newSiblingNos.length; i++) {
                newSiblingNos[i] = containedID.siblingNos[i + this.siblingNos.length];
            }
            newRange = (Range) containedID.range.clone();
        } else {
            newRange = this.range.makeRelativeRange(containedID.range);
        }
        return new NodeID(newSiblingNos, newRange);
    }

    public int hashCode() {
        return this.toString().hashCode();
    }

    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
        Resource nodeIDRes = Utilities.generateUniqueResource();
        rdfc.add(new Statement(nodeIDRes, Constants.s_rdf_type, WrapperManager.NODE_ID_CLASS));
        rdfc.add(new Statement(nodeIDRes, WrapperManager.NODE_ID_SIBLING_NOS_PROP, new Literal(ArrayUtils.join(".", this.siblingNos))));
        rdfc.add(new Statement(nodeIDRes, WrapperManager.NODE_ID_RANGE_PROP, this.range.makeResource(rdfc)));
        return nodeIDRes;
    }

    public static NodeID fromResource(Resource nodeIDRes, IRDFContainer rdfc) throws RDFException {
        try {
            return new NodeID(ArrayUtils.split(".", rdfc.extract(nodeIDRes, WrapperManager.NODE_ID_SIBLING_NOS_PROP, null).getContent()), Range.fromResource((Resource) rdfc.extract(nodeIDRes, WrapperManager.NODE_ID_RANGE_PROP, null), rdfc));
        } catch (NodeIDException e) {
            throw new RDFException("Error in NodeID.fromResource()", e);
        }
    }

}