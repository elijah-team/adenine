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

package edu.mit.lcs.haystack;

import edu.mit.lcs.haystack.rdf.*;

/**
 * Namespace and resource constants used throughout Haystack.
 * @author Dennis Quan
 */
final public class Constants {
	final public static String s_acl_namespace 			= "http://haystack.lcs.mit.edu/schemata/acl#";
	final public static String s_ann_namespace 			= "http://haystack.lcs.mit.edu/schemata/annotation#";
	final public static String s_applet_namespace 		= "http://haystack.lcs.mit.edu/schemata/ozoneapplet#";
	final public static String s_cdo_namespace 			= "http://haystack.lcs.mit.edu/schemata/cdo#";
	final public static String s_collab_namespace 		= "http://haystack.lcs.mit.edu/interface/collaboration#";
	final public static String s_config_namespace 		= "http://haystack.lcs.mit.edu/schemata/config#";
	final public static String s_content_namespace 		= "http://haystack.lcs.mit.edu/schemata/content#";
	final public static String s_daml_namespace 		= "http://www.daml.org/2001/03/daml+oil#";
	final public static String s_dc_namespace 			= "http://purl.org/dc/elements/1.1/";
	final public static String s_editor_namespace 		= "http://haystack.lcs.mit.edu/ui/ozoneeditor#";
	final public static String s_federation_namespace 	= "http://haystack.lcs.mit.edu/schemata/federation#";
	final public static String s_frame_namespace 		= "http://haystack.lcs.mit.edu/ui/frame#";
	final public static String s_haystack_namespace 	= "http://haystack.lcs.mit.edu/schemata/haystack#";
	final public static String s_http_namespace 		= "http://schemas.xmlsoap.org/wsdl/http/";
	final public static String s_info_namespace 		= "http://haystack.lcs.mit.edu/schemata/information#";
	final public static String s_infoint_namespace 		= "http://haystack.lcs.mit.edu/interfaces/information#";
	final public static String s_ldap_namespace 		= "http://haystack.lcs.mit.edu/agents/ldap#";
	final public static String s_lensui_namespace 		= "http://haystack.lcs.mit.edu/ui/lens#";
	final public static String s_mail_namespace 		= "http://haystack.lcs.mit.edu/schemata/mail#";
	final public static String s_metaglue_namespace 	= "http://www.ai.mit.edu/projects/iroom/metaglue/schema#";
	final public static String s_note_namespace 		= "http://haystack.lcs.mit.edu/schemata/note#";
	final public static String s_nlp_namespace 			= "http://haystack.lcs.mit.edu/schemata/nlp#";
	final public static String s_ozone_namespace 		= "http://haystack.lcs.mit.edu/schemata/ozone#";
	final public static String s_query_namespace 		= "http://haystack.lcs.mit.edu/schemata/query#";
	final public static String s_rdf_namespace 			= "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final public static String s_rdfs_namespace 		= "http://www.w3.org/2000/01/rdf-schema#";
	final public static String s_rdfstore_namespace 	= "http://haystack.lcs.mit.edu/agents/rdfstore#";
	final public static String s_scheduler_namespace 	= "http://haystack.lcs.mit.edu/agents/scheduler#";
	final public static String s_slide_namespace 		= "http://haystack.lcs.mit.edu/schemata/ozoneslide#";
	final public static String s_soap_namespace 		= "http://schemas.xmlsoap.org/wsdl/soap/";
	final public static String s_soapenc_namespace 		= "http://schemas.xmlsoap.org/soap/encoding/";
	final public static String s_status_namespace		= "http://haystack.lcs.mit.edu/ui/status#";
	final public static String s_subscriptions_namespace= "http://haystack.lcs.mit.edu/agents/subscriptions#";
	final public static String s_text_namespace 		= "http://haystack.lcs.mit.edu/agents/text#";
	final public static String s_vcard_namespace 		= "http://haystack.lcs.mit.edu/schemata/vcard#";
	final public static String s_verb_namespace 		= "http://haystack.lcs.mit.edu/schemata/verb#";
	final public static String s_web_namespace 			= "http://haystack.lcs.mit.edu/schemata/web#";
	final public static String s_wildcard_namespace 	= "urn:haystack:wildcard:";
	final public static String s_wsdl_namespace 		= "http://schemas.xmlsoap.org/wsdl/";
	final public static String s_navView_namespace		= "http://haystack.lcs.mit.edu/ui/navigationView#";
	final public static String s_xsd_namespace 			= "http://www.w3.org/2001/XMLSchema#";
	final public static String s_cv_namespace			= "http://haystack.lcs.mit.edu/ui/collectionView#";
	final public static String s_layout_namespace		= "http://haystack.lcs.mit.edu/schemata/layout#";
	final public static String s_dataProvider_namespace	= "http://haystack.lcs.mit.edu/schemata/dataProvider#";
	final public static String s_summaryView_namespace	= "http://haystack.lcs.mit.edu/ui/summaryView#";
	final public static String s_rdfLinks_namespace		= "http://haystack.lcs.mit.edu/schemata/RDFLinks#";
	
