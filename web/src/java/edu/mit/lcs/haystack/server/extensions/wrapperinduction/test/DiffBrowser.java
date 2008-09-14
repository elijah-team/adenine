package edu.mit.lcs.haystack.server.extensions.wrapperinduction.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.lcs.haystack.ozone.web.IWebBrowserNavigateListener;
import edu.mit.lcs.haystack.ozone.web.InternetExplorer;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.IProgressMonitor;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Mapping;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.Pair;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.TreeDistance;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.WrapperManager;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.INode;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMDocument;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.IEDOMElement;
import edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie.OLEUtils;

/**
 * @version 1.0
 * @author Andrew Hogue
 */
public class DiffBrowser
		implements
			IWebBrowserNavigateListener,
			IProgressMonitor {

	protected static final String DEFAULT_URL_1 = "file:///c:/cygwin/home/ahogue/projects/haystack/htmltest/test1.html";
	protected static final String DEFAULT_URL_2 = "file:///c:/cygwin/home/ahogue/projects/haystack/htmltest/test2.html";
	protected static final String RDF_STORE_FILE = "ergo_store.rdf";
	protected WrapperManager ergo;
	protected Display display;
	protected Shell shell;
	protected Shell resultShell;
	protected InternetExplorer browserLeft;
	protected InternetExplorer browserRight;
	protected String urlLeft;
	protected String urlRight;
	protected Text locationTextLeft;
	protected Text locationTextRight;
	protected InternetExplorer browser;
	//protected Text saveText;
	protected ProgressBar distanceProgress;
	protected boolean cancelled;
	protected boolean done;
	protected Configuration configuration;

	public DiffBrowser(Display _display) {
		this(_display, null, null);
	}

	public DiffBrowser(Display _display, String _url1, String _url2) {

		this.display = _display;
		this.urlLeft = (_url1 == null) ? DEFAULT_URL_1 : _url1;
		this.urlRight = (_url2 == null) ? DEFAULT_URL_2 : _url2;
	}
	
	public Shell open() throws Exception {

		shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setMaximized(true);
		shell.setText("Diff Browser");
		try {
			GridLayout gl = new GridLayout();
			gl.numColumns = 5;
			shell.setLayout(gl);
			locationTextLeft = new Text(shell, SWT.SINGLE | SWT.BORDER);
			GridData location1GridData = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.FILL_HORIZONTAL);
			location1GridData.grabExcessHorizontalSpace = true;
			locationTextLeft.setLayoutData(location1GridData);
			locationTextLeft.setText(this.urlLeft);
			Button go1 = new Button(shell, SWT.PUSH);
			go1.setText("Go");
			go1.setLayoutData(new GridData());
			go1.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					if (locationTextLeft.getText() != null) {
						browserLeft.navigate(locationTextLeft.getText());
					}
				}
			});
			Button goboth = new Button(shell, SWT.PUSH);
			goboth.setText("Go Both");
			goboth.setLayoutData(new GridData());
			goboth.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					if (locationTextLeft.getText() != null) {
						browserLeft.navigate(locationTextLeft.getText());
						browserRight.navigate(locationTextLeft.getText());
					}
				}
			});
			locationTextRight = new Text(shell, SWT.SINGLE | SWT.BORDER);
			GridData location2GridData = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.FILL_HORIZONTAL);
			location2GridData.grabExcessHorizontalSpace = true;
			locationTextRight.setLayoutData(location2GridData);
			locationTextRight.setText(this.urlRight);
			Button go2 = new Button(shell, SWT.PUSH);
			go2.setText("Go");
			go2.setLayoutData(new GridData());
			go2.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					if (locationTextRight.getText() != null) {
						browserRight.navigate(locationTextRight.getText());
					}
				}
			});
			// browserLeft
			this.browserLeft = new InternetExplorer(shell);
			GridData browserLeftGridData = new GridData(
					GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL
							| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
			browserLeftGridData.horizontalSpan = 2;
			browserLeftGridData.verticalSpan = 9;
			browserLeft.getControl().setLayoutData(browserLeftGridData);
			browserLeft.addNavigateListener(this);
			
			Button diff = new Button(shell, SWT.PUSH);
			diff.setText("SubTree Diff");
			GridData diffGridData = new GridData();
			diff.setLayoutData(new GridData());
			diff.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					cancelled = false;
					long startTime = new java.util.Date().getTime();
					INode root = getCommonSubtree();
				
					IEDOMElement ieDom = (IEDOMElement) root;
					String diffhtml = ieDom.getOuterHTML();
				
					openResultShell(diffhtml);
					long endTime = new java.util.Date().getTime();
					System.out.println("Diff took " + (endTime - startTime)
							+ "ms\n");
				}
			});
			
			// browserRight
			this.browserRight = new InternetExplorer(shell);
			GridData browser2GridData = new GridData(
					GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL
							| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
			browser2GridData.horizontalSpan = 2;
			browser2GridData.verticalSpan = 9;
			browserRight.getControl().setLayoutData(browser2GridData);
			browserRight.addNavigateListener(this);
			// progress
			distanceProgress = new ProgressBar(shell, SWT.VERTICAL | SWT.SMOOTH);
			GridData progGridData = new GridData(
					GridData.VERTICAL_ALIGN_BEGINNING | GridData.GRAB_VERTICAL);
			distanceProgress.setLayoutData(progGridData);
			/* Create a combo box with test urls */
			Combo urls = new Combo(shell, SWT.READ_ONLY | SWT.SINGLE);
			/* populate it with data */
			configuration = new Configuration(
					"src/java/edu/mit/lcs/haystack/server/wrapperinduction/test/diffbrowser.cfg");
			configuration.readFromFile();
			Iterator it = configuration.getSectionKeys();
			String first = null;
			while (it.hasNext()) {
				String item = (String) it.next();
				urls.add(item);
				if (first == null) {
					first = item;
				}
			}
			GridData urlsGridData = new GridData(
					GridData.VERTICAL_ALIGN_BEGINNING);
			urls.setLayoutData(urlsGridData);
			urls.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					Combo w = (Combo) e.widget;
					int index = w.getSelectionIndex();
					System.err.println("selected: index: " + index);
					if (index >= 0) {
						String s = w.getItem(index);
						String left = configuration.get(s, "left");
						String right = configuration.get(s, "right");
						browserLeft.navigate(left);
						browserRight.navigate(right);
					}
				}
			});
			int index = urls.getSelectionIndex();
			if (index >= 0) {
				String s = urls.getItem(index);
				urlLeft = configuration.get(s, "left");
				urlRight = configuration.get(s, "right");
			} else if (first != null) {
				urlLeft = configuration.get(first, "left");
				urlRight = configuration.get(first, "right");
			}
			browserLeft.navigate(urlLeft);
			browserRight.navigate(urlRight);
			shell.pack();
			shell.open();
		} catch (Exception e) {
			System.out.println("Exception while opening DiffBrowser: " + e);
			e.printStackTrace();
		}
		return shell;
	}

	protected void openResultShell(String result) {
		resultShell = new Shell(display, SWT.SHELL_TRIM);
		resultShell.setMaximized(true);
		resultShell.setText("Diff");
		GridLayout gl = new GridLayout();
		gl.numColumns = 1;
		resultShell.setLayout(gl);
		Menu menu = new Menu(resultShell, SWT.BAR);
		resultShell.setMenuBar(menu);
		MenuItem save = new MenuItem(menu, SWT.CASCADE);
		save.setText("Save As..");
		save.addSelectionListener(new SelectionAdapter() {

			/* write the current diffed html page into a file */
			public void widgetSelected(SelectionEvent e) {

				FileDialog dialog = new FileDialog(resultShell, SWT.SAVE);
				dialog.setText("Save As...");
				String filename = dialog.open();
				try {
					File output = new File(filename);
					output.createNewFile();
					BufferedWriter out = new BufferedWriter(new FileWriter(
							output));
					out.write(((IEDOMElement) browser.getDocument().getDocumentElement()).getOuterHTML());
					out.flush();
					out.close();
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});
		resultShell.setMenuBar(menu);
		/* now the browser for rendering the diff page */
		browser = new InternetExplorer(resultShell);
		GridData browserGridData = new GridData(
				GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL
						| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		browserGridData.widthHint = 750;
		browserGridData.heightHint = 500;
		browserGridData.verticalSpan = 2;
		browser.getControl().setLayoutData(browserGridData);
		browser.navigate("about:blank");
		browser.getDocument().write(result);
		resultShell.pack();
		resultShell.open();
	}

	/**
	 * Traverses the tree keeps a count of the most frequent
	 * path sequence (of nodes)
	 * @param tree
	 * @return
	 */
	protected INode countPathFrequency(INode tree) {

		NodeList children = tree.getChildNodes();
		Vector clonedChildren = new Vector();
		INode clone;
		
		/* pair wise comparison of children? */
		for (int i = 0; i < children.getLength(); i++) {
			INode child = (INode) children.item(i);
			String nodeName = child.getNodeName();
			/* ignore style, script and text nodes */
			if (!nodeName.equalsIgnoreCase("STYLE")
					&& !nodeName.equalsIgnoreCase("SCRIPT")
					&& child.getNodeType() != INode.TEXT_NODE) {
				INode childClone = cloneTextFree(child);
				clonedChildren.add(childClone);
			}
		}
		clone = (INode) tree.cloneNode(false);
		for (int i = 0; i < clonedChildren.size(); i++) {
			INode childClone = (INode) clonedChildren.get(i);
			clone.appendChild(childClone);
			childClone.setParent(clone);
		}
		return clone;
	}

	
	
	/**
	 * recursively traverse the tree, clone it but remove Text nodes.
	 */
	protected INode cloneTextFree(INode tree) {

		NodeList children = tree.getChildNodes();
		Vector clonedChildren = new Vector();
		INode clone;
		for (int i = 0; i < children.getLength(); i++) {
			INode child = (INode) children.item(i);
			String nodeName = child.getNodeName();
			/* ignore style, script and text nodes */
			if (!nodeName.equalsIgnoreCase("STYLE")
					&& !nodeName.equalsIgnoreCase("SCRIPT")
					&& child.getNodeType() != INode.TEXT_NODE) {
				INode childClone = cloneTextFree(child);
				clonedChildren.add(childClone);
			}
		}
		clone = (INode) tree.cloneNode(false);
		for (int i = 0; i < clonedChildren.size(); i++) {
			INode childClone = (INode) clonedChildren.get(i);
			clone.appendChild(childClone);
			childClone.setParent(clone);
		}
		return clone;
	}

	/**
	 * recursively traverse the tree, creating a clone of that
	 * tree in the process
	 */
	protected INode cloneTree(INode tree) {

		NodeList children = tree.getChildNodes();
		INode clone;
		Vector clonedChildren = new Vector();
		for (int i = 0; i < children.getLength(); i++) {
			INode child = (INode) children.item(i);
			String nodeName = child.getNodeName();
			if (!nodeName.equalsIgnoreCase("STYLE")
					&& !nodeName.equalsIgnoreCase("SCRIPT")) {
				INode childClone = cloneTree(child);
				clonedChildren.add(childClone);
			}
		}
		clone = (INode) tree.cloneNode(false);
		for (int i = 0; i < clonedChildren.size(); i++) {
			INode childClone = (INode) clonedChildren.get(i);
			clone.appendChild(childClone);
			childClone.setParent(clone);
		}
		return clone;
	}

	/**
	 * checks the and prints out the corresponding open and close tags
	 */
	protected void inspectTree(INode tree) {

		NodeList children = tree.getChildNodes();
		INode clone;
		System.err.println("NODE: <" + tree.getNodeName() + "> height: "
				+ tree.getHeight() + " children: "
				+ tree.getChildNodes().getLength());
		Vector clonedChildren = new Vector();
		for (int i = 0; i < children.getLength(); i++) {
			INode child = (INode) children.item(i);
			inspectTree(child);
		}
		System.err.print("NODE: </" + tree.getNodeName() + "> ");
		for (int i = 0; i < children.getLength(); i++) {
			System.err.print("<" + children.item(i).getNodeName() + "/>");
		}
		System.err.println();
	}

	/**
	 * diffs two trees that is avoid of text
	 * 
	 * @return
	 */
	protected INode getCommonTextFreeSubTree() {

		INode root1 = cloneTextFree((INode) ((IEDOMElement) browserLeft.getDocument().getDocumentElement()));
		INode root2 = cloneTextFree((INode) ((IEDOMElement) browserRight.getDocument().getDocumentElement()));
		IEDOMDocument doc = ((IEDOMDocument) browserLeft.getDocument());
		Mapping mapping = new TreeDistance(root1, root2, .1, this).getMapping();
		Pair[] pairs = mapping.getPairs();
		for (int i = 0; i < pairs.length; i++) {
			Pair curPair = pairs[i];
			if (curPair.node1 != null && curPair.cost > 0) {
				if (((IEDOMElement) curPair.node1).getNodeType() == Node.TEXT_NODE) {
					IEDOMElement newEl = (IEDOMElement) doc.createElement("SPAN");
					String t1 = ((IEDOMElement) curPair.node1).getNodeText();
					String t2 = (curPair.node2 == null)
							? "null"
							: ((IEDOMElement) curPair.node2).getNodeText();
					newEl.setAttribute("nicetitle", t1 + "|" + t2);
					newEl.highlight("yellow", "black");
					IEDOMElement textEl = (IEDOMElement) doc.createTextNode(" Element "
							+ i + " ");
					newEl.appendChild(textEl);
					Node p = curPair.node1.getParentNode();
					p.replaceChild(newEl, curPair.node1);
				} else {
					curPair.node1.removeNode();
				}
			}
		}
		return root1;
	}

	/**
	 * Retrieves the common subtree between the two current browser pages.
	 */
	protected INode getCommonSubtree() {

		INode root1 = cloneTree((INode) ((IEDOMElement) browserLeft.getDocument().getDocumentElement()));
		INode root2 = cloneTree((INode) ((IEDOMElement) browserRight.getDocument().getDocumentElement()));
		IEDOMDocument doc = ((IEDOMDocument) browserLeft.getDocument());
		Mapping mapping = new TreeDistance(root1, root2, .1, this).getMapping();
		Pair[] pairs = mapping.getPairs();
		for (int i = 0; i < pairs.length; i++) {
			Pair curPair = pairs[i];
			if (curPair.node1 != null && curPair.cost > 0) {
				if (((IEDOMElement) curPair.node1).getNodeType() == Node.TEXT_NODE) {
					IEDOMElement newEl = (IEDOMElement) doc.createElement("SPAN");
					String t1 = ((IEDOMElement) curPair.node1).getNodeText();
					String t2 = (curPair.node2 == null)
							? "null"
							: ((IEDOMElement) curPair.node2).getNodeText();
					//System.err.println("LOOP: "+ i + " height: " +
					// root1.getHeight());
					newEl.setAttribute("nicetitle", t1 + "|" + t2);
					newEl.highlight("yellow", "black");
					IEDOMElement textEl = (IEDOMElement) doc.createTextNode(" Element "
							+ i + " ");
					newEl.appendChild(textEl);
					Node p = curPair.node1.getParentNode();
					p.replaceChild(newEl, curPair.node1);
					//((IEDOMElement) curPair.node1).replaceNode(newEl);
				} else {
					curPair.node1.removeNode();
				}
			}
			// this is a hack to get stylesheets and BASE elements to work...
			else if (curPair.node1 != null
					&& curPair.node1.getTagName().equalsIgnoreCase("HEAD")) {
				// first base element
				Element newBase = doc.createElement("BASE");
				newBase.setAttribute("href", doc.getURL());
				curPair.node1.appendChild(newBase);
				// next stylesheets
				OleAutomation stylesheets = doc.getStylesheets();
				int numStyles = OLEUtils.getProperty(stylesheets, "length").getInt();
				System.err.println("STYLESHEET: numStyles: " + numStyles);
				//System.err.println("LOOP: "+ i + " height: " +
				// root1.getHeight());
				/*
				 * for (int j = 0; j < numStyles; j++) { Element newStyle =
				 * doc.createElement("STYLE");
				 * //curPair.node1.appendChild(newBase); String cssText =
				 * IEDOMDocument.getCssText(OLEUtils.invokeCommand( stylesheets,
				 * "item", new Variant[]{new Variant(j)}));
				 * System.err.println("CSSTEXT: " + cssText); if (cssText !=
				 * null && !cssText.equals("")) { Comment css =
				 * doc.createComment(cssText); newStyle.appendChild(css); }
				 * System.err.println("LOOP: j="+ j + " height: " +
				 * root1.getHeight()); }
				 */
				// finally add nicetitle stylesheet and script
				Element niceStyle = doc.createElement("LINK");
				niceStyle.setAttribute("rel", "stylesheet");
				niceStyle.setAttribute(
						"href",
						"file://c:\\cygwin\\home\\ahogue\\projects\\haystack\\htmltest\\scripts\\nicetitle.css");
				curPair.node1.appendChild(niceStyle);
				System.err.println("LOOP: " + i + " height: "
						+ root1.getHeight());
				Element niceScript = doc.createElement("SCRIPT");
				//		niceScript.setAttribute("type", "text/javascript");
				niceScript.setAttribute(
						"src",
						"file://c:\\cygwin\\home\\ahogue\\projects\\haystack\\htmltest\\scripts\\nicetitle.js");
				curPair.node1.appendChild(niceScript);
				//System.err.println("LOOP: "+ i + " height: " +
				// root1.getHeight());
			} else if (curPair.node1 != null
					&& curPair.node1.getTagName().equalsIgnoreCase("LINK")) {
				String rel = curPair.node1.getAttribute("rel");
				if (rel != null && rel.equalsIgnoreCase("stylesheet")) {
					String href = curPair.node1.getAttribute("href");
					if (!href.startsWith("http")) {
						String newHref = href;
						if (href.startsWith("/")) {
							newHref = "http://" + doc.getDomain() + href;
						} else {
							newHref = doc.getBaseUrl() + href;
						}
						curPair.node1.setAttribute("href", newHref);
						System.out.println("Base url: " + doc.getBaseUrl()
								+ "\nOld href: " + href + "\nNew href: "
								+ newHref);
					}
				}
			}
		}
		return root1;
	}

	////// IWebBrowserNavigateListener interface //////
	public void beforeNavigate(String _url) {

	}

	public void navigateComplete() {

		locationTextLeft.setText(browserLeft.getLocationURL());
		locationTextRight.setText(browserRight.getLocationURL());
	}

	public void documentComplete(String url) {

	}

	public void statusTextChange(String status) {

	}

	public void progressChange(int progress, int progressMax) {

	}

	/**
	 * Closes this browser, writing the current store to RDF_STORE_FILE.
	 */
	public void close() {

		// 	boolean retVal = Utilities.generateRDF(ergo.getRDFContainer(), new
		// File(RDF_STORE_FILE));
		// 	if (!retVal) System.out.println("Warning: ErgoBrowser.close() failed
		// to write RDF to file");
	}

	////////////////////////////////
	/// IProgressMonitor methods ///
	////////////////////////////////
	public void beginTask(String name, int totalWork) {

		distanceProgress.setMaximum(totalWork);
	}

	public void done() {

		distanceProgress.setSelection(distanceProgress.getMaximum());
		this.done = true;
	}

	public boolean isCanceled() {

		return this.cancelled;
	}

	public void setCanceled(boolean value) {

		this.cancelled = value;
	}

	public void worked(int work) {

		distanceProgress.setSelection(work);
	}

	public static void main(String[] args) throws Exception {

		try {
			Display display = new Display();
			DiffBrowser ergoBrowser = new DiffBrowser(display);
			Shell ergoShell = ergoBrowser.open();
			while (!ergoShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
			ergoBrowser.close();
			display.dispose();
		} catch (Throwable e) {
			Thread.sleep(1000);
			e.printStackTrace();
		}
	}
}