/*
 * Created on Nov 8, 2003
 */
package edu.mit.lcs.haystack.eclipse;
import java.io.*;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.AdenineLineStyler;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.URIFinder;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.*;

/**
 * @author Dennis Quan
 */
public class AdenineConsoleTextEditor extends TextEditor {
	HashMap prefixes;
	Environment env;
	Interpreter in;
	StyledText t;
	DynamicEnvironment denv;
	AdenineLineStyler m_lineStyler = new AdenineLineStyler();
	PrintWriter m_internalWriter;
	BufferedReader m_internalReader;
	String m_lastCommand = "";
	//private static final String font = Constants.isWindows ? "Tahoma" : "Sans";
	static public final String s_prompt = "\nadenine> ";
	static public final String s_inputPrompt = "\ninput> ";
	String m_prompt = s_prompt;
	boolean m_inInput = false;
	String m_input;

	static void pumpMessages() {
		Display display = Display.getCurrent();
		while (display.readAndDispatch());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.ExtendedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		setup(Plugin.getHaystack().getRootRDFContainer());
	}
	
	public void setup(IRDFContainer rdfc) {
		in = new Interpreter(rdfc);
		env = in.createInitialEnvironment();
		denv = new DynamicEnvironment(rdfc, null);
		denv.setInstructionSource(rdfc);
		denv.setIdentity(Ozone.s_context.getUserIdentity());
		denv.setOutput(m_internalWriter = new PrintWriter(new Writer() {
			public void close() {
			}
			
			public void flush() {
			}
			
			public void write(char[] ach, int start, int cch) {
				t.append(new String(ach, start, cch));
				pumpMessages();
			}
		}));
		denv.setInput(m_internalReader = new BufferedReader(new InputStreamReader(System.in)) {
			public String readLine() {
				m_prompt = s_inputPrompt;
				t.append(m_prompt);
				t.setCaretOffset(t.getText().length());
				t.showSelection();
				m_inInput = true;
				
				while (m_inInput) {
					pumpMessages();
					Display.getCurrent().sleep();
				}
				
				m_inInput = false;
				m_prompt = s_prompt;
				return m_input;
			}
		});
		
		env.setValue("quit", new ICallable() { public Message invoke(Message message, DynamicEnvironment denv) { System.exit(0); return null; }});
		prefixes = new HashMap();
		prefixes.put("adenine", AdenineConstants.NAMESPACE);
		prefixes.put("rdf", Constants.s_rdf_namespace);
		prefixes.put("rdfs", Constants.s_rdfs_namespace);
		prefixes.put("daml", Constants.s_daml_namespace);
		prefixes.put("xsd", Constants.s_xsd_namespace);
		prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
		prefixes.put("hs", "http://haystack.lcs.mit.edu/schemata/haystack#");
		prefixes.put("config", "http://haystack.lcs.mit.edu/schemata/config#");
		prefixes.put("mail", "http://haystack.lcs.mit.edu/schemata/mail#"); 
		prefixes.put("content", "http://haystack.lcs.mit.edu/schemata/content#"); 
		prefixes.put("ozone", "http://haystack.lcs.mit.edu/schemata/ozone#");
		prefixes.put("slide", "http://haystack.lcs.mit.edu/schemata/ozoneslide#");
		prefixes.put("source", "http://haystack.lcs.mit.edu/agents/adenine#");
		prefixes.put("op", "http://haystack.lcs.mit.edu/schemata/operation#");
		prefixes.put("opui", "http://haystack.lcs.mit.edu/ui/operation#");
		
		t = getSourceViewer().getTextWidget();
		t.append(m_prompt);
		t.setCaretOffset(t.getText().length());
		Font font = new Font(t.getDisplay(), "Sans", 10, SWT.NORMAL);
		t.setFont(font);
		Handler h = new Handler();
		env.setValue("compile", new Compile());
		env.setValue("help", new Help());
		t.addKeyListener(h);

		t.addLineStyleListener(m_lineStyler);

		t.addDisposeListener(new DisposeListener() {  
			public void widgetDisposed (DisposeEvent event) {
				t.removeLineStyleListener(m_lineStyler);
				t.removeDisposeListener(this);
			}
		});
	}

	public void setServiceAccessor(IServiceAccessor sa) {
		denv.setServiceAccessor(sa);
	}

	public void setEnvironmentValue(String name, Object o) {
		env.setValue(name, o);
	}
	
	public void setDynamicEnvironmentValue(String name, Object o) {
		denv.setValue(name, o);
	}
	
