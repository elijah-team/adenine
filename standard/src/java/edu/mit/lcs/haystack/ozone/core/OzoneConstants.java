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

package edu.mit.lcs.haystack.ozone.core;

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class OzoneConstants {
	final public static String s_namespace = Constants.s_ozone_namespace;

	/*
	 * Ozone default (automatically generated) collections
	 */
	final public static Resource s_buddyList			= new Resource(s_namespace + "buddyList");
	final public static Resource s_incoming 			= new Resource(s_namespace + "incoming");
	
	/*
	 * Part hierarchy
	 */

	// Generic attributes of part classes
	final public static Resource s_viewDomain 			= new Resource(s_namespace + "viewDomain");
	final public static Resource s_dataDomain 			= new Resource(s_namespace + "dataDomain");
	
	// Part initialization data in context
	final public static Resource s_part	 				= new Resource(s_namespace + "part");
	final public static Resource s_partClass			= new Resource(s_namespace + "partClass");
	final public static Resource s_partData 			= new Resource(s_namespace + "partData");
	final public static Resource s_partDataFromInformationSource = new Resource(s_namespace + "partDataFromInformationSource");
	final public static Resource s_viewInstance 		= new Resource(s_namespace + "viewInstance");
	final public static Resource s_dataSource 			= new Resource(s_namespace + "dataSource");
	final public static Resource s_dataSources 			= new Resource(s_namespace + "dataSources");
	final public static Resource s_underlying			= new Resource(s_namespace + "underlying");
	final public static Resource s_swtControl			= new Resource(s_namespace + "swtControl");

	// Custom part attributes for metadata hooks
	final public static Resource s_initialResource 		= new Resource(s_namespace + "initialResource");
	final public static Resource s_initialView 			= new Resource(s_namespace + "initialView");
	final public static Resource s_target 				= new Resource(s_namespace + "target");
		
	// View part types
	final public static Resource s_ViewPart 			= new Resource(s_namespace + "ViewPart");
	final public static Resource s_InteractiveViewPart 	= new Resource(s_namespace + "InteractiveViewPart");
	final public static Resource s_LineSummaryViewPart 	= new Resource(s_namespace + "LineSummaryViewPart");
	final public static Resource s_PhraseViewPart 		= new Resource(s_namespace + "PhraseViewPart");
	final public static Resource s_IconViewPart 		= new Resource(s_namespace + "IconViewPart");
	final public static Resource s_SWTPart 				= new Resource(s_namespace + "SWTPart");
	final public static Resource s_InlineViewPart 		= new Resource(s_namespace + "InlineViewPart");

	// Data provider for layout manager
	final public static Resource s_dataProvider 			= new Resource(s_namespace + "dataProvider");

	/*
	 * Drag and drop
	 */
	final public static Resource s_draggedPart 			= new Resource(s_namespace + "draggedPart");
	
	/*
	 * Common part attributes
	 */
	final public static Resource s_putProperty	 		= new Resource(s_namespace + "putProperty");
	final public static Resource s_putLocalProperty	 	= new Resource(s_namespace + "putLocalProperty");
	final public static Resource s_registerService 		= new Resource(s_namespace + "registerService");
	final public static Resource s_connector		 		= new Resource(s_namespace + "connector");

	final public static Resource s_user 					= new Resource(s_namespace + "user");
	final public static Resource s_identity				= new Resource(s_namespace + "identity");
	final public static Resource s_serviceAccessor 		= new Resource(s_namespace + "serviceAccessor");
	final public static Resource s_activePackage 		= new Resource(s_namespace + "activePackage");
	final public static Resource s_frame				= new Resource(s_namespace + "frame");

	/*
	 * Miscellaneous
	 */
	final public static Resource s_navigationMaster 	= new Resource(s_namespace + "navigationMaster");
	final public static Resource s_browserWindow	 	= new Resource(s_namespace + "browserWindow");
	final public static Resource s_browserID		 	= new Resource(s_namespace + "browserID");
	final public static Resource s_navigationDataProvider= new Resource(s_namespace + "navigationDataProvider");
	final public static Resource s_viewContainerFactory	= new Resource(s_namespace + "viewContainerFactory");
	final public static Resource s_viewContainerPart	= new Resource(s_namespace + "viewContainerPart");
	final public static Resource s_viewContainer		= new Resource(s_namespace + "viewContainer");
	final public static Resource s_viewNavigator		= new Resource(s_namespace + "viewNavigator");
	final public static Resource s_informationSource	= new Resource(s_namespace + "informationSource");
	final public static Resource s_defaultViewContainer	= new Resource(s_namespace + "defaultViewContainer");	
	final public static Resource s_parentPart			= new Resource(s_namespace + "parentPart");	
	final public static Resource s_viewPartClass		= new Resource(s_namespace + "viewPartClass");
	final public static Resource s_nestingRelation		= new Resource(s_namespace + "nestingRelation");
	final public static Resource s_child 				= new Resource(s_namespace + "child");
	final public static Resource s_children 			= new Resource(s_namespace + "children");
	final public static Resource s_layoutInstance		= new Resource(s_namespace + "layoutInstance");
	final public static Resource s_priority				= new Resource(s_namespace + "priority");
	final public static Resource s_partCache			= new Resource(s_namespace + "partCache");
	final public static Resource s_aspect				= new Resource(s_namespace + "aspect");
	final public static Resource s_viewInstanceDataSource = new Resource(s_namespace + "viewInstanceDataSource");
	final public static Resource s_defaultViewInstance	= new Resource(s_namespace + "defaultViewInstance");
	final public static Resource s_requiresViewInstance	= new Resource(s_namespace + "requiresViewInstance");
	
	/*
	 * For debugging purpose only
	 */
	final public static Resource s_debug 				= new Resource(s_namespace + "debug");
}
