/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.cluster;

import java.util.Vector;


/**
 * @author yks
 */
public interface IRelatableCluster extends ICluster {
    public void addAncestor(ICluster ancestor);
    
    public void addDescendent(ICluster descendent);

    public Vector/*ICluster*/ getDescendents();
    
    public Vector/*ICluster*/ getAncestors();
    
    public String relationDescription();
}