	public void setEnvironment(Environment env) {
		this.env = env;
	}
	
	public void setDynamicEnvironment(DynamicEnvironment denv) {
		this.denv = denv;
	}

	public DynamicEnvironment getDynamicEnvironment() {
		return this.denv;
	}
	
	public Control getControl() {
		return t;
	}
	
	class Help implements ICallable {
		/**
		 * @see ICallable#invoke(Message, DynamicEnvironment)
		 */
		public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
			StringBuffer sb = new StringBuffer();
			java.lang.reflect.Method[] methods = message.m_values[0].getClass().getMethods();
			if (methods.length > 0) {
				sb.append("Methods:\n");
				for (int i = 0; i < methods.length; i++) {
					java.lang.reflect.Method m = methods[i];
					try {
						Object.class.getMethod(m.getName(), m.getParameterTypes());
					} catch (Exception e) {
						sb.append(m.getReturnType().getName() + " " + m.getName() + "(");
						Class[] params = m.getParameterTypes();
						for (int j = 0; j < params.length; j++) {
							sb.append(params[j].getName());
							if (j != (params.length - 1)) {
								sb.append(", ");
							}
						}
						sb.append(")\n");
					}
				}
			}
	
			java.lang.reflect.Field[] fields = message.m_values[0].getClass().getFields();
			if (fields.length > 0) {
				sb.append("Fields:\n");
				for (int i = 0; i < fields.length; i++) {
					java.lang.reflect.Field f = fields[i];
					sb.append(f.getType());
					sb.append(" ");
					sb.append(f.getName());
					sb.append("\n");
				}
				sb.append("\n");
			}
	        
			t.append(sb.toString());
	        
			return new Message();
		}
	}

	class Compile implements ICallable {
		/**
		 * @see ICallable#invoke(Message, DynamicEnvironment)
		 */
		public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
			try {
				in.compileMethodToJava((Resource)message.m_values[0], (String)message.m_values[1]);
				return new Message();
			} catch (Exception e) {
				e.printStackTrace();
				return new Message();
			}
		}
	}

	class Handler implements KeyListener {
		public void keyReleased(KeyEvent ke) {
		}
		public void keyPressed(KeyEvent ke) {
			if (ke.keyCode == SWT.F3) {
				t.append(m_lastCommand.trim());
				t.setCaretOffset(t.getText().length());
				t.showSelection();
			} else if (ke.keyCode == SWT.F2) {
				new URIFinder(denv.getInstructionSource(), t, new Rectangle(100, 100, 300, 300), t, t.getFont());
			} else if (ke.character == '\r') {
				int i = t.getCaretOffset();
				int matching = 0;
				char ch;
				String str = "";
				if (i >= 2) {
					do {
						--i;
						ch = t.getText(i - 1, i - 1).charAt(0);
						str = ch + str;
						if (ch == m_prompt.charAt(m_prompt.length() - matching - 1)) {
							++matching;
						} else {
							// Must start matching again
							if (matching > 1) {
								i += (matching - 1);
								str = str.substring(matching - 1);
							}
							matching = 0;
						}
					} while ((i > 1) && (matching < m_prompt.length()));
					if (matching == m_prompt.length()) {
						str = str.substring(matching);
					}
	
					if (str.trim().length() != 0) {
						if (m_inInput) {
							m_input = str;
							m_inInput = false;
							return;
						} else {
							PrintWriter oldWriter = denv.getOutput();
							BufferedReader oldReader = denv.getInput();
							try {
								denv.setOutput(m_internalWriter);
								denv.setInput(m_internalReader);
								m_lastCommand = str;
								Object o = in.eval(str, prefixes, env, denv);
								t.append("Result: ");
								t.append(o == null ? "null" : o.toString());
							} catch (AdenineException ae) {
								StringWriter sw = new StringWriter();
								ae.printStackTrace(new PrintWriter(sw));
								t.append(sw.getBuffer().toString());
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								denv.setOutput(oldWriter);
								denv.setInput(oldReader);
							}
						}
					}

					t.append(m_prompt);
					t.setCaretOffset(t.getText().length());
					t.showSelection();
				}
			}
		}
	}
	
	public static void showConsole(IWorkbenchPage page) throws Exception {
		IFile console = WorkspaceSynchronizationAgent.getDefault().getBaseFolder().getFile(new Path("Adenine Console.adc"));
		if (!console.exists()) {
			console.create(new ByteArrayInputStream(new byte[] {}), false, null);
		}
		IDE.openEditor(page, console, true);
	}
}
