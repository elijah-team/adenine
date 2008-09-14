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

package edu.mit.lcs.haystack.server.extensions.navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.query.Condition;
import edu.mit.lcs.haystack.adenine.query.ConditionSet;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.DataProviderConsumerConnection;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFEventSource;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;

/**
 * Manages, Launches and tracks the navigation framework
 * 
 * @version 	1.0
 * @author		Vineet Sinha <vineet@ai.mit.edu>
 */
public class NavigationService extends GenericService implements IService {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(NavigationService.class);
	
	
	private static final Resource s_primaryQueryWildcard = new Resource(Constants.s_wildcard_namespace + 'x');
	public static Resource getPrimaryQueryWildcard() {
		return s_primaryQueryWildcard;
	}
	private static Set getNavImpExistentials() {
		Set retVal = new HashSet();
		retVal.add(getPrimaryQueryWildcard());
		return retVal;
	}

	/*
	 * TODO[vineet]: consider moving most of these functions into 
	 * NavigationUtilities.java
	 */

	public static Resource UniqueWildcard() {
		return new Resource(Constants.s_wildcard_namespace + Utilities.generateUniqueIdentifier());
	}

	/**
	 * Creates a <code>Condition</code> and adds it to the given 
	 * <code>ConditionSet</code>
	 */
	public static void addCondition(
		IRDFContainer source,
		ConditionSet tgtCS,
		Resource subj,
		Resource pred,
		RDFNode value)
		throws RDFException {

		if (source.contains(new Statement(pred, Constants.s_rdf_type, NavigationConstants.s_nav_NavigableProperty))) {
			tgtCS.addAll(buildQueryCondFromNavProp(source, pred, value, false));
		} else {
			ArrayList lstParam = new ArrayList(2);
			lstParam.add(subj);
			lstParam.add(value);
			tgtCS.add(new Condition(pred, lstParam));
		}
	}

	/**
	 * Creates a <code>Condition</code> and adds it to the given 
	 * <code>ConditionSet</code>
	 */
	public static void addCondition(
		IRDFContainer source,
		ConditionSet tgtCS,
		Resource subj,
		Resource pred,
		RDFNode value,
		boolean reversePred)
		throws RDFException {

		if (reversePred) {
			addCondition(source, tgtCS, (Resource) value, pred, subj);
		} else {
			addCondition(source, tgtCS, subj, pred, value);
		}
	}

	///**
	// * Creates a <code>Condition</code> and adds it to the given 
	// * <code>ConditionSet</code>
	// */
	//public static void addCondition(
	//	IRDFContainer source,
	//	ConditionSet tgtCS,
	//	Resource subj,
	//	Resource pred,
	//	Resource pred2,
	//	RDFNode value)
	//	throws RDFException {
	//
	//	if (source.contains(new Statement(pred, Constants.s_rdf_type, NavigationConstants.s_nav_NavigableProperty))) {
	//		Resource newWildcard = NavigationService.UniqueWildcard();
	//		addCondition(source, tgtCS, subj, pred, newWildcard);
	//		addCondition(source, tgtCS, newWildcard, pred2, value);
	//	} else {
	//		ArrayList lstParam = new ArrayList(2);
	//		lstParam.add(pred2);
	//		lstParam.add(subj);
	//		lstParam.add(value);
	//		tgtCS.add(new Condition(pred, lstParam));
	//	}
	//}

	public static ConditionSet getSingleCondNestedDifferenceCS(ConditionSet inQueryCS) {
		if (inQueryCS.count() > 1) {
			return null;
		}
		Condition inCond = inQueryCS.get(0);
		if (inCond.getFunction().equals(AdenineConstants.setDifference)) {
			return (ConditionSet) inCond.getParameter(2);
		}
		return null;
	}

	public static ConditionSet negateQueryCond(ConditionSet inQueryCS) {

		// minor optimization for common case: neg of a neg cancels out
		ConditionSet nestCheck = getSingleCondNestedDifferenceCS(inQueryCS);
		if (nestCheck != null) {
			return nestCheck;
		}
		// else return %{ adenine:setDifference ?x ?x inQueryCond }
		ArrayList lstParam = new ArrayList(3);
		lstParam.add(getPrimaryQueryWildcard());
		lstParam.add(getPrimaryQueryWildcard());
		lstParam.add(inQueryCS);
		ConditionSet retCS = new ConditionSet();
		retCS.add(new Condition(AdenineConstants.setDifference, lstParam));
		return retCS;

		/*
		## initial/alternative implementation:
		# prime inQueryCond [convert any ?x in inQueryCond to ?xprime]
		= outQueryCond %{}
		for cond in inQueryCond
			= params (cond.getParameters)
			= primeParams (ArrayList)
			for param in params
				if (== param (Resource (append Constants.s_wildcard_namespace 'x')))
					primeParams.add (Resource (append Constants.s_wildcard_namespace 'xprime'))
				else
					primeParams.add param
			outQueryCond.add (Condition (cond.getFunction) primeParams)
		outQueryCond.addAll %{adenine:setdifference ?x ?xprime}
		return outQueryCond
		 */

	}

