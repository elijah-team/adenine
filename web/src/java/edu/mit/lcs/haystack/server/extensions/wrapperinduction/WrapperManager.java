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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.web.WebViewPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 *  
 *
 *  @author Andrew Hogue
 *  @author Ryan Manuel
 */
public class WrapperManager {

    /////////////////////////
    ///   RDF constants   ///
    /////////////////////////

    public static final String NAMESPACE = "http://haystack.lcs.mit.edu/schemata/wrapperinduction#";    

    /// Wrapped Page ///
    public static final Resource WRAPPED_PAGE_CLASS             = new Resource(NAMESPACE, "WrappedPage");

    /// Pattern ///
    public static final Resource PATTERN_CLASS                  = new Resource(NAMESPACE, "Pattern");
    public static final Resource PATTERN_URL_PROP               = new Resource(NAMESPACE, "url");
    public static final Resource PATTERN_MAX_SIZE_PROP          = new Resource(NAMESPACE, "maxSize");
    public static final Resource PATTERN_EXAMPLE_PROP           = new Resource(NAMESPACE, "example");
    public static final Resource PATTERN_PATTERN_ROOT_PROP      = new Resource(NAMESPACE, "patternRoot");
    public static final Resource PATTERN_SEMANTIC_ROOT_PROP     = new Resource(NAMESPACE, "semanticRoot");
    public static final Resource PATTERN_SEMANTIC_TYPE_PROP     = new Resource(NAMESPACE, "semanticClass");

    /// PatternNode ///
    public static final Resource PATTERN_NODE_CLASS                = new Resource(NAMESPACE, "PatternNode");
    public static final Resource PATTERN_NODE_TAG_NAME_PROP        = new Resource(NAMESPACE, "tagName");
    public static final Resource PATTERN_NODE_CHILD_NODE_PROP      = new Resource(NAMESPACE, "childNode");
    public static final Resource PATTERN_NODE_SIBLING_NO_PROP      = new Resource(NAMESPACE, "siblingNo");
    public static final Resource PATTERN_NODE_IS_WILDCARD_PROP     = new Resource(NAMESPACE, "isWildcard");
    public static final Resource PATTERN_NODE_SEMANTIC_RESOURCE_PROP = new Resource(NAMESPACE, "semanticResource");
    public static final Resource PATTERN_NODE_COMPARATOR_PROP      = new Resource(NAMESPACE, "nodeComparator");
    public static final Resource PATTERN_NODE_MATCHER_PROP         = new Resource(NAMESPACE, "matcher");

    /// NodeComparator ///
    public static final Resource NODE_COMPARATOR_CLASS            = new Resource(NAMESPACE, "NodeComparator");
    public static final Resource NODE_COMPARATOR_JAVA_CLASS_PROP  = new Resource(NAMESPACE, "javaClass");
    
    /// StringNodeComparator ///
    public static final Resource STRING_NODE_COMPARATOR_CLASS          = new Resource(NAMESPACE, "StringNodeComparator");
    public static final Resource STRING_NODE_COMPARATOR_TAG_NAME_PROP  = new Resource(NAMESPACE, "tagName");

    /// DOMNodeComparator ///
    public static final Resource DOM_NODE_COMPARATOR_CLASS          = new Resource(NAMESPACE, "DOMNodeComparator");
    public static final Resource DOM_NODE_COMPARATOR_TAG_NAME_PROP  = new Resource(NAMESPACE, "tagName");
    public static final Resource DOM_NODE_COMPARATOR_TEXT_PROP      = new Resource(NAMESPACE, "nodeText");


    /// Matcher ///
    public static final Resource MATCHER_CLASS             = new Resource(NAMESPACE, "Matcher");
    public static final Resource MATCHER_JAVA_CLASS_PROP   = new Resource(NAMESPACE, "javaClass");
    
    /// StandardMatcher ///
    public static final Resource STANDARD_MATCHER_CLASS    = new Resource(NAMESPACE, "StandardMatcher");

