package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import edu.mit.lcs.haystack.ozone.web.IWebBrowserNavigateListener;
import edu.mit.lcs.haystack.ozone.web.InternetExplorer;

/**
 * @version 	1.0
 * @author	Andrew Hogue
 */
public class IETreeEditor extends Composite implements IWebBrowserNavigateListener {

    protected static final String DEFAULT_URL = "http://www.google.com/";

    protected InternetExplorer browser;
    protected String url;
    protected Text locationText;

    public IETreeEditor(Composite parent, int style, String _url) {
	super(parent, style);

	this.url = (_url == null) ? DEFAULT_URL : _url;

	GridLayout gl = new GridLayout();
	gl.numColumns = 3;
	this.setLayout(gl);

	Label location = new Label(this, SWT.CENTER);
	location.setText("Location:");
	location.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
	
	locationText = new Text(this, SWT.SINGLE | SWT.BORDER);
	GridData locationGridData = new GridData(GridData.GRAB_HORIZONTAL |
						 GridData.FILL_HORIZONTAL);	
	locationGridData.grabExcessHorizontalSpace = true;
	locationText.setLayoutData(locationGridData);
	
	Button go = new Button(this, SWT.PUSH);
	go.setText("Go");
	go.setLayoutData(new GridData());	
	go.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
		    if (locationText.getText() != null) {
			browser.navigate(locationText.getText());
		    }
		}
	    });

	// browser
	this.browser = new InternetExplorer(this);
	GridData browserGridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER |
						GridData.GRAB_HORIZONTAL |
						GridData.GRAB_VERTICAL | 
						GridData.FILL_BOTH);
	browserGridData.horizontalSpan = 3;
	browser.getControl().setLayoutData(browserGridData);
	browser.addNavigateListener(this);
	    
	browser.navigate(this.url);

	this.layout(true);
	this.pack();
    }

    public edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.IDOMBrowser getBrowser() {
	return this.browser;
    }

    public void navigate(String url) {
	this.browser.navigate(url);
    }

    public void beforeNavigate(String _url) {
	if (_url != null) {
	    locationText.setText(_url);
	}
    }

    public void navigateComplete() {
    }
	
    public void documentComplete(String url) {
    }

    public void statusTextChange(String status) {}
    public void progressChange(int progress, int progressMax) {}



}


	
