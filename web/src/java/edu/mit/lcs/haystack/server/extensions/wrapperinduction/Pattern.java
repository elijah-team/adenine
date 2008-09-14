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

import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ITree;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 *  Represents a pattern to find elements in a tree.
 *
 *  @author Andrew Hogue
 */
public class Pattern implements ITree {

    protected static final int ABSOLUTE_MAX_EXAMPLE_SIZE = 250;
    protected static final float ABSOLUTE_MAX_COST_LIMIT = .5f;

    protected Resource patternRes;
    protected boolean patternModified;

    protected PatternNode patternRoot;
    protected int maxSize;
    protected String url;

    protected String wrapperName;

    // the following is of type rdfs:Class or daml:Class, describing
    // the main type matched by this Pattern
    protected Resource semanticType;

    // this is a mapping from Resource objects representing Classes or
    // Properties to NodeIDs of PatternNodes in this Pattern.
    protected HashMap semanticNodes;

    public Pattern(Example example, int maxSize, float costLimit) throws PatternException, ExampleSizeOutOfBoundsException {
	this.maxSize = maxSize;
	this.url = example.getURL();
	this.semanticNodes = new HashMap();

	this.updatePattern(example, costLimit);
    }

    /**
     *  For use by the fromResource() method
     */
    protected Pattern(Resource patternRes,
		      String wrapperName,
		      PatternNode patternRoot,
		      String url,
		      int maxSize,
		      Resource semanticType) {
	this.patternRes = patternRes;
	this.wrapperName = wrapperName;
	this.patternModified = false;
	this.patternRoot = patternRoot;
	this.url = url;
	this.maxSize = maxSize;
	this.semanticType = semanticType;
	rebuildSemanticHash();
    }

    public Resource getSemanticType() {
	return this.semanticType;
    }

    public void setSemanticType(Resource res) {
	this.patternModified = true;
	this.semanticType = res;
    }

    public void setWrapperName(String name) {
	this.patternModified = true;
	this.wrapperName = name;
    }

    public void addPositiveExample(Example example, float costLimit) throws PatternException, ExampleSizeOutOfBoundsException {
	updatePattern(example, costLimit);
    }

    /**
     *  Adds a semantic property to this pattern.  The given NodeID
     *  picks out a PatternNode relative to the root of this pattern.
     *  The given Strings represent the text of the pattern match and
     *  the text of the property in the document, respectively, and
     *  are only used for LAPIS matches.
     */
    public void addProperty(NodeID propertyID,
			    Resource semanticResource,
			    String matchText,
			    String selectedText) throws PatternException {
	PatternNode semanticNode = getSemanticNode(propertyID);
	semanticNode.addSemanticResource(semanticResource, matchText, selectedText);
	System.out.println(this);
	this.patternModified = true;
	this.rebuildSemanticHash();
    }

    /**
     *  Returns the PatternNodes for a property, or the LAPIS node if
     *  it is within one.
     */
    protected PatternNode getSemanticNode(NodeID propertyID) throws PatternException {
	if (this.semanticNodes.containsKey(WrapperManager.PATTERN_SEMANTIC_ROOT_PROP)) {
	    INode semanticRoot = ((NodeID)this.semanticNodes.get(WrapperManager.PATTERN_SEMANTIC_ROOT_PROP)).getNodes(this.patternRoot)[0];
	    return (PatternNode)propertyID.getNodes(semanticRoot)[0];
	}
	else {
	    // try to find LAPIS node
	    PatternNode semanticNode = this.patternRoot;
	    for (int i = 1; i < propertyID.getLength(); i++) {
		if (semanticNode.getChild(i) != null) {
		    semanticNode = (PatternNode)semanticNode.getChild(i);
		}
	    }
	    if (semanticNode.matcher instanceof LapisMatcher) {
		return semanticNode;
	    }
	    else {
		throw new PatternException("Could not find property nodes in Pattern.addProperty()! (NodeID " + propertyID + ")");
	    }
	}
    }

