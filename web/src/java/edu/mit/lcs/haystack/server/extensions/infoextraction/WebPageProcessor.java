/*
 * Created on Jul 22, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.io.PrintStream;
import java.util.Vector;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 *
 * Code for processing a webpage
 **/
public class WebPageProcessor {
    private IDOMDocument doc;
    private INode root;
    private boolean debug = false;
    public WebPageProcessor(IDOMElement doc) {
        System.err.println("doc: "+doc);
        this.root = cloneTree((INode) doc);
        System.err.println("doc.height():" + this.root.getHeight());
    }

    public INode getRoot() {
        return root;
    }
    
    public void printTree(PrintStream io) {
        io.println(generateHTML());
    }

    public StringBuffer generateHTML() {
        return generateHTML(root, " ",0);
    }
    
    public StringBuffer generateHTML(INode node, String indent, int depth) {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < depth; j++) {
            buf.append(indent);
        }
        //TODO[yks] print the attributes
        buf.append("<"+ node.getNodeName());
        
        NamedNodeMap attribs = node.getAttributes();
        for (int n = 0; n < attribs.getLength(); n++) {
            Node attrib = attribs.item(n);
            String name = attrib.getNodeName();
            if ( name != null) {
                //System.err.println("node: "+ node.getNodeName() + " attrib:"+name + " type:" + attrib.getNodeType());
                String val = node.getNodeValue();
                short type = node.getNodeType();
                if (type == Node.ATTRIBUTE_NODE && val != null) {
                        buf.append(" " + name + "=");
                        buf.append("\""+ val +"\"");                
                   
                }
            }
        }

        buf.append(">\n");
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            INode child = (INode) children.item(i);
            if (child.getNodeType() == INode.TEXT_NODE ||
    			child.getNodeName().equalsIgnoreCase("href") ||
    			child.getNodeName().equalsIgnoreCase("src") ) {
                /* ignore these nodes */
            } else {
                buf.append( generateHTML(child, indent, depth+1) );
            }
        }
        for (int j = 0; j < depth; j++) {
            buf.append(indent);
        }
        buf.append("</"+node.getNodeName()+">\n");
        return buf;
    }

    public void removeTextNodes() {
		System.err.println("removeTextNodes: ");
        root = removeTextNodes(root);
    }
    
    private INode removeTextNodes(INode tree) {
        
		if (tree.getNodeType() == INode.TEXT_NODE ||
		    tree.getNodeName().equalsIgnoreCase("href") ||
			tree.getNodeName().equalsIgnoreCase("src")) {
		    return null;
		}
		NodeList children = tree.getChildNodes();
		INode clonedNode = (INode)tree.cloneNode(false);
		if (debug) {
		    System.err.println("<"+tree.getNodeName()+">" + " children: "+ children.getLength() + ":"+clonedNode.getChildNodes().getLength());
		}		
		/* pair wise comparison of children? */
		for (int i = 0; i < children.getLength(); i++) {
			INode child = (INode) children.item(i);
			//clonedNode.removeChild(child);
			
			/* remove all text, href, and image src nodes */
			if (child.getNodeType() == INode.TEXT_NODE ||
			    child.getNodeName().equalsIgnoreCase("href") ||
			    child.getNodeName().equalsIgnoreCase("src") ) {
			    if (debug) {
			        System.err.println("removing: "+child.getNodeName());
			    }
			} else {
			    if (debug) {
			        System.err.println("adding: "+child.getNodeName());
			    }
			    child = removeTextNodes(child);
			    if (child != null) {
			        clonedNode.appendChild( child );
			    }
			}
		}
		if (debug) {
		    System.err.println("</"+tree.getNodeName()+">" + " children: "+ clonedNode.getChildNodes().getLength());
		}
		return clonedNode;		
    }
    
    private INode cloneTree(INode tree) {
        NodeList children = tree.getChildNodes();
		Vector clonedChildren = new Vector();
		INode clone;
		//System.err.println("cloneTree: " + tree.getNodeName());
		for (int i = 0; i < children.getLength(); i++) {
			INode child = (INode) children.item(i);
			if (child != null) {
			    String nodeName = child.getNodeName();
			    /* ignore style, script and text nodes */
			    if ((nodeName != null 
			         && !nodeName.equalsIgnoreCase("STYLE")
			         && !nodeName.equalsIgnoreCase("SCRIPT"))
			        && child.getNodeType() != INode.TEXT_NODE) {
			        
			        INode childClone = cloneTree(child);
			        clonedChildren.add(childClone);
			    }
			}
		}
		clone = (INode) tree.cloneNode(false);
		for (int i = 0; i < clonedChildren.size(); i++) {
			INode childClone = (INode) clonedChildren.get(i);
			clone.appendChild(childClone);
			childClone.setParent(clone);
		}
		return clone;
    }    
}
