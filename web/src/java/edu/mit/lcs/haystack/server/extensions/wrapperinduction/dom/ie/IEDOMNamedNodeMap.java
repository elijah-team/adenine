package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import java.util.HashMap;
import java.util.Vector;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class IEDOMNamedNodeMap implements NamedNodeMap {
    protected Vector nodes;

    protected HashMap map;

    protected IEDOMNamedNodeMap() {
        this.map = new HashMap();
        this.nodes = new Vector();
    }

    public IEDOMNamedNodeMap(Vector nodes) {
        this();
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                setNamedItemNS((Node) nodes.get(i));
            }
        }
    }

    public int getLength() {
        return nodes.size();
    }

    public Node getNamedItem(String name) {
        return (Node) map.get(name);
    }

    public Node getNamedItemNS(String namespaceURI, String localName) {
        //TODO what is the proper way to treat name spaces?
        // I suppose this will do for now
        if (namespaceURI == null) {
            return getNamedItem(localName);
        } else {
            return (Node) map.get(namespaceURI + ":" + localName);
        }
    }

    public Node item(int index) {
        if (index < nodes.size()) {
            return (Node) nodes.get(index);
        } else {
            return null;
        }
    }

    public Node removeNamedItem(String name) {
        Node n = (Node) map.get(name);
        if (n != null) {
            map.remove(name);
            nodes.remove(n);
        }
        return n;
    }

    public Node removeNamedItemNS(String namespaceURI, String localName) {
        if (namespaceURI == null) {
            return removeNamedItem(localName);
        } else {
            String fullName = namespaceURI + ":" + localName;
            Node n = (Node) map.get(fullName);
            if (n != null) {
                map.remove(fullName);
                nodes.remove(n);
            }
            return n;
        }
    }

    public Node setNamedItem(Node arg) {
        // TODO: need to implement LocalName
        //String name = arg.getLocalName();
        String name = arg.getNodeName();

        Node old = (Node) map.get(name);
        if (old != null) {
            nodes.remove(old);
        }
        map.put(name, arg);
        nodes.add(arg);
        return old;
    }

    public Node setNamedItemNS(Node arg) {
        // TODO: need to implement LocalName
        //String name = arg.getLocalName();
        String name = arg.getNodeName();
        //String ns = arg.getNamespaceURI();
        String ns = null;

        if (ns == null) {
            return setNamedItem(arg);
        } else {
            String fullName = ns + ":" + name;
            Node old = (Node) map.get(fullName);
            if (old != null) {
                nodes.remove(old);
            }
            map.put(fullName, arg);
            nodes.add(arg);
            return old;
        }
    }
}