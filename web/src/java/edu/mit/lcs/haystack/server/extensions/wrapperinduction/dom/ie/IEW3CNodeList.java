package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class IEW3CNodeList implements NodeList {

    protected Element[] nodes;

    public IEW3CNodeList(Element[] nodes) {
	System.out.println("created IEW3CNodeList with " + nodes.length + " elements: ");
	for (int i = 0; i < nodes.length; i++) {
	    System.out.println("\t" + ((IEW3CElement)nodes[i]).ancestorsToString());
	}
	this.nodes = nodes;
    }

    public int getLength() {
	return nodes.length;
    }

    public Node item(int index) {
	return nodes[index];
    }

}
