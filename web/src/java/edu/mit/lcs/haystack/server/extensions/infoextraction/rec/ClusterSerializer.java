/*
 * Created on Nov 27, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class ClusterSerializer {
    static String NUMCLUSTERS = "number of clusters";
    static String MAX_FSCORE = "max fscore";
    static String COHESION = "cohesion";
    static Vector /*String*/ order = new Vector(); 
    
    static {
        order.add(NUMCLUSTERS);
        order.add(MAX_FSCORE);
        order.add(COHESION);
    }
    public static String plottableResult(Vector /* IClusters */clusters, 
            Vector/* FScore */scores, Vector/*Double*/sims) {
        StringBuffer buf = new StringBuffer();
        Iterator it = clusters.iterator();
        LinkedHashMap results = new LinkedHashMap();
        results.put(NUMCLUSTERS, new Integer( clusters.size() ));
        int i = ClusterEvaluator.maxFScoreIndex(scores);
        FScore score = (FScore)scores.get(i);
        results.put(MAX_FSCORE, score.fullInfo());
        results.put(COHESION, (Double)sims.get(i));
        
        buf.append( Utilities.HashMapInOrder(results, order) );
        return buf.toString();
    }
    
    public static String prettyResult(Vector /* IClusters */clusters, 
            Vector/* FScore */scores, Vector/*Double*/sims) {
        StringBuffer buf = new StringBuffer();
        Iterator it = clusters.iterator();
        int i = 0;
        while (it.hasNext()) {
            ICluster c = (ICluster) it.next();
            FScore fs = null;
            Double d = null;
            if (scores != null) {
                 fs = (FScore)scores.get(i);
            }
            
            if (sims != null) {
                d = (Double)sims.get(i);
            }
            buf.append("C[" + i + "] = { ");
            if (fs != null) {
                buf.append(fs.fullInfo());
            }
            if (d != null) {
                buf.append(" cohesion: " + d.doubleValue());
            }
            
            buf.append("\n");
            Iterator pit = c.getMembers().iterator();
            while (pit.hasNext()) {
                IPoint p = (IPoint) pit.next();

                INode node = (INode) p.getData();

                buf.append(node.getNodeID().toString() + " - " + node.toString() + "\n");
            }
            buf.append("}\n");
            i++;
        }
        return buf.toString();
    }

    final static String CLUSTER_SEP = "=";
    final static String SEPARATOR = ", ";
    public static String serialize(Vector /* ICluster */clusters) {
        StringBuffer buf = new StringBuffer();

        Iterator it = clusters.iterator();
        int i = 0;
        while (it.hasNext()) {
            ICluster c = (ICluster) it.next();
            buf.append(i + CLUSTER_SEP );
            Iterator pit = c.getMembers().iterator();
            boolean first = true;
            while (pit.hasNext()) {
                IPoint p = (IPoint) pit.next();

                INode node = (INode) p.getData();

                if (first) {
                    first = false;
                } else {
                    buf.append(SEPARATOR);
                }

                buf.append(node.getNodeID().toString());
            }
            i++;
        }
        return buf.toString();
    }

    public static Vector/* ICluster */deserialize(File file) {
        // TODO: need to add in pointFactory.
        Vector vec = new Vector();
        return vec;
    }

    public static Vector/* ICluster */deserialize(String str) {
        Vector vec = new Vector();
        return vec;
    }
}