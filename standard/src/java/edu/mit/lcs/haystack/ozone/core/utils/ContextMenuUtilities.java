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

package edu.mit.lcs.haystack.ozone.core.utils;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;
import edu.mit.lcs.haystack.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.ozone.standard.widgets.parts.PartConstants;
import edu.mit.lcs.haystack.ozone.verbs.*;
import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.adenine.*;
import edu.mit.lcs.haystack.adenine.interpreter.*;
import java.util.*;
import java.net.*;

/**
 * @version 	1.0
 * @author		David Huynh
 */
public class ContextMenuUtilities {
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ContextMenuUtilities.class);
	
	static public int constructMenuForUnderlying(
		IRDFContainer	source,
		Context 		context,
		Menu 			menu
	) {
		Resource resUnderlying = (Resource) context.getLocalProperty(OzoneConstants.s_underlying);
		
		if (resUnderlying == null) {
			return 0;
		}
		
		Set results = null;
		try {
			results = source.query(
				new Statement[] {
					new Statement(resUnderlying, Constants.s_rdf_type, Utilities.generateWildcardResource(2)),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_verb_domain, Utilities.generateWildcardResource(2))
				},
				Utilities.generateWildcardResourceArray(1),
				Utilities.generateWildcardResourceArray(2)
			);
			results.addAll(
				source.query(
					new Statement[] {
						new Statement(Utilities.generateWildcardResource(1), Constants.s_verb_domain, Constants.s_daml_Thing)
					},
					Utilities.generateWildcardResourceArray(1),
					Utilities.generateWildcardResourceArray(1)
				)
			);
				
			if (results.isEmpty()) {
				return 0;
			}
		} catch (RDFException e) {
			return 0;
		}
				
		Iterator 	i = results.iterator();
		int			count = 0;
		while (i.hasNext()) {
			Resource 	resVerb = (Resource) ((RDFNode[]) i.next())[0];
			Context		childContext = new Context(context);
			
			childContext.putLocalProperty(OzoneConstants.s_underlying, resUnderlying);
			childContext.putLocalProperty(OzoneConstants.s_part, resVerb);
			
			if (addVerb(source, childContext, menu, resVerb, resUnderlying)) {
				count++;
			}
		}
		return count;
	}
	
	static public int constructMenuForPartData(
		IRDFContainer	source,
		Context			context,
		Menu			menu
	) {
		Resource resPartData = (Resource) context.getLocalProperty(OzoneConstants.s_partData);
		
		if (resPartData == null) {
			return 0;
		}
		
		Set results = null;
		try {
			results = source.query(
				new Statement[] {
					new Statement(resPartData, Constants.s_rdf_type, Utilities.generateWildcardResource(2)),
					new Statement(Utilities.generateWildcardResource(1), Constants.s_verb_partDomain, Utilities.generateWildcardResource(2))
				},
				Utilities.generateWildcardResourceArray(1),
				Utilities.generateWildcardResourceArray(2)
			);
			if (results.isEmpty()) {
				return 0;
			}
		} catch (RDFException e) {
			return 0;
		}
				
		Iterator	i = results.iterator();
		int			count = 0;
		while (i.hasNext()) {
			Resource 	resVerb = (Resource) ((RDFNode[]) i.next())[0];
			Context		childContext = new Context(context);
			
			childContext.putLocalProperty(OzoneConstants.s_partData, resPartData);
			childContext.putLocalProperty(OzoneConstants.s_part, resVerb);
			
			if (addVerb(source, childContext, menu, resVerb, resPartData)) {
				count++;
			}
		}
		return count;
	}
	
	static public void constructMenu(
		IRDFContainer	source,
		Context			context,
		Menu			menu
	) {
		if (constructMenuForUnderlying(source, context, menu) > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}
		if (constructMenuForPartData(source, context, menu) > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}
		
		MenuItem	itemMore = new MenuItem(menu, SWT.CASCADE);
		Menu 		subMenuMore = new Menu(itemMore);
		
		itemMore.setText("More");
		
		Context		parentContext;
		
		parentContext = context;
		while (true) {
			parentContext = parentContext.getParentContext();
			if (parentContext == null) {
				break;
			}
			
			Resource resUnderlying = (Resource) parentContext.getLocalProperty(OzoneConstants.s_underlying);
			if (resUnderlying != null) {
				String 	title = Utilities.getLiteralProperty(resUnderlying, Constants.s_dc_title, source);
				
				if (title == null) {
					title = Utilities.getLiteralProperty(resUnderlying, Constants.s_rdfs_label, source);
				}
				if (title == null) {
					title = resUnderlying.getURI();
				}

				MenuItem	subItem = new MenuItem(subMenuMore, SWT.CASCADE);
				Menu 		subMenu = new Menu(subItem);
				
				if (title.length() < 30) {
					subItem.setText(title);
				} else {
					subItem.setText(title.substring(30) + "...");
				}
				
				if (constructMenuForUnderlying(source, parentContext, subMenu) == 0) {
					subMenu.dispose();
					subItem.dispose();
				}
			}
		}
		
		if (subMenuMore.getItemCount() > 0) {
			new MenuItem(subMenuMore, SWT.SEPARATOR);
		}

		parentContext = context;
		while (true) {
			parentContext = parentContext.getParentContext();
			if (parentContext == null) {
				break;
			}
			
			Resource resPartData = (Resource) parentContext.getLocalProperty(OzoneConstants.s_partData);
			if (resPartData != null) {
				String 	title = Utilities.getLiteralProperty(resPartData, Constants.s_dc_title, source);
				
				if (title == null) {
					title = Utilities.getLiteralProperty(resPartData, Constants.s_rdfs_label, source);
				}
				if (title == null) {
					title = resPartData.getURI();
				}

				MenuItem	subItem = new MenuItem(subMenuMore, SWT.CASCADE);
				Menu 		subMenu = new Menu(subItem);
				
				if (title.length() < 30) {
					subItem.setText(title);
				} else {
					subItem.setText(title.substring(30) + "...");
				}
				
				if (constructMenuForPartData(source, parentContext, subMenu) == 0) {
					subMenu.dispose();
					subItem.dispose();
				}
			}
		}
		
		if (subMenuMore.getItemCount() == 0) {
			itemMore.setEnabled(false);
		}
	}
	
	static private boolean addVerb(
		IRDFContainer 	source, 
		Context 		context, 
		Menu 			menu, 
		Resource 		resVerb,
		Resource		res
	) {
		Resource 	titleGenerator = Utilities.getResourceProperty(resVerb, Constants.s_verb_titleGenerator, source);
		String		title = null;
		
		if (titleGenerator != null) {
			Interpreter i = Ozone.getInterpreter();
			DynamicEnvironment denv = new DynamicEnvironment(source);
			Ozone.initializeDynamicEnvironment(denv, context);
			
			try {
				title = (String) i.callMethod(titleGenerator, new Object[] { res, context }, denv);
			} catch (AdenineException e) {
			}
		} 		
		if (title == null) {
			title = Utilities.getLiteralProperty(resVerb, Constants.s_dc_title, source);
		}		
		
		if (title != null) {
			MenuItem 	item = new MenuItem(menu, 0);
			
			item.setText(title);
			
			Resource	resImage = Utilities.getResourceProperty(resVerb, PartConstants.s_icon, source);
			if (resImage != null) {
				try {
					Image image = new Image(Ozone.s_display, new URL(resImage.getURI()).openStream());
					
					image.setBackground(Ozone.s_display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				
					item.setImage(image);
				} catch (java.io.IOException e) {
					s_logger.error("Failed to open image " + resImage.getURI());
				}
			}
			
			item.addSelectionListener(new VerbHandler(source, context, resVerb));
			
			return true;
		}
		
		return false;
	}	
}

class VerbHandler extends SelectionAdapter {
	IRDFContainer	m_source;
	Context			m_context;
	Resource		m_resVerb;		
	
	VerbHandler(IRDFContainer source, Context context, Resource resVerb) {
		m_source = source;
		m_context = context;
		m_resVerb = resVerb;
	}
	
	public void widgetSelected(SelectionEvent e) {
		try {
			Class c = Utilities.loadClass(m_resVerb, m_source);
			IVerb verb = (IVerb) c.newInstance();
			
			verb.initialize(m_source, m_context);
			verb.activate();
			verb.dispose();
		} catch (Exception ex) {
			ContextMenuUtilities.s_logger.error("Failed in verb handler", ex);
		}
	}
}

