package edu.mit.lcs.haystack.server.extensions.wrapperinduction.dom.ie;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.internal.ole.win32.COMObject;
import org.eclipse.swt.internal.ole.win32.GUID;
import org.eclipse.swt.internal.ole.win32.IDispatch;
import org.eclipse.swt.internal.ole.win32.TYPEATTR;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleFunctionDescription;
import org.eclipse.swt.ole.win32.OlePropertyDescription;
import org.eclipse.swt.ole.win32.Variant;

/**
 * A set of utilities for dealing with OLE controls
 * 
 * @version 1.0
 * @author Andrew Hogue
 */
public class OLEUtils {

	public static boolean DEBUG = false;
	public static boolean ERRORS = false;
	public static final String UUID_IHTML_DOCUMENT = "{626FC520-A41E-11CF-A731-00A0C9082637}";
	public static final String UUID_IHTML_DOCUMENT2 = "{332C4425-26CB-11D0-B483-00C04FD90119}";
	public static final String UUID_IHTML_DOCUMENT3 = "{3050F485-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_DOCUMENT5 = "{3050F80C-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_LOCATION = "{163BB1E0-6E00-11CF-837A-48DC04C10000}";
	public static final String UUID_IHTML_ELEMENT = "{3050F1FF-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_ELEMENT2 = "{3050F434-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_ELEMENT3 = "{3050F673-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_ELEMENT_COLLECTION = "{3050F21F-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_SELECTION_OBJECT = "{3050F25A-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_SELECTION_OBJECT_2 = "{3050F7EC-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_TXT_RANGE = "{3050F220-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_TXT_RANGE_COLLECTION = "{3050F7ED-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_TEXT_CONTAINER = "{3050F230-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_CONTROL_RANGE = "{3050F29C-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_CONTROL_RANGE_2 = "{3050F65E-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_STYLE = "{3050F25E-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_STYLE_SHEET = "{3050F2E3-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_DOM_NODE = "{3050F5DA-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_DOM_TEXT_NODE = "{3050F4B1-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_DOM_CHILDREN_COLLECTION = "{3050F5AB-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_ATTRIBUTE_COLLECTION = "{3050F4C3-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_STYLE_SHEETS_COLLECTION = "{3050F37E-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IHTML_COMMENT_ELEMENT2 = "{3050F813-98B5-11CF-BB82-00AA00BDCE0B}";
	public static final String UUID_IDOC_HOST_UI_HANDLER = "{BD3F23C0-D43E-11CF-893B-00AA00BDCE1A}";
	public static final String UUID_ITEXT_RANGE = "{8CC497C2-A1DF-11CE-8098-00AA0047BE5D}";
	public static final String UUID_TEXT_RANGE = "{9149348F-5A91-11CF-8700-00AA0060263B}";

	/**
	 * Returns the Variant object for the given property on the given automation
	 */
	public static Variant getProperty(OleAutomation auto, String property) {

		if (DEBUG)
			System.out.println(">>> Getting property " + property);
		int[] rgdispid = auto.getIDsOfNames(new String[]{property});
		if(rgdispid == null)
			return null;
		int dispIdMember = rgdispid[0];
		if (DEBUG)
			System.out.println(">>> " + property + ": dispId = " + dispIdMember);
		Variant var = auto.getProperty(dispIdMember);
		if (var == null) {
			if (ERRORS)
				System.out.println(">>> Warning: variant is null!");
		}
		return var;
	}

	/**
	 * Sets the given property for the given automation
	 */
	public static void setProperty(OleAutomation auto, String property,
			Variant value) {

		try {
			int[] rgdispid = auto.getIDsOfNames(new String[]{property});
			int dispIdMember = rgdispid[0];
			auto.setProperty(dispIdMember, value);
		} catch (Exception e) {
			System.out.println("Exception in OLEUtils.setProperty(): " + e);
			e.printStackTrace();
		}
	}

	/**
	 * Invokes a command on the given automation
	 */
	public static Variant invokeCommand(OleAutomation auto, String commandName,
			Variant[] args) {

		//System.err.println("commandName:" + commandName);
		int[] rgdispid = auto.getIDsOfNames(new String[]{commandName});
		//System.err.println("rgdispid:" + rgdispid.length);
		int cmdId = 0;
		if(rgdispid != null)
			cmdId = rgdispid[0];
		else
			return null;
		if (DEBUG)
			System.out.println(">>> command: " + commandName + " cmdID: "
					+ cmdId);
		//System.err.println("BEFORE auto.invoke");
		Variant retVar = auto.invoke(cmdId, args);
		//System.err.println("AFTER auto.invoke");
		return retVar;
	}

	/**
	 * Invokes a command on the given automation
	 */
	public static Variant invokeCommand(OleAutomation auto, String commandName,
			String[] argNames, Variant[] args) {

		int[] rgdispid = auto.getIDsOfNames(new String[]{commandName});
		int cmdId = rgdispid[0];
		rgdispid = auto.getIDsOfNames(argNames);
		Variant retVar = auto.invoke(cmdId, args, rgdispid);
		return retVar;
	}

	/**
	 * Given an automation, retrieves a property and uses QueryInterface to
	 * switch the interface and return a new automation with the new interface.
	 * 
	 * @param auto
	 *            The automation from which to retrieve the property
	 * @param propertyName
	 *            The name of the property in 'auto'
	 * @param UUID
	 *            The UUID of the interface to use
	 */
	public static OleAutomation getPropertyInterface(OleAutomation auto,
			String propertyName, String UUID) {

		try {
			Variant varProp = getProperty(auto, propertyName);
			if (varProp == null) {
				if (ERRORS)
					System.out.println(">>> varProp is null");
				return null;
			}
			return getInterface(varProp, UUID);
		} catch (Throwable e) {
			System.out.println("%%%%% Exception in getPropertyInterface(): "
					+ e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Given an OleAutomation of one type, retrieves it as a new type using
	 * QueryInterface
	 */
	public static OleAutomation getInterface(OleAutomation auto, String UUID) {

		Variant varAuto = new Variant(auto);
		IDispatch dispatch = varAuto.getDispatch();
		varAuto.dispose();
		if (dispatch == null) {
			if (ERRORS)
				System.out.println("<><><><><> DISPATCH IS NULL");
			return null;
		}
		OleAutomation retAuto = null;
		try {
			int[] ppv = new int[1];
			GUID guid = COMObject.IIDFromString(UUID);
			int result = dispatch.QueryInterface(guid, ppv);
			if (result == COM.S_OK) {
				Variant varResult = new Variant(new IDispatch(ppv[0]));
				if (varResult == null) {
					if (ERRORS)
						System.out.println(">>> varResult is null!");
					return null;
				}
				retAuto = varResult.getAutomation();
				if (retAuto == null) {
					if (ERRORS)
						System.out.println(">>> retAuto OleAutomation is null!");
				}
				varResult.dispose();
			} else {
				if (ERRORS)
					System.out.println(">>> NOT COM.S_OK !!! => " + result);
			}
		} catch (Throwable e) {
			System.out.println("%%%%% Exception in getInterface(): " + e);
		}
		return retAuto;
	}

	/**
	 * Given a Variant containing an automation, retrieves the automation with
	 * the given interface.
	 */
	public static OleAutomation getInterface(Variant varAuto, String UUID) {

		if (DEBUG)
			System.out.println(">>> entering getInterface()");
		IDispatch dispatch = null;
		try {
			dispatch = varAuto.getDispatch();
		} catch (Throwable e) {
			if (ERRORS)
				System.out.println("### Exception: " + e);
			return null;
		}
		if (dispatch == null) {
			if (ERRORS)
				System.out.println(">>> dispatch is null");
			return null;
		}
		OleAutomation retAuto = null;
		try {
			int[] ppv = new int[1];
			GUID guid = COMObject.IIDFromString(UUID);
			int result = dispatch.QueryInterface(guid, ppv);
			if (result == COM.S_OK) {
				Variant varResult = new Variant(new IDispatch(ppv[0]));
				if (varResult == null) {
					if (ERRORS)
						System.out.println(">>> varResult is null!");
					return null;
				}
				retAuto = varResult.getAutomation();
				if (retAuto == null) {
					if (ERRORS)
						System.out.println(">>> retAuto OleAutomation is null!");
				}
				varResult.dispose();
			} else {
				if (ERRORS)
					System.out.println(">>> NOT COM.S_OK !!! " + result
							+ ", interface " + UUID);
			}
		} catch (Throwable e) {
			System.out.println("%%%%% Exception in getInterface(): " + e);
		}
		return retAuto;
	}

	private static String getTypeName(int type) {

		switch (type) {
			case OLE.VT_BOOL :
				return "boolean";
			case OLE.VT_R4 :
				return "float";
			case OLE.VT_R8 :
				return "double";
			case OLE.VT_I4 :
				return "int";
			case OLE.VT_DISPATCH :
				return "IDispatch";
			case OLE.VT_UNKNOWN :
				return "IUnknown";
			case OLE.VT_I2 :
				return "short";
			case OLE.VT_BSTR :
				return "String";
			case OLE.VT_VARIANT :
				return "Variant";
			case OLE.VT_CY :
				return "Currency";
			case OLE.VT_DATE :
				return "Date";
			case OLE.VT_UI1 :
				return "unsigned char";
			case OLE.VT_UI4 :
				return "unsigned int";
			case OLE.VT_USERDEFINED :
				return "UserDefined";
			case OLE.VT_HRESULT :
				return "int";
			case OLE.VT_VOID :
				return "void";
			case OLE.VT_BYREF | OLE.VT_BOOL :
				return "boolean *";
			case OLE.VT_BYREF | OLE.VT_R4 :
				return "float *";
			case OLE.VT_BYREF | OLE.VT_R8 :
				return "double *";
			case OLE.VT_BYREF | OLE.VT_I4 :
				return "int *";
			case OLE.VT_BYREF | OLE.VT_DISPATCH :
				return "IDispatch *";
			case OLE.VT_BYREF | OLE.VT_UNKNOWN :
				return "IUnknown *";
			case OLE.VT_BYREF | OLE.VT_I2 :
				return "short *";
			case OLE.VT_BYREF | OLE.VT_BSTR :
				return "String *";
			case OLE.VT_BYREF | OLE.VT_VARIANT :
				return "Variant *";
			case OLE.VT_BYREF | OLE.VT_CY :
				return "Currency *";
			case OLE.VT_BYREF | OLE.VT_DATE :
				return "Date *";
			case OLE.VT_BYREF | OLE.VT_UI1 :
				return "unsigned char *";
			case OLE.VT_BYREF | OLE.VT_UI4 :
				return "unsigned int *";
			case OLE.VT_BYREF | OLE.VT_USERDEFINED :
				return "UserDefined *";
		}
		return "unknown " + type;
	}

	private static String getDirection(int direction) {

		String dirString = "";
		boolean comma = false;
		if ((direction & OLE.IDLFLAG_FIN) != 0) {
			dirString += "in";
			comma = true;
		}
		if ((direction & OLE.IDLFLAG_FOUT) != 0) {
			if (comma)
				dirString += ", ";
			dirString += "out";
			comma = true;
		}
		if ((direction & OLE.IDLFLAG_FLCID) != 0) {
			if (comma)
				dirString += ", ";
			dirString += "lcid";
			comma = true;
		}
		if ((direction & OLE.IDLFLAG_FRETVAL) != 0) {
			if (comma)
				dirString += ", ";
			dirString += "retval";
		}
		return dirString;
	}

	private static String getInvokeKind(int invKind) {

		switch (invKind) {
			case OLE.INVOKE_FUNC :
				return "METHOD";
			case OLE.INVOKE_PROPERTYGET :
				return "PROPERTY GET";
			case OLE.INVOKE_PROPERTYPUT :
				return "PROPERTY PUT";
			case OLE.INVOKE_PROPERTYPUTREF :
				return "PROPERTY PUT BY REF";
		}
		return "unknown " + invKind;
	}

	/**
	 * Prints function and property descriptions for an OleAutomation, to a file
	 * if specified, else to STDOUT.
	 */
	public static void printDescription(OleAutomation auto) {

		printDescription(auto, null);
	}

	public static void printDescription(OleAutomation auto, String filename) {

		try {
			BufferedWriter out = null;
			if (filename != null) {
				out = new BufferedWriter(new FileWriter(filename));
			} else {
				out = new BufferedWriter(new OutputStreamWriter(System.out));
			}
			TYPEATTR typeattr = auto.getTypeInfoAttributes();
			if (typeattr != null) {
				if (typeattr.cFuncs > 0)
					out.write("\n\nFunctions:\n\n");
				for (int i = 0; i < typeattr.cFuncs; i++) {
					OleFunctionDescription data = auto.getFunctionDescription(i);
					String argList = "";
					int firstOptionalArgIndex = data.args.length
							- data.optionalArgCount;
					for (int j = 0; j < data.args.length; j++) {
						argList += "[";
						if (j >= firstOptionalArgIndex)
							argList += "optional, ";
						argList += getDirection(data.args[j].flags) + "] "
								+ getTypeName(data.args[j].type) + " "
								+ data.args[j].name;
						if (j < data.args.length - 1)
							argList += ", ";
					}
					out.write(getInvokeKind(data.invokeKind) + " (id = "
							+ data.id + ") : " + "\n\tSignature   : "
							+ getTypeName(data.returnType) + " " + data.name
							+ "(" + argList + ")" + "\n\tDescription : "
							+ data.documentation + "\n\tHelp File   : "
							+ data.helpFile + "\n\n");
				}
				if (typeattr.cVars > 0)
					out.write("\n\nVariables:\n\n");
				for (int i = 0; i < typeattr.cVars; i++) {
					OlePropertyDescription data = auto.getPropertyDescription(i);
					out.write("PROPERTY (id = " + data.id + ") :"
							+ "\n\tName : " + data.name + "\n\tType : "
							+ getTypeName(data.type) + "\n\n");
				}
			}
		} catch (IOException e) {
			System.out.println("Error writing to file: " + e);
		}
	}
	
	public static String readSafeArray(Variant varSafeArray) {
		int CodePage = OS.GetACP();
		StringBuffer returnString = new StringBuffer();
		int pPostData = varSafeArray.getByRef();
		short[] vt_type = new short[1];
		OS.MoveMemory(vt_type, pPostData, 2);
		if(vt_type[0] == (short) (OLE.VT_BYREF | OLE.VT_VARIANT)) {
			int[] pVariant = new int[1];
			OS.MoveMemory(pVariant, pPostData + 8, 4);
			vt_type = new short[1];
			OS.MoveMemory(vt_type, pVariant[0], 2);
			if(vt_type[0] == (short) (OLE.VT_ARRAY | OLE.VT_UI1)) {
				int[] pSafearray = new int[1];
				OS.MoveMemory(pSafearray, pVariant[0] + 8, 4);
				short[] cDims = new short[1];
				OS.MoveMemory(cDims, pSafearray[0], 2);
				int[] pvData = new int[1];
				OS.MoveMemory(pvData, pSafearray[0] + 12, 4);
				int offset = 0;
				int safearrayboundOffset = 0;
				for(int i = 0; i < cDims[0]; i++) {
					int[] cElements = new int[1];
					OS.MoveMemory(cElements, pSafearray[0] + 16 + safearrayboundOffset, 4);
					safearrayboundOffset += 8;
					int cchWideChar = OS.MultiByteToWideChar (CodePage, OS.MB_PRECOMPOSED,  pvData[0], -1, null, 0);
					if (cchWideChar != 0) {
						char[] lpWideCharStr = new char [cchWideChar - 1];
						OS.MultiByteToWideChar (CodePage, OS.MB_PRECOMPOSED,  pvData[0], -1, lpWideCharStr, lpWideCharStr.length);
						returnString.append(new String(lpWideCharStr));
					}
					else {
						returnString = new StringBuffer();
						break;
					}
				}
			}
		}
		return returnString.toString();
    }
	
	public static Variant makeSafeArray (String string) {
		// SAFEARRAY looks like this:
		//      short cDims      // Count of dimensions in this array
		//      short fFeatures  // Flags used by the SafeArray
		//      int cbElements   // Size of an element of the array
		//      int cLocks       // Number of times the array has been locked without corresponding unlock
		//      int pvData       // Pointer to the data
		//      SAFEARRAYBOUND[] rgsabound // One bound for each dimension
		// SAFEARRAYBOUND looks like this:
		//      int cElements    // the number of elements in the dimension
		//      int lLbound      // the lower bound of the dimension 
		
		//step 1 - define cDims, fFeatures and cbElements
		int CodePage = OS.GetACP();
		short cDims = 1;
		short FADF_FIXEDSIZE = 0x10;
		short FADF_HAVEVARTYPE = 0x80;
		short fFeatures = (short)(FADF_FIXEDSIZE | FADF_HAVEVARTYPE);
		int cbElements = 1;
		//create a pointer and copy the data into it
		int count = string.length();
		char[] chars = new char[count + 1];
		string.getChars(0, count, chars, 0);
		int cchMultiByte = OS.WideCharToMultiByte(CodePage, 0, chars, -1, null, 0, null, null);
		if (cchMultiByte == 0) return null;
		int pvData = OS.GlobalAlloc(OS.GMEM_FIXED | OS.GMEM_ZEROINIT, cchMultiByte);
		OS.WideCharToMultiByte(CodePage, 0, chars, -1, pvData, cchMultiByte, null, null);
		int cElements1 = cchMultiByte;
		int lLbound1 = 0;
		//step 3 - create a safearray in memory
		// 12 bytes for cDims, fFeatures and cbElements + 4 bytes for pvData + number of dimensions * (size of safearraybound)
		int sizeofSafeArray = 12 + 4 + 1*8;
		int pSafeArray = OS.GlobalAlloc(OS.GMEM_FIXED | OS.GMEM_ZEROINIT, sizeofSafeArray);
		//step 4 copy all the data into the safe array
		int offset = 0;
		OS.MoveMemory(pSafeArray + offset, new short[] {cDims}, 2); offset += 2;
		OS.MoveMemory(pSafeArray + offset, new short[] {fFeatures}, 2); offset += 2;
		OS.MoveMemory(pSafeArray + offset, new int[] {cbElements}, 4); offset += 4;
		OS.MoveMemory(pSafeArray + offset, new int[] {0}, 4); offset += 4;
		OS.MoveMemory(pSafeArray + offset, new int[] {pvData}, 4); offset += 4;
		OS.MoveMemory(pSafeArray + offset, new int[] {cElements1}, 4); offset += 4;
		OS.MoveMemory(pSafeArray + offset, new int[] {lLbound1}, 4); offset += 4;
		//step 5 create a variant in memory to hold the safearray
		// VARIANT looks like this:
		// short vt
		// short wReserved1
		// short wReserved2
		// short wReserved3
		// int parray
		int pVariant = OS.GlobalAlloc(OS.GMEM_FIXED | OS.GMEM_ZEROINIT, Variant.sizeof);
		short vt = (short)(OLE.VT_ARRAY | OLE.VT_UI1);
		OS.MoveMemory(pVariant, new short[] {vt}, 2);
		OS.MoveMemory(pVariant + 8, new int[]{pSafeArray}, 4);
		//step 6 create a by ref variant
		Variant variantByRef = new Variant(pVariant, (short)(OLE.VT_BYREF | OLE.VT_VARIANT));
		return variantByRef;
	}
}
