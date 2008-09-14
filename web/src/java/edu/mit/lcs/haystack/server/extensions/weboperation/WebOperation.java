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

package edu.mit.lcs.haystack.server.extensions.weboperation;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.eclipse.FullScreenHostPart;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Pattern;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.PatternResult;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;

/**
 * @author Ryan Manuel
 */
public class WebOperation implements IWebOpDocCompleteListener {

	public final static String GET_OPERATION = "Get";
	public final static String POST_OPERATION = "Post";
	
	protected String actionURL;
	protected Resource initiatingPage;
	protected String headers;
	protected Hashtable parameters;
	protected String type;
	protected Resource webOperRes;
	protected String operName;
	protected IRDFContainer rdfc;
	protected Resource pat;
	protected Context context;
	protected Resource visibleRes;

	
	/**
	 * constructor for web operation
	 */
	public WebOperation(String url, 
			String headers, 
			String postData, 
			INode[] containingForms,
			Resource pat,
			Resource initPage) {
		this.headers = headers;
		this.webOperRes = null;
		this.rdfc = null;
		this.pat = pat;
		this.visibleRes = null;
		this.initiatingPage = initPage;
		
		// if the web operation is a get
		if(postData != null && postData.equals("")) {
			int index = url.indexOf("?");
			if(index == -1) {
				System.out.println(">>> There is a problem with the structure of the URL: " + url);
			}
			this.actionURL = url.substring(0, index);
			this.parameters = extractParameters(url.substring(index + 1));
			this.type = WebOperation.GET_OPERATION;
		}
		// if the web operation is a post
		else {
			this.actionURL = url;
			this.parameters = extractParameters(postData);
			this.type = WebOperation.POST_OPERATION;
		}
		
		// get the form that represents this operation
		INode form = getForm(containingForms);
		if(form != null) { 
			modifyParamsWithFormData(form);
		}
	}
	
	/**
	 * constructor for web operation that is called from fromResource
	 */
	
	protected WebOperation(Resource webOperRes,
			String operName,
			String actionURL, 
			String headers,
			String type,
			Hashtable parameters,
			Resource pat,
			Resource initiatingPage) {
		this.webOperRes = webOperRes;
		this.operName = operName;
		this.actionURL = actionURL;
		this.headers = headers;
		this.type = type;
		this.parameters = parameters;
		this.rdfc = null;
		this.pat = pat;
		this.visibleRes = null;
		this.initiatingPage = initiatingPage;
	}
	
