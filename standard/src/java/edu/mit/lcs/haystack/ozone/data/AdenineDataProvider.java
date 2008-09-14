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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.utils.GenericPart;

import edu.mit.lcs.haystack.server.core.rdfstore.IRDFListener;
import edu.mit.lcs.haystack.server.core.rdfstore.RDFListener;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;

import java.util.*;

/**
 * @author David Huynh
 */
public class AdenineDataProvider extends GenericPart implements IDataProvider {
	protected ArrayList			m_dataConsumers = new ArrayList();
	protected HashSet			m_supportedChanges = new HashSet();
	transient protected RDFListener		m_rdfListener;
	
	protected Object			m_data;
	protected Resource			m_status;
	
	final static Resource INITIALIZE		= new Resource(DataConstants.NAMESPACE + "initialize");
	final static Resource DISPOSE			= new Resource(DataConstants.NAMESPACE + "dispose");
	final static Resource GET_DATA			= new Resource(DataConstants.NAMESPACE + "getData");
	final static Resource GET_STATUS		= new Resource(DataConstants.NAMESPACE + "getStatus");
	final static Resource REQUEST_CHANGE	= new Resource(DataConstants.NAMESPACE + "requestChange");
	final static Resource SUPPORTS_CHANGE	= new Resource(DataConstants.NAMESPACE + "supportsChange");

	final static Resource ON_CONSUMER_REGISTERED	= new Resource(DataConstants.NAMESPACE + "onConsumerRegistered");
	final static Resource ON_CONSUMER_UNREGISTERED	= new Resource(DataConstants.NAMESPACE + "onConsumerUnregistered");
	
	final static Resource ON_STATEMENT_ADDED		= new Resource(DataConstants.NAMESPACE + "onStatementAdded");
	final static Resource ON_STATEMENT_REMOVED		= new Resource(DataConstants.NAMESPACE + "onStatementRemoved");
	
