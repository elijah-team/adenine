package edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree;

import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.AbstractFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 *
 * Interface for adding additional attributions
 * to a given node.
 */
public interface IAugmentedNode extends INode {

    /**
     * feature representations of the node
     * @param feature
     */
    public void addFeature(AbstractFeature feature);
    public AbstractFeature[] getFeatures();
    public AbstractFeature[] getFeatures(int n);
    public IFeatureSet getFeatureSet();
    
    /**
     * Name of the node
     * @return
     */
    public String nodeName();
    
    /**
     * number of children for this node
     * @return
     */
    public int numChildren();
    
    /**
     * number of descendants this node
     * @return
     */
    public int numDescendants();
    
    /**
     * size of the underlying text
     * @return
     */
    public int textSize();

    /**
     * size of the underlying content
     * @return
     */
    public int contentSize();

    /**
     * String printing with indentation
     */
    public String toString(int level, String indent);

    public String toString();
    
    /**
     * get the cluster associated with the current node
     * @return ICluster
     */
    public ICluster getCluster();
    
    /**
     * sets the cluster associated with the current node
     * @param cluster
     */
    public void setCluster(ICluster cluster);
}
