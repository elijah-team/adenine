/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.HashCountSet;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.IRankableCluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IRelatablePoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;

/**
 * @author yks
 */
public class ClusterRanker {

    /**
     * recursively searches the children of a given node, and finds the ICluster
     * of the first nodes it meets while visiting children. i.e. it returns a
     * frontier of clusters.
     * 
     * @param pt
     * @return
     */
    static public HashMap/* ICluster */getClusterFrontier(IRelatablePoint pt) {
        HashMap/* ICluster */frontier = new HashMap();

        if (pt != null) {

            if (pt.hasAssociation()) {
                frontier.put(pt, pt.getAssociation());
                return frontier;

            } else {
                /* recursively find cluster */
                Vector/* IRelatablePoint */children = pt.getChildren();

                for (int i = 0; i < children.size(); i++) {
                    IRelatablePoint childe = (IRelatablePoint) children.get(i);
                    frontier.putAll(getClusterFrontier(childe));
                }
                return frontier;
            }

        }
        /* return empty vector */
        return frontier;
    }

    /**
     * Finds the entropy per node, and returns the average.
     */
    static private double averageClusterEntropy(ICluster myCluster) {

        Vector members = myCluster.getMembers();

        Iterator memberIt = members.iterator();
        Vector/* Double */sequence = new Vector();

        while (memberIt.hasNext()) {
            /* for each point calculate a entropic score */
            
            HashCountSet frontierCount = new HashCountSet();
            HashCountSet contentWt = new HashCountSet();

            IRelatablePoint pt = ((IRelatablePoint) memberIt.next());
            Vector children = pt.getChildren();

            IAugmentedNode ian = (IAugmentedNode)pt.getData();
            int contentSize = ian.contentSize();
            
//            System.err.println("root: \n" + ian.toString(0, "\t"));
            /*
             * 1. Construct a frontier set,
             * a set of mappings from IRelatablePoint (nodes) => ICluster (indicators of similarity)
             * - a frontier set is the set of clusters that are the immediate descendants of the
             * current node
             */
            for (int i = 0; i < children.size(); i++) {
                IRelatablePoint child = (IRelatablePoint) children.get(i);
                
                /* the frontier is a hashmap of <IRelatablePoint, ICluster> */
                HashMap frontierMap = getClusterFrontier(child);
                
                Iterator it = frontierMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry e = (Map.Entry)it.next();
                    
                    IRelatablePoint p = (IRelatablePoint)e.getKey();
                    ICluster c = (ICluster)e.getValue();

                    IAugmentedNode ia = (IAugmentedNode)p.getData();
                    
//                    System.err.println("child["+i+"]: " + ia.contentSize() + "\n" + ia.toString(0,"\t"));
                    contentWt.incrCount( c, ia.contentSize() );
                    frontierCount.add( c );
                }
            }

            /*
             * find the sum of all cluster (types) in the frontier
             * sum of all entries in frontierCount;
             */
            int csum = contentWt.sum();
            int fsum = frontierCount.sum();
//            System.err.println("contentSum: " + csum + "/" + contentSize);
            
            /* find the entropic score for each type of cluster */
            double diversity = 0;
            
            Iterator cit = frontierCount.iterator();
            while (cit.hasNext()) {
                ICluster curCluster = (ICluster) cit.next();
                double c = (double)frontierCount.getCount(curCluster);
                double p = c  / (double) fsum;
                //System.err.println("p: " + p + " (" + c + "/" + fsum + ")");
                diversity = p * Math.log(p);

                if (contentSize > 0) {
                    double w = contentWt.getCount(curCluster) / (double) csum;
                    diversity *= w;
                    //System.err.println("w: " + w);
                }
            }

//            System.err.println("D: " + diversity);
            sequence.add(new Double(-1 * diversity));
        }

        double entropySum = Utilities.sumDoubleValueVector(sequence);
        double average = entropySum / (double) sequence.size();
        return average;
    }

    //    /**
    //     * determines whether for a given cluster, if all descendents of a member
    //     * are members of a single cluster.
    //     *
    //     * if descendents of a member are members of different clusters then this
    //     * increases probability of being a record.
    //     *
    //     * record == having direct descendents that belong to different clusters
    //     * list == having direct descendents that belong to the same cluster
    //     *
    //     */
    //    private void recordProbabilityPerCluster(ICluster myCluster) {
    //
    //        Vector members = myCluster.getMembers();
    //
    //        Iterator memberIt = members.iterator();
    //
    //        HashSet/* ICluster */frontierClusters = new HashSet();
    //
    //        while (memberIt.hasNext()) {
    //
    //            IAugmentedNode ian = (IAugmentedNode) memberIt.next();
    //            NodeList children = ian.getChildNodes();
    //
    //            /*
    //             * search each children, stop when hitting a node with cluster.
    //             */
    //            for (int i = 0; i < children.getLength(); i++) {
    //                IAugmentedNode child = (IAugmentedNode) children.item(i);
    //                Vector resultClusters = getClusterFrontier(child);
    //                frontierClusters.addAll(resultClusters);
    //            }
    //        }
    //        
    //        /* put all nodes in the frontier clusters
    //         * into a single vector
    //         */
    //        Vector/* IAugmentedNode */frontierNodes = new Vector();
    //        Iterator clusterIt = frontierClusters.iterator();
    //        while (clusterIt.hasNext()) {
    //            ICluster curCluster = (ICluster) clusterIt.next();
    //            frontierNodes.addAll(curCluster.getMembers());
    //        }
    //
    //        double mutualCost = 0;
    //        if (frontierNodes.size() > 0) {
    //            mutualCost = averageAllPairsCost(frontierNodes, mappingNormalizedCost);
    //        }
    //
    //        myCluster.setScore(mutualCost);
    //    }
    /**
     * sort clusters by score
     * 
     * @param clusters
     * @return
     */
    static public Vector sortByClusterScore(Vector/* IRankable */clusters) {
        Timer.printTimeElapsed("sortByClusterScore() -- START");
        Collections.sort(clusters, new Comparator() {
            public int compare(Object a, Object b) {
                IRankableCluster aCluster = (IRankableCluster) a;
                IRankableCluster bCluster = (IRankableCluster) b;

                /*
                 * 1st criteria - higher the score implies more heterogeniety
                 */
                int val = (int) Math.round((bCluster.getScore() - aCluster.getScore()) * 100);
                if (val != 0)
                    return val;

                /* 1st criteria - large the size the better */
                return (bCluster.getMembers().size() - aCluster.getMembers().size());

            }
        });
        Timer.printTimeElapsed("sortByClusterScore() -- END");
        return clusters;
    }

    /**
     * for each cluster calculate its likelihood that its a record
     * 
     * @param clu
     * @return
     */
    static public Vector/* IRankable */scoreByClusterEntropy(Vector/* IRankable */clu) {
        final String func = "scoreByClusterEntropy";
        Timer.printTimeElapsed(func + "() -- START");

        for (int i = 0; i < clu.size(); i++) {
            IRankableCluster curCluster = (IRankableCluster) clu.get(i);
            int numAncestors = curCluster.getAncestors().size();
            int numDescendants = curCluster.getDescendents().size();
            double score = averageClusterEntropy(curCluster);
            curCluster.setScore(score);
        }

        Timer.printTimeElapsed(func + "() -- END");
        return clu;
    }

}