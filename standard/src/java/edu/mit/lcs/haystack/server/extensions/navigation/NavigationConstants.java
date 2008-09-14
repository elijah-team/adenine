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

/*
 * Created on May 8, 2003
 *
 */
package edu.mit.lcs.haystack.server.extensions.navigation;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author vineet
 *
 */
public class NavigationConstants {


	final public static String s_nav_namespace = "http://haystack.lcs.mit.edu/ui/navigationView#";
	
	final public static Resource s_nav_NonNavigableProperty =
														new Resource(s_nav_namespace + "NonNavigableProperty");
	final public static Resource s_nav_domain =			new Resource(s_nav_namespace + "domain");
	final public static Resource s_nav_path = 			new Resource(s_nav_namespace + "path");
	final public static Resource s_nav_rangePath =		new Resource(s_nav_namespace + "rangePath");
	final public static Resource s_nav_enableCaching =	new Resource(s_nav_namespace + "enableCaching");
	final public static Resource s_nav_disableContent =	new Resource(s_nav_namespace + "disableContent");
	final public static Resource s_nav_disableDataCache =	new Resource(s_nav_namespace + "disableDataCache");
	final public static Resource s_nav_reverseNextPred =
														new Resource(s_nav_namespace + "reverseNextPred");


	final public static Resource s_nav_getTitle =		new Resource(s_nav_namespace + "getTitle");
	final public static Resource s_nav_getTitleFromResArray =
														new Resource(s_nav_namespace + "getTitleFromResArray");
	final public static Resource s_nav_NavigationAdvisor =
														new Resource(s_nav_namespace + "NavigationAdvisor");
	final public static Resource s_nav_currTerm =		new Resource(s_nav_namespace + "currTerm");
	final public static Resource s_nav_navAgentValues =
														new Resource(s_nav_namespace + "navAgentValues");
	final public static Resource s_nav_NavigationOption =	
														new Resource(s_nav_namespace + "NavigationOption");
	final public static Resource s_nav_groupByPred =	new Resource(s_nav_namespace + "groupByPred");
	final public static Resource s_nav_pred =			new Resource(s_nav_namespace + "pred");
	final public static Resource s_nav_value =			new Resource(s_nav_namespace + "value");
	final public static Resource s_navView_valueSize =	new Resource(s_nav_namespace + "valueSize");
	final public static Resource s_nav_sortAttrib =		new Resource(s_nav_namespace + "sortAttrib");
	final public static Resource s_nav_onClickMethod =	new Resource(s_nav_namespace + "onClickMethod");
	final public static Resource s_nav_currObj =		new Resource(s_nav_namespace + "currObj");
	final public static Resource s_nav_containingTextQuery =
														new Resource(s_nav_namespace + "containingTextQuery");
	final public static Resource s_nav_volatilePath =	new Resource(s_nav_namespace + "volatilePath");
	final public static Resource s_nav_negatedNavOption =
														new Resource(s_nav_namespace + "negatedNavOption");
	final public static Resource s_nav_buildTitleFromNavOption =
														new Resource(s_nav_namespace + "buildTitleFromNavOption");
	final public static Resource s_nav_querylet =		new Resource(s_nav_namespace + "querylet");
	final public static Resource s_nav_queryStatement =	new Resource(s_nav_namespace + "queryStatement");
	final public static Resource s_nav_resolveAsIdentity =
														new Resource(s_nav_namespace + "resolveAsIdentity");
	final public static Resource s_nav_NavigableProperty =
														new Resource(s_nav_namespace + "NavigableProperty");
	final public static Resource s_nav_NavigationAdvisorType =
														new Resource(s_nav_namespace + "NavigationAdvisorType");
	final public static Resource s_nav_GroupedNavigationAdvisorType =
														new Resource(s_nav_namespace + "GroupedNavigationAdvisorType");
	final public static Resource s_nav_GroupedNavigationAdvisor =
														new Resource(s_nav_namespace + "GroupedNavigationAdvisor");
	final public static Resource s_nav_nestedTitle =
														new Resource(s_nav_namespace + "nestedTitle");
	final public static Resource s_nav_nestedToExpert =
														new Resource(s_nav_namespace + "nestedToExpert");
	final public static Resource s_nav_PromptNavigationAdvisor =
														new Resource(s_nav_namespace + "PromptNavigationAdvisor");
	final public static Resource s_nav_agentSource =	new Resource(s_nav_namespace + "agentSource");
	final public static Resource s_navView_order =
														new Resource(s_nav_namespace + "order");
	final public static Resource s_navView_NavAgentDomain =
														new Resource(s_nav_namespace + "NavAgentDomain");
		
	final public static String s_navPane_namespace = "http://haystack.lcs.mit.edu/ui/navigationPane#";
	
	final public static Resource s_navPane_navigationPaneUIOf =
														new Resource(s_navPane_namespace + "navigationPaneUIOf");
	final public static Resource s_navPane_navigationAgents =
														new Resource(s_navPane_namespace + "navigationAgents");
	final public static Resource s_navPane_embedsNavOptions =
														new Resource(s_navPane_namespace + "embedsNavOptions");
	final public static Resource s_navPane_launchNavFramework =
														new Resource(s_navPane_namespace + "launchNavFramework");
	final public static Resource s_navPane_setNavAgents =
														new Resource(s_navPane_namespace + "setNavAgents");
	final public static Resource s_navPane_callNavAgent =
														new Resource(s_navPane_namespace + "callNavAgent");
	final public static Resource s_navPane_order =		new Resource(s_navPane_namespace + "order");
	final public static Resource s_navPane_initializedExpert =
														new Resource(s_navPane_namespace + "initializedExpert");

	final public static String s_navSvc_namespace = "http://haystack.lcs.mit.edu/agents/navigation#";

	final public static Resource s_navSvc_enabled =
														new Resource(s_navSvc_namespace + "enabled");
}