    /// LapisMatcher ///
    public static final Resource LAPIS_MATCHER_CLASS                = new Resource(NAMESPACE, "LapisMatcher");
    public static final Resource LAPIS_MATCHER_SEMANTIC_CLASS_PROP  = new Resource(NAMESPACE, "semanticClass");
    public static final Resource LAPIS_MATCHER_SEMANTIC_CLASS_FEATURE_PROP  = new Resource(NAMESPACE, "semanticClassFeature");
    public static final Resource LAPIS_MATCHER_SEMANTIC_PROPERTY_PROP  = new Resource(NAMESPACE, "semanticProperty");
    public static final Resource LAPIS_MATCHER_SEMANTIC_PROPERTY_RESOURCE_PROP  = new Resource(NAMESPACE, "semanticPropertyRes");
    public static final Resource LAPIS_MATCHER_SEMANTIC_PROPERTY_FEATURE_PROP  = new Resource(NAMESPACE, "semanticPropertyFeature");
    public static final Resource LAPIS_MATCHER_DOC_HTML_PROP  = new Resource(NAMESPACE, "docHTML");
    public static final Resource LAPIS_MATCHER_EXAMPLE_PROP  = new Resource(NAMESPACE, "example");
    public static final Resource LAPIS_MATCHER_START_CHAR_PROP  = new Resource(NAMESPACE, "startChar");
    public static final Resource LAPIS_MATCHER_END_CHAR_PROP  = new Resource(NAMESPACE, "endChar");


    /// Example ///
    public static final Resource EXAMPLE_CLASS               = new Resource(NAMESPACE, "Example");
    public static final Resource EXAMPLE_SELECTION_ID_PROP   = new Resource(NAMESPACE, "selectionID");
    public static final Resource EXAMPLE_SEMANTIC_RESOURCE_PROP = new Resource(NAMESPACE, "semanticResource");
    public static final Resource EXAMPLE_URL_PROP            = new Resource(NAMESPACE, "url");
    public static final Resource EXAMPLE_HTML_PROP           = new Resource(NAMESPACE, "html");
    public static final Resource EXAMPLE_ROOT_NODE_PROP      = new Resource(NAMESPACE, "rootNode");
    
    /// ExampleNode ///
    public static final Resource EXAMPLE_NODE_CLASS            = new Resource(NAMESPACE, "ExampleNode");
    public static final Resource EXAMPLE_NODE_TAG_NAME_PROP    = new Resource(NAMESPACE, "tagName");
    public static final Resource EXAMPLE_NODE_CHILD_NODE_PROP  = new Resource(NAMESPACE, "childNode");
    public static final Resource EXAMPLE_NODE_SIBLING_NO_PROP  = new Resource(NAMESPACE, "siblingNo");
    public static final Resource EXAMPLE_NODE_COMPARATOR_PROP  = new Resource(NAMESPACE, "nodeComparator");

    /// NodeID ///
    public static final Resource NODE_ID_CLASS                  = new Resource(NAMESPACE, "NodeID");
    public static final Resource NODE_ID_SIBLING_NOS_PROP       = new Resource(NAMESPACE, "siblingNos");
    public static final Resource NODE_ID_RANGE_PROP             = new Resource(NAMESPACE, "range");

    /// Range ///
    public static final Resource RANGE_CLASS            = new Resource(NAMESPACE, "Range");
    public static final Resource RANGE_START_PROP       = new Resource(NAMESPACE, "start");
    public static final Resource RANGE_END_PROP         = new Resource(NAMESPACE, "end");


    /////////////////////////

    public static final String URL_IDENTIFIER = new String("href");
    public static final String URL_TAGNAME = new String("A");
    public static final String SRC_IDENTIFIER = new String("src");
    public static final String SRC_TAGNAME = new String ("IMG");
    
    protected static PatternChooser chooser;

    static {
	chooser = new PatternChooser();
    }

