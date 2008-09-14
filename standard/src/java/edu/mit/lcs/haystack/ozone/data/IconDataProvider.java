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

import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;

/**
 * @author David Huynh
 */
public class IconDataProvider extends ChainedDataProvider {
	Resource	m_underlying;
	Resource	m_icon;
	
	static final String 	SUMMARY_NAMESPACE = "http://haystack.lcs.mit.edu/ui/summaryView#";
	static final Resource	UNDERLYING = new Resource(SUMMARY_NAMESPACE + "underlying");
	static final Resource	DEFAULT_ICON = new Resource(SUMMARY_NAMESPACE + "defaultIcon");
	
	static final Resource	GENERIC_ICON = new Resource("http://haystack.lcs.mit.edu/data/ozone/icons/types/generic.gif");
	static final Resource	MULTITYPE_ICON = new Resource("http://haystack.lcs.mit.edu/data/ozone/icons/types/multitype.gif");
	static final Resource	ICON = new Resource(OzoneConstants.s_namespace + "icon");
	static final Resource	ICON_RESOLVER = new Resource(OzoneConstants.s_namespace + "iconResolver");
	
	static LocalRDFContainer	s_maps = new LocalRDFContainer();
	static Resource			s_withType = new Resource("withType");
	static Resource			s_hasCount = new Resource("hasCount");
	static Resource			s_hasIcon = new Resource("hasIcon");
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);
		
		if (m_dataProvider == null) {
			Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
			Resource underlying = null;
		
			if (dataSource != null) {
				underlying = Utilities.getResourceProperty(dataSource, UNDERLYING, m_partDataSource);
				if (underlying == null) {
					underlying = Utilities.getResourceProperty(dataSource, DataConstants.RESOURCE, m_partDataSource);
				}
			}
			
			if (underlying == null) {
				underlying = (Resource) m_context.getLocalProperty(UNDERLYING);
			}
			if (underlying == null) {
				underlying = (Resource) m_context.getLocalProperty(DataConstants.RESOURCE);
			}
			
			setUnderlying(underlying);
		}
		
		m_initializing = false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_icon != null) {
			dataConsumer.onDataChanged(DataConstants.RESOURCE_CHANGE, m_icon);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
			
		if (m_underlying == null) {
			throw new DataNotAvailableException("No data available");
		}
		
		if (dataType.equals(DataConstants.RESOURCE)) {
			return m_icon;
		}
		return null;
	}
	
	protected IDataConsumer createDataConsumer() {
		return new ResourceDataConsumer() {
			protected void onResourceChanged(Resource newResource) {
				setUnderlying(newResource);
			}
	
			protected void onResourceDeleted(Resource previousResource) {
				setUnderlying(null);
			}
		};
	}


	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		m_underlying = null;
		m_icon = null;
		
		super.dispose();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(Statement)
	 */
	protected void onStatementAdded(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}


	synchronized protected void setUnderlying(Resource underlying) {
		if (underlying != m_underlying) {
			Resource oldIcon = m_icon;
			
			m_underlying = underlying;
			if (m_underlying != null) {
				m_icon = Utilities.getResourceProperty(m_underlying, ICON, m_infoSource);
				
				if (m_icon == null) {
					m_icon = getIconFromTypes(underlying);
				}
				
				if (m_icon == null) {
					Resource partData = (Resource) m_context.getLocalProperty(OzoneConstants.s_partData);
					m_icon = Utilities.getResourceProperty(partData, DEFAULT_ICON, m_partDataSource);
				}
/*				if (m_icon == null) {
					m_icon = GENERIC_ICON;
				}
*/			} else {
				m_icon = null;
			}
				
			if (m_icon != oldIcon) {
				if (m_icon == null) {
					notifyDataConsumers(DataConstants.RESOURCE_DELETION, oldIcon);
				} else {
					notifyDataConsumers(DataConstants.RESOURCE_CHANGE, m_icon);
				}
			}
		}
	}
	
	Resource getIconFromTypes(Resource underlying) {
		Resource[] types = Utilities.getResourceProperties(underlying, Constants.s_rdf_type, m_infoSource);
					
		/*
		 * Look up stored results
		 */
		{	
			Statement[]	statements = new Statement[types.length + 2];
			
			for (int i = 0; i < types.length; i++) {
				statements[i] = new Statement(Utilities.generateWildcardResource(2), s_withType, types[i]);
			}
			statements[types.length] = new Statement(Utilities.generateWildcardResource(2), s_hasCount, new Literal(Integer.toString(types.length)));
			statements[types.length+1] = new Statement(Utilities.generateWildcardResource(2), s_hasIcon, Utilities.generateWildcardResource(1));
			
			try {
				RDFNode[] nodes = s_maps.queryExtract(statements, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
				
				if (nodes != null && nodes.length > 0) {
					return (Resource) nodes[0];
				}
			} catch (RDFException e) {
			}
		}
		
		Resource icon = null;
		{
			if (types.length == 1) {
				icon = Utilities.getResourceProperty(types[0], ICON, m_infoSource);
			} else {
				HashSet superTypes = new HashSet();
							
				for (int i = 0; i < types.length; i++) {
					superTypes.add(types[i]);
				}
							
				HashMap typeHierarchy = Utilities.getTypeHierarchy(superTypes, m_infoSource);
							
				Set icons = getIcons(superTypes, typeHierarchy, m_infoSource);
							
				if (icons != null && icons.size() == 1) {
					icon = (Resource) icons.iterator().next();
				}
			}
			
			if (icon != null) {
				Resource r = Utilities.generateUniqueResource();
			
				for (int j = 0; j < types.length; j++) {
					s_maps.add(new Statement(r, s_withType, types[j]));
				}
				s_maps.add(new Statement(r, s_hasCount, new Literal(Integer.toString(types.length))));
				s_maps.add(new Statement(r, s_hasIcon, icon));
			}
		}
		
		return icon;
	}
	
	Set getIcons(Set types, HashMap typeHierarchy, IRDFContainer source) {
		HashSet icons = new HashSet();
		
		Iterator i = types.iterator();
		
		while (i.hasNext()) {
			Resource 	type = (Resource) i.next();
			Set 		subtypeIcons = getIconsForOneType(type, typeHierarchy, source);
			
			if (subtypeIcons != null) {
				icons.addAll(subtypeIcons);
			}
		}
		
		return icons;
	}
	
	Set getIconsForOneType(Resource type, HashMap typeHierarchy, IRDFContainer source) {
		Set subtypes = (Set) typeHierarchy.get(type);
		if (subtypes != null) {
			Set subtypeIcons = getIcons(subtypes, typeHierarchy, source);
			
			if (subtypeIcons != null) {
				return subtypeIcons;
			}
		}
		
		Resource icon = null;
		Resource iconResolver = Utilities.getResourceProperty(type, ICON_RESOLVER, source);
		if (iconResolver != null) {
			try {
				Interpreter 		interpreter = Ozone.getInterpreter();
				DynamicEnvironment	denv = new DynamicEnvironment(m_source);
				Ozone.initializeDynamicEnvironment(denv, m_context);
				
				icon = (Resource) interpreter.callMethod(iconResolver, new Object[] { m_underlying }, denv);
			} catch (AdenineException e) {
			}
		}
		if (icon == null) {
			icon = Utilities.getResourceProperty(type, ICON, source);
		}
		
		if (icon != null) {
			Set icons = new HashSet();
			
			icons.add(icon);
			
			return icons;
		}
		
		return null;
	}
}
