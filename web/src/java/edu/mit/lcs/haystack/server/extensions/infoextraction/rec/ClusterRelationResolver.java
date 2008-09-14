/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.IRelatableCluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IRelatablePoint;

/**
 * @author yks
 */
public class ClusterRelationResolver {

    /**
     * for each member in the cluster. traverse up its path and add any clusters
     * that it encounters as this current cluster's ancestor.
     * 
     * @param myCluster
     * @return
     */
    static public void resolveRelationship(IRelatableCluster myCluster) {
        
        Iterator it = myCluster.getMembers().iterator();
        while (it.hasNext()) {

            IRelatablePoint p = (IRelatablePoint)it.next();
            
            IRelatablePoint parent = p.getParent();

            while (parent != null) {
                IRelatableCluster parentCluster = (IRelatableCluster)parent.getAssociation();
                
                if (parentCluster != null) {
                    myCluster.addAncestor(parentCluster);
                    parentCluster.addDescendent(myCluster);
                    /* quit when at least one parent cluster is found */
                    break;
                }

                parent = parent.getParent();
                // continue until either reaching a
                // root or finding a node with a cluster association
            }
        }
    }

    /**
     * resolve cluster relationships for each set of clusters find out whether
     * above a certain threshold of nodes are parents of another cluster.
     * 
     * @return
     */
    static public Vector resolveClusterRelationships(Vector/* IRelatableCluster */clusters) {
        Timer.printTimeElapsed("resolveClusterRelationships() -- START");
        try {
            Iterator it = clusters.iterator();
            while (it.hasNext()) {
                resolveRelationship( (IRelatableCluster) it.next() );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Timer.printTimeElapsed("resolveClusterRelationships() -- END");
        return clusters;
    }
}
