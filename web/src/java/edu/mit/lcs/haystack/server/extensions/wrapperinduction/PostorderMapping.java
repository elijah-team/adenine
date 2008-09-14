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
import java.util.Arrays;
import java.util.HashSet;

/**
 *  Represents a mapping between two Tree objects.
 *
 *  @author Andrew Hogue
 */
public class PostorderMapping {

    protected ArrayList pairs;

    public PostorderMapping() {
	this.pairs = new ArrayList();
    }

    public PostorderMapping(PostorderPair[] pairs) {
	this.pairs = new ArrayList();
	for (int i = 0; i < pairs.length; i++) {
	    this.pairs.add(pairs[i]);
	}
    }

    /**
     *  Creates a new mapping with a single pair, with the given values
     */
    public PostorderMapping(int i, int j, int cost) {
	this(new PostorderPair[] {new PostorderPair(i, null, j, null, cost)});
    }

    /**
     *  Creates a new mapping with a single pair, with the given values
     */
    public PostorderMapping(int i, String iLabel, int j, String jLabel, int cost) {
	this(new PostorderPair[] {new PostorderPair(i, iLabel, j, jLabel, cost)});
    }

    /**
     *  Returns the cost of this mapping as per the static cost
     *  variables in TreeDistance.
     */
    public int getCost() {
	int cost = 0;
	for (int i = 0; i < pairs.size(); i++) {
	    cost += ((PostorderPair)pairs.get(i)).cost;
	}
	return cost;
    }

    /**
     *  Returns the normalized cost of the mapping, that is,
     *  (2*cost)/(t1.size + t2.size).
     */
    public float getNormalizedCost() {
	int maxI = 0;
	int maxJ = 0;
	for (int i = 0; i < pairs.size(); i++) {
	    if (this.getPair(i).i > maxI) maxI = this.getPair(i).i;
	    if (this.getPair(i).j > maxJ) maxJ = this.getPair(i).j;
	}

	return (float)(((float)(2*this.getCost())) /
		       ((float)(maxI+maxJ)));
    }

    /**
     *  Adds a mapping from T_1[i] to T_2[j]
     */
    public void add(int i, int j, int cost) {
	pairs.add(new PostorderPair(i, null, j, null, cost));
    }

    /**
     *  Adds a mapping from T_1[i] to T_2[j]
     */
    public void add(int i, String iLabel, int j, String jLabel, int cost) {
	pairs.add(new PostorderPair(i, iLabel, j, jLabel, cost));
    }

    /**
     *  Returns a new instacne of Mapping identical to this one
     *  with the given pair appended.
     */
    public PostorderMapping append(int i, int j, int cost) {
	return merge(this, new PostorderMapping(i, j, cost));
    }

    /**
     *  Returns a new instance of Mapping identical to this one
     *  with the given pair appended.
     */
    public PostorderMapping append(int i, String iLabel, int j, String jLabel, int cost) {
	return merge(this, new PostorderMapping(i, iLabel, j, jLabel, cost));
    }

    public PostorderPair getPair(int n) {
	if (n < 0 || n >= pairs.size()) return null;
	return (PostorderPair)this.pairs.get(n);
    }

    /**
     *  Returns the pairs contained in this mapping.
     */
    public PostorderPair[] getPairs() {
	return (PostorderPair[])this.pairs.toArray(new PostorderPair[0]);
    }

    public String toString() {
	StringBuffer out = new StringBuffer();

	out.append("Mapping (cost " + getCost() + "):\n");
	for (int i = 0; i < pairs.size(); i++) {
	    out.append("\t" + pairs.get(i) + "\n");
	}
	return out.toString();
    }

    /**
     *  Merges the two mappings, eliminating redundant pairs
     */
    public static PostorderMapping merge(PostorderMapping m1, PostorderMapping m2) {
	HashSet newpairs = new HashSet();
	PostorderPair[] pairs1 = m1.getPairs();
	for (int i = 0; i < pairs1.length; i++) {
	    newpairs.add(pairs1[i]);
	}
	PostorderPair[] pairs2 = m2.getPairs();
	for (int i = 0; i < pairs2.length; i++) {
	    newpairs.add(pairs2[i]);
	}

	PostorderPair[] toReturn = (PostorderPair[])newpairs.toArray(new PostorderPair[0]);
	Arrays.sort(toReturn);
	return new PostorderMapping(toReturn);
    }
    
    public boolean containsDeletions() {
	for (int i = 0; i < pairs.size(); i++) {
	    if (((PostorderPair)pairs.get(i)).j == -1) return true;
	}
	return false;
    }

    public boolean containsInsertions() {
	for (int i = 0; i < pairs.size(); i++) {
	    if (((PostorderPair)pairs.get(i)).i == -1) return true;
	}
	return false;
    }

}