	final public static Resource s_acl_anyone 						= new Resource(s_acl_namespace + "anyone");

	final public static Resource s_lensui_LensPart					= new Resource(s_lensui_namespace + "LensPart");
	final public static Resource s_lensui_underlyingSource			= new Resource(s_lensui_namespace + "underlyingSource");

	final public static Resource s_ann_description					= new Resource(s_ann_namespace + "description");
	
	final public static Resource s_cdo_folder 						= new Resource(s_cdo_namespace + "folder");
	
	final public static Resource s_config_adenineMethod 			= new Resource(s_config_namespace + "adenineMethod");
	final public static Resource s_config_AdenineService 			= new Resource(s_config_namespace + "AdenineService");
	final public static Resource s_config_bindingDomain 			= new Resource(s_config_namespace + "bindingDomain");
	final public static Resource s_config_canConnectTo 				= new Resource(s_config_namespace + "canConnectTo");
	final public static Resource s_config_contentService 			= new Resource(s_config_namespace + "contentService");
	final public static Resource s_config_dependsOn 				= new Resource(s_config_namespace + "dependsOn");
	final public static Resource s_config_defaultInformationSource 	= new Resource(s_config_namespace + "defaultInformationSource");
	final public static Resource s_config_HaystackServer			= new Resource(s_config_namespace + "HaystackServer");
	final public static Resource s_config_hostsService 				= new Resource(s_config_namespace + "hostsService");
	final public static Resource s_config_hostsTransport 			= new Resource(s_config_namespace + "hostsTransport");
	final public static Resource s_config_includes 					= new Resource(s_config_namespace + "includes");
	final public static Resource s_config_informationSource 		= new Resource(s_config_namespace + "informationSource");
	final public static Resource s_config_init						= new Resource(s_config_namespace + "init");
	final public static Resource s_config_javaInterface 			= new Resource(s_config_namespace + "javaInterface");
	final public static Resource s_config_Method 					= new Resource(s_config_namespace + "Method");
	final public static Resource s_config_method 					= new Resource(s_config_namespace + "method");
	final public static Resource s_config_OntologyData 				= new Resource(s_config_namespace + "OntologyData");
	final public static Resource s_config_operation 				= new Resource(s_config_namespace + "operation");
	final public static Resource s_config_packages 					= new Resource(s_config_namespace + "packages");
	final public static Resource s_config_packageSet 				= new Resource(s_config_namespace + "packageSet");
	final public static Resource s_config_port 						= new Resource(s_config_namespace + "port");
	final public static Resource s_config_resourceLocation 			= new Resource(s_config_namespace + "resourceLocation");
	final public static Resource s_config_rootDir 					= new Resource(s_config_namespace + "rootDir");
	final public static Resource s_config_secondaryInformationSource= new Resource(s_config_namespace + "secondaryInformationSource");
	final public static Resource s_config_Service 					= new Resource(s_config_namespace + "Service");
	final public static Resource s_config_service 					= new Resource(s_config_namespace + "service");
	final public static Resource s_config_singleton 				= new Resource(s_config_namespace + "singleton");
	final public static Resource s_config_startEarly    			= new Resource(s_config_namespace + "startEarly");
	final public static Resource s_config_transferTo 				= new Resource(s_config_namespace + "transferTo");
	final public static Resource s_config_Transport 				= new Resource(s_config_namespace + "Transport");
	
