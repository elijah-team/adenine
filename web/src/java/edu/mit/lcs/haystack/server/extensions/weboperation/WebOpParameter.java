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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.ListUtilities;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Ryan Manuel
 */
public class WebOpParameter {

	protected String dispName;
	protected String origName;
	protected String origValue;
	protected String rightText;
	protected String leftText;
	protected String type;
	protected Hashtable range;
	protected Resource paramRes;
	
	/**
	 * creates a web op parameter
	 */
	public WebOpParameter(String origName, String origValue) {
		this.origName = origName;
		this.dispName = origName;
		this.origValue = origValue;
		this.type = "default";
		this.range = new Hashtable();
		this.rightText = new String();
		this.leftText = new String();
	}
	
	/**
	 * constructor called from fromResource
	 */
	protected WebOpParameter(Resource paramRes,
			String origName,
			String dispName,
			String origValue,
			String rightText,
			String leftText,
			String type,
			Hashtable range) {
		this.origName = origName;
		this.dispName = dispName;
		this.origValue = origValue;
		this.rightText = rightText;
		this.leftText = leftText;
		this.type = type;
		this.range = range;
		this.paramRes = paramRes;
	}
	
	/**
	 * gets the range
	 */
	public Hashtable getRange() {
		return this.range;
	}
	
	/**
	 * sets the range
	 */
	public void setRange(Hashtable range) {
		this.range = range;
	}
	
	/**
	 * adds a range value
	 */
	public void addRangeValue(String visibleName, String submitName) {
		this.range.put(visibleName, submitName);
	}
	
	/**
	 * gets the type of the parameter
	 */
	public String getType() {
		return this.type;
	}
	
	/**
	 * sets the type of the parameter
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	public String toString() {
		StringBuffer returnString = new StringBuffer("Original Name: " + this.origName + "\n");
		returnString.append("Original Value: " + this.origValue + "\n");
		returnString.append("Type: " + this.type + "\n");
		Iterator iterRange = this.range.values().iterator();
		returnString.append("Range\n");
		while(iterRange.hasNext()) {
			returnString.append("Display Name: " + iterRange.next() + "\n");
		}
		return returnString.toString();
	}
	
	/**
	 * makes a resource from the web operation parameter
	 */
	public Resource makeResource(IRDFContainer rdfc) throws RDFException {
		if (this.paramRes != null) return this.paramRes;
		
		this.paramRes = Utilities.generateUniqueResource();
		rdfc.remove(new Statement(this.paramRes,
					  Constants.s_rdf_type,
					  Utilities.generateWildcardResource(1)),
			    Utilities.generateWildcardResourceArray(1));
		rdfc.add(new Statement(this.paramRes, Constants.s_rdf_type, WebOpManager.PARAMETER));
		rdfc.add(new Statement(this.paramRes, Constants.s_rdf_type, WebOpManager.OPERATION_PARAMETER));

		rdfc.add(new Statement(this.paramRes,
				WebOpManager.PARAMETER_ORIGINAL_NAME,
				new Literal(this.origName)));

		if(this.type.equalsIgnoreCase("select")) {
			rdfc.add(new Statement(this.paramRes,
					WebOpManager.OPERATION_REQUIRED,
					new Literal("true")));
		}
		else {
			rdfc.add(new Statement(this.paramRes,
					WebOpManager.OPERATION_REQUIRED,
					new Literal("false")));
		}
		
		
		rdfc.add(new Statement(this.paramRes,
				Constants.s_rdf_type,
				Constants.s_daml_DatatypeProperty));
		rdfc.add(new Statement(this.paramRes,
				Constants.s_rdf_type,
				Constants.s_daml_UniqueProperty));
		rdfc.add(new Statement(this.paramRes,
				Constants.s_rdfs_label,
				new Literal(this.dispName)));
		rdfc.add(new Statement(this.paramRes,
				Constants.s_rdfs_range,
				Constants.s_xsd_string));

		rdfc.add(new Statement(this.paramRes,
				WebOpManager.PARAMETER_ORIGINAL_NAME,
				new Literal(this.origName)));
		rdfc.add(new Statement(this.paramRes,
		      	WebOpManager.PARAMETER_ORIGINAL_VALUE,
		       	new Literal(this.origValue)));
		rdfc.add(new Statement(this.paramRes,
		      	WebOpManager.PARAMETER_TYPE,
		       	new Literal(this.type)));
		rdfc.add(new Statement(this.paramRes,
				WebOpManager.PARAMETER_DISPLAY_NAME,
				new Literal(this.dispName)));
		rdfc.add(new Statement(this.paramRes,
				WebOpManager.PARAMETER_RIGHT_TEXT,
				new Literal(this.rightText)));
		rdfc.add(new Statement(this.paramRes,
				WebOpManager.PARAMETER_LEFT_TEXT,
				new Literal(this.leftText)));
		
		// sets the range for checkbox inputs to boolean so that haystack will recognize it as
		// a checkbox input
		if(this.type.equalsIgnoreCase("checkbox")) {
			rdfc.add(new Statement(this.paramRes,
					Constants.s_rdfs_range,
					Constants.s_xsd_boolean));
		}
		
		Enumeration rangeKeys = range.keys();
		List possValuesList = new ArrayList();
		while(rangeKeys.hasMoreElements()) {
			String visibleName = (String) rangeKeys.nextElement();
						
			rdfc.add(new Statement(this.paramRes,
					WebOpManager.PARAMETER_RANGE_PROP,
					makeRangeResource(rdfc, visibleName, range)));
			
			// sets the possible values field to the range list so that haystack will recognize it
			// as a select input
			if(this.type.equalsIgnoreCase("select")) {
				Resource currValueRes = Utilities.generateUniqueResource();
				
				rdfc.add(new Statement(currValueRes,
						Constants.s_rdf_type,
						WebOpManager.METADATA_STRING));
				rdfc.add(new Statement(currValueRes,
						Constants.s_rdfs_label,
						new Literal(visibleName)));
				possValuesList.add(currValueRes);
			}
		}
		if(this.type.equalsIgnoreCase("select")) {
			Resource possValuesRes = ListUtilities.createDAMLList(possValuesList.iterator(),rdfc);		
		
			rdfc.add(new Statement(this.paramRes,
				WebOpManager.OPERATION_POSSIBLE_VALUES,
				possValuesRes));
		}

		return this.paramRes;
	}
	
