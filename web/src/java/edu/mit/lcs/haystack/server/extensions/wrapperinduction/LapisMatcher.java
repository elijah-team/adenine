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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import lapis.BasicDocument;
import lapis.MutableRegionSet;
import lapis.NamedRegionSet;
import lapis.Region;
import lapis.RegionEnumeration;
import lapis.ml.HybridInference;
import lapis.tc.TC;
//import lapisx.enum.RestartableEnumeration;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.Range;

/**
 *  A range matcher that utilizes the LAPIS text constraints library.
 */
public class LapisMatcher extends Matcher {

    protected PatternNode patternNode;

    // maps Resource objects to text constrants features (TC objects)
    // that match them
    protected Map semanticPropertyFeatures;

    // the TC feature for the class
    protected TC semanticClassFeature;

    protected String docHTML;
    protected int startChars[];
    protected int endChars[];

    /**
     *  Constructs a new Matcher for the selected region in the given
     *  example.
     */
    public LapisMatcher(PatternNode patternNode,
			Example template) {
	this.patternNode = patternNode;
	this.semanticPropertyFeatures = new HashMap();

	INode[] selectedElements = template.getSelectedNodes();
	IDOMElement parentElement = (IDOMElement)selectedElements[0].getParentNode();
	this.docHTML = parentElement.getOuterHTML();

	// find start and end chars for lapis
	int startChar, endChar;
	StringBuffer selectedStringBuff = new StringBuffer();
	for (int i = 0; i < selectedElements.length; i++) {
	    selectedStringBuff.append(((IDOMElement)selectedElements[i]).getOuterHTML());
	}
	String selectedString = selectedStringBuff.toString();

 	this.docHTML = WrapperManager.fixHTMLString(this.docHTML);
 	selectedString = WrapperManager.fixHTMLString(selectedString);

	this.startChars = new int[] {this.docHTML.indexOf(selectedString)};
	this.endChars = new int[] {this.startChars[0] + selectedString.length()};

	this.semanticClassFeature = makeFeature(this.docHTML, this.startChars, this.endChars);
    }

    protected LapisMatcher(PatternNode patternNode,
			   Map semanticPropertyFeatures,
			   TC semanticClassFeature,
			   String docHTML,
			   int[] startChars,
			   int[] endChars) {
	this.patternNode = patternNode;
	this.semanticPropertyFeatures = semanticPropertyFeatures;
	this.semanticClassFeature = semanticClassFeature;
	this.docHTML = docHTML;
	this.startChars = startChars;
	this.endChars = endChars;
    }

    protected TC makeFeature(String docHTML, int[] startChars, int[] endChars) {
	if (startChars.length != endChars.length ||
	    startChars.length == 0) return null;

	BasicDocument doc = new BasicDocument(docHTML, null);
	HybridInference inference = new HybridInference(new NamedRegionSet(new Region(0, docHTML.length(), doc)));

	MutableRegionSet selectedRegions = new MutableRegionSet();
	for (int i = 0; i < startChars.length; i++) {
// 	    System.out.println("LapisMatcher.makeFeature(): Adding region " + startChars[i] + "-" + endChars[i]);
	    selectedRegions.insert(new Region(startChars[i], endChars[i], doc));
	}

	// SJG replaced RestartableEnumeration by Enumeration, because matches.restart()
	// is never used.
	Enumeration matches = inference.generalize(new NamedRegionSet(selectedRegions),
							      new NamedRegionSet(Region.EMPTY));
	TC feature = null;
	if (matches.hasMoreElements()) {
	    try { feature = new TC(matches.nextElement().toString()); }
	    catch (Exception e) { e.printStackTrace(); }
	}
 	System.out.println(">>> LapisMatcher() found feature: " + feature);
	return feature;
    }

