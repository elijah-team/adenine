package edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.NGramsFragmentSet;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.NodeComparator;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.test.DOMNodeComparator;

/**
 * @author yks
 */
public class AugmentedNode implements INode, IAugmentedNode,
        org.w3c.dom.Element {
    
    protected int index;
    
    protected ICluster cluster;

    protected Vector children;

    protected LinkedHashMap attributes;

    protected Node parent;

    protected String value;

    protected short nodeType;

    protected String nodeName;

    protected String tagName;

    protected IFeatureSet featureSet = null;

    protected NodeID nodeID;

    protected int siblingNo;

    protected int size;
    
    public AugmentedNode(short nodeType, String nodeName) {
        children = new Vector();
        attributes = new LinkedHashMap();
        parent = null;
        value = null;
        setNodeType(nodeType);
        setNodeName(nodeName);
        setTagName(nodeName);
    }

    public AugmentedNode(short nodeType, String nodeName, String value) {
        this(nodeType, nodeName);
        this.setNodeValue(value);
    }

    public void setNodeType(short nodeType) {
        this.nodeType = nodeType;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#addFeature(java.lang.Object)
     */

    public void addFeature(AbstractFeature feature) {
        if (featureSet == null) {
            featureSet = new NGramsFragmentSet(this);
        }

        featureSet.addFeature(feature);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#getFeatures(int
     *      n)
     */
    public AbstractFeature[] getFeatures(int n) {
        if (featureSet != null) {
            return featureSet.getFeatures(n);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#getFeatures()
     */
    public AbstractFeature[] getFeatures() {
        if (featureSet != null) {
            return featureSet.getFeatures();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#getFeatureSet()
     */
    public IFeatureSet getFeatureSet() {
        if (featureSet != null) {
            return featureSet;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IAttributedNode#nodeName()
     */
    public String nodeName() {
        switch (this.getNodeType()) {
        case INode.TEXT_NODE:
            return "#TEXT#";
        case INode.COMMENT_NODE:
            return "#COMMENT#";
        default:
            return this.getNodeName();
        }
    }

    public String getLabel() {
        return nodeName();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getTagName()
     */
    public String getTagName() {
        return nodeName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getAttribute(java.lang.String)
     */
    public String getAttribute(String attribName) {
        Attr attribNode = getAttributeNode(attribName);
        if (attribNode == null) {
            return null;
        } else {
            return attribNode.getValue();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#setAttribute(java.lang.String, java.lang.String)
     */
    public void setAttribute(String name, String value) throws DOMException {
        Attr attribNode = getAttributeNode(name);
        if (attribNode == null) {
            this.attributes.put(name, new AugmentedNode(Node.ATTRIBUTE_NODE, name, value));            
        } else {
            attribNode.setValue(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String attrib) throws DOMException {
        this.attributes.remove(attrib);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getAttributeNode(java.lang.String)
     */
    public Attr getAttributeNode(String attribName) {
        Node attribNode = (Node)this.attributes.get(attribName);
        if (attribNode == null) {
            return null;
        } else {
            return (Attr)attribNode;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#setAttributeNode(org.w3c.dom.Attr)
     */
    public Attr setAttributeNode(Attr attr) throws DOMException {
        this.attributes.put(attr.getName(), attr.getValue());
        return attr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#removeAttributeNode(org.w3c.dom.Attr)
     */
    public Attr removeAttributeNode(Attr attrib) throws DOMException {
        this.attributes.remove(attrib.getName());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getElementsByTagName(java.lang.String)
     */
    public NodeList getElementsByTagName(String tagName) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getAttributeNS(java.lang.String,
     *      java.lang.String)
     */
    public String getAttributeNS(String arg0, String arg1) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#setAttributeNS(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void setAttributeNS(String arg0, String arg1, String arg2)
            throws DOMException { }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#removeAttributeNS(java.lang.String,
     *      java.lang.String)
     */
    public void removeAttributeNS(String arg0, String arg1) throws DOMException { }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getAttributeNodeNS(java.lang.String,
     *      java.lang.String)
     */
    public Attr getAttributeNodeNS(String arg0, String arg1) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#setAttributeNodeNS(org.w3c.dom.Attr)
     */
    public Attr setAttributeNodeNS(Attr arg0) throws DOMException {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#getElementsByTagNameNS(java.lang.String,
     *      java.lang.String)
     */
    public NodeList getElementsByTagNameNS(String arg0, String arg1) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#hasAttribute(java.lang.String)
     */
    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Element#hasAttributeNS(java.lang.String,
     *      java.lang.String)
     */
    public boolean hasAttributeNS(String arg0, String arg1) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getNodeName()
     */
    public String getNodeName() {
        return nodeName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getNodeValue()
     */
    public String getNodeValue() throws DOMException {
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#setNodeValue(java.lang.String)
     */
    public void setNodeValue(String value) throws DOMException {
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getNodeType()
     */
    public short getNodeType() {
        return nodeType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getParentNode()
     */
    public Node getParentNode() {
        return parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getChildNodes()
     */
    public NodeList getChildNodes() {
        return new AugmentedNodeList(children);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getFirstChild()
     */
    public Node getFirstChild() {
        if (children.size() > 0) {
            return (Node) children.firstElement();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getLastChild()
     */
    public Node getLastChild() {
        if (children.size() > 0) {
            return (Node) children.lastElement();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getPreviousSibling()
     */
    public Node getPreviousSibling() {
        if (this.getParentNode() == null) {
            return null;
        } else {
            Node parent = this.getParentNode();

            NodeList siblings = parent.getChildNodes();
            int len = siblings.getLength();
            for (int i = 0; i < len; i++) {
                Node item = siblings.item(i);
                if (item == this) {
                    if (i > 0) {
                        return siblings.item(i - 1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getNextSibling()
     */
    public Node getNextSibling() {
        if (this.getParentNode() == null) {
            return null;
        } else {
            Node parent = this.getParentNode();

            NodeList siblings = parent.getChildNodes();
            int len = siblings.getLength();
            for (int i = 0; i < len; i++) {
                Node item = siblings.item(i);
                if (item == this) {
                    if (i < len - 1) {
                        return siblings.item(i + 1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getAttributes()
     */
    public NamedNodeMap getAttributes() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getOwnerDocument()
     */
    public Document getOwnerDocument() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#insertBefore(org.w3c.dom.Node, org.w3c.dom.Node)
     */
    public Node insertBefore(Node node, Node marker) throws DOMException {
        int index = children.indexOf(marker);
        if (index >= 0) {
            children.insertElementAt(node, index);
            return node;
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#replaceChild(org.w3c.dom.Node, org.w3c.dom.Node)
     */
    public Node replaceChild(Node killer, Node replacement) throws DOMException {
        int index = children.indexOf(killer);
        if (index >= 0) {
            children.remove(index);
            children.insertElementAt(replacement, index);
            return replacement;
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#removeChild(org.w3c.dom.Node)
     */
    public Node removeChild(Node child) throws DOMException {
        if (children.remove(child)) {
            return child;
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
     */
    public Node appendChild(Node child) throws DOMException {
        children.add(child);
        return child;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#hasChildNodes()
     */
    public boolean hasChildNodes() {
        if (children.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#cloneNode(boolean)
     */
    public Node cloneNode(boolean recursive) {
        AugmentedNode clone = new AugmentedNode(this.nodeType, this.nodeName);
        clone.setNodeValue(this.getNodeValue());
        clone.setTagName(this.getTagName());

        if (recursive) {
            int len = this.children.size();
            for (int i = 0; i < len; i++) {
                Node child = (Node) children.get(i);
                clone.appendChild(child.cloneNode(recursive));
                clone.setParent(clone);
            }
        }

        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#normalize()
     */
    public void normalize() { }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#isSupported(java.lang.String, java.lang.String)
     */
    public boolean isSupported(String arg0, String arg1) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getNamespaceURI()
     */
    public String getNamespaceURI() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getPrefix()
     */
    public String getPrefix() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#setPrefix(java.lang.String)
     */
    public void setPrefix(String arg0) throws DOMException { }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#getLocalName()
     */
    public String getLocalName() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.Node#hasAttributes()
     */
    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getNodeID()
     */
    public NodeID getNodeID() {
        if (this.nodeID == null) {
            try {
                this.nodeID = new NodeID(this.getSiblingNo());
                // fill in the path to root
                INode anc = this.getAncestor(1);
                while (anc != null && !anc.toString().equals("#document")) {
                    this.nodeID = this.nodeID.makeParentNodeID(anc
                            .getSiblingNo());
                    anc = anc.getAncestor(1);
                }
            } catch (NodeIDException e) {
                e.printStackTrace();
            }
        }

        return this.nodeID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getSize()
     */
    /**
     * returns the number of nodes under this node
     */
    public int getSize() {
        if (this.size == 0) {
            NodeList children = this.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                this.size += ((INode) children.item(i)).getSize();
            }
            this.size += 1; // this node
        }
        return this.size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getHeight()
     */
    /**
     * returns the maximum height of the subtree rooted at this node
     */
    public int getHeight() {
        int maxHeight = 0;
        NodeList childNodes = this.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            int currHeight = ((INode) childNodes.item(i)).getHeight();
            if (currHeight > maxHeight)
                maxHeight = currHeight;
        }
        return maxHeight + 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPostorderNodes()
     */
    public INode[] getPostorderNodes() {
        ArrayList postorder = new ArrayList();
        postorder.add(null); // to make array 1-based
        getPostorderNodesHelper(postorder);
        return (INode[]) postorder.toArray(new INode[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPostorderNodesHelper(java.util.List)
     */
    public void getPostorderNodesHelper(List postorder) {
        NodeList childNodes = getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            ((IEDOMElement) childNodes.item(i))
                    .getPostorderNodesHelper(postorder);
        }
        postorder.add(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPreorderNodes()
     */
    public INode[] getPreorderNodes() {
        ArrayList postorder = new ArrayList();
        postorder.add(null); // to make array 1-based
        getPostorderNodesHelper(postorder);
        return (INode[]) postorder.toArray(new INode[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getPreorderNodesHelper(java.util.List)
     */
    public void getPreorderNodesHelper(List preorder) {
            NodeList childNodes = getChildNodes();
            preorder.add(this);
            for (int i = 0; i < childNodes.getLength(); i++) {
                ((IEDOMElement) childNodes.item(i))
                        .getPreorderNodesHelper(preorder);
            }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getSiblingNo()
     */
    public int getSiblingNo() {
        if (this.getParentNode() == null) {
            return 0;
        }

        Node parent = this.getParentNode();
        NodeList siblings = parent.getChildNodes();
        
        for (int i = 0; i < siblings.getLength(); i++) {
            Node item = siblings.item(i);
            
            if (item == (Node)(this) || item.equals(this)) {
                siblingNo = i;
                return i;
            }
        }
        
        Utilities.debug(this,"getSiblingNo(): node is not a proper child of its parent.");

        return siblingNo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#setSiblingNo(int)
     */
    public void setSiblingNo(int siblingNo) {
        this.siblingNo = siblingNo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#isOnlyChild()
     */
    public boolean isOnlyChild() {
        if (parent != null && parent.getChildNodes().getLength() == 1) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getChild(int)
     */
    public INode getChild(int index) {
        if (index >= 0 && index < children.size()) {
            return (INode) children.get(index);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getChildren(java.lang.String)
     */
    public INode[] getChildren(String tagName) {
        Vector res = new Vector();
        for (int i = 0; i < children.size(); i++) {
            Node child = (Node) children.get(i);
            if (child.getNodeName().equals(tagName)) {
                res.add(child);
            }
        }
        return (INode[]) res.toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getAncestor(int)
     */
    public INode getAncestor(int generation) {
        if (generation == 0)
            return this;
        if (this.getParentNode() == null)
            return null;
        return ((INode) this.getParentNode()).getAncestor(generation - 1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#setParent(edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode)
     */
    public void setParent(INode parent) {
        this.parent = parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getSiblings()
     */
    public NodeList getSiblings() {
        Node parent = this.getParentNode();
        if (parent == null) {
            return null;
        } else {
            return parent.getChildNodes();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#removeNode()
     */
    public INode removeNode() {
        this.parent.removeChild(this);
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#removeChildNodes()
     */
    public List removeChildNodes() throws DOMException {
        List children = this.children.subList(0,this.children.size());
        this.children = new Vector();
        return children;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#equals(edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode)
     */
    public boolean equals(INode other) {
        return this.getComparator().equals(other);
    }

    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append("<"+this.nodeName() + ":"+ this.getSize() + ";id-"+this.getNodeID()+">");        
        return out.toString();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#toString(int,
     *      java.lang.String)
     */
    public String toString(int depth, String indent) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < depth; i++) {
            out.append(indent);
        }

        out.append(this.getLabel());
        out.append("\n");
        NodeList children = this.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            out.append(((AugmentedNode) children.item(i)).toString(depth + 1,
                    indent));
        }
        return out.toString();
    }

    /**
     * returns the text hanging off a node, minus all the markup tags
     * 
     * @return
     */
    public String getNodeText() {
        if (getNodeType() == Node.TEXT_NODE) {
            return this.getNodeValue();
        }

        if (this.getTagName().equals(WrapperManager.URL_IDENTIFIER)
                || this.getTagName().equals(WrapperManager.SRC_IDENTIFIER)) {
            if (this.getChild(0) != null)
                return this.getChild(0).getTagName();
        }

        StringBuffer text = new StringBuffer();
        NodeList children = this.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!((AugmentedNode) children.item(i)).getTagName().equals(
                    WrapperManager.URL_IDENTIFIER)
                    && !((AugmentedNode) children.item(i)).getTagName().equals(
                            WrapperManager.SRC_IDENTIFIER))
                text.append(((AugmentedNode) children.item(i)).getNodeText());
        }

        return text.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getComparator()
     */
    public NodeComparator getComparator() {
        //if (this.getNodeType() == Node.TEXT_NODE) {
        //    return new DOMNodeComparator(this.getTagName(), this.getNodeText());
        //} else {
        return new DOMNodeComparator(this.getTagName(), this.getTagName());
        //}
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getDeleteCost()
     */
    public int getDeleteCost() {
        return this.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getInsertCost()
     */
    public int getInsertCost() {
        return this.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode#getChangeCost(edu.mit.lcs.haystack.server.extensions.wrapperinduction.INode)
     */
    public int getChangeCost(INode other) {
        return (this.equals(other)) ? 0 : this.getSize() + other.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#height()
     */
    public int height() {
        return this.getHeight();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#numChildren()
     */
    public int numChildren() {
        return children.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#numDescendants()
     */
    public int numDescendants() {
        int numDescendants = 0;
        int numChildren = this.numChildren();
        for (int i = 0; i < numChildren; i++) {
            IAugmentedNode child = (IAugmentedNode) this.getChild(i);
            numDescendants += 1 + child.numDescendants();
        }
        return numDescendants;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#contentSize()
     */
    private int _cachedContentSize = -1;
    public int contentSize() {
        
        if (_cachedContentSize >= 0) {
            return _cachedContentSize;
        }
        
        final int IMG_CONTENT_SIZE = 4;
        int nodeType = this.getNodeType();
        String nodeName = this.getNodeName();
        
        if (nodeType == Node.TEXT_NODE) {
            _cachedContentSize = 1 + this.getNodeValue().length();
        } 
        else if (nodeName != null && nodeName.equalsIgnoreCase("img")){
            // need to add some image processing?
            _cachedContentSize = 1+IMG_CONTENT_SIZE;
        }
        else {
            /* recursively calculate text size */
            int contentSize = 1;
            int numChildren = this.numChildren();
            for (int i = 0; i < numChildren; i++) {
                IAugmentedNode child = (IAugmentedNode) this.getChild(i);
                contentSize += child.contentSize();
            }
            _cachedContentSize = contentSize;
        }
        
        return _cachedContentSize;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#textSize()
     */
    public int textSize() {
        if (this.getNodeType() == Node.TEXT_NODE) {
            return this.getNodeValue().length();
        } else {
            /* recursively calculate text size */
            int textSize = 0;
            int numChildren = this.numChildren();
            for (int i = 0; i < numChildren; i++) {
                IAugmentedNode child = (IAugmentedNode) this.getChild(i);
                textSize += child.textSize();
            }
            return textSize;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#getCluster()
     */
    public ICluster getCluster() {
        return cluster;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#setCluster(edu.mit.lcs.haystack.server.infoextraction.ICluster)
     */
    public void setCluster(ICluster cluster) {
        this.cluster = cluster;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#setIndex(int)
     */
    public void setIndex(int index) {
        this.index = index;  
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.tagtree.IAugmentedNode#getIndex()
     */
    public int getIndex() {
        return this.index;
    }
}