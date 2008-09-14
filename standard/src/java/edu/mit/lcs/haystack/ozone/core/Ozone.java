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

package edu.mit.lcs.haystack.ozone.core;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TreeSet;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.FederationRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.security.Identity;

/**
 * The entry point for Ozone.
 * @author Dennis Quan
 * @author David Huynh
 */
public class Ozone {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Ozone.class);
	
	static public Display	s_display = null;
	static public GC		s_gc = null;
	static public Context	s_context = new Context();
	static public Timer		s_timer = new Timer();


	static final Resource s_viewResolutionTime =  new Resource(OzoneConstants.s_namespace + "viewResolutionTime");
	static final public Resource s_defaultFrame = new Resource(OzoneConstants.s_namespace + "defaultFrame");
	static final Resource s_defaultViewClass =    new Resource(OzoneConstants.s_namespace + "defaultViewClass");
	
	static Interpreter			s_interpreter;
	static public IRDFContainer	s_source;
		
	/**
	 * Initializes an Adenine DynamicEnvironment from a Context object.
	 */
	static public void initializeDynamicEnvironment(DynamicEnvironment denv, Context context) {
		denv.setValue("__context__", context);

		IRDFContainer infoSource = context.getInformationSource();
		IRDFContainer partDataSource = denv.getSource();
		
		if (Boolean.TRUE.equals(context.getProperty(OzoneConstants.s_partDataFromInformationSource)) && infoSource != null) {
			partDataSource = infoSource;
		}
		if (infoSource == null) infoSource = partDataSource;
		
		denv.setValue("__infosource__", infoSource);
		denv.setValue("__partdatasource__", partDataSource);
		
		denv.setServiceAccessor(context.getServiceAccessor());
		denv.setIdentity(context.getUserIdentity());
	}
	
	/**
	 * Returns an array of 2 resources [ view instance resource, part resource ]
	 */
	static Resource[] findViewPart(Resource res, IRDFContainer source, IRDFContainer infoSource) throws RDFException {
		return findViewPartOfType(res, OzoneConstants.s_ViewPart, source, infoSource);
	}
	
	/**
	 * Returns an array of 2 resources [ view instance resource, part resource ]
	 */
	static Resource[] findViewPartOfType(Resource res, Resource resType, IRDFContainer source, IRDFContainer infoSource) {
		if ((res == null) || (resType == null)) {
			s_logger.error("findViewPartOfType called with " + res + " and " + resType);
			return null;
		}

		TreeSet records = new TreeSet();

		Resource[] views = Utilities.getResourceProperties(res, Constants.s_haystack_view, infoSource);
		for (int i = 0; i < views.length; i++) {
			Resource[] types = Utilities.getResourceProperties(views[i], Constants.s_rdf_type, infoSource);
			for (int j = 0; j < types.length; j++) {
				Resource part = getViewPartFromViewClass(res, types[j], resType, source, infoSource);
				
				if (part != null) {
					ResolutionRecord record = new ResolutionRecord();
					
					record.m_viewInstance = views[i];
					record.m_part = part;
					
					try {
						Literal time = (Literal) source.extract(views[i], s_viewResolutionTime, null);
						record.m_resolutionTime = Utilities.parseDateTime(time);
					} catch (Exception e) { }
					
					records.add(record);
				}
			}
		}
		
		if (records.size() > 0) {
			ResolutionRecord record = (ResolutionRecord) records.iterator().next();
			return new Resource[] { record.m_viewInstance, record.m_part };
		}

		return null;
	}
	
	/**
	 * Returns an array of 2 resources [ part resource, view class resource ]
	 */
	static Resource[] findClassViewPart(Resource res, IRDFContainer source, IRDFContainer infoSource) throws RDFException {
		return findClassViewPartOfType(res, OzoneConstants.s_ViewPart, source, infoSource);
	}
	
	static final Resource s_webViewPart = new Resource(OzoneConstants.s_namespace + "webViewPart");
	static final Resource s_WebView = new Resource(OzoneConstants.s_namespace + "WebView");
	static final Resource s_aspectViewPart = new Resource("http://haystack.lcs.mit.edu/ui/lens#lensViewPart");
	static final Resource s_AspectView = new Resource("http://haystack.lcs.mit.edu/ui/lens#LensView");
	static final Resource s_viewPartResolver = new Resource(OzoneConstants.s_namespace + "viewPartResolver");
	
	/**
	 * Returns an array of 2 resources [ part resource, view class resource ]
	 */
	static Resource[] findClassViewPartOfType(Resource res, Resource resType, IRDFContainer source, IRDFContainer infoSource) throws RDFException {
		if ((res == null) || (resType == null)) {
			s_logger.error("findClassViewPartOfType called with " + res + " and " + resType);
			return null;
		}
		
		Resource[]	types = Utilities.getResourceProperties(res, Constants.s_rdf_type, infoSource);
		java.util.List typePriorityList = Utilities.getTypePriorityList(types, source);
		Iterator i = typePriorityList.iterator();
		while (i.hasNext()) {
			Resource type = (Resource) i.next();
		
			RDFNode[] result = source.queryExtract(
				new Statement[] {
					new Statement(type, Constants.s_haystack_classView, Utilities.generateWildcardResource(1)),
					new Statement(Utilities.generateWildcardResource(2), OzoneConstants.s_viewDomain, Utilities.generateWildcardResource(1)),
					new Statement(Utilities.generateWildcardResource(2), Constants.s_rdf_type, resType) 
				},
				Utilities.generateWildcardResourceArray(1), 
				Utilities.generateWildcardResourceArray(2)
			);
			
			if (result != null) {
				Resource viewClass = (Resource) result[0];
				Resource part = getViewPartFromViewClass(res, viewClass, resType, source, infoSource);
				
				if (part != null) return new Resource[] { part, viewClass };
			}
		}
	
		/*
		 * Try web pages and metadata view
		 */
		RDFNode[] a = null;
		if (resType.equals(OzoneConstants.s_InteractiveViewPart)) {
			if (res.getURI().startsWith("http:") && Utilities.getResourceProperty(res, Constants.s_rdf_type, infoSource) == null) {
				a = new Resource[] { s_webViewPart, s_WebView };
			} else {
				a = new Resource[] { s_aspectViewPart, s_AspectView };
			}
		}
		
		if (a == null) {
			Statement[] q = new Statement[] {
				new Statement(resType, s_defaultViewClass, Utilities.generateWildcardResource(2)),
				new Statement(Utilities.generateWildcardResource(1), OzoneConstants.s_viewDomain, Utilities.generateWildcardResource(2))
			};

			// Try to find the default view part for the given class
			a = source.queryExtract(
				q,
				Utilities.generateWildcardResourceArray(2),
				Utilities.generateWildcardResourceArray(2));
			if (a != null) return new Resource[] { (Resource)a[0], (Resource)a[1] };
		}

		return (Resource[]) a;
	}
	
	/**
	 * Returns an array of 2 resources [ resource of View, resource of Part ]
	 */
	static public Resource[] findOrCreateViewPart(Resource res, IRDFContainer source, IRDFContainer infoSource) throws RDFException {
		return findOrCreateViewPartOfType(res, OzoneConstants.s_ViewPart, source, infoSource);
	}
	
	/**
	 * Returns a Resource identifying a view part capable of displaying the supplied view.
	 */
	static public Resource findViewPartForViewOfType(Resource underlying, Resource resViewInstance, Resource resType, IRDFContainer source, IRDFContainer infoSource) throws RDFException {
		if (resViewInstance == null) {
			s_logger.error("findViewPartForView called with null view instance", new Exception());
			return null;
		}
		
		try {
			source.replace(resViewInstance, s_viewResolutionTime, null, Utilities.generateDateTime(new Date()));
		} catch (RDFException e) { }
		
		Resource[] types = Utilities.getResourceProperties(resViewInstance, Constants.s_rdf_type, infoSource);

		for (int i = 0; i < types.length; i++) {
			Resource viewPart = getViewPartFromViewClass(underlying, types[i], resType, source, infoSource);
			if (viewPart != null) return viewPart;
		}
		
		return null;
	}
	
	/**
	 * Returns an array of 2 resources [ view instance resource, part resource ]
	 */
	static public Resource[] findOrCreateViewPartOfType(Resource res, Resource resType, IRDFContainer source, IRDFContainer infoSource) {
		Resource[] a = null;
		
		a = Ozone.findViewPartOfType(res, resType, source, infoSource);
		
		if (a == null) {
			try { a = Ozone.findClassViewPartOfType(res, resType, source, infoSource); }
			catch (RDFException e) { }
			
			if (a == null) return null;
			
			Resource resNewView = Utilities.generateUniqueResource();
			
			try {
				infoSource.add(new Statement(res, Constants.s_haystack_view, resNewView));
				infoSource.add(new Statement(resNewView, Constants.s_rdf_type, a[1]));
			} catch (RDFException e) { }
			
			a[1] = a[0];
			a[0] = resNewView;
		}
		
		try { source.replace(a[0], s_viewResolutionTime, null, Utilities.generateDateTime(new Date())); }
		catch (RDFException e) { }

		return a;
	}	

	/**
	 * Optimized function that uses a default view instance.
	 * @return array of 2 resources [ view instance resource, part resource ]
	 */
	static public Resource[] findOrDefaultViewPartOfType(Resource res, Resource resType, IRDFContainer source, IRDFContainer infoSource) {
		Resource[] a = null;
		
		a = Ozone.findViewPartOfType(res, resType, source, infoSource);
		if (a == null) {
			try { a = Ozone.findClassViewPartOfType(res, resType, source, infoSource); }
			catch (RDFException e) { }
			
			if (a == null) return null;
			
			Resource viewInstance;
			if (Utilities.checkBooleanProperty(a[0], OzoneConstants.s_requiresViewInstance, source)) {
				// We must generate a view instance
				viewInstance = Utilities.generateUniqueResource();
				
				try {
					infoSource.add(new Statement(res, Constants.s_haystack_view, viewInstance));
					infoSource.add(new Statement(viewInstance, Constants.s_rdf_type, a[1]));
				} catch (RDFException e) { }
			} else {
				// Is a default view instance available?
				viewInstance = Utilities.getResourceProperty(a[0], OzoneConstants.s_defaultViewInstance, source);
				if (viewInstance == null) {
					viewInstance = Utilities.generateUniqueResource();
					
					try {
						source.add(new Statement(a[0], OzoneConstants.s_defaultViewInstance, viewInstance));
						infoSource.add(new Statement(viewInstance, Constants.s_rdf_type, a[1]));
					} catch (RDFException e) { }
				}
			}
			
			a[1] = a[0];
			a[0] = viewInstance;
		}
		
/*		try {
			source.replace(a[0], s_viewResolutionTime, null, Utilities.generateDateTime(new Date()));
		} catch (RDFException e) {
		}*/

		return a;
	}	

	/**
	 * Returns the URI of the part whose domain is the type of the given resource.
	 */
	static public Resource findPart(Resource res, IRDFContainer source, IRDFContainer partDataSource) throws RDFException {
		if (res == null) {
			s_logger.error("findPart called with null resource", new Exception());
			return null;
		}

		Resource[] types = Utilities.getResourceProperties(res, Constants.s_rdf_type, partDataSource);
		for (int j = 0; j < types.length; j++) {
			RDFNode part = source.extract(null, OzoneConstants.s_dataDomain, types[j]);
			if (part != null) return (Resource)part;
		}
		
		return null;
	}
	
	static private Resource getViewPartFromViewClass(Resource underlying, Resource viewClass, Resource viewPartClass, IRDFContainer source, IRDFContainer infoSource) {
		try {
			Set results = viewPartClass == null ?
				source.query(
					new Statement[] {
						new Statement(Utilities.generateWildcardResource(1), OzoneConstants.s_viewDomain, viewClass) 
					},
					Utilities.generateWildcardResourceArray(1), 
					Utilities.generateWildcardResourceArray(1)
				) :
				source.query(
					new Statement[] {
						new Statement(Utilities.generateWildcardResource(1), OzoneConstants.s_viewDomain, viewClass),
						new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, viewPartClass) 
					},
					Utilities.generateWildcardResourceArray(1), 
					Utilities.generateWildcardResourceArray(1)
				)
			;
			if (results.size() > 0) {
				if (results.size() > 1) {
					Resource resolver = Utilities.getResourceProperty(viewClass, s_viewPartResolver, infoSource);
					if (resolver != null) {
						Interpreter 		interpreter = new Interpreter(source);
						DynamicEnvironment	denv = new DynamicEnvironment(source);
						
						Ozone.initializeDynamicEnvironment(denv, s_context);
				
						try {
							Resource part = (Resource) interpreter.callMethod(resolver, new Object[] { underlying }, denv);
							
							if (part != null) {
								return part;
							}
						} catch (Exception e) {
							s_logger.error("Error in calling view part resolver " + resolver, e);
						}
					}
				}
				return (Resource) ((RDFNode[]) results.iterator().next())[0];
			}
		} catch (RDFException e) { }
		return null;
	}
	
	static public FontMetrics getFontMetrics(Font font) {
		Font oldFont = Ozone.s_gc.getFont();
		Ozone.s_gc.setFont(font);
		FontMetrics fontMetrics = Ozone.s_gc.getFontMetrics();
		Ozone.s_gc.setFont(oldFont);
		return fontMetrics;
	}
	
	static public Interpreter getInterpreter() {
		if (s_interpreter == null) s_interpreter = new Interpreter(s_source);
		return s_interpreter;
	}

	static CustomRunnable s_runnable = new CustomRunnable();

	/**
	 * Schedules a job to run on idle.
	 */
	static public void idleExec(IdleRunnable r) { s_runnable.schedule(r, false, false); }

	/**
	 * Schedules a job to run on idle.
	 */
	static public void idleExecOnce(IdleRunnable r) {
		s_runnable.schedule(r, false, true);
	}

	/**
	 * Schedules a job to run on idle.
	 */
	static public void idleExecFirst(IdleRunnable r) { s_runnable.schedule(r, true, false); }
	
	/**
	 * Returns true if in UI thread.
	 */
	static public boolean isUIThread() { return s_display.getThread().equals(Thread.currentThread()); }
	
	public static String getIndirectLiteralProperty(
		Resource subject,
		Resource predicate,
		IRDFContainer container,
		Context context) {
		Resource res = Utilities.getResourceProperty(subject, predicate, container);
		if (res != null) {
			Interpreter i = Ozone.getInterpreter();
			DynamicEnvironment denv = new DynamicEnvironment(container);
			initializeDynamicEnvironment(denv, context);

			try { return ((Literal) i.callMethod(res, new Object[] {}, denv)).getContent(); }
			catch (Exception e) { }
		}
		return null;
	}

	static public IRDFContainer getInformationSource(IRDFContainer source, Identity id, IServiceAccessor sa) throws Exception {
		Resource userResource = id.getResource();
		FederationRDFContainer infoSource = new FederationRDFContainer();
		infoSource.addSource(source, 1);

		Resource resInfoSource = Utilities.getResourceProperty(userResource, Constants.s_config_defaultInformationSource, source);
		if (resInfoSource != null) {
			IRDFContainer rdfc2 = (IRDFContainer)sa.connectToService(resInfoSource, id);
			infoSource.addSource(rdfc2, 0);
		}

		Resource[] resInfoSources = Utilities.getResourceProperties(userResource, Constants.s_config_secondaryInformationSource, source);
		if (resInfoSources.length != 0) {
			for (int i = 0; i < resInfoSources.length; i++) {
				IRDFContainer rdfc2 = (IRDFContainer)sa.connectToService(resInfoSources[i], id);
				infoSource.addSource(rdfc2, 2);
			}
		}
		return infoSource;
	}

}

