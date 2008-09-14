/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm;

import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPointCollection;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;

/**
 * @author yks
 */
public interface IClusterAlgorithm {
    /* runs the algorithm */
    public void run();
    public void setPointCollection(IPointCollection pointCollection);
    public void setProgressMonitor(IProgressMonitor ipm);
    public Vector/*ICluster*/ getClusters();
}