	final public static Resource s_content_Content 				= new Resource(s_content_namespace + "Content");
	final public static Resource s_content_content 				= new Resource(s_content_namespace + "content");
	final public static Resource s_content_ContentService 		= new Resource(s_content_namespace + "ContentService");
	final public static Resource s_content_FilesystemContent 	= new Resource(s_content_namespace + "FilesystemContent");
	final public static Resource s_content_HttpContent 			= new Resource(s_content_namespace + "HttpContent");
	final public static Resource s_content_JavaClasspathContent = new Resource(s_content_namespace + "JavaClasspathContent");
	final public static Resource s_content_LiteralContent 		= new Resource(s_content_namespace + "LiteralContent");
	final public static Resource s_content_ServiceBackedContent = new Resource(s_content_namespace + "ServiceBackedContent");
	final public static Resource s_content_service				= new Resource(s_content_namespace + "service");
	final public static Resource s_content_NullContent 			= new Resource(s_content_namespace + "NullContent");
	final public static Resource s_content_path 				= new Resource(s_content_namespace + "path");
	
	final public static Resource s_daml_DatatypeProperty= new Resource(s_daml_namespace + "DatatypeProperty");
	final public static Resource s_daml_first 			= new Resource(s_rdf_namespace + "first");
	final public static Resource s_daml_List 			= new Resource(s_rdf_namespace + "List");
	final public static Resource s_daml_nil 			= new Resource(s_rdf_namespace + "nil");
	final public static Resource s_daml_ObjectProperty 	= new Resource(s_daml_namespace + "ObjectProperty");
	final public static Resource s_daml_rest 			= new Resource(s_rdf_namespace + "rest");
	final public static Resource s_daml_Thing 			= new Resource(s_daml_namespace + "Thing");
	final public static Resource s_daml_equivalentTo 	= new Resource(s_daml_namespace + "equivalentTo");
	final public static Resource s_daml_UniqueProperty 	= new Resource(s_daml_namespace + "UniqueProperty");
	
	final public static Resource s_dc_creator 		= new Resource(s_dc_namespace + "creator");
	final public static Resource s_dc_date 			= new Resource(s_dc_namespace + "date");
	final public static Resource s_dc_description 	= new Resource(s_dc_namespace + "description");
	final public static Resource s_dc_format 		= new Resource(s_dc_namespace + "format");
	final public static Resource s_dc_title 		= new Resource(s_dc_namespace + "title");
	
	final public static Resource s_editor_disallowBlanks		= new Resource(s_editor_namespace + "disallowBlanks");
	final public static Resource s_editor_metadataEditor 		= new Resource(s_editor_namespace + "metadataEditor");
	final public static Resource s_editor_multiline				= new Resource(s_editor_namespace + "multiline");
	final public static Resource s_editor_onValueSet	 		= new Resource(s_editor_namespace + "onValueSet");
	final public static Resource s_editor_propertiesToDisplay	= new Resource(s_editor_namespace + "propertiesToDisplay");
	final public static Resource s_editor_propertyEditor 		= new Resource(s_editor_namespace + "propertyEditor");
	final public static Resource s_editor_target 				= new Resource(s_editor_namespace + "target");
	final public static Resource s_editor_titleSource	 		= new Resource(s_editor_namespace + "titleSource");
	final public static Resource s_editor_valuesSource	 		= new Resource(s_editor_namespace + "valuesSource");

	final public static Resource s_federation_priority 			= new Resource(s_federation_namespace + "priority");
	final public static Resource s_federation_service 			= new Resource(s_federation_namespace + "service");
	final public static Resource s_federation_source 			= new Resource(s_federation_namespace + "source");
	final public static Resource s_federation_suppliedPredicate = new Resource(s_federation_namespace + "suppliedPredicate");
	
