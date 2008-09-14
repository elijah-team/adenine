/*
 * Created on Nov 22, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import edu.mit.lcs.haystack.server.extensions.infoextraction.Configuration;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Utilities;
import edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.HierarchicalClusterer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.IClusterAlgorithm;
import edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.KMeans;
import edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.ThresholdClusterer;

/**
 * @author yks
 */
public class AlgorithmFactory {
    int algorithmType; /* current algorithm type */

    /* algorithms */
    public final static int KMEANS = 0;

    public final static int THRESHOLD = 1;

    public final static int HIERARCHICAL = 2;
    
    public final static int DEFAULT_ALGO = 0;
    
    public final static int MAX_ALGO = 3;

    private String[] algos = { "K-Mean", "Threshold", "Hierarchical"};

    static private HashMap algosHash = new HashMap(); 
    static private HashMap algosHashRev;

    static {
        algosHash.put("K-Means", new Integer(AlgorithmFactory.KMEANS));
        algosHash.put("Threshold", new Integer(AlgorithmFactory.THRESHOLD));
        algosHash.put("Hierarchical", new Integer(AlgorithmFactory.HIERARCHICAL));
        algosHashRev = Utilities.reverseHashMap(algosHash);
    }
    /* public KMeansParam */
    protected Text numMixtures;

    protected Text numIterations;

    /* for threshold setting */
    protected Text threshold;
    
    protected Text hacThresh;

    TabFolder tabfolder;
    
    public int getAlgorithmType() {
        return tabfolder.getSelectionIndex();
    }
    
    public int getAlgorithmType(String name) {
        Integer val = (Integer)algosHash.get(name);
        if (val != null) {
            return val.intValue();
        } else {
            return DEFAULT_ALGO;
        }
    }
    
    public String getAlgorithmName(int type) {
        String val = (String)algosHashRev.get(new Integer( type ));
        if (val == null) {
            return (String)algosHashRev.get(new Integer(AlgorithmFactory.DEFAULT_ALGO));
        } else {
            return val;
        }
    }

    public boolean isValidAlgorithm(int algoType) {
        return (algoType >= 0 && algoType < MAX_ALGO);
    }

    public List makeAlgorithmList(Composite parent) {
        List algorithmList = new List(parent, SWT.NONE | SWT.SINGLE);
        algorithmList.setItems(algos);
        return algorithmList;
    }

    private int text2Int(Text box, int defaultVal) {
        String val = box.getText();
        int intVal = defaultVal;
        if (val != null) {
            intVal = Integer.parseInt(val);
        }
        return intVal;
    }

    private double text2Double(Text box, double defaultVal) {
        String val = box.getText();
        double doubleVal = defaultVal;
        if (val != null) {
            doubleVal = Double.parseDouble(val);
        }
        return doubleVal;
    }

