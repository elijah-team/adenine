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

package edu.mit.lcs.haystack.ozone.core;

import edu.mit.lcs.haystack.rdf.*;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import java.io.*;

/**
 * @author David Huynh
 */
public class PossibleResourceListTransfer extends ByteArrayTransfer {

	private static final String TYPE_NAME = "haystack-possible-resource-list-transfer-format";
	private static final int TYPEID = registerType(TYPE_NAME);

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PossibleResourceListTransfer.class);

	/**
	 * Singleton instance.
	 */
	private static PossibleResourceListTransfer s_instance = new PossibleResourceListTransfer();
	
	/**
	 * Creates a new transfer object.
	 */
	private PossibleResourceListTransfer() {
		super();
	}
	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static PossibleResourceListTransfer getInstance () {
		return s_instance;
	}
	
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
	}
	
	protected void javaToNative (Object data, TransferData transferData){
		java.util.List resources = (java.util.List) data;
		
		if (data == null) return;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);
			
			java.util.Iterator i = resources.iterator();
			while (i.hasNext()) {
				dataOut.writeUTF(((Resource) i.next()).getContent());
			}
			dataOut.close();
			
			super.javaToNative(out.toByteArray(), transferData);
		} catch (IOException e) {
			s_logger.error("Failed to convert d&d resource data from java to native", e);
		}	
	}
	protected Object nativeToJava(TransferData transferData) {
		java.util.List resources = new java.util.LinkedList();
		
		try {
			byte[] 				bytes = (byte[]) super.nativeToJava(transferData);
			ByteArrayInputStream 	in = new ByteArrayInputStream(bytes);
			DataInputStream 		dataIn = new DataInputStream(in);
			
			while (true) {
				String uri = dataIn.readUTF();
				resources.add(new Resource(uri));
			}
		} catch (EOFException e) {
		} catch (IOException e) {
			s_logger.error("Failed to convert d&d resource data from native to java", e);
		}
		return resources;
	}
}