	final public static Resource s_frame_tooltip	 			= new Resource(s_frame_namespace + "tooltip");
	
	
	final public static Resource s_haystack_asserts 			= new Resource(s_haystack_namespace + "asserts");
	final public static Resource s_haystack_category 			= new Resource(s_haystack_namespace + "category");
	final public static Resource s_haystack_className 			= new Resource(s_haystack_namespace + "className");
	final public static Resource s_haystack_classView 			= new Resource(s_haystack_namespace + "classView");
	final public static Resource s_haystack_Collection 			= new Resource(s_haystack_namespace + "Collection");
	final public static Resource s_haystack_ContainmentProperty	= new Resource(s_haystack_namespace + "ContainmentProperty");
	final public static Resource s_haystack_DisposablePackage	= new Resource(s_haystack_namespace + "DisposablePackage");
	final public static Resource s_haystack_JavaClass 			= new Resource(s_haystack_namespace + "JavaClass");
	final public static Resource s_haystack_javaImplementation 	= new Resource(s_haystack_namespace + "javaImplementation");
	final public static Resource s_haystack_lastPackageUse		= new Resource(s_haystack_namespace + "lastPackageUse");
	final public static Resource s_haystack_list 				= new Resource(s_haystack_namespace + "list");
	final public static Resource s_haystack_List 				= new Resource(s_haystack_namespace + "List");
	final public static Resource s_haystack_md5 				= new Resource(s_haystack_namespace + "md5");
	final public static Resource s_haystack_member 				= new Resource(s_haystack_namespace + "member");
	final public static Resource s_haystack_Package 			= new Resource(s_haystack_namespace + "Package");
//	final public static Resource s_haystack_PackageFilename 	= new Resource(s_haystack_namespace + "PackageFilename");
	final public static Resource s_haystack_packageStatement 	= new Resource(s_haystack_namespace + "packageStatement");
	final public static Resource s_haystack_PasswordProperty 	= new Resource(s_haystack_namespace + "PasswordProperty");
	final public static Resource s_haystack_Person 				= new Resource(s_haystack_namespace + "Person");
	final public static Resource s_haystack_pluginName			= new Resource(s_haystack_namespace + "pluginName");
	final public static Resource s_haystack_ProprietalProperty 	= new Resource(s_haystack_namespace + "ProprietalProperty");
	final public static Resource s_haystack_publicKey 			= new Resource(s_haystack_namespace + "publicKey");
	final public static Resource s_haystack_RelationalProperty 	= new Resource(s_haystack_namespace + "RelationalProperty");
	final public static Resource s_haystack_Service 			= new Resource(s_haystack_namespace + "Service");
	final public static Resource s_haystack_Storage 			= new Resource(s_haystack_namespace + "Storage");
	final public static Resource s_haystack_user 				= new Resource(s_haystack_namespace + "user");
	final public static Resource s_haystack_view 				= new Resource(s_haystack_namespace + "view");
	final public static Resource s_haystack_Visitation 			= new Resource(s_haystack_namespace + "Visitation");
	final public static Resource s_haystack_visitedBy 			= new Resource(s_haystack_namespace + "visitedBy");
	final public static Resource s_haystack_visitedResource 	= new Resource(s_haystack_namespace + "visitedResource");
	final public static Resource s_haystack_visitTime 			= new Resource(s_haystack_namespace + "visitTime");
	final public static Resource s_haystack_reversiblePred 		= new Resource(s_haystack_namespace + "reversiblePred");
	
	final public static Resource s_http_address 			= new Resource(s_http_namespace + "address");
	final public static Resource s_http_Binding 			= new Resource(s_http_namespace + "Binding");
	final public static Resource s_http_verb 				= new Resource(s_http_namespace + "verb");
	
	final public static Resource s_info_knowsAbout 			= new Resource(s_info_namespace + "knowsAbout");

	final public static Resource s_infoint_query 			= new Resource(s_infoint_namespace + "query");
	
	final public static Resource s_ldap_baseDN 	= new Resource(s_ldap_namespace + "baseDN");
	final public static Resource s_ldap_child 	= new Resource(s_ldap_namespace + "child");
	final public static Resource s_ldap_host 	= new Resource(s_ldap_namespace + "host");
	final public static Resource s_ldap_password= new Resource(s_ldap_namespace + "password");
	final public static Resource s_ldap_port 	= new Resource(s_ldap_namespace + "port");
	final public static Resource s_ldap_user 	= new Resource(s_ldap_namespace + "user");
	
