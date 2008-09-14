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

package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import java.util.*;

/**
 * @author David Huynh
 */
abstract public class ChainedDataProvider extends GenericDataProvider {
	protected IDataProvider			m_dataProvider;
	protected IDataConsumer			m_dataConsumer;
	protected boolean				m_ownsDataProvider = false;
	protected boolean				m_listenerCreated = false;
	
	protected ArrayList				m_dataConsumers = new ArrayList();

	transient protected RDFListener	m_rdfListener;

	protected Resource addPattern(Resource subject, Resource predicate, RDFNode object) throws RDFException {
		if (m_listenerCreated && m_rdfListener == null) {
			System.out.println(">> something funny going on here");
		}
		return m_rdfListener.addPattern(subject, predicate, object);
	}

	protected void removePattern(Resource cookie) {
		m_rdfListener.removePattern(cookie);
	}

	protected Resource m_chainedProvider;
	protected Resource m_chainedPrescription;
	
	protected Context createChildContext() {
		Context childContext = new Context(m_context);
					
		childContext.putLocalProperty(OzoneConstants.s_part, m_chainedProvider);
		childContext.putLocalProperty(OzoneConstants.s_partData, m_chainedPrescription);
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		
		return childContext;
	} 

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(
		IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_listenerCreated) {
			createListener();
		}
		
		if (m_ownsDataProvider) {
			m_dataProvider.initializeFromDeserialization(source);
		}
	}	
	
	public void internalInitialize(IRDFContainer source, Context context, boolean createListener) {
		if (m_source == null) {
			setupSources(source, context);
		}
		
		if (createListener) {
			createListener();
		}
				
		m_dataProvider = (IDataProvider) m_context.getLocalProperty(OzoneConstants.s_dataProvider);
		if (m_dataProvider == null) {
			m_chainedPrescription = (Resource) Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (m_chainedPrescription != null) {
				m_dataProvider = DataUtilities.createDataProvider(m_chainedPrescription, m_context, m_partDataSource, m_partDataSource);
				m_ownsDataProvider = m_dataProvider != null;
			}
		}
		
		if (m_dataProvider != null) {
			m_dataConsumer = createDataConsumer();
			m_dataProvider.registerConsumer(m_dataConsumer);
		}
	}
	abstract protected IDataConsumer createDataConsumer();

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		if (m_dataProvider != null) {
			m_dataProvider.unregisterConsumer(m_dataConsumer);
			m_dataConsumer = null;
			
			if (m_ownsDataProvider) {
				m_dataProvider.dispose();
			}
			m_dataProvider = null;
		}
		
		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}

		if (m_dataConsumers != null) {
			m_dataConsumers.clear();
			m_dataConsumers = null;
		}

		m_partDataSource = null;
		m_infoSource = null;
		m_context = null;
		
		super.dispose();
	}
	
	protected void createListener() {
		if (m_rdfListener == null) {
			m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource) {
				public void statementAdded(Resource cookie, Statement s) {
					onStatementAdded(s);
				}
				public void statementRemoved(Resource cookie, Statement s) {
					onStatementRemoved(s);
				}
			};
			m_rdfListener.start();
			m_listenerCreated = true;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		throw new UnsupportedOperationException("Data provider does not support changes");
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
	
	protected void onStatementAdded(Statement s) {};
	protected void onStatementRemoved(Statement s) {};
}