    /**
     *  Merges the given matcher with this one by adding it as another
     *  region example.
     */
    public void merge(LapisMatcher other) {
	int offset = (this.docHTML.equals(other.docHTML)) ? 0 : this.docHTML.length();
	if (!this.docHTML.equals(other.docHTML)) {
	    //	    System.out.println(">>> LapisMatcher.merge(): docHTMLs are not equal");
	    this.docHTML = this.docHTML + other.docHTML;
	}
	
	int[] newStartChars = new int[this.startChars.length + other.startChars.length];
	int[] newEndChars = new int[this.endChars.length + other.endChars.length];
	for (int i = 0; i < this.startChars.length; i++) {
	    newStartChars[i] = this.startChars[i];
	    newEndChars[i] = this.endChars[i];
	}
	for (int i = this.startChars.length;
	     i < this.startChars.length + other.startChars.length;
	     i++) {
	    newStartChars[i] = other.startChars[i-this.startChars.length] + offset;
	    newEndChars[i] = other.endChars[i-this.startChars.length] + offset;
	}
	this.startChars = newStartChars;
	this.endChars = newEndChars;

	this.semanticClassFeature = makeFeature(this.docHTML, this.startChars, this.endChars);
    }

    public void addProperty(Resource propRes, String matchText, String propertyText) {
	int startChar = matchText.indexOf(propertyText);
	int endChar = startChar + propertyText.length();
	TC propFeature = makeFeature(matchText, new int[] {startChar}, new int[] {endChar});
	//	System.out.println("LapisMatcher.addProperty() got feature " + propFeature);
	this.semanticPropertyFeatures.put(propRes, propFeature);
    }

    /**
     *  Returns all matches in children of the given element.
     */
    // TODO: make this work with the semanticClassRes
    public PatternResult match(INode parent, Resource semanticClassRes) {
	if (parent == null) {
	    //	    System.out.println("Got null INode parent in LapisMatcher.match()");
	    return new PatternResult(false);
	}
	if (!this.patternNode.equals(parent)) {
	    return new PatternResult(false);
	}

	PatternResult result = new PatternResult(false);
	String parentHTML = ((IDOMElement)parent).getOuterHTML();
	HashMap childIndices = getChildNodeIndices(parent);

	BasicDocument doc = new BasicDocument(parentHTML, null);
	RegionEnumeration matches = this.semanticClassFeature.match(doc).regions();
	Region curr = matches.firstFast();
	while (curr != null) {
	    if (curr.getLength() != 0) {
		result.setMatch(true);
		int matchStart = getClosestChildNode(childIndices, curr.getStart()).getSiblingNo();
		int matchEnd = getClosestChildNode(childIndices, curr.getEnd()).getSiblingNo()-1;
		if (matchStart >= 0 && matchEnd >= 0 && matchEnd > matchStart) {
// 		    System.out.println("start/end [" + matchStart + "," + matchEnd + "]");
		    try {
			NodeID matchID = parent.getNodeID().makeChildNodeID(new Range(matchStart, matchEnd));
			if (semanticClassRes == null)
			    semanticClassRes = Utilities.generateUniqueResource();
			result.add(semanticClassRes, matchID);
			// recursively match any properties
			this.matchProperties(result,
					     semanticClassRes,
					     parentHTML.substring(curr.getStart(), curr.getEnd()));
		    }
		    catch (NodeIDException e) {
			e.printStackTrace();
		    }
		}
	    }
	    curr = matches.nextFast();
	}

	return result;
    }

    protected void matchProperties(PatternResult result, Resource semanticClassRes, String matchText) {
	Iterator propertyIter = this.semanticPropertyFeatures.keySet().iterator();
	while(propertyIter.hasNext()) {
	    Resource currProp = (Resource)propertyIter.next();
	    TC pattern = (TC)this.semanticPropertyFeatures.get(currProp);
	    //	    System.out.println("LapisMatcher.matchProperties() " + currProp + ", pattern:\n" + pattern);
	    // do matching
	    BasicDocument doc = new BasicDocument(matchText, null);
	    RegionEnumeration matches = this.semanticClassFeature.match(doc).regions();
	    Region curr = matches.firstFast();
	    if (curr != null) 
		result.add(semanticClassRes, currProp, curr.extract(matchText));
	}
    }

