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

import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.security.Identity;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Control;

/**
 * Stores context information for a part instance.
 * 
 * @version 	1.0
 * @author		Dennis Quan
 * @author		David Huynh
 * @author		Stephen Garland
 */
public class Context implements Cloneable, Serializable {
	
	HashMap m_properties = new HashMap();
	HashMap m_localProperties = new HashMap();
	Context	m_parent;
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(Context.class);
	
	public Context() { }
	
	public Context(Context parent) { m_parent = parent; }
	
	public Context getParentContext() { return m_parent; }
	
	public void setParentContext(Context c) { m_parent = c; }
	
	public Object clone() {
		Context c = new Context();
		c.m_parent = m_parent;
		c.m_properties = (HashMap)m_properties.clone();
		c.m_localProperties = (HashMap)m_localProperties.clone();
		return c;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		formatContext(sb, this, 0);
		return sb.toString();
	}
	
	static void formatContext(StringBuffer sb, Context c, int level) {
		String ancestor = level == 0 ? "" : " of ancestor +" + level;
		formatProperties(sb, "Local properties" + ancestor, c.m_localProperties);
		formatProperties(sb, "Global properties" + ancestor, c. m_properties);
		if (c.m_parent == null) return;
		sb.append("\n");
		formatContext(sb, c.m_parent, level+1);
	}
	
	public static void formatProperties(StringBuffer buf, String label, Map m) {
		boolean firstTime = true;
		for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
			if (firstTime) buf.append(label);
			Entry e = (Entry) (i.next());
			buf.append("\n  ");
			buf.append(e.getKey() + "=" + e.getValue());
			firstTime = false;
		}
		if (!firstTime) buf.append("\n");
	}
	
	/**
	 * Returns the property named by a given resource, whether it is stored in this context or a parent.
	 */
	public Object getProperty(Resource res) {
		return m_localProperties.containsKey(res) ?  m_localProperties.get(res) : getChainedProperty(res);
	}
	
	/**
	 * Returns the property named by a given resource, 
	 * only if it is stored in this context but can be inherited.
	 */
	public Object getUnchainedProperty(Resource res) { return m_properties.get(res); }
	
	/**
	 * Returns the property named by the given resource, registered locally.
	 */
	public Object getLocalProperty(Resource res) { return m_localProperties.get(res); }
	
	public Context whichDescendantExposesProperty(Resource res) {
		return m_properties.containsKey(res) ? this : m_parent != null ? m_parent.whichDescendantExposesProperty(res) : null;
	}
	
	public Context whichDescendantExposesLocalProperty(Resource res) {
		return m_localProperties.containsKey(res) ? this : m_parent != null ? m_parent.whichDescendantExposesLocalProperty(res) : null;
	}
	
	public Object getDescendantLocalProperty(Resource res) {
		Object o = m_localProperties.get(res);
		return o != null ? o : m_parent != null ? m_parent.getDescendantLocalProperty(res) : null;
	}
	
	/**
	 * Registers a property in this context.
	 * @param res	The name of the property.
	 * @param o	The property value.
	 */
	public void putProperty(Resource res, Object o) { m_properties.put(res, o); }
	
	/**
	 * Registers a non-inheritable property in this context.
	 * @param res	The name of the property.
	 * @param o	The property value.
	 */
	public void putLocalProperty(Resource res, Object o) { m_localProperties.put(res, o); }
	
	/**
	 * Registers a property in the property located at the end of the ancestry chain.
	 * @param res	The name of the property.
	 * @param o	The property value.
	 */
	public void putGlobalProperty(Resource res, Object o) {
		if (m_parent != null) m_parent.putGlobalProperty(res, o);
		else putProperty(res, o);
	}
	
	/**
	 * Unregisters a property from this context.
	 * @param res	The name of the property.
	 */
	public Object removeProperty(Resource res) { return m_properties.remove(res); }
	
	/**
	 * Unregisters a local property from this context.
	 * @param res	The name of the property.
	 */
	public Object removeLocalProperty(Resource res) { return m_localProperties.remove(res); }
	
	/**
	 * Unregisters a property from the context located at the end of the ancestry chain.
	 * @param res	The name of the property.
	 */
	public Object removeGlobalProperty(Resource res) {
		return m_parent != null ? m_parent.removeGlobalProperty(res) : removeProperty(res);
	}
	
	protected Object getChainedProperty(Resource res) {
		return m_properties.containsKey(res) ? m_properties.get(res) : m_parent != null ? m_parent.getChainedProperty(res) : null;
	}
	
	/**
	 * Convenience method that returns the service accessor.
	 */	
	public IServiceAccessor getServiceAccessor() {
		IServiceAccessor sa = (IServiceAccessor)getProperty(OzoneConstants.s_serviceAccessor);
		if (sa == null) {
			s_logger.error("Null service accessor", new Exception());
			System.out.println(this);
		}
		return sa;
	}
	
	/**
	 * Convenience method that returns the current user's resource.
	 */
	public Resource getUserResource() { return (Resource)getProperty(OzoneConstants.s_user); }
	
	/**
	 * Convenience method that returns the current user's identity.
	 */
	public Identity getUserIdentity() { return (Identity)getProperty(OzoneConstants.s_identity); }
	
	/**
	 * Convenience method that returns the information source.
	 */
	public IRDFContainer getInformationSource() {
		UnserializableWrapper uo = (UnserializableWrapper) getProperty(OzoneConstants.s_informationSource);
		return uo == null ? null : (IRDFContainer)uo.m_object;
	}
	
	/**
	 * Convenience method that sets the information source.
	 */
	public void setInformationSource(IRDFContainer rdfc) {
		putProperty(OzoneConstants.s_informationSource, new UnserializableWrapper(rdfc));
	}
	
	/**
	 * Convenience method that returns the current SWT control.
	 */
	public Control getSWTControl() {
		UnserializableWrapper uo = (UnserializableWrapper) getProperty(OzoneConstants.s_swtControl);
		return uo == null ? null : (Control)uo.m_object;
	}
	
	/**
	 * Convenience method that sets the current SWT control.
	 */
	public void setSWTControl(Control c) {
		putProperty(OzoneConstants.s_swtControl, new UnserializableWrapper(c));
	}
	
	/**
	 * Convenience method that returns the current view container factory.
	 */
	public IViewContainerFactory getViewContainerFactory() {
		UnserializableWrapper uo = (UnserializableWrapper) getProperty(OzoneConstants.s_viewContainerFactory);
		return uo == null ? null : (IViewContainerFactory) uo.m_object;
	}
	
	/**
	 * Convenience method that sets the current view container factory.
	 */
	public void setViewContainerFactory(IViewContainerFactory c) {
		putProperty(OzoneConstants.s_viewContainerFactory, new UnserializableWrapper(c));
	}
	
}
