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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.ozone.web.WebViewPart;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 *  Represents a result from matching a pattern to a tree.  Contains
 *  semantic entries, each mapped to an array of PatternMatch objects.
 */
public class PatternResult {

    // maps NodeIDs which matched
    // WrapperManager.PATTERN_SEMANTIC_ROOT_PROP to anonymous Class
    // Resources containing their properties
    protected HashMap semanticRootMatches;

    // A set of NodeIDs that matched semantic properties
    protected HashSet semanticPropertyMatches;
    
    // Maps the same anonymous Class Resources in semanticRootMatches to
    // ArrayLists containing Statements about their properties
    protected HashMap statements;

    protected boolean isMatch;

    public PatternResult(boolean isMatch) {
	this.semanticRootMatches = new HashMap();
	this.semanticPropertyMatches = new HashSet();
	this.statements = new HashMap();
	this.isMatch = isMatch;
    }

    public void setMatch(boolean isMatch) {
	this.isMatch = isMatch;
    }

    public boolean isMatch() {
	return isMatch;
    }

    /**
     *  Returns all INodes that matched WrapperManager.PATTERN_SEMANTIC_ROOT_PROP
     */
    public NodeID[] getMatches() {
	return (NodeID[])this.semanticRootMatches.keySet().toArray(new NodeID[0]);
    }

    /**
     *  Returns all INodes that matched a semantic property.
     */
    public NodeID[] getPropertyMatches() {
	return (NodeID[])this.semanticPropertyMatches.toArray(new NodeID[0]);
    }

    /**
     *  Returns the anonymous class Resource associated with the given
     *  NodeID
     */
    public Resource getMatchResource(NodeID matchedNodeID) {
	if (this.semanticRootMatches.containsKey(matchedNodeID)) 
	    return (Resource)this.semanticRootMatches.get(matchedNodeID);
	else
	    return null;
    }

    /**
     *  Returns statements about the given anonymous Resource
     */
    public Statement[] getStatements(Resource classResource) {
	if (!this.statements.containsKey(classResource)) return new Statement[0];
	return (Statement[])((ArrayList)this.statements.get(classResource)).toArray(new Statement[0]);
    }

    /**
     *  Merges the given result into this one, returning this result.
     */
    public PatternResult merge(PatternResult other) {
	if (!other.isMatch()) {
	    return this;
	}
	
	if (!this.isMatch()) {	// other.isMatch turns this into a match
	    this.isMatch = true;
	}

	this.semanticRootMatches.putAll(other.semanticRootMatches);
	this.semanticPropertyMatches.addAll(other.semanticPropertyMatches);
	
	Iterator statementIter = other.statements.keySet().iterator();
	while (statementIter.hasNext()) {
	    Resource classRes = (Resource)statementIter.next();
	    if (this.statements.containsKey(classRes)) {
		((ArrayList)this.statements.get(classRes)).addAll((ArrayList)other.statements.get(classRes));
	    }
	    else {
		this.statements.put(classRes, (ArrayList)other.statements.get(classRes));
	    }
	}

	return this;
    }
    
    /**
     *  Adds the given match to this result
     *
     *  @param semanticClassRes a unique resource representing an instance of a semantic class
     *  @param semanticResources a set of Property resources to attach to semanticClassRes
     *  @param matchedNode the node which was matched
     */
    public void add(Resource semanticClassRes, Set semanticResources, INode matchedNode) {
	if (semanticClassRes == null) return;

	Iterator resIter = semanticResources.iterator();
	while (resIter.hasNext()) {
	    Resource nextRes = (Resource)resIter.next();
	    if (nextRes.equals(WrapperManager.PATTERN_SEMANTIC_ROOT_PROP)) {
		this.semanticRootMatches.put(matchedNode.getNodeID(), semanticClassRes);
	    }
	    else {
		if (!this.statements.containsKey(semanticClassRes)) {
		    this.statements.put(semanticClassRes, new ArrayList());
		}

		((ArrayList)this.statements.get(semanticClassRes)).add(new Statement(semanticClassRes,
										     nextRes,
										     new Literal(((IDOMElement)matchedNode).getNodeText())));
		this.semanticPropertyMatches.add(matchedNode.getNodeID());
	    }
	}
    }

