/*
 * Created on Jan 29, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;

/**
 * @author yks
 */
public interface IDoubleMatrix {
    
    public boolean hasEntry(IPoint a, IPoint b);
    public double getEntry(IPoint a, IPoint b);
    public void putEntry(IPoint a, IPoint b, double o);
    public IDoubleMatrix copy();
    public void removeRow(IPoint row);
    public void removeCol(IPoint col);
    public void removeEntry(IPoint a, IPoint b);
    public int numRows();
    public int numCols();
}