	/**
	 * creates a webopparameter from a resource
	 */
	public static WebOpParameter fromResource(Resource paramRes, IRDFContainer rdfc) throws RDFException {
		RDFNode rdfOrigName = rdfc.extract(paramRes,
				WebOpManager.PARAMETER_ORIGINAL_NAME,
				null);
		RDFNode rdfOrigValue = rdfc.extract(paramRes,
				WebOpManager.PARAMETER_ORIGINAL_VALUE,
				null);
		RDFNode rdfType = rdfc.extract(paramRes,
				WebOpManager.PARAMETER_TYPE,
				null);
		RDFNode rdfDispName = rdfc.extract(paramRes,
				WebOpManager.PARAMETER_DISPLAY_NAME,
				null);
		RDFNode rdfRightText = rdfc.extract(paramRes,
				WebOpManager.PARAMETER_RIGHT_TEXT,
				null);
		RDFNode rdfLeftText = rdfc.extract(paramRes,
				WebOpManager.PARAMETER_LEFT_TEXT,
				null);
		
		Set rangeSet = rdfc.query(new Statement[] {new Statement(paramRes,
								    WebOpManager.PARAMETER_RANGE_PROP,
								    Utilities.generateWildcardResource(1))},
				     Utilities.generateWildcardResourceArray(1),
				     Utilities.generateWildcardResourceArray(1));
		
		int size = (rangeSet == null) ? 0 : rangeSet.size();
		Hashtable range = new Hashtable();
		if (rangeSet != null) {
		    Iterator iter = rangeSet.iterator();
		    while (iter.hasNext()) {
		    	Resource curRes = (Resource) ((RDFNode[])iter.next())[0];
		    	RDFNode rdfRangeDisplay = rdfc.extract(curRes,
		    			WebOpManager.PARAMETER_RANGE_DISPLAY,
						null);
		    	RDFNode rdfRangeSubmit = rdfc.extract(curRes,
		    			WebOpManager.PARAMETER_RANGE_SUBMIT,
						null);
		    	
		    	range.put(rdfRangeDisplay.getContent(), rdfRangeSubmit.getContent());
		    }
		}
		
		WebOpParameter webOpParam = new WebOpParameter(paramRes, 
				rdfOrigName.getContent(),
				rdfDispName.getContent(),
				rdfOrigValue.getContent(),
				rdfRightText.getContent(),
				rdfLeftText.getContent(),
				rdfType.getContent(),
				range);
		
		return webOpParam;	
	}
	
	
	// makes a resource out of the range
	public Resource makeRangeResource(IRDFContainer rdfc, String visibleName, Hashtable range) throws RDFException {
		String submitName = (String) range.get(visibleName);
		
		Resource rangeValueRes = Utilities.generateUniqueResource();
		rdfc.remove(new Statement(rangeValueRes,
				  Constants.s_rdf_type,
				  Utilities.generateWildcardResource(1)),
				  Utilities.generateWildcardResourceArray(1));
		rdfc.add(new Statement(rangeValueRes, Constants.s_rdf_type, WebOpManager.PARAMETER_RANGE));
		
		rdfc.add(new Statement(rangeValueRes,
				WebOpManager.PARAMETER_RANGE_DISPLAY,
				new Literal(visibleName)));
		rdfc.add(new Statement(rangeValueRes,
				WebOpManager.PARAMETER_RANGE_SUBMIT,
				new Literal(submitName)));
		return rangeValueRes;
	}

	/**
	 * @return Returns the dispName.
	 */
	public String getDispName() {
		return dispName;
	}
	
	/**
	 * @param dispName The dispName to set.
	 */
	public void setDispName(String dispName) {
		this.dispName = dispName;
	}
	/**
	 * @return Returns the origValue.
	 */
	public String getOrigValue() {
		return origValue;
	}
	/**
	 * @param origValue The origValue to set.
	 */
	public void setOrigValue(String origValue) {
		this.origValue = origValue;
	}
	
	/**
	 * @return Returns the leftText.
	 */
	public String getLeftText() {
		return leftText;
	}
	/**
	 * @param leftText The leftText to set.
	 */
	public void setLeftText(String leftText) {
		this.leftText = leftText;
	}
	/**
	 * @return Returns the rightText.
	 */
	public String getRightText() {
		return rightText;
	}
	/**
	 * @param rightText The rightText to set.
	 */
	public void setRightText(String rightText) {
		this.rightText = rightText;
	}
}
