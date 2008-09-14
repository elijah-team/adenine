 
 /*
 * Created on Aug 17, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.FeatureStore;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.IFeatureSet;

/**
 * @author yks
 */
public class SimilarityView extends DefaultComposite {
    protected Combo feature = null;
    protected Text output = null;
    
    /**
     * compares similarity of two urls
     * @param parent
     * @return
     */
    public SimilarityView(Display display, Composite parent, int style, BrowserFrame browserFrame) {
        super(display, parent, style, browserFrame);
        this.setLayout(new GridLayout());
        
        /*
         * List of urls with fragstats
         */
        Group storeEntryGroup = new Group(this, SWT.NONE);
        storeEntryGroup.setText("URLs");
        storeEntryGroup.setLayout(new GridLayout());
        storeEntryGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        cachedUrls = new List(storeEntryGroup, SWT.MULTI | SWT.V_SCROLL
                | SWT.H_SCROLL | SWT.BORDER);
        cachedUrls
                .setToolTipText("Select two urls to perform similarity comparison");
        cachedUrls.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.FILL_BOTH));
        
        
        Group featureGroup = new Group(this, SWT.NONE);
        featureGroup.setText("Features");
        featureGroup.setLayout(new GridLayout());
        featureGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        feature = new Combo(featureGroup, SWT.SINGLE | SWT.BORDER
                | SWT.DROP_DOWN | SWT.READ_ONLY);
        feature.select(FeatureStore.DEFAULT_TYPE);
        feature.setItems(FeatureStore.TYPE_NAMES);
        feature.setLayoutData(new GridData());
        
        /*
         * similarity button
         */
        Button similarity = new Button(this, SWT.PUSH);
        similarity.setText("Compare Pages");
        GridData similarityGD = new GridData(GridData.END);
        
        similarity.setLayoutData(similarityGD);
        similarity.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                processSimilarity();
            }
        });

        /*
         * labels containing stats about the current page
         */
        Group outputGroup = new Group(this, SWT.NONE);
        outputGroup.setText("Output");
        outputGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        outputGroup.setLayout(new GridLayout());
        output = new Text(outputGroup, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER
                | SWT.MULTI | SWT.READ_ONLY);
        output.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
        output.setEditable(false);
        output.setText("");
        output.setSize(80, 100);

    }
    
    /**
     * finds the similarity of two different feature sets, and generates some
     * output representing the similarity values, and what differed.
     */
    private void processSimilarity() {
        String[] selections = cachedUrls.getSelection();
        int featureIndex = feature.getSelectionIndex();
        
        if (selections != null && selections.length >= 2) {

            FeatureStore featureStore = getFeatureStore();

            IFeatureSet source = featureStore.get(featureIndex, selections[0]);
            IFeatureSet target = featureStore.get(featureIndex, selections[1]);

            if (source != null && target != null) {
                String statString = selections[0];
                statString += "\nvs.\n";
                statString += selections[1];
                statString += "\n\n";
                
                statString += "using: "+ source.getFeatureName() + "\n";
                double res = source.similarity(target);

                statString += "similarity: " + res + "\n";
                res = source.weightedSimilarity(target);
                statString += "weighted similarity: " + res + "\n";

                Set diff = source.difference(target);
                Iterator it = diff.iterator();
                statString += "difference:\n";
                while (it.hasNext()) {
                    statString += ((String) it.next()) + "\n";
                }
                output.setText(statString);
            } else {
                // shouldn't get here unless a bug
                output.setText("Error: data missing, either source or target");
            }
        } else {
            output.setText("Please select two URLs.");    
        }
    }

}
