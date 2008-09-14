/*
 * Created on Nov 27, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.rec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Configuration;
import edu.mit.lcs.haystack.server.extensions.infoextraction.Timer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.algorithm.IClusterAlgorithm;
import edu.mit.lcs.haystack.server.extensions.infoextraction.labeller.LabelledDataCache;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.AugmentedTreeBuilder;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;

/**
 * @author yks
 */
public class RecordDetectorBatch {
    final static String LABELLED_DATADIR =  "labelled_data";
    protected PrintStream out;
    
    protected LinkedHashSet/* String */urls;

    protected Configuration params;

    protected LabelledDataCache labelledData;
    
    protected RecordDetector rd;

    protected PointFactory pointFactory;

    protected AlgorithmFactory algorithmFactory = new AlgorithmFactory();

    protected String outputFilename = "recorddetectorbatch_results.txt";
    
    public RecordDetectorBatch(String algoFile, String urlFile) {
        this.params = readParams(algoFile);
        //this.urls = readUrls(urlFile);
        this.labelledData = new LabelledDataCache(LABELLED_DATADIR);
        this.urls = this.labelledData.getURLs();
        
        try {
            this.out = new PrintStream(new FileOutputStream(outputFilename));
        } catch (Exception e) {
            System.err.println("Can't write out to file: "+outputFilename);
            e.printStackTrace();
            this.out = System.out;
        }
        System.err.println("RecordDetectorBatch: urls: "+ urls.size());
    }

    public Configuration readParams(String paramsFile) {
        Configuration c = null;
        try {
            c = new Configuration(paramsFile);
            c.readFromFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    public LinkedHashMap readUrls(String urlFile) {
        LinkedHashMap urls = new LinkedHashMap();
        try {
            File file = new File(urlFile);
            FileInputStream fout = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fout));

            StringBuffer buf = new StringBuffer();
            while (br.ready()) {
                String line = br.readLine();
                String kv[] = line.split("=");
                if (kv != null && kv.length > 1) {
                    urls.put(kv[1], kv[0]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urls;
    }

    /**
     * fetch the page corresponding to the url, and generate the INode for it.
     * 
     * @param url
     * @return
     */
    static HashMap docCache = new HashMap();

    protected void clearCache() {
        Iterator it = docCache.keySet().iterator();
        while (it.hasNext()) {
            String k = (String)it.next();
            docCache.remove(k);
        }
        docCache = new HashMap();
    }
    /* obtains the root of a document, returns it
     * if it is already cached.
     */
    protected IAugmentedNode getRoot(String url) {
        IAugmentedNode res = null;
        if (docCache.containsKey(url)) {
            Timer.printTimeElapsed("fetch doc - start");
            res = (IAugmentedNode)docCache.get(url);
            Timer.printTimeElapsed("fetch doc - end");
            return res;
        } else {
            Timer.printTimeElapsed("fetch doc - start");
            IDOMDocument doc;
            String page = this.labelledData.getPage(url);
            if (page != null) {
                doc = InternetExplorer.parseHTML(page);
            } else {
                doc = InternetExplorer.parseURL(url);
            }
            INode target = (INode)doc.getDocumentElement();
            Timer.printTimeElapsed("fetch doc - end");

            Timer.printTimeElapsed("copying tree - start");
            IAugmentedNode cloneTree = AugmentedTreeBuilder.cloneTree(target);
            Timer.printTimeElapsed("copying tree - end");
            
            docCache.put(url, cloneTree);
            return cloneTree;
        }
    }
    
    public final static String NUM_MIXTURES = "num_mixtures";
    public final static String NUM_ITERATIONS = "num_iterations";
    public final static String THRESHOLD_VAL = "threshold";

    public final static String ALGORITHM = "algorithm";
    public final static String POINT_TYPE = "point-type";
    
    public void runAllCases(String url) {
        Iterator algos = params.getSectionKeys();
        while (algos.hasNext()) {
            String key = (String)algos.next();
            HashMap config = params.getHash(key);
            String algoName = params.get(key, ALGORITHM);
            IClusterAlgorithm ica = algorithmFactory.makeAlgorithm( params, key );
            String typeName = params.get(key, POINT_TYPE);
            PointFactory pf = new PointFactory(typeName);
            runAlgorithm(ica, pf, url);
        }
    }

    public void runDataSet() {
        LinkedHashSet data = (LinkedHashSet)this.labelledData.getURLs();
        
        Iterator urlit = data.iterator();
        while (urlit.hasNext()) {
            String url = (String)urlit.next();
            System.err.println("Processing: " + url);
            runAllCases(url);
            this.clearCache(); /* this will help with garbage collection*/
        }
    }

    protected void runAlgorithm(IClusterAlgorithm algo, PointFactory pf, String url) {
        IAugmentedNode curRoot = getRoot(url);

        if (curRoot != null) {
            try {

                rd = new RecordDetector(curRoot);

                /* set algorithmic parameters */
                rd.setAlgorithm(algo);
                /* set the type of point to use */
                rd.setPointFactory(pf);
                rd.setOutput(out);
                System.err.println("started run");
                rd.run();
                System.err.println("ended run");
                Vector/*FScore*/ fscores = ClusterEvaluator.fscores(rd.getClusters(), this.labelledData.getEntries(url));
                Vector cohesion = ClusterEvaluator.clusterCohesion(rd.getClusters());
                out = System.err;
                out.println( "URL=" + url );
                out.println( "ALGORITHM=" + algo.toString() );
                out.println( "POINT-TYPE=" + pf.toString() );
                out.println( "NUM-POINTS=" + rd.numPoints() );
                out.println( "RESULTS=" + ClusterSerializer.plottableResult( rd.getClusters(), fscores, cohesion ) );
                //out.println( "R=" + ClusterSerializer.prettyResult(rd.getClusters(),fscores, cohesion ));
                out.println( "CLUSTER_POP=" + ClusterEvaluator.pointSum(rd.getClusters()));
                Vector labelledIds = this.labelledData.getEntries(url);
                //System.err.println("LABELLED: {" + Utilities.VectorToString(labelledIds) + "}");            

                out.println( "NUM_LABELLED=" + labelledIds.size());
                //out.println( "LABELLED=" + Utilities.VectorToString( this.labelledIds ));
                out.println();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
    }

    public static void main(String []args) {
        String root = "src/java/edu/mit/lcs/haystack/server/extensions/infoextraction/rec";
        String urlFile = root + "/urls.cfg";
        String algoFile = root + "/recorddetection.cfg";
        RecordDetectorBatch rdb = new RecordDetectorBatch(algoFile, urlFile);
        rdb.runDataSet();
        System.exit(0);
    }
}