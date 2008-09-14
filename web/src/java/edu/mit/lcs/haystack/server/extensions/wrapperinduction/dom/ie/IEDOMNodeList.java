package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IEDOMNodeList implements NodeList {

    protected Element[] nodes;

    public IEDOMNodeList() {
	this.nodes = new Element[0];
    }

    public IEDOMNodeList(Element[] nodes) {
	this.nodes = nodes;
    }

    public int getLength() {
	return nodes.length;
    }

    public Node item(int index) {
	return nodes[index];
    }

}
