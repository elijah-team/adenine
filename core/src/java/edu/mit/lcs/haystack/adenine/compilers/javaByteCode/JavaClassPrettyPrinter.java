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

package edu.mit.lcs.haystack.adenine.compilers.javaByteCode;

import edu.mit.lcs.haystack.HaystackException;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LineNumberGen;
import org.apache.bcel.generic.MethodGen;

import java.text.MessageFormat;
import java.util.Iterator;

/**
 * @author David Huynh
 */
public class JavaClassPrettyPrinter {
	public static void main(String[] argv) {
		try {
			System.out.println("class " + argv[0]);
			
			JavaClass 		clazz = Repository.lookupClass(argv[0]);
			ConstantPoolGen cp = new ConstantPoolGen(clazz.getConstantPool());
	
			if (true) {
				Field[]			fields = clazz.getFields();
				for (int i = 0; i < fields.length; i++) {
					FieldGen fg = new FieldGen(fields[i], cp);
					
					printField(fg);
				}
			}
			if (true) {
				Method[] 		methods = clazz.getMethods();
				for (int i = 0; i < methods.length; i++) {
					MethodGen mg = new MethodGen(methods[i], clazz.getClassName(), cp);
					
					printMethod(mg);
				}
			}
		} catch (Exception e) { HaystackException.uncaught(e); }
	}
	
	protected static void printField(FieldGen fg) {
		System.out.println("Field " + fg.getName() + ": " + fg.getSignature());
	}
	
	protected static void printMethod(MethodGen mg) {
		System.out.println("Method " + mg.getName() + ": " + mg.getSignature());
		
		ConstantPoolGen	cpg = mg.getConstantPool();
		ConstantPool	cp = cpg.getConstantPool();
		InstructionList il = mg.getInstructionList();
		Iterator		i = il.iterator();
		int			maxStack = mg.getMaxStack();
		StringBuffer	s = new StringBuffer(maxStack);
		LineNumberGen[]	lng = mg.getLineNumbers();
		
		int iCount = 1;
		
		while (i.hasNext()) {
			InstructionHandle 	ih = (InstructionHandle) i.next();
			Instruction			ins = ih.getInstruction();
			int				offset = ih.getPosition(); 
			
			String f = MessageFormat.format(
				"{0,number,00000}. {1,number,00000} ",
				new Object[] {
					new Integer(iCount++),
					new Integer(offset)
				}
			);
			
			if (!adjustStringBuffer(s, ins.produceStack(cpg) - ins.consumeStack(cpg))) {
				System.out.println("----stack underflow----");
			}
			System.out.println(f + s.toString() + ih.getInstruction().toString(cp));
		}
		
		System.out.println("\r\n\r\n");
	}
	
	protected static boolean adjustStringBuffer(StringBuffer s, int diff) {
		if (diff > 0) {
			while (diff > 0) {
				s.append(".   ");
				diff--;
			}
		} else if (diff < 0){
			diff = -diff;
			if (diff > s.length()) {
				return false;
			}
			s.delete(0, diff * 4);
		}
		return true;
	}
}