	/**
	 * extracts information from the HTML of the form to add additional information about parameters
	 */
	public void modifyParamsWithFormData(INode form) {
		INode[] members = form.getPreorderNodes();

		// preorder nodes start at index 1
		for(int i = 1; i < members.length; i++) {
			String tagName = members[i].getTagName();
			String paramName = members[i].getAttribute("name");
			
			// if paramName is null, we know this isn't a node we care about
			if(paramName != null) {
				WebOpParameter param = (WebOpParameter) this.parameters.get(paramName);
				if(tagName.equalsIgnoreCase("input")) {
					String paramType = members[i].getAttribute("type");
					if(param != null) {
						if(paramType.equalsIgnoreCase("radio")) {
							// we treat radio inputs like select inputs
							param.setType("select");
							String paramValue = members[i].getAttribute("value");
							String paramDispName = extractRightText(members, i);
							param.addRangeValue(paramDispName, paramValue);
						}
						else if (paramType.equalsIgnoreCase("checkbox") || paramType.equalsIgnoreCase("hidden")){
							param.setType(paramType);
							
							// if the paramType is checkbox we attempt to extract the display name from the HTML
							if(paramType.equalsIgnoreCase("checkbox")) {
								String paramDispName = extractRightText(members, i);
								param.setDispName(paramDispName.toString());
							}
						}
						// we treat the submit inputs as hidden since there is no way to have separate submit methods
						else if (paramType.equals("submit")) {
							param.setType("hidden");
						}
						// this encompasses all other parameter types including file and password types
						else {
							param.setType("default");
						}
					}
					// if param is null, we don't know about it yet.  this could happen
					// if the user didn't check checkbox or select a radio input
					else {
						if(paramType.equalsIgnoreCase("checkbox")) {
							String paramValue = members[i].getAttribute("value");
							// default value to submit is "on"
							if(paramValue == null || paramValue.equals("")) {
								paramValue = "on";
							}
							param = new WebOpParameter(paramName, paramValue);
							param.setType(paramType);
							String paramDispName = extractRightText(members, i);
							param.setDispName(paramDispName.toString());
							this.parameters.put(paramName, param);
						}
						else if(paramType.equalsIgnoreCase("radio")) {
							param = new WebOpParameter(paramName, "");
							param.setType("select");
							String paramValue = members[i].getAttribute("value");
							String paramDispName = extractRightText(members, i);
							param.addRangeValue(paramDispName, paramValue);
							this.parameters.put(paramName, param);
						}
					}
				}
				else if(tagName.equalsIgnoreCase("select")) {
					param.setType("select");
					INode[] selectNodes = members[i].getPreorderNodes();
					
					//preorder nodes are 1 indexed
					for(int j = 1; j < selectNodes.length; j++) {
						INode option = selectNodes[j];
						if(option.getTagName().equalsIgnoreCase("option")) {
							String optionName = new String();
							// the display text is the child of the option node
							INode optionDispNode = selectNodes[j+1];
							String dispName = optionDispNode.getNodeValue();
							if(dispName == null) {
								dispName = "";
							}
							if(!option.hasAttribute("value")) {
								// if option doesn't have the attribute value, the system uses the display
								// name as the submit value
								optionName = dispName;
							}
							else {
								optionName = option.getAttribute("value");
							}
							param.addRangeValue(dispName, optionName);
						}
					}
				}
				
				// If param is not null then it is a parameter we are remembering
				if(param != null) {
					String rightText = new String();
					String leftText = new String();
					rightText = extractRightText(members, i);
					if(param.getLeftText().equals(""))
						leftText = extractLeftText(members, i);
					param.setRightText(rightText);
					param.setLeftText(leftText);
				}
			}
		}
	}
	
	/**
	 * extracts the text to the right of preOrderNodes[index]
	 */
	public String extractRightText(INode[] preOrderNodes, int index) {
		StringBuffer strReturn = new StringBuffer();
		int currIndex = index + 1;
		INode currNode = preOrderNodes[currIndex];
		
		// when extracting right text we get all of the text until the next input or
		// until the end of the form
		while(currIndex < preOrderNodes.length && 
				!currNode.getTagName().equalsIgnoreCase("select") &&
				!currNode.getTagName().equalsIgnoreCase("input")) {
			String currString = currNode.getNodeValue();
			if(currString != null && !currString.matches("\\p{Space}*")) {
				strReturn.append(currString + " ");
			}
			currIndex++;
			if(currIndex < preOrderNodes.length)
				currNode = preOrderNodes[currIndex];
		}		
		return strReturn.toString();
	}
	
	/**
	 * extracts the text to the left of preOrderNodes[index]
	 */
	public String extractLeftText(INode[] preOrderNodes, int index) {
		StringBuffer strReturn = new StringBuffer();
		int currIndex = index - 1;
		INode currNode = preOrderNodes[currIndex];
		
		// for the left text we only get the first text that we see before the current input
		while(currIndex >= 1 && 
				!currNode.getTagName().equalsIgnoreCase("select") &&
				!currNode.getTagName().equalsIgnoreCase("input") && 
				strReturn.toString().matches("\\p{Space}*")) {
			String currString = currNode.getNodeValue();
			if(currString != null && !currString.matches("\\p{Space}*")) {
				strReturn.insert(0, currString);
			}
			currIndex--;
			if(currIndex >= 1)
				currNode = preOrderNodes[currIndex];
		}		
		return strReturn.toString();
	}
	
