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

package edu.mit.lcs.haystack.ozone.standard.layout;

import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class LayoutConstants {
	final public static String s_namespace = "http://haystack.lcs.mit.edu/schemata/layout#";
	
	final public static Resource s_layoutConstraint	= new Resource(s_namespace + "layoutConstraint");
	final public static Resource s_selection			= new Resource(s_namespace + "selection");
	final public static Resource s_focus				= new Resource(s_namespace + "focus");

	/*
	 * Splitter properties
	 */
	final public static Resource s_pack			= new Resource(s_namespace + "pack");			// [FIRST|last]

	final public static Resource s_constraint		= new Resource(s_namespace + "constraint");	// 
	final public static Resource s_element			= new Resource(s_namespace + "element");		// [index literal|child part resource]
	final public static Resource s_resizable			= new Resource(s_namespace + "resizable");	// [TRUE|false]
	final public static Resource s_persistent		= new Resource(s_namespace + "persistent");	// [TRUE|false]
	final public static Resource s_dimension			= new Resource(s_namespace + "dimension");	// in pixels or percents
	
	/*
	 * Stacker properties
	 */
	final public static Resource s_sortBy				= new Resource(s_namespace + "sortBy");					// data:DataSource
	final public static Resource s_groupBy				= new Resource(s_namespace + "groupBy");					// data:DataSource
	final public static Resource s_defaultShowCount		= new Resource(s_namespace + "defaultShowCount");		// xsd:Integer
	final public static Resource s_morePartData			= new Resource(s_namespace + "morePartData");			//
	final public static Resource s_emptyPartData			= new Resource(s_namespace + "emptyPartData");			//
	final public static Resource s_maxVisibleCount		= new Resource(s_namespace + "maxVisibleCount");			// xsd:Integer
	final public static Resource s_itemsCountCollection	= new Resource(s_namespace + "itemsCountCollection");		// data:DataSource

	final public static Resource s_separator				= new Resource(s_namespace + "separator");				// part data
	final public static Resource s_lastSeparator			= new Resource(s_namespace + "lastSeparator");			// part data
	final public static Resource s_moreItems				= new Resource(s_namespace + "moreItems");				// part data
	final public static Resource s_noItems				= new Resource(s_namespace + "noItems");					// part data
	
	/*
	 * Field set properties
	 */
	final public static Resource s_Field					= new Resource(s_namespace + "Field");
	final public static Resource s_fields					= new Resource(s_namespace + "fields");					// data:DataProvider
	final public static Resource s_headers					= new Resource(s_namespace + "headers");					//
	final public static Resource s_fieldsDataProvider		= new Resource(s_namespace + "fieldsDataProvider");		//
	final public static Resource s_fieldPartDataGenerator	= new Resource(s_namespace + "fieldPartDataGenerator");	//
	final public static Resource s_drawFieldDividers			= new Resource(s_namespace + "drawFieldDividers");			//
	final public static Resource s_fieldPartDataMap			= new Resource(s_namespace + "fieldPartDataMap");			//
	final public static Resource s_fieldIDs					= new Resource(s_namespace + "fieldIDs");					//
	final public static Resource s_fieldPartData				= new Resource(s_namespace + "fieldPartData");				//

	/*
	 * Other properties
	 */
	final public static Resource s_spansAllColumns				= new Resource(s_namespace + "spansAllColumns");
	final public static Resource s_spansAllColumnsDataSource	= new Resource(s_namespace + "spansAllColumnsDataSource");
	final public static Resource s_minColumnWidth				= new Resource(s_namespace + "minColumnWidth");
	final public static Resource s_columnWidth					= new Resource(s_namespace + "columnWidth");
	final public static Resource s_drawSeparatorLine			= new Resource(s_namespace + "drawSeparatorLine");
	
	final public static Resource s_layoutManager					= new Resource(s_namespace + "layoutManager");
	final public static Resource s_itemIndex						= new Resource(s_namespace + "itemIndex");
	final public static Resource s_hostPartCache					= new Resource(s_namespace + "hostPartCache");
	final public static Resource s_enableChanges					= new Resource(s_namespace + "enableChanges");
	final public static Resource s_refocus							= new Resource(s_namespace + "refocus");
	final public static Resource s_verticalMargin					= new Resource(s_namespace + "verticalMargin");
}
