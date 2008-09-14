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

package edu.mit.lcs.haystack.server.extensions.query;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.functions.AskFunction;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.adenine.query.DefaultQueryEngine;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.extensions.navigation.NavigationConstants;
import edu.mit.lcs.haystack.server.extensions.navigation.NavigationService;

public class DocumentAcceptor {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(DocumentAcceptor.class);

	/** 
	 * Handle resources that have a type that is a subtype of content:Content
	 * i.e. those that uses the contentClient to retrieve the actual data associated with the 
	 * resource 
	 */	 
	static protected void visitContentForDoc(DocumentVisitor termVisitor, Resource resDoc) throws Exception {
	    /* 
	     * check if resDoc is a subtype of content:Content type
	     */
		if (!Utilities.isSubType(resDoc, Constants.s_content_Content, termVisitor.getRDFContainer())) {
			// not content type
			return;
		}

		/*
		 * Vineet:TODO: this should actually be something like 
		 * {resDoc rdf:type ?x, ?x rdfs:subClass ?y, ?y disableContent "false" }
		 * and not contain
		 * {resDoc rdf:type ?x, ?x disableContent "true" }
		 * ...
		 * i.e not conflicting types
		 */

		/* query resDoc for hs:type that have nav:disableContent set to false 
		 * 
		 * query { resDoc rdf:type ?x ,  ?x nav:disableContent "false" } @(?x)
		 */
		RDFNode[] contentTypes =
			termVisitor.getRDFContainer().queryExtract(
				new Statement[] {
					new Statement(resDoc, Constants.s_rdf_type, Utilities.generateWildcardResource(1)),
					new Statement(
						Utilities.generateWildcardResource(1),
						NavigationConstants.s_nav_disableContent,
						new Literal("false"))},
				new Resource[] { Utilities.generateWildcardResource(1)},
				new Resource[] { Utilities.generateWildcardResource(1)});

		if (contentTypes != null) {
			// type wants content disabled
			return;
		}

		/* if the resDoc itself has "nav:disableContent" set to false then skip indexing
		 * this is the case when resDoc is actually a "rdf:type"
		 */
		if (termVisitor
			.getRDFContainer()
			.contains(new Statement(resDoc, NavigationConstants.s_nav_disableContent, new Literal("false")))) {
			return;
		}

		/* assert(enableCaching is true) */

		/* extract the content using the content client and
		 * feed it to the lucene agent.
		 */
		ContentClient cc = ContentClient.getContentClient(resDoc, termVisitor.getRDFContainer(), termVisitor.getSA());
		termVisitor.visitContent(LuceneAgent.s_str_lucene_content, cc);
	}

	/**
	 * for a given Resource [resDoc] traverse all predicates, and add all objects
	 * linked to this resource to the index.   I.e. index all resources/literals that is one predicate away.
	 * Also add any reverse object references (hs:reversePred). 
	 * @param termVisitor - object that implements methods for indexing literals encountered.
	 * @param resDoc
	 * @throws Exception
	 */
	static public void visitDocument(DocumentVisitor termVisitor, Resource resDoc) throws Exception {

		//s_logger.info("Beg visitDocument: " + ((StackBasedPrefixDocumentVisitorBase)termVisitor).getFieldPrefix() + "**" + resDoc);
		//System.err.print("[" + ((StackBasedPrefixDocumentVisitorBase)termVisitor).getFieldPrefix() + "**" + resDoc);

		termVisitor.visitResource(LuceneAgent.s_str_lucene_uri, resDoc);

		// query resDoc for all forward statements
		// query { resDoc, ?x, ?y } @(?x, ?y)
		Set results =
			termVisitor
				.getRDFContainer()
				.query(
					new Statement[] {
						 new Statement(
							resDoc,
							Utilities.generateWildcardResource(1),
							Utilities.generateWildcardResource(2))},
					Utilities.generateWildcardResourceArray(2),
					new Resource[] {
		});

		Iterator it = results.iterator();
		while (it.hasNext()) {
			RDFNode[] statement = (RDFNode[]) it.next();
			Resource pred = (Resource) statement[0];
			RDFNode val = statement[1];

			// don't add predicates that are non navigable 
			if (termVisitor
				.getRDFContainer()
				.contains(new Statement(pred, Constants.s_rdf_type, NavigationConstants.s_nav_NonNavigableProperty))) {
				continue;
			}

			// act based on the value of the statement
			if (val instanceof Literal) {
				//s_logger.info("entering visitText for: " + val);
				// getContent instead of toString, since we are expecting text here
				termVisitor.visitText(pred.toString(), val.getContent());
			} else {
				// we have a resource in 'val'
				//s_logger.info("entering visitResource for: " + val);
				termVisitor.visitResource(pred.toString(), (Resource) val);
			}
		}

		// query resDoc for all reverse statements
		// query { ?x ?y resDoc, ?y hs:reversiblePred ?z } @{ ?x, ?y, ?z }
		results =
			termVisitor
				.getRDFContainer()
				.query(
					new Statement[] {
						new Statement(
							Utilities.generateWildcardResource(1),
							Utilities.generateWildcardResource(2),
							resDoc),
						new Statement(
							Utilities.generateWildcardResource(2),
							Constants.s_haystack_reversiblePred,
							Utilities.generateWildcardResource(3))},
					Utilities.generateWildcardResourceArray(3),
					new Resource[] {
		});

		it = results.iterator();
		while (it.hasNext()) {
			RDFNode[] statement = (RDFNode[]) it.next();
			Resource pred = (Resource) statement[1];   //?y
			//Resource pred = (Resource) statement[2];  //?z
			RDFNode val = statement[0];                 //?x

			// don't add predicates that are non navigable 
			if (termVisitor
				.getRDFContainer()
				.contains(new Statement(pred, Constants.s_rdf_type, NavigationConstants.s_nav_NonNavigableProperty))) {
				continue;
			}

			// we can only have a resource in 'val'
			//s_logger.info("entering reverse visitResource for: " + val);
			termVisitor.visitReverseResource(pred.toString(), (Resource) val);
		}



		/*
		 * TODO [vineet]: we need to allow general extension here
		 */
		//System.err.print("-");
		//s_logger.info("Checking paths:  " + resDoc);
		visitPathsForDoc(termVisitor, resDoc);

		//s_logger.info("Checking conten: " + resDoc);
		visitContentForDoc(termVisitor, resDoc);
		//System.err.print("]");

		//s_logger.info("End visitDocument: " + fieldPrefix + "**" + resDoc);
	}