	final static Resource ON_REINITIALIZE			= new Resource(DataConstants.NAMESPACE + "onReinitialize");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineDataProvider.class);
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		setupSources(source, context);
		setupListener();
		
		initialize();		
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		setupListener();
		
		Resource method = (Resource) m_context.getLocalProperty(ON_REINITIALIZE);
		if (method != null) {
			try {
				callMethod(method, new Object[] { this });
			} catch (AdenineException e) {
				s_logger.error("Error calling onReinitialize for data source " + m_prescription, e);
			}
		}
	}
	
	protected void initialize() {
		/*	Initialize.
		 */
		Resource methodInitialize = extractWithDefault(m_resPart, INITIALIZE, null);
		methodInitialize = extractWithDefault(m_prescription, INITIALIZE, methodInitialize);
		if (methodInitialize != null) {
			try {
				callMethod(methodInitialize, new Object[] { this });
			} catch (AdenineException e) {
				s_logger.error("Error calling method initialize " + methodInitialize, e);
			}
		}
	}
	
	protected void setupListener() {
		m_rdfListener = new RDFListener((ServiceManager) m_context.getServiceAccessor(), (IRDFEventSource)m_infoSource) {
			public void statementAdded(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					public void run() {
						if (m_context != null) {
							onStatementAdded(m_s);
						}
					}
					
					Statement m_s;
					
					public IdleRunnable init(Statement s) {
						m_s = s;
						return this;
					}
				}.init(s));
			}		
			public void statementRemoved(Resource cookie, Statement s) {
				Ozone.idleExec(new IdleRunnable(m_context) {
					public void run() {
						if (m_context != null) {
							onStatementRemoved(m_s);
						}
					}
					
					Statement m_s;
					
					public IdleRunnable init(Statement s) {
						m_s = s;
						return this;
					}
				}.init(s));
			}		
		};
		m_rdfListener.start();
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	synchronized public void dispose() {
		Resource method = (Resource) m_context.getLocalProperty(DISPOSE);
		if (method != null) {
			try {
				callMethod(method, new Object[] { this });
			} catch (AdenineException e) {
				s_logger.error("Error calling method dispose " + method, e);
			}
		}

		if (m_rdfListener != null) {
			m_rdfListener.stop();
			m_rdfListener = null;
		}

		m_source = null;
		m_context = null;
		m_infoSource = null;
		
		m_dataConsumers.clear();
		m_dataConsumers = null;
		m_data = null;
		m_status = null;
	}

	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		return false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#registerConsumer(IDataConsumer)
	 */
	synchronized public void registerConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer == null) {
			return;
		}

		m_dataConsumers.add(dataConsumer);
		
		dataConsumer.reset();

		Resource method = (Resource) m_context.getLocalProperty(ON_CONSUMER_REGISTERED);
		if (method != null) {
			try {
				callMethod(method, new Object[] { this, dataConsumer });
			} catch (AdenineException e) {
				s_logger.error("Error calling method onConsumerRegister " + method, e);
			}
		}
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#unregisterConsumer(IDataConsumer)
	 */
	synchronized public void unregisterConsumer(IDataConsumer dataConsumer) {
		if (dataConsumer == null) {
			return;
		}

		m_dataConsumers.remove(dataConsumer);

		Resource method = (Resource) m_context.getLocalProperty(ON_CONSUMER_UNREGISTERED);
		if (method != null) {
			try {
				callMethod(method, new Object[] { this, dataConsumer });
			} catch (AdenineException e) {
				s_logger.error("Error calling method onConsumerUnregistered " + method, e);
			}
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications) throws DataNotAvailableException {
		if (m_context == null) {
			return null;
		}
		
		Resource method = (Resource) m_context.getLocalProperty(GET_DATA);
		if (method != null) {
			try {
				Object data = callMethod(method, new Object[] { this, dataType, specifications });
				
				if (data != null) {
					if (data instanceof DataNotAvailableException) {
						throw (DataNotAvailableException) data;
					}
				} else {
					throw new DataNotAvailableException("No data is available yet");
				}
				return data;
			} catch (AdenineException e) {
				s_logger.error("Error calling getData for data source " + m_prescription, e);
				return null;
			}
		} else {
			if (m_data == null) {
				throw new DataNotAvailableException("AdenineDataProvider has not cached data");
			}
			return m_data;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getStatus()
	 */
	synchronized public Resource getStatus() {
		if (m_context == null) {
			return null;
		}

		Resource method = (Resource) m_context.getLocalProperty(GET_STATUS);
		if (method != null) {
			try {
				Resource status = (Resource) callMethod(method, new Object[] { this });
				
				return status;
			} catch (AdenineException e) {
				s_logger.error("Error calling getStatus for data source " + m_prescription, e);
				return null;
			}
		} else {
			return m_status;
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		Ozone.idleExec(new IdleRunnable(m_context) {
			public void run() {
				if (m_context != null) {
					Resource method = (Resource) m_context.getLocalProperty(REQUEST_CHANGE);
					if (method != null) {
						try {
							Object result = callMethod(method, new Object[] { AdenineDataProvider.this, m_changeType, m_change });
							
/*							if (result != null && result instanceof Exception) {
								if (result instanceof UnsupportedOperationException) {
									throw (UnsupportedOperationException) result;
								} else if (result instanceof DataMismatchException) {
									throw (DataMismatchException) result;
								} else {
									// what do we do?
									s_logger.error("Unknown exception: " + result);
								}
							}
*/						} catch (AdenineException e) {
							s_logger.error("Error calling method requestChange " + method, e);
						}
					} else {
						throw new UnsupportedOperationException("No change operation is supported");
					}
				}
			}
			
			Resource 	m_changeType;
			Object		m_change;
			
			public IdleRunnable init(Resource changeType, Object change) {
				m_changeType = changeType;
				m_change = change;
				
				return this;
			}
		}.init(changeType, change));
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	synchronized public boolean supportsChange(Resource changeType) {
		if (m_context == null) {
			return false;
		}

		Resource method = (Resource) m_context.getLocalProperty(SUPPORTS_CHANGE);
		if (method != null) {
			try {
				Boolean result = (Boolean) callMethod(method, new Object[] { this, changeType });
				
				return result.booleanValue();
			} catch (AdenineException e) {
				return false;
			}
		} else {
			return m_supportedChanges.contains(changeType);
		}
	}

	/*
	 * The following methods are for the use of the Adenine methods.
	 */

	/**
	 * Returns the data consumer that this data provider holds, if any.
	 */
	public List getDataConsumers() {
		return m_dataConsumers;
	}
	
	/**
	 * Returns the RDF listener that this class provides for the convenience
	 * of the Adenine methods. The RDF listener will be alive as long as
	 * this AdenineDataProvider object.
	 */
	public IRDFListener getRDFListener() {
		return m_rdfListener;
	}
	
	/**
	 * Lets this AdenineDataProvider object cache the data.
	 */
	public void cacheData(Object data) {
		m_data = data;
	}
	public Object getCachedData() {
		return m_data;
	}
	
	/**
	 * Lets this AdenineDataProvider object cache the status.
	 */
	public void cacheStatus(Resource status) {
		m_status = status;
	}
	public Resource getCachedStatus() {
		return m_status;
	}
	
	public void registerSupportedChange(Resource changeType) {
		m_supportedChanges.add(changeType);
	}
	
	
	protected void onStatementAdded(Statement s) {
		Resource method = (Resource) m_context.getLocalProperty(ON_STATEMENT_ADDED);
		synchronized (this) {
			if (m_dataConsumers == null) {
				return;
			}
			
			if (method != null) {
				try {
					callMethod(method, new Object[] { this, s });
				} catch (AdenineException e) {
					s_logger.error("Error calling method onStatementAdded " + method, e);
				}
			}
		}
	}
	
	protected void onStatementRemoved(Statement s) {
		synchronized (this) {
			if (m_dataConsumers == null) {
				return;
			}
			
			Resource method = (Resource) m_context.getLocalProperty(ON_STATEMENT_REMOVED);
			if (method != null) {
				try {
					callMethod(method, new Object[] { this, s });
				} catch (AdenineException e) {
					s_logger.error("Error calling method onStatementRemoved " + method, e);
				}
			}
		}
	}

	protected Resource extractWithDefault(Resource subject, Resource predicate, Resource defaultValue) {
		try {
			Resource res = (Resource) m_source.extract(subject, predicate, null);
			return res != null ? res : defaultValue;
		} catch (RDFException e) {
		}
		return defaultValue;
	}

	protected DynamicEnvironment makeDynamicEnvironment() {
		DynamicEnvironment	denv = new DynamicEnvironment(m_source);
		Ozone.initializeDynamicEnvironment(denv, m_context);
		
		return denv;
	}
	
	protected Object callMethod(Resource method, Object[] parameters) throws AdenineException {
		Interpreter 		interpreter = Ozone.getInterpreter();
		DynamicEnvironment	denv = makeDynamicEnvironment();
		
		return interpreter.callMethod(method, parameters, denv);
	}
}