	/**
	 * Creates multiple <code>Condition</code> based on given list of predicates
	 * and adds it to the given <code>ConditionSet</code>. i.e. chained version
	 * of <code>addCondition</code>. Additionally adds text:contains when given
	 * <code>containingTextQuery</code> to be <code>true</code>
	 */
	public static ConditionSet addConditionsFromChainedPredList(
		IRDFContainer source,
		ConditionSet queryCS,
		Resource subj,
		Resource listPred,
		RDFNode value,
		boolean containingTextQuery)
		throws RDFException {

		if (!source.contains(new Statement(listPred, Constants.s_rdf_type, Constants.s_daml_List))) {
			addCondition(source, queryCS, subj, listPred, value);
			return queryCS;
		}

		Resource prevWildcard = subj;
		Resource newWildcard = null;
		boolean reverseNextPred = false;

		Resource pathPred = (Resource) source.extract(listPred, Constants.s_daml_first, null);
		while (!source.contains(new Statement(listPred, Constants.s_daml_rest, Constants.s_daml_nil))) {
			
			if (pathPred.equals(NavigationConstants.s_nav_reverseNextPred)) {
				reverseNextPred = true;
			} else {
				newWildcard = NavigationService.UniqueWildcard();

				addCondition(source, queryCS, prevWildcard, pathPred, newWildcard, reverseNextPred);
				prevWildcard = newWildcard;
				reverseNextPred = false;
			}
			
			// follow pointer
			listPred = (Resource) source.extract(listPred, Constants.s_daml_rest, null);
			pathPred = (Resource) source.extract(listPred, Constants.s_daml_first, null);
		}

		// take care of ending functionality
		if (containingTextQuery == true) {
			/* in this case we still need to chain text:contains, so 
			 * chain and then add text:contains
			 */
			newWildcard = NavigationService.UniqueWildcard();
			addCondition(source, queryCS, prevWildcard, pathPred, newWildcard, reverseNextPred);
			addCondition(source, queryCS, newWildcard, Constants.s_text_contains, value);
		} else {
			addCondition(source, queryCS, prevWildcard, pathPred, value, reverseNextPred);
		}

		return queryCS;
	}

	public static ConditionSet addConditionsFromChainedPredList(
		IRDFContainer source,
		ConditionSet queryCS,
		Resource subj,
		Resource listPred,
		RDFNode value)
		throws RDFException {

		return addConditionsFromChainedPredList(source, queryCS, subj, listPred, value, false);
	}

	public static IRDFContainer buildQueryStatementsFromChainedPredList(
		IRDFContainer prePathQuery,
		Resource inPathLink,
		ArrayList pathList,
		Resource outPathLink,
		IRDFContainer postPathQuery)
		throws RDFException {

		//inPathLink = UniqueWildcard();
		LocalRDFContainer pathQuery = new LocalRDFContainer();
		if (prePathQuery != null) {
			pathQuery.add(prePathQuery);
		}
		Resource pe = null;
		Resource prevPathOutLink = null;
		Resource contPathOutLink = inPathLink;

		Iterator i = pathList.iterator();
		while (i.hasNext()) {
			pe = (Resource) i.next();
			prevPathOutLink = contPathOutLink;
			contPathOutLink = UniqueWildcard();
			pathQuery.add(new Statement(prevPathOutLink, pe, contPathOutLink));
		}
		if (pe != null) {
			pathQuery.replace(prevPathOutLink, pe, null, outPathLink);
		}
		if (postPathQuery != null) {
			pathQuery.add(postPathQuery);
		}
		return pathQuery;
	}

