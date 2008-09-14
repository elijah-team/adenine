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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction.suffixtree;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.DocumentImpl;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ElementImpl;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ITree;

/**
 *  Test trees for use with the SuffixTree class.
 *
 *  @author Andrew Hogue
 */
public class TestTrees {

    public static HashMap trees;

    static {
	trees = new HashMap();

	trees.put("google", new DocumentImpl(new ElementImpl("HTML")
					 .appendChild(new ElementImpl("HEAD")
						   .appendChild(new ElementImpl("TITLE")))
					 .appendChild(new ElementImpl("BODY")
						   .appendChild(new ElementImpl("DIV")
							     .appendChild(new ElementImpl("P")
								       .appendChild(new ElementImpl("A")
										 .appendChild(new ElementImpl("\"text1\"")))
								       .appendChild(new ElementImpl("BR"))
								       .appendChild(new ElementImpl("FONT")
										 .appendChild(new ElementImpl("B")
											   .appendChild(new ElementImpl("\"text2\"")))
										 .appendChild(new ElementImpl("\"text3\""))
										 .appendChild(new ElementImpl("FONT")
											   .appendChild(new ElementImpl("\"Cached\"")))))
							     .appendChild(new ElementImpl("BLOCKQUOTE")
								       .appendChild(new ElementImpl("P")
										 .appendChild(new ElementImpl("A")
											   .appendChild(new ElementImpl("\"text4\"")))
										 .appendChild(new ElementImpl("BR"))
										 .appendChild(new ElementImpl("FONT")
											   .appendChild(new ElementImpl("text5"))
											   .appendChild(new ElementImpl("B")
												     .appendChild(new ElementImpl("\"text6\"")))
											   .appendChild(new ElementImpl("\"text7\""))
											   .appendChild(new ElementImpl("FONT")
												     .appendChild(new ElementImpl("\"Cached\""))))))))));

	trees.put("imdb", new DocumentImpl(new ElementImpl("HTML")
				       .appendChild(new ElementImpl("HEAD")
						 .appendChild(new ElementImpl("TITLE")))
				       .appendChild(new ElementImpl("BODY")
						 .appendChild(new ElementImpl("TABLE")
							   .appendChild(new ElementImpl("TR")
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"Cast\""))))
							   .appendChild(new ElementImpl("TR")
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("A")
											 .appendChild(new ElementImpl("\"text1\""))))
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"...\"")))
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"text2\""))))
							   .appendChild(new ElementImpl("TR")
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("A")
											 .appendChild(new ElementImpl("\"text3\""))))
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"...\"")))
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"text4\""))))
							   .appendChild(new ElementImpl("TR")
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("A")
											 .appendChild(new ElementImpl("\"text5\""))))
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"...\"")))
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("\"text6\""))))
							   .appendChild(new ElementImpl("TR")
								     .appendChild(new ElementImpl("TD")
									       .appendChild(new ElementImpl("A")
											 .appendChild(new ElementImpl("\"(more)\"")))))))));
	
	trees.put("nytimes", new DocumentImpl(new ElementImpl("HTML")
					  .appendChild(new ElementImpl("HEAD")
						    .appendChild(new ElementImpl("TITLE")))
					  .appendChild(new ElementImpl("BODY")
						    .appendChild(new ElementImpl("TABLE")
							      .appendChild(new ElementImpl("TR")
									.appendChild(new ElementImpl("TD")
										  .appendChild(new ElementImpl("A")
											    .appendChild(new ElementImpl("\"text1\"")))
										  .appendChild(new ElementImpl("BR"))
										  .appendChild(new ElementImpl("FONT")
											    .appendChild(new ElementImpl("\"text2\"")))
										  .appendChild(new ElementImpl("BR"))
										  .appendChild(new ElementImpl("FONT")
											    .appendChild(new ElementImpl("\"text3\"")))
										  .appendChild(new ElementImpl("BR"))
										  .appendChild(new ElementImpl("A")
											    .appendChild(new ElementImpl("\"text4\"")))
										  .appendChild(new ElementImpl("BR"))
										  .appendChild(new ElementImpl("FONT")
											    .appendChild(new ElementImpl("\"text5\"")))
										  .appendChild(new ElementImpl("BR"))
										  .appendChild(new ElementImpl("FONT")
											    .appendChild(new ElementImpl("\"text6\"")))
										  .appendChild(new ElementImpl("BR"))))))));

	trees.put("subtrees", new DocumentImpl(new ElementImpl("A")
					   .appendChild(new ElementImpl("B")
						     .appendChild(new ElementImpl("C")
							     .appendChild(new ElementImpl("\"v1\"")))
						     .appendChild(new ElementImpl("\"v2\""))
						     .appendChild(new ElementImpl("D")
							     .appendChild(new ElementImpl("E")
								       .appendChild(new ElementImpl("\"v3\"")))))
					   .appendChild(new ElementImpl("B")
						     .appendChild(new ElementImpl("\"v4\""))
						     .appendChild(new ElementImpl("C")
							     .appendChild(new ElementImpl("\"v5\"")))
						     .appendChild(new ElementImpl("\"v6\""))
						     .appendChild(new ElementImpl("D")
							     .appendChild(new ElementImpl("E")
								       .appendChild(new ElementImpl("\"v7\"")))))
					   .appendChild(new ElementImpl("F")
						     .appendChild(new ElementImpl("B")
							       .appendChild(new ElementImpl("C")
									 .appendChild(new ElementImpl("\"v8\"")))
							       .appendChild(new ElementImpl("\"v9\""))
							       .appendChild(new ElementImpl("D")
									 .appendChild(new ElementImpl("E")
										   .appendChild(new ElementImpl("\"v10\""))))))));


	trees.put("siblings", new DocumentImpl(new ElementImpl("A")
					   .appendChild(new ElementImpl("B")
						     .appendChild(new ElementImpl("C")))
					   .appendChild(new ElementImpl("D")
						     .appendChild(new ElementImpl("E")
							       .appendChild(new ElementImpl("\"v1\"")))
						     .appendChild(new ElementImpl("F"))
						     .appendChild(new ElementImpl("G")
							       .appendChild(new ElementImpl("\"v2\"")))
						     .appendChild(new ElementImpl("\"v3\""))
						     .appendChild(new ElementImpl("E")
							       .appendChild(new ElementImpl("\"v4\"")))
						     .appendChild(new ElementImpl("F"))
						     .appendChild(new ElementImpl("G")
							       .appendChild(new ElementImpl("\"v5\"")))
						     .appendChild(new ElementImpl("\"v6\""))
						     .appendChild(new ElementImpl("E")
							       .appendChild(new ElementImpl("\"v7\"")))
						     .appendChild(new ElementImpl("F"))
						     .appendChild(new ElementImpl("G")
							       .appendChild(new ElementImpl("\"v8\""))))));


	// see notes of 2003-12-10 20:47
	trees.put("simple", new DocumentImpl(new ElementImpl("A")
					 .appendChild(new ElementImpl("B")
						   .appendChild(new ElementImpl("A")
							     .appendChild(new ElementImpl("B"))
							     .appendChild(new ElementImpl("C"))))
					 .appendChild(new ElementImpl("C")
						   .appendChild(new ElementImpl("A")
							     .appendChild(new ElementImpl("B"))
							     .appendChild(new ElementImpl("E"))))));


	// see notes of 2004-01-12 19:34
	trees.put("simple2", new DocumentImpl(new ElementImpl("A")
					  .appendChild(new ElementImpl("B")
						    .appendChild(new ElementImpl("C"))
						    .appendChild(new ElementImpl("D")))
					  .appendChild(new ElementImpl("B")
						    .appendChild(new ElementImpl("C"))
						    .appendChild(new ElementImpl("D")))));
    }


    public static ITree getTree(String key) {
	return (ITree)trees.get(key);
    }

    /**
     *  Retrieves the nodes of the requested tree in postorder.
     */
    public static INode[] getNodesPostorder(String key) {
	if (!trees.containsKey(key)) return new INode[0];
	return (INode[])postorderHelper((INode)((ITree)trees.get(key)).getDocumentElement(), new ArrayList()).toArray(new INode[0]);
    }

    protected static ArrayList postorderHelper(INode current, ArrayList nodes) {
	NodeList children = current.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
	    postorderHelper((INode)children.item(i), nodes);
	}
	nodes.add(current);
	return nodes;
    }

    /**
     *  Retrieves the nodes of the requested tree using an inorder traversal.
     */
    public static INode[] getNodesInorder(String key) {
	if (!trees.containsKey(key)) return new INode[0];
	return (INode[])inorderHelper((INode)((ITree)trees.get(key)).getDocumentElement(), new ArrayList()).toArray(new INode[0]);
    }

    protected static ArrayList inorderHelper(INode current, ArrayList nodes) {
	NodeList children = current.getChildNodes();

	if (children.getLength() > 0) {
	    inorderHelper((INode)children.item(0), nodes);
	}

	nodes.add(current);

	for (int i = 1; i < children.getLength(); i++) {
	    inorderHelper((INode)children.item(i), nodes);
	}

	return nodes;
    }

    /**
     *  Retrieves the nodes of the requested tree in preorder.
     */
    public static INode[] getNodesPreorder(String key) {
	if (!trees.containsKey(key)) return new INode[0];
	return (INode[])preorderHelper((INode)((ITree)trees.get(key)).getDocumentElement(), new ArrayList()).toArray(new INode[0]);
    }

    protected static ArrayList preorderHelper(INode current, ArrayList nodes) {
	NodeList children = current.getChildNodes();
	nodes.add(current);
	for (int i = 0; i < children.getLength(); i++) {
	    preorderHelper((INode)children.item(i), nodes);
	}
	return nodes;
    }

    /**
     *  Retrieves the nodes of the requested tree in a depth-first
     *  preorder, but adds all children to the array <i>before</i>
     *  recursing.
     */
    public static INode[] getNodesPreorderAll(String key) {
	if (!trees.containsKey(key)) return new INode[0];
	return (INode[])preorderAllHelper((INode)((ITree)trees.get(key)).getDocumentElement(), new ArrayList()).toArray(new INode[0]);
    }

    protected static ArrayList preorderAllHelper(INode current, ArrayList nodes) {
	NodeList children = current.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
	    nodes.add(children.item(i));
	}
	for (int i = 0; i < children.getLength(); i++) {
	    preorderAllHelper((INode)children.item(i), nodes);
	}
	return nodes;
    }


    /**
     *  Retrieves the nodes of the requested tree in breadth first order
     */
    public static INode[] getNodesBFS(String key) {
	if (!trees.containsKey(key)) return new INode[0];
	
	ArrayList queue = new ArrayList();
	queue.add(((ITree)trees.get(key)).getDocumentElement());
	
	ArrayList nodes = new ArrayList();
	while (!queue.isEmpty()) {
	    INode current = (INode)queue.remove(0);
	    nodes.add(current);

	    NodeList children = current.getChildNodes();
	    for (int i = 0; i < children.getLength(); i++) {
		queue.add((INode)children.item(i));
	    }	    
	}

	return (INode[])nodes.toArray(new INode[0]);
    }

    /**
     *  Retrieves all paths from root to leaf in the requested tree.
     *  If labelEdges is true, will also include the sibling number in
     *  the label of the nodes.
     */
    public static INode[][] getRootToLeafPaths(String key, boolean labelEdges) {
	if (!trees.containsKey(key)) return new INode[0][0];
	ArrayList initPath = new ArrayList();
	initPath.add((INode)((ITree)trees.get(key)).getDocumentElement());
	return (INode[][])rootToLeafHelper((INode)((ITree)trees.get(key)).getDocumentElement(), initPath, labelEdges).toArray(new INode[0][]);
    }

    public static ArrayList rootToLeafHelper(INode current, ArrayList currPath, boolean labelEdges) {
	// assumes that <current> has already been added to currPath
	NodeList children = current.getChildNodes();
	ArrayList allPaths = new ArrayList();
	if (children.getLength() == 0) {
	    allPaths.add((INode[])currPath.toArray(new INode[0]));
	}
	else {
	    for (int i = 0; i < children.getLength(); i++) {
		ArrayList pathClone = (ArrayList)currPath.clone();
		if (labelEdges) pathClone.add(new ElementImpl(String.valueOf(i)));
		pathClone.add(children.item(i));
		
		allPaths.addAll(rootToLeafHelper((INode)children.item(i), pathClone, labelEdges));
	    }
	}

	return allPaths;
    }
    
    /**
     *  Retrieves all paths from root to leaf in the requested tree.
     */
    public static INode[][] getLeafToRootPaths(String key) {
	if (!trees.containsKey(key)) return new INode[0][0];
	return (INode[][])leafToRootHelper((INode)((ITree)trees.get(key)).getDocumentElement(), new ArrayList()).toArray(new INode[0][]);
    }

    // every path needs a unique start node???
    public static ArrayList leafToRootHelper(INode current, ArrayList currPath) {
	// assumes that <current> has not already been added to currPath
	ArrayList path = (ArrayList)currPath.clone();
	path.add(0, current);

	NodeList children = current.getChildNodes();
	ArrayList allPaths = new ArrayList();
	if (children.getLength() == 0) {
	    allPaths.add((INode[])path.toArray(new INode[0]));
	}
	else {
	    for (int i = 0; i < children.getLength(); i++) {
		allPaths.addAll(leafToRootHelper((INode)children.item(i), path));
	    }
	}

	return allPaths;
    }
    
}


