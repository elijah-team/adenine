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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  The standard, non-range pattern matcher
 */
public class StandardMatcher extends Matcher {

    protected PatternNode patternNode;

    public StandardMatcher(PatternNode patternNode) {
	this.patternNode = patternNode;
    }

    /**
     *  Returns all matches in children of the given element.
     */
    public PatternResult match(INode parent, Resource semanticClassRes) {
	if (parent == null || !this.patternNode.equals(parent)) {
	    return new PatternResult(false);
	}

	// if this PatternNode is the root of the semantic class, we
	// pass along a unique resource so we can make statements
	// about it when we find its properties in the subtree below
	if (this.patternNode.hasSemanticResource(WrapperManager.PATTERN_SEMANTIC_ROOT_PROP)) {
	    semanticClassRes = Utilities.generateUniqueResource();
	}

	PatternResult childrenResult = matchHelper(0,
						   parent.getChildNodes(),
						   0,
						   (PatternNode[])this.patternNode.children.toArray(new PatternNode[0]),
						   semanticClassRes);
 
	if (childrenResult.isMatch()) {
	    if (this.patternNode.isSemantic()) {
		// add label to results
		childrenResult.add(semanticClassRes,
				   this.patternNode.getSemanticResources(),
				   parent);
	    }
	}

	return childrenResult;
    }

    /**
     *  Attempts to match the given pattern nodes to the given
     *  INode[], starting at the indices specified.  Assumes that
     *  PatternNode children at lower indices have already been
     *  matched to INodes at lower indices.
     *
     *  Matches as many times as possible.  E.g. the PatternNode[]
     *  children "a,b,c" would match the INodes "a,b,b,b,c" 3 times.
     */
    /*
      returns any matches from this level and down
    */
    protected PatternResult matchHelper(int toMatchIndex,
					NodeList toMatch,
					int patternIndex,
					PatternNode[] patternNodes,
					Resource semanticClassRes) {
	if (patternNodes.length == 0) {
	    return new PatternResult(true);
	}

	// allow a single wildcard pattern node to match nothing
	if (toMatch.getLength() == 0 &&
	    patternNodes.length == 1 &&
	    patternNodes[0].isWildcard()) {
	    return new PatternResult(true);
	}

	PatternResult allResults = new PatternResult(false);
	for (int i = toMatchIndex; i < toMatch.getLength(); i++) {
	    PatternResult thisResult = patternNodes[patternIndex].match((INode)toMatch.item(i),
									semanticClassRes);
	    if (thisResult.isMatch()) {
		if (patternIndex == patternNodes.length-1) {
		    // complete match
		    allResults.merge(thisResult);
		}
		else {
		    // incomplete match - try to match the rest of the pattern
		    PatternResult rest = matchHelper(i+1,
						     toMatch,
						     patternIndex+1,
						     patternNodes,
						     semanticClassRes);
						     
		    // if we match the rest, then merge this result too.
		    if (rest.isMatch()) {
			allResults.merge(thisResult.merge(rest));
		    }
		}
	    }
	    if (patternIndex < patternNodes.length-1 &&
		patternNodes[patternIndex].isWildcard()) {
		// allow wildcards to match zero or 1 times (so don't
		// increment i)
		PatternResult rest = matchHelper(i,
						 toMatch,
						 patternIndex+1,
						 patternNodes,
						 semanticClassRes);
		if (rest.isMatch()) {
		    allResults.merge(rest);
		}
	    }
	}
	return allResults;
    }


    /**
     *  Creates an RDF resource representing this matcher.
     */
    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource matcherRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(matcherRes, Constants.s_rdf_type, WrapperManager.MATCHER_CLASS));
	rdfc.add(new Statement(matcherRes, Constants.s_rdf_type, WrapperManager.STANDARD_MATCHER_CLASS));
	rdfc.add(new Statement(matcherRes,
			       WrapperManager.MATCHER_JAVA_CLASS_PROP,
			       new Literal(this.getClass().getName())));
	return matcherRes;
    }

    public static Matcher fromResource(PatternNode patternNode, Resource matcherRes, IRDFContainer rdfc) throws RDFException {
	return new StandardMatcher(patternNode);
    }




}