    protected String childIndicesToString(HashMap childIndices) {
	Integer[] indices = (Integer[])childIndices.keySet().toArray(new Integer[0]);
	Arrays.sort(indices);
	StringBuffer out = new StringBuffer();
	out.append("{");
	for (int i = 0; i < indices.length; i++) {
	    out.append(indices[i] + "=" + childIndices.get(indices[i]));
	    if (i < indices.length-1) out.append(", ");
	}
	out.append("}");
	return out.toString();
    }

    /**
     *  Gets a HashMap that maps string indices in
     *  parent.getOuterHTML() to the actual INode child objects
     */
    protected HashMap getChildNodeIndices(INode parent) {
	HashMap indices = new HashMap();
	NodeList children = parent.getChildNodes();
	int currInd = ((IDOMElement)parent).startTagHTML().length();
	for (int i = 0; i < children.getLength(); i++) {
	    IDOMElement currEl = (IDOMElement)children.item(i);
	    indices.put(new Integer(currInd), currEl);
	    currInd += currEl.getOuterHTML().length();
	}
	return indices;	
    }

    protected IDOMElement getClosestChildNode(HashMap childIndices, int matchedIndex) {
	if (childIndices.containsKey(new Integer(matchedIndex)))
	    return (IDOMElement)childIndices.get(new Integer(matchedIndex));

	Integer[] indices = (Integer[])childIndices.keySet().toArray(new Integer[0]);
	Arrays.sort(indices);
	int bestMatch = indices[0].intValue();
	int bestDiff = Math.abs(matchedIndex - bestMatch);
	for (int i = 1; i < indices.length; i++) {
	    int currDiff = Math.abs(matchedIndex - indices[i].intValue());
	    if (currDiff < bestDiff) {
		bestMatch = indices[i].intValue();
		bestDiff = currDiff;
	    }
	    else {
		break;		// array is sorted, so diff is monotonically decreasing, then increasing
	    }
	}
	
	return (IDOMElement)childIndices.get(new Integer(bestMatch));
    }

    public String toString() {
	return this.semanticClassFeature.toString();
    }

