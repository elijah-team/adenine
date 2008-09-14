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

import java.io.Serializable;
import java.util.Set;

/**
 * @author David Huynh
 */
public class SetDataProviderWrapper implements Serializable {
	IDataProvider m_provider;
	
	public SetDataProviderWrapper(IDataProvider provider) {
		m_provider = provider;
		if (provider == null) {
			throw new NullPointerException("IDataProvider object to wrap is null");
		}
	}
	
	public IDataProvider getDataProvider() {
		return m_provider;
	}

	public void dispose() {
		m_provider.dispose();
		m_provider = null;
	}

	public Set getSet() throws DataNotAvailableException, DataMismatchException {
		try {
			return (Set) m_provider.getData(DataConstants.SET, null);
		} catch (ClassCastException e) {
			throw new DataMismatchException("Wrapped data provider does not provide a Set");
		}
	}
	
	public void requestAddition(Object item) throws 
		UnsupportedOperationException, DataMismatchException {
		
		m_provider.requestChange(DataConstants.SET_ADDITION, item);
	}
	
	public void requestRemoval(Object item) throws
		UnsupportedOperationException, DataMismatchException {

		m_provider.requestChange(DataConstants.SET_REMOVAL, item);
	}
	
	public void requestClear() throws
		UnsupportedOperationException, DataMismatchException {
			
		m_provider.requestChange(DataConstants.SET_CLEAR, null);
	}
}
