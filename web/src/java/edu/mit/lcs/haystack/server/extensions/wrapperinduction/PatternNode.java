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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 *  Represents a pattern derived from examples by the user.
 */
public class PatternNode extends ElementImpl implements SemanticNode {

    protected NodeComparator nodeComparator;
    protected HashSet semanticResources;
    protected boolean isWildcard;

    /**
     *  The matcher object is used to match the children of this
     *  object against the children of a given INode.
     */
    protected Matcher matcher;

    /**
     *  Recursively constructs a new pattern with this node mapping to
     *  the given NodeID in the example.
     *
     *  @throws PatternException if the given NodeID doesn't return
     *  any nodes from given Example.
     */
    public PatternNode(Example template, NodeID currPatternNode) throws PatternException {
	this.semanticResources = new HashSet();

	// this should always return a single node
	INode templateNode = currPatternNode.getNodes(template.getRoot())[0];
	this.nodeComparator = templateNode.getComparator();
	this.tagName = templateNode.toString();
	this.children = new ArrayList();
	this.isWildcard = false;
	this.siblingNo = 0;	// this will be reset if we're being
				// constructed from a parent PatternNode
	
	NodeID selectionID = template.getSelectionID();
	if (currPatternNode.equalsIgnoreRange(selectionID) &&
	    selectionID.getRange().getSize() > 1) {
	    //	    System.out.println("At node " + this.tagName + ", creating lapis matcher");
	    // we are at a range - we need a range matcher.
	    this.matcher = new LapisMatcher(this, template);
	}
	else {
	    this.matcher = new StandardMatcher(this);
	    if (currPatternNode.equals(selectionID)) {
		this.semanticResources.add(WrapperManager.PATTERN_SEMANTIC_ROOT_PROP);
	    }

	    // add children
	    NodeList currChildren = templateNode.getChildNodes();
	    for (int i = 0; i < currChildren.getLength(); i++) {
		try {
		    INode currChild = (INode)currChildren.item(i);
		    PatternNode newChild =
			new PatternNode(template,
					currPatternNode.makeChildNodeID(i));
		    this.children.add(newChild);
		    newChild.setParent(this);
		    newChild.setSiblingNo(i);	    
		}
		catch (NodeIDException e) {
		    throw new PatternException(e.toString());
		}
	    }
	}
    }


