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

package edu.mit.lcs.haystack.server.standard.adenine;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.IDereferenceable;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.InvalidMemberException;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
//import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.server.core.service.ServiceException;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AdenineService extends GenericService implements IDereferenceable, IRDFListener {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineService.class);

	Interpreter m_interpreter;
//	Context m_context = new Context();
	
	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
		super.init(basePath, manager, res);
		
		m_interpreter = new Interpreter(manager.getRootRDFContainer());
		m_interpreter.setServiceAccessor(manager);

		Resource init = getMethodImplementation(Constants.s_config_init);
		if (init == null) {
			return;
		}
		
		try {
			Message msg = new Message();
			msg.setNamedValue(Constants.s_config_service, m_serviceResource);
			m_interpreter.callMethod(init, msg, createDynamicEnvironment());
		} catch (AdenineException e) {
			s_logger.error("Error invoking " + Constants.s_config_init, e);
		}
	}
	
	protected Resource getMethodImplementation(Resource resMember) {
		// Find method implementation
		Resource method;
		try {
			IRDFContainer rdfc = m_serviceManager.getRootRDFContainer();
			RDFNode[] result = rdfc.queryExtract(new Statement[] {
				new Statement(m_serviceResource, Constants.s_config_method, Utilities.generateWildcardResource(2)),
				new Statement(Utilities.generateWildcardResource(2), Constants.s_config_operation, resMember),
				new Statement(Utilities.generateWildcardResource(2), Constants.s_config_adenineMethod, Utilities.generateWildcardResource(1))
			}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
			method = (Resource)result[0];
		} catch (Exception e) {
			return null;
		}
		return method;
	}

	/**
	 * @see IDereferenceable#getMember(Object)
	 */
	public Object getMember(Object member) throws AdenineException {
		Resource method = getMethodImplementation((Resource)member);
		if (method == null) {
			throw new InvalidMemberException(member);
		}
		
		return new ICallable() {
			Resource m_method;
			
			public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
				message.setNamedValue(Constants.s_config_service, m_serviceResource);
				denv = createDynamicEnvironment();
				return m_interpreter.callMethod(m_method, message, denv);
			}
			
			ICallable init(Resource method2) {
				m_method = method2;
				return this;
			}
		}.init(method);
	}
	
	protected DynamicEnvironment createDynamicEnvironment() {
		DynamicEnvironment denv = new DynamicEnvironment(m_serviceManager.getRootRDFContainer(), m_serviceManager);
		denv.setValue("__infosource__", m_infoSource);
		if (m_userResource != null) {
			denv.setIdentity(m_serviceManager.getIdentityManager().getUnauthenticatedIdentity(m_userResource));
		}
//		denv.setValue("__context__", m_context);
		return denv;
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener#statementAdded(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Statement)
	 */
	public void statementAdded(Resource cookie, Statement s) {
		Resource method = getMethodImplementation(Constants.s_rdfstore_onStatementAdded);
		if (method == null) {
			return;
		}

		try {
			Message msg = new Message(new Object[] { cookie, s.getSubject(), s.getPredicate(), s.getObject() });
			msg.setNamedValue(Constants.s_config_service, m_serviceResource);
			m_interpreter.callMethod(method, msg, createDynamicEnvironment());
		} catch (AdenineException e) {
			s_logger.error("Error invoking " + Constants.s_rdfstore_onStatementAdded, e);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener#statementRemoved(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.rdf.Statement)
	 */
	public void statementRemoved(Resource cookie, Statement s) {
		Resource method = getMethodImplementation(Constants.s_rdfstore_onStatementRemoved);
		if (method == null) {
			return;
		}

		try {
			Message msg = new Message(new Object[] { cookie, s.getSubject(), s.getPredicate(), s.getObject() });
			msg.setNamedValue(Constants.s_config_service, m_serviceResource);
			m_interpreter.callMethod(method, msg, createDynamicEnvironment());
		} catch (AdenineException e) {
			s_logger.error("Error invoking " + Constants.s_rdfstore_onStatementRemoved, e);
		}
	}

}
