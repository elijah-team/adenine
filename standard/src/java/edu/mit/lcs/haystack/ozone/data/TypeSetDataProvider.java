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

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.data.ChainedDataProvider;
import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.rdf.*;
import java.util.*;

/**
 * @author David Huynh
 */
public class TypeSetDataProvider extends ChainedDataProvider {
	protected Set		m_data = new HashSet();
	protected Set		m_types = new HashSet();

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	protected boolean m_initializing = true;
	public void initialize(IRDFContainer source, Context context) {
		Resource dataSource = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		if (dataSource == null) {
			return;
		}

		internalInitialize(source, context, true);
		
		if (m_dataProvider == null) {
			Resource res = Utilities.getResourceProperty(m_prescription, OzoneConstants.s_underlying, m_source);

			if (res != null) {
				HashSet set = new HashSet();

				set.add(res);

				onNewData(set);
			}
		}

		m_initializing = false;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#createDataConsumer()
	 */
	protected IDataConsumer createDataConsumer() {
		return new IDataConsumer() {
			public void reset() {
			}

			public void onDataChanged(Resource changeType, Object change)
				throws IllegalArgumentException {

				HashSet set = new HashSet();

				if (changeType.equals(DataConstants.RESOURCE_CHANGE)) {
					if (change instanceof Resource) {
						set.add(change);
					} else {
						throw new IllegalArgumentException("Expecting a Resource");
					}
				} else if (changeType.equals(DataConstants.RESOURCE_DELETION)) {
				} else if (changeType.equals(DataConstants.SET_ADDITION)) {
					set.addAll(m_data);
					set.addAll((Set) change);
				} else if (changeType.equals(DataConstants.SET_REMOVAL)) {
					set.addAll(m_data);
					set.removeAll((Set) change);
				} else if (changeType.equals(DataConstants.SET_CLEAR)) {
				} else {
					throw new IllegalArgumentException("Unexpected change type");
				}

				onNewData(set);
			}

			public void onStatusChanged(Resource status) {
			}
		};
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		if (m_types.size() > 0) {
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, new HashSet(m_types));
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {

		if (dataType.equals(DataConstants.SET)) {
			return m_types;
		}

		return null;
	}

	protected void onNewData(Set newData) {
		m_data.clear();
		m_data = newData;

		HashSet types = new HashSet();

		Iterator i = newData.iterator();
		while (i.hasNext()) {
			Resource r = (Resource) i.next();
			try {
				Set results = m_infoSource.query(new Statement[] {
					new Statement(r, Constants.s_rdf_type, Utilities.generateWildcardResource(1)) },
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1));

				Iterator j = results.iterator();

				while (j.hasNext()) {
					RDFNode[] nodes = (RDFNode[]) j.next();

					types.add(nodes[0]);
				}
			} catch (RDFException e) {
			}
		}

		if (m_types.size() > 0) {
			m_types.clear();

			if (!m_initializing) {
				notifyDataConsumers(DataConstants.SET_CLEAR, null);
			}
		}

		m_types = types;
		
		if (m_types.size() > 0 && !m_initializing) {
			this.notifyDataConsumers(DataConstants.SET_ADDITION, m_types);
		}
	}
}