	/*
	 * returned ConditionSet has two variables qx{?x} which 'the from'
	 *  and 'value' which represents 'the to' of the path
	 * 
	 * buildQueryCondFromNavPathPred   
	 */
	public static ConditionSet buildQueryCondFromNavProp(
		IRDFContainer source,
		Resource navProp,
		RDFNode value,
		boolean containingTextQuery)
		throws RDFException {

		// VS:TODO support multiple paths [right now we assume there is only one path]

		Resource navPathPred = (Resource) source.extract(navProp, NavigationConstants.s_nav_path, null);

		return buildQueryCondFromNavPathPred(source, navPathPred, value, containingTextQuery);
	}

	public static ConditionSet buildQueryCondFromNavPathPred(
		IRDFContainer source,
		Resource navPathPred,
		RDFNode value,
		boolean containingTextQuery)
		throws RDFException {

		Resource qx = NavigationService.getPrimaryQueryWildcard();

		Resource navRangePath = (Resource) source.extract(navPathPred, NavigationConstants.s_nav_rangePath, null);
		Resource pathType = (Resource) source.extract(navPathPred, NavigationConstants.s_nav_domain, null);

		ConditionSet queryStmt = new ConditionSet();
		addCondition(source, queryStmt, qx, Constants.s_rdf_type, pathType);

		//s_logger.info("0: navRangePath: " + navRangePath);

		return addConditionsFromChainedPredList(
			source,
			queryStmt,
			getPrimaryQueryWildcard(),
			navRangePath,
			value,
			containingTextQuery);

	}

