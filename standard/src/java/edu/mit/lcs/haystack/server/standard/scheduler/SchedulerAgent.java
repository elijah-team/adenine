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

package edu.mit.lcs.haystack.server.standard.scheduler;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.GenericService;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.server.standard.melatonin.MelatoninAgent;

import java.util.Calendar;
import java.util.Date;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class SchedulerAgent extends GenericService implements IScheduler {
	SchedulerThread m_scheduler;

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SchedulerAgent.class);

	ThreadGroup m_tg;
	protected IRDFContainer m_source;
	boolean m_done = false;

	/**
	 * @see IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
		m_done = true;
		
		m_tg.interrupt();
		
		while (m_tg.activeCount() > 0) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		super.cleanup();
	}

	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
		super.init(basePath, manager, res);
		m_tg = new ThreadGroup("Scheduler agent");
		m_scheduler = new SchedulerThread();
		m_source = manager.getRootRDFContainer();
	}

	class SchedulerThread extends Thread {
		SchedulerThread() {
			super(m_tg, "SchedulerAgent " + m_serviceResource);
			setPriority(Thread.MIN_PRIORITY);
			start();
		}
		
		public void run() {
			while (!m_done) {
				try {
					sleep(30000);
				} catch (InterruptedException ie) {
				}
				
				Resource[] tasks = Utilities.getResourceSubjects(Constants.s_rdf_type, Constants.s_scheduler_Task, m_source);
				for (int i = 0; (i < tasks.length) && !m_done; i++) {
					Date now = new Date();
					Resource task = tasks[i];
					try {
						String duration = Utilities.getLiteralProperty(task, Constants.s_scheduler_frequency, m_source);
						String timeOfDay = Utilities.getLiteralProperty(task, Constants.s_scheduler_timeOfDay, m_source);
						Resource resService = Utilities.getResourceProperty(task, Constants.s_scheduler_service, m_source);
						String lastRun = Utilities.getLiteralProperty(task, Constants.s_scheduler_lastRun, m_source);
						boolean runNow = Utilities.checkBooleanProperty(task, Constants.s_scheduler_runNow, m_source);
						if (resService == null) {
							SchedulerAgent.s_logger.error(task + " missing service specification.");
							continue;
						}
						if (duration != null) {
							// TODO[dquan]: handle XSD duration
							long l = Long.parseLong(duration);
							boolean run = true;
							if (lastRun != null) {
								long l2 = Utilities.parseDateTime(lastRun).getTime();
								if ((l + l2) > now.getTime()) {
									run = false;
								}
							}
							
							if (run || runNow) {
								MelatoninAgent.getMelatoninAgent(m_source, m_serviceManager).submitJob(new ScheduledJob(resService, task), null, true);
		
								// Record
								m_source.replace(task, Constants.s_scheduler_lastRun, null, Utilities.generateDateTime(new Date()));
								m_source.remove(new Statement(task, Constants.s_scheduler_runNow, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
							}
						} else if (timeOfDay != null) {
							Date offset = Utilities.parseTime(timeOfDay);
							Calendar current = Calendar.getInstance();
							int year = current.get(Calendar.YEAR);
							int month = current.get(Calendar.MONTH);
							int date = current.get(Calendar.DATE);
							Calendar timeToRun = Calendar.getInstance();
							timeToRun.setTime(offset);
							timeToRun.set(year, month, date);
							
							Date lastRunDate = lastRun != null ? Utilities.parseDateTime(lastRun) : null;
							if (runNow || 
								((timeToRun.getTime().compareTo(now) <= 0) &&
								(lastRunDate != null) && (lastRunDate.compareTo(timeToRun.getTime()) < 0))) {
								MelatoninAgent.getMelatoninAgent(m_source, m_serviceManager).submitJob(new ScheduledJob(resService, task), null, true);
	
								// Record
								m_source.replace(task, Constants.s_scheduler_lastRun, null, Utilities.generateDateTime(new Date()));
							} else if (lastRun == null) {
								// Record
								m_source.replace(task, Constants.s_scheduler_lastRun, null, Utilities.generateDateTime(new Date()));
							}

							m_source.remove(new Statement(task, Constants.s_scheduler_runNow, Utilities.generateWildcardResource(1)), Utilities.generateWildcardResourceArray(1));
						}
					} catch (Exception e) {
						SchedulerAgent.s_logger.error("Error processing task: " + task, e);
					}
				}
			}
		}
	}
}