    /**
     *  Updates this pattern's patternRoot given the current set of
     *  examples.
     */
    public void updatePattern(Example example, float costLimit) throws PatternException, ExampleSizeOutOfBoundsException {
	this.patternModified = true;

	NodeID newID = findPatternRoot(example);
	if (newID == null) return;
	
	PatternNode newPatternRoot = new PatternNode(example, newID);

	//	System.out.println("Got new pattern root 1:\n" + newPatternRoot.toString(1, "  "));

	// next collapse nodes within the pattern
	this.collapseNodes(newPatternRoot, costLimit);

	//	System.out.println("Got new pattern root 3:\n" + newPatternRoot.toString(1, "  "));

	// first, try to find other nodes we can combine the root with...
	INode rootNode = newID.getNodes(example.getRoot())[0];
	INode[] sameTags = rootNode.getAncestor(1).getChildren(rootNode.getTagName());
	for (int i = 0; i < sameTags.length; i++) {
	    if (sameTags[i] == rootNode) continue;
	    mergePattern(newPatternRoot, sameTags[i], costLimit, false, false);
	}

	//	System.out.println("Got new pattern root 2:\n" + newPatternRoot.toString(1, "  "));

	// finally, merge it with the existing patternRoot
	if (this.patternRoot == null) {
	    this.patternRoot = newPatternRoot;
	}
	else {
	    mergePattern(this.patternRoot, newPatternRoot, 0, false, false);
	}

	System.out.println(this);

	this.rebuildSemanticHash();
    }

    /**
     *  Traverses the pattern, rebuilding the semanticNodes hash
     */
    protected void rebuildSemanticHash() {
	this.semanticNodes = new HashMap();
	INode[] nodes = (INode[])this.patternRoot.getPostorderNodes();
	for (int i = 0; i < nodes.length; i++) {
	    if (nodes[i] == null) continue;
	    PatternNode curr = (PatternNode)nodes[i];
	    if (curr.isSemantic()) {
		//		System.out.println("Pattern found semantic resources " + curr.getSemanticResources() + " at " + curr.getNodeID());
		Iterator iter = curr.getSemanticResources().iterator();
		while (iter.hasNext()) {
		    this.semanticNodes.put((Resource)iter.next(), curr.getNodeID());
		}
	    }
	}
    }

    protected void collapseNodes(PatternNode parent, float costLimit) {
	if (parent.getChildNodes().getLength() == 0) return;
	if (parent.getChildNodes().getLength() == 1) {
	    // TODO: this is a hack... want a better way of recursing?
	    collapseNodes((PatternNode)parent.getChild(0), costLimit);
	    return;
	}

	//	System.out.println("collapsing on " + parent + " with " + parent.getChildNodes().getLength() + " children");
	PatternNode currChild = (PatternNode)parent.getChild(0);
	int nextInd = 1;
	PatternNode nextChild = (PatternNode)parent.getChild(nextInd);
	while (true) {
	    // if we can, merge the current child with the next and remove the next.
	    if (currChild.getTagName().equalsIgnoreCase(nextChild.getTagName()) &&
		mergePattern(currChild, nextChild, costLimit, true, false)) {
		if (nextInd < parent.getChildNodes().getLength()) {
		    nextChild = (PatternNode)parent.getChildNodes().item(nextInd);
		}
		else {
		    break;
		}
	    }
	    else {
		if (nextInd < parent.getChildNodes().getLength()-1) {
		    currChild = nextChild;
		    nextInd++;
		    nextChild = (PatternNode)parent.getChildNodes().item(nextInd);
		}
		else {
		    break;
		}
	    }
	}

    }

    /**
     *  Given an example, finds the appropriate node in the example to
     *  create the root of the pattern templates, based on the
     *  maxSize.
     */
    public NodeID findPatternRoot(Example example) throws PatternException, ExampleSizeOutOfBoundsException {
	INode[] semanticNodes = example.getSelectedNodes();
	if (semanticNodes.length == 0) {
	    throw new PatternException("Could not find selected nodes in Example template! (NodeID " + example.getSelectionID() + ")");
	}
		
	if (example.getSize() > ABSOLUTE_MAX_EXAMPLE_SIZE)
	    throw new ExampleSizeOutOfBoundsException("Example node size (" + example.getSize() + ") exceeds maximum allowed (" + ABSOLUTE_MAX_EXAMPLE_SIZE + ")");

	// make the current node either the selected node or its
	// parent if it is a range.
	INode currNode = (semanticNodes.length == 1) ? semanticNodes[0] : semanticNodes[0].getAncestor(1);

	// find the highest ancestor that doesn't exceed maxSize
	while (currNode.getAncestor(1) != null && currNode.getAncestor(1).getSize() <= maxSize) {
	    currNode = currNode.getAncestor(1);
	}

	return currNode.getNodeID();
    }

