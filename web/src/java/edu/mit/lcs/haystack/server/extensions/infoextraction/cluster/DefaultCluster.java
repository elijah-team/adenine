/*
 * Created on Aug 26, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.cluster;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;

/**
 * @author yks
 */
public class DefaultCluster implements IRankableCluster {
    Vector members;

    boolean changed = true;

    static int curClusterID = 0;

    int clusterID;

    HashMap /* ICluster */ancestors = new HashMap();

    HashMap /* ICluster */descendents = new HashMap();

    double score;
    
    public DefaultCluster() {
        members = new Vector();
        this.clusterID = curClusterID++;
    }

    public int hashCode() {
        return this.clusterID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#iterator()
     */
    public Iterator iterator() {
        return members.iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#numMembers()
     */
    public int numMembers() {
        return members.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#addMember(java.lang.Object)
     */
    public Object addMember(Object node) {
        members.add(node);
        changed = true;
        return node;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#removeMember(java.lang.Object)
     */
    public Object removeMember(Object node) {
        members.remove(node);
        changed = true;
        return node;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#split()
     */
    public Vector split() {
        Vector vec = new Vector();
        vec.add(this);
        return vec;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#join(edu.mit.lcs.haystack.server.infoextraction.ICluster)
     */
    public void join(ICluster cluster) {
        Iterator it = cluster.iterator();
        while (it.hasNext()) {
            Object node = it.next();
            IPoint ian = (IPoint) node;
            ian.setAssociation(this);
            this.addMember(node);
        }
    }

    public Vector getMembers() {
        return members;
    }

    public String toString() {
        return description();
    }

    public void addAncestor(ICluster cluster) {
        Integer count = (Integer) ancestors.get(cluster);
        if (count == null) {
            count = new Integer(1);
        } else {
            count = new Integer(count.intValue() + 1);
        }
        ancestors.put(cluster, count);
    }

    public void addDescendent(ICluster cluster) {
        Integer count = (Integer) descendents.get(cluster);
        if (count == null) {
            count = new Integer(1);
        } else {
            count = new Integer(count.intValue() + 1);
        }
        descendents.put(cluster, count);
    }

    public Vector/* ICluster */getDescendents() {
        return Utilities.HashMapKeysToVector(descendents);
    }

    public Vector/* ICluster */getAncestors() {
        return Utilities.HashMapKeysToVector(ancestors);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.ICluster#setProbability(double)
     */
    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return this.score;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.hac.ICluster#clearMembers()
     */
    public void clearMembers() {
        Iterator it = this.members.iterator();
        while (it.hasNext()) {
            IPoint ian = (IPoint) it.next();
            ian.setAssociation(this);
            this.addMember(ian);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.ICluster#description()
     */
    public String description() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        buf.append("#memb: " + this.numMembers() + "; ");
        buf.append("score: " + this.getScore() + "; ");

        Iterator it = this.iterator();
        for (int i = 0; i < 3 && i < this.numMembers(); i++) {
            Object mem = it.next();
            buf.append("memb[" + i + "]:");
            IAugmentedNode ian = (IAugmentedNode)((IPoint)mem).getData();
            buf.append(ian.toString());
            buf.append("; ");
        }
        buf.append("}");
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.extensions.infoextraction.rec.IRelation#relationshipString()
     */
    public String relationDescription() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.description() + "\n");

        //int ancestor
        int numAncestors = ancestors.size();
        int numDescendents = descendents.size();
        int total = numAncestors + numDescendents;

        buf.append("\tancestors [" + numAncestors + "] ");
        if (total > 0) {
            buf.append(Math.round(numAncestors * 100 / total) + "%" + ";\n");
        }

        Iterator ancestIt = ancestors.keySet().iterator();
        while (ancestIt.hasNext()) {
            ICluster ancestor = (ICluster) ancestIt.next();
            buf.append("\t\t" + ancestor.description() + ":" + ancestors.get(ancestor) + "\n");
        }

        buf.append("\tdescendants [" + numDescendents + "] ");
        if (total > 0) {
            buf.append(Math.round(numDescendents * 100 / total) + "%" + ";\n");
        }
        Iterator descIt = descendents.keySet().iterator();
        while (descIt.hasNext()) {
            ICluster descendent = (ICluster) descIt.next();
            buf.append("\t\t" + descendent.description() + ":" + descendents.get(descendent) + "\n");
        }

        return buf.toString();

    }
}