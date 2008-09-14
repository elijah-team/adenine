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
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;

/**
 * @author David Huynh
 */
public class DataUtilities {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(DataUtilities.class);	
	
	public static Resource findDataProvider(Resource dataSource, IRDFContainer source, IRDFContainer partDataSource) {
		try {
			Resource[] types = Utilities.getResourceProperties(dataSource, Constants.s_rdf_type, partDataSource);
			for (int j = 0; j < types.length; j++) {
				RDFNode[] part = source.queryExtract(new Statement[] {
					new Statement(Utilities.generateWildcardResource(1), OzoneConstants.s_dataDomain, types[j]),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_rdf_type, DataConstants.s_DataProvider),
				}, Utilities.generateWildcardResourceArray(1), Utilities.generateWildcardResourceArray(1));
				if (part != null) {
					return (Resource) part[0];
				}
			}
			s_logger.error("Failed to find part for data source " + dataSource, new Exception());
			return null;
		} catch (RDFException e) {
			s_logger.error("Failed to find part for data source " + dataSource, e);
			return null;
		}
	}

	public static IDataProvider createDataProvider(Resource dataSource, Context parentContext, IRDFContainer source) {
		return createDataProvider2(dataSource, new Context(parentContext), source, source);
	}
		
	public static IDataProvider createDataProvider(Resource dataSource, Context parentContext, IRDFContainer source, IRDFContainer partDataSource) {
		return createDataProvider2(dataSource, new Context(parentContext), source, partDataSource);
	}
		
	/**
	 * Like createDataProvider, but fills the supplied context object in with
	 * ozone:partData and ozone:part.
	 * @param dataSource
	 * @param context
	 * @param source
	 * @param partDataSource
	 * @return IDataProvider
	 */
	public static IDataProvider createDataProvider2(Resource dataSource, Context context, IRDFContainer source, IRDFContainer partDataSource) {
		Resource dataProviderPart = DataUtilities.findDataProvider(dataSource, source, partDataSource);
		if (dataProviderPart == null) {
			return null;
		}
		
		IDataProvider dataProvider = null;
		try {
			Class cls = Utilities.loadClass(dataProviderPart, source);
		
			dataProvider = (IDataProvider) cls.newInstance();
		} catch (Exception e) {
			s_logger.error("Failed to load class for part " + dataProviderPart, e);
		}
		if (dataProvider == null) {
			return null;
		}
		
		context.putLocalProperty(OzoneConstants.s_partData, dataSource);
		context.putLocalProperty(OzoneConstants.s_part, dataProviderPart);
		
		dataProvider.initialize(source, context);
		
		return dataProvider;
	}
}