    /**
     *  Merges the given INode into the given template, removing any
     *  nodes that are changed or removed in a best-mapping.  The
     *  given template is modified in place.
     *
     *  If costLimit > 0, checks to make sure the normalized cost of
     *  the mapping is less than the costLimit - if it is greater,
     *  then no modifications are made.  A costLimit of 0 indicates
     *  that any modifications should be considered.
     *
     *  If deleteMerged is true, also deletes the node toMerge from
     *  its tree.
     *
     *  @return true if template is within the costLimit, false if not.
     */
    protected boolean mergePattern(PatternNode template,
				   INode toMerge,
				   float costLimit,
				   boolean deleteMerged,
				   boolean printMap) {
	Mapping m = new TreeDistance(template, toMerge, costLimit).getMapping();
	if (printMap) System.out.println(m);
	if (costLimit > 0 && (float)m.getNormalizedCost() > costLimit) return false;
	if ((float)m.getNormalizedCost() > ABSOLUTE_MAX_COST_LIMIT) {
	    System.out.println(">>> Cannot merge examples, cost (" + m.getNormalizedCost() + ") exceeds ABSOLUTE_MAX_COST_LIMIT (" + ABSOLUTE_MAX_COST_LIMIT + ")");
	    return false;
	}

	Pair[] pairs = m.getPairs();
	//	Arrays.sort(pairs);
	for (int j = pairs.length-1; j >= 0; j--) {
	    // remove any node that is deleted or changed
	    if (pairs[j].node1 != null && pairs[j].cost > 0) {
		((PatternNode)pairs[j].node1).merge(pairs[j].node2);
	    }
	    else if (pairs[j].node1 != null &&
		     pairs[j].node2 != null &&
		     pairs[j].node2 instanceof SemanticNode &&
		     ((SemanticNode)pairs[j].node2).isSemantic()) {
		((PatternNode)pairs[j].node1).addSemanticResources(((SemanticNode)pairs[j].node2).getSemanticResources());
	    }
	}

	if (deleteMerged) toMerge.removeNode();

	return true;
    }

    /**
     *  Attempts to match this pattern against the given tree,
     *  returning all subtrees that match.
     *
     *  @param t the tree against which to match
     */
    public PatternResult match(ITree t) {
	return this.match((INode)t.getDocumentElement());

    }

    public PatternResult match(INode root) {
	if (this.patternRoot == null) return new PatternResult(false);
	INode[] postorder = root.getPostorderNodes();
	PatternResult result = new PatternResult(false);
	// we could speed this up if we maintained 'height' for each INode
	// and didn't search below pattern.height()
	for (int i = 0; i < postorder.length; i++) {
	    result.merge(this.patternRoot.match(postorder[i], null));
	}
	return result;
    }


    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	if (this.patternModified && this.patternRes != null) {
	    removeResource(patternRes, rdfc);
	    this.patternRes = null;
	}

	if (this.patternRes != null) return this.patternRes;
	if (this.patternRoot == null) return null;

	this.patternRes = Utilities.generateUniqueResource();
	rdfc.remove(new Statement(patternRes,
				  Constants.s_rdf_type,
				  Utilities.generateWildcardResource(1)),
		    Utilities.generateWildcardResourceArray(1));
	rdfc.add(new Statement(patternRes, Constants.s_rdf_type, WrapperManager.PATTERN_CLASS));
	
	if (this.wrapperName != null) {
	    rdfc.add(new Statement(patternRes,
				   Constants.s_dc_title,
				   new Literal(this.wrapperName)));
	}
	else if (this.semanticType != null) {
	    // add the type's label as a dc:title for display purposes
	    RDFNode titleRes = rdfc.extract(this.semanticType,
					    Constants.s_rdfs_label,
					    null);
	    if (titleRes != null) {
		rdfc.add(new Statement(patternRes,
				       Constants.s_dc_title,
				       new Literal(titleRes.getContent())));
	    }

	}

	rdfc.add(new Statement(patternRes,
			       WrapperManager.PATTERN_SEMANTIC_TYPE_PROP,
			       this.semanticType));

	rdfc.add(new Statement(patternRes, WrapperManager.PATTERN_PATTERN_ROOT_PROP, this.patternRoot.makeResource(rdfc)));
	rdfc.add(new Statement(patternRes, WrapperManager.PATTERN_URL_PROP, new Literal(this.url)));
	rdfc.add(new Statement(patternRes, WrapperManager.PATTERN_MAX_SIZE_PROP, new Literal(String.valueOf(this.maxSize))));
	
