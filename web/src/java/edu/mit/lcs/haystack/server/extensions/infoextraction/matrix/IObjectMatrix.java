/*
 * Created on Jan 29, 2005
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.matrix;

import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;

/**
 * @author yks
 */
public interface IObjectMatrix {

    public boolean hasEntry(IPoint a, IPoint b);
    public Object getEntry(IPoint a, IPoint b);
    public void putEntry(IPoint a, IPoint b, Object o);
    public IObjectMatrix copy();
    public void removeRow(IPoint row);
    public void removeCol(IPoint col);
    public void removeEntry(IPoint a, IPoint b);
    public int numRows();
    public int numCols();

}