	/**
	 * extracts the parameter names and values from parameterString
	 */
	public Hashtable extractParameters(String parameterString) {
		Hashtable returnSet = new Hashtable();
		String[] splitAnd = parameterString.split("&");
		for(int i = 0; i < splitAnd.length; i++) {
			String[] splitEquals = splitAnd[i].split("=");
			if(splitEquals.length == 2) {
				try {
					String name = splitEquals[0];
					String value = URLDecoder.decode(splitEquals[1], "UTF-8");
					WebOpParameter webOpParam = new WebOpParameter(name, value);
					returnSet.put(name, webOpParam);
				} catch (UnsupportedEncodingException e) {
					// This should not happen since UTF-8 is supported
					e.printStackTrace();
				}
			}
			else if(splitEquals.length == 1) {
				String name = splitEquals[0];
				String value = "";
				WebOpParameter webOpParam = new WebOpParameter(name, value);
				returnSet.put(name, webOpParam);
			}
			else {
				System.out.println("improperly formatted parameter string");
			}
		}
		return returnSet;
	}
	
	/**
	 * extracts the INode that is the root node for the tree that represents the 
	 * correct form for the operation
	 */	
	public INode getForm(INode[] forms) {
		int formsLength = forms.length;
		INode returnNode = null;
		boolean match = false;
		
		for(int i = 0; i < formsLength && !match; i++) {
			returnNode = forms[i];
			String actionAttr = returnNode.getAttribute("action");
			Hashtable copyParams = new Hashtable(this.parameters);
			if(actionAttr.equalsIgnoreCase(this.actionURL) ||
					actionURL.endsWith(actionAttr)) {
				match = true;
				INode[] allNodes = returnNode.getPreorderNodes();
				for(int j = 1; j < allNodes.length; j++) {
					String nameAttr = allNodes[j].getAttribute("name");
					if(nameAttr != null && copyParams.containsKey(nameAttr)) {
						copyParams.remove(nameAttr);
					}
				}
				if(!copyParams.isEmpty()) {
					match = false;
				}
			}
		}
		
		if(!match)
			returnNode = null;
		return returnNode;
	}
	
	/**
	 * creates a resource thar represents the web operation 
	 */
	public Resource makeResource(IRDFContainer rdfc) throws RDFException {
		if (this.webOperRes != null) return this.webOperRes;

		this.webOperRes = Utilities.generateUniqueResource();
		rdfc.remove(new Statement(this.webOperRes,
					  Constants.s_rdf_type,
					  Utilities.generateWildcardResource(1)),
			    Utilities.generateWildcardResourceArray(1));
		rdfc.add(new Statement(this.webOperRes, Constants.s_rdf_type, WebOpManager.WEB_OPERATION));
		rdfc.add(new Statement(this.webOperRes, Constants.s_rdf_type, WebOpManager.OPERATION));
		rdfc.add(new Statement(this.webOperRes, Constants.s_dc_title, new Literal(this.operName)));
		
		rdfc.add(new Statement(this.webOperRes,
				WebOpManager.OPERATION_PERFORM_NO_REQUIRED,
				new Literal("true")));
		rdfc.add(new Statement(this.webOperRes,
				WebOpManager.WEB_OPERATION_ACTION_URL,
				new Literal(this.actionURL)));
		rdfc.add(new Statement(this.webOperRes,
		      	WebOpManager.WEB_OPERATION_HEADERS,
		       	new Literal(this.headers)));
		rdfc.add(new Statement(this.webOperRes,
		      	WebOpManager.WEB_OPERATION_TYPE,
		       	new Literal(this.type)));
		rdfc.add(new Statement(this.webOperRes,
				WebOpManager.WEB_OPERATION_INIT_PAGE,
				this.initiatingPage));
		if(this.pat != null)
			rdfc.add(new Statement(this.webOperRes,
					WebOpManager.WEB_OPERATION_PATTERN,
					this.pat));
		
		Iterator parameter = parameters.values().iterator();
		while(parameter.hasNext()) {
			WebOpParameter visibleName = (WebOpParameter) parameter.next();
			Resource paramRes = visibleName.makeResource(rdfc);
			Resource paramNameRes = Resource.createUnique(rdfc, AdenineConstants.namedParameterSpec);
			rdfc.add(new Statement(paramNameRes,
					AdenineConstants.parameterName,
					paramRes));
			rdfc.add(new Statement(this.webOperRes,
					AdenineConstants.namedParameter,
					paramNameRes));
		}
		
		return this.webOperRes;
	}
	