    /**
     *  Creates a pattern from the given example
     */
    public static Resource createPattern(IRDFContainer rdfc,
					 WebViewPart webView,
					 Resource semanticClass,
					 RDFNode wrapperName,
					 String maxExampleSize,
					 String costLimit) throws RDFException {
	try {
	    Example example = makeExample(webView);
	    if (example == null) return null;
	    Pattern p = new Pattern(example,
				    Integer.parseInt(maxExampleSize),
				    Float.parseFloat(costLimit));
	    System.out.println("Setting semanticClass " + semanticClass);
	    p.setSemanticType(semanticClass);
	    if (wrapperName != null) {
		if (wrapperName instanceof Literal) {
		    p.setWrapperName(wrapperName.getContent());
		}
		else {
		    RDFNode title = rdfc.extract((Resource)wrapperName,
						 Constants.s_dc_title,
						 null);
		    if (title != null) 
			p.setWrapperName(title.getContent());
		    else
			p.setWrapperName(wrapperName.getContent());
		}
	    }
	    Resource patternRes = p.makeResource(rdfc);
	    chooser.insertPattern(example.getURL(), patternRes);

	    Resource webPageResource = new Resource(example.getURL());
	    rdfc.add(webPageResource, Constants.s_rdf_type, WRAPPED_PAGE_CLASS);

	    if (webView != null) 
		webView.addMatch(patternRes, highlightPattern(p, webView));

	    return patternRes;
	}
	catch (PatternException e) {
	    e.printStackTrace();
	    return null;
	}
	catch (ExampleSizeOutOfBoundsException e) {
	    System.out.println(">>> " + e);
	    return null;
	}
    }

    /**
     *  Adds a semantic property to an existing pattern
     */
    public static Resource updatePatternProperty(IRDFContainer rdfc,
						 WebViewPart webView,
						 Resource patternRes,
						 Resource nodeIDRes,
						 Resource semanticResource,
						 RDFNode matchText,
						 RDFNode selectedText) throws RDFException, PatternException {
	if (webView != null) {
	    PatternResult res = webView.clearMatch(patternRes);
	    res.clearHighlightedElements(webView);
	}
	
	// clears the selection
	webView.getDOMBrowser().getDocument().getSelection(true);
	
	Pattern p = Pattern.fromResource(patternRes, rdfc);
	p.addProperty(NodeID.fromResource(nodeIDRes, rdfc),
		      semanticResource,
		      matchText.getContent(),
		      selectedText.getContent());
	patternRes = p.makeResource(rdfc);

	if (webView != null)
	    webView.addMatch(patternRes, highlightPattern(p, webView));

	return patternRes;
    }
				     
    public static Resource addPositiveExample(IRDFContainer rdfc,
					      WebViewPart webView,
					      Resource oldPatternRes,
					      String costLimit) throws RDFException {
	try {
	    Example example = makeExample(webView);
	    if (example == null) return null;

	    Pattern p = Pattern.fromResource(oldPatternRes, rdfc);

	    removeWrapper(rdfc, webView, oldPatternRes);

	    p.addPositiveExample(example,
				 Float.parseFloat(costLimit));

	    Resource patternRes = p.makeResource(rdfc);

	    if (webView != null) 
		webView.addMatch(patternRes, highlightPattern(p, webView));

	    return patternRes;
	}
	catch (PatternException e) {
	    e.printStackTrace();
	    return null;
	}
	catch (ExampleSizeOutOfBoundsException e) {
	    System.out.println(">>> " + e);
	    return null;
	}
    }

