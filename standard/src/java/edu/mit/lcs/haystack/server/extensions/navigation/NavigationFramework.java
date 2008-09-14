/*
 * Created on Mar 2, 2004
 *
 */
package edu.mit.lcs.haystack.server.extensions.navigation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * Represents the blackboard used for launching each navigation agent, this 
 * class is typically instantiated throughs NavigationService
 * 
 * @author vineet
 */
public class NavigationFramework {

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(NavigationFramework.class);
	static org.apache.log4j.Logger s_logger2 = org.apache.log4j.Logger.getLogger(NavigationFramework.class.getName() + ".2");

	private DynamicEnvironment m_denv;
	private Interpreter m_inter;
	private IRDFContainer m_infoSource;

	public Resource m_resObj;
	public Resource m_navAgents;
	public Boolean m_updateOnlyNonCachedResults;
	public List m_navCleanAgents;

	/**
	 * @param m_infoSource
	 */
	protected NavigationFramework(
		IRDFContainer infoSource,
		DynamicEnvironment denv,
		Resource resObj,
		Resource navAgents,
		Boolean updateOnlyNonCachedResults,
		List navCleanAgents) {

		m_infoSource = infoSource;
		m_inter = new Interpreter(m_infoSource);
		m_denv = denv;

		m_resObj = resObj;
		m_navAgents = navAgents;
		m_updateOnlyNonCachedResults = updateOnlyNonCachedResults;
		m_navCleanAgents = navCleanAgents;
	}

	/**
	 * Goes through the various nav agents and calls them for adding options on the current object
	 * 
	 * @param currRes
	 * @param navAgents
	 * @param updateOnlyNonCachedResults
	 * @param navCleanAgents
	 */
	protected void setNavAgents() throws RDFException, AdenineException {

		Set navAgentProviders = null;
		boolean updateOnlyNonCachedResultsBool = m_updateOnlyNonCachedResults.booleanValue();

		// the providers for this type
		Set curObjTypes =
			m_infoSource.query(
				new Statement(m_resObj, Constants.s_rdf_type, Utilities.generateWildcardResource(1)),
				Utilities.generateWildcardResourceArray(1));
				
		Iterator curObjTypeIt = curObjTypes.iterator();
		while (curObjTypeIt.hasNext()) {
			Resource curObjType = (Resource) ((RDFNode[])curObjTypeIt.next())[0];
			getTypeMethodsAndCall(m_resObj, m_navAgents, updateOnlyNonCachedResultsBool, curObjType);
		}
		getTypeMethodsAndCall(m_resObj, m_navAgents, updateOnlyNonCachedResultsBool, Constants.s_daml_Thing);
		
		if (m_navCleanAgents != null) {
			Iterator agentIt = m_navCleanAgents.iterator();
			while (agentIt.hasNext()) {
				callNavAgent((Resource) agentIt.next(), m_resObj, m_navAgents);
			}
		}
		

	}