	/**
	 * takes a resource that represents a web operation and creates a web operation object
	 */
	public static WebOperation fromResource(Resource webOperRes, IRDFContainer rdfc) throws RDFException {
		RDFNode rdfActionURL = rdfc.extract(webOperRes,
				WebOpManager.WEB_OPERATION_ACTION_URL,
				null);
		RDFNode rdfHeaders = rdfc.extract(webOperRes,
				WebOpManager.WEB_OPERATION_HEADERS,
				null);
		RDFNode rdfType = rdfc.extract(webOperRes,
				WebOpManager.WEB_OPERATION_TYPE,
				null);
		RDFNode operNameNode = rdfc.extract(webOperRes,
			    Constants.s_dc_title,
			    null);
		RDFNode pattNode = rdfc.extract(webOperRes,
				WebOpManager.WEB_OPERATION_PATTERN,
				null);
		RDFNode initPage = rdfc.extract(webOperRes,
				WebOpManager.WEB_OPERATION_INIT_PAGE,
				null);
		Resource pattRes = (pattNode == null) ? null : (Resource) pattNode;
		String operName = (operNameNode == null) ? null : operNameNode.getContent();
		
		Set parameterSet = rdfc.query(new Statement[] {new Statement(webOperRes,
				AdenineConstants.namedParameter,
				Utilities.generateWildcardResource(2)),
				new Statement(Utilities.generateWildcardResource(2),
						AdenineConstants.parameterName,
						Utilities.generateWildcardResource(1))},
				Utilities.generateWildcardResourceArray(1),
				Utilities.generateWildcardResourceArray(2));
		
		int size = (parameterSet == null) ? 0 : parameterSet.size();
		Hashtable parameters = new Hashtable();
		if (parameterSet != null) {
			Iterator iter = parameterSet.iterator();
			while (iter.hasNext()) {
				Resource curRes = (Resource) ((RDFNode[])iter.next())[0];
				WebOpParameter curParam = WebOpParameter.fromResource(curRes, rdfc);
				parameters.put(curParam.origName, curParam);
			}
		}
		else {
			System.out.println("parameterSet was null");
		}
		
		return new WebOperation(webOperRes,
				operName,
				rdfActionURL.getContent(),
				rdfHeaders.getContent(),
				rdfType.getContent(),
				parameters,
				pattRes,
				(Resource)initPage);
	}
	