	return patternRes;
    }


    public static void removeResource(Resource oldPatternRes, IRDFContainer rdfc) throws RDFException {
	// TODO: make this work recursively?
	//	System.out.println("removing old pattern " + oldPatternRes);
	rdfc.remove(new Statement(oldPatternRes,
				  Utilities.generateWildcardResource(1),
				  Utilities.generateWildcardResource(2)),
		    Utilities.generateWildcardResourceArray(2));
	rdfc.remove(new Statement(Utilities.generateWildcardResource(1),
				  Utilities.generateWildcardResource(2),
				  oldPatternRes),
		    Utilities.generateWildcardResourceArray(2));
	rdfc.remove(new Statement(oldPatternRes,
				  WrapperManager.PATTERN_URL_PROP,
				  Utilities.generateWildcardResource(1)),
		    Utilities.generateWildcardResourceArray(1));
    }

    public static Pattern fromResource(Resource patternRes, IRDFContainer rdfc) throws RDFException {
	PatternNode patternRoot = PatternNode.fromResource((Resource)rdfc.extract(patternRes,
										  WrapperManager.PATTERN_PATTERN_ROOT_PROP,
										  null),
							   rdfc);
	
	if (patternRoot == null) return null;

	RDFNode maxSizeNode = rdfc.extract(patternRes,
					   WrapperManager.PATTERN_MAX_SIZE_PROP,
					   null);
	//	if (maxSizeNode == null) System.out.println("maxSizeNode is null");

	RDFNode semanticTypeNode = rdfc.extract(patternRes,
						WrapperManager.PATTERN_SEMANTIC_TYPE_PROP,
						null);
	Resource semanticType = (semanticTypeNode == null) ? null : (Resource)semanticTypeNode;

	RDFNode wrapperNameNode = rdfc.extract(patternRes,
					       Constants.s_dc_title,
					       null);
	String wrapperName = (wrapperNameNode == null) ? null : wrapperNameNode.getContent();

	return new Pattern(patternRes,
			   wrapperName,
			   patternRoot,
			   rdfc.extract(patternRes,
					WrapperManager.PATTERN_URL_PROP,
					null).getContent(),
			   Integer.parseInt(rdfc.extract(patternRes,
							 WrapperManager.PATTERN_MAX_SIZE_PROP,
							 null).getContent()),
			   semanticType);

    }


    public String toString() {
	if (this.patternRoot == null) 
	    return "Pattern: null";
	else 
	    return
		"Pattern (" + this.wrapperName + ": " + this.semanticType + ", " + this.url + "):\n" +
		this.patternRoot.toString(1, "  ");
    }

    public int hashCode() {
	return this.toString().hashCode();
    }

    public boolean equals(Object other) {
	return this.hashCode() == other.hashCode();
    }

    public static Example[] push(Example[] array, Example toPush) {
	Example[] pushed = new Example[array.length+1];
	for (int i = 0; i < array.length; i++) {
	    pushed[i] = array[i];
	}
	pushed[array.length] = toPush;
	return pushed;
    }


    //////// ITree interface methods ////////

    /**
     *  Returns the size (number of nodes) of the tree.
     */
    public int getSize() {
	return this.patternRoot.getSize();
    }
    
    /**
     *  Returns the nodes of this tree, in postorder 
     */
    public INode[] getNodes() {
	return this.patternRoot.getPostorderNodes();
    }

    /**
     *  Returns the root of the tree.
     */
    public INode getRoot() {
	return this.patternRoot;
    }

    ////////////////////////////////////
    /// org.w3c.dom.Document methods ///
    ////////////////////////////////////

    public Attr createAttribute(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Attr createAttributeNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public CDATASection createCDATASection(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Comment createComment(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public DocumentFragment createDocumentFragment() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Element createElement(String tagName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Element createElementNS(String namespaceURI, String qualifiedName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public EntityReference createEntityReference(String name) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public ProcessingInstruction createProcessingInstruction(String target, String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Text createTextNode(String data) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public DocumentType getDoctype() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Element getDocumentElement() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Element getElementById(String elementId) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public NodeList getElementsByTagName(String tagname) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public DOMImplementation getImplementation() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node importNode(Node importedNode, boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }


    ///////////////////////////////////
    ///  org.w3c.dom.Node methods   ///
    ///////////////////////////////////



    public Node appendChild(Node newChild) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node cloneNode(boolean deep) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public NamedNodeMap getAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public NodeList getChildNodes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node getFirstChild() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node getLastChild() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public String getLocalName() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public String getNamespaceURI() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node getNextSibling() {
	return null;
    }

    public String getNodeName() {
	return "#document";
    }

    public short getNodeType() {
	return Node.DOCUMENT_NODE;
    }

    public String getNodeValue() {
	return null;		// as per DOM spec
    }

    public Document getOwnerDocument() {
	return this;
    }

    public Node getParentNode() {
	return null;
    }

    public String getPrefix() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");	
    }

    public Node getPreviousSibling() {
	return null;
    }

    public boolean hasAttributes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public boolean hasChildNodes() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public boolean isSupported(String feature, String version) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");	
    }

    public void normalize() {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }

    public Node removeChild(Node oldChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
	throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Can't insert nodes into the Document class");
    }

    public void setNodeValue(String nodeValue) {
    }

    public void setPrefix(String prefix) {
	throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Unimplemented method in Pattern");
    }


}

