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

package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import java.util.Date;
import java.util.LinkedList;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.Pair;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.SingleChildContainerPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.data.*;
import edu.mit.lcs.haystack.ozone.standard.widgets.slide.SlideUtilities;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * Displays a slide based on the output of an Adenine script. Previously called
 * edu.mit.lcs.haystack.ozone.applets.AdenineSlideApplet.
 * 
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 */
public class AdeninePartContainerPart extends SingleChildContainerPartBase implements IViewPart {
	ResourceDataConsumer		m_dataConsumer;
	ResourceDataProviderWrapper	m_dataProviderWrapper;
	
	static {
		for (int i = 0; i < 4; i++) {
			RenderingThread s_renderingThread = new RenderingThread();
			s_renderingThread.setDaemon(true);
			s_renderingThread.setPriority(Thread.MIN_PRIORITY);
			s_renderingThread.start();		
		}
	}
	boolean					m_cache = false;
	
	final public static Resource SYNCHRONOUS = new Resource(PartConstants.s_namespace + "synchronous");
	final public static Resource PART_DATA_GENERATOR = new Resource(PartConstants.s_namespace + "partDataGenerator");
	final public static Resource CACHE_PART_DATA = new Resource(PartConstants.s_namespace + "cachePartData");
	final public static Resource GENERATED_PART_DATA = new Resource(PartConstants.s_namespace + "generatedPartData");
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdeninePartContainerPart.class);

	protected boolean isInitialized() {
		return m_child != null;
	}
	
	protected Resource m_resChildPart;
	protected Resource m_childPartData;
	
	protected Resource m_resService;
	protected boolean m_synchronous;
	protected Resource m_cacheStyle;
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.core.utils.SingleChildContainerPartBase#createChildContext()
	 */
	protected Context createChildContext() {
		Context childContext = new Context(m_context);
					
		childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		childContext.putLocalProperty(OzoneConstants.s_partData, m_childPartData);
		childContext.putLocalProperty(OzoneConstants.s_part, m_resChildPart);
						
		if (m_resID != null) {
			childContext.putLocalProperty(PartConstants.s_id, m_resID);
		}
		if (m_resOnClick != null) {
			childContext.putLocalProperty(PartConstants.s_onClick, m_resOnClick);
		}
		if (m_resOnEnterPressed != null) {
			childContext.putLocalProperty(PartConstants.s_onEnterPressed, m_resOnEnterPressed);
		}
		if (m_tooltip != null) {
			childContext.putLocalProperty(PartConstants.s_tooltip, m_tooltip);
		}
		return childContext;
	}	
	
	/**
	 * @see IPart#dispose()
	 */
	public void dispose() {
		synchronized (this) {
			if (m_dataProviderWrapper != null) {
				m_dataProviderWrapper.getDataProvider().unregisterConsumer(m_dataConsumer);
				m_dataProviderWrapper.dispose();
				m_dataProviderWrapper = null;
			}
			m_dataConsumer = null;
	
			if (m_context != null) {
				SlideUtilities.releaseAmbientProperties(m_context);
			}
			
			super.dispose();
		}
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (isInitialized()) {
			return m_child.getGUIHandler(cls);
		} else {
			return null;
		}
	}

	protected void internalInitialize() {
		super.internalInitialize();

		if (m_prescription == null) {
			m_prescription = m_resUnderlying;
		} else {
			SlideUtilities.recordAmbientProperties(m_context, m_partDataSource, m_prescription);
		}
		
		m_cache = Utilities.checkBooleanProperty(m_resPart, CACHE_PART_DATA, m_partDataSource);
		
		m_cacheStyle = Utilities.getResourceProperty(m_resPart, PartConstants.s_cacheStyle, m_partDataSource);
		if (m_cacheStyle == null) {
			m_cacheStyle = PartConstants.s_cacheAcrossInstances;
		}
		
		m_resService = Utilities.getResourceProperty(m_resPart, PART_DATA_GENERATOR, m_source);
		if (m_resService == null) {
			m_resService = Utilities.getResourceProperty(m_resViewInstance, PART_DATA_GENERATOR, m_infoSource);
		}
		if (m_resService == null) {
			m_resService = Utilities.getResourceProperty(m_prescription, PART_DATA_GENERATOR, m_partDataSource);
		}
		
		Resource dataSource = null;
		
		if (m_resService == null) {
			dataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_partDataSource);
			if (dataSource == null) {
				dataSource = Utilities.getResourceProperty(m_resPart, OzoneConstants.s_dataSource, m_source);
			}
		}
		
		if (dataSource != null) {
			IDataProvider dataProvider = DataUtilities.createDataProvider(dataSource, m_context, m_source, m_partDataSource);
			if (dataProvider != null) {
				m_dataProviderWrapper = new ResourceDataProviderWrapper(dataProvider);
				m_dataConsumer = new ResourceDataConsumer() {
					Resource m_oldResource;
					
					protected void onResourceChanged(Resource newResource) {
						if (setResource(newResource)) {
							if (Ozone.isUIThread()) {
								runRender(newResource);
							} else {
								Ozone.idleExec(new RenderRunnable(newResource));
							}
						}
					}
					
					protected void onResourceDeleted(Resource previousResource) {
						if (setResource(null)) {
							if (Ozone.isUIThread()) {
								runRender(null);
							} else {
								Ozone.idleExec(new RenderRunnable(null));
							}
						}
					}
					
					synchronized boolean setResource(Resource newResource) {
						boolean changed = (newResource == null ? m_oldResource != null : !newResource.equals(m_oldResource));
						m_oldResource = newResource;
						return changed;
					}
				};
				dataProvider.registerConsumer(m_dataConsumer);
			}
		} else {
			m_synchronous = Utilities.checkBooleanProperty(m_resPart, SYNCHRONOUS, m_source);
			if (!m_synchronous && (m_prescription != null)) {
				m_synchronous = Utilities.checkBooleanProperty(m_prescription, SYNCHRONOUS, m_source);
			}
			if (m_synchronous) {
				runInit(m_resService, m_synchronous);
			} else {
				RenderingThread.enqueue(new InitRunnable(m_resService));
			}
		}
	}
	
	class InitRunnable extends IdleRunnable {
		Resource m_resService;
		public InitRunnable(Resource resService) {
			super(m_context);
			m_resService = resService;
		}
		public void run() {
			synchronized (AdeninePartContainerPart.this) {
				if (m_context != null) {
					synchronized (PartCache.s_contextLock) {
						runInit(m_resService, false);
					}
				}
			}
		}
	}
	
	class RenderRunnable extends IdleRunnable {
		Resource m_childPartData;
		public RenderRunnable(Resource childPartData) {
			super(m_context);
			m_childPartData = childPartData;
		}
		public void run() {
			runRender(m_childPartData);
		}
	}
	
	protected void runInit(Resource resService, boolean synchronous) {
		if (m_source == null) {
			return;
		}
		
		if (m_context == null) {
			return;
		}
		
		Resource childPartData = null;
		
		Resource base = m_resUnderlying != null ? m_resViewInstance : m_prescription; 
		
		if (m_cache) {
			childPartData = Utilities.getResourceProperty(
				base,
				GENERATED_PART_DATA,
				m_source
			);
		}
		
		PackageFilterRDFContainer filterContainer = new PackageFilterRDFContainer(m_source, base);
	
		if (childPartData == null) {
			Interpreter i = Ozone.getInterpreter();
			DynamicEnvironment denv = new DynamicEnvironment(m_source);
			Ozone.initializeDynamicEnvironment(denv, m_context);
			denv.setTarget(filterContainer);
			
			try {
				filterContainer.add(new Statement(base, Constants.s_rdf_type, Constants.s_haystack_DisposablePackage));
				childPartData = (Resource) i.callMethod(
					resService, 
					new Object[] { 
						m_resUnderlying != null ? m_resUnderlying : m_prescription 
					},
					denv
				);
			} catch (Exception e) {
				s_logger.error("Error calling method " + resService, e);
			}
		}
		
		if (childPartData != null) {
			if (m_cache) {
				try {
					filterContainer.add(new Statement(
						base,
						GENERATED_PART_DATA,
						childPartData
					));
				} catch (Exception e) {
					s_logger.error(
						"Failed to record generated part data: " 
							+ base
							+ " " + childPartData,
						e
					);
				}
			}
			
			if (synchronous) {
				runRender(childPartData);
			} else {
				Ozone.idleExec(new RenderRunnable(childPartData));
			}
			
			try {
				filterContainer.replace(base, Constants.s_haystack_lastPackageUse, null, Utilities.generateDateTime(new Date()));
			} catch (RDFException e) {
			}
		}
	}
	
	protected void runRender(Resource childPartData) {
		try {
			if (m_source == null) {
				return;
			}
			
			if (m_context == null) {
				return;
			}
		
			if (m_child != null) {
				m_child.dispose();
				m_child = null;
			}
			
			if (childPartData != null) {
				if (PartConstants.s_cacheAcrossInstances.equals(m_cacheStyle)) {
					m_child = (IVisualPart) PartCache.deserialize(childPartData, this, m_context, m_source);
					if (m_child != null) {
						if (!m_initializing) {
							onChildResize(new ChildPartEvent(this));
						}
						return;
					}		
				} else if (PartConstants.s_cachePerResource.equals(m_cacheStyle)) {
					m_child = (IVisualPart) PartCache.deserialize(new Pair(childPartData, m_resUnderlying == null ? m_prescription : m_resUnderlying), this, m_context, m_source);
					if (m_child != null) {
						if (!m_initializing) {
							onChildResize(new ChildPartEvent(this));
						}
						return;
					}		
				}
				try {
					m_resChildPart = Ozone.findPart(childPartData, m_source, m_partDataSource);
					m_childPartData = childPartData;
					Class c = Utilities.loadClass(m_resChildPart, m_source);
					
					m_child = (IVisualPart) c.newInstance();
					Context childContext = createChildContext();
					if (m_child != null) {
						m_child.initialize(m_source, childContext);
						if (PartConstants.s_cacheAcrossInstances.equals(m_cacheStyle)) {
							PartCache.serialize(m_childPartData, this, m_context, m_child, childContext);
						} else if (PartConstants.s_cachePerResource.equals(m_cacheStyle)) {
							PartCache.serialize(new Pair(m_childPartData, m_resUnderlying == null ? m_prescription : m_resUnderlying), this, m_context, m_child, childContext);
						}
					}
				} catch (Exception e) {
					s_logger.error("Failed to initialize child part " + childPartData, e);
				}
			}
					
			if (!m_initializing) {
				onChildResize(new ChildPartEvent(this));
			}
		} catch (Exception e) {
			s_logger.error("Failed to render child part " + childPartData, e);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		if (m_dataProviderWrapper != null) {
			m_dataProviderWrapper.getDataProvider().initializeFromDeserialization(source);
		} else if (!isInitialized() || !m_cache) {
			if (m_synchronous) {
				Ozone.idleExec(new InitRunnable(m_resService));
			} else {
				RenderingThread.enqueue(new InitRunnable(m_resService));
			}
		}
	}
}

class RenderingThread extends Thread {
	static LinkedList s_renderingQueue = new LinkedList();

	static void enqueue(Runnable r) {
		synchronized (s_renderingQueue) {
			s_renderingQueue.addLast(r);
			s_renderingQueue.notifyAll();
		}
	}
	
	/**
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (true) {
			Runnable r = null;
			synchronized (s_renderingQueue) {
				while (true) {
					if (!s_renderingQueue.isEmpty()) {
						r = (Runnable) s_renderingQueue.removeFirst();
						break;
					} else {
						try {
							s_renderingQueue.wait();
						} catch (InterruptedException e) {
						}
					}
				}
			}
			
			if (r != null) {
				try {
					r.run();
				} catch (Exception e) {
					AdeninePartContainerPart.s_logger.error("Error rendering", e);
				}
			}
		}
	}
}