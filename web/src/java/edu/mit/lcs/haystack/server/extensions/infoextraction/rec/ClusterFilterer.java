/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;

/**
 * @author yks
 * container class for code that filters clusters based on *some* criteria
 */
public class ClusterFilterer {
    
    static boolean hasSameValue(Node A, Node B) {
        /*
         * compare the two nodes in terms of number of children
         * and value of the nodes
         */
        String aNodeName = A.getNodeName();
        String bNodeName = B.getNodeName();

        String aNodeValue = A.getNodeValue();
        String bNodeValue = B.getNodeValue();

        return ( aNodeName.equals(bNodeName) && ((aNodeValue == bNodeValue) || (aNodeValue.equals(bNodeValue))));
    }

    static public Vector/* ICluster*/ filterSingletonClusters(Vector/* ICluster */ clusters) {
        final String func = "filterSingletonCluster";
        Timer.printTimeElapsed(func + "() -- START");

        Vector newClusters = new Vector();
        
        for (int i = 0; i < clusters.size(); i++) {
            ICluster cluster = (ICluster) clusters.get(i);
            if (cluster.numMembers() > 1) {
                newClusters.add(cluster);
            }
        }
        Timer.printTimeElapsed(func + "() -- END");
        return newClusters;  
    }
    /**
     * Remove clusters whose members are identical wrt to other members
     * nodes has no children, and all their node values are the same.
     * @param cluster
     * @return
     */
    static public Vector/* ICluster */filterHomogeneousClusters(Vector/* ICluster */clusters) {
        final String func = "filterHomogeneousCluster";
        Timer.printTimeElapsed(func + "() -- START");

        Vector newClusters = new Vector();
        
        for (int i = 0; i < clusters.size(); i++) {
            ICluster cluster = (ICluster) clusters.get(i);
            
            /* skip empty clusters */
            Vector members = cluster.getMembers();
            if (members.size() <= 0) {
                continue;
            }
            
            IPoint firstP = (IPoint) (members.firstElement());
            
            /* this checks if the first point
             * is a singleton
             */
            Node node = (Node) firstP.getData();
            
            NodeList children = node.getChildNodes();
            
            /* checks if the node is a leaf*/
            if (children.getLength() != 0) {
                /*
                 * Heuristic
                 * we assume that if the first element is a
                 * leaf node, the rest of the cluster members
                 * are likely this same.
                 * This guess may breakdown in some instances
                 */
                newClusters.add(cluster);
                continue;
            }
            /* childless nodes; ASSERT (children.getLength == 0) */ 
            
            Iterator memIt = members.iterator();
            Node last = null;
            boolean singleton = true;
            
            while (memIt.hasNext()) {
                Node cur = (Node)((IPoint)memIt.next()).getData();
                
                int numChildren = cur.getChildNodes().getLength();
                if (numChildren > 0) {
                    singleton = false;
                    break;
                }
                
                if (last != null) {
                    if (! hasSameValue( cur, last ) ) {
                        singleton = false;
                        break;
                    }
                }
                last = cur;
            }

            /* don't add singletons to the filtered list */
            if (!singleton) {
                newClusters.add(cluster);
            }
        }
        Timer.printTimeElapsed(func + "() -- END");

        return newClusters;
    }

}