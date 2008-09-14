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

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 *  Represents a pair of INodes, as used in a Mapping object, with a cost.
 *
 *  @author Andrew Hogue
 */
public class Pair {

    public INode node1;
    public INode node2;
    public int cost;

    public Pair(INode node1, INode node2, int cost) {
	this.node1 = node1;
	this.node2 = node2;
	this.cost = cost;
    }

    public String toString() {
	StringBuffer out = new StringBuffer();
	out.append("{");
	if (node1 != null) 
	    out.append(node1.getTagName() + "(" + node1.getSiblingNo() + ")");
	else
	    out.append("null");
	out.append(" -> ");
	if (node2 != null)
	    out.append(node2.getTagName() + "(" + node2.getSiblingNo() + ")");
	else
	    out.append("null");
	out.append("} (cost: " + cost + ")");	
	return out.toString();
    }

    public boolean equals(Object o) {
	if (!(o instanceof Pair)) return false;
	Pair other = (Pair)o;
	return ((this.node1.equals(other.node1)) && (this.node2.equals(other.node2)));
    }

}

