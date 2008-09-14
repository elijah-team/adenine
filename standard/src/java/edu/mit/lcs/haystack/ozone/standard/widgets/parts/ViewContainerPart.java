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

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IGUIHandler;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.IViewContainerPart;
import edu.mit.lcs.haystack.ozone.core.IVisualPart;
import edu.mit.lcs.haystack.ozone.core.IdleRunnable;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.core.PartUtilities;
import edu.mit.lcs.haystack.ozone.core.ViewContainerInformationSourceManager;
import edu.mit.lcs.haystack.ozone.core.VisualPartBase;
import edu.mit.lcs.haystack.ozone.core.utils.ChildPartEvent;
import edu.mit.lcs.haystack.ozone.core.utils.Connector;
import edu.mit.lcs.haystack.ozone.core.utils.InformationSourceManager;
import edu.mit.lcs.haystack.ozone.data.*;

/**
 * A lightweight (windowless) implementation of IViewContainerPart.
 * 
 * @author Dennis Quan
 * @author David Huynh
 */

public class ViewContainerPart extends VisualPartBase implements IViewContainerPart {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ViewContainerPart.class);

	final public static Resource s_ViewContainer = new Resource(Constants.s_ozone_namespace + "ViewContainer"); 

	protected IVisualPart				m_child;
	transient protected Connector		m_connector;
	transient protected IdleRunnable	m_runnable;
	protected IDataProvider				m_dataProvider;
	protected IDataConsumer				m_dataConsumer;

	protected IDataProvider	m_viewInstanceDataProvider;
	
	protected Resource		m_currentResource;
	protected Resource		m_currentViewInstance;
	protected Resource		m_currentPart;
	protected Class			m_currentClass;
	
	protected boolean		m_synchronous = true;
	
	transient protected IRDFContainer				m_childInfoSource;
	transient protected InformationSourceManager	m_infoSourceManager;
	
	protected Context		m_childContext;
	
	protected Resource		m_nestingRelation;
	protected boolean		m_useDefaultViewInstance = true;
	
	public ViewContainerPart() {
		this(true);
	}
	public ViewContainerPart(boolean synchronous) {
		m_synchronous = synchronous;
	}
	public void setNestingRelation(Resource nestingRelation) {
		m_nestingRelation = nestingRelation;
	}
	
	protected void registerViewPartClass() {
		PartUtilities.registerViewPartClass(m_source, m_context, m_prescription);
	}	
	protected void internalInitialize() {
		super.internalInitialize();		
		
		registerViewPartClass();
		
		Resource nestingRelation = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_nestingRelation, m_partDataSource);
		if (nestingRelation != null) {
			m_nestingRelation = nestingRelation;
		}

		Resource resUnderlying = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_initialResource, m_partDataSource);
		Resource resViewInstance = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_initialView, m_partDataSource);

		// TODO[dquan]: change m_infoSource to m_source when data providers support using m_partDataSource as m_infoSource 
		m_infoSourceManager = new ViewContainerInformationSourceManager(m_context, m_infoSource, Utilities.getResourceProperty(m_prescription, InformationSourceManager.informationSourceSpecification, m_partDataSource), this);

		Resource[] resInfoSources = Utilities.getResourceProperties(m_prescription, OzoneConstants.s_informationSource, m_infoSource);
		if (resInfoSources.length > 0) {
			try {
				FederationRDFContainer infoSource = new FederationRDFContainer();
				infoSource.addSource(m_source, 1);
				for (int i = 0; i < resInfoSources.length; i++) {
					IRDFContainer rdfc2 = (IRDFContainer)m_context.getServiceAccessor().connectToService(resInfoSources[i], m_context.getUserIdentity());
					infoSource.addSource(rdfc2, 0);
				}
				m_context.setInformationSource(infoSource);
				m_infoSource = infoSource;
			} catch (Exception e) {
				s_logger.error("Failed to process info source", e);
			}
		}

		Resource resConnector = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_connector, m_source);
		if (resConnector != null) {
			Context	childContext = new Context(m_context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, resConnector);
			
			m_connector = new Connector() {
				public void onChange() {
					Resource resUnderlying = null;
					Resource resViewInstance = null;
					
					try {
						resUnderlying = (Resource) m_context.getProperty(OzoneConstants.s_underlying);
						resViewInstance = (Resource) m_context.getProperty(OzoneConstants.s_viewInstance);
					} catch (ClassCastException e) {
					}
					
					navigate(resUnderlying, resViewInstance);
				}
			};
			m_connector.initialize(m_source, childContext);
		}
		
		Resource resDataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_dataSource, m_source);
		
		if (resDataSource != null) {
			m_dataProvider = DataUtilities.createDataProvider(resDataSource, m_context, m_source, m_partDataSource);			
				
			if (m_dataProvider != null) {
				m_dataConsumer = new ResourceDataConsumer() {
					protected void onResourceChanged(Resource newResource) {						
						if (!newResource.equals(m_currentResource)) {
							navigate(newResource);
						}
					}
				
					protected void onResourceDeleted(Resource previousResource) {
						Ozone.idleExec(new IdleRunnable(m_context) {
							public void run() {
								if (m_child != null) {
									m_child.dispose();
									m_child = null;
									m_currentResource = null;
									onChildResize(new ChildPartEvent(ViewContainerPart.this));
								}
							}
						});
					}
				};
				
				m_dataProvider.registerConsumer(m_dataConsumer);
			}
		}
		
		Resource resViewInstanceDataSource = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_viewInstanceDataSource, m_source);
		if (resViewInstanceDataSource != null) {
			m_viewInstanceDataProvider = DataUtilities.createDataProvider(resViewInstanceDataSource, m_context, m_source, m_partDataSource);
			if (m_viewInstanceDataProvider != null) {
				m_viewInstanceDataProvider.registerConsumer(new ResourceDataConsumer() {
					protected void onResourceChanged(Resource newResource) {
						if (m_currentResource != null && !newResource.equals(m_currentViewInstance)) {
							navigate(m_currentResource, m_currentViewInstance = newResource);
						}
					}
				
					protected void onResourceDeleted(Resource previousResource) {
					}
				});
			}
		}
		
		if (resUnderlying != null) {
			navigate(resUnderlying, resViewInstance);
		}
	}
	
	/**
	 * @see IPart#dispose()
	 */
	synchronized public void dispose() {
		if (m_dataProvider != null) {
			m_dataProvider.unregisterConsumer(m_dataConsumer);
			m_dataProvider.dispose();
			m_dataProvider = null;
			m_dataConsumer = null;
		}
		
		if (m_viewInstanceDataProvider != null) {
			m_viewInstanceDataProvider.dispose();
			m_viewInstanceDataProvider = null;
		}
		
		if (m_connector != null) {
			m_connector.dispose();
			m_connector = null;
		}

		if (m_child != null) {
			m_child.dispose();
			m_child = null;
		}
		
		if (m_runnable != null) {
			m_runnable.expire();
			m_runnable = null;
		}
		
		if (m_infoSourceManager != null) {
			m_infoSourceManager.dispose();
			m_infoSourceManager = null;
		}
		
		super.dispose();
	}
	
	/**
	 * @see IPart#handleEvent(Resource, Object)
	 */
	public boolean handleEvent(Resource eventType, Object event) {
		if (!eventType.equals(PartConstants.s_eventChildResize)) {
			return m_child != null ? m_child.handleEvent(eventType, event) : false;
		} else {
			return super.handleEvent(eventType, event);
		}
	}

	/**
	 * @see IVisualPart#getGUIHandler(Class)
	 */
	public IGUIHandler getGUIHandler(Class cls) {
		if (m_child != null) {
			return m_child.getGUIHandler(cls);
		}
		return null;
	}

	/**
	 * @see IViewContainerPart#navigate(Resource)
	 */
	public void navigate(Resource res) {
		navigate(res, null);
	}

	/**
	 * @see IViewContainerPart#navigate(Resource, Resource)
	 */
	public void navigate(Resource resUnderlying, Resource resViewInstance) {
		if (resUnderlying == null) return;
		internalNavigate(resUnderlying, resViewInstance);
	}
	
	/**
	 * @see IViewContainerPart#refresh()
	 */
	public void refresh() {
		synchronized (this) {
			if (m_currentResource == null) {
				return;
			}
			
			if (m_runnable != null) {
				m_runnable.expire();
				m_runnable = null;
			}
			
			if (m_synchronous && Ozone.isUIThread()) {
				initializeChild();
			} else {
				m_runnable = new IdleRunnable(m_context) {
					public void run() {
						if (m_context != null) {
							initializeChild();
						}
					}
				};
			}
		}
		scheduleRunnable();
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IViewContainerPart#onNavigateComplete(edu.mit.lcs.haystack.rdf.Resource, edu.mit.lcs.haystack.ozone.IPart)
	 */
	public void onNavigateComplete(Resource resource, IPart childPart) {
		m_currentResource = resource;
	}
	
	/**
	 * @see IViewContainerPart#getCurrentResource()
	 */
	public Resource getCurrentResource() {
		return m_currentResource;
	}

	/**
	 * @see IViewContainerPart#getCurrentViewInstance()
	 */
	public Resource getCurrentViewInstance() {
		return m_currentViewInstance;
	}

	/**
	 * @see IViewContainerPart#getCurrentViewPart()
	 */
	public IPart getCurrentViewPart() {
		return m_child;
	}
	
	protected void internalNavigate(Resource resource, Resource viewInstance) {
		synchronized (this) {
			if (m_context == null) {
				s_logger.info("internalNavigate called when part already disposed");
				return; // we've been disposed!
			}
			
			Resource	part = null;
			Class		c = null;
			
			m_childInfoSource = m_infoSourceManager.constructChildInformationSource(resource);

			/*
			 *	Determine which part to use to display the underlying resource
			 */
			try {
				if (viewInstance == null) {
					if (m_viewInstanceDataProvider != null) {
						viewInstance = (Resource) m_viewInstanceDataProvider.getData(DataConstants.RESOURCE, null);
					}
				}
					
				Resource viewClass = (Resource) m_context.getProperty(OzoneConstants.s_viewPartClass);
				if (viewClass == null) {
					viewClass = OzoneConstants.s_InteractiveViewPart;
				}

				if (viewInstance == null) {					
					Resource[] a = m_useDefaultViewInstance ? 
															  Ozone.findOrDefaultViewPartOfType(resource, viewClass, m_source, m_childInfoSource) :
															  	Ozone.findOrCreateViewPartOfType(resource, viewClass, m_source, m_childInfoSource);;
					
					viewInstance = a[0];
					part = a[1];
				} else {
					part = Ozone.findViewPartForViewOfType(resource, viewInstance, viewClass, m_source, m_childInfoSource);
					if (part == null) {
						part = Ozone.findViewPartForViewOfType(resource, viewInstance, null, m_source, m_childInfoSource);
					}
				}
				
				c = Utilities.loadClass(part, m_source);
			} catch (Exception e) {
				s_logger.error("Failed to find Ozone part for " + resource, e);
			}
		
			if (m_runnable != null) {
				m_runnable.expire();
				m_runnable = null;
			}
			
			if (m_viewInstanceDataProvider != null) {
				try {
					m_viewInstanceDataProvider.requestChange(DataConstants.RESOURCE_CHANGE, viewInstance);
				} catch (Exception e) {
					s_logger.error("Failed to notify view instance data provider of new view", e);
				}
			}
			
			if (c != null) {
				m_currentResource = resource;
				m_currentViewInstance = viewInstance;
				m_currentPart = part;
				m_currentClass = c;
			
				if (m_synchronous && Ozone.isUIThread()) {
					initializeChild();
				} else {
					m_runnable = new IdleRunnable(m_context) {
						public void run() {
							if (m_context != null) {
								initializeChild();
							}
						}
					};
				}
			}
		}
		
		scheduleRunnable();
	}
	
	protected void scheduleRunnable() {
		Ozone.idleExec(m_runnable);
	}
	
	protected boolean isOverNested() {
		Context context = m_context;
		int c = 0;
		while (context != null && c < 5) {
			if (m_currentViewInstance.equals(context.getLocalProperty(OzoneConstants.s_viewInstance)) &&
				m_currentResource.equals(context.getLocalProperty(OzoneConstants.s_underlying))) {
				++c;
			}
			context = context.getParentContext();
		}
		
		if (c >= 5) {
			// Cannot nest this deep
			return true;
		}
		
		return false;
	}
	
	synchronized protected void initializeChild() {
		if (m_child != null) {
			m_child.dispose();
			m_child = null;
		}
		
		if (isOverNested()) {
			return;
		}
		
		m_childContext = new Context(m_context);
		
		m_childContext.setInformationSource(m_childInfoSource);
		m_childContext.putLocalProperty(OzoneConstants.s_part, m_currentPart);
		m_childContext.putLocalProperty(OzoneConstants.s_underlying, m_currentResource);
		m_childContext.putLocalProperty(OzoneConstants.s_viewInstance, m_currentViewInstance);
		m_childContext.putLocalProperty(OzoneConstants.s_parentPart, this);
		m_childContext.putProperty(OzoneConstants.s_viewContainer, this);
				
		if (m_nestingRelation != null) {
			m_childContext.putLocalProperty(OzoneConstants.s_nestingRelation, m_nestingRelation);
		}
		
		try {
			//setHostingProperties();
							
			m_child = (IVisualPart) m_currentClass.newInstance();
			m_child.initialize(m_source, m_childContext);

			if (!m_initializing) {
				onChildResize(new ChildPartEvent(this));
			}
		} catch (Exception e) {
			s_logger.error("ViewContainerPart: failed to initialize child part " + m_currentClass.getName() + " for " + m_currentResource, e);
		}
	}
	
	protected void setHostingProperties() {
		if (m_prescription != null) {
			try {
				m_partDataSource.remove(
					new Statement(m_prescription, PartConstants.s_hostedResource, Utilities.generateWildcardResource(1)),
					Utilities.generateWildcardResourceArray(1)
				);
				m_partDataSource.remove(
					new Statement(m_prescription, PartConstants.s_hostedViewInstance, Utilities.generateWildcardResource(1)),
					Utilities.generateWildcardResourceArray(1)
				);
				m_partDataSource.remove(
					new Statement(m_prescription, PartConstants.s_hostedPart, Utilities.generateWildcardResource(1)),
					Utilities.generateWildcardResourceArray(1)
				);
				
				m_partDataSource.add(new Statement(m_prescription, PartConstants.s_hostedResource, m_currentResource));
				if (m_currentViewInstance != null) {
					m_partDataSource.add(new Statement(m_prescription, PartConstants.s_hostedViewInstance, m_currentViewInstance));
				}
				if (m_currentPart != null) {
					m_partDataSource.add(new Statement(m_prescription, PartConstants.s_hostedPart, m_currentPart));
				}
			} catch (RDFException e) {
				s_logger.error("Failed to change dynamic metadata of part", e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);

		m_infoSourceManager = new ViewContainerInformationSourceManager(m_context, m_infoSource, Utilities.getResourceProperty(m_prescription, InformationSourceManager.informationSourceSpecification, m_partDataSource), this);
		
		if (m_child != null) {
			m_childInfoSource = m_infoSourceManager.constructChildInformationSource(m_currentResource);
			m_childContext.setInformationSource(m_childInfoSource);
			m_child.initializeFromDeserialization(source);
		}
		
		if (m_dataProvider != null) {
			m_dataProvider.initializeFromDeserialization(source);
		}		
	}
}
