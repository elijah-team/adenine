package edu.mit.lcs.haystack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;

public class SystemProperties {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(SystemProperties.class);

	/**
	 * Prefix for system properties.
	 */
	private static final String s_prefix = "edu.mit.csail.haystack";
	
	/**
	 * Base path of the data area for the Haystack plugin.  Defaults to the absolute path to 
	 * "runtime-workspace/haystack" under Eclipse.
	 */
	public static String s_basepath = 
		new File(System.getProperty(s_prefix + ".basepath", "runtime-workspace" + File.separatorChar + "haystack")).getAbsolutePath();
	
	/**
	 * Name of the Adenine bootstrap file, which is read to reboot the system.  Defaults to "bootstrap.ad" 
	 * in the directory <code>s_basepath</code>.
	 */
	public static String s_bootstrap = 
		System.getProperty(s_prefix + ".bootstrap",
				s_basepath + File.separatorChar + "bootstrap.ad");
	
	/**
	 * URL of Adenine bootstrap template.  Used to create the Adenine bootstrap file, if it does
	 * not already exist.  Defaults to "/bootstrap/bootstrapEclipse.ad".
	 */
	public static String s_bootstrapTemplate = 
		System.getProperty(s_prefix + ".bootstrapTemplate",
				Haystack.class.getResource("/bootstrap/bootstrapEclipse.ad").toString());
	
	/**
	 * 
	 */
	public static String s_catalinaHome = System.getProperty("catalina.home", "");
	
	/**
	 * 
	 */
	public static String s_hostname = 
		System.getProperty(s_prefix + ".hostname", defaultHostname());
	
	private static String defaultHostname() {
		try { return InetAddress.getLocalHost().getCanonicalHostName(); }
		catch (Exception e) {}
		return "localhost";
	}
	
	/**
	 * Initial perspective for Eclipse workbench.  Null if none is specified.
	 * 
	 * @see edu.mit.lcs.haystack.eclipse.WindowAdvisor#getInitialPerspective
	 */
	public static String s_initialPerspective = 
		System.getProperty(s_prefix + ".initialPerspective", "edu.mit.csail.haystack.eclipse.FullScreenPerspective");
	
	/**
	 * True if the operating system is Macintosh OS X.
	 */
	public static boolean s_isMacOSX = "Mac OS X".equals(System.getProperty("os.name"));
	
	/**
	 * 
	 */
	public static String s_PluginResources = s_prefix + ".PluginResources";
	
	/**
	 * 
	 */
	public static String s_packageSet = 
		System.getProperty(s_prefix + ".packageSet", "http://haystack.lcs.mit.edu/bootstrap/eclipse");
	
	/**
	 * 
	 */
	public static String s_precompile = 
		System.getProperty(s_prefix + ".precompile", ".");
	
	/**
	 * 
	 */
	public static boolean s_purgeRDF = 
		System.getProperty(s_prefix + ".purgeRDF", "false").equals("true");
	
	/**
	 * Base path for Haystack user files.  Defaults to s_basepath.
	 */
	public static String s_userpath = 
		(s_isMacOSX ? System.getProperty("user.home") + "/Library/Application Support/Haystack"
				: System.getProperty(s_prefix + ".userpath", s_basepath));
	
	public static void listAll() {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		System.getProperties().list(new PrintStream(b)); 
		String[] p = b.toString().split("\n");
		//Arrays.sort(p);
		s_logger.info("********** System.getProperties() **********");
		for (int i = 0; i < p.length; i++) s_logger.info(p[i].substring(0, p[i].length()-1));
		s_logger.info("********** System.getProperties() **********");
		s_logger.info("********** SystemProperties **********");
		s_logger.info("basepath:             " + s_basepath);
		s_logger.info("  System.getProperty: " +   System.getProperty(s_prefix + ".basepath"));
		s_logger.info("  Default:            " +   new File("runtime-workspace/haystack").getAbsolutePath());
		s_logger.info("bootstrap:            " + s_bootstrap);
		s_logger.info("  System.getProperty: " +   System.getProperty(s_prefix + ".bootstrap"));
		s_logger.info("  Default:            " +   s_basepath + File.separatorChar + "bootstrap.ad");
		s_logger.info("bootstrapTemplate:    " + s_bootstrapTemplate);
		s_logger.info("  System.getProperty: " +   System.getProperty(s_prefix + ".bootstrapTemplate"));
		s_logger.info("  Default:            " +   Haystack.class.getResource("/bootstrap/bootstrap.ad").toString());
		s_logger.info("catalinaHome:         " + s_catalinaHome);
		s_logger.info("  System.getProperty: " +   System.getProperty("catalina.home"));
		s_logger.info("  Default:            " +   "");
		s_logger.info("hostname:             " + s_hostname);
		s_logger.info("  System.getProperty: " +  System.getProperty(s_prefix + ".hostname"));
		s_logger.info("  Default:            " +  defaultHostname());
		s_logger.info("initialPerspective:   " + s_initialPerspective);
		s_logger.info("  System.getProperty: " +  System.getProperty(s_prefix + ".initialPerspective"));
		s_logger.info("  Default:            " +  "");
		s_logger.info("isMacOSX:             " + s_isMacOSX);
		s_logger.info("  System.getProperty: " +  System.getProperty("os.name"));
		s_logger.info("  Default:            " +  "false");
		s_logger.info("PluginResources:      " + s_PluginResources);
		s_logger.info("  System.getProperty: " +   "(not used)");
		s_logger.info("  Default:            " +   "(none)");
		s_logger.info("packageSet:           " + s_packageSet);
		s_logger.info("  System.getProperty: " +   System.getProperty(s_prefix + ".packageSet"));
		s_logger.info("  Default:            " +   "http://haystack.lcs.mit.edu/bootstrap/eclipse");
		s_logger.info("precompile:           " + s_precompile);
		s_logger.info("  System.getProperty: " +   System.getProperty(s_prefix + ".precompile"));
		s_logger.info("  Default:            " +   ".");
		s_logger.info("purgeRDF:             " + s_purgeRDF);
		s_logger.info("  System.getProperty: " +   System.getProperty(s_prefix + ".purgeRDF"));
		s_logger.info("  Default:            " +   "false");
		s_logger.info("********** SystemProperties **********");
	}
	
}
