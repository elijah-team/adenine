/*
 * Created on Jan 30, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

/**
 * @author yks
 */
public interface ILimited {
    public int limit ();
    public void setLimit(int limit);
    public int size();
    public boolean isFull();
}