    /**
     *  Removes existing wrappers for the current page
     */
    public static void removeWrappers(IRDFContainer rdfc,
				      IPart webView,
				      Resource wrappedPage) throws RDFException {
	if (webView != null && webView instanceof WebViewPart) {
	    clearHighlightedElements((WebViewPart)webView);
	    ((WebViewPart)webView).clearMatches();
	}

	// remove any existing patterns
	Resource[] oldPatternRes = getPatternResources(rdfc, wrappedPage.getContent());
	if (oldPatternRes != null) {
	    System.out.println("in removeWrappers(): removing " + oldPatternRes.length + " patterns");
	    for (int i = 0; i < oldPatternRes.length; i++) {
		Pattern.removeResource(oldPatternRes[i], rdfc); 
	    }
	}

	rdfc.remove(new Statement(wrappedPage, 
				  Constants.s_rdf_type,
				  WRAPPED_PAGE_CLASS),
		    new Resource[0]);
    }

    /**
     *  Removes existing wrappers for the current page
     */
    public static void removeWrapper(IRDFContainer rdfc,
				     IPart webView,
				     Resource existingWrapper) throws RDFException {
	if (webView != null && webView instanceof WebViewPart) {
	    PatternResult res = ((WebViewPart)webView).clearMatch(existingWrapper);
	    if (res != null) 
		res.clearHighlightedElements((WebViewPart)webView);
	}

	Pattern p = Pattern.fromResource(existingWrapper, rdfc);

	System.out.println("in removeWrapper(): removing 1 pattern");
	Pattern.removeResource(existingWrapper, rdfc); 
    }


    /**
     *  Retreives all patterns stored in the given IRDFContainer for
     *  the given url.
     */
    public static Resource[] getPatternResources(IRDFContainer rdfc,
						 String originatingURL) throws RDFException {
	return chooser.getPatternResources(originatingURL, rdfc);
    }

//     /**
//      *  Retreives all patterns stored in the given IRDFContainer for
//      *  the given url.
//      */
//     public static Pattern[] getPatterns(IRDFContainer rdfc,
// 					String originatingURL) throws RDFException {
// 	Set patternSet = rdfc.query(new Statement[] {new Statement(Utilities.generateWildcardResource(1),
// 								   PATTERN_URL_PROP,
// 								   new Literal(getPatternURL(originatingURL)))},				    
// 				    Utilities.generateWildcardResourceArray(1),
// 				    Utilities.generateWildcardResourceArray(1));
// 	ArrayList patterns = new ArrayList();
// 	if (patternSet != null) {
// 	    System.out.println("Found " + patternSet.size() + " patterns for " + originatingURL);
// 	    Iterator iter = patternSet.iterator();
// 	    while (iter.hasNext()) {
// 		Pattern next = Pattern.fromResource((Resource)((RDFNode[])iter.next())[0], rdfc);
// 		if (next != null)
// 		    patterns.add(next);
// 	    }
// 	}
// 	return (Pattern[])patterns.toArray(new Pattern[0]);
//     }
    
    public static RDFNode[] getAttributeSelection(IRDFContainer rdfc,
    		WebViewPart webView, String tagName, String attName) throws NodeIDException, RDFException {
    	DOMSelection sel = webView.getDOMBrowser().getDocument().getSelection(false);
    	IDOMDocument doc = webView.getDOMBrowser().getDocument();
    	String originatingURL = webView.getDOMBrowser().getURL();
    	NodeID selectionID = sel.getNodeID();
    	
    	HashMap results = webView.getCurrentMatches();
    	Iterator patIter = results.keySet().iterator();
    	while (patIter.hasNext()) {
    	    Resource currPatRes = (Resource)patIter.next();
    	    PatternResult currResult = (PatternResult)results.get(currPatRes);
    	    NodeID[] matchedNodes = currResult.getMatches();
    	    for (int j = 0; j < matchedNodes.length; j++) {
    		if (matchedNodes[j].contains(selectionID)) {
    			IDOMElement selElt = null;
    			NodeID urlID = null;
    			String urlText = null;
    			NodeID urlSelectionID = null;
    			
    			if(sel.getParentElement().getTagName().equals(tagName))
    				selElt = sel.getParentElement();
    			else {
    				IDOMElement[] selElts = sel.getSelectedElements();
    				for(int i = 0; i < selElts.length; i++) {
    					if(selElts[i].getTagName().equals(tagName)) {
    						selElt = selElts[i];
    					}
    					else if(selElts[i].getNodeType() != Node.TEXT_NODE){
    						NodeList nl = selElts[i].getElementsByTagName(tagName);
    						if(nl.getLength() > 0) {
    							selElt = (IDOMElement) nl.item(0);
    							break;
    						}
    					}
    				}
    			}
    		
    			if(selElt == null)
    				return null;
    				
    			INode[] iNodeList = selElt.getChildren(attName);
    			for(int l = 0; l < iNodeList.length; l++) {
    				INode currElt = iNodeList[l];
    				urlSelectionID = currElt.getNodeID();
    				urlID = matchedNodes[j].makeRelativeID(urlSelectionID);
    				urlText = currElt.getChild(0).getTagName();
    			}
    			
    			if(urlID != null) {
    				return new RDFNode[] {
    						urlID.makeResource(rdfc),
    						new Literal(urlText)
    				};
    			}
    			else {
    				return null;
    			}	  
    		}
    	    }
    	}
    	return null;
    }
  