	final public static Resource s_mail_AliasEndpoint			= new Resource(s_mail_namespace + "AliasEndpoint");
	final public static Resource s_mail_AsynchronousMessage		= new Resource(s_mail_namespace + "AsynchronousMessage");
	final public static Resource s_mail_Attachment 				= new Resource(s_mail_namespace + "Attachment");
	final public static Resource s_mail_attachment 				= new Resource(s_mail_namespace + "attachment");
	final public static Resource s_mail_away 					= new Resource(s_mail_namespace + "away");
	final public static Resource s_mail_bcc 					= new Resource(s_mail_namespace + "bcc");
	final public static Resource s_mail_body 					= new Resource(s_mail_namespace + "body");
	final public static Resource s_mail_cc 						= new Resource(s_mail_namespace + "cc");
	final public static Resource s_mail_chat 					= new Resource(s_mail_namespace + "chat");
	final public static Resource s_mail_deliveredForMessage		= new Resource(s_mail_namespace + "deliveredForMessage");
	final public static Resource s_mail_dnd 					= new Resource(s_mail_namespace + "dnd");
	final public static Resource s_mail_EmailAddress 			= new Resource(s_mail_namespace + "EmailAddress");
	final public static Resource s_mail_Endpoint 				= new Resource(s_mail_namespace + "Endpoint");
	final public static Resource s_mail_endpoint 				= new Resource(s_mail_namespace + "endpoint");
	final public static Resource s_mail_EndpointSpec 			= new Resource(s_mail_namespace + "EndpointSpec");
	final public static Resource s_mail_from 					= new Resource(s_mail_namespace + "from");
	final public static Resource s_mail_hasEndpoint 			= new Resource(s_mail_namespace + "hasEndpoint");
	final public static Resource s_mail_inReplyTo 				= new Resource(s_mail_namespace + "inReplyTo");
	final public static Resource s_mail_MailAgent 				= new Resource(s_mail_namespace + "MailAgent");
	final public static Resource s_mail_Message 				= new Resource(s_mail_namespace + "Message");
	final public static Resource s_mail_messageID 				= new Resource(s_mail_namespace + "messageID");
	final public static Resource s_mail_nearby 					= new Resource(s_mail_namespace + "nearby");
	final public static Resource s_mail_offline 				= new Resource(s_mail_namespace + "offline");
	final public static Resource s_mail_onlineStatus 			= new Resource(s_mail_namespace + "onlineStatus");
	final public static Resource s_mail_read 					= new Resource(s_mail_namespace + "read");
	final public static Resource s_mail_received 				= new Resource(s_mail_namespace + "received");
	final public static Resource s_mail_replyTo 				= new Resource(s_mail_namespace + "replyTo");
	final public static Resource s_mail_resolvedEndpointSpec	= new Resource(s_mail_namespace + "resolvedEndpointSpec");
	final public static Resource s_mail_resource 				= new Resource(s_mail_namespace + "resource");
	final public static Resource s_mail_sentDtTm 				= new Resource(s_mail_namespace + "sentDtTm");
	final public static Resource s_mail_subject 				= new Resource(s_mail_namespace + "subject");
	final public static Resource s_mail_SynchronousMessage		= new Resource(s_mail_namespace + "SynchronousMessage");
	final public static Resource s_mail_targetCollection		= new Resource(s_mail_namespace + "targetCollection");
	final public static Resource s_mail_textRenderer			= new Resource(s_mail_namespace + "textRenderer");
	final public static Resource s_mail_Thread 					= new Resource(s_mail_namespace + "Thread");
	final public static Resource s_mail_thread 					= new Resource(s_mail_namespace + "thread");
	final public static Resource s_mail_to 						= new Resource(s_mail_namespace + "to");
	final public static Resource s_mail_xaway 					= new Resource(s_mail_namespace + "xaway");

	final public static Resource s_metaglue_agentName 			= new Resource(s_metaglue_namespace + "agentName");
	final public static Resource s_metaglue_designation 		= new Resource(s_metaglue_namespace + "designation");
	
	final public static Resource s_nlp_meaningContent 		= new Resource(s_nlp_namespace + "meaningContent");
	final public static Resource s_nlp_meaningPredicate		= new Resource(s_nlp_namespace + "meaningPredicate");
	final public static Resource s_nlp_meaningType 			= new Resource(s_nlp_namespace + "meaningType");
	final public static Resource s_nlp_part 				= new Resource(s_nlp_namespace + "part");
	final public static Resource s_nlp_querySpecification 	= new Resource(s_nlp_namespace + "querySpecification");
	final public static Resource s_nlp_verbcategory 		= new Resource(s_nlp_namespace + "verbcategory");
	final public static Resource s_nlp_verbform 			= new Resource(s_nlp_namespace + "verbform");
	final public static Resource s_nlp_Word 				= new Resource(s_nlp_namespace + "Word");
	final public static Resource s_nlp_word 				= new Resource(s_nlp_namespace + "word");
	
	final public static Resource s_note_Note 				= new Resource(s_note_namespace + "Note");

