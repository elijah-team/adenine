package edu.mit.lcs.haystack.server.standard.melatonin;

import java.io.Serializable;

import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
abstract public class Job implements Serializable {
	/**
	 * The URI identifying the job.
	 */
	protected Resource m_id = Utilities.generateUniqueResource();
	
	/**
	 * The source RDF container.
	 */
	transient protected IRDFContainer m_source;
	
	/**
	 * The service accessor by which services can be reached.
	 */
	transient protected IServiceAccessor m_serviceAccessor;
	
	void initialize(IRDFContainer source, IServiceAccessor sa) {
		m_source = source;
		m_serviceAccessor = sa;
	}
	
	/**
	 * @return the job's URI.
	 */
	public Resource getID() {
		return m_id;
	}
	
	/**
	 * Runs the job.
	 */
	abstract public void run() throws Exception;
}
