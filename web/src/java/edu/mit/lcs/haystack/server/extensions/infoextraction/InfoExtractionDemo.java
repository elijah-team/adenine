package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.server.extensions.infoextraction.domnav.DOMNavigatorView;
import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.FeatureStore;
import edu.mit.lcs.haystack.server.extensions.infoextraction.labeller.LabellerView;
import edu.mit.lcs.haystack.server.extensions.infoextraction.rec.RecordDetectionView;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMElement;

/**
 * @author yks
 * 
 * Demonstrates syntactic clustering of documents
 */
public class InfoExtractionDemo implements IDocumentProcessor {

    private Display display;

    private Shell shell;

    private SashForm sash;

    /*
     * configuration file for all the commonly used urls.
     */
    protected Configuration configuration;

    /**
     * sub-components/composite that make up this app.
     */

    private BrowserFrame browserFrame;

    private FeatureView featureView;

    private DOMNavigatorView domNavigatorView;

    private SimilarityView similarityView;

    private RecordDetectionView recordDetectionView;
    
    private LabellerView labellerView;

    public InfoExtractionDemo(Display display) {
        this(display, null);
    }

    public InfoExtractionDemo(Display display, String url) {
        this.display = display;
        
        configuration = new Configuration("src/java/edu/mit/lcs/haystack/server/extensions/infoextraction/demo.cfg");
        try {
            configuration.readFromFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Controls frame is a tab folder that contains tabItems for various types
     * of controls.
     * 
     * @param parent
     * @return
     */
    public Composite initControlsFrame(Composite parent) {
        TabFolder tabfolder = new TabFolder(parent, SWT.BORDER);

        TabItem similarityTab = new TabItem(tabfolder, SWT.BORDER);
        similarityTab.setText("Similarity");
        this.similarityView = new SimilarityView(display, tabfolder, SWT.NONE, browserFrame);
        similarityTab.setControl(this.similarityView);

        TabItem featureViewTab = new TabItem(tabfolder, SWT.BORDER);
        featureViewTab.setText("Feature View");
        this.featureView = new FeatureView(display, tabfolder, SWT.NONE, browserFrame);
        featureViewTab.setControl(this.featureView);

        TabItem domNavTab = new TabItem(tabfolder, SWT.BORDER);
        domNavTab.setText("DOM Navigation");
        this.domNavigatorView = new DOMNavigatorView(display, tabfolder, SWT.NONE, browserFrame);
        domNavTab.setControl(this.domNavigatorView);
        
        TabItem recordDetectionTab = new TabItem(tabfolder, SWT.BORDER);
        recordDetectionTab.setText("Record Detection");
        this.recordDetectionView = new RecordDetectionView(display, tabfolder, SWT.NONE, browserFrame);
        recordDetectionTab.setControl(this.recordDetectionView);

        TabItem labellerTab = new TabItem(tabfolder, SWT.BORDER);
        labellerTab.setText("Labeller");
        this.labellerView = new LabellerView(display, tabfolder, SWT.NONE, browserFrame);
        labellerTab.setControl(this.labellerView);

        /* set the default tab folder */
        tabfolder.setSelection(tabfolder.indexOf(recordDetectionTab));
        return tabfolder;
    }

    /**
     * Create a resizable frame (or Sash) which the left pane is the browser and
     * the right pane is a collection of controls layout in tabs.
     * 
     * @param parent
     * @return
     */
    public Composite initSash(Composite parent) {
        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL | SWT.NULL);
        GridData sashFormLD = new GridData(GridData.FILL_BOTH);
        sashForm.setLayoutData(sashFormLD);

        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        sashForm.setLayout(gl);

        /*
         * beware of the order these next lines here are called, there are
         * dependencies. e.g. addDocumentFocus depends on having
         * initControlsFrame been called already.
         */
        this.browserFrame = new BrowserFrame(display, sashForm, SWT.NONE);
        this.browserFrame.addDocumentProcessor(this);

        Composite controlsFrame = initControlsFrame(sashForm);
        GridData gd = new GridData();

        this.browserFrame.addDocumentFocus(domNavigatorView);
        this.browserFrame.addDocumentFocus(labellerView);

        /*
         * navigate to the first url in the configuration list
         */
        Iterator it = configuration.getSectionKeys();
        if (it.hasNext()) {
            String entry = (String) it.next();
            String url = configuration.get(entry, "url");
            this.browserFrame.navigateTo(url);
        }

        gd.widthHint = 50;
        controlsFrame.setLayoutData(gd);
        return sashForm;
    }

    public Shell open() throws Exception {
        Shell shell = new Shell(display, SWT.SHELL_TRIM);
        Menu shellMenu = generateMenu(shell);
        shell.setMenuBar(shellMenu);

        shell.setSize(600, 800);
        shell.setText("Info Extraction Demo");
        shell.setMaximized(true);

        GridLayout gl = new GridLayout();
        shell.setLayout(new GridLayout());

        Composite sash = initSash(shell);

        shell.pack();
        shell.open();
        return shell;
    }

    private InternetExplorer getBrowser() {
        return browserFrame.getBrowserWidget();
    }

    /**
     * encapsulated private method for generating menu items
     * 
     * @param shell
     * @return
     */
private Menu generateMenu(Shell shell) {
        Menu menu = new Menu(shell, SWT.BAR);

        MenuItem file = new MenuItem(menu, SWT.CASCADE);
        file.setText("File");
        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        file.setMenu(fileMenu);
        
        MenuItem saveAs = new MenuItem(fileMenu, SWT.PUSH);
        saveAs.setText("Save As");
        saveAs.addSelectionListener(new SelectionAdapter() {
            Shell shell;

            public SelectionAdapter init(Shell shell) {
                this.shell = shell;
                return this;
            }

            /* write the current diffed html page into a file */
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(shell, SWT.SAVE);
                dialog.setText("Save As...");
                String filename = dialog.open();
                try {
                    File output = new File(filename);
                    output.createNewFile();
                    BufferedWriter out = new BufferedWriter(new FileWriter(
                            output));
                    out.write(((IEDOMElement) getBrowser().getDocument()
                            .getDocumentElement()).getOuterHTML());
                    out.flush();
                    out.close();
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }.init(shell));

        MenuItem back = new MenuItem(menu, SWT.PUSH);
        back.setText("Back");
        back.addSelectionListener(new SelectionAdapter() {
            /* write the current diffed html page into a file */
            public void widgetSelected(SelectionEvent e) {
                browserFrame.back();                
            }
        });

        MenuItem refresh = new MenuItem(menu, SWT.PUSH);
        refresh.setText("Refresh");
        refresh.addSelectionListener(new SelectionAdapter() {
            /* write the current diffed html page into a file */
            public void widgetSelected(SelectionEvent e) {
                browserFrame.refresh();
            }
        });

        MenuItem forward = new MenuItem(menu, SWT.PUSH);
        forward.setText("Forward");
        forward.addSelectionListener(new SelectionAdapter() {
            /* write the current diffed html page into a file */
            public void widgetSelected(SelectionEvent e) {
                browserFrame.forward();
            }
        });

        MenuItem bookmarks = new MenuItem(menu, SWT.CASCADE);
        bookmarks.setText("Favorites");

        
        Menu favoriteCascade = new Menu(shell, SWT.DROP_DOWN);
        bookmarks.setMenu(favoriteCascade);

        Iterator it = configuration.getSectionKeys();
        String first = null;

        while (it.hasNext()) {
            String entry = (String) it.next();
            
            MenuItem item = new MenuItem(favoriteCascade, SWT.PUSH);
            item.setText(entry);
            
            item.addSelectionListener(new SelectionAdapter() {
                private String entryName;
                public SelectionAdapter init(String name) {
                    entryName = name;
                    return this;
                }
                public void widgetSelected(SelectionEvent e) {
                	String url = configuration.get(entryName, "url");
                	browserFrame.navigateTo(url);
                }
            }.init(entry));
        }

        MenuItem encode = new MenuItem(menu, SWT.CASCADE);
        encode.setText("Encode");
        encode.addSelectionListener(new SelectionAdapter() {            
                /* write the current diffed html page into a file */
                public void widgetSelected(SelectionEvent e) {
                    browserFrame.encodePage();
                }
        });

        return menu;
    }
    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.lcs.haystack.server.infoextraction.IDocumentProcessor#process(edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser)
     */
    public void process(IDOMBrowser browser, String url) {
        System.err.println("InfoExtractionDemo.process() -- START");
        FeatureStore.storeFragment(url, (INode) (browser.getDocument().getDocumentElement()));

        this.featureView.refreshURLs();
        this.similarityView.refreshURLs();
        this.recordDetectionView.refreshURLs();
        this.labellerView.refreshURLs();
        
        System.err.println("InfoExtractionDemo.process() -- END");
    }

    public static void main(String[] args) throws Exception {

        try {
            Display display = new Display();

            InfoExtractionDemo demo = new InfoExtractionDemo(display);
            Shell mainShell = demo.open();

            while (!mainShell.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
            display.dispose();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}