    /**
     *  Given the current document and selection, finds a pattern
     *  which contains the selection.  Returns two resources, the
     *  first containing the Pattern that is the context for the
     *  selection, the second containing the relative NodeID of the
     *  selection within the pattern.
     */
    public static RDFNode[] getPatternContext(IRDFContainer rdfc,
					       WebViewPart webView) throws RDFException, NodeIDException {
	DOMSelection sel = webView.getDOMBrowser().getDocument().getSelection(false);
	IDOMDocument doc = webView.getDOMBrowser().getDocument();
	String originatingURL = webView.getDOMBrowser().getURL();
	//	System.out.println("Got url " + originatingURL);
	NodeID selectionID = sel.getNodeID();
	System.out.println("Got property selection " + selectionID);
	
	HashMap results = webView.getCurrentMatches();
	Iterator patIter = results.keySet().iterator();
	while (patIter.hasNext()) {
	    Resource currPatRes = (Resource)patIter.next();
	    PatternResult currResult = (PatternResult)results.get(currPatRes);
	    NodeID[] matchedNodes = currResult.getMatches();
	    //	    System.out.println("getPatternContext() got " + matchedNodes.length + " matches");
	    for (int j = 0; j < matchedNodes.length; j++) {
		if (matchedNodes[j].contains(selectionID)) {
		    //		    System.out.println("Found context pattern rooted at " + matchedNodes[j] + ":\n" + currPatterns[i]);
		    // get relative NodeID in pattern
		    NodeID patternNodeID = matchedNodes[j].makeRelativeID(selectionID);
		    //		    System.out.println("WrapperManager.getPatternContext() got relative ID " + patternNodeID);

		    String matchText = matchedNodes[j].getText(doc);
		    String selectedText = selectionID.getText(doc);
		   		
			return new RDFNode[] {currPatRes,
				  patternNodeID.makeResource(rdfc),
				  new Literal(matchText),
				  new Literal(selectedText)};
				  
		}
	    }
	}
	return null;
    }
						  

    /**
     *  Creates a new Context for any semantic classes represented by
     *  the clicked element, and returns it.  If there are no semantic
     *  elements at the given element, simply returns the parent
     *  page's context.
     */
    // TODO: recursive context?  (need recursive classes in patterns first!)
    public static Context getClickContext(IRDFContainer rdfc,
					  WebViewPart webView,
					  IDOMElement clicked,
					  Context parentPageContext) throws RDFException {
	IDOMDocument doc = webView.getDOMBrowser().getDocument();
	Context clickedContext = parentPageContext;
	NodeID clickedID = clicked.getNodeID();
	//	System.out.println("Looking for context for " + clickedID);
	HashMap results = webView.getCurrentMatches();
	Iterator patIter = results.keySet().iterator();
	while (patIter.hasNext()) {
	    Resource currPatRes = (Resource)patIter.next();
	    PatternResult currResult = (PatternResult)results.get(currPatRes);

	    if (currResult == null) {
		System.out.println("currResult is null");
		continue;
	    }
	    NodeID[] matchedNodes = currResult.getMatches();
	    for (int j = 0; j < matchedNodes.length; j++) {
		if (matchedNodes[j].contains(clickedID)) {
		    clickedContext = createWrapperContext(rdfc,
							  parentPageContext,
							  currPatRes,
							  currResult,
							  matchedNodes[j],
							  doc);
		}
	    }
	}

	return clickedContext;
    }