    /**
     *  Constructor for use by fromResource()
     */
    protected PatternNode(String tagName, int siblingNo, boolean isWildcard, HashSet semanticResources, NodeComparator nodeComparator) {
	this.tagName = tagName;
	this.siblingNo = siblingNo;
	this.isWildcard = isWildcard;
	this.semanticResources = semanticResources;
	this.nodeComparator = nodeComparator;
	this.children = new ArrayList();
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) out.append(indent);
	out.append(this.tagName + " (" + this.siblingNo + ") ");
	if (this.matcher instanceof LapisMatcher) {
	    out.append("\n");
	    out.append(((LapisMatcher)matcher).toString(depth+1, indent));
	    return out.toString();
	}
	else {
	    if (this.isSemantic()) out.append("   " + this.semanticResources);
	    out.append("\n");
	    for (int i = 0; i < this.children.size(); i++) {
		//		if (this.children.get(i) == null) System.out.println("next child is null!");
		out.append(((INode)this.children.get(i)).toString(depth+1, indent));
	    }
	    return out.toString();
	}
    }
    
    public int getSize() {
	int size = 0;
	for (int i = 0; i < this.children.size(); i++) {
	    size += ((INode)this.children.get(i)).getSize();
	}
	size++;			// this node
	return size;
    }
    
    /**
     *  Returns true if the given INode is the same as this node
     *  (based on this node's comparator.
     */
    public boolean equals(INode other) {
	if (this.isWildcard()) return true;
	if (this.matcher instanceof LapisMatcher &&
	    other instanceof PatternNode) return this.matcher.equals(((PatternNode)other).matcher);
	return this.nodeComparator.equals(other);
    }

    public boolean equals(Object other) {
	if (!(other instanceof INode)) return false;
	return this.equals((INode)other);
    }

    public NodeComparator getComparator() {
	return this.nodeComparator;
    }

    public int getChangeCost(INode other) {
	return (this.equals(other)) ? 0 : this.getDeleteCost()+other.getInsertCost();
    }


    /**
     *  Merges this node with the given node by making this one a
     *  wildcard.  Also makes all descendents of this child wildcards
     *  and merges neighboring wildcards
     *
     *  If both nodes are LAPIS nodes, adds the other node's LAPIS
     *  pattern as another example for this node.
     */
    public void merge(INode other) {
	if (other != null &&
	    other instanceof PatternNode &&
	    ((PatternNode)other).matcher instanceof LapisMatcher) {
	    ((LapisMatcher)this.matcher).merge((LapisMatcher)((PatternNode)other).matcher);
	}
	else {
	    this.isWildcard = true;
	    this.tagName = "*";
	    this.nodeComparator.set("tagName", "*");

	    // make all children wildcards
	    if (this.hasChildNodes()) {
		NodeList children = this.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
		    ((PatternNode)children.item(i)).merge(null);
		}
	    }

	    if (!this.hasSemanticDescendent()) {
		// collapse neighboring wildcards
		PatternNode curr = null;
		for (int i = this.getSiblingNo()-1; i > 0; i--) 
		    if (!mergePatternSibling((PatternNode)this.getAncestor(1).getChild(i)))
			break;

		int i = this.getSiblingNo()+1;	
		while (i < this.getAncestor(1).getChildNodes().getLength()) 
		    if (!mergePatternSibling((PatternNode)this.getAncestor(1).getChild(i)))
			break;
	    }
	}
    }

    /**
     *  Merges this node with a sibling PatternNode iff:
     *    - the other node is a wildcard
     *    - the other node has no semantic descendents
     *  (assumes that this node is already a wildcard and has no
     *  semantic descendents). Keeps the number of descendents of the
     *  node with greater height
     */
    protected boolean mergePatternSibling(PatternNode other) {
	if (other.isWildcard() && !other.hasSemanticDescendent()) {
	    if (this.getHeight() >= other.getHeight()) {
		other.removeNode();
	    }
	    else {
		this.removeChildNodes();
		this.appendChild(other.getChild(0));
	    }
	    return true;
	}
	return false;
    }

    public boolean isWildcard() {
	return this.isWildcard;
    }

    /**
     *  Attempts to match this pattern against the given node
     */
    public PatternResult match(INode toMatch, Resource semanticClassRes) {
	return this.matcher.match(toMatch, semanticClassRes);
    }


    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource nodeRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(nodeRes, Constants.s_rdf_type, WrapperManager.PATTERN_NODE_CLASS));
	rdfc.add(new Statement(nodeRes, WrapperManager.PATTERN_NODE_TAG_NAME_PROP, new Literal(this.getTagName())));
	rdfc.add(new Statement(nodeRes, Constants.s_dc_title, new Literal(this.getTagName())));
	rdfc.add(new Statement(nodeRes, WrapperManager.PATTERN_NODE_SIBLING_NO_PROP, new Literal(String.valueOf(this.getSiblingNo()))));
	rdfc.add(new Statement(nodeRes, WrapperManager.PATTERN_NODE_IS_WILDCARD_PROP, new Literal(String.valueOf(this.isWildcard()))));
	if (this.isSemantic()) {
	    Iterator iter = this.semanticResources.iterator();
	    while (iter.hasNext()) {
		rdfc.add(new Statement(nodeRes,
				       WrapperManager.PATTERN_NODE_SEMANTIC_RESOURCE_PROP,
				       (Resource)iter.next()));
	    }
	}
	rdfc.add(new Statement(nodeRes, WrapperManager.PATTERN_NODE_COMPARATOR_PROP, this.nodeComparator.makeResource(rdfc)));
	rdfc.add(new Statement(nodeRes, WrapperManager.PATTERN_NODE_MATCHER_PROP, this.matcher.makeResource(rdfc)));

	for (int i = 0; i < this.children.size(); i++) {
	    rdfc.add(new Statement(nodeRes, WrapperManager.PATTERN_NODE_CHILD_NODE_PROP, ((PatternNode)this.children.get(i)).makeResource(rdfc)));
	}

	return nodeRes;
    }

    public static PatternNode fromResource(Resource nodeRes, IRDFContainer rdfc) throws RDFException {
	if (nodeRes == null) {
	    //	    System.out.println("nodeRes is null");
	    return null;
	}
	String tagName = rdfc.extract(nodeRes,
				      WrapperManager.PATTERN_NODE_TAG_NAME_PROP,
				      null).getContent();
	int siblingNo = Integer.parseInt(rdfc.extract(nodeRes,
						      WrapperManager.PATTERN_NODE_SIBLING_NO_PROP,
						      null).getContent());
	boolean isWildcard = Boolean.valueOf(rdfc.extract(nodeRes,
							  WrapperManager.PATTERN_NODE_IS_WILDCARD_PROP, 
							  null).getContent()).booleanValue();

	Set semResSet = rdfc.query(new Statement[] {new Statement(nodeRes,
								  WrapperManager.PATTERN_NODE_SEMANTIC_RESOURCE_PROP,
								  Utilities.generateWildcardResource(1))},
				   Utilities.generateWildcardResourceArray(1),
				   Utilities.generateWildcardResourceArray(1));

	HashSet semanticResources = new HashSet();
	if (semResSet != null) {
	    Iterator iter = semResSet.iterator();
	    while (iter.hasNext()) {
		Resource semResource = (Resource)((RDFNode[])iter.next())[0];
		semanticResources.add(semResource);
	    }
	}

	NodeComparator nodeComparator = NodeComparator.fromResource((Resource)rdfc.extract(nodeRes,
											   WrapperManager.PATTERN_NODE_COMPARATOR_PROP,
											   null),
								    rdfc);
	PatternNode node = new PatternNode(tagName, siblingNo, isWildcard, semanticResources, nodeComparator);

	Matcher matcher = Matcher.fromResource(node,
					       (Resource)rdfc.extract(nodeRes,
								      WrapperManager.PATTERN_NODE_MATCHER_PROP,
								      null),
					       rdfc);
	node.matcher = matcher;
	
	Set childrenSet = rdfc.query(new Statement[] {new Statement(nodeRes,
								    WrapperManager.PATTERN_NODE_CHILD_NODE_PROP,
								    Utilities.generateWildcardResource(1))},
				     Utilities.generateWildcardResourceArray(1),
				     Utilities.generateWildcardResourceArray(1));

	int size = (childrenSet == null) ? 0 : childrenSet.size();
	PatternNode[] childNodes = new PatternNode[size];
	if (childrenSet != null) {
	    Iterator iter = childrenSet.iterator();
	    while (iter.hasNext()) {
		PatternNode child = PatternNode.fromResource((Resource)((RDFNode[])iter.next())[0], rdfc);
		if (child == null) continue;
		child.setParent(node);
		childNodes[child.getSiblingNo()] = child;
	    }
	}

	ArrayList children = new ArrayList(size);
	for (int i = 0; i < childNodes.length; i++) {
	    if (childNodes[i] == null) continue;
	    children.add(childNodes[i]);
	}
	node.children = children;

	return node;
    }

    
    ////////// SemanticNode Interface methods //////////

    public Set getSemanticResources() {
	return this.semanticResources;
    }

    public boolean hasSemanticResource(Resource semanticResource) {
	return this.semanticResources.contains(semanticResource);
    }

    public void addSemanticResource(Resource semanticResource) {
	this.semanticResources.add(semanticResource);
    }

    public void addSemanticResource(Resource semanticResource,
				    String matchText,
				    String selectedText) {
	this.semanticResources.add(semanticResource);
 	if (this.matcher != null && this.matcher instanceof LapisMatcher) 
 	    ((LapisMatcher)this.matcher).addProperty(semanticResource,
						     matchText,
						     selectedText);
    }

    public void addSemanticResources(Set semanticResources) {
	this.semanticResources.addAll(semanticResources);
    }

    public boolean isSemantic() {
	return (this.semanticResources != null && this.semanticResources.size() > 0);
    }

    public boolean hasSemanticDescendent() {
	if (this.isSemantic()) return true;
	boolean hasDesc = false;
	for (int i = 0; i < this.children.size(); i++) 
	    if (((PatternNode)this.children.get(i)).hasSemanticDescendent())
		hasDesc = true;
	return hasDesc;
    }


}
