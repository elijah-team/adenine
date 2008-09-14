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

package edu.mit.lcs.haystack.server.extensions.wrapperinduction;


/**
 *  Utilities for dealing with arrays
 *
 *  @author Andrew Hogue
 */

public class ArrayUtils {

    public static int[] shift(int[] array, int toShift) {
	int[] newArray = new int[array.length+1];
	newArray[0] = toShift;
	for (int i = 0; i < array.length; i++) {
	    newArray[i+1] = array[i];
	}
	return newArray;
    }

    public static String[] shift(String[] array, String toShift) {
	String[] newArray = new String[array.length+1];
	newArray[0] = toShift;
	for (int i = 0; i < array.length; i++) {
	    newArray[i+1] = array[i];
	}
	return newArray;
    }

    public static Object[] shift(Object[] array, Object toShift) {
	Object[] newArray = new Object[array.length+1];
	newArray[0] = toShift;
	for (int i = 0; i < array.length; i++) {
	    newArray[i+1] = array[i];
	}
	return newArray;
    }

    public static Object[] shift(Object[] array, Object[] toShift) {
	Object[] newArray = new Object[array.length+toShift.length];
	for (int i = 0; i < toShift.length; i++) {
	    newArray[i] = toShift[i];
	}
	for (int i = 0; i < array.length; i++) {
	    newArray[i+toShift.length] = array[i];
	}
	return newArray;
    }

    public static int[] push(int[] array, int toPush) {
	int[] newArray = new int[array.length+1];
	for (int i = 0; i < array.length; i++) {
	    newArray[i] = array[i];
	}
	newArray[array.length] = toPush;
	return newArray;
    }

    public static String[] push(String[] array, String toPush) {
	String[] newArray = new String[array.length+1];
	for (int i = 0; i < array.length; i++) {
	    newArray[i] = array[i];
	}
	newArray[array.length] = toPush;
	return newArray;
    }

    public static Object[] push(Object[] array, Object toPush) {
	Object[] newArray = new Object[array.length+1];
	for (int i = 0; i < array.length; i++) {
	    newArray[i] = array[i];
	}
	newArray[array.length] = toPush;
	return newArray;
    }

    public static String join(String joiner, String[] toJoin) {
	if (toJoin.length <= 0) {
	    return "";
	}
	if (toJoin.length == 1) {
	    return toJoin[0];
	}
	   
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < toJoin.length-1; i++) {
	    out.append(toJoin[i]);
	    out.append(joiner);
	}
	out.append(toJoin[toJoin.length-1]);
	return out.toString();
    }

    public static String join(String joiner, int[] toJoin) {
	if (toJoin.length <= 0) {
	    return "";
	}
	if (toJoin.length == 1) {
	    return String.valueOf(toJoin[0]);
	}
	   
	StringBuffer out = new StringBuffer();
	for (int i = 0; i < toJoin.length-1; i++) {
	    out.append(toJoin[i]);
	    out.append(joiner);
	}
	out.append(toJoin[toJoin.length-1]);
	return out.toString();
    }


    public static int[] split(String joiner, String toSplit) {
	return split(joiner, toSplit, true);
    }

    public static int[] split(String joiner, String toSplit, boolean escapeJoiner) {
	if (toSplit == null || toSplit.equals("")) return new int[0];

	int[] split = null;
	try {
	    String[] splitStrings = toSplit.split(escapeJoiner(joiner));
	    
	    split = new int[splitStrings.length];
	    for (int i = 0; i < splitStrings.length; i++) {
		split[i] = Integer.parseInt(splitStrings[i]);
	    }
	}
	catch (NumberFormatException e) {
	    e.printStackTrace();
	}
	    
	return split;
    }

    /**
     *  Escapes characters in a regular-expression that have specific
     *  meanings
     */
    protected static String escapeJoiner(String joiner) {
	if (joiner == null) return null;
	if (joiner.equals("."))
	    return "\\.";
	return joiner;
    }

    /**
     *  Returns a shallow slice of the given array, inclusive.
     */
    public static Object[] slice(Object[] in, int fromIndex, int toIndex) {
	if (toIndex < fromIndex ||
	    fromIndex < 0 ||
	    toIndex >= in.length)  return new Object[0];

	Object[] out = new Object[toIndex-fromIndex+1];
	for (int i = fromIndex; i <= toIndex; i++) {
	    out[i-fromIndex] = in[i];
	}
	
	return out;
    }

    public static String toString(Object[] in) {
	StringBuffer out = new StringBuffer();
	out.append("{");
	for (int i = 0; i < in.length; i++) {
	    out.append(in[i]);
	    if (i != in.length-1)
		out.append(",");
	}
	out.append("}");
	return out.toString();
    }

}
