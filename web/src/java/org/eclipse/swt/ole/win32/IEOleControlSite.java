package org.eclipse.swt.ole.win32;

import org.eclipse.swt.internal.ole.win32.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

/**
 *  Subclass of OleControlSite specifically for the Shell.Explorer
 *  control, to allow more control over context menus and such.
 * 
 *  @author Andrew Hogue
 */
public class IEOleControlSite extends OleControlSite {

    public static final String UUID_IDOC_HOST_UI_HANDLER          = "{BD3F23C0-D43E-11CF-893B-00AA00BDCE1A}";

    protected COMObject iDocHostUIHandler;
    protected MouseListener contextMenuListener;
    protected OleControlSite controlSite;

    public IEOleControlSite(Composite parent,
			    int style,
			    String progId,
			    MouseListener contextMenuListener) {
	super(parent, style, progId);
	this.controlSite = this; // small hack so the iDocHostUIHandler has access to this object for mouse events
	this.contextMenuListener = contextMenuListener;
    }
  
    protected void createCOMInterfaces() {
	super.createCOMInterfaces();
    
	iDocHostUIHandler = new COMObject(new int[] {2,0,0,4,1,5,0,0,1,1,1,3,3,2,2,1,3,2}) {
		public int method0(int[] args) {return QueryInterface(args[0], args[1]);}
		public int method1(int[] args) {return AddRef();}
		public int method2(int[] args) {return Release();}

		// override IE's default ShowContextMenu method and
		// send an event to the contextMenuListener
		public int method3(int[] args)  {
		    if (contextMenuListener != null) {
			POINT p = new POINT();
			OS.GetCursorPos(p);
			Event click = new Event();
			click.x = p.x;
			click.y = p.y;
			click.button = 3;
			click.stateMask = 0;
			click.widget = controlSite;
			contextMenuListener.mouseDown(new MouseEvent(click));
		    }
		    
		    // return COM.S_OK to disable IE's default context menu
		    return COM.S_OK;
		}
		// method4  GetHostInfo - not implemented
		// method5  ShowUI - not implemented
		// method6  HideUI - not implemented
		// method7  UpdateUI - not implemented
		// method8  EnableModeless - not implemented
		// method9  OnDocWindowActivate - not implemented
		// method10 OnFrameWindowActivate - not implemented
		// method11 ResizeBorder - not implemented
		// method12 TranslateAccelerator - not implemented
		// method13 GetOptionKeyPath - not implemented
		// method14 GetDropTarget - not implemented
		// method15 GetExternal - not implemented
		// method16 TranslateUrl - not implemented
		// method17 FilterDataObject - not implemented
	    };
    }

    protected void disposeCOMInterfaces() {
	super.disposeCOMInterfaces();
	if (iDocHostUIHandler != null) {
	    iDocHostUIHandler.dispose();
	    iDocHostUIHandler = null;
	}
    }

    protected int QueryInterface(int riid, int ppvObject) {
	int result = super.QueryInterface(riid, ppvObject);
	if (result == COM.S_OK) {
	    return result;
	}
	if (riid == 0 || ppvObject == 0) {
	    return COM.E_INVALIDARG;
	}
	GUID guid = new GUID();
	COM.MoveMemory(guid, riid, GUID.sizeof);
	if (COM.IsEqualGUID(guid, COMObject.IIDFromString(UUID_IDOC_HOST_UI_HANDLER))) {
	    COM.MoveMemory(ppvObject, new int[] {iDocHostUIHandler.getAddress()}, 4);
	    AddRef();
	    return COM.S_OK;
	}
	COM.MoveMemory(ppvObject, new int[] {0}, 4);
	return COM.E_NOINTERFACE;
    }
}
