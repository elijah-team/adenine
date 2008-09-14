/*
 * Created on Nov 28, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class ClusterEvaluator {
    public static int pointSum(Vector clusters) {
        int sum = 0;
        for (int i = 0; i < clusters.size(); i++) {
            ICluster c = (ICluster)clusters.get(i);
            sum += c.numMembers();
        }
        return sum;
    }
    public static LinkedHashSet createNodeIDSet(ICluster cluster) {
        LinkedHashSet lhs = new LinkedHashSet();
        Vector/*IPoint*/ members = cluster.getMembers();
        
        Iterator it = members.iterator();
        while (it.hasNext()) {
            IPoint p = (IPoint)it.next();
            INode inode = (INode)p.getData();            
            lhs.add( inode.getNodeID().toString() );
        }
        
        return lhs;
    }

    public static double cohesion(ICluster cluster) {
        Vector/*IPoint*/members = cluster.getMembers();
        if (members.size() > 0) {
            IPoint p = (IPoint)members.firstElement();
            return 1.0 - p.internalDistance(cluster);
        }
        return 0.0;
    }
    
    public static Vector/*Double*/ clusterCohesion(Vector/*ICluster*/clusters) {
        Vector sims = new Vector();
        for (int i = 0; i < clusters.size(); i++) {
            ICluster ithCluster = (ICluster)clusters.get(i);
            Double d = new Double( cohesion( ithCluster ) );
            sims.add(i, d);
        }
        return sims;
    }
    
    public static int maxFScoreIndex(Vector/*FScore*/ scores) {
        double max = -1;
        Iterator it = scores.iterator();
        int i = 0;
        int maxScoreIndex = 0;
        while (it.hasNext()) {
            FScore cur = (FScore)it.next();
            double curScore = cur.getScore();
            if (curScore > max) {
                max = curScore;
                maxScoreIndex = i;
            }
            i++;
        }
        return maxScoreIndex;
    }
    
    public static FScore maxFScore(Vector /*FScore*/ scores) {
        return (FScore) scores.get( maxFScoreIndex( scores ) );
    }

    public static FScore fscore(Vector/*ICluster*/ clusters, Vector/*String*/ labelledIDs) {
        Vector vec = fscores(clusters, labelledIDs);
        return maxFScore(vec);
    }

    public static Vector/*FScore*/ fscores(Vector/*ICluster*/ clusters, Vector/*String*/ labelledIDs) {

        // a vector of FScores, one for each cluster.
        Vector clusterFScores = new Vector();
        
        Vector ids = labelledIDs;
        LinkedHashSet idSet = Utilities.VectorToHashSet( ids );
        
        Iterator it = clusters.iterator();
        while (it.hasNext()) {
            ICluster cluster = (ICluster)it.next();
            
            LinkedHashSet clusterSet = createNodeIDSet(cluster);
    
            int classSize = idSet.size();
            int clusterSize = clusterSet.size();
            
            clusterSet.retainAll( idSet );
            int intersect = clusterSet.size();
            
            FScore fs = new FScore(intersect, classSize, clusterSize);
            clusterFScores.add( fs );
        }
        
        return clusterFScores;
    }

}