    protected static Context createWrapperContext(IRDFContainer rdfc,
						  Context parentPageContext,
						  Resource patternRes,
						  PatternResult res,
						  NodeID matchedID,
						  IDOMDocument doc) throws RDFException {
	Context patternContext = new Context(parentPageContext);
	patternContext.putLocalProperty(OzoneConstants.s_underlying, patternRes);

	Context clickedContext = new Context(patternContext);
	
	Pattern currPattern = Pattern.fromResource(patternRes, rdfc);
	
	Resource clickedResource = WrapperManager.createResource(res, matchedID, currPattern, rdfc, doc);
	
	clickedContext.putLocalProperty(OzoneConstants.s_underlying, clickedResource);

	return clickedContext;
    }
    
    public static Resource createResource(PatternResult patRes, 
    		NodeID matchedID,
			Pattern pattern,
			IRDFContainer rdfc,
			IDOMDocument doc) throws RDFException {
    	Resource viewRes = patRes.getMatchResource(matchedID);
		if (pattern.getSemanticType() != null) {
			rdfc.add(new Statement(viewRes,
					Constants.s_rdf_type,
					pattern.getSemanticType()));
			
			Statement[] matchedStatements = patRes.getStatements(viewRes);
			
			boolean foundTitle = false;
			for (int j = 0; j < matchedStatements.length; j++) {
				if (matchedStatements[j].getPredicate().equals(Constants.s_dc_title))
					foundTitle = true;
				rdfc.add(matchedStatements[j]);
			}
			
			// if we didn't find the title, take the first
			// text element we can find with substantial text
			if (!foundTitle) {
				rdfc.add(new Statement(viewRes,
						Constants.s_dc_title,
						new Literal(WrapperManager.dcTitleHeuristic(matchedID,
								doc))));
			}
		}
		else {
			System.out.println("Got null semantic type for context");
		}
		
		return viewRes;
    }


    public static String dcTitleHeuristic(NodeID matchedID, IDOMDocument doc) {
    INode[] elts = matchedID.getNodes(doc);
    String matchedText = "";
    if(elts.length >= 0) {
	IDOMElement matchedNode = (IDOMElement)matchedID.getNodes(doc)[0];
	matchedText = matchedNode.getNodeText();
	if (matchedText.length() > 30)
	    matchedText = matchedText.substring(0,30) + "...";
    }
	return matchedText;
    }



//     public static String getPatternURL(String originalURL) {
// 	String patternURL = originalURL;
// 	if (patternURL.endsWith("/")) {
// 	    if (patternURL.indexOf("/", 8) != patternURL.length()-1) { // ignore the slashes in "http://"
// 		patternURL = patternURL.substring(0, patternURL.lastIndexOf("/", patternURL.length()-2)+1);
// 	    }
// 	}
// 	else {
// 	    if (patternURL.indexOf("?") != -1) {
// 		patternURL = patternURL.substring(0, patternURL.indexOf("?"));
// 	    }
// 	}
// 	return patternURL;
//     }