class CustomRunnable implements Runnable {
	boolean		m_scheduled = false;
	LinkedList 	m_runnables = new LinkedList();
	
	public void schedule(IdleRunnable r, boolean first, boolean preventDuplicates) {
		if (r != null) {
			synchronized (m_runnables) {
				if (preventDuplicates) m_runnables.remove(r);

				if (!first) m_runnables.addLast(r);
				else m_runnables.addFirst(r);

				/*if (first || r.m_priority == 0) m_runnables.addFirst(r);
				else if (r.m_priority == 10) m_runnables.addLast(r);
				else {
					boolean done = false;
					for (int i = m_runnables.size() - 1; i >= 0; i--) {
						IdleRunnable ir = (IdleRunnable) m_runnables.get(i);
						if (ir.m_priority <= r.m_priority) {
							m_runnables.add(i + 1, r);
							done = true;
							break;
						}
					}
					if (!done) m_runnables.addFirst(r);
				}*/
			}
			schedule();
		}
	}

	public void run() {

		synchronized (this) { m_scheduled = false; }
		
    	IdleRunnable r = null;
    	synchronized (m_runnables) {
    		Iterator i = m_runnables.iterator();
    		while (i.hasNext()) {
    			r = (IdleRunnable) i.next();
    			i.remove();
    			if (r.hasExpired()) r = null;
    			else break;
    		}
    	}
    	
    	if (r != null) {
			try { r.run(); }
			catch (Exception e) {
				Ozone.s_logger.error("Exception caught in custom runnable", e);
			}
    	}
    	
    	synchronized (m_runnables) {
    		if (m_runnables.size() > 0) schedule();
    	}
	}

	void schedule() {
		synchronized (this) {
			if (!m_scheduled) {
				Ozone.s_display.asyncExec(this);
				m_scheduled = true;
//				Ozone.s_logCategory.info("Scheduling runnable");
			}
		}
	}		
}

class ResolutionRecord implements Comparable {
	public Resource	m_viewInstance;
	public Resource	m_part;
	public Date		m_resolutionTime;
	
	
	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof ResolutionRecord) {
			ResolutionRecord r = (ResolutionRecord) o;
			if (m_resolutionTime == null) {
				return r.m_resolutionTime == null ? 0 : -1;
			} else {
				return r.m_resolutionTime == null ? 1 : r.m_resolutionTime.compareTo(m_resolutionTime);
				// Note: we reverse the date comparison order intentionally to ensure the latest record goes first.
			}
		}
		return 0;
	}
}