	/**
	 * invokes the operation with the values specified in namedValues. the resulting document isn't returned
	 * right away.  must wait until the documentComplete event is received.
	 */
	public void invokeOperation(Map namedValues, IRDFContainer rdfc, Context context) throws RDFException {
		StringBuffer url = new StringBuffer(this.actionURL);
		String queryString = createQueryString(namedValues, rdfc);
		String postData = new String();
		String headers = this.headers;
		this.rdfc = rdfc;
		this.context = context;
		if(this.type.equals(WebOperation.GET_OPERATION)) {
			url.append("?" + queryString);
		}
		else {
			postData = queryString;
		}
		
		// create a resource that we can browse to if the wrapper extraction fails
		this.visibleRes = Utilities.generateUniqueResource();
		rdfc.add(new Statement(this.visibleRes, 
				Constants.s_rdf_type, 
				WebOpManager.VISIBLE_WEB_OPERATION));
		rdfc.add(new Statement(this.visibleRes, 
				WebOpManager.VISIBLE_WEB_OPERATION_HEADERS, 
				new Literal(headers)));
		rdfc.add(new Statement(this.visibleRes, 
				WebOpManager.VISIBLE_WEB_OPERATION_URL, 
				new Literal(url.toString())));
		rdfc.add(new Statement(this.visibleRes, 
				WebOpManager.VISIBLE_WEB_OPERATION_POSTDATA, 
				new Literal(postData)));
		
		if(this.pat == null) {
			FullScreenHostPart ofp = (FullScreenHostPart) this.context.getProperty(OzoneConstants.s_frame);
			ofp.requestViewing(this.visibleRes);			
		}
		else if (System.getProperty("os.name").indexOf("Windows") == 0) {
			try {
				InternetExplorer.invokeWebOperation(url.toString(), headers, postData, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		else {
			try {
				
			} catch (Exception e) {
				e.printStackTrace();
				e.getCause().printStackTrace();
			}
		}
	}
	
	/**
	 * creates the query string for the invocation of the operation
	 */
	public String createQueryString(Map namedValues, IRDFContainer rdfc) throws RDFException {
		Iterator keysIter = namedValues.keySet().iterator();
		StringBuffer returnString = new StringBuffer();
		
		while(keysIter.hasNext()) {
			Resource keyRes = (Resource) keysIter.next();
			RDFNode keyNode = rdfc.extract(keyRes,
					Constants.s_rdfs_label,
					null);
			RDFNode origNameNode = rdfc.extract(keyRes,
					WebOpManager.PARAMETER_ORIGINAL_NAME,
					null);
			String keyName = keyNode.getContent();
			String origName = origNameNode.getContent();
			String valueName = new String();
			Object value = namedValues.get(keyRes);
			if(!(value instanceof List)) {
				System.out.println("Named value is not a list");
			}
			else {
				List valueList = (List) value;
				if(valueList.size() == 0) {
					valueName = "";
				}
				else {
					RDFNode valueNode = (RDFNode) valueList.get(0);
					valueName = valueNode.getContent();
				}
			}
			try {
				WebOpParameter param = (WebOpParameter) this.parameters.get(origName);
				String type = param.getType();
				String submitValue = valueName;
				if(type.equalsIgnoreCase("checkbox")) {
					if(valueName.equals("true")) {
						submitValue = param.getOrigValue();
					}
					// if the value name is not true...it is not checked and thus we don't include this parameter
					else {
						continue;
					}
				}
				else if(type.equalsIgnoreCase("select")) {
					submitValue = (String) param.getRange().get(valueName);
				}
				String encodedValue = URLEncoder.encode(submitValue, "UTF-8");
				returnString.append(origName + "=" + encodedValue + "&");
			} catch (UnsupportedEncodingException e) {
				// This should not happen since UTF-8 is a valid encoding type
				e.printStackTrace();
			}
		}
		// we delete the last character since we don't need it
		returnString.deleteCharAt(returnString.length() - 1);
		
		return returnString.toString();
	}
	
	public String toString() {
		StringBuffer returnString = new StringBuffer();
		returnString.append("ActionURL: " + this.actionURL + "\n");
		returnString.append("Headers: " + this.headers + "\n");
		returnString.append("Type: ");
		if(this.type == WebOperation.GET_OPERATION) {
			returnString.append("Get\n");
		}
		else {
			returnString.append("Post\n");
		}
		Iterator iParams = parameters.values().iterator();
		while(iParams.hasNext()) {
			WebOpParameter param = (WebOpParameter) iParams.next();
			returnString.append(param.toString());
		}
		
		return returnString.toString();
	}

	/** 
	 * creates and navigates to the resources that are extracted from doc
	 */
	public void documentComplete(IDOMDocument doc, String url) {
		try {
			Pattern pattern = Pattern.fromResource(this.pat, this.rdfc);
			PatternResult patRes = pattern.match(doc);
			NodeID[] nids = patRes.getMatches();
			Resource viewRes = null;
			
			if(nids.length == 0) {
				FullScreenHostPart ofp = (FullScreenHostPart) this.context.getProperty(OzoneConstants.s_frame);
				ofp.requestViewing(this.visibleRes);
			}
			else if(nids.length == 1) {
				viewRes = WrapperManager.createResource(patRes, nids[0], pattern, rdfc, doc);
			}
			else {
				viewRes = Resource.createUnique(this.rdfc, Constants.s_haystack_Collection);
				for(int i = 0; i < nids.length; i++) {
					Resource resource = WrapperManager.createResource(patRes, nids[i], pattern, rdfc, doc);
					this.rdfc.add(new Statement(viewRes,
							Constants.s_haystack_member,
							resource));
				}
			}
			if(viewRes != null) {
				FullScreenHostPart ofp = (FullScreenHostPart) this.context.getProperty(OzoneConstants.s_frame);
				ofp.requestViewing(viewRes);
			}
		} catch (RDFException e) {
			e.printStackTrace();
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * @param operName The operName to set.
	 */
	public void setOperationName(String operName) {
		this.operName = operName;
	}
	

}
