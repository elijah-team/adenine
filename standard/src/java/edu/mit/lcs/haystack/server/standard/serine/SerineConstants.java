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

package edu.mit.lcs.haystack.server.standard.serine;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Serine constants.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class SerineConstants {
	final public static String NAMESPACE = "http://haystack.lcs.mit.edu/agents/serine#";
	
	final public static Resource SerineAgent = new Resource(NAMESPACE + "SerineAgent");
	
	final public static Resource Pattern = new Resource(NAMESPACE + "Pattern");
	final public static Resource statement = new Resource(NAMESPACE + "statement");
	final public static Resource pattern = new Resource(NAMESPACE + "pattern");
	final public static Resource existentials = new Resource(NAMESPACE + "existentials");
	final public static Resource PatternStatement = new Resource(NAMESPACE + "PatternStatement");
	final public static Resource subject = new Resource(NAMESPACE + "subject");
	final public static Resource predicate = new Resource(NAMESPACE + "predicate");
	final public static Resource object = new Resource(NAMESPACE + "object");

	final public static Resource Transformation = new Resource(NAMESPACE + "Transformation");
	final public static Resource transformation = new Resource(NAMESPACE + "transformation");
	final public static Resource precondition = new Resource(NAMESPACE + "precondition");
	final public static Resource postcondition = new Resource(NAMESPACE + "postcondition");
	final public static Resource priority = new Resource(NAMESPACE + "priority");
	final public static Resource runOnIdle = new Resource(NAMESPACE + "runOnIdle");
	final public static Resource resultStatement = new Resource(NAMESPACE + "resultStatement");
	final public static Resource adenineMethod = new Resource(NAMESPACE + "adenineMethod");
}