	final public static Resource s_query_AndCondition 			= new Resource(s_query_namespace + "AndCondition");
	final public static Resource s_query_ContentContainsCondition = new Resource(s_query_namespace + "ContentContainsCondition");
	final public static Resource s_query_EqualityCondition 		= new Resource(s_query_namespace + "EqualityCondition");
	final public static Resource s_query_existential 			= new Resource(s_query_namespace + "existential");
	final public static Resource s_query_Indexable 				= new Resource(s_query_namespace + "Indexable");
	final public static Resource s_query_luceneAgent 			= new Resource(s_query_namespace + "luceneAgent");
	final public static Resource s_query_object 				= new Resource(s_query_namespace + "object");
	final public static Resource s_query_OrCondition 			= new Resource(s_query_namespace + "OrCondition");
	final public static Resource s_query_parameter 				= new Resource(s_query_namespace + "parameter");
	final public static Resource s_query_predicate 				= new Resource(s_query_namespace + "predicate");
	final public static Resource s_query_Query 					= new Resource(s_query_namespace + "Query");
	final public static Resource s_query_QueryAgent 			= new Resource(s_query_namespace + "QueryAgent");
	final public static Resource s_query_QueryInterpreter 		= new Resource(s_query_namespace + "QueryInterpreter");
	final public static Resource s_query_QuerySource 			= new Resource(s_query_namespace + "QuerySource");
	final public static Resource s_query_results 				= new Resource(s_query_namespace + "results");
	final public static Resource s_query_StatementCondition		= new Resource(s_query_namespace + "StatementCondition");
	final public static Resource s_query_subCondition 			= new Resource(s_query_namespace + "subCondition");
	final public static Resource s_query_subject 				= new Resource(s_query_namespace + "subject");
	final public static Resource s_query_target 				= new Resource(s_query_namespace + "target");
	final public static Resource s_query_targetExistential 		= new Resource(s_query_namespace + "targetExistential");
	final public static Resource s_query_terms 					= new Resource(s_query_namespace + "terms");
	final public static Resource s_query_text 					= new Resource(s_query_namespace + "text");
	final public static Resource s_query_naturalLanguage		= new Resource(s_query_namespace + "naturalLanguage");
	
	final public static Resource s_rdf_object 		= new Resource(s_rdf_namespace + "object");
	final public static Resource s_rdf_predicate 	= new Resource(s_rdf_namespace + "predicate");
	final public static Resource s_rdf_Property		= new Resource(s_rdf_namespace + "Property");
	final public static Resource s_rdf_Statement 	= new Resource(s_rdf_namespace + "Statement");
	final public static Resource s_rdf_subject 		= new Resource(s_rdf_namespace + "subject");
	final public static Resource s_rdf_type 		= new Resource(s_rdf_namespace + "type");
	
	final public static Resource s_rdfs_comment 	= new Resource(s_rdfs_namespace + "comment");
	final public static Resource s_rdfs_domain 		= new Resource(s_rdfs_namespace + "domain");
	final public static Resource s_rdfs_isDefinedBy	= new Resource(s_rdfs_namespace + "isDefinedBy");
	final public static Resource s_rdfs_label		= new Resource(s_rdfs_namespace + "label");
	final public static Resource s_rdfs_range 		= new Resource(s_rdfs_namespace + "range");
	final public static Resource s_rdfs_subClassOf 	= new Resource(s_rdfs_namespace + "subClassOf");
	
	final public static Resource s_rdfstore_databaseURL 		= new Resource(s_rdfstore_namespace + "databaseURL");
	final public static Resource s_rdfstore_jdbcDriver 			= new Resource(s_rdfstore_namespace + "jdbcDriver");
	final public static Resource s_rdfstore_onStatementAdded	= new Resource(s_rdfstore_namespace + "onStatementAdded");
	final public static Resource s_rdfstore_onStatementRemoved	= new Resource(s_rdfstore_namespace + "onStatementRemoved");
	final public static Resource s_rdfstore_password 			= new Resource(s_rdfstore_namespace + "password");
	final public static Resource s_rdfstore_username 			= new Resource(s_rdfstore_namespace + "username");
	
	final public static Resource s_scheduler_frequency 			= new Resource(s_scheduler_namespace + "frequency");
	final public static Resource s_scheduler_timeOfDay 			= new Resource(s_scheduler_namespace + "timeOfDay");
	final public static Resource s_scheduler_lastRun 			= new Resource(s_scheduler_namespace + "lastRun");
	final public static Resource s_scheduler_performScheduledTask = new Resource(s_scheduler_namespace + "performScheduledTask");
	final public static Resource s_scheduler_service 			= new Resource(s_scheduler_namespace + "service");
	final public static Resource s_scheduler_runNow				= new Resource(s_scheduler_namespace + "runNow");
	final public static Resource s_scheduler_Task 				= new Resource(s_scheduler_namespace + "Task");
	