    public static Example makeExample(WebViewPart webView) throws RDFException {
	if (webView == null) return null;
	DOMSelection sel = webView.getDOMBrowser().getDocument().getSelection(true);
	if (!sel.isEmpty()) {
	    return new Example(webView.getDOMBrowser().getDocument(),
			       webView.getDOMBrowser().getURL(),
			       sel);
	}
	else {
	    return null;
	}
    }

    
    /**
     *  Retrieves an ordered list of Resources representing the
     *  children of the given node.
     */
    public static List getChildNodes(IRDFContainer rdfc, Resource patternNodeRes) throws RDFException {
	Set childrenSet = rdfc.query(new Statement[] {new Statement(patternNodeRes,
								    WrapperManager.PATTERN_NODE_CHILD_NODE_PROP,
								    Utilities.generateWildcardResource(1))},
				     Utilities.generateWildcardResourceArray(1),
				     Utilities.generateWildcardResourceArray(1));

	int size = (childrenSet == null) ? 0 : childrenSet.size();
	Resource[] childNodes = new Resource[size];
	if (childrenSet != null) {
	    Iterator iter = childrenSet.iterator();
	    while (iter.hasNext()) {
		Resource childNodeRes = (Resource)((RDFNode[])iter.next())[0];
		int siblingNo = Integer.parseInt(rdfc.extract(childNodeRes,
							      PATTERN_NODE_SIBLING_NO_PROP,
							      null).getContent());
		childNodes[siblingNo] = childNodeRes;
	    }
	}

	ArrayList children = new ArrayList(size);
	for (int i = 0; i < childNodes.length; i++) {
	    if (childNodes[i] == null) continue;
	    children.add(childNodes[i]);
	}
	return children;
    }

    public static PatternResult highlightPattern(Pattern pat, WebViewPart webView) {
	if (pat == null) return null;
	if (webView == null) return null;
	IDOMDocument doc = webView.getDOMBrowser().getDocument();
	PatternResult res = pat.match(doc);
	res.highlight(webView);
	return res;
    }

    public static void clearHighlightedElements(WebViewPart webView) {
	if (webView == null) return;
	IDOMDocument doc = webView.getDOMBrowser().getDocument();
	IDOMElement[] highlightedElements = doc.getHighlightedElements();
	for (int i = 0; i < highlightedElements.length; i++) {
	    highlightedElements[i].unhighlight();
	}
	doc.clearHighlightedElements();
    }

    public static IDOMDocument parseURL(String url) {
	if (System.getProperty("os.name").indexOf("Windows") == 0) {
	    try {
		return (IDOMDocument)CoreLoader.loadClass("edu.mit.lcs.haystack.ozone.web.InternetExplorer").getMethod("parseURL", new Class[] {Class.forName("java.lang.String")}).invoke(null, new Object[] {url});
	    } catch (Exception e) { e.printStackTrace(); }
	}
	else {
	    System.out.println(">>>> WrapperManager.parseURL(): Currently unsupported on non-Windows platforms");
	}
	return null;
    }

    public static IDOMDocument parseHTML(String html) {
	if (System.getProperty("os.name").indexOf("Windows") == 0) {
	    try {
		return (IDOMDocument)CoreLoader.loadClass("edu.mit.lcs.haystack.ozone.web.InternetExplorer").getMethod("parseHTML", new Class[] {Class.forName("java.lang.String")}).invoke(null, new Object[] {html});
	    } catch (Exception e) { e.printStackTrace(); }
	}
	else {
	    System.out.println(">>>> WrapperManager.parseURL(): Currently unsupported on non-Windows platforms");
	}
	return null;
    }

    /**
     *  Replaces newlines in an HTML string to ease in string
     *  matching.  (IE gives weird, nondeterministic newline
     *  characters in "getOuterHTML()")
     */
    public static String fixHTMLString(String in) {
	String[] split = in.split("[\n\r\f]");
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < split.length; i++) {
	    out.append(split[i]);
	}
	return out.toString();
    }



}


/**
 *  A class to maintain and retrieve the proper Pattern object based
 *  on a given URL.  Utilizes the prefix of the URL to decide if there
 *  is an appropriate Pattern.
 *
 *  Pretty inefficient implementation for now - may want to use Suffix
 *  Tree data structure if number of wrappers in system grows.
 */
class PatternChooser {

