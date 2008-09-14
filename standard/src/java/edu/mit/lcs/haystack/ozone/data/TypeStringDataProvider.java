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

/*
 * Created on Feb 9, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.RDFException;
import edu.mit.lcs.haystack.rdf.RDFNode;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Statement;
import edu.mit.lcs.haystack.rdf.Utilities;

/**
 * @author Dennis Quan
 */
public class TypeStringDataProvider extends ChainedDataProvider {
	static final String 	SUMMARY_NAMESPACE = "http://haystack.lcs.mit.edu/ui/summaryView#";
	static final Resource	UNDERLYING = new Resource(SUMMARY_NAMESPACE + "underlying");
	static final Resource	TITLE_SOURCE_PREDICATE = new Resource("http://haystack.lcs.mit.edu/schemata/vowl#titleSourcePredicate");
	
	static LocalRDFContainer	s_maps = new LocalRDFContainer();
	static Resource			s_withType = new Resource("withType");
	static Resource			s_hasCount = new Resource("hasCount");
	static Resource			s_hasLabel = new Resource("hasLabel");

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
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

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		String type = m_type;
		if (type != null) {
			dataConsumer.onDataChanged(DataConstants.STRING_CHANGE, type);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (m_underlying == null) {
			throw new DataNotAvailableException("No data available");
		}
	
		if (dataType.equals(DataConstants.STRING)) {
			return m_type;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.core.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, true);
		
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
	}
	
	protected String determineTypeString(Resource underlying) {
		Resource[] 	types = Utilities.getResourceProperties(underlying, Constants.s_rdf_type, m_infoSource);

		/*
		 * Look up stored results
		 */
		{	
			Statement[]	statements = new Statement[types.length + 2];
			
			for (int i = 0; i < types.length; i++) {
				statements[i] = new Statement(Utilities.generateWildcardResource(2), s_withType, types[i]);
			}
			statements[types.length] = new Statement(Utilities.generateWildcardResource(2), s_hasCount, new Literal(Integer.toString(types.length)));
			statements[types.length+1] = new Statement(Utilities.generateWildcardResource(2), s_hasLabel, Utilities.generateWildcardResource(1));
			
			try {
				RDFNode[] nodes = s_maps.queryExtract(statements, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(2));
				
				if (nodes != null && nodes.length > 0) {
					return ((Literal) nodes[0]).getContent();
				}
			} catch (RDFException e) {
			}
		}
				
		{
			TreeSet typeLabels = new TreeSet(); 
							
			if (types.length == 1) {
				String label = Utilities.getLiteralProperty(types[0], Constants.s_rdfs_label, m_source);
				if (label != null) {
					typeLabels.add(label);
				}
			} else {
				HashSet superTypes = new HashSet();
								
				for (int i = 0; i < types.length; i++) {
					superTypes.add(types[i]);
				}
								
				HashMap typeHierarchy = Utilities.getTypeHierarchy(superTypes, m_source);
				Iterator i = superTypes.iterator();
				while (i.hasNext()) {
					addNames((Resource) i.next(), typeHierarchy, typeLabels);
				}
			}
			
			if (typeLabels.isEmpty()) {
				return null;
			}
			
			StringBuffer sb = new StringBuffer();
			Iterator i = typeLabels.iterator();
			while (i.hasNext()) {
				String label = (String) i.next();
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(label);
			}	
			
			String result = sb.toString();
			
			/*
			 * Store results
			 */
			Resource r = Utilities.generateUniqueResource();
			
			for (int j = 0; j < types.length; j++) {
				s_maps.add(new Statement(r, s_withType, types[j]));
			}
			s_maps.add(new Statement(r, s_hasCount, new Literal(Integer.toString(types.length))));
			s_maps.add(new Statement(r, s_hasLabel, new Literal(result)));
			
			return result;
		}
	} 
	
	protected void addNames(Resource type, HashMap typeHierarchy, Set labels) {
		Set set = (Set) typeHierarchy.get(type);
		if (set == null) {
			String label = Utilities.getLiteralProperty(type, Constants.s_rdfs_label, m_source);
			if (label != null) {
				labels.add(label);
			}
		} else {
			Iterator i = set.iterator();
			while (i.hasNext()) {
				type = (Resource) i.next();
				addNames(type, typeHierarchy, labels);
			}
		}
	}

	protected String m_type = null;
	protected Resource m_underlying = null;
	protected Resource m_cookie = null;
	
	protected void setUnderlying(Resource underlying) {
		String oldType = m_type;
		synchronized (this) {
			m_type = determineTypeString(underlying);
			m_underlying = underlying;
			
			// Redo listeners
			if (m_cookie != null) {
				removePattern(m_cookie);
			}
			
			if (m_underlying != null) {
				try {
					m_cookie = addPattern(m_underlying, Constants.s_rdf_type, null);
				} catch (RDFException e) {
				}
			}
		}
		handleChange(oldType);
	}
	
	protected void handleChange(String oldType) {
		String type = m_type;
		if ((oldType == null && type != null) || (oldType != null && type == null) ||
			(oldType != null && type != null && !oldType.equals(type))) {
			if (type != null) {
				notifyDataConsumers(DataConstants.STRING_CHANGE, type);
			} else {
				notifyDataConsumers(DataConstants.STRING_DELETION, null);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
		String oldType = m_type;
		synchronized (this) {
			if (m_underlying != null) {
				m_type = determineTypeString(m_underlying);
			}
		}
		handleChange(oldType);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
		String oldType = m_type;
		synchronized (this) {
			if (m_underlying != null) {
				m_type = determineTypeString(m_underlying);
			}
		}
		handleChange(oldType);
	}
}
