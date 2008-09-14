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

package edu.mit.lcs.haystack.server.standard.adenine;

import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.javaByteCode.JavaByteCodeCompiler;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.content.ContentClient;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Literal;
import edu.mit.lcs.haystack.rdf.PackageFilterRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.server.core.service.IService;
import edu.mit.lcs.haystack.server.core.service.ServiceException;
import edu.mit.lcs.haystack.server.core.service.ServiceManager;
import edu.mit.lcs.haystack.server.standard.scheduler.IScheduledTask;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * @version 	1.0
 * @author		Dennis Quan
 */
public class AdenineSourceAgent implements IService, IScheduledTask {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(AdenineSourceAgent.class);

	protected ServiceManager m_manager;
	protected Resource m_resource;
	protected String m_basePath;
	protected String m_outputDirectory;
	protected Interpreter m_interpreter;
	protected boolean m_compiling = false;
	
	//TODO: Resolve duplicate definition in haystack.adenine.compiler.Compiler
	static public final String NAMESPACE = "http://haystack.lcs.mit.edu/agents/adenine#";
	static public final Resource BASE = new Resource(NAMESPACE + "base");
	static public final Resource ADENINE_FILE = new Resource(NAMESPACE + "AdenineFile");
	static public final Resource OUTPUT_DIRECTORY = new Resource(NAMESPACE + "outputDirectory");
	static public final Resource PRECOMPILE_TIME = new Resource(NAMESPACE + "precompileTime");
	
	/**
	 * @see IService#init(String, ServiceManager, Resource)
	 */
	public void init(String basePath, ServiceManager manager, Resource res)
		throws ServiceException {
		m_manager = manager;
		m_resource = res;
		IRDFContainer source = manager.getRootRDFContainer();
		m_interpreter = new Interpreter(source);
		try {
			m_basePath = new URL(Utilities.getResourceProperty(res, BASE, source).getURI()).getPath();
		} catch(MalformedURLException e) {
			throw new ServiceException("Invalid source:base passed to AdenineSourceAgent", e);
		}
		m_outputDirectory = Utilities.getLiteralProperty(res, OUTPUT_DIRECTORY, source);
	}

	/**
	 * @see IService#getServiceResource()
	 */
	public Resource getServiceResource() {
		return m_resource;
	}

	/**
	 * @see IService#shutdown()
	 */
	public void shutdown() throws ServiceException {
	}

	/**
	 * @see IService#cleanup()
	 */
	public void cleanup() throws ServiceException {
	}

	/**
	 * @see IScheduledTask#performScheduledTask(Resource)
	 */
	public void performScheduledTask(Resource resTask) throws ServiceException {
		try {
			if (m_compiling) {
				return;
			}

			precompile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void precompile() throws Exception {
		m_compiling = true;
		IRDFContainer source = m_manager.getRootRDFContainer();
		Resource[] toPrecompile = Utilities.getResourceSubjects(AdenineConstants.precompile, new Literal("true"), source);
		for (int i = 0; !m_manager.isShuttingDown() && (i < toPrecompile.length); i++) {
			String lastCompile = Utilities.getLiteralProperty(toPrecompile[i], AdenineConstants.compileTime, source);
			String lastPrecompile = Utilities.getLiteralProperty(toPrecompile[i], PRECOMPILE_TIME, source);
			try {
				if (lastPrecompile == null) {
					//s_logCategory.info("Precompiling " + toPrecompile[i] + "...");

					// Mark precompile time first so that if there is a failure, the system won't keep retrying
					source.replace(toPrecompile[i], PRECOMPILE_TIME, null, new Literal(new Date().toString()));

					m_interpreter.compileMethodToJava(toPrecompile[i], m_outputDirectory);
				} else if (lastCompile != null) {
					long lastCompile2 = Utilities.parseDateTime(lastCompile).getTime();
					long lastPrecompile2 = Utilities.parseDateTime(lastPrecompile).getTime();
					if (lastCompile2 > lastPrecompile2) {
						//s_logCategory.info("Precompiling " + toPrecompile[i] + "...");

						// Mark precompile time first so that if there is a failure, the system won't keep retrying
						source.replace(toPrecompile[i], PRECOMPILE_TIME, null, new Literal(new Date().toString()));
	
						m_interpreter.compileMethodToJava(toPrecompile[i], m_outputDirectory);
					}
				}
			} catch (Exception e) {
				s_logger.warn("Precompilation error occurred compiling " + toPrecompile[i], e);
			}
		}
		m_compiling = false;
	}

	public void compile(Resource res) throws ServiceException {
		try {
			Date now = new Date();
			IRDFContainer rdfc = m_manager.getRootRDFContainer();
			Utilities.uninstallPackage(res, rdfc);
			
			PackageFilterRDFContainer 	rdfc2 = new PackageFilterRDFContainer(rdfc, res);
			ICompiler 					compiler = new JavaByteCodeCompiler(rdfc2);
			java.util.List				errors = compiler.compile(
				null, 
				new InputStreamReader(ContentClient.getContentClient(res, rdfc, m_manager).getContent()), 
				"<generated by adenine source agent>", 
				null,
				null
			);
			
			if (!errors.isEmpty()) {
				throw new ServiceException("", (Exception) errors.get(0));
			}
		} catch(Exception e) {
			throw new ServiceException("", e);
		}
	}
}