    /*
     *  A HashMap mapping URLs to ArrayLists of existing Pattern
     *  Resource objects which handle them.  
     */
    protected HashMap patternMap;

    public PatternChooser() {
    }

    protected void insertPattern(String url, Resource patternRes) {
	System.out.println("Inserting (" + url + ", " + patternRes + ")");
	ArrayList patterns =
	    (this.patternMap.containsKey(url)) ?
	    ((ArrayList)this.patternMap.get(url)) :
	    new ArrayList();
	patterns.add(patternRes);
	this.patternMap.put(url, patterns);
    }


    protected void initialize(IRDFContainer rdfc) {
	System.out.println(">>> Initializing pattern urls");
	this.patternMap = new HashMap();
	try {
	    Set patternSet = rdfc.query(new Statement[] {new Statement(Utilities.generateWildcardResource(1),
								       Constants.s_rdf_type,
								       WrapperManager.PATTERN_CLASS)},
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1));
	    if (patternSet != null) {
		System.out.println("Got " + patternSet.size() + " patterns");
		Iterator iter = patternSet.iterator();
		while (iter.hasNext()) {
		    Resource next = (Resource)((RDFNode[])iter.next())[0];
		    this.insertPattern(rdfc.extract(next,
						    WrapperManager.PATTERN_URL_PROP,
						    null).getContent(),
				       next);
		}
	    }
	}
	catch (RDFException e) {
	    e.printStackTrace();
	}
    }


    /**
     *  Retrieves Resource objects representing Patterns that operate
     *  on the given URL.  To make things easier, we split URLS on the
     *  characters {/, ?, =, &} and treat them as tokens when doing
     *  prefix comparisons.
     */
    public Resource[] getPatternResources(String url, IRDFContainer rdfc) {
	this.initialize(rdfc);
	String[] inTokens = getTokens(url);
	if (inTokens.length == 0) return new Resource[0];

	System.out.println("Got input tokens (" + url + "):");
	for (int i = 0; i < inTokens.length; i++)
	    System.out.println("\t\"" + inTokens[i] + "\"");	    
	
	Iterator urls = this.patternMap.keySet().iterator();
	ArrayList bestURLs = new ArrayList();
	int bestMatches = 0;
	while (urls.hasNext()) {
	    String currURL = (String)urls.next();
	    System.out.println("currURL: " + currURL);
	    String[] currTokens = getTokens(currURL);
	    if (currTokens.length == 0) continue;

	    System.out.println("bestURLs = " + bestURLs + ", bestMatches = " + bestMatches);
	    System.out.println("Got curr tokens (" + currURL + "):");
	    for (int i = 0; i < currTokens.length; i++)
		System.out.println("\t\"" + currTokens[i] + "\"");	    

	    // loop through tokens, counting how many are equal to the input tokens
	    int currMatches = 0;
	    for ( ;
		  currMatches < currTokens.length && currMatches < inTokens.length && 
		  currTokens[currMatches].equalsIgnoreCase(inTokens[currMatches]);
		  currMatches++) ;

	    // store the URL with the greatest number of matching tokens
	    if (currMatches > bestMatches) {
		bestURLs = new ArrayList();
		bestURLs.add(currURL);
		bestMatches = currMatches;
	    }
	    else if (currMatches == bestMatches) {
		bestURLs.add(currURL);
	    }
	}

	if (bestMatches == 0) 
	    return new Resource[0];
	else {
	    System.out.println("Found best urls " + bestURLs + " (matched " + bestMatches + " tokens)");
	    ArrayList resources = new ArrayList();
	    for (int i = 0; i < bestURLs.size(); i++) {
		resources.addAll((ArrayList)this.patternMap.get(bestURLs.get(i)));
	    }
	    return (Resource[])resources.toArray(new Resource[0]);
	}
    }


    protected String[] getTokens(String url) {
	if (url.startsWith("http://"))
	    url = url.substring(7);
	return url.split("[/?=&]");
    }

}





