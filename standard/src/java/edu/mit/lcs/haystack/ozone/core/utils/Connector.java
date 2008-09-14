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

package edu.mit.lcs.haystack.ozone.core.utils;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import java.util.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public abstract class Connector implements IPart {
	final static Resource CONNECT 			= new Resource(OzoneConstants.s_namespace + "connect");
	final static Resource TRIGGER 			= new Resource(OzoneConstants.s_namespace + "trigger");
	final static Resource SOURCES 			= new Resource(OzoneConstants.s_namespace + "sources");
	final static Resource TARGETS 			= new Resource(OzoneConstants.s_namespace + "targets");
	final static Resource INDIRECT_SOURCES 	= new Resource(OzoneConstants.s_namespace + "indirectSources");
	final static Resource INDIRECT_TARGETS 	= new Resource(OzoneConstants.s_namespace + "indirectTargets");
	
	transient protected IRDFContainer	m_source;
	transient protected IRDFContainer	m_infoSource;
	protected Context			m_context;
	
	transient protected RDFListener		m_rdfListener;
	protected Resource		m_resPartData;
	protected Resource		m_resConnect;
	protected Resource		m_resTrigger;
	protected ArrayList		m_resSources;
	protected ArrayList		m_resTargets;
	protected ArrayList		m_resIndirectSources;
	protected ArrayList		m_resIndirectTargets;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Connector.class);
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		m_source = source;
		if (m_resConnect != null && m_resTrigger != null) {
			setupListener();
		}
	}
	
	protected void setupListener() {
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource) {
			public void statementAdded(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					public void run() {
						if (m_infoSource != null) {
							translate();
							onChange();
						}
					}
				});
			}
		};
			
		m_rdfListener.start();
		try {
			m_rdfListener.addPattern(m_resConnect, m_resTrigger, null);
		} catch (Exception e) {
			s_logger.error("Failed to watch for trigger on " + m_resConnect, e);
		}
	}
		
	/**
	 * @see IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		m_source = source;
		m_context = context;
		m_infoSource = context.getInformationSource();
		
		m_resPartData = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);
		
		m_resTrigger = Utilities.getResourceProperty(m_resPartData, TRIGGER, m_source);
		
		m_resConnect = Utilities.getResourceProperty(m_resPartData, CONNECT, m_source);

		if (m_resConnect != null && m_resTrigger != null) {
			try {
				Iterator i;
				
				i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(m_resPartData, SOURCES, m_source), m_source);
				m_resSources = new ArrayList();
				while (i.hasNext()) {
					m_resSources.add(i.next());
				}

				i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(m_resPartData, TARGETS, m_source), m_source);
				m_resTargets = new ArrayList();
				while (i.hasNext()) {
					m_resTargets.add(i.next());
				}

				if (m_resSources.size() != m_resTargets.size()) {
					s_logger.error("sources and targets lists have different lengths");
					return;
				}

				i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(m_resPartData, INDIRECT_SOURCES, m_source), m_source);
				m_resIndirectSources = new ArrayList();
				while (i.hasNext()) {
					m_resIndirectSources.add(i.next());
				}

				i = ListUtilities.accessDAMLList(Utilities.getResourceProperty(m_resPartData, INDIRECT_TARGETS, m_source), m_source);
				m_resIndirectTargets = new ArrayList();
				while (i.hasNext()) {
					m_resIndirectTargets.add(i.next());
				}

				if (m_resIndirectSources.size() != m_resIndirectTargets.size()) {
					s_logger.error("indirect sources and targets lists have different lengths");
					return;
				}
			} catch (Exception e) {
				return;
			}		

			setupListener();
			
			translate();
		}
	}

	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}
		
		m_resPartData = null;
		m_resConnect = null;
		m_resSources = null;
		m_resTargets = null;
		m_resIndirectSources = null;
		m_resIndirectTargets = null;

		m_context = null;
		m_source = null;
	}
	
	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}	

	public Context getContext() {
		return m_context;
	}
	
	public abstract void onChange();
	
	private void translate() {
		Iterator iSource = m_resSources.iterator();
		Iterator iTarget = m_resTargets.iterator();
		
		while (iSource.hasNext()) {
			Resource	resSource = (Resource) iSource.next();
			Resource	resTarget = (Resource) iTarget.next();
			Object		o = Utilities.getProperty(m_resConnect, resSource, m_infoSource);

			m_context.putProperty(resTarget, o);
		}

		Interpreter i = Ozone.getInterpreter();
		DynamicEnvironment denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(denv, m_context);
		
		iSource = m_resIndirectSources.iterator();
		iTarget = m_resIndirectTargets.iterator();

		while (iSource.hasNext()) {
			Resource	resSource = (Resource) iSource.next();
			Resource	resTarget = (Resource) iTarget.next();

			try {
				Object o = i.callMethod(resSource, new Object[] { }, denv);
				m_context.putProperty(resTarget, o);
			} catch(AdenineException e) {
				s_logger.error("Failed to call method " + resSource, e);
			}
		}

	}
}
