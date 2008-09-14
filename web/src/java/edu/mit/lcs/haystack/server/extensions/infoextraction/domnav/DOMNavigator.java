/*
 * Created on Nov 26, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.domnav;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 * @author yks
 */
public class DOMNavigator {
    protected NodeID curNodeID = null;
    
    protected IDOMNavHandler domNavHandler;

    public DOMNavigator(IDOMNavHandler handler) {
        this.domNavHandler = handler;
    }
    
    public NodeID rootNodeID(INode curDOM) {
        NodeID curNodeID = null;
        try {
            curNodeID = curDOM.getNodeID();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return curNodeID;
    }

    public void setCurNodeID( NodeID id ) {
        this.curNodeID = id;
    }
    
    public NodeID getCurNodeID() {
        return this.curNodeID;
    }
    
    public void prevSibling() {
        INode curTarget;
        INode curDOM = this.domNavHandler.getDOM();
        if (curDOM == null) {
            return;
        }

        if (curNodeID == null) {
            curNodeID = rootNodeID(curDOM);
        }

        INode[] curTargets = curNodeID.getNodes(curDOM);
        if (curTargets != null && curTargets.length > 0) {
            curTarget = curTargets[0];

            INode parent = (INode) curTarget.getParentNode();
            int curNo;
            if (parent != null) {
                curNo = curTarget.getSiblingNo();
                if (curNo > 0) {
                    INode prevSibling = parent.getChild(curNo - 1);
                    if (prevSibling != null) {
                        domNavHandler.runAction(prevSibling);
                    }
                }
            }

        } else {
            /* reset node id, next time will reinit node id */
            curNodeID = null;
        }
    }

    public void nextSibling() {
        INode curTarget;
        INode curDOM = this.domNavHandler.getDOM();
        
        if (curDOM == null) {
            return;
        }

        if (curNodeID == null) {
            curNodeID = rootNodeID(curDOM);
        }

        INode[] curTargets = curNodeID.getNodes(curDOM);
        if (curTargets != null && curTargets.length > 0) {
            curTarget = curTargets[0];

            INode parent = (INode) curTarget.getParentNode();
            int curNo;
            if (parent != null) {
                curNo = curTarget.getSiblingNo();
                if (curNo >= 0) {
                    INode nextSibling = parent.getChild(curNo + 1);
                    if (nextSibling != null) {
                        this.domNavHandler.runAction(nextSibling);
                    }
                }
            }

        } else {
            /* reset node id, next time will reinit node id */
            curNodeID = null;
        }

    }

    public void moveUpTree() {
        INode curTarget;
        INode curDOM = this.domNavHandler.getDOM();
        
        if (curDOM == null) {
            return;
        }

        if (curNodeID == null) {
            curNodeID = rootNodeID(curDOM);
        }

        INode[] curTargets = curNodeID.getNodes(curDOM);
        if (curTargets != null && curTargets.length > 0) {
            curTarget = curTargets[0];

            INode parent = (INode) curTarget.getParentNode();
            int curNo;
            if (parent != null) {
                this.domNavHandler.runAction(parent);
            }

        } else {
            /* reset node id, next time will reinit node id */
            curNodeID = null;
        }

    }

    public void moveDownTree() {

        INode curTarget;
        INode curDOM = this.domNavHandler.getDOM();
        if (curDOM == null) {
            return;
        }

        if (curNodeID == null) {
            curNodeID = rootNodeID(curDOM);
        }

        INode[] curTargets = curNodeID.getNodes(curDOM);
        if (curTargets != null && curTargets.length > 0) {
            System.err.println("got targets");
            curTarget = curTargets[0];

            NodeList children = curTarget.getChildNodes();
            if (children != null && children.getLength() > 0) {
                System.err.println("got children");
                INode child = (INode) children.item(0);
                if (child != null) {
                    System.err.println("got child:" + child.getNodeName());
                    this.domNavHandler.runAction(child);
                }
            }

        } else {
            System.err.println("Nada");
            /* reset node id, next time will reinit node id */
            curNodeID = null;
        }
    }

}