    public String toString(int depth, String indent) {
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < depth; i++) out.append(indent);
	out.append("LAPIS: ");
	out.append(this.semanticClassFeature.toString());
	out.append("\n");
	return out.toString();
    }


    /**
     *  Creates an RDF resource representing this matcher.
     */
    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource matcherRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(matcherRes, Constants.s_rdf_type, WrapperManager.MATCHER_CLASS));
	rdfc.add(new Statement(matcherRes, Constants.s_rdf_type, WrapperManager.LAPIS_MATCHER_CLASS));
	rdfc.add(new Statement(matcherRes,
			       WrapperManager.MATCHER_JAVA_CLASS_PROP,
			       new Literal(this.getClass().getName())));

	rdfc.add(new Statement(matcherRes,
			       WrapperManager.LAPIS_MATCHER_SEMANTIC_CLASS_FEATURE_PROP,
			       new Literal(this.semanticClassFeature.toString())));
		 

	Iterator iter = this.semanticPropertyFeatures.keySet().iterator();
	while (iter.hasNext()) {
	    Resource propAnon = Utilities.generateUniqueResource();
	    rdfc.add(new Statement(matcherRes,
				   WrapperManager.LAPIS_MATCHER_SEMANTIC_PROPERTY_PROP,
				   propAnon));
	    Resource propRes = (Resource)iter.next();
	    rdfc.add(new Statement(propAnon,
				   WrapperManager.LAPIS_MATCHER_SEMANTIC_PROPERTY_RESOURCE_PROP,
				   propRes));
	    rdfc.add(new Statement(propAnon,
				   WrapperManager.LAPIS_MATCHER_SEMANTIC_PROPERTY_FEATURE_PROP,
				   new Literal(this.semanticPropertyFeatures.get(propRes).toString())));
	}

	rdfc.add(new Statement(matcherRes,
			       WrapperManager.LAPIS_MATCHER_DOC_HTML_PROP,
			       new Literal(this.docHTML)));
	for (int i = 0; i < this.startChars.length; i++) {
	    Resource exRes = Utilities.generateUniqueResource();
	    rdfc.add(new Statement(matcherRes,
				   WrapperManager.LAPIS_MATCHER_EXAMPLE_PROP,
				   exRes));
	    rdfc.add(new Statement(exRes,
				   WrapperManager.LAPIS_MATCHER_START_CHAR_PROP,
				   new Literal(String.valueOf(this.startChars[i]))));
	    rdfc.add(new Statement(exRes,
				   WrapperManager.LAPIS_MATCHER_END_CHAR_PROP,
				   new Literal(String.valueOf(this.endChars[i]))));
	}

	return matcherRes;
    }

    public static Matcher fromResource(PatternNode patternNode, Resource matcherRes, IRDFContainer rdfc) throws RDFException {
	try {
	    TC semanticClassFeature = TC.make(rdfc.extract(matcherRes,
							   WrapperManager.LAPIS_MATCHER_SEMANTIC_CLASS_FEATURE_PROP,
							   null).getContent());

	    Set propResSet = rdfc.query(new Statement[] {new Statement(matcherRes,
								       WrapperManager.LAPIS_MATCHER_SEMANTIC_PROPERTY_PROP,
								       Utilities.generateWildcardResource(1))},
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1));

	    HashMap semanticPropertyFeatures = new HashMap();
	    if (propResSet != null) {
		Iterator iter = propResSet.iterator();
		while (iter.hasNext()) {
		    Resource propAnon = (Resource)((RDFNode[])iter.next())[0];
		    semanticPropertyFeatures.put((Resource)rdfc.extract(propAnon,
									WrapperManager.LAPIS_MATCHER_SEMANTIC_PROPERTY_RESOURCE_PROP,
									null),
						 TC.make(rdfc.extract(propAnon,
								      WrapperManager.LAPIS_MATCHER_SEMANTIC_PROPERTY_FEATURE_PROP,
								      null).getContent()));
		}
	    }

	    String docHTML = rdfc.extract(matcherRes,
					  WrapperManager.LAPIS_MATCHER_DOC_HTML_PROP,
					  null).getContent();
	    
	    Set exSet = rdfc.query(new Statement[] {new Statement(matcherRes,
								  WrapperManager.LAPIS_MATCHER_EXAMPLE_PROP,
								  Utilities.generateWildcardResource(1))},
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1));

	    int size = (exSet == null) ? 0 : exSet.size();
	    int[] startChars = new int[size];
	    int[] endChars = new int[size];
	    if (exSet != null) {
		Iterator iter = exSet.iterator();
		for (int i = 0; iter.hasNext(); i++) {
		    Resource exRes = (Resource)((RDFNode[])iter.next())[0];
		    startChars[i] = Integer.parseInt(rdfc.extract(exRes,
								  WrapperManager.LAPIS_MATCHER_START_CHAR_PROP,
								  null).getContent());
		    endChars[i] = Integer.parseInt(rdfc.extract(exRes,
								WrapperManager.LAPIS_MATCHER_END_CHAR_PROP,
								null).getContent());
		}
	    }

	    return new LapisMatcher(patternNode,
				    semanticPropertyFeatures,
				    semanticClassFeature,
				    docHTML,
				    startChars,
				    endChars);

	}
	catch (RuntimeException e) {
	    System.out.println("Error in TC.make() in LapisMatcher():\n");
	    e.printStackTrace();
	    return null;
	}

    }

    public boolean equals(Object other) {
	if (!(other instanceof LapisMatcher)) return false;
	return this.semanticClassFeature.toString().equalsIgnoreCase(((LapisMatcher)other).semanticClassFeature.toString());
    }

}
