/*
 * Created on Nov 7, 2003
 */
package edu.mit.lcs.haystack.ozone.data;

import java.util.HashSet;

import edu.mit.lcs.haystack.ozone.data.DataConstants;
import edu.mit.lcs.haystack.ozone.data.DataMismatchException;
import edu.mit.lcs.haystack.ozone.data.DataNotAvailableException;
import edu.mit.lcs.haystack.ozone.data.GenericDataProvider;
import edu.mit.lcs.haystack.ozone.data.IDataConsumer;
import edu.mit.lcs.haystack.ozone.data.IDataProvider;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author Dennis Quan
 */
abstract public class BufferedSetDataProvider extends GenericDataProvider {
	protected HashSet m_lastSet = new HashSet();

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		getProvider().requestChange(changeType, change);
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		HashSet set = new HashSet();

		synchronized (this) {
			set.addAll(m_lastSet);
		}
		
		if (!set.isEmpty()) {
			dataConsumer.onDataChanged(DataConstants.SET_ADDITION, set);
		}
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		if (dataType == null || dataType.equals(DataConstants.SET)) {
			HashSet set = new HashSet();

			synchronized (this) {
				set.addAll(m_lastSet);
			}
		
			return set;
		} else {
			return null;
		}
	}

	abstract protected IDataProvider getProvider();
}
