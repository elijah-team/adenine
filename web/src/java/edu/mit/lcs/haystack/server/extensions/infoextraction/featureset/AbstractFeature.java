package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

/**
 * @author yks
 */
abstract public class AbstractFeature {
    public AbstractFeature() {}
    public AbstractFeature(String fragment) {}
    abstract public String toString();
    
    /**
     * increment this feature's frequency count 
     */
    abstract public void increment();
    abstract public int getFrequency();
    
    /**
     * getter and setters for generic info objects
     * attached to these feature instances.
     * @param info
     */
    abstract public void setInfo(Object info);
    abstract public Object getInfo(Object getInfo);
}