	private void getTypeMethodsAndCall(
		Resource resObj,
		Resource navAgents,
		boolean updateOnlyNonCachedResults,
		Resource curObjType)
		throws RDFException, AdenineException {
			
		Set navAgentProviders;
		if (updateOnlyNonCachedResults) {
			navAgentProviders =
				m_infoSource.query(
					new Statement[] {
						new Statement(
							Utilities.generateWildcardResource(1),
							NavigationConstants.s_navSvc_enabled,
							new Literal("true")),
						new Statement(
							Utilities.generateWildcardResource(1),
							NavigationConstants.s_navView_NavAgentDomain,
							curObjType),
						new Statement(
							Utilities.generateWildcardResource(1),
							NavigationConstants.s_nav_disableDataCache,
							new Literal("true"))},
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1));
		} else {
			navAgentProviders =
				m_infoSource.query(
					new Statement[] {
						new Statement(
							Utilities.generateWildcardResource(1),
							NavigationConstants.s_navSvc_enabled,
							new Literal("true")),
						new Statement(
							Utilities.generateWildcardResource(1),
							NavigationConstants.s_navView_NavAgentDomain,
							curObjType)},
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1));
		}
		
		Iterator navAgentProviderIt = navAgentProviders.iterator();
		while (navAgentProviderIt.hasNext()) {
			Resource navAgentProvider = (Resource) ((RDFNode[]) navAgentProviderIt.next())[0];
			callNavAgent(navAgentProvider, resObj, navAgents);
		}
	}
	
	private Resource m_currentNavAgent = null;

	/**
	 * @param navAgentProvider
	 * @param resObj
	 * @param navAgents
	 */
	private void callNavAgent(Resource navAgentProvider, Resource resObj, Resource navAgents) throws RDFException {
		//print (adenine:currTime) 'calling navAgent: ' navMethProvider
		
		progressLog("calling navAgent: " + navAgentProvider);
		m_currentNavAgent = navAgentProvider;
		try {
			m_inter.callMethod(navAgentProvider, new Object[] { resObj, this }, m_denv);
		} catch (AdenineException e) {
			s_logger.error("Error calling navAgent: " + navAgentProvider, e);
		}
		m_currentNavAgent = null;
		progressLog("called navAgent: " + navAgentProvider);
		
		// call nav Meth's depending on this
		Set childNavAgentProviders =
			m_infoSource.query(
				new Statement(
					Utilities.generateWildcardResource(1),
					NavigationConstants.s_navView_NavAgentDomain,
					navAgentProvider),
				Utilities.generateWildcardResourceArray(1));
				
		Iterator it = childNavAgentProviders.iterator();
		while (it.hasNext()) {
			RDFNode[] childNavAgentProvider = (RDFNode[]) it.next();
			callNavAgent((Resource) childNavAgentProvider[0],resObj,navAgents);
		}


	}
	
	/*
	 * core framework support
	 */
	
	private Resource getExpert(Resource agentSrc, Resource expertType, Resource navAgents) throws RDFException {
		//print '** navPane:getExpert type:' expertType
		//printset (query {navAgents :initializedExpert ?x ?x navFake:NavigationExpertType ?y} @(?x ?y))
		
		// if expert is already instantiated return it, otherwise create it

		RDFNode[] expertResArr =
			m_infoSource.queryExtract(
				new Statement[] {
					new Statement(
						navAgents,
						NavigationConstants.s_navPane_initializedExpert,
						Utilities.generateWildcardResource(1)),
					new Statement(
						Utilities.generateWildcardResource(1),
						NavigationConstants.s_nav_NavigationAdvisorType,
						expertType)},
				Utilities.generateWildcardResourceArray(1),
				Utilities.generateWildcardResourceArray(1));

		if (expertResArr != null) {
			// optimize by returning the expert collection
			//#print '..found:' expertResArr[0] navAgents
			m_infoSource.add((Resource) expertResArr[0], NavigationConstants.s_nav_agentSource, agentSrc);
			return (Resource) m_infoSource.extract(
				(Resource) expertResArr[0],
				NavigationConstants.s_nav_navAgentValues,
				null);
		}
		
		// we need to create expert here
		//print 'Creating expert of type: ' expertType
		Resource expertRes = Utilities.generateUniqueResource();
		Resource expertResValues = Utilities.generateUniqueResource();
		//m_infoSource.add(expertRes, rdf:type, navView:NavigationMode);
		//m_infoSource.add(expertRes, dc:title, (extract expertType dc:title ?x));
		m_infoSource.add(expertRes, NavigationConstants.s_nav_NavigationAdvisorType, expertType);
		m_infoSource.add(expertRes, NavigationConstants.s_nav_navAgentValues, expertResValues);
		m_infoSource.add(expertResValues, Constants.s_rdf_type, Constants.s_haystack_Collection);
		m_infoSource.add(
			expertResValues,
			Constants.s_dc_title,
			m_infoSource.extract(expertType, Constants.s_dc_title, null));

		if (m_infoSource
			.contains(
				new Statement(
					expertType,
					Constants.s_rdf_type,
					NavigationConstants.s_nav_GroupedNavigationAdvisorType))) {
			m_infoSource.add(expertRes, Constants.s_rdf_type, NavigationConstants.s_nav_GroupedNavigationAdvisor);
		} else {
			m_infoSource.add(expertRes, Constants.s_rdf_type, NavigationConstants.s_nav_NavigationAdvisor);
		}

		if (null != m_infoSource.extract(expertType, NavigationConstants.s_nav_nestedTitle, null)) {
			m_infoSource.add(
				expertRes,
				Constants.s_dc_title,
				m_infoSource.extract(expertType, NavigationConstants.s_nav_nestedTitle, null));
		} else {
			m_infoSource.add(
				expertRes,
				Constants.s_dc_title,
				m_infoSource.extract(expertType, Constants.s_dc_title, null));
		}

		m_infoSource.add(navAgents, NavigationConstants.s_navPane_initializedExpert, expertRes);

		//if (m_infoSource.contains(expertType,<navStudy:disableExpertUI>,"true")
		//	return (extract expertRes navView:navAgentValues ?y)


		//expert created!
		Resource parentExpType = (Resource) m_infoSource.extract(expertType, NavigationConstants.s_nav_nestedToExpert, null);
		Resource parentColl;
		if (parentExpType != null) {
			parentColl = getExpert(agentSrc, parentExpType, navAgents);
		} else {
			parentColl = navAgents;
		}
			
		int index = m_infoSource.query(new Statement(parentColl,Constants.s_haystack_member, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1)).size();
		m_infoSource.add(expertRes, NavigationConstants.s_navView_order, new Literal(Integer.toString(index)));
		m_infoSource.add(parentColl, Constants.s_haystack_member, expertRes);
		m_infoSource.add(expertRes, NavigationConstants.s_nav_agentSource, agentSrc);
		return (Resource) m_infoSource.extract(expertRes, NavigationConstants.s_nav_navAgentValues, null);
	}

	private void removeEmptyExperts(Resource expertColl, Resource navAgents) throws RDFException {
		log("entering removeEmptyExperts: " + expertColl + " " + navAgents);
		//print '** navPane:removeEmptyExperts coll:' expertColl

		//if (!= null (extract expertColl hs:member ?x))
		//	navView:addLightCollViewWithoutPreview expertColl
		
		if (m_infoSource.extract(expertColl, Constants.s_haystack_member, null) == null) {
			Resource expertRes =
				(Resource) m_infoSource.extract(null, NavigationConstants.s_nav_navAgentValues, expertColl);
			//print 'Removing expert type:' (extract expertRes navFake:NavigationExpertType ?x)
			Resource parentExpertColl = (Resource) m_infoSource.extract(null, Constants.s_haystack_member, expertRes);
			//print 'removing expert:' expertRes
			
			//if (parentExpertColl == null) {
				//Resource expType =
				//	m_infoSource.extract(expertRes, NavigationConstants.s_nav_NavigationExpertType, null);
				
				//if (contains expType <navStudy:disableExpertUI>	"true")
				//	return

				//# this should not happen, but print an 
				//print '*******************************************************************************'
				//print 'navPane:removeEmptyExperts parentExpertColl == null'
				//print 'expertRes: ' expertRes
				//print 'expertColl: ' expertColl
				//print 'navAgents: ' navAgents
			//}

			m_infoSource
				.remove(
					new Statement(parentExpertColl, Constants.s_haystack_member, expertRes), 
					new Resource[] {}
				);
			m_infoSource
				.remove(
					new Statement(navAgents, NavigationConstants.s_navPane_initializedExpert, expertRes),
					new Resource[] {}
				);
			if (!navAgents.equals(parentExpertColl)) {
				removeEmptyExperts(parentExpertColl, navAgents);
			}
		}
		log("exiting removeEmptyExperts: " + expertColl);
	}



	/*
	 * framework methods [called by agents]
	 */ 
	 
	public void addExpertConsumer(Resource expertRes) throws RDFException {
		// I]] remove duplicates {prev. inst. of this source data}
		Resource curSource = (Resource) m_infoSource.extract(expertRes, NavigationConstants.s_nav_agentSource, null);
		Set dupNavAgents =
			m_infoSource.query(
				new Statement[] {
					new Statement(m_navAgents, Constants.s_haystack_member, Utilities.generateWildcardResource(1)),
					new Statement(
						Utilities.generateWildcardResource(1),
						NavigationConstants.s_nav_agentSource,
						curSource),
					},
				Utilities.generateWildcardResourceArray(1),
				Utilities.generateWildcardResourceArray(1));
		Iterator it = dupNavAgents.iterator();
		while (it.hasNext()) {
			RDFNode[] x = (RDFNode[])it.next();
			m_infoSource.remove(new Statement(m_navAgents, Constants.s_haystack_member, x[0]), new Resource[] {});
		}
		// II]] add expert
		m_infoSource.add(m_navAgents, Constants.s_haystack_member, expertRes);
		//print 'adding expert[' expertRes ']: ' (extract expertRes dc:title ?x)

	}

	public void addGroupedExpertConsumer(Resource expertRes) throws RDFException {
		addExpertConsumer(expertRes);

		// III] use grouping to boost ordering to ensure grouped items 
		// come in the last!
		// i.e. add a 'b' since :order is done using string comparison
		int index =
			m_infoSource
				.query(
					new Statement(m_navAgents, Constants.s_haystack_member, Utilities.generateWildcardResource(1)),
					Utilities.generateWildcardResourceArray(1))
				.size();

		m_infoSource.add(
			new Statement(expertRes, NavigationConstants.s_navPane_order, new Literal(Integer.toString(index))));
		
		/*
		if (!= true setGroupedExpert)
			# default, i.e. not grouped
			add { expertRes :order (Literal (index.toString)) }
			#add { expertRes :order (Literal (append 'a' (index.toString))) }
		else
			add { expertRes :order (Literal (index.toString)) }
			#add { expertRes :order (Literal (append 'b' (index.toString))) }
		*/
	}

	public void removeExpertConsumer(Resource expertRes) {
	}
	
	public void getExpertConsumer(Resource expertSrc) {
	}

	public Resource getExpert(Resource agentSrc, Resource expertType) throws RDFException {
		return getExpert(agentSrc, expertType, m_navAgents);
	}

	public void closeExpert(Resource expertColl) throws RDFException {
		//print 'closeExpert on:' expertColl
		removeEmptyExperts(expertColl, m_navAgents);
	}

	public void addExpertOption(Resource expertColl, Resource navOption) throws RDFException {
		//TODO[vineet]: need to merge navOption if it already exists (and boost sortOrder)
		m_infoSource.add(navOption, Constants.s_rdf_type, NavigationConstants.s_nav_NavigationOption);
		m_infoSource.add(expertColl, Constants.s_haystack_member, navOption);
	}

	public void addExpertObject(Resource expertColl, Resource navObject) throws RDFException {
		//TODO[vineet]: should consider possibly wrapping navObjects to support sort order
		//m_infoSource.add(navOption, Constants.s_rdf_type, NavigationConstants.s_nav_NavigationModeValue);
		m_infoSource.add(expertColl, Constants.s_haystack_member, navObject);
	}

	public void removeExpertOption(Resource expertColl, Resource navOption) throws RDFException {
		m_infoSource.remove(new Statement(expertColl, Constants.s_haystack_member, navOption), new Resource[] {});
	}

	public void removeAllExpertOptions(Resource expertColl) throws RDFException {
		m_infoSource.remove(
			new Statement(expertColl, Constants.s_haystack_member, Utilities.generateWildcardResource(1)),
			Utilities.generateWildcardResourceArray(1));
	}
	
	public void log(String str) {
		s_logger2.info(m_currentNavAgent + ": " + str);
	}
	
	/*
	 * logging as above, but these messages are used for indicating progress
	 */
	public void progressLog(String str) {
		s_logger2.info(m_currentNavAgent + ": " + str);
	}


}
