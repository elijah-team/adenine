/*
 * Created on Nov 18, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.cluster;


/**
 * @author yks
 */
public interface IRankableCluster extends IRelatableCluster {
    public void setScore(double score);
    public double getScore();
}
