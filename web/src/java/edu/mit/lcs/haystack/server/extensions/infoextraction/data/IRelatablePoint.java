/*
 * Created on Nov 19, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.data;

import java.util.Vector;


/**
 * @author yks
 */
public interface IRelatablePoint extends IPoint {
    public IRelatablePoint getParent();
    public Vector/*IRelatablePoint*/ getChildren();
    public void setParent(IRelatablePoint p);
    public void addChild(IRelatablePoint p);
    public int getSize();
    public String toString(int depth, String delimiter);
}
