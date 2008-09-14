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

package edu.mit.lcs.haystack.adenine;

import edu.mit.lcs.haystack.rdf.*;

/**
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class AdenineConstants {
	final public static String NAMESPACE = "http://haystack.lcs.mit.edu/schemata/adenine#";

	final public static Resource INSTRUCTION_HANDLER 	= new Resource(NAMESPACE + "InstructionHandler");
	final public static Resource precompile				= new Resource(NAMESPACE + "precompile");
	final public static Resource source					= new Resource(NAMESPACE + "source");
	final public static Resource target					= new Resource(NAMESPACE + "target");
	final public static Resource compileTime			= new Resource(NAMESPACE + "compileTime");
	final public static Resource newline				= new Resource(NAMESPACE + "newline");

	/*
	 * Types of nodes
	 */	
	 
	final public static Resource Assignment 		= new Resource(NAMESPACE + "Assignment");
	final public static Resource BNode 				= new Resource(NAMESPACE + "BNode");
	final public static Resource Break 				= new Resource(NAMESPACE + "Break");
	final public static Resource Call 				= new Resource(NAMESPACE + "Call");
	final public static Resource Continue			= new Resource(NAMESPACE + "Continue");
	final public static Resource Dereferencement 	= new Resource(NAMESPACE + "Dereferencement");
	final public static Resource ForIn				= new Resource(NAMESPACE + "ForIn");
	final public static Resource Function 			= new Resource(NAMESPACE + "Function");
	final public static Resource FunctionCall 		= new Resource(NAMESPACE + "FunctionCall");
	final public static Resource CallReturn 		= new Resource(NAMESPACE + "CallReturn");
	final public static Resource Identifier 		= new Resource(NAMESPACE + "Identifier");
	final public static Resource If 				= new Resource(NAMESPACE + "If");
	final public static Resource IMPORT 			= new Resource(NAMESPACE + "Import");
	final public static Resource ImportJava 		= new Resource(NAMESPACE + "ImportJava");
	final public static Resource Index 				= new Resource(NAMESPACE + "Index");
	final public static Resource LIBRARY 			= new Resource(NAMESPACE + "Library");
	final public static Resource LIBRARYDEF 		= new Resource(NAMESPACE + "LibraryDef");
	final public static Resource Literal 			= new Resource(NAMESPACE + "Literal");
	final public static Resource Model 				= new Resource(NAMESPACE + "Model");
	final public static Resource Query				= new Resource(NAMESPACE + "Query");
	final public static Resource RDFNode			= new Resource(NAMESPACE + "RDFNode");
	final public static Resource Resource 			= new Resource(NAMESPACE + "Resource");
	final public static Resource Return 			= new Resource(NAMESPACE + "Return");
	final public static Resource Method 			= new Resource(NAMESPACE + "Method");
	final public static Resource MethodDef 			= new Resource(NAMESPACE + "MethodDef");
	final public static Resource MethodDef2 			= new Resource(NAMESPACE + "MethodDef2");
	final public static Resource Statement 			= new Resource(NAMESPACE + "Statement");
	final public static Resource String 			= new Resource(NAMESPACE + "String");
	final public static Resource Var 				= new Resource(NAMESPACE + "Var");
	final public static Resource While 				= new Resource(NAMESPACE + "While");
	final public static Resource With 				= new Resource(NAMESPACE + "With");
	
	/*
	 * Attributes of nodes
	 */
	 
	final public static Resource INSTRUCTION_DOMAIN = new Resource(NAMESPACE + "instructionDomain");

	final public static Resource base 				= new Resource(NAMESPACE + "base");
	final public static Resource body 				= new Resource(NAMESPACE + "body");
	final public static Resource COLLECTION 		= new Resource(NAMESPACE + "collection");
	final public static Resource CONDITION 			= new Resource(NAMESPACE + "condition");
	final public static Resource debug	 			= new Resource(NAMESPACE + "debug");
	final public static Resource ELSEBODY 			= new Resource(NAMESPACE + "elseBody");
	final public static Resource function 			= new Resource(NAMESPACE + "function");
	final public static Resource index 				= new Resource(NAMESPACE + "index");
	final public static Resource identity			= new Resource(NAMESPACE + "identity");
	final public static Resource LHS 				= new Resource(NAMESPACE + "lhs");
	final public static Resource LIBRARY_ 			= new Resource(NAMESPACE + "library");
	final public static Resource line 				= new Resource(NAMESPACE + "line");
	final public static Resource literal 			= new Resource(NAMESPACE + "literal");
	final public static Resource next 				= new Resource(NAMESPACE + "next");
	final public static Resource main 				= new Resource(NAMESPACE + "main");
	final public static Resource member 			= new Resource(NAMESPACE + "member");
	final public static Resource name 				= new Resource(NAMESPACE + "name");
	final public static Resource namedParameter		= new Resource(NAMESPACE + "namedParameter");
	final public static Resource namedParameterSpec	= new Resource(NAMESPACE + "NamedParameterSpec");
	final public static Resource NAMED_RETURN_PARAMETER= new Resource(NAMESPACE + "namedReturnParameter");
	final public static Resource object 			= new Resource(NAMESPACE + "object");
	final public static Resource PACKAGE 			= new Resource(NAMESPACE + "package");
	final public static Resource parameterName		= new Resource(NAMESPACE + "parameterName");
	final public static Resource parameterVariable	= new Resource(NAMESPACE + "parameterVariable");
	final public static Resource PARAMETERS 		= new Resource(NAMESPACE + "parameters");
	final public static Resource BACKQUOTED_PARAMETERS 		= new Resource(NAMESPACE + "backquotedParameters");
	final public static Resource queryConditions	= new Resource(NAMESPACE + "queryConditions");
	final public static Resource RETURN_PARAMETERS 	= new Resource(NAMESPACE + "returnParameters");
	final public static Resource predicate 			= new Resource(NAMESPACE + "predicate");
	final public static Resource preload	 		= new Resource(NAMESPACE + "preload");
	final public static Resource rdfNode 			= new Resource(NAMESPACE + "rdfNode");
	final public static Resource resource 			= new Resource(NAMESPACE + "resource");
	final public static Resource RHS 				= new Resource(NAMESPACE + "rhs");
	final public static Resource start 				= new Resource(NAMESPACE + "start");
	final public static Resource statement 			= new Resource(NAMESPACE + "statement");
	final public static Resource string 			= new Resource(NAMESPACE + "string");
	final public static Resource subject 			= new Resource(NAMESPACE + "subject");
	final public static Resource value 				= new Resource(NAMESPACE + "value");
	final public static Resource var 				= new Resource(NAMESPACE + "var");

	final public static Resource or					= new Resource(NAMESPACE + "or");
	final public static Resource and				= new Resource(NAMESPACE + "and");
	final public static Resource contains			= new Resource(NAMESPACE + "contains");
	final public static Resource multiContains		= new Resource(NAMESPACE + "multiContains");
	final public static Resource setDifference		= new Resource(NAMESPACE + "setDifference");
	final public static Resource ConditionSet		= new Resource(NAMESPACE + "ConditionSet");
	final public static Resource conditions			= new Resource(NAMESPACE + "conditions");

	final public static Resource ResourceConverter	= new Resource(NAMESPACE + "ResourceConverter");
	final public static Resource converter			= new Resource(NAMESPACE + "converter");
	final public static Resource conversionDomain	= new Resource(NAMESPACE + "conversionDomain");

	final public static Resource ConditionHandler	= new Resource(NAMESPACE + "ConditionHandler");
	final public static Resource currentResults		= new Resource(NAMESPACE + "currentResults");
	final public static Resource existentials		= new Resource(NAMESPACE + "existentials");
}
