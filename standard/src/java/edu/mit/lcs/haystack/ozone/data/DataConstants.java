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

package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.rdf.*;

/**
 * @author David Huynh
 */
public class DataConstants {
	final public static String NAMESPACE = "http://haystack.lcs.mit.edu/schemata/dataProvider#";

	final public static Resource DATA_CHANGE			= new Resource(NAMESPACE + "dataChange");
	final public static Resource DATA_DELETION		= new Resource(NAMESPACE + "dataDeletion");
	final public static Resource CONTEXT_SERVICE_REGISTRATION = new Resource(NAMESPACE + "contextServiceRegistration");

	final public static Resource RESOURCE			= new Resource(NAMESPACE + "resource");
	final public static Resource RESOURCE_CHANGE		= new Resource(NAMESPACE + "resourceChange");
	final public static Resource RESOURCE_DELETION 	= new Resource(NAMESPACE + "resourceDeletion");
	
	final public static Resource LITERAL				= new Resource(NAMESPACE + "literal");
	final public static Resource LITERAL_CHANGE		= new Resource(NAMESPACE + "literalChange");
	final public static Resource LITERAL_DELETION 	= new Resource(NAMESPACE + "literalDeletion");
	
	final public static Resource STRING				= new Resource(NAMESPACE + "string");
	final public static Resource STRING_CHANGE		= new Resource(NAMESPACE + "stringChange");
	final public static Resource STRING_DELETION		= new Resource(NAMESPACE + "stringDeletion");

	final public static Resource INTEGER				= new Resource(NAMESPACE + "integer");
	final public static Resource INTEGER_CHANGE		= new Resource(NAMESPACE + "integerChange");
	final public static Resource INTEGER_DELETION	= new Resource(NAMESPACE + "integerDeletion");

	final public static Resource BOOLEAN				= new Resource(NAMESPACE + "boolean");
	final public static Resource BOOLEAN_CHANGE		= new Resource(NAMESPACE + "booleanChange");

	final public static Resource LIST				= new Resource(NAMESPACE + "list");
	final public static Resource LIST_ELEMENT		= new Resource(NAMESPACE + "listElement");
	final public static Resource LIST_ELEMENTS		= new Resource(NAMESPACE + "listElements");
	final public static Resource LIST_COUNT			= new Resource(NAMESPACE + "listCount");
	final public static Resource LIST_ADDITION		= new Resource(NAMESPACE + "listAddition");
	final public static Resource LIST_REMOVAL		= new Resource(NAMESPACE + "listRemoval");
	final public static Resource LIST_CHANGE			= new Resource(NAMESPACE + "listChange");
	final public static Resource LIST_CLEAR			= new Resource(NAMESPACE + "listClear");

	final public static Resource SET					= new Resource(NAMESPACE + "set");
	final public static Resource SET_COUNT			= new Resource(NAMESPACE + "setCount");
	final public static Resource SET_ADDITION		= new Resource(NAMESPACE + "setAddition");
	final public static Resource SET_REMOVAL			= new Resource(NAMESPACE + "setRemoval");
	final public static Resource SET_CLEAR			= new Resource(NAMESPACE + "setClear");

	public final static Resource ORDERING 			= new Resource(NAMESPACE + "ordering");
	public final static Resource STATEMENT 			= new Resource(NAMESPACE + "statement");
	public final static Resource SUBJECT 			= new Resource(NAMESPACE + "subject");
	public final static Resource PREDICATE 			= new Resource(NAMESPACE + "predicate");
	public final static Resource OBJECT	 			= new Resource(NAMESPACE + "object");
	public final static Resource EXISTENTIALS 		= new Resource(NAMESPACE + "existentials");
	public final static Resource TARGET_EXISTENTIAL	= new Resource(NAMESPACE + "targetExistential");
	public final static Resource s_sourceExistential= new Resource(NAMESPACE + "sourceExistential");
	public final static Resource DAML_LIST 			= new Resource(NAMESPACE + "damlList");
	
	public final static Resource s_addNewItemsToBeginning = new Resource(NAMESPACE + "addNewItemsToBeginning");
	public final static Resource s_persistentDataSource   = new Resource(NAMESPACE + "persistentDataSource");

	public final static Resource s_subjectDataSource 	= new Resource(NAMESPACE + "subjectDataSource");
	public final static Resource s_predicateDataSource 	= new Resource(NAMESPACE + "predicateDataSource");
	public final static Resource s_objectDataSource   	= new Resource(NAMESPACE + "objectDataSource");
	public final static Resource s_exclusionDataSource  = new Resource(NAMESPACE + "exclusionDataSource");
	public final static Resource s_extract				= new Resource(NAMESPACE + "extract");
	public final static Resource s_depth				= new Resource(NAMESPACE + "depth");
	public final static Resource s_onAdd				= new Resource(NAMESPACE + "onAdd");
	public final static Resource s_memberDataSource		= new Resource(NAMESPACE + "memberDataSource");
	public final static Resource s_setDataSource		= new Resource(NAMESPACE + "setDataSource");
	public final static Resource s_focusDataSource		= new Resource(NAMESPACE + "focusDataSource");
	public final static Resource s_property				= new Resource(NAMESPACE + "property");
	public final static Resource s_DataProvider			= new Resource(NAMESPACE + "DataProvider");
	
	public final static Resource s_damlObjectPropertySetDataProvider = new Resource(NAMESPACE + "damlObjectPropertySetDataProvider");
	
	public final static Resource REVERSE		= new Resource(DataConstants.NAMESPACE + "reverse");
}
