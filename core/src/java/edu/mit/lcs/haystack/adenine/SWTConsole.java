/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.mit.lcs.haystack.adenine;

import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.URIFinder;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.rdfCode.RDFCodeCompiler;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.Environment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

/**
 * Displays an SWT-based Adenine console.
 * @version 	1.0
 * @author		Dennis Quan
 */
public class SWTConsole {
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
	
	public SWTConsole(Composite parent, IRDFContainer rdfc) {
		in = new Interpreter(rdfc);
		env = in.createInitialEnvironment();
		denv = new DynamicEnvironment(rdfc, null);
		denv.setInstructionSource(rdfc);
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
		
	    t = new StyledText(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
	    t.append("Haystack Adenine Console\nVersion 1.0\nCopyright (c) Massachusetts Institute of Technology, 2001-2002.\n");
	    t.append(m_prompt);
		t.setCaretOffset(t.getText().length());
		Font font = new Font(parent.getDisplay(), "Fixed", 10, SWT.NORMAL);
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
				
				if((ke.stateMask & SWT.CTRL) != 0) {
					t.append("\n");
				} else {
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
					}

					t.setCaretOffset(t.getText().length());
					t.showSelection();
				}
			}
		}
	}

	public static SWTConsole showConsole(IRDFContainer rdfc) {
		return showConsole(new Shell(), rdfc);
	}

	private static SWTConsole showConsole(Shell s, IRDFContainer rdfc) {
		try {
			SWTConsole console;
			s.setText("Adenine Console");

			if("Linux".equals(System.getProperty("os.name"))){
				// Main Window Layout
				GridLayout layout = new GridLayout();
				layout.numColumns = 1;
				s.setLayout(layout);

				// Button Layout
				Button button = new Button(s, SWT.PUSH);
				button.setText("Line Feed");
				GridData gd = new GridData();
				gd.horizontalAlignment = GridData.END;
				button.setLayoutData(gd);

				// Console Layout
				final Composite comp = new Composite(s, SWT.NONE);
				comp.setLayout(new FillLayout());				
				gd = new GridData(GridData.FILL_BOTH);
				gd.widthHint = 640;
				gd.heightHint = 480;
				comp.setLayoutData(gd);
				
				final SWTConsole c = console = new SWTConsole(comp, rdfc);
				button.addSelectionListener (new SelectionAdapter(){
						public void widgetSelected(SelectionEvent e){
							c.t.append("\n");
							c.t.setCaretOffset(c.t.getText().length());
							c.t.showSelection();
							comp.setFocus();
						}});
				s.pack();
				comp.setFocus();
			}
			else{  
				s.setLayout(new FillLayout());
				console = new SWTConsole(s, rdfc);
			}
		    s.open();
			return console;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Displays an Adenine console in an SWT frame window.
	 */
	public static void main(String[] args) {
		try {
			LocalRDFContainer rdfc = new LocalRDFContainer();
			
			ICompiler compiler = new RDFCodeCompiler(rdfc);
			compiler.compile(
				null, 
				new InputStreamReader(CoreLoader.getResourceAsStream("/schemata/adenine.ad")),
				"/schemata/adenine.ad", 
				null,
				null
			);
			
			showConsole2(rdfc);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void showConsole2(IRDFContainer rdfc) {
		Display display = Display.getCurrent();
		if (display == null) {
			display = new Display();
		}
		Shell s = new Shell();
		showConsole(s, rdfc);
		while (!s.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	/**
	 * @author punya
	 * @return the styled text component of this console.
	 */
	public StyledText getTextBox() {
		return t;
	}
}