    public int str2Int( String str, int def) {
        if (str != null) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    public double str2Double( String str, double def) {
        if (str != null) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }

    final static String NUM_MIXTURES = "num_mixtures";
    final static String NUM_ITERATIONS = "num_iterations";
    final static String THRESHOLD_VAL = "threshold";
    final static String ALGORITHM = "algorithm";
    
    public IClusterAlgorithm makeAlgorithm(Configuration config, String section) {
        IClusterAlgorithm ica = null;
        String algoName = config.get(section, ALGORITHM);
        int algoType = this.getAlgorithmType( algoName );
        
        switch (algoType) {
        case KMEANS:
            KMeans kmeans = new KMeans();
            kmeans.setK(str2Int( (String)config.get(section, NUM_MIXTURES) , KMeans.DEFAULT_K));
            kmeans.setIterations(str2Int( (String)config.get(section, NUM_ITERATIONS) , KMeans.DEFAULT_ITERATIONS));
            System.err.println(kmeans.toString());
            ica = kmeans;
            break;
        case THRESHOLD:
            ThresholdClusterer thresh = new ThresholdClusterer();
            thresh.setThreshold(str2Double( (String)config.get(section, THRESHOLD_VAL) , ThresholdClusterer.DEFAULT_THRESHOLD));
            ica = thresh;
            break;
        case HIERARCHICAL:
            HierarchicalClusterer hac = new HierarchicalClusterer();
            hac.setThreshold(str2Double( (String)config.get(section, THRESHOLD_VAL), HierarchicalClusterer.DEFAULT_THRESHOLD));
            ica = hac;
            break;
        }
        
        System.err.println("makeAlgorithm(): "+ ica.toString());
        return ica;    
    }
    
    public IClusterAlgorithm makeAlgorithm() {
        IClusterAlgorithm ica = null;
        switch (this.getAlgorithmType()) {
        case KMEANS:
            KMeans kmeans = new KMeans();
            kmeans.setK(text2Int(numMixtures, KMeans.DEFAULT_K));
            kmeans.setIterations(text2Int(numIterations, KMeans.DEFAULT_ITERATIONS));
            ica = kmeans;
            break;
        case THRESHOLD:
            ThresholdClusterer thresh = new ThresholdClusterer();
            thresh.setThreshold(text2Double(threshold, ThresholdClusterer.DEFAULT_THRESHOLD));
            ica = thresh;
            break;
        case HIERARCHICAL:
            HierarchicalClusterer hac = new HierarchicalClusterer();
            hac.setThreshold(text2Double(hacThresh, HierarchicalClusterer.DEFAULT_THRESHOLD));
            ica = hac;
            break;
        }

        System.err.println("makeAlgorithm(): "+ getAlgorithmName(this.getAlgorithmType()));
        return ica;
    }

    public Group makeGroup(Composite parent, int style, String text) {
        Group group = new Group(parent, style);
        if (text != null) {
            group.setText(text);
        }
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        return group;
    }

    public Group makeKMeansControl(Composite parent) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Group gp = makeGroup(parent, SWT.NONE, null);
        gp.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        gp.setLayout(gl);
        
        Label l1 = new Label(gp, SWT.NONE);
        l1.setText("K:");
        l1.setLayoutData(new GridData());

        numMixtures = new Text(gp, SWT.BORDER);
        numMixtures.setText(Integer.toString(KMeans.DEFAULT_K));
        numMixtures.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label l2 = new Label(gp, SWT.NONE);
        l2.setText("Iterations:");
        l2.setLayoutData(new GridData());

        numIterations = new Text(gp, SWT.BORDER);
        numIterations.setText(Integer.toString(KMeans.DEFAULT_ITERATIONS));
        numIterations.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return gp;
    }

    public Group makeThresholdControl(Composite parent) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Group gp = makeGroup(parent, SWT.NONE, null);
        gp.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        gp.setLayout(gl);
        
        Label l2 = new Label(gp, SWT.NONE);
        l2.setText("Threshold:");
        l2.setLayoutData(new GridData());

        threshold = new Text(gp, SWT.BORDER);
        threshold.setText(Double.toString(ThresholdClusterer.DEFAULT_THRESHOLD));
        threshold.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return gp;
    }

    public Group makeHierarchicalControl(Composite parent) {
        parent.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Group gp = makeGroup(parent, SWT.NONE, null);
        gp.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        gp.setLayout(gl);
        
        Label l2 = new Label(gp, SWT.NONE);
        l2.setText("Threshold:");
        l2.setLayoutData(new GridData());

        hacThresh = new Text(gp, SWT.BORDER);
        hacThresh.setText(Double.toString(HierarchicalClusterer.DEFAULT_THRESHOLD));
        hacThresh.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return gp;
    }

    public Composite makeControls(Composite parent) {
        /*
        Group gp = makeGroup(parent, SWT.NONE, "Parameters");
        gp.setLayoutData(new GridData(GridData.FILL_BOTH));
        */
        tabfolder = new TabFolder(parent, SWT.BORDER);
        tabfolder.setLayoutData(new GridData(GridData.FILL_BOTH));
       
        {/* KMEANS */
            TabItem kmeansTab = new TabItem(tabfolder, SWT.BORDER);
            kmeansTab.setText("KMeans");
            kmeansTab.setControl(makeKMeansControl(tabfolder));
        }
        
        {/* THRESHOLD */
            TabItem thresholdTab = new TabItem(tabfolder, SWT.BORDER);
            thresholdTab.setText("Threshold");
            thresholdTab.setControl(makeThresholdControl(tabfolder));
        }

        {/* HIERARCHICAL */
            TabItem hacTab = new TabItem(tabfolder, SWT.BORDER);
            hacTab.setText("Hierarchical");
            hacTab.setControl(makeHierarchicalControl(tabfolder));
        }

        return tabfolder;
    }
}