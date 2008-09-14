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
 * Created on Feb 11, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.Resource;

/**
 * @author David Huynh
 */
public class BooleanNotDataProvider extends BooleanDataProviderBase {

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.IPart#initialize(edu.mit.lcs.haystack.rdf.IRDFContainer, edu.mit.lcs.haystack.ozone.Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		m_value = true;
		super.initialize(source, context);
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.BooleanDataProviderBase#createDataConsumer()
	 */
	protected BooleanDataConsumer createDataConsumer(IDataProvider dataProvider) {
		return new BooleanDataConsumer() {
			IDataProvider m_dataProvider;
			
			protected void onBooleanChanged(Boolean b) {
				boolean v = b.booleanValue();
				
				synchronized (m_providerToValue) {
					m_providerToValue.put(m_dataProvider, b);
				}
				
				onValue(!v);
			}
			
			public BooleanDataConsumer init(IDataProvider dataProvider) {
				m_dataProvider = dataProvider;
				return this;
			}
		}.init(dataProvider);
	}
	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(edu.mit.lcs.haystack.rdf.Resource, java.lang.Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
			
		IDataProvider dp = (IDataProvider) m_dataProviders.get(0);
				
		if (changeType.equals(DataConstants.LITERAL_CHANGE) ||
			changeType.equals(DataConstants.STRING_CHANGE)) {
				
			String s = null;
			if (change instanceof Literal) {
				s = ((Literal) change).getContent();
			} else if (change instanceof String) {
				s = (String) change;
			} else {
				throw new DataMismatchException("Excpected a String or a Literal");
			}
			
			boolean b = false;
			if (s != null && s.equals("true")) {
				b = true;
			}

			dp.requestChange(DataConstants.BOOLEAN_CHANGE, new Boolean(!b));
			return;
		} else if (changeType.equals(DataConstants.BOOLEAN_CHANGE)) {
			if (change instanceof Boolean) {
				dp.requestChange(changeType, new Boolean(!((Boolean) change).booleanValue()));
				
				return;
			} else {
				throw new DataMismatchException("Expected a Boolean");
			}
		}

		super.requestChange(changeType, change);
	}

}