	final public static Resource s_soap_address 		= new Resource(s_soap_namespace + "address");
	final public static Resource s_soap_encodingStyle	= new Resource(s_soap_namespace + "encodingStyle");
	final public static Resource s_soap_namespace_		= new Resource(s_soap_namespace + "namespace");

	final public static Resource s_status_Error					= new Resource(s_status_namespace + "Error");
	final public static Resource s_status_error					= new Resource(s_status_namespace + "error");
	final public static Resource s_status_JavaExceptionError	= new Resource(s_status_namespace + "JavaExceptionError");
	final public static Resource s_status_stackTrace			= new Resource(s_status_namespace + "stackTrace");
	final public static Resource s_status_TextError				= new Resource(s_status_namespace + "TextError");

	final public static Resource s_subscriptions_source 		= new Resource(s_subscriptions_namespace + "source");
	
	final public static Resource s_text_extractedText 			= new Resource(s_text_namespace + "extractedText");
	final public static Resource s_text_summary 				= new Resource(s_text_namespace + "summary");
	final public static Resource s_text_contentService			= new Resource(s_text_namespace + "contentService");
	final public static Resource s_text_contains				= new Resource(s_text_namespace + "contains");
	final public static Resource s_text_textToCopy				= new Resource(s_text_namespace + "textToCopy");
	
	
	final public static Resource s_verb_adenineService 	= new Resource(s_verb_namespace + "adenineService");
	final public static Resource s_verb_domain 			= new Resource(s_verb_namespace + "domain");
	final public static Resource s_verb_partDomain 		= new Resource(s_verb_namespace + "partDomain");
	final public static Resource s_verb_titleGenerator 	= new Resource(s_verb_namespace + "titleGenerator");
	
	final public static Resource s_web_WebPage 			= new Resource(s_web_namespace + "WebPage");
	
	final public static Resource s_wsdl_Binding 		= new Resource(s_wsdl_namespace + "Binding");
	final public static Resource s_wsdl_binding 		= new Resource(s_wsdl_namespace + "binding");
	final public static Resource s_wsdl_bindingOperation= new Resource(s_wsdl_namespace + "bindingOperation");
	final public static Resource s_wsdl_input			= new Resource(s_wsdl_namespace + "input");
	final public static Resource s_wsdl_inputBinding	= new Resource(s_wsdl_namespace + "inputBinding");
	final public static Resource s_wsdl_output			= new Resource(s_wsdl_namespace + "output");
	final public static Resource s_wsdl_operation		= new Resource(s_wsdl_namespace + "operation");
	final public static Resource s_wsdl_operationBinding= new Resource(s_wsdl_namespace + "operationBinding");
	final public static Resource s_wsdl_part			= new Resource(s_wsdl_namespace + "part");
	final public static Resource s_wsdl_Port 			= new Resource(s_wsdl_namespace + "Port");
	final public static Resource s_wsdl_port 			= new Resource(s_wsdl_namespace + "port");
	final public static Resource s_wsdl_PortType 		= new Resource(s_wsdl_namespace + "PortType");
	final public static Resource s_wsdl_type 			= new Resource(s_wsdl_namespace + "type");
	
	final public static Resource s_xsd_dateTime 		= new Resource(s_xsd_namespace + "dateTime");
	final public static Resource s_xsd_float 			= new Resource(s_xsd_namespace + "float");
	final public static Resource s_xsd_int	 			= new Resource(s_xsd_namespace + "int");
	final public static Resource s_xsd_string			= new Resource(s_xsd_namespace + "string");
	final public static Resource s_xsd_boolean			= new Resource(s_xsd_namespace + "boolean");
	
	final public static Literal s_emptyLiteral = new Literal("");

	private static String osname = System.getProperty("os.name");
	public static final boolean isMacOSX = "Mac OS X".equals(osname);
	public static final boolean isWindows = "Windows".equals(osname);
	public static final boolean isLinux = "Linux".equals(osname);
	
	final public static Resource s_startingPoints = new Resource("http://haystack.lcs.mit.edu/data/operations#startingPoints");
}
