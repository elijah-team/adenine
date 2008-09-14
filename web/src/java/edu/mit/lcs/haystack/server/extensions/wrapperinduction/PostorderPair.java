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

/**
 *  Represents a pair of ints, as used in a Mapping object, with a cost.
 *
 *  @author Andrew Hogue
 */
public class PostorderPair implements Comparable {

    public int i;
    public int j;
    public String iLabel;
    public String jLabel;
    public int cost;

    public PostorderPair(int i, String iLabel, int j, String jLabel, int cost) {
	this.i = i;
	this.iLabel = iLabel;
	this.j = j;
	this.jLabel = jLabel;
	this.cost = cost;
    }

    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append("{");
	if (iLabel != null && !iLabel.equals("")) 
	    out.append(iLabel + "(" + i + ")");
	else
	    out.append(i);
	out.append(" -> ");
	if (jLabel != null && !jLabel.equals("")) 
	    out.append(jLabel + "(" + j + ")");
	else
	    out.append(j);
	out.append("} (cost: " + cost + ")");	
	return out.toString();
    }

    public boolean equals(Object o) {
	if (!(o instanceof PostorderPair)) return false;
	PostorderPair other = (PostorderPair)o;
	return ((this.i == other.i) && (this.j == other.j));
    }

    public int compareTo(Object o) {
	if (!(o instanceof PostorderPair)) throw new ClassCastException("Tried to compare a PostorderPair with something else!");
	PostorderPair p = (PostorderPair)o;

	if (i >= 0 && p.i >= 0) return i - p.i;
	if (j >= 0 && p.j >= 0) return j - p.j;
	if (i > 0) return i - p.j;
	return j - p.i;
    }
    
}