	private static ConditionSet optimizeCS(IRDFContainer source, Set impExistentials, ConditionSet inCS)
		throws RDFException {

		try {

			// simple optimization, remove Conditions that have the property navView:resolveAsIdentity true on them

			Map varMappings = new HashMap();
			Set remConditions = new HashSet();

			// support impExistentials by having a mapping to self for all it's members
			Iterator i = impExistentials.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				varMappings.put(o, o);
			}

			// I: go through ConditionSet and add all variable substitutions to a Map and Condition removal to a Set
			i = inCS.iterator();
			while (i.hasNext()) {
				Condition currCond = (Condition) i.next();

				if (!source
					.contains(
						new Statement(
							currCond.getFunction(),
							NavigationConstants.s_nav_resolveAsIdentity,
							new Literal("true")))) {
					// no need to do optimization in this case
					continue;
				}

				// get a dest param to map for current condition
				Object curCondDst = null;
				Iterator curCondParamI = currCond.getParameterIterator();
				while (curCondParamI.hasNext()) {
					Resource param = (Resource) curCondParamI.next();
					if (varMappings.containsKey(param)
						|| (param.getURI().indexOf(Constants.s_wildcard_namespace) != 0)) {
						// it is important if there is already a mapping from it or if it is not a wildcard
						if (curCondDst == null) {
							curCondDst = param;
						} else {
							continue; // there is more than one important param. in here, so just leave removing this
						}
					}
				}
				if (curCondDst == null) {
					curCondDst = (Resource) currCond.getParameter(0); // well just default it to the first one
				}
				if (varMappings.containsKey(curCondDst)) {
					// it was imp. bec. of a mapping from it, then we need to map to its destination
					//  otherwise, we will be mapping to self and over-riding what was making it important
					curCondDst = varMappings.get(curCondDst);
				}

				// add the mappings
				curCondParamI = currCond.getParameterIterator();
				while (curCondParamI.hasNext()) {
					varMappings.put(curCondParamI.next(), curCondDst);
				}

				// remConditions
				remConditions.add(currCond);
			}

			if (remConditions.size() == 0) {
				return inCS;
			}

			// do the translation as per remConditions and varMappings from above 
			i = inCS.iterator();
			ConditionSet outCS = new ConditionSet();
			while (i.hasNext()) {
				Condition currCond = (Condition) i.next();

				if (remConditions.contains(currCond)) {
					continue;
				}

				Object[] currCondParam = currCond.getParameters();
				for (int j = 0; j < currCondParam.length; j++) {
					Object dstMapping = currCondParam[j];
					while (varMappings.get(dstMapping) != null && dstMapping != varMappings.get(dstMapping)) {
						dstMapping = varMappings.get(dstMapping);
					}
					currCondParam[j] = dstMapping;
				}
				outCS.add(new Condition(currCond.getFunction(), new ArrayList(Arrays.asList(currCondParam))));
				//outCS.add(currCond);
			}

			return outCS;
		} catch (Exception e) {
			s_logger.error("Error while trying to optimize (continuing): " + inCS.toString(), e);
			return inCS;
		}

	}

	public static ConditionSet buildQueryCondFromNavOption(IRDFContainer source, Resource navigationOption)
		throws RDFException {

		try {
			Resource queryStmtRes =
				(Resource) source.extract(navigationOption, NavigationConstants.s_nav_queryStatement, null);
			if (queryStmtRes != null) {
				return new ConditionSet(queryStmtRes, source);
			}

			Resource queryletRes =
				(Resource) source.extract(navigationOption, NavigationConstants.s_nav_querylet, null);
			if (queryletRes != null) {
				return new ConditionSet(queryletRes, source);
			}

			// ok do real building here!
			Resource pred = (Resource) source.extract(navigationOption, NavigationConstants.s_nav_pred, null);
			RDFNode value = source.extract(navigationOption, NavigationConstants.s_nav_value, null);

			boolean containingTextQuery = false;
			if (source
				.contains(
					new Statement(
						navigationOption,
						NavigationConstants.s_nav_containingTextQuery,
						new Literal("true")))) {
				containingTextQuery = true;
			}

			if (source
				.contains(new Statement(pred, Constants.s_rdf_type, NavigationConstants.s_nav_NavigableProperty))) {
				// pred == navPathPred
				return buildQueryCondFromNavProp(source, pred, value, containingTextQuery);
			}

			ConditionSet queryStmtCS = new ConditionSet();
			//addCondition(queryStmtCS, getPrimaryQueryWildcard(), pred, value);
			addConditionsFromChainedPredList(
				source,
				queryStmtCS,
				getPrimaryQueryWildcard(),
				pred,
				value,
				containingTextQuery);

			if (source
				.contains(
					new Statement(
						navigationOption,
						NavigationConstants.s_nav_negatedNavOption,
						new Literal("true")))) {

				return negateQueryCond(optimizeCS(source, getNavImpExistentials(), queryStmtCS));
			} else {
				return optimizeCS(source, getNavImpExistentials(), queryStmtCS);
			}

		} catch (NullPointerException e) {
			s_logger.error("Unexpected error on navOption: " + navigationOption, e);
			return null;
		}

	}

	public DataProviderConsumerConnection m_NavSourceDPCC;
	
	private DynamicEnvironment m_denv;

	// used to copy data to the task pane
	protected RDFListener m_MethodsRDFL;
	
	protected Resource s_taskActiveTask = new Resource("http://haystack.lcs.mit.edu/schemata/task#activeTask");


	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.service.IService#shutdown()
	 */
	public void shutdown() throws ServiceException {
		if (m_NavSourceDPCC != null) {
			m_NavSourceDPCC.dispose();
		}
		m_NavSourceDPCC = null;

		super.shutdown();
	}
	
	public void setNavigationSource(Context context, Resource navSource) throws RDFException {
		m_infoSource.replace(m_serviceResource, new Resource("navSource"), null, navSource);			

		if (m_denv == null) {
			m_denv = new DynamicEnvironment(m_infoSource);
			Ozone.initializeDynamicEnvironment(m_denv, context);
		}
	}

	public void initSourceConnection(Context context) throws RDFException {
		// support for service restarts
		if (m_denv == null) {
			m_denv = new DynamicEnvironment(m_infoSource);
			Ozone.initializeDynamicEnvironment(m_denv, context);
		}

		Resource navSource = (Resource) m_infoSource.extract(m_serviceResource, new Resource("navSource"), null);
		
		if (navSource==null) {
			s_logger.error("initSourceConnection with null navSource", new Exception());
			return;
		}
		
		//Context context = (Context) m_denv.getValue("__context__");
		m_NavSourceDPCC = new DataProviderConsumerConnection(context, m_infoSource);
		m_NavSourceDPCC.connect(navSource, new IDataConsumer() {
			public void reset() {}
			public void onStatusChanged(Resource status) {}
			public void onDataChanged(Resource changeType, Object change) throws IllegalArgumentException {
				//s_logger.info("******************* NavigationService.onDataChanged");
				if (changeType.equals(DataConstants.RESOURCE_CHANGE)) {
					onNavigate((Resource)change);
				}
			}
		});
		
	}
	
	/**
	 * Method is called for any navigation event fired by navSource given in 
	 * setNavigationSource 
	 * 
	 * @param resource
	 */
	protected void onNavigate(Resource currRes) {
		try {
			// clean previous page listeners
			if (m_MethodsRDFL != null) {
				m_MethodsRDFL.stop();

				clearNavAdvisorsFromTaskPane();
			}

			// launch framework
			launchFramework(currRes);
		} catch (RDFException e) {
			s_logger.error("Unexpected error", e);
		}
	}
	private void clearNavAdvisorsFromTaskPane() throws RDFException {
		// remove old items from the task pane
		Set results =
			m_infoSource.query(
				new Statement[] {
					new Statement(m_userResource, s_taskActiveTask, Utilities.generateWildcardResource(1)),
					new Statement(
						Utilities.generateWildcardResource(1),
						Constants.s_rdf_type,
						NavigationConstants.s_nav_GroupedNavigationAdvisor)},
				Utilities.generateWildcardResourceArray(1),
				Utilities.generateWildcardResourceArray(1));
		
		Iterator j = results.iterator();
		while (j.hasNext()) {
			RDFNode[] nodes = (RDFNode[]) j.next();
			m_infoSource.remove(new Statement(m_userResource, s_taskActiveTask, nodes[0]), new Resource[0]);
		}
	}
	
	public void toggleSourceConnection(Context context) throws RDFException {
		if (m_NavSourceDPCC == null) {
			initSourceConnection(context);
			launchFramework();
		} else {
			m_NavSourceDPCC.dispose();
			m_NavSourceDPCC = null;
			clearNavAdvisorsFromTaskPane();
		}
	}

	/**
	 * Performs basic checks, sets flags and forks a thread for actually calculating 
	 * the navigation suggestions.
	 */
	public void launchFramework() {
		try {
			// get current resource
			Resource currRes = (Resource) m_NavSourceDPCC.m_provider.getData(DataConstants.RESOURCE, null);
			if (currRes != null) {
				launchFramework(currRes);
			}
		} catch (DataNotAvailableException e) {
			s_logger.error("Error while trying to launch navigation framework", e);
		}
	}

	/**
	 * Performs basic checks, sets flags and forks a thread for actually calculating 
	 * the navigation suggestions.
	 * 
	 * This methods also tests to see if the navigation methods have already 
	 * been calculated (if yes the results are copied), otherwise a listener 
	 * is set up and the framework is launched.
	 */
	public void launchFramework(Resource currRes) {
		try {
			Resource navAgents = getNavigationAgentRes(currRes);
			launchFramework(currRes, navAgents, new ArrayList());
		} catch (RDFException e) {
			s_logger.error("Error while trying to launch navigation framework", e);
		}
	}

	/**
	 * Performs basic checks, sets flags and forks a thread for actually calculating 
	 * the navigation suggestions.
	 * 
	 * @param currRes - current page
	 * @param navAgents - the resource of which the methods will be
	 * @param navCleanAgents - list of methods to call at the end of running every agent
	 */
	public void launchFramework(
		final Resource currRes,
		final Resource navAgents,
		final List navCleanAgents)
		throws RDFException {

		/*
		 * don't need to calculate navigation options if they are already being 
		 * calculated for the main pane. Ideally we want to see the current 
		 * type of view of resObj, but for now we will see if any of the views 
		 * of resObj have the property 
		 */
		RDFNode[] navOptionView = m_infoSource.queryExtract(new Statement[] {
			new Statement(currRes, Constants.s_haystack_view, Utilities.generateWildcardResource(1)),
			new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, Utilities.generateWildcardResource(2)),
			new Statement(Utilities.generateWildcardResource(2), NavigationConstants.s_navPane_embedsNavOptions, new Literal("true"))
		}, Utilities.generateWildcardResourceArray(2), Utilities.generateWildcardResourceArray(2));
		
		if (navOptionView != null) {
			if (null == m_infoSource.extract(navAgents, Constants.s_haystack_member, null)) {
				Resource member = Utilities.generateUniqueResource();
				m_infoSource.add(navAgents, Constants.s_haystack_member, member);
				m_infoSource.add(member, Constants.s_rdf_type, NavigationConstants.s_nav_PromptNavigationAdvisor);
				m_infoSource.add(member, Constants.s_dc_title, new Literal("Navigation options are shown in the main pane!"));
				m_infoSource.add(member, NavigationConstants.s_navView_order, new Literal("0"));
				return;
			}
		}


		// listen for changes in framework and copy to active task
		m_MethodsRDFL = new RDFListener(m_serviceManager, (IRDFEventSource) m_infoSource) {
			public void statementAdded(Resource cookie, Statement s) {
				try {
					m_infoSource.add(m_userResource, s_taskActiveTask, s.getObject());
				} catch (RDFException e) {
					s_logger.info("Error keeping navigation advisors and views in synch");
				}
			}

			public void statementRemoved(Resource cookie, Statement s) {
				try {
					m_infoSource.remove(new Statement(m_userResource, s_taskActiveTask, s.getObject()), new Resource[0]);
				} catch (RDFException e) {
					s_logger.info("Error keeping navigation advisors and views in synch");
				}
			}
		};
		m_MethodsRDFL.addPattern(navAgents, Constants.s_haystack_member, null);
		m_MethodsRDFL.start();

		
		// finally launch the framework
		boolean tmpUpdateOnlyNonCachedResults = false;
		if (null != m_infoSource.extract(navAgents, Constants.s_haystack_member, Utilities.generateWildcardResource(1))) {
			tmpUpdateOnlyNonCachedResults = true;
			// return;
		}
		final boolean updateOnlyNonCachedResults = tmpUpdateOnlyNonCachedResults;
	
		// disable chaching for debugging purposes
		//= updateOnlyNonCachedResults false
	
		//print 'supposed to do:' :setNavAgents resObj navAgents updateOnlyNonCachedResults navCleanAgents
		
		/*
		 * Since a number of the threads are going to run for a second 
		 * (because of cached data, we should really run the threads only 
		 * after creating a list of the navigation advisors and finding that 
		 * there are going to be more than zero.  
		 */
		Thread navThread = new Thread() {
			public void run() {
				setPriority(Thread.NORM_PRIORITY - 2);
				// :setNavAgents resObj navAgents updateOnlyNonCachedResults navCleanAgents
				try {
					//s_logger.info("calc nav methods beg: updateOnlyNonCachedResults " + updateOnlyNonCachedResults);
					
					NavigationFramework navFmwk =
						new NavigationFramework(
							m_infoSource,
							m_denv,
							currRes,
							navAgents,
							new Boolean(updateOnlyNonCachedResults),
							navCleanAgents);

					navFmwk.setNavAgents();
					
					//ns.m_inter.callMethod(
					//	NavigationConstants.s_navPane_setNavAgents,
					//	new Object[] { currRes, navAgents, new Boolean(updateOnlyNonCachedResults), navCleanAgents },
					//	m_denv);
					//s_logger.info("calc nav methods end");
				} catch (Exception e) {
					s_logger.error("Error launching framework", e);
				}
			}
		};
		navThread.setName("Navigation");
		navThread.start();
	}

	
	private Resource getNavigationAgentRes(Resource currRes) throws RDFException {
		Resource navPaneUI =
			(Resource) m_infoSource.extract(null, NavigationConstants.s_navPane_navigationPaneUIOf, currRes);
			
		if (navPaneUI != null) {
			/* cached (so we need to copy data onto task pane)
			 * 
			 * we also need to make sure that we only copy enabled agents
			 */
			Resource navigationAgents =
				(Resource) m_infoSource.extract(navPaneUI, NavigationConstants.s_navPane_navigationAgents, null);
		
			Set results =
				m_infoSource.query(
					new Statement[] {
						new Statement(
							navigationAgents,
							Constants.s_haystack_member,
							Utilities.generateWildcardResource(1)),
						new Statement(
						Utilities.generateWildcardResource(1),
						NavigationConstants.s_nav_agentSource,
						Utilities.generateWildcardResource(2)
							)},
					Utilities.generateWildcardResourceArray(2),
					Utilities.generateWildcardResourceArray(2));
		
			Iterator j = results.iterator();
			while (j.hasNext()) {
				RDFNode[] nodes = (RDFNode[]) j.next();
				Resource navAdvisorRes = (Resource) nodes[0];
				Resource navAgentSrc = (Resource) nodes[1];
				if (m_infoSource
					.contains(
						new Statement(
							navAgentSrc,
							NavigationConstants.s_navSvc_enabled,
							new Literal("true")))) {
					m_infoSource.add(m_userResource, s_taskActiveTask, navAdvisorRes);
				}
			}
			return navigationAgents;
		} else {
			// not cached (generate)
			navPaneUI = Utilities.generateUniqueResource();
			Resource navigationAgents = Utilities.generateUniqueResource();
			m_infoSource.add(navPaneUI, NavigationConstants.s_navPane_navigationPaneUIOf, currRes);
			m_infoSource.add(navPaneUI, NavigationConstants.s_navPane_navigationAgents, navigationAgents);
			return navigationAgents;
		}
	}

	
}
