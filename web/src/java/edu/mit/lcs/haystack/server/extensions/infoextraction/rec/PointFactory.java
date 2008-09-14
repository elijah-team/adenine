/*
 * Created on Nov 24, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.cluster.ICluster;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.BagOfTags;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.DefaultPointCollection;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IPointCollection;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.IRelatablePoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.PCCTPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.PCPPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.TreeAlignmentPoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.TreeDistancePoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.data.TreeSizePoint;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;

/**
 * @author yks
 */
public class PointFactory {

    final public static int BAG_O_TAGS = 0;

    final public static int PCP = 1;

    final public static int PCCT = 2;

    final public static int SIZE = 3;

    final public static int TREE_DIST = 4;

    final public static int TREE_ALIGN = 5;

    final public static int MAX_POINT_TYPE = 6;

    final public static int DEFAULT_POINT_TYPE = BAG_O_TAGS;

    int pointType = DEFAULT_POINT_TYPE;

    static private HashMap displayPointHash = new HashMap();
    static private HashMap pointHash = new HashMap();

    static private HashMap pointHashRev;

    static {
        displayPointHash.put("BagOfTags (BOT)", new Integer(PointFactory.BAG_O_TAGS));
        displayPointHash.put("Parent-Child-Pairs (PCP)", new Integer(PointFactory.PCP));
        displayPointHash.put("Parent-Child-Child-Triplets (PCCT)", new Integer(PointFactory.PCCT));
        displayPointHash.put("TreeSize (SIZE)", new Integer(PointFactory.SIZE));
        displayPointHash.put("TreeDist [Edit distance]", new Integer(PointFactory.TREE_DIST));
        displayPointHash.put("TreeAlign [Alignment Score]", new Integer(PointFactory.TREE_ALIGN));

        pointHash.put("BagOfTags", new Integer(PointFactory.BAG_O_TAGS));
        pointHash.put("PCP", new Integer(PointFactory.PCP));
        pointHash.put("PCFG", new Integer(PointFactory.PCP));
        pointHash.put("PCCT", new Integer(PointFactory.PCCT));
        pointHash.put("Size", new Integer(PointFactory.SIZE));
        pointHash.put("TreeDist", new Integer(PointFactory.TREE_DIST));
        pointHash.put("TreeAlign", new Integer(PointFactory.TREE_ALIGN));

        pointHashRev = Utilities.reverseHashMap(pointHash);
    }

    static public String[] getPointNames() {
        return Utilities.SetToStringArray(pointHash.keySet());
    }

    static public String[] getDisplayPointNames() {
        return Utilities.SetToStringArray(displayPointHash.keySet());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(PointFactory.getPointName(this.pointType));
        return buf.toString();
    }

    static public String getPointName(int pointType) {
        String name = (String) pointHashRev.get(new Integer(pointType));
        if (name != null) {
            return name;
        } else {
            return (String) pointHashRev.get(new Integer(DEFAULT_POINT_TYPE));
        }
    }

    static public int getPointType(String pointType) {
        Integer val = (Integer) pointHash.get(pointType);
        if (val != null) {
            return val.intValue();
        } else {
            return PointFactory.DEFAULT_POINT_TYPE;
        }
    }

    public PointFactory(String type) {
        int pt = getPointType(type);
        if (pt < 0 || pt >= MAX_POINT_TYPE) {
            pt = DEFAULT_POINT_TYPE;
        }
        System.err.println("PointFactory(): " + getPointName(pt));
        this.pointType = pt;
    }

    public IRelatablePoint makePoint(Object arg) {
        IRelatablePoint p = null;
        switch (pointType) {
        case PCP:
            p = new PCPPoint(arg);
            break;
        case PCCT:
            p = new PCCTPoint(arg);
            break;
        case SIZE:
            p = new TreeSizePoint(arg);
            break;
        case TREE_DIST:
            p = new TreeDistancePoint(arg);
            break;
        case TREE_ALIGN:
            p = new TreeAlignmentPoint(arg);
            break;
        case BAG_O_TAGS:
        default:
            p = new BagOfTags(arg);
        }
        return p;
    }

    public IPoint makeCentroid(ICluster c) {
        IPoint p = makePoint(null);
        return p.centroid(c);
    }

    public IPointCollection makePointCollection(IAugmentedNode arg) {
        IPointCollection ipc = new DefaultPointCollection(arg, this);
        return ipc;
    }

    static public Combo makePointTypeSelector(Composite parent) {
        Combo pointType = new Combo(parent, SWT.READ_ONLY | SWT.BORDER | SWT.SINGLE);
        pointType.setItems(getDisplayPointNames());
        return pointType;
    }
}