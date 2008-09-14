package edu.mit.lcs.haystack.server.extensions.infoextraction.featureset;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.w3c.dom.Node;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.AugmentedTreeBuilder;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 * 
 * Container for FragmentSets keeps an abstraction layer for a store of
 * FragmentSets
 *  
 */
public class FeatureStore {

    final public static int numTypes = 5;

    final public static int TAGPATH = 0;

    final public static int PCC = 1;

    final public static int TAGPATH_PCC = 2;

    final public static int TRIGRAMS = 3;
    
    final public static int NULL = 4;

    final public static String[] TYPE_NAMES = { TagPathSet.NAME, ParentChildChildFragmentSet.NAME, TagPathWithPCCFSet.NAME, NGramsFragmentSet.NAME, NullFeatureSet.NAME };

    public static HashMap Name2Type;

    public static HashMap enableTypes;

    static {
        Name2Type = new HashMap();
        for (int i = 0; i < TYPE_NAMES.length; i++) {
            Name2Type.put(TYPE_NAMES[i], new Integer(i));
        }
        enableTypes = new HashMap();
        /* put enabled types here */
    }

    
    public static boolean isEnabled(int type) {
        return null != enableTypes.get( TYPE_NAMES[type] );
    }
    
    final public static int DEFAULT_TYPE = NULL;

    private int currentType = NULL;

    private HashMap/* <String(URL), FragmentSet> */stores[];
    
    private LinkedHashSet urls = new LinkedHashSet();
    
    final private static FeatureStore theStore = new FeatureStore();

    static public FeatureStore getFeatureStore() {
        return theStore;
    }

    static public IFeatureSet storeFragment(String url, INode dom) {

        Timer.printTimeElapsed("storeFragment -- START");
        System.err.println("storeFragment: root.getHeight(): " + dom.getHeight());
        IAugmentedNode cloneTree = AugmentedTreeBuilder.cloneTree(dom);
        System.err.println("storeFragment: cloneTree - DONE");
        IFeatureSet set = FeatureStore.getFeatureStore().add(url, cloneTree);
        Timer.printTimeElapsed("storeFragment -- END");

        return set;
    }

    public FeatureStore() {
        stores = new HashMap[numTypes];
        for (int type = 0; type < numTypes; type++) {
            stores[type] = new HashMap();
        }
    }

    public int getType() {
        return currentType;
    }

    public void setType(int type) {
        currentType = type;
    }

    public boolean hasEntry(String url) {
        if (stores[currentType].get(url) != null) {
            return true;
        } else {
            return false;
        }
    }

    public IFeatureSet add(String url, Node root) {

        IFeatureSet set;

        for (int type = 0; type < numTypes; type++) {
            if (FeatureStore.isEnabled(type)) {
                set = null;
                switch (type) {
                case TAGPATH:
                    set = new TagPathSet(root);
                    break;
                case PCC:
                    set = new ParentChildChildFragmentSet(root);
                    break;
                case TAGPATH_PCC:
                    set = new TagPathWithPCCFSet(root);
                    break;
                case TRIGRAMS:
                    set = new NGramsFragmentSet(root);
                    set.addFeatures();
                    //((NGramsFragmentSet)set).test();
                    break;
                }
                if (set != null) {
                    stores[type].put(url, set);
                }
            }
            if (!urls.contains(url)) {
                urls.add(url);
                set = new NullFeatureSet(root);
                stores[NULL].put(url, set);
            }
        }
        return (IFeatureSet) stores[currentType].get(url);
    }

    public void add(String url, IFeatureSet set) {
        stores[currentType].put(url, set);
    }

    public IFeatureSet get(String typeString, String url) {
        Integer typeInt = (Integer) Name2Type.get(typeString);
        int type = -1;
        if (typeInt != null) {
            type = typeInt.intValue();
        }
        if (type >= 0 && type < numTypes) {
            return (IFeatureSet) stores[type].get(url);
        } else {
            return get(url);
        }
    }

    public IFeatureSet get(int type, String url) {
        if (type >= 0 && type < numTypes) {
            return (IFeatureSet) stores[type].get(url);
        } else {
            return get(url);
        }
    }

    public IFeatureSet get(String url) {
        return (IFeatureSet) stores[currentType].get(url);
    }

    public String[] getUrls() {
        String[] list = null;
        int size = urls.size();
        if (size > 0) {
            list = new String[size];
            Iterator it = urls.iterator();
            int count = 0;
            while (it.hasNext() && count < size) {
                list[count++] = (String) it.next();
            }
        }
        return list;
    }
}