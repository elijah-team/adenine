/*
 * Created on Jan 3, 2004
 */
package edu.mit.lcs.haystack.server.standard.scheduler;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.server.standard.adenine.AdenineService;
import edu.mit.lcs.haystack.server.standard.melatonin.Job;

/**
 * @author Dennis Quan
 */
class ScheduledJob extends Job {
	Resource m_task;
	Resource m_service;
	
	ScheduledJob(Resource service, Resource task) {
		m_task = task;
		m_service = service;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.server.melatonin.Job#run()
	 */
	public void run() throws Exception {
		// Invoke service
		Object service = m_serviceAccessor.connectToService(m_service, null);
		if (service instanceof IScheduledTask) {
			IScheduledTask st = (IScheduledTask)service;
			st.performScheduledTask(m_task);
		} else {
			AdenineService as = (AdenineService) service;
			DynamicEnvironment denv = new DynamicEnvironment(m_source, m_serviceAccessor);
			denv.setValue("__infosource__", as.getInfoSource());
			denv.setValue("__identity__", m_serviceAccessor.getIdentityManager().getUnauthenticatedIdentity(as.getUserResource()));
			((ICallable) as.getMember(Constants.s_scheduler_performScheduledTask)).invoke(new Message(new Object[] { m_task }), denv);
		}
	}
}
