package edu.mit.lcs.haystack.server.extensions.wrapperinduction.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.ZhangTreeEditDistance;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IETreeEditor;

/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class TreeEditBrowser {

    protected static final String RDF_STORE_FILE = "ergo_store.rdf";
    
    protected Display display;
    protected Shell shell;
    protected Shell comparisonDialog;
    
    protected IETreeEditor editor1;
    protected IETreeEditor editor2;
    protected Text ancestors;

    public TreeEditBrowser(Display _display) {
	this.display = _display;
    }
    
    public static void main(String [] args) throws Exception {
	try {
	    Display display = new Display();
	    TreeEditBrowser treeEditBrowser = new TreeEditBrowser(display);
	    Shell treeEditShell = treeEditBrowser.open();
	    
	    while (!treeEditShell.isDisposed()) {
		if (!display.readAndDispatch()) display.sleep();
	    }
	    treeEditBrowser.close();
	    display.dispose();
	}
	catch (Throwable e) {
	    System.out.println("Exception in main: " + e);
	    e.printStackTrace();
	}
    }

    public Shell open() throws Exception {
	shell = new Shell(display, SWT.SHELL_TRIM);
	shell.setMaximized(true);
	shell.setText("Tree Edit Browser");
	
	try {
	    GridLayout gl = new GridLayout();
	    gl.numColumns = 3;
	    shell.setLayout(gl);

	    Label controls = new Label(shell, SWT.LEFT);
	    controls.setText("Controls:");
	    GridData controlsGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING |
						     GridData.VERTICAL_ALIGN_BEGINNING);
	    controlsGridData.horizontalSpan = 2;
	    controls.setLayoutData(controlsGridData);
	    
	    editor1 = new IETreeEditor(shell, SWT.CENTER, "http://www.google.com/search?q=haystack");
	    GridData editor1GridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER |
						    GridData.GRAB_HORIZONTAL |
						    GridData.GRAB_VERTICAL | 
						    GridData.FILL_BOTH);
	    editor1GridData.widthHint = 700;
	    editor1GridData.heightHint = 250;
	    editor1GridData.verticalSpan = 2;
	    editor1.setLayoutData(editor1GridData);

	    Label ancestorsLabel = new Label(shell, SWT.LEFT);
	    ancestorsLabel.setText("Ancestors:");
	    ancestorsLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING |
						      GridData.VERTICAL_ALIGN_END));

	    ancestors = new Text(shell, SWT.RIGHT);
	    ancestors.setText("0");
	    ancestors.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END |
						 GridData.VERTICAL_ALIGN_END));
	    

	    Button compareButton = new Button(shell, SWT.LEFT);
	    GridData compareButtonGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING |
							  GridData.VERTICAL_ALIGN_BEGINNING);
	    compareButtonGridData.horizontalSpan = 2;
	    compareButton.setLayoutData(compareButtonGridData);
	    compareButton.setText("Compare");
	    compareButton.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent e) {
			ZhangTreeEditDistance td = getTreeDistance();
			System.out.println(td.tree1ToString() + "\n=========\n" + td.tree2ToString());

			System.out.println("Distance: " + td.getDistance());
			System.out.println(td.getMapping());
		    }
		});
	    


	    editor2 = new IETreeEditor(shell, SWT.CENTER, "http://www.google.com/search?q=feynman");
	    GridData editor2GridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER |
						    GridData.GRAB_HORIZONTAL |
						    GridData.GRAB_VERTICAL | 
						    GridData.FILL_BOTH);
	    editor2GridData.widthHint = 700;
	    editor2GridData.heightHint = 250;
	    editor2GridData.verticalSpan = 2;
	    editor2.setLayoutData(editor2GridData);

	    Button showComparisonButton = new Button(shell, SWT.LEFT);
	    GridData showComparisonButtonGridData =
		new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING |
			     GridData.VERTICAL_ALIGN_BEGINNING);
	    showComparisonButtonGridData.horizontalSpan = 2;
	    showComparisonButton.setLayoutData(showComparisonButtonGridData);
	    showComparisonButton.setText("Show Comparison");
	    showComparisonButton.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent e) {
			ZhangTreeEditDistance td = getTreeDistance();

			comparisonDialog = new Shell(display, SWT.DIALOG_TRIM);
			comparisonDialog.setText("Subtree Comparison");
			GridLayout gl = new GridLayout();
			gl.numColumns = 1;
			comparisonDialog.setLayout(gl);
	    
			InternetExplorer browser = new InternetExplorer(comparisonDialog);
			GridData browserGridData =
			    new GridData(GridData.HORIZONTAL_ALIGN_CENTER |
					 GridData.GRAB_HORIZONTAL |
					 GridData.GRAB_VERTICAL | 
					 GridData.FILL_BOTH);
			browserGridData.widthHint = 600;
			browserGridData.heightHint = 300;
			browser.getControl().setLayoutData(browserGridData);
			browser.navigate("about:blank");

			//			((IEDOMDocument)browser.getDocument()).setStylesheets(((IEDOMDocument)editor1.getBrowser().getDocument()).getStylesheets());
			//			browser.getDocument().write(((IEDOMElement)td.getCommonSubtree()).getOuterHTML());

			comparisonDialog.pack();
			comparisonDialog.open();	    
		    }
		});

	    shell.pack();
	    shell.open();
	}
	catch (Exception e) {
	    System.out.println("Exception while opening TreeEditBrowser: " + e);
	    e.printStackTrace();
	}

	return shell;
    }

    public void close() {
	if (this.editor1 != null) this.editor1.dispose();
	if (this.editor2 != null) this.editor2.dispose();
    }

    protected ZhangTreeEditDistance getTreeDistance() {
	INode n1 = editor1.getBrowser().getDocument().getActiveElement().getAncestor(Integer.parseInt(ancestors.getText()));
	INode n2 = editor2.getBrowser().getDocument().getActiveElement().getAncestor(Integer.parseInt(ancestors.getText()));
	
	return new ZhangTreeEditDistance(n1, n2);
    }
}


