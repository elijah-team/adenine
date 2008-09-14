/*
 * Created on Jan 3, 2004
 */
package edu.mit.lcs.haystack.server.standard.melatonin;

import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
public class MelatoninConstants {
	final public static String s_namespace = "http://haystack.lcs.mit.edu/agents/melatonin#";
	
	final public static Resource s_MelatoninAgent = new Resource(s_namespace + "MelatoninAgent");
	final public static Resource s_Job = new Resource(s_namespace + "Job");
	final public static Resource s_state = new Resource(s_namespace + "state");
	final public static Resource s_failed = new Resource(s_namespace + "failed");
	final public static Resource s_running = new Resource(s_namespace + "running");
	final public static Resource s_waitingToStart = new Resource(s_namespace + "waitingToStart");
	final public static Resource s_jobStore = new Resource(s_namespace + "jobStore");
	final public static Resource s_threadCount = new Resource(s_namespace + "threadCount");
	final public static Resource s_pauseBetweenJobs = new Resource(s_namespace + "pauseBetweenJobs");
}