	/**
	 * 
	 * @param termVisitor
	 * @param resDoc
	 * @throws RDFException
	 * @throws AdenineException
	 */
	static protected void visitPathsForDoc(DocumentVisitor termVisitor, Resource resDoc)
		throws RDFException, AdenineException {

		// look for paths applicable to resDoc
		// if document is of type that has a navigable property 
		/*
		 * resObj hs:member ?x{resDoc} , 
		 * ?x{resDoc} rdf:type ?type{0} ,
		 * ?navPathPred{1} navView:domain ?type{0} , 
		 * ?navProp{2} navView:path ?navPathPred{1} ,
		 *  
		 * @(?navProp)
		 * 
		 * ?x = resDoc
		 * ?type = (Resource) val
		 * 
		 * This looks for properties of types of resDoc that 
		 * specifies what paths to look further down on
		 *
		 add { :x navView:path	${	rdf:type			navView:NavigationPath ;
							navView:domain		mail:Message ;
							navView:rangePath	@( mail:body ) 
						}
						
          :x is navProp{2} , ${... navView:domain		mail:Message..} is ?navPathPred,
          mailMessage is ?type, 
		 */
		Set navPathsForDoc =
			termVisitor.getRDFContainer().query(
				new Statement[] {
					new Statement(resDoc, Constants.s_rdf_type, Utilities.generateWildcardResource(1)),
					new Statement(
						Utilities.generateWildcardResource(2),
						NavigationConstants.s_nav_domain,
						Utilities.generateWildcardResource(1)),
					new Statement(
						Utilities.generateWildcardResource(3),
						NavigationConstants.s_nav_path,
						Utilities.generateWildcardResource(2))},
				Utilities.generateWildcardResourceArray(3),
				Utilities.generateWildcardResourceArray(3));
		Iterator navPathsIt = navPathsForDoc.iterator();
		while (navPathsIt.hasNext()) {
			RDFNode[] navPathQueryResult = (RDFNode[]) navPathsIt.next();
			Resource navProp = (Resource) navPathQueryResult[2];
			Resource navPathPred = (Resource) navPathQueryResult[1];

			//s_logger.info("considering navProp: " + navProp + " for doc: " + resDoc);

			// we need to travese nav path and use it to add fields
			ConditionSet queryStatement = new ConditionSet();
			Resource valueWildcard = NavigationService.UniqueWildcard();

			NavigationService.addCondition(
				termVisitor.getRDFContainer(),
				queryStatement,
				NavigationService.getPrimaryQueryWildcard(),
				AdenineConstants.identity,
				resDoc);

			/*
			 * TODO [vineet]: we are using ...PathPred instead of Prop so that we don't
			 * have to worry with multiple types - should we revert once it is 
			 * implemented?
			 */
			ConditionSet cs =
				NavigationService.buildQueryCondFromNavPathPred(
					termVisitor.getRDFContainer(),
					navPathPred,
					valueWildcard,
					false);
			queryStatement.addAll(cs);
			//s_logger.info("queryStatement: " + queryStatement);

			Set existentialsSet = new HashSet();
			AskFunction.findWildcardsInConditionSet(cs, existentialsSet);
			Resource[] existentials = new Resource[existentialsSet.size()];
			existentialsSet.toArray(existentials);

			DynamicEnvironment denv = new DynamicEnvironment(termVisitor.getRDFContainer(), termVisitor.getSA());
			denv.setInstructionSource(termVisitor.getRDFContainer());

			//s_logger.info("Asking: " + queryStatement);
			Set pathDocs =
				new DefaultQueryEngine().query(
					denv,
					queryStatement,
					true,
					new Resource[] { valueWildcard },
					existentials);
			Iterator pathDocsIt = pathDocs.iterator();
			while (pathDocsIt.hasNext()) {
				try {
					Resource childDoc = (Resource) ((RDFNode[]) pathDocsIt.next())[0];
					//s_logger.info("visitPathsForDoc: " + resDoc + " child: " + childDoc);
					termVisitor.enter(navProp);//push field on to the stack
					if (childDoc.equals(resDoc)) {
						s_logger.info("childDoc==resDoc : " + resDoc);
						s_logger.info("navProp: " + navProp);
						s_logger.info("navPathPred: " + navPathPred);
						s_logger.info("queryStatement: " + queryStatement);
						s_logger.info("valueWildcard: " + valueWildcard);
						s_logger.info("");
					}
					visitDocument(termVisitor, childDoc);
					termVisitor.exit(); // pop field off the stack
				} catch (Exception e) {
					s_logger.error("Error following pred: " + navProp + " for doc: " + resDoc, e);
				}
			}
		}
	}
}
