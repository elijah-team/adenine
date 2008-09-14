package edu.mit.lcs.haystack.eclipse;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;

import edu.mit.lcs.haystack.adenine.SWTConsole;

public class AdenineConsoleView extends ViewPart {
	private SWTConsole m_console;

	/**
	 * The constructor.
	 */
	public AdenineConsoleView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		m_console = new SWTConsole(parent, Plugin.getHaystack().getRootRDFContainer());
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		//viewer.getControl().setFocus();
		m_console.getControl().setFocus();
	}
}