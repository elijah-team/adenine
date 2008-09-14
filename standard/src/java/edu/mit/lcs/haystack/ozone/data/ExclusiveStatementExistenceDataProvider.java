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

import edu.mit.lcs.haystack.rdf.*;

/**
 * @author David Huynh
 */
public class ExclusiveStatementExistenceDataProvider extends StatementExistenceDataProvider {
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		if (changeType.equals(DataConstants.BOOLEAN_CHANGE)) {
			if (change instanceof Boolean) {
				Boolean		exists = (Boolean) change;

				if (exists.booleanValue() != m_exists &&
					m_subject != null &&
					m_predicate != null &&
					m_object != null) {
					try {
						if (exists.booleanValue()) {
							if (m_dynamicSubject) {
								m_infoSource.replace(m_subject, m_predicate, null, m_object);
							} else {
								m_infoSource.replace(null, m_predicate, m_object, m_subject);
							}
						} else {
							m_infoSource.remove(new Statement(m_subject, m_predicate, m_object), new Resource[] {});
						}
					} catch (RDFException e) {
						s_logger.error("Failed to perform change", e);
					}
				}
			} else {
				throw new DataMismatchException("Change data type mismatch: expecting java.util.Set instead of " + change.getClass());
			}
		} else {
			throw new UnsupportedOperationException("Data provider does not support change to type " + changeType);
		}
	}
}
