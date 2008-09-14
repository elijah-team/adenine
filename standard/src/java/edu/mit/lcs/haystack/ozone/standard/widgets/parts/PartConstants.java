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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class PartConstants {
	final static public String s_namespace 		= OzoneConstants.s_namespace;
	final static public String s_eventNamespace 	= s_namespace + "event.";
	
	/*
	 * Events
	 */
	final public static Resource s_eventMouseUp			= new Resource(s_eventNamespace + "mouseUp");
	final public static Resource s_eventMouseDown		= new Resource(s_eventNamespace + "mouseDown");
	final public static Resource s_eventMouseDoubleClick	= new Resource(s_eventNamespace + "mouseDoubleClick");
	final public static Resource s_eventMouseHover		= new Resource(s_eventNamespace + "mouseHover");
	final public static Resource s_eventMouseMove		= new Resource(s_eventNamespace + "mouseMove");
	final public static Resource s_eventMouseEnter		= new Resource(s_eventNamespace + "mouseEnter");
	final public static Resource s_eventMouseExit		= new Resource(s_eventNamespace + "mouseExit");

	final public static Resource s_eventKeyPressed		= new Resource(s_eventNamespace + "keyPressed");
	final public static Resource s_eventKeyReleased		= new Resource(s_eventNamespace + "keyReleased");
	
	final public static Resource s_eventDrag				= new Resource(s_eventNamespace + "drag");
	final public static Resource s_eventDragEnd			= new Resource(s_eventNamespace + "dragEnd");
	final public static Resource s_eventDragSetData		= new Resource(s_eventNamespace + "dragSetData");
	
	final public static Resource s_eventDragEnter		= new Resource(s_eventNamespace + "dragEnter");
	final public static Resource s_eventDragHover		= new Resource(s_eventNamespace + "dragHover");
	final public static Resource s_eventDragExit			= new Resource(s_eventNamespace + "dragExit");
	final public static Resource s_eventDragOperationChanged = new Resource(s_eventNamespace + "dragOperationChanged");
	final public static Resource s_eventDropAccept		= new Resource(s_eventNamespace + "dropAccept");
	final public static Resource s_eventDrop				= new Resource(s_eventNamespace + "drop");
	final public static Resource s_eventFakeDrop			= new Resource(s_eventNamespace + "fakeDrop");

	final public static Resource s_eventChildResize		= new Resource(s_eventNamespace + "childResize");

	final public static Resource s_eventGotInputFocus	= new Resource(s_eventNamespace + "gotInputFocus");
	final public static Resource s_eventLostInputFocus	= new Resource(s_eventNamespace + "lostInputFocus");

	final public static Resource s_eventContentHittest	= new Resource(s_eventNamespace + "contentHittest");
	final public static Resource s_eventContentHighlight	= new Resource(s_eventNamespace + "contentHighlight");
	
	final public static Resource s_eventServletRequest = new Resource(s_eventNamespace + "servletRequest");
	
	final public static Resource s_eventSaveState = new Resource(s_eventNamespace + "saveState");
	
	/*
	 * Main Ozone frame attributes
	 */
	final public static Resource s_caption	 		= new Resource(s_namespace + "caption");
	
	/*
	 * Dynamic properties
	 */
	final public static Resource s_targetResource	= new Resource(s_namespace + "targetResource");
	final public static Resource s_targetView		= new Resource(s_namespace + "targetView");
	
	final public static Resource s_hostedResource	= new Resource(s_namespace + "hostedResource");
	final public static Resource s_hostedViewInstance = new Resource(s_namespace + "hostedViewInstance");
	final public static Resource s_hostedPart		= new Resource(s_namespace + "hostedPart");
	
	/*
	 * Properties for data consumer
	 */ 
	final public static Resource s_onDataChanged	= new Resource(s_namespace + "onDataChanged");
	final public static Resource s_onStatusChanged	= new Resource(s_namespace + "onStatusChanged");
	final public static Resource s_reset			= new Resource(s_namespace + "reset");

	/*
	 * Custom part attributes for appearance
	 */ 
	final public static Resource s_dynamicText 		= new Resource(s_namespace + "dynamicText");
	final public static Resource s_height			= new Resource(s_namespace + "height");
	final public static Resource s_icon 				= new Resource(s_namespace + "icon");
	final public static Resource s_id				= new Resource(s_namespace + "id");
	final public static Resource s_multiline			= new Resource(s_namespace + "multiline");
	final public static Resource s_protected			= new Resource(s_namespace + "protected");
	final public static Resource s_text 				= new Resource(s_namespace + "text");
	final public static Resource s_tooltip			= new Resource(s_namespace + "tooltip");
	final public static Resource s_visible			= new Resource(s_namespace + "visible");
	final public static Resource s_width				= new Resource(s_namespace + "width");
	final public static Resource s_wrap				= new Resource(s_namespace + "wrap");
	
	/*
	 * Custom part attributes for events
	 */	 	
	final public static Resource s_onClick 			= new Resource(s_namespace + "onClick");
	final public static Resource s_onChange			= new Resource(s_namespace + "onChange");
	final public static Resource s_onEnterPressed	= new Resource(s_namespace + "onEnterPressed");
	final public static Resource s_onLoad			= new Resource(s_namespace + "onLoad");
	final public static Resource s_onNavigate		= new Resource(s_namespace + "onNavigate");

	/*
	 * Dynamic custom part attributes
	 */
	final public static Resource s_selection 		= new Resource(s_namespace + "selection");
	final public static Resource s_selectionPredicate = new Resource(s_namespace + "selectionPredicate");
	final public static Resource s_properties		= new Resource(s_namespace + "properties");

	/*
	 * Miscellaneous
	 */
	final public static Resource s_ok 					= new Resource(s_namespace + "OK");
	final public static Resource s_navigatorProperties 	= new Resource(s_namespace + "navigatorProperties");
	
	final public static Resource s_routeNavigationNotifications	= new Resource(s_namespace + "routeNavigationNotifications");

	final public static Resource s_child				= new Resource(s_namespace + "child");
	final public static Resource s_templatePartData		= new Resource(s_namespace + "templatePartData");
	final public static Resource s_hostedDataProvider	= new Resource(s_namespace + "hostedDataProvider");
	final public static Resource s_propertyName			= new Resource(s_namespace + "propertyName");
	final public static Resource s_initialChild			= new Resource(s_namespace + "initialChild");
	final public static Resource s_switchImmediately	= new Resource(s_namespace + "switchImmediately");
	final public static Resource s_readOnly				= new Resource(s_namespace + "readOnly");

	/*
	 * Internal
	 */
	final public static Resource s_hovered			= new Resource(s_namespace + "hovered");

	/*
	 * Caching
	 */
	final public static Resource s_cacheStyle			= new Resource(s_namespace + "cacheStyle");
	final public static Resource s_cachePerResource		= new Resource(s_namespace + "cachePerResource");
	final public static Resource s_cacheAcrossInstances	= new Resource(s_namespace + "cacheAcrossInstances");
	final public static Resource s_cacheDisabled		= new Resource(s_namespace + "cacheDisabled");
}
