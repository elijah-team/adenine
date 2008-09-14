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

import java.util.Iterator;

/**
 * @author David Huynh
 */
public class BooleanOrDataProvider extends BooleanDataProviderBase {

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
				
				if (v) {
					onValue(true);
				} else {
					v = false;
					
					synchronized (m_providerToValue) {
						Iterator i = m_providerToValue.values().iterator();
						
						while (i.hasNext()) {
							if (((Boolean) i.next()).booleanValue()) {
								v = true;
								break;						
							}
						}
					}
					
					onValue(v);
				}
			}
			
			public BooleanDataConsumer init(IDataProvider dataProvider) {
				m_dataProvider = dataProvider;
				return this;
			}
		}.init(dataProvider);
	}
}
