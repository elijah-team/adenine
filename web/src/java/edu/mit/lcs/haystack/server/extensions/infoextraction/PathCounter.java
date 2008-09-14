/*
 * Created on Aug 19, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.w3c.dom.Node;

import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeID;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.NodeIDException;

/**
 * @author yks
 * a convenient data structure for counting the frequency of
 * node paths.
 */
public class PathCounter {
    Vector /*HashMap<Integer>*/ pathCounts;
    Vector /*Node*/ members;

    public PathCounter() {
        pathCounts = new Vector();
        members = new Vector();
    }
    
    public void debug() {
        for (int i = 0; i < pathCounts.size(); i++) {
           HashMap level = (HashMap)pathCounts.get(i);
           
           Object []arr = Utilities.HashMapToArray(level);
           
           Arrays.sort(arr, new Comparator() {
               public int compare(Object a, Object b) {
                   Map.Entry meA = (Map.Entry)a;
                   Map.Entry meB = (Map.Entry)b;
                   PathNode intA = (PathNode)meA.getValue();
                   PathNode intB = (PathNode)meB.getValue();
                   return intB.getCount() - intA.getCount();
               }
           });
           
           System.err.print(i + "\t");
           for (int j = 0; j < arr.length; j++) {
               Map.Entry me = (Map.Entry)arr[j];
               int val = ((PathNode)me.getValue()).getCount();
               System.err.print(me.getKey() + ": " + val +"\t");
           }
           System.err.println();
        }
    }
        
    /**
     * generates clusters from the lowest clustering points
     * @param stg
     */
//    public Vector/*SubTreeGroup*/ getLowestSplittingPoints(SubTreeCluster stg) {
//        int maxHeight = pathCounts.size();
//        int found = 0;
//        int numPaths = members.size();
//        boolean []seen = new boolean[numPaths];
//        Vector clusters = new Vector();
//        
//        for (int height = maxHeight-1; height >= 0 ; height--) {
//            
//            HashMap map = (HashMap)pathCounts.get(height);
//            
//            Iterator it = map.entrySet().iterator();
//            while (it.hasNext()) {
//                Map.Entry entry = (Map.Entry)it.next();
//                Integer siblingNo = (Integer)entry.getKey();
//                PathNode pathNode = (PathNode)entry.getValue();
//                
//                if (pathNode.getCount() >= 2) {
//                    SubTreeCluster cluster = null;
//                    
//                    Vector paths = pathNode.getMembers();
//                    for (int k = 0; k < paths.size(); k++) {
//                        Node m = (Node)members.get(k);
//                        
//                        int index = members.indexOf(m);
//                        if (index >= 0) {
//                            if (!seen[index]) {
//                                
//                                if (cluster == null) {
//                                    cluster = new SubTreeCluster(stg.getFeatureVector());
//                                }
//                                
//                                cluster.addMember( (Node)members.get(index) );
//                                seen[index] = true;
//                                found++;
//                            } 
//                        }
//                    }
//                    
//                    if (cluster != null) {
//                        clusters.add(cluster);
//                    }
//                }
//            }
//            
//            if (found == numPaths) {
//                break;
//            }
//        }
//        return clusters;	
//    }
//    
    public int getCount(int level, int siblingNo) {
        if (level >= 0 && level < pathCounts.size()) {
            HashMap map = (HashMap)pathCounts.get(level);
            Iterator levelIt = map.entrySet().iterator();
            while (levelIt.hasNext()) {
                Map.Entry entry = (Map.Entry)levelIt.next();
                PathNode count = (PathNode)entry.getValue();
                Integer key = (Integer)entry.getKey();
                if (key.intValue() == siblingNo) {
                    return count.getCount();
                }
            }            
        }
        return 0;
    }
    
    public int getNumBranches(int level) {
        if (level >= 0 && level < pathCounts.size()) {
            HashMap map = (HashMap)pathCounts.get(level);
            return map.size();
        }
        return 0;
    }
    
    public int getFirstBranch(int level) {
        if (level >= 0 && level < pathCounts.size()) {
            HashMap map = (HashMap)pathCounts.get(level);
            Iterator it = map.keySet().iterator();
            while (it.hasNext()) {
                return ((Integer)it.next()).intValue();
            }
        }
        return 0;     
    }
    
    public int[] getBestPath() {
        int size = pathCounts.size();
        int [] bestPath = new int[size];
        for (int i = 0; i < size; i++) {
            HashMap level = (HashMap)pathCounts.get(i);
            
            /* TODO keep the hashmap sorted but since number of
             * entries is low, sorting may not be worth it. */
            Iterator levelIt = level.entrySet().iterator();
            int max = -1;
            Integer maxKey = null;
            while (levelIt.hasNext()) {
                Map.Entry entry = (Map.Entry)levelIt.next();
                PathNode count = (PathNode)entry.getValue();
                Integer key = (Integer)entry.getKey();
                if (count.getCount() > max) {
                    max = count.getCount();
                    maxKey = key;
                }
            }
            
            if (maxKey != null) {
                bestPath[i] = maxKey.intValue();
            } else {
                System.err.println("Error: no max path found at level: "+i);
                bestPath[i] = 0;
            }
        }
        return bestPath;
    }
    
    public NodeID getBestNodeID() throws NodeIDException {
        return new NodeID(getBestPath());
    }

    public void addPath(Node node) {
        HashMap map;
        
        int siblingNos[] = ((INode)node).getNodeID().getSiblingNos();
        
        for (int i = 0; i < siblingNos.length; i++) {
            if (i < pathCounts.size()) {
                map = (HashMap)pathCounts.get(i);
            } else {
                map = new HashMap();
                pathCounts.add( map );
            }
            
            Integer key = new Integer(siblingNos[i]);
            
            PathNode count = (PathNode)map.get( key );
            if (count == null) {
                count = new PathNode();
            }
            
            count.add(node);
            map.put( key, count );
        }
        members.add(node);
    }
    
}

class PathNode {
    int count;
    Vector /*Node*/ members;
    
    public PathNode() {
        count = 0;
        members = new Vector();
    }
    
    public void add(Node member) {
        members.add(member);
        count++;
    }
    
    public int getCount() {
        return count;
    }
    
    public Vector getMembers() {
        return members;
    }
}