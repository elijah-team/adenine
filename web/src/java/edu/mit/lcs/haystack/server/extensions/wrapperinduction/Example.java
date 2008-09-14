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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ITree;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 *  Represents an example given by the user of semantic information
 *  located in a tree.
 */
public class Example {

    protected ITree tree;
    protected String url;
    protected NodeID selectionID;


    /**
     *  Creates an Example based on the given selection.
     */
    public Example(ITree tree,
		   String url,
		   DOMSelection selection) {
	this.tree = tree;
	this.url = url;
	try {
	    this.selectionID = selection.getNodeID();
	}
	catch (NodeIDException e) {
	    e.printStackTrace();
	}
    }	

    protected Example(ITree tree,
		      String url,
		      NodeID selectionID) {
	this.tree = tree;
	this.url = url;
	this.selectionID = selectionID;
    }

    public INode getRoot() {
	return (INode)this.tree.getDocumentElement();
    }

    public String getURL() {
	return this.url;
    }

    public NodeID getSelectionID() {
	return this.selectionID;
    }

    public String toString() {
	return "Example: " + selectionID;
    }

    public INode[] getSelectedNodes() {
	return this.selectionID.getNodes(this.tree);
    }

    public int getSize() {
	return this.selectionID.getNodeSize(this.tree);
    }

    public Resource makeResource(IRDFContainer rdfc) throws RDFException {
	Resource exampleRes = Utilities.generateUniqueResource();
	rdfc.add(new Statement(exampleRes, Constants.s_rdf_type, WrapperManager.EXAMPLE_CLASS));
	rdfc.add(new Statement(exampleRes,
			       WrapperManager.EXAMPLE_SELECTION_ID_PROP,
			       this.selectionID.makeResource(rdfc)));
	rdfc.add(new Statement(exampleRes,
			       WrapperManager.EXAMPLE_URL_PROP,
			       new Literal(this.url)));
	rdfc.add(new Statement(exampleRes,
			       WrapperManager.EXAMPLE_HTML_PROP,
			       new Literal(((IDOMElement)this.tree.getDocumentElement()).getOuterHTML())));
	return exampleRes;
    }


    public static Example fromResource(Resource exampleRes, IRDFContainer rdfc) throws RDFException {
	return new Example(WrapperManager.parseHTML(rdfc.extract(exampleRes,
								 WrapperManager.EXAMPLE_HTML_PROP,
								 null).getContent()),
			   rdfc.extract(exampleRes,
					WrapperManager.EXAMPLE_URL_PROP,
					null).getContent(),
			   NodeID.fromResource((Resource)rdfc.extract(exampleRes,
								      WrapperManager.EXAMPLE_SELECTION_ID_PROP,
								      null),
					       rdfc));


    }


}