    /**
     *  Adds a semantic root match 
     */
    public void add(Resource semanticClassRes,
		    NodeID matchedNodeID) {
	if (semanticClassRes == null) return;
	this.semanticRootMatches.put(matchedNodeID, semanticClassRes);
    }

    /**
     *  Adds a semantic property match
     */
    public void add(Resource semanticClassRes,
		    Resource semanticRes,
		    String matchText) {
	if (semanticClassRes == null) return;

	if (!this.statements.containsKey(semanticClassRes)) {
	    this.statements.put(semanticClassRes, new ArrayList());
	}

	((ArrayList)this.statements.get(semanticClassRes)).add(new Statement(semanticClassRes,
									     semanticRes,
									     new Literal(matchText)));
    }

    public void highlight(WebViewPart webView) {
	if (webView == null) return;
	IDOMDocument doc = webView.getDOMBrowser().getDocument();
	NodeID[] classMatches = this.getMatches();
	System.out.println("highlight got " + classMatches.length + " matches");
	
	for (int i = 0; i < classMatches.length; i++) {
	    //	    System.out.println("Matched " + classMatches[i]);
	    INode[] toHighlight = classMatches[i].getNodes(doc);
	    for (int j = 0; j < toHighlight.length; j++) {
		IDOMElement highlighted = ((IDOMElement)toHighlight[j]).highlight("yellow", "black");
		doc.addHighlightedElement(highlighted);
	    }
	}

	NodeID[] propertyMatches = this.getPropertyMatches();
	
	for (int i = 0; i < propertyMatches.length; i++) {
	    //	    System.out.println("Matched " + propertyMatches[i]);
	    INode[] toHighlight = propertyMatches[i].getNodes(doc);
	    for (int j = 0; j < toHighlight.length; j++) {
	    	IDOMElement highlighted = null;
	    	IDOMElement preHighlighted = (IDOMElement) toHighlight[j];
	    	if(preHighlighted.getTagName().equalsIgnoreCase(WrapperManager.URL_IDENTIFIER)) {
	    		IDOMElement parentHighlighted = (IDOMElement)preHighlighted.getParentNode();
	    		if(parentHighlighted.getHighlightedColor().equalsIgnoreCase("aqua") ||
	    				parentHighlighted.getHighlightedColor().equalsIgnoreCase("red")) {
	    			parentHighlighted.unhighlight();
	    			highlighted = parentHighlighted.highlight("red", "black");
	    		}
	    		else {
	    			highlighted = parentHighlighted.highlight("lime", "black");
	    		}
	    	}
	    	else {
	    		if(preHighlighted.getHighlightedColor().equalsIgnoreCase("lime") ||
	    				preHighlighted.getHighlightedColor().equalsIgnoreCase("red")) {
	    			preHighlighted.unhighlight();
	    			highlighted = preHighlighted.highlight("red", "black");
	    		}
	    		else {
	    			highlighted = preHighlighted.highlight("aqua", "black");
	    		}
	    	}
		doc.addHighlightedElement(highlighted);
	    }
	}
    }

    public void clearHighlightedElements(WebViewPart webView) {
	if (webView == null) return;
	IDOMDocument doc = webView.getDOMBrowser().getDocument();
	NodeID[] classMatches = this.getMatches();
	for (int i = 0; i < classMatches.length; i++) {
	    //	    System.out.println("Matched " + classMatches[i]);
	    INode[] toHighlight = classMatches[i].getNodes(doc);
	    for (int j = 0; j < toHighlight.length; j++) {
		((IDOMElement)toHighlight[j]).unhighlight();
		doc.removeHighlightedElement((IDOMElement)toHighlight[j]);
	    }
	}

	NodeID[] propertyMatches = this.getPropertyMatches();
	
	for (int i = 0; i < propertyMatches.length; i++) {
	    //	    System.out.println("Matched " + propertyMatches[i]);
	    INode[] toHighlight = propertyMatches[i].getNodes(doc);
	    for (int j = 0; j < toHighlight.length; j++) {
		((IDOMElement)toHighlight[j]).unhighlight();
		doc.removeHighlightedElement((IDOMElement)toHighlight[j]);
	    }
	}
    }


}


